package net.ifeu.edicards;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.itextpdf.text.DocumentException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.widget.LinearLayout.LayoutParams;
import android.text.method.DigitsKeyListener;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.FormaPago;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.Ingresos;
import net.ifeu.edicards.Pdf.PdfAuthorization;
import net.ifeu.edicards.Pdf.PdfCreator;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.ComboBox;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Controls.TextBoxColor;
import net.ifeu.library.Devices.BlueTooth;
import net.ifeu.library.Mail.MailSender;

public class DepositManagerExtension {


// ******************************** DATA TIER **********************	
	static class DataTier {
		public static double getCantidadPagada(AppConfig config, Context context) throws Exception {
	
			Historico historico = new Historico();
			historico.InitializePersistance(config, context);
	
			return historico.getCantidadPagadaOfThisWeek(new Date());
	
		}
		
		public static double getIngresos(AppConfig config, Context context) throws Exception {
	
			Ingresos ingresos = new Ingresos();
			ingresos.InitializePersistance(config, context);
	
			return  ingresos.getTotalIngresosThisWeek(new Date());
	
		}
		
		public static boolean RestriccionIngresos(AppConfig config, Context context) throws Exception {
	
			double cantidadPagada = DepositManagerExtension.DataTier.getCantidadPagada(config, context);
			double ingresos = getIngresos(config, context);
			boolean result = Constants.MAXIMO_SIN_INGRESAR <= (cantidadPagada - ingresos);
			
			String content = "cantidad Pagada: " + String.valueOf(cantidadPagada) + Constants.NEW_LINE;
			content = content + "cantidadIngresada: " + String.valueOf(ingresos) + Constants.NEW_LINE;
			content = content + "Resultado: " + String.valueOf(result) + Constants.NEW_LINE + Constants.NEW_LINE;
			
			return  result;	
		}
		
		public static FormaPago getFormaPagoByDescripcion(String descripcion, AppConfig config) throws Exception {
			
			HashMap<String, FormaPago> itemsPago = null;
	
			itemsPago = config.getCache().getAllFormasPago();
			
			for (FormaPago item : itemsPago.values()) {
				if (item.Descripcion.equals(descripcion))
					return item;
			}
	
			return null;
	
		}
		
		public static boolean IsCustomerDataFilled(Deposito deposito) {
			return !(deposito.NIF.trim().equals(Constants.EMPTY_STRING)
					|| deposito.Nombre.trim().equals(Constants.EMPTY_STRING)
					|| deposito.Direccion1.trim().equals(Constants.EMPTY_STRING)
					|| deposito.Poblacion.trim().equals(Constants.EMPTY_STRING)
					|| deposito.Provincia.trim().equals(Constants.EMPTY_STRING)
					|| deposito.CodigoPostal.trim().equals(Constants.EMPTY_STRING));
		}
		
		public static boolean IsSerieA(ComboBox combo, AppConfig config) {
			return combo.getText().equals(config.getUser().SerialInvoiceA);
		}
		
		public static String getFiliacionCode(String value) {
			int position = value.indexOf("-");
	
			if (position >= 0)
				return value.substring(0, position);
			else
				return Constants.EMPTY_STRING;
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
	}
	// ************************** FORMAT *************************************
	
	static class Format {
		public static double round(double d, int decimalPlace) {
			BigDecimal bd = new BigDecimal(Double.toString(d));
			bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
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
			return Double.valueOf(df2.format(val).replace(",", "."));
		}
		
		
	}
	
	// ********************************** DEVICES *************************************
	
	static class Devices {
		public static boolean PrinterStatus(boolean showMessages, AppConfig config, Context context) {
			boolean isOKBluetooth = true;
			boolean isOKPrinter = false;
			PrintManager printManager = new PrintManager();
	
			try {
				if (!BlueTooth.IsEnabled()) {
					isOKBluetooth = BlueTooth.ActivateBlueTooth();
				}
	
				if (isOKBluetooth) {
	
					for (int i = 1; i < 3; i++) {
	
						if (!isOKPrinter) {
							isOKPrinter = isOKBluetooth && printManager.getStatus(context, config, showMessages);
						}
					}
				}
	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				config.getErrorTrace().Send(config.getUser().User, e);
			} finally {
				printManager.Release();
				printManager = null;
			}
	
			return isOKBluetooth && isOKPrinter;
		}
	}
	
	// ********************************** DOCUMENTS ***********************************
	
	static class Documents {
		public static void GeneratePdf(String GUID, Deposito deposito, AppConfig config) throws DocumentException, MalformedURLException, IOException {
			// Generamos los archivos pdf
	
			PdfCreator pdf = new PdfCreator(deposito, config, config);
	
			if (deposito.isDeposito()) // && _deposito.isDepositoUpdated())
				pdf.createDeposito(GUID);
			else
	
			if (deposito.isAlbaran())
				pdf.createAlbaran(GUID, DataTier.isTransferPayment(deposito.formaPago));
		}
	
		public static void GenerateAuthorization(String GUID, Deposito deposito, AppConfig config) throws FileNotFoundException, DocumentException {
			// Generamos los archivos pdf de autorización de Cuenta Corriente
	
			PdfAuthorization pdf = new PdfAuthorization(deposito, GUID, config, config);
	
			if (deposito.CCCUpdated)
				pdf.createAuthorization();
		}

		public static void sendData(Activity activity, final AppConfig config) throws Exception {
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
						// TODO Auto-generated catch block
						e.printStackTrace();
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
	
		public static void StartCustomerDataDialog(Fragment fragment) throws Exception {
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
		
		public static ComboBox addCombo(Context context, int width, LayoutParams params, List<String> items, String text) {
			ComboBox combo = new ComboBox(context, width);
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

		
		public static ButtonColor addButton(Context context, int color, String text, int size, int width, LayoutParams params) {
			ButtonColor button = new ButtonColor(context, color);

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
			button.setTag(tag);
			
			return button;
		}
	}
}
