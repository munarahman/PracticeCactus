package com.practicecactus.practicecactus.Cactus;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.view.WindowManager;
import android.widget.ImageView;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisListener;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.AudioAnalysis.impl.TemporalSmoother;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.Activities.PracticeActivity;
import com.practicecactus.practicecactus.R;
import com.practicecactus.practicecactus.ServerTasks.SendApplicationTask;
import com.practicecactus.practicecactus.ServerTasks.ServerResponse;
import com.practicecactus.practicecactus.Utils.Synthesizer;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import java.text.SimpleDateFormat;

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
    private Synthesizer synthesizer;

    OfflineManager offlineManager;

    public Cactus(PracticeActivity activity) {
        this.activity = activity;
        this.doingSurvey = false;

        SharedPreferences prefs = activity.getSharedPreferences(
                "USER_SHAREDPREFERENCES", Context.MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);

        DefaultAudioAnalysisPublisher.getInstance(activity.getApplicationContext()).register(this);
        cactusStore = new CactusStore(activity.getApplicationContext(), studentId);
        offlineManager = OfflineManager.getInstance(activity);


        this.mood = this.initMood();
        this.cactusView = new CactusView((ImageView) activity.findViewById(R.id.cactus_view));
        this.temporalSmoother = new TemporalSmoother();

        moodView = (CactusMoodView) activity.findViewById(R.id.mood_bar);
        practiceGoal = cactusStore.load_practice_goal();
        practiceLeft = cactusStore.load_practice_left();
        timeGoalReached = cactusStore.load_time_goal_reached();

//        synthesizer = new Synthesizer();

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
//            return (float) 0.5 / (10000);
            return (float) 1 / (10000);
        }
        //mood step is set such that practice for targetSessionLength minutes increase the mood
        return (float) 1 / (targetSessionLength*60000);
//        return (float) 0.5 / (targetSessionLength*60000);
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

    public void playMetronome() {

        synthesizer.play(Synthesizer.Note0.F, 2, 2.0/4);
        synthesizer.play(Synthesizer.Note0.F, 2, 2.0/4);

    }

    @Override
    public void listenForAnalysis(AudioAnalysis analysis) {
        float newVal;
        int windowFlags = this.activity.getWindow().getAttributes().flags;

//        playMetronome();


        if ( this.temporalSmoother.smooth(analysis.isPiano())) {

            // if the flag to keep screen on is unset, set it
            if ((windowFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) == 0) {
                this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }


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
                System.out.println("UPDATE: " + practiceGoal + " " + practiceLeft + " " + timeGoalReached);
            }
        } else {

            lastUpdate = null;

            // set heardMusic to false indicating the cactus has not heard any music
            if (heardMusic) {
                lastTime = new DateTime();
                heardMusic = false;
            }


            int secBetween = Seconds.secondsBetween(lastTime, new DateTime()).getSeconds();
            System.out.println("NOT LISTENING:" + secBetween);

            // 5 minutes with no playing, let go of control over screen.
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
        // use for debugging
//        practiceLeft = 0L;
//        timeGoalReached = 0L;
        DefaultAudioAnalysisPublisher.getInstance(this.activity.getApplicationContext()).register(this);
        this.initMood();
    }

    public void punch() {
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
        String request = "POST";
        String requestAddress = "/api/practices/finish";

        SendApplicationTask sat = new SendApplicationTask(activity, new SendApplicationTask.AsyncResponse() {
            @Override
            public void processFinish(ServerResponse serverResponse) {
                if (serverResponse.getCode() > 400) {
                    System.out.println("failed to auto post");
                }
            }
        });

        sat.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                request, requestAddress, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this.activity);
        builder.setMessage("Congratulations! You completed your daily practice goal. Please take a few moments to complete the following survey.")
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                activity.doExitSurvey();
                            }
                        })
                .show();
    }
}
