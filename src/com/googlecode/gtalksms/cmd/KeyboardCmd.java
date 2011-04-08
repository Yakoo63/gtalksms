package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.KeyboardInputMethod;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class KeyboardCmd extends CommandHandlerBase {
    public KeyboardCmd(MainService mainService) {
        super(mainService, new String[] {"write", "w"}, CommandHandlerBase.TYPE_COPY);
    }
    
    @Override
    protected void execute(String cmd, String args) {
        KeyboardInputMethod keyboard = _mainService.getKeyboard();
        
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
