package net.ifeu.edicards.Printer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.R;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.Totales;
import net.ifeu.library.Devices.BlueTooth;
import net.ifeu.library.Imaging.BitmapConvertor;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.starmicronics.stario.StarIOPortException;
import com.woosim.bt.WoosimPrinter;

public class PrintDocumentsWoosim implements IPrint {

	protected static final String LANGUAGE = "ISO-8859-1";
	protected String _GUID;
	protected WoosimPrinter _woosim = null;
	protected boolean isInitialized;

	public boolean getStatus(Context context, AppConfig app,
			boolean showMessages) {

		if (_woosim != null)
			this.Release();

		_woosim = new WoosimPrinter();

		ArrayList<String> macAddress = BlueTooth
				.getBluetoothDevicesMacAddress();
		boolean result = false;
		String message = ConstantsTypes.EMPTY_STRING;

		for (String mac : macAddress) {

			int reVal = _woosim.BTConnection(mac, false);
			
			if (reVal == 1) {
				result = result || true;
				message = "DISPOSITIVO CONECTADO CORRECTAMENTE.";

			} else if (reVal == -2) {
				result = result || false;
				message = "NO SE HA PODIDO CONECTAR EL DISPOSITIVO.";

			} else if (reVal == -5) {
				result = result || false;
				message = "NO SE HA PODIDO CONECTAR EL DISPOSITIVO. DISPOSITIVO NO SINCRONIZADO";

			} else if (reVal == -6) {

				result = result || true;
				message = "DISPOSITIVO ANTERIORMENTE CONECTADO.";

			} else if (reVal == -8) {
				result = result || false;
				message = "CONEXIÓN BLUETOOTH DESHABILITADA.";
			} else {

				result = result || false;
				message = "ERROR DESCONOCIDO.";
			}
		}
		if (showMessages)
			app.getMessageBox().Show("Estado de la impresora WOOSIM", message,
					context, MessageBoxType.Information);

		return result;

	}

	public void Release() {
		_woosim.closeConnection();
	}

	protected void printHeader(Context context) throws StarIOPortException,
			IOException {

		this.PrintBitmapImage(context.getResources(),
				R.drawable.edicardsprint, 2, true);

		this.AlignCenter();

		String header = ("\nGRUP EDICIONES ESTER JAEN S L\n"
				+ "NIF: B-61806808\n"
				+ "Ediciones Ester Jaen S.L.  Pol. Ind Pla de la Bruguera\n"
				+ "C/Solsones, 68  08211\n"
				+ "Castellar del Valles  (Spain)\n"
				+ "Telfs: 902007753  937143823    Tel. Internacional +34 937143823\n"
				+ "Fax.902007754\n"
				+ "e-mail: edicards@edicards.com    Web: www.edicards.com\n\n");

		_woosim.saveSpool(LANGUAGE, header, 0, false);
		this.Print();
		this.AlignLeft();

		_woosim.saveSpool(
				LANGUAGE,
				"--------------------------------------------------------------------- \n",
				0, false);

		this.LineFeed();
		this.Print();

	}

	private void printHeaderData(Deposito deposito,
			AppConfig app, int Tipo) {

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		String pago = (Tipo == ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO) ? "\n" : "Pago: " + deposito.PagoDescripcion
				+ "\n";

		String header = ("Fecha: " + formatter.format(deposito.FechaDeposito)
				+ "  Vendedor: " + app.getUser().Name + "\n"
				+ "Codigo Cliente: " + deposito.CodigoCliente + "  Nombre: "
				+ deposito.Nombre + "\n" + "Razón: " + deposito.Razon + "\n"
				+ "NIF: " + deposito.NIF + "\n" + "Direccion: "
				+ deposito.Direccion1 + "\n" + "Cod. Postal: "
				+ deposito.CodigoPostal + "  Poblacion:  " + deposito.Poblacion
				+ "\n" + "Telefono 1: " + deposito.Telefono1 + "  Telefono 2: "
				+ deposito.Telefono2 + "  Mail:" + deposito.Mail + "\n" + pago);

		_woosim.saveSpool(LANGUAGE, header, 0, false);

		this.LineFeed();
		this.Print();

	}

	private void printHeaderFields(int tipo) {

		_woosim.saveSpool(
				LANGUAGE,
				"--------------------------------------------------------------------- \n",
				0, false);

		String total;

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN)
			total = padLeft("TOTAL", 10);
		else
			total = ConstantsTypes.EMPTY_STRING;

		String line = (padRight("COD.", 10) + padRight("DESCRIPCION", 25)
				+ padLeft("UNID.", 10) + padLeft("PRECIO.", 10) + total + "\n");

		_woosim.saveSpool(LANGUAGE, line, 0, false);

		_woosim.saveSpool(
				LANGUAGE,
				"--------------------------------------------------------------------- \n",
				0, false);
	}

	private void printTotals(Context context, AppConfig app, int tipo,
			Deposito deposito, boolean isTransferPayment, DepositoModalidad modalidad) throws Exception {

		DecimalFormat df = new DecimalFormat("0.00");

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO) {
			try {
				deposito.CalculateDeposito();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			_woosim.saveSpool(
					LANGUAGE,
					"--------------------------------------------------------------------- \n",
					0, false);

			this.Print();

		} else {

			if ((deposito.Totales.DescuentoFinanciero != 0 || deposito.Totales.DescuentoProntoPago != 0)) {
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBaseSinDte)), 10);

				_woosim.saveSpool(LANGUAGE, total + "\n", 0, true);
				this.Print();

				if (deposito.Totales.DescuentoProntoPago != 0) {
					String prontoPago = padLeft(" ", 38)
							+ padRight("DTE. COMERCIAL:", 10)
							+ padLeft(" ", 5)
							+ padRight(
									String.valueOf(df
											.format(deposito.Totales.TotalDescuentoProntoPago)),
									10);
					_woosim.saveSpool(LANGUAGE, prontoPago + "\n", 0, true);
					this.Print();
				}

				if (deposito.Totales.DescuentoFinanciero != 0) {
					String financiero = padLeft(" ", 38)
							+ padRight("DTE. FINANCIERO:", 10)
							+ padLeft(" ", 4)
							+ padRight(
									String.valueOf(df
											.format(deposito.Totales.TotalDescuentoFinanciero)),
									10);
					_woosim.saveSpool(LANGUAGE, financiero + "\n", 0, true);
					this.Print();
				}

				String neto = padLeft(" ", 38)
						+ padRight("NETO:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);

				_woosim.saveSpool(LANGUAGE, neto + "\n", 0, true);
			}

			if (deposito.Serie.equals(app.getUser().SerialInvoiceA)) {
				String suma = padLeft(" ", 38)
						+ padRight("SUMA:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);

				_woosim.saveSpool(LANGUAGE, "\n" + suma + "\n", 0, true);
				this.Print();

				String impuestos = padLeft(" ", 38)
						+ padRight("IMPUESTOS:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalIVA
										+ deposito.Totales.TotalRecargo)), 10);

				_woosim.saveSpool(LANGUAGE, impuestos + "\n", 0, true);
				this.Print();
				
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(
								String.valueOf(df.format(deposito.Totales.Total)),
								10);

				_woosim.saveSpool(LANGUAGE, "\n" + total + "\n", 0, true);
				this.Print();
			}
			
			if (deposito.Serie.equals(app.getUser().SerialInvoiceB)) {
				
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(
								String.valueOf(df.format(deposito.Totales.TotalBase)),
								10);

				_woosim.saveSpool(LANGUAGE, "\n" + total + "\n", 0, true);
				this.Print();

				_woosim.saveSpool(LANGUAGE, "\nIVA NO INCLUIDO\n", 0, true);
				this.Print();

				_woosim.saveSpool(LANGUAGE, "Forma de pago: "
						+ deposito.PagoDescripcion + "\n", 1, true);
				this.Print();

				if (isMadeInSpain(deposito.CodigoPostal))
					this.PrintBitmapImage(context.getResources(),
							R.drawable.madeinspain_image, 1, false);

			} else {
				for (Totales.Base base : deposito.Totales.Bases.values()) {

					_woosim.saveSpool(LANGUAGE, "Impuestos:\n", 0, true);
					this.Print();

					String baseText = ("\u0009" + "Base imp: "
							+ padRight(df.format(base.Base), 10)
							+ padRight(df.format(base.IvaPerc) + "%", 10)
							+ padRight(df.format(base.Iva) + " Euros", 16) + "\n");

					_woosim.saveSpool(LANGUAGE, baseText, 0, true);
					this.Print();

					String recEqText = ("\u0009" + "Rec eq: "
							+ padRight(" ", 10)
							+ padRight(df.format(base.RecargoPerc) + "%", 10)
							+ padRight(df.format(base.Recargo) + " Euros ", 16) + "\n");

					_woosim.saveSpool(LANGUAGE, recEqText, 0, true);
					this.Print();

					String formaPago = ("\nForma de pago: "
							+ deposito.PagoDescripcion + "\n");

					_woosim.saveSpool(LANGUAGE, formaPago, 1, true);
					this.Print();

				}
				
				if (deposito.Pagado) {

					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(
								context.getResources(),
								R.drawable.madeinspain_image, 1, false);

					_woosim.saveSpool(LANGUAGE,
							"Conforme firma cliente:\n\n", 0, true);
					this.Print();

					String recibido = ("HE RECIBIDO DE " + deposito.Nombre
							+ " LA CANTIDAD DE "
							+ df.format(deposito.CantidadPagada)
							+ " Euros EN CONCEPTO DEL PAGO DEL ALBARAN "
							+ app.getUser().User + "/"
							+ deposito.NumeroAlbaran + "\n\n");

					_woosim.saveSpool(LANGUAGE, recibido, 0, true);
					this.Print();

					if (deposito.CantidadPagada < deposito.Totales.Total) {
						String pendiente = ("QUEDA PENDIENTE DE PAGO LA CANTIDAD DE "
								+ df.format(deposito.Totales.Total
										- deposito.CantidadPagada)
								+ " Euros EN CONCEPTO DEL PAGO DEL ALBARAN "
								+ app.getUser().User
								+ "/"
								+ deposito.NumeroAlbaran + "\n");

						_woosim.saveSpool(LANGUAGE, pendiente, 0, false);
						this.Print();
					}

					_woosim.saveSpool(LANGUAGE, "\n", 0, false);
					this.Print();

					_woosim.saveSpool(LANGUAGE,
							"FIRMA VENDEDOR: " + app.getUser().Name + "\n\n\n",
							0, false);
					this.Print();

					this.PrintBitmapSignatureVendor();

					_woosim.saveSpool(LANGUAGE, "\n", 0, false);
					this.Print();

					_woosim.saveSpool(LANGUAGE, "FIRMA CLIENTE\n\n\n", 0, false);
					this.Print();

					this.PrintBitmapSignature();

				} else {

					_woosim.saveSpool(
							LANGUAGE,
							"\nOPERACION " +
									"" +
									"ASEGURADA EN CREDITO Y CAUCION\n\n",
							0, false);
					this.Print();

					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(
								context.getResources(),
								R.drawable.madeinspain_image, 1 , false);

					_woosim.saveSpool(LANGUAGE,
							"Conforme - Firma Cliente:\n\n\n", 0, false);
					this.Print();

					this.PrintBitmapSignature();
				}

			}

			if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN && modalidad == DepositoModalidad.Edicards) {
				_woosim.saveSpool (LANGUAGE, "MERCANCIA PENDIENTE DE ENVIO" + "\n", 0, true);
				this.Print();
			}

			if (isTransferPayment && tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN) {
				this.AlignCenter();

				String transferInfo ="\nHACER TRANSFERENCIA EN UNO DE LOS SIGUIENTES NUMEROS DE CUENTA:\n"
						+ "\n"
						+ "BANCO SABADELL\n"
						+ "ES48 0081 0470 0500 0104 3307\n\n"
						+ "LA CAIXA\n"
						+ "ES08 2100 4652 0222 0002 2712\n\n"
						+ "BANCO SANTADER\n"
						+ "ES92 0075 1133 4405 0006 9237\n\n"
						+ "Poner en el concepto: " + deposito.Nombre + " y num de albaran " +  deposito.NumeroAlbaran + "\n\n";

				_woosim.saveSpool(LANGUAGE, transferInfo, 0, false);
				this.Print();
				this.LineFeed();
			}
		}

		this.LineFeed();
		this.Print();
	}

	private void printHeaderDetail(int tipo,
			Deposito deposito) {

		DecimalFormat df = new DecimalFormat("0.00");
		List<LineaDeposito> tempList = new ArrayList<>(deposito.Lineas.values());

		Collections
				.sort(tempList, new LineaDeposito().new ArticuloComparator());

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN)
			for (LineaDeposito linea : tempList) {
				if (linea.UnidadesFacturadas > 0) {
					String desc;
					if (linea.Articulo.Descripcion.length() > 25)
						desc = linea.Articulo.Descripcion.substring(0, 25);
					else
						desc = linea.Articulo.Descripcion;

					String lineaText = (padRight(linea.Articulo.CodigoArticulo,
							10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesFacturadas),
									10)
							+ padLeft(df.format(linea.PVP), 10)
							+ padLeft(
									String.valueOf(df.format(linea.PVP
											* linea.UnidadesFacturadas)), 10) + "\n");

					_woosim.saveSpool(LANGUAGE, lineaText, 0, true);

					if (linea.TotalAbono != 0) {
						String abono = (padRight(linea.Articulo.CodigoArticulo,
								10)
								+ padRight(desc, 25)
								+ padLeft(String.valueOf(linea.UnidadesAbono),
										10)
								+ padLeft(df.format(linea.PVPAbono), 10)
								+ padLeft(String.valueOf(df
										.format(linea.TotalAbono)), 10) + "\n");

						_woosim.saveSpool(LANGUAGE, abono, 0, true);
					}
				} else {
					if (linea.TotalAbono != 0) {

						String desc;
						if (linea.Articulo.Descripcion.length() > 25)
							desc = linea.Articulo.Descripcion.substring(0, 25);
						else
							desc = linea.Articulo.Descripcion;

						String lineaText = (padRight(
								linea.Articulo.CodigoArticulo, 10)
								+ padRight(desc, 25)
								+ padLeft(String.valueOf(linea.UnidadesAbono),
										10)
								+ padLeft(df.format(linea.PVPAbono), 10)
								+ padLeft(String.valueOf(df
										.format(linea.TotalAbono)), 10) + "\n");

						_woosim.saveSpool(LANGUAGE, lineaText, 0, true);
					}
				}
			}
		else {
			for (LineaDeposito linea : tempList) {
				if (linea.UnidadesRepuestas > 0) {
					String desc;
					if (linea.Articulo.Descripcion.length() > 25)
						desc = linea.Articulo.Descripcion.substring(0, 25);
					else
						desc = linea.Articulo.Descripcion;

					String lineaText = (padRight(linea.Articulo.CodigoArticulo,
							10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesRepuestas),
									10)
							+ padLeft(df.format(linea.PVPAnterior), 10) + "\n");

					_woosim.saveSpool(LANGUAGE, lineaText, 0, true);
				}
			}
		}

		this.LineFeed();
		this.Print();

	}

	protected void closePage() throws Exception {

		_woosim.saveSpool(LANGUAGE, "\n", 0, false);
		_woosim.saveSpool(
				LANGUAGE,
				"--------------------------------------------------------------------- \n",
				0, false);
		_woosim.saveSpool(
				LANGUAGE,
				"--------------------------------------------------------------------- \n",
				0, false);

		this.LineFeed();
		this.Print();
	}

	public boolean printAlbaran(Deposito deposito, Context context,
			AppConfig app, String guid, boolean istransferPayment, DepositoModalidad modalidad) {

		_GUID = guid;

		try {

			this.Initialize();

			if (deposito.Serie.equals(app.getUser().SerialInvoiceA))
				printHeader(context);

			if (deposito.Serie.equals(app.getUser().SerialInvoiceB)) {
				_woosim.saveSpool(
						LANGUAGE,
						"PRESUPUESTO: NUM " + app.getUser().User + "/"
								+ deposito.NumeroAlbaran + "\n",
						0, true);

				this.Print();
			}

			else if (!deposito.Pagado) {
				_woosim.saveSpool(
						LANGUAGE,
						"ALBARAN: NUM " + app.getUser().User + "/"
								+ deposito.NumeroAlbaran + "   ASOCIADO A FACTURA\n",
						0, true);

				this.Print();
			} else {
				_woosim.saveSpool(
						LANGUAGE,
						"ALBARAN ENTREGA: NUM " + app.getUser().User + "/"
								+ deposito.NumeroAlbaran + "\n",
						0, true);
				this.Print();
			}
			this.printHeaderData(deposito, app, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			printHeaderFields(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			printHeaderDetail(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN, deposito);
			printTotals(context, app, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN, deposito, istransferPayment, modalidad);

			closePage();

		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			return false;
		}

		return true;

	}

	public boolean printDeposito(Deposito deposito, Context context,
			AppConfig app, String guid, DepositoModalidad modalidad) {

		_GUID = guid;

		try {

			this.Initialize();
			printHeader(context);
			String depositoText = ("DEPOSITO: NUM " + app.getUser().User + "/"
					+ deposito.IdDeposito + "\n");
			_woosim.saveSpool(LANGUAGE, depositoText, 0, true);
			this.printHeaderData(deposito, app, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			printHeaderFields(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			printHeaderDetail(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO, deposito);

			printTotals(context, app, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO, deposito, false, modalidad);

			_woosim.saveSpool(LANGUAGE,
					"\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n", 0, false);

			this.Print();

			if (isMadeInSpain(deposito.CodigoPostal))
				this.PrintBitmapImage(context.getResources(),
						R.drawable.madeinspain_image, 1, false);

			this.PrintBitmapImage(context.getResources(),
					R.drawable.contract, 1 , false);

			_woosim.saveSpool(LANGUAGE, "Conforme - Firma Cliente:\n\n\n", 0, false);
			this.Print();
			this.PrintBitmapSignature();
			closePage();

		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			return false;
		}

		finally {
		}

		return true;
	}

	public void PrintBitmapSignature() throws IOException {

		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e) {}
		
		int result = _woosim.printBitmap(Environment
				.getExternalStorageDirectory().toString()
				+ "/"
				+ ConstantsFolders.FOLDER_ROOT
				+ "/"
				+ ConstantsFolders.FOLDER_FIRMAS
				+ "/"
				+ "C1_" + _GUID + ".bmp");

		if (result == 1) {
			byte[] lf = { 0x0a };
			_woosim.controlCommand(lf, lf.length);
			_woosim.controlCommand(lf, lf.length);
			byte[] ff = { 0x0c };
			_woosim.controlCommand(ff, 1);
			_woosim.printSpool(true);
		}
		
		try
		{
			Thread.sleep(3000);
		}
		catch(InterruptedException e) {}
	}

	public void PrintBitmapSignatureVendor() throws IOException {

		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e) {}
		
		int result = _woosim.printBitmap(Environment
				.getExternalStorageDirectory().toString()
				+ "/"
				+ ConstantsFolders.FOLDER_ROOT
				+ "/"
				+ ConstantsFolders.FOLDER_FIRMAS
				+ "/"
				+ "V1_" + _GUID + ".bmp");

		if (result == 1) {
			byte[] lf = { 0x0a };
			_woosim.controlCommand(lf, lf.length);
			_woosim.controlCommand(lf, lf.length);
			byte[] ff = { 0x0c };
			_woosim.controlCommand(ff, 1);
			_woosim.printSpool(true);
		}
		
		try
		{
			Thread.sleep(3000);
		}
		catch(InterruptedException e) {}	

	}

	public void PrintBitmapImage(Resources res, int source,
			double scale, boolean reduction) throws IOException {

		Bitmap bm = BitmapFactory.decodeResource(res, source);
		
		double width;
		double height;
		
		if (reduction)
		{
			width = bm.getWidth() / scale;
			height = bm.getHeight() / scale;
		}
		else
		{
			width = bm.getWidth() * scale;
			height = bm.getHeight() * scale;
		}
		
		
		BitmapConvertor convertor = new BitmapConvertor();
		convertor.convertBitmap(bm, Environment.getExternalStorageDirectory()
				.toString()
				+ "/"
				+ ConstantsFolders.FOLDER_ROOT
				+ "/"
				+ ConstantsFolders.FOLDER_FIRMAS + "/" + "TMP" + ".bmp", (int)width,
				(int)height);

		int result = _woosim.printBitmap(Environment
				.getExternalStorageDirectory().toString()
				+ "/"
				+ ConstantsFolders.FOLDER_ROOT
				+ "/"
				+ ConstantsFolders.FOLDER_FIRMAS
				+ "/"
				+ "TMP" + ".bmp");

		if (result == 1) {
			byte[] lf = { 0x0a };
			_woosim.controlCommand(lf, lf.length);
			_woosim.controlCommand(lf, lf.length);
			byte[] ff = { 0x0c };
			_woosim.controlCommand(ff, 1);
			_woosim.printSpool(true);
			_woosim.clearSpool();
		}
	}

	protected static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	protected static String padLeft(String s, int n) {
		return String.format("%1$" + n + "s", s);
	}

	protected boolean isMadeInSpain(String codigoPostal) {

		if (codigoPostal.length() < 2)
			return true;

		String codigoProvincia = codigoPostal.substring(0, 2);

		if (codigoProvincia.equals("01"))
			return false;
		if (codigoProvincia.equals("08"))
			return false;
		if (codigoProvincia.equals("17"))
			return false;
		if (codigoProvincia.equals("20"))
			return false;
		if (codigoProvincia.equals("25"))
			return false;
		if (codigoProvincia.equals("31"))
			return false;
		if (codigoProvincia.equals("43"))
			return false;
		if (codigoProvincia.equals("48"))
			return false;

		return true;
	}

	protected void Initialize() {

		if (!isInitialized) {
			byte[] init = { 0x1b, '@' };
			_woosim.controlCommand(init, init.length);
			this.isInitialized = true;
		}
	}

	protected void LineFeed() {
		byte[] lf = { 0x0a };
		_woosim.controlCommand(lf, lf.length);
	}

	protected void Print() {
		// byte[] ff ={0x0a};
		// _woosim.controlCommand(ff, 1);
		_woosim.printSpool(true);
		_woosim.clearSpool();
	}

	protected void AlignCenter() {
		byte[] ac = { 0x1b, 0x61, 1 };
		_woosim.controlCommand(ac, ac.length);
	}

	protected void AlignLeft() {
		byte[] ac = { 0x1b, 0x61, 0 };
		_woosim.controlCommand(ac, ac.length);
	}
	
	protected void finalize () {     //Destructor function 
        this.Release();
    }

}
