package com.googlecode.gtalksms.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

public abstract class DateFile extends File {

    private final Date mDate;
    private static final long serialVersionUID = 1L;
    
    DateFile(File parent, String child, Date date) {
        super(parent, child);
        this.mDate = date;
    }
    
    long getTime() {
        return mDate.getTime();
    }
    
    DataInputStream getDataInputStream() throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(this);
        return new DataInputStream(fis);
    }
    
    DataOutputStream getDataOutputStream(boolean append) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(this, append);
        return new DataOutputStream(fos);
    }
    
    public static void deleteDatefilesOlderThan(List<?> files, Date date) {
        for (Object file : files) {
            DateFile f = (DateFile) file;
            if (f.getTime() < date.getTime()) {
                f.delete();
            }
        }        
    }
}
