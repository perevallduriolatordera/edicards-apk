package net.ifeu.edicards;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class MainMenu extends FragmentActivity {

    // Deshabilitat provisionalment
    /*@Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_placeholder);
        if (!(currentFragment instanceof DepositManager)) {
                super.onBackPressed();
        }
    }*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }
    
		
}