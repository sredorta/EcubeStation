package com.ecube_solutions.ecubestation.Activity;


import android.support.v4.app.Fragment;

import com.ecube_solutions.ecubestation.Fragment.MainFragment;

/**
 * Created by sredorta on 11/10/2016.
 */
public class MainActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return MainFragment.newInstance();
    }

}
