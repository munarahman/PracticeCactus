package com.practicecactus.practicecactus.ServerTasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.practicecactus.practicecactus.Cacheable.impl.AudioRecording;
import com.practicecactus.practicecactus.R;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLOutput;

import static com.practicecactus.practicecactus.Utils.Constants.SERVER_ADDR;

/**
 * do
 * Created by alvinleung on 2016-03-10.
 */
public class SendMultipartTask extends AsyncTask<Object, Void, Integer> {
    private String fileName = null;
    private String description = null;
    private String toTeacher;
    private String toCommunity;

    private final String requestAddress = SERVER_ADDR + "/api/recordings";
    private HttpURLConnection conn = null;
    private URL url;
    DataOutputStream dos = null;
    int serverResponseCode = 0;

    public AsyncResponse delegate = null;
    private Activity callingActivity;
    private AlertDialog.Builder builder;

    //POST formatting stuff
    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary = "*****";
    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1 * 1024 * 1024;

    public SendMultipartTask(Activity activity, AsyncResponse delegate) {
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
        AudioRecording recording = (AudioRecording) params[0];
        Context context = (Context) params[1];
        this.fileName = recording.getFileName();
        this.description = recording.getDescription();
        this.toTeacher = recording.getToTeacher();
        this.toCommunity = recording.getToCommunity();

        System.out.println("sending recording: " + recording );

        SharedPreferences prefs = context.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        String token = prefs.getString("token", null);

        File sourceFile = new File(this.fileName);

        try {

            // open a URL connection to the Servlet
            FileInputStream fileInputStream = new FileInputStream(sourceFile);
            url = new URL(requestAddress);

            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("file", fileName);
            conn.setRequestProperty("Authorization", "Bearer " + token);

            dos = new DataOutputStream(conn.getOutputStream());

/*
--XXX
Content-Disposition: form-data; name="name"

John
 */
            setField("description", this.description);
            setField("isAvailableToTeacher", this.toTeacher);
            setField("isAvailableToCommunity", this.toCommunity);

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + this.fileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);

            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();

            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {

                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            }

            // send multipart form data necessary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            Log.i("uploadFile", "HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            if (serverResponseCode == 200) {
                System.out.println("UPLOAD SUCCESS!!!!");
            }

            //close the streams //
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            System.out.println("MalformedURLException");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Other Exception");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return serverResponseCode;
    }

    private void setField(String keyName, String keyValue) {
        try {
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"" + keyName + "\";" + lineEnd);
            dos.writeBytes(lineEnd);
            dos.writeBytes(keyValue);
            dos.writeBytes(lineEnd);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
