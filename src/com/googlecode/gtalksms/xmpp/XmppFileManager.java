package com.googlecode.gtalksms.xmpp;

import java.io.File;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;

import android.content.Context;
import android.util.Log;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

public class XmppFileManager implements FileTransferListener {
    private SettingsManager _settings;
    private XMPPConnection _connection;
    private FileTransferManager _fileTransferManager = null;
    private XmppManager _xmppMgr;
    
    public XmppFileManager(Context context, SettingsManager settings, XmppManager xmppMgr) {
        _settings = settings;
        _xmppMgr = xmppMgr;
    }
    
    public void initialize(XMPPConnection connection) {
        _connection = connection;      
        _fileTransferManager = new FileTransferManager(_connection);
        _fileTransferManager.addFileTransferListener(this);
   }
    
    public void sendFile(String path) {
        OutgoingFileTransfer transfer = _fileTransferManager.createOutgoingFileTransfer(_settings.notifiedAddress);

        try {
            transfer.sendFile(new File(path), "Sending you: " + path);
            send("File transfert: " + path + " - " + transfer.getFileSize() / 1024 + " KB");
            
            while (!transfer.isDone()) {
                if (transfer.getStatus() == FileTransfer.Status.refused) {
                    send("Could not send the file. Refused by peer.");
                    return;
                }
                if (transfer.getStatus() == FileTransfer.Status.error) {
                    printError(transfer);
                    return;
               }
               Thread.sleep(500);
            }
        } catch (Exception ex) {
            String message = "Cannot send the file because an error occured during the process." 
                + Tools.LineSep + ex.getMessage();
            Log.e(Tools.LOG_TAG, message, ex);
            send(message);
        }
    }

    @Override
    public void fileTransferRequest(FileTransferRequest request) {
        if (!request.getRequestor().contains(_settings.notifiedAddress)) {
            send("File transfert from " + request.getRequestor() + " rejected.");
            return;
        }
            
        IncomingFileTransfer transfer = request.accept();
        //TODO check if destination dir exists, create if not   
        String filePath = "/sdcard/GTalkSMS/" + request.getFileName();
        send("File transfer: " + filePath + " - " + request.getFileSize() / 1024 + " KB");
        try {
            transfer.recieveFile(new File(filePath));
            send("File transfert: " + filePath + " - " + transfer.getStatus());
            double percents = 0.0;
            while (!transfer.isDone()) {
                if (transfer.getStatus().equals(Status.in_progress)) {
                    percents = ((int)(transfer.getProgress() * 10000)) / 100.0;
                    send("File transfer: " + filePath + " - " + percents + "%");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
                } else if (transfer.getStatus().equals(Status.error)) {
                    printError(transfer);
                    return;
                }
                Thread.sleep(500);
            }
            if (transfer.getStatus().equals(Status.complete)) {
                send("File transfert: " + filePath + " - 100%");
            } else {
                printError(transfer);
            }
        } catch (Exception ex) {
            String message = "Cannot receive the file because an error occured during the process." 
                + Tools.LineSep + ex;
            Log.e(Tools.LOG_TAG, message, ex);
            send(message);
        }
    }

    private void printError(FileTransfer transfer) {
        String message = "Cannot process the file because an error occured during the process." + Tools.LineSep;
        if (transfer.getError() != null) {
            message += transfer.getError() + Tools.LineSep;
        }
        if (transfer.getException() != null) {
            message += transfer.getException() + Tools.LineSep;
        }
        Log.w(Tools.LOG_TAG, message);
        send(message);
    }
    
    private void send(String msg) {
        _xmppMgr.send(msg);
    }
}
