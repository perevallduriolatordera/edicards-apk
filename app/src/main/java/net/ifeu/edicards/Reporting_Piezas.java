package net.ifeu.edicards;

import java.util.LinkedHashMap;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Utils.Screen.ScreenManager;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.LinearLayout;

public class Reporting_Piezas extends Activity {

	AppConfig _appConfig;
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting_piezas);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.95f).getWidth();
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
        try {
			fillPiezas();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    private void fillPiezas() throws Exception
    {
    	_appConfig = (AppConfig) this.getApplicationContext();

    	LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
		mainLinearLayout.setBackgroundColor(Color.WHITE);
		mainLinearLayout.setPadding(16, 16, 16, 16);

		// Header gris
		final LinearLayout headerLayout = new LinearLayout(this);
		headerLayout.setBackgroundColor(Color.parseColor("#607D8B"));
		headerLayout.setOrientation(LinearLayout.HORIZONTAL);
		headerLayout.setPadding(16, 12, 16, 12);
		android.widget.LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		headerParams.setMargins(0, 0, 0, 16);
		headerLayout.setLayoutParams(headerParams);

		LabelColor titleLabel = new LabelColor(this, Color.WHITE, Gravity.CENTER);
		titleLabel.setText("PIEZAS - MOVIMIENTO DE ARTÍCULOS");
		titleLabel.setTextSize(18);
		android.widget.LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		titleLabel.setLayoutParams(titleParams);
		headerLayout.addView(titleLabel);
		mainLinearLayout.addView(headerLayout);

		// Headers de columnas
		final LinearLayout columnHeaderLayout = new LinearLayout(this);
		columnHeaderLayout.setBackgroundColor(Color.parseColor("#455A64"));
		columnHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
		columnHeaderLayout.setPadding(12, 10, 12, 10);
		android.widget.LinearLayout.LayoutParams columnHeaderParams = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		columnHeaderParams.setMargins(0, 0, 0, 8);
		columnHeaderLayout.setLayoutParams(columnHeaderParams);

		android.widget.LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		LabelColor colArticulo = new LabelColor(this, Color.WHITE, Gravity.LEFT);
		colArticulo.setText("Artículo");
		colArticulo.setTextSize(13);
		colArticulo.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.3f).getWidth());
		colArticulo.setLayoutParams(colParams);
		columnHeaderLayout.addView(colArticulo);

		LabelColor colVendidas = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
		colVendidas.setText("Vendidas");
		colVendidas.setTextSize(13);
		colVendidas.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
		colVendidas.setLayoutParams(colParams);
		columnHeaderLayout.addView(colVendidas);

		LabelColor colRetiradas = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
		colRetiradas.setText("Retiradas");
		colRetiradas.setTextSize(13);
		colRetiradas.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
		colRetiradas.setLayoutParams(colParams);
		columnHeaderLayout.addView(colRetiradas);

		LabelColor colStock = new LabelColor(this, Color.WHITE, Gravity.RIGHT);
		colStock.setText("Stock");
		colStock.setTextSize(13);
		colStock.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
		colStock.setLayoutParams(colParams);
		columnHeaderLayout.addView(colStock);

		mainLinearLayout.addView(columnHeaderLayout);
    	    	   		 
	 	final LinearLayout layout = new LinearLayout(this);
		layout.removeAllViews();

    	layout.setOrientation(LinearLayout.VERTICAL);
		layout.setBackgroundColor(Color.WHITE);
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);

    	LinkedHashMap<String,Articulo> articulos = _appConfig.getCache().getAllArticulos();

		int rowCount = 0;
    	for (Articulo art : articulos.values())
    	{
    		final LinearLayout layout2 = new LinearLayout(this);
    		layout2.removeAllViews();
        	layout2.setOrientation(LinearLayout.HORIZONTAL);

			// Alternar colores de fila
			if (rowCount % 2 == 0) {
				layout2.setBackgroundColor(Color.parseColor("#F5F5F5"));
			} else {
				layout2.setBackgroundColor(Color.WHITE);
			}
    		layout2.setPadding(12, 8, 12, 8);
        	
        	int unidades = 0;
    		if (_appConfig.getWorkingArea().CurrentReporting.Defectuosos.containsKey(art.Descripcion))
    		{
    			Reporting.Defectuoso defectuoso = _appConfig.getWorkingArea().CurrentReporting.Defectuosos.get(art.Descripcion);
    			unidades = defectuoso.unidades;
    		}

			int TEXT_SIZE = 14;
			if (!_appConfig.getWorkingArea().CurrentReporting.Vendidos.containsKey(art.Descripcion))
    		{
    			LabelColor label = new LabelColor(this,Color.BLACK, Gravity.LEFT);
        		label.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		label.setText(art.Descripcion);
        		label.setTextSize(TEXT_SIZE);
        		label.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.3f).getWidth());
        		label.setLayoutParams(params);
        		
        		layout2.addView(label);
        		
        		LabelColor vendido = new LabelColor(this,Color.RED,true,  Gravity.RIGHT);
        		vendido.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		vendido.setText("0 un. vendidas");
        		vendido.setTextSize(TEXT_SIZE);
        		vendido.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
        		vendido.setLayoutParams(params);
        		
        		layout2.addView(vendido);
        		
        		int color = (unidades != 0) ? Color.RED : Color.MAGENTA;
        		
        		LabelColor retirado = new LabelColor(this,color, true, Gravity.RIGHT);
        		retirado.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		retirado.setText(unidades + " un. retiradas");
        		retirado.setTextSize(TEXT_SIZE);
        		retirado.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
        		retirado.setLayoutParams(params);
            	
        		layout2.addView(retirado);
            	
            	LabelColor stock = new LabelColor(this,Color.BLACK, true, Gravity.RIGHT);
            	stock.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	stock.setText(art.Stock + " un. almacén");
            	stock.setTextSize(TEXT_SIZE);
            	stock.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
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
        		label.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.3f).getWidth());
        		label.setLayoutParams(params);
        		
        		layout2.addView(label);
        		
        		LabelColor vendido = new LabelColor(this,Color.MAGENTA, true,  Gravity.RIGHT);
        		vendido.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		vendido.setText(venta.unidades + " un. vendidas");
        		vendido.setTextSize(TEXT_SIZE);
        		vendido.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
        		vendido.setLayoutParams(params);
            	
            	layout2.addView(vendido);
            	
            	int color = (unidades != 0) ? Color.RED : Color.MAGENTA;
        		
        		LabelColor retirado = new LabelColor(this,color, true, Gravity.RIGHT);
        		retirado.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		retirado.setText(unidades + " un. retiradas");
        		retirado.setTextSize(TEXT_SIZE);
        		retirado.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
        		retirado.setLayoutParams(params);
        		
        		layout2.addView(retirado);
            	
            	LabelColor stock = new LabelColor(this,Color.BLACK, true,  Gravity.RIGHT);
            	stock.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	stock.setText(art.Stock + " un. almacén");
            	stock.setTextSize(TEXT_SIZE);
            	stock.setWidth(ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth());
            	stock.setLayoutParams(params);
            	
            	layout2.addView(stock);

    		}
    		
    		layout.addView(layout2);
			rowCount++;
    	}

		// Botón Cerrar
		final LinearLayout layout3 = new LinearLayout(this);
		layout3.removeAllViews();

		layout3.setOrientation(LinearLayout.HORIZONTAL);
		layout3.setBackgroundColor(Color.WHITE);
		layout3.setGravity(Gravity.CENTER);
		layout3.setPadding(16, 24, 16, 24);

		android.widget.LinearLayout.LayoutParams layout3Params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		layout3.setLayoutParams(layout3Params);

		ButtonColor closeButton = new ButtonColor(this, Color.parseColor("#D32F2F"));

		closeButton.setText("CERRAR");
		int TEXT_SIZE_BUTTON = 16;
		closeButton.setTextSize(TEXT_SIZE_BUTTON);
		closeButton.setTextColor(Color.WHITE);
		closeButton.setPadding(40, 16, 40, 16);
		closeButton.setGravity(Gravity.CENTER);

		android.widget.LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		buttonParams.setMargins(0, 0, 0, 0);
		closeButton.setLayoutParams(buttonParams);

		closeButton.setOnClickListener(arg0 -> {
			try {
				finish();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		layout3.addView(closeButton);

    	mainLinearLayout.addView(layout);
		mainLinearLayout.addView(layout3);

    }
}
