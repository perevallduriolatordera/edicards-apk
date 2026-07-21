package net.ifeu.edicards;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.DTOLineaDeposito;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Efectivo;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.LineaHistorico;
import net.ifeu.edicards.DataTier.Reporting;
import net.ifeu.edicards.Pdf.document.IPdfDocumentGenerator;
import net.ifeu.edicards.Pdf.document.PdfDTOCreator;
import net.ifeu.edicards.Pdf.incident.IncidentPdfCreator;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.edicards.Services.ExportResult;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.edicards.Xml.XmlCreator;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Errors.ResultResponse;
import net.ifeu.library.LogBook.LogBookStock;
import net.ifeu.library.Utils.Inactivate;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.ScreenManager;

public class Reports extends Fragment {

	private final int TEXT_SIZE_BUTTON = 14;
	AppConfig _appConfig;
	private Calendar _calendar1;
	private Calendar _calendar2;
	private DatePicker _datePicker1;
	private DatePicker _datePicker2;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		_appConfig = (AppConfig) this.getActivity().getApplicationContext();
	}
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

		// Configurar DatePickers en modo spinner
		configureDatePickers();

		// Configurar botón de toggle y label de fecha
		setupDateToggle();

		FillButtonsHeader();
	}

	private void configureDatePickers() {
		LinearLayout datePickersContainer = getActivity().findViewById(R.id.datePickersContainer);

		// Crear DatePickers dinámicamente igual que en GastosManager
		_datePicker1 = new DatePicker(this.getActivity().getApplicationContext());
		_datePicker2 = new DatePicker(this.getActivity().getApplicationContext());

		// Configurar DatePicker 1 (Fecha Inicio) - reducido para más espacio
		android.widget.LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		_datePicker1.setLayoutParams(params1);
		_datePicker1.setScaleX(0.45f);
		_datePicker1.setScaleY(0.45f);
		_datePicker1.init(_calendar1.get(Calendar.YEAR),
			_calendar1.get(Calendar.MONTH),
			_calendar1.get(Calendar.DATE), null);

		// Configurar DatePicker 2 (Fecha Fin) - reducido para más espacio
		android.widget.LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params2.setMargins(5, 0, 0, 0);
		_datePicker2.setLayoutParams(params2);
		_datePicker2.setScaleX(0.45f);
		_datePicker2.setScaleY(0.45f);
		_datePicker2.init(_calendar2.get(Calendar.YEAR),
			_calendar2.get(Calendar.MONTH),
			_calendar2.get(Calendar.DATE), null);

		// Crear layout horizontal para los dos DatePickers
		final LinearLayout layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER);
		layout.removeAllViews();

		layout.addView(_datePicker1);
		layout.addView(_datePicker2);

		datePickersContainer.addView(layout);
	}

	private void setupDateToggle() {
		final LinearLayout datePickersContainer = getActivity().findViewById(R.id.datePickersContainer);
		final android.widget.TextView lblDateRange = getActivity().findViewById(R.id.lblDateRange);
		final android.widget.Button btnToggle = getActivity().findViewById(R.id.btnToggleDatePickers);

		// Actualizar el label con las fechas actuales
		updateDateRangeLabel(lblDateRange);

		// Configurar el botón de toggle
		btnToggle.setOnClickListener(v -> {
			if (datePickersContainer.getVisibility() == View.VISIBLE) {
				datePickersContainer.setVisibility(View.GONE);
				btnToggle.setText("Cambiar Fechas");
				updateDateRangeLabel(lblDateRange);
			} else {
				datePickersContainer.setVisibility(View.VISIBLE);
				btnToggle.setText("Ocultar");
			}
		});
	}

	private void updateDateRangeLabel(android.widget.TextView label) {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		String fecha1 = sdf.format(_calendar1.getTime());
		String fecha2 = sdf.format(_calendar2.getTime());
		label.setText("Desde: " + fecha1 + " hasta: " + fecha2);
	}

	private void CreateReportLayout() throws Exception {

		Historico historico = Factory.build(Historico.class, _appConfig);
		ArrayList<Historico> list = historico.getHistoricosBetweenDates(
				_calendar1.getTime(), _calendar2.getTime());

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutArticles);
		mainLinearLayout.removeAllViews();

		if (list.size() == 0) {

			_appConfig.getMessageBox().Show("Informes",
					"No se han encontrado resultados", getActivity(),
					MessageBoxType.Information);

			_appConfig.getWorkingArea().CurrentReporting = null;

			return;
		}

		Reporting reporting = new Reporting();
		for (Historico hist : list) {

			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");

			String date = formatter.format(hist.Fecha);
			if (reporting.Agrupado.containsKey(date)) {
				ArrayList<Historico> oldList = reporting.Agrupado
						.get(date);
				oldList.add(hist);
			} else {
				ArrayList<Historico> newList = new ArrayList<>();
				newList.add(hist);
				reporting.Agrupado.put(date, newList);
			}

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_NUEVO)
				reporting.totales.Nuevos = reporting.totales.Nuevos + 1;

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_BAJA)
				reporting.totales.Retirados = reporting.totales.Retirados + 1;

			if (hist.Total != 0)
				reporting.totales.Visitas = reporting.totales.Visitas + 1;

			if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceB)) {
				if (reporting.totales.InicialSerieB
						.equals(ConstantsTypes.EMPTY_STRING))
					if (hist.NumeroAlbaran != null)
						reporting.totales.InicialSerieB = hist.NumeroAlbaran;

				if (hist.NumeroAlbaran != null)
					reporting.totales.FinalSerieB = hist.NumeroAlbaran;
			} else if (hist.Serie.equals(_appConfig.getUser().SerialInvoiceA)) {
				if (reporting.totales.InicialSerieA
						.equals(ConstantsTypes.EMPTY_STRING))
					if (hist.NumeroAlbaran != null)
						reporting.totales.InicialSerieA = hist.NumeroAlbaran;

				if (hist.NumeroAlbaran != null)
					reporting.totales.FinalSeriaA = hist.NumeroAlbaran;
			}

			for (LineaHistorico linea : hist.Lineas.values()) {
				if (linea.Tipo == ConstantsTypes.TIPO_LINEA_HISTORICO_POTENCIADAS) {
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

				if (linea.Tipo == ConstantsTypes.TIPO_LINEA_HISTORICO_FACTURADAS) {
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

				if (linea.Tipo == ConstantsTypes.TIPO_LINEA_HISTORICO_BAJAS) {
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

				if (linea.Tipo == ConstantsTypes.TIPO_LINEA_HISTORICO_DEFECTUOSAS) {
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

    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);

		RelativeLayout card = new RelativeLayout(this.getActivity());
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width= ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.2f).getWidth();
		params.height= ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getHeight();
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
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this.getActivity());
    	card.setBackgroundResource(R.drawable.card_background);
		card.setGravity(Gravity.CENTER);

		params.width= ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.2f).getWidth();
		params.height= ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getHeight();	card.setLayoutParams(params);
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

	private void addLine(final ArrayList<Historico> list) {

		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity()
				.findViewById(R.id.mainLinearLayoutArticles);

		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		params.setMargins(0, 0, 0, 10);

		final LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);

		int nuevos = 0;
		int retirados = 0;
		String fecha = ConstantsTypes.EMPTY_STRING;

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		for (Historico hist : list) {
			fecha = formatter.format(hist.Fecha);

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_NUEVO)
				nuevos = nuevos + 1;

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_BAJA)
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

		int TEXT_SIZE = 16;
		for (final Historico hist : list) {

			int colorText = Color.BLACK;

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_EXISTENTE)
				colorText = Color.BLUE;

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_NUEVO)
				colorText = Color.MAGENTA;

			if (hist.Tipo == ConstantsTypes.TIPO_HISTORICO_CLIENTE_BAJA)
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
			poblacion.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.1f).getWidth());
			poblacion.setLayoutParams(params);

			layout2.addView(poblacion);

			LabelColor cp = new LabelColor(this.getActivity(), colorText, true);
			cp.setText(hist.CodigoPostalPresentacion);
			cp.setTextSize(TEXT_SIZE);
			cp.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.1f).getWidth());
			cp.setLayoutParams(params);

			layout2.addView(cp);

			LabelColor nombre = new LabelColor(this.getActivity(), colorText,
					true);
			nombre.setText(hist.NombrePresentacion);
			nombre.setTextSize(TEXT_SIZE);
			nombre.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.3f).getWidth());
			nombre.setLayoutParams(params);

			layout2.addView(nombre);

			LabelColor numeroAlbaran = new LabelColor(this.getActivity(),
					colorText, true);

			String numAlb;

			if (hist.NumeroAlbaran == null)
				numAlb = "Solo depósito";
			else
				numAlb = "Albarán núm: " + hist.NumeroAlbaran;

			numeroAlbaran.setText(numAlb);
			numeroAlbaran.setTextSize(TEXT_SIZE);
			numeroAlbaran.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.1f).getWidth());
			numeroAlbaran.setLayoutParams(params);

			layout2.addView(numeroAlbaran);

			DecimalFormat df = new DecimalFormat("0.00");
			LabelColor total = new LabelColor(this.getActivity(), colorText,
					true);
			total.setText("(" +df.format(hist.Total) + " €)");
			total.setTextSize(TEXT_SIZE);
			total.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.1f).getWidth());
			total.setLayoutParams(params);

			layout2.addView(total);

			color = Color.parseColor("#1976D2");
			ButtonColor reImpresion = new ButtonColor(this.getActivity(), color, getResources().getDrawable(R.drawable.ic_send));
			reImpresion.setTextSize(TEXT_SIZE_BUTTON);
			reImpresion.setText("Reimprimir");
			reImpresion.setTextColor(Color.WHITE);
			reImpresion.setBackgroundResource(R.drawable.button_primary);
			reImpresion.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth());

			reImpresion.setLayoutParams(params);

			reImpresion.setOnClickListener(arg0 -> {
				

				DTODeposito dto = new DTODeposito(_appConfig, getActivity());
				dto.deserialize(hist.Serializacion);

				try {
					dto.Calculate();
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}
				try {
					dto.CalculateDeposito();
				} catch (Exception e1) {
					throw new RuntimeException(e1);
				}

				DepositoModalidad modalidad = hist.getModalidad();
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

				// Comprobamos que el dispositivo esté funcionando
				// correctamente

				PrintManager printManager = new PrintManager();

				boolean cancelStartPrint;
				boolean printerStatus = false;

				do {

					for (int i = 1; i < 3; i++) {
						if (!printerStatus) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							printerStatus = printManager.getStatusDTO(
									getActivity().getApplicationContext(),
									_appConfig, false);
						}
					}

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
										_appConfig, _appConfig, hist.GUID, modalidad);
							} catch (Exception e) {
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

					if (dto.isAlbaran()) {
						do {

							ResultResponse resultResponse;
							try {

								resultResponse = printManager.printAlbaran(dto,
										getActivity(), _appConfig,
										hist.GUID,
										DepositManagerExtension.DataTier.isTransferPayment(dto.PagoDescripcion),
										modalidad);
								result = resultResponse.Success;

							} catch (Exception e) {
								resultResponse = new ResultResponse(false, e.getMessage());
								result = false;
							}
							if (!result)
								cancel = _appConfig
										.getMessageBox()
										.ShowWithResult(
												"Impresión de albarán",
												"No se pudo imprimir el albarán. \n Motivo: " + resultResponse.Message +
												"\n Desea volverlo a intentar?",
												getActivity(),
												MessageBoxType.Information);
						} while (cancel);
					}

					if (result)
						printManager.Release();
				}
			});

			layout2.addView(reImpresion);

			color = Color.parseColor("#FF9800");
			ButtonColor anular = new ButtonColor(this.getActivity(), color, getResources().getDrawable(R.drawable.ic_recycled));
			anular.setTextSize(TEXT_SIZE_BUTTON);
			anular.setText("Anular");
			anular.setTextColor(Color.WHITE);
			anular.setBackgroundResource(R.drawable.button_warning);
			anular.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth());
			anular.setLayoutParams(params);
			
			final Reports that = this;

			anular.setOnClickListener(arg0 -> {
				try {

                    boolean drop = _appConfig
							.getMessageBox()
							.ShowWithResult(
									"Anular operación",
									"Se va a proceder a anular la operación. Desea Continuar?",
									getActivity(),
									MessageBoxType.Information);

					if (drop) {

						DTODeposito dto = new DTODeposito(_appConfig, getActivity());
						dto.deserialize(hist.Serializacion);

						that.upgradeStock(hist);
						that.restoreEfectivo(dto);

						Deposito depositoRestaurado = null;
						if (!dto.isNTV) {
							depositoRestaurado = that.upgradeDeposito(hist, dto);
							if (depositoRestaurado == null) return;

							_appConfig.getMessageBox().Show("Información",
									"Se ha restaurado de nuevo el depísito del cliente " + hist.NombrePresentacion,
									getActivity(), MessageBoxType.Information);
						} else {
							_appConfig.getMessageBox().Show("Información",
									"Al ser un pedido NTV, el depósito NO será restaurado " + hist.NombrePresentacion,
									getActivity(), MessageBoxType.Information);
						}

						that.sendIncidencia(hist);

						try {
							dto.Calculate();
						} catch (Exception e1) {
							throw new RuntimeException(e1);
						}
						try {
							dto.CalculateDeposito();
						} catch (Exception e1) {
							throw new RuntimeException(e1);
						}

						that.generateXML(dto, hist, depositoRestaurado);
						hist.delete();
						getHistoricos();
						
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

			});

			layout2.addView(anular);

			layout1.addView(layout2);
			layout.addView(layout1);

		}

		LabelColor blank = new LabelColor(this.getActivity(), color, true);
		blank.setText("     ");
		blank.setTextSize(TEXT_SIZE);
		layout.addView(blank);

		mainLinearLayout.addView(layout);

	}
	
	private void sendIncidencia(Historico historico) throws Exception {
		
		// Generamos la incidencia de anulación de albarán
		String text = "Se ha anulado el albarán con los siguientes datos: "
				+ ConstantsTypes.NEW_LINE
				+ ConstantsTypes.NEW_LINE
				+ "NÚM. ALBARÁN TABLET (RefExt): "
				+ historico.NumeroAlbaran
				+ ConstantsTypes.NEW_LINE
				+ "CLIENTE: "
				+ historico.NombrePresentacion
				+ ConstantsTypes.NEW_LINE;

		Incidencia incidencia = new Incidencia(_appConfig
				.getUser().User, new Date(),
				historico.ActualizarStock ? IncidenciaType.AlbaranAnulado : IncidenciaType.AlbaranAnuladoDesdeEdicards, text);
		incidencia.create(new IncidentPdfCreator(_appConfig));

	}
	
	private void upgradeStock(Historico historico) {

		if (!historico.ActualizarStock) return;
		_appConfig.getCache().invalidate();

		try {

			long lastId = 0;
			int lastStock = 0;
			int lastStockDefectuoso = 0;

			LogBookStock logBookWriter= Factory.build(LogBookStock.class, _appConfig);

			for (LineaHistorico linea : historico.Lineas.values()) {

				if (lastId == linea.Articulo.IdArticulo) {
					linea.Articulo.Stock = lastStock;
					linea.Articulo.StockDefectuoso = lastStockDefectuoso;
				}

				switch (linea.Tipo) {
					case ConstantsTypes.TIPO_LINEA_HISTORICO_FACTURADAS: {
						int stockInicial = linea.Articulo.Stock;
						linea.Articulo.Stock = stockInicial + linea.Unidades;

						if (stockInicial != linea.Articulo.Stock) {

							logBookWriter.setData("ANULACIÓN DE ARTÍCULO FACTURADO", historico.Cliente.CodigoCliente,
									historico.Cliente.Razon, LogBookStock.normalizeArticleCode(linea.Articulo.CodigoArticulo), linea.Articulo.Descripcion,
									stockInicial, linea.Articulo.Stock, linea.Unidades, 0,
									0, 0, 0,
									0, 0);

							logBookWriter.save();
						}
						break;
					}

					case ConstantsTypes.TIPO_LINEA_HISTORICO_POTENCIADAS: {
						int stockInicial = linea.Articulo.Stock;
						linea.Articulo.Stock = stockInicial + linea.Unidades;

						if (stockInicial != linea.Articulo.Stock) {

							logBookWriter.setData("ANULACIÓN DE ARTÍCULO POTENCIADO", historico.Cliente.CodigoCliente,
									historico.Cliente.Razon, LogBookStock.normalizeArticleCode(linea.Articulo.CodigoArticulo), linea.Articulo.Descripcion,
									stockInicial, linea.Articulo.Stock, linea.Unidades, 0,
									0, 0, 0,
									0, 0);

							logBookWriter.save();

						}
						break;
					}

					case ConstantsTypes.TIPO_LINEA_HISTORICO_BAJAS: {
						int stockInicial = linea.Articulo.Stock;
						linea.Articulo.Stock = stockInicial - linea.Unidades;

						if (stockInicial != linea.Articulo.Stock) {

							logBookWriter.setData("ANULACIÓN DE ARTÍCULO BAJA", historico.Cliente.CodigoCliente,
									historico.Cliente.Razon, LogBookStock.normalizeArticleCode(linea.Articulo.CodigoArticulo), linea.Articulo.Descripcion,
									stockInicial, linea.Articulo.Stock, 0, 0,
									linea.Unidades, 0, 0,
									0, 0);

							logBookWriter.save();
						}
						break;
					}

					case ConstantsTypes.TIPO_LINEA_HISTORICO_DEFECTUOSAS: {
						linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso
								- linea.Unidades;
						break;
					}

				}

				lastId = linea.Articulo.IdArticulo;
				lastStock = linea.Articulo.Stock;
				lastStockDefectuoso = linea.Articulo.StockDefectuoso;

				linea.Articulo.update();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private Deposito upgradeDeposito(Historico historico, DTODeposito dto) {

		try {
			Deposito deposito = Factory.build(Deposito.class, _appConfig);

			// Si el cliente es nuevo (código 99), buscar por IdDeposito del DTO
			// En caso contrario, buscar por código de cliente
			List<Deposito> deps = new ArrayList<>();
			if (historico.Cliente.CodigoCliente != null && historico.Cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {
				// Cliente nuevo: buscar el depósito específico por IdDeposito
				try {
					deposito.setDepositoById(String.valueOf(dto.IdDeposito));
					// setDepositoById siempre retorna false, pero carga el depósito
					if (deposito.IdDeposito != null && deposito.IdDeposito > 0) {
						deps.add(deposito);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				// Cliente normal: buscar por código cliente
				deps = deposito.getDepositosByCodigoCliente(historico.Cliente.CodigoCliente);
			}

			// Crear lista de artículos a restablecer para mostrar al usuario
			// Usar las líneas del DTO si es cliente nuevo, sino usar las del histórico
			StringBuilder articulosInfo = new StringBuilder();
			articulosInfo.append("Se van a restablecer los siguientes artículos en el depósito:\n\n");

			if (historico.Cliente.CodigoCliente != null && historico.Cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) {
				// Para cliente nuevo, mostrar líneas del DTO (que ya fue deserializado)
				for (DTOLineaDeposito linea : dto.Lineas.values()) {
					if (linea.UnidadesIniciales > 0) {
						articulosInfo.append("• ")
							.append(linea.CodigoArticulo)
							.append(" - ")
							.append(linea.Descripcion)
							.append(" (")
							.append(linea.UnidadesIniciales)
							.append(" unidades)\n");
					}
				}
			} else {
				// Para cliente normal, mostrar líneas del histórico
				for (LineaHistorico historicoLinea : historico.Lineas.values()) {
					if (historicoLinea.Tipo == ConstantsTypes.TIPO_LINEA_HISTORICO_UNIDADES_INICIALES) {
						articulosInfo.append("• ")
							.append(historicoLinea.Articulo.CodigoArticulo)
							.append(" - ")
							.append(historicoLinea.Articulo.Descripcion)
							.append(" (")
							.append(historicoLinea.Unidades)
							.append(" unidades)\n");
					}
				}
			}

			articulosInfo.append("\n¿Desea continuar con el restablecimiento del depósito?");

			// Mostrar diálogo de confirmación con la lista de artículos
			boolean confirmar = _appConfig.getMessageBox().ShowWithResult(
				"Restablecimiento de Depósito",
				articulosInfo.toString(),
				getActivity(),
				MessageBoxType.Information);

			if (!confirmar) {
				return null;
			}

			Cliente cliente = Factory.build(Cliente.class, _appConfig);
			cliente.setClienteById(historico.Cliente.CodigoCliente);

			if (deps.size() > 1) {
				_appConfig.getMessageBox().Show("Atención",
						"Se ha encontrado mas de un depósito para este cliente: " + historico.NombrePresentacion,
						getActivity(), MessageBoxType.Error);

				return null;
			}
			if (deps.size() == 0) {
				deposito.ClienteInfo = Factory.build(ClienteInfo.class, _appConfig);
				deposito.assingFromCliente(cliente);
				deposito.IsNtvDeposit = false;

				deposito.save();
			} else {
				deposito = deps.stream().findFirst().get();
			}

			deposito.DeleteAllLines();

			for (LineaHistorico historicoLinea : historico.Lineas.values()) {
				
				switch (historicoLinea.Tipo) {
					case ConstantsTypes.TIPO_LINEA_HISTORICO_UNIDADES_INICIALES: {

						LineaDeposito linea = Factory.build(LineaDeposito.class, _appConfig);

						linea.IdDeposito = deposito.IdDeposito;
						linea.Articulo = historicoLinea.Articulo;

						linea.UnidadesIniciales = historicoLinea.Unidades;
						linea.UnidadesInicialesFijas = linea.UnidadesIniciales;
						linea.UnidadesRepuestas = linea.UnidadesIniciales;
	
						linea.PVP = historicoLinea.PVP;
						linea.PVPAnterior = linea.PVP;
						linea.PVPInicial = linea.PVPAnterior;
						
						linea.Descuento1 = 0;
						linea.Descuento2 = 0;

						linea.save();
						deposito.Lineas.put(linea.Articulo.CodigoArticulo, linea);
						break;
					}

				}
			}

			return deposito;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void restoreEfectivo(DTODeposito deposito) throws Exception {

		if (deposito.CantidadPagada > 0) {
			Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
			efectivo.getEfectivo();
			efectivo.Efectivo = efectivo.Efectivo - deposito.CantidadPagada;
			efectivo.UpdateDateEfectivo = new Date();

			efectivo.update();
		}
	}


	private void generateXML(DTODeposito deposito, Historico historico, Deposito depositoRestaurado) {

		try {
			// No generar XML para clientes nuevos (99) si el depósito restaurado es null
			// ya que es una operación temporal que se está anulando
			// Un cliente nuevo tendrá depositoRestaurado == null después de la anulación
			if (depositoRestaurado == null &&
				((deposito.CodigoCliente != null && deposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)) ||
				 (historico.Cliente.CodigoCliente != null && historico.Cliente.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE)))) {
				return;
			}

			XmlCreator creator = new XmlCreator(_appConfig);
			creator.createXmlArticulos();

			generateAlbaran(deposito, historico, depositoRestaurado);

			DepositManagerExtension.Documents.sendData(this.getActivity(), this._appConfig);

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	private void generateAlbaran(DTODeposito deposito, Historico historico, Deposito depositoRestaurado) throws Exception {

		// Check if there are any sold items
		int totalVentas = 0;
		for (DTOLineaDeposito linea : deposito.Lineas.values()) {
			totalVentas += linea.UnidadesFacturadas;
		}

		// If no items were sold, don't generate invoice
		if (totalVentas == 0) {
			return;
		}

		Contador contador = Factory.build(Contador.class, _appConfig);
		contador.getContadores();

		if (deposito.Serie.equals(_appConfig.getUser().SerialInvoiceA)) {
			contador.ContadorSerieA = contador.ContadorSerieA + 1;
			deposito.NumeroAlbaran = String.valueOf(contador.ContadorSerieA);
		} else {
			contador.ContadorSerieB = contador.ContadorSerieB + 1;
			deposito.NumeroAlbaran = String.valueOf(contador.ContadorSerieB);
		}

		contador.update();
		deposito.cancel();
		deposito.Calculate();
		
		XmlCreator creator = new XmlCreator(_appConfig);
		creator.createXmlAlbaran(deposito, historico.NumeroAlbaran);
		
		if (depositoRestaurado != null) {
			creator.createXmlDeposito(depositoRestaurado);
		} else {
			creator.createXmlDeposito(deposito);
		}

		IPdfDocumentGenerator pdf = new PdfDTOCreator(deposito, _appConfig);
		try {
			pdf.createAlbaran(historico.GUID, DepositManagerExtension.DataTier.isTransferPayment(deposito.PagoDescripcion), historico.getModalidad());
		} catch (Exception e) {
			DepositManagerExtension.Incidencias.createErrorPdfDocument(_appConfig, deposito.getDeposito(), e);
		}
		
		try {
			this.sendData();	
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private void sendData()  {

		final ProgressDialog progressDialog;
		progressDialog = ProgressDialog.show(this.getActivity(), "Enviando Datos a Central",
				"Enviando...Espere unos instantes", true);

		final Context context = _appConfig;

		new Thread() {

			@Override
			public void run() {
				try {
					ServiceWorker worker = new ServiceWorker();
					worker.RunExport(context);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				progressDialog.dismiss();

			}

		}.start();
	}

	
	private void FillButtonsHeader() {

		Reports that = this;

		// Layout params moderno con margen
		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(8, 8, 8, 8);

		// Nuevo Layout
		final LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER);
		layout.setPadding(8, 8, 8, 8);

		// Botón Ver - Estilo moderno azul
		ButtonColor ver = new ButtonColor(getActivity(), Color.parseColor("#1976D2"), getResources().getDrawable(R.drawable.ic_view_all));
		ver.setText("Ver");
		ver.setTextSize(TEXT_SIZE_BUTTON);
		ver.setTextColor(Color.WHITE);
		ver.setBackgroundResource(R.drawable.button_primary);
		ver.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth());
		ver.setLayoutParams(params);

		ver.setOnClickListener(v -> {

			try {

				if (Inactivate.inactivateIfNecessary()) {
					that.getActivity().finish();
					System.exit(0);
				}

				getHistoricos();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});


		// Botón Potenciados - Estilo moderno verde
		ButtonColor potenciados = new ButtonColor(getActivity(), Color.parseColor("#4CAF50"), getResources().getDrawable(R.drawable.ic_potenciados));
		potenciados.setText("Potenciados");
		potenciados.setTextSize(TEXT_SIZE_BUTTON);
		potenciados.setTextColor(Color.WHITE);
		potenciados.setBackgroundResource(R.drawable.button_success);
		potenciados.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth());
		potenciados.setLayoutParams(params);

		potenciados.setOnClickListener(arg0 -> {
			
			try {
				if (_appConfig.getWorkingArea().CurrentReporting != null)
					StartPotenciadosDialog();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		// Botón Retirados - Estilo moderno naranja
		ButtonColor retirados = new ButtonColor(getActivity(), Color.parseColor("#FF9800"), getResources().getDrawable(R.drawable.ic_retirados));
		retirados.setText("Retirados");
		retirados.setTextSize(TEXT_SIZE_BUTTON);
		retirados.setTextColor(Color.WHITE);
		retirados.setBackgroundResource(R.drawable.button_warning);
		retirados.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth());
		retirados.setLayoutParams(params);

		retirados.setOnClickListener(arg0 -> {

			try {
				if (_appConfig.getWorkingArea().CurrentReporting != null)
					StartRetiradosDialog();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		// Botón Piezas - Estilo moderno gris
		ButtonColor piezas = new ButtonColor(getActivity(), Color.parseColor("#757575"), getResources().getDrawable(R.drawable.ic_piezas));
		piezas.setText("Piezas");
		piezas.setTextSize(TEXT_SIZE_BUTTON);
		piezas.setTextColor(Color.WHITE);
		piezas.setBackgroundResource(R.drawable.button_secondary);
		piezas.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth());
		piezas.setLayoutParams(params);

		piezas.setOnClickListener(arg0 -> {
			if (_appConfig.getWorkingArea().CurrentReporting != null)
				StartPiezasDialog();
		});

		// Botón Totales - Estilo moderno azul
		ButtonColor totales = new ButtonColor(getActivity(), Color.parseColor("#1976D2"), getResources().getDrawable(R.drawable.ic_totals));
		totales.setText("Totales");
		totales.setTextSize(TEXT_SIZE_BUTTON);
		totales.setTextColor(Color.WHITE);
		totales.setBackgroundResource(R.drawable.button_primary);
		totales.setWidth(ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(),  0.15f).getWidth());
		totales.setLayoutParams(params);

		totales.setOnClickListener(arg0 -> {
			if (_appConfig.getWorkingArea().CurrentReporting != null)
				StartTotalesDialog();
		});

		layout.addView(ver);
		layout.addView(potenciados);
		layout.addView(retirados);
		layout.addView(piezas);
		layout.addView(totales);

		LinearLayout footerLinearLayout = this.getActivity()
				.findViewById(R.id.headerReportButtonsLinearLayout);
		footerLinearLayout.removeAllViews();
		footerLinearLayout.setOrientation(LinearLayout.VERTICAL);
		footerLinearLayout.addView(layout);
	}

	private void StartPotenciadosDialog() {

		_appConfig = (AppConfig) getActivity().getApplicationContext();
		Intent intent = new Intent(this.getActivity(),
				Reporting_Potenciados.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivityForResult(intent, 1);
	}

	private void StartRetiradosDialog() {

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

	private void getHistoricos() {
		try {

			_calendar1.set(_datePicker1.getYear(), _datePicker1.getMonth(),
					_datePicker1.getDayOfMonth());
			_calendar2.set(_datePicker2.getYear(), _datePicker2.getMonth(),
					_datePicker2.getDayOfMonth());

			CreateReportLayout();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
