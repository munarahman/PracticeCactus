package com.practicecactus.practicecactus.Activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.practicecactus.practicecactus.AnalyticsApplication;
import com.practicecactus.practicecactus.AudioAnalysis.impl.DefaultAudioAnalysisPublisher;
import com.practicecactus.practicecactus.Cacheable.impl.AudioRecording;
import com.practicecactus.practicecactus.OfflineManager;
import com.practicecactus.practicecactus.R;

import java.io.File;
import java.io.IOException;

public class ShareActivity extends AppCompatActivity implements View.OnClickListener, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    /*
    PLAN:
    1. Record data using AudioRecord
    2. Save data to pcm file in internal storage
    3. When the send button is clicked, (must be after pause button is clicked), start an async task
    4. In the async task, open HttpUrlConnection and send pcm file as a JSON array along with filename via post to localhost:3000/upload
    5. On node, convert the JSON array (pcm data) to wav file using npm package pcmjs
    6. (?) Save the data to the database (maybe just the filename?), along with time info and student info
    7. Save the wav file (using fs)
    8. When teacher app requests, use load from wavesurfer.js
    9. Use wavesurfer.js to visualize

    -Send a file stream directly to server as it is recording

     */
    private static String mFileName = null;
    private static File mDirName;

    private String description;
    private String toTeacher = "false";
    private String toCommunity = "false";

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // init the media variables
    private MediaPlayer mPlayer = null;
    private MediaRecorder mRecorder = null;
    private Handler mHandler = null;
    private boolean mStartRecording = true;
    private boolean mStartPlaying = true;
    private SeekBar mSeekBar;

    private TextView record_activity_header_text;

    // init the main Buttons on the Share Page
    private Button shareButton;
    private Button playbackButton;
    private Button recordButton;

    private String username = null;

    OfflineManager manager;

    private AnalyticsApplication analytics;
    private String eventCategory = this.getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        analytics = (AnalyticsApplication) getApplication();
        analytics.getDefaultTracker();

        // get an offline manager
        manager = OfflineManager.getInstance(this);

        // get the buttons on share Page
        shareButton = (Button) findViewById(R.id.button_share_recording);
        playbackButton = (Button) findViewById(R.id.button_play_recording);
        recordButton = (Button) findViewById(R.id.button_record_stop);

        shareButton.setOnClickListener(this);
        playbackButton.setOnClickListener(this);
        recordButton.setOnClickListener(this);

        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mSeekBar.setEnabled(false);

        this.username = getPref("username");
        configureAudioFile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        analytics.trackScreen(this.getClass().getSimpleName());
    }

    @Override
    public void onClick(View v) {

        // call the appropriate functions when certain buttons are pressed

        switch (v.getId()) {
            case R.id.button_record_stop:
                // start recording
                onRecord(mStartRecording);

                // toggle startRecording
                mStartRecording = !mStartRecording;
                break;

            case R.id.button_play_recording:
                // start playing playback
                onPlay(mStartPlaying);

                // toggle startPlaying
                mStartPlaying = !mStartPlaying;
                break;

            case R.id.button_share_recording:
                //check if we have recording and we are not currently recording
                if (mStartRecording && mStartPlaying) {
                    writeDescription(v);
                }
                break;

            default:
                break;
        }
    }

    private void onRecord(boolean start) {
        record_activity_header_text = (TextView) findViewById(R.id.share_text_view);

        if (start) {
            // if not currently recording, start recording
            startRecording();

            record_activity_header_text.setText("Record and Share: Recording ...");
            Drawable img = getApplicationContext().getResources().getDrawable(
                    R.drawable.button_record_stop_default);
            recordButton.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
            recordButton.setText("Stop");

            // disable the record button while it is recording
            temporarilyDisableRecordButton(recordButton);
            actionButtonsEnabled(false);

        } else {
            // if currently recording stop recording
            stopRecording();

            record_activity_header_text.setText("Record and Share: Ready to share.");
            Drawable img = getApplicationContext().getResources().getDrawable(
                    R.drawable.button_record_default);
            recordButton.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
            recordButton.setText("Re-record");

            temporarilyDisableRecordButton(recordButton);
            actionButtonsEnabled(true);
        }

        mSeekBar.setProgress(0);
    }

    private void startRecording() {
        analytics.trackEvent(eventCategory, "StartRecording");

        // release media player if one exists
        if (mPlayer != null) {
            mPlayer = null;
        }

        // create new file
        Long now = System.currentTimeMillis();
        mFileName = mDirName.getPath() + File.separator + now + ".3pg";
        File mediaFile = new File(mFileName);

        // set up the media player
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mediaFile.toString());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioEncodingBitRate(160 * 1024);
        mRecorder.setAudioChannels(1);

        mSeekBar.setEnabled(false);

        // start recording
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording() {
        analytics.trackEvent(eventCategory, "StopRecording");

        try {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            System.out.println("stopped recording");
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSeekBar.setEnabled(true);
    }

    private void temporarilyDisableRecordButton(View bttn) {
        bttn.setEnabled(false);
        bttn.postDelayed(new Runnable() {
            @Override
            public void run() {
                recordButton.setEnabled(true);
            }
        }, 1000);
    }

    private void actionButtonsEnabled(boolean enabled) {
        playbackButton.setEnabled(enabled);
        shareButton.setEnabled(enabled);
    }

    private void nonPlayButtonsEnabled(boolean enabled) {
        recordButton.setEnabled(enabled);
        shareButton.setEnabled(enabled);
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        analytics.trackEvent(eventCategory, "StartPlaying");

        // if no media player exists then create one
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mPlayer) {
                    seekBarConnect();
                }
            });

            // when a recording has finished playing
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mPlayer) {
                    mStartPlaying = true;
                    mPlayer.seekTo(0);
                    showPlayButton();
                    nonPlayButtonsEnabled(true);
                }
            });

            try {
                mPlayer.setDataSource(mFileName);
                mPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        mPlayer.seekTo(mSeekBar.getProgress() * 100);
        mPlayer.start();

        // once the play button is pressed, show the pause button
        nonPlayButtonsEnabled(false);
        playbackButton.setText("Pause");
        playbackButton.setCompoundDrawablesWithIntrinsicBounds(null, null,
                ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause), null);
    }

    private void stopPlaying() {
        analytics.trackEvent(eventCategory, "StopPlaying");

        mPlayer.stop();
        mPlayer.reset();
        mPlayer.release();
        mPlayer = null;

        showPlayButton();
        nonPlayButtonsEnabled(true);
    }

    private void showPlayButton() {
        playbackButton.setText("Play");
        playbackButton.setCompoundDrawablesWithIntrinsicBounds(null, null,
                ContextCompat.getDrawable(this, android.R.drawable.ic_media_play), null);
    }

    private void seekBarConnect() {
        mSeekBar.setMax(mPlayer.getDuration() / 100);

        // update seek bar to match media player's progress
        ShareActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null) {
                    int mCurrentPosition = mPlayer.getCurrentPosition() / 100;
                    mSeekBar.setProgress(mCurrentPosition);
                }
                mHandler = new Handler();
                mHandler.postDelayed(this, 100);
            }
        });

        // update media player's position to match where user drags seek bar
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlayer != null && fromUser) {
                    mPlayer.seekTo(progress * 100);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void writeDescription(View view) {
        // Setting Dialog Message
        final EditText input = new EditText(ShareActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        AlertDialog.Builder descriptionAlertDialog = new AlertDialog.Builder(ShareActivity.this);
        descriptionAlertDialog.setView(input)

                // Setting Dialog Title
                .setTitle("Title")

                // Setting Positive "Yes" Button
                .setPositiveButton("Next",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                description = input.getText().toString();
                                chooseReceivers();

                            }
                        })

                // Setting Negative "NO" Button
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                dialog.cancel();
                            }
                        })

                .show();
    }

    public void chooseReceivers() {
        AlertDialog.Builder chooseReceiverAlertDialog = new AlertDialog.Builder(ShareActivity.this);



        chooseReceiverAlertDialog.setMultiChoiceItems(R.array.share_array, null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                        /*
                            IF the teacher box was selected OR the teacher box was already selected
                           but the user selected/deselected the community box THEN set the toTeacher
                           boolean to true, else false.

                           IF the community box was selected OR the community box was already selected
                           but the user selected/deselected the teacher box THEN set the toCommunity
                           boolean to true.

                        */
                        toTeacher = ((which == 0 && isChecked) ||
                                    (toTeacher == "true" && which != 0)) ? "true" : "false";
                        toCommunity = ((which == 1 && isChecked) ||
                                    (toCommunity == "true" && which != 1)) ? "true" : "false";

                        /*
                            IF one of/both the teacher or community checkboxes are checked then
                            enable the share button, ELSE disable it (i.e neither selection is
                            chosen).

                         */

                        if ((toTeacher == "true" && toCommunity == "true") ||
                                (toTeacher == "true" && toCommunity == "false") ||
                                (toTeacher == "false" && toCommunity == "true")) {

                            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                        else {
                            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }


                    }
                })

                .setTitle("Please choose one or more destinations.")

                .setPositiveButton("Share",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                // Once you click the share button, send the info
                                share();

                            }
                        })

                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                dialog.cancel();
                                System.out.println("in here");
                            }
                        });

//                .show();

        final AlertDialog newDialog = chooseReceiverAlertDialog.create();
        newDialog.show();
        newDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);




    }

    private void share() {
        analytics.trackEvent(eventCategory, "SendRecording");

        AudioRecording recording = new AudioRecording(
                this, mFileName, description, toTeacher, toCommunity);
        manager.sendFileAttempt(recording);
        DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).resume();
        finish();
    }

    private void configureAudioFile() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // ask for permission for use of external storage
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        // create a new directory in the external storage to store the recording
        mDirName = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "PracticeCactus");

        if (!mDirName.exists()) {
            if (!mDirName.mkdirs()) {
                Log.d("PracticeCactus", "failed to create directory");
            }
        }
    }

    private String getPref(String preference) {

        // get the preference passed in from sharedPrefs

        SharedPreferences sharedPref = this.getSharedPreferences(
                this.getString(R.string.shared_preferences_user),
                Context.MODE_PRIVATE);
        return sharedPref.getString(preference, null);
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Toast.makeText(this, "Recording error", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {

        // create an error message if the recording is too long, or has exceeded the max file size
        String msg = null;
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                msg = "max duration reached";
                break;

            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                msg = "max filesize reached";
                break;
        }

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        DefaultAudioAnalysisPublisher.getInstance(getApplicationContext()).resume();
        // Otherwise defer to system default behavior.

        //stop and release mediaplayer
        super.onBackPressed();
    }
}
