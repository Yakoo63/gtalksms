package com.googlecode.gtalksms.xmpp;

import com.googlecode.gtalksms.tools.GoogleAnalyticsHelper;

public class XmppFriend {
    
    public static final int ONLINE = 0;
    public static final int AWAY = 1;
    public static final int EXAWAY = 2;
    public static final int BUSY = 3;
    public static final int FFC = 4;
    public static final int OFFLINE = 5;
    

//    public static enum UserStateType { //TODO enum is bad in android programming
//        OFFLINE,
//        BUSY,
//        AWAY,
//        ONLINE
//    }
    
    public String id;
    public String name;
    public String status;
    public int state;

    public XmppFriend(String userID, String username, String retrieveStatus, int retrieveState) {
        id = userID;
        name = username;
        status = retrieveStatus;
        state = retrieveState;
    }
    
    public static String stateToString(int state) {
        switch(state) {
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
             GoogleAnalyticsHelper.trackAndLogError("XMPP Friend state unknown: " + state);
             return "unkown";
        }
    }
}

