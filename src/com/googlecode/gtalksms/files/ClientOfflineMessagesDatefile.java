package com.googlecode.gtalksms.files;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.jivesoftware.smack.packet.Message;

public class ClientOfflineMessagesDateFile extends DateFile {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final String DELIMITER = "_";
    
    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_CHAT = 1;
    private static final int TYPE_GROUPCHAT = 2;
    private static final int TYPE_HEADLINE = 3;
    private static final int TYPE_ERROR = 4;
    
//    private ClientOfflineMessagesFile(File parent, String child) {
//        super(parent, child);
//    }
    
    private ClientOfflineMessagesDateFile(File parent, String child, Date date) {
        super(parent, child, date);
    }
    
    public static ClientOfflineMessagesDateFile reconstruct(File f) throws NumberFormatException {
        int delimiter = f.getName().indexOf(DELIMITER);
        String ms;
        if (delimiter > 0) {
            ms = f.getName().substring(0, delimiter);
        } else {
            ms = f.getName();
        }
        long msLong = Long.parseLong(ms);
        Date date = new Date(msLong);
        return new ClientOfflineMessagesDateFile(f.getParentFile(), f.getName(), date);
    }
    
    public static ClientOfflineMessagesDateFile construct(File parent) throws IOException {
        ClientOfflineMessagesDateFile res;
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
        res = new ClientOfflineMessagesDateFile(parent, f.getName(), date);
        return res;
    }
    
    public void setMessage(Message msg) throws IOException {
        setMessage(msg.getBody(), msg.getTo(), msg.getType());
    }
    
    void setMessage(String body, String to, Message.Type type) throws IOException {
        // make sure we have an empty file
        if (this.isFile() && this.length() > 0) {
            this.delete();
            this.createNewFile();
        }
        int typeInt = typeEnumToInt(type);
        DataOutputStream dos = getDataOutputStream(false);
        dos.writeUTF(to == null ? "" : to);
        dos.writeInt(typeInt);
        dos.writeUTF(body);
        dos.close();        
    }
    
    public Message getMessage() throws IOException {
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
        int res;
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
        Message.Type res;
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
