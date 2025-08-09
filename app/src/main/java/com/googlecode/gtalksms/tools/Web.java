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

public class Web {
    public static String DownloadFromUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            return DownloadFromUrl(url);
        } catch (MalformedURLException e) {
            return "";
        }
    }
    
    public static String DownloadFromUrl(URL url) {
        StringBuilder baf = new StringBuilder(1024);
        try {
            long startTime = System.currentTimeMillis();
            Log.d("Downloading URL: " + url);

            HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            int current;
            while ((current = bis.read()) != -1) {
                baf.append((char) current);
            }
            Log.d("Downloaded " + url + " in " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");

        } catch (IOException e) {
            Log.e("IOException in DownloadFromUrl(): " + e);
        }
        return baf.toString();
    }

    public static void DownloadFromUrl(String strURL, String fileName, Context context) {
        try {
            String path = context.getFilesDir() + "/";

            URL url = new URL(strURL); // you can write here any link
            File file = new File(path + fileName);

            long startTime = System.currentTimeMillis();
            Log.d("Download begins. url:" + url + " file name:" + fileName);

            URLConnection ucon = url.openConnection();
            InputStream is = ucon.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);

            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current;
            while ((current = bis.read()) != -1) {
                baf.append((byte) current);
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            Log.d("Downloaded in " + ((System.currentTimeMillis() - startTime) / 1000) + " sec");

        } catch (IOException e) {
            Log.e("Error: " + e);
        }
    }
}
