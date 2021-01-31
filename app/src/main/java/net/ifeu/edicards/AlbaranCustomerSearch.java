package net.ifeu.edicards;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Cliente;

public class AlbaranCustomerSearch extends Activity {

	int _request_code = 1;
	AppConfig _appConfig;
	Cliente _cliente;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_albaran_customer_search);
		
		 //android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
         //params.height = 800;
         //params.width  = 950;
         //getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
	        
		
		final RadioGroup radioGroup = (RadioGroup) findViewById(
				R.id.grpFilterField);
		radioGroup.check(R.id.optNombre);
		

		final Button acceptButton = (Button) findViewById(
				R.id.btnAccept);

		acceptButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {

				RadioGroup radioGroup = (RadioGroup) 
						findViewById(R.id.grpFilterField);
				int radioButtonID = radioGroup.getCheckedRadioButtonId();
				View radioButton = radioGroup.findViewById(radioButtonID);
				final int indexFilter = radioGroup.indexOfChild(radioButton);

				final EditText editText = (EditText) findViewById(
						R.id.txtTextSearch);
				final CheckBox checkBox = (CheckBox) findViewById(
						R.id.chkOnlyStartsWith);
				
				final ProgressDialog progressDialog;
		    	progressDialog = ProgressDialog.show(v.getContext(), "Búsqueda de cliente", "Cargando clientes...Espere unos instantes",true);

		    	
		    	new Thread() {
		    		
		    		@Override
		    		public void run() {
		    			try{
		    				StartCustomerSearchDialog(editText.getText().toString(),
		    						indexFilter, checkBox.isChecked());
		    			
		    			} catch (Exception e) {

		    				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		    			}

		    			progressDialog.dismiss();
	
		    	}

		    	}.start();
		    			
			}
			
		});
		
		final Button newCustomerButton = (Button) findViewById(
				R.id.btnNewCustomer);

		newCustomerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {

				Cliente cliente = new Cliente();

				try {
					cliente.InitializePersistance(_appConfig, v.getContext()
							.getApplicationContext());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
				}

				
				try {
					if (cliente.setClienteByCodigo(Constants.NEW_CUSTOMER_CODE)) 
						_appConfig.getWorkingArea().CurrentCliente = cliente;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
				}
				
				setResult(0);
				finish();
			}
		});
				

		final Button closeButton = (Button) findViewById(
				R.id.btnClose);

		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {

				_appConfig.getWorkingArea().CancelSearchDeposit = true;
				
				setResult(0);
				finish();
			}
		});

	}

	
	@Override
	protected void onResume()
	{
		super.onResume();
		_appConfig = (AppConfig) this.getApplicationContext();
		
		if (_appConfig.getWorkingArea().CurrentCliente != null)
			finish();
	}
	
	private void StartCustomerSearchDialog(String text, int filter,
			boolean onlyStartsWith) {

		Intent intent = new Intent(this,
				CustomerSearchListDialog.class);

		intent.putExtra("Text", text);
		intent.putExtra("Filter", filter);
		intent.putExtra("OnlyStartsWith", onlyStartsWith);

		startActivityForResult(intent,1);
		
	}
	
	@Override
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		   //
		setResult(0);
		finish();
	 }

}
