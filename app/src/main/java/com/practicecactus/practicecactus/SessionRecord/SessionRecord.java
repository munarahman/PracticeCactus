package com.practicecactus.practicecactus.SessionRecord;

import android.content.Context;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 3/5/2016.
 */
public interface SessionRecord {


    long get_piano_time();

    void end_session();

    long get_total_time();

    long get_start_time();

    long get_end_time();

    //returns a dictionary of dominant piano keys played (key) and the absolute number of times the key was the dominant key (val)
    int[] get_dominant_keys_played();

    int[] get_piano_key_count();

}
