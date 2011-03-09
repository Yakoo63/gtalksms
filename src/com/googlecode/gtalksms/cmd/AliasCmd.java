package com.googlecode.gtalksms.cmd;

import java.util.ArrayList;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.data.phone.Phone;
import com.googlecode.gtalksms.tools.AliasHelper;
import com.googlecode.gtalksms.xmpp.XmppMsg;

/**
 * XMPP Frontend command class for the alias feature This Class handles the
 * inserting/updating/deleting of aliases via XMPP Commands
 * 
 * @author Florian Schmaus fschmaus@gmail.com - on behalf of the GTalkSMS Team
 * 
 */
public class AliasCmd extends Command {
    private AliasHelper aliasHelper;

    public AliasCmd(MainService mainService) {
        super(mainService, new String[] { "alias" });
        this.aliasHelper = mainService.createAndGetAliasHelper();
    }

    @Override
    protected void execute(String cmd, String args) {
        int sepPos = args.indexOf(":");
        if (sepPos == -1) {
            send("Alias needs more arguments");
        } else {
            String[] subCommand = splitArgs(args);
            if (subCommand[0].equals("add")) {
                if (subCommand.length == 1) {
                    send("No name or number given");
                } else if (Phone.isCellPhoneNumber(subCommand[2])) {
                    aliasHelper.addAliasByNumber(subCommand[1], subCommand[2]);
                } else {
                    ArrayList<Phone> res = aliasHelper.addAliasByName(subCommand[1], subCommand[2]);
                    if (res.size() != 1) {
                        send("Contact name not distinct or unkown");
                    } else {
                        Phone p = res.get(0);
                        send("Added alias \'" + subCommand[1] + "\' for \'" + p.contactName + "\' with number " + p.number);
                    }
                }
            } else if (subCommand[0].equals("del")) {
                if (aliasHelper.deleteAlias(subCommand[1])) {
                    send("Deleted Alias: " + subCommand[1]);
                } else {
                    send("Failed to delete Alias: " + subCommand[1]);
                }
            } else if (subCommand[0].equals("show")) {
                if (subCommand[1].equals("all")) {
                    XmppMsg msg = new XmppMsg();
                    String[][] aliases = aliasHelper.getAllAliases();
                    for (int i = 0; i < aliases.length; i++) {
                        msg.appendBold("Alias: " + aliases[i][0] + " ");
                        if (aliases[i][2] == null) {
                            msg.appendLine(aliases[i][1]);
                        } else {
                            msg.append(aliases[i][1] + " - ");
                            msg.appendLine(aliases[i][2]);
                        }
                    }
                    send(msg);
                }
            }
        }
    }

    @Override
    public String[] help() {
        // TODO Auto-generated method stub
        return null;
    }

}
