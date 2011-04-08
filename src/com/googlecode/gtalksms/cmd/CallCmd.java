package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.data.phone.Call;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.data.phone.PhoneManager;
import com.googlecode.gtalksms.databases.AliasHelper;
import com.googlecode.gtalksms.receivers.PhoneCallListener;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class CallCmd extends CommandHandlerBase {
    private PhoneManager _phoneMgr;
    private PhoneCallListener _phoneListener = null;
    private TelephonyManager _telephonyMgr = null;
    
    private AliasHelper _aliasHelper;
    
    public CallCmd(MainService mainService) {
        super(mainService, new String[] {"calls", "dial"}, CommandHandlerBase.TYPE_CONTACTS);
        _phoneMgr = new PhoneManager(_context);
        _telephonyMgr = (TelephonyManager) mainService.getSystemService(Context.TELEPHONY_SERVICE);
        
        if (_settingsMgr.notifyIncomingCalls) {
            _phoneListener = new PhoneCallListener(mainService);
            _telephonyMgr.listen(_phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
        _aliasHelper = AliasHelper.getAliasHelper(mainService.getBaseContext());
    }
    
    @Override
    protected void execute(String cmd, String args) {
        if (cmd.equals("dial")) {
            dial(args);
        } else if (cmd.equals("calls")) {
            readCallLogs(args);
        }  
    }

    /** reads last Call Logs from all contacts */
    private void readCallLogs(String args) {

        ArrayList<Call> arrayList = _phoneMgr.getPhoneLogs();
        XmppMsg all = new XmppMsg();
        int callLogsNumber;
        if (args.equals("")) {
            callLogsNumber = _settingsMgr.callLogsNumber;
        } else {
            try {
            callLogsNumber = Integer.parseInt(args);
            } catch (Exception e) {
                callLogsNumber = _settingsMgr.callLogsNumber;
            }
        }
        
        List<Call> callList = Tools.getLastElements(arrayList, callLogsNumber);
        if (callList.size() > 0) {
            for (Call call : callList) {
                all.appendItalic(call.date.toLocaleString());
                all.append(" - ");
                all.appendBold(ContactsManager.getContactName(_context, call.phoneNumber));
                all.appendLine(" - " + call.type(_context) + getString(R.string.chat_call_duration) + call.duration());
            }
        } else {
            all.appendLine(getString(R.string.chat_no_call));
        }
        send(all);
    }
    
    /** dial the specified contact */
    private void dial(String contactInfo) {
        String number = null;
        String contact = null;
        contactInfo = _aliasHelper.convertAliasToNumber(contactInfo);
        
        if (Phone.isCellPhoneNumber(contactInfo)) {
            number = contactInfo;
            contact = ContactsManager.getContactName(_context, number);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(_context, contactInfo);
            if (mobilePhones.size() > 1) {
                XmppMsg phones = new XmppMsg(getString(R.string.chat_specify_details));
                phones.newLine();
                for (Phone phone : mobilePhones) {
                    phones.appendLine(phone.contactName + " - " + phone.cleanNumber);
                }
                send(phones);
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                contact = phone.contactName;
                number = phone.cleanNumber;
            } else {
                send(R.string.chat_no_match_for, contactInfo);
            }
        }

        if (number != null) {
            send(R.string.chat_dial, contact + " (" + number + ")");
            if (!_phoneMgr.Dial(number)) {
                send(R.string.chat_error_dial);
            }
        }
    }
    
    @Override
    public void cleanUp() {
        if (_phoneListener != null) {
            _telephonyMgr.listen(_phoneListener, 0);
            _phoneListener = null;
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
}
