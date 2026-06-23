package net.ifeu.edicards;

import android.annotation.SuppressLint;
import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Gasto;
import net.ifeu.edicards.DataTier.GastosInfo;
import net.ifeu.edicards.Excel.LogBookCreator;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.IO.IOUtils;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

public class GastosManager extends Fragment {

	private AppConfig _appConfig ;
	private LinkedHashMap<String,Articulo> _articulos;
	private DatePicker _datePic;
	private Calendar _calendar;
	private EditText _editTextComment;

	private boolean _isRendered = false;
	private LinearLayout _mainLayout;

	public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
			_appConfig = (AppConfig) getActivity().getApplicationContext();
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

			if (!_isRendered)
	    		return inflater.inflate(R.layout.activity_gastos_manager, container,false);
			else
				return _mainLayout;
	    }

	    @Override
	    public void onActivityCreated(Bundle savedInstanceState)
	    {
	    	super.onActivityCreated(savedInstanceState);

			if (!_isRendered)
	    		addCalendar();

	        try {
				_articulos = _appConfig.getCache().getAllGastos();
			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}

	        try {
				if (!_isRendered) addArticles();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			_isRendered = true;
			_mainLayout = (LinearLayout) this.getActivity().findViewById(R.id.gastosManagerLayout);
		
	    }

		private void addArticles() throws Exception
		{
			for (Articulo articulo : _articulos.values()) {
				addLine(articulo);
			}

			if (_articulos.size() == 0)
				_appConfig.getMessageBox().Show("Control de gastos", "No se han encontrado artículos. Sincronice datos con el servidor", getActivity(), MessageBoxType.Information);

		}

	    private void addCalendar()
	    {	
	    	LinearLayout calendarLayout = (LinearLayout) getActivity().findViewById(R.id.calendarMainLinearLayout);

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
	    	
	    	calendarLayout.addView(layout);

	    }
	    
	    private final DatePicker.OnDateChangedListener dateSetListener = new DatePicker.OnDateChangedListener() {

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
					  throw new RuntimeException(e);					}
	  		
	    	  }
	    	 };
	    
	    @SuppressLint("SuspiciousIndentation")
		private void addLine(Articulo articulo) throws Exception
	    {
	    	articulo.Activo = true;

	    	LinearLayout articleLayout = (LinearLayout) getActivity().findViewById(R.id.mainLinearLayout);

	    	// Usar LayoutParams con peso para alineación correcta
	    	android.widget.LinearLayout.LayoutParams paramsLayout = new LinearLayout.LayoutParams(
	    		LayoutParams.MATCH_PARENT,
	    		LayoutParams.WRAP_CONTENT
	    	);
	    	paramsLayout.setMargins(0, 0, 0, 8);

	    	final LinearLayout layout = new LinearLayout(getActivity());

	    	layout.setBackgroundResource(R.drawable.card_background);
	    	layout.setLayoutParams(paramsLayout);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			layout.setPadding(12, 12, 12, 12);
			layout.setGravity(Gravity.CENTER_VERTICAL);

			int TEXT_SIZE = 14;

			// Código - 25% del ancho
	    	android.widget.LinearLayout.LayoutParams paramsCodigo = new LinearLayout.LayoutParams(
	    		0, LayoutParams.WRAP_CONTENT, 0.25f
	    	);
	    	LabelColor codigoArticulo = new LabelColor(getActivity(),Color.parseColor("#424242"));
	    	codigoArticulo.setText(articulo.CodigoArticulo);
			codigoArticulo.setTextSize(TEXT_SIZE);
	    	codigoArticulo.setLayoutParams(paramsCodigo);
	    	codigoArticulo.setGravity(Gravity.CENTER);

	    	// Descripción - 45% del ancho
	    	android.widget.LinearLayout.LayoutParams paramsDesc = new LinearLayout.LayoutParams(
	    		0, LayoutParams.WRAP_CONTENT, 0.45f
	    	);
	    	LabelColor articuloDescripcion = new LabelColor(getActivity(),Color.parseColor("#212121"), true);
	    	articuloDescripcion.setTag(articulo);
	    	articuloDescripcion.setText(articulo.Descripcion);
	    	articuloDescripcion.setTextSize(TEXT_SIZE);
	    	articuloDescripcion.setPaintFlags(articuloDescripcion.getPaintFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
	    	articuloDescripcion.setLayoutParams(paramsDesc);
	    	articuloDescripcion.setGravity(Gravity.CENTER);
	    	
	    	Gasto gasto = Factory.build(Gasto.class, _appConfig);

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
	    	
	    	GastosInfo gastosInfo = Factory.build(GastosInfo.class, _appConfig);

	    	gastosInfo = gastosInfo.getGastoInfoByFecha(_calendar.getTime());
	    	_editTextComment.setText(gastosInfo.Comentario);
	    	_editTextComment.setTag(gastosInfo);
	    	
	    	_editTextComment.setOnFocusChangeListener((view, hasFocus) -> {
				EditText textBox = (EditText) view;

				GastosInfo gastosInfo1 = (GastosInfo) view.getTag();
				gastosInfo1.Comentario = textBox.getText().toString();
				gastosInfo1.Fecha = setWeekStart(_calendar).getTime();

				if (gastosInfo1.IsNew)
					try {

						if (!gastosInfo1.Comentario.equals(ConstantsTypes.EMPTY_STRING))
							gastosInfo1.save();

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				else
					try {

						if (!gastosInfo1.Comentario.equals(ConstantsTypes.EMPTY_STRING))
							gastosInfo1.update();

					} catch (Exception e) {
						throw new RuntimeException(e);					}
			});
	    	
	    	// Cantidad - 30% del ancho
	    	android.widget.LinearLayout.LayoutParams paramsCantidad = new LinearLayout.LayoutParams(
	    		0, LayoutParams.WRAP_CONTENT, 0.30f
	    	);
	    	TextBoxColor cantidad = new TextBoxColor(getActivity(),Color.parseColor("#424242"),Gravity.CENTER);
	    	cantidad.setInputType(InputType.TYPE_CLASS_NUMBER);
	    	cantidad.setHint(String.valueOf(gasto.Cantidad));
	    	cantidad.setTextSize(TEXT_SIZE);
	    	cantidad.setLayoutParams(paramsCantidad);
	    	cantidad.setKeyListener(DigitsKeyListener.getInstance(false,true));

			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	    	imm.showSoftInput(cantidad, InputMethodManager.SHOW_IMPLICIT);
	    	cantidad.setTag(gasto);
	    	
	    	cantidad.setOnFocusChangeListener((view, hasFocus) -> {
				if (!hasFocus)
				{
					EditText textBox = (EditText) view;
					double cantidad1;

					if (textBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						cantidad1 = Double.parseDouble(((EditText) view).getHint().toString());
					else
						cantidad1 = Double.parseDouble(((EditText) view).getText().toString());


					Gasto gasto1 = (Gasto) view.getTag();
					gasto1.Cantidad = cantidad1;

					if (gasto1.IsNew) {
						if (cantidad1 > 0) {
							gasto1.IsNew = false;
							try {
								gasto1.save();

								XmlCreator creator = new XmlCreator(_appConfig);
								creator.createXmlGastos(_calendar.getTime());

								// Creamos excel de trazabilidad si es necesario

								if (!isLogBookCreated())
									createLogBook();

							} catch (Exception e) {
								throw new RuntimeException(e);							}
						}
					} else
						try {
							gasto1.update();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
				}
			});
	    	
	    	layout.addView(codigoArticulo);
	    	layout.addView(articuloDescripcion);
	    	layout.addView(cantidad);
	    	
	    	articleLayout.addView(layout);

	    }
	    
	    private Calendar setWeekStart(Calendar calendar) {
	    	  while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
	    	    calendar.add(Calendar.DATE, -1);
	    	  }
	    	  return calendar;
	    }

		private void createLogBook() {
			LogBookCreator logBookCreator = new LogBookCreator(_appConfig);
			try {
				logBookCreator.createExcel30Days();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private boolean isLogBookCreated() {
			String directory = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
					+ ConstantsFolders.FOLDER_LOGBOOK;

			return IOUtils.getFilesFromDirectory(directory).size() > 0;
		}
	    
}
