package com.ecube_solutions.ecubestation.Fragment;


import android.animation.Animator;
import android.app.Activity;
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
import android.widget.Toast;

import com.ecube_solutions.ecubestation.Activity.RunningActivity;
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
    private static final int CODE_SETTINGS_CHECK = 2;   // Used to check settings
    private static final int CODE_GPIO_CHECK = 3;       // Used to check GPIO access
    private static final int CODE_REGISTER = 4;   // Used for register the station
    Intent GpsServiceIntent;
    public BroadcastReceiver GpsServiceReceiver;
    public Locker mLocker;
    private Boolean mStopService = true;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_fragment,container,false);
        final IconView IconSettings = (IconView) v.findViewById(R.id.imageSettings);
        final IconView IconNetwork = (IconView) v.findViewById(R.id.imageNetwork);
        final IconView IconCloud = (IconView) v.findViewById(R.id.imageCloud);
        final IconView IconGPIO = (IconView) v.findViewById(R.id.imageGpio);
        final IconView IconGPS = (IconView) v.findViewById(R.id.imageGps);
        final Locker mLocker = Locker.getLocker();
        //Define all views of ImageViewCheckers and start animations
        IconGPS.loadBitmapAsset("gps.png");
        IconNetwork.loadBitmapAsset("network.png");
        IconCloud.loadBitmapAsset("cloud.png");
        IconGPIO.loadBitmapAsset("gpio.png");
        IconSettings.loadBitmapAsset("settings.png");





        IconSettings.setIterations(2);
        IconSettings.setDelay(0);
        IconNetwork.setIterations(4);
        IconNetwork.setDelay(200);
        IconCloud.setIterations(6);
        IconCloud.setDelay(400);
        IconGPIO.setIterations(8);
        IconGPIO.setDelay(600);
        IconGPS.setIterations(10);
        IconGPS.setDelay(800);
        IconSettings.startAnimation();
        IconNetwork.startAnimation();
        IconCloud.startAnimation();
        Animator gpioAnim = IconGPIO.startAnimation();

        gpioAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.i("GPS_START", "lStatusGPS is : " + Locker.lStatusGPS);
                if (Locker.lStatusGPS) {
                    IconGPS.setResult(Locker.lStatusGPS);
                    Log.i("GPS_START", "Register station now !");
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(createRunnable(CODE_REGISTER, IconGPS));
                    executor.shutdown();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });


        Animator test = IconGPS.startAnimation();
        test.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }
            @Override
            public void onAnimationEnd(Animator animator) {

                startRunningActivity();
            }
            @Override
            public void onAnimationCancel(Animator animator) {
            }
            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });


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
    //Run now the main activity
    public void startRunningActivity() {
        Log.i("SERGI", "startRunningActivity !!!!");
        if (Locker.isValid()) {

            Activity myActivity = getActivity();

            Toast.makeText(getActivity(), "All params ok !", Toast.LENGTH_LONG).show();
            Intent i = RunningActivity.newIntent(getActivity(), "test");
            mStopService = false;
            startActivity(i);
            getActivity().finish();

        } else {
            Toast.makeText(getActivity(), "Parameters invalid ! ", Toast.LENGTH_LONG).show();
        }
    }

    // Create a runnable with the desired task to accomplish
    public Runnable createRunnable(final int code, IconView v) {
        final IconView myIcon = v;


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
                    case MainFragment.CODE_REGISTER:
                        Log.i("ASYNC", "Code_register");
                        if (Locker.isValid()) {
                            myResult = false;
                            myResult = new CloudFetchr().registerStation();
                            Locker.lStatusRegistered = true;
                            Log.i("GPS", "Station registered, result is : " + myResult);
                        }
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
    }

    @Override
    public void onDestroy() {
        if (mStopService) getActivity().stopService(GpsServiceIntent);
        super.onDestroy();
    }

    //Checks status of wifi/gps...
    public boolean checkSettings() {
        WifiManager wifiManager = (WifiManager)this.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        return true;
    }

    //Checks that everything is ok in the cloud and adds station if does not exists
    public boolean checkCloud() {
        Boolean myResult = false;
        //Check server connectivity
        myResult = new CloudFetchr().isCloudConnected();
        if (!myResult) return myResult;
        return myResult;
    }


}


