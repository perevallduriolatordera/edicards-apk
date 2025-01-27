package net.ifeu.edicards;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.Pdf.incident.IncidentPdfCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.ScreenManager;

import java.util.Date;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class CustomerData extends Activity {

	private AppConfig _appConfig;
	private static final int REQUEST_IMAGE_CAPTURE = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_customer_data);

		_appConfig = (AppConfig) this.getApplicationContext();

		android.view.WindowManager.LayoutParams params = getWindow()
				.getAttributes();
		params.height = android.app.ActionBar.LayoutParams.WRAP_CONTENT;
		ScreenManager.ScreenSize screenSize= ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.90f);
		params.width = 	screenSize.getWidth();
		params.height = screenSize.getHeight();

		getWindow().setAttributes(
				(android.view.WindowManager.LayoutParams) params);

		ButtonColor apply = (ButtonColor) findViewById(R.id.btnApplyChanges);
		apply.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_save));

		ButtonColor cancel = (ButtonColor) findViewById(R.id.btnDiscardChanges);
		cancel.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_close));

		ButtonColor takePhotos = (ButtonColor) findViewById(R.id.btnDocumentPhoto);
		takePhotos.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_camera));
		takePhotos.setVisibility(_appConfig.getWorkingArea().CurrentDeposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE) ? VISIBLE : GONE);

		takePhotos.setOnClickListener( (View v)-> {
			this.showAttachmentsDialog();
		});

		try {
			this.fillFields();
		} catch (Exception e) {
			throw new RuntimeException(e);
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

		deposito.ClienteInfo.setClienteInfoByCliente(deposito.Cliente);

	    ((EditText) findViewById(R.id.lblCCCCliente)).setText(deposito.ClienteInfo.CCC);
	    ((EditText) findViewById(R.id.lblRepresentanteCliente)).setText(deposito.ClienteInfo.Representante);
	    ((EditText) findViewById(R.id.lblDniRepresentanteCliente)).setText(deposito.ClienteInfo.DniRepresentante);
	    
	}
	
	public void OnClose(View v) {
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
				!_appConfig.getWorkingArea().CurrentDeposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE);
		
		_appConfig.getWorkingArea().CurrentDeposito.CCCUpdated = this.isUpdatedCCC();

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
		
		_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.update();
		
		Deposito deposito = _appConfig.getWorkingArea().CurrentDeposito;
		if (deposito.DatosFiscalesUpdated) {
			// Generamos la incidencia de cambio de datos fiscales
			
			boolean result = _appConfig.getMessageBox().ShowWithResult("Datos del cliente",
					"Se va a proceder a enviar los cambios de los datos fiscales del cliente. Desea Continuar?", this,
					MessageBoxType.Information);
			if (!result)
				return;

			createIncidenciaDatosFiscales(deposito, _appConfig);
		}

		if (deposito.CCCUpdated) {
			// Generamos la incidencia de cambio de datos de cuenta
			// corriente
			
			boolean result = _appConfig.getMessageBox().ShowWithResult("Datos del cliente",
					"Se va a proceder a enviar los cambios de la cuenta corriente del cliente. Desea Continuar?", this,
					MessageBoxType.Information);
			if (!result)
				return;

			createIncidenciaCCC(deposito, _appConfig);

		}
		finish();
	}

	public static void createIncidenciaCCC(Deposito deposito, AppConfig appConfig) {
		String text = "Datos de la cuenta corriente del cliente: " + ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE + "CODIGO CLIENTE: " + deposito.CodigoCliente
				+ ConstantsTypes.NEW_LINE + "NOMBRE DEL CLIENTE: " + deposito.Nombre + ConstantsTypes.NEW_LINE
				+ "NUM CUENTA CORRIENTE: " + deposito.ClienteInfo.CCC + ConstantsTypes.NEW_LINE
				+ "REPRESENTANTE: " + deposito.ClienteInfo.Representante + ConstantsTypes.NEW_LINE
				+ "DNI REPRESENTANTE: " + deposito.ClienteInfo.DniRepresentante + ConstantsTypes.NEW_LINE;

		Incidencia incidencia = new Incidencia(appConfig.getUser().User, new Date(),
				IncidenciaType.CuentaCorriente, text);
        try {
            incidencia.create(new IncidentPdfCreator(appConfig));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
	public static void createIncidenciaDatosFiscales(Deposito deposito, AppConfig appConfig) {
		String text = "Datos antiguos: " + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
				+ "NÚM. DEPOSITO DIMONI: " + deposito.NumDoc + ConstantsTypes.NEW_LINE
				+ "NÚM. DEPOSITO TABLET (RefExt): " + deposito.IdDeposito + ConstantsTypes.NEW_LINE
				+ "NIF/CIF: " + deposito.NIFPrevious + ConstantsTypes.NEW_LINE + "NOMBRE: "
				+ deposito.NombrePrevious + ConstantsTypes.NEW_LINE + "RAZÓN: " + deposito.RazonPrevious
				+ ConstantsTypes.NEW_LINE + "DIRECCION: " + deposito.DireccionPrevious
				+ ConstantsTypes.NEW_LINE + "POBLACION: " + deposito.PoblacionPrevious
				+ ConstantsTypes.NEW_LINE + "CODIGO POSTAL: " + deposito.CodigoPostalPrevious
				+ ConstantsTypes.NEW_LINE + "PROVINCIA: " + deposito.ProvinciaPrevious
				+ ConstantsTypes.NEW_LINE + "TELEFONO 1: " + deposito.Telefono1Previous
				+ ConstantsTypes.NEW_LINE + "TELEFONO 2: " + deposito.Telefono2Previous
				+ ConstantsTypes.NEW_LINE + "FAX: " + deposito.FaxPrevious + ConstantsTypes.NEW_LINE + "MAIL: "
				+ deposito.MailPrevious + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE + "Han sido modificados por: " + ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE + "NIF/CIF: " + deposito.NIF + ConstantsTypes.NEW_LINE + "NOMBRE: "
				+ deposito.Nombre + ConstantsTypes.NEW_LINE + "RAZÓN: " + deposito.Razon
				+ ConstantsTypes.NEW_LINE + "DIRECCION: " + deposito.Direccion1 + ConstantsTypes.NEW_LINE
				+ "POBLACION: " + deposito.Poblacion + ConstantsTypes.NEW_LINE + "CODIGO POSTAL: "
				+ deposito.CodigoPostal + ConstantsTypes.NEW_LINE + "PROVINCIA: " + deposito.Provincia
				+ ConstantsTypes.NEW_LINE + "TELEFONO 1: " + deposito.Telefono1 + ConstantsTypes.NEW_LINE
				+ "TELEFONO 2: " + deposito.Telefono2 + ConstantsTypes.NEW_LINE + "FAX: " + deposito.Fax
				+ ConstantsTypes.NEW_LINE + "MAIL: " + deposito.Mail;

		Incidencia incidencia = new Incidencia(appConfig.getUser().User, new Date(),
				IncidenciaType.DatosFiscales, text);
        try {
            incidencia.create(new IncidentPdfCreator(appConfig));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

		if (!_appConfig.getWorkingArea().CurrentDeposito.Mail.trim().equals(((EditText) findViewById(R.id.lblMailCliente))
				.getText().toString().trim()))
			return true;
		
		return false;
	}
	
	private boolean isUpdatedCCC()
	{
		if (_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo == null)
			_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo = Factory.build(ClienteInfo.class, _appConfig);

		if (!_appConfig.getWorkingArea().CurrentDeposito.ClienteInfo.CCC.trim().equals(((EditText) findViewById(R.id.lblCCCCliente))
				.getText().toString().trim()))
				return true;
		return false;
	}

	private void showAttachmentsDialog() {
		DepositManagerExtension.Dialogs.StartAttachmentsDialog(this);
	}
}


