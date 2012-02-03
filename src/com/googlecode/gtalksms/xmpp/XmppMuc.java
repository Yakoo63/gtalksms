package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.XHTMLManager;
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
	
    public static final int MODE_SMS = 1;
    public static final int MODE_SHELL = 2;
    
	private static final String ROOM_START_TAG = Tools.APP_NAME + "_";
	private static final int ROOM_START_TAG_LENGTH = ROOM_START_TAG.length();
	private static final int REPLAY_TIMEOUT = 500;

    private static XmppMuc sXmppMuc;
	
    private Map<String, MultiUserChat> mRooms = new HashMap<String, MultiUserChat>();
    private Set<Integer> mRoomNumbers = new HashSet<Integer>();
    private Context mCtx;
    private SettingsManager mSettings;
    private XMPPConnection mConnection;
    private Random mRndGen = new Random();
    private MUCHelper mMucHelper;
    private DiscussionHistory mDiscussionHistory;
    private String mMucServer;
    
    private XmppMuc(Context context) {
        mCtx = context;
        mSettings = SettingsManager.getSettingsManager(context);
        mMucHelper = MUCHelper.getMUCHelper(context);
        mDiscussionHistory = new DiscussionHistory();
        // this should disable history replay on MUC rooms
        mDiscussionHistory.setMaxChars(0);        
    }
    
    public void registerListener(XmppManager xmppMgr) {
        XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
            public void newConnection(XMPPConnection connection) {
                mConnection = connection;
                // clear the roomNumbers and room ArrayList as we have a new connection
                mRoomNumbers.clear();
                mRooms.clear();
                rejoinRooms();
                
                
                try {
                    Collection<String> mucComponents = MultiUserChat.getServiceNames(connection);
                    if (mucComponents.size() > 0) {
                        Iterator<String> i = mucComponents.iterator();
                        mMucServer = i.next();
                    }
                } catch (XMPPException e) {
                    // This is not fatal, just log a warning
                    GoogleAnalyticsHelper.trackAndLogWarning("Could not discover local MUC component: ", e);            
                }
            }
        };
        xmppMgr.registerConnectionChangeListener(listener);
    }
    
    public static XmppMuc getInstance(Context ctx) {
        if (sXmppMuc == null) {
            sXmppMuc = new XmppMuc(ctx);
        }
        return sXmppMuc;
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
    public void writeRoom(String number, String contact, String message, int mode) throws XMPPException {
        writeRoom(number, contact, new XmppMsg(message), mode);
    }
    
    /**
     * Writes a formated message to a room and creates the room if necessary,
     * followed by an invite to the default notification address 
     * to join the room
     * 
     * @param number
     * @param contact
     * @param message
     * @throws XMPPException
     */
    public void writeRoom(String number, String contact, XmppMsg message, int mode) throws XMPPException {
        MultiUserChat muc;
        muc = inviteRoom(number, contact, mode);
        if (muc != null) {
            try {
                Message msg = new Message(muc.getRoom());
                msg.setBody(message.generateFmtTxt());
                if (mode == MODE_SHELL) {
                    XHTMLManager.addBody(msg, message.generateXHTMLText().toString());
                }
                msg.setType(Message.Type.groupchat);
                muc.sendMessage(msg);
            } catch (Exception e) {
                muc.sendMessage(message.generateTxt());
            }
        }
    }
    
    /**
     * Invites the user to a room for the given contact name and number
     * if the user (or someone else) writes to this room, a SMS is send to the number
     * 
     * @param number
     * @param mName
     * @return true if successful, otherwise false
     * @throws XMPPException 
     */
	public MultiUserChat inviteRoom(String number, String contact, int mode)
			throws XMPPException {
		MultiUserChat muc;
		if (!mRooms.containsKey(number)) {
			muc = createRoom(number, contact, mode);
			mRooms.put(number, muc);

		} else {
			muc = mRooms.get(number);
			// TODO: test if occupants contains also the sender (in case we
			// invite other people)
			if (muc != null && muc.getOccupantsCount() < 2) {
				muc.invite(mSettings.notifiedAddress, "SMS conversation with "
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
    	return mRooms.containsKey(number);
    }    
    
    /**
     * Returns the MultiUserChat given in roomname, 
     * which is a full JID (e.g. room@conference.jabber.com),
     * if the room is in your internal data structure.
     * Otherwise null will be returned
     * 
     * 
     * @param roomname - the full roomname as JID
     * @return the room or null
     */
    public MultiUserChat getRoomViaRoomName(String roomname) {
        Collection<MultiUserChat> mucSet = mRooms.values();
        for(MultiUserChat muc : mucSet) {
            if(muc.getRoom().equals(roomname)) {
                return muc;
            }
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
    private MultiUserChat createRoom(String number, String name, int mode) throws XMPPException {
        String room = getRoomString(number, name);
        MultiUserChat multiUserChat = null;
        boolean passwordMode = false;
        Integer randomInt;
 
        // With "@conference.jabber.org" messages are sent several times...
        // Jwchat seems to work fine and is the default
        final String roomJID;
        final String subjectInviteStr;

        do {
            randomInt = mRndGen.nextInt();
        } while (mRoomNumbers.contains(randomInt));

        
        // TODO localize
        switch (mode) {
            case MODE_SMS:
                roomJID = ROOM_START_TAG + randomInt + "_SMS_" + mSettings.login.replaceAll("@", "_") + "@" + getMUCServer();
                subjectInviteStr =  "SMS conversation with " + getRoomString(number, name);
                break;

            case MODE_SHELL:
                roomJID = ROOM_START_TAG + randomInt + "_Shell_" + mSettings.login.replaceAll("@", "_") + "@" + getMUCServer();
                subjectInviteStr =  "New Android Terminal " + getRoomString(number, name);
                break;

            default:
                roomJID = null;
                subjectInviteStr = null;
                break;
        }
        
        // See issue 136
        try {
            multiUserChat = new MultiUserChat(mConnection, roomJID);
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
                if (mSettings.useDifferentAccount) {
                    owners.add(mSettings.login);
                    owners.add(mSettings.notifiedAddress);
                } else {
                    owners.add(mSettings.login);
                }
                submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            } catch (Exception ex) {
                // Password protected MUC fallback code begins here
                GoogleAnalyticsHelper.trackAndLogWarning("Unable to configure room owners on Server " + getMUCServer()
                        + ". Falling back to room passwords", ex);
                // Seee http://xmpp.org/registrar/formtypes.html#http:--jabber.org-protocol-mucroomconfig
                try {
                    submitForm.setAnswer("muc#roomconfig_passwordprotectedroom", true);
                    submitForm.setAnswer("muc#roomconfig_roomsecret", mSettings.roomPassword);
                } catch (IllegalArgumentException iae) {
                    // If a server doesn't provide even password protected MUC, the setAnswer
                    // call will result in an IllegalARgumentException, which we wrap into an XMPPException
                    // See also Issue 247 http://code.google.com/p/gtalksms/issues/detail?id=247
                    throw new XMPPException(iae);
                }
                passwordMode = true;
            }

            if (!passwordMode) {
                submitForm.setAnswer("muc#roomconfig_membersonly", true);
            }

            multiUserChat.sendConfigurationForm(submitForm);
            multiUserChat.changeSubject(subjectInviteStr);
        } catch (XMPPException e1) {
            GoogleAnalyticsHelper.trackAndLogWarning("Unable to send conference room configuration form.", e1);
            send(mCtx.getString(R.string.chat_sms_muc_conf_error, e1.getMessage()));
            // then we also should not send an invite as the room will be locked
            throw e1;
        }

        multiUserChat.invite(mSettings.notifiedAddress, subjectInviteStr);
        registerRoom(multiUserChat, number, name, randomInt, mode);
        return multiUserChat;
    }
    
    private String getMUCServer() {
        if (mMucServer == null || mSettings.forceMucServer) {
            return mSettings.mucServer;
        } else {
            return mMucServer;
        }
    }
    
    private void rejoinRooms() {
    	String[][] mucDB = mMucHelper.getAllMUC();
    	if (mucDB == null)
    		return;
    		
    	for (int i = 0; i < mucDB.length; i++) {
    		RoomInfo info = getRoomInfo(mucDB[i][0]);
    		// if info is not null, the room exists on the server
    		// so lets check if we can reuse it
			if (info != null) {
				MultiUserChat muc = new MultiUserChat(mConnection, mucDB[i][0]);
				String name = ContactsManager.getContactName(mCtx,
						mucDB[i][1]);
				try {
					if (info.isPasswordProtected()) {
						muc.join(name, mSettings.roomPassword, mDiscussionHistory, REPLAY_TIMEOUT);
					} else {
						muc.join(name, null, mDiscussionHistory, REPLAY_TIMEOUT);
						// check here if we are still owner of these room, in case somebody has taken over ownership
						// sadly this (getOwners()) throws sometimes a 403 on my openfire server
						try {
						if (!affilateCheck(muc.getOwners())) {
							if (mSettings.debugLog) 
								Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because of affilateCheck failed");
							leaveRoom(muc);
							continue;
						}
						// catch the 403 that sometimes shows up and fall back to some easier check if the room
						// is still under our control
                        } catch (XMPPException e) {
                            if (!(info.isMembersOnly() || info.isPasswordProtected())) {
                                if (mSettings.debugLog)
                                    Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because of membersOnly=" 
                                            + info.isMembersOnly() + " passwordProteced=" + info.isPasswordProtected());
                                leaveRoom(muc);
                                continue;
                            }
                        }
					}
					// looks like there is no one in the room
					if (info.getOccupantsCount() > 0) {
						if (mSettings.debugLog)
							Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because there is no one there");
						leaveRoom(muc);
						continue;
					}
				} catch (XMPPException e) {
					if (mSettings.debugLog) {
						Log.i(Tools.LOG_TAG, "rejoinRooms: leaving " + muc.getRoom() + " because of XMMPException", e);
					}
					// TODO decide in which cases it would be the best to remove the room from the db, because of a persistent error
					// and in which cases the error will not be permanent
					if (mConnection.isAuthenticated()) {
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
		mMucHelper.deleteMUC(muc.getRoom());
		if (muc.isJoined())
			muc.leave();

		if (mRooms.size() > 0) {
			Integer i = getRoomInt(muc.getRoom());
			String number = mMucHelper.getNumber(muc.getRoom());
			mRoomNumbers.remove(i);
			mRooms.remove(number);
		}
    }
    
    private void registerRoom(MultiUserChat muc, String number, String name) {
    	String roomJID = muc.getRoom();
    	Integer randomInt = getRoomInt(roomJID);
    	// TODO This contains not so safe, if we have a user that has 
    	// the string "_SMS_" in his name. A cleaner way would be to 
    	// extend the MUC DB with this information.
    	registerRoom(muc, number, name, randomInt, roomJID.toUpperCase().contains("_SMS_") ? MODE_SMS : MODE_SHELL);
    }
    
    private void registerRoom(MultiUserChat muc, String number, String name, Integer randomInt, int mode) {
        MUCPacketListener chatListener = new MUCPacketListener(number, muc, name, mode, mCtx);
        muc.addMessageListener(chatListener);
        mRoomNumbers.add(randomInt);
        mRooms.put(number, muc);
        mMucHelper.addMUC(muc.getRoom(), number);
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
    		info = MultiUserChat.getRoomInfo(mConnection, room);
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
    	return jids.contains(mSettings.login);    	
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
    
    private void send(String msg) {
        Tools.send(msg, null, mCtx);
    }    
}