package com.googlecode.gtalksms.geo;

import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;

import com.googlecode.gtalksms.LocationService;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.panels.GeoPopup;


public class GeoManager {
    
    Context _context;
    SettingsManager _settings;
    
    public GeoManager(SettingsManager settings, Context baseContext) {
        _settings = settings;
        _context = baseContext;
    }
    
    /** Starts the geolocation service */
    public void startLocatingPhone() {
        Intent intent = new Intent(_context, LocationService.class);
        intent.setAction(LocationService.START_SERVICE);
        _context.startService(intent);
    }

    /** Stops the geolocation service */
    public void stopLocatingPhone() {
        Intent intent = new Intent(_context, LocationService.class);
        intent.setAction(LocationService.STOP_SERVICE);
        _context.startService(intent);
    }

    /** Return List of <Address> from searched location */
    public List<Address> geoDecode(String searchedLocation) {
        try {
            Geocoder geo = new Geocoder(_context, Locale.getDefault());
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
    public void launchExternal(String url) {
        Intent popup = new Intent(_context, GeoPopup.class);
        popup.putExtra("url", url);
        popup.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        _context.startActivity(popup);
    }
}
