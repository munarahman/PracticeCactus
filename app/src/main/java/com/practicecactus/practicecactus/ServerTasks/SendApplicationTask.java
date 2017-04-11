package com.practicecactus.practicecactus.ServerTasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import static com.practicecactus.practicecactus.Utils.Constants.SERVER_ADDR;

/**
 * Created by christopherarnold on 2016-09-26.
 */

public class SendApplicationTask extends AsyncTask<String, Void, ServerResponse> {

    public AsyncResponse delegate = null;
    private int responseCode = 0;
    private JSONObject responseJSON = null;
    private HttpURLConnection conn = null;
    private Activity callingActivity;
    private AlertDialog.Builder builder;

    public SendApplicationTask(Activity activity, AsyncResponse delegate) {
        this.callingActivity = activity;
        this.delegate = delegate;
    }

    public interface AsyncResponse {
        void processFinish(ServerResponse serverResponse);
    }

    protected void onPreExecute() {
        super.onPreExecute();
        builder = new AlertDialog.Builder(callingActivity);
    }

    @Override
    protected ServerResponse doInBackground(String... params) {
        sendRequest(params[0], params[1], params[2]);
        return new ServerResponse(responseCode, responseJSON);
    }

    private void sendRequest(String request, String requestAddress, String requestBody) {
        boolean needToken = true;
        String retrieveData = "";

        // token not needed for logging in or creating an account
        if (requestAddress.equals("/auth/local") || requestAddress.equals("/api/users")) {
            needToken = false;
        }

        if (requestAddress.equals("/api/users/me")) {
            retrieveData = requestAddress;
        }

        String HTTPAddress = SERVER_ADDR + requestAddress;
//        System.out.println("sending " + request + " request to " + HTTPAddress);

        try {
            URL url = new URL(HTTPAddress);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(request);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // if the user is logging in then get their token
            if (needToken) {
                SharedPreferences prefs = this.callingActivity.getSharedPreferences(
                        "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
                String token = prefs.getString("token", null);
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (request.equals("POST") || request.equals("PUT")) {
                conn.setDoOutput(true);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                if (requestBody != null)
                    dos.writeBytes(requestBody);

                dos.flush();
                dos.close();
            }

            responseCode = conn.getResponseCode();

            if (responseCode < 400) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));

                try {
                    String brOutput = br.readLine();

                    if (brOutput != null) {

                        JSONObject json = new JSONObject(brOutput);

                        // save token and other user data only when logging in or creating a new account
                        if (!needToken || !retrieveData.equals("")) {
                            savePreferences(json, retrieveData);
                        }
                        responseJSON = json;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                br.close();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // save account information to SharedPreferences via CactusStore
    private void savePreferences(JSONObject json, String retrieveData) {
        try {
            // if you are retrieving user data, make a GET request to the server with new token
            if (retrieveData.equals("")) {
                String token = (String) json.get("token");

                // save token to shared preferences
                SharedPreferences.Editor editor = this.callingActivity.getSharedPreferences(
                        "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE).edit();

                editor.putString("token", token);
                editor.apply();

                // get all the users data
                sendRequest("GET", "/api/users/me", null);

            } else if (retrieveData.equals("/api/users/me")) {

                // otherwise, save data from response into CactusStore
                String userId = (String) json.get("_id");
                String userRole = (String) json.get("role");
                String cactusName = (json.isNull("cactusName")) ? null : (String) json.get("cactusName");


                // if a student is logging in, save their student and user ID
                if (!userRole.equals("teacher") && !userRole.equals("user")) {

                    String studentId = (String) json.get("studentId");


                    // save userId studentId, cactusname, and userId to shared preferences
                    SharedPreferences.Editor editor = this.callingActivity.getSharedPreferences(
                            "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE).edit();

                    editor.putString("studentId", studentId);
                    editor.putString("userId", userId);
                    editor.putString("cactusName", cactusName);
                    editor.apply();

                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void onPostExecute(ServerResponse serverResponse) {
        delegate.processFinish(serverResponse);
    }
}
