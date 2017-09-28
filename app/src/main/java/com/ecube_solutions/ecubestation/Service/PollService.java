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
import com.ecube_solutions.ecubestation.DAO.JsonItem;
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
    private static final int CODE_UPDATE_TIMESTAMP = 1;
    private static final int CODE_UPDATE_ACTION = 2;
    private static Handler handler = new Handler();
    private static Handler handlerImagePoll = new Handler();
    Intent intent;

    //Constructor
    public PollService() {
        super();
        Log.i(TAG,"Created poll service !");
    }

    //Make sure that only one Intent can be created
    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Created poll service onCreate!");
        handler.postDelayed(sendData, POLL_INTERVAL);
        handlerImagePoll.postDelayed(pollImages, POLL_INTERVAL_IMAGES);
        intent = new Intent(BROADCAST_ACTION);
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
            FetchCloudTask task = new FetchCloudTask(CODE_UPDATE_TIMESTAMP);
            task.execute();
            handler.postDelayed(this, POLL_INTERVAL);
        }};


    //Update timestamp of the station on a POLL_INTERVAL basis and check if an action on locker is required
    private class FetchCloudTask extends AsyncTask<Void,Void,JsonItem> {
        public int mCode;
        public String mAction;
        public FetchCloudTask(int code) {
            mCode = code;
        }
        public FetchCloudTask(int code, String action) {
            mCode = code;
            mAction = action;
        }

        @Override
        protected JsonItem doInBackground(Void... params) {
            Log.i(TAG,"Do in Background");
            if (mCode == CODE_UPDATE_TIMESTAMP) {
                return (new CloudFetchr().queryStation());
            } else {
                return (new CloudFetchr().setAction(mAction));
            }
        }

        @Override
        protected void onPostExecute(JsonItem item) {
            //Send Intent to activity with action to be performed if there is any
            if (item.getResult()) {
                if (mCode == CODE_UPDATE_TIMESTAMP) {
                    if (!item.getAction().equals("none")) {
                        intent.putExtra("action", item.getAction());
                        sendBroadcast(intent);
                    }
                    //Update action as we have done it
                    FetchCloudTask task = new FetchCloudTask(CODE_UPDATE_ACTION, "none");
                    task.execute();
                }
            }
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
