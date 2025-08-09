package com.googlecode.gtalksms.cmd.shellCmd;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import android.content.Context;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.ShellCmd;
import com.googlecode.gtalksms.tools.RootTools;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFont;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class Shell {
    class ShellThread implements Runnable {
        boolean mStop = false;
        
        public void stop() {
            mStop = true;
        }
        
        public void run() {
            mResults.append(mCurrentCommand);
            mResults.append(Tools.LineSep);
            
            Process myproc;
            
            try { 
                if (!RootTools.askRootAccess()) {
                    mResults.append(mContext.getString(R.string.chat_error_root)).append(Tools.LineSep);
                    myproc = Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c", mCurrentCommand});
                } else {
                    myproc = Runtime.getRuntime().exec("su");

                    DataOutputStream os = new DataOutputStream(myproc.getOutputStream());
                    os.writeBytes(mCurrentCommand + "\n");
                    os.writeBytes("exit\n");
                    os.flush();
                    os.close();
                }
    
                readStream(myproc.getInputStream());
                readStream(myproc.getErrorStream());
                
                if (!mStop) {
                    sendResults();
                }
            }
            catch (InterruptedException ex) {
                sendMessage(mCurrentCommand + " killed.");
            }
            catch (Exception ex) {
                Log.w("Shell command error", ex);
            }
            
            mThread = null;
            mShellThread = null;
            mCurrentCommand = null;
        }
        
        /**
         * Reads the given InputStream and sends a XmppMsg every 5000 chars
         * or every 10 seconds, whatever comes first.
         * If we happen to encounter an InputStream that never stops, like from
         * "tail -f" or "logcat" without the "-d" option, the method will never 
         * return. See executeCommand on how we handle this.
         * 
         * @param is
         * @throws Exception
         */
        void readStream(InputStream is) throws Exception {
            String line;
            Date start = new Date();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            
            while ((line = reader.readLine()) != null && !mStop) {
                mResults.append(line);
                mResults.append(Tools.LineSep);
                
                Date end = new Date();
                if ((end.getTime() - start.getTime()) / 1000 > 10 || mResults.length() > 5000 ) {
                    start = end;
                    int last = mResults.lastIndexOf("\n");
                    if (last != -1) {
                        XmppMsg msg = new XmppMsg(_font);
                        msg.append(mResults.substring(0, last + 1));
                        mCmdBase.send(mShellId, msg);
                        // fastest way to empty a StringBuilder
                        mResults.setLength(0);
                    }
                }
            }
        }
    }
    
    // Id to identify the console/room (0 for main chat windows)
    private final int mShellId;
    
    // Execution thread
    private Thread mThread;
    private ShellThread mShellThread;
    
    // Buffered results
    private StringBuilder mResults = new StringBuilder();
    
    // Command
    // TODO an array ?
    private String mCurrentCommand;

    // Reference to shell command manager to manage results
    private final ShellCmd mCmdBase;
    
    // Android context reference
    private final Context mContext;
    
    // Default result font
    // TODO allow modifications ?
    private final XmppFont _font = new XmppFont("consolas", "red");

    public Shell(int id, ShellCmd cmdBase, Context context) {
        mShellId = id;
        mCmdBase = cmdBase;
        mContext = context;
    }
       
    /**
     * Executes a given command, if the previous command is still running, it's
     * thread will be stopped.
     * 
     * @param shellCmd
     */
    public void executeCommand(String shellCmd) {
        // check if the previous Command Thread still exists
        if (mThread != null && mThread.isAlive()) {
            mShellThread.stop();
            mResults = new StringBuilder();
        }
        mCurrentCommand = shellCmd;
        mShellThread = new ShellThread();
        mThread = new Thread(mShellThread);
        mThread.start();
    }
    
    private void sendResults() {
        XmppMsg msg = new XmppMsg(_font);
        msg.append(mResults.toString());
        mCmdBase.send(mShellId, msg);
        mResults = new StringBuilder();
    }
    
    private void sendMessage(String msg) {
        mCmdBase.send(mShellId, msg);
    }

    public void stop() {
        try {
            if (mThread != null && mThread.isAlive()) {
                mShellThread.stop();
                mThread.join(1000);
            }
            mThread = null;
            mShellThread = null;
        } catch (Exception e) {
            Log.d("Error while stopping shell " + mShellId + ": " + e.getMessage());
        }
    }
}
