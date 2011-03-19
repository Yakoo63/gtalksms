package com.googlecode.gtalksms.cmd;

import java.io.File;

import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFileManager;

public class FileCmd extends Command {
    private XmppManager _xmppMgr;

    
    public FileCmd(MainService mainService) {
        super(mainService, new String[] {"send"});
        _xmppMgr = _mainService.getXmppmanager();
    }
    
    @Override
    protected void execute(String cmd, String args) {
        File file = new File(args);
        if (file.exists()) {
            sendFile(file);
        } else {
            send("File '" + file.getAbsolutePath() + "' doesn't exist!");  // TODO localization
        }
    }
    
    @Override
    public String[] help() {
        return null;
    }
    
    private void sendFile(File file) {
        FileTransferManager fileTransferManager = _xmppMgr.getXmppFileMgr().getFileTransferManager();
        OutgoingFileTransfer transfer = fileTransferManager.createOutgoingFileTransfer(_answerTo);

        try {
            transfer.sendFile(file, "Sending you: " + file.getAbsolutePath() + " to: " + _answerTo);
            send("File transfer: " + file.getAbsolutePath() + " - " + transfer.getFileSize() / 1024 + " KB");
            
            while (!transfer.isDone()) {
                if (transfer.getStatus() == FileTransfer.Status.refused) {
                    send("Could not send the file. Refused by peer.");
                    return;
                }
                if (transfer.getStatus() == FileTransfer.Status.error) {
                    send(XmppFileManager.returnAndLogError(transfer));
                    return;
               }
               Thread.sleep(1000);
            }
        } catch (Exception ex) {
            String message = "Cannot send the file because an error occured during the process." 
                + Tools.LineSep + ex.getMessage();
            Log.e(Tools.LOG_TAG, message, ex);
            send(message);
        }
    }

}
