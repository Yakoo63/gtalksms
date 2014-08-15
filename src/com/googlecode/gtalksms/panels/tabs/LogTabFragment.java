package com.googlecode.gtalksms.panels.tabs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Logs;

public class LogTabFragment extends SherlockFragment {

    private final static String LINE_SEPARATOR = System.getProperty("line.separator");
    private Button mButtonStartStop;
    private Button mButtonClear;
    private Button mButtonAutoScroll;
    private TextView mTextView;
    private ScrollView mScrollView;
    private LogsThread mLogsThread;
    private Thread mThread;
    private Handler mMainLooper;
    private boolean mAutoScoll = true;
    private final Semaphore mIsAvailable = new Semaphore(1, true);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_log, container, false);

        mMainLooper = new Handler(getActivity().getMainLooper());
        mScrollView = (ScrollView) view.findViewById(R.id.ScrollView);
        mButtonAutoScroll = (Button) view.findViewById(R.id.buttonAutoScroll);
        mButtonAutoScroll.setText("No scroll");
        mButtonClear = (Button) view.findViewById(R.id.buttonClear);
        mButtonStartStop = (Button) view.findViewById(R.id.buttonStartStop);
        mButtonStartStop.setText("Start");
        mTextView = (TextView) view.findViewById(R.id.Text);

        mButtonAutoScroll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mAutoScoll = ! mAutoScoll;
                mButtonAutoScroll.setText(mAutoScoll ? "No scroll" : "Scroll");
            }
        });

        mButtonClear.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mTextView.setText("");
                Log.i("Logs cleared");
            }
        });
        
        mButtonStartStop.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mLogsThread == null ) {
                    mLogsThread = new LogsThread();
                    mThread = new Thread(mLogsThread);
                    mThread.start();
                    mButtonStartStop.setText("Stop");
                    writeLine("Starting acquisition...");
                } else {
                    mLogsThread.stop();
                    mLogsThread = null;
                    mThread = null;
                    mButtonStartStop.setText("Start");
                    writeLine("Stopping acquisition...");
                }
            }
        });
        
        return view;
    }
    
    @Override
    public void onDestroyView() {
        if (mLogsThread != null ) {
            mLogsThread.stop();
            mLogsThread = null;
            mThread = null;
        }
        super.onDestroyView();
    }

    private void writeLine(String line) {
        final String msg = line + LINE_SEPARATOR;
        
        mMainLooper.post(new Runnable() {
            public void run() {
                mTextView.append(msg);
                if (mAutoScoll) {
                    mScrollView.fullScroll(ScrollView.FOCUS_DOWN); 
                }
            } 
        }); 
    }
    
    class LogsThread implements Runnable {
        Logs mLogs;
        boolean mStop;
        final Thread mLogCatThread;
        List<String> mLines = new ArrayList<String>();
        
        public LogsThread() {
            mStop = false;
            mLogCatThread = new Thread(new Runnable() {
                public void run() {
                    List<String> list = Arrays.asList("AndroidRuntime:E", "gtalksms:V");
                    ArrayList<String> commandLine = new ArrayList<String>();
                    commandLine.add("logcat");//$NON-NLS-1$
                    commandLine.addAll(list);
                   
                    try {
                        Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                       
                        String line;
                        while (!mStop && (line = bufferedReader.readLine()) != null) {
                            if (line.toLowerCase().contains("gtalksms")) {
                                mIsAvailable.acquire();
                                mLines.add(line);
                                mIsAvailable.release();
                            }
                        }
                    } catch (Exception e) {
                        writeLine(e.getLocalizedMessage());
                    }
                } 
            });
            mLogCatThread.start();
        }
        
        public void stop() {
            mStop = true;
        }
        
        public void run() {
            try {
                int size;
                // Initial Log
                do {
                    size = mLines.size();
                    Thread.sleep(1000);
                } while (size == 0 || mLines.size() > size);
                
                // Shrink initial log
                mIsAvailable.acquire();
                mLines = mLines.subList(Math.max(0, mLines.size() - 10), mLines.size());
                mIsAvailable.release();
                
                // Manage live logs
                while (!mStop) {
                    mIsAvailable.acquire();
                    for (String mLine : mLines) {
                        writeLine(mLine);
                    }
                    mLines.clear();
                    mIsAvailable.release();
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                writeLine(e.getLocalizedMessage());
            }
        }
    }
}