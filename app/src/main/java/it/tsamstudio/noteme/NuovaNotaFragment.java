package it.tsamstudio.noteme;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;


/**
 * A simple {@link Fragment} subclass.
 */
public class NuovaNotaFragment extends DialogFragment {

    private View dialogView;
    private BottomBar bottomBar;

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


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogView = inflater.inflate(R.layout.fragment_nuova_nota, null, false);
        //bottomBar = setupBottomBar(dialogView, savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView);

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (bottomBar != null) {
            bottomBar.onSaveInstanceState(outState);
        }
    }
}
