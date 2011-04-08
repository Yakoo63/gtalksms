package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;


public class ExitCmd extends CommandHandlerBase {

    public ExitCmd(MainService mainService) {
        super(mainService, new String[] {"exit"}, CommandHandlerBase.TYPE_SYSTEM);
    }

    @Override
    protected void execute(String cmd, String args) {
       _mainService.stopSelf();
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_stop, makeBold("\"exit\""))
                };
        return s;
    }

}
