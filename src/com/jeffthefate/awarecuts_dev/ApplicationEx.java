package com.jeffthefate.awarecuts_dev;

import java.util.Calendar;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

import com.jeffthefate.awarecuts_dev.common.Constants;
import com.jeffthefate.awarecuts_dev.common.DatabaseHelper;
import com.jeffthefate.awarecuts_dev.common.Util;

/**
 * Used as a holder of many values and objects for the entire application.
 * 
 * @author Jeff Fate
 */
public class ApplicationEx extends Application {
    /**
     * The application's context
     */
    private static Context app;
    /**
     * Battery level of the device
     */
    private static int battLevel = 0;
    /**
     * Current device location
     */
    private static Location mCurrLoc = new Location(
            LocationManager.NETWORK_PROVIDER);
    /**
     * Current device time
     */
    private static Calendar mCurrTime;
    /**
     * Is GPS enabled?
     */
    private static boolean gpsEnabled = false;
    /**
     * Battery broadcast receiver for getting battery info
     */
    private static BroadcastReceiver mBatInfoReceiver;
    /**
     * Receiver to detect when a headset is plugged or unplugged
     */
    private static BroadcastReceiver mHeadsetReceiver;
    /**
     * How many milliseconds there are in a day
     */
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
    /**
     * Global DatabaseHelper object for the application
     */
    public static DatabaseHelper dbHelper;
    /**
     * Global SQLiteDatabase object for the application
     */
    public static SQLiteDatabase db;
    /**
     * Minimum time to request for location updates, in milliseconds
     */
    final private static long MIN_LOC_TIME = 900000;
    /**
     * Minimum distance to request for location updates, in meters
     */
    final private static float MIN_LOC_ACCURACY = 100.0f;
    /**
     * If there is a headset plugged in or not
     */
    private static int mHeadsetState = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                battLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            }
        };
        this.registerReceiver(mBatInfoReceiver, 
                      new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        requestLocation();
        mCurrTime = Calendar.getInstance();
        mHeadsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mHeadsetState = intent.getIntExtra("state", 0);
            }
        };
        this.registerReceiver(mHeadsetReceiver, 
                new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }
    /**
     * Used by other classes to get the application's global context.
     * @return  the context of the application
     */
    public static Context getApp() {
        return app;
    }
    /**
     * Gets the current battery level of the device and writes it to a local 
     * file.
     * @return  the battery level
     */
    public static int getBattery() {
        if (battLevel == 0) {
            int lastLevel = 0;
            String textLevel = Util.readStringFromFile(
                    Util.BATTERY_FILENAME);
            if (!textLevel.equals(""))
                try {
                    lastLevel = Integer.parseInt(textLevel);
                } catch (NumberFormatException e) {
                    Log.e(Constants.LOG_TAG, "String " + textLevel + 
                            " cannot be converted to an integer", e);
                }
            return lastLevel;
        }
        else {
            Util.writeBufferToFile(Integer.toString(battLevel).getBytes(), 
                    Util.BATTERY_FILENAME);
            return battLevel;
        }
    }
    /**
     * Gets the last good location from either GPS or network.
     * @return  device's last known location
     */
    private static void requestLocation() {
        boolean networkEnabled = false;
        LocationManager lm = (LocationManager) app.getSystemService(
                Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                getBestLocation(location);
            }
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
            public void onStatusChanged(String provider, int status, 
                    Bundle extras) {}
        };
        // Register the listener with the Location Manager to receive location 
        // updates
        try {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 
                    MIN_LOC_TIME, MIN_LOC_ACCURACY, locationListener);
        } catch (IllegalArgumentException e) {
            Log.e(Constants.LOG_TAG, "Unavailable provider " + 
                    LocationManager.NETWORK_PROVIDER, e);
        }
    }
    /**
     * Set the location when it is better than the currently set one.
     * @param   location    location when it changes
     */
    private static void getBestLocation(Location location) {
        if (location.getAccuracy() < mCurrLoc.getAccuracy() || 
                location.getTime() > mCurrLoc.getTime())
            mCurrLoc = location;
    }
    /**
     * Retrieve the application's saved location.
     * @return  the current location saved
     */
    private static Location getLastKnown() {
        LocationManager lm = (LocationManager) app.getSystemService(
                Context.LOCATION_SERVICE);
        Location lastKnownLocation = lm.getLastKnownLocation(
                LocationManager.NETWORK_PROVIDER);
        return lastKnownLocation;
    }
    /**
     * Gets device's latitude and longitude coordinates.
     * @return  array of device's lat/long, 0.0 if it couldn't get a location
     */
    public static double[] getLatLong() {
        Location currLoc = getLastKnown();
        double[] latLong = new double[2];
        if (currLoc != null) {
            latLong[0] = currLoc.getLatitude();
            latLong[1] = currLoc.getLongitude();
        }
        else {
            latLong[0] = mCurrLoc.getLatitude();
            latLong[1] = mCurrLoc.getLongitude();
        }
        return latLong;
    }
    /**
     * Grabs the device's current time using {@link Time}.
     * @return  device's time of day in milliseconds
     */
    public static long getTimeOfDay() {
        return Calendar.getInstance().getTime().getTime() % MILLIS_PER_DAY;
    }
    /**
     * Gets the device's bluetooth status.
     * @return  the state of bluetooth
     */
    public static int getBluetooth() {
        if (BluetoothAdapter.getDefaultAdapter() != null)
            return BluetoothAdapter.getDefaultAdapter().getState();
        return BluetoothAdapter.STATE_OFF;
    }
    /**
     * Gets the device's wifi status.
     * @return  the state of wifi (ENABLED/DISABLED/ENABLING/DISABLING/UNKNOWN)
     */
    public static int getWifi() {
        return ((WifiManager) app.getSystemService(
                Context.WIFI_SERVICE)).getWifiState();
    }
    /**
     * Gets the day of the week.
     * @return  number of the day of the week (0-6)
     */
    public static int getDay() {
        return mCurrTime.get(Calendar.DAY_OF_WEEK);
    }
    /**
     * Gets the headphone plugged state.
     * @return  if a headphone is plugged in, returns 1; 0 otherwise
     */
    public static int isHeadphonePlugged() {
        return mHeadsetState;
    }
    /**
     * Gets the A2DP state.
     * @return  if a A2DP is active, returns 1; 0 otherwise
     */
    public static int isA2DPOn() {
        if (((AudioManager) app.getSystemService(
                Context.AUDIO_SERVICE)).isBluetoothA2dpOn())
            return 1;
        else
            return 0;
    }
    
}
