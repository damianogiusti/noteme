package it.tsamstudio.noteme;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.couchbase.lite.CouchbaseLiteException;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import it.tsamstudio.noteme.utils.NoteMeUtils;
import it.tsamstudio.noteme.utils.S3Manager;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NuovaNotaFragment.INuovaNota,
        MostraNotaFragment.IMostraNota {

    private static final String TAG = "HomeActivity";
    public static final String TAG_DIALOG_NUOVA_NOTA = "dialognuovanota";
    public static final int CAMERA_CODE = 1000;
    public static final int GALLERY_CODE = 2000;
    private static final String TAG_DIALOG_MOSTRA_NOTA = "dialogmostranota";
    private static final String TAG_LIST_NOTE_FOR_BUNDLE = "taglistnoteforbundle";
    private static final String TAG_LIST_STATE_FOR_BUNDLE = "tagliststatefrasdflj";

    private RecyclerView recyclerView;
    private NotesRecyclerViewAdapter mAdapter;
    private RecyclerView.Adapter searchAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private FloatingActionButton fab;

    private NuovaNotaFragment nuovaNotaFragment;
    private MostraNotaFragment fragmentMostraNota;

    ArrayList<Nota> notesList, searchList;

    private SearchView searchView;
    private ImageView closeSearch;  //"x" nella SearchView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            nuovaNotaFragment = ((NuovaNotaFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_DIALOG_NUOVA_NOTA));
            fragmentMostraNota = ((MostraNotaFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_DIALOG_MOSTRA_NOTA));
            notesList = savedInstanceState.getParcelableArrayList(TAG_LIST_NOTE_FOR_BUNDLE);
//            mLayoutManager.onRestoreInstanceState(savedInstanceState.getParcelable(TAG_LIST_STATE_FOR_BUNDLE));
        }

        fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (nuovaNotaFragment == null) {
                        nuovaNotaFragment = NuovaNotaFragment.newInstance();
                    }
                    nuovaNotaFragment.show(getSupportFragmentManager(), TAG_DIALOG_NUOVA_NOTA);
                }
            });
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_for_notes);

        int colonne = 2;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            colonne = 3;
        }

        mLayoutManager = new StaggeredGridLayoutManager(colonne, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);

        if (notesList == null) {
            try {
                notesList = CouchbaseDB.getInstance().leggiNote();
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final SwipeableRecyclerViewTouchListener swipeListener = new SwipeableRecyclerViewTouchListener(recyclerView,
                new SwipeableRecyclerViewTouchListener.SwipeListener() {
                    @Override
                    public boolean canSwipeLeft(int position) {
                        // ELIMINAZIONE
                        // true per attivare
                        return true;
                    }

                    @Override
                    public boolean canSwipeRight(int position) {
                        //
                        // true per attivare
                        return false;
                    }

                    @Override
                    public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        deleteNotesByIndex(reverseSortedPositions);
                    }

                    @Override
                    public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        // TODO
                    }
                });

        recyclerView.addOnItemTouchListener(swipeListener);
        mAdapter = new NotesRecyclerViewAdapter(notesList);
        recyclerView.setAdapter(mAdapter);

        // elimino le note scadute
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Date today = new Date();
                        ArrayList<Integer> indexPool = new ArrayList<>();
                        for (int i = 0; i < notesList.size(); i++) {
                            if (notesList.get(i).getExpireDate() != null &&
                                    notesList.get(i).getExpireDate().before(today)) {
                                indexPool.add(i);
                            }
                        }
//                        for (int index : indexPool) {
//                            notesList.remove(index);
//                            mAdapter.notifyItemRemoved(index);
//                        }
//                        if (indexPool.size() > 0)
//                            Snackbar.make(recyclerView, getString(R.string.note_scadute_eliminate), Snackbar.LENGTH_LONG).show();
                        if (indexPool.size() > 0)
                            deleteNotesByIndex(NoteMeUtils.arrayListToArray(indexPool));
                    }
                }, 500);
            }
        });
    }

    private void deleteNotesByIndex(int[] indexes) {
        // TODO eliminazione con possibilità di ripristino
        final ArrayList<Nota> noteEliminate = new ArrayList<>(notesList.size());

        for (int i = 0; i < notesList.size(); i++)
            noteEliminate.add(null);

        // ciclo gli indici al contrario per non tirare IndexOutOfBoundsException
        for (int i = indexes.length - 1; i >= 0; i--) {
            noteEliminate.set(indexes[i], notesList.get(indexes[i]));
            notesList.remove(indexes[i]);
            mAdapter.notifyItemRemoved(indexes[i]);
        }
        Snackbar snackNotaEliminata = Snackbar.make(recyclerView, getString(R.string.nota_eliminata), Snackbar.LENGTH_LONG);
        snackNotaEliminata.setAction(getString(R.string.annulla), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < noteEliminate.size(); i++) {
                    if (noteEliminate.get(i) != null) {
                        notesList.add(i, noteEliminate.get(i));
                        mAdapter.notifyItemInserted(i);
                    }
                }
                noteEliminate.clear();
            }
        });
        snackNotaEliminata.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                super.onDismissed(snackbar, event);
                for (Nota n : noteEliminate) {
                    try {
                        if (n != null) {
                            CouchbaseDB.getInstance().eliminaNota(n);
                            S3Manager.getInstance().deleteNoteFromRemote(n, new S3Manager.DeletionListener() {
                                @Override
                                public void onDeleted(Nota nota) {
                                    Log.d(TAG, "onDeleted: nota (" + nota.getID() + ") cancellata");
                                }
                            });
                        }
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        snackNotaEliminata.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {

        if (fragmentMostraNota != null)
            if (!fragmentMostraNota.onBackPressed())
                return;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        setupSearchBar(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            // TODO impostazioni
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        mAdapter.setOnItemClickListener(new NotesRecyclerViewAdapter.MyClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                Log.d("DEBUG CLICK NOTA", "NOTA PREMUTA:" + position);
                Nota n = notesList.get(position);
                if (searchList != null)
                    n = searchList.get(position);

                fragmentMostraNota = MostraNotaFragment.newInstance(n, position);
                fragmentMostraNota.show(getSupportFragmentManager(), TAG_DIALOG_MOSTRA_NOTA);
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(TAG_LIST_NOTE_FOR_BUNDLE, notesList);

        if (nuovaNotaFragment != null &&
                getSupportFragmentManager().findFragmentByTag(TAG_DIALOG_NUOVA_NOTA) != null) {
            getSupportFragmentManager().putFragment(outState, TAG_DIALOG_NUOVA_NOTA, nuovaNotaFragment);
        }
        if (fragmentMostraNota != null &&
                getSupportFragmentManager().findFragmentByTag(TAG_DIALOG_MOSTRA_NOTA) != null) {
            getSupportFragmentManager().putFragment(outState, TAG_DIALOG_MOSTRA_NOTA, fragmentMostraNota);
        }
    }

    //metodo per creare note a caso (per testare)
    private ArrayList<Nota> getDataSet() {
        notesList = new ArrayList<>();

        for (int index = 0; index < 20; index++) {
            Nota note = new Nota();
            note.setTitle("TITOLONE " + index);
            if (index % 2 == 0) {
                note.setText("robe a caso per debug, numero: " + index);
            } else {
                note.setText("robe a caso per debug, lorem ipsum darem sit dolor amet vamos alla playa ritmo de las noce: " + index);
            }

            note.setTag("family");
            note.setExpireDate(new Date());
            notesList.add(index, note);
        }
        /*CouchbaseDB db = new CouchbaseDB(getApplicationContext());
         *note = db.leggiNote()
        * */
        return notesList;
    }

    @Override
    @UiThread
    public void onNuovaNotaAggiunta(Nota nota) {
        Log.d(TAG, "onNuovaNotaAggiunta: nota is null? " + (nota == null));
        if (nota != null) {
            notesList.add(0, nota);
            mAdapter.notifyItemInserted(0);
//            Collections.sort(notesList, new Comparator<Nota>() {
//                @Override
//                public int compare(Nota lhs, Nota rhs) {
//                    return -1 * lhs.getLastModifiedDate().compareTo(rhs.getLastModifiedDate());
//                }
//            });
        }
    }

    @Override
    public void onButtonClick(int request) {
        launchIntent(request);
    }

    private void launchIntent(int request) {
        if (request == GALLERY_CODE) {       //galleria
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(i, GALLERY_CODE);
        } else if (request == CAMERA_CODE) {    //scatta foto
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // se ricevo un intent da galleria o da fotocamera
        // chiamo il metodo del fragment che salva la foto compressa
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_CODE || requestCode == NuovaNotaFragment.CAMERA_REQUEST) {
                nuovaNotaFragment.activityResult(data, requestCode);
            }
        }

    }

    public void onNotaModificata(Nota nota, int position) {
        if (nota != null && searchList == null) {
            try {
                if (position > -1) {
                    notesList.set(position, nota);
                } else {
                    notesList = CouchbaseDB.getInstance().leggiNote();
                }
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (position > -1) {
                mAdapter.notifyItemChanged(position);
            } else {
                mAdapter.notifyDataSetChanged();
            }
            Log.d(TAG, "onNotaModificata: ");
        }
    }

    private void setupSearchBar(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            //ricerca quando il testo nella searchview cambia
            @Override
            public boolean onQueryTextChange(String newText) {
                if (fab.isShown())
                    fab.setVisibility(View.GONE);

                newText = newText.toLowerCase().trim();

                searchList = new ArrayList<>();
                searchAdapter = new NotesRecyclerViewAdapter(searchList);
                recyclerView.swapAdapter(searchAdapter, false);
                for (Nota x : notesList) {
                    if (x.getText().toLowerCase().contains(newText) ||
                            x.getTitle().toLowerCase().contains(newText) ||
                            (x.getTag() != null && x.getTag().toLowerCase().contains(newText))) {
                        searchList.add(x);
                    }
                }
                if (!searchList.isEmpty()) {
                    searchAdapter.notifyDataSetChanged();
                }
                return true;
            }
        });

        // Define the listener
        MenuItemCompat.OnActionExpandListener expandListener = new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.setQuery("", false);
                searchView.setIconified(true);
//                searchList.clear();
                recyclerView.swapAdapter(mAdapter, false);
                searchAdapter = null;
                searchList = null;
                fab.setVisibility(View.VISIBLE);
                return true;  // Return true to collapse action view
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (fab.isShown())
                    fab.setVisibility(View.GONE);
                return true;  // Return true to expand action view
            }
        };


        // Assign the listener to that action item
        MenuItemCompat.setOnActionExpandListener(searchItem, expandListener);


//        int searchCloseButtonId = searchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null);
//        closeSearch = (ImageView) this.searchView.findViewById(searchCloseButtonId);
//        closeSearch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                searchView.setQuery("", false);
//                searchView.setIconified(true);
////                searchList.clear();
//                recyclerView.swapAdapter(mAdapter, false);
//                searchAdapter = null;
//                searchList = null;
//                fab.setVisibility(View.VISIBLE);
//            }
//        });
    }
}
