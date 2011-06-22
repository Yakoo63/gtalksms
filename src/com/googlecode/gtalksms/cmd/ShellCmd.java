package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;

import org.jivesoftware.smack.XMPPException;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.shell.Shell;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFont;
import com.googlecode.gtalksms.xmpp.XmppMsg;
import com.googlecode.gtalksms.xmpp.XmppMuc;

public class ShellCmd extends CommandHandlerBase {
    
    ArrayList<Shell> mShells = new ArrayList<Shell>();
    static final String sRoomName = "Shell";
    XmppFont _font = new XmppFont("consolas", "red");
    Integer sIndex = 0;
    
    public ShellCmd(MainService mainService) {
        super(mainService, new String[] {"cmd", "shell"}, CommandHandlerBase.TYPE_SYSTEM);
        
        // Create the main shell
        mShells.add(new Shell(0, this, sContext));
    }   
        
    
    @Override
    public void execute(Command cmd) {
        
        if (cmd.getCommand().equals("cmd")) {
            try {
                mShells.get(Tools.parseInt(cmd.getReplyTo(), 0)).executeCommand(cmd.getAllArguments());
            } catch (Exception e) {
                send("Failed to access to the shell #" + cmd.getReplyTo() + " : " + e.getLocalizedMessage());     
            }
        } else if (cmd.getCommand().equals("shell")) {
            
            // TODO see how to re-use shells 
            try {
                ++sIndex;
                XmppMuc.getInstance(sContext).inviteRoom(sIndex.toString(), sRoomName, XmppMuc.MODE_SHELL);
                mShells.add(new Shell(sIndex, this, sContext));
                
            } catch (XMPPException e) {
                // TODO internationalization
                send("Failed to create shell instance:" + e.getLocalizedMessage());
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
                msgError.appendLine("SHELL_RESULTS - Error writing to MUC: " + e);
                msgError.appendBold(getString(R.string.chat_sms_from, sRoomName + " " + id));
                msgError.append(msg);
                send(msgError);
            }
        }
    }
  
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_cmd, makeBold("\"cmd:#command#\""))
            };
        return s;
    }
}
