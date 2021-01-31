package net.ifeu.edicards;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Gasto;
import net.ifeu.edicards.DataTier.GastosInfo;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.Utils.MessageBoxType;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;

public class GastosManager extends Fragment {

		private AppConfig _appConfig ;
		private LinkedHashMap<String,Articulo> _articulos;
		private DatePicker _datePic;
		private Calendar _calendar;
		private EditText _editTextComment;
		
		private final int TEXT_SIZE = 24;
		private final int FIELDS_WIDTH = 200;
		
		public void onCreate(Bundle savedInstanceState) {
			
	        super.onCreate(savedInstanceState);
	        Log.i("GastosManager","Create");
	       _calendar = Calendar.getInstance();
	       _datePic = new DatePicker(this.getActivity().getApplicationContext());
	       _editTextComment = new EditText(this.getActivity().getApplicationContext());
	       
	    }
		
		@Override
		public void onDestroy()
		{
			super.onDestroy();
		}
	    
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
		
	    	Log.i("GastosManager","Abans de carregar layout");
	    	return inflater.inflate(R.layout.activity_gastos_manager, container,false);
	    }
	    
	    private void addArticles() throws Exception
	    {
	    
	    	for (Articulo articulo : _articulos.values())
	    			addLine(articulo);
	  		  	        
  	        if (_articulos.size() == 0)
  	        	_appConfig.getMessageBox().Show("Control de gastos", "No se han encontrado artículos. Sincronice datos con el servidor", getActivity(), MessageBoxType.Information);
  		
	    }
	    
	    @Override
	    public void onActivityCreated(Bundle savedInstanceState)
	    {
	    	super.onActivityCreated(savedInstanceState);
	    	
	    	addCalendar();
	    	//Inicialitzem l'objecte AppConfig

	        _appConfig = (AppConfig) getActivity().getApplicationContext();
	        
	        try {
				_articulos = _appConfig.getCache().getAllGastos();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e1);
			}
	        
	        
	        try {
				addArticles();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			}
		
	    }
	    
	    @Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
		}
	    
	    
	    private void addCalendar()
	    {	
	    	LinearLayout mainLinearLayout = (LinearLayout) getActivity().findViewById(R.id.calendarMainLinearLayout);
	    	//mainLinearLayout.removeAllViews();
	    	
	    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
	    	
	    	_datePic.init(_calendar.get(Calendar.YEAR), _calendar
	    			    .get(Calendar.MONTH), _calendar.get(Calendar.DATE),
	    			    dateSetListener);
	    	 
	    	_datePic.setLayoutParams(params);
	    	
	    	android.widget.LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
	    	params2.setMargins(0, 55, 10, 0);
	    	_editTextComment.setLayoutParams(params2);
	    	_editTextComment.setHint("Escriba aquí su comentario referente a la gestión de dietas semanal");
	    	
	    	final LinearLayout layout = new LinearLayout(getActivity());
	    	layout.setOrientation(LinearLayout.HORIZONTAL);
	    	layout.removeAllViews();
	    	
	    	layout.addView(_datePic);
	    	layout.addView(_editTextComment);
	    	
	    	mainLinearLayout.addView(layout);
	    	
	    }
	    
	    private DatePicker.OnDateChangedListener dateSetListener = new DatePicker.OnDateChangedListener() {

	    	  @Override
	    	  public void onDateChanged(DatePicker view, int year, int monthOfYear,
	    	    int dayOfMonth) {
	    		  _calendar.set(year, monthOfYear, dayOfMonth);
	    		  _calendar = setWeekStart(_calendar);
	    		  
	    		  LinearLayout mainLinearLayout = (LinearLayout) getActivity().findViewById(R.id.mainLinearLayout);
			      mainLinearLayout.removeAllViews();
			    	
			      try {
						addArticles();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
					}
	  		
	    	  }
	    	 };
	    
	    private void addLine(Articulo articulo) throws Exception
	    {
	    	
	    	articulo.Activo = true; 
	    	
	    	LinearLayout mainLinearLayout = (LinearLayout) getActivity().findViewById(R.id.mainLinearLayout);
	    	
	    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
	    	
	    	final LinearLayout layout = new LinearLayout(getActivity());
	    	
	    	layout.setBackgroundResource(R.drawable.card_background);
	    	layout.setLayoutParams(params);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			layout.setPadding(20, 20, 20, 20);

	    	
	    	LabelColor codigoArticulo = new LabelColor(getActivity(),Color.BLACK);
	    	codigoArticulo.setText(articulo.CodigoArticulo);
	    	codigoArticulo.setTextSize(TEXT_SIZE-8);
	    	codigoArticulo.setWidth(150);
	    	codigoArticulo.setLayoutParams(params);
	    	
	    	LabelColor articuloDescripcion = new LabelColor(getActivity(),Color.BLACK, true);
	    	articuloDescripcion.setTag(articulo);
	    	articuloDescripcion.setText(articulo.Descripcion);
	    	articuloDescripcion.setTextSize(TEXT_SIZE-8);
	    	articuloDescripcion.setWidth(300);
	    	articuloDescripcion.setPaintFlags(articuloDescripcion.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
	    	articuloDescripcion.setLayoutParams(params);
	    	
	    	Gasto gasto = new Gasto();
	    	gasto.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
	    	
	    	@SuppressWarnings("deprecation")
			Date fecha = new Date(_datePic.getYear() - 1900, _datePic.getMonth(), _datePic.getDayOfMonth());
	    	_calendar.setTime(fecha);
	    	_calendar = setWeekStart(_calendar);
	    	
	    	if (gasto.setGastoByFechaArticulo(_calendar.getTime(), articulo))
	    		gasto.IsNew = false;
	    	else
	    	{
	    		gasto.Fecha = setWeekStart(_calendar).getTime();
	    		gasto.Articulo = articulo;
	    		gasto.Cantidad = 0;
	    		gasto.IsNew = true;
	    	}
	    	
	    	GastosInfo gastosInfo = new GastosInfo();
	    	gastosInfo.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
	    	
	    	gastosInfo = gastosInfo.getGastoInfoByFecha(_calendar.getTime());
	    	_editTextComment.setText(gastosInfo.Comentario);
	    	_editTextComment.setTag(gastosInfo);
	    	
	    	_editTextComment.setOnFocusChangeListener(new OnFocusChangeListener() {
	    	
	    		public void onFocusChange(View view, boolean hasFocus)
	    		{
	    			EditText textBox = (EditText) view;
    				
    				GastosInfo gastosInfo = (GastosInfo) view.getTag();
    				gastosInfo.Comentario = textBox.getText().toString();
    				gastosInfo.Fecha = setWeekStart(_calendar).getTime();
    				try {
						gastosInfo.InitializePersistance(_appConfig, getActivity());
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e1);
					}
    				
    				if (gastosInfo.IsNew)
						try {
							
							if (!gastosInfo.Comentario.equals(Constants.EMPTY_STRING))
							{
								Log.i("GastosManager Save Info",gastosInfo.Comentario);
								Log.i("GastosManager","Save GastosInfo");
								gastosInfo.save();
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
						}
					else
						try {
							
							if (!gastosInfo.Comentario.equals(Constants.EMPTY_STRING))
							{
								Log.i("GastosManager","Update GastosInfo");
								gastosInfo.update();
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
						}
	    		}
	    	});
	    	
	    	TextBoxColor cantidad = new TextBoxColor(getActivity(),Color.argb(255, 100, 100, 50),Gravity.RIGHT);
	    	//unidadesEntradas.setInputType(InputType.TYPE_CLASS_NUMBER);
	    	cantidad.setInputType(InputType.TYPE_CLASS_NUMBER);
	    	cantidad.setHint(String.valueOf(gasto.Cantidad));
	    	cantidad.setTextSize(TEXT_SIZE);
	    	cantidad.setWidth(FIELDS_WIDTH);
	    	cantidad.setKeyListener(DigitsKeyListener.getInstance(false,true));
	    	InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	    	imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	    	imm.showSoftInput(cantidad, InputMethodManager.SHOW_IMPLICIT);
	    	cantidad.setLayoutParams(params);
	    	cantidad.setTag(gasto);
	    	
	    	cantidad.setOnFocusChangeListener(new OnFocusChangeListener() {
	    		public void onFocusChange(View view, boolean hasFocus)
	    		{
	    			if (!hasFocus)
	    			{
	    				EditText textBox = (EditText) view;
	    				double cantidad = 0;
	    				
	    				if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
	    					cantidad = Double.parseDouble(((EditText) view).getHint().toString());
	    				else
	    					cantidad = Double.parseDouble(((EditText) view).getText().toString());
	    				
	    				
	    				Gasto gasto = (Gasto) view.getTag();
	    				gasto.Cantidad = cantidad;
	    				
	    				if (gasto.IsNew) {
	    					if (cantidad > 0) {
		    					gasto.IsNew = false;
		    					try {
									gasto.save();
									
									XmlCreator creator = new XmlCreator(_appConfig, getActivity());
									creator.createXmlGastos(_calendar.getTime());;
									
								} catch (Exception e) {
									// TODO Auto-generated catch block
									_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
								}
	    					}
	    				} else
							try {
								gasto.update();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
							}
	    				
	    			}
	    		}
	    	});
	    	
	    	layout.addView(codigoArticulo);
	    	layout.addView(articuloDescripcion);
	    	layout.addView(cantidad);
	    	
	    	mainLinearLayout.addView(layout);
	    	
	    	//getActivity().Ç(layout, layoutParams);
	    	
	    }
	    
	    private Calendar setWeekStart(Calendar calendar) {
	    	  while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
	    	    calendar.add(Calendar.DATE, -1);
	    	  }
	    	  return calendar;
	    }
	    
}
