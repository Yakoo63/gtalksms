package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.databases.AliasDatabase;

/**
 * XMPP Frontend command class for the alias feature
 * This Class handles the inserting/updating/deleting of aliases
 * via XMPP Commands
 * 
 * @author Florian Schmaus fschmaus@gmail.com - 
 *
 */
public class AliasCmd extends Command {
    private AliasDatabase db;
    
    public AliasCmd(MainService mainService) {
        super(mainService, new String[] { "alias" });
        this.db = new AliasDatabase(mainService.getBaseContext());
    }

    @Override
    public void execute(String cmd, String args) {
        int sepPos = args.indexOf(":");
        if (sepPos == -1) {
            /* no subcommand given */
            return;
        } else {
            String[] subCommand = splitArgs(args);  // TODO
        }

    }

    @Override
    public String[] help() {
        // TODO Auto-generated method stub
        return null;
    }

}
