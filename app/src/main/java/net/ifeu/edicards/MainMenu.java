package net.ifeu.edicards;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class MainMenu extends FragmentActivity {
    
	AppConfig _appConfig;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	Log.i("MainMenu", "Creem el menú principal");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        
        // Fixem la posició de la pantalla a horitzontal
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
    }
    
    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }*/
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
		// Inicialitzem l'objecte AppConfig
    	Log.i("MainMenu","Capturem event onActivityResult");
    	
    	/*switch (requestCode)
    	{
    		case Constants.REQUEST_SEARCH_CUSTOMER: this.callDepositManager(resultCode, data); 
    	}*/
    }
    
		
}