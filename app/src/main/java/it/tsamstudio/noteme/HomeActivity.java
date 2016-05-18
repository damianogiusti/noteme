package it.tsamstudio.noteme;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.couchbase.lite.CouchbaseLiteException;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NuovaNotaFragment.INuovaNota {

    private static final String TAG = "HomeActivity";
    public static final String TAG_DIALOG_NUOVA_NOTA = "dialognuovanota";
    public static final int CAMERA_CODE = 1000;
    public static final int GALLERY_CODE = 2000;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private NuovaNotaFragment nuovaNotaFragment;

    private CouchbaseDB database;
    ArrayList<Nota> notesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    nuovaNotaFragment = NuovaNotaFragment.newInstance();
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

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_for_notes);
        //mLayoutManager = new GridLayoutManager(getApplicationContext(), 2);
        mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        database = new CouchbaseDB(getApplicationContext());
        notesList = new ArrayList<>();
        try {
            notesList = database.leggiNote();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SwipeableRecyclerViewTouchListener swipeListener = new SwipeableRecyclerViewTouchListener(mRecyclerView,
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
                        // TODO eliminazione con possibilità di ripristino
                        final ArrayList<Nota> noteEliminate = new ArrayList<>(notesList.size());
                        boolean devoRipristinare = false;

                        for (int i = 0; i < notesList.size(); i++)
                            noteEliminate.add(null);

                        for (int i : reverseSortedPositions) {
                            noteEliminate.set(i, notesList.get(i));
                            notesList.remove(i);
                            mAdapter.notifyDataSetChanged();
                        }
                        Snackbar snackNotaEliminata = Snackbar.make(recyclerView, getString(R.string.nota_eliminata), Snackbar.LENGTH_LONG);
                        snackNotaEliminata.setAction(getString(R.string.annulla), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                for (int i = 0; i < noteEliminate.size(); i++) {
                                    if (noteEliminate.get(i) != null)
                                        notesList.add(i, noteEliminate.get(i));
                                }
                                mAdapter.notifyDataSetChanged();
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
                                            database.eliminaNota(n);
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
                    public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                        // TODO
                    }
                });
        mRecyclerView.addOnItemTouchListener(swipeListener);
        mAdapter = new NotesRecyclerViewAdapter(notesList);
        mRecyclerView.setAdapter(mAdapter);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //onClick della nota da implementare
    @Override
    protected void onResume() {
        super.onResume();
        ((NotesRecyclerViewAdapter) mAdapter).setOnItemClickListener(new NotesRecyclerViewAdapter
                .MyClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                Log.d("DEBUG CLICK NOTA", "NOTA PREMUTA:" + position);
                Nota n = (Nota) notesList.get(position);
                String t = n.getTitle();
                String cont = n.getText();
                MostraNotaFragment fragmentMostraNota = MostraNotaFragment.newInstance(n);
                fragmentMostraNota.show(getSupportFragmentManager(), "DIALOG");
            }
        });
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
    public void onNuovaNotaAggiunta(Nota nota) {
        if (nota != null) {
            notesList.add(nota);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onButtonClick(int request) {
        launchIntent(request);
    }

    private void launchIntent(int request){
        if (request == GALLERY_CODE){       //galleria
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(i, GALLERY_CODE);
        }else if (request == CAMERA_CODE) {    //scatta foto
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, CAMERA_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK){
            nuovaNotaFragment.activityResult(data.getData());
        }

    }
}
