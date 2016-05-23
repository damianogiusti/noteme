package it.tsamstudio.noteme.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    public static Drawable resizeDrawable(Drawable drawable, int newWidth, int newHeight) {

        if (!(drawable instanceof BitmapDrawable)) {
            return null;
        }
        if (((BitmapDrawable) drawable).getBitmap() == null) {
            return null;
        }

        Bitmap bitmapOrg = ((BitmapDrawable) drawable).getBitmap();
// calculate the scale - in this case = 0.4f
        float scaleWidth = ((float) newWidth) / bitmapOrg.getWidth();
        float scaleHeight = ((float) newHeight) / bitmapOrg.getHeight();

// createa matrix for the manipulation
        Matrix matrix = new Matrix();
// resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

// recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
                bitmapOrg.getWidth(), bitmapOrg.getHeight(), matrix, true);

// make a Drawable from Bitmap to allow to set the BitMap
// to the ImageView, ImageButton or what ever
        BitmapDrawable bmd = new BitmapDrawable(resizedBitmap);

        return bmd;
    }

    /**
     * Convenience method for creating an image file
     *
     * @return File object
     * @throws IOException
     */
    public static File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = NoteMeApp.getInstance().getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    /**
     * Convenience method for saving a compressed picture
     *
     * @param uri           uri path to save file
     * @param compressRatio amount of JPEG compression to apply (0-100)
     * @return FIle object
     * @throws IOException
     */
    public static File saveCompressedPicture(Uri uri, int compressRatio) throws IOException {
        File file;
        InputStream is = NoteMeApp.getInstance().getContentResolver().openInputStream(uri);
        file = createImageFile();

        OutputStream outputStream = new FileOutputStream(file);
        Bitmap bmp = BitmapFactory.decodeStream(is);

        if (bmp.getWidth() > 800) {
            // width : 800 = height : x
            int newWidth = 800;
            int newHeight = 800 * bmp.getHeight() / bmp.getWidth();
            bmp = Bitmap.createScaledBitmap(bmp, newWidth, newHeight, false);
        }
        bmp.compress(Bitmap.CompressFormat.JPEG, compressRatio, outputStream);
//                    byte[] buffer = new byte[is.available()];
//                    is.read(buffer);
//                    outputStream.write(buffer);
        outputStream.close();
        is.close();

        return file;
    }
}
