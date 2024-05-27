package net.ifeu.edicards;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

public class Login extends Activity {

	AppConfig _appConfig;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

		// Inicialitzem l'objecte d'aplicació
        _appConfig = (AppConfig) this.getApplicationContext();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_new_user, menu);
        return true;
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
				 Intent intent = new Intent();
				 setResult(2,intent);
				 
				 finish();
			}

	}

	public void OnCancelClick(View view) {
		boolean result;

		result = _appConfig.getMessageBox().ShowWithResult("Validación de usuario",
				"Está seguro que quiere cancelar la validación de usuario", view.getContext(), MessageBoxType.Error);

		if (result) {
			 Intent intent = new Intent();
			 setResult(0,intent);
			 
			 finish();
		}
	}

    
}
