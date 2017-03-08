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

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText nameEditText;
    private EditText emailEditText;

    private Button submitButton;
//    private boolean isValid = true;
    private boolean isUsernameValid = true;
    private boolean isPasswordValid = true;
    private boolean isConfirmPasswordValid = true;
    private boolean isNameValid = true;
    private boolean isEmailValid = true;

    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        usernameEditText = ((EditText) findViewById(R.id.create_username));
        passwordEditText = ((EditText) findViewById(R.id.create_password));
        confirmPasswordEditText = ((EditText) findViewById(R.id.confirm_password));
        nameEditText = ((EditText) findViewById(R.id.create_name));
        emailEditText = ((EditText) findViewById(R.id.email));

        submitButton = (Button) findViewById(R.id.submit_button);
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
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();

        isUsernameValid = checkEmpty(username, usernameEditText);
        isPasswordValid = checkEmpty(password, passwordEditText);
        isConfirmPasswordValid = checkEmpty(confirmPassword, confirmPasswordEditText);
        isNameValid = checkEmpty(name, nameEditText);
        isEmailValid = checkEmpty(email, emailEditText);

        String request = "POST";
        String requestAddress = "/api/users";
        String requestBody =
                "username=" + username +
                        "&password=" + password +
                        "&name=" + name +
                        "&email=" + email +
                        "&role=student";

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords don't match");

        } else if (isUsernameValid && isPasswordValid && isConfirmPasswordValid && isNameValid && isEmailValid) {
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() == 422) {
                        usernameEditText.setError("Username already exists");
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CreateAccountActivity.this);
                        builder.setMessage("Account created successfully!")
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // practice suggestions must be reset when creating new account
                                                resetPracticeList();

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
        String request = "POST";
        String requestAddress = "/api/suggestions/reset";

        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() >= 400) {
                    System.out.println("Could not reset practice list");
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);
    }

    private boolean checkEmpty(String field, EditText fieldEntry) {
        if (field.isEmpty()) {
            fieldEntry.setError("Missing credentials");
            return false;
        }
        else {
            return true;
        }
    }
}
