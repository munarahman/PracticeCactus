package com.practicecactus.practicecactus.Cactus;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

/**
 * Created by Yuan on 3/14/2016.
 */
public class CactusStore {

//    private static CactusStore instance;
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
    private final String LATEST_HEARD = "latest_heard";
    private final String SESSION_LENGTH = "session_length";
    private final String SUGGESTION_ON = "suggestion_on";


//    public static CactusStore getInstance(Context AppContext, String setting_file_name){
//        if (instance == null){
//            instance = new CactusStore();
//            instance.init(AppContext, setting_file_name);
//        }
//        return instance;
//    }
//
//    public static CactusStore getInstance(Context AppContext){
//        if (instance == null){
//            instance = new CactusStore();
//            instance.init(AppContext, "DEFAULT");
//        }
//        return instance;
//    }
//
//    public void init(Context appContext, String setting_file_name){
//        this.settings = appContext.getSharedPreferences(setting_file_name, 0);
//    }

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

    public void save_nickname(String nickname){
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

    public void save_last_time_music_heard(Date latest_time){
        save_time(latest_time, LATEST_HEARD);
    }

    public void save_suggestion_on(boolean suggestion_on) {
        SharedPreferences.Editor editor = this.settings.edit();
        editor.putBoolean(this.SUGGESTION_ON, suggestion_on);
        editor.commit();
    }

    public String load_username(){
        String username = settings.getString(this.USERNAME, null);
        return username;
    }

    public String load_cactusName() {
        String cactusName = settings.getString(this.CACTUSNAME, null);
        return cactusName;
    }

    public String load_nickname(){
        String nickname = settings.getString(this.NICKNAME, null);
        return nickname;
    }

    public String load_grade() {
        String grade = settings.getString(this.GRADE, null);
        return grade;
    }

    public Long load_practice_goal() {
        Long goal = settings.getLong(this.PRACTICE_GOAL, 60 * 60 * 1000);
        return goal;
    }

    public Long load_practice_left() {
        Long practice_left = settings.getLong(this.PRACTICE_LEFT, load_practice_goal());
        return practice_left;
    }

    public Long load_time_goal_reached() {
        Long time_goal_reached = settings.getLong(this.TIME_GOAL_REACHED, 0L);
        return time_goal_reached;
    }

    public float load_latest_mood(){
        float latest_mood = settings.getFloat(this.LATEST_MOOD, 0);
        return latest_mood;
    }

    public int load_session_length(){
        int session_length = settings.getInt(this.SESSION_LENGTH, 5);
        return session_length;
    }

    public Date load_last_mood_time(){
        Date last_mood_time = load_time(this.LATEST_PRACTICE);
        return last_mood_time;
    }
    public Date load_last_time_music_heard(){
        Date last_time_music_heard = load_time(this.LATEST_HEARD);
        return last_time_music_heard;
    }

    public boolean load_suggestion_on() {
        boolean suggestion_on = settings.getBoolean(this.SUGGESTION_ON, false);
        return suggestion_on;
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

        Date date_time = new Date(time_millis);

        return date_time;
    }
}
