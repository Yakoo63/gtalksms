package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DelayInformation;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

public abstract class XmppMuc {

    private Map<String, MultiUserChat> _rooms = new HashMap<String, MultiUserChat>();
    private Set<Integer> _roomNumbers = new HashSet<Integer>();
    private Context _context;
    private SettingsManager _settings;
    private XMPPConnection _connection;
    
    public XmppMuc(Context context, SettingsManager settings) {
        _context = context;
        _settings = settings;
    }
    
    public void initialize(XMPPConnection connection) {
        _connection = connection;
        _roomNumbers.clear();  //clear the roomNumbers and room ArrayList as we have a new connection
        _rooms.clear();
    }
    
    /**
     * Sends a message to a MUC, creates the MUC if necessary
     * 
     * @param number 	the phone number of the receiver
     * @param sender    the name of the receiver
     * @param message   the message to send
     */
    public void writeRoom(String number, String sender, String message) {
        String room = getRoomString(number, sender);
        try {
            MultiUserChat muc;
            if (!_rooms.containsKey(room)) {
                muc = createRoom(number, room, sender);
                
                if (muc != null) {
                    _rooms.put(room, muc);
                }
                
            } else {
                muc = _rooms.get(room);              
                // TODO: test if occupants contains also the sender (in case we invite other people)
                if (muc != null && muc.getOccupantsCount() < 2) {
                    muc.invite(_settings.notifiedAddress, "SMS conversation with " + sender);
                }
            }
            
            if (muc != null) {
                muc.sendMessage(message);
            }
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "writeRoom: room = " + room, ex);
        }
    }
    
    public void inviteRoom(String number, String sender) {
        String room = getRoomString(number, sender);
        try {
            MultiUserChat muc;
            if (!_rooms.containsKey(room)) {
                muc = createRoom(number, room, sender);             
                if (muc != null) {  //create successful
                    _rooms.put(room, muc);
                }
                
            } else {
                muc = _rooms.get(room);
                
                // TODO: test if occupants contains also the sender (in case we invite other people)
                if (muc != null && muc.getOccupantsCount() < 2) {
                    muc.invite(_settings.notifiedAddress, "SMS conversation with " + sender);
                }
            }
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "writeRoom: room = " + room, ex);
        }
    }
    
    /**
     * creates a string from number and contact
     * for use as the room key in the rooms array
     * (the actual room name will have another string
     * + an randInt)
     * 
     * @param number
     * @param contact
     * @return
     */
    private static String getRoomString(String number, String contact) {
        String contactLowerCase = new String(contact);
        contactLowerCase.toLowerCase();
        return contactLowerCase + " (" + number + ")";
    }
    
    /**
     * Checks if a room for the specific contact 
     * AND corresponding number exists
     * 
     * @param number
     * @param contact
     * @return true if the room exists and gtalksms is in it, otherwise false
     */
    public boolean roomExists(String number, String contact) {
    	String room = getRoomString(number, contact);
    	if(_rooms.containsKey(room)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Creates a new MUC AND invites the user
     * room name will be extended with an random number
     * 
     * @param number
     * @param room
     * @param sender
     * @return
     */
    private MultiUserChat createRoom(String number, String room, String name) {
        
        MultiUserChat multiUserChat = null;
        boolean passwordMode = false;
        Integer randomInt;
        
        do {
            randomInt = (new Random()).nextInt();
        } while (_roomNumbers.contains(randomInt));
        _roomNumbers.add(randomInt);
        
        // With "@conference.jabber.org" messages are sent several times... Jwchat seems to work fine and is the default
        String cnx = "GTalkSMS_" + randomInt + "_" + _settings.login.replaceAll("@", "_") + "@" + _settings.mucServer; 
        try {
            // Create the room
            multiUserChat = new MultiUserChat(_connection, cnx);
            multiUserChat.create(name + "(" + number + ")");
                
            try {
                // Since this is a private room, make the room not public and set user as owner of the room.
                Form submitForm = multiUserChat.getConfigurationForm().createAnswerForm();
                submitForm.setAnswer("muc#roomconfig_publicroom", false);
                submitForm.setAnswer("muc#roomconfig_roomname", room);

                    
                try {
                    List<String> owners = new ArrayList<String>();
                    owners.add(_settings.login);
                    owners.add(_settings.notifiedAddress);
                    submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                    //submitForm.setAnswer("muc#roomconfig_roomadmins", owners);  //throws exception (at least on my server)
                }
                catch (Exception ex) {
                    Log.e(Tools.LOG_TAG, "Unable to configure room owners. Falling back to room passwords", ex);
                    submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
                    submitForm.setAnswer("muc#roomconfig_roomsecret", _settings.roomsPassword);
                    passwordMode = true;
                }
                    
                if (!passwordMode) {
                    submitForm.setAnswer("muc#roomconfig_membersonly", true);
                }
                                    
                multiUserChat.sendConfigurationForm(submitForm);
            }
            catch (XMPPException e1) {
                Log.e(Tools.LOG_TAG, "Unable to send conference room configuration form.", e1);
                send(_context.getString(R.string.chat_sms_muc_conf_error, e1.getMessage()));
                return null; //then we also should not send an invite as the room will be locked
            }
               
            multiUserChat.invite(_settings.notifiedAddress, "SMS conversation with " + name);

            ChatPacketListener chatListener = new ChatPacketListener(number, name, multiUserChat);
            multiUserChat.addMessageListener(chatListener);
        } catch (Exception ex) {
            Log.e(Tools.LOG_TAG, "createRoom() - Error creating room: " + room, ex);
            send(_context.getString(R.string.chat_sms_muc_error, ex.getMessage()));
            return null;
        }
        return multiUserChat;
    }
    
    class ChatPacketListener implements PacketListener {
        private String _name;
        private String _number;
        private Date _lastDate;
        private MultiUserChat _muc;
        
        public ChatPacketListener(String number, String name, MultiUserChat muc) {
            _number = number;
            _name = name;
            _lastDate = new Date(0);
            _muc = muc;
        }
        
        @Override
        public void processPacket(Packet packet) {
            Message message = (Message) packet;
            String from = message.getFrom();
        
            Log.d(Tools.LOG_TAG, "Xmpp chat room packet received");
            
            if (!from.contains(_number)) {
                if (message.getBody() != null) {
                    DelayInformation inf = (DelayInformation)message.getExtension("x", "jabber:x:delay");
                    Date sentDate;
                    if (inf != null) {
                        sentDate = inf.getStamp();
                    } else {
                        sentDate = new Date();
                    }
                    
                    if (sentDate.compareTo(_lastDate) > 0 ) {
                        Intent intent = new Intent(XmppManager.ACTION_MESSAGE_RECEIVED);
                        if(_muc.getOccupantsCount() > 2) {
                            intent.putExtra("message", "sms:" + _name + ":" + from + ": " + message.getBody());
                        } else {
                            intent.putExtra("message", "sms:" + _name + ":" + message.getBody());
                        }
                        
                        _context.sendBroadcast(intent);
                        _lastDate = sentDate;
                    } else {
                        Log.w(Tools.LOG_TAG, "Receive old message: date=" + sentDate.toLocaleString() + " ; message=" + message.getBody());
                    }
                }
            }
        }
    }

    protected abstract void send(String msg);
}
