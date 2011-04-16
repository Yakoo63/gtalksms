package com.googlecode.gtalksms.cmd;

import java.io.File;
import java.util.Date;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceView;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.Tools;

public class CameraCmd extends CommandHandlerBase {
    
    Camera _camera = null;
    private final String _path = "/sdcard/DCIM/GTalkSMS";
    
    public CameraCmd(MainService mainService) {
        super(mainService, new String[] {"camera"}, CommandHandlerBase.TYPE_SYSTEM);
        
        try {
            File repository = new File(_path);
            if(!repository.exists()) {
                repository.mkdirs();
            }
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "Failed to create repository.", e);
        }
    }
    
    @Override
    protected void execute(String cmd, String args) {
        if (cmd.equals("camera")) {
            cleanUp();
            
            try {
                _camera = Camera.open();
                SurfaceView view = new SurfaceView(_context);
                _camera.setPreviewDisplay(view.getHolder());
                _camera.startPreview();

                _camera.takePicture(null, null, jpegCallback);
            } catch (Exception e) {
                send("error while getting picture: " + e);
            }
        } 
    }
    
    PictureCallback jpegCallback = new PictureCallback() {
        
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            
            String filename = _path + "/photo-" + new Date().getTime() + ".jpg";
            Tools.writeFile(data, filename);
            cleanUp();
             
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "GTalkSMS Picture");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "GTalkSMS Picture");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {_settingsMgr.notifiedAddress});
                emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filename)));  
                emailIntent.setType("image/jpeg");  
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _context.startActivity(emailIntent);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };
    
    @Override
    public void cleanUp() {
        if (_camera != null) {
            try {
                _camera.release();
            } catch (Exception e) {
                Log.e(Tools.LOG_TAG, "Failed to release Camera", e);
            }
            _camera = null;
        }
    }

    @Override
    public String[] help() {
        return null;
    }
}
