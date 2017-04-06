package com.practicecactus.practicecactus.Activities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import com.practicecactus.practicecactus.Utils.AudioGenerator;
import com.practicecactus.practicecactus.Utils.Metronome;
import com.practicecactus.practicecactus.Utils.NotificationsList;
import com.practicecactus.practicecactus.Utils.Synthesizer;

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
    private MediaPlayer mPlayer;

    public boolean leaving;
    public OfflineManager offlineManager;
    private SharedPreferences prefs;
    private Thread metronomeThread;
    private Synthesizer synthesizer;

    private ArrayList<String> notificationsList = new ArrayList<>();
    private ArrayList<String> commentHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previousIntent = getIntent();
        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        prefs = this.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        studentId = prefs.getString("studentId", null);
        System.out.println("studentID:" + studentId);

        setContentView(R.layout.activity_practice);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cactus = new Cactus(PracticeActivity.this);
        cactusStore = new CactusStore(getApplicationContext(), studentId);
        offlineManager = OfflineManager.getInstance(this);

        soundSummary = new ArrayList<>();

        /* set the listening activity to be this activity, so that the session can be unregistered
            from any activity
        */
        ((AnalyticsApplication) getApplication()).setListeningActivity(this);
        ((AnalyticsApplication) getApplication()).createNewSessionRecord(getApplicationContext());
        sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();


        greeting_text = (TextView) findViewById(R.id.greeting_text);
        activity_cactus_name = (TextView) findViewById(R.id.activity_cactus_name);

        DefaultAudioAnalysisPublisher.getInstance(getApplicationContext())
                .register(this);

        commentHistory = cactusStore.load_comments_history();

    }



    @Override
    protected void onPause() {
        super.onPause();
        this.cactus.pause();
        DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).unregister(this);

        if (leaving) {
            this.finishPractice();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCactusStore();

        offlineManager.clearCache();

        this.cactus.resume();

        boolean sentData = prefs.getBoolean("sentData", false);

        if (ended || sentData){
            ((AnalyticsApplication) getApplication()).setListeningActivity(this);

            ((AnalyticsApplication) getApplication()).createNewSessionRecord(getApplicationContext());
            sessionRecord = ((AnalyticsApplication) getApplication()).getSessionRecord();
            ended = false;
        }

        System.out.println("practice resume start time:" + sessionRecord.get_start_time());

        analytics.trackScreen(this.getClass().getSimpleName());

        leaving = true;
    }

    public void onClick(View v) {

        if (v.getId() == R.id.notifications) {
            System.out.println("HEY IM ACTUALLY USED *********");
            displayNotifications(v);
        }

    }

    void displayNotifications1(View v) {

//        notificationsList = new ArrayList<>(Arrays.asList(
//                "Muna Commented on POst", "Mags Commented on Post"));

        DialogFragment newFragment = NotificationsFragment.newInstance(notificationsList);
        newFragment.show(getFragmentManager(), "dialog");

        System.out.println("TOKEN:" + prefs.getString("token", "default"));
//        getNotifications();
    }

    void displayNotifications(View v) {

        notificationsList.clear();


        String request = "GET";
        String requestAddress = "/api/recordings/recordingsWithNewComments";

        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() < 400) {
                    System.out.println("getNotifications response:" + serverResponse.getResponse());

                    try {
                        JSONObject cactusNameOBJ = serverResponse.getResponse();
                        if (cactusNameOBJ != null) {
                            JSONArray notList = (JSONArray) cactusNameOBJ.get("recordings");

                            int i;
                            int j;
                            String str = null;

                            for (i = 0; i < notList.length(); i++) {
                                JSONObject elem = (JSONObject) notList.get(i);

                                // get the title of the recording
                                String title = elem.getString("description");

                                // get all the comments
                                JSONArray comments = (JSONArray) elem.get("comments");

                                // loop through all the comments
                                for (j = 0; j < comments.length(); j++) {

                                    // get all the information from the actual comment
                                    JSONObject specificNot = (JSONObject) comments.get(j);

                                    String commentID = specificNot.getString("_id");
                                    String contents = specificNot.getString("contents");
                                    String whoCommented = specificNot.getString("commenterName");

                                    // build the notification
                                    str = ("Username " + whoCommented + " commented on your " +
                                            "recording '" + title + "'. They said '" + contents + "'");

                                    // if the commentID is not in the commentsHistory dictionary then add it
                                    // else do not add it in twice
                                    if (commentHistory != null){
                                        System.out.println("YAYAYYAY THERES HISTORY");
                                        if (!commentHistory.contains(commentID)) {
                                            commentHistory.add(commentID);
                                            notificationsList.add(str);
                                        }
                                    }else {
                                        System.out.println("BOOOOO");
                                        commentHistory.add(commentID);
                                        notificationsList.add(str);
                                    }
                                }

                            }

                            // if no new notifications not save anything
                            if (notificationsList.isEmpty()) {

                                notificationsList = new ArrayList<>(Arrays.asList(
                                        "You have no unread notifications!"));
                            }
                            else {
                                System.out.println("LIST----:" + notificationsList.toString());
                                // save the new comments to sharedPref
                                cactusStore.save_comment_history(commentHistory);
                            }

                            System.out.println("notificationsList:**********************");
                            System.out.println(notificationsList.toString());
                            System.out.println("TOKEN:" + prefs.getString("token", "default"));

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

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);
    }


    public void playMetronome() {
        mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.metronome);

        // run the metronome beat on a separate thread
        metronomeThread = new Thread(new Runnable() {

            @Override
            public void run() {
                synthesizer = new Synthesizer();
                AudioGenerator audio = new AudioGenerator(7000);

                double[] silence = audio.getSineWave(500, 7000, 0);

                // fast is 2400
                int noteDuration = 4000;
                double frequency = 277.18;

                double[] reNote = audio.getSineWave(noteDuration, 7000, frequency);

                while(true){
                    try {
                        Thread.sleep(100);
                        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mPlayer.start();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });

        metronomeThread.start();
    }

    public void doExitSurvey() {
        leaving = false;
        Intent exitSurveyIntent = new Intent(PracticeActivity.this, ExitSurveyActivity.class);
        startActivity(exitSurveyIntent);
    }

    public String updateCactusName(final SharedPreferences.Editor editor) {

        String request = "GET";
        String requestAddress = "/api/users/me";

        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() < 400) {
                    System.out.println("updateCactusName response:" + serverResponse.getResponse().toString());

                    try {
                        JSONObject cactusNameOBJ = serverResponse.getResponse();
                        if (cactusNameOBJ != null) {
                            cactusName = cactusNameOBJ.get("cactusName").toString();
                        }
                        else {
                            cactusName = null;
                        }
                    }
                    catch (JSONException e) {
                        cactusName = null;
                    }
                }

                if (cactusName != "null") {
                    editor.putString("cactusName", cactusName);
                    editor.apply();
                }
                else {
                    cactusName = null;
                }


                System.out.println("CACTUSNAME in FUnc:" + cactusName);
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

        System.out.println("Student ID *****:" + studentId);

        System.out.println("IN UPDATECACTUSSTORE");
        String request = "GET";
        String requestAddress = "/api/students/" + studentId;

        if (offlineManager.networkAvailable()) {

            final SharedPreferences.Editor newEditor = this.getSharedPreferences(
                    "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE).edit();

            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    System.out.println("response:" + serverResponse.getResponse());
                    if (serverResponse.getCode() >= 400) {
                        System.out.println("Error retrieving student information");
                    } else {
                        try {
                            JSONObject json = serverResponse.getResponse();

                            cactusStore.save_username(getJsonString(json, "username"));
                            cactusStore.save_name(getJsonString(json, "name"));
                            cactusStore.save_grade(getJsonString(json, "grade"));
                            cactusStore.save_suggestion_on((boolean) json.get("suggestionOn"));

                            // get the ID of the teacher, null if student not enrolled
                            String teacherExistsStr = getJsonString(json, "teacher");
                            boolean teacherExists = true;

                            // if teacherExistsStr is null or empty, no teacher has enrolled this student
                            if (("".equals(teacherExistsStr)) || teacherExistsStr == null) {
                                teacherExists = false;
                            }

                            System.out.println("teacherExists:" + teacherExists);
                            cactusStore.save_student_enrolled(teacherExists);


                            // get cactusName from shared Prefs and save it in Cactus Store
                            String newCactusName = updateCactusName(newEditor);


                            Long newGoal = Long.valueOf((int) json.get("desired_practice_time") * 60 * 1000);
                            System.out.println("+++++NEW GOAL:" + newGoal);
                            Long oldGoal = cactusStore.load_practice_goal();
                            Long practiceLeft = cactusStore.load_practice_left();
                            cactusStore.save_practice_goal(newGoal);


                            if (oldGoal != newGoal) {

                                int amountPracticed = (int) (oldGoal - practiceLeft);

                                System.out.println("++++OLD GOAL:" + oldGoal);
                                System.out.println("++++PRACTICE LEFT:" + practiceLeft);
                                long newPracticeLeft = newGoal - amountPracticed;
//                                cactusStore.save_practice_left(newPracticeLeft);
                                cactus.setPracticeLeft(newPracticeLeft);
                                cactus.setPracticeGoal(newGoal);
                            }



                            // when creating new account, student has not practiced yet
                            if (previousIntent.getStringExtra("newAccount") != null) {
                                System.out.println("*********I'M A NEW ACCOUNT: setting new goal");
                                cactusStore.save_practice_left(newGoal);
                                cactusStore.save_time_goal_reached(0L);
                            }

                            String usrname = cactusStore.load_username();

                            if (!"".equals(usrname) && teacherExists) {
                                greeting_text.setText("Hello " + usrname + "!");
                            }

                            if (newCactusName != null && !"null".equals(newCactusName)) {
//                                System.out.println("SETTING CACTUS NAME");
//                                System.out.println("newCactusName:" + newCactusName);
                                activity_cactus_name.setText(newCactusName);

                            }

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

    @Override
    public void listenForAnalysis(AudioAnalysis analysis) {
        float[] thisSegment = analysis.getFFTSound();

        float mean = 0;
        for (float f : thisSegment){
            mean += f/thisSegment.length;
        }

        soundSummary.add(mean);
    }

    public void onMenuPressed(View view) {
        leaving = false;
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    public void toShare(View view){

        // if student doesnt have a teacher; dont allow them to share
        if (!cactusStore.load_student_enrolled()) {
            Toast.makeText(PracticeActivity.this, R.string.not_enrolled, Toast.LENGTH_LONG).show();
        }
        else {

            leaving = false;
            DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).unregister(this);
            DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).pause();
            Intent shareIntent = new Intent(this, ShareActivity.class);
            shareIntent.putExtra("username", username);
            startActivity(shareIntent);

        }
    }

    public void toPracticeList(View view) {
        analytics.trackEvent(eventCategory, "PracticeListGetFirst");

        if (!offlineManager.networkAvailable()) {
            AlertDialog.Builder notConnected = new AlertDialog.Builder(this);
            notConnected.setMessage(R.string.not_connected)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    })
                    .show();

        } else {
            String request = "GET";
            String requestAddress = "/api/suggestions/fetch";

            SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
                @Override
                public void processFinish(ServerResponse serverResponse) {
                    if (serverResponse.getCode() < 400) {
                        practiceSuggestion = serverResponse.getResponse();
                        displaySuggestion(practiceSuggestion);
                    } else if (serverResponse.getCode() == 404) {
                        Toast.makeText(PracticeActivity.this, "Your teacher has not enabled this yet.", Toast.LENGTH_LONG).show();
                    } else if (serverResponse.getCode() == 444) {
                        Toast.makeText(PracticeActivity.this, R.string.not_enrolled, Toast.LENGTH_LONG).show();
                    } else {
                        System.out.println("Could not fetch new practice suggestion");
                    }
                }
            });

            sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    request, requestAddress, null);
        }
    }

    private void displaySuggestion(JSONObject practiceSuggestion) {
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
        String request = "POST";
        String requestAddress = "/api/suggestions/feedback";
        String requestBody =
                "isGood=" + isGood +
                        "&suggestion=" + practiceSuggestion;

        SendApplicationTask sat = new SendApplicationTask(this, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if(serverResponse.getCode() >= 400) {
                    System.out.println("Error sending practice suggestion feedback");
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, requestBody);
    }

    public void punchCactus(View view) {
        this.cactus.punch();
        analytics.trackEvent("PunchCactus", String.valueOf(cactus.getMood()));
    }

    public void finishPractice() {
        ended = true;

        sessionRecord.end_session();
        int[] pianoKeyCount = sessionRecord.get_piano_key_count();
        String keyCountString = "";

        for (int value : pianoKeyCount) {
            keyCountString += String.valueOf(value) + ",";
        }

        if (sessionRecord.get_piano_time() / 1000 > 0) {

            PracticeSession session = new PracticeSession(this,
                    String.valueOf(sessionRecord.get_piano_time() / 1000),
                    String.valueOf(sessionRecord.get_start_time()),
                    String.valueOf(sessionRecord.get_end_time()),
                    // get rid of the last comma
                    keyCountString.substring(0, keyCountString.length() - 1)
            );

            System.out.println("PIANO TIME IN PRACTICE: " + sessionRecord.get_piano_time());

            offlineManager.sendFileAttempt(session);
        }

        System.out.println("practice finishPractice start time:" + sessionRecord.get_start_time());
    }
}
