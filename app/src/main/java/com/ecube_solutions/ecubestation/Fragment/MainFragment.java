package com.ecube_solutions.ecubestation.Fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ecube_solutions.ecubestation.R;
import com.ecube_solutions.ecubestation.Service.GpsService;
import com.ecube_solutions.ecubestation.View.IconView;
import com.ecube_solutions.ecubestation.Singleton.Locker;
import com.ecube_solutions.ecubestation.DAO.CloudFetchr;
import com.ecube_solutions.ecubestation.DAO.GPIO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFragment extends Fragment {
    private static Boolean DEBUG_MODE = true;           // Enables/disables verbose logging
    private static final String TAG ="MainFragment::";
    private static final int CODE_CLOUD_CHECK = 0;      // Used for the async task to check cloud connection
    private static final int CODE_INTERNET_CHECK = 1;   // Used for the async task to check internet connection
    private static final int CODE_CLOUD_LOCATION = 2;   // Used for update location task
    private static final int CODE_SETTINGS_CHECK = 3;   // Used to check settings
    private static final int CODE_GPIO_CHECK = 4;       // Used to check GPIO access
    Intent GpsServiceIntent;
    public BroadcastReceiver GpsServiceReceiver;
    public Locker mLocker;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_fragment,container,false);
        TextView tv = (TextView) v.findViewById(R.id.textView);
        tv.setText("This is a test");
        final IconView IconSettings = (IconView) v.findViewById(R.id.imageSettings);
        final IconView IconNetwork = (IconView) v.findViewById(R.id.imageNetwork);
        final IconView IconCloud = (IconView) v.findViewById(R.id.imageCloud);
        final IconView IconGPIO = (IconView) v.findViewById(R.id.imageGpio);
        final IconView IconGPS = (IconView) v.findViewById(R.id.imageGps);

        //Define all views of ImageViewCheckers and start animations
        IconGPS.loadBitmapAsset("gps.png");
        IconNetwork.loadBitmapAsset("network.png");
        IconCloud.loadBitmapAsset("cloud.png");
        IconGPIO.loadBitmapAsset("gpio.png");
        IconSettings.loadBitmapAsset("settings.png");





        IconSettings.setIterations(4);
        IconSettings.setDelay(0);
        IconNetwork.setIterations(8);
        IconNetwork.setDelay(200);
        IconCloud.setIterations(12);
        IconCloud.setDelay(400);
        IconGPIO.setIterations(16);
        IconGPIO.setDelay(600);
        IconGPS.setIterations(20);
        IconGPS.setDelay(800);
        IconSettings.startAnimation();
        IconNetwork.startAnimation();
        IconCloud.startAnimation();
        IconGPIO.startAnimation();
        IconGPS.startAnimation();
        //RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_beat_box_recycler_view);
        //recyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));
        //recyclerView.setAdapter(new SoundAdapter(mBeatBox.getSounds()));


        //Executes sequentially tasks
        CloudFetchr.setDebugMode(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(this.createRunnable(CODE_SETTINGS_CHECK, IconSettings));
        executor.execute(this.createRunnable(CODE_INTERNET_CHECK, IconNetwork));
        executor.execute(this.createRunnable(CODE_CLOUD_CHECK, IconCloud));
        executor.execute(this.createRunnable(CODE_GPIO_CHECK, IconGPIO));
        executor.shutdown();

        return v;
    }
    // Create a runnable with the desired task to accomplish
    public Runnable createRunnable(final int code, IconView v) {
        final IconView myIcon = v;
        final Locker mLocker = Locker.getLocker();

        return new Runnable() {
            @Override
            public void run() {
                Boolean myResult = false;
                switch (code) {
                    case MainFragment.CODE_SETTINGS_CHECK:
                        myResult = checkSettings();
                        myIcon.setResult(myResult);     //Set attribute to Icon
                        //mLocker.set
                        break;
                    case MainFragment.CODE_GPIO_CHECK:
                        myResult = GPIO.isSuperUserAvailable();
                        myIcon.setResult(myResult);  // set Attribute on icon
                        Locker.lStatusGPIO = myResult;                     //set Attribute on Locker singleton
                        break;
                    case MainFragment.CODE_CLOUD_CHECK :
                        Log.i("ASYNC:", "We are in isCoudConnected");
                        myResult = checkCloud();
                        myIcon.setResult(myResult);
                        Locker.lStatusCloud = myResult;
                        break;
                    case MainFragment.CODE_INTERNET_CHECK:
                        if (DEBUG_MODE) Log.i(TAG,"We are in isInternetConnected");
                        myResult = CloudFetchr.isNetworkConnected();
                        myIcon.setResult(myResult);
                        Locker.lStatusNetwork = myResult;
                        break;
                    case MainFragment.CODE_CLOUD_LOCATION:
                        Log.i("ASYNC:", "We are in setLocation");
                        if (Locker.lStatusGPS) {
                            longitude = String.valueOf(Locker.lLongitude);
                            latitude = String.valueOf(Locker.lLatitude);
                        } else {
                            //longitude = "0";
                            //latitude= "0";
                        }
                        //new CloudFetchr().setLocation(longitude,latitude);
                        break;
                    default:
                        new CloudFetchr().isCloudConnected();
                }
            }
        };

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Declare a new Locker Singleton
        mLocker = Locker.getLocker();
        mLocker.init(getContext());
        GpsServiceIntent = new Intent(getActivity(), GpsService.class);
        getActivity().startService(GpsServiceIntent);
        Log.i("GPS", "Started GPS service !!!");
        GpsServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("GPS","Recieved GPS data !");
                Bundle data = new Bundle();
                //intent.getFloatExtra("longitude", 0);
                //intent.getFloatExtra("latitude", 0);
                Log.i("GPS", "Got longitude : " + intent.getStringExtra("longitude"));
                //Update the locker location
                Locker.lLongitude = Float.valueOf(intent.getStringExtra("longitude"));
                Locker.lLatitude = Float.valueOf(intent.getStringExtra("latitude"));
                Locker.lStatusGPS = true;

            }
        };
        getActivity().registerReceiver(GpsServiceReceiver, new IntentFilter(GpsService.BROADCAST_ACTION));

    }

    @Override
    public void onDestroy() {
        getActivity().stopService(GpsServiceIntent);
        getActivity().unregisterReceiver(GpsServiceReceiver);
        super.onDestroy();
    }

    //Checks status of wifi/gps...
    public boolean checkSettings() {
        WifiManager wifiManager = (WifiManager)this.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        //Remove all downloaded images from disk so that we start in a clean way
        Locker.removeImagesFromDisk("all");

        return true;
    }

    //Checks that everything is ok in the cloud and adds station if does not exists
    public boolean checkCloud() {
        Boolean myResult = false;
        //Check server connectivity
        myResult = new CloudFetchr().isCloudConnected();
        if (!myResult) return myResult;
        //TODO : Here we need to wait GPS coords and then register the station
        // if the station is registered, only update coords and timestamp
        // else create the station with data

        /*
        //Check if station registered and if not register
        myResult = new CloudFetchr().isStationRegistered();
        if (!myResult) {
            Log.i(TAG, "Registering new station...");
            myResult = new CloudFetchr().registerStation();
            if (!myResult) return myResult;
        }
        //Download all images one by one and store them in the singleton
        myResult = new CloudFetchr().getImages("all");
        if (!myResult) {
            Log.i(TAG, "No images found...");
            return myResult;
        }
        Locker.saveImagesToDisk("all");
        Locker.loadImagesfromDisk("all");
*/
        return myResult;
    }


}


