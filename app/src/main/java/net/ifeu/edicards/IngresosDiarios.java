package net.ifeu.edicards;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.IngresoDiario;
import net.ifeu.edicards.DataTier.Ingresos;
import net.ifeu.edicards.Pdf.incident.IncidentPdfCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.util.Calendar;
import java.util.Date;

public class IngresosDiarios extends Activity {

	AppConfig _appConfig;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ingresos_diarios);

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
		takePhotos.setVisibility(_appConfig.getWorkingArea().CurrentDeposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE) ? VISIBLE : GONE);

		takePhotos.setOnClickListener( (View v)-> {
			this.showIngresosDiariosDialog();
		});
	}

	private void showIngresosDiariosDialog() {
		DepositManagerExtension.Dialogs.StartIngresoDiarioDialog(this);
	}


	@Override
	public void onStart() {
		super.onStart();

	}

	public void OnSaveIngreso() throws Exception {

		_appConfig = (AppConfig) this.getApplicationContext();

		String ingresos = ((EditText) findViewById(R.id.txtIngresosDiarios)).getText().toString();
		String gastos = ((EditText) findViewById(R.id.txtGastosDiarios)).getText().toString();
		String cantidad = ((EditText) findViewById(R.id.txtTotalIngresar)).getText().toString();

		if (gastos.equals(ConstantsTypes.EMPTY_STRING)) {
			_appConfig.getMessageBox().Show("Ingreso", "Tiene que informar de la cantidad de gastos", _appConfig,
					MessageBoxType.Information);
			return;
		}

		IngresoDiario ingresoDiario = Factory.build(IngresoDiario.class, _appConfig);

		ingresoDiario.Fecha = new Date();
		ingresoDiario.Ingresos = Double.parseDouble(ingresos);
		ingresoDiario.Gastos = Double.parseDouble(gastos);
		ingresoDiario.Cantidad = Double.parseDouble(cantidad);
		ingresoDiario.save();

		_appConfig.getMessageBox().Show("Ingreso", "El ingreso se ha realizado correctamente", this._appConfig,
				MessageBoxType.Information);

				
		String text = "Se ha efectuado un nuevo ingreso diario con los siguientes datos: " + ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE + "Comercial: " + this._appConfig.getUser().User + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE + "FECHA: " + ingresoDiario.Fecha.toString()
				+ ConstantsTypes.NEW_LINE + "CANTIDAD:" + cantidad
				+ ConstantsTypes.NEW_LINE + "INGRESOS:" + ingresos
				+ ConstantsTypes.NEW_LINE + "GASTOS:" + gastos
				+ ConstantsTypes.NEW_LINE;

		Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(), IncidenciaType.IngresoDiario,
				text);
		incidencia.create(new IncidentPdfCreator(_appConfig));

	}
}
