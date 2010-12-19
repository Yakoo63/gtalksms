package com.googlecode.gtalksms;


public interface XmppListener {
    void onConnectionStatusChanged(int oldStatus, int status);
    void onMessageReceived(String message);
    void onPresenceStatusChanged(String person, String status);
}