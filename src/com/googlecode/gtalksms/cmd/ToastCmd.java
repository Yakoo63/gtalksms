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
        if (isMatchingCmd(cmd, "toast") && !cmd.getAllArguments().equals("")) {
            MainService.displayToast(cmd.getAllArguments(), null, false);
        }
    }
    
    @Override
    protected void initializeSubCommands() {
    }
}
