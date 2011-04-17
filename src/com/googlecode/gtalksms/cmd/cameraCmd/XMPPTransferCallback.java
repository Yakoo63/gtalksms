package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.cmd.CameraCmd;
import com.googlecode.gtalksms.tools.Tools;

public class XMPPTransferCallback implements PictureCallback {
    private File path;
    private Context ctx;
    private CameraCmd cameraCmd;
    private String recipient;

    public XMPPTransferCallback(CameraCmd cameraCmd, File path, Context ctx, String recipient) {
        this.cameraCmd = cameraCmd;
        this.path = path;
        this.ctx = ctx;
        this.recipient = recipient;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        File filename = new File(path, "photo-" + new Date().getTime() + ".jpg");
        cameraCmd.cleanUp();

        if (Tools.writeFile(data, filename)) {
            Intent i = new Intent(MainService.ACTION_COMMAND);
            i.setClass(ctx, MainService.class);
            i.putExtra("from", recipient);
            i.putExtra("cmd", "send");
            i.putExtra("args", filename.getAbsolutePath());
            ctx.startService(i);
        } else {
            Log.e(Tools.LOG_TAG, "Error writing file");
        }
    }
}
