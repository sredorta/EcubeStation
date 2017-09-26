
package com.ecube_solutions.ecubestation.Activity;


import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import com.ecube_solutions.ecubestation.Fragment.RunningFragment;

/**
 * Created by sredorta on 11/10/2016.
 */
public class RunningActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return RunningFragment.newInstance();
    }
    public static Intent newIntent(Context packageContext, String param) {
        Intent intent = new Intent(packageContext,RunningActivity.class);
        intent.putExtra("test", param);
        return intent;
    }
}