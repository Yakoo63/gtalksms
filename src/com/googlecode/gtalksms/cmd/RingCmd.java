package com.googlecode.gtalksms.cmd;

import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;


public class RingCmd extends CommandHandlerBase {
    private final static long[] VIB_PATTERN = {0, 1000, 100};
    
    private static AudioManager sAudioManager;
    private MediaPlayer mMediaPlayer;
    private Vibrator mVibrator;
    private boolean mCanRing;
   
    public RingCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, "Ring", new Cmd("ring"), new Cmd("ringmode"));
    }

    @Override
    protected void onCommandActivated() {
        sAudioManager = (AudioManager) sMainService.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) sMainService.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onCommandDeactivated() {
        stop();
        sAudioManager = null;
        mVibrator = null;
    }

    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "ring")) {
            if (cmd.getArg1().equals("stop")) {
                send(R.string.chat_stop_ringing);
                stop();
            } else if (ring(Tools.parseInt(cmd.getArg1(), 100))) {
                send(R.string.chat_start_ringing);
            } else {
                send(R.string.chat_error_ringing);
            }
        } else if (isMatchingCmd(cmd, "ringmode")) {
            int mode;
            String arg1 = cmd.getArg1();
            if (arg1.equals("vibrate")) {
                sAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            } else if (arg1.equals("normal")) {
                sAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            } else if (arg1.equals("silent")) {
                sAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            } else if (!arg1.equals("")) {
                send(R.string.chat_ringer_error_cmd, arg1);
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
        
        if (res) {
            sMainService.displayRingingNotification();
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
                Log.w("Could not set chosen ringtone, falling back to system default ringtone");
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
            if (mCanRing) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }
        mMediaPlayer = null;
    }
    
    @Override
    public void stop() {
        clearMediaPlayer();
        if (mVibrator != null) {
            mVibrator.cancel();
        }
        sMainService.hideRingingNotification();
    }

    @Override
    protected void initializeSubCommands() {
        Cmd ring = mCommandMap.get("ring");
        ring.setHelp(R.string.chat_help_ring, "[0-100]");
        ring.AddSubCmd("stop", R.string.chat_help_ring_stop, null);
  
        Cmd ringmode = mCommandMap.get("ringmode");
        ringmode.setHelp(R.string.chat_help_ringmode, null);
        ringmode.AddSubCmd("silent", R.string.chat_help_ringmode_silent, null);
        ringmode.AddSubCmd("vibrate", R.string.chat_help_ringmode_vibrate, null);
        ringmode.AddSubCmd("normal", R.string.chat_help_ringmode_normal, null);
    }
}
