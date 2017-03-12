package com.practicecactus.practicecactus.Utils;

import android.app.Activity;

import com.practicecactus.practicecactus.Activities.PracticeActivity;
import com.practicecactus.practicecactus.Cacheable.impl.PracticeSession;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;

/**
 * Created by Muna on 2017-03-09.
 */

public class CommonFunctions {

    public boolean finishPractice(DefaultSessionRecord sessionRecord,Activity activity, OfflineManager offlineManager) {

        sessionRecord.end_session();
        int[] pianoKeyCount = sessionRecord.get_piano_key_count();
        String keyCountString = "";

        for (int value : pianoKeyCount) {
            keyCountString += String.valueOf(value) + ",";
        }

        if (sessionRecord.get_piano_time() / 1000 > 0) {

            PracticeSession session = new PracticeSession(activity,
                    String.valueOf(sessionRecord.get_piano_time() / 1000),
                    String.valueOf(sessionRecord.get_start_time()),
                    String.valueOf(sessionRecord.get_end_time()),
                    // get rid of the last comma
                    keyCountString.substring(0, keyCountString.length() - 1)
            );

            System.out.println("PIANO TIME: " + sessionRecord.get_piano_time());

            offlineManager.sendFileAttempt(session);
        }

        return true;


    }
}
