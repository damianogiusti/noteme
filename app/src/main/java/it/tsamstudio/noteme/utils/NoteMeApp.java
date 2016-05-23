package it.tsamstudio.noteme.utils;

import android.app.Application;

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
    }
}
