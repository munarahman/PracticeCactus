package com.practicecactus.practicecactus.Activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.practicecactus.practicecactus.R;

public class SessionDataActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_data);

        Intent intent = getIntent();
        String username_str = intent.getStringExtra("username");
        long playtime = intent.getLongExtra("playtime", 0);
        long totaltime = intent.getLongExtra("totaltime", 0);

        TextView greeting_text = (TextView) findViewById(R.id.session_data_greet);
        TextView playtime_text = (TextView) findViewById(R.id.session_data_playtime);

        greeting_text.setText("Great Job, " + username_str + "!");
        playtime_text.setText("You played " + Long.toString(playtime) + " seconds out of " + Long.toString(totaltime) + " seconds.");

    }
}
