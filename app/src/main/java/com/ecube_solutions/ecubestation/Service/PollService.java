package com.ecube_solutions.ecubestation.Service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.ecube_solutions.ecubestation.DAO.CloudFetchr;
import com.ecube_solutions.ecubestation.DAO.GPIO;
import com.ecube_solutions.ecubestation.Singleton.Locker;
import com.ecube_solutions.ecubestation.View.IconView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by sredorta on 11/29/2016.
 */

public class PollService extends Service {
    private static Boolean DEBUG_MODE = true;
    private static final String TAG = "PollService::";

    static final public String BROADCAST_ACTION = "com.clickandbike.bikestation.PollService";

    public static Context mContext;
    private static final int POLL_INTERVAL = 1000*10; // 6 seconds
    private static final int POLL_INTERVAL_IMAGES = 1000*30;    //Interval to see if new images are available
    private static final int CODE_POLL_IMAGES = 0;
    private static Handler handler = new Handler();
    private static Handler handlerImagePoll = new Handler();

    //Constructor
    public PollService() {
        super();
        Log.i(TAG,"Created poll service !");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Created poll service onCreate!");
        handler.postDelayed(sendData, POLL_INTERVAL);
        handlerImagePoll.postDelayed(pollImages, POLL_INTERVAL_IMAGES);

    }

    //This is the code that will be generated
    private final Runnable pollImages = new Runnable() {
        @Override
        public void run() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(PollService.createRunnable(CODE_POLL_IMAGES));
            executor.shutdown();

            handlerImagePoll.postDelayed(this, POLL_INTERVAL_IMAGES);
        }};



    //This is the code that will be generated
    private final Runnable sendData = new Runnable() {
        @Override
        public void run() {
            //Do something after POLL_INTERVAL
            String mAction="nothing ";
            Log.i(TAG, "Polling...");
            //mAction = new CloudFetchr().getAction();
            FetchCloudTask task = new FetchCloudTask();
            task.execute();
            handler.postDelayed(this, POLL_INTERVAL);
        }};


    //Update timestamp of the station on a POLL_INTERVAL basis and check if an action on locker is required
    private class FetchCloudTask extends AsyncTask<Void,Void,Boolean> {
        public FetchCloudTask() {}

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG,"Do in Background");
                    return (new CloudFetchr().queryStation());
        }

        @Override
        protected void onPostExecute(Boolean action) {
            Log.i(TAG,"Updated timestamp during poll !");
            //TODO: We should no wo any action if required like open locker...
        }
    }

    // Create a runnable with the desired task to accomplish
    public static Runnable createRunnable(final int code) {
        final Locker mLocker = Locker.getLocker();

        return new Runnable() {
            @Override
            public void run() {
                Boolean myResult = false;
                switch (code) {
                    case PollService.CODE_POLL_IMAGES:
                        if (DEBUG_MODE) Log.i(TAG, "Polling to synchronize Ads images...");
                        myResult = new CloudFetchr().getImages();

                        break;
                    default:
                        new CloudFetchr().isCloudConnected();
                }
            }
        };

    }






    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Destroyed poll service !");
        handler.removeCallbacks(sendData);
        handlerImagePoll.removeCallbacks(pollImages);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
