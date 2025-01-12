package net.ifeu.edicards;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.itextpdf.text.DocumentException;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.DataTier.IncidenciaType;
import net.ifeu.edicards.DataTier.IngresoDiario;
import net.ifeu.edicards.DataTier.Ingresos;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.Pdf.document.IPdfDocumentGenerator;
import net.ifeu.edicards.Pdf.document.PdfAlmacenCreator;
import net.ifeu.edicards.Pdf.authorization.PdfAuthorization;
import net.ifeu.edicards.Pdf.document.PdfCreator;
import net.ifeu.edicards.Pdf.incident.IncidentPdfCreator;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.ComboBox;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class DepositManagerExtension {


// ******************************** DATA TIER **********************	
	static class DataTier {
		public static double getCantidadPagada(AppConfig config) throws Exception {
	
			Historico historico = Factory.build(Historico.class, config);
			return historico.getCantidadPagadaOfThisWeek(new Date());
		}

		public static Optional<Double> getCantidadPagadaDiaria(AppConfig config) throws Exception {
			Historico historico = Factory.build(Historico.class, config);
			return historico.getCantidadPagadaLastDay();
		}
		
		public static double getIngresos(AppConfig config) throws Exception {
			Ingresos ingresos = Factory.build(Ingresos.class, config);
			return  ingresos.getTotalIngresosThisWeek(new Date());
		}
		
		public static boolean calculateCantidadIngresos(AppConfig config) throws Exception {
	
			double cantidadPagada = DepositManagerExtension.DataTier.getCantidadPagada(config);
			double ingresos = getIngresos(config);

			return ConstantsTypes.MAXIMO_SIN_INGRESAR <= (cantidadPagada - ingresos);
		}

		public static Optional<Double> getIngresosDiarios(AppConfig config) throws Exception {
			return DepositManagerExtension.DataTier.getCantidadPagadaDiaria(config);
		}
		
		public static FormaPago getFormaPagoByDescripcion(String descripcion, AppConfig config) {
			
			HashMap<String, FormaPago> itemsPago;
	
			itemsPago = config.getCache().getAllFormasPago();
			
			for (FormaPago item : itemsPago.values()) {
				if (item.Descripcion.equals(descripcion))
					return item;
			}
	
			return null;
	
		}
		
		public static boolean IsCustomerDataFilled(Deposito deposito) {
			return !(deposito.NIF.trim().equals(ConstantsTypes.EMPTY_STRING)
					|| deposito.Nombre.trim().equals(ConstantsTypes.EMPTY_STRING)
					|| deposito.Direccion1.trim().equals(ConstantsTypes.EMPTY_STRING)
					|| deposito.Poblacion.trim().equals(ConstantsTypes.EMPTY_STRING)
					|| deposito.Provincia.trim().equals(ConstantsTypes.EMPTY_STRING)
					|| deposito.CodigoPostal.trim().equals(ConstantsTypes.EMPTY_STRING));
		}

		public static boolean isCustomerAttachedFilled(AppConfig appConfig) {
			return !StringUtils.isEmpty(appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerFrontDocument)
					&& !StringUtils.isEmpty(appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerBackDocument);
		}
		
		public static boolean IsSerieA(ComboBox combo, AppConfig config) {
			return combo.getText().equals(config.getUser().SerialInvoiceA);
		}
		
		public static String getFiliacionCode(String value) {
			int position = value.indexOf("-");
	
			if (position >= 0)
				return value.substring(0, position);
			else
				return ConstantsTypes.EMPTY_STRING;
		}

		public static boolean isTransferPayment(FormaPago pago) {
			return pago.CodigoFormaPago.equals("T 30")
					|| pago.CodigoFormaPago.equals("003")
					|| pago.CodigoFormaPago.equals("0062")
					|| pago.CodigoFormaPago.equals("0040")
					|| pago.CodigoFormaPago.equals("0061")
					|| pago.CodigoFormaPago.equals("0064");
		}

		public static boolean isTransferPayment(String descripcion) {
			return descripcion.startsWith("TRANSFERENCIA 30 DIAS F.F.")
					|| descripcion.equals("TRANSFERENCIA")
					|| descripcion.startsWith("HACER TRANSFERENCIA 60 D.F.F.")
					|| descripcion.startsWith("HACER TRANSFERENCIA 30 D.F.F.")
					|| descripcion.startsWith("HACER TRANSFERENCIA")
					|| descripcion.startsWith("HACE TRANSFERENCIA (30 Y 60 D)");
		}

		public static List<LineaDeposito> getSortedLineasDeposito(Deposito deposuito, boolean isNtvImport) {

			List<LineaDeposito> lines = new ArrayList<>();
			List<LineaDeposito> potentialLines = new ArrayList<>();
			List<LineaDeposito> ntvLines = new ArrayList<>();

			for (LineaDeposito linea : deposuito.Lineas.values()) {

				linea.PVPAnterior = linea.PVP;
				if (!isNtvImport) {
					if (linea.IsNew)
						potentialLines.add(linea);
					else
						lines.add(linea);
				} else {
					if (linea.IsNtvLine)
						ntvLines.add(linea);
					else if (!linea.IsNew)
						lines.add(linea);
					else
						potentialLines.add(linea);
				}
			}

			Collections.sort(lines, new LineaDeposito().new ArticuloComparator());
			Collections.sort(ntvLines, new LineaDeposito().new ArticuloComparator());
			Collections.sort(potentialLines, new LineaDeposito().new ArticuloComparator());

			lines.addAll(ntvLines);
			lines.addAll(potentialLines);
			return lines;
		}

		public static boolean RestriccionIngresosDiaria(AppConfig appConfig) {
			try {
				Optional<Double> cantidad = DepositManagerExtension.DataTier.getIngresosDiarios(appConfig);
				if (cantidad.isPresent() && cantidad.get() > 0) {

					IngresoDiario ingresoDiario = Factory.build(IngresoDiario.class, appConfig);
					IngresoDiario ingresoDiarioFromDate = ingresoDiario.getIngresosDiariosFromDate();

					return ingresoDiarioFromDate == null;
				}
				return false;

			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

	public static boolean RestriccionIngresosFromCantidad(AppConfig appConfig) {
		try {
			if (DepositManagerExtension.DataTier.calculateCantidadIngresos(appConfig)) {

				appConfig.getMessageBox().Show("Atención",
						"Ha superado los " + ConstantsTypes.MAXIMO_SIN_INGRESAR
								+ " € pendientes de ingresar. Realice un ingreso para poder seguir trabajando",
						appConfig, MessageBoxType.Error);

				return true;

			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return false;
	}
	}
	// ************************** FORMAT *************************************
	
	public static class Format {
		public static double round(double d, int decimalPlace) {
			BigDecimal bd = new BigDecimal(Double.toString(d));
			bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
			return bd.doubleValue();
		}
		
		public static String CurrencyFormat(double value) {
			DecimalFormat df = new DecimalFormat("0.00");
			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
	
			symbols.setDecimalSeparator('.');
			symbols.setGroupingSeparator(' ');
			df.setDecimalFormatSymbols(symbols);
			return df.format(value);
		}
	
		public static double RoundTo2Decimals(double val) {
			DecimalFormat df2 = new DecimalFormat("0.00");
			return Double.parseDouble(df2.format(val).replace(",", "."));
		}
		
		
	}

	// ********************************** DOCUMENTS ***********************************
	
	static class Documents {
		public static void GeneratePdf(String GUID, Deposito deposito, AppConfig config) throws IOException, DocumentException {
			// Generamos los archivos pdf
	
			IPdfDocumentGenerator pdf = new PdfCreator(deposito, config);
			IPdfDocumentGenerator pdfAlmacen = new PdfAlmacenCreator(deposito,config);
	
			if (deposito.isDeposito()) { // && _deposito.isDepositoUpdated())
				try {
					pdf.createDeposito(GUID);
				} catch (Exception e) {
					Incidencias.createErrorPdfDocument(config, deposito, e);
				}
			}

			if (!TextUtils.isEmpty(deposito.NumeroAlbaran)) {

				try {
					pdf.createAlbaran(GUID, DataTier.isTransferPayment(deposito.FormaPago));
				} catch (Exception e) {
					Incidencias.createErrorPdfDocument(config, deposito, e);
				}

				if (config.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards) {
					try {
						pdfAlmacen.createAlbaran(GUID, DataTier.isTransferPayment(deposito.FormaPago));
					} catch (Exception e) {
						Incidencias.createErrorPdfDocument(config, deposito, e);
					}
				}
			}
		}
	
		public static void GenerateAuthorization(String GUID, Deposito deposito, AppConfig config) throws FileNotFoundException, DocumentException {
			// Generamos los archivos pdf de autorización de Cuenta Corriente
	
			PdfAuthorization pdf = new PdfAuthorization(deposito, GUID, config);
	
			if (deposito.CCCUpdated)
				pdf.createAuthorization();
		}

		public static void sendData(Activity activity, final AppConfig config)  {
			// Envíamos los datos pendientes

			final ProgressDialog progressDialog;
			progressDialog = ProgressDialog.show(activity, "Enviando Datos a Central",
					"Enviando...Espere unos instantes", true);

			new Thread() {

				@Override
				public void run() {
					
					ServiceWorker worker = new ServiceWorker();
					try {
						worker.RunExport(config);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}new ServiceWorker();
					progressDialog.dismiss();

				}

			}.start();
		}
	}
	
	// ********************************** DIALOGS *************************************
	static class Dialogs {
	
		public static void StartArticuloDialog(Articulo articulo, Fragment fragment) {
			AppConfig config = (AppConfig) fragment.getActivity().getApplicationContext();
			config.getWorkingArea().CurrentArticulo = articulo;
			Intent intent = new Intent(fragment.getActivity(), ArticleDialog.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}
	
		public static void StartTotalesDialog( Fragment fragment) {
			Intent intent = new Intent(fragment.getActivity(), Totals.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}

		public static void StartIngresoDiarioDialog(Fragment fragment) {
			Intent intent = new Intent(fragment.getActivity(), IngresosDiarios.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}
		public static void StartCustomerDataDialog(Fragment fragment) {
			Intent intent = new Intent(fragment.getActivity(), CustomerData.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}
	
		public static void StartSignatureCustomerDialog(Fragment fragment) {
			Intent intent = new Intent(fragment.getActivity(), SignatureCustomer.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}
	
		public static void StartSignatureVendorDialog(Fragment fragment) {
			Intent intent = new Intent(fragment.getActivity(), SignatureVendor.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}
		
		public static void StartCustomerSearchDialog(Fragment fragment) {
			Intent intent = new Intent(fragment.getActivity(), AlbaranCustomerSearch.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			fragment.startActivityForResult(intent, 1);
		}

		public static void StartAttachmentsDialog(Activity activity) {
			Intent intent = new Intent(activity, AttachmentsDialog.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivityForResult(intent, 1);
		}

		public static void StartIngresoDiarioDialog(Activity activity) {
			Intent intent = new Intent(activity, IngresoDiarioDialog.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			activity.startActivityForResult(intent, 1);
		}
	}

	// ********************************** INCIDENCIAS ****************************

	static class Incidencias{

		public static void createIncidenciaBajaCliente(AppConfig appConfig, Deposito deposito) {
			String text = "Se ha dado de baja el deposito con los siguientes datos: " + ConstantsTypes.NEW_LINE
					+ ConstantsTypes.NEW_LINE + "NUM. DEPOSITO DIMONI: " + deposito.NumDoc
					+ ConstantsTypes.NEW_LINE + "NÚM. DEPOSITO TABLET (RefExt): " + deposito.IdDeposito
					+ ConstantsTypes.NEW_LINE + "NIF/CIF:" + deposito.NIF + ConstantsTypes.NEW_LINE + "NOMBRE: "
					+ deposito.Nombre + ConstantsTypes.NEW_LINE + "RAZON: " + deposito.Razon
					+ ConstantsTypes.NEW_LINE + "MOTIVO DE LA BAJA: " + deposito.MotivoRetirado
					+ ConstantsTypes.NEW_LINE;


			Incidencia incidencia = new Incidencia(appConfig.getUser().User, new Date(),
					IncidenciaType.BajaCliente, text);
			try {
				incidencia.create(new IncidentPdfCreator(appConfig));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public static void createIncidenciaNuevoCliente(AppConfig appConfig, Deposito deposito) {
			String text = "Se ha creado un nuevo cliente con los siguientes datos: " + ConstantsTypes.NEW_LINE
					+ ConstantsTypes.NEW_LINE + "NIF/CIF: " + deposito.NIF +
					  ConstantsTypes.NEW_LINE + "NOMBRE: " + deposito.Nombre +
					  ConstantsTypes.NEW_LINE + "RAZON: " + deposito.Razon +
					  ConstantsTypes.NEW_LINE + "DIRECCIÓN:" + deposito.Direccion1 +
					  ConstantsTypes.NEW_LINE + "CP:" + deposito.CodigoPostal +
					  ConstantsTypes.NEW_LINE + "POBLACIÓN:" + deposito.Poblacion +
					  ConstantsTypes.NEW_LINE + "TELEFONO:" + deposito.Telefono1 +
					  ConstantsTypes.NEW_LINE + "MAIL:" + deposito.Mail +
					  ConstantsTypes.NEW_LINE + "NÚMERO DE CUENTA:" + deposito.ClienteInfo.CCC +
					  ConstantsTypes.NEW_LINE;

			Incidencia incidencia = new Incidencia(appConfig.getUser().User, new Date(), IncidenciaType.ClienteNuevo,
					text);

			//PVT : Versió MIREIA

			incidencia.Attachments.put("ANVERSO",
					appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerFrontDocument);

			incidencia.Attachments.put("REVERSO",
					appConfig.getWorkingArea().CurrentTransactionMetadata.NewCustomerBackDocument);


			try {
				incidencia.create(new IncidentPdfCreator(appConfig));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public static void createErrorPdfDocument(AppConfig appConfig, Deposito deposito, Exception e) {

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String stackTraceAsString = sw.toString();

			String text = "Se ha producido un error generando el documento pdf con los siguientes datos: " + ConstantsTypes.NEW_LINE
					+ ConstantsTypes.NEW_LINE + "NUM. DEPOSITO DIMONI: " + deposito.NumDoc
					+ ConstantsTypes.NEW_LINE + "NÚM. DEPOSITO TABLET (RefExt): " + deposito.IdDeposito
					+ ConstantsTypes.NEW_LINE + "NUM. ALBARÁN: " +
					(StringUtils.isEmpty(deposito.NumeroAlbaran) ? "" : deposito.NumeroAlbaran)
					+ ConstantsTypes.NEW_LINE + "NIF/CIF:" + deposito.NIF + ConstantsTypes.NEW_LINE + "NOMBRE: "
					+ deposito.Nombre + ConstantsTypes.NEW_LINE + "RAZON: " + deposito.Razon
					+ ConstantsTypes.NEW_LINE
					+ "INFORMACIÓN DEL ERROR: " + ConstantsTypes.NEW_LINE + stackTraceAsString;

			Incidencia incidencia = new Incidencia(appConfig.getUser().User, new Date(),
					IncidenciaType.ErrorDocumento, text);
			try {
				incidencia.create(new IncidentPdfCreator(appConfig));
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	// ********************************** UI *************************************
    static class UI {
		public static LabelColor addLabel(Context context, int color, int gravity, int input, String text, int size,
				int width, LayoutParams params) {
			
			LabelColor label = new LabelColor(context, color, gravity);
			label.setRawInputType(input);
			label.setText(text);
			label.setTextSize(size);
			label.setWidth(width);
			label.setLayoutParams(params);
			
			return label;
		}

		public static LabelColor addLabel(Context context, int color, int gravity, int input, String text, int size,
			LayoutParams params) {

			LabelColor label = new LabelColor(context, color, gravity);
			label.setRawInputType(input);
			label.setText(text);
			label.setTextSize(size);

			label.setLayoutParams(params);

			return label;
		}
		
		public static LabelColor addLabelByText(Context context, int color, int gravity, int input, String text, int size,
				int width, LayoutParams params) {
			
			String mask="";
            for (int i=0; i < width; i++) mask = mask.concat("A");
			
			LabelColor label = new LabelColor(context, color);
			label.setTypeface(Typeface.MONOSPACE);    //all characters the same width
			label.setRawInputType(input);
			
			label.setText(mask);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int wSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.AT_MOST);
            int hSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.AT_MOST);
            label.measure(wSpec, hSpec);
            int finalWidth = Math.max(label.getMeasuredWidth(), label.getMeasuredHeight());
            
			label.setTextSize(size);
			label.setWidth(finalWidth);
			label.setLayoutParams(params);
			
			String spaces="";
			for (int i=0; i < (width - text.length()); i++) spaces = spaces.concat(" ");
			
			switch (gravity) {
				case Gravity.RIGHT: {
					label.setText(spaces+text);
					break;
				}
				case Gravity.LEFT: {
					label.setText(text+spaces);
					break;
				}
				case Gravity.CENTER: {
					label.setText(spaces.subSequence(0, spaces.length() / 2) + text + spaces.subSequence(0, spaces.length() / 2));
					break;
				}
			}
			
			return label;
		}
		
		public static LabelColor addLabelByText(Context context, int color, int gravity, int input, String text, int size,
				int width, LayoutParams params, boolean bold) {
			
			String mask="";
            for (int i=0; i < width; i++) mask = mask.concat("A");
			
			LabelColor label = new LabelColor(context, color, bold);
			label.setTypeface(Typeface.MONOSPACE);    //all characters the same width
			label.setRawInputType(input);
			
			label.setText(mask);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int wSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.AT_MOST);
            int hSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.AT_MOST);
            label.measure(wSpec, hSpec);
            int finalWidth = Math.max(label.getMeasuredWidth(), label.getMeasuredHeight());
			
			label.setTextSize(size);
			label.setWidth(finalWidth);
			label.setLayoutParams(params);
			
			String spaces="";
			for (int i=0; i < (width - text.length()); i++) spaces = spaces.concat(" ");
			
			switch (gravity) {
				case Gravity.RIGHT: {
					label.setText(spaces+text);
					break;
				}
				case Gravity.LEFT: {
					label.setText(text+spaces);
					break;
				}
				case Gravity.CENTER: {
					label.setText(spaces.subSequence(0, spaces.length() / 2) + text + spaces.subSequence(0, spaces.length() / 2));
					break;
				}
			}
			
			return label;
		}
		
		public static LabelColor addLabelByText(Context context, int color, int gravity, int input, String text, int size,
				int width, LayoutParams params, boolean bold, Object tag) {
			
			String mask="";
            for (int i=0; i < width; i++) mask = mask.concat("A");
			
			LabelColor label = new LabelColor(context, color, bold,  gravity);
			label.setTypeface(Typeface.MONOSPACE);    //all characters the same width
			label.setRawInputType(input);
			
			label.setText(mask);
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int wSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.AT_MOST);
            int hSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.AT_MOST);
            label.measure(wSpec, hSpec);
            int finalWidth = Math.max(label.getMeasuredWidth(), label.getMeasuredHeight());
			
			label.setTextSize(size);
			label.setWidth(finalWidth);
			label.setLayoutParams(params);
			label.setTag(tag);
			
			String spaces="";
			for (int i=0; i < (width - text.length()); i++) spaces = spaces.concat(" ");
			
			switch (gravity) {
				case Gravity.RIGHT: {
					label.setText(spaces+text);
					break;
				}
				case Gravity.LEFT: {
					label.setText(text+spaces);
					break;
				}
				case Gravity.CENTER: {
					label.setText(spaces.subSequence(0, spaces.length() / 2) + text + spaces.subSequence(0, spaces.length() / 2));
					break;
				}
			}
			
			
			return label;
		}
		
		public static LabelColor addLabel(Context context, int color, int gravity, int input, String text, int size,
				int width, LayoutParams params, boolean bold) {
			
			LabelColor label = new LabelColor(context, color, bold, gravity);
			label.setRawInputType(input);
			label.setText(text);
			label.setTextSize(size);
			label.setWidth(width);
			label.setLayoutParams(params);
			
			return label;
		}
		
		public static LabelColor addLabel(Context context, int color, int gravity, int input, String text, int size,
				int width, LayoutParams params, boolean bold, Object tag) {
			
			LabelColor label = new LabelColor(context, color, bold, gravity);
			label.setRawInputType(input);
			label.setText(text);
			label.setTextSize(size);
			label.setWidth(width);
			label.setLayoutParams(params);
			label.setTag(tag);
			
			return label;
		}
		
		public static ComboBox addCombo(Context context, LayoutParams params, List<String> items, String text) {
			ComboBox combo = new ComboBox(context);
			combo.setLayoutParams(params);

			combo.setSuggestionArray(items);
			combo.setText(text);
			
			return combo;
		}
		
		public static TextBoxColor addEdit(Activity activity, int color, int gravity, String hint, int size, int width,
				LayoutParams params, boolean focusable) {
			TextBoxColor edit = new TextBoxColor(activity, color, gravity);
			edit.setHint(hint);

			edit.setTextSize(size);
			edit.setWidth(width);
			edit.setKeyListener(DigitsKeyListener.getInstance(false, true));
			edit.setHintTextColor(Color.WHITE);

			InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
			edit.setLayoutParams(params);
			
			if (focusable)
				edit.setFocusableInTouchMode(true);
			else
				edit.setFocusable(false);
			
			return edit;
		}
		
		public static TextBoxColor addEdit(Activity activity, int color, int gravity, String hint, int size, int width,
				LayoutParams params, boolean focusable, Object tag) {
			TextBoxColor edit = new TextBoxColor(activity, color, gravity);
			edit.setHint(hint);

			edit.setTextSize(size);
			edit.setWidth(width);
			edit.setTag(tag);
			edit.setKeyListener(DigitsKeyListener.getInstance(false, true));
			edit.setHintTextColor(Color.WHITE);

			InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
			edit.setLayoutParams(params);
			
			if (focusable)
				edit.setFocusableInTouchMode(true);
			else
				edit.setFocusable(false);
			
			edit.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

			return edit;
		}

		public static ButtonColor addButton(Context context, int color, String text, int size, int width, LayoutParams params, Drawable drawable) {
			ButtonColor button = new ButtonColor(context, color, drawable);

			button.setText(text);
			button.setTextSize(size);
			button.setWidth(width);
			button.setLayoutParams(params);

			return button;
		}
		
		public static ButtonColor addButton(Context context, int color, String text, int size, int width, LayoutParams params, Object tag) {
			ButtonColor button = new ButtonColor(context, color);

			button.setText(text);
			button.setTextSize(size);
			button.setWidth(width);
			button.setLayoutParams(params);
			button.setHeight(25);

			button.setTag(tag);
			
			return button;
		}

		public static void addViewsToLayout(LinearLayout layout, View ...views) {
			for (View view : views) {
				layout.addView(view);
			}
		}

		public static int getWidthOfEditText(Context context, int size, int width) {

			String mask = "";
			for (int i = 0; i < width; i++) mask = mask.concat("A");

			TextView textView = new TextView(context);
			textView.setTextSize(size);
			textView.setText(mask);

			DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
			int wSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.AT_MOST);
			int hSpec = View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.AT_MOST);
			textView.measure(wSpec, hSpec);
			return Math.max(textView.getMeasuredWidth(), textView.getMeasuredHeight());
		}

	}
}
