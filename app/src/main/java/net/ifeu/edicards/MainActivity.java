package net.ifeu.edicards;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.library.Devices.BlueTooth;
import net.ifeu.library.Devices.Wifi;
import net.ifeu.library.Devices._3G;
import net.ifeu.library.Utils.Inactivate;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

public class MainActivity extends Activity {
	private AppConfig _appConfig;
	private ServiceWorker _serviceWorker;
	
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
		Manifest.permission.READ_EXTERNAL_STORAGE,
		Manifest.permission.WRITE_EXTERNAL_STORAGE
	};

	@Override
	public void onCreate(Bundle savedInstanceState)  {
		
		try {

			 super.onCreate(savedInstanceState);
			 _appConfig = (AppConfig) this.getApplicationContext();
			 this._serviceWorker = new ServiceWorker();

			// Activamos los dipositivos 
			if (!Wifi.IsEnabled(_appConfig))                                                                                
				Wifi.ActivateWifi(_appConfig);                                                                              
                                                                                                                         
			if (!_3G.IsEnabled(_appConfig))                                                                                 
				_3G.Activate3G(_appConfig, true);  
						
			 // Activem el bluetooth en cas de que sigui necessari
			 
			if (!BlueTooth.IsEnabled()) {
				BlueTooth.ActivateBlueTooth();
			}
			
			_appConfig.getConnectivity().WIFI = Wifi.IsEnabled(_appConfig);
			_appConfig.getConnectivity().DataMobile = _3G.IsEnabled(_appConfig);
			_appConfig.getConnectivity().Bluetooth = BlueTooth.IsEnabled();
	
			// Solicitar permisos de almacenamiento
			checkStoragePermission();
	
			// Obtenim les dades de l'usuari
			getUserData();

			//desactivar la app en el cas de que sigui necessari
			if (Inactivate.inactivateIfNecessary()) {
				this.finish();
				System.exit(0);
			}

			if (_appConfig.getUser().isEmpty()) {
				StartNewUser();
			} else {
				this.setContentView(R.layout.activity_main);
				this.ShowAppVersion();
			}

		}
		catch (Exception ex) {
			//_appConfig.getMessageBox().Show("Error app Edicards", "Se ha producido un error al inicializar la aplicación. Motivo: " + ex.getMessage(), _appConfig, MessageBoxType.Error );
			throw new RuntimeException(ex.getMessage());
		}

	}

	public void ShowAppVersion() {

		PackageInfo pInfo;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
		} catch (PackageManager.NameNotFoundException e) {
			throw new RuntimeException(e);
		}
		int version = pInfo.versionCode;
		String versionName = pInfo.versionName;

		((TextView) this.findViewById(R.id.lblVersion)).setText("Versión : " + version );
		((TextView) this.findViewById(R.id.lblRevision)).setText("Revisión : " + versionName);
		((TextView) this.findViewById(R.id.lblWifi)).setText("Wifi habilitada : " + (_appConfig.getConnectivity().WIFI ? "Si" :"No"));
		((TextView) this.findViewById(R.id.lblDataMobile)).setText("Datos Móviles habilitados : " + (_appConfig.getConnectivity().DataMobile ? "Si" :"No"));
		((TextView) this.findViewById(R.id.lblBlueTooth)).setText("Bluetooth habilitado : " + (_appConfig.getConnectivity().Bluetooth ? "Si" :"No"));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		try {
			getMenuInflater().inflate(R.menu.activity_main, menu);
			return true;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void GoToSync() {
		
		try {
			// Mostrem la versió de l'aplicació
	
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			int version = pInfo.versionCode;
			String versionName = pInfo.versionName;
	
			_appConfig.getWorkingArea().UpgradeDataPost = _appConfig.getMessageBox().ShowWithResult(
					"Aplicación de gestión comercial edicards",
					"Aplicación de gestión comercial Edicards " + ConstantsTypes.NEW_LINE + "Versión: " + version
							+ ConstantsTypes.NEW_LINE + "Revisión: " + versionName + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
							+ "Indique la versión  y la revisión de la aplicación en caso de requerir asistencia técnica"
							+ ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
							+ "Desea sincronizar los depósitos?. Esta acción puede tardar unos minutos"
							+ ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
							+ "Si elige sí, se borrarán los depósitos y se crearán de nuevo."
							+ ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
			
							+ "WIFI Activada: " + (_appConfig.getConnectivity().WIFI ? "Si" :"No")  + ConstantsTypes.NEW_LINE
							+ "Datos móviles Activados: " + (_appConfig.getConnectivity().DataMobile ? "Si" :"No")  + ConstantsTypes.NEW_LINE
							+ "Bluetooth Activado: " + (_appConfig.getConnectivity().Bluetooth ? "Si" :"No")  + ConstantsTypes.NEW_LINE,
					this, MessageBoxType.Information);
			
		} catch (Exception ex) {
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(ex),
					_appConfig, MessageBoxType.Error);	

		}
	}
	
	// Llencem la activitat per donar d'alta un nou usuari.

	private void StartNewUser() {
		int requestCode = 1;
		Intent intent = new Intent(MainActivity.this, NewUser.class);

		startActivityForResult(intent, requestCode);
	}

	// Llencem el menú principal

	private void StartMainMenu() {

		try {
			final ProgressDialog progressDialog;
			progressDialog = ProgressDialog.show(this, "Actualizando Datos de Central", "Cargando...Espere unos instantes",
					true);

			final MainActivity that = this;

			new Thread() {

				@Override
				public void run() {

					try {
						getData();
						sendData();

						that._appConfig.getWorkingArea().Monitor = that._serviceWorker.Monitor();

						if (progressDialog != null && progressDialog.isShowing()) {
							runOnUiThread(() -> {
								progressDialog.dismiss();
								Intent intent = new Intent(MainActivity.this, MainMenu.class);
								startActivity(intent);
							});
						}

					} catch (Exception e) {
						// Manejar el error correctamente o mostrar un mensaje al usuario
						runOnUiThread(() -> {
							if (progressDialog.isShowing()) {
								progressDialog.dismiss();
							}
							Toast.makeText(that, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
						});
					}

				}

			}.start();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}


	private void getData() {
		
		final Context context = _appConfig;

		try {
			this._serviceWorker.RunImport(context, false);
		} catch (Exception e) {
			throw new RuntimeException(e);		}
	}

	private void sendData() {

		final Context context = _appConfig;

		try {
			this._serviceWorker.RunExport(context);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Obtenim el resultat de l'activitat StartNewUser

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		try {
			super.onActivityResult(requestCode, resultCode, data);
	
			if (resultCode == 0) {
				System.exit(0);
			} else if (resultCode == 1) {
	
				if (!(_appConfig.getUser().isEmpty())) {
					setUserData();
					StartMainMenu();
				} 
			} else if (resultCode == 2) {
				StartMainMenu();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// Guardem les dades de preferencia de l'usuari

	private void setUserData() {
		
		try {
			SharedPreferences preferences = getSharedPreferences("net.ifeu.edicards_preferences",
					android.content.Context.MODE_APPEND);
			SharedPreferences.Editor editor = preferences.edit();
	
			editor.putString("User", _appConfig.getUser().User);
			editor.putString("Password", _appConfig.getUser().Password);
			editor.putString("SerialInvoiceA", _appConfig.getUser().SerialInvoiceA);
			editor.putString("SerialInvoiceB", _appConfig.getUser().SerialInvoiceB);
			editor.putString("Company", _appConfig.getUser().Company);
			editor.putString("Name", _appConfig.getUser().Name);
			editor.putString("SerieA", _appConfig.getUser().InitSerieA);
			editor.putString("SerieB", _appConfig.getUser().InitSerieB);
	
			editor.commit();
		
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// Obtenim les dades de preferencia de l'usuari

	private void getUserData() {
		
		try {
			SharedPreferences preferences = getSharedPreferences("net.ifeu.edicards_preferences",
					android.content.Context.MODE_PRIVATE);
	
			_appConfig.getUser().User = preferences.getString("User", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().Password = preferences.getString("Password", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().SerialInvoiceA = preferences.getString("SerialInvoiceA", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().SerialInvoiceB = preferences.getString("SerialInvoiceB", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().Company = preferences.getString("Company", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().Name = preferences.getString("Name", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().InitSerieA = preferences.getString("SerieA", ConstantsTypes.EMPTY_STRING);
			_appConfig.getUser().InitSerieB = preferences.getString("SerieB", ConstantsTypes.EMPTY_STRING);
		
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void OnAcceptClick(View view) {
		String password = ((EditText) findViewById(R.id.txtPassword)).getText().toString();
		String user = ((EditText) findViewById(R.id.txtUser)).getText().toString();

		if (!user.equals(_appConfig.getUser().User))
			_appConfig.getMessageBox().Show("Error de validación de usuario", "El usuario especificado no existe",
					view.getContext(), MessageBoxType.Error);
		else if (!password.equals(_appConfig.getUser().Password))
			_appConfig.getMessageBox().Show("Error de validación de usuario",
					"La contraseña especificada no es correcta", view.getContext(), MessageBoxType.Error);
		else {
			this.GoToSync();
			StartMainMenu();
		}

	}

	public void OnCancelClick(View view) {
		boolean result;
		result = _appConfig.getMessageBox().ShowWithResult("Validación de usuario",
				"Está seguro que quiere cancelar la validación de usuario", view.getContext(), MessageBoxType.Error);

		if (result)
			System.exit(0);
	}
	
	private void checkStoragePermission() {
		int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		
		if (permission != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(
				this,
				PERMISSIONS_STORAGE,
				REQUEST_EXTERNAL_STORAGE
			);
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		
		switch (requestCode) {
			case REQUEST_EXTERNAL_STORAGE: {
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					// Permiso otorgado
					Toast.makeText(this, "Permisos de almacenamiento otorgados", Toast.LENGTH_SHORT).show();
				} else {
					// Permiso denegado
					Toast.makeText(this, "Se necesitan permisos de almacenamiento para algunas funciones de la app", Toast.LENGTH_LONG).show();
				}
				return;
			}
		}
	}
}
