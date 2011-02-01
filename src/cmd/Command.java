package cmd;

import android.content.Context;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;

public abstract class Command {
    protected static SettingsManager _settingsMgr;
    protected static XmppManager _xmppMgr; 
    protected static Context _ctx;
    
    
    Command(Context ctx, SettingsManager sm, XmppManager xm) {
        Command._ctx = ctx;
        Command._settingsMgr = sm;
        Command._xmppMgr = xm;
    }
    
    /**
     * Executes the given command
     * has no return value, the method has to do the error reporting by itself
     * 
     * @param cmd command
     * @param args substring after the first ":" 
     */
    
    public abstract void executeCommand(String cmd, String args);

}
