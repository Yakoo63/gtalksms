package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;

import android.content.Context;
import android.util.Log;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.data.contacts.ContactsManager;
import com.googlecode.gtalksms.databases.MUCHelper;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;
import com.googlecode.gtalksms.tools.Tools;

public class XmppMuc {
	
	private static final String ROOM_START_TAG = "GTalkSMS_";
	private static final int ROOM_START_TAG_LENGTH = ROOM_START_TAG.length();
	private static final int REPLAY_TIMEOUT = 500;

    private Map<String, MultiUserChat> _rooms = new HashMap<String, MultiUserChat>();
    private Set<Integer> _roomNumbers = new HashSet<Integer>();
    private Context _context;
    private SettingsManager _settings;
    private XMPPConnection _connection;
    private XmppManager _xmppMgr;
    private static Random _rndGen = new Random();
    private MUCHelper _mucHelper;
    private DiscussionHistory discussionHistory;

    
    public XmppMuc(Context context, XmppManager xmppMgr) {
        _context = context;
        _settings = SettingsManager.getSettingsManager(context);
        _xmppMgr = xmppMgr;
        _mucHelper = MUCHelper.getMUCHelper(context);
        discussionHistory = new DiscussionHistory();
        // this should disable history replay on MUC rooms
        discussionHistory.setMaxChars(0);
    }
    
    public void initialize(XMPPConnection connection) {
        _connection = connection;
        // clear the roomNumbers and room ArrayList as we have a new connection
        _roomNumbers.clear();
        _rooms.clear();
        rejoinRooms();
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
        MultiUserChat muc;
        muc = inviteRoom(number, contact);
        if (muc != null)
        	muc.sendMessage(message);
    }
    
    /**
     * Invites the user to a room for the given contact name and number
     * if the user (or someone else) writes to this room, a SMS is send to the number
     * 
     * @param number
     * @param name
     * @return true if successful, otherwise false
     * @throws XMPPException 
     */
	public MultiUserChat inviteRoom(String number, String contact)
			throws XMPPException {
		MultiUserChat muc;
		if (!_rooms.containsKey(number)) {
			muc = createRoom(number, contact);
			_rooms.put(number, muc);

		} else {
			muc = _rooms.get(number);
			// TODO: test if occupants contains also the sender (in case we
			// invite other people)
			if (muc != null && muc.getOccupantsCount() < 2) {
				muc.invite(_settings.notifiedAddress, "SMS conversation with "
						+ contact);
			}
		}
		return muc;
	}   
    
    /**
     * Checks if a room for the specific number
     * 
     * @param number
     * @param contact
     * @return true if the room exists and gtalksms is in it, otherwise false
     */
    public boolean roomExists(String number) {
    	return _rooms.containsKey(number);
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
    public MultiUserChat getRoomViaRoomname(String roomname) {
        Collection<MultiUserChat> mucSet = _rooms.values();
        for(MultiUserChat muc : mucSet) {
            if(muc.getRoom().equals(roomname))
                return muc;
        }
        return null;
    }
	
    /**
     * Creates a new MUC AND invites the user
     * room name will be extended with an random number for security purposes
     * 
     * @param number
     * @param name - the name of the contact to chat via SMS with
     * @return
     * @throws XMPPException 
     */
    private MultiUserChat createRoom(String number, String name) throws XMPPException {
        String room = getRoomString(number, name);
        MultiUserChat multiUserChat = null;
        boolean passwordMode = false;
        Integer randomInt;                
        // TODO localize
        final String subjectInviteStr =  "SMS conversation with " + getRoomString(number, name);

        do {
            randomInt = _rndGen.nextInt();
        } while (_roomNumbers.contains(randomInt));

        // With "@conference.jabber.org" messages are sent several times...
        // Jwchat seems to work fine and is the default
        final String roomJID = ROOM_START_TAG + randomInt + "_" + _settings.login.replaceAll("@", "_") + "@" + _settings.mucServer;
        
        // See issue 136
        try {
            multiUserChat = new MultiUserChat(_connection, roomJID);
            multiUserChat.create(name);
        } catch (Exception e) {  
            throw new XMPPException("MUC creation failed", e);
        }
        
        try {
            // Since this is a private room, make the room not public and set
            // user as owner of the room.
            Form submitForm = multiUserChat.getConfigurationForm().createAnswerForm();
            submitForm.setAnswer("muc#roomconfig_publicroom", false);
            submitForm.setAnswer("muc#roomconfig_roomname", room);

            try {
                List<String> owners = new ArrayList<String>();
				if (_settings.useDifferentAccount) {
					owners.add(_settings.login);
					owners.add(_settings.notifiedAddress);
				} else {
					owners.add(_settings.login);
				}
                submitForm.setAnswer("muc#roomconfig_roomowners", owners);
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
            multiUserChat.changeSubject(subjectInviteStr);
        } catch (XMPPException e1) {
            GoogleAnalyticsHelper.trackAndLogWarning("Unable to send conference room configuration form.", e1);
            send(_context.getString(R.string.chat_sms_muc_conf_error, e1.getMessage()));
            // then we also should not send an invite as the room will be locked
            throw e1;
        }

        multiUserChat.invite(_settings.notifiedAddress, subjectInviteStr);
        registerRoom(multiUserChat, number, name, randomInt);
        return multiUserChat;
    }

    private void send(String msg) {
        _xmppMgr.send(new XmppMsg(msg), null);
    }
    
    private void rejoinRooms() {
    	String[][] mucDB = _mucHelper.getAllMUC();
    	if (mucDB == null)
    		return;
    		
    	for (int i = 0; i < mucDB.length; i++) {
    		RoomInfo info = getRoomInfo(mucDB[i][0]);
    		// if info is not null, the room exists on the server
    		// so lets check if we can reuse it
			if (info != null) {
				MultiUserChat muc = new MultiUserChat(_connection, mucDB[i][0]);
				String name = ContactsManager.getContactName(_context,
						mucDB[i][1]);
				try {
					if (info.isPasswordProtected()) {
						muc.join(name, _settings.roomPassword, discussionHistory, REPLAY_TIMEOUT);
					} else {
						muc.join(name, null, discussionHistory, REPLAY_TIMEOUT);
						if (!affilateCheck(muc.getOwners())) {
							if (_settings.debugLog) 
								Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because of affilateCheck failed");
							leaveRoom(muc);
							continue;
						}
					}
					// looks like there is no one in the room
					if (info.getOccupantsCount() > 0) {
						if (_settings.debugLog)
							Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because there is no one there");
						leaveRoom(muc);
						continue;
					}
				} catch (XMPPException e) {
					if (_settings.debugLog) {
						Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because of XMMPException", e);
					}
					// TODO decide in which cases it would be the best to remove the room from the db, because of a persistent error
					// and in which cases the error will not be permanent
					if (_connection.isAuthenticated()) {
						leaveRoom(muc);
						continue;
					} else {
						break;
					}
				}
				// muc has passed all tests and is fully usable
				registerRoom(muc, mucDB[i][1], name);
			}
    	}
    }
    
    /**
     * leaves the muc and deletes its record from the db
     * 
     * @param muc
     */
    private void leaveRoom(MultiUserChat muc) {
		_mucHelper.deleteMUC(muc.getRoom());
		if (muc.isJoined())
			muc.leave();

		if (_rooms.size() > 0) {
			Integer i = getRoomInt(muc.getRoom());
			String number = _mucHelper.getNumber(muc.getRoom());
			_roomNumbers.remove(i);
			_rooms.remove(number);
		}
    }
    
    private void registerRoom(MultiUserChat muc, String number, String name) {
    	String roomJID = muc.getRoom();
    	Integer randomInt = getRoomInt(roomJID);
    	registerRoom(muc, number, name, randomInt);
    }
    
    private void registerRoom(MultiUserChat muc, String number, String name, Integer randomInt) {
        MUCPacketListener chatListener = new MUCPacketListener(number, muc, name,  _context);
        muc.addMessageListener(chatListener);
        _roomNumbers.add(randomInt);
        _rooms.put(number, muc);
        _mucHelper.addMUC(muc.getRoom(), number);
    }
    
    /**
     * Returns the RoomInfo if the room exits
     * Allows an simple check for existence of a room
     * 
     * @param room
     * @return the roomInfo or null
     */
    private RoomInfo getRoomInfo(String room) {
    	RoomInfo info;
    	try {
    		info = MultiUserChat.getRoomInfo(_connection, room);
    	} catch (XMPPException e) {
    		return null;
    	}
    	return info;
    }
    
    /**
     * Checks if we are in this list of Affiliates
     * 
     * @param affCol
     * @return
     */
    private boolean affilateCheck(Collection<Affiliate> affCol) {
    	Set<String> jids = new HashSet<String>();
    	for (Affiliate a : affCol) {
    		jids.add(a.getJid());
    	}
    	return jids.contains(_settings.login);    	
    }
    /**
     * Extracts the room random integer from the room JID
     * 
     * @param room
     * @return
     */
    private Integer getRoomInt(String room) {
    	int intEnd = room.indexOf("_", ROOM_START_TAG_LENGTH);
    	return new Integer(room.substring(ROOM_START_TAG_LENGTH, intEnd));    	
    }
        
    /**
     * creates a formated string from number and contact
     * 
     * @param number
     * @param contact
     * @return
     */
    private static String getRoomString(String number, String contact) {
        return contact + " (" + number + ")";
    }
    
}
