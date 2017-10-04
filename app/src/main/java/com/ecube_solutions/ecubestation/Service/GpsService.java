package com.ecube_solutions.ecubestation.Service;

/**
 * Created by sergi on 22/09/2017.
 */

import android.app.Service;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.location.Location;
        import android.location.LocationListener;
        import android.location.LocationManager;
        import android.net.Uri;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.IBinder;
        import android.provider.Settings;
        import android.support.v4.content.ContextCompat;
        import android.util.Log;
        import android.widget.Toast;

import com.ecube_solutions.ecubestation.Singleton.Locker;

/**
 GpsService class
 This is the service that gets GPS location for a Locker
 It doesn't use any google API
 */

public class GpsService extends Service implements LocationListener {
    static final public String BROADCAST_ACTION = "com.ecube_solutions.ecubestation.GpsService";

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 5 meters
    // Minimum distance to considere data stable
    private static final long MIN_DISTANCE_STABLE_DATA = 500;

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0; // 10 second
    //Minimum time between intervals to considere stable data
    private static final long MIN_TIME_STABLE_DATA = 1000*10*10;
    // Minimum accuracy required for considering good data
    private static final long  MIN_ACCURACY_STABLE_DATA = 1000; //Should be 100
    private int mCount = 0;

    //    private final Context mContext;
    Location location = new Location("Point NEW"); // location
    Location previousLocation = new Location("point OLD");;

    Intent intent;
    private Locker mLocker;

    // Declaring a Location Manager
    protected LocationManager locationManager;

    //Make sure that only one Intent can be created
    public static Intent newIntent(Context context) {
        return new Intent(context, GpsService.class);
    }

    private static String getBestProvider(LocationManager locationManager) {
        boolean isGPSEnabled = false;
        boolean isNetworkEnabled = false;
        boolean isPassiveEnabled = false;
        try {
            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            isPassiveEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);
        } catch (Exception e) {
            Log.i("GPS", "GPS not enabled, using network for now");
        }
        //TODO: This needs to be enabled once we are outside because we need GPS coords !!!!!!
 //      if (isGPSEnabled) {
 //           return LocationManager.GPS_PROVIDER;
 //       }
        if (isNetworkEnabled) {
            return LocationManager.NETWORK_PROVIDER;
        }
        if (isPassiveEnabled) {
            return LocationManager.PASSIVE_PROVIDER;
        }
        return null;
    }

    @Override
    public void onCreate() {
        // flag for GPS status

        super.onCreate();
        Log.i("GPS", "GPS Service onCreate !!");
        //Chec for permissions on latest versions
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }
        intent = new Intent(BROADCAST_ACTION);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Use GPS if available, otherwise use Network or passive
        Log.i("GPS", "Best provider found is :" + getBestProvider(locationManager));

        //If there is no way to get coordinates then broadcast data
        if (getBestProvider(locationManager) == null) {
            intent.putExtra("isLocationValid", "false");
            sendBroadcast(intent);
            return;
        }
        Log.i("GPS","Requesting location update !");
        locationManager.requestLocationUpdates(
                getBestProvider(locationManager),
                MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        //Initialize previousLocation
        previousLocation.setTime(0);
        previousLocation.setLongitude(0);
        previousLocation.setLatitude(0);
        location.setTime(MIN_TIME_BW_UPDATES*5);
        location.setLongitude(0);
        location.setLatitude(0);
        //Save data in singleton
        mLocker = Locker.getLocker();

        //WA for  now !!!
        Log.i("GPS","WORKAROUND: creating dummy coordinates");
        Location location = new Location("workaround");
        location.setLatitude(43.5d);
        location.setLongitude(7.5d);
        mLocker.lLocation = location;
        mLocker.lStatusGPS = true;
    }


    @Override
    public void onDestroy() {
        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        super.onDestroy();
        locationManager.removeUpdates(GpsService.this);
        if(intent != null) {
            intent = null;
        }
        if(locationManager != null){
            locationManager.removeUpdates(this);
        }
    }


    public Location getLocation() {
        return location;
    }
    // Stores what's in location into previousLocation
    private void updatePreviousLocation(Location location, Location previousLocation) {
        //Store into previous location for next update
        previousLocation.setTime(location.getTime());
        previousLocation.setLongitude(location.getLongitude());
        previousLocation.setLatitude(location.getLatitude());
        previousLocation.setAltitude(location.getAltitude());
        previousLocation.setAccuracy(location.getAccuracy());
        previousLocation.setSpeed(location.getSpeed());

    }

    // Uses location and previousLocation to compute all data
    private void computeData(Location location, Location previousLocation) {
        double distance = 0;
        long timeDelta = 0;
        boolean isWithinDistance = false;
        boolean isWithinTime = false;
        boolean isWithinAccuracy = false;

        //Check if distance honors the threshold
        if (previousLocation.getLatitude() != 0 && previousLocation.getLongitude() != 0 ) {
            distance = location.distanceTo(previousLocation);
        }
        //Log.i("SERGI","distance = " + distance);
        if (distance <= MIN_DISTANCE_STABLE_DATA) {
            isWithinDistance = true;
        }
        //Check if time honors the threshold
        timeDelta = location.getTime() - previousLocation.getTime();
        //Log.i("SERGI","timeDelta = " + timeDelta);
        if (timeDelta <= MIN_TIME_STABLE_DATA) {
            isWithinTime = true;
        }
        //Check if accuracy honors the threshold
        if (location.getAccuracy()<= MIN_ACCURACY_STABLE_DATA) {
            isWithinAccuracy = true;
        }
        //Log.i("GPS", "Got new coords !");
        //Log.i("GPS", "mCount : " + mCount);
        //Log.i("GPS", "provider : " + location.getProvider());
        //Log.i("GPS", "isWithinAccuracy : " + isWithinAccuracy);
        //Log.i("GPS", "isWithinDistance : " + isWithinDistance);
        //Log.i("GPS", "isWithinTime : " + isWithinTime);
        //Broadcast data if everything is respected
        if ((isWithinAccuracy && isWithinDistance && isWithinTime) || mCount==0) {
            intent.putExtra("isLocationValid", "true");
            intent.putExtra("longitude", " " + location.getLongitude());
            intent.putExtra("latitude", "" + location.getLatitude());
            intent.putExtra("provider", "" + location.getProvider());
            intent.putExtra("time", "" + location.getTime());
            intent.putExtra("accuracy"," " + location.getAccuracy() );
            intent.putExtra("time_delta"," " + timeDelta );
            intent.putExtra("distance", String.format("%.1f", distance ));
            sendBroadcast(intent);
            Log.i("GPS", "SAVED !!: Longitude : " +location.getLongitude()+ " latitude : " + location.getLatitude());
            mLocker.lLocation = location;
            mLocker.lStatusGPS = true;
            mCount++;
        }

    }

    @Override
    public void onLocationChanged(Location location) {

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.i("SERGI","onLocationChanged !");
        //Get new location
        location = locationManager
                .getLastKnownLocation(getBestProvider(locationManager));
        //Do any calculation requiring old/new location
        computeData(location,previousLocation);
        //Update old location with current location for new round
        updatePreviousLocation(location,previousLocation);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i("SERGI","onProviderDisabled: called function !");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i("GPS_TEST","onProviderEnabled: called function !");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("GPS_TEST","onStatusChanged: called function !");
    }



    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}

