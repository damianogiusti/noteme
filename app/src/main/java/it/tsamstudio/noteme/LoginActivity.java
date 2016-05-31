package it.tsamstudio.noteme;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;

import java.io.IOException;

import it.tsamstudio.noteme.utils.NoteMeUtils;

public class LoginActivity extends AppCompatActivity {

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

        Button btnLogin = (Button) findViewById(R.id.btnLogin);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = txtUsername.getText().toString().trim();
                    String password = txtPassword.getText().toString().trim();
                    if (validaUtente(username, password)) {
                        User.getInstance().initWithCredentials(username, password);
                        try {
                            CouchbaseDB.getInstance().salvaUtente();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (CouchbaseLiteException e) {
                            e.printStackTrace();
                        }
                        try {
                            CouchbaseDB.getInstance().leggiUtente();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            CouchbaseDB.getInstance().eliminaUtente();
                        } catch (CouchbaseLiteException e) {
                            e.printStackTrace();
                        }
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
}
