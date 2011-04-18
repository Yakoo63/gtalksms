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
    
    private static final int XMPP_CALLBACK = 1;
    private static final int EMAIL_CALLBACK = 2;
    
    private static Camera camera = null;
    private static File repository;
    private static String emailReceiving;
    private static boolean api9orGreater;
    
    public CameraCmd(MainService mainService) {
        super(mainService, new String[] {"camera", "photo"}, CommandHandlerBase.TYPE_SYSTEM);
        File path;
        
        SettingsManager settings = SettingsManager.getSettingsManager(_context);
        if (settings.api8orGreater) {  // API Level >= 8 check
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        } else {
            path = new File(Environment.getExternalStorageDirectory(), "DCIM");
        }
        emailReceiving = settings.notifiedAddress;
        api9orGreater = settings.api9orGreater;
        try {
            repository = new File(path, Tools.APP_NAME);
            if(!repository.exists()) {
                repository.mkdirs();
            }
        } catch (Exception e) {
            Log.e(Tools.LOG_TAG, "Failed to create repository.", e);
        }
    }
    
    @Override
    protected void execute(String cmd, String args) {
        String[] splitedArgs = splitArgs(args);
        if (cmd.equals("camera") || cmd.equals("photo")) {
            if (splitedArgs[0].equals("") || splitedArgs[0].equals("email")) {
                takePicture(EMAIL_CALLBACK);
            } else if (splitedArgs[0].equals("xmpp")) {
                takePicture(XMPP_CALLBACK);
            } else if (splitedArgs[0].equals("list")) {
                listCameras();
            } else if (splitedArgs[0].equals("set")) {
                setCamera(splitedArgs);
            }           
        } 
    }
    
    private void setCamera(String[] splitedArgs) {
        // TODO does nothing atm, we need API >= 9 for this feature
        if (api9orGreater) {
            // TODO set the camera
        } else {
            // TODO print error message
        }
    }

    private void listCameras() {
        // TODO does nothing atm, we need API >= 9 for this feature
        if (api9orGreater) {
            // TODO list the available cameras
        } else {
            // TODO print error message
        }
    }

    private void takePicture(int pCallbackMethod) {
        cleanUp();
        PictureCallback pictureCallback;
        
        try {
            camera = Camera.open();
            SurfaceView view = new SurfaceView(_context);
            camera.setPreviewDisplay(view.getHolder());
            camera.startPreview();
            
            switch (pCallbackMethod) {
            case XMPP_CALLBACK:
                pictureCallback = new XMPPTransferCallback(repository, _context, _answerTo);
                break;
            case EMAIL_CALLBACK:
            default:
                pictureCallback = new EmailCallback(repository, _context, emailReceiving);
                break;
            }
            
            camera.takePicture(null, null, pictureCallback);
        } catch (Exception e) {
            send("error while getting picture: " + e);
        }
    }
       
    
    @Override
    public synchronized void cleanUp() {
        if (camera != null) {
            try {
                camera.unlock();
                camera.release();
            } catch (Exception e) {
                Log.e(Tools.LOG_TAG, "Failed to release Camera", e);
            }
            camera = null;
        }
    }

    @Override
    public String[] help() {
        return null;
    }
}
