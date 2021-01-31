package net.ifeu.edicards;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.library.Utils.MessageBoxType;

import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class CustomerData extends Activity {

	AppConfig _appConfig;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_customer_data);

		_appConfig = (AppConfig) this.getApplicationContext();
		try {
			_appConfig.getWorkingArea().CurrentDeposito.InitializePersistance(
					_appConfig, this);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		android.view.WindowManager.LayoutParams params = getWindow()
				.getAttributes();
		params.height = android.app.ActionBar.LayoutParams.WRAP_CONTENT;
		params.width = 1200;
		getWindow().setAttributes(
				(android.view.WindowManager.LayoutParams) params);

		try {
			this.fillFields();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_customer_data, menu);
		return true;
	}

	private void fillFields() throws Exception {
		
		Log.i("CustomerData", "FillFields");

		Deposito deposito = _appConfig.getWorkingArea().CurrentDeposito;

		((EditText) findViewById(R.id.lblNIFCliente)).setText(deposito.NIF);
		((EditText) findViewById(R.id.lblNombreCliente))
				.setText(deposito.Nombre);
		((EditText) findViewById(R.id.lblRazonCliente))
		.setText(deposito.Razon);
		((EditText) findViewById(R.id.lblDireccionCliente))
				.setText(deposito.Direccion1);
		((EditText) findViewById(R.id.lblLocalidadCliente))
				.setText(deposito.Poblacion);
		((EditText) findViewById(R.id.lblProvinciaCliente))
				.setText(deposito.Provincia);
		((EditText) findViewById(R.id.lblCodigoPostalCliente))
				.setText(deposito.CodigoPostal);
		((EditText) findViewById(R.id.lblTelefono1Cliente))
				.setText(deposito.Telefono1);
		((EditText) findViewById(R.id.lblTelefono2Cliente))
				.setText(deposito.Telefono2);
		((EditText) findViewById(R.id.lblFaxCliente)).setText(deposito.Fax);
		((EditText) findViewById(R.id.lblMailCliente)).setText(deposito.Mail);
		
		deposito.ClienteInfo.InitializePersistance(_appConfig, this.getApplicationContext());
		
		deposito.ClienteInfo.setClienteInfoByCliente(deposito.Cliente);
		Log.i("CustomerData" , deposito.Cliente.Nombre);
		Log.i("CustomerData", deposito.Cliente.ClienteInfo.CCC);

	    ((EditText) findViewById(R.id.lblCCCCliente)).setText(deposito.ClienteInfo.CCC);
	    ((EditText) findViewById(R.id.lblRepresentanteCliente)).setText(deposito.ClienteInfo.Representante);
	    ((EditText) findViewById(R.id.lblDniRepresentanteCliente)).setText(deposito.ClienteInfo.DniRepresentante);
	    
	}
	
	public void OnClose(View v) throws Exception {
		finish();
	}
	
	public void OnApply(View v) throws Exception {
		
		_appConfig.getWorkingArea().CurrentDeposito.NIFPrevious = _appConfig.getWorkingArea().CurrentDeposito.NIF;
		_appConfig.getWorkingArea().CurrentDeposito.NombrePrevious = _appConfig.getWorkingArea().CurrentDeposito.Nombre;
		_appConfig.getWorkingArea().CurrentDeposito.RazonPrevious = _appConfig.getWorkingArea().CurrentDeposito.Razon;
		_appConfig.getWorkingArea().CurrentDeposito.DireccionPrevious = _appConfig.getWorkingArea().CurrentDeposito.Direccion1;
		_appConfig.getWorkingArea().CurrentDeposito.PoblacionPrevious = _appConfig.getWorkingArea().CurrentDeposito.Poblacion;
		_appConfig.getWorkingArea().CurrentDeposito.CodigoPostalPrevious = _appConfig.getWorkingArea().CurrentDeposito.CodigoPostal;
		_appConfig.getWorkingArea().CurrentDeposito.ProvinciaPrevious = _appConfig.getWorkingArea().CurrentDeposito.Provincia;
		_appConfig.getWorkingArea().CurrentDeposito.Telefono1Previous = _appConfig.getWorkingArea().CurrentDeposito.Telefono1;
		_appConfig.getWorkingArea().CurrentDeposito.Telefono2Previous = _appConfig.getWorkingArea().CurrentDeposito.Telefono2;
		_appConfig.getWorkingArea().CurrentDeposito.FaxPrevious = _appConfig.getWorkingArea().CurrentDeposito.Fax;
		_appConfig.getWorkingArea().CurrentDeposito.MailPrevious = _appConfig.getWorkingArea().CurrentDeposito.Mail;
		_appConfig.getWorkingArea().CurrentDeposito.CCCPrevious = _appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.CCC;
		
		
		_appConfig.getWorkingArea().CurrentDeposito.DatosFiscalesUpdated = this.isUpdated() && 
				!_appConfig.getWorkingArea().CurrentDeposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE);
		
		_appConfig.getWorkingArea().CurrentDeposito.CCCUpdated = this.isUpdatedCCC();
		
		Log.i("CustomerData","Updated CCC:" + String.valueOf(_appConfig.getWorkingArea().CurrentDeposito.CCCUpdated));
		
		_appConfig.getWorkingArea().CurrentDeposito.NIF = ((EditText) findViewById(R.id.lblNIFCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Nombre = ((EditText) findViewById(R.id.lblNombreCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Razon = ((EditText) findViewById(R.id.lblRazonCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Direccion1 = ((EditText) findViewById(R.id.lblDireccionCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Poblacion = ((EditText) findViewById(R.id.lblLocalidadCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Provincia = ((EditText) findViewById(R.id.lblProvinciaCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.CodigoPostal = ((EditText) findViewById(R.id.lblCodigoPostalCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Telefono1 = ((EditText) findViewById(R.id.lblTelefono1Cliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Telefono2 = ((EditText) findViewById(R.id.lblTelefono2Cliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Fax = ((EditText) findViewById(R.id.lblFaxCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.Mail = ((EditText) findViewById(R.id.lblMailCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.CCC = ((EditText) findViewById(R.id.lblCCCCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.Representante = ((EditText) findViewById(R.id.lblRepresentanteCliente))
				.getText().toString();
		_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.DniRepresentante = ((EditText) findViewById(R.id.lblDniRepresentanteCliente))
				.getText().toString();
		
		_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.InitializePersistance(_appConfig, this.getApplicationContext());
		_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.update();
		
		Deposito deposito = _appConfig.getWorkingArea().CurrentDeposito;
		if (deposito.DatosFiscalesUpdated) {
			// Generamos la incidencia de cambio de datos fiscales
			
			boolean result = _appConfig.getMessageBox().ShowWithResult("Datos del cliente",
					"Se va a proceder a enviar los cambios de los datos fiscales del cliente. Desea Continuar?", this,
					MessageBoxType.Information);
			if (!result)
				return;

			String text = "Datos antiguos: " + Constants.NEW_LINE + Constants.NEW_LINE
					+ "NÚM. DEPOSITO DIMONI: " + deposito.NumDoc + Constants.NEW_LINE
					+ "NÚM. DEPOSITO TABLET (RefExt): " + deposito.IdDeposito + Constants.NEW_LINE
					+ "NIF/CIF: " + deposito.NIFPrevious + Constants.NEW_LINE + "NOMBRE: "
					+ deposito.NombrePrevious + Constants.NEW_LINE + "RAZÓN: " + deposito.RazonPrevious
					+ Constants.NEW_LINE + "DIRECCION: " + deposito.DireccionPrevious
					+ Constants.NEW_LINE + "POBLACION: " + deposito.PoblacionPrevious
					+ Constants.NEW_LINE + "CODIGO POSTAL: " + deposito.CodigoPostalPrevious
					+ Constants.NEW_LINE + "PROVINCIA: " + deposito.ProvinciaPrevious
					+ Constants.NEW_LINE + "TELEFONO 1: " + deposito.Telefono1Previous
					+ Constants.NEW_LINE + "TELEFONO 2: " + deposito.Telefono2Previous
					+ Constants.NEW_LINE + "FAX: " + deposito.FaxPrevious + Constants.NEW_LINE + "MAIL: "
					+ deposito.MailPrevious + Constants.NEW_LINE + Constants.NEW_LINE
					+ Constants.NEW_LINE + "Han sido modificados por: " + Constants.NEW_LINE
					+ Constants.NEW_LINE + "NIF/CIF: " + deposito.NIF + Constants.NEW_LINE + "NOMBRE: "
					+ deposito.Nombre + Constants.NEW_LINE + "RAZÓN: " + deposito.Razon
					+ Constants.NEW_LINE + "DIRECCION: " + deposito.Direccion1 + Constants.NEW_LINE
					+ "POBLACION: " + deposito.Poblacion + Constants.NEW_LINE + "CODIGO POSTAL: "
					+ deposito.CodigoPostal + Constants.NEW_LINE + "PROVINCIA: " + deposito.Provincia
					+ Constants.NEW_LINE + "TELEFONO 1: " + deposito.Telefono1 + Constants.NEW_LINE
					+ "TELEFONO 2: " + deposito.Telefono2 + Constants.NEW_LINE + "FAX: " + deposito.Fax
					+ Constants.NEW_LINE + "MAIL: " + deposito.Mail;

			Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(),
					IncidenciaType.DatosFiscales, text);
			incidencia.create();

		}

		if (deposito.CCCUpdated) {
			// Generamos la incidencia de cambio de datos de cuenta
			// corriente
			
			boolean result = _appConfig.getMessageBox().ShowWithResult("Datos del cliente",
					"Se va a proceder a enviar los cambios de la cuenta corriente del cliente. Desea Continuar?", this,
					MessageBoxType.Information);
			if (!result)
				return;

			String text = "Datos de la cuenta corriente del cliente: " + Constants.NEW_LINE
					+ Constants.NEW_LINE + "CODIGO CLIENTE: " + deposito.CodigoCliente
					+ Constants.NEW_LINE + "NOMBRE DEL CLIENTE: " + deposito.Nombre + Constants.NEW_LINE
					+ "NUM CUENTA CORRIENTE: " + deposito.ClienteInfo.CCC + Constants.NEW_LINE
					+ "REPRESENTANTE: " + deposito.ClienteInfo.Representante + Constants.NEW_LINE
					+ "DNI REPRESENTANTE: " + deposito.ClienteInfo.DniRepresentante + Constants.NEW_LINE;

			Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(),
					IncidenciaType.CuentaCorriente, text);
			incidencia.create();

		}
		
		finish();
	}
	
	private boolean isUpdated()
	{
		if (!_appConfig.getWorkingArea().CurrentDeposito.NIF.trim().equals(((EditText) findViewById(R.id.lblNIFCliente))
				.getText().toString().trim()))
				return true;
		
		if (!_appConfig.getWorkingArea().CurrentDeposito.Nombre.trim().equals(((EditText) findViewById(R.id.lblNombreCliente))
				.getText().toString().trim()))
			return true;
		
		if (!_appConfig.getWorkingArea().CurrentDeposito.Razon.trim().equals(((EditText) findViewById(R.id.lblRazonCliente))
				.getText().toString().trim()))
			return true;
		
		if (!_appConfig.getWorkingArea().CurrentDeposito.Direccion1.trim().equals(((EditText) findViewById(R.id.lblDireccionCliente))
				.getText().toString().trim()))
			return true;
		
		if (!_appConfig.getWorkingArea().CurrentDeposito.Poblacion.trim().equals( ((EditText) findViewById(R.id.lblLocalidadCliente))
				.getText().toString().trim()))
			return true;
		
		if (!_appConfig.getWorkingArea().CurrentDeposito.Provincia.trim().equals(((EditText) findViewById(R.id.lblProvinciaCliente))
				.getText().toString().trim()))
			return true;
		
		if (!_appConfig.getWorkingArea().CurrentDeposito.CodigoPostal.trim().equals(((EditText) findViewById(R.id.lblCodigoPostalCliente))
				.getText().toString().trim()))
			return true;
		
		return false;
	}
	
	private boolean isUpdatedCCC()
	{
		
		if (_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo == null)
		{
			Log.i("CustomerData", "ClienteInfo es null");
			_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo = new ClienteInfo();
		}
			
		if (!_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.CCC.trim().equals(((EditText) findViewById(R.id.lblCCCCliente))
				.getText().toString().trim()))
				return true;
		return false;
	}

}


