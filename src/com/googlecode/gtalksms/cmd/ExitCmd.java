package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;


public class ExitCmd extends CommandHandlerBase {

    public ExitCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_SYSTEM, new Cmd("exit", "quit"));
    }

    @Override
    protected void execute(String cmd, String args) {
       sMainService.stopSelf();
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_stop, makeBold("\"exit\""))
            };
        return s;
    }

}
