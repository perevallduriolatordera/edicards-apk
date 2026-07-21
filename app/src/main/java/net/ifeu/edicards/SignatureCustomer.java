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

		TextView lblFormaPago = this.findViewById(R.id.lblFormaDePago);
		lblFormaPago.setText("La forma de pago elegida por el cliente " + _app.getWorkingArea().CurrentCliente.Razon +
				" es: ");
		FontSizeManager.adjustFontSize(lblFormaPago, 12);

		TextView txtFormaPago = this.findViewById(R.id.txtFormaDePago);
		txtFormaPago.setText(_app.getWorkingArea().CurrentDeposito.FormaPago.Descripcion);
		FontSizeManager.adjustFontSize(txtFormaPago, 16);
		_bundle = savedInstanceState;
	}

	public void OnClick(View v) {
		SignatureView signature = this.findViewById(R.id.signatureView);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(() -> {
			signature.save(1, _app.getWorkingArea().CurrentHistorico.GUID);
			signature.saveBitmap1Color(1, _app.getWorkingArea().CurrentHistorico.GUID);
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
