package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.Cmd.SubCmd;
import com.googlecode.gtalksms.data.contacts.ResolvedContact;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public abstract class CommandHandlerBase {
    
    static final int TYPE_MESSAGE = 1;
    static final int TYPE_CONTACTS = 2;
    static final int TYPE_GEO = 3;
    static final int TYPE_SYSTEM = 4;
    static final int TYPE_COPY = 5;
    static final int TYPE_MEDIA = 6;
    public static final int TYPE_INTERNAL = 7;
    
    static SettingsManager sSettingsMgr;
    static Context sContext;
    static MainService sMainService = null;
    final HashMap<String,Cmd> mCommandMap;
    final int mCmdType;
    String mAnswerTo;
    
    private boolean mIsActivated;
    private final String mName;

    CommandHandlerBase(MainService mainService, int cmdType, String name, Cmd... commands) {
        if (sMainService == null) {
            sMainService = mainService;
            sSettingsMgr = SettingsManager.getSettingsManager(sContext);
            sContext = mainService.getBaseContext();
            Cmd.setContext(sContext);
        }
        
        mCommandMap = new HashMap<String, Cmd>();
        for (Cmd c : commands) {
            mCommandMap.put(c.getName().toLowerCase(), c);
        }
        mCmdType = cmdType;
        mAnswerTo = null;
        mName = name;
        
        initializeSubCommands();
        
        mIsActivated = false;
    }

    protected abstract void onCommandActivated();
    protected abstract void onCommandDeactivated();
    protected abstract void initializeSubCommands();

    /**
     * Executes the given command
     * sends the results, if any, back to the given JID
     *
     * @param userCommand the base command
     */
    abstract void execute(Command userCommand);

    /**
     * Stop all ongoing actions caused by a command
     * gets called in mainService when "stop" command received
     */
    public void stop() {}

    /**
     * Setups the command to get working. Usually called when the user wants
     * GTalkSMS to be active (meaning connected) and if the command is
     * activated
     */
    public final void activate() {
        mIsActivated = true;
        Log.i("Activating command " + mName);
        onCommandActivated();
    }

    /**
     * Cleans up the structures holden by the CommandHandlerBase Class.
     * Common actions are: unregister broadcast receivers etc.
     * Usually issued on the stop of the MainService
     */
    public final void deactivate() {
        mIsActivated = false;
        Log.i("Deactivating command " + mName);
        onCommandDeactivated();
    }

    public boolean updateAndReturnStatus() {
        boolean atLeastOneCommandActive = false;
        for (Cmd c : mCommandMap.values()) {
            if (c.isActive()) {
                atLeastOneCommandActive = true;
            }
        }
        
        if (atLeastOneCommandActive && !mIsActivated) {
            activate();
        } else if (!atLeastOneCommandActive && mIsActivated) {
            deactivate();
            MainService.displayToast("Disabled " + mName, null, true);
        }
        
        return mIsActivated;
    }

    String getString(int id, Object... args) {
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
    void send(int id, Object... args) {
        send(getString(id, args));
    }    
    
    void send(String message) {
        send(message, mAnswerTo);
    }
    
    void send(XmppMsg message) {
        send(message, mAnswerTo);
    }
    
    void sendAndClear(XmppMsg message) {
        send(message, mAnswerTo);
        message.clear();
    }
    
    void send(String message, String to) {
        sMainService.send(message, to);
    }

    void send(XmppMsg message, String to) {
        sMainService.send(message, to);
    }
    
    /**
     * Sends an exception back to the user and
     * also logs this exception with info level
     * @param e
     */
    void send(Exception e) {
        send("Exception: " + e.toString(), mAnswerTo);
        Log.i("Exception", e);
    }

    public Cmd[] getCommands() {
        return mCommandMap.values().toArray(new Cmd[mCommandMap.values().size()]);
    }   
    
    public Cmd getCommand(String name) {
        Cmd cmd = mCommandMap.get(name.toLowerCase());
        
        if (cmd != null) {
            return cmd;
        }
        
        for (Cmd c : mCommandMap.values()) {
            for (String a : c.getAlias()) {
                if (a.equals(name.toLowerCase())) {
                    return c;
                }
            }
        }
        
        return null;
    }

    boolean isMatchingCmd(Command command, String ref) {
        Cmd cmd = getCommand(command.getCommand());
        return cmd != null && cmd.getName().equals(ref);
    }
    
    void executeNewCmd(String cmd) {
        executeNewCmd(cmd, null);
    }
    
    void executeNewCmd(String cmd, String args) {
        Intent i = new Intent(MainService.ACTION_COMMAND);
        i.putExtra("cmd", cmd);
        if (args != null) {
            i.putExtra("args", args);
        }
        i.setClassName("com.googlecode.gtalksms", "com.googlecode.gtalksms.MainService");
        sMainService.startService(i);
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
        this.mAnswerTo = answerTo;
        execute(new Command(cmd, args, answerTo));
    }

    /**
     * Request a help String array from the command
     * The String is formatted with your internal BOLD/ITALIC/etc Tags
     * 
     * @return Help String array, null if there is no help available
     */
    ArrayList<String> help() {
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
        
        return res;            
    }

    String makeBold(String msg) {
        return XmppMsg.makeBold(msg);
    }
    
    /**
     * Returns a nice formatted String of the Commands this class handles
     * 
     * @return
     */
    final String getCommandsAsString() {
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
    final void sendHelp() {
    	ArrayList<String> help = help();
        if (help.isEmpty()) {
            return;
        }
        
        XmppMsg msg = new XmppMsg();
        msg.addStringArray(help.toArray(new String[help.size()]));
        send(msg);
    }
    
    /**
     * This method presents the user with possible candidates, when the user
     * given contact information is not distinct enough, so that there are
     * more possible contacts that match these information.
     * This is a quite common task, so it has its own method.
     * 
     * @param candidates
     */
    void askForMoreDetails(ResolvedContact[] candidates) {
        XmppMsg msg = new XmppMsg(getString(R.string.chat_specify_details));
        msg.newLine();
        for (ResolvedContact rc : candidates) {
            msg.appendLine(rc.getName() + " - " + rc.getNumber());
        }
        send(msg);
    }
}
