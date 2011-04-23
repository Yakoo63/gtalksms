package com.googlecode.gtalksms.cmd;

import java.util.StringTokenizer;

import android.content.Context;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public abstract class CommandHandlerBase { 
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
        
    CommandHandlerBase(MainService mainService, String[] commands, int cmdType) {
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
    public void send(int id, Object... args) {
        send(getString(id, args));
    }    
    
    public void send(String message) {
        send(message, _answerTo);
    }
    
    public void send(XmppMsg message) {
        send(message, _answerTo);
    }
    
    public void send(String message, String to) {
        _mainService.send(message, to);
    }

    public void send(XmppMsg message, String to) {
        _mainService.send(message, to);
    }
    
    public String[] getCommands() {
        return _commands;
    }   
    
    /**
     * Executes the given command
     * args and answerTo may be null
     * 
     * @param cmd
     * @param args
     * @param answerTo
     */
    public final void execute(String cmd, String args, String answerTo) {
    	/*
    	 * This method should be deprecated, and currently contains an 
    	 * experiment to isolate Xmpp From the commands altogether.
    	 * As the XmppUserCommand class is verified to be good, the XmppUserCommand
    	 * initialization should be moved out to the caller of this method.
    	 */
    	execute(new XmppUserCommand(XmppManager.getInstance(_context), cmd, args, answerTo));
    }
    
    private static class XmppUserCommand extends Command {
    	private final XmppManager xmppManager;
		public XmppUserCommand(XmppManager xmppManager, String cmd, String args, String replyTo) {
			super(cmd + ":" + args, replyTo);
			this.xmppManager = xmppManager;
		}

		@Override
		public void respond(String message) {
			xmppManager.send(new XmppMsg(message), getReplyTo());
		}
		
		@SuppressWarnings("unused")
		public void respond(XmppMsg msg) {
			xmppManager.send(msg, getReplyTo());
		}
    	
    }
    
    public void execute(Command userCommand) {
    	/*
    	 * Default implementation is to fall back to old behavoir with
    	 * _answerTo variable and Xmpp awareness in sub classes.
    	 * <p>
    	 * Make abstract when execute(String, String) is gone, but for now default to it for
    	 * backwards compatibility.
    	 */
    	this._answerTo = userCommand.getReplyTo();
    	execute(userCommand.getCommand(), userCommand.getAllArguments());
    }
    
    /**
     * Executes the given command
     * sends the results, if any, back to the given JID
     * 
     * @param cmd the base command
     * @param args the arguments - substring after the first ":", if no other arguments where given this will be ""
     * @param answerTo JID for command output, null to send to default notification address
     * @deprecated Use {@link #execute(Command)} instead
     */
    @Deprecated
    protected void execute(String cmd, String args) {
    	throw new RuntimeException("Must implement execute(UserCommand)");
    }
        
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
    
    /**
     * Adds the Strings in the given Arrary to the XmppMsg
     * One String per line
     * 
     * @param msg
     * @param s
     */
    protected final static void addStringArraytoXmppMsg(XmppMsg msg, String[] s) {
        for(String line : s) {
            msg.appendLine(line);
        }
    }
    
    /**
     * Sends the help messages from the current command
     * to the user, does nothing if there are no help
     * messages available
     * 
     */
    protected final void sendHelp() {
        String[] help = help();
        if (help == null)
            return;
        
        XmppMsg msg = new XmppMsg();
        addStringArraytoXmppMsg(msg, help);
        send(msg);
    }
    
    protected SettingsManager getSettingsManager() {
    	if (_mainService == null)
    		throw new IllegalStateException("Command._mainService is not set.");
    	return _mainService.getSettingsManager();
    }
}
