package com.googlecode.gtalksms.tools;

import java.net.URL;
import java.util.HashMap;

import android.os.AsyncTask;

public class UrlToStringDownloader extends AsyncTask<URL, Float, HashMap<URL, String>> {
    
    @Override
    protected HashMap<URL, String> doInBackground(URL... arg0) {
        HashMap<URL, String> result = new HashMap<URL, String>();
        for (int i = 0; i < arg0.length; i++) {
            URL u = arg0[i];
            String urlString = Web.DownloadFromUrl(u);
            result.put(u, urlString);
            publishProgress((float)i / (float)arg0.length);
        }
        return result;
    }

}
