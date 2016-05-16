package it.tsamstudio.noteme;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class MostraNotaFragment extends DialogFragment {

    private View dialogNoteView;
    private TextView txtTitle, txtContent;
    private String title, content;

    private final static String TITOLO = "title";
    private final static String CONTENUTO = "content";

    public MostraNotaFragment() {
        // Required empty public constructor
    }

    public static MostraNotaFragment newInstance(String title, String content) {
        MostraNotaFragment mostraNotaFragment = new MostraNotaFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TITOLO, title);
        bundle.putString(CONTENUTO, content);
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

        txtTitle = (TextView) dialogNoteView.findViewById(R.id.txtTitle);
        txtContent = (TextView) dialogNoteView.findViewById(R.id.txtContent);

        Bundle dBundle = getArguments();
        title = dBundle.getString(TITOLO);
        content = dBundle.getString(CONTENUTO);

        txtTitle.setText(title);
        txtContent.setText(content);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogNoteView);

        Dialog dialogShowNote = builder.create();
        dialogShowNote.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialogShowNote;

    }
}
