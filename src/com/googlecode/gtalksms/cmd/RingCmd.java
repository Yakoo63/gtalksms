package com.googlecode.gtalksms.cmd;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;


public class RingCmd extends Command {
    private static AudioManager _audioManager;
    private MediaPlayer _mediaPlayer;
    private Vibrator _vibrator;
    private boolean _canRing;
    private long[] _pattern = {0, 1000, 100};
   
    public RingCmd(MainService mainService) {
        super(mainService);
        _audioManager = (AudioManager) mainService.getSystemService(Context.AUDIO_SERVICE);
        _vibrator = (Vibrator) mainService.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public void execute(String cmd, String args) {
        if (cmd.equals("ring")) {
            Integer volume = Tools.parseInt(args);
            
            if (volume == null) {
                volume = 100;
            }
            
            send(getString(R.string.chat_start_ringing));
            if (!ring(volume)) {
                send(getString(R.string.chat_error_ringing));
            }
        } else {
            int mode;
            if (args.equals("vibrate")) {
                _audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            } else if (args.equals("normal")) {
                _audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } else if (args.equals("silent")) {
                _audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else if (!args.equals("")) {
                send(getString(R.string.chat_ringer_error_cmd, args));
                return;
            }
            mode = _audioManager.getRingerMode();
            switch (mode) {
            case AudioManager.RINGER_MODE_VIBRATE:
                send(getString(R.string.chat_ringer_vibrate));
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                send(getString(R.string.chat_ringer_normal));
                break;
            case AudioManager.RINGER_MODE_SILENT:
                send(getString(R.string.chat_ringer_silent));
                break;

            }
        }
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

    /** init the media player */
    public void initMediaPlayer() {
        _canRing = true;
        Uri alert = Uri.parse(_settingsMgr.ringtone);
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
   
    /** clears the media player */
    private void clearMediaPlayer() {
        if (_mediaPlayer != null) {
            _mediaPlayer.stop();
        }
        _mediaPlayer = null;
    }
    
    public void stop() {
        if (_canRing) {
            _mediaPlayer.stop();
        }
        _vibrator.cancel();
    }
    
    public void cleanUp() {
        clearMediaPlayer();
    }

}
