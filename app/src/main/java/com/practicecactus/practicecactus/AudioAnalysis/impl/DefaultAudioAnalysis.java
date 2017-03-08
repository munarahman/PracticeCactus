package com.practicecactus.practicecactus.AudioAnalysis.impl;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by matthew on 2016-02-26.
 */
public class DefaultAudioAnalysis implements AudioAnalysis {
    private boolean isPiano;
    private float[] rawsound;
    private float[] powerSpectrum;
    private float[] FFTSound;
    private Date analysisTime;
    private ArrayList<Integer> dominantPianoKeys;


    public DefaultAudioAnalysis(boolean isPiano, float[] rawsound, float[] powerSpectrum, ArrayList<Integer> dominantPianoKeys, float[] FFTSound){
        this.isPiano = isPiano;
        this.rawsound = rawsound;
        this.powerSpectrum = powerSpectrum;
        this.analysisTime = new Date();
        this.FFTSound = FFTSound;
        this.dominantPianoKeys = dominantPianoKeys;
    }
    @Override
    public boolean isPiano() {
        return isPiano;
    }

    @Override
    public float[] getRawSound() {
        return this.rawsound;
    }

    @Override
    public float[] getPowerSpectrum(){return this.powerSpectrum;}

    @Override
    public Date getTime(){return this.analysisTime;}

    @Override
    public ArrayList<Integer> getDominantPianoKeys(){return this.dominantPianoKeys;}

    @Override
    public float[] getFFTSound(){
        return this.FFTSound;
    }

}
