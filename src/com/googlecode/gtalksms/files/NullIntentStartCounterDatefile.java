package com.googlecode.gtalksms.files;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class NullIntentStartCounterDateFile extends DateFile {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String DATEFORMAT = "dd-MM-yyyy";

    private NullIntentStartCounterDateFile(File parent, String child, Date date) {
        super(parent, child, date);
    }
    
    public void count() throws IOException {
        DataOutputStream dos = getDataOutputStream(true);
        dos.writeByte(0);
        dos.close();
    }
    
//    public long getCount() throws IOException, FileNotFoundException {
//        DataInputStream dis = getDataInputStream();
//        long res = 0;
//        try {
//            while(true) {
//                dis.readByte();
//                res++;
//            }
//        } catch (EOFException e) {          
//        }
//        return res;
//    }
    
    public long getCount() {
        if (this.isFile()) {
            return this.length();
        } else {
            return -1;
        }        
    }
    
    public static NullIntentStartCounterDateFile reconstruct(File file) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        Date date = dateFormat.parse(file.getName());
        return new NullIntentStartCounterDateFile(file.getParentFile(), file.getName(), date);
        
    }
    
    public static NullIntentStartCounterDateFile reconstruct(File parent, Date date) {
        DateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        String filename = dateFormat.format(date);
        return new NullIntentStartCounterDateFile(parent, filename, date);
    }
    
    public static NullIntentStartCounterDateFile construct(File parent) {
        Date date = new Date();
        String filename = new SimpleDateFormat(DATEFORMAT).format(date);
        File f = new File(parent, filename);
        return new NullIntentStartCounterDateFile(parent, f.getName(), date);
    }

}
