package it.tsamstudio.noteme;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.couchbase.lite.CouchbaseLiteException;

import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 */
public class NuovaNotaFragment extends DialogFragment {

    private View dialogView;
    private TextView titolo, nota;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDetach(){
        super.onDetach();
        saveNote();
        Log.d("DETACH", "onDETACH");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogView = inflater.inflate(R.layout.fragment_nuova_nota, null, false);
        AHBottomNavigation bottomNavigation = (AHBottomNavigation) dialogView.findViewById(R.id.bottomNavigation);

        titolo = (TextView)dialogView.findViewById(R.id.etxtTitolo);
        nota = (TextView)dialogView.findViewById(R.id.etxtNota);

        //for (int i = 0; i < 4; i++) {
        bottomNavigation.addItem(new AHBottomNavigationItem("" + 0, R.drawable.ic_menu_camera));
        bottomNavigation.addItem(new AHBottomNavigationItem("" + 1, R.drawable.ic_mic));
        //}

        //bottomBar = setupBottomBar(dialogView, savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    /*
        private BottomBar setupBottomBar(View view, Bundle savedInstanceState) {
            BottomBar bottomBar = BottomBar.attach(view, savedInstanceState);
            bottomBar.setItemsFromMenu(R.menu.bottom_bar_menu, new OnMenuTabClickListener() {
                @Override
                public void onMenuTabSelected(int menuItemId) {
                    // TODO switch
                }

                @Override
                public void onMenuTabReSelected(int menuItemId) {

                }
            });

            return bottomBar;
        }
    */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    //metodo chiamato quando viene chiuso il dialog
    private void saveNote(){
        if (titolo.getText().length() > 0 || nota.getText().length() > 0) {   //se c'è almeno uno dei parametri
            Nota nota = new Nota();
            String titoloNota = "Nota senza titolo";
            if (titolo.getText().length() > 0)
                titoloNota = "" + titolo.getText();
            nota.setTitle("" + titoloNota);
            nota.setText("" + nota.getText());
            CouchbaseDB db = new CouchbaseDB(getContext());
            try {
                db.salvaNota(nota);
                Log.d("NOTA", "NOTA SALVATA");
                Log.d("NOTA", "ci sono" + db.leggiNote().size());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        } else{
            Log.d("NOTA", "NOTA NON SALVATA");
        }

    }
}
