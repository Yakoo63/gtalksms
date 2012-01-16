package com.googlecode.gtalksms.receivers;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.SettingsManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PublicIntentReceiver extends BroadcastReceiver {
	private static PublicIntentReceiver sPublicIntentReceiver;
	private static IntentFilter sIntentFilter;
	
	private SettingsManager mSettings;
	private Context mContext;
	
	static {
		sIntentFilter = new IntentFilter();
		// ACTION_CONNECT is received by filter within the Apps Manifest
		// sIntentFilter.addAction(MainService.ACTION_CONNECT);
		sIntentFilter.addAction(MainService.ACTION_COMMAND);
		sIntentFilter.addAction(MainService.ACTION_SEND);
		sIntentFilter.addAction(MainService.ACTION_TOGGLE);
	}
	
	private PublicIntentReceiver(Context context) {
		this.mSettings = SettingsManager.getSettingsManager(context);
		this.mContext = context;
	}
	
	public static PublicIntentReceiver getReceiver(Context ctx) {
		if (sPublicIntentReceiver == null)
			sPublicIntentReceiver = new PublicIntentReceiver(ctx);
		
		return sPublicIntentReceiver;
	}
	
	public void onServiceStart() {
		mContext.registerReceiver(this, sIntentFilter);
	}
	
	public void onServiceStop() {
		mContext.unregisterReceiver(this);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (sPublicIntentReceiver == null)
			sPublicIntentReceiver = new PublicIntentReceiver(context);
		
		String token = intent.getStringExtra("token");
		if (mSettings.publicIntentsEnabled) {
			if (mSettings.publicIntentTokenRequired) {
				if (token == null 
						|| !mSettings.publicIntentToken.equals(token)) {
					// token required but no token set or it doesn't macht
					Log.w("Public intent without correct security token received");
					return;
				}
			}					
			context.startService(intent);		
		}			
	}
}