package com.googlecode.gtalksms.xmpp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.files.ClientOfflineMessagesDatefile;
import com.googlecode.gtalksms.files.Datefile;

import android.content.Context;

public class ClientOfflineMessages {
    private static final String DIRECTORY = "clientOfflineMessagesData";
    private static File sDirFile;
    private static XmppMuc sXmppMuc;
    private static XMPPConnection sXMPPConnection;

    private static ClientOfflineMessages sClientOfflineMessages;
    private static Context sContext;
    
    private ClientOfflineMessages(Context ctx) {
        sContext = ctx;
        sDirFile = new File(ctx.getFilesDir(), DIRECTORY);
        if (!sDirFile.exists())
            sDirFile.mkdir();
        sXmppMuc = XmppMuc.getInstance(ctx);
        cleanUp();
    }

    public static ClientOfflineMessages getInstance(Context ctx) {
        if (sClientOfflineMessages == null) {
            sClientOfflineMessages = new ClientOfflineMessages(ctx);
        }
        return sClientOfflineMessages;
    }
    
    public void registerListener(XmppManager xmppMgr) {
        XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
            public void newConnection(XMPPConnection connection) {
                sXMPPConnection = connection;
                sendOfflineMessages();
            }            
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
    
    private static void sendOfflineMessages() {
        List<ClientOfflineMessagesDatefile> files = getDatefiles();
        for (ClientOfflineMessagesDatefile f : files) {
            try {
                Message msg = f.getMessage();
                MultiUserChat muc = sXmppMuc.getRoomViaRoomname(msg.getTo());
                if (muc == null) {
                    sXMPPConnection.sendPacket(msg);
                } else {
                    muc.sendMessage(msg);
                }
            } catch (FileNotFoundException e) {                
            } catch (IOException e) {
            } catch (XMPPException e) {
                // dont delete the file here
                continue;
            }
            f.delete();
        }
    }
    
    public boolean addOfflineMessage(Message msg) {
        ClientOfflineMessagesDatefile file;
        try {
            file = ClientOfflineMessagesDatefile.construct(sDirFile);
            file.setMessage(msg);
        } catch (IOException e) {
            return false;
        } 
        return true;
    }
    
    private static void cleanUp() {
        List<ClientOfflineMessagesDatefile> datefiles = getDatefiles();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        Date date = cal.getTime();
        
        Datefile.deleteDatefilesOlderThan(datefiles, date);
    }
    
    private static List<ClientOfflineMessagesDatefile> getDatefiles() {
        File[] files = sDirFile.listFiles();
        List<ClientOfflineMessagesDatefile> datefiles = new ArrayList<ClientOfflineMessagesDatefile>();
        for (File f : files) {
            try {
                ClientOfflineMessagesDatefile df = ClientOfflineMessagesDatefile.reconstruct(f);
                datefiles.add(df);
            } catch (NumberFormatException e) {} 
        }
        return datefiles;
    }
}
