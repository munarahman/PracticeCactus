package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

import java.net.HttpURLConnection;

public class ChangePasswordActivity extends AppCompatActivity {

    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
    private String userId;

    private EditText oldPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmNewPasswordEditText;

    private Button changePasswordButton;
    private boolean isValid = true;
    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        oldPasswordEditText = ((EditText) findViewById(R.id.current_password));
        newPasswordEditText = ((EditText) findViewById(R.id.new_password));
        confirmNewPasswordEditText = ((EditText) findViewById(R.id.confirm_new_password));

        changePasswordButton = (Button) findViewById(R.id.change_password_button);
        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePassword();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

    private void changePassword() {
        oldPassword = oldPasswordEditText.getText().toString();
        newPassword = newPasswordEditText.getText().toString();
        confirmNewPassword = confirmNewPasswordEditText.getText().toString();

        checkEmpty(oldPassword, oldPasswordEditText);
        checkEmpty(newPassword, newPasswordEditText);
        checkEmpty(confirmNewPassword, confirmNewPasswordEditText);

        String request = "PUT";
        String requestAddress = "/api/users/password";
        String requestBody =
                "oldPassword=" + oldPassword +
                "&newPassword=" + newPassword;

        if (!newPassword.equals(confirmNewPassword)) {
            confirmNewPasswordEditText.setError("New passwords don't match");
        } else if (isValid) {
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() == HttpURLConnection.HTTP_FORBIDDEN) {
                        oldPasswordEditText.setError("Incorrect password");
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ChangePasswordActivity.this);
                        builder.setMessage("Password changed successfully!")
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
            fieldEntry.setError("Missing credentials");
            isValid = false;
        }
    }
}
