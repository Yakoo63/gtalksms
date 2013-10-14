package com.googlecode.gtalksms.panels;

import java.io.File;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;

public class Video extends Activity implements SurfaceHolder.Callback {

    public final static String VIDEO_START = "com.googlecode.gtalksms.action.VIDEO_START";
    public final static String VIDEO_STOP = "com.googlecode.gtalksms.action.VIDEO_STOP";

    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private AudioManager mAudioManager;
    private int mStreamVolume;
    private boolean mIsRecording;

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Log.d("Video::onHandleIntent " + action);
        if (action.equals(VIDEO_STOP)) {
            finish();
        } else {
            Log.i("Video::already recording");
            Tools.send("Video recording already started", null, getBaseContext());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("Video::onCreate. Intent=" + getIntent());
        if (getIntent() == null || !getIntent().getAction().equals(VIDEO_START)) {
            finish();
            return;
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.camera_surface);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void onDestroy() {
        stopVideo();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startVideo();
    }

    private void startVideo() {
        try {
            Log.i("Video::startVideo");

            mCamera = Camera.open();
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                }

                mCamera.setParameters(parameters);

                // Start recording and hide the window
                startMediaRecorder();
                setVisible(false);
            } else {
                finish();
            }
        } catch (Exception e) {
            Log.e("Video::startVideo", e);
            finish();
        }
    }

    private void stopVideo() {
        Log.i("Video::stopVideo");
        if (mIsRecording) {
            stopMediaRecorder();
        }

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        Tools.send("Video recording stopped", null, getBaseContext());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Do nothing
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Do nothing
    }


    public boolean startMediaRecorder() {
        try {
            mIsRecording = true;

            mMediaRecorder = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            // Step 2: Set sources
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//                sMediaRecorder.setProfile(_cameraSettings.getCamcorderProfileApi11());
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));

            } else {
//                sMediaRecorder.setProfile(_cameraSettings.getCamcorderProfileApi8());
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
            }

            // Step 4: Set output file
            mMediaRecorder.setOutputFile(getDestinationFile().getAbsolutePath());

            // Step 5: Set the preview output
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            // To update ?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
//                mMediaRecorder.setOrientationHint(_cameraSettings.RotationInDegree);
                mMediaRecorder.setOrientationHint(90);
            } else {
                // TODO find a way to change the orientation
            }

//            if (_cameraSettings.MaxDurationInMs > 0) {
//                mMediaRecorder.setMaxDuration(_cameraSettings.MaxDurationInMs);
//            }
//
//            if (_cameraSettings.MaxFileSizeInBytes > 0) {
//                mMediaRecorder.setMaxFileSize(_cameraSettings.MaxFileSizeInBytes);
//            }

            mMediaRecorder.setOnInfoListener(new OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.i("Max duration reached");
                        finish();
                    } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        Log.i("Max file size reached");
                        finish();
                    }
                }
            });

            mMediaRecorder.setOnErrorListener(new OnErrorListener() {
                @Override
                public void onError(MediaRecorder mr, int what, int extra) {
                    Log.e("Error while recording. Error=" + what + ", Param=" + extra);
                    finish();
                }
            });

            mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);

            // Step 6: Prepare configured MediaRecorder
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, mStreamVolume, 0);

            Tools.send("Video recording started", null, getBaseContext());

            return true;
        } catch (Exception e) {
            mIsRecording = false;
            Log.e("Video::startMediaRecorder", e);
            finish();
            return false;
        }
    }

    public void stopMediaRecorder() {
        if (mIsRecording) {
            try {
                mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
                mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
                mMediaRecorder.stop();
            } catch (Exception e) {
                Log.e("Video::stopMediaRecorder", e);
            }

            try {
                mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, mStreamVolume, 0);
                if (mMediaRecorder != null) {
                    mMediaRecorder.reset(); // clear recorder configuration
                    mMediaRecorder.release(); // release the recorder object
                    mMediaRecorder = null;
                }
            } catch (Exception e) {
                Log.e("Video::stopMediaRecorder", e);
            }
        }
    }



    public File getDestinationFile() {
        File repository = new File(new File(Environment.getExternalStorageDirectory(), "DCIM"), "GTalkSMS");
        if (!repository.exists()) {
            repository.mkdirs();
        }
        return new File(repository, Tools.getFileFormat(GregorianCalendar.getInstance()) + ".mp4");
    }
}