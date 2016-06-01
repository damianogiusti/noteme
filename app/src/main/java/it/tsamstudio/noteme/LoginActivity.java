package it.tsamstudio.noteme;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;

import java.io.File;
import java.io.IOException;

import it.tsamstudio.noteme.utils.NoteMeUtils;
import it.tsamstudio.noteme.utils.S3Manager;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextView txtUsername;
    private TextView txtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        txtUsername = (TextView) findViewById(R.id.txtUsername);
        txtPassword = (TextView) findViewById(R.id.txtPassword);

        final Button btnLogin = (Button) findViewById(R.id.btnLogin);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = txtUsername.getText().toString().trim();
                    String password = txtPassword.getText().toString().trim();
                    if (validaUtente(username, password)) {
                        User.getInstance().initWithCredentials(username, password);

                        testSync();

                    } else {
                        if (NoteMeUtils.isBlank(username)) {
                            txtUsername.requestFocus();
                        } else {
                            txtPassword.requestFocus();
                        }
                        Snackbar.make(v, getString(R.string.invalid_credentials), Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private boolean validaUtente(String username, String password) {
        username = username.trim();
        password = password.trim();

        return !NoteMeUtils.isBlank(username) && !NoteMeUtils.isBlank(password);
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void testSync() {
        S3Manager.getInstance().sync(new S3Manager.SyncListener() {
                                         @Override
                                         public void onSyncStarted() {
                                             Log.d(TAG, "onSyncStarted: ");
                                         }

                                         @Override
                                         public void onSyncFinished() {
                                             Log.d(TAG, "onSyncFinished: ");
                                             toast("Finita la sync");
                                         }
                                     },
                new S3Manager.MultipleTransferListener() {
                    @Override
                    public void onFileTransferred(File file, int transferType) {
                        Log.d(TAG, "onFileTransferred: " + file.getName());
                    }

                    @Override
                    public void onFileTransferFailed(File file, int transferType, Exception e) {
                        if (e != null) {
                            Log.e(TAG, "onFileTransferFailed: " + file.getName(), e);
                        }
                        Log.wtf(TAG, "onFileTransferFailed: " + file.getName());
                    }

                    @Override
                    public void onFilesProgressChanged(int currentFile, int totalFiles, int transferType) {
                        Log.d(TAG, "onFilesProgressChanged: " + currentFile + "/" + totalFiles);
                    }

                    @Override
                    public void onSingleFileProgressChanged(File file, long currentBytes, long totalBytes, int transferType) {
                        Log.d(TAG, "onSingleFileProgressChanged: " + file.getName());
                    }

                    @Override
                    public void onFinish(int transferType) {
                        Log.d(TAG, "onFinish: ");
                    }
                });
    }

    private void testEliminazione() {
        try {
            for (Nota nota : CouchbaseDB.getInstance().leggiNote()) {
                CouchbaseDB.getInstance().eliminaNota(nota);
                S3Manager.getInstance().deleteNoteFromRemote(nota, new S3Manager.DeletionListener() {
                    @Override
                    public void onDeleted(Nota nota) {
                        toast("nota cancellata");
                    }
                });
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testUploadNote() {
        try {
            for (Nota nota : CouchbaseDB.getInstance().leggiNote()) {
                S3Manager.getInstance().uploadNota(nota, null);
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testDownloadAll() {
        S3Manager.getInstance().downloadAllNotes(null);
    }
}