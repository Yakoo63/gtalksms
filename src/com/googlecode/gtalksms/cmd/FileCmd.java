package com.googlecode.gtalksms.cmd;

import java.io.File;
import java.io.FileFilter;

import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.databases.KeyValueHelper;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFileManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class FileCmd extends CommandHandlerBase {
    private static final int MAX_CYCLES = 30;
    
    private File landingDir;
    private File sendDir;  // where the files come from if send:filename is given
    private KeyValueHelper keyValueHelper;

    private Exception ex;
    
    public FileCmd(MainService mainService) {
        super(mainService, new String[] {"send", "ls"}, CommandHandlerBase.TYPE_SYSTEM);
        try {
            landingDir = XmppFileManager.getLandingDir();
        } catch (Exception e) {
            ex = e;
        }
        keyValueHelper = KeyValueHelper.getKeyValueHelper(mainService.getBaseContext());
        restoreSendDir();
    }
    
    @Override
    protected void execute(String cmd, String args) {
        if ( ex != null)
            throw new IllegalStateException(ex);
        
        if (cmd.equals("send")) {
            sendFile(args);
        } else if (cmd.equals("ls")) {
            ls(args);
        }
    }
    
    @Override
    public String[] help() {
        return null;
    }
    
    private void sendFile(String args) {
        if (args.equals(""))
            return;        
    
        File file;
        if (args.startsWith("/")) {
            file = new File(args);
        } else {
            file = new File(sendDir, args);
        }
        
        if (file.exists()) {
            sendFile(file);
        } else {
            send("File '" + file.getAbsolutePath() + "' doesn't exist!");  // TODO localization
        }
    }
    
    private void sendFile(File file) {
        FileTransferManager fileTransferManager = XmppFileManager.getInstance(sContext).getFileTransferManager();
        OutgoingFileTransfer transfer = fileTransferManager.createOutgoingFileTransfer(mAnswerTo);

        try {
            transfer.sendFile(file, "Sending you: " + file.getAbsolutePath() + " to: " + mAnswerTo);
            send("File transfer starting: " + file.getAbsolutePath() + " - " + transfer.getFileSize() / 1024 + " KB");
            
            // We allow 30s before that status go to in progress
           int currentCycle = 0;
            while (!transfer.isDone()) {
                if (transfer.getStatus() == FileTransfer.Status.refused) {
                    send("Could not send the file. Refused by peer.");
                    return;
                } else if (transfer.getStatus() == FileTransfer.Status.error) {
                    send(XmppFileManager.returnAndLogError(transfer));
                    return;
                } else if (transfer.getStatus() == FileTransfer.Status.negotiating_transfer) {
                    // user has not accepted the transfer yet
                    // reset the cycle count
                    currentCycle = 0; 
                } else if (transfer.getStatus() != FileTransfer.Status.in_progress) {
                    // there is still not transfer going on
                    currentCycle++;
                }
                if (currentCycle > MAX_CYCLES) {
                    send(XmppFileManager.returnAndLogError(transfer));
                    break;
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
    
    private void ls(String args) {
        if (args.equals("")) {
            setSendDir(landingDir);
            listDir(landingDir);
        } else if (args.startsWith("/")) {
            File dir = new File(args);
            listDir(dir);
        } else if (args.startsWith("./")) {  // emulate the current working directory with help of sendDir
            File dir = new File(sendDir, args.substring(1));
            listDir(dir);
        }
    }
    
    private void listDir(File dir) {
        if (dir.isDirectory()) {
            setSendDir(dir);
            XmppMsg res = new XmppMsg();
            File[] dirs = dir.listFiles(new FileCmd.DirFileFilter());
            File[] files = dir.listFiles(new FileCmd.FileFileFilter());

            if (dirs.length != 0) {
                res.appendBoldLine("Directories within " + dir.getAbsolutePath());
                for (File d : dirs) {
                    res.appendLine(d.getName() + "/");
                }
            }
            if (files.length != 0) {
                res.appendBoldLine("Files within " + dir.getAbsolutePath());
                for (File f : files) {
                    appendFileInfo(res, f);
                }
            }
            
            if (files.length == 0 && dirs.length == 0) {
                res.append("No file in " + dir.getAbsolutePath());
            }
            
            send(res);
        } else {
            send(dir.getAbsolutePath() + " is not a direcotry");
        }      
    }
    
    private static void appendFileInfo(XmppMsg msg, File f) {
        String name = f.getName();
        long size = f.length(); // the size of the file in bytes
        if (size > 1023) {
            msg.appendLine(name + " - " + size / 1024 + " KiB");
        } else {
            msg.appendLine(name + " - " + size + " Bytes");  
        }
    }
    
    /**
     * Sets the sendDir and saves it's value into the key-store database
     * @param dir
     */
    private void setSendDir(File dir) {
        sendDir = dir;
        String dirStr = dir.getAbsolutePath();
        keyValueHelper.addKey(KeyValueHelper.KEY_SEND_DIR, dirStr);
    }
    
    /**
     * Restores the sendDir from the key-value database
     */
    private void restoreSendDir() {
        String dir = keyValueHelper.getValue(KeyValueHelper.KEY_SEND_DIR);
        if (dir != null) {
            sendDir = new File(dir);
        } else {
            sendDir = landingDir;
        }
    }
    
    private class DirFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }      
    }
    
    private class FileFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isFile();
        }      
    }
}
