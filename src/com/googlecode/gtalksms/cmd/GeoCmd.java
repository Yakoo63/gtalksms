package com.googlecode.gtalksms.cmd;

import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;

import com.googlecode.gtalksms.services.LocationService;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.panels.GeoPopup;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class GeoCmd extends CommandHandlerBase {
    public GeoCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_GEO, "Geo", new Cmd("geo"), new Cmd("where"));
    }
    
    @Override
    protected void execute(Command cmd) {
        if (isMatchingCmd(cmd, "geo")) {
            geo(cmd.getAllArg1());
        } else if (isMatchingCmd(cmd, "where")) {
            if (cmd.getArg1().equals("stop")) {
                send(R.string.chat_stop_locating);
                stopLocatingPhone();    
            } else {
                send(R.string.chat_start_locating);
                startLocatingPhone();
            }
        }  
    }

    /** Open geolocation application */
    private void geo(String text) {
        List<Address> addresses = geoDecode(text);
        if (addresses != null) {
            if (addresses.size() > 1) {
                XmppMsg addr = new XmppMsg(getString(R.string.chat_specify_details));
                addr.newLine();
                for (Address address : addresses) {
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addr.appendLine(address.getAddressLine(i));
                    }
                }
                send(addr);
            } else if (addresses.size() == 1) {
                launchExternal(addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude());
            }
        } else {
            send(R.string.chat_no_match_for, text);
        }
    }
    
    /** 
     * Starts the geolocation service 
     */
    void startLocatingPhone() {
        Intent intent = new Intent(sContext, LocationService.class);
        intent.setAction(LocationService.START_SERVICE);
        intent.putExtra("to", this.mAnswerTo);
        sContext.startService(intent);
    }

    /** 
     * Stops the geolocation service 
     */
    void stopLocatingPhone() {
        Intent intent = new Intent(sContext, LocationService.class);
        intent.setAction(LocationService.STOP_SERVICE);
        sContext.startService(intent);
    }

    /** Return List of <Address> from searched location */
    List<Address> geoDecode(String searchedLocation) {
        try {
            Geocoder geo = new Geocoder(sContext, Locale.getDefault());
            List<Address> addresses = geo.getFromLocationName(searchedLocation, 10);
            if (addresses != null && addresses.size() > 0) {
               return addresses;
            }
        }
        catch(Exception ex) {
        }

        return null;
    }
    
    /** launches an activity on the url */
    void launchExternal(String url) {
        Intent popup = new Intent(sContext, GeoPopup.class);
        popup.putExtra("url", url);
        popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        sContext.startActivity(popup);
    }

    @Override
    public void stop() {
        stopLocatingPhone();
    }

    @Override
    protected void onCommandActivated() {
    }

    @Override
    protected void onCommandDeactivated() {
    }

    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("geo").setHelp(R.string.chat_help_geo, "#address#");
        Cmd where = mCommandMap.get("where");
        where.setHelp(R.string.chat_help_where, null);
        where.AddSubCmd("stop", R.string.chat_help_where_stop, null);
    }
}
