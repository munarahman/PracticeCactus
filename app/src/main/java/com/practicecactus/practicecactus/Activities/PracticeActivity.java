package com.practicecactus.practicecactus.Activities;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.practicecactus.practicecactus.Activities.Notifications.NotificationsFragment;
import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisListener;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;

import com.practicecactus.practicecactus.Cacheable.impl.PracticeSession;
import com.practicecactus.practicecactus.Cactus.Cactus;
import com.practicecactus.practicecactus.Cactus.CactusStore;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class PracticeActivity extends AppCompatActivity implements AudioAnalysisListener {

    private DefaultSessionRecord sessionRecord;
    private ArrayList<Float> soundSummary;
    private Intent previousIntent;
    private Cactus cactus;
    private CactusStore cactusStore;
    private String username;
    private boolean ended = false;
    private JSONObject practiceSuggestion;
    private TextView suggestionView;
    private String studentId;
    private TextView greeting_text;
    private TextView activity_cactus_name;
    private AnalyticsApplication analytics;
    private String eventCategory = this.getClass().getSimpleName();
    private String cactusName;
    private String request;
    private String requestAddress;

    public boolean leaving;
    public OfflineManager offlineManager;
    private SharedPreferences prefs;

    private ArrayList<String> notificationsList = new ArrayList<>();
    private ArrayList<String> commentHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        previousIntent = getIntent();

        // set the view
        setContentView(R.layout.activity_practice);

        // used for google analytics
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        // prevent the screen from going to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // get shared preferences
        prefs = this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        studentId = prefs.getString("studentId", null);

        // init a new cactus store and cactus
        cactus = new Cactus(PracticeActivity.this);
        cactusStore = new CactusStore(getApplicationContext(), studentId);

        offlineManager = OfflineManager.getInstance(this);

        // load the comments History Array from the cactus store
        commentHistory = cactusStore.load_comments_history();

        // get the textViews of the greeting text and cactus name
        greeting_text = (TextView) findViewById(R.id.greeting_text);
        activity_cactus_name = (TextView) findViewById(R.id.activity_cactus_name);

        soundSummary = new ArrayList<>();

        /* set the listening activity to be this activity, so that the session can be unregistered
            from any activity
        */
        ((AnalyticsApplication) getApplication()).setListeningActivity(this);
        ((AnalyticsApplication) getApplication()).createNewSessionRecord(getApplicationContext());
        sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();


        DefaultAudioAnalysisPublisher.getInstance(getApplicationContext())
                .register(this);

    }

    @Override
    protected void onPause() {

        // called whenever the user leaves this activity

        super.onPause();
        this.cactus.pause();

        DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).unregister(this);

        // if the user is leaving this page, send a practice report to server
        if (leaving) {
            this.finishPractice();
        }


    }

    @Override
    protected void onResume() {

        // called whenever the app returns to this activity
        // onCreate and onResume will always both be called the first time an Activity is started.

        super.onResume();
        updateCactusStore();

        offlineManager.clearCache();

        this.cactus.resume();

        // this tracks whether or not the currently stored practice report has been
        // sent from another activity
        boolean sentData = prefs.getBoolean("sentData", false);

        // if the practice report has been sent to the server then start a new session
        if (ended || sentData){
            ((AnalyticsApplication) getApplication()).setListeningActivity(this);

            // create a new sessionRecord if the previous session report has been sent to the server
            ((AnalyticsApplication) getApplication()).createNewSessionRecord(getApplicationContext());
            sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();
            ended = false;
        }

        analytics.trackScreen(this.getClass().getSimpleName());

        // leaving marks that the user is leaving from PracticeActivity and onPause if leaving
        // is set to true, then a practice report will be sent to the server
        leaving = true;
    }

    @Override
    public void listenForAnalysis(AudioAnalysis analysis) {
        float[] thisSegment = analysis.getFFTSound();

        float mean = 0;
        for (float f : thisSegment){
            mean += f/thisSegment.length;
        }

        soundSummary.add(mean);
    }

    public void displayNotifications(View v) {

        // This function gets called every time the user clicks on the bell image on top left corner


        // clear the notifications list
        notificationsList.clear();

        // set the server information for this API call
        request = getString(R.string.get_server_Request);
        requestAddress = getString(R.string.notifications_server_call);

        // create a new SendApplication Task to send the sever request
        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {

                String str;

                if (serverResponse.getCode() == 200) {

                    try {
                        // get the server response
                        JSONObject cactusNameOBJ = serverResponse.getResponse();

                        if (cactusNameOBJ != null) {
                            JSONArray notList = (JSONArray) cactusNameOBJ.get("recordings");

                            for (int i = 0; i < notList.length(); i++) {
                                JSONObject elem = (JSONObject) notList.get(i);

                                // get the title of the recording
                                String title = elem.getString("description");

                                // get all the comments
                                JSONArray comments = (JSONArray) elem.get("comments");

                                // loop through all the comments
                                for (int j = 0; j < comments.length(); j++) {

                                    // get all the information from the actual comment
                                    JSONObject specificNot = (JSONObject) comments.get(j);

                                    String commentID = specificNot.getString("_id");
                                    String contents = specificNot.getString("contents");
                                    String whoCommented = specificNot.getString("commenterName");

                                    // build the notification
                                    str = whoCommented + " commented on: '" + title + "'\n'"
                                            + contents + "'";

                                    // if commentID is not in commentsHistory dictionary then add it
                                    // else do not add it in twice
                                    if (commentHistory != null) {
                                        if (!commentHistory.contains(commentID)) {
                                            commentHistory.add(commentID);
                                            notificationsList.add(str);
                                        }
                                    }else {
                                        commentHistory.add(commentID);
                                        notificationsList.add(str);
                                    }
                                }

                            }

                            // if no new notifications don't save the comment History
                            if (notificationsList.isEmpty()) {

                                notificationsList = new ArrayList<>(Arrays.asList(
                                        "You have no unread notifications!"));
                            }
                            else {

                                // save the new comments to sharedPref
                                cactusStore.save_comment_history(commentHistory);
                            }


                            // create a new DialogFragment to display the notifications
                            DialogFragment newFragment = NotificationsFragment.newInstance(notificationsList);
                            newFragment.show(getFragmentManager(), "dialog");


                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // send the task
        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);
    }

    public void doExitSurvey() {

        // this function is called in the Cactus.java when the student has met their daily practice goal

        leaving = false;

        Intent exitSurveyIntent = new Intent(PracticeActivity.this, ExitSurveyActivity.class);
        startActivity(exitSurveyIntent);
    }

    public String updateCactusName(final SharedPreferences.Editor editor) {

        // this function gets called once inside of updateCactusStore

        request = getString(R.string.get_server_Request);
        requestAddress = getString(R.string.user_info_server_call);

        System.out.println("token:" + prefs.getString("token", "default"));

        // create a new SendApplication Task to send the sever request
        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() == 200) {
                    try {

                        // get the response
                        JSONObject cactusNameOBJ = serverResponse.getResponse();

                        // if an object was sent back, then get the name of the cactus
                        // else set the value to null
                        cactusName = ((cactusNameOBJ != null) ? cactusNameOBJ.get("cactusName").toString() : null);
                    }
                    catch (JSONException e) {
                        cactusName = null;
                    }
                } else {
                    Toast.makeText(PracticeActivity.this, R.string.user_network_error,
                                Toast.LENGTH_LONG).show();
                }


                // if cactusName does not equal "null" then store the cactus name in sharedPrefs
                if (editor != null && !cactusName.equals("null")) {
                    editor.putString("cactusName", cactusName);
                    editor.apply();
                } else {
                    cactusName = null;
                }

                // since this task is asynchronous, update the textView when it comes with the new cactus name
                // and save the new cactus name
                if (cactusName != null && !serverResponse.getResponse().isNull("cactusName")) {
                    activity_cactus_name.setText(cactusName);
                    cactusStore.save_cactusName(cactusName);
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);

        return cactusName;

    }

    public void updateCactusStore() {

        // gets called every time the onResume function is called

        request = getString(R.string.get_server_Request);
        requestAddress = getString(R.string.student_info_server_call) + studentId;

        System.out.println("***addr:" + requestAddress);


        // if there is an internet connection
        if (offlineManager.networkAvailable()) {

            // get the editor for shared prefs
            final SharedPreferences.Editor newEditor = this.getSharedPreferences(
                    "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE).edit();

            // create a new SendApplication Task to send the sever request
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {

                    // if we get a 500 error then there was an error with server
                    System.out.println("code:" + serverResponse.getCode());
                    if (serverResponse.getCode() > 400) {

                        if (studentId != null) {
                            Toast.makeText(PracticeActivity.this, R.string.student_network_error,
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        try {
                            JSONObject json = serverResponse.getResponse();

                            // get the ID of the teacher, null if student not enrolled
                            String teacherExistsStr = getJsonString(json, "teacher");
                            boolean teacherExists = true;

                            // if teacherExistsStr is null or empty, no teacher has enrolled this student
                            if (("".equals(teacherExistsStr)) || teacherExistsStr == null) {
                                teacherExists = false;
                            }

                            // get the practice goal the teacher has set for student (in milliseconds)
                            Long newGoal = Long.valueOf((int) json.get("desired_practice_time") * 60 * 1000);

                            // save all the student info
                            cactusStore.save_username(getJsonString(json, "username"));
                            cactusStore.save_name(getJsonString(json, "name"));
                            cactusStore.save_grade(getJsonString(json, "grade"));
                            cactusStore.save_suggestion_on((boolean) json.get("suggestionOn"));
                            cactusStore.save_student_enrolled(teacherExists);
                            cactusStore.save_practice_goal(newGoal);

                            // get cactusName from shared Prefs and save it in Cactus Store
                            String newCactusName = updateCactusName(newEditor);

                            // get the practice goal and how much practice is left
                            Long oldGoal = cactusStore.load_practice_goal();
                            Long practiceLeft = cactusStore.load_practice_left();

                            /* if the old goal and the new goal are not the same then
                            *      subtract the practice left from the old goal to get amount practiced
                            *      subtract the recent practice goal from  the amount practiced
                            *      save this value as the updated practice left
                            */
                            if (!oldGoal.equals(newGoal)) {

                                int amountPracticed = (int) (oldGoal - practiceLeft);
                                long newPracticeLeft = newGoal - amountPracticed;

                                cactus.setPracticeLeft(newPracticeLeft);
                                cactus.setPracticeGoal(newGoal);
                            }

                            // when creating new account, student has not practiced yet
                            if (previousIntent.getStringExtra("newAccount") != null) {
                                cactusStore.save_practice_left(newGoal);

                                // 0L means the number zero of type long
                                cactusStore.save_time_goal_reached(0L);
                            }

                            username = cactusStore.load_username();

                            if (!"".equals(username) && teacherExists) {
                                greeting_text.setText("Hello " + username + "!");
                            }

                            if (newCactusName != null && !"null".equals(newCactusName)) {
                                activity_cactus_name.setText(newCactusName);

                            }

                            // if the teacher has enabled practice list on the web portal, show it
                            if (cactusStore.load_suggestion_on()) {
                                findViewById(R.id.button_practice_list).setVisibility(Button.VISIBLE);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    request, requestAddress, null);
        }
    }

    private String getJsonString(JSONObject json, String fieldName) {

        // called whenever we have a response from server and we want to get variable from server
        // return the field if it is not null

        try {
            if (!json.isNull(fieldName)) {
                return (String) json.get(fieldName);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void onMenuPressed(View view) {

        // called whenever the sun menu (top right hand corner) is tapped

        leaving = false;
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    public void toShare(View view){

        // called whenever the shared button is pressed

        // if student doesnt have a teacher; don't allow them to share
        if (!cactusStore.load_student_enrolled()) {
            Toast.makeText(PracticeActivity.this, R.string.not_enrolled, Toast.LENGTH_LONG).show();
        }
        else {

            // stop the cactus from listening, and start new ShareActivity
            leaving = false;
            DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).unregister(this);
            DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).pause();
            Intent shareIntent = new Intent(this, ShareActivity.class);
            shareIntent.putExtra("username", username);
            startActivity(shareIntent);

        }
    }

    public void toPracticeList(View view) {

        // called whenever the practiceList button is pressed

        analytics.trackEvent(eventCategory, "PracticeListGetFirst");

        // if no internet connection, notify the user
        if (!offlineManager.networkAvailable()) {

            // create an alert
            AlertDialog.Builder notConnected = new AlertDialog.Builder(this);
            notConnected.setMessage(R.string.not_connected)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .show();

        } else {
            request = getString(R.string.get_server_Request);
            requestAddress = getString(R.string.practice_list_server_call);

            // create a new SendApplication Task to send the sever request
            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() == 200) {

                        // everything is okay
                        practiceSuggestion = serverResponse.getResponse();
                        displaySuggestion();
                    } else if (serverResponse.getCode() == 404) {

                        // teacher has not enabled this feature for this student
                        Toast.makeText(PracticeActivity.this, R.string.not_enabled, Toast.LENGTH_LONG).show();
                    } else if (serverResponse.getCode() == 444) {

                        // student is not yet enrolled by teacher
                        Toast.makeText(PracticeActivity.this, R.string.not_enrolled, Toast.LENGTH_LONG).show();
                    } else {

                        // server error
                        Toast.makeText(PracticeActivity.this, R.string.practice_list_network_error, Toast.LENGTH_LONG).show();
                    }
                }
            });

            sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    request, requestAddress, null);
        }
    }

    private void displaySuggestion() {

        // called when a valid practiceList JSONObject is sent back from server in toPracticeList()

        String suggestion = "";

        try {
            boolean isCustom = practiceSuggestion.getBoolean("isCustom");
            if (isCustom) {
                suggestion = "\n" + practiceSuggestion.optString("contents");
            } else {
                String otherInstructions = StringEscapeUtils.unescapeJava(practiceSuggestion.optString("otherInstructions"));
                if (otherInstructions != null && otherInstructions.length() > 0)
                    otherInstructions += "\n";

                String style = practiceSuggestion.optString("style");
                if (style != null && style.length() > 0)
                    style += ", ";

                suggestion =
                        StringEscapeUtils.unescapeJava(practiceSuggestion.optString("key")) + " " +
                                practiceSuggestion.optString("quality") + " " +
                                practiceSuggestion.optString("pattern") + "\n" +
                                otherInstructions +
                                practiceSuggestion.optString("playedWith") + ", " +
                                style +
                                practiceSuggestion.optString("length") + "\n" +
                                "Tempo: \uD834\uDD5F\u0020 = " +
                                practiceSuggestion.optString("tempo") +
                                ", Note Value = " +
                                StringEscapeUtils.unescapeJava(practiceSuggestion.optString("value"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        suggestionView = new TextView(this);
        suggestionView.setGravity(Gravity.CENTER);
        suggestionView.setTextSize(18);
        suggestionView.setTextColor(Color.BLACK);
        suggestionView.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/FreeSerif.ttf"));
        suggestionView.setText(suggestion);

        // create the dialog that displays each practice suggestion
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(suggestionView)
                .setPositiveButton("\uD83D\uDC4D", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        analytics.trackEvent(eventCategory, "PracticeListThumbsUp");
                        sendFeedback(true);
                        toPracticeList(null);
                    }
                })
                .setNeutralButton("\uD83D\uDC4E", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        analytics.trackEvent(eventCategory, "PracticeListThumbsDown");
                        sendFeedback(false);
                        toPracticeList(null);
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().getAttributes().y = 500; // TODO: should be dynamic
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                buttonSettings(dialog.getButton(AlertDialog.BUTTON_POSITIVE));
                buttonSettings(dialog.getButton(AlertDialog.BUTTON_NEUTRAL));
            }
        });
        dialog.show();
    }

    private void buttonSettings(Button button) {

        // called in displaySuggestion() to style the passed in button


        button.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Quivira.otf"));
        button.setTextSize(28);

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    view.setBackgroundColor(Color.GRAY);
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    view.setBackgroundColor(Color.WHITE);
                }
                return false; // false so that onClickListener will still fire
            }
        });
    }

    private void sendFeedback(boolean isGood) {

        // called whenever student clicks the thumbs up button to a practice list item

        String request = "POST";
        requestAddress = getString(R.string.practice_suggestions_server_call);
        String requestBody = "isGood=" + isGood + "&suggestion=" + practiceSuggestion;

        // create a new SendApplication Task to send the sever request
        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if(serverResponse.getCode() >= 400) {
                    Toast.makeText(PracticeActivity.this, R.string.practice_suggestion_network_error,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, requestBody);
    }

    public void punchCactus(View view) {

        // this function is called everytime the cactus is tapped


        // mood is made sadder whenever the cactus is tapped
        this.cactus.punch();
        analytics.trackEvent("PunchCactus", String.valueOf(cactus.getMood()));
    }

    public void finishPractice() {

        // this function is called in the onPause() function

        // it sends the current session data to the server and displayed in the Progress tab

        // set ended to true indicating a new sessionRecord has to be created
        ended = true;

        // end the current session
        sessionRecord.end_session();
        int[] pianoKeyCount = sessionRecord.get_piano_key_count();
        String keyCountString = "";

        // get all the notes played
        for (int value : pianoKeyCount) {
            keyCountString += String.valueOf(value) + ",";
        }

        // if the amount of piano time played (in milliseconds) is not 0, send the data
        if (sessionRecord.get_piano_time() / 1000 > 0) {

            // create a new practiceSession object
            PracticeSession session = new PracticeSession(this,
                    String.valueOf(sessionRecord.get_piano_time() / 1000),
                    String.valueOf(sessionRecord.get_start_time()),
                    String.valueOf(sessionRecord.get_end_time()),

                    // get rid of the last comma
                    keyCountString.substring(0, keyCountString.length() - 1)
            );

            // send the session to the offline manager in case the network is interrupted
            offlineManager.sendFileAttempt(session);
        }

    }
}
