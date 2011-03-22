package com.googlecode.gtalksms.cmd;

import java.util.StringTokenizer;

import android.content.Context;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public abstract class Command { 
    protected static final int TYPE_MESSAGE = 1;
    protected static final int TYPE_CONTACTS = 2;
    protected static final int TYPE_GEO = 3;
    protected static final int TYPE_SYSTEM = 4;
    protected static final int TYPE_COPY = 5;
    
    protected static SettingsManager _settingsMgr;
    protected static Context _context;
    protected static MainService _mainService = null;
    protected final String[] _commands;
    protected final int _cmdType;
    protected String _answerTo;
        
    Command(MainService mainService, String[] commands, int cmdType) {
        if (_mainService == null) {
            _mainService = mainService;
            _settingsMgr = mainService.getSettingsManager();
            _context = mainService.getBaseContext();
        }
        this._commands = commands;
        this._cmdType = cmdType;
        this._answerTo = null;
    }

    protected String getString(int id, Object... args) {
        return _context.getString(id, args);
    }   
    
    /**
     * Nice send() wrapper that includes
     * the context.getString Method
     * 
     * @param id
     * @param args
     */
    protected void send(int id, Object... args) {
        send(getString(id, args));
    }    
    
    protected void send(String message) {
        send(message, _answerTo);
    }
    
    protected void send(XmppMsg message) {
        send(message, _answerTo);
    }
    
    protected void send(String message, String to) {
        _mainService.send(message, to);
    }

    protected void send(XmppMsg message, String to) {
        _mainService.send(message, to);
    }
    
    public String[] getCommands() {
        return _commands;
    }   
    
    public void execute(String cmd, String args, String answerTo) {
        this._answerTo = answerTo;
        execute(cmd,args);
    }
    
    /**
     * Executes the given command
     * sends the results, if any, back to the given JID
     * 
     * @param cmd the base command
     * @param args the arguments - substring after the first ":", if no other arguments where given this will be ""
     * @param answerTo JID for command output, null to send to default notification address
     */
    protected abstract void execute(String cmd, String args); 
        
    /**
     * Stop all ongoing actions caused by a command
     * gets called in mainService when "stop" command recieved
     */
    public void stop() {}
    
    /**
     * Cleans up the structures holden by the Command Class
     * Usually issued on exit of GtalkSMS
     */
    public void cleanUp() {};
    
    /**
     * Request a help String array from the command
     * The String is formated with your internal BOLD/ITALIC/etc Tags
     * 
     * @return Help String array, null if there is no help available
     */
    public abstract String[] help();
    
    protected String makeBold(String msg) {
        return XmppMsg.makeBold(msg);
    }
    
    /**
     * Useful Method to split the arguments into an String Array
     * The Arguments are split by ":"
     * 
     * @param args
     * @return
     */
    protected String[] splitArgs(String args) {
        StringTokenizer strtok = new StringTokenizer(args, ":");
        int tokenCount = strtok.countTokens();
        String[] res = new String[tokenCount];
        for(int i = 0; i < tokenCount; i++)
            res[i] = strtok.nextToken();
        return res;
    }
    
    /**
     * Returns a nice formated String of the Commands this class handles
     * 
     * @return
     */
    protected final String getCommandsAsString() {
        String res = "";
        for(String c : _commands) {
            res  = res + c + ", ";
        }
        res = res.substring(0, res.length() - 1);
        return res;
    }
}
