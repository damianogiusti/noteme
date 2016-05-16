package it.tsamstudio.noteme;


import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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

import com.squareup.picasso.Picasso;

import java.io.File;

import nl.changer.audiowife.AudioWife;


/**
 * A simple {@link Fragment} subclass.
 */
public class MostraNotaFragment extends DialogFragment {

    private static final String TAG = "MostraNotaFragment";

    private View dialogNoteView;
    private EditText txtTitle, txtContent;
    private Nota nota;

    // player audio
    private ImageButton btnPlayPause;
    private SeekBar seekbarTime;
    private TextView txtTimer;
    // anteprima immagine
    private ImageView imgThumbnail;

    private final static String NOTA_KEY_FOR_BUNDLE = "notaParceable";

    public MostraNotaFragment() {
        // Required empty public constructor
    }

    public static MostraNotaFragment newInstance(Nota nota) {
        MostraNotaFragment mostraNotaFragment = new MostraNotaFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(NOTA_KEY_FOR_BUNDLE, nota);
        mostraNotaFragment.setArguments(bundle);
        return mostraNotaFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogNoteView = inflater.inflate(R.layout.fragment_mostra_nota, null, false);

        Bundle dBundle = getArguments();
        nota = dBundle.getParcelable(NOTA_KEY_FOR_BUNDLE);

        txtTitle = (EditText) dialogNoteView.findViewById(R.id.txtTitle);
        txtContent = (EditText) dialogNoteView.findViewById(R.id.txtContent);

        txtTitle.setText(nota.getTitle());
        txtContent.setText(nota.getText());

        // se ho una nota audio do la possibilita di riprodurla, altrimenti non mostro il player
        RelativeLayout audioPlayerLayout = (RelativeLayout) dialogNoteView.findViewById(R.id.audioPlayerLayout);
        if (nota.getAudio() != null) {

            btnPlayPause = (ImageButton) dialogNoteView.findViewById(R.id.btnPlayPause);
            seekbarTime = (SeekBar) dialogNoteView.findViewById(R.id.seekbarTime);
            txtTimer = (TextView) dialogNoteView.findViewById(R.id.txtTimer);

            Log.d(TAG, nota.getAudio());

            File file = new File(nota.getAudio());
            AudioWife.getInstance()
                    .init(getContext(), Uri.fromFile(file))
                    .setPlayView(btnPlayPause)
                    .setPauseView(btnPlayPause)
                    .setSeekBar(seekbarTime)
                    .setRuntimeView(txtTimer)
                    .addOnPlayClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            btnPlayPause.setBackgroundResource(R.drawable.ic_play_circle_orange);
                        }
                    })
                    .addOnPauseClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            btnPlayPause.setBackgroundResource(R.drawable.ic_pause_circle_orange);
                        }
                    });
        } else {
            audioPlayerLayout.setVisibility(View.GONE);
        }

        // se ho una nota con immagine mostro l'immagine, altrimenti non mostro nulla
        imgThumbnail = (ImageView) dialogNoteView.findViewById(R.id.imgThumbnail);
        if (nota.getImage() != null) {
            Picasso.with(getContext())
                    .load(nota.getImage())
                    .fit()
                    .into(imgThumbnail);
        } else {
            imgThumbnail.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogNoteView);

        Dialog dialogShowNote = builder.create();
        return dialogShowNote;

    }
}
