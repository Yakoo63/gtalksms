package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Date;


public class Sms implements Comparable<Sms> {
    private String message;          
    private String shortendMessage;
    private String number;
    private String sender;
    private String to;
    private String answerTo;
    private Date date;
    private int resSentIntent;
    private int resDelIntent;
    private boolean[] sentIntents;
    private boolean[] delIntents;
    
    /**
     * 
     * @param phoneNumber
     * @param message
     * @param date
     */
    public Sms(String phoneNumber, String message, Date date) {
    	this.setNumber(phoneNumber);
    	this.setMessage(message);
    	this.date = date;
    }
    
    /**
     * this constructor gets called when sending an sms to put the sms in the sms map
     * 
     * @param phoneNumber
     * @param toName
     * @param shortendMessage
     * @param numParts
     * @param answerTo - which jid should be informed about the status (sent/delivered) of the sms
     */
    public Sms(String phoneNumber, String toName, String shortendMessage, int numParts, String answerTo) {
        this.setResSentIntent(-1);
        this.setResDelIntent(-1);
        
        this.sentIntents = new boolean[numParts];
        this.delIntents = new boolean[numParts];
        this.setNumber(phoneNumber);
        this.setTo(toName);
        this.setShortendMessage(shortendMessage);
        this.setAnswerTo(answerTo);
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
    
    public void setDelIntentTrue(int partNumber) {
        delIntents[partNumber] = true;
    }
    
    public void setSentIntentTrue(int partNumber) {
        sentIntents[partNumber] = true;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setShortendMessage(String shortendMessage) {
        this.shortendMessage = shortendMessage;
    }

    public String getShortendMessage() {
        return shortendMessage;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public void setAnswerTo(String answerTo) {
        this.answerTo = answerTo;
    }

    public String getAnswerTo() {
        return answerTo;
    }

    public void setResSentIntent(int resSentIntent) {
        this.resSentIntent = resSentIntent;
    }

    public int getResSentIntent() {
        return resSentIntent;
    }

    public void setResDelIntent(int resDelIntent) {
        this.resDelIntent = resDelIntent;
    }

    public int getResDelIntent() {
        return resDelIntent;
    }
    
    public Date getDate() {
        return date;
    }
}
