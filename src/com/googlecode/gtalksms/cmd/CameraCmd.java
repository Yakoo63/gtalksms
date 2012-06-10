package com.googlecode.gtalksms.cmd;

import java.io.File;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.cameraCmd.EmailCallback;
import com.googlecode.gtalksms.cmd.cameraCmd.VoidCallback;
import com.googlecode.gtalksms.cmd.cameraCmd.XMPPTransferCallback;
import com.googlecode.gtalksms.tools.Tools;

public class CameraCmd extends CommandHandlerBase {
    
    private static final int VOID_CALLBACK = 0;
    private static final int XMPP_CALLBACK = 1;
    private static final int EMAIL_CALLBACK = 2;

    private static AudioManager audioManager;
    private static WindowManager windowManager;
    private static Camera sCamera = null;
    private static File repository;
    private static String[] emailReceiving;
    private static int streamVolume;
    private static int cameraId = 0;
    
    public CameraCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, new Cmd("camera", "photo"), new Cmd("flash", "light"));
        File path;
        
        windowManager = (WindowManager) sMainService.getSystemService(Context.WINDOW_SERVICE);
        audioManager = (AudioManager) mainService.getSystemService(Context.AUDIO_SERVICE);
        
        SettingsManager settings = SettingsManager.getSettingsManager(sContext);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {  // API Level >= 8 check
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        } else {
            path = new File(Environment.getExternalStorageDirectory(), "DCIM");
        }
        emailReceiving = settings.getNotifiedAddresses();
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
        if (isMatchingCmd("camera", cmd)) {
            if (args.equals("") || splitedArgs[0].equals("")) {
                takePicture(VOID_CALLBACK);
            } else if (splitedArgs[0].equals("email")) {
                takePicture(EMAIL_CALLBACK);
            } else if (splitedArgs[0].equals("xmpp")) {
                takePicture(XMPP_CALLBACK);
            } else if (splitedArgs[0].equals("list")) {
                listCameras();
            } else if (splitedArgs[0].equals("set") && splitedArgs.length > 1) {
                setCamera(splitedArgs[1]);
            }           
        } 
        else if (isMatchingCmd("flash", cmd)) {
            if (args.equals("") || splitedArgs[0].equals("on")) {
                setLight(true);
            } else {
                setLight(false);
            }
        }
    }
    
    private void setCamera(String arg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Integer id = Tools.parseInt(arg);
            if (id == null || id < 0 || id >= Camera.getNumberOfCameras()) {
                listCameras();
            } else {
                cameraId = id.intValue(); 
                CameraInfo info = new CameraInfo(); 
                Camera.getCameraInfo(cameraId, info);
                
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    send(R.string.chat_camera_back_activated);
                } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    send(R.string.chat_camera_front_activated);
                }    
            }                
        } else {
            Log.w(Tools.LOG_TAG, "Android version doesn't allow setCamera command.");
            send(R.string.chat_camera_error_version);
        }
    }

    private void listCameras() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StringBuilder res = new StringBuilder();
            for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
                CameraInfo info = new CameraInfo(); 
                Camera.getCameraInfo(i, info);
                
                res.append(i);
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    res.append(sContext.getString(R.string.chat_camera_back) + Tools.LineSep);
                } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    res.append(sContext.getString(R.string.chat_camera_front) + Tools.LineSep);
                }    
            }
            send(res.toString());
        } else {
            Log.w(Tools.LOG_TAG, "Android version doesn't allow listCamera command.");
            send(R.string.chat_camera_error_version);
        }
    }
    
    private int getCameraOrientation() {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
    
    private void takePicture(int pCallbackMethod) {
        cleanUp();
        PictureCallback pictureCallback;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                sCamera = Camera.open(cameraId);
                sCamera.setDisplayOrientation(getCameraOrientation());
            } else {
                sCamera = Camera.open();
            }
            SurfaceView view = new SurfaceView(sContext);
            sCamera.setPreviewDisplay(view.getHolder());
            sCamera.startPreview();
            
            switch (pCallbackMethod) {
            case XMPP_CALLBACK:
                pictureCallback = new XMPPTransferCallback(repository, sContext, mAnswerTo);
                break;
            case EMAIL_CALLBACK:
                pictureCallback = new EmailCallback(repository, sContext, emailReceiving);
                break;
            case VOID_CALLBACK:
            default:
                pictureCallback = new VoidCallback(repository, sContext, mAnswerTo);
            }
            
            streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM); 
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0); 
            
            Camera.ShutterCallback cb = new Camera.ShutterCallback() {
                
                @Override
                public void onShutter() {
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, streamVolume, 0); 
                }
            };

            sCamera.takePicture(cb, null, pictureCallback);
        } catch (Exception e) {
            send(R.string.chat_camera_error_picture, e.getLocalizedMessage());
            cleanUp();
        }
    }
    
    public void setLight(boolean turnOn) {
        if (sCamera == null) {
            sCamera = Camera.open();
        }
        Parameters params = sCamera.getParameters();
        params.setFlashMode(turnOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        sCamera.setParameters(params);
        
        if (!turnOn) {
            cleanUp();
        }
    }
       
    @Override
    public synchronized void cleanUp() {
        if (sCamera != null) {
            try {
                sCamera.stopPreview();
                sCamera.setPreviewCallback(null);
                sCamera.unlock();
                sCamera.release();
            } catch (Exception e) {
                Log.e(Tools.LOG_TAG, "Failed to release Camera", e);
            }
            sCamera = null;
        }
    }

    @Override
    protected void initializeSubCommands() {
        Cmd cam = mCommandMap.get("camera");
        cam.setHelp(R.string.chat_help_camera, null);
        cam.AddSubCmd("email", R.string.chat_help_camera_email, null);
        cam.AddSubCmd("xmpp", R.string.chat_help_camera_xmpp, null);
        cam.AddSubCmd("list", R.string.chat_help_camera_list, null);
        cam.AddSubCmd("set", R.string.chat_help_camera_set, "#number#");
        Cmd flash = mCommandMap.get("flash");
        flash.setHelp(R.string.chat_help_flash_on, null);
        flash.AddSubCmd("on", R.string.chat_help_flash_on, null);
        flash.AddSubCmd("off", R.string.chat_help_flash_off, null);
    }
}
