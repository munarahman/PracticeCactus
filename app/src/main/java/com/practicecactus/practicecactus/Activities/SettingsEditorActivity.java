package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;


import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.Cactus.CactusStore;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.BuildConfig;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;
import com.practicecactus.practicecactus.Utils.AudioGenerator;
import com.practicecactus.practicecactus.Utils.CommonFunctions;
import com.practicecactus.practicecactus.Utils.Metronome;

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

    private DefaultSessionRecord sessionRecord;
    private OfflineManager offlineManager;
    private boolean ended;
    private SharedPreferences.Editor editor;

    private double beatSound;
    private double sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_editor);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        offlineManager = OfflineManager.getInstance(this);

        // get the shared preferences
        SharedPreferences prefs = this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        editor = prefs.edit();

        studentId = prefs.getString("studentId", null);

        this.preferences = new CactusStore(getApplicationContext(), studentId);

        // get the settings edit fields
        nameEditText = ((EditText) findViewById((R.id.settings_name)));
        cactusEditText = ((EditText) findViewById(R.id.settings_cactus_name));
        sessionEditText = ((EditText) findViewById(R.id.settings_practice_goal));

        // set their default values
        nameEditText.setText(this.preferences.load_name());
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

        ended = false;

    }


    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());

        if (ended) {
            ((AnalyticsApplication) getApplication()).createNewSessionRecord(getApplicationContext());
            sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*
        * isFinishing() will be false if HOME button or the screen turns off.
        * isFinishing() Will be t rue if BACK button is pressed
        * */

        if (!this.isFinishing()){
            System.out.println("leaving");

            // get the practice Activity and unregister it
            PracticeActivity activity = ((AnalyticsApplication) getApplication()).getListeningActivity();
            DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).unregister(activity);

            sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();
            CommonFunctions cf = new CommonFunctions();
            cf.finishPractice(sessionRecord, this, offlineManager);

            offlineManager.clearCache();
            ended = true;

            editor.putBoolean("sentData", ended);
            editor.commit();
        }
        else {
            System.out.println("JOKES NOT LEAVING");
        }
    }


    private void saveSettings() {
        final String name = nameEditText.getText().toString();
        final String cactusName = cactusEditText.getText().toString();
        final int sessionLength = Integer.parseInt(sessionEditText.getText().toString());

        checkEmpty(name, nameEditText);
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

            SharedPreferences prefs = this.getSharedPreferences(
                    "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
            String userId = prefs.getString("userId", null);


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

                        preferences.save_name(name);
                        preferences.save_cactusName(cactusName);
                        preferences.save_session_length(sessionLength);


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


    private void checkEmpty(String field, EditText fieldEntry) {
        if (field.isEmpty()) {
            fieldEntry.setError("Cannot be empty");
            isValid = false;
        }
    }
}
