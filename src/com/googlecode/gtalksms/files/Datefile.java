package com.googlecode.gtalksms.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

public abstract class Datefile extends File {

    protected Date mDate;
    private static final long serialVersionUID = 1L;
    
    protected Datefile(File parent, String child, Date date) {
        super(parent, child);
        this.mDate = date;
    }
    
    public long getTime() {
        return mDate.getTime();
    }

    public int compareTo(Datefile other) {
        return mDate.compareTo(other.mDate);
    }
    
    protected DataInputStream getDataInputStream() throws FileNotFoundException {
        FileInputStream fis = new FileInputStream(this);
        DataInputStream dis = new DataInputStream(fis);
        return dis;
    }
    
    protected DataOutputStream getDataOutputStream(boolean append) throws FileNotFoundException {
        FileOutputStream fos = new FileOutputStream(this, append);
        DataOutputStream dos = new DataOutputStream(fos);
        return dos;
    }
    
    public static void deleteDatefilesOlderThan(List<?> files, Date date) {
        for (int i = 0; i < files.size(); i++) {
            Datefile f = (Datefile) files.get(i);
            if (f.getTime() < date.getTime())
                f.delete();
        }        
    }

}
