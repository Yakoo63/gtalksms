package com.googlecode.gtalksms.panels;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.googlecode.gtalksms.R;
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
    final int MAX_LOG_MESSAGE_LENGTH = 100000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showDialog(0);
    }

    protected Dialog onCreateDialog(int id) {
        return new AlertDialog.Builder(this)
        .setTitle(getString(R.string.app_name))
        .setIcon(android.R.drawable.ic_dialog_info)
        // we don't really want these turning up directly on the gtalksms mailing lists, so
        // don't default a 'to' address and tell the user to send it to themself.
        .setMessage(getString(R.string.log_panel_collect))
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
                collectAndSendLog();
            }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();
    }

    private void sendLog(String logString) {
        try {
            Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "GTalkSMS log");
            //emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {"me@whereever.com"});
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, logString);
            emailIntent.setType("text/plain");
            startActivity(emailIntent);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    // from android-log-collector.
    @SuppressWarnings("unchecked")
    void collectAndSendLog() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("-v");
        list.add("time");
        // filters.
        list.add("AndroidRuntime:E");
        list.add("gtalksms:V");
        list.add("*:S");
        mCollectLogTask = (CollectLogTask) new CollectLogTask().execute(list);
    }
   
    private class CollectLogTask extends AsyncTask<ArrayList<String>, Void, StringBuilder>{
        @Override
        protected void onPreExecute(){
            showProgressDialog(getString(R.string.log_panel_acquiring));
        }

        @Override
        protected StringBuilder doInBackground(ArrayList<String>... params){
            final StringBuilder log = new StringBuilder();
            try{
                ArrayList<String> commandLine = new ArrayList<String>();
                commandLine.add("logcat");//$NON-NLS-1$
                commandLine.add("-d");//$NON-NLS-1$
                ArrayList<String> arguments = ((params != null) && (params.length > 0)) ? params[0] : null;
                if (null != arguments){
                    commandLine.addAll(arguments);
                }

                Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
               
                String line;
                while ((line = bufferedReader.readLine()) != null){
                    log.append(line);
                    log.append(LINE_SEPARATOR);
                }
            }
            catch (IOException e){
                Log.e(Tools.LOG_TAG, "CollectLogTask.doInBackground failed", e);//$NON-NLS-1$
            }
            return log;
        }

        @Override
        protected void onPostExecute(StringBuilder log){
            if (null != log){
                //truncate if necessary
                int keepOffset = Math.max(log.length() - MAX_LOG_MESSAGE_LENGTH, 0);
                if (keepOffset > 0){
                    log.delete(0, keepOffset);
                }

                log.insert(0, LINE_SEPARATOR);
                log.insert(0, LINE_SEPARATOR);
                log.insert(0, "Kernel info: " + getFormattedKernelVersion());
                log.insert(0, LINE_SEPARATOR);
                log.insert(0, "Version: " + getVersionNumber(LogCollector.this));

                sendLog(log.toString());
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
        .setTitle(getString(R.string.app_name))
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
                cancellCollectTask();
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
   
    void cancellCollectTask(){
        if (mCollectLogTask != null && mCollectLogTask.getStatus() == AsyncTask.Status.RUNNING)
        {
            mCollectLogTask.cancel(true);
            mCollectLogTask = null;
        }
    }
   
    @Override
    protected void onPause(){
        cancellCollectTask();
        dismissProgressDialog();
       
        super.onPause();
    }
   
    private static String getVersionNumber(Context context)
    {
        String version = "?";
        try
        {
            PackageInfo packagInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packagInfo.versionName;
        }
        catch (PackageManager.NameNotFoundException e){};
       
        return version;
    }
   
    private String getFormattedKernelVersion()
    {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                "\\w+\\s+" + /* ignore: Linux */
                "\\w+\\s+" + /* ignore: version */
                "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
                "\\([^)]+\\)\\s+" + /* ignore: (gcc ..) */
                "([^\\s]+)\\s+" + /* group 3: #26 */
                "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.e(Tools.LOG_TAG, "Regex did not match on /proc/version: " + procVersionStr);
            } else if (m.groupCount() < 4) {
                Log.e(Tools.LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {  
            Log.e(Tools.LOG_TAG, "IO Exception when getting kernel version for Device Info screen", e);
        }
        return getString(R.string.chat_log_unavailable);
    }
}
