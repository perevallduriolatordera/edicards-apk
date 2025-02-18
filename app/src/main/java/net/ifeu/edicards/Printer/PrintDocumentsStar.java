package net.ifeu.edicards.Printer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.starmicronics.stario.StarIOPort;
import com.starmicronics.stario.StarIOPortException;
import com.starmicronics.stario.StarPrinterStatus;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.Totales;
import net.ifeu.edicards.R;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrintDocumentsStar implements IPrint {

	protected final static String PORT = "BT:";
	protected final static String SETTINGS = "mini";
	protected String _GUID;

	public boolean getStatus(Context context, AppConfig app,
			boolean showMessages) {
		StarIOPort port;
		try {
			port = StarIOPort.getPort(PORT, SETTINGS, 10000, context);
		} catch (StarIOPortException e1) {
			return false;
		}

		StarPrinterStatus status;
		try {
			status = port.retreiveStatus();
		} catch (StarIOPortException e) {
			return false;
		}

		if (showMessages) {
			if (!status.offline) {
				if (status.receiptPaperEmpty)
					app.getMessageBox().Show("Estado de la impresora STAR",
							"La impresora está fuera de línea", context,
							MessageBoxType.Information);
				else if (status.compulsionSwitch)
					app.getMessageBox().Show("Estado de la impresora STAR",
							"La impresora está abierta", context,
							MessageBoxType.Information);
				else
					app.getMessageBox().Show("Estado de la impresora STAR",
							"La impresora está preparada para imprimir",
							context, MessageBoxType.Information);
			} else
				app.getMessageBox().Show("Estado de la impresora STAR",
						"La impresora está fuera de línea", context,
						MessageBoxType.Information);
		}

		return true;
	}

	protected void printHeader(StarIOPort port, Context context)
			throws StarIOPortException {

		this.PrintBitmapImage(context, PORT, SETTINGS, context.getResources(),
				R.drawable.edicardsprint, 540);

		byte[] outputByteBuffer;
		port.writePort(new byte[] { 0x1d, 0x57, 0x40, 0x32 }, 0, 4); // Page
																		// Area
																		// Setting
																		// <GS>
																		// <W>
																		// nL nH
																		// (nL =
																		// 64,
																		// nH =
																		// 2)

		port.writePort(new byte[] { 0x1b, 0x61, 0x01 }, 0, 3); // Center
																// Justification
																// <ESC> a n (0
																// Left, 1
																// Center, 2
																// Right)

		outputByteBuffer = ("\nGRUP EDICIONES ESTER JAEN S L\n"
				+ "NIF: B-61806808\n"
				+ "Ediciones Ester Jaen S.L.  Pol. Ind Pla de la Bruguera\n"
				+ "C/Solsones, 68  08211\n"
				+ "Castellar del Valles  (Spain)\n"
				+ "Telfs: 902007753  937143823    Tel. Internacional +34 937143823\n"
				+ "Fax.902007754\n"
				+ "e-mail: edicards@edicards.com    Web: www.edicards.com\n\n")
				.getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
		port.writePort(new byte[] { 0x1b, 0x61, 0x00 }, 0, 3); // Left Alignment
		port.writePort(new byte[] { 0x1b, 0x44, 0x02, 0x1b, 0x34, 0x00 }, 0, 6); // Setting
																					// Horizontal
																					// Tab

		outputByteBuffer = ("--------------------------------------------------------------------- \n")
				.getBytes();

		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

	}

	private void printHeaderData(StarIOPort port,
			Deposito deposito, AppConfig app, int Tipo)
			throws StarIOPortException {
		byte[] outputByteBuffer;

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		String pago = (Tipo == ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO) ? "\n" : "Pago: " + deposito.PagoDescripcion
				+ "\n";

		outputByteBuffer = ("Fecha: "
				+ formatter.format(deposito.FechaDeposito) + "  Vendedor: "
				+ app.getUser().Name + "\n" + "Codigo Cliente: "
				+ deposito.Cliente.CodigoCliente + "  Nombre: "
				+ deposito.Nombre + "\n" + "Razón: " + deposito.Razon + "\n"
				+ "NIF: " + deposito.NIF + "\n" + "Direccion: "
				+ deposito.Direccion1 + "\n" + "Cod. Postal: "
				+ deposito.CodigoPostal + "  Poblacion:  " + deposito.Poblacion
				+ "\n" + "Telefono 1: " + deposito.Telefono1 + "  Telefono 2: "
				+ deposito.Telefono2 + "  Mail:" + deposito.Mail + "\n" + pago)
				.getBytes();

		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

	}

	private void printHeaderFields(StarIOPort port, int tipo) throws StarIOPortException {
		byte[] outputByteBuffer;

		outputByteBuffer = ("--------------------------------------------------------------------- \n")
				.getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

		String total;

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN)
			total = padLeft("TOTAL", 10);
		else
			total = ConstantsTypes.EMPTY_STRING;

		outputByteBuffer = (padRight("COD.", 10) + padRight("DESCRIPCION", 25)
				+ padLeft("UNID.", 10) + padLeft("PRECIO.", 10) + total + "\n")
				.getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

		outputByteBuffer = ("--------------------------------------------------------------------- \n")
				.getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
	}

	private void printTotals(StarIOPort port, Context context, AppConfig app,
			int tipo, Deposito deposito, boolean isTransferPayment) throws StarIOPortException {

		DecimalFormat df = new DecimalFormat("0.00");
		byte[] outputByteBuffer;

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO) {
			try {
				deposito.CalculateDeposito();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			outputByteBuffer = ("--------------------------------------------------------------------- \n")
					.getBytes();
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

		} else {
			try {
				deposito.Calculate();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if ((deposito.Totales.DescuentoFinanciero != 0 || deposito.Totales.DescuentoProntoPago != 0)) {
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBaseSinDte)), 10);

				outputByteBuffer = ("\n" + total + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

				if (deposito.Totales.DescuentoProntoPago != 0) {
					String prontoPago = padLeft(" ", 38)
							+ padRight("DTE. COMERCIAL:", 10)
							+ padLeft(" ", 5)
							+ padRight(
									String.valueOf(df
											.format(deposito.Totales.TotalDescuentoProntoPago)),
									10);
					outputByteBuffer = (prontoPago + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				}

				if (deposito.Totales.DescuentoFinanciero != 0) {
					String financiero = padLeft(" ", 38)
							+ padRight("DTE. FINANCIERO:", 16)
							+ padLeft(" ", 4)
							+ padRight(
									String.valueOf(df
											.format(deposito.Totales.TotalDescuentoFinanciero)),
									10);
					outputByteBuffer = (financiero + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				}

				String neto = padLeft(" ", 43)
						+ padRight("NETO:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);
				outputByteBuffer = (neto + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			}

			if (deposito.Serie.equals(app.getUser().SerialInvoiceA)) {
				String suma = padLeft(" ", 38)
						+ padRight("SUMA:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);
				outputByteBuffer = ("\n" + suma + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

				String impuestos = padLeft(" ", 38)
						+ padRight("IMPUESTOS:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalIVA
										+ deposito.Totales.TotalRecargo)), 10);
				outputByteBuffer = (impuestos + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(
								String.valueOf(df.format(deposito.Totales.Total)),
								10);
				outputByteBuffer = (total + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

			}
			
			if (deposito.Serie.equals(app.getUser().SerialInvoiceB)) {
				
				String total = padLeft(" ", 38)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(deposito.Totales.TotalBase)), 10);

				outputByteBuffer = ("\n" + total + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

			}

			if (deposito.Serie.equals(app.getUser().SerialInvoiceB)) {
				port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																		// Emphasized
																		// Printing
																		// ON

				outputByteBuffer = ("\nIVA NO INCLUIDO\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

				outputByteBuffer = ("Forma de pago: "
						+ deposito.PagoDescripcion + "\n").getBytes();
				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

				if (isMadeInSpain(deposito.CodigoPostal))
					this.PrintBitmapImage(context, PORT, SETTINGS,
							context.getResources(), R.drawable.madeinspain_image,
							1500);

				port.writePort(new byte[] { 0x1b, 0x45, 0x00 }, 0, 3); // Set
																		// Emphasized
																		// Printing
																		// OFF
																		// (same
																		// command
																		// as
																		// on)
			} else {
				for (Totales.Base base : deposito.Totales.Bases.values()) {
					port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																			// Emphasized
																			// Printing
																			// ON

					outputByteBuffer = ("Impuestos:\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

					outputByteBuffer = ("\u0009" + "Base imp: "
							+ padRight(df.format(base.Base), 10)
							+ padRight(df.format(base.IvaPerc) + "%", 10)
							+ padRight(df.format(base.Iva) + " Euros", 16) + "\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
					outputByteBuffer = ("\u0009" + "Rec eq: "
							+ padRight(" ", 10)
							+ padRight(df.format(base.RecargoPerc) + "%", 10)
							+ padRight(df.format(base.Recargo) + " Euros ", 16) + "\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

					port.writePort(new byte[] { 0x1b, 0x45, 0x00 }, 0, 3); // Set
																			// Emphasized
																			// Printing
																			// OFF
																			// (same
																			// command
																			// as
																			// on)

					outputByteBuffer = ("\nForma de pago: "
							+ deposito.PagoDescripcion + "\n").getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

				}
				
				if (deposito.Pagado) {
					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(context, PORT, SETTINGS,
								context.getResources(),
								R.drawable.madeinspain_image, 1500);

					outputByteBuffer = ("Conforme   firma cliente:\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					outputByteBuffer = ("\n").getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					outputByteBuffer = ("HE RECIBIDO DE "
							+ deposito.Cliente.Nombre + " LA CANTIDAD DE "
							+ df.format(deposito.CantidadPagada)
							+ " Euros EN CONCEPTO DEL PAGO DEL ALBARAN "
							+ app.getUser().User + "/"
							+ deposito.NumeroAlbaran + "\n\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					if (deposito.CantidadPagada < deposito.Totales.Total) {
						outputByteBuffer = ("QUEDA PENDIENTE DE PAGO LA CANTIDAD DE "
								+ df.format(deposito.Totales.Total
										- deposito.CantidadPagada)
								+ " Euros EN CONCEPTO DEL PAGO DEL ALBARAN "
								+ app.getUser().User
								+ "/"
								+ deposito.NumeroAlbaran + "\n")
								.getBytes();
						port.writePort(outputByteBuffer, 0,
								outputByteBuffer.length);
					}

					outputByteBuffer = ("\n").getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					outputByteBuffer = ("FIRMA VENDEDOR: "
							+ app.getUser().Name + "\n").getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					this.PrintBitmapSignatureVendor(context, PORT,
							SETTINGS, 150);

					outputByteBuffer = ("\n").getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					outputByteBuffer = ("FIRMA CLIENTE\n").getBytes();

				} else {
					outputByteBuffer = ("\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0,
							outputByteBuffer.length);

					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(context, PORT, SETTINGS,
								context.getResources(),
								R.drawable.madeinspain_image, 1500);

					port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																			// Emphasized
																			// Printing
																			// ON
					outputByteBuffer = ("Conforme - Firma Cliente:\n")
							.getBytes();

				}
				port.writePort(outputByteBuffer, 0,
						outputByteBuffer.length);
				this.PrintBitmapSignature(context, PORT, SETTINGS, 150);
			}

			if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN &&  app.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards) {

				port.writePort(new byte[]{0x1b, 0x45, 0x01}, 0, 3);
				outputByteBuffer = ("MERCANCIA PENDIENTE DE ENVIO" + "\n").getBytes();

				port.writePort(outputByteBuffer, 0,
						outputByteBuffer.length);

				port.writePort(new byte[]{0x1b, 0x45, 0x00}, 0, 3);
			}

			if (isTransferPayment && tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN) {

				port.writePort(new byte[] { 0x1b, 0x61, 0x01 }, 0, 3); // Center

				outputByteBuffer = ("\nHACER TRANSFERENCIA EN UNO DE LOS SIGUIENTES NUMEROS DE CUENTA:\n"
						+ "\n"
						+ "BANCO SABADELL\n"
						+ "ES48 0081 0470 0500 0104 3307\n\n"
						+ "LA CAIXA\n"
						+ "ES08 2100 4652 0222 0002 2712\n\n"
						+ "BANCO SANTADER\n"
						+ "ES92 0075 1133 4405 0006 9237\n\n").getBytes();

				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				port.writePort(new byte[] { 0x1b, 0x61, 0x00 }, 0, 3); // Left Alignment
				port.writePort(new byte[] { 0x1b, 0x44, 0x02, 0x1b, 0x34, 0x00 }, 0, 6); // Setting
				// Horizontal
				// Tab

				port.writePort(new byte[] { 0x1b, 0x61, 0x01 }, 0, 3); // Center
				port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
				// Emphasized
				// Printing
				// ON

				outputByteBuffer = ("Poner en el concepto: " + deposito.Nombre + " y num de albaran " + app.getUser().User + "/" + deposito.NumeroAlbaran + "\n\n")
						.getBytes();

				port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				port.writePort(new byte[] { 0x1b, 0x61, 0x00 }, 0, 3); // Left Alignment
				port.writePort(new byte[] { 0x1b, 0x44, 0x02, 0x1b, 0x34, 0x00 }, 0, 6); // Setting
				// Horizontal
				// Tab

			}
		}
	}

	private void printHeaderDetail(StarIOPort port, int tipo, Deposito deposito)
			throws StarIOPortException {
		DecimalFormat df = new DecimalFormat("0.00");

		byte[] outputByteBuffer;

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

					outputByteBuffer = (padRight(linea.Articulo.CodigoArticulo,
							10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesFacturadas),
									10)
							+ padLeft(df.format(linea.PVP), 10)
							+ padLeft(
									String.valueOf(df.format(linea.PVP
											* linea.UnidadesFacturadas)), 10) + "\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

					if (linea.TotalAbono != 0) {
						outputByteBuffer = (padRight(
								linea.Articulo.CodigoArticulo, 10)
								+ padRight(desc, 25)
								+ padLeft(String.valueOf(linea.UnidadesAbono),
										10)
								+ padLeft(df.format(linea.PVPAbono), 10)
								+ padLeft(String.valueOf(df
										.format(linea.TotalAbono)), 10) + "\n")
								.getBytes();
						port.writePort(outputByteBuffer, 0,
								outputByteBuffer.length);
					}
				} else {
					if (linea.TotalAbono != 0) {

						String desc;
						if (linea.Articulo.Descripcion.length() > 25)
							desc = linea.Articulo.Descripcion.substring(0, 25);
						else
							desc = linea.Articulo.Descripcion;

						outputByteBuffer = (padRight(
								linea.Articulo.CodigoArticulo, 10)
								+ padRight(desc, 25)
								+ padLeft(String.valueOf(linea.UnidadesAbono),
										10)
								+ padLeft(df.format(linea.PVPAbono), 10)
								+ padLeft(String.valueOf(df
										.format(linea.TotalAbono)), 10) + "\n")
								.getBytes();
						port.writePort(outputByteBuffer, 0,
								outputByteBuffer.length);
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

					outputByteBuffer = (padRight(linea.Articulo.CodigoArticulo,
							10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesRepuestas),
									10)
							+ padLeft(df.format(linea.PVPAnterior), 10) + "\n")
							.getBytes();
					port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
				}
			}
		}

	}

	protected void closePage(StarIOPort port) throws StarIOPortException {
		byte[] outputByteBuffer;
		outputByteBuffer = ("\n").getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
		outputByteBuffer = ("--------------------------------------------------------------------- \n")
				.getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
		outputByteBuffer = ("--------------------------------------------------------------------- \n")
				.getBytes();
		port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

	}

	public boolean printAlbaran(Deposito deposito, Context context,
			AppConfig app, String guid, boolean isTransferPayment) {
		_GUID = guid;

		StarIOPort port = null;
		try {
			port = StarIOPort.getPort(PORT, SETTINGS, 10000, context);

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			byte[] outputByteBuffer;

			if (deposito.Serie.equals(app.getUser().SerialInvoiceA))
				printHeader(port, context);

			port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// ON

			if (deposito.Serie.equals(app.getUser().SerialInvoiceB))
				outputByteBuffer = ("PRESUPUESTO: NUM " + app.getUser().User
						+ "/" + deposito.NumeroAlbaran + "\n")
						.getBytes();
			else if (!deposito.Pagado)
				outputByteBuffer = ("ALBARAN: NUM " + app.getUser().User + "/"
						+ deposito.NumeroAlbaran + "   ASOCIADO A FACTURA\n")
						.getBytes();
			else
				outputByteBuffer = ("ALBARAN ENTREGA: NUM "
						+ app.getUser().User + "/"
						+ deposito.NumeroAlbaran + "\n")
						.getBytes();

			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

			port.writePort(new byte[] { 0x1b, 0x45, 0x00 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// OFF (same
																	// command
																	// as on)

			this.printHeaderData(port, deposito, app, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);

			port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// ON

			printHeaderFields(port, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			printHeaderDetail(port, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN, deposito);
			printTotals(port, context, app, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN, deposito, isTransferPayment);
			port.writePort(new byte[] { 0x1b, 0x45, 0x00 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// OFF (same
																	// command
																	// as on)

			closePage(port);

		} catch (StarIOPortException e) {
			return false;
		}

		finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return true;
	}

	public boolean printDeposito(Deposito deposito, Context context,
			AppConfig app, String guid) {

		_GUID = guid;

		StarIOPort port = null;
		try {

			port = StarIOPort.getPort(PORT, SETTINGS, 10000, context);

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			byte[] outputByteBuffer;

			printHeader(port, context);

			port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// ON

			outputByteBuffer = ("DEPOSITO: NUM " + app.getUser().User + "/"
					+ deposito.IdDeposito + "\n").getBytes();

			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

			port.writePort(new byte[] { 0x1b, 0x45, 0x00 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// OFF (same
																	// command
																	// as on)

			this.printHeaderData(port, deposito, app, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// ON

			printHeaderFields(port, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			printHeaderDetail(port, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO, deposito);
			printTotals(port, context, app, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO, deposito, false);
			outputByteBuffer = ("\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n")
					.getBytes();
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);

			if (isMadeInSpain(deposito.CodigoPostal))
				this.PrintBitmapImage(context, PORT, SETTINGS,
						context.getResources(), R.drawable.madeinspain_image, 1500);

			this.PrintBitmapImage(context, PORT, SETTINGS,
					context.getResources(), R.drawable.contract, 1500);

			port.writePort(new byte[] { 0x1b, 0x45, 0x01 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// ON

			outputByteBuffer = ("Conforme - Firma Cliente:\n").getBytes();
			port.writePort(outputByteBuffer, 0, outputByteBuffer.length);
			this.PrintBitmapSignature(context, PORT, SETTINGS, 150);
			port.writePort(new byte[] { 0x1b, 0x45, 0x00 }, 0, 3); // Set
																	// Emphasized
																	// Printing
																	// OFF (same
																	// command
																	// as on)

			closePage(port);

		} catch (StarIOPortException e) {
			return false;
		}

		finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
		}

		return true;
	}

	public void PrintBitmapSignature(Context context, String portName,
			String portSettings, int maxWidth) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS + "/" + "C_" + _GUID + ".png", options);
		
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);
		StarIOPort port = null;
		try 
    	{
			
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}

			byte[] command = starbitmap.getImageEscPosDataForPrinting();
			port.writePort(command, 0, command.length);
			
			try
			{
				Thread.sleep(3000);
			}
			catch(InterruptedException e) {}
		}
    	catch (StarIOPortException e)
    	{
    	}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {}
			}
		}
	}

	public void PrintBitmapSignatureVendor(Context context, String portName,
			String portSettings, int maxWidth) {
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		Bitmap bm = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS + "/" + "V_" + _GUID + ".png", options);
		
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);
		
		StarIOPort port = null;
		try 
    	{
		
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e) {}

			byte[] command = starbitmap.getImageEscPosDataForPrinting();
			port.writePort(command, 0, command.length);
			 
			try
			{
				Thread.sleep(3000);
			}
			catch(InterruptedException e) {}			
		}
    	catch (StarIOPortException e)
    	{
    	}
		finally
		{
			if(port != null)
			{
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {}
			}
		}
	}

	public void PrintBitmapImage(Context context, String portName,
			String portSettings, Resources res, int source, int maxWidth) {

		Bitmap bm = BitmapFactory.decodeResource(res, source);
		StarBitmap starbitmap = new StarBitmap(bm, false, maxWidth);

		StarIOPort port = null;
		try {
			port = StarIOPort.getPort(portName, portSettings, 10000, context);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			byte[] command = starbitmap.getImageEscPosDataForPrinting();
			port.writePort(command, 0, command.length);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
		} catch (StarIOPortException e) {
		} finally {
			if (port != null) {
				try {
					StarIOPort.releasePort(port);
				} catch (StarIOPortException e) {
				}
			}
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

	@Override
	public void Release() {
		

	}

}
