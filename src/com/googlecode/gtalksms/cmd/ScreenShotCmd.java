package com.googlecode.gtalksms.cmd;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ShortBuffer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.Tools;

public class ScreenShotCmd extends CommandHandlerBase {
    
    private static final int VOID_CALLBACK = 0;
    private static final int XMPP_CALLBACK = 1;
    private static final int EMAIL_CALLBACK = 2;

    private static File repository;
    
    public ScreenShotCmd(MainService mainService) {
        super(mainService, new String[] {"screenshot", "sc"}, CommandHandlerBase.TYPE_SYSTEM);
        File path;
        
        SettingsManager settings = SettingsManager.getSettingsManager(_context);
        if (settings.api8orGreater) {  // API Level >= 8 check
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        } else {
            path = new File(Environment.getExternalStorageDirectory(), "DCIM");
        }

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
        if (cmd.equals("sc") || cmd.equals("screenshot")) {
            if (args.equals("") || splitedArgs[0].equals("")) {
                takePicture(VOID_CALLBACK);
            } else if (splitedArgs[0].equals("email")) {
                takePicture(EMAIL_CALLBACK);
            } else if (splitedArgs[0].equals("xmpp")) {
                takePicture(XMPP_CALLBACK);
            }           
        } 
    }
    
    private void takePicture(int pCallbackMethod) {
        cleanUp();
        
        try {
            //DisplayMetrics dm = new DisplayMetrics();
            //_mainService.getWindowManager().getDefaultDisplay().getMetrics(dm);
            int width = 320;//dm.widthPixels;
            int height = 480;//dm.heightPixels;
            
            int screenshotSize = width * height;
            
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("cat /dev/graphics/fb0 > /sdcard/frame.raw\n");
            os.flush();
            os.close();
      
            process.waitFor();
            
            // /sdcard/frame.raw
            //Bitmap bitmap = BitmapFactory.decodeFile("/dev/graphics/fb0");
            File file = new File("/sdcard/frame.raw");
            if(!file.exists()) throw new Exception("File doesn't exist");
            
            InputStream in = null;
            in = new FileInputStream(file);
            
            byte sBuffer[] = new byte[screenshotSize*2];
            short sBuffer2[] = new short[screenshotSize];
            in.read(sBuffer);
            
            //Bitmap bitmap = BitmapFactory.decodeFile("/dev/graphics/fb0");
            //if (null == bitmap) throw new Exception("Faild to decode bitmap");
            ShortBuffer sb = ShortBuffer.wrap(sBuffer2);
            //bitmap.copyPixelsToBuffer(sb);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            //Making created bitmap (from OpenGL points) compatible with Android bitmap
            for (int i = 0; i < screenshotSize*2; i+=2) {                  
                sBuffer2[i/2] = (short)(((short) sBuffer[i]) << 8);
                sBuffer2[i/2] |= sBuffer[i+1];
                
                short s = sBuffer2[i/2];
                sBuffer2[i/2] = (short) (((s&0x1f) << 11) | (s&0x7e0) | ((s&0xf800) >> 11));
                
            }
            sb.rewind();
            
            bitmap.copyPixelsFromBuffer(sb);
            String SCREENSHOT_DIR = "/screenshots";
            String filename = saveBitmap(bitmap, SCREENSHOT_DIR, "capturedImage");
            
            Intent i = new Intent(MainService.ACTION_COMMAND);
            i.setClass(_context, MainService.class);
            i.putExtra("from", _answerTo);
            i.putExtra("cmd", "send");
            i.putExtra("args", filename);
            _context.startService(i);
        } catch (Exception e) {
            send("error while getting picture: " + e);
        }
    }
       
    String saveBitmap(Bitmap bitmap, String dir, String baseName) {
        try {
            File sdcard = Environment.getExternalStorageDirectory();
            File pictureDir = new File(sdcard, dir);
            pictureDir.mkdirs();
            File f = null;
            for (int i = 1; i < 2000; ++i) {
                String name = baseName + i + ".png";
                f = new File(pictureDir, name);
                if (!f.exists()) {
                    break;
                }
            }
            if (!f.exists()) {
                String name = f.getAbsolutePath();
                FileOutputStream fos = new FileOutputStream(name);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                return name;
            }
        } catch (Exception e) {
        }
        return null;
    }
    
    @Override
    public synchronized void cleanUp() {
    }

    @Override
    public String[] help() {
        return null;
    }
}
