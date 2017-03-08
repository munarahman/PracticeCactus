package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;


import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.Cactus.CactusStore;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.BuildConfig;

import java.sql.SQLOutput;

public class SettingsEditorActivity extends AppCompatActivity {
    private CactusStore preferences;

    private EditText nameEditText;
    private EditText sessionEditText;
    private EditText cactusEditText;

    private Button saveButton;
    private Button cancelButton;
    private TextView changePasswordLink;
    private TextView versionCode;

    private boolean isValid = true;
    private AnalyticsApplication analytics;
    private String studentId;
    private String verCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_editor);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        SharedPreferences prefs = this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);

        this.preferences = new CactusStore(getApplicationContext(), studentId);

        nameEditText = ((EditText) findViewById((R.id.settings_name)));
        cactusEditText = ((EditText) findViewById(R.id.settings_cactus_name));
        sessionEditText = ((EditText) findViewById(R.id.settings_practice_goal));


        nameEditText.setText(this.preferences.load_nickname());
        cactusEditText.setText(this.preferences.load_cactusName());
        sessionEditText.setText(Integer.toString(this.preferences.load_session_length()));

        saveButton = (Button) findViewById(R.id.settings_submit);
        cancelButton = (Button) findViewById(R.id.settings_cancel);

        versionCode = (TextView) findViewById(R.id.version_code);
        changePasswordLink = (TextView) findViewById(R.id.change_password_link);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        changePasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent passwordIntent = new Intent(getApplicationContext(), ChangePasswordActivity.class);
                startActivity(passwordIntent);
            }
        });

        verCode = Integer.toString(BuildConfig.VERSION_CODE);
        versionCode.setText(verCode);
    }

    private void saveSettings() {
        String name = nameEditText.getText().toString();
        String cactusName = cactusEditText.getText().toString();
        int sessionLength = Integer.parseInt(sessionEditText.getText().toString());

        checkEmpty(name, nameEditText);
        checkEmpty(cactusName, cactusEditText);
        checkEmpty(sessionEditText.getText().toString(), sessionEditText);

        if (!TextUtils.isDigitsOnly(sessionEditText.getText())) {
            sessionEditText.setError("Must be a number");
            isValid = false;
        }

        if (sessionLength < 1) {
            sessionEditText.setError("Must be greater than zero");
            isValid = false;
        }

        if (isValid) {
            this.preferences.save_nickname(name);
            this.preferences.save_cactusName(cactusName);
            this.preferences.save_session_length(sessionLength);

            SharedPreferences prefs = this.getSharedPreferences(
                    "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
            String userId = prefs.getString("userId", null);
            System.out.println("NAME: " + name);

            String request = "PUT";
            String requestAddress = "/api/users/" + userId;
            String requestBody = "name=" + name +
                    "&cactusName=" + cactusName;


            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() == 444) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsEditorActivity.this);
                        builder.setMessage("Sorry, you have not been enrolled by your teacher yet!")
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .show();

                    }
                    else if(serverResponse.getCode() >= 400) {
                        System.out.println("Error setting new name");
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsEditorActivity.this);
                        builder.setMessage("Saved settings!")
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        })
                                .show();
                    }
                }
            });

            sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    request, requestAddress, requestBody);
        }

        isValid = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

    private void checkEmpty(String field, EditText fieldEntry) {
        if (field.isEmpty()) {
            fieldEntry.setError("Cannot be empty");
            isValid = false;
        }
    }
}
