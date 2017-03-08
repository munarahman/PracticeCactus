package com.practicecactus.practicecactus.AudioAnalysis.impl;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysisPublisher;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalyzer;

/**
 * Created by matthew on 2016-02-22.
 */
public class AudioAnalysisTask extends AsyncTask<Void, AudioAnalysis, Void> {

    private Context context;
    private AudioAnalyzer analyzer;
    private AudioAnalysisPublisher publisher;

    static final int SAMPLE_LENGTH = 125;
    private int SAMPLE_LENGTH_SHORTS;
    private int bufferSize;

    private AudioRecord recorder;

    private static int[] mSampleRates = new int[] { 8000, 11025, 16000, 22050, 44100 };

    public AudioAnalysisTask(AudioAnalysisPublisher publisher, Context context){
        this.analyzer = DefaultAudioAnalyzer.getInstance();
        this.publisher = publisher;
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {

        this.recorder = findAudioRecord();
        this.recorder.startRecording();
        this.SAMPLE_LENGTH_SHORTS = (int)((this.SAMPLE_LENGTH*recorder.getSampleRate())/1000);


        short[] audioData = new short[this.SAMPLE_LENGTH_SHORTS];
        short[] discardAudioData = new short[this.bufferSize - this.SAMPLE_LENGTH_SHORTS];
        try {
            while(true) {
                int discardAudioSize = this.recorder.read(discardAudioData, 0, this.bufferSize - this.SAMPLE_LENGTH_SHORTS);
                int recordSize = this.recorder.read(audioData, 0, this.SAMPLE_LENGTH_SHORTS);
                this.publishProgress(this.analyzer.analyze(audioData, recorder.getSampleRate(), this.SAMPLE_LENGTH_SHORTS, this.SAMPLE_LENGTH));
                Thread.sleep(this.SAMPLE_LENGTH);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(AudioAnalysis... values) {
        super.onProgressUpdate(values);
        this.publisher.distribute(values[0]);
    }


    public void releaseRecord(){
        System.out.println("releasing record");
        this.recorder.release();
    }

    /**
     * Cycles through settings to find a working set
     * @return AudioRecord
     */
    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            try {
                this.bufferSize = Math.max(((rate*SAMPLE_LENGTH)/1000), AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT));
                if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                    // check if we can instantiate and have a success
                    AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, this.bufferSize);
                    if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                        return recorder;
                }
            } catch (Exception e) {
                System.out.println("Exception - keep trying");
            }
        }
        return null;
    }
}
