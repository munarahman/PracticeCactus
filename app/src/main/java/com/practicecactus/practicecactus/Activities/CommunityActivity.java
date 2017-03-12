package com.practicecactus.practicecactus.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.R;

import static com.practicecactus.practicecactus.Utils.Constants.SERVER_ADDR;

public class CommunityActivity extends AppCompatActivity {

    private AnalyticsApplication analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        SharedPreferences prefs = CommunityActivity.this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);


        String server_address = SERVER_ADDR + "/feed";
        String cookieString = "token=" + token;

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(server_address + "/", cookieString);

        WebView webView = (WebView) this.findViewById(R.id.community_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(server_address);
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

}
