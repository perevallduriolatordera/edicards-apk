package net.ifeu.edicards;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.itextpdf.text.DocumentException;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEvents;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Efectivo;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.NTV.DepositoNTVDTO;
import net.ifeu.edicards.DataTier.TipoIVA;
import net.ifeu.edicards.DataTier.TransactionMetadata;
import net.ifeu.edicards.DataTier.TransferMode;
import net.ifeu.edicards.Html.notification.customer.HtmlCustomerNotification;
import net.ifeu.edicards.Html.notification.customer.ICustomerNotification;
import net.ifeu.edicards.Pdf.gdpr.PdfGDPR;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.ComboBox;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.IO.IOUtils;
import net.ifeu.library.LogBook.LogBookStock;
import net.ifeu.library.Mediator.IMediator;
import net.ifeu.library.Utils.MessageBox.AdvancedMessageBox;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.ScreenManager;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DepositManager extends Fragment implements  IMediator {

	private Deposito _deposito;
	private Cliente _cliente;
	private AppConfig _appConfig;
	private boolean _abonoMode = false;
	private ComboBox _comboPago;
	private ComboBox _comboCopias;
	private ComboBox _comboSerie;
	private ComboBox _comboFiliacion;
	private TextBoxColor _textBoxCantidadPagada;
	private CheckBox _checkPagado;
	private TextBoxColor _textBoxColorRequestFocus;
	private TextBoxColor _lastTextBox;
	private final LinkedList<String> _articles = new LinkedList<>();
	private AdvancedMessageBox _dialogDepositoModalidad;
	private LinearLayout _headerLayout;
	private LinearLayout _headerAbonoLayout;
	private LinearLayout _articlesLayout;
	private final int TEXT_SIZE = 14;
	private boolean _isRendered = false;
	private LinearLayout _mainLayout;
	private ListView _articlesListView;
	private final List<LineaDeposito> _currentLines = new ArrayList<>();
	private final List<LineaDeposito> _originalLines = new ArrayList<>();
	ArrayAdapter<LineaDeposito> _adapter;

	private DepositoModalidad _modalidad;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_appConfig = (AppConfig) this.getActivity().getApplicationContext();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return !_isRendered ?
				inflater.inflate(R.layout.activity_deposit_manager, container, false)
				: _mainLayout;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		_mainLayout = getActivity().findViewById(R.id.depositManagerLayout);
		_articlesLayout = this.getActivity()
				.findViewById(R.id.mainLinearLayoutArticles);

		_articlesListView = this.getActivity().findViewById(R.id.listViewArticles);
		showCustomerSearchDialog();

	}

	@Override
	public void onStop() {
		super.onStop();System.gc();
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
	}

	private void showCustomerSearchDialog() {
		_appConfig = (AppConfig) this.getActivity().getApplicationContext();

		if (_appConfig.getWorkingArea().CurrentCliente == null
				|| _appConfig.getWorkingArea().CurrentCliente.IdCliente == 0) {

			DepositManagerExtension.Dialogs.StartCustomerSearchDialog(this);
		}
	}

	private void createHeaderControls() throws Exception {

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 5, 0, 0);

		// Formas de pago

		params.weight = 1;
		LabelColor labelPago = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Forma de pago", TEXT_SIZE,  params);
		
		_comboPago = DepositManagerExtension.UI.addCombo(_appConfig, params, _appConfig.getCache().getAllFormasPagoList(), _deposito.Cliente.FormaPago.Descripcion);
		
		// Filiacion
		
		LabelColor labelFiliacion = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Filiación", TEXT_SIZE, params);

		
		TipoIVA iva = Factory.build(TipoIVA.class, _appConfig);
		_comboFiliacion = DepositManagerExtension.UI.addCombo(_appConfig, params, iva.getFiliaciones(), iva.getFiliacionByCode(_deposito.Cliente.Filiacion));

		// //Copias
		
		LabelColor labelCopias = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Copias", TEXT_SIZE, 90, params);
		List<String> copias = new ArrayList<>();
		for (int i=1; i < 6; i++) copias.add(String.valueOf(i));
		
		_comboCopias = DepositManagerExtension.UI.addCombo(_appConfig, params, copias, "1");
		
		// Series

		LabelColor labelSeries = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Series", TEXT_SIZE, 75, params);
		
		List<String> series = new ArrayList<>();
		series.add(_appConfig.getUser().SerialInvoiceA);
		series.add(_appConfig.getUser().SerialInvoiceB);

		_comboSerie = DepositManagerExtension.UI.addCombo(_appConfig, params, series, _appConfig.getUser().SerialInvoiceA);

		// //Pagado

		_checkPagado = new CheckBox(_appConfig);
		_checkPagado.setText("Pagado");
		_checkPagado.setTextSize(TEXT_SIZE);
		_checkPagado.setTextColor(Color.WHITE);
		_checkPagado.setLayoutParams(params);
		
		LabelColor labelCantidadPagada = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
				"Cantidad Pagada", TEXT_SIZE, 150, params);
		
		_textBoxCantidadPagada = DepositManagerExtension.UI.addEdit(getActivity(), Color.GREEN, Gravity.LEFT,
				"0", TEXT_SIZE, 75, params, false);

		// descuento 1
		
		LabelColor labelDescuento1 = DepositManagerExtension.UI.addLabel(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Dte. com.", TEXT_SIZE, 75, params);

		DecimalFormat dec = new DecimalFormat("0.00");

		TextBoxColor descuento1 = DepositManagerExtension.UI.addEdit(getActivity(), Color.WHITE, Gravity.LEFT,
				dec.format(_cliente.DescuentoProntoPago), TEXT_SIZE, 60, params, true);
		
		descuento1.setOnFocusChangeListener((view, hasFocus) -> {
			if (!hasFocus) {

				_lastTextBox = (TextBoxColor) view;
				EditText textBox = (EditText) view;
				float descuentoAplicar;

				if (textBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
					descuentoAplicar = Float.parseFloat(((EditText) view).getHint().toString().replace(",", "."));
				else
					descuentoAplicar = Float.parseFloat(((EditText) view).getText().toString().replace(",", "."));

				if (descuentoAplicar > 100) {
					_appConfig.getMessageBox().Show("Error", "El porcentage de descuento comercial es incorrecto",
							view.getContext(), MessageBoxType.Error);
				} else {

					if (_deposito != null)
						_deposito.DescuentoComercial = descuentoAplicar;
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
				"Dte. fin.", TEXT_SIZE, 75, params);

		TextBoxColor descuento2 = DepositManagerExtension.UI.addEdit(getActivity(), Color.WHITE, Gravity.LEFT,
				dec.format(_cliente.DescuentoFinanciero), TEXT_SIZE, 60, params, true);
	
		descuento2.setOnFocusChangeListener((view, hasFocus) -> {
			if (!hasFocus) {

				_lastTextBox = (TextBoxColor) view;

				EditText textBox = (EditText) view;
				float descuento21;

				if (textBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
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

			if (_deposito == null) return;
			_deposito.Pagado = isChecked;

			if (isChecked) {
				_textBoxCantidadPagada.setFocusableInTouchMode(true);
				_textBoxCantidadPagada.setText(String.valueOf(DepositManagerExtension.Format.RoundTo2Decimals(_deposito.Totales.Total)));
			}
			else {
				_textBoxCantidadPagada.setFocusable(false);
				_textBoxCantidadPagada.setText("");
			}
		});
		
		if (_deposito.Cliente.FormaPago != null && _deposito.Cliente.FormaPago.Descripcion != null) {
			if (_deposito.Cliente.FormaPago.Descripcion.trim().equalsIgnoreCase("CONTADO")) {
				this._checkPagado.setChecked(true);
				_deposito.CalculateDeposito();
				_textBoxCantidadPagada.setText(String.valueOf(DepositManagerExtension.Format.RoundTo2Decimals(_deposito.Totales.Total)));
				_deposito.Pagado = true;
			}
			else
			{
				this._checkPagado.setChecked(false);
				_deposito.Pagado = false;
			}
		}

		LinearLayout topLinearLayout = this.getActivity().findViewById(R.id.headerMainLinearLayout2);
		LinearLayout topLinearLayout2 = this.getActivity().findViewById(R.id.headerMainLinearLayout3);
		LinearLayout topLinearLayout3 = this.getActivity().findViewById(R.id.headerMainLinearLayout4);

		topLinearLayout.removeAllViews();
		topLinearLayout2.removeAllViews();
		topLinearLayout3.removeAllViews();

		DepositManagerExtension.UI.addViewsToLayout(topLinearLayout, labelPago, _comboPago, _checkPagado,
		labelCantidadPagada, _textBoxCantidadPagada, labelSeries, _comboSerie);

		DepositManagerExtension.UI.addViewsToLayout(topLinearLayout2, labelFiliacion, _comboFiliacion, labelCopias,
				_comboCopias, labelDescuento1, descuento1, labelDescuento2, descuento2);

		//DepositManagerExtension.UI.addViewsToLayout(topLinearLayout3, );

		this.addComboObservers();
	}

	private void addComboObservers() {

		_comboPago.addObserver("PAGADO", (String id, String text)-> {
			if (((CharSequence) text).toString().trim().equalsIgnoreCase("CONTADO")) {
				this._checkPagado.setChecked(true);
                try {
                    _deposito.CalculateDeposito();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                _textBoxCantidadPagada.setText(String.valueOf(DepositManagerExtension.Format.RoundTo2Decimals(_deposito.Totales.Total)));
				_deposito.Pagado = true;
			}
			else
			{
				this._checkPagado.setChecked(false);
				_deposito.Pagado = false;
			}
			_deposito.FormaPago = DepositManagerExtension.DataTier.getFormaPagoByDescripcion(_comboPago.getText(), this._appConfig);
			_deposito.PagoDescripcion = _comboPago.getText();
		});
		_comboSerie.addObserver("SERIE", (String id, String text) -> {
			try {

				if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
					_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
					_appConfig.getWorkingArea().CurrentDeposito.Serie = _appConfig.getUser().SerialInvoiceA;
				} else {
					_deposito.Serie = _appConfig.getUser().SerialInvoiceB;
					_appConfig.getWorkingArea().CurrentDeposito.Serie = _appConfig.getUser().SerialInvoiceB;
				}

				this.refreshTotals();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		_comboFiliacion.addObserver("FILIACION", (String id, String text) -> {
			try {
				this.refreshTotals();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void createHeaderButtons(boolean onlyNewDeposit) {

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(5, 0, 5, 0);
		
		LayoutParams params2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params2.setMargins(2, 2, 0, 0);

		// Nuevo Layout

		final LinearLayout layout = new LinearLayout(_appConfig);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setLayoutParams(params2);

		// Botón Datos

		int TEXT_SIZE_BUTTON = 12;
		ButtonColor datos = DepositManagerExtension.UI.addButton(getActivity(), Color.BLUE, "Cliente",
				TEXT_SIZE_BUTTON, ScreenManager.getViewWidthByLength(_appConfig, 20, TEXT_SIZE_BUTTON, Gravity.LEFT), params, getResources().getDrawable(R.drawable.ic_customer_data));
		
		final DepositManager that = this;
		datos.setOnClickListener(arg0 -> {

				if (_textBoxColorRequestFocus != null)
					_textBoxColorRequestFocus.requestFocus();

				try {
					DepositManagerExtension.Dialogs.StartCustomerDataDialog(that);
				} catch (Exception e) {
						throw new RuntimeException(e);
				}

		});
		
		// Botón Totales

		ButtonColor totales = DepositManagerExtension.UI.addButton(getActivity(), Color.WHITE, "Resumen",
				TEXT_SIZE_BUTTON, ScreenManager.getViewWidthByLength(_appConfig, 20, TEXT_SIZE_BUTTON, Gravity.LEFT), params, getResources().getDrawable(R.drawable.ic_totals));

		totales.setOnClickListener(arg0 -> {

			if (_textBoxColorRequestFocus != null)
				_textBoxColorRequestFocus.requestFocus();

			try {
				DepositManagerExtension.Dialogs.StartTotalesDialog(that);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		// Botón Albarán
		
		ButtonColor albaran = DepositManagerExtension.UI.addButton(getActivity(), Color.RED, "Cerrar operación",
				TEXT_SIZE_BUTTON, ScreenManager.getViewWidthByLength(_appConfig, 20, TEXT_SIZE_BUTTON, Gravity.LEFT), params, getResources().getDrawable(R.drawable.ic_save));

		albaran.setOnClickListener(arg0 -> {
			albaran.setVisibility(View.GONE);

			try {

				if (_textBoxColorRequestFocus != null)
					_textBoxColorRequestFocus.requestFocus();

				if (_deposito == null) {
					_appConfig.getMessageBox().Show("Atención",
							"No hay ningún depósito cargado",
							getActivity(), MessageBoxType.Error);

					albaran.setVisibility(View.VISIBLE);
					return;
				}

				//PVT : Versió MIREIA

				if(_cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE) &&
						!DepositManagerExtension.DataTier.isCustomerAttachedFilled(_appConfig)) {
					_appConfig.getMessageBox().Show("Advertencia",
							"Tiene que tomar fotos del DNI del cliente nuevo. Tome las fotos desde la ventana 'Cliente' y vuelva a cerrar la operación",
							this.getActivity(), MessageBoxType.Information);

					albaran.setVisibility(View.VISIBLE);
					return;
				}

				if (!DepositManagerExtension.DataTier.IsCustomerDataFilled(that._deposito)) {
					boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
							"El cliente NO está correctamente rellenado. Desea editarlo?", arg0.getContext(),
							MessageBoxType.Information);
					if (!result) {
						albaran.setVisibility(View.VISIBLE);
						return;
					} else {
						DepositManagerExtension.Dialogs.StartCustomerDataDialog(that);
						albaran.setVisibility(View.VISIBLE);
						return;
					}
				}

				if (_deposito.Serie.equals(_appConfig.getUser().SerialInvoiceA)
					&& !DepositManagerExtension.DataTier.IsCustomerEmailFilled(that._deposito)) {

					boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
							"El cliente NO tiene informada un cuenta de correo para que se le envíe la notificación del pedido. Desea editarlo?", arg0.getContext(),
							MessageBoxType.Information);
					if (!result) {
						albaran.setVisibility(View.VISIBLE);
						return;
					} else {
						DepositManagerExtension.Dialogs.StartCustomerDataDialog(that);
						albaran.setVisibility(View.VISIBLE);
						return;
					}
				}

				if (!this.GenerateOperation()) {
					albaran.setVisibility(View.VISIBLE);
				}

			} catch (Exception e) {
				_appConfig.getMessageBox().Show("Advertencia",
						"No se ha podido cerrar la operación debido a un error\n\n. Motivo: " + e.getMessage(),
						this.getActivity(), MessageBoxType.Error);
				throw new RuntimeException(e);
			}
		});

		// Botón Nuevo
		ButtonColor nuevo = DepositManagerExtension.UI.addButton(getActivity(), Color.MAGENTA, "Nuevo depósito",
				TEXT_SIZE_BUTTON, ScreenManager.getViewWidthByLength(_appConfig, 20, TEXT_SIZE_BUTTON, Gravity.LEFT), params, getResources().getDrawable(R.drawable.ic_new));

		nuevo.setTag("NUEVO");
		nuevo.setOnClickListener(arg0 -> {

			try {
				_appConfig.getWorkingArea().TransferMode = TransferMode.None;
				resetDepositData();
				showCustomerSearchDialog();

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		// Autocompletado para articulos

		AutoCompleteTextView searchArticulos = new AutoCompleteTextView(getContext());
		searchArticulos.setWidth(ScreenManager.getViewWidthByLength(_appConfig, 30, TEXT_SIZE, Gravity.LEFT));
		searchArticulos.setTextColor(getResources().getColor(R.color.Black));
		searchArticulos.setHint("nombre del artículo");
		searchArticulos.setThreshold(1);
		searchArticulos.setCompletionHint("Pulse el artículo que desea visualizar");
		this.createAutoComplete(searchArticulos);

		ButtonColor buttonSearchArticulos = DepositManagerExtension.UI.addButton(getActivity(), Color.RED, "",
				TEXT_SIZE_BUTTON, ScreenManager.getViewWidthByLength(_appConfig, 20, TEXT_SIZE_BUTTON, Gravity.LEFT), params, getResources().getDrawable(R.drawable.ic_view_all));

		buttonSearchArticulos.setOnClickListener( v-> {
			_currentLines.clear();
			_currentLines.addAll(_originalLines);
			_adapter.notifyDataSetChanged();
		});

		if (!onlyNewDeposit)
			DepositManagerExtension.UI.addViewsToLayout(layout, datos, totales, albaran, nuevo, searchArticulos, buttonSearchArticulos);
		else
			DepositManagerExtension.UI.addViewsToLayout(layout, nuevo);

		LinearLayout footerLinearLayout = this.getActivity().findViewById(R.id.headerButtonsLinearLayout);
		footerLinearLayout.removeAllViews();
		footerLinearLayout.setOrientation(LinearLayout.VERTICAL);

		footerLinearLayout.addView(layout);

	}

	private void prepareScreenRegions(boolean visible) {
		this.showHeader(visible);
		this.showArticlesGrid(visible);
		this.showButtonBar(visible);
	}

	private void CreateDepositView(DepositoNTVDTO depositoNTVDTO) throws Exception {

		_appConfig.getWorkingArea().CurrentTransactionMetadata = new TransactionMetadata();

		if (DepositManagerExtension.DataTier.RestriccionIngresosDiaria(_appConfig)) {
			_appConfig.getWorkingArea().IsIngresoDiarioVoluntario = false;
			DepositManagerExtension.Dialogs.StartIngresoDiarioDialog(this);
		}

		this.prepareScreenRegions(true);

		Cliente cliente = _appConfig.getWorkingArea().CurrentCliente;
		_cliente = _appConfig.getWorkingArea().CurrentCliente;

		TextView labelCliente = getActivity().findViewById(R.id.lblCliente);
		labelCliente.setText(cliente.Nombre + " - " + cliente.NIF);

		TextView labelBaseFactura = getActivity().findViewById(R.id.lblBase);
		labelBaseFactura.setText("0 €");

		TextView labelTotalFactura = getActivity().findViewById(R.id.lblTotalFactura);
		labelTotalFactura.setText("0 €");

		try {

			// Busquem el número de dipòsits del client

			Deposito deposito = Factory.build(Deposito.class, _appConfig);

			try {

				boolean isNTV = _deposito != null && _deposito.IsNtvDeposit;
				ArrayList<Deposito> depositos = deposito
						.getDepositosByCodigoCliente(String.valueOf(cliente.CodigoCliente));

				if (_cliente != null && _cliente.CodigoCliente != null) {
					if  (_cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {

						if (!_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)) {
							_appConfig.getMessageBox().Show("Información", "Se va a proceder a crear un cliente nuevo.",
									this.getActivity(), MessageBoxType.Information);

							_deposito = Factory.build(Deposito.class, _appConfig);
							_deposito.ClienteInfo = Factory.build(ClienteInfo.class, _appConfig);
							_deposito.assingFromCliente(_cliente);
							_deposito.Lineas.clear();
							_deposito.IsNtvDeposit = isNTV;
							this.addPotentialArticles();

						} else {
							_appConfig.getMessageBox().Show("Advertencia",
									"No se puede hacer un traspaso de un cliente nuevo. La operación va a ser cancelada",
									this.getActivity(), MessageBoxType.Information);

							_appConfig.getWorkingArea().TransferMode = TransferMode.None;

							return;
						}
					}
				}

				if (depositos.size() == 0 && !_cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {

					if (!_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)) {

						_deposito = Factory.build(Deposito.class, _appConfig);
						_deposito.ClienteInfo = Factory.build(ClienteInfo.class, _appConfig);
						_deposito.assingFromCliente(_cliente);
						_deposito.IsNtvDeposit = isNTV;

						this.addPotentialArticles();

						if (!_deposito.IsNtvDeposit) {
							_appConfig.getMessageBox().Show("Información",
									"El cliente " + cliente.Nombre
											+ " no tiene ningún depósito. Se va a proceder a crear uno de nuevo",
									this.getActivity(), MessageBoxType.Information);
						}
					} else {
						_appConfig.getMessageBox().Show("Advertencia",
								"No se puede hacer un traspaso de un cliente sin depósito asignado. La operación va a ser cancelada",
								this.getActivity(), MessageBoxType.Information);

						_appConfig.getWorkingArea().TransferMode = TransferMode.None;

						return;
					}

				} else if (depositos.size() > 0 && !_cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {
					_deposito = Factory.build(Deposito.class, _appConfig);
					_deposito.setFirstDepositoByCliente(cliente.CodigoCliente);
					_deposito.assingFromCliente(_cliente);
					_deposito.IsNtvDeposit = isNTV;
					this.addPotentialArticles();
				}
			//} else {
				try {
					if (depositoNTVDTO != null)
						this.addNTVArticles(depositoNTVDTO);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				//}
				_deposito.Serie = _appConfig.getUser().SerialInvoiceA;
				_appConfig.getWorkingArea().CurrentDeposito = _deposito;
				_appConfig.getWorkingArea().CurrentDeposito.DatosFiscalesUpdated = false;

			} catch (Exception e1) {
				throw new RuntimeException(e1);
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		_currentLines.addAll(DepositManagerExtension.DataTier.getSortedLineasDeposito(_deposito, depositoNTVDTO != null));
		_originalLines.addAll(_currentLines);

		for (LineaDeposito lineaDeposito : _originalLines)
			_articles.add(lineaDeposito.Articulo.Descripcion.trim().toUpperCase());

		this.createHeaderLabels();
		createHeaderControls();
		this.createHeaderButtons(false);

		_adapter = new ArrayAdapter<LineaDeposito>(_appConfig, R.layout.list_item_deposit_article, _currentLines) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {

				LayoutInflater layoutInflater = (LayoutInflater) _appConfig.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				if(convertView == null) {
					convertView = Objects.requireNonNull(layoutInflater).inflate(R.layout.list_item_deposit_article, parent, false);
				}

				LineaDeposito lineaDeposito = getItem(position);

				int color = Objects.requireNonNull(lineaDeposito).IsNew ? Color.BLUE : Color.BLACK;
				color = Objects.requireNonNull(lineaDeposito).IsNtvLine ? Color.MAGENTA : color;

				TextView itemTextView = convertView.findViewById(R.id.itemCodigoArticulo);
				itemTextView.setText(lineaDeposito.Articulo.CodigoArticulo);
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 8));

				itemTextView = convertView.findViewById(R.id.itemDescripcionArticulo);
				itemTextView.setText(lineaDeposito.Articulo.Descripcion.length() > 25 ? lineaDeposito.Articulo.Descripcion.substring(0, 24) + "..." : lineaDeposito.Articulo.Descripcion);
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 15));

				itemTextView = convertView.findViewById(R.id.itemUnidadesInicial);
				itemTextView.setText(String.valueOf(lineaDeposito.UnidadesIniciales));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 13));

				itemTextView = convertView.findViewById(R.id.itemUnidadesContadas);
				itemTextView.setText(String.valueOf(lineaDeposito.UnidadesDevueltas));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 11));

				itemTextView = convertView.findViewById(R.id.itemUnidadesRecicladas);
				itemTextView.setText(String.valueOf(lineaDeposito.UnidadesDefectuosas));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 11));

				if (lineaDeposito.PVPAbono == 0)
					lineaDeposito.PVPAbono = lineaDeposito.PVP;

				itemTextView = convertView.findViewById(R.id.itemPVP);
				itemTextView.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 11));

				itemTextView = convertView.findViewById(R.id.itemUnidadesFacturadas);
				itemTextView.setText(String.valueOf(lineaDeposito.UnidadesFacturadas));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 11));

				itemTextView = convertView.findViewById(R.id.itemUnidadesRepuestas);
				itemTextView.setText(String.valueOf(lineaDeposito.UnidadesRepuestas));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 11));

				itemTextView = convertView.findViewById(R.id.itemPVPPost);
				itemTextView.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior));
				itemTextView.setTextColor(color);
				itemTextView.setWidth(DepositManagerExtension.UI.getWidthOfEditText(_appConfig, TEXT_SIZE, 18));

				ImageView itemImageView = convertView.findViewById(R.id.itemImage);
				if (lineaDeposito.TotalAbono < 0)
					itemImageView.setImageResource(R.drawable.abono);
				else
				if (lineaDeposito.Articulo.StockPropio)
					itemImageView.setImageResource(R.drawable.stock_ok_png);
				else
					itemImageView.setImageResource(R.drawable.stock_ko_png);

				if (lineaDeposito.IsSelected)
					convertView.setBackgroundColor(Color.GRAY);
				else
					convertView.setBackgroundColor(Color.TRANSPARENT);

				return convertView;
			}
		};

		_articlesListView.setOnItemClickListener((parent, view, position, id) -> {

			for (LineaDeposito linea : _currentLines)
				linea.IsSelected = false;

			// Handle item click here
			LineaDeposito lineaDeposito = (LineaDeposito) parent.getItemAtPosition(position);
			lineaDeposito.IsSelected = true;

			if (_lastTextBox != null)
				_lastTextBox.clearFocus();

			try {
				if (_abonoMode)
					addLineHeaderAbono(lineaDeposito);
				else
					addLineHeader(lineaDeposito);

				refreshTotals();

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			_adapter.notifyDataSetChanged();
		});

		_articlesListView.setAdapter(_adapter);
		_isRendered = true;
	}
	
	private void createAutoComplete(AutoCompleteTextView autocomplete) {

		autocomplete.addTextChangedListener(new DepositManagerTextWatcher());
		ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_dropdown_item_1line, this._articles);
                
        autocomplete.setAdapter(adapter);
		autocomplete.setOnItemClickListener((listView, view, position, id) -> {

			String selectedArticle =  listView.getItemAtPosition(position).toString().trim().toUpperCase();
			_currentLines.clear();

			for (LineaDeposito lineaDeposito : _originalLines) {
				if (lineaDeposito.Articulo.Descripcion.trim().toUpperCase().equals(selectedArticle))
					_currentLines.add(lineaDeposito);
			}

			_adapter.notifyDataSetChanged();
			autocomplete.setText(ConstantsTypes.EMPTY_STRING);
		});
	}
	
	private void addPotentialArticles() throws Exception {
		// Buscamos los artículos que no están asociados al depósito

		LinkedHashMap<String, Articulo> articulos = _appConfig.getCache().getAllArticulos();

		for (Articulo articuloInCatalgo : articulos.values())
			try {

				if (_deposito.Lineas.containsKey(articuloInCatalgo.CodigoArticulo))
					continue;

				LineaDeposito linea = Factory.build(LineaDeposito.class, _appConfig);
				linea.Articulo = articuloInCatalgo;
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

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	private void addNTVArticles(DepositoNTVDTO ntvDepositoDTO) throws Exception {
		// Buscamos los artículos que no están asociados al depósito

		LinkedHashMap<String, Articulo> articulos = _appConfig.getCache().getAllArticulos();

		for (Articulo articuloInCatalgo : articulos.values())
			try {

				boolean isNtv = ntvDepositoDTO.Lineas.containsKey(articuloInCatalgo.CodigoArticulo);
				if (!isNtv)
					continue;

				LineaDeposito linea = _deposito.Lineas.get(String.valueOf(articuloInCatalgo.CodigoArticulo));
				linea.PVP = ntvDepositoDTO.Lineas.get(articuloInCatalgo.CodigoArticulo).precio;

				linea.UnidadesFacturadas = ntvDepositoDTO.Lineas.get(articuloInCatalgo.CodigoArticulo).cantidad;
				linea.UnidadesInicialesFijas = 0;
				linea.PVPAnterior = linea.PVP;
				linea.PVPInicial = linea.PVPAnterior;

				linea.Descuento1 = ntvDepositoDTO.Lineas.get(articuloInCatalgo.CodigoArticulo).dto1;
				linea.IsNew = true;
				linea.IsNtvLine = true;

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}

	private void updateEfectivo() {
		Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
        try {
            efectivo.getEfectivo();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

		efectivo.Efectivo = efectivo.Efectivo + _deposito.CantidadPagada;
		efectivo.UpdateDateEfectivo = new Date();

        try {
            efectivo.update();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showCashStatus() {
        try {
            Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
            efectivo.getEfectivo();
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.getDefault());
            String lastUpdateTime = timeFormat.format(efectivo.UpdateDateEfectivo);
            
            String message = String.format(
                "INFORMACIÓN DE EFECTIVO:\n\n" +
                "💰 Cantidad actual: %.2f €\n\n" +
                "🕒 Última actualización: %s",
                efectivo.Efectivo,
                lastUpdateTime
            );
            
            _appConfig.getMessageBox().Show("Estado del Efectivo", message, this.getActivity(), MessageBoxType.Information);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	private void SaveDeposito() {
		_deposito.PagoDescripcion = _comboPago.getText();
		_deposito.saveChangesToDeposito(_modalidad);

		if (_deposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {
			_deposito.IdDeposito = DepositManagerExtension.DataTier.assignDepositoToNuevoCliente(_deposito, _appConfig);
		}
	}

	private boolean GenerateOperation() throws Exception {

		String GUID;

		double cantidadPagada;
		String cantidadPagadaText = _textBoxCantidadPagada.getText().toString();

		if (_textBoxCantidadPagada.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
			cantidadPagada = 0;
		else
			cantidadPagada = Double.parseDouble(cantidadPagadaText.replace(",", "."));

		_deposito.CantidadPagada = cantidadPagada;

		if (_deposito.Totales.Total < 0 && cantidadPagada > 0) {
			_appConfig.getMessageBox().Show("Atención",
					"no se puede ingresar cantidad positiva en devolución. Revíselo y vuelva a cerrer la operación",
					this.getActivity(), MessageBoxType.Error);

			return false;
		}

		if ((_checkPagado.isChecked())
				&& (DepositManagerExtension.Format.RoundTo2Decimals(_deposito.CantidadPagada) > DepositManagerExtension.Format.RoundTo2Decimals(_deposito.Totales.Total))
				&& (_deposito.Totales.Total > 0)) {
			_appConfig.getMessageBox().Show("Atención",
					"La cantidad pagada no puede ser superior al total de la factura. Revíselo y vuelva a cerrer la operación",
					this.getActivity(), MessageBoxType.Error);

			_textBoxCantidadPagada.requestFocus();

			return false;

		} else if ((!_checkPagado.isChecked())) {
			_deposito.CantidadPagada = 0;
		}

		boolean close = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
				"Se va a proceder a cerrar la operación. Desea Continuar?", this.getActivity(),
				MessageBoxType.Information);
		if (!close)
			return false;

		_deposito.Retirado = false;

		if (_appConfig.getWorkingArea().TransferMode.equals(TransferMode.Old)
				&& !_deposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {
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
				if (result) _deposito.RetirarDeposito();
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
				} while (motivo.trim().equals(ConstantsTypes.EMPTY_STRING));
				
				_deposito.MotivoRetirado = motivo;

				// Generamos la incidencia de baja de cliente

				DepositManagerExtension.Incidencias.createIncidenciaBajaCliente(_appConfig, _deposito);

				XmlCreator creator = new XmlCreator(_appConfig);
				creator.createXmlDeposito(_deposito);
			}

			Map<String, LineaDeposito> processed =new HashMap<>();
			
			for (LineaDeposito linea : _deposito.Lineas.values()) {

				if (!processed.containsKey(linea.Articulo.CodigoArticulo)) {

					int stockInicial = linea.Articulo.Stock;
					linea.calculateStock();

					if (linea.UnidadesInicialesFijas > 0) {
						String codigoArticulo = linea.Articulo.CodigoArticulo;
						LineaDeposito logBookLinea = new LineaDeposito();
						logBookLinea.Articulo = new Articulo();
						logBookLinea.Articulo.CodigoArticulo = codigoArticulo;
						logBookLinea.Articulo.Descripcion = linea.Articulo.Descripcion;
						logBookLinea.Articulo.Stock = stockInicial;
						logBookLinea.UnidadesInicialesFijas = linea.UnidadesInicialesFijas;
						logBookLinea.UnidadesDevueltas = linea.UnidadesDevueltas;
						logBookLinea.UnidadesDefectuosas = linea.UnidadesDefectuosas;
						logBookLinea.UnidadesRepuestas = linea.UnidadesRepuestas;
						logBookLinea.UnidadesFacturadas = linea.UnidadesFacturadas;
						logBookLinea.UnidadesAbono = linea.UnidadesAbono;
							
						LogBookStock logBookTrace = Factory.build(LogBookStock.class, _appConfig);
						logBookTrace.setData("DEPOSITO RETIRADO", _deposito.Cliente.CodigoCliente,
								_deposito.Cliente.Razon, codigoArticulo, linea.Articulo.Descripcion,
								stockInicial, linea.Articulo.Stock, logBookLinea.UnidadesDevueltas, logBookLinea.UnidadesDefectuosas,
								logBookLinea.UnidadesRepuestas, logBookLinea.UnidadesFacturadas, logBookLinea.UnidadesInicialesFijas,
								logBookLinea.UnidadesAbono, logBookLinea.UnidadesDefectuosas);

						logBookTrace.save();
					}
					linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.UnidadesDefectuosas;
					linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
							+ linea.UnidadesDefectuosas;

					linea.Articulo.Activo = true;
					linea.Articulo.update();
				}

				processed.put(linea.Articulo.CodigoArticulo, linea);
			}
			_deposito.DeleteAllLines();
		}

		if (_deposito.isDeposito() && (!_deposito.isDepositoUpdated() || _deposito.isDepositoUpdatedOnlyVentaDirecta())) {
			boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
					"NO se ha modificado el depósito. Desea Continuar?", this.getActivity(),
					MessageBoxType.Information);
			if (!result)
				return false;
		}

		if (!_deposito.isAlbaran()) {
			boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
					"La operación realizada NO generará factura. Desea Continuar?", this.getActivity(),
					MessageBoxType.Information);
			if (!result)
				return false;
		}

		if (_deposito.isDeposito() || _deposito.isAlbaran() || _deposito.isDepositoRetirado()) {

			Historico historico = Factory.build(Historico.class, _appConfig);
			GUID = historico.GUID;
			_appConfig.getWorkingArea().CurrentHistorico = historico;

			_deposito.Cliente.CheckIfNewFiliacion(_deposito.Filiacion, _deposito.Cliente.CodigoCliente);

			if (_deposito.isDeposito() || _deposito.isAlbaran() || _deposito.isDepositoRetirado()) {
				DepositManagerExtension.Dialogs.StartSignatureCustomerDialog(this);
				
				boolean result = _appConfig.getMessageBox().ShowWithResult("Cierre de operación",
						"Se va a proceder a guardar los datos. Si sigue adelante, el depósito y/o el albarán ya no podrán ser modificados. Desea Continuar?",
						this.getActivity(), MessageBoxType.Information);

				if (!result)
					return false;

				SaveDeposito();
				SaveHistorico();

				if (_deposito.Pagado && _deposito.CantidadPagada != 0)
					updateEfectivo();

				generateXML();

				// Mostrar estado actual del efectivo después de completar la operación
				showCashStatus();

				// Generem el consentiment GDPR si és necessari

				if (!_deposito.Cliente.hasGDPRSigned()) {

					PdfGDPR gdpr = new PdfGDPR(this._deposito.Cliente, GUID, this._appConfig);
					if (gdpr.createGDPR())
						_deposito.Cliente.setGDPRSigned();
					else
						_appConfig.getMessageBox().Show("Cierre de operación",
								"Se ha producido un error al generar el documento GDPR. Contacte con el servicio técnico",
								this.getActivity(), MessageBoxType.Error);

					DepositManagerExtension.Documents.GenerateAuthorization(GUID, _deposito, _appConfig);
				}

				if (_deposito.isAlbaran() && _deposito.Pagado) {
					DepositManagerExtension.Dialogs.StartSignatureVendorDialog(this);
				} else {
					_appConfig.getMediator().notify(ConstantsEvents.EVENT_CLOSE_OPERATION, GUID);
				}

			}
		}

		return true;
	}

	private void SaveHistorico() {
		_deposito.Cliente.Filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText()).trim();
        try {
            _deposito.Cliente.update();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        _deposito.Filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText());
		_deposito.Pagado = _checkPagado.isChecked();
		_deposito.PagoDescripcion = _comboPago.getText();

		if (_deposito.isAlbaran()) {
			Contador contador = Factory.build(Contador.class, _appConfig);
			_deposito.NumeroAlbaran = contador.updateContador(_deposito, _appConfig.getUser());
		}
		_appConfig.getWorkingArea().CurrentHistorico.saveChangesToHistorico(_deposito, _modalidad);
	}

	private void generateXML() {

		try {
			XmlCreator creator = new XmlCreator(_appConfig);
			creator.createXmlArticulos();

			if (_deposito.isAlbaran())
				creator.createXmlAlbaran(_deposito);

			if (_deposito.isDeposito() || (!_deposito.isDeposito() && _deposito.isDepositoUpdated()))
				creator.createXmlDeposito(_deposito);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void printDeposito(String GUID) {
		boolean printDeposito = true;

		if (!_deposito.isDepositoUpdated() || _deposito.isDepositoUpdatedOnlyVentaDirecta()) {
			printDeposito = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"El depósito no ha sido modificado. Desea imprimirlo de todos modos ?", this.getActivity(),
					MessageBoxType.Information);
		}

		if (_deposito.IsNtvDeposit) {
			printDeposito = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"El depósito es una importación de la app de NTV y no ha sido modificado. Desea imprimirlo de todos modos ?", this.getActivity(),
					MessageBoxType.Information);
		}

		if (_deposito.isDeposito() || _deposito.isAlbaran()) {

			try {
				DepositManagerExtension.Documents.GeneratePdf(GUID, _deposito, _modalidad, _appConfig);
			} catch (IOException e) {
				_appConfig.getMessageBox().Show("Impresión de documentos",
						"NO SE HA PODIDO GENERAR LOS DOCUMENTOS PDF. Revise el dispositivo. Motivo: " + e.getMessage(),
						this.getActivity(), MessageBoxType.Error);
			} catch (DocumentException e) {
				_appConfig.getMessageBox().Show("Impresión de documentos",
						"NO SE HA PODIDO GENERAR LOS DOCUMENTOS PDF. Revise el dispositivo. Motivo: " + e.getMessage(),
						this.getActivity(), MessageBoxType.Error);
			}

			boolean resultImp = _appConfig.getMessageBox().ShowWithResult("Impresión de documentos",
					"Desea iniciar la impresión ?", this.getActivity(), MessageBoxType.Information);
			if (!resultImp) return;

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
								result = printManager.printDeposito(_deposito, _appConfig, _appConfig, GUID, _modalidad);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
							if (!result)
								cancel = _appConfig.getMessageBox().ShowWithResult("Impresión de depósito",
										"No se pudo imprimir el depósito. Desea volverlo a intentar?",
										this.getActivity(), MessageBoxType.Information);
						} while (cancel);

						if (!result) {
							printManager.Release();
							return;
						}

					}
					result = false;
					if (_deposito.isAlbaran()) {
						do {
							try {
								result = printManager.printAlbaran(_deposito, this.getActivity(), _appConfig, GUID,
										DepositManagerExtension.DataTier.isTransferPayment(_deposito.FormaPago), _modalidad);
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
						printManager.Release();
						return;
					}

				}
			}
		}
	}
	private void closeOperation() {

		try {

			if (_deposito.Serie.equals(_appConfig.getUser().SerialInvoiceA)) {
				if (DepositManagerExtension.DataTier.IsCustomerEmailFilled(_appConfig.getWorkingArea().CurrentDeposito)) {
					_deposito.Cliente.Mail = _appConfig.getWorkingArea().CurrentDeposito.Mail;
					ICustomerNotification<Deposito> notification = new HtmlCustomerNotification();
					notification.notify(_deposito, _appConfig);
				}
			}

			DepositManagerExtension.Documents.sendData(this.getActivity(), this._appConfig);

			_appConfig.getMessageBox().Show("Cierre de operación", "La operación se ha cerrado correctamente.",
					this.getActivity(), MessageBoxType.Information);

			if (_deposito.IsNtvDeposit) {
				IOUtils.deleteFilesFromDirectory("/sdcard/" + ConstantsFolders.FOLDER_NTV_IMPORT);
			}

			if (_appConfig.getWorkingArea().TransferMode.equals(TransferMode.New))
				_appConfig.getWorkingArea().TransferMode = TransferMode.None;

			_appConfig.getWorkingArea().invalidateDepositData();
			_deposito = null;
			_appConfig.getCache().invalidate();
			_appConfig.getMediator().notify(ConstantsEvents.EVENT_STOCK_CHANGED, null);
			_appConfig.getMediator().notify(ConstantsEvents.EVENT_DEPOSIT_CLOSED, null);
			this.prepareScreenRegions(false);

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void resetHeaders() {
		LinearLayout lineHeader = this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		lineHeader.removeAllViews();

		LinearLayout abonoLineHader = this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		abonoLineHader.removeAllViews();

		_headerLayout = null;
		_headerAbonoLayout = null;
	}

	private void addLineHeader(LineaDeposito lineaDeposito) {

		final DepositManager that = this;

		LinearLayout mainLinearLayout = this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		mainLinearLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		boolean createHeaderLayout = (_headerLayout == null) || (_headerLayout.getChildCount() == 0);
		int color = Color.WHITE;

		if (createHeaderLayout) {
			_headerLayout = new LinearLayout(_appConfig);
			_headerLayout.setOrientation(LinearLayout.HORIZONTAL);
		} else {
			_headerLayout.getChildAt(1).setOnClickListener(null);
			for (int i=3; i<9; i++) _headerLayout.setOnFocusChangeListener(null);
		}

		try {
			LabelColor codigoArticulo = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
					lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, 8, params, true)
					: (LabelColor) _headerLayout.getChildAt(0);
			codigoArticulo.setText(lineaDeposito.Articulo.CodigoArticulo);

			LabelColor articuloDescripcion = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
					lineaDeposito.Articulo.Descripcion.length() > 25 ? lineaDeposito.Articulo.Descripcion.substring(0, 24) + "..." : lineaDeposito.Articulo.Descripcion, TEXT_SIZE, 12,
					params, true, lineaDeposito.Articulo)
					: (LabelColor) _headerLayout.getChildAt(1);
			articuloDescripcion.setText(lineaDeposito.Articulo.Descripcion.length() > 25 ? lineaDeposito.Articulo.Descripcion.substring(0, 24) + "..." : lineaDeposito.Articulo.Descripcion);
			articuloDescripcion.setTag(lineaDeposito.Articulo);

			articuloDescripcion.setOnClickListener(v -> DepositManagerExtension.Dialogs.StartArticuloDialog((Articulo) v.getTag(), that));

			LabelColor unidadesInicialesFijas = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
					String.valueOf(lineaDeposito.UnidadesInicialesFijas), TEXT_SIZE, 13, params, true, lineaDeposito)
					: (LabelColor) _headerLayout.getChildAt(2);
			unidadesInicialesFijas.setText(String.valueOf(lineaDeposito.UnidadesInicialesFijas));
			unidadesInicialesFijas.setTag(lineaDeposito);

			TextBoxColor unidadesDevueltas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesDevueltas), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 11, TEXT_SIZE, Gravity.CENTER ), params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(3);
			unidadesDevueltas.setText(String.valueOf(lineaDeposito.UnidadesDevueltas));
			unidadesDevueltas.setTag(lineaDeposito);

			unidadesDevueltas.setOnFocusChangeListener((view, hasFocus) -> {

 				_lastTextBox = (TextBoxColor) view;
				if (!hasFocus) {
					int unidadesDevueltasValue;

					if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						unidadesDevueltasValue = NumberUtils.toInt(_lastTextBox.getHint().toString(), 0);
					else
						unidadesDevueltasValue = NumberUtils.toInt(_lastTextBox.getText().toString(), 0);

					LineaDeposito lineaDepositoValue = (LineaDeposito) _lastTextBox.getTag();
					if ((unidadesDevueltasValue) > lineaDepositoValue.UnidadesInicialesFijas) {
						_appConfig.getMessageBox()
								.Show("Atención",
										"La cantidad devuelta no puede ser superior a la inicial en el artículo "
												+ lineaDepositoValue.Articulo.Descripcion,
										getActivity(), MessageBoxType.Error);

						unidadesDevueltasValue = lineaDepositoValue.UnidadesInicialesFijas;
						_lastTextBox.setText(String.valueOf(unidadesDevueltasValue));
						lineaDepositoValue.UnidadesDevueltas = unidadesDevueltasValue;
					} else {
						lineaDepositoValue.UnidadesDevueltas = unidadesDevueltasValue;
						try {
							refreshLayout(lineaDepositoValue, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				} else {
					_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
					_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
				}

			});

			TextBoxColor unidadesDefectuosas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesDefectuosas), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 11, TEXT_SIZE, Gravity.CENTER), params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(4);
			unidadesDefectuosas.setText(String.valueOf(lineaDeposito.UnidadesDefectuosas));
			unidadesDefectuosas.setTag(lineaDeposito);

			unidadesDefectuosas.setEnabled(false);
			unidadesDefectuosas.setInputType(InputType.TYPE_NULL);

			unidadesDefectuosas.setOnFocusChangeListener((view, hasFocus) -> {

				_lastTextBox = (TextBoxColor) view;
				if (!hasFocus) {

					int unidadesDefectuosasValue;

					if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						unidadesDefectuosasValue = NumberUtils.toInt(_lastTextBox.getHint().toString(), 0);
					else
						unidadesDefectuosasValue = NumberUtils.toInt(_lastTextBox.getText().toString(), 0);

					LineaDeposito lineaDepositoValue = (LineaDeposito) _lastTextBox.getTag();

					if ((unidadesDefectuosasValue) > lineaDepositoValue.UnidadesDevueltas) {
						_appConfig.getMessageBox()
								.Show("Atención",
										"La cantidad de defectuosas no puede ser superior a las devueltas en el artículo "
												+ lineaDepositoValue.Articulo.Descripcion,
										getActivity(), MessageBoxType.Error);

						unidadesDefectuosasValue = 0;
						_lastTextBox.setText(String.valueOf(unidadesDefectuosasValue));
						lineaDepositoValue.UnidadesDefectuosas = unidadesDefectuosasValue;
						_lastTextBox.setHint(String.valueOf(unidadesDefectuosasValue));
					} else {
						lineaDepositoValue.UnidadesDefectuosas = unidadesDefectuosasValue;
						try {
							refreshLayout(lineaDepositoValue, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}

					}
				} else {
					_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
					_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
				}

			});

			TextBoxColor pvp = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT,
					DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP),
					TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 11, TEXT_SIZE, Gravity.CENTER), params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(5);
			pvp.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVP));
			pvp.setTag(lineaDeposito);

			pvp.setOnFocusChangeListener((view, hasFocus) -> {
				_lastTextBox = (TextBoxColor) view;
				if (!hasFocus) {

					float pvp1;
					LineaDeposito lineaDepositoValue = (LineaDeposito) _lastTextBox.getTag();

					if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						pvp1 = NumberUtils.toFloat(_lastTextBox.getHint().toString(), 0);
					else {
						pvp1 = NumberUtils.toFloat(_lastTextBox.getText().toString(), 0);
						lineaDepositoValue.PVPAnterior = pvp1;
					}

					lineaDepositoValue.PVP = pvp1;
					try {
						refreshLayout(lineaDepositoValue, false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else {
					_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
					_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
				}

			});

			TextBoxColor unidadesFacturadas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.rgb(0, 128, 0), Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesFacturadas), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 11, TEXT_SIZE, Gravity.CENTER), params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(6);
			unidadesFacturadas.setText(String.valueOf(lineaDeposito.UnidadesFacturadas));
			unidadesFacturadas.setTag(lineaDeposito);

			unidadesFacturadas.setOnFocusChangeListener((view, hasFocus) -> {
				_lastTextBox = (TextBoxColor) view;
				if (!hasFocus) {
					int unidadesFacturadasValue;

					if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						unidadesFacturadasValue = NumberUtils.toInt(_lastTextBox.getHint().toString(), 0);
					else
						unidadesFacturadasValue = NumberUtils.toInt(_lastTextBox.getText().toString(), 0);

					LineaDeposito lineaDepositoValue = (LineaDeposito) view.getTag();

					lineaDepositoValue.IsVentaDirecta = false;
					boolean directa = false;

					if (unidadesFacturadasValue < (lineaDepositoValue.UnidadesIniciales - lineaDepositoValue.UnidadesDevueltas)
							&& (!lineaDepositoValue.IsNew)) {
						_appConfig.getMessageBox().Show("Unidades Facturadas",
								"No puede realizar una venta directa con una cantidad inferior a la devuelta",
								getActivity(), MessageBoxType.Information);

						unidadesFacturadasValue = lineaDepositoValue.UnidadesIniciales - lineaDepositoValue.UnidadesDevueltas;
						_lastTextBox.setText(String.valueOf(unidadesFacturadasValue));
						lineaDepositoValue.UnidadesFacturadas = unidadesFacturadasValue;
					} else {
						if (unidadesFacturadasValue > 0 && (unidadesFacturadasValue != (lineaDepositoValue.UnidadesInicialesFijas
								- lineaDepositoValue.UnidadesDevueltas))) {
							directa = true;
						}

						lineaDepositoValue.IsVentaDirecta = directa;
						if (lineaDepositoValue.IsVentaDirecta) {
							lineaDepositoValue.UnidadesFacturadas = unidadesFacturadasValue;
						}

						try {
							refreshLayout(lineaDepositoValue, false);
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}

				} else {
					_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
					_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
				}

			});

			TextBoxColor unidadesRepuestas = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT,
					String.valueOf(lineaDeposito.UnidadesRepuestas), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 11, TEXT_SIZE, Gravity.CENTER), params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(7);
			unidadesRepuestas.setText(String.valueOf(lineaDeposito.UnidadesRepuestas));
			unidadesRepuestas.setTag(lineaDeposito);

			unidadesRepuestas.setOnFocusChangeListener((view, hasFocus) -> {
				_lastTextBox = (TextBoxColor) view;
				if (!hasFocus) {

					int unidadesRepuestasValue;

					if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						unidadesRepuestasValue = NumberUtils.toInt(_lastTextBox.getHint().toString(), 0);
					else
						unidadesRepuestasValue = NumberUtils.toInt(_lastTextBox.getText().toString(), 0);

					LineaDeposito lineaDepositoValue = (LineaDeposito) view.getTag();
					lineaDepositoValue.UnidadesRepuestas = unidadesRepuestasValue;

					try {
						refreshLayout(lineaDepositoValue, false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				} else {
					_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
					_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
				}

			});

			TextBoxColor pvpAnterior = createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), color, Gravity.LEFT,
					DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior),
					TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 11, TEXT_SIZE, Gravity.CENTER), params, true, lineaDeposito)
					: (TextBoxColor) _headerLayout.getChildAt(8);
			pvpAnterior.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAnterior));
			pvpAnterior.setTag(lineaDeposito);

			pvpAnterior.setOnFocusChangeListener((view, hasFocus) -> {
				_lastTextBox = (TextBoxColor) view;
				if (!hasFocus) {
					float pvpAnteriorValue;

					if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
						pvpAnteriorValue = NumberUtils.toFloat(_lastTextBox.getHint().toString(), 0);
					else
						pvpAnteriorValue = NumberUtils.toFloat(_lastTextBox.getText().toString(), 0);

					LineaDeposito lineaDepositoValue = (LineaDeposito) view.getTag();
					lineaDepositoValue.PVPAnterior = pvpAnteriorValue;

					try {
						refreshLayout(lineaDepositoValue, false);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				} else {
					_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
					_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
				}


			});

			double total = (lineaDeposito.UnidadesFacturadas * lineaDeposito.PVP);

			LabelColor totalLinea = createHeaderLayout ? DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
					String.valueOf(DepositManagerExtension.Format.round(total, 2)), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 13, TEXT_SIZE, Gravity.CENTER), params, true, lineaDeposito)
					: (LabelColor) _headerLayout.getChildAt(9);

			totalLinea.setText(String.valueOf(DepositManagerExtension.Format.round(total, 2)));
			totalLinea.setTag(lineaDeposito);

			// Botón Abono

			LayoutParams buttonParams = new LayoutParams(params);
			buttonParams.setMargins(10,10,10,10);

			ButtonColor modoAbono = createHeaderLayout ? DepositManagerExtension.UI.addButton(getActivity(), Color.GREEN, "ABONO",
					10, ScreenManager.getViewWidthByLength(_appConfig, 10, TEXT_SIZE, Gravity.LEFT), buttonParams, lineaDeposito)
					: (ButtonColor) _headerLayout.getChildAt(10);

			modoAbono.setTag(lineaDeposito);

			modoAbono.setVisibility(_modalidad == DepositoModalidad.Edicards ?
					View.INVISIBLE : View.VISIBLE);

			modoAbono.setOnClickListener(arg0 -> {

				_abonoMode = true;
				LineaDeposito lineaDepositoValue = (LineaDeposito) arg0.getTag();
				try {
					refreshLayout(lineaDepositoValue, true);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			if (createHeaderLayout) {

				DepositManagerExtension.UI.addViewsToLayout(_headerLayout, codigoArticulo, articuloDescripcion,
						unidadesInicialesFijas, unidadesDevueltas, unidadesDefectuosas, pvp, unidadesFacturadas,
						unidadesRepuestas, pvpAnterior, totalLinea, modoAbono);
			}
			mainLinearLayout.addView(_headerLayout);
			_textBoxColorRequestFocus = unidadesDefectuosas;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void addLineHeaderAbono(LineaDeposito lineaDeposito) {

		final DepositManager that = this;

		LinearLayout mainLinearLayout = this.getActivity().findViewById(R.id.headerMainLinearLayout7);
		mainLinearLayout.removeAllViews();

		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		boolean createHeaderLayout = _headerAbonoLayout	 == null;
		int color = Color.WHITE;

		if (createHeaderLayout) {
			_headerAbonoLayout = new LinearLayout(_appConfig);
			_headerAbonoLayout.setOrientation(LinearLayout.HORIZONTAL);
		} else {
			_headerAbonoLayout.getChildAt(1).setOnClickListener(null);
			for (int i=3; i<8; i++) _headerAbonoLayout.setOnFocusChangeListener(null);
		}

		LabelColor codigoArticulo = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.CodigoArticulo, TEXT_SIZE, 8, params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(0);
		codigoArticulo.setText(lineaDeposito.Articulo.CodigoArticulo);

		LabelColor articuloDescripcion = createHeaderLayout ? DepositManagerExtension.UI.addLabelByText(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				lineaDeposito.Articulo.Descripcion.length() > 25 ? lineaDeposito.Articulo.Descripcion.substring(0, 24) + "..." : lineaDeposito.Articulo.Descripcion + " (Abono) ", TEXT_SIZE, 13,
						params, true, lineaDeposito.Articulo)
				: (LabelColor) _headerAbonoLayout.getChildAt(1);
		articuloDescripcion.setText((lineaDeposito.Articulo.Descripcion.length() > 25 ? lineaDeposito.Articulo.Descripcion.substring(0, 24) + "..." : lineaDeposito.Articulo.Descripcion));
		articuloDescripcion.setTag(lineaDeposito.Articulo);

		articuloDescripcion.setOnClickListener(v -> DepositManagerExtension.Dialogs.StartArticuloDialog((Articulo) v.getTag(), that));

		LabelColor labelCantidadAbono = createHeaderLayout ?  DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Cantidad", TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(2);

		TextBoxColor unidadesAbono =  createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
				String.valueOf(lineaDeposito.UnidadesAbono), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true, lineaDeposito)
				: (TextBoxColor) _headerAbonoLayout.getChildAt(3);
		unidadesAbono.setText(String.valueOf(lineaDeposito.UnidadesAbono));
		unidadesAbono.setTag(lineaDeposito);

		unidadesAbono.setOnFocusChangeListener((view, hasFocus) -> {
			_lastTextBox = (TextBoxColor) view;
			if (!hasFocus) {

				int unidadesAbonoValue;

				LineaDeposito lineaDepositoValue = (LineaDeposito) view.getTag();

				if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
					unidadesAbonoValue = NumberUtils.toInt(_lastTextBox.getHint().toString(), 0);
				else
					unidadesAbonoValue = NumberUtils.toInt(_lastTextBox.getText().toString(), 0);

				int defectuosas = lineaDepositoValue.DefectuosasAbono;

				if (defectuosas > unidadesAbonoValue) {
					_appConfig.getMessageBox()
							.Show("Atención",
									"La cantidad de unidades devueltas es inferior a las defectuosas en el artículo "
											+ lineaDepositoValue.Articulo.Descripcion,
									getActivity(), MessageBoxType.Error);

					_lastTextBox.setText(String.valueOf(lineaDepositoValue.UnidadesAbono));
					_lastTextBox.setHint(String.valueOf(lineaDepositoValue.UnidadesAbono));
				} else {
					lineaDepositoValue.UnidadesAbono = unidadesAbonoValue;
					lineaDepositoValue.DefectuosasAbono = defectuosas;
				}

				try {
					refreshLayout(lineaDepositoValue,false);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			} else {
				_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
				_lastTextBox.setHint(ConstantsTypes.EMPTY_STRING);
			}


		});

		LabelColor labelDefectuosasAbono =  createHeaderLayout ? DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"Defect.", TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(4);

		
		TextBoxColor defectuosasAbono = createHeaderLayout ?  DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
				String.valueOf(lineaDeposito.DefectuosasAbono), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true, lineaDeposito)
				: (TextBoxColor) _headerAbonoLayout.getChildAt(5);
		defectuosasAbono.setText(String.valueOf(lineaDeposito.DefectuosasAbono));
		defectuosasAbono.setTag(lineaDeposito);

		defectuosasAbono.setEnabled(false);
		defectuosasAbono.setInputType(InputType.TYPE_NULL);

		defectuosasAbono.setOnFocusChangeListener((view, hasFocus) -> {
			_lastTextBox = (TextBoxColor) view;
			if (!hasFocus) {

				LineaDeposito lineaDepositoValue = (LineaDeposito) view.getTag();
				int defectuosas;

				if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
					defectuosas = NumberUtils.toInt(_lastTextBox.getHint().toString(),0);
				else
					defectuosas = NumberUtils.toInt(_lastTextBox.getText().toString(),0);

				lineaDepositoValue.DefectuosasAbono = defectuosas;

				if ((defectuosas) > lineaDepositoValue.UnidadesAbono) {
					_appConfig.getMessageBox()
							.Show("Atención",
									"La cantidad de defectuosas no puede ser superior a las devueltas en el artículo "
											+ lineaDepositoValue.Articulo.Descripcion,
									getActivity(), MessageBoxType.Error);

					defectuosas = 0;
					lineaDepositoValue.DefectuosasAbono = defectuosas;
					_lastTextBox.setHint(String.valueOf(defectuosas));
					_lastTextBox.setText(String.valueOf(defectuosas));
				}

				try {
					refreshLayout(lineaDepositoValue,false);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			} else {
				((TextBoxColor) view).setText(ConstantsTypes.EMPTY_STRING);
				((TextBoxColor) view).setText(ConstantsTypes.EMPTY_STRING);
				_lastTextBox = (TextBoxColor) view;
			}

		});
		
		LabelColor labelPVPAbono = createHeaderLayout ?  DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
				"PVP", TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(6);
		
		TextBoxColor pvpAbono =  createHeaderLayout ? DepositManagerExtension.UI.addEdit(getActivity(), Color.RED, Gravity.LEFT,
				DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAbono), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true, lineaDeposito)
				:(TextBoxColor) _headerAbonoLayout.getChildAt(7);
		pvpAbono.setText(DepositManagerExtension.Format.CurrencyFormat(lineaDeposito.PVPAbono));
		pvpAbono.setTag(lineaDeposito);

		pvpAbono.setOnFocusChangeListener((view, hasFocus) -> {
			_lastTextBox = (TextBoxColor) view;
			if (!hasFocus) {
				float pvpAbono1;

				if (_lastTextBox.getText().toString().equals(ConstantsTypes.EMPTY_STRING))
					pvpAbono1 = NumberUtils.toFloat(_lastTextBox.getHint().toString(), 0);
				else
					pvpAbono1 = NumberUtils.toFloat(_lastTextBox.getText().toString(), 0);

				LineaDeposito lineaDepositoValue = (LineaDeposito) view.getTag();
				lineaDepositoValue.PVPAbono = pvpAbono1;

				try {
					refreshLayout(lineaDepositoValue,false);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
				_lastTextBox.setText(ConstantsTypes.EMPTY_STRING);
			}

		});

		double totalAbonoImporte = lineaDeposito.TotalAbono;
		
		LabelColor totalAbono =  createHeaderLayout ?  DepositManagerExtension.UI.addLabel(_appConfig, color, Gravity.RIGHT, InputType.TYPE_CLASS_NUMBER,
				DepositManagerExtension.Format.CurrencyFormat(totalAbonoImporte), TEXT_SIZE, ScreenManager.getViewWidthByLength(_appConfig, 12, TEXT_SIZE, Gravity.LEFT), params, true)
				: (LabelColor) _headerAbonoLayout.getChildAt(8);
		totalAbono.setText(DepositManagerExtension.Format.CurrencyFormat(totalAbonoImporte));
		totalAbono.setTag(lineaDeposito);
				
		// Botón Venta

		LayoutParams buttonParams = new LayoutParams(params);
		buttonParams.setMargins(10,10,10,10);
		ButtonColor modoVenta = createHeaderLayout ?  DepositManagerExtension.UI.addButton(getActivity(), Color.GREEN, "VENTA",
				10, ScreenManager.getViewWidthByLength(_appConfig, 10, TEXT_SIZE, Gravity.LEFT), buttonParams, lineaDeposito)
				: (ButtonColor) _headerAbonoLayout.getChildAt(9);

		modoVenta.setTag(lineaDeposito);
		modoVenta.setOnClickListener(arg0 -> {

			_abonoMode = false;
			LineaDeposito lineaDepositoValue = (LineaDeposito) arg0.getTag();
			try {
				refreshLayout(lineaDepositoValue,true);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		if (createHeaderLayout) {

			DepositManagerExtension.UI.addViewsToLayout(_headerAbonoLayout, codigoArticulo, articuloDescripcion,
					labelCantidadAbono, unidadesAbono, labelDefectuosasAbono, defectuosasAbono, labelPVPAbono,
					pvpAbono, totalAbono, modoVenta);
		}

		mainLinearLayout.addView(_headerAbonoLayout);
		_textBoxColorRequestFocus = defectuosasAbono;

	}

	private void showHeader(boolean visible) {
		if (this.getActivity().findViewById(R.id.headerMainLinearLayout) == null) return;
		(this.getActivity().findViewById(R.id.headerMainLinearLayout)).setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	private void showArticlesGrid(boolean visible) {
		_articlesLayout.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}

	private void showButtonBar(boolean visible) {
		LinearLayout linearLayout = this.getActivity().findViewById(R.id.headerButtonsLinearLayout);
		LinearLayout subLinearLayout = (LinearLayout) linearLayout.getChildAt(0);

		if (subLinearLayout == null) return;

		for (int i = 0; i < subLinearLayout.getChildCount(); i++) {
			View view = subLinearLayout.getChildAt(i);

			if (view.getTag() == null)
				view.setVisibility(visible ? View.VISIBLE : View.GONE);
			else
				view.setVisibility(view.getTag().equals("NUEVO") ? View.VISIBLE : (visible ? View.VISIBLE : View.GONE));
		}
	}

	private void createHeaderLabels() {
		
		LinearLayout mainLinearLayout = this.getActivity()
				.findViewById(R.id.headerLabelsLinearLayout);
		
		mainLinearLayout.removeAllViews();
		
		LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		DepositManagerExtension.UI.addViewsToLayout(mainLinearLayout,
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
						"Código".toUpperCase(), TEXT_SIZE, 8, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.LEFT, InputType.TYPE_CLASS_NUMBER,
						"Artículo".toUpperCase(), TEXT_SIZE, 12, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
						"Dep. Inicial".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
						"Contadas".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
						"Reciclado".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
						"Pvp".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
						"Facturadas".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
						"Repuestas".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
								"PVP Post.".toUpperCase(), TEXT_SIZE, 13, params),
				DepositManagerExtension.UI.addLabelByText(_appConfig, Color.WHITE, Gravity.CENTER, InputType.TYPE_CLASS_NUMBER,
								"Total".toUpperCase(), TEXT_SIZE, 13, params));

	}

	private void refreshLayout(LineaDeposito linea,  boolean redraw)
			throws Exception {

		if (redraw) {
			if (_abonoMode)
				addLineHeaderAbono(linea);
			else
				addLineHeader(linea);
		}

		if (_deposito != null) {
			if (!_deposito.IsNtvDeposit) {
				linea.StockInicial = linea.Articulo.Stock + linea.UnidadesDevueltas - linea.UnidadesRepuestas;

				if (!linea.IsVentaDirecta) {
					linea.UnidadesFacturadas = linea.UnidadesInicialesFijas - linea.UnidadesDevueltas;
					linea.UnidadesIniciales = linea.UnidadesInicialesFijas + linea.UnidadesRepuestas - linea.UnidadesFacturadas
							- linea.UnidadesDevueltas;
				} else {
					linea.UnidadesIniciales = linea.UnidadesRepuestas;
				}
			}
		}
		refreshTotals();
		linea.TotalAbono = DepositManagerExtension.Format.round((linea.UnidadesAbono * linea.PVPAbono * -1), 2);

		_adapter.notifyDataSetChanged();

	}
	private void refreshTotals() throws Exception {
		if (_deposito != null) {

			_deposito.Cliente.Filiacion = DepositManagerExtension.DataTier.getFiliacionCode(_comboFiliacion.getText()).trim();
			_deposito.Calculate();

			((TextView) getActivity().findViewById(R.id.lblBase)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase) + " €");
			if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
				((TextView) getActivity().findViewById(R.id.lblTotalFactura)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.Total) + " €");
			} else {
				((TextView) getActivity().findViewById(R.id.lblTotalFactura)).setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase) + " €");
			}

			((TextView) getActivity().findViewById(R.id.lblTipoEntrega)).setText(_modalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

			if (_checkPagado.isChecked()) {
				if (DepositManagerExtension.DataTier.IsSerieA(this._comboSerie, this._appConfig)) {
					_textBoxCantidadPagada.setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.Total));
				} else {
					_textBoxCantidadPagada.setText(DepositManagerExtension.Format.CurrencyFormat(_deposito.Totales.TotalBase));
				}
			}
		} else {
			((TextView) getActivity().findViewById(R.id.lblBase)).setText(ConstantsTypes.EMPTY_STRING);
			((TextView) getActivity().findViewById(R.id.lblTotalFactura)).setText(ConstantsTypes.EMPTY_STRING);
			((TextView) getActivity().findViewById(R.id.lblTipoEntrega)).setText(ConstantsTypes.EMPTY_STRING);
		}
	}
	private void resetDepositData() {
		_abonoMode = false;
		_appConfig.getWorkingArea().CurrentCliente = null;
		_appConfig.getWorkingArea().CurrentDeposito = null;
		this._articles.clear();
		this._currentLines.clear();
		this._originalLines.clear();
		if (this._adapter != null) this._adapter.clear();
		this.resetHeaders();
	}
	@Override
	public void notify(String event, Object payload) {

		switch (event) {
			case "EventCustomerSelected": {
				try {
					_appConfig.getWorkingArea().CurrentCliente = (Cliente) payload;
					this.CreateDepositView( null);

					this._dialogDepositoModalidad = new AdvancedMessageBox();
					boolean resultDepositoModalidad = _dialogDepositoModalidad.Show("Gestión de Depósito", "Qué tipo de albarán Deseas ?", "Entregar mercancía físicamente", "Enviar desde Edicards", DepositManager.this.getContext(), MessageBoxType.Information);
					_modalidad = resultDepositoModalidad ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
					TextView labelTipoEntrega = getActivity().findViewById(R.id.lblTipoEntrega);
					labelTipoEntrega.setText(_modalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				break;
			}

			case "EventCustomerNew": {

				try {
					_appConfig.getWorkingArea().CurrentCliente = (Cliente) payload;
					this.CreateDepositView(null);

					this._dialogDepositoModalidad = new AdvancedMessageBox();
					boolean resultDepositoModalidad = _dialogDepositoModalidad.Show("Gestión de Depósito", "Qué tipo de albarán Deseas ?", "Entregar mercancía físicamente", "Enviar desde Edicards", DepositManager.this.getContext(), MessageBoxType.Information);
					_modalidad = resultDepositoModalidad ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
					TextView labelTipoEntrega = getActivity().findViewById(R.id.lblTipoEntrega);
					labelTipoEntrega.setText(_modalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				_appConfig.getWorkingArea().CurrentCliente = (Cliente) payload;
				break;
			}

			case "EventCustomerCancelled": {
				_appConfig.getMediator().notify(ConstantsEvents.EVENT_DEPOSIT_CLOSED, null);
				this.createHeaderButtons(true);
				break;
			}

			case "EventCloseOperation" : {

				if (_appConfig.getWorkingArea().CurrentHistorico.GUID == null) {
					_appConfig.getMessageBox().Show("Cierre de operación",
							"Se ha producido un error al cerrar la operación. Contacte con el servicio técnico",
							this.getActivity(), MessageBoxType.Information);
					return;
				}
				this.printDeposito(payload.toString());
				closeOperation();
				resetDepositData();

				break;
			}

			case "EventNtvImportStarted": {
				DepositoNTVDTO depositoNTVDTO = (DepositoNTVDTO) payload;

				_cliente = Factory.build(Cliente.class, _appConfig);
				boolean exists;
				try {
					exists = _cliente.setClienteByCodigo(depositoNTVDTO.IdCliente);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				if (!exists && depositoNTVDTO.IdCliente.equals(ConstantsTypes.NEW_NTV_CUSTOMER_CODE)) {
					try {
						_cliente.setClienteByCodigo(ConstantsTypes.NEW_CUSTOMER_CODE);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				_appConfig.getWorkingArea().CurrentCliente = _cliente;

				try {
					_deposito = Factory.build(Deposito.class, _appConfig);
					_deposito.assingFromCliente(_cliente);
					_deposito.IsNtvDeposit = true;
					this.CreateDepositView(depositoNTVDTO);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				this._dialogDepositoModalidad = new AdvancedMessageBox();
				boolean resultDepositoModalidad = _dialogDepositoModalidad.Show("Gestión de Depósito", "Qué tipo de albarán Deseas ?", "Entregar mercancía físicamente", "Enviar desde Edicards", DepositManager.this.getContext(), MessageBoxType.Information);
				_modalidad = resultDepositoModalidad ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
				TextView labelTipoEntrega = getActivity().findViewById(R.id.lblTipoEntrega);
				labelTipoEntrega.setText(_modalidad == DepositoModalidad.Edicards ? "Enviar desde Edicards" : "Entregar mercancia físicamente");

				try {
					this.refreshTotals();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				break;
			}
		}
	}
}
