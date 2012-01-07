package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.KeyboardInputMethod;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class KeyboardCmd extends CommandHandlerBase {
    public KeyboardCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_COPY, new Cmd("write", "w"));
    }
    
    @Override
    protected void execute(String cmd, String args) {
        KeyboardInputMethod keyboard = sMainService.getKeyboard();
        
        if (keyboard != null) {
            keyboard.setText(args);
        }
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_write, makeBold("\"write:#text#\""), makeBold("\"w:#text#\""))
                };
        return s;
    }
}
