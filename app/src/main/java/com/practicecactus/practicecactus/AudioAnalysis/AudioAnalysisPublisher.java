package com.practicecactus.practicecactus.AudioAnalysis;

/**
 * Created by matthew on 2016-02-26.
 */
public interface AudioAnalysisPublisher {

    /**
     * Pauses the publisher. This is useful if there is some other class that want to use
     * the AudioRecord object. Remember to resume after.
     * @return True iff successful.
     */
    boolean pause();

    /**
     * Resumes publisher after a pause.
     * @return True iff successful.
     */
    boolean resume();

    void register(AudioAnalysisListener listener);

    boolean unregister(AudioAnalysisListener listener);

    void distribute(AudioAnalysis analysis);
}
