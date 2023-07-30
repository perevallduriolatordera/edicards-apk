package net.ifeu.edicards;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.Ingresos;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.util.Calendar;
import java.util.Date;

public class IngresoData extends Fragment {

	AppConfig _appConfig;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		_appConfig = (AppConfig) getActivity().getApplicationContext();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	   return inflater.inflate(R.layout.activity_ingreso_data, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
	   Button button = (Button) getActivity().findViewById(R.id.btnSaveIngreso);
		   
	   final IngresoData that = this;
	   
	   button.setOnClickListener(v -> {
		  try {
			  that.OnSaveIngreso();
		  } catch (Exception e) {
			  throw new RuntimeException(e);
		  }
	   });
		   
		super.onActivityCreated(savedInstanceState);
	}

	public void OnSaveIngreso() throws Exception {

		String entidad = ((EditText) getActivity().findViewById(R.id.lblEntidad)).getText().toString();
		String cantidad = ((EditText) getActivity().findViewById(R.id.lblCantidad)).getText().toString();
		String referencia = ((EditText) getActivity().findViewById(R.id.lblReferencia)).getText().toString();
		String descripcion = ((EditText) getActivity().findViewById(R.id.lblObservaciones)).getText().toString();
		DatePicker datePicker = ((DatePicker) getActivity().findViewById(R.id.lblFecha));

		int day = datePicker.getDayOfMonth();
		int month = datePicker.getMonth();
		int year = datePicker.getYear();

		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day);

		Date fecha = calendar.getTime();
		
		if (entidad.equals(Constants.EMPTY_STRING)) {
			_appConfig.getMessageBox().Show("Ingreso", "Tiene que informar de la entidad", getActivity(),
					MessageBoxType.Information);
			return;
		}
		
		if (referencia.equals(Constants.EMPTY_STRING)) {
			_appConfig.getMessageBox().Show("Ingreso", "Tiene que informar de la referencia", getActivity(),
					MessageBoxType.Information);
			return;
		}
		
		if (cantidad.equals(Constants.EMPTY_STRING)) {
			_appConfig.getMessageBox().Show("Ingreso", "Tiene que informar una cantidad", getActivity(),
					MessageBoxType.Information);
			return;
		}

		Ingresos ingreso = new Ingresos();

		ingreso.Entidad = entidad;
		ingreso.Fecha = fecha;
		ingreso.Cantidad = Double.parseDouble(cantidad);
		ingreso.Referencia = referencia;
		ingreso.Descripcion = descripcion;

		
		ingreso.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
		ingreso.save();

		_appConfig.getMessageBox().Show("Ingreso", "El ingreso se ha realizado correctamente", getActivity(),
				MessageBoxType.Information);

				
		String text = "Se ha efectuado un nuevo ingreso con los siguientes datos: " + Constants.NEW_LINE
				+ Constants.NEW_LINE + "Comercial: " + this._appConfig.getUser().User + Constants.NEW_LINE + "ENTIDAD: "
				+ entidad + Constants.NEW_LINE + "FECHA: " + fecha.toString()  
				+ Constants.NEW_LINE + "CANTIDAD:" + cantidad 
				+ Constants.NEW_LINE + "REFERENCIA:" + referencia 
				+ Constants.NEW_LINE + "DESCRIPCION:" + descripcion 
				+ Constants.NEW_LINE;

		Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(), IncidenciaType.Ingreso,
				text);
		incidencia.create();

	}

}
