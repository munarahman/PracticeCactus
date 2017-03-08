package com.practicecactus.practicecactus.Activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.Cacheable.impl.ExitSurvey;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;


public class ExitSurveyActivity extends AppCompatActivity {

    private OfflineManager offlineManager;
    private RatingBar r1;
    private RatingBar r2;
    private RatingBar r3;
    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exit_survey);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        offlineManager = OfflineManager.getInstance(this);

        r1 = (RatingBar) findViewById(R.id.rating1);
        r2 = (RatingBar) findViewById(R.id.rating2);
        r3 = (RatingBar) findViewById(R.id.rating3);

        Button submit = (Button) findViewById(R.id.submit_exit_survey);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ExitSurvey survey = new ExitSurvey(ExitSurveyActivity.this, String.valueOf(r1.getRating()),
                        String.valueOf(r2.getRating()), String.valueOf(r3.getRating()));

                offlineManager.sendFileAttempt(survey);

                AlertDialog.Builder builder = new AlertDialog.Builder(ExitSurveyActivity.this);
                builder.setMessage("Thank you!")
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }
}
