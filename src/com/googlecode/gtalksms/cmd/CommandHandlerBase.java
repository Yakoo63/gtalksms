package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.content.Context;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.cmd.Cmd.SubCmd;
import com.googlecode.gtalksms.data.contacts.ResolvedContact;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public abstract class CommandHandlerBase {
    
    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_CONTACTS = 2;
    public static final int TYPE_GEO = 3;
    public static final int TYPE_SYSTEM = 4;
    public static final int TYPE_COPY = 5;
    public static final int TYPE_MEDIA = 6;
    public static final int TYPE_INTERNAL = 7;
    
    protected static SettingsManager sSettingsMgr;
    protected static Context sContext;
    protected static MainService sMainService = null;
    protected final HashMap<String,Cmd> mCommandMap;
    protected final int mCmdType;
    protected String mAnswerTo;
        
    CommandHandlerBase(MainService mainService, int cmdType, Object... commands) {
        if (sMainService == null) {
            sMainService = mainService;
            sSettingsMgr = SettingsManager.getSettingsManager(sContext);
            sContext = mainService.getBaseContext();
            Cmd.setContext(sContext);
        }
        mCommandMap = new HashMap<String, Cmd>();
        for (Object o : commands) {
            Cmd c = (Cmd)o;
            mCommandMap.put(c.getName(), c);
        }
        mCmdType = cmdType;
        mAnswerTo = null;
        
        initializeSubCommands();
    }

    protected String getString(int id, Object... args) {
        return sContext.getString(id, args);
    }
    
    public int getType() {
        return mCmdType;
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
        send(message, mAnswerTo);
    }
    
    protected void send(XmppMsg message) {
        send(message, mAnswerTo);
    }
    
    protected void send(String message, String to) {
        sMainService.send(message, to);
    }

    protected void send(XmppMsg message, String to) {
        sMainService.send(message, to);
    }

    public Cmd[] getCommands() {
        return mCommandMap.values().toArray(new Cmd[mCommandMap.values().size()]);
    }   
    
    public Cmd getCommand(String name) {
        Cmd cmd = mCommandMap.get(name);
        
        if (cmd != null) {
            return cmd;
        } else {
            for (Cmd c : mCommandMap.values()) {
                for (String a : c.getAlias()) {
                    if (a.equals(name)) {
                        return c;
                    }
                }
            }
        }
        
        return null;
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
    	 * This method should be depreciated, and currently contains an 
    	 * experiment to isolate Xmpp From the commands altogether.
    	 * As the XmppUserCommand class is verified to be good, the XmppUserCommand
    	 * initialization should be moved out to the caller of this method.
    	 */
    	execute(new XmppUserCommand(XmppManager.getInstance(sContext), cmd, args, answerTo));
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
    	 * Default implementation is to fall back to old behavior with
    	 * _answerTo variable and Xmpp awareness in sub classes.
    	 * <p>
    	 * Make abstract when execute(String, String) is gone, but for now default to it for
    	 * backwards compatibility.
    	 */
    	this.mAnswerTo = userCommand.getReplyTo();
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
     * gets called in mainService when "stop" command received
     */
    public void stop() {}
    
    /**
     * Setups the command to get working. Usually called when the user want's 
     * GTalkSMS to be active (meaning connected)
     * setup() the contrary to cleanUp()
     */
    public void setup() {};
    
    /**
     * Cleans up the structures holden by the CommandHanlderBase Class.
     * Common actions are: unregister broadcast receivers etc.
     * Usually issued on the stop of the MainService
     */
    public void cleanUp() {};   
    
    /**
     * Request a help String array from the command
     * The String is formated with your internal BOLD/ITALIC/etc Tags
     * 
     * @return Help String array, null if there is no help available
     */
    public String[] help() {
        ArrayList<String> res = new ArrayList<String>();
        
        for (Cmd c : mCommandMap.values()) {
            if (c.getHelp() != null) {
                res.add(c.getHelp());
            }
            for (SubCmd sc : c.getSubCmds()) {
                if (sc.getHelp() != null) {
                    res.add(sc.getHelp());
                }
            }
        }
        
        if (res.size() > 0) {
            return res.toArray(new String[res.size()]);
        } else {
            return null;
        }            
    }
    
    protected abstract void initializeSubCommands();
    
    protected String makeBold(String msg) {
        return XmppMsg.makeBold(msg);
    }
    
    /**
     * Useful Method to split the arguments into an String Array
     * The Arguments are split by ":"
     * 
     * @param args
     * @return args split in an array or an array only containing the empty string
     */
    protected String[] splitArgs(String args) {
        StringTokenizer strtok = new StringTokenizer(args, ":");
        int tokenCount = strtok.countTokens();
        String[] res;
        if (tokenCount != 0) {
            res = new String[tokenCount];
            for (int i = 0; i < tokenCount; i++)
                res[i] = strtok.nextToken();
        } else {
            res = new String[] { "" };
        }
        return res;
    }
    
    /**
     * Returns a nice formated String of the Commands this class handles
     * 
     * @return
     */
    protected final String getCommandsAsString() {
        String res = "";
        for(Cmd c : mCommandMap.values()) {
            res += c.getName(); 
            if (c.getAlias().length > 0) {
                res += " (";
                for(String s : c.getAlias()) {
                    res += s + ", ";
                }
                res = res.substring(0, res.length() - 2) + ")";
            }
            res += ", ";
        }
        return res;
    }    
    
    /**
     * Sends the help messages from the current command
     * to the user, does nothing if there are no help
     * messages available
     * 
     */
    protected final void sendHelp() {
        String[] help = help();
        if (help == null) {
            return;
        }
        
        XmppMsg msg = new XmppMsg();
        msg.addStringArray(help);
        send(msg);
    }
    
    /**
     * This method presents the user with possible candidates, when the user
     * given contact information is not distinct enough, so that there are
     * more possible contacts that match these informations.
     * This is a quite common task, so it has its own method.
     * 
     * @param candidates
     */
    protected void askForMoreDetails(ResolvedContact[] candidates) {
        XmppMsg msg = new XmppMsg(getString(R.string.chat_specify_details));
        msg.newLine();
        for (ResolvedContact rc : candidates) {
            msg.appendLine(rc.getName() + " - " + rc.getNumber());
        }
        send(msg);
    }
}
