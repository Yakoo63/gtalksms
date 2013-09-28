package com.googlecode.gtalksms.cmd.recipientCmd;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.RecipientCmd;
import com.googlecode.gtalksms.tools.Tools;


public class SetLastRecipientRunnable implements Runnable {

    private static final int sleepTime = 10;
    
    private final RecipientCmd mRecipientCmd;
    private final SettingsManager mSettings;
    private boolean mIsOutdated;  // avoiding atomic boolean, because there is only one setter
    private final String mNumber;
	private final WakeLock mWl;

    private static final int sleepTimeMs = sleepTime * 1000;
    
	public SetLastRecipientRunnable(RecipientCmd recpientCmd, String number, SettingsManager settings, Context ctx) {
        mRecipientCmd = recpientCmd;
        mSettings = settings;
        mNumber = number;
        mIsOutdated = false;
		mWl = ((PowerManager) ctx.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
		        Tools.APP_NAME + "LastRecipientRunnable WakeLock");
    }
    
    @Override
    public void run() {
		mWl.acquire();
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            /* Ignore - we don't send interrupts to this thread */
        }
        if (!mIsOutdated) {
            mRecipientCmd.setLastRecipientNow(mNumber, mSettings.dontDisplayRecipient);
        }
		mWl.release();
    }
    
    public void setOutdated() {
        mIsOutdated = true;
    }
    
    public boolean isOutdated() {
        return mIsOutdated;
    }
}
