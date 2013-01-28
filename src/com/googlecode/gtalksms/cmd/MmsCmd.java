package com.googlecode.gtalksms.cmd;

import java.text.DateFormat;
import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.cmd.smsCmd.Mms;
import com.googlecode.gtalksms.cmd.smsCmd.MmsManager;
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
        	readLastMms(20);
        }
    }  
    
    private void readLastMms(int nb) {
    	ArrayList<Mms> allMms = mMmsManager.getLastReceivedMmsDetails(nb);
        
    	XmppMsg mmsMsg = new XmppMsg();
   	 	for (Mms mms: allMms) {
   	 		mmsMsg.append(DateFormat.getDateTimeInstance().format(mms.getDate()));
   	 		mmsMsg.append(" - ");
   	 		mmsMsg.appendBold(mms.getSender());
        	mmsMsg.append(" - ");
        	mmsMsg.appendItalicLine(mms.getSubject() == null || mms.getSubject().isEmpty() ? "<No subject>" : mms.getSubject());
        	mmsMsg.appendLine(mms.getMessage());
        }
   	 	send(mmsMsg);
    }

    @Override
    protected void initializeSubCommands() {
    }
}
