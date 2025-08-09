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
    private String shortenedMessage;
    private String number;
    private String sender;
    private String to;
    private String answerTo;
    private String receiver;
    private final Date date;
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
     * @param shortenedMessage
     * @param numParts
     * @param answerTo - which jid should be informed about the status (sent/delivered) of the sms
     */
    public Sms(String phoneNumber, String toName, String shortenedMessage, int numParts, String answerTo, Integer id) {
        this.id = id;
        this.setResSentIntent(-1);
        this.setResDelIntent(-1);
        
        this.sentIntents = new boolean[numParts];
        this.delIntents = new boolean[numParts];
        this.setNumber(phoneNumber);
        this.setTo(toName);
        this.setShortenedMessage(shortenedMessage);
        this.setAnswerTo(answerTo);
        this.date = new Date();
    }
    
    public Sms(int smsID, String phoneNumber, String name, String shortenedMessage, String answerTo, String dIntents, String sIntents, int resSIntent, int resDIntent, long date) {
        this.id = smsID;
        this.number = phoneNumber;
        this.to = name;
        this.shortenedMessage = shortenedMessage;
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
            res[i] = c == 'X';
        }
        return res;
    }

    public boolean sentIntentsComplete() {
        for (boolean sentIntent : sentIntents) {
            if (!sentIntent) return false;
        }
        return true;
    }
    
    public boolean delIntentsComplete() {
        for (boolean delIntent : delIntents) {
            if (!delIntent) return false;
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

    void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    void setShortenedMessage(String shortenedMessage) {
        this.shortenedMessage = shortenedMessage;
    }

    public String getShortenedMessage() {
        return shortenedMessage;
    }

    void setNumber(String number) {
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

    void setTo(String to) {
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    void setAnswerTo(String answerTo) {
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

    void setResDelIntent(int resDelIntent) {
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
            if (b) {
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
            if (b) {
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
