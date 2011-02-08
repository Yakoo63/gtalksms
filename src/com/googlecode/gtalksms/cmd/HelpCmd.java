package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class HelpCmd extends Command {
    private XmppMsg _msg = new XmppMsg();
    private XmppMsg _msgAll = new XmppMsg();
    private XmppMsg _msgSms = new XmppMsg();
    private XmppMsg _msgContact = new XmppMsg();
    private XmppMsg _msgGeo = new XmppMsg();
    private XmppMsg _msgSystem = new XmppMsg();
    private XmppMsg _msgCopy = new XmppMsg();
    
    
    public HelpCmd(MainService mainService) {
        super(mainService, new String[] {"?", "help"});
        
        _msg.appendLine(getString(R.string.chat_help_title));
        _msg.appendLine(getString(R.string.chat_help_help, makeBold("\"?\""), makeBold("\"help\"")));
        _msg.appendLine(getString(R.string.chat_help_help_all, makeBold("\"help:all\"")));
        _msg.appendLine("- " + makeBold("\"help:contact\"") + ": dial, calls, contact");
        _msg.appendLine("- " + makeBold("\"help:sms\"") + ": sms, reply, delsms, chat, markAsRead, findsms");
        _msg.appendLine("- " + makeBold("\"help:geo\"") + ": geo, where, stop");
        _msg.appendLine("- " + makeBold("\"help:system\"") + ": battery, ring, ringmode, cmd, exit");
        _msg.appendLine("- " + makeBold("\"help:copy\"") + ": copy, write, http");
       
        _msgContact.appendLine(getString(R.string.chat_help_calls, makeBold("\"calls\"")));
        _msgContact.appendLine(getString(R.string.chat_help_dial, makeBold("\"dial:#contact#\"")));
        _msgContact.appendLine(getString(R.string.chat_help_contact, makeBold("\"contact:#contact#\"")));
        
        _msgSms.appendLine(getString(R.string.chat_help_sms_reply, makeBold("\"reply:#message#\"")));
        _msgSms.appendLine(getString(R.string.chat_help_sms_show_all, makeBold("\"sms\"")));
        _msgSms.appendLine(getString(R.string.chat_help_sms_show_unread, makeBold("\"sms:unread\"")));
        _msgSms.appendLine(getString(R.string.chat_help_sms_show_contact, makeBold("\"sms:#contact#\"")));
        _msgSms.appendLine(getString(R.string.chat_help_sms_send, makeBold("\"sms:#contact#:#message#\"")));
        _msgSms.appendLine(getString(R.string.chat_help_sms_chat, makeBold("\"chat:#contact#")));
        _msgSms.appendLine(getString(R.string.chat_help_find_sms_all, makeBold("\"findsms:#message#\""), makeBold("\"fs:#message#\"")));
        _msgSms.appendLine(getString(R.string.chat_help_find_sms, makeBold("\"findsms:#contact#:#message#\""), makeBold("\"fs:#contact#:#message#\"")));
        _msgSms.appendLine(getString(R.string.chat_help_mark_as_read, makeBold("\"markAsRead:#contact#\""), makeBold("\"mar\"")));
        _msgSms.appendLine(getString(R.string.chat_help_del_sms_all, makeBold("\"delsms:all\"")));
        _msgSms.appendLine(getString(R.string.chat_help_del_sms_sent, makeBold("\"delsms:sent\"")));
        _msgSms.appendLine(getString(R.string.chat_help_del_sms_contact, makeBold("\"delsms:contact:#contact#\"")));
        
        _msgGeo.appendLine(getString(R.string.chat_help_geo, makeBold("\"geo:#address#\"")));
        _msgGeo.appendLine(getString(R.string.chat_help_where, makeBold("\"where\""), makeBold("\"stop\"")));
        
        _msgSystem.appendLine(getString(R.string.chat_help_battery, makeBold("\"battery\""), makeBold("\"batt\"")));
        _msgSystem.appendLine(getString(R.string.chat_help_ring, makeBold("\"ring\""), makeBold("\"ring:[0-100]\""), makeBold("\"stop\"")));
        _msgSystem.appendLine(getString(R.string.chat_help_ringmode, makeBold("\"ringmode:#mode#\"")));        
        _msgSystem.appendLine(getString(R.string.chat_help_cmd, makeBold("\"cmd:#command#\"")));
        _msgSystem.appendLine(getString(R.string.chat_help_stop, makeBold("\"exit\"")));
        
        _msgCopy.appendLine(getString(R.string.chat_help_copy, makeBold("\"copy:#text#\"")));
        _msgCopy.appendLine(getString(R.string.chat_help_write, makeBold("\"write:#text#\""), makeBold("\"w:#text#\"")));
        _msgCopy.appendLine(getString(R.string.chat_help_urls, makeBold("\"http\"")));
        
        _msgAll.append(_msg).append(_msgContact).append(_msgSms).append(_msgGeo).append(_msgSystem).append(_msgCopy);
        _msgContact.insertLineBegin(getString(R.string.chat_help_title));
        _msgSms.insertLineBegin(getString(R.string.chat_help_title));
        _msgGeo.insertLineBegin(getString(R.string.chat_help_title));
        _msgSystem.insertLineBegin(getString(R.string.chat_help_title));
        _msgCopy.insertLineBegin(getString(R.string.chat_help_title));
    }

    @Override
    public void execute(String cmd, String args) {
        if (args.equals("sms")) {
            send(_msgSms);
        } else if (args.equals("contact")) {
            send(_msgContact);
        } else if (args.equals("geo")) {
            send(_msgGeo);
        } else if (args.equals("system")) {
            send(_msgSystem);
        } else if (args.equals("copy")) {
            send(_msgCopy);
        } else if (args.equals("all")) {
            send(_msgAll);
        } else {
            send(_msg);
        }
    }
 
    private String makeBold(String msg) {
        return XmppMsg.makeBold(msg);
    }
}
