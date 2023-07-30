package net.ifeu.edicards;

import java.util.ArrayList;
import java.util.List;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Application.WorkingArea;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Cliente;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class CustomerSearchListDialog extends ListActivity {

	List<String> _customers = new ArrayList<>();
	AppConfig _appConfig;
	String _customerSelected = Constants.EMPTY_STRING;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_customer_search_list_dialog);

		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = 850;
		getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

		// Inicialitzem l'objecte AppConfig

		_appConfig = (AppConfig) this.getApplicationContext();

		// Obtenim les dades passades des de l'activitat

		Bundle bundle = getIntent().getExtras();
		String text = bundle != null ? bundle.getString("Text") : null;
		int filter = bundle != null ? bundle.getInt("Filter") : 0;
		boolean onlyStartsWith = bundle != null ? bundle.getBoolean("OnlyStartsWith") : false;

		Cliente cliente = new Cliente();
		cliente.InitializePersistance(_appConfig, this.getApplicationContext());

		try {
			_customers = cliente.getClientesByFilter(text, filter, onlyStartsWith);

			if (_customers.size() == 0)
				finish();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		ListView lstView = getListView();

		lstView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		lstView.setTextFilterEnabled(true);

		setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_gallery_item, _customers));

	}

	public void onListItemClick(ListView parent, View v, int position, long id) {

		Button button = (Button) findViewById(R.id.btnAccept);
		button.setText("Seleccionar Cliente " + getClienteName(_customers.get(position)));
	}

	private String getClienteCode(String value) {
		int position = value.indexOf(" --- ");

		if (position >= 0)
			return value.substring(0, position);
		else
			return Constants.EMPTY_STRING;
	}

	private String getClienteName(String value) {
		int position = value.indexOf(" --- ");

		if (position >= 0) {
			String newValue = value.substring(position + 5);
			int position2 = newValue.indexOf(" --- ");
			return newValue.substring(0, position2);
		} else
			return Constants.EMPTY_STRING;
	}

	public void onClick(View view) throws Exception {

		ListView lstView = getListView();

		if (lstView != null) {
			for (int i = 0; i < lstView.getCount(); i++) {
				if (lstView.isItemChecked(i)) {
					_customerSelected = getClienteCode((String) lstView.getItemAtPosition(i));
					break;
				}
			}

			// Asignamos el cliente a la workingArea

			if (!_customerSelected.equals(Constants.EMPTY_STRING)) {
				AppConfig app = (AppConfig) this.getApplicationContext();
				WorkingArea workingArea = app.getWorkingArea();

				Cliente cliente = new Cliente();
				cliente.InitializePersistance(_appConfig, this.getApplicationContext().getApplicationContext());

				if (cliente.setClienteByCodigo(_customerSelected)) {
					workingArea.CurrentCliente = cliente;
				}

				finish();
			}

		}

	}

}