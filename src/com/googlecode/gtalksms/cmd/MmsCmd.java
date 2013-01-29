package com.googlecode.gtalksms.cmd;

import java.text.DateFormat;
import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.cmd.smsCmd.Mms;
import com.googlecode.gtalksms.cmd.smsCmd.MmsManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class MmsCmd extends CommandHandlerBase {
    private MmsManager mMmsManager;
              
    public MmsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MESSAGE, "MMS", new Cmd("mms", "m"));
        mMmsManager = new MmsManager(sContext);
    }

    @Override
    protected void execute(String command, String args) {
        if (isMatchingCmd("mms", command)) {
            String[] arg = splitArgs(args);
            if (arg[0].equals("sent")) {
                printMmsList(mMmsManager.getLastSentMmsDetails(Tools.parseInt(arg, 1, 10)));
            } else {
                printMmsList(mMmsManager.getLastReceivedMmsDetails(Tools.parseInt(arg, 0, 10)));
            }
        }
    }  
    
    private void printMmsList(ArrayList<Mms> allMms) {
        XmppMsg mmsMsg = new XmppMsg();
            for (Mms mms: allMms) {
                mmsMsg.append(DateFormat.getDateTimeInstance().format(mms.getDate()));
                mmsMsg.append(" - ");
             mmsMsg.appendBold(mms.getSender());
             mmsMsg.append(" --> ");
                mmsMsg.appendBold(mms.getRecipients());
            mmsMsg.appendItalicLine(mms.getSubject() == null || mms.getSubject().isEmpty() ? "" : "\n<" + mms.getSubject() + ">");
            mmsMsg.appendLine(mms.getMessage());
        }
            send(mmsMsg);
    }

    @Override
    protected void initializeSubCommands() {
    }
}
