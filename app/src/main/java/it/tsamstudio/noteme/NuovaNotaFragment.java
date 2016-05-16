package it.tsamstudio.noteme;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.AHClickListener;
import com.couchbase.lite.CouchbaseLiteException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import it.tsamstudio.noteme.utils.NoteMeUtils;


/**
 * A simple {@link Fragment} subclass.
 */
public class NuovaNotaFragment extends DialogFragment {

    private View dialogView;
    private TextView titolo, etxtNota, titoloAudio;
    private MediaRecorder mRecorder;
    private String audioOutputPath = null;
    private String imageOutputPath;
    private ImageView immagine, immagineAudio;
    private RelativeLayout relativeLayout;

    private Snackbar timeProgressSnackbar;
    private Timer recordingTimer;
    private Date timerTime;

    private boolean isRecording;

    INuovaNota listener = new INuovaNota() {
        @Override
        public void onNuovaNotaAggiunta(Nota nota) {
            Log.d("onNuovaNotaAggiunta", "dummy init");
        }
    };

    public interface INuovaNota {
        void onNuovaNotaAggiunta(Nota nota);
    }

    public NuovaNotaFragment() {
        // Required empty public constructor
    }

    public static NuovaNotaFragment newInstance() {
        NuovaNotaFragment nuovaNotaFragment = new NuovaNotaFragment();
        Bundle bundle = new Bundle();
        // TODO
        nuovaNotaFragment.setArguments(bundle);
        return nuovaNotaFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof INuovaNota) {
            listener = (INuovaNota) activity;
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        listener.onNuovaNotaAggiunta(saveNote());

        Log.d("DETACH", "onDETACH");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogView = inflater.inflate(R.layout.fragment_nuova_nota, null, false);
        titolo = (TextView) dialogView.findViewById(R.id.etxtTitolo);
        etxtNota = (TextView) dialogView.findViewById(R.id.etxtNota);
        relativeLayout = (RelativeLayout) dialogView.findViewById(R.id.relativo);
        immagine = (ImageView) dialogView.findViewById(R.id.imageView);
        immagineAudio = (ImageView) dialogView.findViewById(R.id.imageAudio);
        titoloAudio = (TextView) dialogView.findViewById(R.id.textAudio);

        titoloAudio.setVisibility(View.GONE);
        immagineAudio.setVisibility(View.GONE);

        recordingTimer = new Timer();
        timerTime = new Date(0);

        final AHBottomNavigation bottomNavigation = (AHBottomNavigation) dialogView.findViewById(R.id.bottomNavigation);
        bottomNavigation.setForceTitlesDisplay(false);
        AHBottomNavigationItem item1 = new AHBottomNavigationItem("1", R.drawable.ic_attach_file_white_48dp);
        item1.setListener(new AHClickListener() {
            @Override
            public void onClickListener(View view) {

            }

            @Override
            public boolean onLongClickListener(View view) {
                Log.d("onLongPress", "" + bottomNavigation.getCurrentItem());
                return true;
            }
        });
        AHBottomNavigationItem item2 = new AHBottomNavigationItem("2", R.drawable.ic_mic_white_48dp);
        item2.setListener(new AHClickListener() {
            @Override
            public void onClickListener(View view) {

                //startRecording();
            }

            @Override
            public boolean onLongClickListener(View view) {
                Log.d("onLongPress", "" + bottomNavigation.getCurrentItem());
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }
                return true;
            }
        });
        AHBottomNavigationItem item3 = new AHBottomNavigationItem("3", R.drawable.ic_add_a_photo_white_48dp);
        item3.setListener(new AHClickListener() {
            @Override
            public void onClickListener(View view) {

            }

            @Override
            public boolean onLongClickListener(View view) {
                takeImageFromCamera();
                return false;
            }
        });

        bottomNavigation.addItem(item1);
        bottomNavigation.addItem(item2);
        bottomNavigation.addItem(item3);

        //bottomBar = setupBottomBar(dialogView, savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    //metodo chiamato quando viene chiuso il dialog per salvare la etxtNota
    private Nota saveNote() {
        String titoloTemp = titolo.getText().toString().trim();
        String testoTemp = etxtNota.getText().toString().trim();

        if (titoloTemp.length() > 0 || testoTemp.length() > 0) {   //se c'è almeno uno dei parametri
            Nota nota = new Nota();
            String titoloNota = (titoloTemp.length() > 0 ? titoloTemp : "Nota senza titolo");
            nota.setTitle("" + titoloNota);
            /*if (audioOutputPath != null) {
                nota.setAudio(audioOutputPath);
                audioOutputPath = null;
                Log.d("AUDIO", "audio salvato nella nota");
            }*/
            nota.setText("" + testoTemp);
            nota.setCreationDate(new Date());
            nota.setAudio(audioOutputPath);
            nota.setImage(imageOutputPath);

            CouchbaseDB db = new CouchbaseDB(getContext());
            try {
                db.salvaNota(nota);
                Toast.makeText(getContext(), "Nota salvata", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return nota;
        } else {
            Toast.makeText(getContext(), "Nota non salvata", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void startRecording() {
        try {
            mRecorder = setupRecorderWithPermission();
            if (mRecorder != null) {
                mRecorder.prepare();
                mRecorder.start();
                isRecording = true;
                timeProgressSnackbar = Snackbar.make(relativeLayout, getString(R.string.sto_registrando) + " - 00:00", Snackbar.LENGTH_INDEFINITE);
                timeProgressSnackbar.show();
                recordingTimer = new Timer();
                recordingTimer.schedule(createTimerTask(), 1000, 1000);
            }
        } catch (IOException e) {
            Log.d("AUDIO", "prepare() failed");
            e.printStackTrace();
            isRecording = false;
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        isRecording = false;
        recordingTimer.cancel();
        if (timeProgressSnackbar != null) {
            timeProgressSnackbar.dismiss();
        }
        mRecorder = setupRecorder();
        Toast.makeText(getContext(), "Registrazione salvata", Toast.LENGTH_SHORT).show();
        setAudioPreview();
    }

    private MediaRecorder setupRecorder() {
        MediaRecorder mRecorder = new MediaRecorder();

        audioOutputPath = getContext().getExternalFilesDir("NoteMeAudios") + "/" + (new Date()).getTime() + ".amr";
        Log.d("FILE PATH", audioOutputPath);

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mRecorder.setOutputFile(audioOutputPath);
        return mRecorder;
    }

    private MediaRecorder setupRecorderWithPermission() {
        isRecording = false;

        NoteMeUtils.askForPermissions(getActivity());

        if (!NoteMeUtils.needsToAskForPermissions(getActivity())) {
            return setupRecorder();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case NoteMeUtils.MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                startRecording();
                break;
            case NoteMeUtils.MY_PERMISSIONS_REQUEST_STORAGE:
                break;
            default:
                break;
        }
    }

    private TimerTask createTimerTask() {
        final Handler handler = new Handler();
        return new TimerTask() {
            private Date data = new Date(0);
            private SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");

            @Override
            public void run() {
                data.setTime(data.getTime() + 1000);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        timeProgressSnackbar.setText(getString(R.string.sto_registrando) + " - " + sdf.format(data));
                    }
                });
            }
        };
    }

    private static final int CAMERA_REQUEST = 1888;

    public void takeImageFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            // Error occurred while creating the File
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            startActivityForResult(takePictureIntent, CAMERA_REQUEST);
        }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(imageOutputPath, options);
            immagine.setImageBitmap(bitmap);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        imageOutputPath = image.getAbsolutePath();
        return image;
    }

    private void setAudioPreview() {
        titoloAudio.setVisibility(View.VISIBLE);
        immagineAudio.setVisibility(View.VISIBLE);
        titoloAudio.setText(getString(R.string.audio));
        immagineAudio.setImageResource(R.drawable.ic_volume_up_24dp);
    }

}
