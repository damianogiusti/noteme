package it.tsamstudio.noteme.utils;

import java.util.List;

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
        void onProgressChanged();
        void onFinish();
        void onFailure();
    }

    private static final String BUCKET_DIR = "tsac-its/noteme";

    private static S3Manager instance;

    public static S3Manager getInstance() {
        if (instance == null) {
            instance = new S3Manager();
        }
        return instance;
    }

    /**
     * Metodo che carica una nota nel server remoto.
     *
     * @param nota Nota da caricare
     */
    public void uploadNote(Nota nota) {
        // TODO
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
