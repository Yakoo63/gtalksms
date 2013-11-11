package com.googlecode.gtalksms.xmpp;

import com.googlecode.gtalksms.tools.Log;

public class XmppFriend {
    
    public static final int ONLINE = 0;
    public static final int AWAY = 1;
    public static final int EXAWAY = 2;
    public static final int BUSY = 3;
    public static final int FFC = 4;
    public static final int OFFLINE = 5;
    
    public final String mId;
    public final String mName;
    public final String mStatus;
    public final int mState;

    public XmppFriend(String userID, String username, String retrieveStatus, int retrieveState) {
        mId = userID;
        mName = username;
        mStatus = retrieveStatus;
        mState = retrieveState;
    }
    
    public static String stateToString(int state) {
        switch (state) {
        case ONLINE:
            return "Online";
        case AWAY:
            return "Away";
        case EXAWAY:
            return "Extended Away";
        case BUSY:
            return "Busy";
        case FFC:
            return "Free for chat";
        case OFFLINE:
            return "Offline";
        default:
            Log.e("XMPP Friend state unknown: " + state);
            return "Unknown";
        }
    }
}

