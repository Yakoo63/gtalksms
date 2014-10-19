package com.googlecode.gtalksms.xmpp;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.SettingsManager;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.address.MultipleRecipientManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Send a message to all resources not blacklisted
 * Created by Florent on 10/11/13.
 */
public class XmppMultipleRecipientManager {
    private static SettingsManager sSettingsManager;

    /**
     * Set the setting manager.
     * To be called First
     * @param manager Setting Manager of the Application
     */
    public static void setSettingsManager(SettingsManager manager) { sSettingsManager = manager; }

    /**
     * Send a message to all resources not blacklisted
     * @param connection Xmpp connection
     * @param msg Message
     * @return True if succeeded, false otherwise
     */
    public static boolean send(XMPPConnection connection, Message msg) {

        List<String> toList = getAllowedNotifiedAddresses(connection);
        toList = filterHangoutAddresses(toList);

        if (toList.size() > 0) {
            try {
                Log.d("Sending message to " + toList.size() + " recipients");
                MultipleRecipientManager.send(connection, msg, toList, null, null);
            } catch (Exception e) {
                Log.d("Failed to send message using MultipleRecipientManager method. Sending messages one by one. Ex: " + e.getMessage());
                for (String notifiedAddress : toList) {
                    msg.setTo(notifiedAddress);
                    try {
                        connection.sendPacket(msg);
                    } catch (SmackException.NotConnectedException ex) {
                        Log.e("Send message error. Ex: " + ex.getMessage());
                    }
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Looking for JIDs not blacklisted in the settings
     * @param connection Used to retrieved connected resources
     * @return the list of allowed resources
     */
    private static List<String> getAllowedNotifiedAddresses(XMPPConnection connection) {
        List<String> toList = new LinkedList<String>();
        List<String> toListWithEmptyResources = new LinkedList<String>();

        // Removing blacklisted resources for notified addresses
        for (String notifiedAddress : sSettingsManager.getNotifiedAddresses().getAll()) {
            List<Presence> presences = connection.getRoster().getPresences(notifiedAddress);
            for (Presence p : presences) {
                String toPresence = p.getFrom();
                String toResource = StringUtils.parseResource(toPresence);
                // Don't send messages to GTalk Android devices
                // It would be nice if there was a better way to detect
                // an Android gTalk XMPP client, but currently there is none
                if (toResource != null && !toResource.equals("")) {
                    boolean found = false;
                    for (String blockedResourcePrefix : sSettingsManager.getBlockedResourcePrefixes().getAll()) {
                        if (toResource.startsWith(blockedResourcePrefix)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        Log.d("Sending message to " + toPresence);
                        toList.add(toPresence);
                    } else {
                        Log.d("Message not sent to " + toPresence + " because resource is blacklisted");
                    }
                } else {
                    Log.d("Message not sent to " + toPresence + " because resource is empty");
                    toListWithEmptyResources.add(toPresence);
                }
            }
        }

        if (toList.size() > 0) {
            return toList;
        }
        Log.d("No valid address with resources. Trying with empty resources.");
        return toListWithEmptyResources;
    }

    /**
     * Replace all JIDs associated to at least 1 hangout connection per the Bare Address
     * @param toList list of allowed JIDs
     * @return list of JIDs (all except hangout) and Bare Address (for hangout)
     */
    private static List<String> filterHangoutAddresses(List<String> toList) {
        List<String> results = new LinkedList<String>();

        Log.d("Looking for hangout addresses");
        for (String notifiedAddress : toList) {
            String toResource = StringUtils.parseResource(notifiedAddress);
            if (toResource.toLowerCase().startsWith("messaging")) {
                Log.d("Hangout address detected: " + notifiedAddress);
                String bareAddress = StringUtils.parseBareAddress(notifiedAddress);
                if (!results.contains(bareAddress)) {
                    results.add(bareAddress);
                    Log.d("Sending message to " + bareAddress);
                }
            }
        }

        for (String notifiedAddress : toList) {
            String bareAddress = StringUtils.parseBareAddress(notifiedAddress);
            if (!results.contains(bareAddress)) {
                results.add(notifiedAddress);
            }
        }

        return results;
    }
}
