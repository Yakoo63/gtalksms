package com.googlecode.gtalksms.receivers;

import com.googlecode.gtalksms.xmpp.XmppEntityCapsCache;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StorageLowReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        XmppEntityCapsCache.emptyCache();        
    }

}
