package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;

import android.content.Context;

import com.googlecode.gtalksms.cmd.CommandHandlerBase;

public class VoidCallback extends ExtentedPictureCallback {

    CommandHandlerBase _command;
    
    public VoidCallback(CommandHandlerBase cmd, File path, Context ctx, String recipient) {
        super(path, ctx, recipient);
        _command = cmd;
    }

    @Override
    protected boolean onPictureSaved(File picture) {
        _command.send("Photo saved in " + picture.getAbsolutePath());
        return true;
    }
}
