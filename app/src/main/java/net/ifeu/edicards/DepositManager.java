package net.ifeu.edicards;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.itextpdf.text.DocumentException;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.TipoIVA;
import net.ifeu.edicards.DataTier.TransferMode;
import net.ifeu.edicards.Pdf.PdfGDPR;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.ComboBox;
import net.ifeu.library.Controls.IComboBoxChangeEvent;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.LogBook.LogBook;
import net.ifeu.library.Mediator.IMediator;
import net.ifeu.library.Utils.MessageBox.AdvancedMessageBox;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DepositManager extends Fragment implements IComboBoxChangeEvent, IMediator {

	private Deposito _deposito;
	private Cliente _cliente;
	private AppConfig _appConfig;
	private boolean _newDeposit = false;
	private boolean _abonoMode = false;

	private ComboBox _comboPago;
	private ComboBox _comboCopias;
	private ComboBox _comboSerie;
	private ComboBox _comboFiliacion;
	private TextBoxColor _textBoxCantidadPagada;
	private CheckBox _checkPagado;

	private TextBoxColor _textBoxColorRequestFocus;
	private TextBoxColor _lastTextBox;
	private LinearLayout _lastSelectedLayout;

	private LinkedList<String> _articles = new LinkedList<>();
	private AdvancedMessageBox _dialogDepositoModalidad;

	private LinearLayout _headerLayout;
	private LinearLayout _headerAbonoLayout;

	private final int TEXT_SIZE = 14;
	private final int TEXT_SIZE_BUTTON = 12;
	private final int BUTTONS_WIDTH = 150;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_deposit_manager, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onStop() {
		super.onStop();System.gc();
		_appConfig.getWorkingArea().CurrentCliente = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		super.onDestroyView();
		System.gc();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (_dialogDepositoModalidad != null)
			_dialogDepositoModalidad.Close();

		System.gc();
	}

	@Override
	public void onResume() {
		super.onResume();

		if (_appConfig == null)
			_appConfig = (AppConfig) this.getActivity().getApplicationContext();

/*		if (!_searched) {

			_appConfig = (AppConfig) this.getActivity().getApplicationContext();

			if (_appConfig.getWorkingArea().CancelSearchDeposit) {
				resetDeposit(true, false);
				_appConfig.getWorkingArea().CancelSearchDeposit = false;
				return;
			}

			if (_appConfig.getWorkingArea().CurrentCliente == null
					|| _appConfig.getWorkingArea().CurrentCliente.IdCliente == 0 || _newDeposit) {

				DepositManagerExtension.Dialogs.StartCustomerSearchDialog(this);
			} else {

				try {
					_searched = true;
					FillWindow();
					this._dialogDepositoModalidad = new AdvancedMessageBox();
					boolean resultDepositoModalidad = _dialogDepositoModalidad.Show("Gestión de Depósito", "Qué tipo de albarán Deseas ?", "Entregar mercancía físicamente", "Enviar desde Edicards", DepositManager.this.getContext(), MessageBoxType.Information);
					this._appConfig.getWorkingArea().CurrentDepositoModalidad = resultDepositoModalidad ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
					TextView labelTipoEntrega = (TextView) getActivity().findViewById(R.id.lblTipoEntrega);
					labelTipoEntrega.setText(_appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		System.gc();*/
	}

	@Override
	public void onStart() {
		super.onStart();
		_appConfig = (AppConfig) this.getActivity().getApplicationContext();

		/*if (_appConfig.getWorkingArea().CancelSearchDeposit) {
			resetDeposit(true, false);
			_appConfig.getWorkingArea().CancelSearchDeposit = false;
			return;
		}*/

		//this.showWaiting("Cargando depósito");

		if (_appConfig.getWorkingArea().CurrentCliente == null
				|| _appConfig.getWorkingArea().CurrentCliente.IdCliente == 0 || _newDeposit) {

			DepositManagerExtension.Dialogs.StartCustomerSearchDialog(this);
		}
	}

	public void callback (String id, String text) {
		
		try {
			
			if (id == "PAGADO") {
				CharSequence t = text;
				
				if (t.toString().trim().equalsIgnoreCase("CONTADO")) {
					this._checkPagado.setChecked(true);
					_deposito.Pagado = true;
				}
				else
				{
					this._checkPagado.setChecked(false);
					_deposito.Pagado = false;
				}
			}
			else if (id == "SERIE") {
				this.refreshTotals();
			}
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);

		}
	}

	private void resetFooter() {

		LinearLayout footerLinearLayout1 = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout2);
		footerLinearLayout1.removeAllViews();

		LinearLayout footerLinearLayout2 = (LinearLayout) this.getActivity().findViewById(R.id.footerMainLinearLayout);
		footerLinearLayout2.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 10, 0, 0);

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.removeAllViews();

		layout.setOrientation(LinearLayout.HORIZONTAL);

		ButtonColor nuevo = new ButtonColor(getActivity(), Color.MAGENTA);

		nuevo.setText("Nuevo depósito");
		nuevo.setTextSize(TEXT_SIZE_BUTTON);
		nuevo.setWidth(BUTTONS_WIDTH);

		nuevo.setLayoutParams(params);

		nuevo.setOnClickListener(arg0 -> resetDeposit(true, true));

		layout.addView(nuevo);
		footerLinearLayout2.addView(layout);
	}

	private void createHeaderControls() throws Exception {

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 5, 0, 0);

		FormaPago formaPago = Factory.build(FormaPago.class, _appConfig);

		// Formas de pago
		
		LabelColor labelPago = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Forma de pago", TEXT_SIZE, 200, params);
		
		_comboPago = DepositManagerExtension.UI.addCombo(_appConfig, 350, params, _appConfig.getCache().getAllFormasPagoList(), _deposito.Cliente.formaPago.Descripcion);
		
		// Filiacion
		
		LabelColor labelFiliacion = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Filiación", TEXT_SIZE, 200, params);

		
		TipoIVA iva = Factory.build(TipoIVA.class, _appConfig);
		_comboFiliacion = DepositManagerExtension.UI.addCombo(_appConfig, 350, params, iva.getFiliaciones(), iva.getFiliacionByCode(_deposito.Cliente.Filiacion));
		
		// //Copias
		
		LabelColor labelCopias = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Copias", TEXT_SIZE, 90, params);
		List<String> copias = new ArrayList<>();
		for (int i=1; i < 6; i++) copias.add(String.valueOf(i));
		
		_comboCopias = DepositManagerExtension.UI.addCombo(_appConfig, 75, params, copias, "1");
		
		// Series

		LabelColor labelSeries = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Series", TEXT_SIZE, 90, params);
		
		List<String> series = new ArrayList<>();
		series.add(_appConfig.getUser().SerialInvoiceA);
		series.add(_appConfig.getUser().SerialInvoiceB);

		_comboSerie = DepositManagerExtension.UI.addCombo(_appConfig, 150, params, series, _appConfig.getUser().SerialInvoiceA);

		_deposito.Serie = _appConfig.getUser().SerialInvoiceA;

		// //Pagado

		_checkPagado = new CheckBox(_appConfig);
		_checkPagado.setText("Pagado");
		_checkPagado.setTextSize(TEXT_SIZE);
		_checkPagado.setLayoutParams(params);
		
		LabelColor labelCantidadPagada = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Cantidad Pagada", TEXT_SIZE, 200, params);
		
		_textBoxCantidadPagada = DepositManagerExtension.UI.addEdit(getActivity(), Color.GREEN, Gravity.LEFT, 
				"0", TEXT_SIZE, 100, params, false);

		// descuento 1
		
		LabelColor labelDescuento1 = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Dte. com.", TEXT_SIZE, 150, params);

		DecimalFormat dec = new DecimalFormat("0.00");

		TextBoxColor descuento1 = DepositManagerExtension.UI.addEdit(getActivity(), Color.WHITE, Gravity.LEFT,
				dec.format(_cliente.DescuentoProntoPago), TEXT_SIZE, 60, params, true);
		
		descuento1.setOnFocusChangeListener((view, hasFocus) -> {
			if (!hasFocus) {
				_lastTextBox = (TextBoxColor) view;

				EditText textBox = (EditText) view;
				float descuento11;

				if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
					descuento11 = Float.parseFloat(((EditText) view).getHint().toString());
				else
					descuento11 = Float.parseFloat(((EditText) view).getText().toString());

				if (descuento11 > 100) {
					_appConfig.getMessageBox().Show("Error", "El porcentage de descuento comercial es incorrecto",
							view.getContext(), MessageBoxType.Error);
				} else {

					_deposito.DescuentoComercial = descuento11;
				}


			} else {
				_lastTextBox = (TextBoxColor) view;
			}

			try {
				refreshTotals();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		});

		// descuento 2
		
		LabelColor labelDescuento2 = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Dte. fin.", TEXT_SIZE, 150, params);
		
		TextBoxColor descuento2 = DepositManagerExtension.UI.addEdit(getActivity(), Color.WHITE, Gravity.LEFT,
				dec.format(_cliente.DescuentoFinanciero), TEXT_SIZE, 60, params, true);
	
		descuento2.setOnFocusChangeListener((view, hasFocus) -> {
			if (!hasFocus) {

				_lastTextBox = (TextBoxColor) view;

				EditText textBox = (EditText) view;
				float descuento21;

				if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
					descuento21 = Float.parseFloat(((EditText) view).getHint().toString());
				else
					descuento21 = Float.parseFloat(((EditText) view).getText().toString());

				if (descuento21 > 100) {
					_appConfig.getMessageBox().Show("Error", "El porcentage de descuento financiero es incorrecto",
							view.getContext(), MessageBoxType.Error);
				} else {

					_deposito.DescuentoFinanciero = descuento21;
				}
			} else {
				_lastTextBox = (TextBoxColor) view;
			}

			try {
				refreshTotals();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		_checkPagado.setOnCheckedChangeListener((buttonView, isChecked) -> {
			_deposito.Pagado = isChecked;

			if (isChecked)
				_textBoxCantidadPagada.setFocusableInTouchMode(true);
			else
				_textBoxCantidadPagada.setFocusable(false);

		});
		
		if (_deposito.Cliente.formaPago != null && _deposito.Cliente.formaPago.Descripcion != null) {
			if (_deposito.Cliente.formaPago.Descripcion.trim().equalsIgnoreCase("CONTADO")) {
				this._checkPagado.setChecked(true);
				_deposito.Pagado = true;
			}
			else
			{
				this._checkPagado.setChecked(false);
				_deposito.Pagado = false;
			}
		}

		LinearLayout topLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout2);
		LinearLayout topLinearLayout2 = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout3);
		LinearLayout topLinearLayout3 = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout4);

		topLinearLayout.addView(labelPago);
		topLinearLayout.addView(_comboPago);
		topLinearLayout.addView(_checkPagado);
		topLinearLayout.addView(labelCantidadPagada);
		topLinearLayout.addView(_textBoxCantidadPagada);
		topLinearLayout2.addView(labelFiliacion);
		topLinearLayout2.addView(_comboFiliacion);
		topLinearLayout2.addView(labelCopias);
		topLinearLayout2.addView(_comboCopias);
		topLinearLayout2.addView(labelSeries);
		topLinearLayout2.addView(_comboSerie);
		topLinearLayout3.addView(labelDescuento1);
		topLinearLayout3.addView(descuento1);
		topLinearLayout3.addView(labelDescuento2);
		topLinearLayout3.addView(descuento2);

		_comboPago.addObserver("PAGADO", this);
		_comboSerie.addObserver("SERIE", this);
		
	}

	private void createHeaderButtons() {

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(5, 0, 5, 0);
		
		LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params2.setMargins(2, 2, 0, 0);

		// Nuevo Layout

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(params2);

		// Botón Datos
		
		ButtonColor datos = DepositManagerExtension.UI.addButton(getActivity(), Color.BLUE, "Cliente",
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params, getResources().getDrawable(R.drawable.ic_customer_data));
		
		final DepositManager that = this;
		datos.setOnClickListener(arg0 -> {

				if (_textBoxColorRequestFocus != null)
					_textBoxColorRequestFocus.requestFocus();

				try {
					DepositManagerExtension.Dialogs.StartCustomerDataDialog((Fragment) that);
				} catch (Exception e) {
						throw new RuntimeException(e);
				}


		});
		
		// Botón Totales
		ButtonColor totales = DepositManagerExtension.UI.addButton(getActivity(), Color.WHITE, "Resumen",
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params, getResources().getDrawable(R.drawable.ic_totals));

		totales.setOnClickListener(arg0 -> {

			if (_textBoxColorRequestFocus != null)
				_textBoxColorRequestFocus.requestFocus();

			try {
				if (DepositManagerExtension.DataTier.IsSerieA(that._comboSerie, that._appConfig))
					_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
				else
					_deposito.Serie = _appConfig.getUser().SerialInvoiceB;

				DepositManagerExtension.Dialogs.StartTotalesDialog((Fragment) that);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		// Botón Albarán
		
		ButtonColor albaran = DepositManagerExtension.UI.addButton(getActivity(), Color.RED, "Cerrar operación", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params, getResources().getDrawable(R.drawable.ic_save));
		
		albaran.setOnClickListener(arg0 -> {

			try {

				if (_textBoxColorRequestFocus != null)
					_textBoxColorRequestFocus.requestFocus();

				if (_deposito == null) {
					_appConfig.getMessageBox().Show("Atención",
							"No hay ningún depósito cargado",
							getActivity(), MessageBoxType.Error);

					return;
				}
				if (!DepositManagerExtension.DataTier.IsCustomerDataFilled(that._deposito)) {
					boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
							"El cliente NO está correctamente rellenado. Desea editarlo?", arg0.getContext(),
							MessageBoxType.Information);
					if (!result) {
						return;
					} else {
						DepositManagerExtension.Dialogs.StartCustomerDataDialog( (Fragment) that);
					}
				}

				GenerateAlbaran();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		ButtonColor nuevo = DepositManagerExtension.UI.addButton(getActivity(), Color.MAGENTA, "Nuevo depósito", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params, getResources().getDrawable(R.drawable.ic_new));
		
		nuevo.setOnClickListener(arg0 -> {

			try {
				_appConfig.getWorkingArea().TransferMode = TransferMode.None;
				resetDeposit(true, true);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});


		// Autocompletado para articulos

		AutoCompleteTextView searchArticulos = new AutoCompleteTextView(getContext());
		searchArticulos.setWidth(300);
		searchArticulos.setTextColor(getResources().getColor(R.color.Black));
		searchArticulos.setHint("nombre del artículo");
		searchArticulos.setThreshold(1);
		searchArticulos.setCompletionHint("Pulse el artículo que desea visualizar");
		this.createAutoComplete(searchArticulos);

		ButtonColor buttonSearchArticulos = DepositManagerExtension.UI.addButton(getActivity(), Color.RED, "",
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params, getResources().getDrawable(R.drawable.ic_view_all));

		buttonSearchArticulos.setOnClickListener( v-> {
			LinearLayout mainLayout = (LinearLayout) that.getActivity()
					.findViewById(R.id.mainLinearLayoutDepositManager);
			int count = mainLayout.getChildCount();

			for(int i=0; i<count; i++) {
				View view = mainLayout.getChildAt(i);
				view.setVisibility(View.VISIBLE);
			}
		});

		layout.addView(datos);
		layout.addView(totales);
		layout.addView(albaran);
		layout.addView(nuevo);
		layout.addView(searchArticulos);
		layout.addView(buttonSearchArticulos);
		
		LinearLayout footerLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerButtonsLinearLayout);
		footerLinearLayout.removeAllViews();
		footerLinearLayout.setOrientation(LinearLayout.VERTICAL);

		footerLinearLayout.addView(layout);

	}

	private void FillWindow() throws Exception {

		this.showHeader();
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutDepositManager);

		if (DepositManagerExtension.DataTier.RestriccionIngresos(this._appConfig)) {

			_appConfig.getMessageBox().Show("Atención",
					"Ha superado los " + Constants.MAXIMO_SIN_INGRESAR
							+ " € pendientes de ingresar. Realice un ingreso para poder seguir trabajando",
					getActivity(), MessageBoxType.Error);

			return;

		}

		Cliente cliente = _appConfig.getWorkingArea().CurrentCliente;
		_cliente = _appConfig.getWorkingArea().CurrentCliente;

		TextView label = (TextView) getActivity().findViewById(R.id.lblCliente);
		label.setText(cliente.Nombre + " - " + cliente.NIF);

		try {

			// Busquem el número de dipòsits del client

			Deposito deposito = Factory.build(Deposito.class, _appConfig);

			try {

				ArrayList<Deposito> depositos = (ArrayList<Deposito>) deposito
						.getDepositosByCodigoCliente(String.valueOf(cliente.CodigoCliente));

				if (_cliente.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {

					if (!_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)) {
						_appConfig.getMessageBox().Show("Información", "Se va a proceder a crear un cliente nuevo.",
								this.getActivity(), MessageBoxType.Information);

						_deposito = Factory.build(Deposito.class, _appConfig);
						_deposito.ClienteInfo = Factory.build(ClienteInfo.class, _appConfig);
						_deposito.assingFromCliente(_cliente);
						_deposito.Lineas.clear();
						this.addPotentialArticles();

					} else {
						_appConfig.getMessageBox().Show("Advertencia",
								"No se puede hacer un traspaso de un cliente nuevo. La operación va a ser cancelada",
								this.getActivity(), MessageBoxType.Information);

						_appConfig.getWorkingArea().TransferMode = TransferMode.None;

						return;
					}
				}

				if (depositos.size() == 0 && !_cliente.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {

					if (!_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)) {

						_deposito = Factory.build(Deposito.class, _appConfig);
						_deposito.ClienteInfo = Factory.build(ClienteInfo.class, _appConfig);
						_deposito.assingFromCliente(_cliente);

						this.addPotentialArticles();

						_appConfig.getMessageBox().Show("Información",
								"El cliente " + cliente.Nombre
										+ " no tiene ningún depósito. Se va a proceder a crear uno de nuevo",
								this.getActivity(), MessageBoxType.Information);
					} else {
						_appConfig.getMessageBox().Show("Advertencia",
								"No se puede hacer un traspaso de un cliente sin depósito asignado. La operación va a ser cancelada",
								this.getActivity(), MessageBoxType.Information);

						_appConfig.getWorkingArea().TransferMode = TransferMode.None;

						return;
					}

				} else if (depositos.size() > 0 && !_cliente.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {
					_deposito = Factory.build(Deposito.class, _appConfig);
					_deposito.setFirstDepositoByCliente(cliente.CodigoCliente);
					_deposito.assingFromCliente(_cliente);
					this.addPotentialArticles();
				}

				_appConfig.getWorkingArea().InitialDeposito = _deposito.CloneOnlyLineas();
				_appConfig.getWorkingArea().CurrentDeposito = _deposito;
				_appConfig.getWorkingArea().CurrentDeposito.DatosFiscalesUpdated = false;

			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.createHeaderLabels();
		createHeaderControls();
		this.createHeaderButtons();

		List<LineaDeposito> depositLines = new ArrayList<>();
		List<LineaDeposito> potentialLines = new ArrayList<>();

		for (LineaDeposito linea : _deposito.Lineas.values()) {
			if (linea.IsNew)
				potentialLines.add(linea);
			else
				depositLines.add(linea);
		}

		Collections.sort(depositLines, new LineaDeposito().new ArticuloComparator());
		Collections.sort(potentialLines, new LineaDeposito().new ArticuloComparator());

		for (LineaDeposito linea : depositLines) {
			try {
				mainLinearLayout.addView(addLine(linea));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		for (LineaDeposito linea : potentialLines) {
			try {
				mainLinearLayout.addView(addLine(linea));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void createAutoComplete(AutoCompleteTextView autocomplete) {
		
		final DepositManager that = this;

		autocomplete.addTextChangedListener(new DepositManagerTextWatcher());
		ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_dropdown_item_1line, this._articles);
                
        autocomplete.setAdapter(adapter);
		autocomplete.setOnItemClickListener((listView, view, position, id) -> {

			String selectedArticle =  listView.getItemAtPosition(position).toString().trim().toUpperCase();

			LinearLayout layout = (LinearLayout) that.getActivity()
					.findViewById(R.id.mainLinearLayoutDepositManager);
			int count = layout.getChildCount();

			boolean isShowed = false;

			for(int i=0; i<count; i++) {
				View v = layout.getChildAt(i);

				LabelColor articleLabel = (LabelColor) ((LinearLayout) v).getChildAt(1);
				String currentArticle = articleLabel.getText().toString().trim().toUpperCase();

				if (selectedArticle.equals(currentArticle) && !isShowed) {
					v.setVisibility(View.VISIBLE);
					isShowed = true;
				} else {
					v.setVisibility(View.GONE);
				}

			}
			autocomplete.setText(Constants.EMPTY_STRING);
		});
	}
	
	private void addPotentialArticles() throws Exception {
		// Buscamos los artículos que no están asociados al depósito

		Articulo articulo = Factory.build(Articulo.class, _appConfig);
		LinkedHashMap<String, Articulo> articulos = _appConfig.getCache().getAllArticulos();

		for (Articulo articuloInCatalgo : articulos.values())
			try {

				if (_deposito.Lineas.containsKey(articuloInCatalgo.CodigoArticulo))
					continue;

				LineaDeposito linea = Factory.build(LineaDeposito.class, _appConfig);
				linea.Articulo = articuloInCatalgo;
				linea.Deposito = _deposito;
				linea.StockInicial = 0;
				linea.UnidadesIniciales = 0;
				linea.UnidadesInicialesFijas = linea.UnidadesIniciales;

				linea.PVP = linea.getPVP(_cliente, articuloInCatalgo);
				//linea.PVPAnterior = linea.getPVP(_cliente, articuloInCatalgo);
				linea.PVPAnterior = linea.PVP;
				linea.PVPInicial = linea.PVPAnterior;

				linea.Descuento1 = 0; // linea.getDte(_cliente,

				linea.IsNew = true;

				_deposito.Lineas.put(String.valueOf(linea.Articulo.CodigoArticulo), linea);
				//layouts.add(addLine(linea));

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}
	
	private void SaveDeposito() throws Exception {

		_deposito.saveChangesToDeposito();

		if (_deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {

			Deposito depositoNuevoCliente = Factory.build(Deposito.class, _appConfig);
			depositoNuevoCliente.assingFromDeposito(_deposito);
			depositoNuevoCliente.save();

			depositoNuevoCliente.DeleteAllLines();
			depositoNuevoCliente.update();

			_deposito.IdDeposito = depositoNuevoCliente.IdDeposito;

			// Generamos la incidencia de nuevo cliente

			String text = "Se ha creado un nuevo cliente con los siguientes datos: " + Constants.NEW_LINE
					+ Constants.NEW_LINE + "NIF/CIF: " + _deposito.NIF + Constants.NEW_LINE + "NOMBRE: "
					+ _deposito.Nombre + Constants.NEW_LINE + "RAZON: " + _deposito.Razon
					+ Constants.NEW_LINE;

			Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(), IncidenciaType.ClienteNuevo,
					text);
			incidencia.create();

		}

		this.SaveHistorico();

		XmlCreator creator = new XmlCreator(_appConfig, _appConfig);
		creator.createXmlArticulos();

		if (_deposito.isAlbaran())
			creator.createXmlAlbaran(_deposito);

		if (_deposito.isDeposito() || (!_deposito.isDeposito() && _deposito.isDepositoUpdated()))
			creator.createXmlDeposito(_deposito);

	}

	private void GenerateAlbaran() throws Exception {

		String GUID = "";

		if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
			_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
			_appConfig.getWorkingArea().CurrentDeposito.Serie = _appConfig.getUser().SerialInvoiceA;
		} else {
			_deposito.Serie = _appConfig.getUser().SerialInvoiceB;
			_appConfig.getWorkingArea().CurrentDeposito.Serie = _appConfig.getUser().SerialInvoiceB;
		}

		double cantidadPagada;
		String cantidadPagadaText = _textBoxCantidadPagada.getText().toString();

		if (_textBoxCantidadPagada.getText().toString().equals(Constants.EMPTY_STRING))
			cantidadPagada = Double.parseDouble("0");
		else
			cantidadPagada = Double.parseDouble(cantidadPagadaText);

		_deposito.CantidadPagada = cantidadPagada;
		if ((_checkPagado.isChecked())
				&& (DepositManagerExtension.Format.RoundTo2Decimals(_deposito.CantidadPagada) > DepositManagerExtension.Format.RoundTo2Decimals(_deposito.Totales.Total))
				&& (_deposito.Totales.Total > 0)) {
			_appConfig.getMessageBox().Show("Atención",
					"La cantidad pagada no puede ser superior al total de la factura. Revíselo y vuelva a cerrer la operación",
					this.getActivity(), MessageBoxType.Error);

			_textBoxCantidadPagada.requestFocus();

			return;

		} else if ((!_checkPagado.isChecked())) {
			_deposito.CantidadPagada = 0;
		}

		boolean close = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
				"Se va a proceder a cerrar la operación. Desea Continuar?", this.getActivity(),
				MessageBoxType.Information);
		if (!close)
			return;

		_deposito.Retirado = false;

		if (_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)
				&& !_deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {
			boolean retirarDeposito = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
					"Este es un proceso de traspaso de cliente. Desea Retirar todo el depósito?", this.getActivity(),
					MessageBoxType.Information);

			if (retirarDeposito)
				_deposito.RetirarDeposito();
		}

		if (_deposito.isDepositoRetirado()) {

			boolean result;
			if (!_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)) {
				result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
						"NO se ha dejado ningún artículo en depósito. Desea asignar el depósito como una baja?",
						this.getActivity(), MessageBoxType.Information);

				_deposito.Retirado = result;
				_deposito.RetirarDeposito();
			} else {
				_deposito.Retirado = true;
				result = true;
			}

			if (result) {

				// Introducimos el motivo de la baja del depósito

				String motivo;
				
				do {
					motivo = _appConfig.getMessageBox().InputBox("Cierre de operación",
							"Introduzca el motivo de la baja", getActivity());	
				} while (motivo.trim().equals(Constants.EMPTY_STRING));
				
				_deposito.MotivoRetirado = motivo;

				Map<String, LineaDeposito> processed =new HashMap<>();

				for (LineaDeposito linea : _deposito.Lineas.values()) {
					
					if (linea.UnidadesDevueltas > 0) {
						LogBook logBookTrace = Factory.build(LogBook.class, _appConfig);

						int stockInicial = linea.Articulo.Stock;
						linea.Articulo.Stock = stockInicial + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
								- linea.UnidadesRepuestas
								- (linea.UnidadesFacturadas - (linea.UnidadesInicialesFijas - linea.UnidadesDevueltas));

						if (stockInicial != linea.Articulo.Stock) {

							if (!processed.containsKey(linea.Articulo.CodigoArticulo))
							{
								logBookTrace.setData("DEPOSITO RETIRADO", _deposito.Cliente.CodigoCliente,
										_deposito.Cliente.Razon, linea.Articulo.CodigoArticulo, linea.Articulo.Descripcion,
										stockInicial, linea.Articulo.Stock, linea.UnidadesDevueltas, linea.UnidadesDefectuosas,
										linea.UnidadesRepuestas, linea.UnidadesFacturadas, linea.UnidadesInicialesFijas,
										linea.UnidadesAbono, linea.UnidadesDefectuosas);

								logBookTrace.save();
								processed.put(linea.Articulo.CodigoArticulo, linea);
							}
						}

						linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
								- linea.UnidadesDefectuosas - linea.UnidadesRepuestas
								- (linea.UnidadesFacturadas - linea.UnidadesInicialesFijas + linea.UnidadesDevueltas);
						
						linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.UnidadesDefectuosas;
						
						linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
								+ linea.UnidadesDefectuosas;

						linea.Articulo.update();	
					}
				}	
				
				_deposito.DeleteAllLines();

				// Generamos la incidencia de baja de cliente

				String text = "Se ha dado de baja el deposito con los siguientes datos: " + Constants.NEW_LINE
						+ Constants.NEW_LINE + "NUM. DEPOSITO DIMONI: " + _deposito.NumDoc
						+ Constants.NEW_LINE + "NÚM. DEPOSITO TABLET (RefExt): " + _deposito.IdDeposito
						+ Constants.NEW_LINE + "NIF/CIF: " + _deposito.NIF + Constants.NEW_LINE + "NOMBRE: "
						+ _deposito.Nombre + Constants.NEW_LINE + "RAZON: " + _deposito.Razon
						+ Constants.NEW_LINE + "MOTIVO DE LA BAJA: " + _deposito.MotivoRetirado
						+ Constants.NEW_LINE;

				
				Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(),
						IncidenciaType.BajaCliente, text);
				incidencia.create();
				 
				XmlCreator creator = new XmlCreator(_appConfig, _appConfig);
				creator.createXmlDeposito(_deposito);
			}
		}

		if (_deposito.isDeposito() && (!_deposito.isDepositoUpdated() || _deposito.isDepositoUpdatedOnlyVentaDirecta())) {
			boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
					"NO se ha modificado el depósito. Desea Continuar?", this.getActivity(),
					MessageBoxType.Information);
			if (!result)
				return;
		}

		if (!_deposito.isAlbaran()) {
			boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
					"La operación realizada NO generará factura. Desea Continuar?", this.getActivity(),
					MessageBoxType.Information);
			if (!result)
				return;
		}

		if (_deposito.isDeposito() || _deposito.isAlbaran() || _deposito.isDepositoRetirado()) {

			Historico historico = new Historico();
			GUID = historico.GUID;
			_appConfig.getWorkingArea().CurrentHistorico = historico;

			_deposito.Cliente.CheckIfNewFiliacion(_deposito.Filiacion, _deposito.Cliente.CodigoCliente);

			if (_deposito.isDeposito() || _deposito.isAlbaran() || _deposito.isDepositoRetirado()) {
				DepositManagerExtension.Dialogs.StartSignatureCustomerDialog(this);
				
				boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
						"Se va a proceder a guardar los datos. Si sigue adelante, el depósito y/o el albarán ya no podrán ser modificados. Desea Continuar?",
						this.getActivity(), MessageBoxType.Information);

				if (!result)
					return;

				SaveDeposito();

				if (_deposito.isAlbaran() && _deposito.Pagado) {
					DepositManagerExtension.Dialogs.StartSignatureVendorDialog(this);
				}

				// Generem el consentiment GDPR si és necessari

				if (!_deposito.Cliente.hasGDPRSigned()) {

					PdfGDPR gdpr = new PdfGDPR(this._deposito.Cliente, GUID, this._appConfig, this._appConfig);
					if (gdpr.createGDPR())
						_deposito.Cliente.setGDPRSigned();
					else
						_appConfig.getMessageBox().Show("Cierre de operación",
							"Se ha producido un error al generar el documento GDPR. Contacte con el servicio técnico",
							this.getActivity(), MessageBoxType.Error);

					DepositManagerExtension.Documents.GenerateAuthorization(GUID, _deposito, _appConfig);
				}
			}

			if (_appConfig.getWorkingArea().CurrentHistorico.GUID == null) {
				_appConfig.getMessageBox().Show("Cierre de operación",
						"Se ha producido un error al cerrar la operación. Contacte con el servicio técnico",
						this.getActivity(), MessageBoxType.Information);
				return;
			}

			resetDeposit(false, true);
		}

		this.printDeposito(GUID);
		closeOperation();
	}

	private void SaveHistorico() throws Exception {

		_deposito.PagoDescripcion = _comboPago.getText();
		_deposito.Filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText());
		_deposito.Pagado = _checkPagado.isChecked();
		_deposito.formaPago = DepositManagerExtension.DataTier.getFormaPagoByDescripcion(_comboPago.getText(), this._appConfig);

		if (_deposito.isAlbaran()) {
			if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig))
				_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
			else
				_deposito.Serie = _appConfig.getUser().SerialInvoiceB;

			Contador contador = Factory.build(Contador.class, _appConfig);
			_deposito.NumeroAlbaran = contador.updateContador(_deposito, _appConfig.getUser());
		}

		_appConfig.getWorkingArea().CurrentHistorico.saveChangesToHistorico(_deposito);
	}

	private void printDeposito(String GUID) {
		boolean printDeposito = true;

		if (!_deposito.isDepositoUpdated() || _deposito.isDepositoUpdatedOnlyVentaDirecta()) {
			printDeposito = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"El depósito no ha sido modificado. Desea imprimirlo de todos modos ?", this.getActivity(),
					MessageBoxType.Information);
		}

		if (_deposito.isDeposito() || _deposito.isAlbaran()) {

			try {
				DepositManagerExtension.Documents.GeneratePdf(GUID, _deposito, _appConfig);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (DocumentException e) {
				throw new RuntimeException(e);
			}

			boolean resultImp = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"Desea iniciar la impresión ?", this.getActivity(), MessageBoxType.Information);
			if (!resultImp) {
				try {
					closeOperation();
				} catch (Exception e) {
					return;
				}
				return;
			}

			int copias = Integer.parseInt(_comboCopias.getText());

			PrintManager printManager = new PrintManager();

			// Comprobamos que el dispositivo esté funcionando correctamente
			boolean cancelStartPrint;
			boolean printerStatus = false;

			do {

				for (int i = 1; i < 3; i++) {
					if (!printerStatus) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						printerStatus = printManager.getStatus(getActivity().getApplicationContext(), _appConfig,
								false);
					}
				}

				if (!printerStatus)
					cancelStartPrint = _appConfig.getMessageBox().ShowWithResult("No se pudo iniciar la impresión",
							"No se pudo iniciar la impresión. Revise el dispositivo. Desea volverlo a intentar?",
							this.getActivity(), MessageBoxType.Information);
				else
					cancelStartPrint = false;
			} while (cancelStartPrint);

			if (printerStatus) {

				for (int i = 1; i <= copias; i++) {
					boolean result;
					boolean cancel = false;

					if (_deposito.isDeposito() && printDeposito) {
						do {
							try {
								result = printManager.printDeposito(_deposito, _appConfig, _appConfig, GUID);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
							if (!result)
								cancel = _appConfig.getMessageBox().ShowWithResult("Impresión de depósito",
										"No se pudo imprimir el depósito. Desea volverlo a intentar?",
										this.getActivity(), MessageBoxType.Information);
						} while (cancel);

						if (!result) {
							try {
								closeOperation();
							} catch (Exception e) {
								printManager.Release();
								return;
							}
							printManager.Release();
							return;
						}

					}
					result = false;
					cancel = false;
					if (_deposito.isAlbaran()) {
						do {
							try {
								result = printManager.printAlbaran(_deposito, this.getActivity(), _appConfig, GUID, DepositManagerExtension.DataTier.isTransferPayment(_deposito.formaPago));
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
							if (!result)
								cancel = _appConfig.getMessageBox().ShowWithResult("Impresión de depósito",
										"No se pudo imprimir el albarán. Desea volverlo a intentar?",
										this.getActivity(), MessageBoxType.Information);
						} while (cancel);
					}

					if (!result) {
						try {
							closeOperation();
						} catch (Exception e) {
							printManager.Release();
							return;
						}
						printManager.Release();
						return;
					}

				}
			}
		}
	}

	private void closeOperation() throws Exception {

		try {

			_appConfig.getMessageBox().Show("Cierre de operación", "La operación se ha cerrado correctamente.",
					this.getActivity(), MessageBoxType.Information);

			_appConfig.getWorkingArea().CurrentCliente = null;
			_appConfig.getWorkingArea().CurrentDeposito = null;
			_deposito = null;

			DepositManagerExtension.Documents.sendData(this.getActivity(), this._appConfig);

			if (_appConfig.getWorkingArea().TransferMode.equals(TransferMode.New))
				_appConfig.getWorkingArea().TransferMode = TransferMode.None;

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

	}


	private void resetHeader() {
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		mainLinearLayout.removeAllViews();
	}
	
	private void addLineHeader(LineaDeposito lineaDeposito, final LinearLayout layoutGrid) throws Exception {

		final DepositManager that = this;

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		mainLinearLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		boolean createHeaderLayout = _headerLayout == null;
		int color = Color.WHITE;

		if (createHeaderLayout) {
			_headerLayout = new LinearLayout(_appConfig);
			_headerLayout.setOrientation(LinearLayout.HORIZONTAL);
		}


		try {
			LabelColor codigoArticulo = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
					lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, 8, params, true)
					: (LabelColor) _headerLayout.getChildAt(0);
			codigoArticulo.setText(lineaDeposito.Articulo.CodigoArticulo);

			LabelColor articuloDescripcion = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
					lineaDeposito.Articulo.Descripcion.trim(), TEXT_SIZE, 12,
					params, true, lineaDeposito.Articulo)
					: (LabelColor) _headerLayout.getChildAt(1);
			articuloDescripcion.setText(lineaDeposito.Articulo.Descripcion.trim());
			articuloDescripcion.setTag(lineaDeposito.Articulo);

			articuloDescripcion.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					DepositManagerExtension.Dialogs.StartArticuloDialog((Articulo) ((LabelColor) v).getTag(), (Fragment) that);
				}
			});

			LabelColor unidadesInicialesFijas = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
					String.valueOf(lineaDeposito.UnidadesInicialesFijas), TEXT_SIZE, 13, params, true, lineaDeposito)
					: (LabelColor) _headerLayout.getChildAt(2);
			unidadesInicialesFijas.setText(String.valueOf(lineaDeposito.UnidadesInicialesFijas));
			unidadesInicialesFijas.setTag(lineaDeposito);

			TextBoxColor unidadesDevueltas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesDevueltas), TEXT_SIZE, 100, params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(3);
			unidadesDevueltas.setText(String.valueOf(lineaDeposito.UnidadesDevueltas));
			unidadesDevueltas.setWidth(100);
			unidadesDevueltas.setTag(lineaDeposito);

			unidadesDevueltas.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {

						_lastTextBox = (TextBoxColor) view;
						TextBoxColor textBox = (TextBoxColor) view;
						int unidadesDevueltas;

						if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
							unidadesDevueltas = Integer.parseInt(textBox.getHint().toString());
						else
							unidadesDevueltas = Integer.parseInt(textBox.getText().toString());

						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();
						if ((unidadesDevueltas) > lineaDeposito.UnidadesInicialesFijas) {
							_appConfig.getMessageBox()
									.Show("Atención",
											"La cantidad devuelta no puede ser superior a la inicial en el artículo "
													+ lineaDeposito.Articulo.Descripcion,
											getActivity(), MessageBoxType.Error);

							unidadesDevueltas = lineaDeposito.UnidadesInicialesFijas;
							textBox.setText(String.valueOf(unidadesDevueltas));
							((LineaDeposito) view.getTag()).UnidadesDevueltas = unidadesDevueltas;
						} else {
							((LineaDeposito) view.getTag()).UnidadesDevueltas = unidadesDevueltas;
							try {
								refreshLayout(lineaDeposito, layoutGrid, false);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}

						}
					} else {
						((TextBoxColor) view).setText(Constants.EMPTY_STRING);
						_lastTextBox = (TextBoxColor) view;
					}

					that.closeKeyboard((EditText) view);
				}

			});

			TextBoxColor unidadesDefectuosas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesDefectuosas), TEXT_SIZE, 100, params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(4);
			unidadesDefectuosas.setText(String.valueOf(lineaDeposito.UnidadesDefectuosas));
			unidadesDefectuosas.setWidth(100);
			unidadesDefectuosas.setTag(lineaDeposito);

			unidadesDefectuosas.setEnabled(false);
			unidadesDefectuosas.setInputType(InputType.TYPE_NULL);

			unidadesDefectuosas.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {
						_lastTextBox = (TextBoxColor) view;
						EditText textBox = (EditText) view;
						int unidadesDefectuosas;

						if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
							unidadesDefectuosas = Integer.parseInt(((EditText) view).getHint().toString());
						else
							unidadesDefectuosas = Integer.parseInt(((EditText) view).getText().toString());

						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();

						if ((unidadesDefectuosas) > lineaDeposito.UnidadesDevueltas) {
							_appConfig.getMessageBox()
									.Show("Atención",
											"La cantidad de defectuosas no puede ser superior a las devueltas en el artículo "
													+ lineaDeposito.Articulo.Descripcion,
											getActivity(), MessageBoxType.Error);

							unidadesDefectuosas = 0;
							textBox.setText(String.valueOf(unidadesDefectuosas));
							((LineaDeposito) view.getTag()).UnidadesDefectuosas = unidadesDefectuosas;
							textBox.setHint(String.valueOf(unidadesDefectuosas));
						} else {
							((LineaDeposito) view.getTag()).UnidadesDefectuosas = unidadesDefectuosas;
							try {
								refreshLayout(lineaDeposito, layoutGrid, false);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}

						}
					} else {
						((TextBoxColor) view).setText(Constants.EMPTY_STRING);
						_lastTextBox = (TextBoxColor) view;
					}

					that.closeKeyboard((EditText) view);
				}
			});

			TextBoxColor pvp = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT,
					DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP),
					TEXT_SIZE, 100, params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(5);
			pvp.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP));
			pvp.setWidth(100);
			pvp.setTag(lineaDeposito);

			pvp.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {
						_lastTextBox = (TextBoxColor) view;
						EditText textBox = (EditText) view;
						float pvp;

						if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
							pvp = Float.parseFloat(textBox.getHint().toString());
						else {
							pvp = Float.parseFloat(textBox.getText().toString());
							((LineaDeposito) view.getTag()).PVPAnterior = pvp;
						}

						((LineaDeposito) view.getTag()).PVP = pvp;
						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();
						try {
							refreshLayout(lineaDeposito, layoutGrid, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} else {
						((TextBoxColor) view).setText(Constants.EMPTY_STRING);
						_lastTextBox = (TextBoxColor) view;
					}

					that.closeKeyboard((EditText) view);
				}
			});

			TextBoxColor unidadesFacturadas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.rgb(0, 128, 0), Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesFacturadas), TEXT_SIZE, 100, params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(6);
			unidadesFacturadas.setText(String.valueOf(lineaDeposito.UnidadesFacturadas));
			unidadesFacturadas.setWidth(100);
			unidadesFacturadas.setTag(lineaDeposito);


			unidadesFacturadas.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {
						_lastTextBox = (TextBoxColor) view;
						EditText textBox = (EditText) view;
						int unidadesFacturadas = 0;

						if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
							unidadesFacturadas = Integer.parseInt(((EditText) view).getHint().toString());
						else
							unidadesFacturadas = Integer.parseInt(((EditText) view).getText().toString());

						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();

						lineaDeposito.IsVentaDirecta = false;
						boolean directa = false;

						if (unidadesFacturadas < (lineaDeposito.UnidadesIniciales - lineaDeposito.UnidadesDevueltas)
								&& (!lineaDeposito.IsNew)) {
							_appConfig.getMessageBox().Show("Unidades Facturadas",
									"No puede realizar una venta directa con una cantidad inferior a la devuelta",
									getActivity(), MessageBoxType.Information);

							unidadesFacturadas = lineaDeposito.UnidadesIniciales - lineaDeposito.UnidadesDevueltas;
							((EditText) view).setText(String.valueOf(unidadesFacturadas));
							((LineaDeposito) view.getTag()).UnidadesFacturadas = unidadesFacturadas;
						} else {
							if (unidadesFacturadas > 0 && (unidadesFacturadas != (lineaDeposito.UnidadesInicialesFijas
									- lineaDeposito.UnidadesDevueltas))) {

								directa = true;

							}

							lineaDeposito.IsVentaDirecta = directa;
							if (lineaDeposito.IsVentaDirecta) {
								((LineaDeposito) view.getTag()).UnidadesFacturadas = unidadesFacturadas;

							}

							try {
								refreshLayout(lineaDeposito, layoutGrid, false);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}

					} else {
						((TextBoxColor) view).setText(Constants.EMPTY_STRING);
						_lastTextBox = (TextBoxColor) view;
					}

					that.closeKeyboard((EditText) view);
				}
			});

			TextBoxColor unidadesRepuestas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesRepuestas), TEXT_SIZE, 100, params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(7);
			unidadesRepuestas.setText(String.valueOf(lineaDeposito.UnidadesRepuestas));
			unidadesRepuestas.setWidth(100);
			unidadesRepuestas.setTag(lineaDeposito);

			unidadesRepuestas.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {
						_lastTextBox = (TextBoxColor) view;
						EditText textBox = (EditText) view;
						int unidadesRepuestas;

						if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
							unidadesRepuestas = Integer.parseInt(((EditText) view).getHint().toString());
						else
							unidadesRepuestas = Integer.parseInt(((EditText) view).getText().toString());

						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();

						((LineaDeposito) view.getTag()).UnidadesRepuestas = unidadesRepuestas;

						try {
							refreshLayout(lineaDeposito, layoutGrid, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

					} else {
						((TextBoxColor) view).setText(Constants.EMPTY_STRING);
						_lastTextBox = (TextBoxColor) view;
					}

					that.closeKeyboard((EditText) view);
				}
			});

			TextBoxColor pvpAnterior = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT,
					DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior),
					TEXT_SIZE, 100, params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(8);
			pvpAnterior.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior));
			pvpAnterior.setWidth(100);
			pvpAnterior.setTag(lineaDeposito);

			pvpAnterior.setOnFocusChangeListener(new OnFocusChangeListener() {
				public void onFocusChange(View view, boolean hasFocus) {
					if (!hasFocus) {
						_lastTextBox = (TextBoxColor) view;
						EditText textBox = (EditText) view;
						float pvpAnterior;

						if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
							pvpAnterior = Float.parseFloat(textBox.getHint().toString());
						else
							pvpAnterior = Float.parseFloat(textBox.getText().toString());

						((LineaDeposito) view.getTag()).PVPAnterior = pvpAnterior;
						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();
						try {
							refreshLayout(lineaDeposito, layoutGrid, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					} else {
						((TextBoxColor) view).setText(Constants.EMPTY_STRING);
						_lastTextBox = (TextBoxColor) view;
					}


					that.closeKeyboard((EditText) view);
				}
			});

			double total = (lineaDeposito.UnidadesFacturadas * lineaDeposito.PVP);

			LabelColor totalLinea = createHeaderLayout ? DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
					String.valueOf(DepositManagerExtension.Format.round(total, 2)), TEXT_SIZE, 50, params, true, lineaDeposito)
					: (LabelColor) _headerLayout.getChildAt(9);

			totalLinea.setText(String.valueOf(DepositManagerExtension.Format.round(total, 2)));
			totalLinea.setTag(lineaDeposito);

			// Botón Abono

			ButtonColor modoAbono = createHeaderLayout ? DepositManagerExtension.UI.addButton(getActivity(), Color.GREEN, "ABONO",
					10, 70, params, lineaDeposito)
					: (ButtonColor) _headerLayout.getChildAt(10);

			modoAbono.setTag(lineaDeposito);
			if (_appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards)
				modoAbono.setEnabled(false);

			modoAbono.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {

					_abonoMode = true;
					LineaDeposito lineaDeposito = (LineaDeposito) arg0.getTag();
					try {
						refreshLayout(lineaDeposito, layoutGrid, true);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});


			if (createHeaderLayout) {
				_headerLayout.addView(codigoArticulo);
				_headerLayout.addView(articuloDescripcion);
				_headerLayout.addView(unidadesInicialesFijas);
				_headerLayout.addView(unidadesDevueltas);
				_headerLayout.addView(unidadesDefectuosas);
				_headerLayout.addView(pvp);
				_headerLayout.addView(unidadesFacturadas);
				_headerLayout.addView(unidadesRepuestas);
				_headerLayout.addView(pvpAnterior);
				_headerLayout.addView(totalLinea);
				_headerLayout.addView(modoAbono);
			}
			mainLinearLayout.addView(_headerLayout);
			_textBoxColorRequestFocus = unidadesDefectuosas;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void addLineHeaderAbono(LineaDeposito lineaDeposito,  final LinearLayout layoutGrid) throws Exception {

		final DepositManager that = this;

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		mainLinearLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		boolean createHeaderLayout = _headerAbonoLayout	 == null;
		int color = Color.WHITE;

		if (createHeaderLayout) {
			_headerAbonoLayout = new LinearLayout(_appConfig);
			_headerAbonoLayout.setOrientation(LinearLayout.HORIZONTAL);
		}

		LabelColor codigoArticulo = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, 8, params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(0);
		codigoArticulo.setText(lineaDeposito.Articulo.CodigoArticulo);

		LabelColor articuloDescripcion = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.Descripcion.trim() + " (Abono) ", TEXT_SIZE, 13,
						params, true, lineaDeposito.Articulo)
				: (LabelColor) _headerAbonoLayout.getChildAt(1);
		articuloDescripcion.setText((lineaDeposito.Articulo.Descripcion.trim()));
		articuloDescripcion.setTag(lineaDeposito.Articulo);

		articuloDescripcion.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				DepositManagerExtension.Dialogs.StartArticuloDialog((Articulo) ((LabelColor) v).getTag(), (Fragment) that);
			}
		});

		LabelColor labelCantidadAbono = createHeaderLayout ?  DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Cantidad", TEXT_SIZE, 100, params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(2);

		TextBoxColor unidadesAbono =  createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
				String.valueOf(lineaDeposito.UnidadesAbono), TEXT_SIZE, 100, params, true, lineaDeposito)
				: (TextBoxColor) _headerAbonoLayout.getChildAt(3);
		unidadesAbono.setText(String.valueOf(lineaDeposito.UnidadesAbono));
		unidadesAbono.setTag(lineaDeposito);

		unidadesAbono.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					_lastTextBox = (TextBoxColor) view;
					EditText textBox = (EditText) view;
					int unidadesAbono;

					LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						unidadesAbono = Integer.parseInt(((EditText) view).getHint().toString());
					else
						unidadesAbono = Integer.parseInt(((EditText) view).getText().toString());

					int defectuosas = lineaDeposito.DefectuosasAbono;

					if (defectuosas > unidadesAbono) {
						_appConfig.getMessageBox()
								.Show("Atención",
										"La cantidad de unidades devueltas es inferior a las defectuosas en el artículo "
												+ lineaDeposito.Articulo.Descripcion,
										getActivity(), MessageBoxType.Error);

						textBox.setText(String.valueOf(lineaDeposito.UnidadesAbono));
						textBox.setHint(String.valueOf(lineaDeposito.UnidadesAbono));
					} else {
						((LineaDeposito) view.getTag()).UnidadesAbono = unidadesAbono;
						((LineaDeposito) view.getTag()).DefectuosasAbono = defectuosas;

					}

					try {
						refreshLayout(lineaDeposito, layoutGrid,false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				} else {
					((TextBoxColor) view).setText(Constants.EMPTY_STRING);
					_lastTextBox = (TextBoxColor) view;
				}

				that.closeKeyboard((EditText) view);
			}
		});

		LabelColor labelDefectuosasAbono =  createHeaderLayout ? DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Defect.", TEXT_SIZE, 100, params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(4);

		
		TextBoxColor defectuosasAbono = createHeaderLayout ?  DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
				String.valueOf(lineaDeposito.DefectuosasAbono), TEXT_SIZE, 100, params, true, lineaDeposito)
				: (TextBoxColor) _headerAbonoLayout.getChildAt(5);
		defectuosasAbono.setText(String.valueOf(lineaDeposito.DefectuosasAbono));
		defectuosasAbono.setTag(lineaDeposito);

		defectuosasAbono.setEnabled(false);
		defectuosasAbono.setInputType(InputType.TYPE_NULL);

		defectuosasAbono.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					_lastTextBox = (TextBoxColor) view;
					EditText textBox = (EditText) view;

					LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();
					int defectuosas;

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						defectuosas = Integer.parseInt(((EditText) view).getHint().toString());
					else
						defectuosas = Integer.parseInt(((EditText) view).getText().toString());

					((LineaDeposito) view.getTag()).DefectuosasAbono = defectuosas;

					if ((defectuosas) > lineaDeposito.UnidadesAbono) {
						_appConfig.getMessageBox()
								.Show("Atención",
										"La cantidad de defectuosas no puede ser superior a las devueltas en el artículo "
												+ lineaDeposito.Articulo.Descripcion,
										getActivity(), MessageBoxType.Error);

						defectuosas = 0;
						((LineaDeposito) view.getTag()).DefectuosasAbono = defectuosas;
						textBox.setHint(String.valueOf(defectuosas));
						textBox.setText(String.valueOf(defectuosas));

					}

					try {
						refreshLayout(lineaDeposito, layoutGrid,false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				} else {
					((TextBoxColor) view).setText(Constants.EMPTY_STRING);
					_lastTextBox = (TextBoxColor) view;
				}

				that.closeKeyboard((EditText) view);
			}
		});
		
		LabelColor labelPVPAbono = createHeaderLayout ?  DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"PVP", TEXT_SIZE, 100, params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(6);
		
		TextBoxColor pvpAbono =  createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAbono), TEXT_SIZE, 100, params, true, lineaDeposito)
				:(TextBoxColor) _headerAbonoLayout.getChildAt(7);
		pvpAbono.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAbono));
		pvpAbono.setTag(lineaDeposito);

		pvpAbono.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					_lastTextBox = (TextBoxColor) view;
					EditText textBox = (EditText) view;
					float pvpAbono;

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						pvpAbono = Float.parseFloat(textBox.getHint().toString());
					else
						pvpAbono = Float.parseFloat(textBox.getText().toString());

					((LineaDeposito) view.getTag()).PVPAbono = pvpAbono;
					LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();
					try {
						refreshLayout(lineaDeposito, layoutGrid,false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else {
					((TextBoxColor) view).setText(Constants.EMPTY_STRING);
					_lastTextBox = (TextBoxColor) view;
				}

				that.closeKeyboard((EditText) view);
			}
		});

		double totalAbonoImporte = lineaDeposito.TotalAbono;
		
		LabelColor totalAbono =  createHeaderLayout ?  DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.RIGHT, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(totalAbonoImporte), TEXT_SIZE, 100, params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(8);
		totalAbono.setText(DepositManagerExtension.Format.CurrencyFormat(totalAbonoImporte));
		totalAbono.setTag(lineaDeposito);
				
		// Botón Venta
		
		ButtonColor modoVenta = createHeaderLayout ?  DepositManagerExtension.UI.addButton(getActivity(), Color.GREEN, "Venta",
				10, 70, params, lineaDeposito)
				: (ButtonColor) _headerAbonoLayout.getChildAt(9);

		modoVenta.setTag(lineaDeposito);

		modoVenta.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				_abonoMode = false;
				LineaDeposito lineaDeposito = (LineaDeposito) arg0.getTag();
				try {
					refreshLayout(lineaDeposito, layoutGrid,true);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		if (createHeaderLayout) {

			_headerAbonoLayout.addView(codigoArticulo);
			_headerAbonoLayout.addView(articuloDescripcion);
			_headerAbonoLayout.addView(labelCantidadAbono);
			_headerAbonoLayout.addView(unidadesAbono);
			_headerAbonoLayout.addView(labelDefectuosasAbono);
			_headerAbonoLayout.addView(defectuosasAbono);
			_headerAbonoLayout.addView(labelPVPAbono);
			_headerAbonoLayout.addView(pvpAbono);
			_headerAbonoLayout.addView(totalAbono);
			_headerAbonoLayout.addView(modoVenta);
		}

		mainLinearLayout.addView(_headerAbonoLayout);
		_textBoxColorRequestFocus = defectuosasAbono;

	}

	private void showHeader() {
		(this.getActivity().findViewById(R.id.headerMainLinearLayout)).setVisibility(View.VISIBLE);

		((LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutDepositManager)).removeAllViews();
	}

	private void createHeaderLabels() {
		
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.headerMainLinearLayout6);
		
		mainLinearLayout.removeAllViews();
		
		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Código".toUpperCase(), TEXT_SIZE, 8, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Artículo".toUpperCase(), TEXT_SIZE, 12, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Dep. Inicial".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Contadas".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Reciclado".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"PVP Fact.".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Facturadas".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Repuestas".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"PVP Post.".toUpperCase(), TEXT_SIZE, 13, params));
		
		mainLinearLayout.addView(DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Total".toUpperCase(), TEXT_SIZE, 13, params));

	}

	private LinearLayout addLine(final LineaDeposito lineaDeposito) throws Exception {

		this._articles.add(lineaDeposito.Articulo.Descripcion);

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		int color = lineaDeposito.IsNew ? Color.BLUE : Color.BLACK;

		LabelColor codigoArticulo = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.CodigoArticulo.trim(), TEXT_SIZE, 8, params, true);

		LabelColor articuloDescripcion = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.Descripcion.trim(), TEXT_SIZE, 12, params, true, lineaDeposito.Articulo);

		LabelColor unidadesInicialesFijas = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesInicialesFijas), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.UnidadesDevueltas = lineaDeposito.UnidadesInicialesFijas;
		LabelColor unidadesDevueltas = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.RED, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesDevueltas), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.UnidadesDefectuosas = 0;
		LabelColor unidadesDefectuosas = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.RED, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesDefectuosas), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.UnidadesFacturadas = 0;
		LabelColor unidadesFacturadas = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.rgb(0, 128, 0), Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesFacturadas), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.UnidadesRepuestas = lineaDeposito.UnidadesInicialesFijas;
		LabelColor unidadesRepuestas = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesRepuestas), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.PVPAbono = lineaDeposito.PVP;
		lineaDeposito.PVPAnterior = lineaDeposito.PVP;
		
		LabelColor pvp = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.Descuento1 = lineaDeposito.getDte(_cliente, lineaDeposito.Articulo);

		LabelColor pvpAnterior = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior), TEXT_SIZE, 13, params, true, lineaDeposito);

		LabelColor totalLinea = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(0), TEXT_SIZE, 13, params, true, lineaDeposito);

		layout.addView(codigoArticulo);
		layout.addView(articuloDescripcion);
		layout.addView(unidadesInicialesFijas);
		layout.addView(unidadesDevueltas);
		layout.addView(unidadesDefectuosas);
		layout.addView(pvp);
		layout.addView(unidadesFacturadas);
		layout.addView(unidadesRepuestas);
		layout.addView(pvpAnterior);
		layout.addView(totalLinea);

		ImageView imageView = new ImageView(this._appConfig);
		LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
		imageViewParams.gravity = Gravity.CENTER_VERTICAL;

		imageView.setLayoutParams(imageViewParams);

		if (lineaDeposito.Articulo.StockPropio)
			imageView.setImageResource(R.drawable.stock_ok_png);
		else
			imageView.setImageResource(R.drawable.stock_ko_png);

		LinearLayout.LayoutParams layoutParamsImage = new LinearLayout.LayoutParams(25, 25);
		imageView.setLayoutParams(layoutParamsImage);
		layout.addView(imageView);

		layout.setClickable(true);
		layout.setOnClickListener(v -> {

			if (_lastTextBox != null)
				_lastTextBox.clearFocus();

			if (_lastSelectedLayout != null)
				_lastSelectedLayout.setBackgroundColor(Color.TRANSPARENT);

			v.setBackgroundColor(Color.rgb(240,140,40));
			_lastSelectedLayout = (LinearLayout) v;

			if (_abonoMode)
				try {
					addLineHeaderAbono(lineaDeposito, _lastSelectedLayout);
					refreshTotals();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			else
				try {
					addLineHeader(lineaDeposito, _lastSelectedLayout);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

		return layout;

	}

	private void refreshLayout(LineaDeposito linea, LinearLayout layoutGrid,  boolean redraw)
			throws Exception {

		if (redraw) {
			if (_abonoMode)
				addLineHeaderAbono(linea, layoutGrid);
			else
				addLineHeader(linea, layoutGrid);
		}

		linea.StockInicial = linea.Articulo.Stock + linea.UnidadesDevueltas - linea.UnidadesRepuestas;

		if (!linea.IsVentaDirecta) {
			linea.UnidadesFacturadas = linea.UnidadesInicialesFijas - linea.UnidadesDevueltas;
			linea.UnidadesIniciales = linea.UnidadesInicialesFijas + linea.UnidadesRepuestas - linea.UnidadesFacturadas
					- linea.UnidadesDevueltas;
		} else {
			linea.UnidadesIniciales = linea.UnidadesRepuestas;
		}

		refreshTotals();
		
		linea.TotalAbono = DepositManagerExtension.Format.round((linea.UnidadesAbono * linea.PVPAbono * -1), 2);

		double totalLinea = DepositManagerExtension.Format.round((linea.UnidadesFacturadas * linea.PVP)
				- ((linea.UnidadesFacturadas * linea.PVP) * (linea.Descuento1 / 100)), 2);


		//((LabelColor) controlsGrid.get(0)).setText(String.valueOf(linea.UnidadesIniciales));
		((LabelColor) layoutGrid.getChildAt(3)).setText(String.valueOf(linea.UnidadesDevueltas));

		((LabelColor) layoutGrid.getChildAt(4)).setText(String.valueOf(linea.UnidadesDefectuosas));

		((LabelColor) layoutGrid.getChildAt(5)).setText(String.valueOf(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVP))));
		((LabelColor) layoutGrid.getChildAt(6)).setText(String.valueOf(linea.UnidadesFacturadas));
		((LabelColor) layoutGrid.getChildAt(7)).setText(String.valueOf(linea.UnidadesRepuestas));

		//((LabelColor) controlsGrid.get(5)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVP)));

		((LabelColor) layoutGrid.getChildAt(9)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(totalLinea)));
		((LabelColor) layoutGrid.getChildAt(8)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVPAnterior)));

		if (linea.TotalAbono < 0)
			((ImageView) layoutGrid.getChildAt(10)).setImageResource(R.drawable.abono);
		else
			if (linea.Articulo.StockPropio)
				((ImageView) layoutGrid.getChildAt(10)).setImageResource(R.drawable.stock_ok_png);
			else
				((ImageView) layoutGrid.getChildAt(10)).setImageResource(R.drawable.stock_ko_png);

		if (!_abonoMode) {

			if (_headerLayout != null) {
				((TextView) _headerLayout.getChildAt(3)).setText(String.valueOf(linea.UnidadesIniciales));
				((TextView) _headerLayout.getChildAt(6)).setText(String.valueOf(linea.UnidadesFacturadas));
				((TextView) _headerLayout.getChildAt(8)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVPAnterior)));
				((TextView) _headerLayout.getChildAt(9)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(totalLinea)));
			}
		} else {

			((LabelColor) _headerAbonoLayout.getChildAt(8)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.TotalAbono)));
		}
	}



	private void refreshTotals() throws Exception {
		if (_deposito != null) {

			String filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText()).trim();
			_deposito.Cliente.Filiacion = filiacion;
			_deposito.Calculate();

			if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
				((TextView) getActivity().findViewById(R.id.lblBase)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase) + " €");
				((TextView) getActivity().findViewById(R.id.lblTotalFactura)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.Total) + " €");
			} else {
				((TextView) getActivity().findViewById(R.id.lblBase)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase) + " €");
				((TextView) getActivity().findViewById(R.id.lblTotalFactura)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase) + " €");
			}
			((TextView) getActivity().findViewById(R.id.lblTipoEntrega)).setText(_appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");
		} else {
			((TextView) getActivity().findViewById(R.id.lblBase)).setText(Constants.EMPTY_STRING);
			((TextView) getActivity().findViewById(R.id.lblTotalFactura)).setText(Constants.EMPTY_STRING);
			((TextView) getActivity().findViewById(R.id.lblTipoEntrega)).setText(Constants.EMPTY_STRING);
		}
	}

	private void resetDeposit(boolean initializeAttributes, boolean callOnResume) {

		try {
			
			if (DepositManagerExtension.DataTier.RestriccionIngresos(this._appConfig)) {

				_appConfig.getMessageBox().Show("Atención",
						"Ha superado los " + Constants.MAXIMO_SIN_INGRESAR
								+ " € pendientes de ingresar. Realice un ingreso para poder seguir trabajando",
						getActivity(), MessageBoxType.Error);

				return;

			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (initializeAttributes) {
			_newDeposit = true;
			_abonoMode = false;
			_appConfig.getWorkingArea().CurrentCliente = null;
			_appConfig.getWorkingArea().CurrentDeposito = null;
			_headerLayout = null;
			_headerAbonoLayout = null;

			if (callOnResume)
				onResume();

			_newDeposit = false;
		}

		resetHeader();
		showHeader();
		//resetFooter();
	}

	private void closeKeyboard(EditText editText) {
		InputMethodManager imm = (InputMethodManager) this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}

	@Override
	public void notify(String event, Object payload) {

		switch (event) {
			case "EventCustomerSelected": {
				try {

					try {
						this.FillWindow();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					this._dialogDepositoModalidad = new AdvancedMessageBox();
					boolean resultDepositoModalidad = _dialogDepositoModalidad.Show("Gestión de Depósito", "Qué tipo de albarán Deseas ?", "Entregar mercancía físicamente", "Enviar desde Edicards", DepositManager.this.getContext(), MessageBoxType.Information);
					this._appConfig.getWorkingArea().CurrentDepositoModalidad = resultDepositoModalidad ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
					TextView labelTipoEntrega = (TextView) getActivity().findViewById(R.id.lblTipoEntrega);
					labelTipoEntrega.setText(_appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			break;
			case "EventCustomerNew": {

				try {
					_appConfig.getWorkingArea().CurrentCliente = (Cliente) payload;
					this.FillWindow();

					this._dialogDepositoModalidad = new AdvancedMessageBox();
					boolean resultDepositoModalidad = _dialogDepositoModalidad.Show("Gestión de Depósito", "Qué tipo de albarán Deseas ?", "Entregar mercancía físicamente", "Enviar desde Edicards", DepositManager.this.getContext(), MessageBoxType.Information);
					this._appConfig.getWorkingArea().CurrentDepositoModalidad = resultDepositoModalidad ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
					TextView labelTipoEntrega = (TextView) getActivity().findViewById(R.id.lblTipoEntrega);
					labelTipoEntrega.setText(_appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				_appConfig.getWorkingArea().CurrentCliente = (Cliente) payload;
			}
			case "EventCustomerCancelled": {
				_appConfig.getWorkingArea().CancelSearchDeposit = true;
				//resetFooter();
			}
		}
	}
}
