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


public class RingCmd extends CommandHandlerBase {
    private final static long[] VIB_PATTERN = {0, 1000, 100};
    
    private static AudioManager sAudioManager;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private boolean mCanRing;
   
    public RingCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, new Cmd("ring"), new Cmd("ringmode"));
        sAudioManager = (AudioManager) mainService.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) mainService.getSystemService(Context.VIBRATOR_SERVICE);
    }
    
    @Override
    protected void execute(String cmd, String args) {
        if (isMatchingCmd("ring", cmd)) {
            if (args.equals("stop")) {
                send(R.string.chat_stop_ringing);
                stop();
            } else if (ring(Tools.parseInt(args, 100))) {
                send(R.string.chat_start_ringing);
            } else {
                send(R.string.chat_error_ringing);
            }
        } else if (isMatchingCmd("ringmode", cmd)) {
            int mode;
            if (args.equals("vibrate")) {
                sAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            } else if (args.equals("normal")) {
                sAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } else if (args.equals("silent")) {
                sAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else if (!args.equals("")) {
                send(R.string.chat_ringer_error_cmd, args);
                return;
            }
            mode = sAudioManager.getRingerMode();
            switch (mode) {
            case AudioManager.RINGER_MODE_VIBRATE:
                send(R.string.chat_ringer_vibrate);
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                send(R.string.chat_ringer_normal);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                send(R.string.chat_ringer_silent);
                break;

            }
        }
    }
    
    /**
     * Makes the phone ring
     * 
     * @param volume from [0-100] where 0 means vibration mode
     * @return true is phone is able to ring, otherwise false
     */
    private boolean ring(float volume) {
        boolean res = false;
        if (volume > 0) {
            clearMediaPlayer();
            initMediaPlayer();
            
            final AudioManager audioManager = (AudioManager) sContext.getSystemService(Context.AUDIO_SERVICE);
            if (mCanRing && audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                
                try {
                    mMediaPlayer.prepare();
                } catch (Exception e) {
                    mCanRing = false;
                }
                
                mMediaPlayer.setVolume(volume / 100, volume / 100);
                if (!mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }                         
                res = true;
            } 
        } else {
            mVibrator.vibrate(VIB_PATTERN, 0);
            res = true;
        }
        return res;
    }

    /** init the media player */
    private void initMediaPlayer() {
        mCanRing = true;
        Uri alert = Uri.parse(sSettingsMgr.ringtone);
        // if URI is empty string user has set ringtone to "no sound"/"silent"
        if (alert.toString().equals("")) { 
            mCanRing = false;
            return;
        }
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(sContext, alert);
        } catch (IOException ioe) {
            try {
                Log.w(Tools.LOG_TAG, "Could not set choosen ringtone, falling back to system default ringtone");
                mMediaPlayer.setDataSource(sContext, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)); //the emulator wont find the default ringtone as he has none
            } catch (Exception e) {
                mCanRing = false;
            }
        } catch (Exception e) {
            mCanRing = false;
        }
        if (mCanRing) { 
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.setLooping(true);
        } else {
            mMediaPlayer = null;
        }
    }
   
    /** clears the media player */
    private void clearMediaPlayer() {
        if (mMediaPlayer != null) {
            // stop will throw an IllegalStateEx when called but not initialized
            // we have the mCanRing bool to signal an successful initialization
            if (mCanRing) mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
    }
    
    @Override
    public void stop() {
        clearMediaPlayer();        
        mVibrator.cancel();
    }
    
    @Override
    public void cleanUp() {
        clearMediaPlayer();
    }

    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("ring").setHelp(R.string.chat_help_ring, "|[0-100]|stop");   
        mCommandMap.get("ringmode").setHelp(R.string.chat_help_ringmode, "#mode#");   
    }
}
