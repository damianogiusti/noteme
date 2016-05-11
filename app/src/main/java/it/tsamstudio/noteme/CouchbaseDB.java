package it.tsamstudio.noteme;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;

/**
 * Created by damiano on 11/05/16.
 */
public class CouchbaseDB {
    private static final String TAG = "CouchbaseDB";
    private static final String TYPE_KEY = "type";
    private static final String DB_NAME = "noteme";

    private Manager man;
    private Database db;
    private Context ctx;

    public CouchbaseDB(Context c) {
        ctx = c;
        createManager();
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
                Log.d(TAG, "Database creato\n");


            } catch (CouchbaseLiteException e) {
                Log.d(TAG, "Impossibile accedere al database\n");
                e.printStackTrace();
            }
        }
    }

    /**
     * Salva una singola nota nel database
     * @param nota
     */
    public void salvaNota(Nota nota) {

    }
}
