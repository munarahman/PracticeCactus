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
import com.practicecactus.practicecactus.Utils.CommonFunctions;


import java.net.HttpURLConnection;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;

    private AnalyticsApplication analytics;

    public static final int MY_PERMISSIONS_AUDIO_RECORD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        audioRecordPermissions();

        // get the sharedPref
        SharedPreferences prefs = this.getSharedPreferences("USER_SHAREDPREFERENCES", this.MODE_PRIVATE);

        // if the sharedPrefs already contains a token, then go straight to practice activity
        if (prefs.contains("token")) {
            loginToApp();
        }

        // get the appropriate fields
        usernameEditText = (EditText) findViewById(R.id.username);
        passwordEditText = (EditText) findViewById(R.id.password);

        Button loginButton = (Button) findViewById(R.id.login_button);
        TextView signUpButton = (TextView) findViewById(R.id.sign_up);

        // set listeners
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                studentLogin();
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
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        // store all the values and their respective fields in a hasmap
        CommonFunctions cf = new CommonFunctions();
        HashMap<String, EditText> fields = new HashMap<>();

        fields.put(username, usernameEditText);
        fields.put(password, passwordEditText);

        // isValid is false if any fields are empty
        boolean isValid = cf.checkEmpty(fields);

        // set the parameters for the request
        String request = "POST";
        String requestAddress = getString(R.string.login_server_call);
        String requestBody =
                "username=" + username +
                "&password=" + password +
                "&requestedRole=student";

        if (isValid) {

            // create a new SendApplication Task to send the sever request
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {

                        // display error message
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

        // called in studentLogin()

        Intent practiceIntent = new Intent(getApplicationContext(), PracticeActivity.class);
        startActivity(practiceIntent);
    }


    private void audioRecordPermissions() {

        // called onCreate

        // ask user for permission to the phone's microphone

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {android.Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_AUDIO_RECORD);
        }
    }
}
