package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.Utils.CommonFunctions;

import java.util.HashMap;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText nameEditText;
    private EditText emailEditText;

    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        // get all the EditText fields
        usernameEditText = ((EditText) findViewById(R.id.create_username));
        passwordEditText = ((EditText) findViewById(R.id.create_password));
        confirmPasswordEditText = ((EditText) findViewById(R.id.confirm_password));
        nameEditText = ((EditText) findViewById(R.id.create_name));
        emailEditText = ((EditText) findViewById(R.id.email));

        // when the submit button is clicked, call createAccount()
        Button submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAccount();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

    private void createAccount() {

        // called when the submit button is clicked

        // get the text from all the EditText fields
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();

        // store all the values and their respective fields in a hasmap
        CommonFunctions cf = new CommonFunctions();
        HashMap<String, EditText> fields = new HashMap<>();

        fields.put(username, usernameEditText);
        fields.put(password, passwordEditText);
        fields.put(confirmPassword, confirmPasswordEditText);
        fields.put(name, nameEditText);
        fields.put(email, emailEditText);

        // isValid is false if any fields are empty
        boolean isValid = cf.checkEmpty(fields);

        // set the parameters for the request
        String request = "POST";
        String requestAddress = getString(R.string.new_user_server_call);
        String requestBody =
                "username=" + username +
                        "&password=" + password +
                        "&name=" + name +
                        "&email=" + email +
                        "&role=student";

        if (!password.equals(confirmPassword)) {

            // display error message if passwords don't match
            confirmPasswordEditText.setError("Passwords don't match");

        } else if (isValid) {

            // create a new SendApplication Task to send the sever request
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() == 422) {

                        // display error message if username already exists
                        usernameEditText.setError("Username already exists");
                    } else {

                        // display success dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
                        builder.setMessage("Account created successfully!")
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {

                                                // practice suggestions must be reset when creating new account
                                                resetPracticeList();

                                                // move to practiceActivity once done
                                                Intent practiceIntent = new Intent(getApplicationContext(), PracticeActivity.class);
                                                practiceIntent.putExtra("newAccount", "true");
                                                startActivity(practiceIntent);
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

    private void resetPracticeList() {

        // called when a new account is created in createAccount()

        String request = "POST";
        String requestAddress = getString(R.string.reset_practice_list_server_call);

        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {

                if (serverResponse.getCode() >= 400) {

                    // display error if failed
                    Toast.makeText(CreateAccountActivity.this, R.string.failed_reset_error,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);
    }
}
