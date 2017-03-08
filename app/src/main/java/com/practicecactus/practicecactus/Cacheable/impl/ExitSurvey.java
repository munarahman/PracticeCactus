package com.practicecactus.practicecactus.Cacheable.impl;

import android.app.Activity;
import android.os.AsyncTask;

import com.practicecactus.practicecactus.Cacheable.Cacheable;
import com.practicecactus.practicecactus.ServerTasks.SendJSONTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by christopherarnold on 2016-12-13.
 */

public class ExitSurvey implements Cacheable {
    private Activity activity;
    private String r1;
    private String r2;
    private String r3;

    public ExitSurvey(Activity activity, String r1, String r2, String r3) {
        this.activity = activity;
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
    }

    public JSONObject getExitSurveyJSON() {
        try {
            JSONObject jsonExitSurvey = new JSONObject();
            JSONArray jsonExitQuestions = new JSONArray();

            JSONObject q1 = new JSONObject();
            JSONObject q2 = new JSONObject();
            JSONObject q3 = new JSONObject();

            q1.put("id", 1);
            q1.put("question", "I feel good about that practice session.");
            q1.put("rate", this.r1);

            q2.put("id", 2);
            q2.put("question", "I used effective practice strategies.");
            q2.put("rate", this.r2);

            q3.put("id", 3);
            q3.put("question", "If I keep practicing like that, I will improve.");
            q3.put("rate", this.r3);

            jsonExitQuestions.put(q1);
            jsonExitQuestions.put(q2);
            jsonExitQuestions.put(q3);

            jsonExitSurvey.put("questions", jsonExitQuestions);

            return jsonExitSurvey;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void send() {
        String request = "POST";
        String requestAddress = "/api/surveyQuestions";

        SendJSONTask sjt = new SendJSONTask(activity, new SendJSONTask.AsyncResponse() {
            @Override
            public void processFinish(Integer responseCode) {

            }
        });
        sjt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, getExitSurveyJSON());
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s", this.r1, this.r2, this.r3);
    }
}
