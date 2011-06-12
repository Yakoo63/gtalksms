package com.googlecode.gtalksms.xmpp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.googlecode.gtalksms.XmppManager;

import android.content.Context;

public class XmppStatus {
    
    private static final String STATEFILE_NAME = "xmppStatus";
    
    private static XmppStatus sXmppStatus;
    private static File sStatefile;
    
    
    private XmppStatus(Context ctx) {
        File filesDir = ctx.getFilesDir();
        sStatefile = new File(filesDir, STATEFILE_NAME);
    }
    
    public static XmppStatus getInstance(Context ctx) {
        if (sXmppStatus == null) {
            sXmppStatus = new XmppStatus(ctx);            
        }
        return sXmppStatus;
    }
    
    /**
     * Gets the last known XMPP status from the statefile
     * if there is no statefile the status for DISCONNECTED is returned
     * 
     * @return integer representing the XMPP status as defined in XmppManager
     */
    public int getStatusFromStatefile() {
        int res = XmppManager.DISCONNECTED;        
        FileInputStream fis;
        try {
            fis = new FileInputStream(sStatefile);
            DataInputStream dis = new DataInputStream(fis);
            res = dis.readInt();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {            
        }

        return res;
        
    }
    
    /**
     * Writes the current status int into the statefile
     * 
     * @param status
     */
    public void setStatus(int status) {
        try {
            if (sStatefile.isFile()) {

                RandomAccessFile raf = new RandomAccessFile(sStatefile, "rw");
                raf.setLength(0);

            } else {
                sStatefile.createNewFile();
            }
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(sStatefile);
            DataOutputStream dos = new DataOutputStream(fos);
            dos.writeInt(status);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }
}
