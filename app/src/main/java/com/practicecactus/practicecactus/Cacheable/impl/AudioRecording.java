package com.practicecactus.practicecactus.Cacheable.impl;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.practicecactus.practicecactus.Cacheable.Cacheable;
import com.practicecactus.practicecactus.ServerTasks.SendMultipartTask;

/**
 * Created by christopherarnold on 2016-10-05.
 */

public class AudioRecording implements Cacheable {
    private Activity activity;
    private String fileName;
    private String description;
    private String toTeacher;   // boolean String
    private String toCommunity; // boolean String

    public AudioRecording(Activity activity, String fileName,
                          String description, String toTeacher,
                          String toCommunity) {
        this.activity = activity;
        this.fileName = fileName;
        this.description = description;
        this.toTeacher = toTeacher;
        this.toCommunity = toCommunity;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getDescription() {
        return this.description;
    }

    public String getToTeacher() {
        return this.toTeacher;
    }

    public String getToCommunity() {
        return this.toCommunity;
    }

    public void send() {
        SendMultipartTask smt = new SendMultipartTask(activity, new SendMultipartTask.AsyncResponse() {
            @Override
            public void processFinish(Integer responseCode) {

            }
        });

        // see doInBackground() in SendMultipartTask.java
        smt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this, activity.getApplicationContext());
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s",
                this.fileName, this.description, this.toTeacher, this.toCommunity
        );
    }
}
