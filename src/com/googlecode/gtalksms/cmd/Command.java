package com.googlecode.gtalksms.cmd;

import android.content.Context;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public abstract class Command {
    protected SettingsManager _settingsMgr;
    protected Context _context;
    protected MainService _mainService;
    
    Command(MainService mainService) {
        _mainService = mainService;
        _settingsMgr = mainService.getSettingsManager();
        _context = mainService.getBaseContext();
    }
    
    protected String getString(int id, Object... args) {
        return _context.getString(id, args);
    }
    
    protected void send(String message) {
        _mainService.send(message);
    }
    
    protected void send(XmppMsg message) {
        _mainService.send(message);
    }
    
    /**
     * Executes the given command
     * has no return value, the method has to do the error reporting by itself
     * 
     * @param cmd command
     * @param args substring after the first ":" 
     */
    public abstract void execute(String cmd, String args);
    public void stop() {}
    public void cleanUp() {}
}
