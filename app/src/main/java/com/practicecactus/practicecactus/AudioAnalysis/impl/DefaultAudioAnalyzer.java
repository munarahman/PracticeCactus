package com.practicecactus.practicecactus.AudioAnalysis.impl;

import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalysis;
import com.practicecactus.practicecactus.AudioAnalysis.AudioAnalyzer;

import org.jtransforms.fft.FloatFFT_1D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Created by matthew on 2016-02-24.
 */
public class DefaultAudioAnalyzer implements AudioAnalyzer {
    private static AudioAnalyzer instance;
    private int sampleRate;
    private int sampleLength;
    private int bufferSize;

    private final float piano_heuristic_threshold = 3;
    private final float dominant_key_heuristic_threshold = 5;


    private static final double[] PIANO_KEYS = {4186.01,3951.07,3729.31,3520.00,3322.44,3135.96,
            2959.96,2793.83,2637.02,2489.02,2349.32,2217.46,2093.00,1975.53,1864.66,1760.00,1661.22,
            1567.98,1479.98,1396.91,1318.51,1244.51,1174.66,1108.73,1046.50,987.767,932.328,880.000,
            830.609,783.991,739.989,698.456,659.255,622.254,587.330,554.365,523.251,493.883,466.164,
            440.000,415.305,391.995,369.994,349.228,329.628,311.127,293.665,277.183,261.626,246.942,
            233.082,220.000,207.652,195.998,184.997,174.614,164.814,155.563,146.832,138.591,130.813,
            123.471,116.541,110.000,103.826,97.9989,92.4986,87.3071,82.4069,77.7817,73.4162,69.2957,
            65.4064,61.7354,58.2705,55.0000,51.9131,48.9994,46.2493,43.6535,41.2034,38.8909,36.7081,
            34.6478,32.7032,30.8677,29.1352,27.5000};

    private static final String[] PIANO_KEYS_SYM = {"C8","B7","A#7","A7","G#7","G7","F#7","F7","E7","D#7",
            "D7","C#7","C7","B6","A#6","A6","G#6","G6","F#6","F6","E6","D#6","D6","C#6","C6","B5","A#5",
            "A5","G#5","G5","F#5", "F5", "E5", "D#5", "D5", "C#5", "C5","B4","A#4","A4","G#4","G4","F#4",
            "F4","E4","D#4","D4","C#4","C4","B3","A#3","A3","G#3","G3","F#3","F3","E3","D#3","D3","C#3",
            "C3","B2","A#2","A2","G#2","G2","F#2","F2","E2","D#2","D2","C#2","C2","B1","A#1","A1","G#1",
            "G1","F#1","F1","E1","D#1","D1","C#1","C1","B0","A#0","A0"};

    private static final float BP_WIDTH = 2;

    private DefaultAudioAnalyzer(){}

    public static AudioAnalyzer getInstance() {
        if(instance == null) {
            instance = new DefaultAudioAnalyzer();
        }
        return instance;
    }

    @Override
    public AudioAnalysis analyze(short[] sound, int sampleRate, int bufferSize, int sampleLength) {
        //set parameters based on arguments
        this.sampleRate = sampleRate;
        this.sampleLength = sampleLength;
        this.bufferSize = bufferSize;

        //First create an AudioEvent
        float[] values = this.shortArr2FloatArr(sound);
        float[] rawSound = new float[values.length];

        System.arraycopy(values, 0, rawSound, 0, values.length);

        //now to convert to FFT
        FloatFFT_1D fft = new FloatFFT_1D(values.length);

        fft.realForward(values);

        float[] powerSpectrum = this.fft2power(values);

        float frequency = this.getIndexOfMax(powerSpectrum) * this.sampleRate/sound.length;
        float[] soundVector = this.getSoundVector(powerSpectrum);

        ArrayList<Integer> dominantKeyIndices = this.getDominantKeyIndices(powerSpectrum);
        ArrayList<String> dominantKeysArray = this.keysIndices2Syms(dominantKeyIndices);


        AudioAnalysis analysis = new DefaultAudioAnalysis(this.applyHeuristic(soundVector), rawSound, powerSpectrum, dominantKeyIndices, values);

        return analysis;
    }

    private ArrayList<Integer> getDominantKeyIndices(float[] powerSpectrum){
        ArrayList<Integer> dominantKeyIndices = new ArrayList<Integer>();

        //calculate mean and standard deviation of powerSpectrum
        float sum = 0;
        float sumSquares = 0;

        for (int i = 0; i<powerSpectrum.length; i++){
            sum += powerSpectrum[i];
            sumSquares += (float)Math.pow((double)powerSpectrum[i], (double)2);
        }

        float mean = sum/powerSpectrum.length;
        float stdev = (float)Math.sqrt(sumSquares/powerSpectrum.length - Math.pow((double)mean,2));

        //add the most dominant piano key
        float[] soundVector = this.getSoundVector(powerSpectrum);

        //add dominant piano keys that are over a heuristic threshold
        for (int i=0; i<soundVector.length; i++) {
            if (soundVector[i] - mean > dominant_key_heuristic_threshold * stdev) {
                dominantKeyIndices.add(i);
            }
        }

        //if no notes were added, add the most dominant note:
        if (dominantKeyIndices.size()<1){
            int most_dominant_key_idx = this.getIndexOfMax(soundVector);
            dominantKeyIndices.add(most_dominant_key_idx);
        }

        return dominantKeyIndices;
    }

    private ArrayList<String> keysIndices2Syms(ArrayList<Integer> dominantKeyIndices){

        ArrayList<String> dominantKeysArray = new ArrayList<String>();

        if (dominantKeyIndices.size()>0) {
            for (int i = 0; i < dominantKeyIndices.size(); i++) {
                dominantKeysArray.add(this.PIANO_KEYS_SYM[dominantKeyIndices.get(i)]);
            }
        }

        return dominantKeysArray;

    }

    private boolean applyHeuristic(float[] soundVector) {
        float average = 0;
        for (float feature:soundVector){
            average = average + feature/soundVector.length;
        }
        double averageDiff = 0;
        //Spikiness will be measured by the mean square difference in heights between peaks
        for (int i = 1; i< soundVector.length; i++) {
            double diff = soundVector[i] - soundVector[i-1];
            diff = diff/average;
            diff = Math.pow(diff, 2);
            averageDiff += diff/(soundVector.length-1);
        }


        //Log.i("Average square diff:", Double.toString(averageDiff));

        return averageDiff > piano_heuristic_threshold;
    }

    private float[] getSoundVector(float[] powerSpectrum) {
        float[] vector = new float[this.PIANO_KEYS.length];
        for(int i = 0; i < this.PIANO_KEYS.length; i++) {
            double key = this.PIANO_KEYS[i];
            //We have to determine which bin this frequency corresponds to
            int bin = (int) Math.round(key * (double) this.bufferSize / this.sampleRate);
            if (bin > powerSpectrum.length){
                //Unfortunately any key above this is above the nyqvist freq. >4000hz
                //This should only be one key at most though
                bin = powerSpectrum.length-1;
            }
            vector[i] = powerSpectrum[bin];
        }
        return vector;
    }

    private int getIndexOfMax(float[] powerSpectrum) {
        int maxIndex = -1;
        float maxValue = -1;
        for (int i = 0; i < powerSpectrum.length; i++) {
            if (powerSpectrum[i] > maxValue){
                maxValue = powerSpectrum[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * Computes the power spectrum given a fourier transform. Power is defined by:
     *                      sqrt(real^2 + imaginary^2)
     * @param values the fourier transform in the format described by {@link FloatFFT_1D}
     * @return
     */
    private float[] fft2power(float[] values) {
        float[] result = new float[values.length/2];
        for (int i = 0; i <result.length; i++){
            result[i] = (float) Math.sqrt(Math.pow((double) values[2*i],2) + (double) Math.pow(values[2*i + 1],2));
        }
        return result;
    }

    public static float[] shortArr2FloatArr(short[] values){
        float[] result = new float[values.length];
        for (int i = 0; i<result.length; i++){
            result[i] = (float) values[i];
        }
        return result;
    }
    public static float[] byteArr2FloatArr(byte [] bytes, int bytesPerSample, ByteOrder byteOrder){
        int size = bytes.length/bytesPerSample;
        float[] result = new float[size];
        for (int i = 0; i<bytes.length; i+=bytesPerSample){
            byte[] indSample = new byte[bytesPerSample];
            int counter = 0;
            while(counter<bytesPerSample){
                indSample[counter] = bytes[i+counter];
            }
            float value = ByteBuffer.wrap(indSample).order(byteOrder).asFloatBuffer().get();
            result[i/bytesPerSample] = value;
        }
        return result;
    }
}
