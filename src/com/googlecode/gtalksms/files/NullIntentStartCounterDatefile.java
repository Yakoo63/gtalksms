package com.googlecode.gtalksms.files;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class NullIntentStartCounterDatefile extends Datefile {    

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String DATEFORMAT = "dd-MM-yyyy";

    private NullIntentStartCounterDatefile(File parent, String child, Date date) {
        super(parent, child, date);
    }
    
    public void count() throws IOException, FileNotFoundException {
        DataOutputStream dos = getDataOutputStream();
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
    
    public static NullIntentStartCounterDatefile reconstruct(File file) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        Date date = dateFormat.parse(file.getName());
        return new NullIntentStartCounterDatefile(file.getParentFile(), file.getName(), date);
        
    }
    
    public static NullIntentStartCounterDatefile reconstruct(File parent, Date date) {
        DateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        String filename = dateFormat.format(date);
        return new NullIntentStartCounterDatefile(parent, filename, date);        
    }
    
    public static NullIntentStartCounterDatefile construct(File parent) throws IOException {
        NullIntentStartCounterDatefile res;
        DateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);
        Date date = new Date();
        String filename = dateFormat.format(date);
        File f = new File(parent, filename);
        res = new NullIntentStartCounterDatefile(parent, f.getName(), date);
        return res;
    }

}
