package com.googlecode.gtalksms.panels;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Logs;
import com.googlecode.gtalksms.tools.Tools;

// From android-log-collector - http://code.google.com/p/android-log-collector
// (sadly I failed to use android-log-collector itself, so cloned some of it here)
/*
 * Copyright (C) 2009 Xtralogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class LogCollector extends Activity {
    public final static String LINE_SEPARATOR = System.getProperty("line.separator");
    private ProgressDialog mProgressDialog;
    private CollectLogTask mCollectLogTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
   
        new AlertDialog.Builder(this)
        .setTitle(Tools.APP_NAME)
        .setIcon(android.R.drawable.ic_dialog_info)
        .setMessage(getString(R.string.log_panel_collect))
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                collectAndSendLog();
            }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                finish();
            }
        })
        .create()
        .show();
    }

    private void sendLog(String logString) {
        try {
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Tools.APP_NAME + " log - Issue 000");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {"gtalksms-logs@googlegroups.com"});
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, logString);
            emailIntent.setType("text/plain");
            startActivity(emailIntent);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    // from android-log-collector.
    void collectAndSendLog() {
        mCollectLogTask = (CollectLogTask) new CollectLogTask().execute();
    }
   
    private class CollectLogTask extends AsyncTask<Void, Void, String>{
        @Override
        protected void onPreExecute(){
            showProgressDialog(getString(R.string.log_panel_acquiring));
        }

        @Override
        protected String doInBackground(Void... params){
            return new Logs(true).getLogs(LogCollector.this, 2000);
        }

        @Override
        protected void onPostExecute(String log){
            if (null != log){
                sendLog(log);
                dismissProgressDialog();
                finish();
            }
            else{
                dismissProgressDialog();
                showErrorDialog(getString(R.string.chat_log_failed));
            }
        }
    }
   
    void showErrorDialog(String errorMessage){
        new AlertDialog.Builder(this)
        .setTitle(Tools.APP_NAME)
        .setMessage(errorMessage)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                finish();
            }
        })
        .show();
    }
   
    void showProgressDialog(String message){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(message);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
            public void onCancel(DialogInterface dialog){
                cancelCollectTask();
                finish();
            }
        });
        mProgressDialog.show();
    }
   
    private void dismissProgressDialog(){
        if (null != mProgressDialog && mProgressDialog.isShowing())
        {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
   
    void cancelCollectTask(){
        if (mCollectLogTask != null && mCollectLogTask.getStatus() == AsyncTask.Status.RUNNING)
        {
            mCollectLogTask.cancel(true);
            mCollectLogTask = null;
        }
    }
   
    @Override
    protected void onPause(){
        cancelCollectTask();
        dismissProgressDialog();
       
        super.onPause();
    }
}
