package it.tsamstudio.noteme;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.aurelhubert.ahbottomnavigation.AHBottomNavigation;
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem;
import com.aurelhubert.ahbottomnavigation.AHClickListener;



/**
 * A simple {@link Fragment} subclass.
 */
public class NuovaNotaFragment extends DialogFragment {

    private View dialogView;

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

            }

            @Override
            public boolean onLongClickListener(View view) {
                Log.d("onLongPress", "" + bottomNavigation.getCurrentItem());
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
                Log.d("onLongPress", "" + bottomNavigation.getCurrentItem());
                return true;
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
}
