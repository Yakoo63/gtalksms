package com.googlecode.gtalksms.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.googlecode.gtalksms.files.Datefile;
import com.googlecode.gtalksms.files.NullIntentStartCounterDatefile;

import android.content.Context;
import java.text.ParseException;

public class NullIntentStartCounter {
    private static final String DIRECTORY = "nullIntentStartCounterData";
    private static File sDirFile;
    
    private static NullIntentStartCounter sNullIntentStartCounter;
    private static Context sContext;
    
    private NullIntentStartCounter(Context ctx) {
        sContext = ctx;
        sDirFile = new File(ctx.getFilesDir(), DIRECTORY);
        if (!sDirFile.exists())
            sDirFile.mkdir();
        cleanUp();
    }
    
    public static NullIntentStartCounter getInstance(Context ctx) {
        if (sNullIntentStartCounter == null) {
            sNullIntentStartCounter = new NullIntentStartCounter(ctx);
        }        
        return sNullIntentStartCounter;
    }
    
    public boolean count() {
        NullIntentStartCounterDatefile df;
        try {
            df = NullIntentStartCounterDatefile.construct(sDirFile);
            df.count();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
    
    public long[] getLastValues(int days) {
        long[] res = new long[days];
        for (int i = 0; i < days; i++) {
            // create an date for every 'days' day in the past
            // if there is a file getCount() will return a value >= 0
            // if there is no file getCount() will return -1
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, i*(-1));
            Date date = cal.getTime();
            NullIntentStartCounterDatefile df = NullIntentStartCounterDatefile.reconstruct(sDirFile, date);
            res[i] = df.getCount();
        }
        return res;
    }
    
    private static void cleanUp() {
        List<NullIntentStartCounterDatefile>  datefiles = getDatefiles();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        Date date = cal.getTime();
        
        Datefile.deleteDatefilesOlderThan(datefiles, date);
    }
    
    private static List<NullIntentStartCounterDatefile>  getDatefiles() {
        File[] files = sDirFile.listFiles();
        List<NullIntentStartCounterDatefile> datefiles = new ArrayList<NullIntentStartCounterDatefile>();
        for (File f : files) {
            try {
                NullIntentStartCounterDatefile df = NullIntentStartCounterDatefile.reconstruct(f);
                datefiles.add(df);
            } catch (ParseException e) {} 
        }
        return datefiles;
    }
}
