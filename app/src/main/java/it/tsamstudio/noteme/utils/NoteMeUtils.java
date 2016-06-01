package it.tsamstudio.noteme.utils;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by damiano on 16/05/16.
 */
public class NoteMeUtils {

    private static final String TAG = "NoteMeUtils";

    public static final String AES_KEY = "TsAmStUdIo2KI6projectwork";
    public static final String AES_PASSWORD_KEY = "TsAmStUdIo2KI6projectworkpA$$w0Rd_LOL";

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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", NoteMeApp.getInstance().getLocale()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getImagePath());
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

    private static Animator animatorInstance;

    public static Animator getAnimatorInstance() {
        return animatorInstance;
    }

    /**
     * Convenience but scopiazzated method for zooming an image shitta out
     *
     * @param container         container view to fill
     * @param expandedImageView big image view to display
     * @param thumbView         current thumbnail view
     * @param path              path to the image to zoom
     */
    public static void zoomImageFromThumb(View container,
                                          final ImageView expandedImageView,
                                          final View thumbView,
                                          String path,
                                          final Callback callback) {

        final int shortAnimationDuration = NoteMeApp.getInstance()
                .getResources().getInteger(android.R.integer.config_shortAnimTime);

        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (animatorInstance != null) {
            animatorInstance.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageURI(Uri.parse("file://" + path));

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        container.getGlobalVisibleRect(finalBounds, globalOffset);
        Log.d(TAG, String.format("zoomImageFromThumb: globalOffset(%s;%s)", globalOffset.x, globalOffset.y));
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImageView, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(expandedImageView,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatorInstance = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animatorInstance = null;
            }
        });
        set.start();
        animatorInstance = set;

        Log.d(TAG, "onAnimation: ");
        callback.call(true);

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        final AnimatorSet closingSet = new AnimatorSet();
        closingSet.play(ObjectAnimator
                .ofFloat(expandedImageView, View.X, startBounds.left))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.Y, startBounds.top))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(expandedImageView,
                                View.SCALE_Y, startScaleFinal));
        closingSet.setDuration(shortAnimationDuration);
        closingSet.setInterpolator(new DecelerateInterpolator());
        closingSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                thumbView.setAlpha(1f);
                expandedImageView.setVisibility(View.GONE);
                animatorInstance = null;
                Log.d(TAG, "onAnimationEnd: ");
                callback.call(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                thumbView.setAlpha(1f);
                expandedImageView.setVisibility(View.GONE);
                Log.d(TAG, "onAnimationCancel: ");
                animatorInstance = null;
                callback.call(false);
            }
        });
        animatorInstance = closingSet;
        Log.d(TAG, "impostato closing set");

        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (animatorInstance != null) {
                    animatorInstance.cancel();
                }
                closingSet.start();
            }
        });
    }

    public static String intColorToHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public static int[] arrayListToArray(ArrayList<Integer> arrayList) {
        int[] array = new int[arrayList.size()];
        for (int i = 0; i< arrayList.size(); i++) {
            array[i] = arrayList.get(i);
        }
        return array;
    }

    public static boolean isBlank(String string) {
        return string == null || string.trim().equals("");
    }

    public static String getAudioPath() {
        return NoteMeApp.getInstance().getApplicationContext().getExternalFilesDir("NoteMeAudios") + "/";
    }

    public static String getImagePath() {
        return NoteMeApp.getInstance().getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES).getAbsolutePath();
    }
}
