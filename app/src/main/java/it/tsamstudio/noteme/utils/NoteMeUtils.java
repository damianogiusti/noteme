package it.tsamstudio.noteme.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by damiano on 16/05/16.
 */
public class NoteMeUtils {

    public static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    public static final int MY_PERMISSIONS_REQUEST_STORAGE = 2;

    public static boolean needsToAskForPermissions(Activity activity) {
        return Build.VERSION.SDK_INT >= 23 &&
                (!isPermissionGranted(activity, Manifest.permission.RECORD_AUDIO) &&
                        !isPermissionGranted(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                        !isPermissionGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                );
    }

    public static void askForPermissions(Activity activity) {
        // applicabile solo se siamo su marshmallow
        if (needsToAskForPermissions(activity)) {
                /*
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {*/

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE);

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    private static boolean isPermissionGranted(Activity activity, String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }
}
