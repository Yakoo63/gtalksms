package com.googlecode.gtalksms.cmd;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.view.KeyEvent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;

public class MusicCmd extends CommandHandlerBase {

    private static AudioManager audioManager;
    
    public MusicCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, "Music", new Cmd("music", "zic"), new Cmd("volume", "vol"));
    }

    @Override
    protected void onCommandActivated() {
        audioManager = (AudioManager) sContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onCommandDeactivated() {
        audioManager = null;
    }

    @Override
    protected void execute(Command cmd) {
        String arg = cmd.getArg1();
        if (isMatchingCmd(cmd, "music")) {
            if (arg.toLowerCase().equals("next")) {
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            } else if (arg.toLowerCase().equals("previous")) {
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            } else if (arg.toLowerCase().equals("play")) {
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
            } else if (arg.toLowerCase().equals("pause")) {
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
            } else if (arg.toLowerCase().equals("stop")) {
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP);
            } else if (arg.toLowerCase().equals("playpause")) {
                sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        }
        if (isMatchingCmd(cmd, "music") || isMatchingCmd(cmd, "volume")) {
            if (arg.toLowerCase().equals("up")) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
            } else if (arg.toLowerCase().equals("down")) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            } else if (arg.toLowerCase().equals("mute")) {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            } else if (arg.toLowerCase().equals("unmute")) {
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            } else if (isMatchingCmd(cmd, "volume")) {
                Integer value = Tools.parseInt(arg);
                if (value != null) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
                }
                send(R.string.chat_volume_level, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        }
    }

    private void sendKeyEvent(int key) {
        Log.d("Sending event key " + key);
        handleMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        handleMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
    }

    private void handleMediaKeyEvent(KeyEvent keyEvent) {
        boolean hasDispatchSucceeded = false;
        try {
            // Get binder from ServiceManager.checkService(String)
            IBinder iBinder = (IBinder) Class.forName("android.os.ServiceManager")
                    .getDeclaredMethod("checkService", String.class).invoke(null, Context.AUDIO_SERVICE);

            // get audioService from IAudioService.Stub.asInterface(IBinder)
            Object audioService = Class.forName("android.media.IAudioService$Stub")
                    .getDeclaredMethod("asInterface", IBinder.class).invoke(null, iBinder);

            // Dispatch keyEvent using IAudioService.dispatchMediaKeyEvent(KeyEvent)
            Class.forName("android.media.IAudioService").getDeclaredMethod("dispatchMediaKeyEvent", KeyEvent.class)
                    .invoke(audioService, keyEvent);
            hasDispatchSucceeded = true;
        } catch (Exception e) {
            Log.e("Error sending event key " + e.getMessage(), e);
        }
        
        // If dispatchMediaKeyEvent failed then try using broadcast
        if (!hasDispatchSucceeded) {
            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
            sContext.sendOrderedBroadcast(intent, null);
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd music = mCommandMap.get("music");
        music.setHelp(R.string.chat_help_music_general);
        music.AddSubCmd("play", R.string.chat_help_music_play);
        music.AddSubCmd("pause", R.string.chat_help_music_pause);
        music.AddSubCmd("playpause", R.string.chat_help_music_playpause);
        music.AddSubCmd("stop", R.string.chat_help_music_stop);
        music.AddSubCmd("next", R.string.chat_help_music_next);
        music.AddSubCmd("previous", R.string.chat_help_music_previous);
        music.AddSubCmd("up", R.string.chat_help_volume_up);
        music.AddSubCmd("down", R.string.chat_help_volume_down);
        music.AddSubCmd("mute", R.string.chat_help_volume_mute);
        music.AddSubCmd("unmute", R.string.chat_help_volume_unmute);

        Cmd volume = mCommandMap.get("volume");
        volume.setHelp(R.string.chat_help_volume_general, "#volumeLevel#");
        volume.AddSubCmd("up", R.string.chat_help_volume_up);
        volume.AddSubCmd("down", R.string.chat_help_volume_down);
        volume.AddSubCmd("mute", R.string.chat_help_volume_mute);
        volume.AddSubCmd("unmute", R.string.chat_help_volume_unmute);
    }
}