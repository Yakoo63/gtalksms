package com.googlecode.gtalksms.cmd;

import java.util.Map;
import java.util.Set;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class HelpCmd extends Command {
    private XmppMsg _msg;  // brief help message
    private XmppMsg _msgAll;   // full help message    
    private Map<String, Command> commands;
    
    public HelpCmd(MainService mainService) {
        super(mainService, new String[] {"?", "help"});
        commands = mainService.getCommands();
        Set<Command> commandSet = mainService.getCommandSet();
        _msg = new XmppMsg();
        _msgAll = new XmppMsg();
        
        _msg.appendLine(getString(R.string.chat_help_title));
        _msg.appendLine(getString(R.string.chat_help_help, makeBold("\"?\""), makeBold("\"help\"")));
        _msg.appendLine(getString(R.string.chat_help_help_all, makeBold("\"help:all\"")));
              
        _msgAll.append(_msg);
        for (Command c : commandSet) {
            String[] helpLines = c.help();
            if (helpLines != null)
                addLinesToMsg(_msgAll, helpLines);
        }    
        
        _msg.appendLine("- " + makeBold("\"help:#command#\""));
    }

    @Override
    protected void execute(String cmd, String args) {
        if (args.equals("all")) {
            send(_msgAll);
        } else if (commands.containsKey(args)) {
            String[] helpLines = commands.get(args).help();
            if (helpLines != null) {
                XmppMsg helpMsg = new XmppMsg();
                addLinesToMsg(helpMsg, helpLines);
                send(helpMsg);
            }
        } else {
            send(_msg);
        }
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_battery, makeBold("\"battery\""), makeBold("\"batt\"")) 
                };
        return s;
    }
    
    private static void addLinesToMsg(XmppMsg msg, String[] lines) {
        if (lines == null) return;
        for (String line : lines) 
            msg.appendLine(line);
    }
}
