package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class HelpCmd extends Command {

    private XmppMsg _msg = new XmppMsg();
    
    public HelpCmd(MainService mainService) {
        super(mainService);
        
        _msg.appendLine(getString(R.string.chat_help_title));
        _msg.appendLine(getString(R.string.chat_help_help, get("\"?\""), get("\"help\"")));
        _msg.appendLine(getString(R.string.chat_help_stop, get("\"exit\"")));
        _msg.appendLine(getString(R.string.chat_help_dial, get("\"dial:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_reply, get("\"reply:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_show_all, get("\"sms\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_show_unread, get("\"sms:unread\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_show_contact, get("\"sms:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_send, get("\"sms:#contact#:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_chat, get("\"chat:#contact#")));
        _msg.appendLine(getString(R.string.chat_help_find_sms_all, get("\"findsms:#message#\""), get("\"fs:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_find_sms, get("\"findsms:#contact#:#message#\""), get("\"fs:#contact#:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_mark_as_read, get("\"markAsRead:#contact#\""), get("\"mar\"")));
        _msg.appendLine(getString(R.string.chat_help_battery, get("\"battery\""), get("\"batt\"")));
        _msg.appendLine(getString(R.string.chat_help_calls, get("\"calls\"")));
        _msg.appendLine(getString(R.string.chat_help_contact, get("\"contact:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_geo, get("\"geo:#address#\"")));
        _msg.appendLine(getString(R.string.chat_help_where, get("\"where\""), get("\"stop\"")));
        _msg.appendLine(getString(R.string.chat_help_ring, get("\"ring\""), get("\"ring:[0-100]\""), get("\"stop\"")));
        _msg.appendLine(getString(R.string.chat_help_ringmode, get("\"ringmode:#mode#\"")));        
        _msg.appendLine(getString(R.string.chat_help_copy, get("\"copy:#text#\"")));
        _msg.appendLine(getString(R.string.chat_help_cmd, get("\"cmd:#command#\"")));
        _msg.appendLine(getString(R.string.chat_help_write, get("\"write:#text#\""), get("\"w:#text#\"")));
        _msg.appendLine(getString(R.string.chat_help_urls, get("\"http\"")));
    }

    @Override
    public void execute(String cmd, String args) {
        send(_msg);
    }
 
    private String get(String msg) {
        return XmppMsg.makeBold(msg);
    }
}
