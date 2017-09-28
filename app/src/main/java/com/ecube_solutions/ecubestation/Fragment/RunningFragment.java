package com.ecube_solutions.ecubestation.Fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.widget.LinearLayout;
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
    private static final int SHIFT_INTERVAL_IMAGES = 1000*6;    //Interval to switch to new image
    private static final String TAG ="MainFragment::";
    private Intent GpsServiceIntent;
    private BroadcastReceiver GpsServiceReceiver;
    private Intent PollServiceIntent;
    private BroadcastReceiver PollServiceReceiver;
    private static Handler handlerImageShift = new Handler();

    private int mImageIndex;
    public Locker mLocker;
    ImageView productImage;
    TextView productDescription;
    TextView productPrice;
    LinearLayout productShow;
    TextView productDisplay;
    ImageView mainImage;    //Contains logo...
    TextView mainText;      //Contains Welcome...



    public static RunningFragment newInstance() {
        return new RunningFragment();
    }

    //test
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.running_fragment,container,false);
        productImage = (ImageView) v.findViewById(R.id.product_image);
        productDescription = (TextView) v.findViewById(R.id.product_description);
        productPrice = (TextView) v.findViewById(R.id.product_price);
        productShow = (LinearLayout) v.findViewById(R.id.product_container);
        productDisplay = (TextView) v.findViewById(R.id.productDisplay);
        mainImage = (ImageView) v.findViewById(R.id.mainImage);
        mainText = (TextView) v.findViewById(R.id.mainText);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //Declare a new Locker Singleton
        mLocker = Locker.getLocker();
        mLocker.init(getContext());
        //Register to the GPS service and keep alive position and timestamp of the station
        mImageIndex = 0;
        this.registerGpsService();
        this.startPollService();
        this.startImageShift();
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
        handlerImageShift.removeCallbacks(shiftImages);
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
    private void startImageShift() {
        Log.i(TAG,"Starting image SYNC!");
        handlerImageShift.postDelayed(shiftImages, SHIFT_INTERVAL_IMAGES);

    }
    //This is the code that will be generated
    private final Runnable shiftImages = new Runnable() {
        @Override
        public void run() {
            if (Locker.lImages.size()>0) {
                productShow.setVisibility(View.VISIBLE);
                productDisplay.setVisibility(View.VISIBLE);
            } else {
                productShow.setVisibility(View.INVISIBLE);
                productDisplay.setVisibility(View.INVISIBLE);
            }
            if (mImageIndex > Locker.lImages.size()) mImageIndex = 0;
            //if (Locker.lImages.size()>0 && Locker.lImages.get(mImageIndex) == null) mImageIndex =0;
            Log.i("SHIMG", "ShiftImage Index is :" + mImageIndex);
            if (Locker.lImages.size()> 0) {
                if (Locker.lImages.get(mImageIndex) != null) {
                    ObjectAnimator animatorExit = ObjectAnimator.ofFloat(productShow, "alpha", 1, 0).setDuration(300);
                    animatorExit.setRepeatCount(0);
                    ObjectAnimator animatorEnter = ObjectAnimator.ofFloat(productShow, "alpha", 0, 1).setDuration(300);
                    animatorEnter.setRepeatCount(0);
                    AnimatorSet animatorSet = new AnimatorSet();
                    animatorSet.playSequentially(animatorExit,animatorEnter);
                    animatorSet.setStartDelay(0);
                    animatorSet.start();
                    animatorExit.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            ImageItem test = Locker.lImages.get(mImageIndex);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(test.getStream(), 0, test.getStream().length);
                            productImage.setImageBitmap(decodedByte);
                            productDescription.setText(test.getDescription());
                            productPrice.setText(test.getPrice());
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });

                    mImageIndex++;
                    if (mImageIndex >= Locker.lImages.size()) mImageIndex = 0;
                }
            } else {

                mImageIndex =0;
            }

            handlerImageShift.postDelayed(this, SHIFT_INTERVAL_IMAGES);
        }};


    //----------------------------------------------------------------------------------------------
    // Poll service handling
    //----------------------------------------------------------------------------------------------
    //Handle poll service !
    private void startPollService() {
        PollServiceIntent = new Intent(getActivity(), PollService.class);
        getActivity().startService(PollServiceIntent);
        Log.i("GPS", "Started GPS service !!!");
        PollServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("POLL","Poll data recieved !!!!");
                Log.i("POLL", "Got action : " + intent.getStringExtra("action"));
                handleLockerAction(intent.getStringExtra("action"));
            }
        };
        getActivity().registerReceiver(PollServiceReceiver, new IntentFilter(PollService.BROADCAST_ACTION));

    }

    //----------------------------------------------------------------------------------------------
    // Handle actions required on the locker
    //----------------------------------------------------------------------------------------------
    private void handleLockerAction(String action) {
        switch (action) {
            case "OPEN_LOCKER_0":
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.open));
                mainText.setText(getResources().getString(R.string.mainTextOpen) + " #" + 0);
                resetScreen();
                //TODO: Open the locker 1
                break;
            case "CLOSE_LOCKER_0":
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.closed));
                mainText.setText(getResources().getString(R.string.mainTextClose) + " #" + 0);
                resetScreen();
                //TODO: Close the locker 1
                break;

            case "OPEN_LOCKER_1":
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.open));
                mainText.setText(getResources().getString(R.string.mainTextOpen) + " #" + 1);
                resetScreen();
                //TODO: Open the locker 1
                break;
            case "CLOSE_LOCKER_1":
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.closed));
                mainText.setText(getResources().getString(R.string.mainTextClose) + " #" + 1);
                resetScreen();
                break;
                //TODO: Close the locker 1
            case "OPEN_LOCKER_2":
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.open));
                mainText.setText(getResources().getString(R.string.mainTextOpen) + " #" + 2);
                resetScreen();
                //TODO: Open the locker 1
                break;
            case "CLOSE_LOCKER_2":
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.closed));
                mainText.setText(getResources().getString(R.string.mainTextClose) + " #" + 2);
                resetScreen();
                //TODO: Close the locker 1
                break;



         }






    }

    //Restore main screen after some time
    private void resetScreen() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mainImage.setImageDrawable(getResources().getDrawable(R.drawable.logo));
                mainText.setText(getResources().getString(R.string.mainText));
            }
        }, 5000);
    }


}


