package com.practicecactus.practicecactus.AudioAnalysis;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by matthew on 2016-02-22.
 */
public interface AudioAnalysis {
    boolean isPiano();
    float[] getRawSound();
    float[] getPowerSpectrum();
    float[] getFFTSound();
    Date getTime();
    ArrayList<Integer> getDominantPianoKeys();
}
