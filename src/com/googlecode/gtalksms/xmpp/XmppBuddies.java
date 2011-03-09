package com.googlecode.gtalksms.xmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;
import android.content.Intent;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

public class XmppBuddies implements RosterListener {
    
    private Context _context;
    private XMPPConnection _connection;
    
    public XmppBuddies(Context context, SettingsManager settings) {
        _context = context;
    }
    
    public void initialize(XMPPConnection connection) {
        _connection = connection;
    }
    
    public void addFriend(String userID) {
        Roster roster = null;
        String nickname = null;

        nickname = StringUtils.parseBareAddress(userID);

        roster = _connection.getRoster();
        if (!roster.contains(userID)) {
            try {
                roster.createEntry(userID, nickname, null);
            } catch (XMPPException e) {
                System.err.println("Error in adding friend");
            }
        }

        return;
    }
    
    public ArrayList<XmppFriend> retrieveFriendList() {
        
        ArrayList<XmppFriend> friends = new ArrayList<XmppFriend>();

        if (_connection == null) {
            return friends;
        }
        
        try {
            String userID = null;
            String status = null;
            Roster roster = _connection.getRoster();
    
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
    
    public void sendFriendList(ArrayList<XmppFriend> list) {
        
        for (XmppFriend xmppFriend : list) {
            Intent intent = new Intent(MainService.ACTION_XMPP_PRESENCE_CHANGED);
            intent.putExtra("userid", xmppFriend.id);
            intent.putExtra("name", xmppFriend.name == null ? xmppFriend.id : xmppFriend.name);
            intent.putExtra("status", xmppFriend.status);
            intent.putExtra("state", xmppFriend.state);
            _context.sendBroadcast(intent);
        }
    }

    public String retrieveStatus(String userID) {
        String userStatus = ""; // default return value

        try {
            userStatus = _connection.getRoster().getPresence(userID).getStatus();
        } catch (NullPointerException e) {
            System.err.println("Invalid connection or user in retrieveStatus()");
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
            userFromServer = _connection.getRoster().getPresence(userID);
            userState = retrieveState(userFromServer.getMode(), userFromServer.isAvailable());
        } catch (NullPointerException e) {
            GoogleAnalyticsHelper.trackAndLogError("retrieveState(): Invalid connection or user");
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
        _context.sendBroadcast(intent);
    }
}
