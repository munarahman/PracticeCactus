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
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.Utils.CommonFunctions;

import java.net.HttpURLConnection;
import java.util.HashMap;

public class ChangePasswordActivity extends AppCompatActivity {


    private String userId;

    private EditText oldPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmNewPasswordEditText;

    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // for google analytics
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        // get the EditText fields
        oldPasswordEditText = ((EditText) findViewById(R.id.current_password));
        newPasswordEditText = ((EditText) findViewById(R.id.new_password));
        confirmNewPasswordEditText = ((EditText) findViewById(R.id.confirm_new_password));

        // when the submit button is clicked call changePassword()
        Button submitButton = (Button) findViewById(R.id.change_password_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
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

        // called when the submit button is clicked

        CommonFunctions cf = new CommonFunctions();
        HashMap<String, EditText> fields = new HashMap<>();

        // get the text from the text fields
        String oldPassword = oldPasswordEditText.getText().toString();
        String newPassword = newPasswordEditText.getText().toString();
        String confirmNewPassword = confirmNewPasswordEditText.getText().toString();

        // store all the values and their respective fields in a hasmap
        fields.put(oldPassword, oldPasswordEditText);
        fields.put(newPassword, newPasswordEditText);
        fields.put(confirmNewPassword, confirmNewPasswordEditText);

        // check that they are not empty
        boolean isValid = cf.checkEmpty(fields);

        // set the request parameters
        String request = getString(R.string.post_server_Request);
        String requestAddress = getString(R.string.change_password_server_call);
        String requestBody = "oldPassword=" + oldPassword + "&newPassword=" + newPassword;

        // if the passwords aren't the same, show error message
        if (!newPassword.equals(confirmNewPassword)) {
            confirmNewPasswordEditText.setError("New passwords don't match");
        } else if (isValid) {

            // if isValid is true that means no text fields are null

            // create a new SendApplication Task to send the sever request
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {

                    if (serverResponse.getCode() == HttpURLConnection.HTTP_FORBIDDEN) {

                        // set the error message when the old password is incorrect
                        oldPasswordEditText.setError("Incorrect password");
                    } else {

                        // build success dialog

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
}
