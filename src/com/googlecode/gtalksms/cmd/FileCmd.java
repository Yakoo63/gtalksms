package com.googlecode.gtalksms.cmd;

import java.io.File;

import com.googlecode.gtalksms.MainService;

public class FileCmd extends Command {

    public FileCmd(MainService mainService) {
        super(mainService);
    }

    @Override
    public void execute(String cmd, String args) {
        
        if (new File(args).exists()) {
            _mainService.sendFile(args);
        } else {
            send("File '" + args + "' doesn't exist!" );
        }
    }

}
