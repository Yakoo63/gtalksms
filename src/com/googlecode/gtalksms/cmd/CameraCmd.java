package com.googlecode.gtalksms.cmd;

import java.io.File;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.cameraCmd.EmailCallback;
import com.googlecode.gtalksms.cmd.cameraCmd.VoidCallback;
import com.googlecode.gtalksms.cmd.cameraCmd.XMPPTransferCallback;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;

public class CameraCmd extends CommandHandlerBase {
    
    private static final int VOID_CALLBACK = 0;
    private static final int XMPP_CALLBACK = 1;
    private static final int EMAIL_CALLBACK = 2;

    private AudioManager mAudioManager;
    private WindowManager mWindowManager;
    private Camera mCamera = null;
    private File mRepository;
    private String[] mEmailReceiving;
    private int mStreamVolume;
    private int mCameraId = 0;
    
    public CameraCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MEDIA, "Camera", new Cmd("camera", "photo"), new Cmd("flash", "light"));
    }

    @Override
    protected void onCommandActivated() {
        mWindowManager = (WindowManager) sMainService.getSystemService(Context.WINDOW_SERVICE);
        mAudioManager = (AudioManager) sMainService.getSystemService(Context.AUDIO_SERVICE);
        mEmailReceiving = sSettingsMgr.getNotifiedAddresses().getAll();
        try {
            File path = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ?
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) :
                    new File(Environment.getExternalStorageDirectory(), "DCIM");

            mRepository = new File(path, Tools.APP_NAME);
            if(!mRepository.exists()) {
                mRepository.mkdirs();
            }
        } catch (Exception e) {
            Log.e("Failed to create mRepository.", e);
        }
    }

    @Override
    protected void onCommandDeactivated() {
        releaseResources();

        mWindowManager = null;
        mAudioManager = null;
        mEmailReceiving = null;
        mRepository = null;
    }
    
    private void releaseResources() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.unlock();
                mCamera.release();
            } catch (Exception e) {
                Log.e("Failed to release Camera", e);
            }
            mCamera = null;
        }
    }

    @Override
    protected void execute(Command cmd) {
        String arg1 = cmd.getArg1();
        if (isMatchingCmd(cmd, "camera")) {
            if (arg1.equals("")) {
                takePicture(VOID_CALLBACK);
            } else if (arg1.equals("email")) {
                takePicture(EMAIL_CALLBACK);
            } else if (arg1.equals("xmpp")) {
                takePicture(XMPP_CALLBACK);
            } else if (arg1.equals("list")) {
                listCameras();
            } else if (arg1.equals("set")) {
                String arg2 = cmd.getArg2();
                if (!arg2.equals("")){
                    setCamera(arg2);
                }
            }           
        } 
        else if (isMatchingCmd(cmd, "flash")) {
            if (!arg1.equals("")) {
                setLight(arg1.equals("on"));
            } else {
                send(R.string.chat_camera_flash_mode, getLightMode());
            }
        }
    }
    
    private void setCamera(String arg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Integer id = Tools.parseInt(arg);
            if (id == null || id < 0 || id >= Camera.getNumberOfCameras()) {
                listCameras();
            } else {
                mCameraId = id;
                CameraInfo info = new CameraInfo(); 
                Camera.getCameraInfo(mCameraId, info);
                
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    send(R.string.chat_camera_back_activated);
                } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    send(R.string.chat_camera_front_activated);
                }    
            }                
        } else {
            Log.w("Android version doesn't allow setCamera command.");
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
                    res.append(sContext.getString(R.string.chat_camera_back)).append(Tools.LineSep);
                } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    res.append(sContext.getString(R.string.chat_camera_front)).append(Tools.LineSep);
                }    
            }
            send(res.toString());
        } else {
            Log.w("Android version doesn't allow listCamera command.");
            send(R.string.chat_camera_error_version);
        }
    }
    
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private int getCameraOrientation() {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        int rotation = mWindowManager.getDefaultDisplay().getRotation();
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
        releaseResources();
        PictureCallback pictureCallback;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                mCamera = Camera.open(mCameraId);
                mCamera.setDisplayOrientation(getCameraOrientation());
            } else {
                mCamera = Camera.open();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mCamera.setPreviewTexture(new SurfaceTexture(0));
            } else {
                mCamera.setPreviewDisplay(new SurfaceView(sContext).getHolder());
            }

            mCamera.startPreview();
            
            switch (pCallbackMethod) {
                case XMPP_CALLBACK:
                    pictureCallback = new XMPPTransferCallback(mRepository, sContext, mAnswerTo);
                    break;
                case EMAIL_CALLBACK:
                    pictureCallback = new EmailCallback(mRepository, sContext, mEmailReceiving);
                    break;
                case VOID_CALLBACK:
                default:
                    pictureCallback = new VoidCallback(mRepository, sContext, mAnswerTo);
            }
            
            mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
            
            Camera.ShutterCallback cb = new Camera.ShutterCallback() {
                
                @Override
                public void onShutter() {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, mStreamVolume, 0);
                }
            };
            send("Taking picture");
            mCamera.takePicture(cb, null, pictureCallback);
        } catch (Exception e) {
            Log.w("Error taking picture", e);
            send(R.string.chat_camera_error_picture, e.getLocalizedMessage());
            releaseResources();
        }
    }

    private void setLight(boolean turnOn) {
        if (mCamera == null) {
            mCamera = Camera.open();
        }

        Parameters params = mCamera.getParameters();
        params.setFlashMode(turnOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);

        if (!turnOn) {
            releaseResources();
        }
    }


    private String getLightMode() {
        if (mCamera == null) {
            mCamera = Camera.open();
        }

        Parameters params = mCamera.getParameters();
        return params.getFlashMode();
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
        flash.setHelp(R.string.chat_help_flash_state, null);
        flash.AddSubCmd("on", R.string.chat_help_flash_on, null);
        flash.AddSubCmd("off", R.string.chat_help_flash_off, null);
    }
}
