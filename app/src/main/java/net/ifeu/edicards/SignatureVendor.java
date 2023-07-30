package net.ifeu.edicards;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.library.Signature.SignatureView;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class SignatureVendor extends Activity {

	private Bundle _bundle;
	private SignatureView _signature;
	AppConfig _app;
	Boolean _isSaved = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signature_vendor);
		
		setFinishOnTouchOutside (false);

		_bundle = savedInstanceState;
		_app = (AppConfig) this.getApplicationContext();
	}

	public void OnClick(View v) {
		// final SignatureView signature = (SignatureView)
		// this.findViewById(R.id.signatureView);
		_signature = (SignatureView) this.findViewById(R.id.signatureView);
		_signature.save(2, _app.getWorkingArea().CurrentHistorico.GUID);
		_signature.saveBitmap1Color(2, _app.getWorkingArea().CurrentHistorico.GUID);
		_isSaved = true;

		finish();

	}

	public void OnDelete(View v) {
		this.onCreate(_bundle);
	}

	@Override
	public void onDestroy() {
		
		super.onDestroy();

	}
}
