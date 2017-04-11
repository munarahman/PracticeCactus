package com.practicecactus.practicecactus.Utils;

import android.app.Activity;
import android.widget.EditText;

import com.practicecactus.practicecactus.Cacheable.impl.PracticeSession;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.SessionRecord.impl.DefaultSessionRecord;

import java.util.HashMap;

/**
 * Created by Muna on 2017-03-09.
 */

public class CommonFunctions {
    // these functions are common amoungst many activities

    public boolean finishPractice(DefaultSessionRecord sessionRecord,Activity activity, OfflineManager offlineManager) {

        // it sends the current session data to the server and displayed in the Progress tab

        // end the current session
        sessionRecord.end_session();

        // get all the keys played
        int[] pianoKeyCount = sessionRecord.get_piano_key_count();
        String keyCountString = "";

        for (int value : pianoKeyCount) {
            keyCountString += String.valueOf(value) + ",";
        }

        // if the amount of piano time played (in milliseconds) is not 0, send the data
        if (sessionRecord.get_piano_time() / 1000 > 0) {

            // create a new session record
            PracticeSession session = new PracticeSession(activity,
                    String.valueOf(sessionRecord.get_piano_time() / 1000),
                    String.valueOf(sessionRecord.get_start_time()),
                    String.valueOf(sessionRecord.get_end_time()),

                    // get rid of the last comma
                    keyCountString.substring(0, keyCountString.length() - 1)
            );

            // try to send the file to server
            offlineManager.sendFileAttempt(session);
        }

        return true;
    }

    public boolean checkEmpty(HashMap<String, EditText> fields) {

        // this function is called to check whether or not the dictionary of fields passed contain
        // any empty values

        boolean isValid = true;

        // loop through the dictionary and check if the field is empty
        for (String field: fields.keySet()) {
            EditText fieldEditText = fields.get(field);

            if (field.isEmpty()) {
                fieldEditText.setError("Missing field");
                isValid = false;
            }
        }

        return isValid;

    }
}
