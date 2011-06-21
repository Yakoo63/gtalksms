package com.googlecode.gtalksms.cmd.shell;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import android.content.Context;
import android.util.Log;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.ShellCmd;
import com.googlecode.gtalksms.tools.RootTools;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFont;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class Shell {
    // Id to identify the console/room (0 for main chat windows)
    int mShellId;
    
    // Execution thread
    Thread mThread;
    
    // Buffered results
    StringBuilder mResults = new StringBuilder();
    
    // Command
    // TODO an array ?
    String mCurrentCommand;

    // Reference to shell command manager to manage results
    ShellCmd mCmdBase;
    
    // Android context reference
    Context mContext;
    
    // Default result font
    // TODO allow modifications ?
    XmppFont _font = new XmppFont("consolas", "red");

    public Shell(int id, ShellCmd cmdBase, Context context) {
        mShellId = id;
        mCmdBase = cmdBase;
        mContext = context;
    }
    
    private Runnable _cmdRunnable = new Runnable() {
        
        public void run() {
            mResults.append(mCurrentCommand);
            mResults.append(Tools.LineSep);
            
           
            Process myproc = null;
            
            try { 
                if (!RootTools.askRootAccess()) {
                    mResults.append(mContext.getString(R.string.chat_error_root) + Tools.LineSep);
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
                
                sendResults();
            }
            catch (Exception ex) {
                Log.w(Tools.LOG_TAG, "Shell command error", ex);
            }
            
            mThread = null;
            mCurrentCommand = null;
        }
        
        void readStream(InputStream is) throws Exception {
            String line;
            Date start = new Date();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            
            while ((line = reader.readLine()) != null) {
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
                        mResults.delete(0, last + 1);
                    }
                }
            }
        }
    };
    
    public void executeCommand(String shellCmd) {
        // check if the previous Command Thread still exists
        if (mThread != null && mThread.isAlive()) {
            sendMessage(mCurrentCommand + " killed.");
            try { 
                mThread.interrupt();
                mThread.join(1000); 
            } catch (Exception e) {}
            
            try { mThread.stop(); } catch (Exception e) {}
            
            sendResults();
        }
        mCurrentCommand = shellCmd;
        mThread = new Thread(_cmdRunnable);
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
}
