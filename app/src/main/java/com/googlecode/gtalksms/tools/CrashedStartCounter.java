package com.googlecode.gtalksms.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.googlecode.gtalksms.files.DateFile;
import com.googlecode.gtalksms.files.NullIntentStartCounterDateFile;

import android.content.Context;
import java.text.ParseException;

/**
 * A helper that counts the starts of GTalkSMS after a crash
 *
 */
public class CrashedStartCounter {
    private static final String DIRECTORY = "nullIntentStartCounterData";
    private static File sDirFile;
    
    private static CrashedStartCounter sNullIntentStartCounter;

    private CrashedStartCounter(Context ctx) {
        sDirFile = new File(ctx.getFilesDir(), DIRECTORY);
        if (!sDirFile.exists())
            sDirFile.mkdir();
        cleanUp();
    }
    
    public static CrashedStartCounter getInstance(Context ctx) {
        if (sNullIntentStartCounter == null) {
            sNullIntentStartCounter = new CrashedStartCounter(ctx);
        }        
        return sNullIntentStartCounter;
    }
    
    public boolean count() {
        NullIntentStartCounterDateFile df;
        try {
            df = NullIntentStartCounterDateFile.construct(sDirFile);
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
            NullIntentStartCounterDateFile df = NullIntentStartCounterDateFile.reconstruct(sDirFile, date);
            res[i] = df.getCount();
        }
        return res;
    }
    
    private static void cleanUp() {
        List<NullIntentStartCounterDateFile>  datefiles = getDatefiles();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        Date date = cal.getTime();
        
        DateFile.deleteDatefilesOlderThan(datefiles, date);
    }
    
    private static List<NullIntentStartCounterDateFile>  getDatefiles() {
        File[] files = sDirFile.listFiles();
        List<NullIntentStartCounterDateFile> datefiles = new ArrayList<NullIntentStartCounterDateFile>();
        for (File f : files) {
            try {
                NullIntentStartCounterDateFile df = NullIntentStartCounterDateFile.reconstruct(f);
                datefiles.add(df);
            } catch (ParseException e) {} 
        }
        return datefiles;
    }
}
