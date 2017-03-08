package com.practicecactus.practicecactus.AudioAnalysis;

/**
 * Interface describing a Singleton AudioAnalyzer object.
 */
public interface AudioAnalyzer {

    AudioAnalysis analyze(short[] sound, int sampleRate, int bufferSize, int sameLength);

}
