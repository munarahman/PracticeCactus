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
import android.widget.Toast;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;
import com.practicecactus.practicecactus.Utils.CommonFunctions;

import java.util.HashMap;

public class ContactActivity extends AppCompatActivity {

    private EditText messageEditText;
    private AnalyticsApplication analytics;
    private CommonFunctions cf;
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

        // get shared prefs
        SharedPreferences prefs = ContactActivity.this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        editor = prefs.edit();

        messageEditText = ((EditText) findViewById(R.id.contact_message));

        Button submitButton = (Button) findViewById(R.id.contact_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendContactMessage();
            }
        });

        cf = new CommonFunctions();

        ended = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());

        // if the session record info has already been sent, create a new session record
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

            // get the practice Activity and unregister it
            PracticeActivity activity = ((AnalyticsApplication) getApplication()).getListeningActivity();
            DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).unregister(activity);

            // get the session Record
            sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();
            CommonFunctions cf = new CommonFunctions();
            cf.finishPractice(sessionRecord, this, offlineManager);

            offlineManager.clearCache();

            // set ended to true to start a new Session in PracticeActivity
            ended = true;

            // save it in sharedPref
            editor.putBoolean("sentData", ended);
            editor.commit();
        }
    }

    private void sendContactMessage() {

        // called when the user clicks on the submit button in the Contact page

        HashMap<String, EditText> fields = new HashMap<>();
        String message = messageEditText.getText().toString();

        fields.put(message, messageEditText);

        // check that they are not empty
        boolean isValid = cf.checkEmpty(fields);


        String request = "POST";
        String requestAddress = getString(R.string.contact_server_call);
        String requestBody = "contents=" + message;

        if (isValid) {
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() > 400) {
                        // display the error

                        Toast.makeText(ContactActivity.this, R.string.contact_error,
                                Toast.LENGTH_LONG).show();
                    } else {

                        // create success dialog
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

}
