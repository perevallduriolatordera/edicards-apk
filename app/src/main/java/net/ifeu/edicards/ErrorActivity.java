package net.ifeu.edicards;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ErrorActivity extends Fragment {
		
	public void onCreate(Bundle savedInstanceState) {
		
        super.onCreate(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
        
                
    	return inflater.inflate(R.layout.error_activity, container,false);
    	
    	 //Inicialitzem l'objecte AppConfig
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
    	super.onActivityCreated(savedInstanceState);
    	
    }

}
