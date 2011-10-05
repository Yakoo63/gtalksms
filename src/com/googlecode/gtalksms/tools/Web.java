package com.googlecode.gtalksms.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.util.ByteArrayBuffer;

import android.content.Context;
import android.util.Log;

public class Web {
    public static String DownloadFromUrl(String urlStr) throws MalformedURLException {
            URL url = new URL(urlStr);
            return DownloadFromUrl(url);
    }
    
    public static String DownloadFromUrl(URL url) {
        StringBuffer baf = new StringBuffer(1024);
        try {
            long startTime = System.currentTimeMillis();
            Log.d(Tools.LOG_TAG, "Downloading url:" + url);

            HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((char) current);
            }
            Log.d(Tools.LOG_TAG, "downloaded in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");

        } catch (IOException e) {
            Log.d(Tools.LOG_TAG, "IOException in DownloadFromUrl(): " + e);
        }
        return baf.toString();
    }

    public static void DownloadFromUrl(String strURL, String fileName, Context context) {
        try {
            String path = context.getFilesDir() + "/";

            URL url = new URL(strURL); // you can write here any link
            File file = new File(path + fileName);

            long startTime = System.currentTimeMillis();
            Log.d(Tools.LOG_TAG, "Download begins. url:" + url + " file name:" + fileName);

            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d(Tools.LOG_TAG, "Downloaded in" + ((System.currentTimeMillis() - startTime) / 1000) + " sec");

        } catch (IOException e) {
            Log.d(Tools.LOG_TAG, "Error: " + e);
        }
    }
}
