package net.ifeu.edicards;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import android.app.Application;
import android.content.SharedPreferences;

import com.androidnetworking.AndroidNetworking;

import net.ifeu.edicards.Cache.CacheData;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.User;
import net.ifeu.edicards.DatabaseOperations.DatabaseOperations;
import net.ifeu.library.Connectivity.Connectivity;
import net.ifeu.library.Errors.ErrorTrace;
import net.ifeu.library.UCE.UCEHandler;
import net.ifeu.library.Utils.MessageBox;
import net.ifeu.library.Utils.MessageBoxType;

public class AppConfig extends Application {
	
	private User _user;
	private MessageBox _messageBox;
	private DatabaseOperations _databaseOperations;
	private WorkingArea _workingArea;
	private ErrorTrace _errorTrace;
	private Hashtable<String, Double> _traspasoAlmacen;
	private CacheData _cache;
	private Connectivity _connectivity;

	//private Recordatorio _recordatorio;
	
	public AppConfig() throws Exception {

		_user = new User();
		_messageBox = new MessageBox();
		_workingArea = new WorkingArea();
		_errorTrace = new ErrorTrace();
		_traspasoAlmacen = new Hashtable<String, Double>();
		_cache = new CacheData(this, this);
		_connectivity = new Connectivity();

		try {
			_databaseOperations = new DatabaseOperations();
			
		} catch (Exception e) {
			_messageBox.Show("Error en base de datos",
					"Se ha producido el siguiente error: "
							+ e.getMessage().toString(), this,
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

	public ErrorTrace getErrorTrace() {
		return _errorTrace;
	}
	
	public CacheData getCache() {
		return _cache;
	}
	
	public Connectivity getConnectivity() {
		return _connectivity;
	}

	public void setMessageBox(MessageBox messageBox) {
		_messageBox = messageBox;
	}

	public DatabaseOperations getDatabaseOperations() {
		return _databaseOperations;
	}

	public void setDatabaseOperations(DatabaseOperations databaseOperations) {
		_databaseOperations = databaseOperations;
	}

	public void setWorkingArea(WorkingArea workingArea) {
		_workingArea = workingArea;
	}

	public void setErrorTrace(ErrorTrace errorTrace) {
		_errorTrace = errorTrace;
	}

	public Hashtable<String, Double> getTraspasoAlmacen() {
		return _traspasoAlmacen;
	}
	

	@Override
	public void onCreate() {
		super.onCreate();
		
		//Initialize UCE Handler library
        new UCEHandler.Builder(getApplicationContext())
                .setTrackActivitiesEnabled(true)
                .addCommaSeparatedEmailAddresses("valldu@hotmail.com")
                .build();

		AndroidNetworking.initialize(getApplicationContext());

	}
	
	private void getUserData() {
		
		try {
			SharedPreferences preferences = getSharedPreferences("net.ifeu.edicards_preferences",
					android.content.Context.MODE_PRIVATE);
	
			User user = new User();
			user.User = preferences.getString("User", Constants.EMPTY_STRING);
			user.Password = preferences.getString("Password", Constants.EMPTY_STRING);
			user.SerialInvoiceA = preferences.getString("SerialInvoiceA", Constants.EMPTY_STRING);
			user.SerialInvoiceB = preferences.getString("SerialInvoiceB", Constants.EMPTY_STRING);
			user.Company = preferences.getString("Company", Constants.EMPTY_STRING);
			user.Name = preferences.getString("Name", Constants.EMPTY_STRING);
			user.InitSerieA = preferences.getString("SerieA", Constants.EMPTY_STRING);
			user.InitSerieB = preferences.getString("SerieB", Constants.EMPTY_STRING);
			
			this.setUser(user);
		
		} catch (Exception ex) {
			this.getErrorTrace().Send(this.getUser().User, ex);

		}
	}
	
	public String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
}
