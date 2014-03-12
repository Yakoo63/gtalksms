package com.googlecode.gtalksms.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.tools.Tools;

public class WidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // The widget needs to be updated - just ask the service to broadcast it's current
        // status - the actual update happens when we receive that...
        Tools.startSvcIntent(context, MainService.ACTION_BROADCAST_STATUS);

        doUpdate(context, appWidgetManager, appWidgetIds, -1);
    }

    void doUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, int state) {
        // Create an Intent to launch activity
        Intent intent = new Intent(MainService.ACTION_WIDGET_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get the layout for the AppWidget and attach an on-click listener to the button
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
        views.setOnClickPendingIntent(R.id.Button, pendingIntent);
        
        // set the icons.
        switch (state) {
            case XmppManager.CONNECTED:
                views.setImageViewResource(R.id.Button, R.drawable.icon_green);
                break;
            case XmppManager.DISCONNECTED:
                views.setImageViewResource(R.id.Button, R.drawable.icon_red);
                break;
            case XmppManager.DISCONNECTING:
            case XmppManager.WAITING_TO_CONNECT:
            case XmppManager.CONNECTING:
            case XmppManager.WAITING_FOR_NETWORK:
                views.setImageViewResource(R.id.Button, R.drawable.icon_orange);
                break;
            default:
                break;
         }
        
        // Set FREE label for not donate version
        if (Tools.isDonateAppInstalled(context)) {
            views.setViewVisibility(R.id.Label, View.GONE);
        } else {
            views.setViewVisibility(R.id.Label, View.VISIBLE);
        }
        
        // Tell the AppWidgetManager to perform an update on the current AppWidget
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(MainService.ACTION_WIDGET_ACTION)) {
            Tools.startSvcIntent(context, MainService.ACTION_TOGGLE);
        } else if (action.equals(MainService.ACTION_XMPP_CONNECTION_CHANGED)) {
            int state = intent.getIntExtra("new_state", 0);
            // Update all AppWidget with current status
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int [] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
            doUpdate(context, manager, appWidgetIds, state);
        } else {
            super.onReceive(context, intent);
        }
    }
}
