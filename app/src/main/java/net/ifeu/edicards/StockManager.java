package net.ifeu.edicards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEvents;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.MovimientosAlmacen;
import net.ifeu.edicards.Pdf.inventory.PdfInventoryRecycled;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.LogBook.LogBook;
import net.ifeu.library.Mediator.IMediator;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

public class StockManager extends Fragment implements IMediator {

	private AppConfig _appConfig;
	private HashMap<String, Articulo> _articulos;
	private final int TEXT_SIZE_BUTTON = 12;
	private final int BUTTONS_WIDTH = 150;

	private boolean _isManagerPasswordMode;

	private boolean _isRendered = false;
	private LinearLayout _mainLayout;

	private ListView _articlesListView;
	ArrayAdapter<Articulo> _adapter;
	List<Articulo> _listArticulos;
	
	Activity _activity;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this._isManagerPasswordMode = false;
		this._activity = getActivity();
		_appConfig = (AppConfig) _activity.getApplicationContext();

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		if (!_isRendered)
			return inflater.inflate(R.layout.activity_stock_manager, container,
					false);
		else
			return _mainLayout;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_articlesListView = (ListView) this.getActivity().findViewById(R.id.listViewArticles);

		if (!_isRendered)
			this.createSotckView(true);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private void createSotckView(boolean addHeader) {

		try {
			_articulos = _appConfig.getCache().getAllArticulos();
			_listArticulos = new ArrayList<>(_articulos.values());

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
							_activity, MessageBoxType.Information);
		} else {

			final StockManager that = this;
			_adapter = new ArrayAdapter<Articulo>(_appConfig, R.layout.list_item_stock_article, _listArticulos) {
				@NonNull
				@Override
				public View getView(int position, View convertView, @NonNull ViewGroup parent) {

					LayoutInflater layoutInflater = (LayoutInflater) _appConfig.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					if(convertView == null) {
						convertView = layoutInflater.inflate(R.layout.list_item_stock_article, parent, false);
					}

					Articulo articulo = getItem(position);
					articulo.Activo = true;

					TextView itemTextView = (TextView) convertView.findViewById(R.id.itemCodigoArticulo);
					itemTextView.setText(articulo.CodigoArticulo);

					itemTextView = (TextView) convertView.findViewById(R.id.itemDescripcionArticulo);
					itemTextView.setText(articulo.Descripcion);
					itemTextView.setOnClickListener(v -> StartArticuloDialog(articulo));

					itemTextView = (TextView) convertView.findViewById(R.id.itemUnidadesInicial);
					itemTextView.setText(String.valueOf(articulo.Stock));
					itemTextView.setTextColor(Color.BLUE);

					itemTextView = (TextView) convertView.findViewById(R.id.itemUnidadesInicialDefectuoso);
					itemTextView.setText(String.valueOf(articulo.StockDefectuoso));
					itemTextView.setTextColor(Color.RED);

					EditText editText = (EditText) convertView.findViewById(R.id.itemUnidadesRecuento);
					editText.setInputType(InputType.TYPE_CLASS_NUMBER);
					editText.setHint(String.valueOf(articulo.Stock));
					editText.setText(String.valueOf(articulo.Stock));
					editText.setOnFocusChangeListener((view, hasFocus) -> {

						if (!hasFocus) {
							Deposito deposito = Factory.build(Deposito.class, _appConfig);

							EditText textBox = (EditText) view;

							try {
								LogBook logBookWriter = Factory.build(LogBook.class, _appConfig);

								if (deposito.getDepositosToday().size() > 0) {
									_appConfig.getMessageBox().Show(
											"Atención",
											"No se puede hacer recuento de almacén, ya que ya se han producido operaciones durante el día de hoy! "
											, _activity,
											MessageBoxType.Error);

									int unidades = articulo.Stock;
									textBox.setText(unidades);

								} else {

									if (that._isManagerPasswordMode) {

										int unidades = Integer.parseInt(!Objects.equals(textBox.getText().toString(), "") ? textBox.getText().toString() : "0");

										((Articulo) editText.getTag()).Stock = unidades;
										logBookWriter.setData("ASIGNACION DE ALMACÉN", ConstantsTypes.EMPTY_STRING,
												ConstantsTypes.EMPTY_STRING, articulo.CodigoArticulo, articulo.Descripcion,
												articulo.Stock, unidades, 0, 0, 0, 0, 0, 0,0);

										logBookWriter.save();

									} else {

										String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", _activity);

										if (password.equals(ConstantsTypes.MANAGER_PASSWORD)) {

											that._isManagerPasswordMode = true;

											int unidades = Integer.parseInt(!Objects.equals(textBox.getText().toString(), "") ? textBox.getText().toString(): "0");
											articulo.Stock = unidades;

											logBookWriter.setData("ASIGNACION DE ALMACÉN", ConstantsTypes.EMPTY_STRING,
													ConstantsTypes.EMPTY_STRING, articulo.CodigoArticulo, articulo.Descripcion,
													articulo.Stock, unidades, 0, 0, 0, 0, 0, 0,0);

											logBookWriter.save();

										} else {
											_appConfig.getMessageBox().Show(
													"Error",
													"La clave introducida no es correcta",
													that.getContext(),
													MessageBoxType.Error);
										}
									}
								}
							} catch (Exception e) {
								throw new RuntimeException(e);

							}

						}
					});

					ImageView itemImageView = (ImageView) convertView.findViewById(R.id.itemImage);
					if (articulo.StockPropio)
						itemImageView.setImageResource(R.drawable.stock_ok_png);
					else
						itemImageView.setImageResource(R.drawable.stock_ko_png);

					return convertView;
				}
			};

			_articlesListView.setAdapter(_adapter);

			_isRendered = true;
			if (_articulos.size() == 0)
				_appConfig
						.getMessageBox()
						.Show("Control de almacén",
								"No se han encontrado artículos. Sincronice datos con el servidor",
								_activity, MessageBoxType.Information);
		}

		_isRendered = true;
		_mainLayout = (LinearLayout) this._activity.findViewById(R.id.mainLayoutStockManager);
	}
	
	private void SendRecuento() {
		this.SaveRecuento();
		this.CreateXmlRecuento();
		try {
			this.sendData();
		} catch (Exception e) {
			throw new RuntimeException(e);
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
					logBookWriter.setData("INICIALIZACIÓN DE ALMACÉN", ConstantsTypes.EMPTY_STRING,
							ConstantsTypes.EMPTY_STRING, articulo.CodigoArticulo, articulo.Descripcion,
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
		
		XmlCreator xml = new XmlCreator(_appConfig);
		
		try {
			xml.createXmlRecuento();	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
		
	}
	
	private void sendData()  {

		final ProgressDialog progressDialog;
		progressDialog = ProgressDialog.show(this._activity,
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

				}

				progressDialog.dismiss();

			}

		}.start();
	}
	
	private void addHeader() {
		
		LinearLayout mainHeaderButtonsLinearLayout = (LinearLayout) _activity
				.findViewById(R.id.headerButtonsLinearLayout);
		
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		ButtonColor recuento = new ButtonColor(_activity, Color.BLUE,
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
											, _activity,
									MessageBoxType.Error);
						} else {
							that.SendRecuento();

							_appConfig
							.getMessageBox()
							.Show("Control de almacén",
									"El recuento de almacén se ha enviado correctamente",
									_activity, MessageBoxType.Error);

							that.createSotckView(false);
						}

					} else {
						String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", _activity);

						if (password.equals(ConstantsTypes.MANAGER_PASSWORD)) {

							that._isManagerPasswordMode = true;

							Deposito deposito = Factory.build(Deposito.class, _appConfig);

							if (deposito.getDepositosToday().size() > 0) {
								_appConfig.getMessageBox().Show(
										"Atención",
										"No se puede enviar el recuento de almacén, ya que ya se han producido operaciones durante el día de hoy! "
												, _activity,
										MessageBoxType.Error);
							} else {
								that.SendRecuento();

								_appConfig
								.getMessageBox()
								.Show("Control de almacén",
										"El recuento de almacén se ha enviado correctamente",
										_activity, MessageBoxType.Error);

								that.createSotckView(false);

							}
						} else {
							_appConfig
							.getMessageBox()
							.Show("Control de almacén",
									"La clave indicada no es correcta",
									_activity, MessageBoxType.Error);
						}

					}

				} catch (Exception e) {
							throw new RuntimeException(e);
				}


			} catch (Exception e) {
					throw new RuntimeException(e);
			}
		});
		
		ButtonColor inicializar = new ButtonColor(_activity, Color.RED,
				getResources().getDrawable(R.drawable.ic_restart));

		inicializar.setText("Inicializar Stock");
		inicializar.setTextSize(TEXT_SIZE_BUTTON);
		inicializar.setWidth(BUTTONS_WIDTH);
		
		inicializar.setLayoutParams(params);
		
		inicializar.setOnClickListener(arg0 -> {

			try {

				String password = _appConfig.getMessageBox().InputBox("Recuento de artículo", "introduzca la contraseña", _activity);

				if (password.equals(ConstantsTypes.MANAGER_PASSWORD)) {
					that.initializeStock(false);
					_appConfig
					.getMessageBox()
					.Show("Control de almacén",
							"La inicialización de stock se ha realizado correctamente",
							_activity, MessageBoxType.Information);

					that.createSotckView(false);
				} else {
					_appConfig
					.getMessageBox()
					.Show("Control de almacén",
							"La clave introducida no es correcta",
							_activity, MessageBoxType.Information);
				}

			} catch (Exception e) {
					throw new RuntimeException(e);
			}
		});
		
		ButtonColor reciclado = new ButtonColor(_activity, Color.MAGENTA,
				getResources().getDrawable(R.drawable.ic_recycled));

		reciclado.setText("Reciclado");
		reciclado.setTextSize(TEXT_SIZE_BUTTON);
		reciclado.setWidth(BUTTONS_WIDTH);
		
		reciclado.setLayoutParams(params);
		
		reciclado.setOnClickListener(arg0 -> {

			try {

				boolean result = _appConfig.getMessageBox().ShowWithResult("Recuento de reciclado", "Está seguro que quiere reinicializar el stock", _activity, MessageBoxType.Information);

				if (result) {

					PdfInventoryRecycled pdf = new PdfInventoryRecycled(_appConfig);
					if (pdf.createInventory()) {

						that.initializeStock(true);
						that.sendData();

						that.createSotckView(false);
						_appConfig
						.getMessageBox()
						.Show("Recuento de reciclado",
								"La inicialización de stock reciclado se ha realizado correctamente",
								_activity, MessageBoxType.Information);
					} else {
						that.initializeStock(true);
						_appConfig
						.getMessageBox()
						.Show("Recuento de reciclado",
								"Se ha producido un error al generar La inicialización de stock reciclado",
								_activity, MessageBoxType.Information);
					}

				}

			} catch (Exception e) {
				throw new RuntimeException(e);

			}
		});

		TextView space = new TextView(_activity);
		space.setWidth(30);
		space.setLayoutParams(params);
		
		mainHeaderButtonsLinearLayout.addView(space);
		mainHeaderButtonsLinearLayout.addView(recuento);
		mainHeaderButtonsLinearLayout.addView(inicializar);
		mainHeaderButtonsLinearLayout.addView(reciclado);

	}

	private void StartArticuloDialog(Articulo articulo) {

		_appConfig.getWorkingArea().CurrentArticulo = articulo;

		Intent intent = new Intent(this._activity, ArticleDialog.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);
	}

	@Override
	public void notify(String event, Object payload) {

		if (Objects.equals(event, ConstantsEvents.EVENT_STOCK_CHANGED)) {
			_isRendered = false;
		}
	}
}
