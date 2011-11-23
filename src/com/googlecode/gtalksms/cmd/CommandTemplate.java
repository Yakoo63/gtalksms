package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;

public class CommandTemplate extends CommandHandlerBase {

    public CommandTemplate(MainService mainService) {
        super(mainService, new String[] {"command1", "command2"}, CommandHandlerBase.TYPE_SYSTEM);
        // TODO if your command needs references, init them here
    }

    protected void execute(String cmd, String args) {        
       // TODO Start here        
        String[] sArgs = splitArgs(args);
        if (cmd.equals("Your command")) {
            // TODO do something usefull
            if (sArgs[0].equals("firstArgument")) {
                somethingUsefull();
            }
        } else {
            send("Unkown argument \"" + args + "\" for command \"" + cmd + "\"");
        }
    }
    
    private void somethingUsefull() {
          return;
    }
    
    @Override
    public String[] help() {
        return null;
    }

}
