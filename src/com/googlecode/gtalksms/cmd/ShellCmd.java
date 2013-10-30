package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;

import org.jivesoftware.smack.XMPPException;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.shellCmd.Shell;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFont;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;

public class ShellCmd extends CommandHandlerBase {
    
    private final ArrayList<Shell> mShells = new ArrayList<Shell>();
    private static final String sRoomName = "Shell";
    XmppFont _font = new XmppFont("consolas", "red");
    private Integer sIndex = 0;
    
    public ShellCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "Shell", new Cmd("cmd"), new Cmd("shell"));
    }
        
    
    @Override
    public void execute(Command cmd) {
        
        if (cmd.getCommand().equals("cmd")) {
            try {
                mShells.get(Tools.parseInt(cmd.getReplyTo(), 0)).executeCommand(cmd.getAllArg1());
            } catch (Exception e) {
                send(R.string.chat_shell_error_access, cmd.getReplyTo(), e.getLocalizedMessage());     
            }
        } else if (cmd.getCommand().equals("shell")) {
            
            // TODO see how to re-use shells 
            try {
                ++sIndex;
                XmppMuc.getInstance(sContext).inviteRoom(sIndex.toString(), sRoomName, XmppMuc.MODE_SHELL);
                mShells.add(new Shell(sIndex, this, sContext));
                
            } catch (XMPPException e) {
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
            } catch (XMPPException e) {
                // room creation and/or writing failed - 
                // notify about this error
                // and send the message to the notification address
                XmppMsg msgError = new XmppMsg();
                msgError.appendLine(getString(R.string.chat_shell_error_muc_write, e.getLocalizedMessage()));
                msgError.appendBold(sRoomName + " " + id);
                msgError.append(msg);
                send(msgError);
            }
        }
    }

    @Override
    protected void onCommandActivated() {
        mShells.add(new Shell(0, this, sContext));
    }

    @Override
    protected void onCommandDeactivated() {
        for (Shell s : mShells){
            s.stop();
        }
        mShells.clear();
    }

    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("cmd").setHelp(R.string.chat_help_cmd, "#command#");   
        mCommandMap.get("shell").setHelp(R.string.chat_help_shell, null);   
    }
}
