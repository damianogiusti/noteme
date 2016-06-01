package it.tsamstudio.noteme.utils;

import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
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

    public static final int TRANSFER_UPLOAD = 1;
    public static final int TRANSFER_DOWNLOAD = 2;

    public interface MultipleTransferListener {
        void onFileTransferred(File file, int transferType);

        void onFileTransferFailed(File file, int transferType, Exception e);

        void onFilesProgressChanged(int currentFile, int totalFiles, int transferType);

        void onSingleFileProgressChanged(File file, long currentBytes, long totalBytes, int transferType);

        void onFinish(int transferType);
    }

    public interface SyncListener {
        //        void onLocalToRemoteSyncCompleted();
//        void onRemoteToLocalSyncCompleted();
        void onSyncStarted();

        void onSyncFinished();
    }

    public interface DeletionListener {
        void onDeleted(Nota nota);
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
    private boolean upSyncCompleted = true;
    private boolean downSyncCompleted = true;

    private S3Manager() {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                NoteMeApp.getInstance().getApplicationContext(),
                "eu-west-1:840029f3-c8ed-4720-8490-5e010e6875e9",
                Regions.EU_WEST_1
        );
        amazonS3 = new AmazonS3Client(credentialsProvider);

        transferUtility = new TransferUtility(amazonS3, NoteMeApp.getInstance().getApplicationContext());
    }

    /**
     * Metodo che carica una nota nel server remoto.
     *
     * @param nota Nota da caricare
     */
    public void uploadNota(Nota nota,
                           MultipleTransferListener multipleTransferListener)
            throws IOException {

        if (multipleTransferListener == null) {
            multipleTransferListener = dummyInitForMultipleTransferListener();
        }
        final MultipleTransferListener onMultipleTransferListener = multipleTransferListener;

        List<File> noteFiles;
        noteFiles = CouchbaseDB.getInstance().getNoteFiles(nota.getID());

        final int totalFiles = noteFiles.size();
        for (int i = 0; i < totalFiles; i++) {
            final int currentIndex = i + 1;
            final File file = noteFiles.get(i);

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
                                onMultipleTransferListener.onFilesProgressChanged(currentIndex, totalFiles, TRANSFER_UPLOAD);
                            } else if (state == TransferState.COMPLETED) {
                                onMultipleTransferListener.onFileTransferred(file, TRANSFER_UPLOAD);
                            } else if (state == TransferState.FAILED) {
                                onMultipleTransferListener.onFileTransferFailed(file, TRANSFER_UPLOAD, null);
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            onMultipleTransferListener.onSingleFileProgressChanged(file, bytesCurrent, bytesTotal, TRANSFER_UPLOAD);
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            onMultipleTransferListener.onFileTransferFailed(file, TRANSFER_UPLOAD, ex);
                        }
                    });
        }
    }

    /**
     * Metodo che scarica tutte le note dal server remoto e le memorizza nel database.
     *
     * @return Lista di note scaricate
     */
    public void downloadAllNotes(MultipleTransferListener transferListener) {

        if (transferListener == null) {
            transferListener = dummyInitForMultipleTransferListener();
        }
        final MultipleTransferListener listener = transferListener;

        getNotesList(listener, new Callback() {
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
                    downloadNoteMedias(nota, listener);
                }
            }
        });
    }

    /**
     * Metodo utile per scaricare una sola nota, data la key del bucket.
     *
     * @param
     */
    private void downloadNoteMedias(Nota nota, final MultipleTransferListener listener) {
        if (nota.getAudio() != null) {
            final File localFile = new File(nota.getAudio());
            if (!localFile.exists()) {
                try {
                    localFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            transferUtility.download(BUCKET_NAME, BUCKET_AUDIO_DIR + localFile.getName(), localFile)
                    .setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (state == TransferState.COMPLETED) {
                                listener.onFinish(TRANSFER_DOWNLOAD);
                            } else if (state == TransferState.WAITING_FOR_NETWORK) {
//                                            listener.onWaitingForNetwork(id);
                            } else if (state == TransferState.FAILED) {
                                listener.onFileTransferFailed(localFile, TRANSFER_DOWNLOAD, null);
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            listener.onSingleFileProgressChanged(localFile, bytesCurrent, bytesTotal, TRANSFER_DOWNLOAD);
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            listener.onFileTransferFailed(localFile, TRANSFER_DOWNLOAD, ex);
                        }
                    });
        }
        if (nota.getImage() != null) {
            final File localFile = new File(nota.getImage());
            if (!localFile.exists()) {
                try {
                    localFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            transferUtility.download(BUCKET_NAME, BUCKET_IMAGES_DIR + localFile.getName(), localFile)
                    .setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (state == TransferState.COMPLETED) {
                                listener.onFinish(id);
                            } else if (state == TransferState.WAITING_FOR_NETWORK) {

                            } else if (state == TransferState.FAILED) {
                                listener.onFileTransferFailed(localFile, TRANSFER_DOWNLOAD, null);
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            listener.onSingleFileProgressChanged(localFile, bytesCurrent, bytesTotal, TRANSFER_DOWNLOAD);
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            listener.onFileTransferFailed(localFile, TRANSFER_DOWNLOAD, ex);
                        }
                    });
        }
    }

    /**
     * Metodo che sincronizza tutte le note locali con quelle in remoto, scaricando le note remote
     * non presenti in locale, e caricando le note locali non presenti in remoto.
     * Si appoggia al database locale.
     */
    public void sync(SyncListener syncListener, MultipleTransferListener multipleTransferListener) {
        if (syncListener == null) {
            syncListener = dummyInitForSyncListener();
        }
        if (multipleTransferListener == null) {
            multipleTransferListener = dummyInitForMultipleTransferListener();
        }
        final SyncListener onSyncListener = syncListener;
        final MultipleTransferListener onMultipleTransferListener = multipleTransferListener;

        getNotesList(onMultipleTransferListener, new Callback() {
            @Override
            public void call(Object... args) {
                ArrayList<Nota> remoteNoteList = (ArrayList<Nota>) args[0];

                upSyncCompleted = false;
                onSyncListener.onSyncStarted();
                syncLocalWithRemote(remoteNoteList,
                        onMultipleTransferListener,
                        new Callback() {
                            @Override
                            public void call(Object... args) {
                                int currentIndex = (int) args[0];
                                int poolSize = (int) args[1];
                                if (currentIndex == poolSize) {
                                    upSyncCompleted = true;
                                    if (!isSyncing()) {
                                        // questo viene chiamato una volta sola perchè
                                        // non finiranno mai nello stesso tempo
                                        onSyncListener.onSyncFinished();
                                    }
                                }
                            }
                        });
                downSyncCompleted = false;
                syncRemoteWithLocal(remoteNoteList,
                        onMultipleTransferListener,
                        new Callback() {
                            @Override
                            public void call(Object... args) {
                                int currentIndex = (int) args[0];
                                int poolSize = (int) args[1];
                                if (currentIndex == poolSize) {
                                    downSyncCompleted = true;
                                    if (!isSyncing()) {
                                        // questo sarà chiamato una volta sola perchè
                                        // non finiranno mai nello stesso tempo
                                        onSyncListener.onSyncFinished();
                                    }
                                }
                            }
                        });
            }
        });

    }

    /**
     * Metodo che sincronizza tutte le note locali con quelle in remoto, scaricando le note remote
     * non presenti in locale, e caricando le note locali non presenti in remoto.<br/><br/>
     * <b>NON TIENE TRACCIA DI EVENTUALI ERRORI SUI FILE.</b><br/><br/>
     * Si appoggia al database locale.
     */
    public void sync(SyncListener syncListener) {
        sync(syncListener, null);
    }

    /**
     * Metodo privato per pulizia del codice, che sincronizza le note locali con le note remote.
     *
     * @param remoteNotesList lista delle note remote, passata per ottimizzare le operazioni
     * @param listener        listener per il trasferimento multiplo di file
     * @param onFinish        callback che stabilisce la fine del processo
     */
    private void syncLocalWithRemote(final ArrayList<Nota> remoteNotesList,
                                     final MultipleTransferListener listener,
                                     final Callback onFinish) {
        new AsyncTask<Void, Void, Void>() {

            ArrayList<Nota> uploadPool;
            ArrayList<Nota> localNotesList;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                uploadPool = new ArrayList<>();
                try {
                    localNotesList = CouchbaseDB.getInstance().leggiNote();
                } catch (Exception e) {
                    throw new RuntimeException("Unable to read notes from database. " + e.getMessage());
                }
                // aggiungo le note locali da caricare al pool
                for (Nota localNote : localNotesList) {
                    if (!remoteNotesList.contains(localNote)) {
                        uploadPool.add(localNote);
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                // TODO capire come notificare che tutte le note sono state caricate
                final int poolSize = uploadPool.size();
                if (poolSize == 0) {
                    onFinish.call(0, 0);
                    return null;
                }
                for (int i = 0; i < uploadPool.size(); i++) {
                    final Nota nota = uploadPool.get(i);
                    final int index = i + 1;
                    try {
                        uploadNota(nota,
                                new MultipleTransferListener() {
                                    @Override
                                    public void onFileTransferred(File file, int transferType) {
                                        listener.onFileTransferred(file, transferType);
                                    }

                                    @Override
                                    public void onSingleFileProgressChanged(File file, long currentBytes, long totalBytes, int transferType) {
                                        listener.onSingleFileProgressChanged(file, currentBytes, totalBytes, transferType);
                                    }

                                    @Override
                                    public void onFileTransferFailed(File file, int transferType, Exception e) {
                                        listener.onFileTransferFailed(file, transferType, e);
                                    }

                                    @Override
                                    public void onFilesProgressChanged(int currentFile, int totalFiles, int transferType) {
                                        listener.onFilesProgressChanged(currentFile, totalFiles, transferType);
                                    }

                                    @Override
                                    public void onFinish(int transferType) {
                                        listener.onFinish(transferType);
                                        onFinish.call(index, poolSize);
                                    }
                                });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute();
    }

    /**
     * Metodo privato per pulizia del codice, che sincronizza le note remote con le note locali.
     *
     * @param remoteNotesList lista delle note remote, passata per ottimizzare le operazioni
     * @param listener        listener per il trasferimento multiplo di file
     * @param onFinish        callback che stabilisce la fine del processo
     */
    private void syncRemoteWithLocal(ArrayList<Nota> remoteNotesList,
                                     final MultipleTransferListener listener,
                                     final Callback onFinish) {
        ArrayList<Nota> downloadPool = new ArrayList<>();

        ArrayList<Nota> localNotesList;
        try {
            localNotesList = CouchbaseDB.getInstance().leggiNote();
        } catch (Exception e) {
            throw new RuntimeException("Unable to read notes from database. " + e.getMessage());
        }

        // aggiungo le note remote da scaricare al pool
        for (Nota remoteNote : remoteNotesList) {
            if (!localNotesList.contains(remoteNote)) {
                downloadPool.add(remoteNote);
            }
        }

        try {
            CouchbaseDB.getInstance().salvaNote(downloadPool);
        } catch (Exception e) {
            throw new RuntimeException("Unable to store downloaded notes. " + e.getMessage());
        }

        final int poolSize = downloadPool.size();
        if (poolSize == 0) {
            onFinish.call(0, 0);
            return;
        }
        for (int i = 0; i < downloadPool.size(); i++) {
            final Nota nota = downloadPool.get(i);
            final int currentIndex = i + 1;
            downloadNoteMedias(nota, new MultipleTransferListener() {
                @Override
                public void onFileTransferred(File file, int transferType) {
                    listener.onFileTransferred(file, transferType);
                }

                @Override
                public void onFileTransferFailed(File file, int transferType, Exception e) {
                    listener.onFileTransferFailed(file, transferType, e);
                }

                @Override
                public void onFilesProgressChanged(int currentFile, int totalFiles, int transferType) {
                    listener.onFilesProgressChanged(currentFile, totalFiles, transferType);
                }

                @Override
                public void onSingleFileProgressChanged(File file, long currentBytes, long totalBytes, int transferType) {
                    listener.onSingleFileProgressChanged(file, currentBytes, totalBytes, transferType);
                }

                @Override
                public void onFinish(int transferType) {
                    listener.onFinish(transferType);
                    onFinish.call(currentIndex, poolSize);

                }
            });
        }
        // TODO capire come notificare che tutte le note sono state scaricate
    }

    public void syncLocalWithRemote(SyncListener transferListener) {
        // TODO
    }

    public void syncRemoteWithLocal(SyncListener transferListener) {
        // TODO
    }

    /**
     * Metodo che elimina da remoto la nota specificata.
     *
     * @param nota Nota da eliminare su S3
     */
    public void deleteNoteFromRemote(final Nota nota, DeletionListener deletionListener) {
        if (deletionListener == null)
            deletionListener = dummyInitForDeletionListener();
        final DeletionListener onDeletionListener = deletionListener;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                amazonS3.deleteObject(new DeleteObjectRequest(BUCKET_NAME, BUCKET_NOTES_DIR + nota.getID() + ".note"));
                if (nota.getAudio() != null) {
                    amazonS3.deleteObject(new DeleteObjectRequest(BUCKET_NAME, BUCKET_AUDIO_DIR + new File(nota.getAudio()).getName()));
                }
                if (nota.getImage() != null) {
                    amazonS3.deleteObject(new DeleteObjectRequest(BUCKET_NAME, BUCKET_IMAGES_DIR + new File(nota.getImage()).getName()));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                onDeletionListener.onDeleted(nota);
            }
        }.execute();
    }

    /**
     * Scarica l'elenco di note presenti su S3 e popola un ArrayList ritornato come primo parametro
     * della callback.
     *
     * @param listener               OnSingleTransferListener per la gestione degli eventi
     * @param scaricamentoNoteFinito callback che restituisce le note scaricate
     */
    private void getNotesList(final MultipleTransferListener listener, final Callback scaricamentoNoteFinito) {
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

                    if (notesKeys.size() == 0) {
                        scaricamentoNoteFinito.call(notesList);
                        return;
                    }

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
                            transferUtility.download(BUCKET_NAME, key, file)
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
                                                listener.onFileTransferFailed(file, TRANSFER_DOWNLOAD, null);
                                            }
                                        }

                                        @Override
                                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                            listener.onSingleFileProgressChanged(file, bytesCurrent, bytesTotal, TRANSFER_DOWNLOAD);
                                        }

                                        @Override
                                        public void onError(int id, Exception ex) {
                                            listener.onFileTransferFailed(file, TRANSFER_DOWNLOAD, ex);
                                        }
                                    });
                        }

                    }
                }
            }
        }.execute();
    }


    /**
     * Valore booleano che determina se la sincronizzazione è in corso.
     */
    public boolean isSyncing() {
        return !downSyncCompleted && !upSyncCompleted;
    }

//    private OnSingleTransferListener dummyInitForSingleTransferListener() {
//        return new OnSingleTransferListener() {
//            @Override
//            public void onStart(int transferID) {
//
//            }
//
//            @Override
//            public void onProgressChanged(int transferID, long bytesCurrent, long totalBytes) {
//
//            }
//
//            @Override
//            public void onWaitingForNetwork(int transferID) {
//
//            }
//
//            @Override
//            public void onFinish(int transferID) {
//
//            }
//
//            @Override
//            public void onFailure(int transferID) {
//
//            }
//
//            @Override
//            public void onError(int transferID, Exception e) {
//
//            }
//        };
//    }

    private MultipleTransferListener dummyInitForMultipleTransferListener() {
        return new MultipleTransferListener() {
            @Override
            public void onFileTransferred(File file, int transferType) {

            }

            @Override
            public void onFilesProgressChanged(int currentFile, int totalFiles, int transferType) {

            }

            @Override
            public void onSingleFileProgressChanged(File file, long currentBytes, long totalBytes, int transferType) {

            }

            @Override
            public void onFileTransferFailed(File file, int transferType, Exception e) {

            }

            @Override
            public void onFinish(int transferType) {

            }
        };
    }

    private SyncListener dummyInitForSyncListener() {
        return new SyncListener() {
            @Override
            public void onSyncStarted() {

            }

            @Override
            public void onSyncFinished() {

            }
        };
    }

    private DeletionListener dummyInitForDeletionListener() {
        return new DeletionListener() {
            @Override
            public void onDeleted(Nota nota) {

            }
        };
    }
}
