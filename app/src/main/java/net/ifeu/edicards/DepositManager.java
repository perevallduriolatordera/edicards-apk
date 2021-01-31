package net.ifeu.edicards;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.LineaHistorico;
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
import net.ifeu.library.Signature.SignatureView;
import net.ifeu.library.Utils.MessageBoxType;
import net.ifeu.library.Utils.OS;

public class DepositManager extends Fragment implements IComboBoxChangeEvent, TextWatcher {

	private Deposito _deposito;
	private Cliente _cliente;
	private AppConfig _appConfig;

	private boolean _searched = false;
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
	private AutoCompleteTextView _myAutoComplete;
	
	private LinkedList<String> _articles = new LinkedList<String>();


	private final int TEXT_SIZE = 14;
	private final int TEXT_SIZE_LARGE = 16;
	private final int TEXT_SIZE_BUTTON = 12;
	private final int BUTTONS_WIDTH = 100;

	private final int WIDTH_OLDER_VERSION_CODIGO = 50;
	private final int WIDTH_NEWER_VERSION_CODIGO = 100;
	private final int WIDTH_OLDER_VERSION_DESCRIPCION = 200;
	private final int WIDTH_NEWER_VERSION_DESCRIPCION = 100;
			
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		try {
			this.FillWindow();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_deposit_manager, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		final Button viewAllButton = (Button) this.getActivity().findViewById(
				R.id.btnSearchClear);

		final DepositManager that = this;
		
		viewAllButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				LinearLayout layout = (LinearLayout) that.getActivity()
						.findViewById(R.id.mainLinearLayoutDepositManager);
				
		    	int count = layout.getChildCount();
		    	
		    	for(int i=0; i<count; i++) {
		    	    View view = layout.getChildAt(i);
		    	    view.setVisibility(View.VISIBLE);
		    	}
		    	
		    	that._myAutoComplete.setText(Constants.EMPTY_STRING);
			}
		});
		
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (!_searched) {

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
					FillWindow();
				} catch (Exception e) {
					e.printStackTrace();
				}
				_searched = true;

			}
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
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(ex),
					getActivity(), MessageBoxType.Error);

		}
	}

	private void resetFooter() {

		LinearLayout footerLinearLayout1 = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout4);
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

		nuevo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				resetDeposit(true, true);				
			}
		});

		layout.addView(nuevo);
		footerLinearLayout2.addView(layout);
	}

	private void FillFooter() throws Exception {

		_appConfig = (AppConfig) this.getActivity().getApplicationContext();

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.removeAllViews();
		
		final LinearLayout layout2 = new LinearLayout(_appConfig);
		layout2.removeAllViews();

		final LinearLayout topLayout = new LinearLayout(_appConfig);
		topLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 5, 0, 0);

		LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 4, 0, 0);

		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		topLayout.setOrientation(LinearLayout.HORIZONTAL);

		FormaPago formaPago = new FormaPago();
		formaPago.InitializePersistance(_appConfig, _appConfig);
		
		// Formas de pago
		
		LabelColor labelPago = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Forma de pago", TEXT_SIZE, 200, params);
		
		_comboPago = DepositManagerExtension.UI.addCombo(_appConfig, 300, params, _appConfig.getCache().getAllFormasPagoList(), _deposito.Cliente.formaPago.Descripcion);
		
		// Filiacion
		
		LabelColor labelFiliacion = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Filiación", TEXT_SIZE, 200, params);

		
		TipoIVA iva = new TipoIVA();
		iva.InitializePersistance(_appConfig, _appConfig);
		
		_comboFiliacion = DepositManagerExtension.UI.addCombo(_appConfig, 350, params, iva.getFiliaciones(), iva.getFiliacionByCode(_deposito.Cliente.Filiacion));
		
		// //Copias
		
		LabelColor labelCopias = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Copias", TEXT_SIZE, 150, params);
		List<String> copias = new ArrayList<String>();
		for (int i=1; i < 6; i++) copias.add(String.valueOf(i));
		
		_comboCopias = DepositManagerExtension.UI.addCombo(_appConfig, 75, params, copias, "1");
		
		// Series

		LabelColor labelSeries = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Series", TEXT_SIZE, 150, params);
		
		List<String> series = new ArrayList<String>();
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
				"0", TEXT_SIZE, 100, params2, false);

		_textBoxCantidadPagada.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					TextBoxColor textBox = (TextBoxColor) view;
					double cantidadPagada;
					String cantidadPagadaText = textBox.getText().toString();

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						cantidadPagada = Double.parseDouble("0");
					else
						cantidadPagada = Double.parseDouble(cantidadPagadaText);

					textBox.setText(DepositManagerExtension.Format.CurrencyFormat(cantidadPagada));
					_deposito.CantidadPagada = Double.parseDouble(textBox.getText().toString());
				}
			}
		});
		
		// descuento 1
		
		LabelColor labelDescuento1 = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Dte. com.", TEXT_SIZE, 150, params);

		DecimalFormat dec = new DecimalFormat("0.00");

		TextBoxColor descuento1 = DepositManagerExtension.UI.addEdit(getActivity(), Color.WHITE, Gravity.LEFT,
				dec.format(_cliente.DescuentoProntoPago), TEXT_SIZE, 60, params, true);
		
		descuento1.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					_lastTextBox = (TextBoxColor) view;

					EditText textBox = (EditText) view;
					float descuento1;

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						descuento1 = Float.parseFloat(((EditText) view).getHint().toString());
					else
						descuento1 = Float.parseFloat(((EditText) view).getText().toString());

					if (descuento1 > 100) {
						_appConfig.getMessageBox().Show("Error", "El porcentage de descuento comercial es incorrecto",
								view.getContext(), MessageBoxType.Error);
					} else {

						_deposito.DescuentoComercial = descuento1;
					}
					
					
				} else {
					_lastTextBox = (TextBoxColor) view;
				}
				
				try {
					refreshTotals();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		// descuento 2
		
		LabelColor labelDescuento2 = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Dte. fin.", TEXT_SIZE, 150, params);
		
		TextBoxColor descuento2 = DepositManagerExtension.UI.addEdit(getActivity(), Color.WHITE, Gravity.LEFT,
				dec.format(_cliente.DescuentoFinanciero), TEXT_SIZE, 60, params, true);
	
		descuento2.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {

					_lastTextBox = (TextBoxColor) view;

					EditText textBox = (EditText) view;
					float descuento2;

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						descuento2 = Float.parseFloat(((EditText) view).getHint().toString());
					else
						descuento2 = Float.parseFloat(((EditText) view).getText().toString());

					if (descuento2 > 100) {
						_appConfig.getMessageBox().Show("Error", "El porcentage de descuento financiero es incorrecto",
								view.getContext(), MessageBoxType.Error);
					} else {

						_deposito.DescuentoFinanciero = descuento2;
					}
				} else {
					_lastTextBox = (TextBoxColor) view;
				}
				
				try {
					refreshTotals();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
		_checkPagado.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				_deposito.Pagado = isChecked;

				if (isChecked)
					_textBoxCantidadPagada.setFocusableInTouchMode(true);
				else
					_textBoxCantidadPagada.setFocusable(false);

			}
		});
		
		if (_deposito.Cliente.formaPago != null && _deposito.Cliente.formaPago.Descripcion != null) {
			if (_deposito.Cliente.formaPago.Descripcion.toString().trim().equalsIgnoreCase("CONTADO")) {
				this._checkPagado.setChecked(true);
				_deposito.Pagado = true;
			}
			else
			{
				this._checkPagado.setChecked(false);
				_deposito.Pagado = false;
			}
		}

		layout.addView(labelPago);
		layout.addView(_comboPago);
		topLayout.addView(_checkPagado);
		topLayout.addView(labelCantidadPagada);
		topLayout.addView(_textBoxCantidadPagada);
		topLayout.addView(labelFiliacion);
		topLayout.addView(_comboFiliacion);
		layout.addView(labelCopias);
		layout.addView(_comboCopias);
		layout.addView(labelSeries);
		layout.addView(_comboSerie);
		layout2.addView(labelDescuento1);
		layout2.addView(descuento1);
		layout2.addView(labelDescuento2);
		layout2.addView(descuento2);

		LinearLayout topLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout4);
		topLinearLayout.removeAllViews();
		topLinearLayout.setOrientation(LinearLayout.VERTICAL);
		topLinearLayout.addView(topLayout);

		this.FillFooterButtons(layout, layout2);
		
		_comboPago.addObserver("PAGADO", this);
		_comboSerie.addObserver("SERIE", this);
		
	}

	private void FillFooterButtons(LinearLayout layout, LinearLayout layoutAux) {

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(10, 0, 10, 0);
		
		LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params2.setMargins(5, 5, 0, 0);

		// Nuevo Layout

		final LinearLayout layout2 = new LinearLayout(_appConfig);
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		layout2.setLayoutParams(params2);

		// Botón Datos
		
		ButtonColor datos = DepositManagerExtension.UI.addButton(getActivity(), Color.BLUE, "Datos del cliente", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params);
		
		final DepositManager that = this;
		datos.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

					if (_textBoxColorRequestFocus != null)
						_textBoxColorRequestFocus.requestFocus();

					try {
						DepositManagerExtension.Dialogs.StartCustomerDataDialog((Fragment) that);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				
			}
		});
		
		// Botón Totales
		ButtonColor totales = DepositManagerExtension.UI.addButton(getActivity(), Color.WHITE, "Resumen Totales", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params);

		totales.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				if (_textBoxColorRequestFocus != null)
					_textBoxColorRequestFocus.requestFocus();

				try {
					if (DepositManagerExtension.DataTier.IsSerieA(that._comboSerie, that._appConfig))
						_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
					else
						_deposito.Serie = _appConfig.getUser().SerialInvoiceB;

					DepositManagerExtension.Dialogs.StartTotalesDialog((Fragment) that);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}
		});

		// Botón Albarán
		
		ButtonColor albaran = DepositManagerExtension.UI.addButton(getActivity(), Color.RED, "Cerrar operación", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params);
		
		albaran.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
			
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
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				
				}
			}
			
		});

		ButtonColor nuevo = DepositManagerExtension.UI.addButton(getActivity(), Color.MAGENTA, "Nuevo depósito", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params);
		
		nuevo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					_appConfig.getWorkingArea().TransferMode = TransferMode.None;
					resetDeposit(true, true);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}
		});

		ButtonColor print = DepositManagerExtension.UI.addButton(getActivity(), Color.CYAN, "Estado impresora", 
				TEXT_SIZE_BUTTON, BUTTONS_WIDTH, params);
				
		print.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub

				try {

					if (!DepositManagerExtension.Devices.PrinterStatus(false, that._appConfig, that.getActivity().getApplicationContext() ))
						_appConfig.getMessageBox().Show("Estado de la impresora",
								"La impresora no está activada. Revise que esté correctamente encendida y con la batería cargada.",
								view.getContext(), MessageBoxType.Error);
					else
						_appConfig.getMessageBox().Show("Estado de la impresora",
								"La impresora está activada, preparada para imprimir.", view.getContext(),
								MessageBoxType.Error);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}
		});

		print.setVisibility(View.GONE);

		
		layout2.addView(datos);
		layout2.addView(totales);
		layout2.addView(albaran);
		layout2.addView(nuevo);
		layout2.addView(print);
		layoutAux.addView(layout2);
		
		LinearLayout footerLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.footerMainLinearLayout);
		footerLinearLayout.removeAllViews();
		footerLinearLayout.setOrientation(LinearLayout.VERTICAL);
		footerLinearLayout.addView(layout);
		
		footerLinearLayout.addView(layoutAux);

	}


	
	private void FillWindow() throws Exception {

		this.createHeaderLabels();
		
		if (DepositManagerExtension.DataTier.RestriccionIngresos(this._appConfig, this.getActivity().getApplicationContext())) {

			_appConfig.getMessageBox().Show("Atención",
					"Ha superado los " + Constants.MAXIMO_SIN_INGRESAR
							+ " € pendientes de ingresar. Realice un ingreso para poder seguir trabajando",
					getActivity(), MessageBoxType.Error);

			return;

		}
		
		Cliente cliente = _appConfig.getWorkingArea().CurrentCliente;
		_cliente = _appConfig.getWorkingArea().CurrentCliente;

		TextView label = (TextView) getActivity().findViewById(R.id.lblCliente);
		label.setText(cliente.Nombre + " - " + cliente.NIF + "     ");

		try {
			cliente.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);		}

		try {

			// Busquem el número de dipòsits del client

			Deposito deposito = new Deposito();
			deposito.InitializePersistance(_appConfig, _appConfig);

			try {

				ArrayList<Deposito> depositos = (ArrayList<Deposito>) deposito
						.getDepositosByCodigoCliente(String.valueOf(cliente.CodigoCliente));

				if (_cliente.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {

					if (!_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)) {
						_appConfig.getMessageBox().Show("Información", "Se va a proceder a crear un cliente nuevo.",
								this.getActivity(), MessageBoxType.Information);

						_deposito = new Deposito();
						_deposito.ClienteInfo = new ClienteInfo();
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

						_deposito = new Deposito();
						_deposito.ClienteInfo = new ClienteInfo();
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
					// _deposito = depositos.get(0);

					_deposito = new Deposito();
					_deposito.InitializePersistance(_appConfig, _appConfig);
					_deposito.setFirstDepositoByCliente(cliente.CodigoCliente);
					_deposito.assingFromCliente(_cliente);

				}

				_appConfig.getWorkingArea().InitialDeposito = _deposito.CloneOnlyLineas();

				_appConfig.getWorkingArea().CurrentDeposito = _deposito;
				_appConfig.getWorkingArea().CurrentDeposito.DatosFiscalesUpdated = false;

			} catch (Exception e1) {
				// TODO Auto-generated catch block
				_appConfig.getMessageBox().Show("Atención",
						_appConfig.getStackTrace(e1),
						getActivity(), MessageBoxType.Error);			}

		} catch (Exception e) {
			// TODO Auto-generated catch block

			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);		}

		FillFooter();
		
		List<LineaDeposito> tempList = new ArrayList<LineaDeposito>();

		for (LineaDeposito linea : _deposito.Lineas.values()) {
			tempList.add(linea);
		}
		
		Collections.sort(tempList, new LineaDeposito().new ArticuloComparator());
		for (LineaDeposito linea : tempList) {
			try {

				addLine(linea);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				_appConfig.getMessageBox().Show("Atención",
						_appConfig.getStackTrace(e),
						getActivity(), MessageBoxType.Error);			}
		}

		this.addPotentialArticles();
		this.createAutoComplete();

	}
	
	private void createAutoComplete() {
		
		final DepositManager that = this;
		
    	this._myAutoComplete = (AutoCompleteTextView) getActivity().findViewById(R.id.myautocomplete);
        this._myAutoComplete.addTextChangedListener(this);	
        
        this._myAutoComplete.setThreshold(1);
        this._myAutoComplete.setCompletionHint("Pulse el artículo que desea visualizar");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_dropdown_item_1line, this._articles);
                
        this._myAutoComplete.setAdapter(adapter);
        
        this._myAutoComplete.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View view,
                        int position, long id) {
                // Get the cursor, positioned to the corresponding row in the
                // result set
            	
            	String selectedArticle =  listView.getItemAtPosition(position).toString();
            	
            	LinearLayout layout = (LinearLayout) that.getActivity()
        				.findViewById(R.id.mainLinearLayoutDepositManager);
            	int count = layout.getChildCount();
            	
            	boolean isShowed = false;
            	
            	for(int i=0; i<count; i++) {
            	    View v = layout.getChildAt(i);
            	    
            	    LabelColor articleLabel = (LabelColor) ((LinearLayout) v).getChildAt(1);
            	    String currentArticle = articleLabel.getText().toString().trim();
            	    
            	    if (selectedArticle.equals(currentArticle) && !isShowed) {
            	    	v.setVisibility(View.VISIBLE);
            	    	isShowed = true;
            	    } else {
            	    	v.setVisibility(View.GONE);
            	    }
            	    
            	}
            	that._myAutoComplete.setText(Constants.EMPTY_STRING);
            }
        });
 
	}
	
	private void addPotentialArticles() throws Exception {
		// Buscamos los artículos que no están asociados al depósito
		
		Articulo articulo = new Articulo();
		articulo.InitializePersistance(_appConfig, _appConfig);
		
		LinkedHashMap<String, Articulo> articulos = articulo.getAllArticulos(1);

		//for (Articulo articuloInCatalgo : _appConfig.getCache().getAllArticulos().values())
		for (Articulo articuloInCatalgo : articulos.values())
			try {
				
				if (_deposito.Lineas.containsKey(articuloInCatalgo.CodigoArticulo)) 
					continue;
				
				LineaDeposito linea = new LineaDeposito();

				try {
					linea.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
				}

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
				addLine(linea);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				_appConfig.getMessageBox().Show("Atención",
						_appConfig.getStackTrace(e),
						getActivity(), MessageBoxType.Error);			}
	}

	
	private String SaveDeposito(Historico historico) throws Exception {
		try {
			_deposito.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			return null;
		}

		// if (_deposito.IdDeposito == null)
		_deposito.FechaDeposito = new Date();

		if (_deposito.IdDeposito == null)
			_deposito.save();
		else {
			_deposito.DeleteAllLines();
			_deposito.update();
		}
		
		for (LineaDeposito linea : _deposito.Lineas.values()) {
			
			if (!_deposito.isDepositoRetirado()) {
			
				if (linea.UnidadesRepuestas > 0) {
					linea.save();
	
					linea.Articulo.InitializePersistance(_appConfig, _appConfig);
					linea.Articulo.Activo = true;
	
					if (linea.IsVentaDirecta) {
						linea.Articulo.Stock = linea.Articulo.Stock + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
								- linea.UnidadesRepuestas
								- (linea.UnidadesFacturadas - (linea.UnidadesInicialesFijas - linea.UnidadesDevueltas));
	
						linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
								- linea.UnidadesDefectuosas - linea.UnidadesRepuestas
								- (linea.UnidadesFacturadas - linea.UnidadesInicialesFijas + linea.UnidadesDevueltas);
	
					} else {
						linea.Articulo.Stock = linea.Articulo.Stock + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
								- linea.UnidadesRepuestas;
	
						linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
								- linea.UnidadesDefectuosas - linea.UnidadesRepuestas;
					}
	
					linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.UnidadesDefectuosas;
	
					linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
							+ linea.UnidadesDefectuosas;
	
					linea.Articulo.update();
	
				} else {
					linea.Articulo.InitializePersistance(_appConfig, _appConfig);
					linea.Articulo.Activo = true;
	
					if (linea.IsVentaDirecta) {
						linea.Articulo.Stock = linea.Articulo.Stock - linea.UnidadesFacturadas;
	
						linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock - linea.UnidadesFacturadas;
					} else {
						linea.Articulo.Stock = linea.Articulo.Stock + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
								- linea.UnidadesRepuestas;
	
						linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
								- linea.UnidadesDefectuosas - linea.UnidadesRepuestas;
					}
					
					linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.UnidadesDefectuosas;
					
					linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
							+ linea.UnidadesDefectuosas;
	
					linea.Articulo.update();
				}
	
				if (linea.UnidadesAbono > 0) {
					linea.Articulo.InitializePersistance(_appConfig, _appConfig);
					linea.Articulo.Activo = true;
	
					linea.Articulo.Stock = linea.Articulo.Stock + linea.UnidadesAbono - linea.DefectuosasAbono;
	
					linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesAbono
							- linea.DefectuosasAbono;
	
					linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.DefectuosasAbono;
	
					linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
							+ linea.Articulo.MovimientoStockDefectuosas + linea.DefectuosasAbono;
	
					linea.Articulo.update();
	
				}
			}

		}
	

		// Guardamos el nuevo cliente

		if (_deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE)) {

			Deposito depositoNuevoCliente = new Deposito();
			depositoNuevoCliente.InitializePersistance(_appConfig, _appConfig);
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

		// Generamos el historico

		historico = this.SaveHistorico(historico);

		// Guardamos los xml a enviar

		XmlCreator creator = new XmlCreator(_appConfig, _appConfig);
		creator.createXmlArticulos();

		if (_deposito.isAlbaran())
			creator.createXmlAlbaran(_deposito);

		if (_deposito.isDeposito() || (!_deposito.isDeposito() && _deposito.isDepositoUpdated()))
			creator.createXmlDeposito(_deposito);

		// Serializamos el objeto deposito a JSON
		try {
			
			return historico.GUID;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);
			return null;
		}

	}

	private void GenerateAlbaran() throws Exception {

		String GUID = null;

		if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
			_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
			_appConfig.getWorkingArea().CurrentDeposito.Serie = _appConfig.getUser().SerialInvoiceA;
		} else {
			_deposito.Serie = _appConfig.getUser().SerialInvoiceB;
			_appConfig.getWorkingArea().CurrentDeposito.Serie = _appConfig.getUser().SerialInvoiceB;
		}

		if ((_checkPagado.isChecked())

				&& (DepositManagerExtension.Format.RoundTo2Decimals(_deposito.CantidadPagada) > DepositManagerExtension.Format.RoundTo2Decimals(_deposito.Totales.Total))
				&& (_deposito.Totales.Total > 0)) {
			_appConfig.getMessageBox().Show("Atención",
					"La cantidad pagada no puede ser superior al total de la factura. Revíselo y vuelva a cerrer la operación",
					this.getActivity(), MessageBoxType.Error);

			_textBoxCantidadPagada.requestFocus();

			return;

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

				String motivo = Constants.EMPTY_STRING;
				
				do {
					motivo = _appConfig.getMessageBox().InputBox("Cierre de operación",
							"Introduzca el motivo de la baja", getActivity());	
				} while (motivo.trim().equals(null) || motivo.trim().equals(Constants.EMPTY_STRING));
				
				_deposito.MotivoRetirado = motivo;
				
				for (LineaDeposito linea : _deposito.Lineas.values()) {
					
					if (linea.UnidadesDevueltas > 0) {
						
						
						linea.Articulo.Stock = linea.Articulo.Stock + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
								- linea.UnidadesRepuestas
								- (linea.UnidadesFacturadas - (linea.UnidadesInicialesFijas - linea.UnidadesDevueltas));

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

		if (_deposito.isDeposito() && !_deposito.isDepositoUpdated()) {
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
			
			// Guardem la nova filiació i enviem incidència si és necessari

			this.CheckIfNewFiliacion(_deposito.Cliente.Filiacion);

			if (_deposito.isDeposito() || _deposito.isAlbaran() || _deposito.isDepositoRetirado()) {
				DepositManagerExtension.Dialogs.StartSignatureCustomerDialog(this);
				
				boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
						"Se va a proceder a guardar los datos. Si sigue adelante, el depósito y/o el albarán ya no podrán ser modificados. Desea Continuar?",
						this.getActivity(), MessageBoxType.Information);

				if (!result)
					return;
				else
					GUID = SaveDeposito(_appConfig.getWorkingArea().CurrentHistorico);

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
				}
			}

			if (GUID == null) {
				_appConfig.getMessageBox().Show("Cierre de operación",
						"Se ha producido un error al cerrar la operación. Contacte con el servicio técnico",
						this.getActivity(), MessageBoxType.Information);
				return;
			}

			resetDeposit(false, true);
		}

		boolean printDeposito = true;

		if (!_deposito.isDepositoUpdated()) {
			printDeposito = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"El depósito no ha sido modificado. Desea imprimirlo de todos modos ?", this.getActivity(),
					MessageBoxType.Information);
		}

		if (_deposito.isDeposito() || _deposito.isAlbaran()) {

			boolean resultImp = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"Desea iniciar la impresión ?", this.getActivity(), MessageBoxType.Information);
			if (!resultImp) {
				closeOperation(GUID);
				return;
			}

			int copias = Integer.parseInt(_comboCopias.getText());

			PrintManager printManager = new PrintManager();

			try {

				// Comprobamos que el dispositivo esté funcionando correctamente
				boolean cancelStartPrint = false;
				boolean printerStatus = false;

				do {

					for (int i = 1; i < 3; i++) {
						if (!printerStatus) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							printerStatus = printManager.getStatus(getActivity().getApplicationContext(), _appConfig,
									false);
						}
					}

					Log.i("DepositManager", "Get Status: " + String.valueOf(printerStatus));

					if (!printerStatus)
						cancelStartPrint = _appConfig.getMessageBox().ShowWithResult("No se pudo iniciar la impresión",
								"No se pudo iniciar la impresión. Revise el dispositivo. Desea volverlo a intentar?",
								this.getActivity(), MessageBoxType.Information);
					else
						cancelStartPrint = false;
				} while (cancelStartPrint);

				if (!cancelStartPrint && printerStatus) {

					for (int i = 1; i <= copias; i++) {
						boolean result = false;
						boolean cancel = false;

						if (_deposito.isDeposito() && printDeposito) {
							do {
								result = printManager.printDeposito(_deposito, _appConfig, _appConfig, GUID);
								if (!result)
									cancel = _appConfig.getMessageBox().ShowWithResult("Impresión de depósito",
											"No se pudo imprimir el depósito. Desea volverlo a intentar?",
											this.getActivity(), MessageBoxType.Information);
							} while (cancel);

							if (!result) {
								closeOperation(GUID);
								printManager.Release();
								printManager = null;
								return;
							}

						}
						result = false;
						cancel = false;
						if (_deposito.isAlbaran()) {
							do {
								result = printManager.printAlbaran(_deposito, this.getActivity(), _appConfig, GUID);
								if (!result)
									cancel = _appConfig.getMessageBox().ShowWithResult("Impresión de depósito",
											"No se pudo imprimir el albarán. Desea volverlo a intentar?",
											this.getActivity(), MessageBoxType.Information);
							} while (cancel);
						}

						if (!result) {
							closeOperation(GUID);
							printManager.Release();
							printManager = null;
							return;
						}

					}
				}
			}

			finally {

			}

		}

		closeOperation(GUID);
	}

	private Historico SaveHistorico(Historico historico) throws Exception {

		historico.InitializePersistance(_appConfig, getActivity());
		historico.Cliente = _deposito.Cliente;
		historico.NombrePresentacion = _deposito.Nombre;
		historico.PoblacionPresentacion = _deposito.Poblacion;
		historico.CodigoPostalPresentacion = _deposito.CodigoPostal;

		_deposito.PagoDescripcion = _comboPago.getText();
		_deposito.Filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText());
		_deposito.Pagado = _checkPagado.isChecked();
		_deposito.formaPago = DepositManagerExtension.DataTier.getFormaPagoByDescripcion(_comboPago.getText(), this._appConfig);

		historico.Serie = Constants.EMPTY_STRING;

		if (_deposito.isAlbaran()) {
			if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig))
				_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
			else
				_deposito.Serie = _appConfig.getUser().SerialInvoiceB;

			historico.Serie = _deposito.Serie;

			Contador contador = new Contador();
			contador.InitializePersistance(_appConfig, getActivity());

			contador.getContadores();

			if (_deposito.Serie == _appConfig.getUser().SerialInvoiceA) {
				contador.ContadorSerieA = contador.ContadorSerieA + 1;
				_deposito.NumeroAlbaran = String.valueOf(contador.ContadorSerieA);
				historico.NumeroAlbaran = _deposito.NumeroAlbaran;
			} else {
				contador.ContadorSerieB = contador.ContadorSerieB + 1;
				_deposito.NumeroAlbaran = String.valueOf(contador.ContadorSerieB);
				historico.NumeroAlbaran = _deposito.NumeroAlbaran;
			}

			contador.update();
		}

		historico.Fecha = new Date();

		try {
			_deposito.Calculate();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e1);
		}

		historico.Total = _deposito.Totales.TotalBase;
		historico.CantidadPagada = _deposito.CantidadPagada;

		if (!_deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE) && !_deposito.Retirado)
			historico.Tipo = Constants.TIPO_HISTORICO_CLIENTE_EXISTENTE;
		else if (_deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE))
			historico.Tipo = Constants.TIPO_HISTORICO_CLIENTE_NUEVO;
		else if (_deposito.Retirado)
			historico.Tipo = Constants.TIPO_HISTORICO_CLIENTE_BAJA;
		
		// Serializamos el objeto deposito a JSON
		try {
			historico.Serializacion = _deposito.getDTO().serialize();
			Log.i("DepositManager", "Serializacion: " + historico.Serializacion);
			Log.i("DepositManager", "Total Lineas DTO: " + _deposito.getDTO().Lineas.size());
		} catch (Exception e) {
		}

		try {
			historico.save();
		} catch (Exception e) {
		}

		_appConfig.getWorkingArea().CurrentHistorico = historico;

		for (LineaDeposito linea : _deposito.Lineas.values()) {

			if (linea.UnidadesFacturadas > 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				try {
					lineaHistorico.InitializePersistance(_appConfig, getActivity());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = linea.UnidadesFacturadas;
				lineaHistorico.MovimientoStock = linea.UnidadesRepuestas;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_FACTURADAS;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}

			if (linea.UnidadesInicialesFijas == 0 && linea.UnidadesRepuestas > 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				lineaHistorico.InitializePersistance(_appConfig, getActivity());
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = linea.UnidadesRepuestas;
				lineaHistorico.MovimientoStock = linea.Articulo.MovimientoStock;
				lineaHistorico.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_POTENCIADAS;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}

			}

			if (!linea.IsNew && linea.UnidadesRepuestas == 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				lineaHistorico.InitializePersistance(_appConfig, getActivity());
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = linea.UnidadesInicialesFijas; // 
				lineaHistorico.MovimientoStock = 0;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_BAJAS;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
				}

			}

			if (linea.UnidadesDefectuosas > 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				lineaHistorico.InitializePersistance(_appConfig, getActivity());
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = linea.UnidadesDefectuosas; // 
				lineaHistorico.MovimientoStock = 0;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				// linea.UnidadesFacturadas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_DEFECTUOSAS;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}

			if (linea.Articulo.MovimientoStock != 0 || linea.Articulo.MovimientoStockDefectuosas != 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				lineaHistorico.InitializePersistance(_appConfig, getActivity());
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = 0;
				lineaHistorico.MovimientoStock = linea.Articulo.MovimientoStock;
				lineaHistorico.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_STOCK;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}

			if (linea.UnidadesAbono > 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				lineaHistorico.InitializePersistance(_appConfig, getActivity());
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = linea.UnidadesAbono * -1;
				lineaHistorico.MovimientoStock = 0;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_FACTURADAS;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}
			
			if (linea.UnidadesInicialesFijas > 0) {
				LineaHistorico lineaHistorico = new LineaHistorico();
				lineaHistorico.InitializePersistance(_appConfig, getActivity());
				lineaHistorico.Historico = historico;
				lineaHistorico.Articulo = linea.Articulo;
				lineaHistorico.Unidades = linea.UnidadesInicialesFijas;
				lineaHistorico.PVP = (float) linea.PVP;
				lineaHistorico.MovimientoStock = 
				lineaHistorico.MovimientoStockDefectuosas = 0;

				// linea.UnidadesFacturadas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_UNIDADES_INICIALES;

				try {
					lineaHistorico.save();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}

		}

		return historico;
	}

	private void closeOperation(String GUID) throws Exception {

		try {

			DepositManagerExtension.Documents.GeneratePdf(GUID, _deposito, _appConfig);
			DepositManagerExtension.Documents.GenerateAuthorization(GUID, _deposito, _appConfig);

			_appConfig.getMessageBox().Show("Cierre de operación", "La operación se ha cerrado correctamente.",
					this.getActivity(), MessageBoxType.Information);

			_appConfig.getWorkingArea().CurrentCliente = null;
			_appConfig.getWorkingArea().CurrentDeposito = null;
			_deposito = null;

			DepositManagerExtension.Documents.sendData(this.getActivity(), this._appConfig);

			if (_appConfig.getWorkingArea().TransferMode.equals(TransferMode.New))
				_appConfig.getWorkingArea().TransferMode = TransferMode.None;

		} catch (Exception ex) {
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(ex),
					getActivity(), MessageBoxType.Error);		}

	}


	private void resetHeader() {
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout6);
		mainLinearLayout.removeAllViews();
	}
	
	private void addLineHeader(LineaDeposito lineaDeposito, final LinearLayout layoutGrid) throws Exception {

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout6);
		mainLinearLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		int color = Color.WHITE;
		
		LabelColor codigoArticulo = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, 8, params, true);
		
		LabelColor articuloDescripcion = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.Descripcion.trim(), TEXT_SIZE, 12,
						params, true, lineaDeposito.Articulo);
		 
		final DepositManager that = this;

		articuloDescripcion.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				DepositManagerExtension.Dialogs.StartArticuloDialog((Articulo) ((LabelColor) v).getTag(), (Fragment) that);
			}
		});
		
		LabelColor unidadesInicialesFijas = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesInicialesFijas), TEXT_SIZE, 13, params, true, lineaDeposito);

		LabelColor unidadesIniciales = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesIniciales), TEXT_SIZE, 13, params, true, lineaDeposito);
		
		TextBoxColor unidadesDevueltas = DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT, 
				String.valueOf(lineaDeposito.UnidadesDevueltas), TEXT_SIZE, 100, params, true, lineaDeposito);


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
						Log.i("Text_Devueltas", String.valueOf(unidadesDevueltas));
						try {
							refreshLayout(layout, layoutGrid, lineaDeposito, false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getMessageBox().Show("Atención",
									_appConfig.getStackTrace(e),
									getActivity(), MessageBoxType.Error);						}

					}
				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});
		
		TextBoxColor unidadesDefectuosas = DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT, 
				String.valueOf(lineaDeposito.UnidadesDefectuosas), TEXT_SIZE, 100, params, true, lineaDeposito);

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
						Log.i("Text_Defectuosas", String.valueOf(unidadesDefectuosas));
						try {
							refreshLayout(layout, layoutGrid, lineaDeposito, false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getMessageBox().Show("Atención",
									_appConfig.getStackTrace(e),
									getActivity(), MessageBoxType.Error);						}

					}
				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});
		
		TextBoxColor unidadesFacturadas = DepositManagerExtension.UI.addEdit(getActivity(), Color.rgb(0, 128, 0), Gravity.LEFT, 
				String.valueOf(lineaDeposito.UnidadesFacturadas), TEXT_SIZE, 100, params, true, lineaDeposito);

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
							refreshLayout(layout, layoutGrid, lineaDeposito, false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getMessageBox().Show("Atención",
									_appConfig.getStackTrace(e),
									getActivity(), MessageBoxType.Error);						}
					}

				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});

		TextBoxColor unidadesRepuestas = DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT, 
				String.valueOf(lineaDeposito.UnidadesRepuestas), TEXT_SIZE, 100, params, true, lineaDeposito);

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
						refreshLayout(layout, layoutGrid, lineaDeposito, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);					}

				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});
		
		
		TextBoxColor pvp = DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT, 
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP), 
				TEXT_SIZE, 100, params, true, lineaDeposito);

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
						refreshLayout(layout, layoutGrid, lineaDeposito, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);					}
				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});
		
		TextBoxColor descuento1 = DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT, 
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.Descuento1), 
				TEXT_SIZE, 100, params, true, lineaDeposito);

		descuento1.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					_lastTextBox = (TextBoxColor) view;
					EditText textBox = (EditText) view;
					float descuento1;

					if (textBox.getText().toString().equals(Constants.EMPTY_STRING))
						descuento1 = Float.parseFloat(textBox.getHint().toString());
					else
						descuento1 = Float.parseFloat(textBox.getText().toString());

					if (descuento1 > 100) {
						_appConfig.getMessageBox().Show("Error", "El porcentage de descuento es incorrecto",
								getActivity(), MessageBoxType.Error);
					} else {

						((LineaDeposito) view.getTag()).Descuento1 = descuento1;

						LineaDeposito lineaDeposito = (LineaDeposito) view.getTag();
						try {
							refreshLayout(layout, layoutGrid, lineaDeposito, false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							_appConfig.getMessageBox().Show("Atención",
									_appConfig.getStackTrace(e),
									getActivity(), MessageBoxType.Error);						}
					}
				} else
					_lastTextBox = (TextBoxColor) view;
				
				
			}
		});
		
		TextBoxColor pvpAnterior = DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT, 
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior), 
				TEXT_SIZE, 100, params, true, lineaDeposito);

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
						refreshLayout(layout, layoutGrid, lineaDeposito, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);					}
				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});
		
		double total = (lineaDeposito.UnidadesFacturadas * lineaDeposito.PVP);
		LabelColor totalLinea = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(DepositManagerExtension.Format.round(total, 2)), TEXT_SIZE, 50, params, true, lineaDeposito);

		// Botón Abono
		
		ButtonColor modoAbono = DepositManagerExtension.UI.addButton(getActivity(), Color.GREEN, "ABONO", 
				10, 70, params, lineaDeposito);
		
		modoAbono.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				_abonoMode = true;
				LineaDeposito lineaDeposito = (LineaDeposito) arg0.getTag();
				try {
					refreshLayout(layout, layoutGrid, lineaDeposito, true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}
		});

		ArrayList<TextView> controls = new ArrayList<TextView>();
		controls.add(unidadesIniciales);
		controls.add(unidadesFacturadas);
		controls.add(totalLinea);
		controls.add(pvpAnterior);

		layout.setTag(controls);

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
		layout.addView(modoAbono);

		mainLinearLayout.addView(layout);

		_textBoxColorRequestFocus = unidadesDefectuosas;

	}

	private void addLineHeaderAbono(LineaDeposito lineaDeposito, final LinearLayout layoutGrid) throws Exception {

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.headerMainLinearLayout6);
		mainLinearLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		
		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		int color = Color.WHITE;

		LabelColor codigoArticulo = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, 8, params, true);
		
		
		LabelColor articuloDescripcion = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.Descripcion.trim() + " (Abono) ", TEXT_SIZE, 13,
						params, true, lineaDeposito.Articulo);
		final DepositManager that = this;
		articuloDescripcion.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				DepositManagerExtension.Dialogs.StartArticuloDialog((Articulo) ((LabelColor) v).getTag(), (Fragment) that);
			}
		});

		LabelColor labelCantidadAbono = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Cantidad", TEXT_SIZE, 100, params, true);
		
		TextBoxColor unidadesAbono = DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT, 
				String.valueOf(lineaDeposito.UnidadesAbono), TEXT_SIZE, 100, params, true, lineaDeposito);

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
						refreshLayout(layout, layoutGrid, lineaDeposito, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);					}

				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});

		LabelColor labelDefectuosasAbono = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Defect.", TEXT_SIZE, 100, params, true);
		
		TextBoxColor defectuosasAbono = DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT, 
				String.valueOf(lineaDeposito.DefectuosasAbono), TEXT_SIZE, 100, params, true, lineaDeposito);

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
						refreshLayout(layout, layoutGrid, lineaDeposito, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);					}

				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});
		
		LabelColor labelPVPAbono = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"PVP", TEXT_SIZE, 100, params, true);
		
		TextBoxColor pvpAbono = DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT, 
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAbono), TEXT_SIZE, 100, params, true, lineaDeposito);

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
						refreshLayout(layout, layoutGrid, lineaDeposito, false);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);					}
				} else
					_lastTextBox = (TextBoxColor) view;
			}
		});

		LabelColor labelTotalAbono = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Total", TEXT_SIZE, 100, params, true);

		double totalAbonoImporte = lineaDeposito.TotalAbono;
		
		LabelColor totalAbono = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.RIGHT, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(totalAbonoImporte), TEXT_SIZE, 100, params, true);
				
		// Botón Venta
		
		ButtonColor modoVenta = DepositManagerExtension.UI.addButton(getActivity(), Color.GREEN, "Venta", 
				10, 70, params, lineaDeposito);
		
		modoVenta.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				_abonoMode = false;
				LineaDeposito lineaDeposito = (LineaDeposito) arg0.getTag();
				try {
					refreshLayout(layout, layoutGrid, lineaDeposito, true);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);				}
			}
		});

		ArrayList<TextView> controls = new ArrayList<TextView>();
		controls.add(totalAbono);

		layout.setTag(controls);

		layout.setTag(controls);
		layout.addView(codigoArticulo);
		layout.addView(articuloDescripcion);
		layout.addView(labelCantidadAbono);
		layout.addView(unidadesAbono);
		layout.addView(labelDefectuosasAbono);
		layout.addView(defectuosasAbono);
		layout.addView(labelPVPAbono);
		layout.addView(pvpAbono);
		// layout.addView(labelTotalAbono);
		layout.addView(totalAbono);
		layout.addView(modoVenta);

		mainLinearLayout.addView(layout);

		_textBoxColorRequestFocus = defectuosasAbono;

	}

	private void resetLines() {

		TextView label = (TextView) getActivity().findViewById(R.id.lblCliente);
		label.setText(Constants.EMPTY_STRING);

		TextView label2 = (TextView) getActivity().findViewById(R.id.lblBase);
		label2.setText(Constants.EMPTY_STRING);

		TextView label3 = (TextView) getActivity().findViewById(R.id.lblTotalFactura);
		label3.setText(Constants.EMPTY_STRING);

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutDepositManager);
		mainLinearLayout.removeAllViews();
	}

	private void createHeaderLabels() {
		
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.headerMainLinearLayout5);
		
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

	private void addLine(final LineaDeposito lineaDeposito) throws Exception {

		lineaDeposito.InitializePersistance(_appConfig, getActivity());
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutDepositManager);
		
		this._articles.add(lineaDeposito.Articulo.Descripcion);

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.setOrientation(LinearLayout.HORIZONTAL);

		int color = lineaDeposito.IsNew ? Color.BLUE : Color.BLACK;
		
		//LabelColor codigoArticulo = DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
		//		lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, labelCodigoWidth - 30, params, true);
		
		LabelColor codigoArticulo = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.CodigoArticulo.trim(), TEXT_SIZE, 8, params, true);

		//LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		//params2.setMargins(0, 0, 10, 0);

		LabelColor articuloDescripcion = DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.Descripcion.trim(), TEXT_SIZE, 12, params, true, lineaDeposito.Articulo);

		LabelColor unidadesInicialesFijas = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesInicialesFijas), TEXT_SIZE, 13, params, true, lineaDeposito);

		lineaDeposito.UnidadesIniciales = lineaDeposito.UnidadesIniciales;
		LabelColor unidadesIniciales = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.UnidadesIniciales), TEXT_SIZE, 13, params, true, lineaDeposito);

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
		
		LabelColor descuento1 = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				String.valueOf(lineaDeposito.Descuento1), TEXT_SIZE, 13, params, true, lineaDeposito);

		LabelColor pvpAnterior = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior), TEXT_SIZE, 13, params, true, lineaDeposito);

		LabelColor totalLinea = DepositManagerExtension.UI.addLabelByText(_appConfig, Color.BLACK, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(0), TEXT_SIZE, 13, params, true, lineaDeposito);

		LabelColor conAbono = DepositManagerExtension.UI.addLabel(_appConfig, Color.RED, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				Constants.EMPTY_STRING, TEXT_SIZE, 100, params, true, lineaDeposito);

		ArrayList<LabelColor> controls = new ArrayList<LabelColor>();
		controls.add(unidadesIniciales);
		controls.add(unidadesDevueltas);
		controls.add(unidadesDefectuosas);
		controls.add(unidadesFacturadas);
		controls.add(unidadesRepuestas);
		controls.add(pvp);
		controls.add(totalLinea);
		controls.add(pvpAnterior);
		controls.add(conAbono);

		layout.setTag(controls);

		layout.addView(codigoArticulo);
		layout.addView(articuloDescripcion);
		layout.addView(unidadesInicialesFijas);
		layout.addView(unidadesDevueltas);
		layout.addView(unidadesDefectuosas);
		layout.addView(pvp);
		layout.addView(unidadesFacturadas);
		layout.addView(unidadesRepuestas);
		// layout.addView(unidadesIniciales);
		layout.addView(pvpAnterior);
		layout.addView(totalLinea);
		layout.addView(conAbono);

		layout.setClickable(true);
		layout.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				if (_lastTextBox != null)
					_lastTextBox.clearFocus();
				
				if (_lastSelectedLayout != null)
					_lastSelectedLayout.setBackgroundColor(Color.TRANSPARENT);
				//setTransparentLayouts();
				v.setBackgroundColor(Color.rgb(240,140,40));
				_lastSelectedLayout = (LinearLayout) v;

				if (_abonoMode)
					try {
						addLineHeaderAbono(lineaDeposito, (LinearLayout) v);
						refreshTotals();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				else
					try {
						addLineHeader(lineaDeposito, (LinearLayout) v);
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				//refreshTotals();
				}
		});

		mainLinearLayout.addView(layout);

	}

	private void setTransparentLayouts() {
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutDepositManager);

		for (int i = 0; i < mainLinearLayout.getChildCount(); i++) {
			mainLinearLayout.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
		}

	}

	private void refreshLayout(LinearLayout layout, LinearLayout layoutGrid, LineaDeposito linea, boolean redraw)
			throws Exception {

		if (redraw) {
			if (_abonoMode)
				addLineHeaderAbono(linea, (LinearLayout) layoutGrid);
			else
				addLineHeader(linea, (LinearLayout) layoutGrid);
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

		ArrayList<LabelColor> controlsGrid = (ArrayList<LabelColor>) layoutGrid.getTag();
		((LabelColor) controlsGrid.get(0)).setText(String.valueOf(linea.UnidadesIniciales));
		((LabelColor) controlsGrid.get(1)).setText(String.valueOf(linea.UnidadesDevueltas));

		
		
		((LabelColor) controlsGrid.get(2)).setText(String.valueOf(linea.UnidadesDefectuosas));

		((LabelColor) controlsGrid.get(3)).setText(String.valueOf(linea.UnidadesFacturadas));
		((LabelColor) controlsGrid.get(4)).setText(String.valueOf(linea.UnidadesRepuestas));
		((LabelColor) controlsGrid.get(5)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVP)));

		((LabelColor) controlsGrid.get(6)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(totalLinea)));
		((LabelColor) controlsGrid.get(7)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVPAnterior)));

		if (linea.TotalAbono < 0)
			((LabelColor) controlsGrid.get(8)).setText("Abono");
		else
			((LabelColor) controlsGrid.get(8)).setText(Constants.EMPTY_STRING);

		if (!_abonoMode) {

			ArrayList<TextView> controls = (ArrayList<TextView>) layout.getTag();
			if (controls.size() > 1) {
				((TextView) controls.get(0)).setText(String.valueOf(linea.UnidadesIniciales));
				((TextView) controls.get(1)).setText(String.valueOf(linea.UnidadesFacturadas));
				((TextView) controls.get(2)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(totalLinea)));
				((TextView) controls.get(3)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.PVPAnterior)));	
			}

		} else {
			ArrayList<TextView> controls = (ArrayList<TextView>) layout.getTag();

			((LabelColor) controls.get(0)).setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(linea.TotalAbono)));
		}


	}

	final private void CheckIfNewFiliacion(String filiacion) throws Exception {
		Cliente cliente = new Cliente();
		cliente.InitializePersistance(_appConfig, this.getActivity());

		cliente.setClienteByCodigo(_cliente.CodigoCliente);

		if (!cliente.Filiacion.equals(filiacion)) {
			cliente.Filiacion = filiacion;
			cliente.update();

			String text = "Datos de filiacion del cliente: " + Constants.NEW_LINE + Constants.NEW_LINE
					+ "CODIGO CLIENTE: " + _deposito.CodigoCliente + Constants.NEW_LINE + "NOMBRE DEL CLIENTE: "
					+ _deposito.Nombre + Constants.NEW_LINE + "CODIGO FILIACION: " + filiacion
					+ Constants.NEW_LINE + "NOMBRE FILIACION: " + _comboFiliacion.getText()
					+ Constants.NEW_LINE;

			Incidencia incidencia = new Incidencia(_appConfig.getUser().User, new Date(), IncidenciaType.Filiacion,
					text);
			incidencia.create();
		}
	}

	final private void refreshTotals() throws Exception {
		if (_deposito != null) {

			String filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText()).trim();

			_deposito.Cliente.Filiacion = filiacion;

			_deposito.InitializePersistance(_appConfig, _appConfig);
			_deposito.Calculate();

			if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
				TextView label = (TextView) getActivity().findViewById(R.id.lblBase);
				label.setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase)) + " €");

				TextView label2 = (TextView) getActivity().findViewById(R.id.lblTotalFactura);
				label2.setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.Total)) + " €");
			} else {
				TextView label = (TextView) getActivity().findViewById(R.id.lblBase);
				label.setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase)) + " €");

				TextView label2 = (TextView) getActivity().findViewById(R.id.lblTotalFactura);
				label2.setText(String.valueOf(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase)) + " €");
			}
		} else {
			TextView label = (TextView) getActivity().findViewById(R.id.lblBase);
			label.setText(Constants.EMPTY_STRING);

			TextView label2 = (TextView) getActivity().findViewById(R.id.lblTotalFactura);
			label2.setText(Constants.EMPTY_STRING);
		}

	}

		
	private void resetDeposit(boolean initializeAttributes, boolean callOnResume) {

		try {
			
			if (DepositManagerExtension.DataTier.RestriccionIngresos(this._appConfig, this.getActivity().getApplicationContext())) {

				_appConfig.getMessageBox().Show("Atención",
						"Ha superado los " + Constants.MAXIMO_SIN_INGRESAR
								+ " € pendientes de ingresar. Realice un ingreso para poder seguir trabajando",
						getActivity(), MessageBoxType.Error);

				return;

			}
			
		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		if (initializeAttributes) {
			_searched = false;
			_newDeposit = true;
			_appConfig.getWorkingArea().CurrentCliente = null;
			_appConfig.getWorkingArea().CurrentDeposito = null;

			if (callOnResume)
				onResume();

			_newDeposit = false;
		}

		resetHeader();
		resetLines();
		resetFooter();
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

}
