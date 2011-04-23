package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;

import android.content.Context;

import com.googlecode.gtalksms.cmd.CommandHandlerBase;
import com.googlecode.gtalksms.tools.Tools;

public class VoidCallback extends ExtentedPictureCallback {

    CommandHandlerBase _command;
    
    public VoidCallback(File path, Context ctx, String recipient) {
        super(path, ctx, recipient);
    }

    @Override
    protected boolean onPictureSaved(File picture) {
        Tools.send("Photo saved as " + picture.getAbsolutePath(), recipient, ctx);
        return true;
    }
}
