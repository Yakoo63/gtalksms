package com.googlecode.gtalksms.cmd.smsCmd;

import com.googlecode.gtalksms.cmd.SmsCmd;

public class SetLastRecipientRunnable implements Runnable {

    public static final int sleepTime = 10;
    
    private SmsCmd smsCmd;
    private boolean outdated;  // avoiding atomic boolean, because there is only one setter
    private String number;
    private static final int sleepTimeMs = sleepTime * 1000;
    
    public SetLastRecipientRunnable(SmsCmd smsCmd, String number) {
        this.smsCmd = smsCmd;
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
            smsCmd.setLastRecipientNow(number, false);

    }
    
    public void setOutdated() {
        outdated = true;
    }
    
    public boolean isOutdated() {
        return outdated;
    }
    

}
