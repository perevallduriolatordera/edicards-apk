package net.ifeu.edicards;

import java.util.HashMap;

import android.app.ActionBar.LayoutParams;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.MovimientosAlmacen;
import net.ifeu.edicards.Pdf.PdfInventoryRecycled;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.LogBook.LogBook;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

public class StockManager extends Fragment {

	private AppConfig _appConfig;
	private HashMap<String, Articulo> _articulos;
	private final int TEXT_SIZE_BUTTON = 12;
	private final int BUTTONS_WIDTH = 150;

	private TextBoxColor _lastTextBox;
	
	private boolean _isManagerPasswordMode;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.activity_stock_manager, container,
				false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		this._isManagerPasswordMode = false;
		_appConfig = (AppConfig) getActivity().getApplicationContext();
		this.FillForm(true);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void FillForm(boolean addHeader) {
		
		LinearLayout mainLinearLayout = (LinearLayout) getActivity()
				.findViewById(R.id.mainLinearLayout);
		
		mainLinearLayout.removeAllViews();

		try {
			_articulos = _appConfig.getCache().getAllArticulos();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (addHeader)
			this.addHeader();

		if (_articulos == null) {
			_appConfig
					.getMessageBox()
					.Show("Control de almacén",
							"No se han encontrado artículos. Sincronice datos con el servidor",
							getActivity(), MessageBoxType.Information);
		} else {
			for (Articulo articulo : _articulos.values())
				try {
					addLine(articulo);
				} catch (Exception e) {
						throw new RuntimeException(e);
				}

			if (_articulos.size() == 0)
				_appConfig
						.getMessageBox()
						.Show("Control de almacén",
								"No se han encontrado artículos. Sincronice datos con el servidor",
								getActivity(), MessageBoxType.Information);
		}
	}
	
	private void SendRecuento() {
		this.SaveRecuento();
		this.CreateXmlRecuento();
		try {
			this.sendData();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
			//		e);
		}
		
	}
	
	private void SaveRecuento () {
		
		for (Articulo articulo : _articulos.values()) {
			try {
				articulo.update();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	
	}
	
	private void initializeStock(boolean onlyReciclado) {

		for (Articulo articulo : _articulos.values()) {
			try {

				LogBook logBookWriter = Factory.build(LogBook.class, _appConfig);

				if (!onlyReciclado) {
					articulo.Stock = 0;
					logBookWriter.setData("INICIALIZACIÓN DE ALMACÉN", Constants.EMPTY_STRING,
							Constants.EMPTY_STRING, articulo.CodigoArticulo, articulo.Descripcion,
							articulo.Stock, 0, 0, 0, 0, 0, 0, 0,0);

					logBookWriter.save();
				}

				articulo.StockDefectuoso = 0;
				articulo.update();
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void CreateXmlRecuento()  {
		
		XmlCreator xml = new XmlCreator(_appConfig, getActivity());
		
		try {
			xml.createXmlRecuento();	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		
	}
	
	private void sendData()  {

		final ProgressDialog progressDialog;
		progressDialog = ProgressDialog.show(this.getActivity(),
				"Enviando Datos a Central", "Enviando...Espere unos instantes",
				true);

		final Context context = _appConfig;

		new Thread() {

			@Override
			public void run() {
				try {
					ServiceWorker worker = new ServiceWorker();
					worker.RunExport(context);

				} catch (Exception e) {

					//_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
					//		e);
				}

				progressDialog.dismiss();

			}

		}.start();
	}
	
	private void addHeader() {
		
		LinearLayout mainHeaderButtonsLinearLayout = (LinearLayout) getActivity()
				.findViewById(R.id.headerButtonsLinearLayout);
		
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		ButtonColor recuento = new ButtonColor(getActivity(), Color.BLUE,
				getResources().getDrawable(R.drawable.ic_send));

		recuento.setText("Enviar recuento");
		recuento.setTextSize(TEXT_SIZE_BUTTON);
		recuento.setWidth(BUTTONS_WIDTH);
		
		recuento.setLayoutParams(params);

		final StockManager that = this;
		
		recuento.setOnClickListener(arg0 -> {

			try {

				try {

					if (that._isManagerPasswordMode)
					{
						Deposito deposito = Factory.build(Deposito.class, _appConfig);

						if (deposito.getDepositosToday().size() > 0) {
							_appConfig.getMessageBox().Show(
									"Atención",
									"No se puede enviar el recuento de almacén, ya que ya se han producido operaciones durante el día de hoy! "
											, getActivity(),
									MessageBoxType.Error);
						} else {
							that.SendRecuento();

							_appConfig
							.getMessageBox()
							.Show("Control de almacén",
									"El recuento de almacén se ha enviado correctamente",
									getActivity(), MessageBoxType.Error);

							that.FillForm(false);
						}

					} else {
						String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", getActivity());

						if (password.equals(Constants.MANAGER_PASSWORD)) {

							that._isManagerPasswordMode = true;

							Deposito deposito = Factory.build(Deposito.class, _appConfig);

							if (deposito.getDepositosToday().size() > 0) {
								_appConfig.getMessageBox().Show(
										"Atención",
										"No se puede enviar el recuento de almacén, ya que ya se han producido operaciones durante el día de hoy! "
												, getActivity(),
										MessageBoxType.Error);
							} else {
								that.SendRecuento();

								_appConfig
								.getMessageBox()
								.Show("Control de almacén",
										"El recuento de almacén se ha enviado correctamente",
										getActivity(), MessageBoxType.Error);

								that.FillForm(false);

							}
						} else {
							_appConfig
							.getMessageBox()
							.Show("Control de almacén",
									"La clave indicada no es correcta",
									getActivity(), MessageBoxType.Error);
						}

					}

				} catch (Exception e) {
							throw new RuntimeException(e);
				}


			} catch (Exception e) {
					throw new RuntimeException(e);
			}
		});
		
		ButtonColor inicializar = new ButtonColor(getActivity(), Color.RED,
				getResources().getDrawable(R.drawable.ic_restart));

		inicializar.setText("Inicializar Stock");
		inicializar.setTextSize(TEXT_SIZE_BUTTON);
		inicializar.setWidth(BUTTONS_WIDTH);
		
		inicializar.setLayoutParams(params);
		
		inicializar.setOnClickListener(arg0 -> {

			try {

				String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", getActivity());

				if (password.equals(Constants.MANAGER_PASSWORD)) {
					that.initializeStock(false);
					_appConfig
					.getMessageBox()
					.Show("Control de almacén",
							"La inicialización de stock se ha realizado correctamente",
							getActivity(), MessageBoxType.Information);

					that.FillForm(false);
				} else {
					_appConfig
					.getMessageBox()
					.Show("Control de almacén",
							"La clave introducida no es correcta",
							getActivity(), MessageBoxType.Information);
				}

			} catch (Exception e) {
					throw new RuntimeException(e);
			}
		});
		
		ButtonColor reciclado = new ButtonColor(getActivity(), Color.MAGENTA,
				getResources().getDrawable(R.drawable.ic_recycled));

		reciclado.setText("Reciclado");
		reciclado.setTextSize(TEXT_SIZE_BUTTON);
		reciclado.setWidth(BUTTONS_WIDTH);
		
		reciclado.setLayoutParams(params);
		
		reciclado.setOnClickListener(arg0 -> {

			try {

				boolean result = _appConfig.getMessageBox().ShowWithResult("Recuento de reciclado", "Está seguro que quiere reinicializar el stock", getActivity(), MessageBoxType.Information);

				if (result) {

					PdfInventoryRecycled pdf = new PdfInventoryRecycled(_appConfig, _appConfig);
					if (pdf.createInventory()) {

						that.initializeStock(true);
						that.sendData();

						that.FillForm(false);
						_appConfig
						.getMessageBox()
						.Show("Recuento de reciclado",
								"La inicialización de stock reciclado se ha realizado correctamente",
								getActivity(), MessageBoxType.Information);
					} else {
						that.initializeStock(true);
						_appConfig
						.getMessageBox()
						.Show("Recuento de reciclado",
								"Se ha producido un error al generar La inicialización de stock reciclado",
								getActivity(), MessageBoxType.Information);
					}

				}

			} catch (Exception e) {
				throw new RuntimeException(e);

			}
		});

		TextView space = new TextView(getActivity());
		space.setWidth(30);
		space.setLayoutParams(params);
		
		mainHeaderButtonsLinearLayout.addView(space);
		mainHeaderButtonsLinearLayout.addView(recuento);
		mainHeaderButtonsLinearLayout.addView(inicializar);
		mainHeaderButtonsLinearLayout.addView(reciclado);

	}
	private void addLine(Articulo articulo) {

		final StockManager that = this;
		
		articulo.Activo = true;

		LinearLayout mainLinearLayout = (LinearLayout) getActivity()
				.findViewById(R.id.mainLinearLayout);

		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		LinearLayout layout = new LinearLayout(this.getActivity());
		
    	layout.setBackgroundResource(R.drawable.card_background);
    	layout.setLayoutParams(params);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setPadding(20, 20, 20, 20);

		LabelColor codigoArticulo = new LabelColor(getActivity(), Color.BLACK);
		codigoArticulo.setText(articulo.CodigoArticulo);
		int TEXT_SIZE = 16;
		codigoArticulo.setTextSize(TEXT_SIZE);
		int CODE_WIDTH = 75;
		codigoArticulo.setWidth(CODE_WIDTH);
		codigoArticulo.setLayoutParams(params);

		LabelColor articuloDescripcion = new LabelColor(getActivity(),
				Color.BLACK, true);
		articuloDescripcion.setTag(articulo);
		articuloDescripcion.setText(articulo.Descripcion);
		articuloDescripcion.setTextSize(TEXT_SIZE);
		int DESCRIPTION_WIDTH = 225;
		articuloDescripcion.setWidth(DESCRIPTION_WIDTH);
		articuloDescripcion.setPaintFlags(articuloDescripcion.getPaintFlags()
				| Paint.FAKE_BOLD_TEXT_FLAG);
		articuloDescripcion.setLayoutParams(params);

		articuloDescripcion.setOnClickListener(v -> StartArticuloDialog((Articulo) ((LabelColor) v).getTag()));

		LabelColor unidadesIniciales = new LabelColor(getActivity(),
				Color.argb(255, 100, 100, 50), true, Gravity.RIGHT);
		unidadesIniciales.setText(String.valueOf(articulo.Stock));
		unidadesIniciales.setTag(articulo);
		unidadesIniciales.setTextSize(TEXT_SIZE);
		int FIELDS_WIDTH = 70;
		unidadesIniciales.setWidth(FIELDS_WIDTH);
		unidadesIniciales.setLayoutParams(params);
		layout.setTag(unidadesIniciales);

		TextBoxColor unidadesEntradas = new TextBoxColor(getActivity(),
				Color.argb(255, 100, 100, 50), Gravity.RIGHT);
		unidadesEntradas.setInputType(InputType.TYPE_CLASS_NUMBER);
		unidadesEntradas.setHint(String.valueOf(articulo.Entradas));
		unidadesEntradas.setTag(articulo);
		unidadesEntradas.setTextSize(TEXT_SIZE);
		unidadesEntradas.setWidth(FIELDS_WIDTH);
		unidadesEntradas.setLayoutParams(params);

		TextBoxColor unidadesSalidas = new TextBoxColor(getActivity(),
				Color.argb(255, 100, 100, 50), Gravity.RIGHT);
		unidadesSalidas.setInputType(InputType.TYPE_CLASS_NUMBER);
		unidadesSalidas.setHint(String.valueOf(articulo.Salidas));
		unidadesSalidas.setTag(articulo);
		unidadesSalidas.setTextSize(TEXT_SIZE);
		unidadesSalidas.setWidth(FIELDS_WIDTH);
		unidadesSalidas.setLayoutParams(params);

		TextView space = new TextView(getActivity());
		space.setWidth(30);
		space.setLayoutParams(params);
		
		TextView space2 = new TextView(getActivity());
		space2.setWidth(30);
		space2.setLayoutParams(params);

		LabelColor unidadesInicialesDefectuoso = new LabelColor(getActivity(),
				Color.RED, true, Gravity.RIGHT);
		unidadesInicialesDefectuoso
				.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		unidadesInicialesDefectuoso.setText(String
				.valueOf(articulo.StockDefectuoso));
		unidadesInicialesDefectuoso.setTag(articulo);
		unidadesInicialesDefectuoso.setTextSize(TEXT_SIZE);
		unidadesInicialesDefectuoso.setWidth(FIELDS_WIDTH);
		unidadesInicialesDefectuoso.setTextColor(Color.RED);
		unidadesInicialesDefectuoso.setLayoutParams(params);
		layout.setTag(unidadesInicialesDefectuoso);

		TextBoxColor unidadesEntradasDefectuoso = new TextBoxColor(
				getActivity(), Color.RED, Gravity.RIGHT);
		unidadesEntradasDefectuoso.setInputType(InputType.TYPE_CLASS_NUMBER);
		unidadesEntradasDefectuoso.setHint(String.valueOf(articulo.Entradas));
		unidadesEntradasDefectuoso.setTag(articulo);
		unidadesEntradasDefectuoso.setTextSize(TEXT_SIZE);
		unidadesEntradasDefectuoso.setWidth(FIELDS_WIDTH);
		unidadesEntradasDefectuoso.setTextColor(Color.RED);
		unidadesEntradasDefectuoso.setLayoutParams(params);

		TextBoxColor unidadesSalidasDefectuoso = new TextBoxColor(
				getActivity(), Color.RED, Gravity.RIGHT);
		unidadesSalidasDefectuoso.setInputType(InputType.TYPE_CLASS_NUMBER);
		unidadesSalidasDefectuoso.setHint(String.valueOf(articulo.Salidas));
		unidadesSalidasDefectuoso.setTag(articulo);
		unidadesSalidasDefectuoso.setTextSize(TEXT_SIZE);
		unidadesSalidasDefectuoso.setWidth(FIELDS_WIDTH);
		unidadesSalidasDefectuoso.setTextColor(Color.RED);
		unidadesSalidasDefectuoso.setLayoutParams(params);

		ButtonColor regularizacion = new ButtonColor(getActivity(),
				Color.DKGRAY);

		regularizacion.setText("Inventario");
		regularizacion.setTextSize(TEXT_SIZE_BUTTON);
		regularizacion.setWidth(BUTTONS_WIDTH);
		regularizacion.setTag(new SwapStorage(articulo, unidadesEntradas,
				unidadesSalidas, unidadesIniciales, unidadesEntradasDefectuoso,
				unidadesSalidasDefectuoso, unidadesInicialesDefectuoso));
		regularizacion.setLayoutParams(params);

		regularizacion.setOnClickListener(arg0 -> {

			try {
				saveStock(arg0, 1);
			} catch (Exception e) {
				throw new RuntimeException(e);

			}
		});

		ButtonColor intercambio = new ButtonColor(getActivity(), Color.BLUE);

		intercambio.setText("Camión");
		intercambio.setTextSize(TEXT_SIZE_BUTTON);
		intercambio.setWidth(BUTTONS_WIDTH);
		intercambio.setTag(new SwapStorage(articulo, unidadesEntradas,
				unidadesSalidas, unidadesIniciales, unidadesEntradasDefectuoso,
				unidadesSalidasDefectuoso, unidadesInicialesDefectuoso));
		intercambio.setLayoutParams(params);

		intercambio.setOnClickListener(arg0 -> {

			try {
				saveStock(arg0, 2);
			} catch (Exception e) {
				throw new RuntimeException(e);

			}
		});

		final TextBoxColor unidadesRecuento = new TextBoxColor(
				getActivity(), Color.BLACK, Gravity.RIGHT);
		unidadesRecuento.setInputType(InputType.TYPE_CLASS_NUMBER);
		unidadesRecuento.setHint(String.valueOf(articulo.Stock));
		unidadesRecuento.setTag(articulo);
		unidadesRecuento.setTextSize(TEXT_SIZE);
		unidadesRecuento.setWidth(FIELDS_WIDTH);
		unidadesRecuento.setTextColor(Color.BLACK);
		unidadesRecuento.setLayoutParams(params);
		
		unidadesRecuento.setOnFocusChangeListener((view, hasFocus) -> {

			if (!hasFocus) {
				Deposito deposito = Factory.build(Deposito.class, _appConfig);

				try {
					LogBook logBookWriter = Factory.build(LogBook.class, _appConfig);

					if (deposito.getDepositosToday().size() > 0) {
						_appConfig.getMessageBox().Show(
								"Atención",
								"No se puede hacer recuento de almacén, ya que ya se han producido operaciones durante el día de hoy! "
										, getActivity(),
								MessageBoxType.Error);

						EditText textBox = (EditText) view;

						int unidades = ((Articulo) unidadesRecuento.getTag()).Stock;

						textBox.setText(Constants.EMPTY_STRING);
						textBox.setHint(unidades);

					} else {

						if (that._isManagerPasswordMode) {

							_lastTextBox = (TextBoxColor) view;
							EditText textBox = (EditText) view;
							int unidades = Integer.parseInt(textBox.getText().toString());

							((Articulo) unidadesRecuento.getTag()).Stock = unidades;
							logBookWriter.setData("ASIGNACION DE ALMACÉN", Constants.EMPTY_STRING,
									Constants.EMPTY_STRING, ((Articulo) unidadesRecuento.getTag()).CodigoArticulo, ((Articulo) unidadesRecuento.getTag()).Descripcion,
									articulo.Stock, unidades, 0, 0, 0, 0, 0, 0,0);

							logBookWriter.save();

						} else {

							String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", getActivity());

							if (password.equals(Constants.MANAGER_PASSWORD)) {

								that._isManagerPasswordMode = true;
								_lastTextBox = (TextBoxColor) view;
								EditText textBox = (EditText) view;

								int unidades = Integer.parseInt(textBox.getText().toString());
								((Articulo) unidadesRecuento.getTag()).Stock = unidades;

								logBookWriter.setData("ASIGNACION DE ALMACÉN", Constants.EMPTY_STRING,
										Constants.EMPTY_STRING, ((Articulo) unidadesRecuento.getTag()).CodigoArticulo, ((Articulo) unidadesRecuento.getTag()).Descripcion,
										articulo.Stock, unidades, 0, 0, 0, 0, 0, 0,0);

								logBookWriter.save();

							} else {
								_appConfig.getMessageBox().Show(
										"Error",
										"La clave introducida no es correcta",
										_appConfig,
										MessageBoxType.Error);

								_lastTextBox.setText("0");

							}
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);

				}

			} else {
				_lastTextBox = (TextBoxColor) view;
			}
		});

		ImageView imageView = new ImageView(this._appConfig);

		LinearLayout.LayoutParams imageViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
		imageViewParams.gravity = Gravity.CENTER_VERTICAL;

		imageView.setLayoutParams(imageViewParams);

		if (!articulo.StockPropio)
			imageView.setImageResource(R.drawable.stock_ko_png);
		else
			imageView.setImageResource(R.drawable.stock_ok_png);

		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(150, 60);
		imageView.setLayoutParams(layoutParams);
		
		layout.addView(codigoArticulo);
		layout.addView(articuloDescripcion);
		layout.addView(unidadesIniciales);
		//layout.addView(unidadesEntradas);
		//layout.addView(unidadesSalidas);
		layout.addView(space);
		layout.addView(unidadesInicialesDefectuoso);
		//layout.addView(unidadesEntradasDefectuoso);
		//layout.addView(unidadesSalidasDefectuoso);
		//layout.addView(regularizacion);
		//layout.addView(intercambio);
		layout.addView(space2);
		layout.addView(unidadesRecuento);
		layout.addView(imageView);
		
		mainLinearLayout.addView(layout);

	}

	private void StartArticuloDialog(Articulo articulo) {

		//_appConfig = (AppConfig) getActivity().getApplicationContext();

		_appConfig.getWorkingArea().CurrentArticulo = articulo;

		Intent intent = new Intent(this.getActivity(), ArticleDialog.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);
	}
	
	private void saveStock(View view, int tipo) throws Exception {

		// Movimientos Para Stock
		SwapStorage swap = (SwapStorage) view.getTag();
		int stock = swap.Articulo.Stock;

		int entradas;
		int salidas;

		if (swap.Entradas.getText().toString().equals(Constants.EMPTY_STRING))
			entradas = 0;
		else
			entradas = Integer.parseInt(swap.Entradas.getText().toString());

		if (swap.Salidas.getText().toString().equals(Constants.EMPTY_STRING))
			salidas = 0;
		else
			salidas = Integer.parseInt(swap.Salidas.getText().toString());

		if (salidas > (stock + entradas))
			_appConfig.getMessageBox().Show(
					"Error",
					"Cuidado! El stock es negativo en el articulo "
							+ swap.Articulo.Descripcion, getActivity(),
					MessageBoxType.Error);

		int stockInicial = swap.Articulo.Stock;
		swap.Articulo.Stock = stockInicial + entradas - salidas;

		LogBook logBookWriter = Factory.build(LogBook.class, _appConfig);

		logBookWriter.setData("ASIGNACION DE ALMACÉN", Constants.EMPTY_STRING,
				Constants.EMPTY_STRING, swap.Articulo.CodigoArticulo, swap.Articulo.Descripcion,
				stockInicial, swap.Articulo.Stock, entradas, 0, salidas, 0, 0, 0,0);

		logBookWriter.save();

		swap.Articulo.Entradas = entradas;
		swap.Articulo.Salidas = salidas;
		swap.Articulo.Tipo = tipo;

		swap.Inicial.setText(String.valueOf(swap.Articulo.Stock));
		swap.Entradas.setHint(String.valueOf(0));
		swap.Entradas.setText(Constants.EMPTY_STRING);
		swap.Salidas.setHint(String.valueOf(0));
		swap.Salidas.setText(Constants.EMPTY_STRING);

		try {
			swap.Articulo.update();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Damos de alta el movimiento de almacén

		MovimientosAlmacen movimiento = Factory.build(MovimientosAlmacen.class, _appConfig);

		try {
			movimiento.Articulo = swap.Articulo;
			movimiento.Entradas = swap.Articulo.Entradas;
			movimiento.Salidas = swap.Articulo.Salidas;
			movimiento.Tipo = tipo;
			movimiento.TipoStock = 1;

			movimiento.save();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Movimientos Stock Defectuoso

		int stockDefectuoso = swap.Articulo.StockDefectuoso;
		int entradasDefectuoso;
		int salidasDefectuoso;

		if (swap.EntradasDefectuoso.getText().toString()
				.equals(Constants.EMPTY_STRING))
			entradasDefectuoso = 0;
		else
			entradasDefectuoso = Integer.parseInt(swap.EntradasDefectuoso
					.getText().toString());

		if (swap.SalidasDefectuoso.getText().toString()
				.equals(Constants.EMPTY_STRING))
			salidasDefectuoso = 0;
		else
			salidasDefectuoso = Integer.parseInt(swap.SalidasDefectuoso
					.getText().toString());

		if (salidasDefectuoso > (stockDefectuoso + entradasDefectuoso))
			_appConfig.getMessageBox().Show(
					"Error",
					"Cuidado! El stock de material defectuoso es negativo en el articulo  "
							+ swap.Articulo.Descripcion, getActivity(),
					MessageBoxType.Error);

		swap.Articulo.StockDefectuoso = swap.Articulo.StockDefectuoso
				+ entradasDefectuoso - salidasDefectuoso;
		swap.Articulo.Entradas = entradasDefectuoso;
		swap.Articulo.Salidas = salidasDefectuoso;
		swap.Articulo.Tipo = tipo;

		swap.InicialDefectuoso.setText(String
				.valueOf(swap.Articulo.StockDefectuoso));
		swap.EntradasDefectuoso.setHint(String.valueOf(0));
		swap.EntradasDefectuoso.setText(Constants.EMPTY_STRING);
		swap.SalidasDefectuoso.setHint(String.valueOf(0));
		swap.SalidasDefectuoso.setText(Constants.EMPTY_STRING);

		try {
			swap.Articulo.update();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Damos de alta el movimiento de almacén

		MovimientosAlmacen movimientoDefectuoso = Factory.build(MovimientosAlmacen.class, _appConfig);

		try {
			movimientoDefectuoso.Articulo = swap.Articulo;
			movimientoDefectuoso.Entradas = swap.Articulo.Entradas;
			movimientoDefectuoso.Salidas = swap.Articulo.Salidas;
			movimientoDefectuoso.Tipo = tipo;
			movimientoDefectuoso.TipoStock = 2;

			movimientoDefectuoso.save();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static class SwapStorage {
		public Articulo Articulo;
		public EditText Entradas;
		public EditText Salidas;
		public TextView Inicial;
		public EditText EntradasDefectuoso;
		public EditText SalidasDefectuoso;
		public TextView InicialDefectuoso;

		public SwapStorage(Articulo articulo, EditText entradas,
				EditText salidas, TextView inicial,
				EditText entradasDefectuoso, EditText salidasDefectuoso,
				TextView inicialDefectuoso) {
			this.Articulo = articulo;
			this.Entradas = entradas;
			this.Salidas = salidas;
			this.Inicial = inicial;
			this.EntradasDefectuoso = entradasDefectuoso;
			this.SalidasDefectuoso = salidasDefectuoso;
			this.InicialDefectuoso = inicialDefectuoso;
		}
	}

}
