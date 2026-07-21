package net.ifeu.edicards;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsEvents;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.NTV.DepositoNTVDTO;
import net.ifeu.edicards.DataTier.NTV.support.ExcelNTVParser;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.IO.IOUtils;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.ScreenManager;

import java.io.File;

public class AlbaranCustomerSearch extends Activity {

	AppConfig _appConfig;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_albaran_customer_search);
		_appConfig = (AppConfig) this.getApplicationContext();

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = ScreenManager.getScreenSizeByPercentage(getWindowManager(), 0.95f).getWidth();
		getWindow().setAttributes(params);

		final RadioGroup radioGroup = (RadioGroup) findViewById(
				R.id.grpFilterField);
		radioGroup.check(R.id.optNombre);

		final ButtonColor acceptButton = (ButtonColor) findViewById(
				R.id.btnAccept);

		acceptButton.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_search));

		acceptButton.setOnClickListener(v -> {

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
						throw new RuntimeException(e);
					}

					progressDialog.dismiss();

			}

			}.start();

		});
		
		final ButtonColor newCustomerButton = (ButtonColor) findViewById(
				R.id.btnNewCustomer);

		newCustomerButton.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_new_customer));

		newCustomerButton.setOnClickListener(v -> {

			Cliente cliente = Factory.build(Cliente.class, _appConfig);

			try {
				finish();
				if (cliente.setClienteByCodigo(ConstantsTypes.NEW_CUSTOMER_CODE))
					_appConfig.getMediator().notify(ConstantsEvents.EVENT_CUSTOMER_NEW, cliente);
			} catch (Exception e) {
				throw new RuntimeException(e);			}
		});

		final ButtonColor ntvImportButton = (ButtonColor) findViewById(
                R.id.btnNTVImport);
		ntvImportButton.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_import));

		ntvImportButton.setOnClickListener(v-> {

			String file = IOUtils.getMostRecentlyModifiedFile(
					new File("/sdcard/" + ConstantsFolders.FOLDER_NTV_IMPORT));
			DepositoNTVDTO depositoNTVDTO =  ExcelNTVParser.parseExcelFile(file);

			if (depositoNTVDTO == null)
				_appConfig.getMessageBox().Show("Atención",
						"No se han encontrado ningún depósito de NTV a importar",
						this, MessageBoxType.Error);
			else {
				depositoNTVDTO.File = file;
				finish();

				if (depositoNTVDTO != null) {
					_appConfig.getMediator().notify(ConstantsEvents.EVENT_NTV_IMPORT_STARTED, depositoNTVDTO);
				}
			}
		});

		final ButtonColor closeButton = (ButtonColor) findViewById(
				R.id.btnClose);
		closeButton.changeAspect(this, R.color.Black, getResources().getDrawable(R.drawable.ic_close));

		closeButton.setOnClickListener(v -> {
			_appConfig.getMediator().notify(ConstantsEvents.EVENT_CUSTOMER_CANCELLED, null);
			finish();
		});

	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
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

		if (_appConfig.getWorkingArea().CurrentCliente != null) {
			finish();
			_appConfig.getMediator().notify(ConstantsEvents.EVENT_CUSTOMER_SELECTED, _appConfig.getWorkingArea().CurrentCliente);
		} else {
			_appConfig.getMessageBox().Show("Atención",
					"No se han encontrado resultados",
					this, MessageBoxType.Information);
		}
	 }
}
