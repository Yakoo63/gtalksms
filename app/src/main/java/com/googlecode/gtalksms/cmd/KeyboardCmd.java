package com.googlecode.gtalksms.cmd;

import com.googlecode.gtalksms.services.KeyboardInputMethodService;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class KeyboardCmd extends CommandHandlerBase {
    public KeyboardCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_COPY, "Keyboard", new Cmd("write", "w"), new Cmd("wappend", "wa"), new Cmd("wsend", "ws", "wl"));
    }
    
    @Override
    protected void execute(Command cmd) {
        KeyboardInputMethodService keyboard = sMainService.getKeyboard();
        String msg = cmd.getAllArg1().replace("\\n", "\n");
        
        if (keyboard != null) {
            if (isMatchingCmd(cmd, "write")) {
                keyboard.setText(msg);
            } else if (isMatchingCmd(cmd, "wappend")) {
                keyboard.setText(keyboard.getText() + msg);
            } else if (isMatchingCmd(cmd, "wsend")) {
                if (msg.length() > 0) {
                    keyboard.setText(msg);
                }
                keyboard.sendDefaultEditorAction(false);
            }
        }
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }
    
    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("write").setHelp(R.string.chat_help_write, "#text#");   
        mCommandMap.get("wappend").setHelp(R.string.chat_help_write_append, "#text#");   
        mCommandMap.get("wsend").setHelp(R.string.chat_help_write_send, "#text#");   
    }
}
