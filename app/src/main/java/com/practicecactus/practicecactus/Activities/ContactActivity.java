package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;
import com.practicecactus.practicecactus.Utils.CommonFunctions;

public class ContactActivity extends AppCompatActivity {

    private EditText messageEditText;
    private Button submitButton;
    private boolean isValid = true;
    private AnalyticsApplication analytics;

    private DefaultSessionRecord sessionRecord;
    private OfflineManager offlineManager;
    private boolean ended;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        offlineManager = OfflineManager.getInstance(this);

        SharedPreferences prefs = ContactActivity.this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        editor = prefs.edit();

        messageEditText = ((EditText) findViewById(R.id.contact_message));
        submitButton = (Button) findViewById(R.id.contact_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendContactMessage();
            }
        });

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
        * isFinishing() Will be true if BACK button is pressed
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

    private void sendContactMessage() {
        String message = messageEditText.getText().toString();

        checkEmpty(message, messageEditText);

        String request = "POST";
        String requestAddress = "/api/contacts";
        String requestBody = "contents=" + message;

        if (isValid) {
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() >= 400) {
                        System.out.println("Cannot send message.");
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ContactActivity.this);
                        builder.setMessage("Message sent!")
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
    }

    private void checkEmpty(String field, EditText fieldEntry) {
        if (field.isEmpty()) {
            fieldEntry.setError("Missing field");
            isValid = false;
        }
    }
}
