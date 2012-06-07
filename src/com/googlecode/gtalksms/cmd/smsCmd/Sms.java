package com.googlecode.gtalksms.cmd.smsCmd;

import java.util.Date;

/**
 * This is "our" internal class for holding SMS information in memory
 * It's currently used when:
 * - querying for SMS messages
 * - tracking delivery and sent notifications for SMS
 *
 */
public class Sms implements Comparable<Sms> {
    private String message;          
    private String shortendMessage;
    private String number;
    private String sender;
    private String to;
    private String answerTo;
    private String receiver;
    private Date date;
    private int resSentIntent;
    private int resDelIntent;
    private boolean[] sentIntents;
    private boolean[] delIntents;
    private Integer id;
    
    /**
     * This constructor is called when querying sms
     * 
     * @param phoneNumber
     * @param message
     * @param date
     */
    public Sms(String phoneNumber, String message, Date date, String receiver) {
        this.setNumber(phoneNumber);
        this.setMessage(message);
        this.date = date;
        this.receiver = receiver;
    }
    
    /**
     * This constructor gets called when sending an sms to put the sms in the sms map
     * 
     * @param phoneNumber
     * @param toName
     * @param shortendMessage
     * @param numParts
     * @param answerTo - which jid should be informed about the status (sent/delivered) of the sms
     */
    public Sms(String phoneNumber, String toName, String shortendMessage, int numParts, String answerTo, Integer id) {
        this.id = id;
        this.setResSentIntent(-1);
        this.setResDelIntent(-1);
        
        this.sentIntents = new boolean[numParts];
        this.delIntents = new boolean[numParts];
        this.setNumber(phoneNumber);
        this.setTo(toName);
        this.setShortendMessage(shortendMessage);
        this.setAnswerTo(answerTo);
        this.date = new Date();
    }
    
    public Sms(int smsID, String phoneNumber, String name, String shortendMessage, String answerTo, String dIntents, String sIntents, int resSIntent, int resDIntent, long date) {
        this.id = Integer.valueOf(smsID);
        this.number = phoneNumber;
        this.to = name;
        this.shortendMessage = shortendMessage;
        this.answerTo = answerTo;
        this.delIntents = toBoolArray(dIntents);
        this.sentIntents = toBoolArray(sIntents);
        this.resDelIntent = resDIntent;
        this.resSentIntent = resSIntent;
        this.date = new Date(date);
    }

    private boolean[] toBoolArray(String string) {
        boolean[] res = new boolean[string.length()];
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == 'X') {
                res[i] = true;
            } else {
                res[i] = false;
            }
        }
        return res;
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
        if (sender != null) {
            return sender;    
        } else {
            return number;
        }
        
    }
    
    public String getReceiver() {
        return receiver;
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
    
    public int getNumParts() {
        return sentIntents.length;
    }
    
    public int getID() {
        return id;
    }
    
    public String getDelIntents() {
        StringBuilder res = new StringBuilder(delIntents.length);
        for (boolean b : delIntents) {
            if (b == true) {
                res.append('X');
            } else {
                res.append('O');
            }
        }
        return new String(res);
    }
    
    public String getSentIntents() {
        StringBuilder res = new StringBuilder(sentIntents.length);
        for (boolean b : sentIntents) {
            if (b == true) {
                res.append('X');
            } else {
                res.append('O');
            }
        }
        return new String(res);
    }
    
    public Date getCreatedDate() {
        return date;
    }
}
