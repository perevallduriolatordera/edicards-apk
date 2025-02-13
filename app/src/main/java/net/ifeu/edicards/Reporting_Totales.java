package net.ifeu.edicards;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.IngresoDiario;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Utils.Screen.ScreenManager;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Reporting_Totales extends Activity {

	AppConfig _appConfig;
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reporting__totales);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = LayoutParams.FILL_PARENT;
		params.width  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.8f).getWidth();
        getWindow().setAttributes(params);

        fillTotales();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_reporting__totales, menu);
        return true;
    }
    
	private LinearLayout addCounter(String text, String value, int color, boolean compress) {
		
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this);
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.15f).getWidth();
		params.height  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.1f).getWidth();

		card.setLayoutParams(params);
    	card.setPadding(20, 20, 20, 20);
    	
    	LinearLayout layout1 = new LinearLayout(this);
		layout1.setOrientation(LinearLayout.VERTICAL);
		
		LabelColor label = new LabelColor(this, color, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(compress ? 12 : 16);
		
		
    	LabelColor label2 = new LabelColor(this, Color.BLACK, true, Gravity.CENTER);
		label2.setText(String.valueOf(value));
		label2.setTextSize(24);
    	
    	layout1.addView(label);
    	layout1.addView(label2);
    	
    	card.addView(layout1);
    	
    	layout.addView(card);
    	
    	return layout;
    	
	}
    
    private void fillTotales()
    {
    	
    	DecimalFormat df = new DecimalFormat("0.00");
    	
    	_appConfig = (AppConfig) this.getApplicationContext();
    	Reporting reporting = _appConfig.getWorkingArea().CurrentReporting;
    	
    	if (reporting == null) {
    		return;
    	}
    	    	 
    	LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
    	    	   		 
	 	final LinearLayout layout = new LinearLayout(this);
		layout.removeAllViews();
		layout.setOrientation(LinearLayout.VERTICAL);
    	
    	LinearLayout layout2 = new LinearLayout(this);
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		    	
		// Clientes nuevos
		
		layout2.addView(this.addCounter("Clientes Nuevos", String.valueOf(reporting.totales.Nuevos), Color.MAGENTA, false));
    	
    	// Depósitos retirados
		
		layout2.addView(this.addCounter("Clientes de Baja", String.valueOf(reporting.totales.Retirados), Color.RED, false));
    	
    	// Visitas
    	
		layout2.addView(this.addCounter("Número de Visitas", String.valueOf(reporting.totales.Visitas), Color.BLUE, false));
    	
    	// Visitas con albarán
		
		//layout2.addView(this.addCounter("Número de Visitas con albarán", String.valueOf(reporting.totales.NumeroSerieA), Color.BLACK, false));
    	
    	//layout.addView(layout2);
		
		// Total Facturación
		
		layout2.addView(this.addCounter("Total facturado", reporting.totales.Facturado + " €", Color.BLACK, true));
				
		layout.addView(layout2);
		
    	// siguiente linea
    	
    	layout2 = new LinearLayout(this);
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		
		
		// Albaranes Edi
		
		layout2.addView(this.addCounter("Albaranes EDI (" + reporting.totales.InicialSerieA + " - " + reporting.totales.FinalSeriaA + ")", reporting.totales.NumeroSerieA + " / " + df.format(reporting.totales.TotalSerieA) + " €", Color.BLACK, true));
		
		//Cantidad Pagada Serie A
		
		layout2.addView(this.addCounter("COBROS EDI Total", reporting.totales.CantidadPagadaSerieA + " €", Color.BLACK, false));
						
		layout.addView(layout2);
		
		// siguiente linea
		
		layout2 = new LinearLayout(this);
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);

		// Albaranes B
		
		layout2.addView(this.addCounter("Albaranes (" + reporting.totales.InicialSerieB + " - " + reporting.totales.FinalSerieB + ")", reporting.totales.NumeroSerieB + " / " + df.format(reporting.totales.TotalSerieB) + " €", Color.BLACK, true));
		
    	//Cantidad Pagada Serie B
		
		layout2.addView(this.addCounter("COBROS", reporting.totales.CantidadPagadaSerieB + " €", Color.BLACK, false));

		layout.addView(layout2);
		
		// siguiente linea
		
		layout2 = new LinearLayout(this);
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		
    	// Cantidad Pagada Serie A + B
		
    	double cantidad = reporting.totales.CantidadPagadaSerieA + reporting.totales.CantidadPagadaSerieB;
		layout2.addView(this.addCounter("Total COBROS", df.format(cantidad) + " €", Color.BLACK, false));

		
    	// Cantidad Ingresos
    	
    	double ingresos = this.getIngresos();
    	layout2.addView(this.addCounter("Total INGRESADO", df.format(ingresos) + " €", Color.BLACK, false));

		// Cantidad Gastos

		double gastos = this.getGastos();
		layout2.addView(this.addCounter("Total GASTOS", df.format(gastos) + " €", Color.BLACK, false));

		
    	// 	Cantidad Pendiente a ingresar
    	double pendiente = cantidad - gastos;
    	layout2.addView(this.addCounter("Total PENDIENTE INGRESAR", df.format(pendiente) + " €", Color.BLACK, true));
    	
    	layout.addView(layout2);
    	
    	mainLinearLayout.addView(layout);
 
    }
    
    private double getIngresos()  {

    	double IngresosTotales = 0;
    	
    	try {
    		IngresoDiario ingresos = Factory.build(IngresoDiario.class, _appConfig);
    		ArrayList<IngresoDiario> list = ingresos.getListIngresosOfThisWeek(new Date());

    		for (IngresoDiario ingreso : list) {

    			if (ingreso.Cantidad != 0)
    				IngresosTotales += ingreso.Ingresos;
    		}

    	} catch (Exception e) {
			throw new RuntimeException(e);
    	}
    	
    	return IngresosTotales;
    	
	}

	private double getGastos()  {

		double GastosTotales = 0;

		try {
			IngresoDiario ingresos = Factory.build(IngresoDiario.class, _appConfig);
			ArrayList<IngresoDiario> list = ingresos.getListIngresosOfThisWeek(new Date());

			for (IngresoDiario ingreso : list) {

				if (ingreso.Cantidad != 0)
					GastosTotales += ingreso.Gastos;
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return GastosTotales;
	}
}
