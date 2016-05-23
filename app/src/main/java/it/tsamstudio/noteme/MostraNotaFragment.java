package it.tsamstudio.noteme;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Date;

import it.tsamstudio.noteme.utils.AudioPlayerManager;


/**
 * A simple {@link Fragment} subclass.
 */
public class MostraNotaFragment extends DialogFragment {

    private static final String TAG = "MostraNotaFragment";

    private Dialog dialogShowNote;

    private View dialogNoteView;
    private EditText txtTitle, txtContent;
    private Nota nota;
    private CouchbaseDB database;

    // player audio
    private ImageButton btnPlayPause;
    private SeekBar seekbarTime;
    private TextView txtTimer;
    // anteprima immagine
    private ImageView imgThumbnail;

    private final static String NOTA_KEY_FOR_BUNDLE = "notaParceable";
    private final static String POSITION_KEY_FOR_BUNDLE = "posizioneNota";

    public interface IMostraNota {
        void onNotaModificata(Nota nota, int position);
    }

    private IMostraNota listener = new IMostraNota() {
        @Override
        public void onNotaModificata(Nota nota, int position) {
            Log.d(TAG, "IMostraNota: dummy init");
        }
    };

    public MostraNotaFragment() {
        // Required empty public constructor
    }

    public static MostraNotaFragment newInstance(Nota nota) {
        return newInstance(nota, -1);
    }

    public static MostraNotaFragment newInstance(Nota nota, int position) {
        MostraNotaFragment mostraNotaFragment = new MostraNotaFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(NOTA_KEY_FOR_BUNDLE, nota);
        bundle.putInt(POSITION_KEY_FOR_BUNDLE, position);
        mostraNotaFragment.setArguments(bundle);
        return mostraNotaFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        database = new CouchbaseDB(getActivity().getApplicationContext());
        getDialog().setCanceledOnTouchOutside(false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogNoteView = inflater.inflate(R.layout.fragment_mostra_nota, null, false);

        if (savedInstanceState == null)
            savedInstanceState = getArguments();
        nota = savedInstanceState.getParcelable(NOTA_KEY_FOR_BUNDLE);

        txtTitle = (EditText) dialogNoteView.findViewById(R.id.txtTitle);
        txtContent = (EditText) dialogNoteView.findViewById(R.id.txtContent);

        txtTitle.setText(nota.getTitle());
        txtTitle.setFocusableInTouchMode(false);
        txtTitle.setClickable(true);
        txtTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!txtTitle.isFocusableInTouchMode()) {
                    Log.d(TAG, "onClick: txtTitle");
                    txtTitle.setFocusableInTouchMode(true);
                    txtContent.setFocusableInTouchMode(false);
                }
            }
        });

        txtContent.setText(nota.getText());
        txtContent.setFocusableInTouchMode(false);
        txtContent.setClickable(true);
        Linkify.addLinks(txtContent, Linkify.WEB_URLS);
        txtContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!txtContent.isFocusableInTouchMode()) {
                    Log.d(TAG, "onClick: txtContent");
                    txtContent.setFocusableInTouchMode(true);
                    txtTitle.setFocusableInTouchMode(false);
                }
            }
        });

        // se ho una nota audio do la possibilita di riprodurla, altrimenti non mostro il player
        RelativeLayout audioPlayerLayout = (RelativeLayout) dialogNoteView.findViewById(R.id.audioPlayerLayout);
        if (nota.getAudio() != null) {

            btnPlayPause = (ImageButton) dialogNoteView.findViewById(R.id.btnPlayPause);
            seekbarTime = (SeekBar) dialogNoteView.findViewById(R.id.seekbarTime);
            txtTimer = (TextView) dialogNoteView.findViewById(R.id.txtTimer);

            Log.d(TAG, nota.getAudio());

            // istanzio il player
            AudioPlayerManager.getInstance()
                    // lo inizializzo col percorso del file
                    .init(nota.getAudio())
                            // imposto il listener per aggiornare il cursore quando riproduce l'audio
                    .setSeekChangeListener(new AudioPlayerManager.SeekChangeListener() {
                        @Override
                        public void onSeekChanged(int position) {
                            seekbarTime.setProgress(position);
                            txtTimer.setText(AudioPlayerManager.formatTiming(position));
                        }
                    })
                            // imposto il listener per sapere quando è finita la riproduzione dell'audio
                    .setAudioPlayingListener(new AudioPlayerManager.AudioPlayingListener() {
                        @Override
                        public void onPlayingFinish() {
                            btnPlayPause.setBackgroundResource(R.drawable.ic_play_circle_orange);
                            seekbarTime.setProgress(0);
                            txtTimer.setText(AudioPlayerManager.formatTiming(0));
                        }
                    });

            seekbarTime.setProgress(0);
            seekbarTime.setMax(AudioPlayerManager.getInstance().getAudioDuration());
            txtTimer.setText(AudioPlayerManager.formatTiming(0));

            seekbarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    txtTimer.setText(AudioPlayerManager.formatTiming(progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    AudioPlayerManager.getInstance().changeSeek(seekBar.getProgress());
                }
            });

            btnPlayPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (AudioPlayerManager.getInstance().isStopped()) {
                        AudioPlayerManager.getInstance().startPlaying();
                        btnPlayPause.setBackgroundResource(R.drawable.ic_pause_circle_orange);
                    } else if (AudioPlayerManager.getInstance().isPlaying()) {
                        AudioPlayerManager.getInstance().pausePlaying();
                        btnPlayPause.setBackgroundResource(R.drawable.ic_play_circle_orange);
                    } else if (AudioPlayerManager.getInstance().isPaused()) {
                        AudioPlayerManager.getInstance().resumePlaying();
                        btnPlayPause.setBackgroundResource(R.drawable.ic_pause_circle_orange);
                    }
                }
            });
        } else {
            audioPlayerLayout.setVisibility(View.GONE);
        }

        // se ho una nota con immagine mostro l'immagine, altrimenti non mostro nulla
        imgThumbnail = (ImageView) dialogNoteView.findViewById(R.id.imgThumbnail);
        if (nota.getImage() != null) {
            Log.d(TAG, "onCreateDialog: ho una immagine");
            Log.d(TAG, "onCreateDialog: " + nota.getImage());
            imgThumbnail.setVisibility(View.VISIBLE);
            imgThumbnail.post(new Runnable() {
                @Override
                public void run() {
                    Picasso.with(getActivity())
                            .load("file://" + nota.getImage())
                            .resize(imgThumbnail.getWidth(), imgThumbnail.getHeight())
                            .centerCrop()
                            .into(imgThumbnail, new Callback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "onSuccess: picasso loaded");
                                }

                                @Override
                                public void onError() {
                                    Log.e(TAG, "onError: picasso error");
                                }
                            });
                }
            });

        } else {
            imgThumbnail.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogNoteView);

        dialogShowNote = builder.create();
        return dialogShowNote;

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof IMostraNota) {
            listener = (IMostraNota) activity;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        listener.onNotaModificata(updateNote(), getArguments().getInt(POSITION_KEY_FOR_BUNDLE));

    }

    private Nota updateNote() {
        String title = txtTitle.getText().toString().trim();
        String text = txtContent.getText().toString().trim();

        nota.setTitle((title.length() > 0) ? title : getString(R.string.nota_senza_titolo));
        nota.setLastModifiedDate(new Date());

        if ((title.length() > 0 && text.length() > 0)
                || (title.length() > 0 && text.length() == 0)
                || (title.length() == 0 && text.length() > 0)) {
            nota.setText(txtContent.getText().toString());
            try {
                database.salvaNota(nota);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }
        return nota;
    }

    public boolean onBackPressed() {
        Log.d(TAG, "onBackPressed: ");
        if (txtTitle.isFocusableInTouchMode() || txtContent.isFocusableInTouchMode()) {
            txtTitle.setFocusableInTouchMode(false);
            txtContent.setFocusableInTouchMode(false);
            return false;
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(NOTA_KEY_FOR_BUNDLE, nota);
    }
}
