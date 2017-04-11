package com.practicecactus.practicecactus.Cactus;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yuan on 3/14/2016.
 */
public class CactusStore {

    private SharedPreferences settings;

    //define Constants:
    private final String USERNAME = "username";
    private final String CACTUSNAME = "cactusName";
    private final String NICKNAME = "nickname";
    private final String GRADE = "grade";
    private final String PRACTICE_GOAL = "practice_goal";
    private final String PRACTICE_LEFT = "practice_left";
    private final String TIME_GOAL_REACHED = "time_goal_reached";
    private final String LATEST_MOOD = "latest_mood";
    private final String LATEST_PRACTICE = "latest_practice";
    private final String SESSION_LENGTH = "session_length";
    private final String SUGGESTION_ON = "suggestion_on";
    private final String ENROLLED = "enrolled";
    private final String COMMENTS_HISTORY = "comments_history";


    public CactusStore(Context appContext, String setting_file_name) {
        this.settings = appContext.getSharedPreferences(setting_file_name, 0);
    }

    public void save_username(String username){
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putString(USERNAME, username);
        editor.commit();
    }

    public void save_cactusName(String cactusName){
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putString(CACTUSNAME, cactusName);
        editor.commit();
    }

    public void save_name(String nickname){
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putString(NICKNAME, nickname);
        editor.commit();
    }

    public void save_grade(String grade) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putString(GRADE, grade);
        editor.commit();
    }

    public void save_practice_goal(Long goal) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putLong(PRACTICE_GOAL, goal);
        editor.commit();
    }

    public void save_practice_left(Long practice_left) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putLong(PRACTICE_LEFT, practice_left);
        editor.commit();
    }

    public void save_time_goal_reached(Long time_goal_reached) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putLong(TIME_GOAL_REACHED, time_goal_reached);
        editor.commit();
    }

    public void save_latest_mood(float latest_mood){
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putFloat(LATEST_MOOD, latest_mood);
        editor.commit();
    }

    public void save_session_length(int session_length) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putInt(this.SESSION_LENGTH, session_length);
        editor.commit();
    }

    public void save_last_mood_time(Date latest_practice){
        save_time(latest_practice, LATEST_PRACTICE);
    }

    public void save_suggestion_on(boolean suggestion_on) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean(this.SUGGESTION_ON, suggestion_on);
        editor.commit();
    }

    public void save_student_enrolled(boolean enrolled) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean(this.ENROLLED, enrolled);
        editor.commit();
    }

    public void save_comment_history(ArrayList<String> commentsHistory) {

        // convert the list passed in as a set then save it to sharedPrefs
        // this is because sharedPrefs can save a set but not an array

        SharedPreferences.Editor editor = this.settings.edit();

        Set<String> set = new HashSet<String>();
        set.addAll(commentsHistory);
        editor.putStringSet(this.COMMENTS_HISTORY, set);

        editor.commit();
    }

    public String load_username(){
        return settings.getString(this.USERNAME, null);
    }

    public String load_cactusName() {
        return settings.getString(this.CACTUSNAME, null);
    }

    public String load_name(){
        return settings.getString(this.NICKNAME, null);
    }

    public String load_grade() {
        return settings.getString(this.GRADE, null);
    }

    public Long load_practice_goal() {
        // set the default time for all new users to 100 minutes for practice goal
        return settings.getLong(this.PRACTICE_GOAL, 6000000);
    }

    public Long load_practice_left() {
        return settings.getLong(this.PRACTICE_LEFT, load_practice_goal());
    }

    public Long load_time_goal_reached() {
        return settings.getLong(this.TIME_GOAL_REACHED, 0L);
    }

    public float load_latest_mood(){
        return settings.getFloat(this.LATEST_MOOD, 0);
    }

    public int load_session_length(){
        return settings.getInt(this.SESSION_LENGTH, 5);
    }

    public Date load_last_mood_time(){
        return load_time(this.LATEST_PRACTICE);
    }

    public boolean load_suggestion_on() {
        return settings.getBoolean(this.SUGGESTION_ON, false);
    }

    public boolean load_student_enrolled() {
        return settings.getBoolean(this.ENROLLED, false);

    }


    public ArrayList<String> load_comments_history() {

        // get the set from sharedprefs then convert it back to an array

        ArrayList<String> newList = new ArrayList<>();

        Set<String> newSet = settings.getStringSet(this.COMMENTS_HISTORY, null);

        if (newSet != null){
            newList.addAll(newSet);
        }

        return newList;
    }

    private void save_time(Date time, String key) {
        if (time == null) {
            time = new Date();
        }

        long latest_practice_millis = time.getTime();

        SharedPreferences.Editor editor = this.settings.edit();
        editor.putLong(key, latest_practice_millis);
        editor.commit();
    }

    private Date load_time(String key) {
        Date curr_time = new Date();

        long curr_time_long = curr_time.getTime();
        long time_millis = settings.getLong(key, curr_time_long);

        return new Date(time_millis);
    }
}
