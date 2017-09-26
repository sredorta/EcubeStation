package com.ecube_solutions.ecubestation.Fragment;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ecube_solutions.ecubestation.R;
import com.ecube_solutions.ecubestation.Singleton.Locker;

/**
 * Created by sergi on 26/09/2017.
 */

public class RunningFragment extends Fragment {
    private static Boolean DEBUG_MODE = true;           // Enables/disables verbose logging
    private static final String TAG ="MainFragment::";
    Intent GpsServiceIntent;
    public BroadcastReceiver GpsServiceReceiver;
    public Locker mLocker;

    public static RunningFragment newInstance() {
        return new RunningFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.running_fragment,container,false);


        return v;
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
//        GpsServiceIntent = new Intent(getActivity(), GpsService.class);
//        getActivity().startService(GpsServiceIntent);
//        Log.i("GPS", "Started GPS service !!!");
/*        GpsServiceReceiver = new BroadcastReceiver() {
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
*/
    }

    @Override
    public void onDestroy() {
//        getActivity().stopService(GpsServiceIntent);
//        getActivity().unregisterReceiver(GpsServiceReceiver);
        super.onDestroy();
    }

}


