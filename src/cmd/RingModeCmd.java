package cmd;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;

import android.content.Context;
import android.media.AudioManager;



public class RingModeCmd extends Command {
    private static AudioManager _audioManager;
    
    public RingModeCmd(Context ctx, SettingsManager sm, XmppManager xm) {
        super(ctx, sm, xm);
        _audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void executeCommand(String cmd, String args) {
        int mode;
        if(args.equals("vibrate")) {
            _audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        } else if (args.equals("normal")) {
            _audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        } else if (args.equals("silent")) {
            _audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        } else if (!args.equals("")) {
            _xmppMgr.send(_ctx.getString(R.string.chat_ringer_error_cmd, args));
            return;
        }
        mode = _audioManager.getRingerMode();
        switch (mode) {
        case AudioManager.RINGER_MODE_VIBRATE:
            _xmppMgr.send(_ctx.getString(R.string.chat_ringer_vibrate));
            break;
        case AudioManager.RINGER_MODE_NORMAL:
            _xmppMgr.send(_ctx.getString(R.string.chat_ringer_normal));
            break;
        case AudioManager.RINGER_MODE_SILENT:
            _xmppMgr.send(_ctx.getString(R.string.chat_ringer_silent));
            break;            
        }
        
    }
    

}
