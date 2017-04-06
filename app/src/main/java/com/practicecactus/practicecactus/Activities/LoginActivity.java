package com.practicecactus.practicecactus.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private String username;
    private String password;

    private EditText usernameEditText;
    private EditText passwordEditText;

    private Button loginButton;
    private TextView signUpButton;
    private AnalyticsApplication analytics;

    private boolean isValid = true;
    public static final int MY_PERMISSIONS_AUDIO_RECORD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        audioRecordPermissions();



        SharedPreferences prefs = this.getSharedPreferences("USER_SHAREDPREFERENCES", this.MODE_PRIVATE);

        Map<String,?> keys = prefs.getAll();
        System.out.println(" LOGIN ACTIVITY KEYS:*******************:");
        System.out.println(keys.toString());

        System.out.println("token:" + prefs.getString("token", "default"));

//        if (prefs.contains("token") && prefs.contains("userId")) {
        if (prefs.contains("token")) {
            System.out.println("****** WHAT THE ******");
            loginToApp();
        }

        usernameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);

        loginButton = (Button) findViewById(R.id.login_button);
        signUpButton = (TextView) findViewById(R.id.sign_up);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                studentLogin();
                isValid = true;
            }
        });
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createAccountIntent = new Intent(view.getContext(), CreateAccountActivity.class);
                startActivity(createAccountIntent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

    private void studentLogin() {
        username = usernameEditText.getText().toString();
        password = passwordEditText.getText().toString();

        checkEmpty(username, usernameEditText);
        checkEmpty(password, passwordEditText);

        String request = "POST";
        String requestAddress = "/auth/local";
        String requestBody =
                "username=" + username +
                "&password=" + password +
                "&requestedRole=student";

        if (isValid) {
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    System.out.println("CODE:" + serverResponse.getCode());
                    if (serverResponse.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        passwordEditText.setError("Username and password do not match");
                    } else if (serverResponse.getCode() == 499) {

                        // If the code is 499, this is a special code that indicates a teacher
                        // is trying to login.

                        Toast.makeText(getApplicationContext(),
                                R.string.not_student,
                                Toast.LENGTH_LONG).show();
                    } else {
                        loginToApp();
                    }
                }
            });

            sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    request, requestAddress, requestBody);
        }
    }

    private void loginToApp() {
        Intent practiceIntent = new Intent(getApplicationContext(), PracticeActivity.class);
        startActivity(practiceIntent);
    }

    private void checkEmpty(String field, EditText fieldEntry) {
        if (field.isEmpty()) {
            fieldEntry.setError("Missing credentials");
            isValid = false;
        }
    }

    private void audioRecordPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_AUDIO_RECORD);
        }
    }
}
