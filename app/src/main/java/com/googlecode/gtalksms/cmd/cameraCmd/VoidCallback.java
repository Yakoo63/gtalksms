package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;

import android.content.Context;

import com.googlecode.gtalksms.tools.Tools;

public class VoidCallback extends ExtendedPictureCallback {

    private final String mRecipient;
    
    public VoidCallback(File path, Context ctx, String recipient) {
        super(path, ctx);
        mRecipient = recipient;
    }

    @Override
    protected boolean onPictureSaved(File picture) {
        Tools.send("Photo saved as " + picture.getAbsolutePath(), mRecipient, ctx);
        return true;
    }
}
