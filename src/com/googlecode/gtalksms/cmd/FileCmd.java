package com.googlecode.gtalksms.cmd;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.databases.KeyValueHelper;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFileManager;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class FileCmd extends CommandHandlerBase {
    private static final int MAX_CYCLES = 30;
    
    private File mLandingDir;
    private File mSendDir;  // where the files come from if send:filename is given
    private KeyValueHelper mKeyValueHelper;
    private XmppFileManager mXmppFileManager;
    private Exception mLastException;
    
    public FileCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, "File", new Cmd("send"), new Cmd("ls"), new Cmd("rm"));
    }

    @Override
    protected void onCommandActivated() {
        mXmppFileManager = XmppFileManager.getInstance(sContext);
        try {
            mLandingDir = mXmppFileManager.getLandingDir();
        } catch (Exception e) {
            mLastException = e;
        }
        mKeyValueHelper = KeyValueHelper.getKeyValueHelper(sContext);
        restoreSendDir();
    }

    @Override
    protected void onCommandDeactivated() {
        mXmppFileManager = null;
        mLandingDir = null;
        mLastException = null;
        mKeyValueHelper = null;
    }

    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "send")) {
            if (mLastException != null) {
                throw new IllegalStateException(mLastException);
            }
            sendFile(cmd.getAllArg1());
        } else if (isMatchingCmd(cmd, "ls")) {
            ls(cmd.getAllArg1());
        } else if (isMatchingCmd(cmd, "rm")) {
            rm(cmd.getAllArg1());
        }
    }
    
    private void sendFile(String args) {
        if (args.equals(""))
            return;        
    
        File file;
        if (args.startsWith("/")) {
            file = new File(args);
        } else {
            file = new File(mSendDir, args);
        }
        
        if (file.exists()) {
            sendFile(file);
        } else {
            send(R.string.chat_file_error, file.getAbsolutePath());
        }
    }
    
    class SendFileThread extends Thread {
        
        private final File mFile;
        
        public SendFileThread(File file) {
            this.mFile = file;
        }
        
        public void run() {
            FileTransferManager fileTransferManager = XmppFileManager.getInstance(sContext).getFileTransferManager();
            OutgoingFileTransfer transfer = fileTransferManager.createOutgoingFileTransfer(mAnswerTo);

            try {
                transfer.sendFile(mFile, getString(R.string.chat_file_sending, mFile.getAbsolutePath(), mAnswerTo));
                send(R.string.chat_file_transfer_started, mFile.getAbsolutePath(), transfer.getFileSize() / 1024);
                
                // We allow 30s before that status go to in progress
                int currentCycle = 0;
                while (!transfer.isDone()) {
                    if (transfer.getStatus() == FileTransfer.Status.refused) {
                        send(R.string.chat_file_transfer_refused);
                        return;
                    } else if (transfer.getStatus() == FileTransfer.Status.error) {
                        send(mXmppFileManager.returnAndLogError(transfer));
                        return;
                    } else if (transfer.getStatus() == FileTransfer.Status.negotiating_transfer) {
                        // user has not accepted the transfer yet reset the cycle count
                        currentCycle = 0; 
                    } else if (transfer.getStatus() != FileTransfer.Status.in_progress) {
                        // there is still not transfer going on
                        currentCycle++;
                    }
                    
                    if (currentCycle > MAX_CYCLES) {
                        send(mXmppFileManager.returnAndLogError(transfer));
                        break;
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                Log.e("Cannot send the file because an error occurred during the process.", ex);
                send(R.string.chat_file_transfer_error, ex.getMessage());
            }
        }
    }
    
    private void sendFile(File file) {
        SendFileThread t = new SendFileThread(file);
        // we don't want this thread to block the shutdown
        t.setDaemon(true);
        t.setName("sendFileThread:" + file.getName());
        t.start();
    }
    
    private void ls(String args) {
        if (args.equals("")) {
            setSendDir(mLandingDir);
            listDir(mLandingDir);
        } else if (args.startsWith("/")) {
            File dir = new File(args);
            listDir(dir);
        } else if (args.startsWith("./")) {  // emulate the current working directory with help of mSendDir
            File dir = new File(mSendDir, args.substring(1));
            listDir(dir);
        }
    }
    
    private void rm(String args) {
        File f;
        if (args.startsWith("/")) {
            f = new File(args);
        } else if (args.startsWith("./")) { // emulate the current working directory with help of mSendDir
            f = new File(mSendDir, args.substring(1));
        } else {
            send("Wrong Syntax");
            return;
        }
        if (f.delete()) {
            send("Deleted " + f.getAbsoluteFile());
        } else {
            send("Failed to delete " + f.getAbsolutePath());
        }
    }
    
    private void listDir(File dir) {
        if (dir.isDirectory()) {
            setSendDir(dir);
            XmppMsg res = new XmppMsg();
            File[] dirs = dir.listFiles(new FileCmd.DirFileFilter());
            File[] files = dir.listFiles(new FileCmd.FileFileFilter());

            if (dirs.length != 0) {
                Arrays.sort(dirs);
                res.appendBoldLine(getString(R.string.chat_file_transfer_dir, dir.getAbsolutePath()));
                for (File d : dirs) {
                    res.appendLine(d.getName() + "/");
                }
            }
            if (files.length != 0) {
                Arrays.sort(files);
                res.appendBoldLine(getString(R.string.chat_file_transfer_files, dir.getAbsolutePath()));
                for (File f : files) {
                    appendFileInfo(res, f);
                }
            }
            
            if (files.length == 0 && dirs.length == 0) {
                res.append(getString(R.string.chat_file_transfer_no_file, dir.getAbsolutePath()));
            }
            
            send(res);
        } else {
            send(R.string.chat_file_transfer_not_dir, dir.getAbsolutePath());
        }      
    }
    
    private void appendFileInfo(XmppMsg msg, File f) {
        String name = f.getName();
        long size = f.length(); // the size of the file in bytes
        if (size > 1023) {
            msg.appendLine(name + " " + size / 1024 + " KiB");
        } else {
            msg.appendLine(name + " " + size + " B");  
        }
    }
    
    /**
     * Sets the mSendDir and saves it's value into the key-store database
     * @param dir
     */
    private void setSendDir(File dir) {
        mSendDir = dir;
        String dirStr = dir.getAbsolutePath();
        mKeyValueHelper.addKey(KeyValueHelper.KEY_SEND_DIR, dirStr);
    }
    
    /**
     * Restores the mSendDir from the key-value database
     */
    private void restoreSendDir() {
        String dir = mKeyValueHelper.getValue(KeyValueHelper.KEY_SEND_DIR);
        mSendDir = dir != null ? new File(dir) : mLandingDir;
    }
    
    private class DirFileFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }      
    }
    
    private class FileFileFilter implements FileFilter {
        public boolean accept(File pathname) {
            return pathname.isFile();
        }      
    }
    
    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("send").setHelp(R.string.chat_help_send, "#file#");   
        mCommandMap.get("ls").setHelp(R.string.chat_help_ls, "#path#");   
        mCommandMap.get("rm").setHelp(R.string.chat_help_rm, "#filepath#");   
    }
}
