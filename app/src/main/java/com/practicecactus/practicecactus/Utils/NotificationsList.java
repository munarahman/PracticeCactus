package com.practicecactus.practicecactus.Utils;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.practicecactus.practicecactus.Activities.PracticeActivity;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Muna on 2017-04-04.
 */

public class NotificationsList {

    private static final String storageDirName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/local_storage";
    private static final String dangerZonesPath = storageDirName + "/notifications.json";

    private File storageDir;
    private File notificationsFile;
    private List<String> notificationsList = new ArrayList<>();
    private Activity callingActivity;

    public NotificationsList(Activity callingActivity) {

        this.callingActivity = callingActivity;

        storageDir = new File(storageDirName);
        if (!storageDir.exists() && !storageDir.isDirectory()) {
            storageDir.mkdir();
        }

        notificationsFile = new File(dangerZonesPath);
        if (!notificationsFile.exists() && !notificationsFile.isDirectory()) {
            try {
                notificationsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void updateNotifications() {


        // call the notifications api
        String request = "GET";
        String requestAddress = "/api/recordings/recordingsWithNewComments";


        SendApplicationTask sat = new SendApplicationTask(callingActivity, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() < 400 && serverResponse.getResponse() != null) {

                    System.out.println("displayNOtifciations response:" + serverResponse.getResponse());
                    int i;
                    String content = "";

                    try {
                        JSONArray notificationsArr = (JSONArray) serverResponse.getResponse().get("recordings");

                        for (i = 0; i < notificationsArr.length(); i++) {
                            JSONObject recordingObj = (JSONObject)(notificationsArr.get(i));

                            // build the notifications string
                            content += "Your recording with title " + recordingObj.getString("description")
                                    + " was commented on by me";

                            notificationsList.add(content);

                            // loop through list view and insert rows in

                            content = "";
                        }

                        System.out.println("LENGTH***:" + notificationsArr.length());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);

    }

    public void saveNotifications() {

        try {
            if (notificationsFile.exists()) {
                notificationsFile.delete();
            }

            FileOutputStream out = new FileOutputStream(notificationsFile, true);
            String content = "";


//            for (JSONObject nOBJ : notificationsList) {
//                content += nOBJ.toString();
//            }


            out.write(content.getBytes());
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
