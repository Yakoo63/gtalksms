package com.googlecode.gtalksms.xmpp;

import java.io.File;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

public class XmppFileManager implements FileTransferListener {
   private static final int MAX_CYCLES = 30; 
    
    private static SettingsManager _settings;
    private static XMPPConnection _connection;
    private static FileTransferManager _fileTransferManager = null;
    private static String answerTo;
    private static File externalFilesDir;
    private static File landingDir;
    private static Context ctx;
    
    private static XmppFileManager xmppFileManager;
    
    private static final String gtalksmsDir = Tools.APP_NAME;
    
    private XmppFileManager(Context context) {
        _settings = SettingsManager.getSettingsManager(context);
        ctx = context;
        if (Build.VERSION.SDK_INT >= 8) {  // API Level >= 8 check
            externalFilesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            externalFilesDir = Environment.getExternalStorageDirectory();
        }
        landingDir = new File(externalFilesDir, gtalksmsDir);
        if (!landingDir.exists()) {
            landingDir.mkdirs();
        }
    }
    
    public void registerListener(XmppManager xmppMgr) {
        XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
            public void newConnection(XMPPConnection connection) {
                _connection = connection;
                _fileTransferManager = new FileTransferManager(_connection);
                _fileTransferManager.addFileTransferListener(XmppFileManager.this);
            }            
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
    
    public static XmppFileManager getInstance(Context ctx) {
        if (xmppFileManager == null) {
            xmppFileManager = new XmppFileManager(ctx);
        }
        return xmppFileManager;
    }
   
   /**
    * returns the FileTransferManager for the current connection
    * @return
    */
   public FileTransferManager getFileTransferManager() {
       return _fileTransferManager;
   }

    @Override
    public void fileTransferRequest(FileTransferRequest request) {
        File saveTo;
        answerTo = request.getRequestor();  // set answerTo for replies and send()        
        if (!answerTo.startsWith(_settings.notifiedAddress)) { 
            send(R.string.chat_file_transfer_file_rejected, answerTo);
            request.reject();
            return;                
        } else if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            send(R.string.chat_file_transfer_file_not_mount);
            request.reject();
            return;
        } else if (!landingDir.isDirectory()) {
            send(R.string.chat_file_transfer_not_dir, landingDir.getAbsolutePath());
            request.reject();
            return;
        }
        
        saveTo = new File(landingDir, request.getFileName());
        if (saveTo.exists()) {
            send(R.string.chat_file_transfer_file_already_exists, saveTo.getAbsolutePath());
            request.reject();
            return;
        }
        
        IncomingFileTransfer transfer = request.accept();
        send(R.string.chat_file_transfer_file_kilobytes, saveTo.getName(), request.getFileSize() / 1024);
        try {
            transfer.recieveFile(saveTo);
            send(R.string.chat_file_transfer_file_status, saveTo.getName(), transfer.getStatus());
            double percents = 0.0;
            
            // We allow 30s before that status go to in progress
            int currentCycle = 0; 
            while (!transfer.isDone()) {
                if (transfer.getStatus() == Status.in_progress) {
                    percents = ((int)(transfer.getProgress() * 10000)) / 100.0;
                    send(R.string.chat_file_transfer_file_percents, saveTo.getName(), percents);
                } else if (transfer.getStatus() == Status.error) {
                    send(returnAndLogError(transfer));
                    if (saveTo.exists()) {
                        saveTo.delete();
                    }
                    return;
                // If we are not in progress state, increase the cycles count;
                } else {
                    currentCycle++;
                }
                if (currentCycle > MAX_CYCLES) {
                    break;
                }
                Thread.sleep(1000);
            }
            if (transfer.getStatus().equals(Status.complete)) {
                send(R.string.chat_file_transfer_file_complete, saveTo.getAbsolutePath());
            } else {
                send(returnAndLogError(transfer));
            }
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "Cannot send the file because an error occured during the process.", ex);
            send(R.string.chat_file_transfer_error, ex.getMessage());
        }
    }

    public static XmppMsg returnAndLogError(FileTransfer transfer) {
        XmppMsg message = new XmppMsg();
        message.appendBoldLine(ctx.getString(R.string.chat_file_transfer_error_msg));
        if (transfer.getError() != null) {
            message.appendLine(transfer.getError().getMessage());
            Log.w(Tools.LOG_TAG, transfer.getError().getMessage());
        }
        if (transfer.getException() != null) {
            message.appendLine(transfer.getException().getMessage());
            Log.w(Tools.LOG_TAG, transfer.getException().getMessage(), transfer.getException());
        }
        if (transfer.getStatus() == Status.negotiating_stream) {
            message.appendLine(ctx.getString(R.string.chat_file_transfer_error_stream));
            Log.w(Tools.LOG_TAG, "Negotiating stream failed");
        }
        return message;
    }
    
    public static File getLandingDir() {
        return landingDir;
    }   
    
    private void send(String msg) {
        Tools.send(msg, answerTo, ctx);
    }
    
    private void send(XmppMsg msg) {
        Tools.send(msg, answerTo, ctx);
    }
    
    private void send(int id, Object... args) {
        send(ctx.getString(id, args));
    }    
}
