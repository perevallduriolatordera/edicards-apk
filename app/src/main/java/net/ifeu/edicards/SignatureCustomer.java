package net.ifeu.edicards;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.library.Signature.SignatureView;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.FontSizeManager;

import android.app.Activity;
import android.app.ActionBar.LayoutParams;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        params.width  = 1000;
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

		// PASO 1: Validar que el usuario dibujó algo
		if (!signature.isSignatureFilled()) {
			_app.getMessageBox().Show("Firma Cliente", "Por favor, dibuja tu firma antes de continuar", this, MessageBoxType.Warning);
			return;
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(() -> {
			try {
				// PASO 2: Limpiar firmas antiguas antes de intentar guardar
				deleteOldSignatures(1);

				// PASO 3: Guardar las firmas (PNG y BMP)
				signature.save(1, _app.getWorkingArea().CurrentHistorico.GUID);
				signature.saveBitmap1Color(1, _app.getWorkingArea().CurrentHistorico.GUID);

				// PASO 4: Validar que se guardó al menos uno de los archivos
				if (validateSignatureFiles(1)) {
					_isSaved = true;
				} else {
					_isSaved = false;
				}
			} catch (Exception e) {
				_isSaved = false;
				e.printStackTrace();
			}
		});

		// Esperar a que terminen los guardados de la firma antes de continuar
		try {
			executor.shutdown();
			// Esperar máximo 10 segundos a que terminen
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				// Si no termina en 10 segundos, forzar parada
				executor.shutdownNow();
				_isSaved = false;
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
			_isSaved = false;
		}

		// PASO 5: Mostrar resultado y manejar error
		if (_isSaved) {
			_app.getMessageBox().Show("Firma Cliente", "Firma guardada correctamente", this, MessageBoxType.Information);
			finish();
		} else {
			boolean retry = _app.getMessageBox().ShowWithResult("Firma Cliente",
					"Error al guardar la firma. ¿Deseas intentarlo de nuevo?",
					this, MessageBoxType.Error);
			if (retry) {
				// Usuario quiere reintentar - limpiar el canvas y permitir nueva firma
				signature.clear();  // Limpiar el dibujo anterior
				// Informar al usuario que puede volver a dibujar
				_app.getMessageBox().Show("Firma Cliente",
					"Canvas limpiado. Por favor, vuelve a dibujar tu firma y pulsa el botón de nuevo",
					this, MessageBoxType.Information);
			} else {
				// Usuario rechaza reintentar
				finish();
			}
		}
	}

	/**
	 * Elimina firmas antiguas del mismo cliente para evitar conflictos
	 */
	private void deleteOldSignatures(int tipo) {
		try {
			String path = Environment.getExternalStorageDirectory().toString() + "/"
					+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS + "/";
			String prefix = (tipo == 1) ? "C_" : "V_";
			String guid = _app.getWorkingArea().CurrentHistorico.GUID;

			// Eliminar PNG anterior
			File pngFile = new File(path + prefix + guid + ".png");
			if (pngFile.exists()) {
				pngFile.delete();
			}

			// Eliminar BMP anterior
			File bmpFile = new File(path + prefix + "1_" + guid + ".bmp");
			if (bmpFile.exists()) {
				bmpFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Valida que se guardó al menos UN archivo de firma correctamente
	 * (PNG o BMP). Esto es suficiente porque en impresión usamos fallback.
	 */
	private boolean validateSignatureFiles(int tipo) {
		try {
			String path = Environment.getExternalStorageDirectory().toString() + "/"
					+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS + "/";
			String prefix = (tipo == 1) ? "C_" : "V_";
			String guid = _app.getWorkingArea().CurrentHistorico.GUID;

			// Verificar que existe el PNG y no está vacío
			File pngFile = new File(path + prefix + guid + ".png");
			boolean pngOk = pngFile.exists() && pngFile.length() > 0;

			// Verificar que existe el BMP y no está vacío
			File bmpFile = new File(path + prefix + "1_" + guid + ".bmp");
			boolean bmpOk = bmpFile.exists() && bmpFile.length() > 0;

			// Aceptar si AL MENOS UNO está guardado correctamente
			// (porque en impresión usamos fallback: PNG → BMP o BMP → PNG)
			return pngOk || bmpOk;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
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
