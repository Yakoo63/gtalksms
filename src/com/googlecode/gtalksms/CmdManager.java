package com.googlecode.gtalksms;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.googlecode.gtalksms.tools.Tools;

public abstract class CmdManager {
    Context _context;
    SettingsManager _settings;
    Handler _cmdHandler = new Handler();
    Thread _cmdThread;
    final StringBuilder _cmdResults = new StringBuilder();
    String _currentCommand;
    
    public CmdManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
    }
    
    private boolean askRootAccess() {
        try {
            Process p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
            if (p.exitValue() != 255) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
        
    Runnable _cmdRunnable = new Runnable() {
        public void run() {
            _cmdResults.append(_currentCommand);
            _cmdResults.append(Tools.LineSep);
            
            if (!askRootAccess()) {
                _cmdResults.append(_context.getString(R.string.chat_error_root) + Tools.LineSep);
            }
    
            Process myproc = null;
            
            try {
                myproc = Runtime.getRuntime().exec(new String[] {"/system/bin/sh", "-c", _currentCommand});
                readStream(myproc.getInputStream());
                readStream(myproc.getErrorStream());
                
                sendResults(_cmdResults.toString());
                _cmdResults.setLength(0);
            }
            catch (Exception ex) {
                Log.w(Tools.LOG_TAG, "Shell command error", ex);
            }
            
            _cmdThread = null;
            _currentCommand = null;
        }
        
        void readStream(InputStream is) throws Exception {
            String line;
            Date start = new Date();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            
            while ((line = reader.readLine()) != null) {
                _cmdResults.append(line);
                _cmdResults.append(Tools.LineSep);
                
                Date end = new Date();
                if ((end.getTime() - start.getTime()) / 1000 > 10 || _cmdResults.length() > 5000 ) {
                    start = end;
                    int last = _cmdResults.lastIndexOf("\n");
                    if (last != -1) {
                        sendResults(_cmdResults.substring(0, last + 1));
                        _cmdResults.delete(0, last + 1);
                    }
                }
            }
        }
    };
    
    public void shellCmd(String cmd) {
        if (_cmdThread != null && _cmdThread.isAlive()) {
            sendResults(_currentCommand + " killed.");
            try { 
                _cmdThread.interrupt();
                _cmdThread.join(1000); 
            } catch (Exception e) {}
            
            try { _cmdThread.stop(); } catch (Exception e) {}
            
            sendResults(_cmdResults.toString());
            _cmdResults.setLength(0);
        }
        
        _currentCommand = cmd;
        _cmdThread = new Thread(_cmdRunnable);
        _cmdThread.start();
    }

    abstract void sendResults(String message);
}
