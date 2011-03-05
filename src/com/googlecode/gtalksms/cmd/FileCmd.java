package com.googlecode.gtalksms.cmd;

import java.io.File;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;

public class FileCmd extends Command {
    private XmppManager _xmppMgr;

    
    public FileCmd(MainService mainService) {
        super(mainService, new String[] {"send"});
        _xmppMgr = _mainService.getXmppmanager();
    }
    
    @Override
    public void execute(String cmd, String args) {
        if (new File(args).exists()) {
            _xmppMgr.sendFile(args);
        } else {
            send("File '" + args + "' doesn't exist!" );
        }
    }
    
    @Override
    public String[] help() {
        return null;
    }

}
