package com.googlecode.gtalksms.xmpp;

import java.util.Date;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.packet.DelayInformation;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.Tools;

class MUCPacketListener implements PacketListener {
	private String number;
	private String name; // the name of GTalkSMS in this room
	private Date lastDate;
	private MultiUserChat muc;
	private String roomName;
	private SettingsManager settings;
	private Context ctx;

	public MUCPacketListener(String number, MultiUserChat muc, String name, Context ctx) {
		this.name = name;
		this.number = number;
		this.lastDate = new Date(0);
		this.muc = muc;
		this.roomName = muc.getRoom();
		this.settings = SettingsManager.getSettingsManager(ctx);
		this.ctx = ctx;
	}

	@Override
	public void processPacket(Packet packet) {
		Message message = (Message) packet;
		String from = message.getFrom();
		String fromBareResource = StringUtils.parseResource(from);

		if (settings.debugLog)
			Log.d(Tools.LOG_TAG, "MUCPacketListener: packet received. messageFrom=" + message.getFrom()
					+ " messageBody=" + message.getBody());
		
		// messages from the room JID itself, are matched here, because they have no resource part
		// these are normally status messages about the room
		if (from.equals(roomName)) {
			Intent intent = new Intent(MainService.ACTION_SEND);
			intent.putExtra("message", name + ": " + message.getBody());
			// fromMuc sounds right at first, but it servers no purpose here atm
			// intent.putExtra("fromMuc", true);
			ctx.startService(intent);
		} else if (!fromBareResource.equals(name)) {
			if (message.getBody() != null) {
				DelayInformation inf = (DelayInformation) message.getExtension(
						"x", "jabber:x:delay");
				Date sentDate;
				if (inf != null) {
					sentDate = inf.getStamp();
				} else {
					sentDate = new Date();
				}

				if (sentDate.compareTo(lastDate) > 0) {
					Intent intent = new Intent(MainService.ACTION_COMMAND);
					intent.setClass(ctx, MainService.class);

					intent.putExtra("from", roomName);
					intent.putExtra("cmd", "sms");
					intent.putExtra("fromMuc", true);
					// if there are more than 2 users in the
					// room, we include also a tag in the response of the sms message
					if (muc.getOccupantsCount() > 2) { 
						intent.putExtra("args", number + ":" + fromBareResource + ": "
								+ message.getBody());
					} else {
						intent.putExtra("args", number + ":"
								+ message.getBody());
					}

					ctx.startService(intent);
					lastDate = sentDate;
				} else {
					// this seems to be caused by the history replay of MUC rooms
					// which is now disabled
					Log.w(Tools.LOG_TAG, "MUCPacketListener: Received old message: date="
							+ sentDate.toLocaleString() + " ; message="
							+ message.getBody());
				}
			}
		} else if (settings.debugLog) {
			Log.i(Tools.LOG_TAG, "MUCPacketListener: Received message which equals our room nick. message=" + message.getBody());
		}
	}
}