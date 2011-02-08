package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class HelpCmd extends Command {
    private final String[] commands = {"?", "help"};

    private XmppMsg _msg = new XmppMsg();
    
    public HelpCmd(MainService mainService) {
        super(mainService);
        
        _msg.appendLine(getString(R.string.chat_help_title));
        _msg.appendLine(getString(R.string.chat_help_help, makeBold("\"?\""), makeBold("\"help\"")));
        _msg.appendLine(getString(R.string.chat_help_stop, makeBold("\"exit\"")));
        _msg.appendLine(getString(R.string.chat_help_dial, makeBold("\"dial:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_reply, makeBold("\"reply:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_show_all, makeBold("\"sms\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_show_unread, makeBold("\"sms:unread\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_show_contact, makeBold("\"sms:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_send, makeBold("\"sms:#contact#:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_sms_chat, makeBold("\"chat:#contact#")));
        _msg.appendLine(getString(R.string.chat_help_find_sms_all, makeBold("\"findsms:#message#\""), makeBold("\"fs:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_find_sms, makeBold("\"findsms:#contact#:#message#\""), makeBold("\"fs:#contact#:#message#\"")));
        _msg.appendLine(getString(R.string.chat_help_mark_as_read, makeBold("\"markAsRead:#contact#\""), makeBold("\"mar\"")));
        _msg.appendLine(getString(R.string.chat_help_del_sms_all, makeBold("\"delsms:all\"")));
        _msg.appendLine(getString(R.string.chat_help_del_sms_sent, makeBold("\"delsms:sent\"")));
        _msg.appendLine(getString(R.string.chat_help_del_sms_contact, makeBold("\"delsms:contact:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_battery, makeBold("\"battery\""), makeBold("\"batt\"")));
        _msg.appendLine(getString(R.string.chat_help_calls, makeBold("\"calls\"")));
        _msg.appendLine(getString(R.string.chat_help_contact, makeBold("\"contact:#contact#\"")));
        _msg.appendLine(getString(R.string.chat_help_geo, makeBold("\"geo:#address#\"")));
        _msg.appendLine(getString(R.string.chat_help_where, makeBold("\"where\""), makeBold("\"stop\"")));
        _msg.appendLine(getString(R.string.chat_help_ring, makeBold("\"ring\""), makeBold("\"ring:[0-100]\""), makeBold("\"stop\"")));
        _msg.appendLine(getString(R.string.chat_help_ringmode, makeBold("\"ringmode:#mode#\"")));        
        _msg.appendLine(getString(R.string.chat_help_copy, makeBold("\"copy:#text#\"")));
        _msg.appendLine(getString(R.string.chat_help_cmd, makeBold("\"cmd:#command#\"")));
        _msg.appendLine(getString(R.string.chat_help_write, makeBold("\"write:#text#\""), makeBold("\"w:#text#\"")));
        _msg.appendLine(getString(R.string.chat_help_urls, makeBold("\"http\"")));
    }
    
    public String[] getCommands() {
        return commands;
    }
    
    @Override
    public void execute(String cmd, String args) {
        send(_msg);
    }
 
    private String makeBold(String msg) {
        return XmppMsg.makeBold(msg);
    }
}
