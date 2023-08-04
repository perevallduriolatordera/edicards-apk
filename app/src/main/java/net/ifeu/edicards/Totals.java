package net.ifeu.edicards;

import java.text.DecimalFormat;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Totales;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class Totals extends Activity {

	AppConfig _appConfig;
	LinearLayout _mainLayout;

	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_totals);
        
        this._mainLayout = (LinearLayout) this.findViewById(R.id.BasesMainLinearLayout);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = LayoutParams.FILL_PARENT;
        params.width  = 1000;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
    	ButtonColor button = this.addButton(this, Color.WHITE, "Cerrar pantalla", 14, 150, null);
    	button.setOnClickListener(arg0 -> finish());
    	
		_mainLayout.addView(button);
    	
		fillBases();
		fillTotals();
		
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_totals, menu);
        return true;
    }
    
    private void fillTotals()
    {
    	Totales totales = _appConfig.getWorkingArea().CurrentDeposito.Totales;
    	DecimalFormat dec = new DecimalFormat("0.00");
    	 
    	this._mainLayout.setOrientation(LinearLayout.VERTICAL);
    	    	   		 
	 	final LinearLayout layout = new LinearLayout(this);
		//layout.removeAllViews();
		
		layout.addView(this.addCounter("Total Base (Sin Dte)", dec.format(totales.TotalBaseSinDte) + " €  ", Color.BLUE, false));
		layout.addView(this.addCounter("Total descuento comercial", dec.format(totales.TotalDescuentoProntoPago) + " €  ", Color.BLUE, false));
		layout.addView(this.addCounter("Total descuento financiero", dec.format(totales.TotalDescuentoFinanciero) + " €  ", Color.BLUE, false));
		layout.addView(this.addCounter("Total IVA", dec.format(totales.TotalIVA) + " €  ", Color.BLUE, false));
		layout.addView(this.addCounter("Total Recargo", dec.format(totales.TotalRecargo) + " €  ", Color.BLUE, false));
		layout.addView(this.addCounter("Total Albarán", dec.format(totales.Total) + " €  ", Color.BLUE, false));
    	
    	ButtonColor button = this.addButton(this, Color.WHITE, "Cerrar pantalla", 14, 150, null);
    	button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				finish();				
			}
		});

    	
		_mainLayout.addView(layout);
    	
 
    }
    
    private void fillBases()
    {
    	
    	_appConfig = (AppConfig) this.getApplicationContext();

    	try {
			_appConfig.getWorkingArea().CurrentDeposito.Calculate();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    	
    	DecimalFormat dec = new DecimalFormat("0.00");
    	 
    	 for (Totales.Base base : _appConfig.getWorkingArea().CurrentDeposito.Totales.Bases.values())
    	 {    		 
    		 	final LinearLayout layout = new LinearLayout(this);
    			
    	    	layout.setOrientation(LinearLayout.HORIZONTAL);
    	    	
    			layout.addView(this.addCounter("Base (" + base.IvaPerc +" %)", dec.format(base.Base) + " €  ", Color.BLUE, false));
    			layout.addView(this.addCounter("Descuentos (" + base.IvaPerc +" %)", dec.format(base.Descuento) + " €  ", Color.BLUE, false));
    			layout.addView(this.addCounter("IVA (" + base.IvaPerc +" %)", dec.format(base.Iva) + " €  ", Color.BLUE, false));
    			layout.addView(this.addCounter("Recargo (" + base.IvaPerc +" %)", dec.format(base.Recargo) + " €  ", Color.BLUE, false));
    			layout.addView(this.addCounter("Total (" + base.IvaPerc +" %)", dec.format(base.Total) + " €  ", Color.BLUE, false));

    			_mainLayout.addView(layout);
    	    		 
    	 }
		
    }
    
    private LinearLayout addCounter(String text, String value, int color, boolean compress) {
		
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this);
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width=150;
    	params.height=100;
    	card.setLayoutParams(params);
    	card.setPadding(20, 20, 20, 20);
    	
    	LinearLayout layout1 = new LinearLayout(this);
		layout1.setOrientation(LinearLayout.VERTICAL);
		
		LabelColor label = new LabelColor(this, color, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(compress ? 8 : 12);
		
		
    	LabelColor label2 = new LabelColor(this, Color.BLACK, true, Gravity.CENTER);
		label2.setText(String.valueOf(value));
		label2.setTextSize(16);
    	
    	layout1.addView(label);
    	layout1.addView(label2);
    	
    	card.addView(layout1);
    	
    	layout.addView(card);
    	
    	return layout;
    	
	}
    
	public ButtonColor addButton(Context context, int color, String text, int size, int width, LayoutParams params) {
		ButtonColor button = new ButtonColor(context, color);

		button.setText(text);
		button.setTextSize(size);
		button.setWidth(width);
		button.setTextColor(Color.WHITE);
		if (params!= null) button.setLayoutParams(params);
		
		return button;
	}

    
	
	public void OnClose(View v) {
		finish();
	}

    
}
