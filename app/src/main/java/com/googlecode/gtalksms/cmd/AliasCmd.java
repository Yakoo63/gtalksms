package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.databases.AliasHelper;
import com.googlecode.gtalksms.xmpp.XmppMsg;

/**
 * XMPP Frontend command class for the alias feature This Class handles the
 * inserting/updating/deleting of aliases via XMPP Commands
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 * 
 */
public class AliasCmd extends CommandHandlerBase {
    private AliasHelper mAliasHelper;

    public AliasCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_CONTACTS, "Alias", new Cmd("alias"));
    }

    @Override
    protected void onCommandActivated() {
        mAliasHelper = AliasHelper.getAliasHelper(sContext);
    }

    @Override
    protected void onCommandDeactivated() {
        mAliasHelper = null;
    }

    @Override
    protected void initializeSubCommands() {
        Cmd alias = mCommandMap.get("alias");
        alias.setHelp(R.string.chat_help_alias_general, null);
        
        alias.AddSubCmd("add", R.string.chat_help_alias_add, "#aliasname#:#contact#");
        alias.AddSubCmd("show",R.string.chat_help_alias_show, "#aliasname#");
        alias.AddSubCmd("del",R.string.chat_help_alias_del, "#aliasname#");
    }
    
    @Override
    protected void execute(Command cmd) {
        String subCmd = cmd.getArg1();
        if (subCmd.equals("add")) {
            add(cmd.getArg1(), cmd.getArg2(), cmd.getAllArg(3));
        } else if (subCmd.equals("del")) {
            del(cmd.getArg1(), cmd.getArg2());
        } else if (subCmd.equals("show")) {
            show(cmd.getArg1(), cmd.getArg2());
        } else {
            sendHelp();
        }
    }
    
    private void add(String command, String arg1, String arg2) {
        if (arg1.equals("") || arg2.equals("")) {
            send(R.string.chat_error_more_arguments, command);
        } else if (Phone.isCellPhoneNumber(arg2)) {
                if (mAliasHelper.addAliasByNumber(arg1, arg2)) {
                    send(R.string.chat_alias_add_by_number, arg1, arg2);
                } else {
                    send(R.string.chat_alias_invalid_char);
                }
            } else {
                ArrayList<Phone> res = mAliasHelper.addAliasByName(arg1, arg2);
                if (res == null) {
                    send(R.string.chat_alias_invalid_char);
                } else if (res.size() != 1) {
                    send(R.string.chat_error_unknown_name);
                } else {
                    Phone p = res.get(0);
                    send(R.string.chat_alias_add_by_name, arg1, p.getContactName(), p.getNumber());
                }
            }
    }
    
    private void del(String command, String arg1) {
        if (arg1.equals("")) {
            send(R.string.chat_error_more_arguments, command);
        } else if (mAliasHelper.deleteAlias(arg1)) {
            send(R.string.chat_alias_del_suc, arg1);
        } else {
            send(R.string.chat_alias_del_fail, arg1);
        }
    }
    
    private void show(String command, String arg1) {
        if (arg1.equals("")) {
            send(R.string.chat_error_more_arguments, command);
        } else if (arg1.equals("all")) {
            String[][] aliases = mAliasHelper.getAllAliases();
            if (aliases == null) {
                send(R.string.chat_alias_empty);
            } else {
                XmppMsg msg = new XmppMsg();
                for (String[] alias : aliases) {
                    msg.appendBold("Alias: " + alias[0] + " ");
                    if (alias[2] == null) {
                        msg.appendLine(alias[1]);
                    } else {
                        msg.append(alias[1] + " - ");
                        msg.appendLine(alias[2]);
                    }
                }
                send(msg);
            }
        } else {
            String[] res = mAliasHelper.getAliasOrNull(arg1);
            if (res == null) {
                send(R.string.chat_alias_show_non_existent, arg1);
            } else if (res.length == 2) {
                send("\'" + res[0] + "\' -> " + res[1]);
            } else {
                send("\'" + res[0] + "\' - " + res[2] + " -> " + res[1]);
            }
        }
    }
}
