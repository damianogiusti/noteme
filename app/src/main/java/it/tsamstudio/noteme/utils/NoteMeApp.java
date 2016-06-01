package it.tsamstudio.noteme.utils;

import android.app.Application;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by damiano on 23/05/16.
 */
public class NoteMeApp extends Application {

    private static NoteMeApp instance;

    public static NoteMeApp getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Application itself is null D:");
        }
        return instance;
    }

    public NoteMeApp() {
        super();

    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        File audioDir = new File(NoteMeUtils.getAudioPath());
        if (!audioDir.exists()) {
            try {
                audioDir.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        File imageDir = new File(NoteMeUtils.getImagePath());
        if (!imageDir.exists()) {
            try {
                imageDir.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Locale getLocale() {
        return getResources().getConfiguration().locale;
    }
}
