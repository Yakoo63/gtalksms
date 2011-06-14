package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;

public class CommandTemplate extends CommandHandlerBase {

    public CommandTemplate(MainService mainService) {
        super(mainService, new String[] {"command1", "command2"}, CommandHandlerBase.TYPE_SYSTEM);
        // TODO if your command needs references, init them here
    }

    protected void execute(String cmd, String args) {
       // TODO Start here
        if (args.equals("Your command arg")) {
            // TODO do something useful
        } else {
            send("Unkown argument \"" + args + "\" for command \"" + cmd + "\"");
        }
    }
    
    @Override
    public String[] help() {
        return null;
    }

}
