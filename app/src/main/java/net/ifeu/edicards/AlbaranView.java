package net.ifeu.edicards;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;

public class AlbaranView extends Activity {

	private final int TEXT_SIZE = 14;
	private final int BUTTONS_WIDTH = 130;
	private final int TEXT_SIZE_BUTTON = 14;
	
	AppConfig _appConfig;
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit_view);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = LayoutParams.FILL_PARENT;
        params.width  = 1000;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
        try {
			fillAlbaran();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}
    }
    
    private void fillAlbaran() throws Exception
    {
    	_appConfig = (AppConfig) this.getApplicationContext();
    	 
    	LinearLayout mainLinearLayout = (LinearLayout) this.findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
    	mainLinearLayout.setBackgroundColor(Color.BLACK);
    	    	   		 
	 	final LinearLayout layout = new LinearLayout(this);
	 	layout.setBackgroundColor(Color.BLACK);
		layout.removeAllViews();
		
    	layout.setOrientation(LinearLayout.VERTICAL);
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
    	
    	_appConfig.getWorkingArea().CurrentDeposito.Calculate();
    	
    	List<LineaDeposito> tempList = new ArrayList<LineaDeposito>();

		for (LineaDeposito linea : _appConfig.getWorkingArea().CurrentDeposito.Lineas.values()) {
			tempList.add(linea);
		}

		Collections
				.sort(tempList, new LineaDeposito().new ArticuloComparator());

    	for (LineaDeposito linea : tempList)
    	{
    		final LinearLayout layout2 = new LinearLayout(this);
    		layout2.removeAllViews();
    		
        	layout2.setOrientation(LinearLayout.HORIZONTAL);
        	layout2.setBackgroundColor(Color.BLACK);
    		
    		if (linea.UnidadesFacturadas > 0)
    		{
    			LabelColor codigoArticuloLabel = new LabelColor(this,Color.WHITE, Gravity.LEFT);
        		codigoArticuloLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		codigoArticuloLabel.setText(linea.Articulo.CodigoArticulo);
        		codigoArticuloLabel.setTextSize(TEXT_SIZE);
        		codigoArticuloLabel.setWidth(150);
        		codigoArticuloLabel.setLayoutParams(params);
        		
        		layout2.addView(codigoArticuloLabel);
        		
        		LabelColor descripcionLabel = new LabelColor(this,Color.WHITE, Gravity.LEFT);
        		descripcionLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		descripcionLabel.setText(linea.Articulo.Descripcion);
        		descripcionLabel.setTextSize(TEXT_SIZE);
        		descripcionLabel.setWidth(300);
        		descripcionLabel.setLayoutParams(params);
        		
        		layout2.addView(descripcionLabel);
        		
        		LabelColor unidadesLabel = new LabelColor(this,Color.WHITE, Gravity.RIGHT);
        		unidadesLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		unidadesLabel.setText(String.valueOf(linea.UnidadesFacturadas) + " unidades");
        		unidadesLabel.setTextSize(TEXT_SIZE);
        		unidadesLabel.setWidth(150);
        		unidadesLabel.setLayoutParams(params);
            	
        		layout2.addView(unidadesLabel);
            	
            	LabelColor pvpLabel = new LabelColor(this,Color.WHITE, Gravity.RIGHT);
            	pvpLabel.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	pvpLabel.setText(String.valueOf(linea.PVP) + " €");
            	pvpLabel.setTextSize(TEXT_SIZE);
            	pvpLabel.setWidth(150);
            	pvpLabel.setLayoutParams(params);
            	
            	layout2.addView(pvpLabel);
            	
            	double totalLinea = RoundTo2Decimals(linea.PVP * linea.UnidadesFacturadas);
            	
            	LabelColor pvpTotalLinea = new LabelColor(this,Color.WHITE, Gravity.RIGHT);
            	pvpTotalLinea.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	pvpTotalLinea.setText(String.valueOf(totalLinea) + " €");
            	pvpTotalLinea.setTextSize(TEXT_SIZE);
            	pvpTotalLinea.setWidth(150);
            	pvpTotalLinea.setLayoutParams(params);
            	
            	layout2.addView(pvpTotalLinea);
            	
            	layout.addView(layout2);
            	
    		}
    		
    		if (linea.TotalAbono != 0)
        	{
        		final LinearLayout layout2Bis = new LinearLayout(this);
        		layout2Bis.removeAllViews();
        		
            	layout2Bis.setOrientation(LinearLayout.HORIZONTAL);
            	layout2Bis.setBackgroundColor(Color.BLACK);
            	
        		LabelColor codigoArticuloLabelAbono = new LabelColor(this,Color.WHITE, Gravity.LEFT);
        		codigoArticuloLabelAbono.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		codigoArticuloLabelAbono.setText(linea.Articulo.CodigoArticulo);
        		codigoArticuloLabelAbono.setTextSize(TEXT_SIZE);
        		codigoArticuloLabelAbono.setWidth(150);
        		codigoArticuloLabelAbono.setLayoutParams(params);
        		
        		layout2Bis.addView(codigoArticuloLabelAbono);
        		
        		LabelColor descripcionLabelAbono = new LabelColor(this,Color.WHITE, Gravity.LEFT);
        		descripcionLabelAbono.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		descripcionLabelAbono.setText(linea.Articulo.Descripcion);
        		descripcionLabelAbono.setTextSize(TEXT_SIZE);
        		descripcionLabelAbono.setWidth(300);
        		descripcionLabelAbono.setLayoutParams(params);
        		
        		layout2Bis.addView(descripcionLabelAbono);
        		
        		LabelColor unidadesLabelAbono = new LabelColor(this,Color.WHITE, Gravity.RIGHT);
        		unidadesLabelAbono.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        		unidadesLabelAbono.setText(String.valueOf(linea.UnidadesAbono) + " unidades");
        		unidadesLabelAbono.setTextSize(TEXT_SIZE);
        		unidadesLabelAbono.setWidth(150);
        		unidadesLabelAbono.setLayoutParams(params);
            	
        		layout2Bis.addView(unidadesLabelAbono);
            	
            	LabelColor pvpLabelAbono = new LabelColor(this,Color.WHITE, Gravity.RIGHT);
            	pvpLabelAbono.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	pvpLabelAbono.setText(String.valueOf(linea.PVPAbono * -1) + " €");
            	pvpLabelAbono.setTextSize(TEXT_SIZE);
            	pvpLabelAbono.setWidth(150);
            	pvpLabelAbono.setLayoutParams(params);
            	
            	layout2Bis.addView(pvpLabelAbono);
            	
            	double totalLinea = RoundTo2Decimals(linea.PVPAbono * -1 * linea.UnidadesAbono);
            	
            	LabelColor pvpTotalLinea = new LabelColor(this,Color.WHITE, Gravity.RIGHT);
            	pvpTotalLinea.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            	pvpTotalLinea.setText(String.valueOf(totalLinea) + " €");
            	pvpTotalLinea.setTextSize(TEXT_SIZE);
            	pvpTotalLinea.setWidth(150);
            	pvpTotalLinea.setLayoutParams(params);
            	
            	layout2.addView(pvpTotalLinea);
            	
            	layout.addView(layout2Bis);
        	}
    		
    	}
    	
    	android.widget.LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
		params2.setMargins(0, 30, 0, 0);
    	
    	// Totales
    	
    	final LinearLayout layoutTotales = new LinearLayout(this);
		layoutTotales.removeAllViews();
		
    	layoutTotales.setOrientation(LinearLayout.HORIZONTAL);
    	layoutTotales.setBackgroundColor(Color.BLACK);
    	
    	LabelColor TotalBase = new LabelColor(this,Color.YELLOW, Gravity.LEFT);
		TotalBase.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		TotalBase.setText("Base: " + String.valueOf(_appConfig.getWorkingArea().CurrentDeposito.Totales.TotalBase) + " €");
		TotalBase.setTextSize(TEXT_SIZE);
		TotalBase.setWidth(150);
		TotalBase.setLayoutParams(params2);
    	
		layoutTotales.addView(TotalBase);
		
		double total = 0;
		
		if (_appConfig.getWorkingArea().CurrentDeposito.Serie.equals(_appConfig.getUser().SerialInvoiceA))
			total = _appConfig.getWorkingArea().CurrentDeposito.Totales.Total;
		else
			total = _appConfig.getWorkingArea().CurrentDeposito.Totales.TotalBase;
		Log.i("AlbaranViewe", "Serie: " + _appConfig.getWorkingArea().CurrentDeposito.Serie);
    	
    	LabelColor TotalAlbaran = new LabelColor(this,Color.YELLOW, Gravity.LEFT);
    	TotalAlbaran.setRawInputType(InputType.TYPE_CLASS_NUMBER);
    	TotalAlbaran.setText("Total: " + String.valueOf(total) + " €");
    	TotalAlbaran.setTextSize(TEXT_SIZE);
    	TotalAlbaran.setWidth(150);
    	TotalAlbaran.setLayoutParams(params2);
    	
    	layoutTotales.addView(TotalAlbaran);
    	layout.addView(layoutTotales);
    	
    	// Botón Cerrar
		
		final LinearLayout layout3 = new LinearLayout(this);
		layout3.removeAllViews();
		
    	layout3.setOrientation(LinearLayout.HORIZONTAL);
    	layout3.setBackgroundColor(Color.BLACK);
    	
		ButtonColor closeButton = new ButtonColor(this, Color.WHITE);

		closeButton.setText("Cerrar");
		closeButton.setTextSize(TEXT_SIZE_BUTTON);
		closeButton.setWidth(BUTTONS_WIDTH);

		
		closeButton.setLayoutParams(params2);

		closeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {

					finish();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}
		});
		
		layout3.addView(closeButton);
		
    	mainLinearLayout.addView(layout);
    	mainLinearLayout.addView(layout3);
 
    }
    
    double RoundTo2Decimals(double val) {
		DecimalFormat df2 = new DecimalFormat("0.00");
		return Double.valueOf(df2.format(val).replace(",", "."));
	}

    @Override
	public void onDestroy() {
		
		super.onDestroy();

	}
    
}
