package com.googlecode.gtalksms.panels.tabs;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.ping.PingManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class ConnectionStatusTabFragment extends SherlockFragment {

    private Button mSendPingButton;
    private Button mRefreshMemoryUsageButton;
    private ImageView mPingStatus;
    private TextView mPingTime;
    private TextView mMemoryUsage;
    private TextView mPingDate;
    
    private volatile MainService mMainService;
    
    private PingMyServerAsyncTask mPingMyServerAsyncTask;

    private final Handler mPingStatusHandler = new Handler() {
        public void handleMessage(Message msg) {
            int successful = msg.arg1;
            if (successful > 0) { 
                updateLastPingTime();
                mPingStatus.setImageResource(R.drawable.icon_green);
            } else {
                mPingStatus.setImageResource(R.drawable.icon_red);
            }
        }
    };
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_connection_status, container, false);

        mSendPingButton = (Button) view.findViewById(R.id.sendPing);
        mRefreshMemoryUsageButton = (Button) view.findViewById(R.id.refreshMemoryUsage);
        mPingStatus = (ImageView) view.findViewById(R.id.pingState);
        mPingTime = (TextView) view.findViewById(R.id.pingTime);
        mPingDate = (TextView) view.findViewById(R.id.pingDate);
        mMemoryUsage = (TextView) view.findViewById(R.id.memoryUsage);
        
        mPingStatus.setImageResource(R.drawable.icon_red);

        mRefreshMemoryUsageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d("Send ping button pressed");
                mMemoryUsage.setText(Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
            }
        });

        mSendPingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Log.d("Send ping button pressed");
                if (mPingMyServerAsyncTask != null) {
                    AsyncTask.Status status = mPingMyServerAsyncTask.getStatus();
                    switch (status) {
                        case PENDING:
                        case RUNNING:
                            mPingMyServerAsyncTask.cancel(true);
                        default:
                            break;
                    }
                }

                PingManager pingManager = maybeGetPingManager();
                if (pingManager == null) {
                    Log.d("pingManager was null when send ping button was pressed");
                    return;
                }

                mPingStatus.setImageResource(R.drawable.icon_orange);
                mPingMyServerAsyncTask = new PingMyServerAsyncTask();
                mPingMyServerAsyncTask.execute(pingManager);
            }
        });
        
        return view;
    }
    
    public void setMainService(MainService mainService) {
        this.mMainService = mainService;
    }
    
    public void unsetMainService()  {
        this.mMainService = null;
    }
    
    private PingManager maybeGetPingManager() {
        if (mMainService == null) {
            Log.d("maybeGetPingManager: MainService was null");
            return null;
        } else {
            return mMainService.getPingManager();
        }
    }
    
    private void updateLastPingTime() {
        PingManager pingManager = maybeGetPingManager();
        if (pingManager == null) {
            return;
        }
        
        long lastPing = pingManager.getLastReceivedPong();
        Date date = new Date(lastPing);
        mPingTime.setText(SimpleDateFormat.getTimeInstance().format(date));
        mPingDate.setText(SimpleDateFormat.getDateInstance().format(date));
    }
    
    private class PingMyServerAsyncTask extends AsyncTask<PingManager, Void, Boolean> {

        @Override
        protected Boolean doInBackground(PingManager... params) {
            if (params.length != 1) {
                return false;
            }
            
            PingManager pingManager = params[0];
            Log.d("Issuing pingMyServer in PingMyServerAsyncTask");
            Boolean res = null;
            try {
                res = pingManager.pingMyServer();
            } catch (SmackException.NotConnectedException e) {
                res = false;
            }
            Log.d("Ping result was " + res);
            
            return res;
        }
        
        protected void onPostExecute(Boolean res) {
            Message msg = mPingStatusHandler.obtainMessage();
            msg.arg1 = res ? 1 : 0;
            mPingStatusHandler.sendMessage(msg);
        }
    }
}
