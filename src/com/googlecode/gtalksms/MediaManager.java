package com.googlecode.gtalksms;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

public class MediaManager {
    private MediaPlayer _mediaPlayer;
    private boolean _canRing;
    
    Context _context;
    SettingsManager _settings;
    
    public MediaManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
    }

    /** clears the media player */
    public void clearMediaPlayer() {
        if (_mediaPlayer != null) {
            _mediaPlayer.stop();
        }
        _mediaPlayer = null;
    }

    /** init the media player */
    public void initMediaPlayer() {
        _canRing = true;
        Uri alert = Uri.parse(_settings.ringtone);
        _mediaPlayer = new MediaPlayer();
        try {
            _mediaPlayer.setDataSource(_context, alert);
        } catch (Exception e) {
            _canRing = false;
        }
        _mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        _mediaPlayer.setLooping(true);
    }
    
    /** makes the phone ring */
    public boolean ring() {
        boolean res = false;
        final AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (_canRing && audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            try {
                _mediaPlayer.prepare();
            } catch (Exception e) {
                _canRing = false;
            }
            _mediaPlayer.start();
            
            res = true;
        }
        return res;
    }

    /** Stops the phone from ringing */
    public void stopRinging() {
        if (_canRing) {
            _mediaPlayer.stop();
        }
    }
}