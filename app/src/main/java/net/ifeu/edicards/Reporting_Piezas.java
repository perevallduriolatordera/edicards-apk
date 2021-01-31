package net.ifeu.edicards;

import java.util.LinkedHashMap;

import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.library.Controls.LabelColor;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.LinearLayout;

public class Reporting_Piezas extends Activity {

	private final int TEXT_SIZE = 14;
	AppConfig _appConfig;
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting_piezas);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = LayoutParams.FILL_PARENT;
        params.width  = 800;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
        try {
			fillPiezas();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}
    }
    
    private void fillPiezas() throws Exception
    {
    	_appConfig = (AppConfig) this.getApplicationContext();
    	 
    	LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
    	    	   		 
	 	final LinearLayout layout = new LinearLayout(this);
		layout.removeAllViews();
		
    	layout.setOrientation(LinearLayout.VERTICAL);
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
    	
    	LinkedHashMap<String,Articulo> articulos = _appConfig.getCache().getAllArticulos();
    	
    	for (Articulo art : articulos.values())
    	{
    		final LinearLayout layout2 = new LinearLayout(this);
    		layout2.removeAllViews();
        	layout2.setOrientation(LinearLayout.HORIZONTAL);
    		layout2.setBackgroundResource(R.drawable.card_background);
    		layout2.setGravity(Gravity.CENTER);
    		layout2.setPadding(20, 20, 20, 20);
        	
        	int unidades = 0;
    		if (_appConfig.getWorkingArea().CurrentReporting.Defectuosos.containsKey(art.Descripcion))
    		{
    			Reporting.Defectuoso defectuoso = _appConfig.getWorkingArea().CurrentReporting.Defectuosos.get(art.Descripcion);
    			unidades = defectuoso.unidades;
    		}
    		
    		if (!_appConfig.getWorkingArea().CurrentReporting.Vendidos.containsKey(art.Descripcion))
    		{
    			LabelColor label = new LabelColor(this,Color.BLACK, Gravity.LEFT);
        		label.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		label.setText(art.Descripcion);
        		label.setTextSize(TEXT_SIZE);
        		label.setWidth(200);
        		label.setLayoutParams(params);
        		
        		layout2.addView(label);
        		
        		LabelColor vendido = new LabelColor(this,Color.RED,true,  Gravity.RIGHT);
        		vendido.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		vendido.setText("0 un. vendidas");
        		vendido.setTextSize(TEXT_SIZE);
        		vendido.setWidth(150);
        		vendido.setLayoutParams(params);
        		
        		layout2.addView(vendido);
        		
        		int color = (unidades != 0) ? Color.RED : Color.MAGENTA;
        		
        		LabelColor retirado = new LabelColor(this,color, true, Gravity.RIGHT);
        		retirado.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		retirado.setText(String.valueOf(unidades) + " un. retiradas");
        		retirado.setTextSize(TEXT_SIZE);
        		retirado.setWidth(150);
        		retirado.setLayoutParams(params);
            	
        		layout2.addView(retirado);
            	
            	LabelColor stock = new LabelColor(this,Color.BLACK, true, Gravity.RIGHT);
            	stock.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	stock.setText(String.valueOf(art.Stock) + " un. almacén");
            	stock.setTextSize(TEXT_SIZE);
            	stock.setWidth(150);
            	stock.setLayoutParams(params);
            	
            	layout2.addView(stock);

    		}
    		else
    		{
    			Reporting.Vendido venta = _appConfig.getWorkingArea().CurrentReporting.Vendidos.get(art.Descripcion);
    			
    			LabelColor label = new LabelColor(this,Color.BLACK, Gravity.LEFT);
        		label.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		label.setText(art.Descripcion);
        		label.setTextSize(TEXT_SIZE);
        		label.setWidth(200);
        		label.setLayoutParams(params);
        		
        		layout2.addView(label);
        		
        		LabelColor vendido = new LabelColor(this,Color.MAGENTA, true,  Gravity.RIGHT);
        		vendido.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		vendido.setText(String.valueOf(venta.unidades) + " un. vendidas");
        		vendido.setTextSize(TEXT_SIZE);
        		vendido.setWidth(150);
        		vendido.setLayoutParams(params);
            	
            	layout2.addView(vendido);
            	
            	int color = (unidades != 0) ? Color.RED : Color.MAGENTA;
        		
        		LabelColor retirado = new LabelColor(this,color, true, Gravity.RIGHT);
        		retirado.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		retirado.setText(String.valueOf(unidades) + " un. retiradas");
        		retirado.setTextSize(TEXT_SIZE);
        		retirado.setWidth(150);
        		retirado.setLayoutParams(params);
        		
        		layout2.addView(retirado);
            	
            	LabelColor stock = new LabelColor(this,Color.BLACK, true,  Gravity.RIGHT);
            	stock.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	stock.setText(String.valueOf(art.Stock) + " un. almacén");
            	stock.setTextSize(TEXT_SIZE);
            	stock.setWidth(150);
            	stock.setLayoutParams(params);
            	
            	layout2.addView(stock);

    		}
    		
    		layout.addView(layout2);
    	}
    	
    	mainLinearLayout.addView(layout);
 
    }


    
}
