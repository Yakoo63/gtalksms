package com.googlecode.gtalksms.xmpp;

public class XmppFriend {

    public static enum UserStateType { //TODO enum is bad in android programming
        OFFLINE,
        BUSY,
        AWAY,
        ONLINE
    }
    
    public String id;
    public String name;
    public String status;
    public UserStateType state;

    public XmppFriend(String userID, String username, String retrieveStatus, UserStateType retrieveState) {
        id = userID;
        name = username;
        status = retrieveStatus;
        state = retrieveState;
    }
}

