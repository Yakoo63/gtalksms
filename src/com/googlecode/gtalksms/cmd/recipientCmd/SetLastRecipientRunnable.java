package com.googlecode.gtalksms.cmd.recipientCmd;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.RecipientCmd;


public class SetLastRecipientRunnable implements Runnable {

    public static final int sleepTime = 10;
    
    private RecipientCmd mRecipientCmd;
    private SettingsManager mSettings;
    private boolean outdated;  // avoiding atomic boolean, because there is only one setter
    private String number;
    private static final int sleepTimeMs = sleepTime * 1000;
    
    public SetLastRecipientRunnable(RecipientCmd recpientCmd, String number, SettingsManager settings) {
        this.mRecipientCmd = recpientCmd;
        this.number = number;
        this.outdated = false;
    }
    
    @Override
    public void run() {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            /* Ignore - we don't send interrupts to this thread */
        }
        if (!outdated)
            mRecipientCmd.setLastRecipientNow(number, mSettings.dontDisplayRecipient);

    }
    
    public void setOutdated() {
        outdated = true;
    }
    
    public boolean isOutdated() {
        return outdated;
    }
}
