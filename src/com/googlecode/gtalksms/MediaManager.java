package com.googlecode.gtalksms;

import java.io.IOException;

import com.googlecode.gtalksms.tools.Tools;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

public class MediaManager {
    private MediaPlayer _mediaPlayer;
    private Vibrator _vibrator;
    private boolean _canRing;
    private long[] _pattern = {0, 1000, 100};
    
    Context _context;
    SettingsManager _settings;
    
    public MediaManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
        _vibrator = (Vibrator) _context.getSystemService(Context.VIBRATOR_SERVICE);
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
        } catch (IOException ioe) {
        	try {
        		Log.w(Tools.LOG_TAG, "Could not set choosen ringtone, falling back to system default ringtone");
				_mediaPlayer.setDataSource(_context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)); //the emulator wont find the default ringtone as he has none
			} catch (Exception e) {
				_canRing = false;
			}
        } catch (Exception e) {
            _canRing = false;
        }
        _mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        _mediaPlayer.setLooping(true);
    }
    
    /** makes the phone ring */
    public boolean ring(float volume) {
        boolean res = false;
        if (volume > 0) {
            clearMediaPlayer();
            initMediaPlayer();
            
            final AudioManager audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
            if (_canRing && audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                
                try {
                    _mediaPlayer.prepare();
                } catch (Exception e) {
                    _canRing = false;
                }
                
                _mediaPlayer.setVolume(volume / 100, volume / 100);
                if (!_mediaPlayer.isPlaying()) {
                    _mediaPlayer.start();
                }
                
                res = true;
            }
        } else {
            _vibrator.vibrate(_pattern, 0);
            res = true;
        }
        return res;
    }

    /** Stops the phone from ringing */
    public void stopRinging() {
        if (_canRing) {
            _mediaPlayer.stop();
        }
        _vibrator.cancel();
    }
}