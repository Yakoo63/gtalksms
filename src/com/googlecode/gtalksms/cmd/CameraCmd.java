package com.googlecode.gtalksms.cmd;

import java.io.File;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.cameraCmd.EmailCallback;
import com.googlecode.gtalksms.cmd.cameraCmd.XMPPTransferCallback;
import com.googlecode.gtalksms.tools.Tools;

public class CameraCmd extends CommandHandlerBase {
    
    private Camera _camera = null;
    private static File _path;
    private String emailReceiving;
    
    public CameraCmd(MainService mainService) {
        super(mainService, new String[] {"camera"}, CommandHandlerBase.TYPE_SYSTEM);
        
        SettingsManager settings = SettingsManager.getSettingsManager(_context);
        if (settings.backupAgentAvailable) {  // API Level >= 8 check
            _path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        } else {
            _path = Environment.getExternalStorageDirectory();
        }
        emailReceiving = settings.notifiedAddress;
        try {
            File repository = new File(_path, Tools.APP_NAME);
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
            PictureCallback pictureCallback;
            
            try {
                _camera = Camera.open();
                SurfaceView view = new SurfaceView(_context);
                _camera.setPreviewDisplay(view.getHolder());
                _camera.startPreview();
                
                if (args.equals("xmpp")) {
                    pictureCallback = new XMPPTransferCallback(this, _path, _context, _answerTo);
                } else {
                    pictureCallback = new EmailCallback(this, _path, _context, emailReceiving);
                }
                _camera.takePicture(null, null, pictureCallback);
            } catch (Exception e) {
                send("error while getting picture: " + e);
            }
        } 
    }
       
    
    @Override
    public synchronized void cleanUp() {
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
