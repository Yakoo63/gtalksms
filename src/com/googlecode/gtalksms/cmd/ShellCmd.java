package com.googlecode.gtalksms.cmd;

import java.util.HashMap;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.shellCmd.Shell;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFont;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;

public class ShellCmd extends CommandHandlerBase {
    
    private final HashMap<Integer, Shell> mShells = new HashMap<Integer, Shell>();
    private static final String sRoomName = "Shell";
    XmppFont _font = new XmppFont("consolas", "red");

    public ShellCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Shell", new Cmd("cmd"), new Cmd("shell"));
    }
        
    
    @Override
    public void execute(Command cmd) {
        Integer shellId;
        if (cmd.getCommand().equals("cmd")) {
            // Try to parse the roomId, if error we'll not use chat rooms
            shellId = Tools.parseInt(cmd.getReplyTo(), 0);
            try {
                if (!mShells.containsKey(shellId)) {
                    mShells.put(shellId, new Shell(shellId, this, sContext));
                }
                mShells.get(shellId).executeCommand(cmd.getAllArg1());
            } catch (Exception e) {
                send(R.string.chat_shell_error_access, shellId, e.getLocalizedMessage());
            }
        } else if (cmd.getCommand().equals("shell")) {
            // Try to parse the roomId, if error we'll use the Shell 1
            shellId = Tools.parseInt(cmd.getArg1(), 1);
            try {
                XmppMuc.getInstance(sContext).inviteRoom(shellId.toString(), sRoomName, XmppMuc.MODE_SHELL);
                mShells.put(shellId, new Shell(shellId, this, sContext));
                
            } catch (Exception e) {
                send(R.string.chat_shell_error_instance, e.getLocalizedMessage());
            }
        }
    }
    
    public void send(Integer id, String msg) {
        send(id, new XmppMsg(msg));
    }
    
    public void send(Integer id, XmppMsg msg) {
        if (id == 0) {
            send(msg);
        } else {
            try {
                XmppMuc.getInstance(sContext).writeRoom(id.toString(), sRoomName, msg, XmppMuc.MODE_SHELL);
            } catch (Exception e) {
                // room creation and/or writing failed - 
                // notify about this error
                // and send the message to the notification address
                XmppMsg msgError = new XmppMsg();
                msgError.appendLine(getString(R.string.chat_shell_error_muc_write, e.getLocalizedMessage()));
                msgError.appendBold(sRoomName + " " + id);
                msgError.append(msg);
                send(msgError, null);
            }
        }
    }

    @Override
    protected void onCommandActivated() {
        // Create the default shell for the cmd command
        mShells.put(0, new Shell(0, this, sContext));
    }

    @Override
    protected void onCommandDeactivated() {
        for (Shell s : mShells.values()){
            s.stop();
        }
        mShells.clear();
    }

    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("cmd").setHelp(R.string.chat_help_cmd, "#command#");   
        mCommandMap.get("shell").setHelp(R.string.chat_help_shell, "#ShellId#");
    }
}
