package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.MainService;

public class EmailCallback extends ExtendedPictureCallback {

    private final String[] mRecipient;
    
    public EmailCallback(File path, Context ctx, String[] recipient) {
        super(path, ctx);
        mRecipient = recipient;
    }

    @Override
    protected boolean onPictureSaved(File picture) {
        Intent i = new Intent(MainService.ACTION_COMMAND);
        i.setClass(ctx, MainService.class);
        i.putExtra("from", mRecipient);
        i.putExtra("cmd", "emailfile");
        i.putExtra("args", ":GTalkSMS:" + picture.getAbsolutePath());
        MainService.sendToServiceHandler(i);
        return true;
    }
}
