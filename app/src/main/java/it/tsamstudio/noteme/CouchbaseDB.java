package it.tsamstudio.noteme;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.tsamstudio.noteme.utils.NoteMeApp;

/**
 * Created by damiano on 11/05/16.
 */
public class CouchbaseDB {
    private static final String TAG = "CouchbaseDB";
    private static final String TYPE_KEY = "type";
    private static final String DB_NAME = "noteme";

    private static final String VIEW_NOTE = "viewNote";

    private Manager man;
    private Database db;
    private Context ctx;

    public CouchbaseDB(Context c) {
        ctx = c;
        createManager();
        createViewForNota();
    }

    /**
     * Crea il database manager
     */
    private void createManager() {
        try {
            man = new Manager(new AndroidContext(ctx), Manager.DEFAULT_OPTIONS);
            Log.d(TAG, "Manager Creato\n");
        } catch (IOException e) {
            Log.d(TAG, "Impossibile creare l'oggetto Manager");
            e.printStackTrace();
        }
        if (!Manager.isValidDatabaseName(DB_NAME)) {
            Log.d(TAG, "Nome del Database errato");

        } else {
            try {
                DatabaseOptions options = new DatabaseOptions();
                options.setCreate(true);
                db = man.getDatabase(DB_NAME);
                //db = man.openDatabase(DB_NAME, options);
                Log.d(TAG, "Database creato");


            } catch (CouchbaseLiteException e) {
                Log.d(TAG, "Impossibile accedere al database");
                e.printStackTrace();
            }
        }
    }

    private void createViewForNota() {
        View view = db.getView(VIEW_NOTE);
        view.setMap(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (document.containsKey(TYPE_KEY) &&
                        document.get(TYPE_KEY).equals(Nota.class.getName())) {
                    emitter.emit(Nota.class.getName(), document.get(Nota.class.getName()));
                }
            }
        }, "1");
    }

    /**
     * Salva una singola nota nel database
     *
     * @param nota
     * @throws IOException
     * @throws CouchbaseLiteException
     */
    public void salvaNota(Nota nota) throws IOException, CouchbaseLiteException {
        Document document = db.getDocument(nota.getID());
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> documentProperties = document.getProperties();

        if (documentProperties != null)
            properties.putAll(documentProperties);

        ObjectMapper objectMapper = new ObjectMapper();
        String s = objectMapper.writeValueAsString(nota);
        properties.put(Nota.class.getName(), s);            // metto nelle properties una stringa json

        properties.put(TYPE_KEY, Nota.class.getName());
        document.putProperties(properties);
    }

    /**
     * Salva un ArrayList di oggetti di tipo Nota nel database
     *
     * @param note
     * @throws IOException
     * @throws CouchbaseLiteException
     */
    public void salvaNote(ArrayList<Nota> note) throws IOException, CouchbaseLiteException {
        long time = System.currentTimeMillis();
        for (Nota nota : note)
            salvaNota(nota);
        Log.d(TAG, String.format("note salvate in %s ms", System.currentTimeMillis() - time));
    }

    /**
     * Legge una nota dal database
     *
     * @param id guid della nota da leggere
     * @return nota letta
     */
    public Nota leggiNota(String id) {
        Document document = db.getExistingDocument(id);
        if (document == null) {
            Log.e(TAG, "leggiNota: documento non trovato per id=" + id);
            return null;
        }
        String jsonNota = (String) document.getProperty(Nota.class.getName());
        try {
            return (new ObjectMapper().readValue(jsonNota, Nota.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Restituisce una lista di File coinvolti nella Nota richiesta
     *
     * @param id GUID della nota da trovare
     * @throws IOException
     */
    public List<File> getNoteFiles(String id) throws IOException {
        ArrayList<File> files = new ArrayList<>(1);

        Nota nota = leggiNota(id);
        if (nota == null)
            return null;

        File file = new File(
                NoteMeApp.getInstance().getApplicationContext().getExternalFilesDir("jsonNotes")
                        + id);
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fileWriter = new FileWriter(file);
        ObjectMapper objectMapper = new ObjectMapper();
        fileWriter.write(objectMapper.writeValueAsString(nota));
        fileWriter.close();

        files.add(file);
        if (nota.getAudio() != null && !nota.getAudio().trim().equals(""))
            files.add(new File(nota.getAudio()));
        if (nota.getImage() != null && !nota.getImage().trim().equals(""))
            files.add(new File(nota.getImage()));

        return files;
    }

    /**
     * Legge le note memorizzate nel database
     *
     * @return
     * @throws CouchbaseLiteException
     * @throws IOException
     */
    public ArrayList<Nota> leggiNote() throws CouchbaseLiteException, IOException {
        long time = System.currentTimeMillis();
        Query query = db.getView(VIEW_NOTE).createQuery();
        query.setMapOnly(true);
        QueryEnumerator rows = query.run();

        ArrayList<Nota> note = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (QueryRow row : rows) {
            note.add(objectMapper.readValue(((String) row.getValue()), Nota.class));
        }
        Log.d(TAG, String.format("note lette in %s ms", System.currentTimeMillis() - time));
        Collections.sort(note, new Comparator<Nota>() {
            @Override
            public int compare(Nota lhs, Nota rhs) {
                return -1 * lhs.getCreationDate().compareTo(rhs.getCreationDate());
            }
        });
        return note;
    }

    public void eliminaNota(Nota n) throws CouchbaseLiteException {
        eliminaNota(n.getID());
    }

    public void eliminaNota(String guid) throws CouchbaseLiteException {
        Document document = db.getExistingDocument(guid);
        if (document != null) {
            try {
                Nota nota = (new ObjectMapper()).readValue((String) document.getProperty(Nota.class.getName()), Nota.class);
                if (nota.getAudio() != null) {
                    File fileAudio = new File(nota.getAudio());
                    fileAudio.delete();
                }
                if (nota.getImage() != null) {
                    File fileImmagine = new File(nota.getImage());
                    fileImmagine.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            document.delete();
        }
    }
}
