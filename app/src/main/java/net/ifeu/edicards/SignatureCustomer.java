package net.ifeu.edicards;

import net.ifeu.library.Signature.SignatureView;
import android.app.Activity;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SignatureCustomer extends Activity {

	private Bundle _bundle;
	private SignatureView _signature;
	AppConfig _app;
	Boolean _isSaved = false;;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signature_customer);
		
		setFinishOnTouchOutside (false);
		
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes(); 
        params.height = LayoutParams.FILL_PARENT;
        params.width  = 1000;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		_bundle = savedInstanceState;
		_app = (AppConfig) this.getApplicationContext();
	}

	public void OnClick(View v) {
		// final SignatureView signature = (SignatureView)
		// this.findViewById(R.id.signatureView);
		_signature = (SignatureView) this.findViewById(R.id.signatureView);
		_signature.save(1, _app.getWorkingArea().CurrentHistorico.GUID);
		_signature.saveBitmap1Color(1, _app.getWorkingArea().CurrentHistorico.GUID);
		_isSaved = true;

		finish();

	}
	
	private void StartDepositView() throws Exception {
		Intent intent = new Intent(this, DepositView.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);

	}
	
	private void StartAlbaranView() throws Exception {
		Intent intent = new Intent(this, AlbaranView.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);

	}


	public void OnDepositView(View v) throws Exception {
		this.StartDepositView();
	}
	
	public void OnAlbaranView(View v) throws Exception {
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
