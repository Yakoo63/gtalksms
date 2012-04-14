package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;

public class CommandTemplate extends CommandHandlerBase {

    public CommandTemplate(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, new Cmd("cmd1", "cmd1alias"), new Cmd("cms2","cmd2alias"));
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
    protected void initializeSubCommands() {
        Cmd cmd = mCommandMap.get("cmd");
        cmd.setHelp(0, null);
        cmd.AddSubCmd("subCmd", 0, null);
    }
}
