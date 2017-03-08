package com.practicecactus.practicecactus.AudioAnalysis.impl;

/**
 * Created by matthew on 2016-02-28.
 */
public class TemporalSmoother {

    Double runningAverage;
    boolean prediction;

    private final double SMOOTHING = 10;
    private final double UNCERTAINTY_RANGE = 0.2;

    public boolean smooth(boolean signal){
        double newValue = (signal) ? 1 : 0;
        if (runningAverage != null) {
            runningAverage += (newValue - runningAverage)/SMOOTHING;
        } else {
            runningAverage = newValue;
        }
        if (runningAverage > (0.5 + UNCERTAINTY_RANGE/2) || runningAverage < (0.5 - UNCERTAINTY_RANGE/2)) {
            prediction = runningAverage > 0.5;
        }
        return prediction;
    }
}
