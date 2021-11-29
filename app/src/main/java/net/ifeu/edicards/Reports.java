package net.ifeu.edicards;

import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.LineaHistorico;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.edicards.Pdf.PdfDTOCreator;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Utils.Inactivate;
import net.ifeu.library.Utils.MessageBoxType;

public class Reports extends Fragment {

	private final int TEXT_SIZE = 16;
	private final int TEXT_SIZE_BUTTON = 14;
	private final int BUTTONS_WIDTH = 140;

	AppConfig _appConfig;
	Date _fecha1;

	Date _fecha2;
	private Calendar _calendar1;
	private Calendar _calendar2;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_reports, container, false);
	}

	@Override
	public void onResume() {

		super.onResume();
		//desactivar la app en el cas de que sigui necessari
		if (Inactivate.inactivateIfNecessary()) {
			this.getActivity().finish();
			System.exit(0);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		_calendar1 = Calendar.getInstance();
		_calendar2 = Calendar.getInstance();

		final Button acceptButton = (Button) this.getActivity().findViewById(
				R.id.btnAccept);

		final Reports that = this;

		acceptButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {

				try {

					if (Inactivate.inactivateIfNecessary()) {
						that.getActivity().finish();
						System.exit(0);
					}

					getHistoricos();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);		
				}
			}
		});

	}

	private void FillWindow() throws Exception {
		
		_appConfig = (AppConfig) this.getActivity().getApplicationContext();

		Historico historico = new Historico();
		historico.InitializePersistance(_appConfig, this.getActivity());

		ArrayList<Historico> list = historico.getHistoricosBetweenDates(
				_calendar1.getTime(), _calendar2.getTime());

		if (list.size() > 0) {
			LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
					.findViewById(R.id.mainLinearLayoutDepositManager);
			mainLinearLayout.removeAllViews();
			FillFooter();
		} else {
			_appConfig.getMessageBox().Show("Informes",
					"No se han encontrado resultados", getActivity(),
					MessageBoxType.Information);

			LinearLayout footerLinearLayout = (LinearLayout) this.getActivity()
					.findViewById(R.id.footerMainLinearLayout);
			footerLinearLayout.removeAllViews();

			LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
					.findViewById(R.id.mainLinearLayoutDepositManager);
			mainLinearLayout.removeAllViews();

			return;
		}

		Reporting reporting = new Reporting();
		for (Historico hist : list) {

			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");

			String date = formatter.format(hist.Fecha);
			if (reporting.Agrupado.containsKey(date)) {
				ArrayList<Historico> oldList = (ArrayList<Historico>) reporting.Agrupado
						.get(date);
				oldList.add(hist);
			} else {
				ArrayList<Historico> newList = new ArrayList<Historico>();
				newList.add(hist);
				reporting.Agrupado.put(date, newList);
			}

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_NUEVO)
				reporting.totales.Nuevos = reporting.totales.Nuevos + 1;

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_BAJA)
				reporting.totales.Retirados = reporting.totales.Retirados + 1;

			if (hist.Total != 0)
				reporting.totales.Visitas = reporting.totales.Visitas + 1;

			// if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceB) ||
			// hist.Serie != null)
			if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceB)) {
				if (reporting.totales.InicialSerieB
						.equals(Constants.EMPTY_STRING))
					if (hist.NumeroAlbaran != null)
						reporting.totales.InicialSerieB = hist.NumeroAlbaran;

				if (hist.NumeroAlbaran != null)
					reporting.totales.FinalSerieB = hist.NumeroAlbaran;
			} else if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceA)) {
				if (reporting.totales.InicialSerieA
						.equals(Constants.EMPTY_STRING))
					if (hist.NumeroAlbaran != null)
						reporting.totales.InicialSerieA = hist.NumeroAlbaran;

				if (hist.NumeroAlbaran != null)
					reporting.totales.FinalSeriaA = hist.NumeroAlbaran;
			}

			for (LineaHistorico linea : hist.Lineas.values()) {
				if (linea.Tipo == Constants.TIPO_LINEA_HISTORICO_POTENCIADAS) {
					if (!reporting.Potenciados
							.containsKey(linea.Articulo.Descripcion)) {
						Reporting.Potenciado potenciado = reporting.new Potenciado();
						potenciado.articulo = linea.Articulo;
						potenciado.unidades = Math.round(linea.Unidades);

						reporting.Potenciados.put(linea.Articulo.Descripcion,
								potenciado);
					} else {
						Reporting.Potenciado potenciado = reporting.Potenciados
								.get(linea.Articulo.Descripcion);
						potenciado.unidades = potenciado.unidades
								+ Math.round(linea.Unidades);
					}
				}

				if (linea.Tipo == Constants.TIPO_LINEA_HISTORICO_FACTURADAS) {
					if (!reporting.Vendidos
							.containsKey(linea.Articulo.Descripcion)) {
						Reporting.Vendido vendido = reporting.new Vendido();
						vendido.articulo = linea.Articulo;
						vendido.unidades = Math.round(linea.Unidades);

						reporting.Vendidos.put(linea.Articulo.Descripcion,
								vendido);
					} else {
						Reporting.Vendido vendido = reporting.Vendidos
								.get(linea.Articulo.Descripcion);
						vendido.unidades = vendido.unidades
								+ Math.round(linea.Unidades);
					}
				}

				if (linea.Tipo == Constants.TIPO_LINEA_HISTORICO_BAJAS) {
					if (!reporting.Retirados
							.containsKey(linea.Articulo.Descripcion)) {
						Reporting.Retirado retirado = reporting.new Retirado();
						retirado.articulo = linea.Articulo;
						retirado.unidades = Math.round(linea.Unidades);

						reporting.Retirados.put(linea.Articulo.Descripcion,
								retirado);
					} else {
						Reporting.Retirado retirado = reporting.Retirados
								.get(linea.Articulo.Descripcion);
						retirado.unidades = retirado.unidades
								+ Math.round(linea.Unidades);
					}
				}

				if (linea.Tipo == Constants.TIPO_LINEA_HISTORICO_DEFECTUOSAS) {
					if (!reporting.Defectuosos
							.containsKey(linea.Articulo.Descripcion)) {
						Reporting.Defectuoso defectuoso = reporting.new Defectuoso();
						defectuoso.articulo = linea.Articulo;
						defectuoso.unidades = Math.round(linea.Unidades);

						reporting.Defectuosos.put(linea.Articulo.Descripcion,
								defectuoso);
					} else {
						Reporting.Defectuoso defectuoso = reporting.Defectuosos
								.get(linea.Articulo.Descripcion);
						defectuoso.unidades = defectuoso.unidades
								+ Math.round(linea.Unidades);
					}
				}
			}

			if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceA)) {
				if (hist.NumeroAlbaran != null) {
					reporting.totales.TotalSerieA = reporting.totales.TotalSerieA
							+ hist.Total;

					reporting.totales.CantidadPagadaSerieA = reporting.totales.CantidadPagadaSerieA
							+ hist.CantidadPagada;

					reporting.totales.NumeroSerieA = reporting.totales.NumeroSerieA + 1;
				}
			} else if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceB)) {
				if (hist.NumeroAlbaran != null) {
					reporting.totales.TotalSerieB = reporting.totales.TotalSerieB
							+ hist.Total;

					reporting.totales.CantidadPagadaSerieB = reporting.totales.CantidadPagadaSerieB
							+ hist.CantidadPagada;

					reporting.totales.NumeroSerieB = reporting.totales.NumeroSerieB + 1;
				}
			}

			reporting.totales.Facturado = reporting.totales.Facturado
					+ hist.Total;

		}

		_appConfig.getWorkingArea().CurrentReporting = reporting;

		for (ArrayList<Historico> array : reporting.Agrupado.values()) {
			addLine(array);
		}

	}
	
	private LinearLayout addInfo(String text, int color) {
		
		LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this.getActivity());
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width=200;
    	params.height=100;
    	card.setLayoutParams(params);
    	card.setPadding(20, 20, 20, 20);
    	
    	LinearLayout layout1 = new LinearLayout(this.getActivity());
		layout1.setOrientation(LinearLayout.VERTICAL);
		
		LabelColor label = new LabelColor(this.getActivity(), color, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(24);
		
    	layout1.addView(label);
    	
    	card.addView(layout1);
    	
    	layout.addView(card);
    	
    	return layout;
    	
	}

	
	private LinearLayout addCounter(String text, String value, int color) {
		
		LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this.getActivity());
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width=200;
    	params.height=100;
    	card.setLayoutParams(params);
    	card.setPadding(20, 20, 20, 20);
    	
    	LinearLayout layout1 = new LinearLayout(this.getActivity());
		layout1.setOrientation(LinearLayout.VERTICAL);
		
		LabelColor label = new LabelColor(this.getActivity(), color, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(16);
		
		
    	LabelColor label2 = new LabelColor(this.getActivity(), Color.BLACK, true, Gravity.CENTER);
		label2.setText(String.valueOf(value));
		label2.setTextSize(24);
    	
    	layout1.addView(label);
    	layout1.addView(label2);
    	
    	card.addView(layout1);
    	
    	layout.addView(card);
    	
    	return layout;
    	
	}

	private void addLine(final ArrayList<Historico> list) throws Exception {

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutDepositManager);

		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		((MarginLayoutParams) params).setMargins(0, 0, 0, 10);

		final LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);

		int nuevos = 0;
		int retirados = 0;
		String fecha = Constants.EMPTY_STRING;

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		for (Historico hist : list) {
			fecha = formatter.format(hist.Fecha);

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_NUEVO)
				nuevos = nuevos + 1;

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_BAJA)
				retirados = retirados + 1;
		}

		int color = Color.BLACK;
		
		final LinearLayout layoutLegends = new LinearLayout(this.getActivity());
		layoutLegends.setOrientation(LinearLayout.HORIZONTAL);
		layoutLegends.setLayoutParams(params);
		
		layoutLegends.addView(this.addInfo(fecha, Color.BLACK));
		layoutLegends.addView(this.addCounter("VISITAS", String.valueOf(list.size()), Color.BLUE));
		layoutLegends.addView(this.addCounter("NUEVOS", String.valueOf(nuevos), Color.MAGENTA));
		layoutLegends.addView(this.addCounter("BAJAS", String.valueOf(retirados), Color.RED));
		
		layout.addView(layoutLegends);

		for (final Historico hist : list) {

			hist.InitializePersistance(_appConfig, getActivity());

			int colorText = Color.BLACK;

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_EXISTENTE)
				colorText = Color.BLUE;

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_NUEVO)
				colorText = Color.MAGENTA;

			if (hist.Tipo == Constants.TIPO_HISTORICO_CLIENTE_BAJA)
				colorText = Color.RED;

			final LinearLayout layout1 = new LinearLayout(this.getActivity());
			layout1.setOrientation(LinearLayout.VERTICAL);

			final LinearLayout layout2 = new LinearLayout(this.getActivity());
			layout2.setOrientation(LinearLayout.HORIZONTAL);			
	    	layout2.setBackgroundResource(R.drawable.card_background);

	    	layout2.setPadding(20, 10, 20, 10);

			LabelColor poblacion = new LabelColor(this.getActivity(),
					colorText, true);
			
			poblacion.setText(hist.PoblacionPresentacion);
			poblacion.setTextSize(TEXT_SIZE);
			poblacion.setWidth(150);
			poblacion.setLayoutParams(params);

			layout2.addView(poblacion);

			LabelColor cp = new LabelColor(this.getActivity(), colorText, true);
			cp.setText(hist.CodigoPostalPresentacion);
			cp.setTextSize(TEXT_SIZE);
			cp.setWidth(75);
			cp.setLayoutParams(params);

			layout2.addView(cp);

			LabelColor nombre = new LabelColor(this.getActivity(), colorText,
					true);
			nombre.setText(hist.NombrePresentacion);
			nombre.setTextSize(TEXT_SIZE);
			nombre.setWidth(300);
			nombre.setLayoutParams(params);

			layout2.addView(nombre);

			LabelColor numeroAlbaran = new LabelColor(this.getActivity(),
					colorText, true);

			String numAlb = Constants.EMPTY_STRING;

			if (hist.NumeroAlbaran == null)
				numAlb = "Solo depósito";
			else
				numAlb = "Albarán núm: " + hist.NumeroAlbaran;

			numeroAlbaran.setText(numAlb);
			numeroAlbaran.setTextSize(TEXT_SIZE);
			numeroAlbaran.setWidth(120);
			numeroAlbaran.setLayoutParams(params);

			layout2.addView(numeroAlbaran);

			DecimalFormat df = new DecimalFormat("0.00");
			LabelColor total = new LabelColor(this.getActivity(), colorText,
					true);
			total.setText("(" +df.format(hist.Total) + " €)");
			total.setTextSize(TEXT_SIZE);
			total.setWidth(120);
			total.setLayoutParams(params);

			layout2.addView(total);

			color = Color.BLACK;
			ButtonColor reImpresion = new ButtonColor(this.getActivity(), color);
			reImpresion.setTextSize(TEXT_SIZE_BUTTON);
			reImpresion.setText("Reimpresión");
			reImpresion.setWidth(120);

			reImpresion.setLayoutParams(params);

			reImpresion.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					// TODO Auto-generated method stub

					DTODeposito dto = new DTODeposito(_appConfig, getActivity());
					dto.deserialize(hist.Serializacion);

					Log.i("Reports", "Serializacion: " + hist.Serializacion);

					try {
						dto.Calculate();
					} catch (Exception e1) {

						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e1),
								getActivity(), MessageBoxType.Error);		
					}
					try {
						dto.CalculateDeposito();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e1),
								getActivity(), MessageBoxType.Error);		
					}

					_appConfig.getWorkingArea().CurrentDepositoModalidad = hist.ActualizarStock ? DepositoModalidad.Furgoneta : DepositoModalidad.Edicards;
					boolean printDeposito = true;

					if (!dto.isDepositoUpdated()) {
						printDeposito = _appConfig
								.getMessageBox()
								.ShowWithResult(
										"Impresión de documentos",
										"El depósito no ha sido modificado. Desea imprimirlo de todos modos ?",
										getActivity(),
										MessageBoxType.Information);
					}

					// try {
					// _appConfig.getMessageBox().Show("Atención",
					// hist.Serializacion, getActivity(),
					// MessageBoxType.Error);
					// } catch (Exception e) {
					// // TODO Auto-generated catch block
					// _appConfig.getErrorTrace().Send(
					// _appConfig.getUser().User, e);
					// }

					// Comprobamos que el dispositivo esté funcionando
					// correctamente

					PrintManager printManager = new PrintManager();

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
								printerStatus = printManager.getStatusDTO(
										getActivity().getApplicationContext(),
										_appConfig, false);
							}
						}

						Log.i("Reports",
								"Get Status: " + String.valueOf(printerStatus));

						if (!printerStatus) {
							cancelStartPrint = _appConfig
									.getMessageBox()
									.ShowWithResult(
											"No se pudo iniciar la impresión",
											"No se pudo iniciar la impresión. Revise el dispositivo. Desea volverlo a intentar?",
											getActivity(),
											MessageBoxType.Information);

							printManager.Release();
						} else
							cancelStartPrint = false;
					} while (cancelStartPrint);

					if (!cancelStartPrint && printerStatus) {
						boolean result = false;
						boolean cancel = false;

						if (dto.isDeposito() && printDeposito) {
							do {
								try {
									result = printManager.printDeposito(dto,
											_appConfig, _appConfig, hist.GUID);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								if (!result)
									cancel = _appConfig
											.getMessageBox()
											.ShowWithResult(
													"Impresión de depósito",
													"No se pudo imprimir el depósito. Desea volverlo a intentar?",
													getActivity(),
													MessageBoxType.Information);
							} while (cancel);

							if (!result)
								return;

						}
						result = false;
						cancel = false;

						Log.i("Reports", "Previ a imprimir albarà com a DTO");
						Log.i("Reports",
								"Longitut Lineas: "
										+ String.valueOf(dto.Lineas.size()));
						Log.i("Reports",
								"Resultat isAlbaran;: "
										+ String.valueOf(dto.isAlbaran()));

						if (dto.isAlbaran()) {
							do {

								Log.i("Reports",
										"Entro a imprimir albarà com a DTO");
								try {
									result = printManager.printAlbaran(dto,
											getActivity(), _appConfig,
											hist.GUID);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								if (!result)
									cancel = _appConfig
											.getMessageBox()
											.ShowWithResult(
													"Impresión de albarán",
													"No se pudo imprimir el albarán. Desea volverlo a intentar?",
													getActivity(),
													MessageBoxType.Information);
							} while (cancel);
						}

						if (!result)
							return;
						else
							printManager.Release();
					}
				}

			});

			layout2.addView(reImpresion);

			color = Color.RED;
			ButtonColor anular = new ButtonColor(this.getActivity(), color);
			anular.setTextSize(TEXT_SIZE_BUTTON);
			anular.setText("Anular");
			anular.setWidth(160);
			anular.setLayoutParams(params);
			
			final Reports that = this;

			anular.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					try {

						boolean drop = _appConfig
								.getMessageBox()
								.ShowWithResult(
										"Anular operación",
										"Se va a proceder a anular la operación. Desea Continuar?",
										getActivity(),
										MessageBoxType.Information);

						if (drop) {
							Log.i("Reports", "Entro a Click Anular");
														
							that.upgradeStock(hist);
							that.upgradeDeposito(hist);
							that.sendIncidencia(hist);

														
							// CREAMOS EL NUEVO ALBARÁN DE ABONO
							
							DTODeposito dto = new DTODeposito(_appConfig, getActivity());
							dto.deserialize(hist.Serializacion);

							Log.i("Reports", "Serializacion: " + hist.Serializacion);

							try {
								dto.Calculate();
							} catch (Exception e1) {

								// TODO Auto-generated catch block
								_appConfig.getMessageBox().Show("Atención",
										_appConfig.getStackTrace(e1),
										getActivity(), MessageBoxType.Error);		
							}
							try {
								dto.CalculateDeposito();
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								_appConfig.getMessageBox().Show("Atención",
										_appConfig.getStackTrace(e1),
										getActivity(), MessageBoxType.Error);		
							}

							that.GenerateAlbaran(dto, hist);
							
							hist.delete();
							
							getHistoricos();
							
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						_appConfig.getMessageBox().Show("Atención",
								_appConfig.getStackTrace(e),
								getActivity(), MessageBoxType.Error);		
					}

				}

			});

			layout2.addView(anular);

			layout1.addView(layout2);
			layout.addView(layout1);

		}

		LabelColor blank = new LabelColor(this.getActivity(), color, true);
		blank.setText("     ");
		blank.setTextSize(TEXT_SIZE);
		//header.setLayoutParams(params);
		layout.addView(blank);

		mainLinearLayout.addView(layout);

		// this.getActivity().addContentView(layout, layoutParams);

	}
	
	private void sendIncidencia(Historico historico) throws Exception {
		
		// Generamos la incidencia de anulación de albarán
		String text = "Se ha anulado el albarán con los siguientes datos: "
				+ Constants.NEW_LINE
				+ Constants.NEW_LINE
				+ "NÚM. ALBARÁN TABLET (RefExt): "
				+ historico.NumeroAlbaran
				+ Constants.NEW_LINE
				+ "CLIENTE: "
				+ historico.NombrePresentacion
				+ Constants.NEW_LINE;

		Incidencia incidencia = new Incidencia(_appConfig
				.getUser().User, new Date(),
				historico.ActualizarStock ? IncidenciaType.AlbaranAnulado : IncidenciaType.AlbaranAnuladoDesdeEdicards, text);
		incidencia.create();

	}
	
	private void upgradeStock(Historico historico) throws Exception {

		if (!historico.ActualizarStock) return;

		long lastId = 0;
		int lastStock = 0;
		int lastStockDefectuoso = 0;
		
		for (LineaHistorico linea : historico.Lineas.values()) {
			
			if (lastId == linea.Articulo.IdArticulo) {
				linea.Articulo.Stock = lastStock;
				linea.Articulo.StockDefectuoso = lastStockDefectuoso;
			}
			
			Log.i("Reports", "Stock: "
					+ linea.Articulo.Descripcion);
			Log.i("Reports",
					"Tipo: " + String.valueOf(linea.Tipo));
			Log.i("Reports",
					String.valueOf(linea.MovimientoStock));
			Log.i("Reports",
					String.valueOf(linea.MovimientoStockDefectuosas));
			
			linea.Articulo.InitializePersistance(
					_appConfig, _appConfig);
			
			switch (linea.Tipo) {
				case Constants.TIPO_LINEA_HISTORICO_FACTURADAS: {
					linea.Articulo.Stock = (int) (linea.Articulo.Stock
							+ linea.Unidades);
					break;
				}
				
				case Constants.TIPO_LINEA_HISTORICO_POTENCIADAS: {
					linea.Articulo.Stock = (int) (linea.Articulo.Stock
							+ linea.Unidades);
					break;
				}
				
				case Constants.TIPO_LINEA_HISTORICO_BAJAS: {
					linea.Articulo.Stock = (int) (linea.Articulo.Stock
							- linea.Unidades);
					break;
				}
				
				case Constants.TIPO_LINEA_HISTORICO_DEFECTUOSAS: {
					linea.Articulo.StockDefectuoso = (int) (linea.Articulo.StockDefectuoso
							- linea.Unidades);
					break;
				}
				
			}
			
			lastId = linea.Articulo.IdArticulo;
			lastStock = linea.Articulo.Stock;
			lastStockDefectuoso = linea.Articulo.StockDefectuoso;

			linea.Articulo.update();
		}

	}
	
	private void upgradeDeposito(Historico historico) throws Exception {
		
		try {
			Deposito deposito = new Deposito();
			deposito.InitializePersistance(_appConfig, _appConfig);
			deposito.setFirstDepositoByCliente(historico.Cliente.CodigoCliente);
			
			deposito.DeleteAllLines();
			
			for (LineaHistorico historicoLinea : historico.Lineas.values()) {
				
				//LineaDeposito linea = deposito.Lineas.get(historicoLinea.Articulo.CodigoArticulo);
				
				switch (historicoLinea.Tipo) {
					case Constants.TIPO_LINEA_HISTORICO_UNIDADES_INICIALES: {
						
						LineaDeposito linea = new LineaDeposito();
	
						linea.InitializePersistance(_appConfig, this.getActivity().getApplicationContext());
	
						linea.Articulo = historicoLinea.Articulo;
						linea.Deposito = deposito;
						
						linea.UnidadesIniciales = (int) historicoLinea.Unidades;
						linea.UnidadesInicialesFijas = linea.UnidadesIniciales;
	
						linea.PVP = (double) historicoLinea.PVP;
						//linea.PVPAnterior = linea.getPVP(_cliente, articuloInCatalgo);
						linea.PVPAnterior = linea.PVP;
						linea.PVPInicial = linea.PVPAnterior;
						
						linea.Descuento1 = 0;
						linea.Descuento2 = 0;
							
						if (linea.UnidadesIniciales > 0)
							linea.save();
						
						break;
					}
					
				}			
			}
					
			//if (deposito.isDepositoRetirado()) {
			//	deposito.delete();
			//}
		
		} catch (Exception e) {
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);		
				
		}
	}
	
	private void GenerateAlbaran(DTODeposito deposito, Historico historico) throws Exception {
		
		Contador contador = new Contador();
		contador.InitializePersistance(_appConfig, getActivity());

		contador.getContadores();

		if (deposito.Serie.equals(_appConfig.getUser().SerialInvoiceA)) {
			contador.ContadorSerieA = contador.ContadorSerieA + 1;
			deposito.NumeroAlbaran = String.valueOf(contador.ContadorSerieA);
			//historico.NumeroAlbaran = _deposito.NumeroAlbaran;
		} else {
			contador.ContadorSerieB = contador.ContadorSerieB + 1;
			deposito.NumeroAlbaran = String.valueOf(contador.ContadorSerieB);
			//historico.NumeroAlbaran = _deposito.NumeroAlbaran;
		}
		
		contador.update();
		
		deposito.cancel();
		
		deposito.Calculate();
		
		XmlCreator creator = new XmlCreator(_appConfig, _appConfig);

		creator.createXmlAlbaran(deposito, historico.NumeroAlbaran);
		
		PdfDTOCreator pdf = new PdfDTOCreator(deposito, _appConfig, _appConfig);

		pdf.createAlbaran(historico.GUID);
		
		try {
			this.sendData();	
		} catch (Exception e) {
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);		
		}
		
	}
	
	private void sendData() throws Exception {
		// Envíamos los datos pendientes

		final ProgressDialog progressDialog;
		progressDialog = ProgressDialog.show(this.getActivity(), "Enviando Datos a Central",
				"Enviando...Espere unos instantes", true);

		final Context context = _appConfig;

		new Thread() {

			@Override
			public void run() {
				try {
					Log.i("DepositManager", "Before sending");
					ServiceWorker worker = new ServiceWorker();
					worker.RunExport(context);

				} catch (Exception e) {

					//_appConfig.getErrorTrace().Send(_appConfig.getUser().User, e);
				}

				progressDialog.dismiss();

			}

		}.start();
	}

	
	private void FillFooter() throws Exception {

		_appConfig = (AppConfig) this.getActivity().getApplicationContext();

		final LinearLayout layout = new LinearLayout(this.getActivity());
		layout.removeAllViews();

		layout.setOrientation(LinearLayout.HORIZONTAL);
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		params.setMargins(15, 0, 15, 0);

		// Nuevo Layout

		final LinearLayout layout2 = new LinearLayout(this.getActivity());
		layout2.setOrientation(LinearLayout.HORIZONTAL);

		// Botón Potenciados
		ButtonColor potenciados = new ButtonColor(getActivity(), Color.BLUE);

		potenciados.setText("Potenciados");
		potenciados.setTextSize(TEXT_SIZE_BUTTON);
		potenciados.setWidth(BUTTONS_WIDTH);

		potenciados.setLayoutParams(params);

		potenciados.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					StartPotenciadosDialog();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);		
				}
			}
		});

		// Botón Retirados
		ButtonColor retirados = new ButtonColor(getActivity(), Color.MAGENTA);

		retirados.setText("Retirados");
		retirados.setTextSize(TEXT_SIZE_BUTTON);
		retirados.setWidth(BUTTONS_WIDTH);

		retirados.setLayoutParams(params);

		retirados.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				try {
					StartRetiradosDialog();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					_appConfig.getMessageBox().Show("Atención",
							_appConfig.getStackTrace(e),
							getActivity(), MessageBoxType.Error);		
				}
			}
		});

		// Botón Piezas
		ButtonColor piezas = new ButtonColor(getActivity(), Color.GRAY);

		piezas.setText("Piezas");
		piezas.setTextSize(TEXT_SIZE_BUTTON);
		piezas.setWidth(BUTTONS_WIDTH);

		piezas.setLayoutParams(params);

		piezas.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				StartPiezasDialog();
			}
		});

		// Botón Totales
		ButtonColor totales = new ButtonColor(getActivity(), Color.RED);

		totales.setText("Totales");
		totales.setTextSize(TEXT_SIZE_BUTTON);
		totales.setWidth(BUTTONS_WIDTH);

		totales.setLayoutParams(params);

		totales.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				StartTotalesDialog();
			}
		});

		
		layout2.addView(potenciados);
		layout2.addView(retirados);
		layout2.addView(piezas);
		layout2.addView(totales);

		LinearLayout footerLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.footerMainLinearLayout);
		footerLinearLayout.removeAllViews();
		footerLinearLayout.setOrientation(LinearLayout.VERTICAL);
		footerLinearLayout.addView(layout);
		footerLinearLayout.addView(layout2);
		// this.getActivity().addContentView(footerLinearLayout, layoutParams);

	}

	private void StartPotenciadosDialog() throws FileNotFoundException {

		_appConfig = (AppConfig) getActivity().getApplicationContext();

		Intent intent = new Intent(this.getActivity(),
				Reporting_Potenciados.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);
	}

	private void StartRetiradosDialog() throws FileNotFoundException {

		_appConfig = (AppConfig) getActivity().getApplicationContext();

		Intent intent = new Intent(this.getActivity(),
				Reporting_Retirados.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);
	}

	private void StartPiezasDialog() {

		_appConfig = (AppConfig) getActivity().getApplicationContext();

		Intent intent = new Intent(this.getActivity(), Reporting_Piezas.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);
	}

	private void StartTotalesDialog() {

		_appConfig = (AppConfig) getActivity().getApplicationContext();

		Intent intent = new Intent(this.getActivity(), Reporting_Totales.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		this.startActivityForResult(intent, 1);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	private void getHistoricos() {
		try {

			DatePicker picker1 = (DatePicker) getActivity().findViewById(
					R.id.dpResult1);
			DatePicker picker2 = (DatePicker) getActivity().findViewById(
					R.id.dpResult2);

			_calendar1.set(picker1.getYear(), picker1.getMonth(),
					picker1.getDayOfMonth());
			_calendar2.set(picker2.getYear(), picker2.getMonth(),
					picker2.getDayOfMonth());

			FillWindow();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_appConfig.getMessageBox().Show("Atención",
					_appConfig.getStackTrace(e),
					getActivity(), MessageBoxType.Error);		
		}
	}

}
