package com.googlecode.gtalksms;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

public class MediaManager {
    private MediaPlayer mMediaPlayer;
    private boolean canRing;
    
    Context _context;
    SettingsManager _settings;
    
    public MediaManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
    }

    /** clears the media player */
    public void clearMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        mMediaPlayer = null;
    }

    /** init the media player */
    public void initMediaPlayer() {
        canRing = true;
        Uri alert = Uri.parse(_settings.ringtone);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(_context, alert);
        } catch (Exception e) {
            canRing = false;
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setLooping(true);
    }
    
    /** makes the phone ring */
    public boolean ring() {
        boolean res = false;
        final AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (canRing && audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            try {
                mMediaPlayer.prepare();
            } catch (Exception e) {
                canRing = false;
            }
            mMediaPlayer.start();
            
            res = true;
        }
        return res;
    }

    /** Stops the phone from ringing */
    public void stopRinging() {
        if (canRing) {
            mMediaPlayer.stop();
        }
    }

}