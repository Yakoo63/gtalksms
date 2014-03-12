package com.googlecode.gtalksms.cmd.cameraCmd;

import java.io.File;
import java.util.GregorianCalendar;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;

abstract class ExtendedPictureCallback implements PictureCallback {
    private final File path;
    final Context ctx;
    
    /**
     * 
     * @param path the path were the picture will be saved in
     */
    ExtendedPictureCallback(File path, Context ctx) {
        this.path = path;
        this.ctx = ctx;
    }
    
    public void onPictureTaken(byte[] data, Camera camera) {
        File filename = new File(path, Tools.getFileFormat(GregorianCalendar.getInstance()) + ".jpg");

        if (Tools.writeFile(data, filename)) {
            onPictureSaved(filename);
        } else {
            Log.e("Error writing file");
        }
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.unlock();
        camera.release();
    }
    
    protected abstract boolean onPictureSaved(File picture);
}
