package com.practicecactus.practicecactus.SessionRecord.impl;

import android.content.Context;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisListener;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.SessionRecord.SessionRecord;
import com.practicecactus.practicecactus.AudioAnalysis.impl.TemporalSmoother;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Yuan on 3/5/2016.
 */
public class DefaultSessionRecord implements SessionRecord, AudioAnalysisListener {

    //objects:
    private TemporalSmoother smoothPredictor;

    private Context appContext;

    //attributes
    private long start_time;
    private long end_time;
    private Date last_update = new Date();
    private int milliseconds_played = 0;
    private int[] dominantKeysPlayed = new int[88];
    private ArrayList<Integer> last_keys_played = new ArrayList<Integer>();
    private int[] pianoKeyCounter = new int[88];


    public DefaultSessionRecord(Context appContext){
        this.appContext = appContext;
        this.start_time = System.currentTimeMillis(); //set the start time to the current time

        smoothPredictor = new TemporalSmoother();

        DefaultAudioAnalysisPublisher.getInstance(appContext)
                .register(this);
    }

    public void listenForAnalysis(AudioAnalysis analysis){
        if(this.smoothPredictor.smooth(analysis.isPiano())){
            this.update_playtime(true);
            this.update_keys_played(analysis.getDominantPianoKeys());
            this.update_keys_count(analysis.getDominantPianoKeys());;
        }
        else{
            this.update_playtime(false);
        }
    }

    private void update_playtime(Boolean is_piano){
        Date curr_time = new Date();
        if (is_piano){
            milliseconds_played += curr_time.getTime() - last_update.getTime();
        }
        last_update = curr_time;
    }

    private void update_keys_played(ArrayList<Integer> dominantKeysPlayed){
        if (dominantKeysPlayed.size()>0) {

            for (int i = 0; i < dominantKeysPlayed.size(); i++) {
                int curr_key = (int) dominantKeysPlayed.get(i);
                this.dominantKeysPlayed[curr_key] += 1;
            }
        }
        //System.out.println("Dominant piano keys:" + Arrays.toString(this.dominantKeysPlayed));

    }

    private void update_keys_count(ArrayList<Integer> dominantKeysPlayed){
        ArrayList<Integer> last_keys_array = this.last_keys_played;

        if (dominantKeysPlayed.size()>0) {
            for (int i = 0; i < dominantKeysPlayed.size(); i++) {
                int curr_key = (int) dominantKeysPlayed.get(i);

                //if the key was not in the previous set, we assume that it is a new key press
                if (!(last_keys_array.contains(curr_key))) {
                    this.pianoKeyCounter[curr_key] += 1;

                }

            }
            this.last_keys_played = dominantKeysPlayed;

        }
        //System.out.println("Dominant piano keys:" + Arrays.toString(this.pianoKeyCounter));

    }


    public long get_piano_time(){
        return milliseconds_played;
    }

    public void end_session(){
        end_time = System.currentTimeMillis();
        DefaultAudioAnalysisPublisher.getInstance(this.appContext)
                .unregister(this);
    }

    public long get_total_time() {
        return (this.end_time - this.start_time);
    }

    public long get_start_time(){
        return this.start_time;
    }

    public long get_end_time(){
        return this.end_time;
    }

    public int[] get_dominant_keys_played(){
        //returns a dictionary of dominant piano keys played (key) and the absolute number of times the key was the dominant key (val)
        return this.dominantKeysPlayed;
    }

    public int[] get_piano_key_count(){
        return this.pianoKeyCounter;
    }

}
