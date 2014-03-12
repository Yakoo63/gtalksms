package com.googlecode.gtalksms.receivers;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;

public class PhoneCallListener extends PhoneStateListener {
    public PhoneCallListener(MainService svc) {
        super();
        this.svc = svc;
        settingsMgr = SettingsManager.getSettingsManager(svc);
        mAudioMgr = (AudioManager) svc.getSystemService(Context.AUDIO_SERVICE);
        mReconnectHandler = new Handler(svc.getServiceLooper());
    }

    private final MainService svc;
    private final SettingsManager settingsMgr;
    private final AudioManager mAudioMgr;
    private final Handler mReconnectHandler;

    private boolean mIsSpeakerOn;
    private boolean mIsModifiedSettings = false;
    private boolean mEnableSpeakerOnTheNextCall = false;

    // Android seems to send the intent not only once per call
    // but every 10 seconds for ongoing ringing
    // we prevent multiple "is calling" notifications with this boolean
    private static boolean manageIncoming = true;

    /**
     * Enable the phone speaker on the next out/ingoing call
     */
    public void enableSpeakerOnTheNextCall() {
        mEnableSpeakerOnTheNextCall = true;
    }

    /**
     * Runnable to backup the speaker state and enabled it after
     */
    private final Runnable mReconnectRunnable = new Runnable() {
        public void run() {
            if (mAudioMgr != null) {
                if (!mIsModifiedSettings) {
                    mIsSpeakerOn = mAudioMgr.isSpeakerphoneOn();
                    mIsModifiedSettings = true;
                }

                mAudioMgr.setSpeakerphoneOn(true);
            }
        }
    };

    /**
     * incomingNumber is null when the caller ID is hidden
     * 
     * @param state
     * @param incomingNumber
     */
    public void onCallStateChanged(int state, String incomingNumber) {
        Log.d("onCallStateChanged, state=" + state);
        if (MainService.IsRunning) {
            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                manageIncoming = true;
                mReconnectHandler.removeCallbacks(mReconnectRunnable);

                if (mIsModifiedSettings) {
                    mAudioMgr.setSpeakerphoneOn(mIsSpeakerOn);
                    mIsModifiedSettings = false;
                }

                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                // One shot to avoid enabling the speaker on every phone calls
                if (mEnableSpeakerOnTheNextCall) {
                    mEnableSpeakerOnTheNextCall = false;

                    // Adding a 1s delay before turning on the speaker due to the Android implementation
                    // The speaker is automatically disabled on outgoing call right after this event
                    Log.d("Setting speaker on in 1s, state=" + state);
                    mReconnectHandler.postDelayed(mReconnectRunnable, 1000);
                }
                manageIncoming = true;
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                Log.d("PhoneCallListener Call State Ringing with incomingNumber=" + incomingNumber + " manageIncoming=" + manageIncoming);

                if (manageIncoming) {
                    manageIncoming = false;
                    String contact = ContactsManager.getContactName(svc, incomingNumber);
                    svc.send(svc.getString(R.string.chat_is_calling, contact), null);
                }
                break;
            default:
                break;
            }
        }
    }
}
