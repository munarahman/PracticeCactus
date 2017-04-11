package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.R;

public class MainMenuActivity extends AppCompatActivity {

    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        // get all the appropriate buttons
        Button community = (Button) findViewById(R.id.community_button);
        Button progress = (Button) findViewById(R.id.progress_button);
        Button settings = (Button) findViewById(R.id.menu_settings);
        Button contact = (Button) findViewById(R.id.contact);
        Button logout = (Button) findViewById(R.id.logout);
        Button goBack = (Button) findViewById(R.id.go_back);

        // set the listeners for each button
        community.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent communityIntent = new Intent(getApplicationContext(), CommunityActivity.class);
                startActivity(communityIntent);
            }
        });
        progress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent progressIntent = new Intent(getApplicationContext(), ProgressActivity.class);
                startActivity(progressIntent);
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(getApplicationContext(), SettingsEditorActivity.class);
                startActivity(settingsIntent);
            }
        });
        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactIntent = new Intent(getApplicationContext(), ContactActivity.class);
                startActivity(contactIntent);
            }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainMenuActivity.this);
                builder.setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        signOut();
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .show();
            }
        });
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

    private void signOut(){

        // get shared prefs
        SharedPreferences prefs = this.getSharedPreferences("USER_SHAREDPREFERENCES", this.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // remove the token from shared prefs
        editor.remove("token");
        editor.clear();
        editor.apply();

        /* set FLAG_ACTIVITY_NEW_TASK so that loginScreenIntent will become the start of a new task
         * on this history stack.
         *
         * FLAG_ACTIVITY_CLEAR_TASK will cause any existing task that is with loginScreenIntent to
         * be cleared before the activity is started.
         */

        Intent loginScreenIntent = new Intent(this, LoginActivity.class);
        loginScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginScreenIntent);
    }
}
