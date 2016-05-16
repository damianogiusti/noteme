package it.tsamstudio.noteme;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        NuovaNotaFragment.INuovaNota {
    private static final String TAG = "HomeActivity";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

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
                    NuovaNotaFragment nuovaNotaFragment = NuovaNotaFragment.newInstance();
                    nuovaNotaFragment.show(getSupportFragmentManager(), "DIALOG");
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

        mAdapter = new NotesRecyclerViewAdapter(notesList);
        mRecyclerView.setAdapter(mAdapter);
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
                MostraNotaFragment fragmentMostraNota = MostraNotaFragment.newInstance(t, cont);
                fragmentMostraNota.show(getSupportFragmentManager(), "DIALOG");
            }
        });
    }

    //metodo per creare note a caso (per testare)
    private ArrayList<Nota> getDataSet() {
        notesList = new ArrayList<Nota>();

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
        notesList.add(nota);
        mAdapter = new NotesRecyclerViewAdapter(notesList);
        mRecyclerView.setAdapter(mAdapter);
    }
}
