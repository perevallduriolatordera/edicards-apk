package net.ifeu.edicards;

import java.util.HashMap;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
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
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.MovimientosAlmacen;
import net.ifeu.edicards.Pdf.PdfInventoryRecycled;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.Utils.MessageBoxType;

public class StockManager extends Fragment {

	private AppConfig _appConfig;
	private HashMap<String, Articulo> _articulos;

	private final int TEXT_SIZE = 16;
	private final int TEXT_SIZE_BUTTON = 12;
	private final int FIELDS_WIDTH = 70;
	private final int BUTTONS_WIDTH = 150;
	private final int CODE_WIDTH = 75;
	private final int DESCRIPTION_WIDTH = 225;
	
	private TextBoxColor _lastTextBox;
	
	private boolean _isManagerPasswordMode;

	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
	//	Deposito d = null;
	//	Log.i("TEST", d.NumDoc);
		
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
		
		this.FillForm(true);

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void FillForm(boolean addHeader) {
		
		LinearLayout mainLinearLayout = (LinearLayout) getActivity()
				.findViewById(R.id.mainLinearLayout);
		
		mainLinearLayout.removeAllViews();

		// Inicialitzem l'objecte AppConfig

		_appConfig = (AppConfig) getActivity().getApplicationContext();

		Articulo articulos = new Articulo();
		try {
			articulos.InitializePersistance(_appConfig, _appConfig);
			_articulos = articulos.getAllArticulos(1);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e1);
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
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
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
				articulo.InitializePersistance(_appConfig, getActivity());
				
				articulo.update();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			}
		}
	
	}
	
	private void initializeStock(boolean onlyReciclado) {
		
		for (Articulo articulo : _articulos.values()) {
			try {
				articulo.InitializePersistance(_appConfig, getActivity());
				
				if (!onlyReciclado)
					articulo.Stock = 0;
				
				articulo.StockDefectuoso = 0;
				
				articulo.update();
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
			}
		}
	}
	
	private void CreateXmlRecuento()  {
		
		XmlCreator xml = new XmlCreator(_appConfig, getActivity());
		
		try {
			xml.createXmlRecuento();	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}
		
	}
	
	private void sendData() throws Exception {
		// Envíamos los datos pendientes

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
		
		LinearLayout mainHeaderLinearLayout = (LinearLayout) getActivity()
				.findViewById(R.id.headerMainLinearLayout);
		
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		ButtonColor recuento = new ButtonColor(getActivity(), Color.BLUE);

		recuento.setText("Enviar recuento");
		recuento.setTextSize(TEXT_SIZE_BUTTON);
		recuento.setWidth(BUTTONS_WIDTH);
		
		recuento.setLayoutParams(params);

		final StockManager that = this;
		
		recuento.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					
					try {
						
						if (that._isManagerPasswordMode)
						{
							
							Deposito deposito = new Deposito();
							
							deposito.InitializePersistance(_appConfig, 
									that.getActivity());
							
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
							
								Deposito deposito = new Deposito();
								
								deposito.InitializePersistance(_appConfig, 
										that.getActivity());
								
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
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
					}

					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}
		});
		
		ButtonColor inicializar = new ButtonColor(getActivity(), Color.RED);

		inicializar.setText("Inicializar Stock");
		inicializar.setTextSize(TEXT_SIZE_BUTTON);
		inicializar.setWidth(BUTTONS_WIDTH);
		
		inicializar.setLayoutParams(params);
		
		inicializar.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

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
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}
		});
		
		ButtonColor reciclado = new ButtonColor(getActivity(), Color.MAGENTA);

		reciclado.setText("Reciclado");
		reciclado.setTextSize(TEXT_SIZE_BUTTON);
		reciclado.setWidth(BUTTONS_WIDTH);
		
		reciclado.setLayoutParams(params);
		
		reciclado.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

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
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
			}
		});

		TextView space = new TextView(getActivity());
		space.setWidth(30);
		space.setLayoutParams(params);
		
		mainHeaderLinearLayout.addView(space);
		mainHeaderLinearLayout.addView(recuento);
		mainHeaderLinearLayout.addView(inicializar);
		mainHeaderLinearLayout.addView(reciclado);

	}
	private void addLine(Articulo articulo) throws Exception {

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
		codigoArticulo.setTextSize(TEXT_SIZE);
		codigoArticulo.setWidth(CODE_WIDTH);
		codigoArticulo.setLayoutParams(params);

		LabelColor articuloDescripcion = new LabelColor(getActivity(),
				Color.BLACK, true);
		articuloDescripcion.setTag(articulo);
		articuloDescripcion.setText(articulo.Descripcion);
		articuloDescripcion.setTextSize(TEXT_SIZE);
		articuloDescripcion.setWidth(DESCRIPTION_WIDTH);
		articuloDescripcion.setPaintFlags(articuloDescripcion.getPaintFlags()
				| Paint.FAKE_BOLD_TEXT_FLAG);
		articuloDescripcion.setLayoutParams(params);

		articuloDescripcion.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				StartArticuloDialog((Articulo) ((LabelColor) v).getTag());
			}
		});

		LabelColor unidadesIniciales = new LabelColor(getActivity(),
				Color.argb(255, 100, 100, 50), true, Gravity.RIGHT);
		unidadesIniciales.setText(String.valueOf(articulo.Stock));
		unidadesIniciales.setTag(articulo);
		unidadesIniciales.setTextSize(TEXT_SIZE);
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

		regularizacion.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					saveStock(arg0, 1);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
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

		intercambio.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					saveStock(arg0, 2);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
							e);
				}
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
		
		unidadesRecuento.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View view, boolean hasFocus) {
				if (!hasFocus) {
					
					Deposito deposito = new Deposito();
					
					try {
						deposito.InitializePersistance(_appConfig, 
								that.getActivity());
						
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
										
							} else {
								
								String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", getActivity());
	
								if (password.equals(Constants.MANAGER_PASSWORD)) {
									
									that._isManagerPasswordMode = true;
								
									_lastTextBox = (TextBoxColor) view;
				
									EditText textBox = (EditText) view;
									
									int unidades = Integer.parseInt(textBox.getText().toString());
									
									((Articulo) unidadesRecuento.getTag()).Stock = unidades;
									
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
						// TODO Auto-generated catch block
						_appConfig.getErrorTrace().Send(_appConfig.getUser().User,
								e);
					}

				} else {
					_lastTextBox = (TextBoxColor) view;
				}
			}
		});

		ImageView imageView = new ImageView(this._appConfig);

		if (!articulo.StockPropio)
			imageView.setImageResource(R.drawable.stock_ko);
		else
			imageView.setImageResource(R.drawable.stock_ok);

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

		_appConfig = (AppConfig) getActivity().getApplicationContext();

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

		swap.Articulo.Stock = swap.Articulo.Stock + entradas - salidas;
		swap.Articulo.Entradas = entradas;
		swap.Articulo.Salidas = salidas;
		swap.Articulo.Tipo = tipo;

		swap.Inicial.setText(String.valueOf(swap.Articulo.Stock));
		swap.Entradas.setHint(String.valueOf(0));
		swap.Entradas.setText(Constants.EMPTY_STRING);
		swap.Salidas.setHint(String.valueOf(0));
		swap.Salidas.setText(Constants.EMPTY_STRING);

		try {
			swap.Articulo.InitializePersistance(_appConfig, getActivity()
					.getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		try {
			swap.Articulo.update();
		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		// Damos de alta el movimiento de almacén

		MovimientosAlmacen movimiento = new MovimientosAlmacen();

		try {
			movimiento.InitializePersistance(_appConfig, getActivity()
					.getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		try {
			movimiento.Articulo = swap.Articulo;
			movimiento.Entradas = swap.Articulo.Entradas;
			movimiento.Salidas = swap.Articulo.Salidas;
			movimiento.Tipo = tipo;
			movimiento.TipoStock = 1;

			movimiento.save();

		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
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
			swap.Articulo.InitializePersistance(_appConfig, getActivity()
					.getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		try {
			swap.Articulo.update();
		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		// Damos de alta el movimiento de almacén

		MovimientosAlmacen movimientoDefectuoso = new MovimientosAlmacen();

		try {
			movimientoDefectuoso.InitializePersistance(_appConfig,
					getActivity().getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

		try {
			movimientoDefectuoso.Articulo = swap.Articulo;
			movimientoDefectuoso.Entradas = swap.Articulo.Entradas;
			movimientoDefectuoso.Salidas = swap.Articulo.Salidas;
			movimientoDefectuoso.Tipo = tipo;
			movimientoDefectuoso.TipoStock = 2;

			movimientoDefectuoso.save();

		} catch (Exception e) {
			_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
		}

	}

	private class SwapStorage {
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
