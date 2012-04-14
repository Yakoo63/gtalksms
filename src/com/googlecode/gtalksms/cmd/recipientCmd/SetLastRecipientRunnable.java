package com.googlecode.gtalksms.cmd.recipientCmd;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.RecipientCmd;


public class SetLastRecipientRunnable implements Runnable {

    public static final int sleepTime = 10;
    
    private RecipientCmd mRecipientCmd;
    private SettingsManager mSettings;
    private boolean mIsOutdated;  // avoiding atomic boolean, because there is only one setter
    private String mNumber;
    private static final int sleepTimeMs = sleepTime * 1000;
    
    public SetLastRecipientRunnable(RecipientCmd recpientCmd, String number, SettingsManager settings) {
        mRecipientCmd = recpientCmd;
        mSettings = settings;
        mNumber = number;
        mIsOutdated = false;
    }
    
    @Override
    public void run() {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            /* Ignore - we don't send interrupts to this thread */
        }
        if (!mIsOutdated) {
            mRecipientCmd.setLastRecipientNow(mNumber, mSettings.dontDisplayRecipient);
        }
    }
    
    public void setOutdated() {
        mIsOutdated = true;
    }
    
    public boolean isOutdated() {
        return mIsOutdated;
    }
}
