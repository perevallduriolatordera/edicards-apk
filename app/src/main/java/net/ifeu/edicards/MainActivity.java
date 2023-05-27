package net.ifeu.edicards;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.Excel.LogBookCreator;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.library.Devices.BlueTooth;
import net.ifeu.library.Devices.Wifi;
import net.ifeu.library.Devices._3G;
import net.ifeu.library.LogBook.LogBook;
import net.ifeu.library.Utils.Inactivate;
import net.ifeu.library.Utils.MessageBoxType;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import java.io.PrintWriter;
import java.io.StringWriter;
//import com.parse.ParseAnalytics;

public class MainActivity extends Activity {

	private static final String tag = "MainActivity";
	//private int debug = 0;

	private AppConfig _appConfig;
	private ServiceWorker _serviceWorker;

	private IntentFilter _intentFilter;


	@Override
	public void onBackPressed() {
		// super.onBackPressed();
		// Not calling **super**, disables back button in current screen.
	}

	@Override
	public void onCreate(Bundle savedInstanceState)  {
		
		try {
			
			 super.onCreate(savedInstanceState);
			
			 Log.i(tag, "Inicialitzem l'objecte appConfig després d'haver carregat la vista");
	
			// Inicialitzem l'objecte AppConfig
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
	
			// Obtenim les dades de l'usuari
			getUserData();

			//desactivar la app en el cas de que sigui necessari
			if (Inactivate.inactivateIfNecessary()) {
				this.finish();
				System.exit(0);
			}

			if (_appConfig.getUser().isEmpty()) {
				Log.i(tag, "Cridem a l'activitat StartNewUser");
				StartNewUser();
			} else {
				this.setContentView(R.layout.activity_main);
			}

			Log.i(tag, "Fi activitat");

		}
		catch (Exception ex) {
			StringWriter errors = new StringWriter();
        	ex.printStackTrace(new PrintWriter(errors));
        	Log.e("Error al iniciar la app de edicards", errors.toString());

			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(ex),
					this, MessageBoxType.Error);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		try {
			getMenuInflater().inflate(R.menu.activity_main, menu);
			return true;
		} catch (Exception ex) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, ex);
			return true;
		}
	}

	private void ShowAppVersion() throws NameNotFoundException {
		
		try {
			// Mostrem la versió de l'aplicació
	
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
			int version = pInfo.versionCode;
			String versionName = pInfo.versionName;
	
			_appConfig.getWorkingArea().UpgradeDataPost = _appConfig.getMessageBox().ShowWithResult(
					"Aplicación de gestión comercial edicards",
					"Aplicación de gestión comercial Edicards " + Constants.NEW_LINE + "Versión: " + String.valueOf(version)
							+ Constants.NEW_LINE + "Revisión: " + versionName + Constants.NEW_LINE + Constants.NEW_LINE
							+ "Indique la versión  y la revisión de la aplicación en caso de requerir asistencia técnica"
							+ Constants.NEW_LINE + Constants.NEW_LINE
							+ "Desea sincronizar los depósitos?. Esta acción puede tardar unos minutos"
							+ Constants.NEW_LINE + Constants.NEW_LINE
							+ "Si elige sí, se borrarán los depósitos y se crearán de nuevo."
							+ Constants.NEW_LINE + Constants.NEW_LINE 
			
							+ "WIFI Activada: " + (_appConfig.getConnectivity().WIFI ? "Si" :"No")  + Constants.NEW_LINE
							+ "Datos móviles Activados: " + (_appConfig.getConnectivity().DataMobile ? "Si" :"No")  + Constants.NEW_LINE
							+ "Bluetooth Activado: " + (_appConfig.getConnectivity().Bluetooth ? "Si" :"No")  + Constants.NEW_LINE,
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
					Log.i("MainActivity", "Before upgrade");
					try {
						getData();	
					} catch (Exception e) {
					}
	
					try {
						sendData();
					} catch (Exception e) {
					}
					
					that._appConfig.getWorkingArea().Monitor = that._serviceWorker.Monitor();
	
					progressDialog.dismiss();
	
					Intent intent = new Intent(MainActivity.this, MainMenu.class);
	
					Log.i(tag, "Cridem a MainMenuFragments");
					startActivity(intent);		
	
				}
	
			}.start();
		} catch (Exception ex) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, ex);

		}

	}
	
	private void getData() throws Exception {
		
		final Context context = _appConfig;
		
		try {

			Log.i("MainActivity", "Before getting");
			this._serviceWorker.RunImport(context, false);

		} catch (Exception e) {

			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

	}

	private void sendData() throws Exception {
		// Envíamos los datos pendientes

		final Context context = _appConfig;

		try {

			Log.i("MainActivity", "Before sending");
			this._serviceWorker.RunExport(context);

		} catch (Exception e) {

			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

	}

	// Obtenim el resultat de l'activitat StartNewUser

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		try {
			Log.i(tag, "onActivityResult and resultCode = " + resultCode);
	
			super.onActivityResult(requestCode, resultCode, data);
	
			if (resultCode == 0) {
				System.exit(0);
			} else if (resultCode == 1) {
	
				if (!(_appConfig.getUser().isEmpty())) {
					Log.i(tag, "Guardo les dades");
					setUserData();
	
					StartMainMenu();
				} 
			} else if (resultCode == 2) {
				StartMainMenu();
			}
		} catch (Exception ex) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, ex);
		}
	}

	// Guardem les dades de preferencia de l'usuari

	private void setUserData() {
		
		try {
			SharedPreferences preferences = getSharedPreferences("net.ifeu.edicards_preferences",
					android.content.Context.MODE_APPEND);
			SharedPreferences.Editor editor = preferences.edit();
	
			editor.putString("User", _appConfig.getUser().User);
			Log.i(tag, "Escribimos Usuario: " + _appConfig.getUser().User);
			editor.putString("Password", _appConfig.getUser().Password);
			editor.putString("SerialInvoiceA", _appConfig.getUser().SerialInvoiceA);
			editor.putString("SerialInvoiceB", _appConfig.getUser().SerialInvoiceB);
			editor.putString("Company", _appConfig.getUser().Company);
			editor.putString("Name", _appConfig.getUser().Name);
			editor.putString("SerieA", _appConfig.getUser().InitSerieA);
			editor.putString("SerieB", _appConfig.getUser().InitSerieB);
	
			editor.commit();
		
		} catch (Exception ex) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, ex);

		}
	}

	// Obtenim les dades de preferencia de l'usuari

	private void getUserData() {
		
		try {
			SharedPreferences preferences = getSharedPreferences("net.ifeu.edicards_preferences",
					android.content.Context.MODE_PRIVATE);
	
			_appConfig.getUser().User = preferences.getString("User", Constants.EMPTY_STRING);
			Log.i(tag, "Usuario: " + _appConfig.getUser().User);
			_appConfig.getUser().Password = preferences.getString("Password", Constants.EMPTY_STRING);
			_appConfig.getUser().SerialInvoiceA = preferences.getString("SerialInvoiceA", Constants.EMPTY_STRING);
			_appConfig.getUser().SerialInvoiceB = preferences.getString("SerialInvoiceB", Constants.EMPTY_STRING);
			_appConfig.getUser().Company = preferences.getString("Company", Constants.EMPTY_STRING);
			_appConfig.getUser().Name = preferences.getString("Name", Constants.EMPTY_STRING);
			_appConfig.getUser().InitSerieA = preferences.getString("SerieA", Constants.EMPTY_STRING);
			_appConfig.getUser().InitSerieB = preferences.getString("SerieB", Constants.EMPTY_STRING);
		
		} catch (Exception ex) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, ex);

		}
	}

	public void OnAcceptClick(View view) throws NameNotFoundException {
		String password = ((EditText) findViewById(R.id.txtPassword)).getText().toString();
		String user = ((EditText) findViewById(R.id.txtUser)).getText().toString();

		if (!user.equals(_appConfig.getUser().User))
			_appConfig.getMessageBox().Show("Error de validación de usuario", "El usuario especificado no existe",
					view.getContext(), MessageBoxType.Error);
		else if (!password.equals(_appConfig.getUser().Password))
			_appConfig.getMessageBox().Show("Error de validación de usuario",
					"La contraseña especificada no es correcta", view.getContext(), MessageBoxType.Error);
		else {
			this.ShowAppVersion();
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

}
