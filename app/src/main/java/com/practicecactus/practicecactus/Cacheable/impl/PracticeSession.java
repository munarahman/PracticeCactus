package com.practicecactus.practicecactus.Cacheable.impl;

import android.app.Activity;
import android.os.AsyncTask;

import com.practicecactus.practicecactus.Cacheable.Cacheable;
import com.practicecactus.practicecactus.ServerTasks.SendJSONTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by christopherarnold on 2016-12-13.
 */

public class PracticeSession implements Cacheable {
    private Activity activity;
    private String pianoTime;
    private String startTime;
    private String endTime;
    private String keyCount;

    public PracticeSession(Activity activity, String pianoTime,
                           String startTime, String endTime,
                           String keyCount) {
        this.activity = activity;
        this.pianoTime = pianoTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.keyCount = keyCount.toString();
    }

    public JSONArray getKeyCount() {
        JSONArray jsonKeyCount = new JSONArray();

        for (String value : Arrays.asList(this.keyCount.split(","))) {
            jsonKeyCount.put(value);
        }
        return jsonKeyCount;
    }

    public JSONObject getPracticeSessionJSON() {
        try {
            JSONObject jsonPracticeSession = new JSONObject();
            jsonPracticeSession.put("piano_time", pianoTime);
            jsonPracticeSession.put("start_time", startTime);
            jsonPracticeSession.put("end_time", endTime);
            jsonPracticeSession.put("key_count", this.getKeyCount());
            System.out.println(jsonPracticeSession.toString());
            return jsonPracticeSession;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void send() {
        String request = "POST";
        String requestAddress = "/api/practices";

        SendJSONTask sjt = new SendJSONTask(activity, new SendJSONTask.AsyncResponse() {
            @Override
            public void processFinish(Integer responseCode) {
            }
        });
        sjt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, getPracticeSessionJSON());
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s\n%s",
                this.pianoTime, this.startTime, this.endTime, this.keyCount
        );
    }
}
