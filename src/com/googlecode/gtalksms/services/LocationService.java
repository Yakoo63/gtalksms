package com.googlecode.gtalksms.services;

/*
 * Source code of this class originally written by Kevin AN <anyupu@gmail.com>
 * from the project android-phonefinder: http://code.google.com/p/android-phonefinder/
 */

import java.lang.reflect.Method;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppMsg;

public class LocationService extends Service {

    private Location _currentBestLocation = null;
    private String _answerTo;

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public static final String STOP_SERVICE = "com.googlecode.gtalksms.LOCATION_STOP_SERVICE";
    public static final String START_SERVICE = "com.googlecode.gtalksms.LOCATION_START_SERVICE";

    private SettingsManager _settingsManager = null;
    private LocationManager _locationManager = null;

    private LocationListener _locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, _currentBestLocation)) {
                _currentBestLocation = location;
                sendLocationUpdate(_currentBestLocation);
            }
        }
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
        public void onProviderDisabled(String arg0) {}
        public void onProviderEnabled(String arg0) {}
    };


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        _settingsManager = SettingsManager.getSettingsManager(this);
    }

    /*
     * http://www.maximyudin.com/2008/12/07/android/vklyuchenievyklyuchenie-gps-na-g1-programmno/
     */
    private boolean getGPSStatus() {
        String allowedLocationProviders = Settings.System.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (allowedLocationProviders == null) {
            allowedLocationProviders = "";
        }
        return allowedLocationProviders.contains(LocationManager.GPS_PROVIDER);
    }

    private void setGPSStatus(final boolean newGPSStatus) throws Exception {
        String allowedLocationProviders = Settings.System.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        // use the old way to get the GPS going
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            if (allowedLocationProviders == null) {
                allowedLocationProviders = "";
            }
            boolean networkProviderStatus = allowedLocationProviders.contains(LocationManager.NETWORK_PROVIDER);
            allowedLocationProviders = "";
            if (networkProviderStatus) {
                allowedLocationProviders += LocationManager.NETWORK_PROVIDER;
            }
            if (newGPSStatus) {
                allowedLocationProviders += "," + LocationManager.GPS_PROVIDER;
            }
            Settings.System.putString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED, allowedLocationProviders);
            Method m = _locationManager.getClass().getMethod("updateProviders", new Class[] {});
            m.setAccessible(true);
            m.invoke(_locationManager);
        // use the security hole from http://code.google.com/p/android/issues/detail?id=7890
        } else {
            // the GPS is not in the requested state
            if (!allowedLocationProviders.contains("gps") == newGPSStatus) {                
                final Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                intent.setData(Uri.parse("3"));
                sendBroadcast(intent);
            }
        }
    }

    /**
     * Sends the location to the user.
     * @param location the location to send.
     */
    void sendLocationUpdate(Location location) {
        if (location == null) {
            return;
        }

        XmppMsg msg = new XmppMsg();
        if (_settingsManager.useGoogleMapUrl) {
            msg.appendLine("http://maps.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude());
        }
        if (_settingsManager.useOpenStreetMapUrl) {
            msg.appendLine("http://www.openstreetmap.org/?mlat=" + location.getLatitude() + "&mlon=" + location.getLongitude() + "&zoom=14&layers=M");
        }
        msg.appendLine(getString(R.string.chat_geo_accuracy, location.getAccuracy()));
        msg.appendLine(getString(R.string.chat_geo_altitude, location.getAltitude()));
        msg.appendLine(getString(R.string.chat_geo_speed, location.getSpeed()));
        msg.appendLine(getString(R.string.chat_geo_provider, location.getProvider()));
        send(msg);
    }

    @SuppressWarnings("deprecation")
    public void onStart(final Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent != null) {
            Log.i("LocationService onStart with intent action=" + intent.getAction());
        } else {
            Log.i("Location Service onStart with null intent");
        }

        // Cleanup previous instance
        destroy();
        if (intent != null && intent.getAction().equals(STOP_SERVICE)) {
            stopSelf();
            return;
        }

        // Initializing the new instance
        _locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (intent != null) {
            _answerTo = intent.getStringExtra("to");
        }
        
        // try to enable the GPS
        if (!getGPSStatus()) {
            try {
                setGPSStatus(true);
            } catch (Exception e) {
                send("Could not enable GPS: " + e);
            }
        }

        // We query every available location providers
        _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, _locationListener);
        _locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, _locationListener);

        Location location = _locationManager.getLastKnownLocation("gps");
        if (location == null) {
            location = _locationManager.getLastKnownLocation("network");
            if (location != null && isBetterLocation(location, _currentBestLocation)) {
                _currentBestLocation = location;
            }
        }
        sendLocationUpdate(_currentBestLocation);
    }

    public void onDestroy() {
        destroy();
    }

    private void destroy() {
        if (_locationManager != null) {
            _locationManager.removeUpdates(_locationListener);
            _locationManager = null;
        }
    }
    
    private void send(String message) {
        Tools.send(message, _answerTo, this);
    }
    
    private void send(XmppMsg msg) {
        Tools.send(msg, _answerTo, this);
    }

    /** 
     * From the SDK documentation. Determines whether one Location reading is better than the current Location fix
     * 
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     * @return true if the location is better then the currentBestLocation
     */
    private static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        
        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSameAccuracy = accuracyDelta == 0;
        boolean isSame = accuracyDelta == 0 && location.getAltitude() == currentBestLocation.getAltitude() 
                            && location.getLongitude() == currentBestLocation.getLongitude();

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if ((isSignificantlyNewer || isSameAccuracy) && !isSame) {
            return true;
        }
        return false;
    }
}
