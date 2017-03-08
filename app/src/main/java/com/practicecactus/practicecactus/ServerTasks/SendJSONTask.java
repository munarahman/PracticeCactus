package com.practicecactus.practicecactus.ServerTasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import com.practicecactus.practicecactus.R;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.practicecactus.practicecactus.Utils.Constants.SERVER_ADDR;

/**
 * Created by alvinleung on 2016-02-29.
 */
public class SendJSONTask extends AsyncTask<Object, Void, Integer> {

    public AsyncResponse delegate = null;
    private HttpURLConnection conn = null;
    private int responseCode = 0;
    private Activity callingActivity;
    private AlertDialog.Builder builder;

    public SendJSONTask(Activity activity, AsyncResponse delegate) {
        this.callingActivity = activity;
        this.delegate = delegate;
    }

    public interface AsyncResponse {
        void processFinish(Integer responseCode);
    }

    protected void onPreExecute() {
        super.onPreExecute();
        builder = new AlertDialog.Builder(callingActivity);
    }

    @Override
    protected Integer doInBackground(Object... params) {
        String request = (String) params[0];
        String requestAddress = SERVER_ADDR + params[1];
        JSONObject jsonParam = (JSONObject) params[2];

        System.out.println("sending " + request + " request to " + requestAddress);

        SharedPreferences prefs = this.callingActivity.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        try {
            URL url = new URL(requestAddress);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.connect();

            byte[] data = jsonParam.toString().getBytes("UTF-8");

            //send POST output
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.write(data);
            dos.flush();
            dos.close();

            //read response:
            responseCode = conn.getResponseCode();
            System.out.println("SendJSONTask response: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                System.out.println("Successfully sent practice data: " + params[0].toString());
            } else {
                System.out.println(conn.getResponseMessage());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return responseCode;
    }

    protected void onPostExecute(Integer responseCode) {
        if (responseCode == 444) {
            Toast.makeText(
                    callingActivity.getApplicationContext(),
                    R.string.not_enrolled,
                    Toast.LENGTH_LONG).show();
        }
    }
}
