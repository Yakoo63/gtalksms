package com.googlecode.gtalksms.cmd;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
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
    private PhoneManager _phoneMgr;
    private PhoneCallListener _phoneListener = null;
    private TelephonyManager _telephonyMgr = null;
    private ContactsResolver mContactsResolver = null;
        
    public CallCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_CONTACTS, new Cmd("calls"), new Cmd("dial"), new Cmd("ignore"), new Cmd("reject"));
        _phoneMgr = new PhoneManager(sContext);
        _telephonyMgr = (TelephonyManager) mainService.getSystemService(Context.TELEPHONY_SERVICE);
        mContactsResolver = ContactsResolver.getInstance(sContext);
        
        setup();
    }
    
    public void setup() {
        if (sSettingsMgr.notifyIncomingCalls && !sListenerActive) {
            if (_phoneListener == null) {
                _phoneListener = new PhoneCallListener(sMainService);
            }
            _telephonyMgr.listen(_phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            sListenerActive = true;
        }
    }
    
    @Override
    protected void execute(String cmd, String args) {
        if (cmd.equals("dial")) {
            dial(args);
        } else if (cmd.equals("calls")) {
            readCallLogs(args);
        } else if (cmd.equals("ignore")) {
            ignoreIncomingCall();
        } else if (cmd.equals("reject")) {
            rejectIncomingCall();
        }
    }

    /** reads last Call Logs from all contacts */
    private void readCallLogs(String args) {

        ArrayList<Call> arrayList = _phoneMgr.getPhoneLogs();
        XmppMsg all = new XmppMsg();
        int callLogsNumber;
        if (args.equals("")) {
            callLogsNumber = sSettingsMgr.callLogsNumber;
        } else {
            try {
            callLogsNumber = Integer.parseInt(args);
            } catch (Exception e) {
                callLogsNumber = sSettingsMgr.callLogsNumber;
            }
        }
        
        List<Call> callList = Tools.getLastElements(arrayList, callLogsNumber);
        if (callList.size() > 0) {
            for (Call call : callList) {
                all.appendItalic(call.date.toLocaleString());
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
    private void dial(String contactInformation) {
        ResolvedContact resolvedContact = mContactsResolver.resolveContact(contactInformation, ContactsResolver.TYPE_ALL);
        if (resolvedContact == null) {
            send(R.string.chat_no_match_for, contactInformation);
        } else if (resolvedContact.isDistinct()) {
            send(R.string.chat_dial, resolvedContact.getName() + " (" + resolvedContact.getNumber() + ")");
            if (!_phoneMgr.Dial(resolvedContact.getNumber())) {
                send(R.string.chat_error_dial);
            }
        } else if (!resolvedContact.isDistinct()) {
            askForMoreDetails(resolvedContact.getCandidates());
        }
    }
    
    @Override
    public void cleanUp() {
        if (_phoneListener != null) {
            _telephonyMgr.listen(_phoneListener, 0);
            sListenerActive = false;
        }
    }

    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_calls, makeBold("\"calls:#count#\"")),
                getString(R.string.chat_help_dial, makeBold("\"dial:#contact#\"")) 
                };
        return s;
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
        com.android.internal.telephony.ITelephony telephonyService = null;
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
}
