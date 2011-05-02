package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
//import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

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

//    public void addFriend(String userID) {
//        Roster roster = null;
//        String nickname = null;
//
//        nickname = StringUtils.parseBareAddress(userID);
//
//        roster = _connection.getRoster();
//        if (!roster.contains(userID)) {
//            try {
//                roster.createEntry(userID, nickname, null);
//            } catch (XMPPException e) {
//                System.err.println("Error in adding friend");
//            }
//        }
//
//        return;
//    }
    
    /**
     * retrieves the current xmpp rooster
     * and sends a broadcast ACTION_XMPP_PRESENCE_CHANGED
     * for every friend
     * 
     * @return
     */
    public ArrayList<XmppFriend> retrieveFriendList() {
        
        ArrayList<XmppFriend> friends = new ArrayList<XmppFriend>();

        if (sConnection == null) {
            return friends;
        }
        
        try {
            String userID = null;
            String status = null;
            Roster roster = sConnection.getRoster();
    
            for (RosterEntry r : roster.getEntries()) {
                userID = r.getUser();
                status = retrieveStatus(userID);
                friends.add(new XmppFriend(userID, r.getName(), status, retrieveState(userID)));
            }
    
            sendFriendList(friends);
        } catch (Exception ex) {
            GoogleAnalyticsHelper.trackAndLogWarning("Failed to retrieve Xmpp Friend list", ex);
        }
        
        return friends;
    }
    
    /**
     * sends an XMPP_PRESENCE_CHANGED intent for every known xmpp rooster item (friend)
     * with the actual status information
     * 
     * @param list
     */
    public void sendFriendList(ArrayList<XmppFriend> list) {
        
        for (XmppFriend xmppFriend : list) {
            Intent intent = new Intent(MainService.ACTION_XMPP_PRESENCE_CHANGED);
            intent.putExtra("userid", xmppFriend.id);
            intent.putExtra("name", xmppFriend.name == null ? xmppFriend.id : xmppFriend.name);
            intent.putExtra("status", xmppFriend.status);
            intent.putExtra("state", xmppFriend.state);
            sContext.sendBroadcast(intent);
        }
    }

    public String retrieveStatus(String userID) {
        String userStatus = ""; // default return value

        try {
            userStatus = sConnection.getRoster().getPresence(userID).getStatus();
        } catch (NullPointerException e) {
            GoogleAnalyticsHelper.trackAndLogError("Invalid connection or user in retrieveStatus() - NPE");
            userStatus = "";
        }
        // Server may set their status to null; we want empty string
        if (userStatus == null) {
            userStatus = "";
        }

        return userStatus;
    }

    public int retrieveState(String userID) {
        int userState = XmppFriend.OFFLINE; // default return value
        Presence userFromServer = null;

        try {
            userFromServer = sConnection.getRoster().getPresence(userID);
            userState = retrieveState(userFromServer.getMode(), userFromServer.isAvailable());
        } catch (NullPointerException e) {
            GoogleAnalyticsHelper.trackAndLogError("retrieveState(): Invalid connection or user - NPE");
        }

        return userState;
    }
    

    public int retrieveState(Mode userMode, boolean isOnline) {
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
        // TODO Auto-generated method stub
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
        // TODO Auto-generated method stub
    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {
        // TODO Auto-generated method stub
    }

    @Override
    public void presenceChanged(Presence presence) {
        Intent intent = new Intent(MainService.ACTION_XMPP_PRESENCE_CHANGED);
        intent.putExtra("userid", StringUtils.parseBareAddress(presence.getFrom()));
        intent.putExtra("fullid", presence.getFrom());
        intent.putExtra("state", retrieveState(presence.getMode(), presence.isAvailable()));
        intent.putExtra("status", presence.getStatus());
        sContext.sendBroadcast(intent);
    }
    
    public boolean isNotificationAddressAvailable() {
        if (sRoster != null) {
            // getPresence retrieves eventually the status of the notified Address in an internal data structure cache
            // thus avoiding an extra data packet
            Presence presence = sRoster.getPresence(sSettings.notifiedAddress);
            return presence.isAvailable();
        }
        return true;
    }
    
    private void checkNotificationAddressRoster() {
        if (sRoster != null && sSettings.useDifferentAccount) {
            if (!sRoster.contains(sSettings.notifiedAddress)) {
                try {
                    // this sends a new subscription request to the other side
                    sRoster.createEntry(sSettings.notifiedAddress, sSettings.notifiedAddress, null);
                } catch (XMPPException e) { /* Ignore */  }
            } else {
                RosterEntry rosterEntry = sRoster.getEntry(sSettings.notifiedAddress);
                RosterPacket.ItemType type = rosterEntry.getType();
                switch (type) {
                case both:
                    break;
                case from:
                    requestSubscription(sSettings.notifiedAddress, sConnection);
                    break;
                case to:
                    grantSubscription(sSettings.notifiedAddress, sConnection);
                    break;
                case none:
                    grantSubscription(sSettings.notifiedAddress, sConnection);
                    requestSubscription(sSettings.notifiedAddress, sConnection);
                    break;
                default:
                    break;
                }
                
            }
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
    public static void requestSubscription(String jid, XMPPConnection connection) {
        Presence presence = new Presence(Presence.Type.subscribe);
        sendPresenceTo(jid, presence, connection);
    }
    
    private static void sendPresenceTo(String to, Presence presence, XMPPConnection connection) {
        presence.setTo(to);
        connection.sendPacket(presence); 
    }
}
