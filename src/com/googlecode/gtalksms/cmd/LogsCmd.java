package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Logs;
import com.googlecode.gtalksms.tools.Tools;

public class LogsCmd extends CommandHandlerBase {
    
    class LogsThread implements Runnable {
        final Logs mLogs;
        boolean mStop;
        final int mLength;
        
        public LogsThread() {
            this(null, 100);
        }
        
        public LogsThread(int length) {
            this(null, length);
        }
        
        public LogsThread(String tags) {
            this(tags, 100);
        }
        
        public LogsThread(String tags, int length) {
            if (tags == null) {
                mLogs = new Logs(false);
            } else {
                mLogs = new Logs(tags, false);
            }
            mStop = false;
            mLength = length;
        }
        
        public void stop() {
            mLogs.stop();
            mStop = true;
        }
        
        public void run() {
            try { 
                send("Building Logs...");
                String logs = mLogs.getLogs(sContext, mLength);
                int index;
                while ((index = logs.indexOf(Logs.LINE_SEPARATOR, 1000)) != -1 && !mStop) {
                    send(logs.substring(0, index));
                    logs = logs.substring(index);
                }
                if (logs.length() > 0 && !logs.equals("\n")) {
                    send(logs);
                }
            } catch (Exception e) {
                send(e.getMessage());
                Log.w("Failed to send logs", e);
            }
            
            mLogsThread = null;
            mThread = null;
        }
    }
     
    // Execution thread
    private Thread mThread;
    private LogsThread mLogsThread;
    
    public LogsCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Logs", new Cmd("logs", "log"));
    }

    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "logs")) {
            if (mThread != null && mThread.isAlive()) {
                mLogsThread.stop();
            }
            String arg1 = cmd.getArg1();
            String arg2 = cmd.getAllArg2();
            if (!arg2.equals("")) {
                mLogsThread = new LogsThread(arg1, Tools.parseInt(arg2, 100));
            } else if (!arg1.equals("")) {
                if (Tools.isInt(arg1)) {
                    mLogsThread = new LogsThread(Tools.parseInt(arg1, 100));
                } else {
                    mLogsThread = new LogsThread(cmd.getAllArg1());
                }
            } else {
                mLogsThread = new LogsThread();
            } 
            mThread = new Thread(mLogsThread);
            mThread.start();
        }
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }
    
    @Override
    protected void initializeSubCommands() {
    }  
}
