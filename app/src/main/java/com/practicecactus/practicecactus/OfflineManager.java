package com.practicecactus.practicecactus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.widget.Toast;

import com.practicecactus.practicecactus.Cacheable.Cacheable;
import com.practicecactus.practicecactus.Cacheable.impl.AudioRecording;
import com.practicecactus.practicecactus.Cacheable.impl.ExitSurvey;
import com.practicecactus.practicecactus.Cacheable.impl.PracticeSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by christopherarnold on 2016-09-27.
 */

public class OfflineManager {

    private static OfflineManager instance;
    public Context context;
    public Activity activity;

    private File offlineDir;

    // these variables could be moved to Utils/Constants ?
    private static final String offlineDirName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/offline_cache";
    private static final String recordingsCache = offlineDirName + "/recordings_cache.csv";
    private static final String practicesCache = offlineDirName + "/practices_cache.csv";
    private static final String surveysCache = offlineDirName + "/surveys_cache.csv";

    public static OfflineManager getInstance(Activity callingActivity) {
        if (instance == null) {
            instance = new OfflineManager();
            instance.init(callingActivity);
        }
        return instance;
    }

    public void init(Activity callingActivity) {
        activity = callingActivity;
        context = callingActivity.getApplicationContext();

        offlineDir = new File(offlineDirName);

        if (!offlineDir.exists() && !offlineDir.isDirectory()) {
            offlineDir.mkdir();
        }
    }

    public void clearCache() {
        File offlineCaches[] = offlineDir.listFiles();

        if (networkAvailable() && offlineCaches != null) {
            System.out.println("CLEARING CACHE...");

            int recordingCount = 0;
            int sessionCount = 0;
            int surveyCount = 0;

            try {

                for (int i = 0; i < offlineCaches.length; i++) {

                    String filePath = offlineCaches[i].getAbsolutePath();
                    File file = new File(filePath);
                    Scanner scanner = new Scanner(new FileInputStream(filePath));
                    String[] line;

                    while (scanner.hasNextLine()) {
                        line = scanner.nextLine().split(",");

                        // case: audio recordings
                        if (filePath.equals(recordingsCache)) {

                            AudioRecording recording = new AudioRecording(
                                    activity, line[0], line[1], line[2], line[3]);

                            recording.send();
                            recordingCount++;

                            // case: practice sessions
                        } else if (filePath.equals(practicesCache)) {
                            String pianoTime = line[0];
                            String startTime = line[1];
                            String endTime = line[2];
                            String keyCountLine = "";

                            if (scanner.hasNextLine()) {
                                keyCountLine = scanner.nextLine();
                            }

                            PracticeSession session = new PracticeSession(
                                    activity, pianoTime, startTime, endTime, keyCountLine
                            );

                            session.send();
                            sessionCount++;

                            // case: exit survey
                        } else if (filePath.equals(surveysCache)) {

                            ExitSurvey survey = new ExitSurvey(
                                    activity, line[0], line[1], line[2]
                            );

                            survey.send();
                            surveyCount++;
                        }
                    }

                    file.delete();

                    if (recordingCount != 0) {
                        Toast.makeText(
                                this.context,
                                Integer.toString(recordingCount) + " recordings sent!",
                                Toast.LENGTH_SHORT).show();

                        // delete all recordings saved in storage
//                        File recordingsDir = new File(Environment.getExternalStoragePublicDirectory(
//                                Environment.DIRECTORY_MUSIC), "PracticeCactus");
//                        for (File child : recordingsDir.listFiles()) {
//                            child.delete();
//                        }
//                        recordingsDir.delete();
                    }
                }

                System.out.println(recordingCount + " recording(s), " +
                        sessionCount + " session(s), " +
                        surveyCount + " survey(s) sent");

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean networkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void sendFileAttempt(Cacheable cacheable) {
        if (networkAvailable()) {
            cacheable.send();
        } else {
            save(cacheable);
        }
    }

    // write cacheable object ToString to file
    private void save(Cacheable cacheable) {
        String cacheFileName = "";
        if (cacheable instanceof AudioRecording) {
            cacheFileName = recordingsCache;
        } else if (cacheable instanceof PracticeSession) {
            cacheFileName = practicesCache;
        } else if (cacheable instanceof ExitSurvey) {
            cacheFileName = surveysCache;
        }

        File CSVfile = new File(cacheFileName);
        if (!CSVfile.exists() && !CSVfile.isDirectory()) {
            try {
                CSVfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            FileOutputStream out = new FileOutputStream(cacheFileName, true);
            out.write((cacheable + "\n").getBytes());
            out.flush();
            out.close();

            System.out.println("OfflineManager saving the following data to " + cacheFileName + ": ");
            System.out.println(cacheable);

            if (cacheable instanceof AudioRecording) {
                AlertDialog.Builder notConnected = new AlertDialog.Builder(this.activity);
                notConnected.setMessage(R.string.offline_recordings)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
