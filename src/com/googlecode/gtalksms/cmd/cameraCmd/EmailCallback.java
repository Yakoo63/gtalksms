package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.util.Log;

import com.googlecode.gtalksms.cmd.CameraCmd;
import com.googlecode.gtalksms.tools.Tools;

public class EmailCallback implements PictureCallback {

    private File path;
    private Context ctx;
    private CameraCmd cameraCmd;
    private String recipient;

    public EmailCallback(CameraCmd cameraCmd, File path, Context ctx, String recipient) {
        this.cameraCmd = cameraCmd;
        this.path = path;
        this.ctx = ctx;
        this.recipient = recipient;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        File filename = new File(path, "photo-" + new Date().getDate() + ".jpg");
        cameraCmd.cleanUp();

        if (Tools.writeFile(data, filename)) {
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "GTalkSMS Picture");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "GTalkSMS Picture");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { recipient });
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(filename));
            emailIntent.setType("image/jpeg");
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(emailIntent);
        } else {
            Log.e(Tools.LOG_TAG, "Error writing file");
        }
    }
}
