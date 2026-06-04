package net.ifeu.edicards.Application;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;

import net.ifeu.edicards.Cache.CacheData;
import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.User;
import net.ifeu.edicards.DatabaseOperations.DatabaseOperations;
import net.ifeu.library.Connectivity.Connectivity;
import net.ifeu.library.Debugger.Debugger;
import net.ifeu.library.Mediator.MediatorFragments;
import net.ifeu.library.UCE.UCEHandler;
import net.ifeu.library.Utils.MessageBox.MessageBox;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

public class AppConfig extends Application {
	
	private User _user;
	private final MessageBox _messageBox;
	private DatabaseOperations _databaseOperations;
	private final WorkingArea _workingArea;
	private final Hashtable<String, Double> _traspasoAlmacen;
	private final CacheData _cache;
	private final Connectivity _connectivity;

	private final MediatorFragments _mediator;

	public AppConfig() throws Exception {

		_user = new User();
		_messageBox = new MessageBox();
		_workingArea = new WorkingArea();
		_traspasoAlmacen = new Hashtable<>();
		_cache = new CacheData(this, this);
		_connectivity = new Connectivity();
		_mediator = new MediatorFragments();

		try {
			_databaseOperations = new DatabaseOperations();
			
		} catch (Exception e) {
			_messageBox.Show("Error en base de datos",
					"Se ha producido el siguiente error: "
							+ e.getMessage(), this,
					MessageBoxType.Error);
		}
	}
	
	public User getUser() {
		
		if (_user == null || _user.User == null)
			this.getUserData();
		
		return _user;
	}

	public void setUser(User user) {
		_user = user;
	}

	public MessageBox getMessageBox() {
		return _messageBox;
	}

	public WorkingArea getWorkingArea() {
		return _workingArea;
	}

	public CacheData getCache() {
		return _cache;
	}
	
	public Connectivity getConnectivity() {
		return _connectivity;
	}

	public DatabaseOperations getDatabaseOperations() {
		return _databaseOperations;
	}

	public Hashtable<String, Double> getTraspasoAlmacen() {
		return _traspasoAlmacen;
	}

	public MediatorFragments getMediator() {
		return _mediator;
	}

	@Override
	public void onCreate() {

		try {
			super.onCreate();

			//Initialize UCE Handler library
			new UCEHandler.Builder(getApplicationContext())
					.setTrackActivitiesEnabled(true)
					.addCommaSeparatedEmailAddresses("valldu@hotmail.com")
					.build();

			AndroidNetworking.initialize(getApplicationContext());

			// Inicializar ORS_API_KEY desde BuildConfig
			ConstantsEndpoints.ORS_API_KEY = BuildConfig.ORS_API_KEY;
			if (ConstantsEndpoints.ORS_API_KEY != null && !ConstantsEndpoints.ORS_API_KEY.isEmpty()) {
				Log.d("AppConfig", "ORS API Key inicializada correctamente");
			} else {
				Log.w("AppConfig", "Advertencia: ORS_API_KEY no está configurada en local.properties");
			}

		} catch (Exception ex) {
			Log.e("App Edicards", "Error: " + ex.getMessage());
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);

				Debugger.Crash(this, "", "Error: " + sw, null);
			} catch (PackageManager.NameNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

	}
	
	private void getUserData() {
		
		try {
			SharedPreferences preferences = getSharedPreferences("net.ifeu.edicards_preferences",
					android.content.Context.MODE_PRIVATE);
	
			User user = new User();
			user.User = preferences.getString("User", ConstantsTypes.EMPTY_STRING);
			user.Password = preferences.getString("Password", ConstantsTypes.EMPTY_STRING);
			user.SerialInvoiceA = preferences.getString("SerialInvoiceA", ConstantsTypes.EMPTY_STRING);
			user.SerialInvoiceB = preferences.getString("SerialInvoiceB", ConstantsTypes.EMPTY_STRING);
			user.Company = preferences.getString("Company", ConstantsTypes.EMPTY_STRING);
			user.Name = preferences.getString("Name", ConstantsTypes.EMPTY_STRING);
			user.InitSerieA = preferences.getString("SerieA", ConstantsTypes.EMPTY_STRING);
			user.InitSerieB = preferences.getString("SerieB", ConstantsTypes.EMPTY_STRING);
			
			this.setUser(user);
		
		} catch (Exception ex) {
			throw new RuntimeException(ex);

		}
	}
	
	public String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
}
