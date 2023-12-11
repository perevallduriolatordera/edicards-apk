package net.ifeu.edicards;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class Upgrade_App extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade__app);
		
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = 700;
        params.width  = 600;
        getWindow().setAttributes(params);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.upgrade__app, menu);
		return true;
	}

}
