package net.ifeu.edicards;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Efectivo;
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
import java.util.Timer;

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

		CheckBox checkBox = findViewById(R.id.chkTipoAplicar);
		checkBox.setOnCheckedChangeListener( (buttonView, isChecked) -> {
			updateIngresos(!isChecked);
		});

		Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
        try {
            efectivo.getEfectivo();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

		TextView txtFecha = findViewById(R.id.txtFechaIngresosDiarios);
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		String formattedDate = format.format(efectivo.UpdateDateEfectivo);
		txtFecha.setText(formattedDate);

		EditText txtTotal = findViewById(R.id.txtTotalIngresar);
		txtTotal.setEnabled(false);

		final double ingresoReal = efectivo.Efectivo;;
		txtTotal.setText(String.valueOf(NumberDecimal.roundToNearestFive(ingresoReal)));

		EditText txtGasto = findViewById(R.id.txtGastosDiarios);
		txtGasto.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// Se ejecuta con cada tecla que cambia el texto
				CheckBox checkBox = findViewById(R.id.chkTipoAplicar);
				boolean applyNearestFive = !checkBox.isChecked();
				updateIngresos(applyNearestFive);
			}

			@Override
			public void afterTextChanged(Editable s) {}
		});

		EditText txtIngreso = findViewById(R.id.txtIngresosDiarios);
		txtIngreso.setText(String.valueOf(efectivo.Efectivo));
		txtIngreso.setEnabled(false);
		txtIngreso.setFocusable(false);
		txtIngreso.setFocusableInTouchMode(false);
		txtIngreso.setCursorVisible(false);
	}

	private IngresoDiario updateIngresos(boolean applyNearestFive) {
		EditText txtTotal = findViewById(R.id.txtTotalIngresar);
		EditText txtGasto = findViewById(R.id.txtGastosDiarios);
		EditText txtIngreso = findViewById(R.id.txtIngresosDiarios);

		Double ingreso = Double.parseDouble(txtIngreso.getText().toString());
		Double gasto;
		if (txtGasto.getText().length() == 0)
			gasto = 0.0;
		else
			gasto = Double.parseDouble(txtGasto.getText().toString());

		float total;
		if (applyNearestFive) {
			total = NumberDecimal.roundToNearestFive(ingreso - gasto);
		} else {
			total = (float) (ingreso - gasto);
		}

		txtTotal.setText(String.valueOf(total));

		Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
		try {
			efectivo.getEfectivo();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		IngresoDiario ingresoDiario = Factory.build(IngresoDiario.class, _appConfig);
		ingresoDiario.Ingresos = ingreso;
		ingresoDiario.Gastos = gasto;
		ingresoDiario.Cantidad = total;
		ingresoDiario.Fecha = new Date();
		ingresoDiario.FechaRegistro = efectivo.UpdateDateEfectivo;

		return ingresoDiario;
    }

	private void showIngresosDiariosDialog() {
		DepositManagerExtension.Dialogs.StartIngresoDiarioDialog(this);
	}


	@Override
	public void onStart() {
		super.onStart();
	}

	public void OnSaveIngreso() throws Exception {

		CheckBox checkBox = findViewById(R.id.chkTipoAplicar);

		_appConfig = (AppConfig) this.getApplicationContext();
		IngresoDiario ingresoDiario = updateIngresos(!checkBox.isChecked());

		String ingresos = ((EditText) findViewById(R.id.txtIngresosDiarios)).getText().toString();
		String gastos = ((EditText) findViewById(R.id.txtGastosDiarios)).getText().toString();
		String cantidad = ((EditText) findViewById(R.id.txtTotalIngresar)).getText().toString();

		if (gastos.equals(ConstantsTypes.EMPTY_STRING)) {
			_appConfig.getMessageBox().Show("Ingreso", "Tiene que informar de la cantidad de gastos", IngresosDiarios.this,
					MessageBoxType.Information);
			return;
		}

		if (_appConfig.getWorkingArea().CurrentTransactionMetadata == null || _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument == null || _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument.equals(ConstantsTypes.EMPTY_STRING)) {
			if (Double.parseDouble(ingresos) != Double.parseDouble(gastos)) {
				_appConfig.getMessageBox().Show("Ingreso", "Debe adjuntar una fotografía obligatoriamente", IngresosDiarios.this,
						MessageBoxType.Information);
				return;
			}
		}

		ingresoDiario.save();

		_appConfig.getMessageBox().Show("Ingreso", "El ingreso se ha realizado correctamente", IngresosDiarios.this,
				MessageBoxType.Information);

		updateEfectivo(Double.parseDouble(ingresos), Double.parseDouble(gastos), Double.parseDouble(cantidad));
		createIncidencia(cantidad, ingresos, gastos);
		finish();
	}
	private void updateEfectivo(double ingresos, double gastos, double cantidad) {

		Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
        try {
            efectivo.getEfectivo();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (cantidad > 0) {
			efectivo.Efectivo = efectivo.Efectivo - (cantidad + gastos);
		} else {
			efectivo.Efectivo = ingresos - gastos;
		};
        try {
			efectivo.UpdateDateIngreso = new Date();
            efectivo.update();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	private void createIncidencia(String cantidad, String ingresos, String gastos) {
		SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
		String formattedDate = formato.format(new Date());

		double ingresosDouble = Double.parseDouble(ingresos);
		double gastosDouble = Double.parseDouble(gastos);
		
		String text = "Se ha efectuado un nuevo ingreso diario con los siguientes datos: " + ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE + "Comercial: " + this._appConfig.getUser().User + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE + "FECHA: " + formattedDate
				+ ConstantsTypes.NEW_LINE + "RECAUDADO: " + DepositManagerExtension.Format.RoundTo2Decimals(ingresos).toString()
				+ ConstantsTypes.NEW_LINE + "GASTOS: " + DepositManagerExtension.Format.RoundTo2Decimals(gastos).toString()
				+ ConstantsTypes.NEW_LINE + "INGRESOS: " + DepositManagerExtension.Format.RoundTo2Decimals(cantidad).toString();

		if (gastosDouble > ingresosDouble) {
			double cantidadAdeudada = gastosDouble - ingresosDouble;
			text += ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE + "Edicards adeuda al comercial la cantidad de: " + DepositManagerExtension.Format.RoundTo2Decimals(String.valueOf(cantidadAdeudada)).toString() + " €";
		}
		
		text += ConstantsTypes.NEW_LINE;

		Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(), IncidenciaType.IngresoDiario,
				text);

		if (_appConfig.getWorkingArea().CurrentTransactionMetadata != null && _appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument != null && !_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument.equals(ConstantsTypes.EMPTY_STRING)) {
			String attachmentLabel = gastosDouble > ingresosDouble ? "GASTOS" : "INGRESO";
			incidencia.Attachments.put(attachmentLabel,
					_appConfig.getWorkingArea().CurrentTransactionMetadata.IngresoDocument);
		}
        try {
            incidencia.create(new IncidentPdfCreator(_appConfig));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

	@Override
	public void onBackPressed() {
		// Evita que la actividad se cierre con el botón Atrás
	}
}
