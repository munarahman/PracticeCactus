package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

public class ContactActivity extends AppCompatActivity {

    private EditText messageEditText;
    private Button submitButton;
    private boolean isValid = true;
    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        messageEditText = ((EditText) findViewById(R.id.contact_message));
        submitButton = (Button) findViewById(R.id.contact_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendContactMessage();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
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
