package com.googlecode.gtalksms.data.sms;

import java.util.Date;


public class Sms implements Comparable<Sms> {
    public String message;
    public String shortendMessage;
    public String number;
    public String sender;
    public String to;
    public String answerTo;
    public Date date;
    public int resSentIntent;
    public int resDelIntent;
    public boolean[] sentIntents;
    public boolean[] delIntents;
    
    public Sms(String phoneNumber, String message, Date date) {
    	this.number = phoneNumber;
    	this.message = message;
    	this.date = date;
    }
    
    public Sms(String phoneNumber, String toName, String shortendMessage, int numParts, String answerTo) {
        this.resSentIntent = -1;
        this.resDelIntent = -1;
        
        sentIntents = new boolean[numParts];
        delIntents = new boolean[numParts];
        this.number = phoneNumber;
        this.to = toName;
        this.shortendMessage = shortendMessage;
        this.answerTo = answerTo;
    }
    
    public boolean sentIntentsComplete() {
    	for (int i = 0; i < sentIntents.length; i++) {
    		if (sentIntents[i] == false) return false;
    	}
    	return true;
    }
    
    public boolean delIntentsComplete() {
    	for (int i = 0; i < delIntents.length; i++) {
    		if (delIntents[i] == false) return false;
    	}
    	return true;
    }

    @Override
    public int compareTo(Sms another) {
        return date.compareTo(another.date);
    }
}
