package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;

public class XmppBuddies implements RosterListener {
    
    private static Context sContext;
    private static XMPPConnection sConnection;
    private static XmppBuddies sXmppBuddies;
    private static SettingsManager sSettings;
    private static Roster sRoster;
    
    private XmppBuddies(Context context) {
        sContext = context;
        sSettings = SettingsManager.getSettingsManager(context);

    }

	public void registerListener(XmppManager xmppMgr) {
		XmppConnectionChangeListener listener = new XmppConnectionChangeListener() {
			public void newConnection(XMPPConnection connection) {
				sConnection = connection;
				sRoster = connection.getRoster();
				checkNotificationAddressRoster();

				connection.addPacketListener(new PresencePacketListener(connection, sSettings), new PacketTypeFilter(
				        Presence.class));

				try {
					connection.getRoster().addRosterListener(XmppBuddies.this);
					retrieveFriendList();
				} catch (Exception ex) {
					Log.e("Failed to setup XMPP friend list roster.", ex);
				}
			}
		};
		xmppMgr.registerConnectionChangeListener(listener);
	}
    
    public static XmppBuddies getInstance(Context ctx) {
        if (sXmppBuddies == null) {
            sXmppBuddies = new XmppBuddies(ctx);
        }
        return sXmppBuddies;
    }

    public void addFriend(String userID) {
        if (sRoster != null) {
            if (!sRoster.contains(userID)) {
                try {
                    sRoster.createEntry(userID, StringUtils.parseBareAddress(userID), null);
                    retrieveFriendList();
                } catch (Exception e) {
                    System.err.println("Error in adding friend " + e.getMessage());
                }
            } else {
                RosterEntry rosterEntry = sRoster.getEntry(userID);
                RosterPacket.ItemType type = rosterEntry.getType();
                switch (type) {
                    case from:
                        requestSubscription(userID, sConnection);
                        break;
                    case to:
                        grantSubscription(userID, sConnection);
                        break;
                    case none:
                        grantSubscription(userID, sConnection);
                        requestSubscription(userID, sConnection);
                        break;
                    case both:
                    default:
                        break;
                }
            }
        }
    }
    
    public boolean removeFriend(String userID) {
        if (sConnection != null && sConnection.isConnected()) {
            Roster roster = sConnection.getRoster();
            if (roster.contains(userID)) {
                try {
                    roster.removeEntry(roster.getEntry(userID));
                    return true;
                } catch (Exception e) {
                    System.err.println("Error in removing friend " + e.getMessage());
                }
            }
        }
        return false;
    }
    
    public boolean renameFriend(String userID, String name) {
        if (sConnection != null && sConnection.isConnected()) {
            Roster roster = sConnection.getRoster();
            if (roster.contains(userID)) {
                RosterEntry entry  = roster.getEntry(userID);
                try {
                    entry.setName(name);
                } catch (SmackException.NotConnectedException e) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * retrieves the current xmpp rooster
     * and sends a broadcast ACTION_XMPP_PRESENCE_CHANGED
     * for every friend
     * does nothing if we are offline
     * 
     * @return
     */
    public ArrayList<XmppFriend> retrieveFriendList() {
        
        ArrayList<XmppFriend> friends = new ArrayList<XmppFriend>();

        if (sConnection != null && sConnection.isAuthenticated()) {
            try {
                String userID;
                String status;
                Roster roster = sConnection.getRoster();

                for (RosterEntry r : roster.getEntries()) {
                    userID = r.getUser();
                    status = retrieveStatusMessage(userID);
                    friends.add(new XmppFriend(userID, r.getName(), status, retrieveState(userID)));
                }

                sendFriendList(friends);
            } catch (Exception ex) {
                Log.w("Failed to retrieve Xmpp Friend list", ex);
            }
        }
        
        return friends;
    }
    
    /**
     * sends an XMPP_PRESENCE_CHANGED intent for every known xmpp rooster item (friend)
     * with the actual status information
     * 
     * @param list
     */
    void sendFriendList(ArrayList<XmppFriend> list) {
        
        for (XmppFriend xmppFriend : list) {
            Intent intent = new Intent(MainService.ACTION_XMPP_PRESENCE_CHANGED);
            intent.putExtra("userid", xmppFriend.mId);
            intent.putExtra("name", xmppFriend.mName == null ? xmppFriend.mId : xmppFriend.mName);
            intent.putExtra("status", xmppFriend.mStatus);
            intent.putExtra("state", xmppFriend.mState);
            sContext.sendBroadcast(intent);
        }
    }
    
    /**
     * returns the status message for a given bare or full JID
     * 
     * @param userID
     * @return
     */
    String retrieveStatusMessage(String userID) {
        String userStatus; // default return value

        try {
            userStatus = sConnection.getRoster().getPresence(userID).getStatus();
        } catch (NullPointerException e) {
            Log.e("Invalid connection or user in retrieveStatus() - NPE", e);
            userStatus = "";
        }
        // Server may set their status to null; we want empty string
        if (userStatus == null) {
            userStatus = "";
        }

        return userStatus;
    }

    int retrieveState(String userID) {
        int userState = XmppFriend.OFFLINE; // default return value
        Presence userFromServer;

        try {
            userFromServer = sConnection.getRoster().getPresence(userID);
            userState = retrieveState(userFromServer.getMode(), userFromServer.isAvailable());
        } catch (NullPointerException e) {
            Log.e("retrieveState(): Invalid connection or user - NPE", e);
        }

        return userState;
    }
    
    /**
     * Maps the smack internal userMode enums into our int status mode flags
     * 
     * @param userMode
     * @param isOnline
     * @return
     */
    // TODO do we need the isOnline boolean?
    // Mode.available should be an equivalent
    int retrieveState(Mode userMode, boolean isOnline) {
        int userState = XmppFriend.OFFLINE; // default return value
        
        if (userMode == Mode.dnd) {
            userState = XmppFriend.BUSY;
        } else if (userMode == Mode.away
                || userMode == Mode.xa) {
            userState = XmppFriend.AWAY;
        } else if (isOnline) {
            userState = XmppFriend.ONLINE;
        }

        return userState;
    }

    @Override
    public void entriesAdded(Collection<String> addresses) {
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {
    }

    // careful, this method does also get called by the SmackListener Thread
    @Override    
    public void presenceChanged(Presence presence) {
        String bareUserId = StringUtils.parseBareAddress(presence.getFrom());
        
        Intent intent = new Intent(MainService.ACTION_XMPP_PRESENCE_CHANGED);
        intent.putExtra("userid", bareUserId);
        intent.putExtra("fullid", presence.getFrom());
        intent.putExtra("state", retrieveState(presence.getMode(), presence.isAvailable()));
        intent.putExtra("status", presence.getStatus());
        sContext.sendBroadcast(intent);
        
        // TODO Make this a general intent action.NOTIFICATION_ADDRESS_AVAILABLE
        // and handle it for example within XmppPresenceStatus
        // if the notification address is/has become available, update the resource status string
        if (sSettings.getNotifiedAddresses().contains(bareUserId) && presence.isAvailable()) {
            intent = new Intent(MainService.ACTION_COMMAND);
            intent.setClass(sContext, MainService.class);
            intent.putExtra("cmd", "batt");
            intent.putExtra("args", "silent");
            MainService.sendToServiceHandler(intent);
        }       
    }
    
    /**
     * Checks if the notification address is available
     * return also true if no roster is loaded
     * @return
     */
    public boolean isNotificationAddressAvailable() {
        if (sRoster != null) {
            // getPresence retrieves eventually the status of the notified Address in an internal data structure cache
            // thus avoiding an extra data packet
            for (String notifiedAddress : sSettings.getNotifiedAddresses().getAll()) {
                Presence presence = sRoster.getPresence(notifiedAddress);
                if (presence.isAvailable()) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
    
    private void checkNotificationAddressRoster() {
        for (String notifiedAddress : sSettings.getNotifiedAddresses().getAll()) {
            addFriend(notifiedAddress);
        }
    }
    
    /**
     * grants the given JID the subscription (e.g. viewing your online state)
     * 
     * @param jid
     * @param connection
     */
    public static void grantSubscription(String jid, XMPPConnection connection) {
        Presence presence = new Presence(Presence.Type.subscribed);
        sendPresenceTo(jid, presence, connection);
    }
    
    /**
     * request the subscription from a given JID
     * 
     * @param jid
     * @param connection
     */
    private static void requestSubscription(String jid, XMPPConnection connection) {
        Presence presence = new Presence(Presence.Type.subscribe);
        sendPresenceTo(jid, presence, connection);
    }
    
    private static void sendPresenceTo(String to, Presence presence, XMPPConnection connection) {
        presence.setTo(to);
        try {
            connection.sendPacket(presence);
        } catch (SmackException.NotConnectedException e) {
            Log.e("Failed to send presence.", e);
        }
    }
}
