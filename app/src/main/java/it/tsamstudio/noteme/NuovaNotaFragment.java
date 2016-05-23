package it.tsamstudio.noteme;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.AHClickListener;
import com.couchbase.lite.CouchbaseLiteException;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import it.tsamstudio.noteme.utils.AudioPlayerManager;
import it.tsamstudio.noteme.utils.NoteMeUtils;


/**
 * A simple {@link Fragment} subclass.
 */
public class NuovaNotaFragment extends DialogFragment implements View.OnClickListener, ToolTipView.OnToolTipViewClickedListener{

    private static final String TAG = "NuovaNotaFragment";

    private View dialogView;
    private TextView titolo, etxtNota, titoloAudio, tag;
    private MediaRecorder mRecorder;
    private String audioOutputPath = null;
    private String imageOutputPath = null;
    private ImageView immagine, immagineAudio;
    private RelativeLayout relativeLayout;
    AHBottomNavigation bottomNavigation;
    private AHBottomNavigationItem item1, item2, item3;

    private Snackbar timeProgressSnackbar;
    private Timer recordingTimer;
    private Date timerTime;
    private boolean isRecording;

    public ToolTipView myToolTipView;

    INuovaNota listener = new INuovaNota() {
        @Override
        public void onNuovaNotaAggiunta(Nota nota) {
            Log.d("onNuovaNotaAggiunta", "dummy init");
        }

        @Override
        public void onButtonClick(int request) {

        }
    };

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onToolTipViewClicked(ToolTipView toolTipView) {
        Log.d("CLICK ON POPUP","DEBUG BEAUTIFUL TIP TOOL");
    }

    public interface INuovaNota {
        void onNuovaNotaAggiunta(Nota nota);

        void onButtonClick(int request);
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
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(TAG, "onDismiss: ");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener.onNuovaNotaAggiunta(saveNote());
        imageOutputPath = null;
        audioOutputPath = null;
        Log.d(TAG, "onDETACH");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setCanceledOnTouchOutside(false);
        return super.onCreateView(inflater, container, savedInstanceState);
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
        tag = (TextView) dialogView.findViewById(R.id.chosenTag);

        // TODO qui non prende l'imageView giusta, bisogna fare il layout per la visualizzazione
        immagine = (ImageView) dialogView.findViewById(R.id.immagine);

        immagineAudio = (ImageView) dialogView.findViewById(R.id.imageAudio);
        titoloAudio = (TextView) dialogView.findViewById(R.id.textAudio);

        titoloAudio.setVisibility(View.GONE);
        immagineAudio.setVisibility(View.GONE);
        immagine.setVisibility(View.GONE);

        recordingTimer = new Timer();
        timerTime = new Date(0);


        ToolTipRelativeLayout toolTipRelativeLayout = (ToolTipRelativeLayout) dialogView.findViewById(R.id.tooltipRelativeLayout);

        ToolTip toolTip = new ToolTip()
                //.withContentView(dialogView.findViewById()) per contenuto customizzato
                .withText("Insert tag here")
                .withColor(Color.RED)
                .withShadow();
        myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip, dialogView.findViewById(R.id.redtv));
        myToolTipView.setOnToolTipViewClickedListener(this);

        bottomNavigation = (AHBottomNavigation) dialogView.findViewById(R.id.bottomNavigation);


        item1 = new AHBottomNavigationItem("", R.drawable.ic_attach_file_white_48dp);
        item1.setListener(new AHClickListener() {
            @Override
            public void onClickListener(View view) {

            }

            @Override
            public boolean onLongClickListener(View view) {
                listener.onButtonClick(HomeActivity.GALLERY_CODE);
                Log.d("onLongPress", "" + bottomNavigation.getCurrentItem());
                return true;
            }
        });

        item2 = new AHBottomNavigationItem("", R.drawable.ic_mic_white_48dp);
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

        item3 = new AHBottomNavigationItem("", R.drawable.ic_add_a_photo_white_48dp);
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

    private void updateBottomMenu() {
        item1.setEnabled(true);
        item2.setEnabled(true);
        item3.setEnabled(true);

        if (item1 != null && item2 != null && item3 != null) {
            if (imageOutputPath != null) {
                item1.setEnabled(false);
                item3.setEnabled(false);
            } else if (audioOutputPath != null) {
                item2.setEnabled(false);
            }
        }

        bottomNavigation.notifyItemsChanged();
    }

    //metodo chiamato quando viene chiuso il dialog per salvare la etxtNota
    private Nota saveNote() {
        String titoloTemp = titolo.getText().toString().trim();
        String testoTemp = etxtNota.getText().toString().trim();
        String testoTag = tag.getText().toString().trim();

        if (titoloTemp.length() > 0 || testoTemp.length() > 0 ||
                (audioOutputPath != null && audioOutputPath.trim().length() > 0) ||
                (imageOutputPath != null && imageOutputPath.trim().length() > 0)) {   //se c'è almeno uno dei parametri

            Nota nota = new Nota();
            String titoloNota = (titoloTemp.length() > 0 ? titoloTemp : "Nota senza titolo");
            nota.setTitle("" + titoloNota);
            nota.setText("" + testoTemp);
            nota.setTag("" + testoTag);
            nota.setCreationDate(new Date());
            nota.setLastModifiedDate(new Date());
            // TODO set data scadenza
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

        mRecorder = setupRecorderWithPermission();
        if (mRecorder != null) {
            try {
                mRecorder.prepare();
            } catch (IOException e) {
                Log.d("AUDIO", "prepare() failed");
                e.printStackTrace();
            }
            mRecorder.start();
            isRecording = true;

            timeProgressSnackbar = Snackbar.make(relativeLayout, getString(R.string.sto_registrando) + " - 00:00", Snackbar.LENGTH_INDEFINITE);
            timeProgressSnackbar.show();
            recordingTimer = new Timer();
            recordingTimer.schedule(createTimerTask(), 1000, 1000);
        }
    }

    private void stopRecording() {
        isRecording = false;
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        recordingTimer.cancel();

        if (timeProgressSnackbar != null) {
            timeProgressSnackbar.dismiss();
        }
        Toast.makeText(getContext(), "Registrazione salvata", Toast.LENGTH_SHORT).show();
        setAudioPreview();
        updateBottomMenu();
    }

    private MediaRecorder setupRecorder() {
        MediaRecorder recorder = new MediaRecorder();
        isRecording = false;
        audioOutputPath = getContext().getExternalFilesDir("NoteMeAudios") + "/" + (new Date()).getTime() + ".3gp";
        Log.d("Setup recorder", "Path: " + audioOutputPath);

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setOutputFile(audioOutputPath);
        return recorder;
    }

    private MediaRecorder setupRecorderWithPermission() {
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

/*
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(imageOutputPath, options);

            // immagine.setImageBitmap(bitmap); TODO ora come ora fa crashare l'app perchè 'immagine' non esiste
        }
    }*/

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
        immagineAudio.setImageResource(R.drawable.ic_play_circle_orange);

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
                final Dialog dialog = dialogBuilder.setMessage(getString(R.string.eliminare_nota))
                        .setPositiveButton(R.string.elimina, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File file = new File(audioOutputPath);
                                if (file.exists()) {
                                    file.delete();
                                }
                                audioOutputPath = null;
                                titoloAudio.setVisibility(View.GONE);
                                immagineAudio.setVisibility(View.GONE);
                            }
                        })
                        .setNegativeButton(R.string.annulla, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                dialog.show();
                return true;
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioPlayerManager.getInstance()
                        .init(audioOutputPath)
                        .setAudioPlayingListener(new AudioPlayerManager.AudioPlayingListener() {
                            @Override
                            public void onPlayingFinish() {
                                immagineAudio.setImageResource(R.drawable.ic_play_circle_orange);
                            }
                        });
                if (AudioPlayerManager.getInstance().isPlaying()) {
                    AudioPlayerManager.getInstance().pausePlaying();
                    immagineAudio.setImageResource(R.drawable.ic_play_circle_orange);
                } else if (AudioPlayerManager.getInstance().isStopped()) {
                    AudioPlayerManager.getInstance().startPlaying();
                    immagineAudio.setImageResource(R.drawable.ic_pause_circle_orange);
                } else if (AudioPlayerManager.getInstance().isPaused()) {
                    AudioPlayerManager.getInstance().resumePlaying();
                    immagineAudio.setImageResource(R.drawable.ic_pause_circle_orange);
                }
            }
        };

        titoloAudio.setOnClickListener(onClickListener);
        immagineAudio.setOnClickListener(onClickListener);
        titoloAudio.setOnLongClickListener(onLongClickListener);
        immagineAudio.setOnLongClickListener(onLongClickListener);
    }

    public void activityResult(final Intent intent) {
        immagine.setVisibility(View.VISIBLE);
        new AsyncTask<Void, Void, Void>() {
            File file;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // salvo una copia dell'immagine in una directory a parte
                    InputStream is = getContext().getContentResolver().openInputStream(intent.getData());
                    file = createImageFile();
                    OutputStream outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[is.available()];
                    is.read(buffer);
                    outputStream.write(buffer);
                    outputStream.close();
                    is.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                imageOutputPath = file.getPath();
                Log.d("IMAGE_PATH", imageOutputPath);
                immagine.setVisibility(View.VISIBLE);
                Picasso.with(getContext())
                        .load(file)
                        .fit()
                        .into(immagine);

                updateBottomMenu();
            }
        }.execute();
    }
}
