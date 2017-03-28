package com.practicecactus.practicecactus.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;
import com.practicecactus.practicecactus.Utils.CommonFunctions;

import static com.practicecactus.practicecactus.Utils.Constants.SERVER_ADDR;

public class ProgressActivity extends AppCompatActivity {

    private AnalyticsApplication analytics;
    private DefaultSessionRecord sessionRecord;
    private OfflineManager offlineManager;
    private boolean ended;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_progress);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        offlineManager = OfflineManager.getInstance(this);

        SharedPreferences prefs = ProgressActivity.this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        editor = prefs.edit();

        String token = prefs.getString("token", null);
        String studentId = prefs.getString("studentId", null);

        String server_address = SERVER_ADDR + "/student-info/" + studentId;
        String cookieString = "token=" + token;

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(server_address + "/", cookieString);

        WebView webView = (WebView) this.findViewById(R.id.progress_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(server_address);

//        webView.loadUrl("http://a.bestmetronome.com/");
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

//        sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();
//        System.out.println("progress resume start time:" + sessionRecord.get_start_time());
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
}
