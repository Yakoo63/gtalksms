package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;

public class XMPPTransferCallback extends ExtentedPictureCallback {

    public XMPPTransferCallback(CommandHandlerBase cmd, File path, Context ctx, String recipient) {
        super(path, ctx, recipient);
    }

    @Override
    protected boolean onPictureSaved(File picture) {
        Intent i = new Intent(MainService.ACTION_COMMAND);
        i.setClass(ctx, MainService.class);
        i.putExtra("from", recipient);
        i.putExtra("cmd", "send");
        i.putExtra("args", picture.getAbsolutePath());
        ctx.startService(i);
        return true;
    }
}
