package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Date;

import android.graphics.Bitmap;

/**
 * This is "our" internal class for holding MMS information in memory
 * It's currently used when:
 * - querying for MMS messages
 *
 */
public class Mms implements Comparable<Mms> {
    private String subject;          
    private String sender;
    private String message = "";
    private Bitmap bitmap;
    private String id;
    private Date date;
    
    public Mms(String subject, Date date, String id, String sender) {
        this.setSubject(subject);
        this.date = date;
        this.id = id;
        this.sender = sender;
    }

    @Override
    public int compareTo(Mms another) {
        return date.compareTo(another.date);
    }
    
    public void appendMessage(String message) {
        this.message += message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public Date getDate() {
        return date;
    }
    
    public String getId() {
        return id;
    }    
    
    public Date getCreatedDate() {
        return date;
    }
}
