package com.googlecode.gtalksms.cmd;

import java.text.DateFormat;
import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.smsCmd.Mms;
import com.googlecode.gtalksms.cmd.smsCmd.MmsManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class MmsCmd extends CommandHandlerBase {
    private MmsManager mMmsManager;

    public MmsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MESSAGE, "MMS", new Cmd("mms", "m"));
    }

    @Override
    protected void onCommandActivated() {
        mMmsManager = new MmsManager(sContext);
    }

    @Override
    protected void onCommandDeactivated() {
        mMmsManager = null;
    }

    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "mms")) {
            sendMmsListOnXmpp(mMmsManager.getLastMmsDetails(Tools.parseInt(cmd.getArg1(), 10)), null, null);
        }
    }

    private void appendMms(XmppMsg mmsMsg, Mms mms) {
        mmsMsg.append(DateFormat.getDateTimeInstance().format(mms.getDate()));
        mmsMsg.append(" - ");
        mmsMsg.appendBold(mms.getSender());
        mmsMsg.append(" --> ");
        mmsMsg.appendBold(mms.getRecipients());
        mmsMsg.appendItalicLine(mms.getSubject() == null || mms.getSubject().isEmpty() ? "" : "\n<" + mms.getSubject() + ">");
        String msg = mms.getMessage();
        if (msg != null && !msg.isEmpty()) {
            mmsMsg.appendLine(msg);
        }
        mmsMsg.appendLine("");
    }
    
    /** Helper to send messages via xmpp according to the settings */
    private void sendMmsListOnXmpp(ArrayList<Mms> mmsList, String preMsg, String postMsg) {
        XmppMsg message = new XmppMsg();
        if (sSettingsMgr.smsReplySeparate) {
            if (preMsg != null) {
                message.appendBold(preMsg);
                sendAndClear(message);
            }
            for (Mms mms : mmsList) {
                appendMms(message, mms);
                sendAndClear(message);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {}
            }
            if (postMsg != null) {
                message.appendItalicLine(postMsg);
                sendAndClear(message);
            }
        } else {
            if (preMsg != null) {
                message.appendBoldLine(preMsg);
            }
            for (Mms mms : mmsList) {
                appendMms(message, mms);
            }
            if (postMsg != null) {
                message.appendItalicLine(postMsg);
            }
            send(message);
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd mms = mCommandMap.get("mms");
        mms.setHelp(R.string.chat_help_mms_general, null);
    }
}
