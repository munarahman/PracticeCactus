package com.practicecactus.practicecactus.Cactus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisListener;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.AudioAnalysis.impl.TemporalSmoother;
import com.practicecactus.practicecactus.Activities.PracticeActivity;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;


/**
 * An object to act as a controller for the cactus pet
 */
public class Cactus implements AudioAnalysisListener{

    public boolean doingSurvey = false;

    private PracticeActivity activity;
    private CactusMoodView moodView;
    private CactusView cactusView;
    private CactusStore cactusStore;
    private float mood;
    private TemporalSmoother temporalSmoother;
    private DateTime lastUpdate;
    private float moodStep;
    private String studentId;
    private Long practiceGoal;      // in milliseconds (unix time)
    private Long practiceLeft;      // in milliseconds
    private Long timeGoalReached;   // in milliseconds
    private boolean heardMusic = false;
    private DateTime lastTime = new DateTime();


    public Cactus(PracticeActivity activity) {
        this.activity = activity;
        this.doingSurvey = false;

        // get the shared prefs
        SharedPreferences prefs = activity.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);

        // get student ID
        studentId = prefs.getString("studentId", null);

        DefaultAudioAnalysisPublisher.getInstance(activity.getApplicationContext()).register(this);
        cactusStore = new CactusStore(activity.getApplicationContext(), studentId);

        // init the mood, create a new cactus view
        this.mood = this.initMood();
        this.cactusView = new CactusView((ImageView) activity.findViewById(R.id.cactus_view));
        this.temporalSmoother = new TemporalSmoother();

        // get useful variables
        moodView = (CactusMoodView) activity.findViewById(R.id.mood_bar);
        practiceGoal = cactusStore.load_practice_goal();
        practiceLeft = cactusStore.load_practice_left();
        timeGoalReached = cactusStore.load_time_goal_reached();

    }

    public float getMood() {
        return this.mood;
    }

    public void setPracticeLeft(Long amount) {
        this.practiceLeft = amount;
    }
    public void setPracticeGoal(Long amount) {
        this.practiceGoal = amount;
    }

    private float getMoodStep() {
        int targetSessionLength = cactusStore.load_session_length();
        if (targetSessionLength == 0) {
            return (float) 1 / (10000);
        }

        //mood step is set such that practice for targetSessionLength minutes increase the mood
        return (float) 1 / (targetSessionLength*60000);
    }

    private float initMood() {
        float lastVal = this.cactusStore.load_latest_mood();
        if (lastVal < 0) {
            lastVal = 0;
        }
        DateTime lastTime = new DateTime(this.cactusStore.load_last_mood_time());
        DateTime curTime = new DateTime();
        int minsBetween = Minutes.minutesBetween(lastTime, curTime).getMinutes();
        double newVal = Math.exp(Math.log(lastVal) - (minsBetween/1440.0 * Math.log(2)));
        this.mood = (Float.compare(lastVal, -1) == 0) ? (float) 0.3 : (float) newVal;
        return this.mood;
    }


    @Override
    public void listenForAnalysis(AudioAnalysis analysis) {
        float newVal;

        // get all the flags set for the screen for PracticeActivity
        int windowFlags = this.activity.getWindow().getAttributes().flags;

        // if the sound is piano
        if ( this.temporalSmoother.smooth(analysis.isPiano())) {

            // if the flag to keep screen on is unset, set it
            if ((windowFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }

            // if there was no last update then set it to now,
            // else it is the current time
            if (lastUpdate == null) {
                lastUpdate = new DateTime(analysis.getTime());
            } else {

                DateTime currTime = new DateTime();
                long msOfPractice = currTime.getMillis() - lastUpdate.getMillis();

                newVal = this.mood + msOfPractice * moodStep;
                this.mood = (newVal > 1) ? 1 : newVal;
                lastUpdate = currTime;

                // reminder: auto posting is independent of cactus's state,
                // only corresponds with daily practice goal set by teacher
                if (!practicedToday()) {
                    practiceLeft -= msOfPractice;

                    // if finished daily practice, auto post to server!
                    if (practiceLeft <= 0) {
                        sendAutoPost();
                        timeGoalReached = System.currentTimeMillis();
                        practiceLeft = practiceGoal; // reset practice time for tomorrow
                    }
                }

                // set heardMusic to true to indicate the cactus has heard music
                heardMusic = true;
            }
        } else {

            lastUpdate = null;

            // set heardMusic to false indicating the cactus has not heard any music
            if (heardMusic) {
                lastTime = new DateTime();
                heardMusic = false;
            }


            int secBetween = Seconds.secondsBetween(lastTime, new DateTime()).getSeconds();

            // if it has been 5 minutes with no playing, let go of control over screen.
            if (secBetween >= 300) {

                // if the flag to keep screen on is set, unset it
                if ((windowFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0) {
                    this.activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

            }
            else {
                // if the flag to keep screen on is set, unset it
                if ((windowFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                    this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

            }
        }
        this.moodView.setMood(this.mood);
        this.cactusView.setMood(this.mood);
    }

    public void pause() {
        DefaultAudioAnalysisPublisher.getInstance(this.activity.getApplicationContext()).unregister(this);
        this.cactusStore.save_latest_mood(this.mood);
        this.cactusStore.save_last_mood_time(null);
        this.cactusStore.save_practice_left(practiceLeft);
        this.cactusStore.save_time_goal_reached(timeGoalReached);
    }

    public void resume() {
        moodStep = this.getMoodStep();
        practiceLeft = this.cactusStore.load_practice_left();
        timeGoalReached = this.cactusStore.load_time_goal_reached();
        DefaultAudioAnalysisPublisher.getInstance(this.activity.getApplicationContext()).register(this);
        this.initMood();
    }

    public void punch() {
        // decrease mood if cactus is tapped
        double newVal = this.mood - 0.1;
        this.mood = (newVal > 0) ? (float) newVal : 0;
    }

    public boolean practicedToday() {
        int millisInDay = 1000 * 60 * 60 * 24;
        int hoursAwayFromGMT = 5;
        int timeZoneAdjust = 1000 * 60 * 60 * hoursAwayFromGMT;
        Long previousMidnight = (timeGoalReached / millisInDay) * millisInDay + timeZoneAdjust;

        return System.currentTimeMillis() - previousMidnight < millisInDay;
    }

    // reminder: auto posting is independent of cactus's state
    public void sendAutoPost() {

        // set the parameters for the request
        String request = "POST";
        String requestAddress = "/api/practices/finish";

        // create a new SendApplication Task to send the sever request
        SendApplicationTask sat = new SendApplicationTask(activity, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() > 400) {
                    Toast.makeText(activity, R.string.failed_autopost_error, Toast.LENGTH_LONG).show();
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);

        // create the success dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        builder.setMessage(R.string.success_autopost)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                activity.doExitSurvey();
                            }
                        })
                .show();
    }
}
