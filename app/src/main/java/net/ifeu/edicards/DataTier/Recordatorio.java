/*package net.ifeu.edicards.DataTier;

import java.util.List;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import android.content.SharedPreferences;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

public class Recordatorio {

	private final static String PARSE_OBJECT = "Message";
	private final static String PARSE_TIPO_MENSAJE = "TipoMensaje";
	private final static String PARSE_TIPO_MENSAJE_VALUE = "Recordatorio";
	private final static String PARSE_TEXTO = "Texto";

	private String _value;
	private AppConfig _appConfig;

	public Recordatorio (AppConfig app)
	{
		_appConfig = app;
		this.getLast();
	}
	
	public String getValue()
	{
		return _value;
	}
	
	public void save(final String text) {

		// Create the object.
		final ParseObject message = new ParseObject(PARSE_OBJECT);

		message.put(PARSE_TIPO_MENSAJE, PARSE_TIPO_MENSAJE_VALUE);
		message.put(PARSE_TEXTO, text);

		message.saveInBackground(new SaveCallback() {
			public void done(ParseException e) {

//				if (e == null)
//					
//					_appConfig
//					.getMessageBox()
//					.ShowWithResult(
//							"Envío de recordatorio",
//							"El recordatorio se ha enviado correctamente",
//							_appConfig,
//							MessageBoxType.Information);
//				
//				else
//					
//					_appConfig
//					.getMessageBox()
//					.ShowWithResult(
//							"Envío de recordatorio",
//							"El recordatorio NO se ha podido enviar. Inténtelo mas tarde",
//							_appConfig,
//							MessageBoxType.Information);
			}
		});
	}

	public void getLast() {

		_value = Constants.EMPTY_STRING;

		try {
			
			ParseQuery query = new ParseQuery(PARSE_OBJECT);
			query.whereEqualTo(PARSE_TIPO_MENSAJE, PARSE_TIPO_MENSAJE_VALUE);

			query.findInBackground(new FindCallback() {
				public void done(List<ParseObject> recordatorios,
						ParseException e) {
					if (e == null) {
						Log.i("Recordatorio",
								"Retrieved " + recordatorios.size()
										+ " recordatorios");

						if (recordatorios.size() > 0) {
							ParseObject current = null;
							for (ParseObject object : recordatorios) {
								if (current == null)
									current = object;
								else {
									if (object.getCreatedAt().after(
											current.getCreatedAt()))
										current = object;
								}

							}
							_value = (String) current.get(PARSE_TEXTO);

							SharedPreferences preferences = _appConfig.getApplicationContext()
									.getSharedPreferences(
											"net.ifeu.edicards_preferences",
											android.content.Context.MODE_APPEND);

							preferences.edit().remove("UserMessage");
							preferences.edit().putString("UserMessage", _value);
							
						}
					} else {
						Log.i("score", "Error: " + e.getMessage());
					}
				}
			});
			
			if (_value.equals(Constants.EMPTY_STRING)) {
				SharedPreferences preferences = _appConfig.getApplicationContext().getSharedPreferences(
						"net.ifeu.edicards_preferences",
						android.content.Context.MODE_PRIVATE);
				_value = preferences.getString("UserMessage",
						Constants.EMPTY_STRING);
			}
		} catch (Exception e) {
			Log.i("Recordatorio", e.getMessage());
			_value = Constants.EMPTY_STRING;
		}

		Log.i("Recordatorio", "Valor final:" + _value);
	}

}*/
