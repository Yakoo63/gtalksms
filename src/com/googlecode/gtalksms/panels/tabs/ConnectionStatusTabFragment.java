package com.googlecode.gtalksms.panels.tabs;

import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class ConnectionStatusTabFragment extends SherlockFragment {

    private Button mSendPingButton;
    private ImageView mPingStatus;
    private TextView mPingTime;
    private TextView mPingDate;
    
    private MainService mMainService;
    
    private PingMyServerAsyncTask mPingMyServerAsyncTask;

    final Handler mPingStatusHandler = new Handler() {
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
    
    public void onResume() {
        super.onResume();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_connection_status, container,
                false);

        mSendPingButton = (Button) view.findViewById(R.id.sendPing);
        mPingStatus = (ImageView) view.findViewById(R.id.pingState);
        mPingTime = (TextView) view.findViewById(R.id.pingTime);
        mPingDate = (TextView) view.findViewById(R.id.pingDate);
        
        mPingStatus.setImageResource(R.drawable.icon_red);

        mSendPingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mPingMyServerAsyncTask != null) {
                    AsyncTask.Status status = mPingMyServerAsyncTask.getStatus();
                    switch (status) {
                    case PENDING:
                    case RUNNING:
                        // no action if there is already an AsyncTask running
                        return;
                    default:
                        break;
                    }
                }
                
                PingManager pingManager = maybeGetPingManager();
                if (pingManager == null) {
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
        
        long lastPing = pingManager.getLastSuccessfulPing();
        Date date = new Date(lastPing);
        String timeStr = SimpleDateFormat.getTimeInstance().format(date);
        String dateStr = SimpleDateFormat.getDateInstance().format(date);
        mPingTime.setText(timeStr);
        mPingDate.setText(dateStr);
    }
    
    private class PingMyServerAsyncTask extends AsyncTask<PingManager, Void, Boolean> {

        @Override
        protected Boolean doInBackground(PingManager... params) {
            if (params.length != 1) {
                return false;
            }
            
            PingManager pingManager = params[0];
            Boolean res = pingManager.pingMyServer();
            
            return res;
        }
        
        protected void onPostExecute(Boolean res) {
            Message msg = mPingStatusHandler.obtainMessage();
            if (res) {
                msg.arg1 = 1;
            } else {
                msg.arg1 = 0;
            }
            mPingStatusHandler.sendMessage(msg);
        }
    }
}
