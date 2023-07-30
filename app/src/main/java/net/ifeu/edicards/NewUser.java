package net.ifeu.edicards;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Support.ValidationResult;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import android.app.Activity;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class NewUser extends Activity {
	
	AppConfig _appConfig;
	
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user);
        
        // Inicialitzem l'objecte d'aplicació
        _appConfig = (AppConfig) this.getApplicationContext();

		((EditText) findViewById(R.id.txtSerialInoviceA)).setText(Constants.PREFILL_INVOICE_A);
		((EditText) findViewById(R.id.txtSerialInoviceB)).setText(Constants.PREFILL_INVOICE_B);
		((EditText) findViewById(R.id.txtCompany)).setText(Constants.PREFILL_COMPANY);
        
        android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = LayoutParams.FILL_PARENT;
        params.width  = 500;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
      
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_new_user, menu);
        return true;
    }
    
    public void OnAcceptClick(View view)
    {
    	String password1 = ((EditText) findViewById(R.id.txtPassword)).getText().toString();
    	String password2 = ((EditText) findViewById(R.id.txtPasswordRepeat)).getText().toString();
    	
    	if (!password1.equals(password2))
    	{
    		_appConfig.getMessageBox().Show("Error de validación", "La contraseña no es idéntica", view.getContext(),MessageBoxType.Error);
    	}
    	else
    	{
    		_appConfig.getUser().User = ((EditText) findViewById(R.id.txtUser)).getText().toString();
    		_appConfig.getUser().Password = password1;
    		_appConfig.getUser().SerialInvoiceA = ((EditText) findViewById(R.id.txtSerialInoviceA)).getText().toString();
    		_appConfig.getUser().SerialInvoiceB = ((EditText) findViewById(R.id.txtSerialInoviceB)).getText().toString();
    		_appConfig.getUser().Company = ((EditText) findViewById(R.id.txtCompany)).getText().toString();
    		_appConfig.getUser().Name = ((EditText) findViewById(R.id.txtName)).getText().toString();
    		_appConfig.getUser().InitSerieA = ((EditText) findViewById(R.id.txtSerieA)).getText().toString();
    		_appConfig.getUser().InitSerieB = ((EditText) findViewById(R.id.txtSerieB)).getText().toString();
    		
    		ValidationResult validation = _appConfig.getUser().Validate();
    		
    		if (validation.Result) {
    			 Intent intent = new Intent();
    			 setResult(1,intent);
    			 
    			 finish();
    		} 
    		else {
    			 _appConfig.getMessageBox().Show("Error de Validación", validation.MessageResult, view.getContext(),MessageBoxType.Error);
    			
    		}
    	}
    }
    
    public void OnCancelClick(View view)
    {
    	boolean result;
    	result = _appConfig.getMessageBox().ShowWithResult("Edición de usuario", "Está seguro que quiere cancelar la edición del usuario", view.getContext(),MessageBoxType.Error);
    	
    	if (result){
    		 Intent intent = new Intent();
			 setResult(0,intent);
			 
			 finish();
    	}
    	
    }
        
}
