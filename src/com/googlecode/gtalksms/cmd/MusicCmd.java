package com.googlecode.gtalksms.cmd;

import android.content.Context;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Tools;

public class MusicCmd extends CommandHandlerBase {
              
    public MusicCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, "Music", new Cmd("music"));
    }

    @Override
    protected void execute(String command, String args) {
        if (isMatchingCmd("music", command)) {
             if (args.toLowerCase().equals("next")) {
                 sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
             } else if (args.toLowerCase().equals("previous")) {
                 sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
             } else if (args.toLowerCase().equals("play")) {
                 sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
             } else if (args.toLowerCase().equals("pause")) {
                 sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE);
             } else if (args.toLowerCase().equals("stop")) {
                 sendKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP);
             }
        }
    }  

	private void handleMediaKeyEvent(KeyEvent keyEvent) {
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
		} catch (Exception e) {
	    	Log.e(Tools.LOG_TAG, "Error sending event key " + e.getMessage(), e);
		}
	}
    
    private void sendKeyEvent(int key) {
    	Log.d(Tools.LOG_TAG, "Sending event key " + key);
        handleMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        handleMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));

//        Log.d(Tools.LOG_TAG, "Sending event key " + key);
//        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
//        synchronized (this) {
//            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, key));
//            sContext.sendOrderedBroadcast(i, null);
//
//            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, key));
//            sContext.sendOrderedBroadcast(i, null);
//        }
    }

    @Override
    protected void initializeSubCommands() {
    }
}