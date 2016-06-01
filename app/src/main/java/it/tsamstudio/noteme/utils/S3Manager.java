package it.tsamstudio.noteme.utils;

import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.couchbase.lite.CouchbaseLiteException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scottyab.aescrypt.AESCrypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import it.tsamstudio.noteme.CouchbaseDB;
import it.tsamstudio.noteme.Nota;

/**
 * Created by damianogiusti on 31/05/16.
 */
public class S3Manager {

    private static final String TAG = "S3Manager";

    /**
     * Interfaccia che penso useremo per tornare indietro lo stato del trasferimento,
     * che ovviamente sarà asincrono.
     * TODO pensare bene come implementarla
     */
    public interface OnTransferListener {
        void onStart(int transferID);

        void onProgressChanged(int transferID, long bytesCurrent, long totalBytes);

        void onWaitingForNetwork(int transferID);

        void onFinish(int transferID);

        void onFailure(int transferID);

        void onError(int transferID, Exception e);
    }

    private static final String BUCKET_NAME = "tsac-its";
    private static final String BUCKET_DIR = "noteme/";
    private static final String BUCKET_NOTES_DIR = BUCKET_DIR + "notes/";
    private static final String BUCKET_IMAGES_DIR = BUCKET_DIR + "images/";
    private static final String BUCKET_AUDIO_DIR = BUCKET_DIR + "audio/";

    private static S3Manager instance;

    public static S3Manager getInstance() {
        if (instance == null) {
            instance = new S3Manager();
        }
        return instance;
    }

    private AmazonS3 amazonS3;
    private TransferUtility transferUtility;

    private S3Manager() {
        amazonS3 = new AmazonS3Client(new CognitoCachingCredentialsProvider(
                NoteMeApp.getInstance().getApplicationContext(),
                "eu-west-1:840029f3-c8ed-4720-8490-5e010e6875e9",
                Regions.EU_WEST_1
        ));

        transferUtility = new TransferUtility(amazonS3, NoteMeApp.getInstance().getApplicationContext());
    }

    /**
     * Metodo che carica una nota nel server remoto.
     *
     * @param nota Nota da caricare
     */
    public void uploadNota(Nota nota, OnTransferListener transferListener) throws IOException {
        if (transferListener == null) {
            // dummy init
            transferListener = dummyInitForListener();
        }
        final OnTransferListener listener = transferListener;

        List<File> noteFiles;
        noteFiles = CouchbaseDB.getInstance().getNoteFiles(nota.getID());

        for (File file : noteFiles) {
            String bucketDir = BUCKET_NOTES_DIR;
            if (file.getName().toLowerCase().endsWith(".jpg")
                    || file.getName().toLowerCase().endsWith(".jpeg")) {
                bucketDir = BUCKET_IMAGES_DIR;
            } else if (file.getName().endsWith(".3gp")) {
                bucketDir = BUCKET_AUDIO_DIR;
            }
            transferUtility.upload(BUCKET_NAME, bucketDir + file.getName(), file)
                    .setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (state == TransferState.IN_PROGRESS) {
                                listener.onStart(id);
                            } else if (state == TransferState.COMPLETED) {
                                listener.onFinish(id);
                            } else if (state == TransferState.FAILED) {
                                listener.onFailure(id);
                            } else if (state == TransferState.WAITING_FOR_NETWORK) {
                                listener.onWaitingForNetwork(id);
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            listener.onProgressChanged(id, bytesCurrent, bytesTotal);
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            listener.onError(id, ex);
                        }
                    });
        }
    }

    /**
     * Metodo che scarica tutte le note dal server remoto.
     *
     * @return Lista di note scaricate
     */
    public List<Nota> downloadAllNotes(OnTransferListener transferListener) {

        if (transferListener == null) {
            transferListener = dummyInitForListener();
        }
        final OnTransferListener listener = transferListener;

        new AsyncTask<Void, Void, Void>() {
            List<String> notesKeys;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                ObjectListing objectListing = amazonS3.listObjects(BUCKET_NAME, BUCKET_NOTES_DIR);
                List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();

                while (objectListing.isTruncated()) {
                    objectListing = amazonS3.listNextBatchOfObjects(objectListing);
                    summaries.addAll(objectListing.getObjectSummaries());
                }
                notesKeys = new ArrayList<>(summaries.size());
                for (S3ObjectSummary objectSummary : summaries) {
                    notesKeys.add(objectSummary.getKey());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (notesKeys != null) {
                    final List<Nota> notesList = new ArrayList<>(notesKeys.size());

                    final TransferUtility transferUtility = new TransferUtility(amazonS3, NoteMeApp.getInstance());

                    final Callback scaricamentoNoteFinito = new Callback() {
                        @Override
                        public void call(Object... args) {
                            // ho finito lo scaricamento delle note
                            // posso scaricarmi i file multimediali
                            ArrayList<Nota> notesList = (ArrayList<Nota>) args[0];
                            try {
                                CouchbaseDB.getInstance().salvaNote(notesList);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (CouchbaseLiteException e) {
                                e.printStackTrace();
                            }
                            for (Nota nota : notesList) {
                                if (nota.getAudio() != null) {
                                    File localFile = new File(nota.getAudio());
                                    transferUtility.download(BUCKET_NAME, BUCKET_AUDIO_DIR + localFile.getName(), localFile)
                                            .setTransferListener(new TransferListener() {
                                                @Override
                                                public void onStateChanged(int id, TransferState state) {
                                                    if (state == TransferState.COMPLETED) {
                                                        listener.onFinish(id);
                                                    } else if (state == TransferState.WAITING_FOR_NETWORK) {
                                                        listener.onWaitingForNetwork(id);
                                                    } else if (state == TransferState.FAILED) {
                                                        listener.onFailure(id);
                                                    }
                                                }

                                                @Override
                                                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                                    listener.onProgressChanged(id, bytesCurrent, bytesTotal);
                                                }

                                                @Override
                                                public void onError(int id, Exception ex) {
                                                    listener.onError(id, ex);
                                                }
                                            });
                                }
                                if (nota.getImage() != null) {
                                    File localFile = new File(nota.getImage());
                                    transferUtility.download(BUCKET_NAME, BUCKET_IMAGES_DIR + localFile.getName(), localFile)
                                            .setTransferListener(new TransferListener() {
                                                @Override
                                                public void onStateChanged(int id, TransferState state) {
                                                    if (state == TransferState.COMPLETED) {
                                                        listener.onFinish(id);
                                                    } else if (state == TransferState.WAITING_FOR_NETWORK) {
                                                        listener.onWaitingForNetwork(id);
                                                    } else if (state == TransferState.FAILED) {
                                                        listener.onFailure(id);
                                                    }
                                                }

                                                @Override
                                                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                                    listener.onProgressChanged(id, bytesCurrent, bytesTotal);
                                                }

                                                @Override
                                                public void onError(int id, Exception ex) {
                                                    listener.onError(id, ex);
                                                }
                                            });
                                }
                            }
                        }
                    };

                    for (String key : notesKeys) {
                        // ottengo un filename come questo: noteme/notes/c31c06b2-ac23-4c9d-9f3e-2701b159fd00.note
                        String filename = (key.split("/")[2]).replace(".note", "");
                        File tempFile = null;
                        try {
                            tempFile = File.createTempFile(filename, "");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (tempFile != null) {
                            final File file = tempFile;
                            transferUtility.download(BUCKET_NAME, key, tempFile)
                                    .setTransferListener(new TransferListener() {
                                        @Override
                                        public void onStateChanged(int id, TransferState state) {
                                            // se ho scaricato il file
                                            if (state == TransferState.COMPLETED) {
                                                BufferedReader reader = null;
                                                try {
                                                    // creo un reader dal file scaricato
                                                    reader = new BufferedReader(new FileReader(file));
                                                } catch (FileNotFoundException e) {
                                                    e.printStackTrace();
                                                }
                                                // se ho il reader
                                                if (reader != null) {
                                                    String encryptedNote = null;
                                                    try {
                                                        // leggo la nota criptata nel file
                                                        encryptedNote = reader.readLine();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    // se ho letto la nota
                                                    if (encryptedNote != null) {
                                                        String decryptedNote = null;
                                                        try {
                                                            // decripto la nota
                                                            decryptedNote = AESCrypt.decrypt(NoteMeUtils.AES_KEY, encryptedNote);
                                                        } catch (GeneralSecurityException e) {
                                                            e.printStackTrace();
                                                        }
                                                        // se ho decriptato la nota
                                                        if (decryptedNote != null) {
                                                            try {
                                                                // la aggiungo in lista
                                                                notesList.add((new ObjectMapper()).readValue(decryptedNote, Nota.class));
                                                                // quando il numero di note salvate
                                                                // è pari al numero di note in remoto
                                                                if (notesList.size() == notesKeys.size()) {
                                                                    // notifico la fine dello scaricamento delle note
                                                                    scaricamentoNoteFinito.call(notesList);
                                                                }
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                }
                                                // se ho un errore lo mando su
                                            } else if (state == TransferState.FAILED) {
                                                listener.onFailure(id);
                                            }
                                        }

                                        @Override
                                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

                                        }

                                        @Override
                                        public void onError(int id, Exception ex) {
                                            listener.onError(id, ex);
                                        }
                                    });
                        }

                    }
                }
            }
        }.execute();

        return null;
    }

    /**
     * Metodo che sincronizza tutte le note locali con quelle in remoto, scaricando le note remote
     * non presenti in locale, e caricando le note locali non presenti in remoto.
     * Si appoggia al database locale.
     *
     * @return List di Nota contenente tutte le note
     */
    public List<Nota> sync() {
        // TODO
        return null;
    }

    private OnTransferListener dummyInitForListener() {
        return new OnTransferListener() {
            @Override
            public void onStart(int transferID) {

            }

            @Override
            public void onProgressChanged(int transferID, long bytesCurrent, long totalBytes) {

            }

            @Override
            public void onWaitingForNetwork(int transferID) {

            }

            @Override
            public void onFinish(int transferID) {

            }

            @Override
            public void onFailure(int transferID) {

            }

            @Override
            public void onError(int transferID, Exception e) {

            }
        };
    }
}
