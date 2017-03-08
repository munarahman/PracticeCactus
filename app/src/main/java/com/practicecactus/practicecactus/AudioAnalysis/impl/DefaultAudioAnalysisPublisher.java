package com.practicecactus.practicecactus.AudioAnalysis.impl;

import android.content.Context;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisListener;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisPublisher;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by matthew on 2016-02-26.
 */
public class DefaultAudioAnalysisPublisher implements AudioAnalysisPublisher {
    private static DefaultAudioAnalysisPublisher instance;

    private List<AudioAnalysisListener> subscribers;
    private AudioAnalysisTask mtask;
    private Context context;

    public static AudioAnalysisPublisher getInstance(Context applicationContext){
        if(instance == null){
            instance = new DefaultAudioAnalysisPublisher(applicationContext);
        }
        return instance;
    }

    private DefaultAudioAnalysisPublisher(Context applicationContext){
        this.subscribers = new LinkedList<AudioAnalysisListener>();
        this.context = applicationContext;

    }

    @Override
    public boolean pause() {
        this.stopBackgroundThread();
        return true;
    }

    @Override
    public boolean resume() {
        this.startBackgroundThread();
        return true;
    }

    @Override
    public void register(AudioAnalysisListener listener) {
        if(this.subscribers.size() == 0) {
            this.startBackgroundThread();
        }
        if (!this.subscribers.contains(listener)) {
            this.subscribers.add(listener);
        }
    }

    @Override
    public boolean unregister(AudioAnalysisListener listener) {
        boolean result = this.subscribers.remove(listener);
        System.out.println("subscribers size: " + this.subscribers.size());
        if (this.subscribers.size() == 0) {
            this.stopBackgroundThread();
        }
        return result;
    }

    private void startBackgroundThread() {
        if (this.mtask == null) {
            this.mtask = new AudioAnalysisTask(this, this.context);
            this.mtask.execute();
        }
    }

    private void stopBackgroundThread() {
        if (this.mtask != null) {
            this.mtask.releaseRecord();
            this.mtask.cancel(true);
            this.mtask = null;
        }
    }

    @Override
    public void distribute(AudioAnalysis analysis) {
        for (AudioAnalysisListener listener: this.subscribers){
            listener.listenForAnalysis(analysis);
        }
    }
}
