package com.googlecode.gtalksms.xmpp;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.content.Context;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.Tools;

import org.jivesoftware.smack.PacketListener;

public class ChatPacketListener implements PacketListener {
		private SettingsManager settings;
		private Context ctx;
		private XMPPConnection connection;
		
		public ChatPacketListener(XMPPConnection connection, Context ctx) {
			this.connection = connection;
			this.ctx = ctx;
			this.settings = SettingsManager.getSettingsManager(ctx);
		}
		
        public void processPacket(Packet packet) {
            Message message = (Message) packet;
            String from = message.getFrom();
            
            if (from.toLowerCase().startsWith(settings.notifiedAddress.toLowerCase() + "/") 
                    && !message.getFrom().equals(connection.getUser())
                    && message.getBody() != null) {
                if (settings.debugLog)
                    Log.i(Tools.LOG_TAG, "XMPP packet received - sending Intent: " + MainService.ACTION_XMPP_MESSAGE_RECEIVED);
                
                Tools.startSvcXMPPMsg(ctx, message.getBody(), from);
            } else if (settings.debugLog) {
                if (!from.toLowerCase().startsWith(settings.notifiedAddress.toLowerCase() + "/")) {
                    Log.i(Tools.LOG_TAG, "XMPP packet received - but from address \"" + from.toLowerCase() + "\" does not match notification address \"" 
                            + settings.notifiedAddress.toLowerCase() + "\"");
                } else if (message.getFrom().equals(connection.getUser())) {
                    Log.i(Tools.LOG_TAG, "XMPP packet received - but from the same user as the XMPP connection");
                } else if (message.getBody() == null) {
                    Log.i(Tools.LOG_TAG, "XMPP Packet received - but without body (body == null)");
                }
            }
        }
}
