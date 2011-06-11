package com.googlecode.gtalksms.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import org.jivesoftware.smack.packet.Message;

public class ClientOfflineMessagesDatefile extends Datefile {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String DELIMITER = "_";
    
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_CHAT = 1;
    public static final int TYPE_GROUPCHAT = 2;
    public static final int TYPE_HEADLINE = 3;
    public static final int TYPE_ERROR = 4;
    
//    private ClientOfflineMessagesFile(File parent, String child) {
//        super(parent, child);
//    }
    
    private ClientOfflineMessagesDatefile(File parent, String child, Date date) {
        super(parent, child, date);
    }
    
    public static ClientOfflineMessagesDatefile reconstruct(File f) throws NumberFormatException {
        int delimiter = f.getName().indexOf(DELIMITER);
        String ms;
        if (delimiter > 0) {
            ms = f.getName().substring(0, delimiter);
        } else {
            ms = f.getName();
        }
        long msLong = Long.parseLong(ms);
        Date date = new Date(msLong);
        return new ClientOfflineMessagesDatefile(f.getParentFile(), f.getName(), date);
    }
    
    public static ClientOfflineMessagesDatefile construct(File parent) throws IOException {
        ClientOfflineMessagesDatefile res;
        Date date = new Date();
        long ms = date.getTime();
        String filename = Long.toString(ms);
        File f = new File(parent, filename);
        int i = 0;
        while (f.exists()) {
            if (i > 10000)
                throw new IllegalStateException();
            String newFilename = filename + DELIMITER + i;
            f = new File(parent, newFilename);
        }
        f.createNewFile();
        res = new ClientOfflineMessagesDatefile(parent, f.getName(), date);
        return res;
    }
    
    public void setMessage(Message msg) throws IOException, FileNotFoundException {
        setMessage(msg.getBody(), msg.getTo(), msg.getType());
    }
    
    public void setMessage(String body, String to, Message.Type type) throws IOException, FileNotFoundException {
        // make sure we have an empty file
        if (this.isFile() && this.length() > 0) {
            this.delete();
            this.createNewFile();
        }
        int typeInt = typeEnumToInt(type);
        DataOutputStream dos = getDataOutputStream();
        dos.writeUTF(to);
        dos.writeInt(typeInt);
        dos.writeUTF(body);
        dos.close();        
    }
    
    public Message getMessage() throws IOException, FileNotFoundException {
        DataInputStream dis = getDataInputStream();
        String to = dis.readUTF();
        int typeInt = dis.readInt();
        String body = dis.readUTF();
        Message.Type type = intToTypeEnum(typeInt);
        Message msg = new Message(to, type);
        msg.setBody(body);               
        return msg;
    }
    
    private static int typeEnumToInt(Message.Type type) {
        int res = -1;
        switch (type) {
        case chat:
            res = TYPE_CHAT;
            break;
        case error:
            res = TYPE_ERROR;
            break;
        case groupchat:
            res = TYPE_GROUPCHAT;
            break;
        case headline:
            res = TYPE_HEADLINE;
            break;
        case normal:
            res = TYPE_NORMAL;
            break;
        default:
            throw new IllegalStateException();            
        }
        return res;       
    }
    
    private static Message.Type intToTypeEnum(int type) {
        Message.Type res = null;
        switch (type) {
        case TYPE_NORMAL:
            res = Message.Type.normal;
            break;
        case TYPE_CHAT:
            res = Message.Type.chat;
            break;
        case TYPE_GROUPCHAT:
            res = Message.Type.groupchat;
            break;
        case TYPE_HEADLINE:
            res = Message.Type.headline;
            break;
        case TYPE_ERROR:
            res = Message.Type.error;
            break;
        default:
            throw new IllegalStateException();                
        }
        return res;        
    }
}
