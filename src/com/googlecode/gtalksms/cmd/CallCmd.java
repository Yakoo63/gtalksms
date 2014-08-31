package com.googlecode.gtalksms.cmd;

import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.contacts.ContactsResolver;
import com.googlecode.gtalksms.data.contacts.ResolvedContact;
import com.googlecode.gtalksms.data.phone.Call;
import com.googlecode.gtalksms.data.phone.PhoneManager;
import com.googlecode.gtalksms.receivers.PhoneCallListener;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

import com.android.internal.telephony.ITelephony;

public class CallCmd extends CommandHandlerBase {
    private static boolean sListenerActive = false;
    private PhoneManager mPhoneMgr = null;
    private AudioManager mAudioMgr = null;
    private PhoneCallListener mPhoneListener = null;
    private TelephonyManager mTelephonyMgr = null;
    private ContactsResolver mContactsResolver = null;
        
    public CallCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_CONTACTS, "Phone", new Cmd("phone", "p"));
    }

    @Override
    protected void onCommandActivated() {
        mPhoneMgr = new PhoneManager(sContext);
        mTelephonyMgr = (TelephonyManager) sMainService.getSystemService(Context.TELEPHONY_SERVICE);
        mAudioMgr = (AudioManager) sMainService.getSystemService(Context.AUDIO_SERVICE);
        mContactsResolver = ContactsResolver.getInstance(sContext);

        if (sSettingsMgr.notifyIncomingCalls && !sListenerActive) {
            if (mPhoneListener == null) {
                mPhoneListener = new PhoneCallListener(sMainService);
            }
            mTelephonyMgr.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            sListenerActive = true;
        }
    }

    @Override
    protected void onCommandDeactivated() {
        if (mPhoneListener != null && mTelephonyMgr != null) {
            mTelephonyMgr.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
            sListenerActive = false;
        }
        mAudioMgr = null;
        mPhoneMgr = null;
        mTelephonyMgr = null;
        mContactsResolver = null;
    }
    
    @Override
    protected void execute(Command cmd) {
        String subCmd = cmd.getArg1();
        if (subCmd.equals("speaker")) {
            setSpeaker(cmd.getArg2());
        } else if (subCmd.equals("dial")) {
            dial(cmd.getArg2(), false, false);
        } else if (subCmd.equals("call")) {
            dial(cmd.getArg2(), true, false);
        } else if (subCmd.equals("callspeaker")) {
            dial(cmd.getArg2(), true, true);
        } else if (subCmd.equals("hangout")) {
            hangUp();
        } else if (subCmd.equals("calls")) {
            readCallLogs(cmd.getArg2());
        } else if (subCmd.equals("ignore")) {
            ignoreIncomingCall();
        } else if (subCmd.equals("reject")) {
            rejectIncomingCall();
        } else {
            executeNewCmd("?", "phone");
        }
    }

    /** reads last Call Logs from all contacts */
    private void readCallLogs(String args) {

        ArrayList<Call> arrayList = mPhoneMgr.getPhoneLogs();
        XmppMsg all = new XmppMsg();
        int callLogsNumber = Tools.parseInt(args, 10);
        List<Call> callList = Tools.getLastElements(arrayList, callLogsNumber);
        if (callList.size() > 0) {
            for (Call call : callList) {
                all.appendItalic(DateFormat.getDateTimeInstance().format(call.date));
                all.append(" - ");
                all.appendBold(ContactsManager.getContactName(sContext, call.phoneNumber));
                all.appendLine(" - " + call.type(sContext) + getString(R.string.chat_call_duration) + call.duration());
            }
        } else {
            all.appendLine(getString(R.string.chat_no_call));
        }
        send(all);
    }
    
    /** dial the specified contact */
    private void dial(String contactInformation, boolean makeTheCall, boolean usePhoneSpeaker) {
        if (contactInformation.equals("")) {
            String lastRecipient = RecipientCmd.getLastRecipientNumber();
            String lastRecipientName = RecipientCmd.getLastRecipientName();
            if (lastRecipient != null) {
                doDial(lastRecipientName, lastRecipient, makeTheCall, usePhoneSpeaker);
            } else {
                // TODO l18n
                send("error: last recipient not set");
            }
        } else {
            ResolvedContact resolvedContact = mContactsResolver.resolveContact(contactInformation, ContactsResolver.TYPE_ALL);
            if (resolvedContact == null) {
                send(R.string.chat_no_match_for, contactInformation);
            } else if (resolvedContact.isDistinct()) {
                doDial(resolvedContact.getName(), resolvedContact.getNumber(), makeTheCall, usePhoneSpeaker);
            } else if (!resolvedContact.isDistinct()) {
                askForMoreDetails(resolvedContact.getCandidates());
            }
        }
    }

    private void setSpeaker(String arg) {
        if (arg.equals("on")) {
            mAudioMgr.setSpeakerphoneOn(true);
        } else if (arg.equals("off")) {
            mAudioMgr.setSpeakerphoneOn(false);
        } else {
            send("Speaker state is " + (mAudioMgr.isSpeakerphoneOn() ? "on" : "off"));
        }
    }

    private void doDial(String name, String number, boolean makeTheCall, boolean usePhoneSpeaker) {
        if (number != null) {
            send(R.string.chat_dial, name + " (" + number + ")");
        } else {
            send(R.string.chat_dial, name);
        }

        if (usePhoneSpeaker) {
            mPhoneListener.enableSpeakerOnTheNextCall();
        }

        // check if the dial is successful
        if (!mPhoneMgr.Dial(number, makeTheCall)) {
            send(R.string.chat_error_dial);
        }
    }

    /**
     * Ends the current call and send a notification.
     * Doesn't throw if there is no pending call
     */
    private void hangUp() {
        send("Hanging up");
        getTelephonyService().endCall();
    }

    /**
     * Rejects an incoming call
     * 
     * @return true if an call was rejected, otherwise false 
     */
    private boolean rejectIncomingCall() {
        send("Rejecting incoming Call");
        ITelephony ts = getTelephonyService();
        return ts.endCall();
    }
    
    /**
     * Ignores an incoming call
     */
    // TODO does not work atm gives:
    // Exception: Neither user 10081 nor current process has android.permission.MODIFY_PHONE_STATE.
    // Although the permission is in the manifest
    private void ignoreIncomingCall() {
        send("Ignoring incoming call");
        ITelephony ts = getTelephonyService();
        ts.silenceRinger();
    }
    
    private ITelephony getTelephonyService() {
        TelephonyManager tm = (TelephonyManager) sContext.getSystemService(Context.TELEPHONY_SERVICE);
        com.android.internal.telephony.ITelephony telephonyService;
        try {
            Class<?> c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            telephonyService = (ITelephony) m.invoke(tm);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return telephonyService;
    }

    @Override
    protected void initializeSubCommands() {
        Cmd cmd = mCommandMap.get("phone");
        cmd.setHelp(R.string.chat_help_phone, null);
        cmd.AddSubCmd("calls", R.string.chat_help_phone_calls, "#count#");
        cmd.AddSubCmd("call", R.string.chat_help_phone_call, "#contact#");
        cmd.AddSubCmd("callspeaker", R.string.chat_help_phone_callspeaker, "#contact#");
        cmd.AddSubCmd("dial", R.string.chat_help_phone_dial, "#contact#");
        cmd.AddSubCmd("hangup", R.string.chat_help_phone_hangup);
        cmd.AddSubCmd("ignore", R.string.chat_help_phone_ignore);
        cmd.AddSubCmd("reject", R.string.chat_help_phone_reject);
        cmd.AddSubCmd("speaker", R.string.chat_help_phone_speaker);
        cmd.AddSubCmd("speaker", R.string.chat_help_phone_speaker_set, "[on|off]");
    }
}
