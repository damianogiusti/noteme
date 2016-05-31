package it.tsamstudio.noteme.utils;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.IOException;
import java.util.List;

import it.tsamstudio.noteme.CouchbaseDB;
import it.tsamstudio.noteme.Nota;

/**
 * Created by damianogiusti on 31/05/16.
 */
public class S3Manager {

    /**
     * Interfaccia che penso useremo per tornare indietro lo stato del trasferimento,
     * che ovviamente sarà asincrono.
     * TODO pensare bene come implementarla
     */
    public interface OnTransferListener {
        void onStart();

        void onProgressChanged(long bytesCurrent, long totalBytes);

        void onWaitingForNetwork();

        void onFinish();

        void onFailure();

        void onError(Exception e);
    }

    private static final String BUCKET_NAME = "tsac-its";
    private static final String BUCKET_DIR = "noteme/";

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
    public void uploadNote(Nota nota, OnTransferListener transferListener) throws IOException {
        if (transferListener == null) {
            // dummy init
            transferListener = new OnTransferListener() {
                @Override
                public void onStart() {

                }

                @Override
                public void onProgressChanged(long bytesCurrent, long totalBytes) {

                }

                @Override
                public void onWaitingForNetwork() {

                }

                @Override
                public void onFinish() {

                }

                @Override
                public void onFailure() {

                }

                @Override
                public void onError(Exception e) {

                }
            };
        }
        final OnTransferListener listener = transferListener;

        CouchbaseDB couchbaseDB = new CouchbaseDB(NoteMeApp.getInstance().getApplicationContext());
        List<File> noteFiles;
        noteFiles = couchbaseDB.getNoteFiles(nota.getID());

        for (File file : noteFiles) {
            transferUtility.upload(BUCKET_NAME, BUCKET_DIR + file.getName(), file)
                    .setTransferListener(new TransferListener() {
                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            if (state == TransferState.IN_PROGRESS) {
                                listener.onStart();
                            } else if (state == TransferState.COMPLETED) {
                                listener.onFinish();
                            } else if (state == TransferState.FAILED) {
                                listener.onFailure();
                            } else if (state == TransferState.WAITING_FOR_NETWORK) {
                                listener.onWaitingForNetwork();
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                            listener.onProgressChanged(bytesCurrent, bytesTotal);
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            listener.onError(ex);
                        }
                    });
        }
    }

    /**
     * Metodo che scarica tutte le note dal server remoto.
     *
     * @return Lista di note scaricate
     */
    public List<Nota> downloadAllNotes() {
        // TODO
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
}
