package net.ifeu.edicards;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.library.Signature.SignatureView;
import net.ifeu.library.Utils.Screen.FontSizeManager;
import net.ifeu.library.Utils.Screen.ScreenManager;

import android.app.Activity;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignatureCustomer extends Activity {

	private Bundle _bundle;
	AppConfig _app;
	Boolean _isSaved = false;
	private String _guid; // GUID recibido del Intent

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signature_customer);

		_app = (AppConfig) this.getApplicationContext();

		setFinishOnTouchOutside (false);

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width  = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.95f).getWidth();
        getWindow().setAttributes(params);

		// Recuperar el GUID del Intent o del Bundle si la actividad se recreó
		if (savedInstanceState != null && savedInstanceState.containsKey("GUID")) {
			_guid = savedInstanceState.getString("GUID");
		} else if (getIntent() != null && getIntent().hasExtra("GUID")) {
			_guid = getIntent().getStringExtra("GUID");
		} else if (_app.getWorkingArea().CurrentHistorico != null) {
			_guid = _app.getWorkingArea().CurrentHistorico.GUID;
		}

		TextView lblFormaPago = this.findViewById(R.id.lblFormaDePago);
		lblFormaPago.setText("La forma de pago elegida por el cliente " + _app.getWorkingArea().CurrentCliente.Razon +
				" es: ");
		FontSizeManager.adjustFontSize(lblFormaPago, 12);

		TextView txtFormaPago = this.findViewById(R.id.txtFormaDePago);
		txtFormaPago.setText(_app.getWorkingArea().CurrentDeposito.FormaPago.Descripcion);
		FontSizeManager.adjustFontSize(txtFormaPago, 16);
		_bundle = savedInstanceState;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (_guid != null) {
			outState.putString("GUID", _guid);
		}
	}

	public void OnClick(View v) {

		// Verificar que tenemos el GUID necesario
		if (_guid == null) {
			android.util.Log.e("SignatureCustomer", "GUID es null, no se puede completar la operación");
			android.widget.Toast.makeText(this, "Error: No se pudo obtener el GUID de la operación",
					android.widget.Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		SignatureView signature = this.findViewById(R.id.signatureView);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(() -> {
			signature.save(1, _guid);
			signature.saveBitmap1Color(1, _guid);
			_isSaved = true;
		});
		finish();

	}
	
	private void StartDepositView()  {
		Intent intent = new Intent(this, DepositView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivityForResult(intent, 1);

	}
	
	private void StartAlbaranView()  {
		Intent intent = new Intent(this, AlbaranView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivityForResult(intent, 1);

	}

	public void OnDepositView(View v)  {
		this.StartDepositView();
	}
	
	public void OnAlbaranView(View v) {
		this.StartAlbaranView();
	}
	
	public void OnDelete(View v) {
		this.onCreate(_bundle);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
