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
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.R;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.DTOLineaDeposito;
import net.ifeu.edicards.DataTier.Totales;
import net.ifeu.library.Errors.ResultResponse;

import android.content.Context;


public class PrintDTODocumentsWoosim extends PrintDocumentsWoosim implements
		IPrintDTO {

	private void printHeaderData(DTODeposito deposito,
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

	private void printHeaderFields(int tipo)
	{
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

		_woosim.saveSpool(LANGUAGE, line, 0, true);

		_woosim.saveSpool(
				LANGUAGE,
				"--------------------------------------------------------------------- \n",
				0, false);
	}

	private void printTotals(Context context, AppConfig app, int tipo,
			DTODeposito deposito, boolean isTransferPayment) throws IOException {

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
						+ deposito.PagoDescripcion + "\n", 0, true);
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

					_woosim.saveSpool(LANGUAGE, formaPago, 0, true);
					this.Print();
					
				}
				
				if (deposito.Pagado) {

					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(
								context.getResources(),
								R.drawable.madeinspain_image, 1 , false);

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
							"FIRMA VENDEDOR: " + app.getUser().Name + "\n",
							0, false);
					this.Print();

					this.PrintBitmapSignatureVendor();

					_woosim.saveSpool(LANGUAGE, "\n", 0, false);
					this.Print();

					_woosim.saveSpool(LANGUAGE, "FIRMA CLIENTE\n", 0, false);

				} else {

					_woosim.saveSpool(
							LANGUAGE,
							"\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n",
							0, false);
					this.Print();

					if (isMadeInSpain(deposito.CodigoPostal))
						this.PrintBitmapImage(
								context.getResources(),
								R.drawable.madeinspain_image, 1, false);

					_woosim.saveSpool(LANGUAGE,
							"Conforme - Firma Cliente:\n", 0, false);
				}
				this.Print();
				this.PrintBitmapSignature();

			}

			if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN &&  app.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Edicards) {
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
			DTODeposito deposito) {

		DecimalFormat df = new DecimalFormat("0.00");
		List<DTOLineaDeposito> tempList = new ArrayList<>(deposito.Lineas.values());

		Collections.sort(tempList,
				new DTOLineaDeposito().new ArticuloComparator());

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN)
			for (DTOLineaDeposito linea : tempList) {
				if (linea.UnidadesFacturadas > 0) {
					String desc;
					if (linea.Descripcion.length() > 25)
						desc = linea.Descripcion.substring(0, 25);
					else
						desc = linea.Descripcion;

					String lineaText = (padRight(linea.CodigoArticulo, 10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesFacturadas),
									10)
							+ padLeft(df.format(linea.PVP), 10)
							+ padLeft(
									String.valueOf(df.format(linea.PVP
											* linea.UnidadesFacturadas)), 10) + "\n");

					_woosim.saveSpool(LANGUAGE, lineaText, 0, true);

					if (linea.TotalAbono != 0) {
						String abono = (padRight(linea.CodigoArticulo, 10)
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
						if (linea.Descripcion.length() > 25)
							desc = linea.Descripcion.substring(0, 25);
						else
							desc = linea.Descripcion;

						String lineaText = (padRight(linea.CodigoArticulo, 10)
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
			for (DTOLineaDeposito linea : tempList) {
				if (linea.UnidadesRepuestas > 0) {
					String desc;
					if (linea.Descripcion.length() > 25)
						desc = linea.Descripcion.substring(0, 25);
					else
						desc = linea.Descripcion;

					String lineaText = (padRight(linea.CodigoArticulo, 10)
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

	public ResultResponse printAlbaran(DTODeposito deposito, Context context,
									   AppConfig app, String guid, boolean isTransferPayment) {

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
			printTotals(context, app, ConstantsTypes.TIPO_DOCUMENTO_ALBARAN, deposito, isTransferPayment);
			closePage();

		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			return new ResultResponse(false, e.getMessage());
		}

		return new ResultResponse(true, ConstantsTypes.EMPTY_STRING);

	}

	public boolean printDeposito(DTODeposito deposito, Context context,
			AppConfig app, String guid) {

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
			printTotals(context, app, ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO, deposito, false);

			_woosim.saveSpool(LANGUAGE,
					"\nOPERACION ASEGURADA EN CREDITO Y CAUCION\n\n", 0, false);

			this.Print();

			if (isMadeInSpain(deposito.CodigoPostal))
				this.PrintBitmapImage(context.getResources(),
						R.drawable.madeinspain_image, 1, false);

			this.PrintBitmapImage(context.getResources(),
					R.drawable.contract, 1 , false);
			
			_woosim.saveSpool(LANGUAGE, "Conforme - Firma Cliente:\n", 0, false);
			this.Print();
			this.PrintBitmapSignature();
			closePage();

		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			return false;
		}

		return true;
	}

}
