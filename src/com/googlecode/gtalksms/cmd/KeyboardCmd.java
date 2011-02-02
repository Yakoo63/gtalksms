package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.KeyboardInputMethod;
import com.googlecode.gtalksms.MainService;

public class KeyboardCmd extends Command {

    public KeyboardCmd(MainService mainService) {
        super(mainService);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void execute(String cmd, String args) {
        KeyboardInputMethod keyboard = _mainService.getKeyboard();
        
        if (keyboard != null) {
            keyboard.setText(args);
        }
    }

}
