package com.googlecode.gtalksms.cmd;

import android.app.Service;
import android.text.ClipboardManager;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;

public class ClipboardCmd extends Command {
    ClipboardManager _clipboardMgr;
    
    public ClipboardCmd(MainService mainService) {
        super(mainService, new String[] {"copy"}, Command.TYPE_COPY);
        _clipboardMgr = (ClipboardManager) mainService.getSystemService(Service.CLIPBOARD_SERVICE);
    }
    
    @Override
    protected void execute(String cmd, String text) {
        try {
            if (text.length() > 0) {
                _clipboardMgr.setText(text);
                send(R.string.chat_text_copied);
            } else {
                send(R.string.chat_clipboard, _clipboardMgr.getText());
            }
        } catch (Exception ex) {
            Log.w(Tools.LOG_TAG, "Clipboard error", ex);
            send(R.string.chat_error_clipboard);
        }
    }
    
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_copy, makeBold("\"copy:#text#\""))
                };
        return s;
    }

}
