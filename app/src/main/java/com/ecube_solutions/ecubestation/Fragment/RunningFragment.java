package com.ecube_solutions.ecubestation.Fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ecube_solutions.ecubestation.Activity.RunningActivity;
import com.ecube_solutions.ecubestation.DAO.CloudFetchr;
import com.ecube_solutions.ecubestation.DAO.ImageItem;
import com.ecube_solutions.ecubestation.R;
import com.ecube_solutions.ecubestation.Service.GpsService;
import com.ecube_solutions.ecubestation.Service.PollService;
import com.ecube_solutions.ecubestation.Singleton.Locker;

/**
 * Created by sergi on 26/09/2017.
 */

public class RunningFragment extends Fragment {
    private static Boolean DEBUG_MODE = true;           // Enables/disables verbose logging
    private static final int SYNC_INTERVAL_IMAGES = 1000*30;    //Interval to see if new images are available
    private static final String TAG ="MainFragment::";
    private Intent GpsServiceIntent;
    private BroadcastReceiver GpsServiceReceiver;
    private Intent PollServiceIntent;
    private BroadcastReceiver PollServiceReceiver;
    private static Handler handlerImageSync = new Handler();

    public Locker mLocker;
    ImageView productImage;
    TextView productDescription;


    public static RunningFragment newInstance() {
        return new RunningFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.running_fragment,container,false);
        productImage = (ImageView) v.findViewById(R.id.product_image);
        productDescription = (TextView) v.findViewById(R.id.product_description);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Declare a new Locker Singleton
        mLocker = Locker.getLocker();
        mLocker.init(getContext());
        //Register to the GPS service and keep alive position and timestamp of the station
        this.registerGpsService();
        this.startPollService();
        this.startImageSync();
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        GpsServiceIntent = new Intent(getActivity(), GpsService.class);
        getActivity().stopService(GpsServiceIntent);
        getActivity().unregisterReceiver(GpsServiceReceiver);
        getActivity().stopService(PollServiceIntent);
        getActivity().unregisterReceiver(PollServiceReceiver);
        handlerImageSync.removeCallbacks(syncImages);
        super.onDestroy();
    }

    //----------------------------------------------------------------------------------------------
    // GPS handling
    //----------------------------------------------------------------------------------------------
    //Handle GPS coords update on the fly
    private void registerGpsService() {
        Log.i("GPS", "Started GPS service !!!");
        GpsServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("GPS","Recieved GPS data !");
                //intent.getFloatExtra("longitude", 0);
                //intent.getFloatExtra("latitude", 0);
                Log.i("GPS", "Got longitude : " + intent.getStringExtra("longitude"));
                Log.i("GPS", "Got latitude : " + intent.getStringExtra("latitude"));
                FetchStationLocationTask task = new FetchStationLocationTask();
                task.execute();

            }
        };
        getActivity().registerReceiver(GpsServiceReceiver, new IntentFilter(GpsService.BROADCAST_ACTION));
    }


    //Update new GPS coords into the server and update timestamp
    private class FetchStationLocationTask extends AsyncTask<Void,Void,Boolean> {
        public FetchStationLocationTask() {}

        @Override
        protected Boolean doInBackground(Void... params) {
            Log.i(TAG,"Do in Background");
            return (new CloudFetchr().registerStation());
        }

        @Override
        protected void onPostExecute(Boolean result) {
            Log.i(TAG,"Updated coords in database");
        }
    }

    //----------------------------------------------------------------------------------------------
    // Image handling
    //----------------------------------------------------------------------------------------------
    private void startImageSync() {
        Log.i(TAG,"Starting image SYNC!");
        handlerImageSync.postDelayed(syncImages, SYNC_INTERVAL_IMAGES);

    }
    //This is the code that will be generated
    private final Runnable syncImages = new Runnable() {
        @Override
        public void run() {
            //Do something after POLL_INTERVAL
            String mAction="nothing ";
            Log.i(TAG, "Sync of images !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            int i;
            Log.i("SYNC IMAGES", "Now syncikng!");
            for (i=0; i<Locker.lImages.size() ; i++) {
                ImageItem test = Locker.lImages.get(i);
                Log.i("IMG", "Product id : " + test.getId() + " description : " + test.getDescription());
                if (test.getStream()!= null) {
                    Log.i("Stream", test.getStream().toString());
                    //byte[] decodedString = Base64.decode(test.getStream(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(test.getStream(), 0, test.getStream().length);
                    productImage.setImageBitmap(decodedByte);
                    productDescription.setText(test.getDescription());
                }
            }


            handlerImageSync.postDelayed(this, SYNC_INTERVAL_IMAGES);
        }};




    //Handle poll service !
    private void startPollService() {
        PollServiceIntent = new Intent(getActivity(), PollService.class);
        getActivity().startService(PollServiceIntent);
        Log.i("GPS", "Started GPS service !!!");
        PollServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("POLL","Poll data recieved !!!!");

            }
        };
        getActivity().registerReceiver(PollServiceReceiver, new IntentFilter(PollService.BROADCAST_ACTION));

    }
}


