package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;

public class ToastCmd extends CommandHandlerBase {

    public ToastCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_INTERNAL, "Toast", new Cmd("toast"));
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }

    @Override
    protected void execute(Command cmd) {
        String args = cmd.getAllArg1();
        if (isMatchingCmd(cmd, "toast") && !args.equals("")) {
            MainService.displayToast(args, null, false);
        }
    }
    
    @Override
    protected void initializeSubCommands() {
    }
}
