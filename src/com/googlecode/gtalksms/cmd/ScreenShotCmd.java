package com.googlecode.gtalksms.cmd;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.GregorianCalendar;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.tools.RootTools;
import com.googlecode.gtalksms.tools.Tools;

public class ScreenShotCmd extends CommandHandlerBase {

    private static final int VOID_CALLBACK = 0;
    private static final int XMPP_CALLBACK = 1;
    private static final int EMAIL_CALLBACK = 2;

    private static File repository;
    private static File tmpDir;
    private static int displayHeight;
    private static int displayWidth;
    private static int screenshotSize;

    public ScreenShotCmd(MainService mainService) {
        super(mainService, new String[] { "screenshot", "sc" }, CommandHandlerBase.TYPE_SYSTEM);        
        if (repository == null) {
            File path;

            if (Build.VERSION.SDK_INT >= 8) { // API Level >= 8 check
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            } else {
                path = new File(Environment.getExternalStorageDirectory(), "DCIM");
            }
            repository = new File(path, Tools.APP_NAME);
            tmpDir = _context.getCacheDir();
            
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager wm = (WindowManager)_context.getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(dm);
            displayHeight = dm.heightPixels;
            displayWidth = dm.widthPixels;
            screenshotSize = displayWidth * displayHeight;
        }
    }

    @Override
    protected void execute(String cmd, String args) {
        if (!repository.exists()) {
            repository.mkdirs();
        }
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
        
        if (!RootTools.askRootAccess()) {
            send("Root not given!");
            return;
        }
        try {
            String raw = tmpDir.getAbsolutePath() + "/frame.raw";
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("cat /dev/graphics/fb0 > " + raw + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();

            process.waitFor();

            File rawTmpFile = new File(raw);
            if (!rawTmpFile.exists())
                throw new Exception("File doesn't exist");

            InputStream in = null;
            in = new FileInputStream(rawTmpFile);

            byte sBuffer[] = new byte[screenshotSize * 2];
            in.read(sBuffer);

            short sBuffer2[] = new short[screenshotSize];

            for (int i = 0; i < screenshotSize * 2; i += 2) {
                sBuffer2[i / 2] = (short) (((0xFF00 & ((short) sBuffer[i + 1]) << 8)) | (0x00FF & sBuffer[i]));
            }

            Bitmap bitmap = Bitmap.createBitmap(displayWidth, displayHeight, Bitmap.Config.RGB_565);

            for (int i = 0; i < displayWidth; ++i) {
                for (int j = 0; j < displayHeight; ++j) {
                    short pixel = sBuffer2[displayWidth * j + i];

                    int red = (255 * ((pixel & 0xF800) >> 11)) / 32;
                    int green = (255 * ((pixel & 0x07E0) >> 5)) / 64;
                    int blue = (255 * (pixel & 0x001F)) / 32;

                    bitmap.setPixel(i, j, Color.rgb(red, green, blue));
                }
            }
                        
            File picture = new File(repository, "screenshot_" + Tools.getFileFormat(GregorianCalendar.getInstance()) + ".png");
            FileOutputStream fos = new FileOutputStream(picture);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Tools.send("Screenshot saved as " + picture.getAbsolutePath(), _answerTo, _context);
            
            rawTmpFile.delete();
            
            // TODO manage subcommand
            Intent i = new Intent(MainService.ACTION_COMMAND);
            i.setClass(_context, MainService.class);
            i.putExtra("from", _answerTo);
            i.putExtra("cmd", "send");
            i.putExtra("args", picture.getAbsolutePath());
            _context.startService(i);
        } catch (Exception e) {
            send("error while getting picture: " + e);
        }
    }

    @Override
    public synchronized void cleanUp() {
    }

    @Override
    public String[] help() {
        return null;
    }
}
