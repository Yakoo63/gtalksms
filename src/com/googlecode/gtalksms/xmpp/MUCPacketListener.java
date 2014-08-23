package com.googlecode.gtalksms.xmpp;

import java.text.DateFormat;
import java.util.Date;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.muc.MultiUserChat;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;

class MUCPacketListener implements PacketListener {
    private final String mNumber;
    private final String mName; // the name of GTalkSMS in this room
    private Date mLastDate;
    private final MultiUserChat mMuc;
    private final String mRoomName;
    private final SettingsManager mSettings;
    private final Context mCtx;
    private final int mMode;
    
    /**
     * Creates a new MUCPacketListener
     * The MUC can either be use for a SMS chat/conversation or for a shell session
     * 
     * @param number
     * @param muc
     * @param name
     * @param mode
     * @param ctx
     */
    public MUCPacketListener(String number, MultiUserChat muc, String name, int mode, Context ctx) {
        this.mName = name;
        this.mNumber = number;
        this.mLastDate = new Date(0);
        this.mMuc = muc;
        this.mRoomName = muc.getRoom();
        this.mSettings = SettingsManager.getSettingsManager(ctx);
        this.mMode = mode;
        this.mCtx = ctx;
        
        Log.initialize(mSettings);
    }

    @Override
    public void processPacket(Packet packet) {
        Message message = (Message) packet;
        String from = message.getFrom();
        String fromBareResource = StringUtils.parseResource(from);

        Log.d("MUCPacketListener: packet received. messageFrom=" + message.getFrom() + " messageBody=" + message.getBody());
        
        // messages from the room JID itself, are matched here, because they have no 
        // resource part these are normally status messages about the room we send them 
        // to the notification address
        if (from.equals(mRoomName)) {
            Intent intent = new Intent(MainService.ACTION_SEND);
            intent.putExtra("message", mName + ": " + message.getBody());
            // fromMuc sounds right at first, but it serves no purpose here atm
            // intent.putExtra("fromMuc", true);
            MainService.sendToServiceHandler(intent);
        } else if (mMode == XmppMuc.MODE_SMS) {
            if (!fromBareResource.equals(mName)) {
                if (message.getBody() != null) {
                    DelayInformation inf = (DelayInformation) message.getExtension("x", "jabber:x:delay");
                    Date sentDate = inf != null ? inf.getStamp(): new Date();

                    if (sentDate.compareTo(mLastDate) > 0) {
                        Intent intent = new Intent(MainService.ACTION_COMMAND);
                        intent.setClass(mCtx, MainService.class);
    
                        intent.putExtra("from", mRoomName);
                        intent.putExtra("cmd", "sms");
                        intent.putExtra("fromMuc", true);
                        // if there are more than 2 users in the
                        // room, we include also a tag in the response of the sms message
                        if (mMuc.getOccupantsCount() > 2) { 
                            intent.putExtra("args", mNumber + ":" + fromBareResource + ": " + message.getBody());
                        } else {
                            intent.putExtra("args", mNumber + ":" + message.getBody());
                        }
                        
                        MainService.sendToServiceHandler(0, intent);
                        mLastDate = sentDate;
                    } else {
                        // this seems to be caused by the history replay of MUC rooms
                        // which is now disabled, lets get some metrics and decide later if we 
                        // can remove this check
                        Log.w("MUCPacketListener: Received old message: date="
                                + DateFormat.getDateTimeInstance().format(sentDate) + " ; message="
                                + message.getBody());
                    }
                }
            } else {
                Log.i("MUCPacketListener: Received message which equals our room nick. message=" + message.getBody());
            }
        } else if (mMode == XmppMuc.MODE_SHELL) {
            if (!fromBareResource.equals(mName)) {
                Intent intent = new Intent(MainService.ACTION_COMMAND);
                intent.setClass(mCtx, MainService.class);
    
                intent.putExtra("args", message.getBody());
                intent.putExtra("cmd", "cmd");
                intent.putExtra("from", mNumber);
                // Must not be set for Shell because everything in a shell session
                // should be returned to the according MUC
                // intent.putExtra("fromMuc", true);
                
                MainService.sendToServiceHandler(intent);
            }
        }
    }
}