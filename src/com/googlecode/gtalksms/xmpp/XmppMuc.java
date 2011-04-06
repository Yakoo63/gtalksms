package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Collection;
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

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;
import com.googlecode.gtalksms.tools.Tools;

public class XmppMuc {

    private Map<String, MultiUserChat> _rooms = new HashMap<String, MultiUserChat>();
    private Set<Integer> _roomNumbers = new HashSet<Integer>();
    private Context _context;
    private SettingsManager _settings;
    private XMPPConnection _connection;
    private XmppManager _xmppMgr;

    
    public XmppMuc(Context context, SettingsManager settings, XmppManager xmppMgr) {
        _context = context;
        _settings = settings;
        _xmppMgr = xmppMgr;
    }
    
    public void initialize(XMPPConnection connection) {
        _connection = connection;
        _roomNumbers.clear();  //clear the roomNumbers and room ArrayList as we have a new connection
        _rooms.clear();
    }
    
    /**
     * Writes a message to a room and creates the room if necessary,
     * followed by an invite to the default notification address 
     * to join the room
     * 
     * @param number
     * @param contact
     * @param message
     * @throws XMPPException
     */
    public void writeRoom(String number, String contact, String message) throws XMPPException {
        String room = getRoomString(number, contact);
        MultiUserChat muc;
        if (!_rooms.containsKey(room)) {
            muc = createRoom(number, room, contact);
            _rooms.put(room, muc);
        } else {
            muc = _rooms.get(room);
            // TODO: test if occupants contains also the sender (in case we
            // invite other people)
            if (muc != null && muc.getOccupantsCount() < 2) {
                muc.invite(_settings.notifiedAddress, "SMS conversation with " + contact);
            }
        }
        muc.sendMessage(message);
    }
    
    /**
     * Invites the user to a room for the given contact name and number
     * if the user (or someone else) writes to this room, a SMS is send to the number
     * 
     * @param number
     * @param name
     * @return true if successful, otherwise false
     */
    public void inviteRoom(String number, String sender) {
        String room = getRoomString(number, sender);
        try {
            MultiUserChat muc;
            if (!_rooms.containsKey(room)) {
                muc = createRoom(number, room, sender);             
                _rooms.put(room, muc);
                
            } else {
                muc = _rooms.get(room);                
                // TODO: test if occupants contains also the sender (in case we invite other people)
                if (muc != null && muc.getOccupantsCount() < 2) {
                    muc.invite(_settings.notifiedAddress, "SMS conversation with " + sender);
                }
            }
        } catch (Exception ex) {
            GoogleAnalyticsHelper.trackAndLogError("XmppMuc inviteRoom: exception", ex);
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
     * Returns the MultiUserChat given in roomname, 
     * which is a full JID (e.g. room@conference.jabber.com),
     * if the room is in your internal data structure.
     * Return null otherwise
     * 
     * 
     * @param roomname - the full roomname as JID
     * @return the room or null
     */
    public MultiUserChat getRoom(String roomname) {
        Collection<MultiUserChat> mucSet = _rooms.values();
        for(MultiUserChat muc : mucSet) {
            if(muc.getRoom().equals(roomname))
                return muc;
        }
        return null;
    }
    
    /**
     * Creates a new MUC AND invites the user
     * room name will be extended with an random number
     * 
     * @param number
     * @param room
     * @param sender
     * @return
     * @throws XMPPException 
     */
    private MultiUserChat createRoom(String number, String room, String name) throws XMPPException {
        
        MultiUserChat multiUserChat = null;
        boolean passwordMode = false;
        Integer randomInt;

        do {
            randomInt = (new Random()).nextInt();
        } while (_roomNumbers.contains(randomInt));
        _roomNumbers.add(randomInt);

        // With "@conference.jabber.org" messages are sent several times...
        // Jwchat seems to work fine and is the default
        String cnx = "GTalkSMS_" + randomInt + "_" + _settings.login.replaceAll("@", "_") + "@" + _settings.mucServer;
        
        // See issue 136
        try {
            multiUserChat = new MultiUserChat(_connection, cnx);
            multiUserChat.create(name + " (" + number + ")");
        } catch (Exception e) {  
            throw new XMPPException("MUC create failed", e);
        }
        
        try {
            // Since this is a private room, make the room not public and set
            // user as owner of the room.
            Form submitForm = multiUserChat.getConfigurationForm().createAnswerForm();
            submitForm.setAnswer("muc#roomconfig_publicroom", false);
            submitForm.setAnswer("muc#roomconfig_roomname", room);

            try {
                List<String> owners = new ArrayList<String>();
                owners.add(_settings.login);
                owners.add(_settings.notifiedAddress);
                submitForm.setAnswer("muc#roomconfig_roomowners", owners);
                // submitForm.setAnswer("muc#roomconfig_roomadmins", owners);
                // //throws exception (at least on my server)
            } catch (Exception ex) {
                GoogleAnalyticsHelper.trackAndLogWarning("Unable to configure room owners on Server " + _settings.mucServer
                        + ". Falling back to room passwords", ex);
                submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
                submitForm.setAnswer("muc#roomconfig_roomsecret", _settings.roomPassword);
                passwordMode = true;
            }

            if (!passwordMode) {
                submitForm.setAnswer("muc#roomconfig_membersonly", true);
            }

            multiUserChat.sendConfigurationForm(submitForm);
        } catch (XMPPException e1) {
            GoogleAnalyticsHelper.trackAndLogWarning("Unable to send conference room configuration form.", e1);
            send(_context.getString(R.string.chat_sms_muc_conf_error, e1.getMessage()));
            throw e1; // then we also should not send an invite as the room will be locked
        }

        multiUserChat.invite(_settings.notifiedAddress, "SMS conversation with " + name);

        ChatPacketListener chatListener = new ChatPacketListener(number, multiUserChat);
        multiUserChat.addMessageListener(chatListener);
        return multiUserChat;
    }
    
    class ChatPacketListener implements PacketListener {
        private String _number;
        private Date _lastDate;
        private MultiUserChat _muc;
        private String _roomName;
        
        public ChatPacketListener(String number, MultiUserChat muc) {
            _number = number;
            _lastDate = new Date(0);
            _muc = muc;
            _roomName = muc.getRoom();
        }
        
        @Override
        public void processPacket(Packet packet) {
            Message message = (Message) packet;
            String from = message.getFrom();
        
            if (_settings.debugLog) Log.d(Tools.LOG_TAG, "Xmpp chat room packet received");
            
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
                        Intent intent = new Intent(MainService.ACTION_COMMAND);
                        intent.setClass(_context, MainService.class);
                       
                        intent.putExtra("from", _roomName);
                        intent.putExtra("cmd", "sms");
                        intent.putExtra("fromMuc", true);
                        if(_muc.getOccupantsCount() > 2) {  // if there are more than 2 users in the room, we include also the from tag
                            intent.putExtra("args", _number + ":" + from + ": " + message.getBody());  
                        } else {
                            intent.putExtra("args", _number + ":" + message.getBody());  
                        }
                        
                        _context.startService(intent);
                        _lastDate = sentDate;
                    } else {
                        Log.w(Tools.LOG_TAG, "Receive old message: date=" + sentDate.toLocaleString() + " ; message=" + message.getBody());
                        GoogleAnalyticsHelper.trackWarning("MUC ChatPacketListener - received old message on server " + _settings.mucServer);
                    }
                }
            }
        }
    }

    private void send(String msg) {
        _xmppMgr.send(new XmppMsg(msg), null);
    }
}
