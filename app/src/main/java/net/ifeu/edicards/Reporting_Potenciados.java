package net.ifeu.edicards;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.edicards.DataTier.Reporting.Potenciado;
import net.ifeu.library.Controls.LabelColor;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.widget.LinearLayout;

public class Reporting_Potenciados extends Activity {

	AppConfig _appConfig;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting__potenciados);
        
        fillPotenciados();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_reporting__potenciados, menu);
        return true;
    }
    
    private void fillPotenciados()
    {
    	_appConfig = (AppConfig) this.getApplicationContext();
    	Reporting reporting = _appConfig.getWorkingArea().CurrentReporting;
    	 
    	LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
    	    	   		 
	 	final LinearLayout layout = new LinearLayout(this);
		layout.removeAllViews();
		layout.setOrientation(LinearLayout.VERTICAL);
    	
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
    	
    	for (Potenciado potenciado : reporting.Potenciados.values())
    	{
    		
    		final LinearLayout layout2 = new LinearLayout(this);
    		layout2.removeAllViews();
    		layout2.setOrientation(LinearLayout.HORIZONTAL);
    		layout2.setBackgroundResource(R.drawable.card_background);
    		layout2.setGravity(Gravity.CENTER);
    		layout2.setPadding(20, 20, 20, 20);

    		LabelColor label = new LabelColor(this,Color.BLACK, Gravity.LEFT);
    		label.setRawInputType(InputType.TYPE_CLASS_NUMBER);
    		label.setText(potenciado.articulo.Descripcion);
			int TEXT_SIZE = 20;
			label.setTextSize(TEXT_SIZE);
    		label.setWidth(300);
    		label.setLayoutParams(params);
        	
        	layout2.addView(label);
        	
        	LabelColor unidades = new LabelColor(this,Color.BLUE, true, Gravity.CENTER);
        	unidades.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        	unidades.setText(potenciado.unidades + " unidades");
        	unidades.setTextSize(TEXT_SIZE);
        	unidades.setWidth(150);
        	unidades.setLayoutParams(params);
        	
        	layout2.addView(unidades);
        	
        	layout.addView(layout2);
    	}
    	
    	mainLinearLayout.addView(layout);
 
    }

    
}
