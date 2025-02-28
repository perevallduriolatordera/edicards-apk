package net.ifeu.edicards;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.IngresoDiario;
import net.ifeu.edicards.Pdf.incident.IncidentPdfCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Number.NumberDecimal;
import net.ifeu.library.Utils.Screen.ScreenManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IngresosDiarios extends Activity {

	AppConfig _appConfig;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ingresos_diarios);

		// Evita que la actividad se cierre al tocar fuera de ella
		setFinishOnTouchOutside(false);

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		params.height = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.6f).getHeight();
		params.width  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.8f).getWidth();
		getWindow().setAttributes(params);

		_appConfig = (AppConfig) this.getApplicationContext();

		Button button = findViewById(R.id.btnSaveIngresoDiario);

		final IngresosDiarios that = this;

		button.setOnClickListener(v -> {
			try {
				that.OnSaveIngreso();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		ButtonColor takePhotos = findViewById(R.id.btnIngresoDiarioPhoto);
		takePhotos.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_camera));

		takePhotos.setOnClickListener( (View v)-> {
			this.showIngresosDiariosDialog();
		});

		ButtonColor close = findViewById(R.id.btnCloseIngresoDiario);
		close.setVisibility(_appConfig.getWorkingArea().IsIngresoDiarioVoluntario ? View.VISIBLE : View.GONE);

		close.setOnClickListener( (View v)-> {
			finish();
		});


		this.assignValues();
	}

	private void assignValues() {

		boolean isCalculated = _appConfig.getWorkingArea().CurrentIngresoDiario != null;

		if (_appConfig.getWorkingArea().CurrentIngresoDiario == null) {
			_appConfig.getWorkingArea().CurrentIngresoDiario = Factory.build(IngresoDiario.class, _appConfig);
			_appConfig.getWorkingArea().CurrentIngresoDiario.Fecha = new Date();
			_appConfig.getWorkingArea().CurrentIngresoDiario.Ingresos = 0;
			_appConfig.getWorkingArea().CurrentIngresoDiario.Gastos = 0;
			_appConfig.getWorkingArea().CurrentIngresoDiario.Cantidad = 0;

		}

		TextView txtFecha = findViewById(R.id.txtFechaIngresosDiarios);
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		String formattedDate = format.format(_appConfig.getWorkingArea().CurrentIngresoDiario.Fecha);
		txtFecha.setText(formattedDate);

		EditText txtTotal = findViewById(R.id.txtTotalIngresar);
		txtTotal.setEnabled(false);

		final double ingresoReal = _appConfig.getWorkingArea().CurrentIngresoDiario.Ingresos;
		txtTotal.setText(String.valueOf(ingresoReal));

		EditText txtGasto = findViewById(R.id.txtGastosDiarios);
		txtGasto.setText("0");
		txtGasto.setOnFocusChangeListener((v, hasFocus) -> {
			// Cuando pierde el foco
			if (!hasFocus) {
				updateIngresos();
			}
		});

		EditText txtIngreso = findViewById(R.id.txtIngresosDiarios);
		txtIngreso.setText(String.valueOf(_appConfig.getWorkingArea().CurrentIngresoDiario.Ingresos));
		txtIngreso.setEnabled(!isCalculated);
		txtIngreso.setFocusable(!isCalculated);
		txtIngreso.setFocusableInTouchMode(!isCalculated);
		txtIngreso.setCursorVisible(!isCalculated);

	}

	private void updateIngresos() {
		EditText txtTotal = findViewById(R.id.txtTotalIngresar);
		EditText txtGasto = findViewById(R.id.txtGastosDiarios);
		EditText txtIngreso = findViewById(R.id.txtIngresosDiarios);

		Double ingreso = Double.parseDouble(txtIngreso.getText().toString());
		Double gasto = Double.parseDouble(txtGasto.getText().toString());
		double total = NumberDecimal.roundToNearestFive(ingreso - gasto);
		txtTotal.setText(String.valueOf(total));
		_appConfig.getWorkingArea().CurrentIngresoDiario.Ingresos = ingreso;
		_appConfig.getWorkingArea().CurrentIngresoDiario.Gastos = gasto;
		_appConfig.getWorkingArea().CurrentIngresoDiario.Cantidad = total;
        try {
            _appConfig.getWorkingArea().CurrentIngresoDiario.Fecha = new Date();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	private void showIngresosDiariosDialog() {
		DepositManagerExtension.Dialogs.StartIngresoDiarioDialog(this);
	}


	@Override
	public void onStart() {
		super.onStart();
	}

	public void OnSaveIngreso() throws Exception {

		SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		String formattedDate = formato.format(_appConfig.getWorkingArea().CurrentIngresoDiario.Fecha);

		_appConfig = (AppConfig) this.getApplicationContext();
		updateIngresos();

		String ingresos = ((EditText) findViewById(R.id.txtIngresosDiarios)).getText().toString();
		String gastos = ((EditText) findViewById(R.id.txtGastosDiarios)).getText().toString();
		String cantidad = ((EditText) findViewById(R.id.txtTotalIngresar)).getText().toString();

		if (gastos.equals(ConstantsTypes.EMPTY_STRING)) {
			_appConfig.getMessageBox().Show("Ingreso", "Tiene que informar de la cantidad de gastos", IngresosDiarios.this,
					MessageBoxType.Information);
			return;
		}

		/*if (Double.parseDouble(gastos) > Double.parseDouble(ingresos)) {
			_appConfig.getMessageBox().Show("Ingreso", "La cantidad de gastos no puede ser mayor que la de ingresos", IngresosDiarios.this,
					MessageBoxType.Information);
			return;
		}*/

		if (_appConfig.getWorkingArea().CurrentTransactionMetadata == null || _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument == null || _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument.equals(ConstantsTypes.EMPTY_STRING)) {
			if (Double.parseDouble(ingresos) > Double.parseDouble(gastos)) {

				_appConfig.getMessageBox().Show("Ingreso", "Tiene que adjuntar una imagen del ingreso", IngresosDiarios.this,
						MessageBoxType.Information);
				return;
			}
		}

		_appConfig.getWorkingArea().CurrentIngresoDiario.save();

		_appConfig.getMessageBox().Show("Ingreso", "El ingreso se ha realizado correctamente", IngresosDiarios.this,
				MessageBoxType.Information);

				
		String text = "Se ha efectuado un nuevo ingreso diario con los siguientes datos: " + ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE + "Comercial: " + this._appConfig.getUser().User + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE + "FECHA: " + formattedDate
				+ ConstantsTypes.NEW_LINE + "CANTIDAD:" + cantidad
				+ ConstantsTypes.NEW_LINE + "INGRESOS:" + ingresos
				+ ConstantsTypes.NEW_LINE + "GASTOS:" + gastos
				+ ConstantsTypes.NEW_LINE;

		Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(), IncidenciaType.IngresoDiario,
				text);

		if (_appConfig.getWorkingArea().CurrentTransactionMetadata != null && _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument != null && !_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument.equals(ConstantsTypes.EMPTY_STRING)) {
			incidencia.Attachments.put("INGRESO",
					_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument);
		}
		incidencia.create(new IncidentPdfCreator(_appConfig));

		finish();
	}

	@Override
	public void onBackPressed() {
		// Evita que la actividad se cierre con el botón Atrás
	}
}
