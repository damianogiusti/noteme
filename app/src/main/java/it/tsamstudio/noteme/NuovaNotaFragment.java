package it.tsamstudio.noteme;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.michaldrabik.tapbarmenulib.TapBarMenu;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import com.squareup.picasso.Picasso;
import com.turkialkhateeb.materialcolorpicker.ColorChooserDialog;
import com.turkialkhateeb.materialcolorpicker.ColorListener;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import it.tsamstudio.noteme.utils.AudioPlayerManager;
import it.tsamstudio.noteme.utils.Callback;
import it.tsamstudio.noteme.utils.NoteMeApp;
import it.tsamstudio.noteme.utils.NoteMeUtils;


/**
 * A simple {@link Fragment} subclass.
 */
public class NuovaNotaFragment extends DialogFragment implements View.OnClickListener, ToolTipView.OnToolTipViewClickedListener {

    private static final String TAG = "NuovaNotaFragment";
    private static final String TAG_DATE_PICKER = "datepickerdialog";
    private static final String TAG_TIME_PICKER = "timepickerdialog";
    public static final int CAMERA_REQUEST = 1888;
    // tag per ciclo di vita
    private static final String TAG_KEYBOARD_FOR_BUNDLE = "iskeybboardshown";
    private static final String TAG_AUDIO_PATH_FOR_BUNDLE = "audiopath";
    private static final String TAG_IMAGE_PATH_FOR_BUNDLE = "imagepath";
    private static final String TAG_EXPIRATION_DATE_FOR_BUNDLE = "expirationdatebundle";
    private static final String TAG_IS_RECORDING_FOR_BUNDLE = "isrecordingmadafaka";
    private static final String TAG_TITLE_NOTA_FOR_BUNDLE = "titolonotanelbundle";
    private static final String TAG_BODY_NOTA_FOR_BUNDLE = "bodydellanotanelbundle";
    private static final String TAG_GUID_FOR_BUNDLE = "tagguidforbundle";
    private static final String TAG_COLOR_FOR_BUNDLE = "tagcolorforbundle";

    private View dialogView;
    private EditText tag;
    private TextView titolo, etxtNota, titoloAudio;
    private TextView txtDataScadenza;
    private MediaRecorder mRecorder;

    private String guid;
    private String audioOutputPath = null;
    private String imageOutputPath = null;
    private Date expirationDate = null;
    private int noteColor = 0;

    private ImageView immagine, immagineAudio;
    private RelativeLayout relativeLayout;

    private TapBarMenu tapBarMenu;
    private ImageView menuImgAttach;
    private ImageView menuImgMic;
    private ImageView menuImgCamera;
    private ImageView itemExpirationDate;
    private ImageView menuImgColor;
    private ImageView menuImgTag;

    private Snackbar timeProgressSnackbar;
    private Timer recordingTimer;
    private Date timerTime;
    private boolean isRecording;

    private boolean isKeyboardShown;

    private ToolTipRelativeLayout toolTipRelativeLayout;
    private ToolTipView myToolTipView;
    private ToolTip toolTip;
    private Boolean isToolTipShown = false;

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
        toolTipView.setVisibility(View.VISIBLE);
        Log.d("CLICK ON POPUP", "DEBUG BEAUTIFUL TIP TOOL");
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
        Log.d(TAG, "onDETACH");
        listener.onNuovaNotaAggiunta(saveNote());
        isToolTipShown = false;
        myToolTipView = null;
        imageOutputPath = null;
        audioOutputPath = null;
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
        txtDataScadenza = (TextView) dialogView.findViewById(R.id.txtDataScadenza);

        // TODO qui non prende l'imageView giusta, bisogna fare il layout per la visualizzazione
        immagine = (ImageView) dialogView.findViewById(R.id.immagine);

        immagineAudio = (ImageView) dialogView.findViewById(R.id.imageAudio);
        titoloAudio = (TextView) dialogView.findViewById(R.id.textAudio);

        titoloAudio.setVisibility(View.GONE);
        immagineAudio.setVisibility(View.GONE);
        immagine.setVisibility(View.GONE);

        recordingTimer = new Timer();
        timerTime = new Date(0);

        toolTipRelativeLayout = (ToolTipRelativeLayout) dialogView.findViewById(R.id.tooltipRelativeLayout);

        toolTip = new ToolTip()
                .withContentView(LayoutInflater.from(getActivity()).inflate(R.layout.edittext_layout_tooltip, null)) // per contenuto customizzato
                .withColor(getResources().getColor(R.color.colorAccent))
                .withShadow();

        View vTool = toolTip.getContentView();
        tag = (EditText) vTool.findViewById(R.id.tagInToolTip);
//        myToolTipView.setOnToolTipViewClickedListener(this);

        tapBarMenu = (TapBarMenu) dialogView.findViewById(R.id.tapBarMenu);

        tapBarMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tapBarMenu.isOpened()) {
                    hideKeyboard(dialogView);
                }
                tapBarMenu.toggle();
            }
        });

        menuImgAttach = ((ImageView) dialogView.findViewById(R.id.menuImgAttach));
        menuImgAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!NoteMeUtils.needsToAskForPermissions(getActivity())) {
                    listener.onButtonClick(HomeActivity.GALLERY_CODE);
                } else {
                    NoteMeUtils.askForPermissions(getActivity());
                }
            }
        });

        menuImgMic = ((ImageView) dialogView.findViewById(R.id.menuImgMic));
        menuImgMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }
            }
        });

        menuImgCamera = (ImageView) dialogView.findViewById(R.id.menuImgCamera);
        menuImgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeImageFromCamera();
            }
        });

        itemExpirationDate = (ImageView) dialogView.findViewById(R.id.menuImgExpireDate);
        itemExpirationDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expirationDate = null;
                Log.d(TAG, "onClickListener: ");
                showExpirationDateDialogs(new Callback() {
                    @Override
                    public void call(Object... args) {
                        if (args.length == 1) {
                            expirationDate = new Date(((long) args[0]));
                            setExpirationDatePreview();
                        }
                    }
                });
            }
        });

        menuImgColor = (ImageView) dialogView.findViewById(R.id.menuImgColor);
        menuImgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorChooserDialog dialog = new ColorChooserDialog(getActivity());
                dialog.setTitle(R.string.scegli_un_colore);
                dialog.setColorListener(new ColorListener() {
                    @Override
                    public void OnColorClick(View v, final int color) {
                        noteColor = color;
                        setColorPreview();
                    }
                });
                dialog.show();
            }
        });

        menuImgTag = (ImageView) dialogView.findViewById(R.id.menuImgTag);
        menuImgTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myToolTipView == null || !myToolTipView.isShown()) {
                    if (myToolTipView == null)
                        myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip, dialogView.findViewById(R.id.tapBarMenu));
                    myToolTipView.setVisibility(View.VISIBLE);
                    isToolTipShown = true;
                } else {
                    if (myToolTipView.isShown()) {
                        myToolTipView.setVisibility(View.INVISIBLE);
                        isToolTipShown = false;
                    }
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Dialog dialog = builder.create();
        showKeyboard(dialog.getWindow());
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // ripristino lo stato del fragment
        if (savedInstanceState != null) {
            guid = savedInstanceState.getString(TAG_GUID_FOR_BUNDLE);
            isKeyboardShown = savedInstanceState.getBoolean(TAG_KEYBOARD_FOR_BUNDLE);
            audioOutputPath = savedInstanceState.getString(TAG_AUDIO_PATH_FOR_BUNDLE);
            Log.d(TAG, "onActivityCreated: " + audioOutputPath);
            if (audioOutputPath != null) {
                setAudioPreview();
            }
            imageOutputPath = savedInstanceState.getString(TAG_IMAGE_PATH_FOR_BUNDLE);
            Log.d(TAG, "onActivityCreated: " + imageOutputPath);
            if (imageOutputPath != null) {
                setImagePreview();
            }
            expirationDate = (savedInstanceState.getLong(TAG_EXPIRATION_DATE_FOR_BUNDLE) > 0
                    ? new Date(savedInstanceState.getLong(TAG_EXPIRATION_DATE_FOR_BUNDLE)) : null);
            if (expirationDate != null) {
                setExpirationDatePreview();
            }
            isRecording = savedInstanceState.getBoolean(TAG_IS_RECORDING_FOR_BUNDLE);
            titolo.setText(savedInstanceState.getString(TAG_TITLE_NOTA_FOR_BUNDLE));
            etxtNota.setText(savedInstanceState.getString(TAG_BODY_NOTA_FOR_BUNDLE));
            noteColor = savedInstanceState.getInt(TAG_COLOR_FOR_BUNDLE);
            if (noteColor != 0) {
                setColorPreview();
            }
            updateBottomMenu();
            savedInstanceState.clear();
        }
    }

    private void showKeyboard(Window window) {
        if (!isKeyboardShown)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) NoteMeApp.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        listener.onNuovaNotaAggiunta(saveNote());
        outState.putString(TAG_GUID_FOR_BUNDLE, guid);
        outState.putBoolean(TAG_KEYBOARD_FOR_BUNDLE, isKeyboardShown);
        outState.putString(TAG_AUDIO_PATH_FOR_BUNDLE, audioOutputPath);
        outState.putString(TAG_IMAGE_PATH_FOR_BUNDLE, imageOutputPath);
        outState.putInt(TAG_COLOR_FOR_BUNDLE, noteColor);
        outState.putLong(TAG_EXPIRATION_DATE_FOR_BUNDLE,
                (expirationDate != null) ? expirationDate.getTime() : 0);
        outState.putBoolean(TAG_IS_RECORDING_FOR_BUNDLE, isRecording);
        if (titolo != null) {
            outState.putString(TAG_TITLE_NOTA_FOR_BUNDLE, titolo.getText().toString());
        }
        if (etxtNota != null) {
            outState.putString(TAG_BODY_NOTA_FOR_BUNDLE, etxtNota.getText().toString());
        }
        imageOutputPath = null;
        audioOutputPath = null;
    }

    private void updateBottomMenu() {

        // se queste callback ritornano true, onClickListener non viene chiamato

        menuImgAttach.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (imageOutputPath != null)
                    v.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.wiggle));
                return imageOutputPath != null;
            }
        });
        menuImgCamera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (imageOutputPath != null)
                    v.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.wiggle));
                return imageOutputPath != null;
            }
        });
        menuImgMic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (audioOutputPath != null)
                    v.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.wiggle));
                return audioOutputPath != null;
            }
        });
    }

    // metodo chiamato quando viene chiuso il dialog per salvare la nota
    private Nota saveNote() {
        String titoloTemp = titolo.getText().toString().trim();
        String testoTemp = etxtNota.getText().toString().trim();
        String tagTemp = tag.getText().toString().trim();
        tagTemp = tagTemp.equals("") ? null : tagTemp;
        Log.d(TAG, String.format("saveNote: %s, %s, %s", titoloTemp, testoTemp, tagTemp));

        if (titoloTemp.length() > 0 || testoTemp.length() > 0 ||
                (audioOutputPath != null && audioOutputPath.trim().length() > 0) ||
                (imageOutputPath != null && imageOutputPath.trim().length() > 0)) {   //se c'è almeno uno dei parametri


            Nota nota;
            if (guid == null) {
                nota = new Nota();
            } else {
                nota = CouchbaseDB.getInstance().leggiNota(guid);
            }
            if (nota == null) {
                nota = new Nota();
            }
            guid = nota.getID();
            String titoloNota = (titoloTemp.length() > 0 ? titoloTemp : "Nota senza titolo");
            nota.setTitle(titoloNota);
            nota.setText(testoTemp);
            nota.setColor(noteColor);
            nota.setTag(tagTemp);
            nota.setCreationDate(new Date());
            nota.setLastModifiedDate(new Date());
            nota.setExpireDate(expirationDate);
            nota.setAudio(audioOutputPath);
            nota.setImage(imageOutputPath);

            try {
                CouchbaseDB.getInstance().salvaNota(nota);
                Log.d(TAG, "saveNote: nota salvata");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            return nota;
        } else {
            Log.d(TAG, "saveNote: nota non salvata");
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
            timeProgressSnackbar.setAction(getString(R.string.stop), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopRecording();
                    timeProgressSnackbar.dismiss();
                    tapBarMenu.setVisibility(View.VISIBLE);
                }
            });
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
        audioOutputPath = NoteMeUtils.getAudioPath() + (new Date()).getTime() + ".3gp";
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

    public void takeImageFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = NoteMeUtils.createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
            // Error occurred while creating the File
        }
        // Continue only if the File was successfully created
        if (photoFile != null) {
//            takePictureIntent.putExtra(MediaStore., Uri.fromFile(photoFile));
            getActivity().startActivityForResult(takePictureIntent, CAMERA_REQUEST);
        }

    }

    public void activityResult(final Intent intent, final int requestCode) {
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
                    file = NoteMeUtils.saveCompressedPicture(intent.getData(), 50);

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
                if (file != null) {
                    imageOutputPath = file.getPath();
                }
                Log.d(TAG, "onPostExecute: " + imageOutputPath);
                setImagePreview();
                updateBottomMenu();
            }
        }.execute();
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
                                    if (file.delete()) {
                                        audioOutputPath = null;
                                        titoloAudio.setVisibility(View.GONE);
                                        immagineAudio.setVisibility(View.GONE);
                                        updateBottomMenu();
                                    }
                                }
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

    private void setImagePreview() {
        if (imageOutputPath != null) {

            Log.d("IMAGE_PATH", "" + imageOutputPath);
            immagine.setVisibility(View.VISIBLE);
            immagine.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                    dialogBuilder.setMessage(getString(R.string.eliminare_foto))
                            .setPositiveButton(getString(R.string.elimina), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (imageOutputPath != null) {
                                        File file = new File(imageOutputPath);
                                        if (file.exists()) {
                                            if (file.delete()) {
                                                immagine.setVisibility(View.GONE);
                                                imageOutputPath = null;
                                                updateBottomMenu();
                                            }
                                        }
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(getString(R.string.annulla), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    Dialog dialog = dialogBuilder.create();
                    dialog.show();
                    return false;
                }
            });
            immagine.post(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(getContext())
                            .load(new File(imageOutputPath))
                            .resize(immagine.getWidth(), immagine.getHeight())
                            .centerCrop()
                            .into(immagine);
                }
            });
        } else {
            immagine.setVisibility(View.GONE);
        }
    }

    private void setExpirationDatePreview() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", NoteMeApp.getInstance().getLocale());
        txtDataScadenza.setText(getString(R.string.scade) + " " + sdf.format(expirationDate));
        txtDataScadenza.setVisibility(View.VISIBLE);
    }

    private void setColorPreview() {
        dialogView.post(new Runnable() {
            @Override
            public void run() {
                dialogView.setBackgroundColor(noteColor);
            }
        });
    }

    private void showExpirationDateDialogs(final Callback callback) {
        final Date selectedDate = new Date(0);
        final Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = DatePickerDialog.newInstance(
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
                        selectedDate.setYear(year - 1900);
                        selectedDate.setMonth(monthOfYear);
                        selectedDate.setDate(dayOfMonth);

                        TimePickerDialog timePickerDialog = TimePickerDialog.newInstance(
                                new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
                                        selectedDate.setHours(hourOfDay);
                                        selectedDate.setMinutes(minute);
                                        selectedDate.setSeconds(second);
                                        callback.call(selectedDate.getTime());
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                        );
                        timePickerDialog.show(getActivity().getFragmentManager(), TAG_TIME_PICKER);
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show(getActivity().getFragmentManager(), TAG_DATE_PICKER);
    }
}
