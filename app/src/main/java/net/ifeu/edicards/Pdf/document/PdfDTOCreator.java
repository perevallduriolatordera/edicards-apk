package net.ifeu.edicards.Pdf.document;

import android.os.Environment;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.DTOLineaDeposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.Totales;
import net.ifeu.edicards.Pdf.pdfBase;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PdfDTOCreator extends pdfBase implements IPdfDocumentGenerator {

	DTODeposito _deposito;

	public PdfDTOCreator(DTODeposito deposito, AppConfig app) {
		super(app);
		_deposito = deposito;
	}	

	@Override
	protected void insertSeparators() throws DocumentException {
		_document.add(new Paragraph(
				"---------------------------------------------------------------------"
						+ "--------------------------------------- \n",
				_fontNormal));
	}

	private void printHeaderData(int Tipo) throws DocumentException {

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		String pago = (Tipo == 1) ? "\n" : "Pago: " + _deposito.PagoDescripcion
				+ "\n";

		String text = "Fecha: " + formatter.format(_deposito.FechaDeposito)
				+ "  Vendedor: " + _app.getUser().Name + "\n"
				+ "Codigo Cliente: " + _deposito.CodigoCliente
				+ "  Nombre: " + _deposito.Nombre + "\n" + "Razón: "
				+ _deposito.Razon + "\n" + "NIF: " + _deposito.NIF + "\n"
				+ "Direccion: " + _deposito.Direccion1 + "\n" + "Cod. Postal: "
				+ _deposito.CodigoPostal + "  Poblacion:  "
				+ _deposito.Poblacion + "\n" + "Telefono 1: "
				+ _deposito.Telefono1 + "  Telefono 2: " + _deposito.Telefono2
				+ "  Mail:" + _deposito.Mail + "\n" + pago;

		_document.add(new Paragraph(text, _fontNormal));
	}

	private void printHeaderFields(int tipo) throws DocumentException {

		this.insertSeparators();

		String total;
		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN)
			total = padLeft("TOTAL", 10);
		else
			total = ConstantsTypes.EMPTY_STRING;

		String text = padRight("COD.", 10) + padRight("DESCRIPCION", 25)
				+ padLeft("UNID.", 10) + padLeft("PRECIO.", 10) + total + "\n";

		_document.add(new Paragraph(text, _fontNormal));
		this.insertSeparators();
	}

	private void printTotals(int tipo) throws DocumentException {
		DecimalFormat df = new DecimalFormat("0.00");

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO) {
			try {
				_deposito.CalculateDeposito();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			this.insertSeparators();

		} else {
			try {
				_deposito.Calculate();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if ((_deposito.Totales.DescuentoFinanciero != 0 || _deposito.Totales.DescuentoProntoPago != 0)
					&& (_deposito.Serie.equals(_app.getUser().SerialInvoiceA))) {
				String total = padLeft(" ", 43)
						+ padRight("TOTAL:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(_deposito.Totales.TotalBaseSinDte)), 10);
				_document.add(new Paragraph("\n" + total + "\n", _fontBold));

				if (_deposito.Totales.DescuentoProntoPago != 0) {
					String prontoPago = padLeft(" ", 43)
							+ padRight("DTE. COMERCIAL:", 15)
							+ padLeft(" ", 5)
							+ padRight(
									String.valueOf(df
											.format(_deposito.Totales.TotalDescuentoProntoPago)),
									10);
					_document.add(new Paragraph(prontoPago + "\n", _fontBold));
				}

				if (_deposito.Totales.DescuentoFinanciero != 0) {
					String financiero = padLeft(" ", 43)
							+ padRight("DTE. FINANCIERO:", 16)
							+ padLeft(" ", 4)
							+ padRight(
									String.valueOf(df
											.format(_deposito.Totales.TotalDescuentoFinanciero)),
									10);
					_document.add(new Paragraph(financiero + "\n", _fontBold));
				}
				
				String neto = padLeft(" ", 43)
						+ padRight("NETO:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(_deposito.Totales.TotalBase)), 10);
				_document.add(new Paragraph(neto + "\n", _fontBold));
			}

			if (_deposito.Serie.equals(_app.getUser().SerialInvoiceA)) {
				String suma = padLeft(" ", 43)
						+ padRight("SUMA:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(_deposito.Totales.TotalBase)), 10);

				_document.add(new Paragraph("\n" + suma + "\n", _fontBold));

				String impuestos = padLeft(" ", 43)
						+ padRight("IMPUESTOS:", 10)
						+ padLeft(" ", 10)
						+ padRight(String.valueOf(df
								.format(_deposito.Totales.TotalIVA
										+ _deposito.Totales.TotalRecargo)), 10);

				_document.add(new Paragraph(impuestos + "\n", _fontBold));

			}

			String total;
			
			if (_deposito.Serie.equals(_app.getUser().SerialInvoiceA)) {
					total = padLeft(" ", 43)
					+ padRight("TOTAL:", 10)
					+ padLeft(" ", 10)
					+ padRight(
							String.valueOf(df.format(_deposito.Totales.Total)),
							10);
			} else {
				try {
					total = padLeft(" ", 43)
							+ padRight("TOTAL:", 10)
							+ padLeft(" ", 10)
							+ padRight(
									String.valueOf(df.format(_deposito.Totales.TotalBase)),
									10);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}

			_document.add(new Paragraph(total + "\n", _fontBold));

			if (_deposito.Serie.equals(_app.getUser().SerialInvoiceB)) {

				_document.add(new Paragraph("\nIVA NO INCLUIDO\n", _fontBold));
				_document.add(new Paragraph(_deposito.PagoDescripcion + "\n",
						_fontBold));

			} else {
				for (Totales.Base base : _deposito.Totales.Bases.values()) {

					_document.add(new Paragraph("Impuestos:\n"));

					String baseText = "\u0009" + "Base imp: "
							+ padRight(df.format(base.Base), 10)
							+ padRight(df.format(base.IvaPerc) + "%", 10)
							+ padRight(df.format(base.Iva) + " Euros", 16)
							+ "\n";

					_document.add(new Paragraph(baseText, _fontBold));

					String reqEqvText = "\u0009" + "Rec eq: "
							+ padRight(" ", 10)
							+ padRight(df.format(base.RecargoPerc) + "%", 10)
							+ padRight(df.format(base.Recargo) + " Euros ", 16)
							+ "\n";

					_document.add(new Paragraph(reqEqvText, _fontBold));

					String formaPagoText = "\nForma de pago: "
							+ _deposito.PagoDescripcion + "\n";

					_document.add(new Paragraph(formaPagoText, _fontBoldExtra));

					if (_deposito.Pagado) {

						_document.add(new Paragraph(
								"Conforme   firma cliente:\n", _fontNormal));

						_document.add(new Paragraph("\n"));

						String pagadoText = "HE RECIBIDO DE "
								+ _deposito.Nombre + " LA CANTIDAD DE "
								+ df.format(_deposito.CantidadPagada)
								+ " Euros EN CONCEPTO DEL PAGO DEL ALBARAN "
								+ _app.getUser().User + "/"
								+ _deposito.NumeroAlbaran
								+ "\n\n";

						_document.add(new Paragraph(pagadoText, _fontNormal));

						if (_deposito.CantidadPagada < _deposito.Totales.Total) {
							String pendienteText = "QUEDA PENDIENTE DE PAGO LA CANTIDAD DE "
									+ df.format(_deposito.Totales.Total
											- _deposito.CantidadPagada)
									+ " Euros EN CONCEPTO DEL PAGO DEL ALBARAN "
									+ _app.getUser().User
									+ "/"
									+ _deposito.NumeroAlbaran
									+ "\n";

							_document.add(new Paragraph(pendienteText,
									_fontNormal));
						}

						_document.add(new Paragraph("\n"));

						_document.add(new Paragraph("\n"));

						String firmaVendedorText = "FIRMA VENDEDOR: "
								+ _app.getUser().Name + "\n";

						_document.add(new Paragraph(firmaVendedorText,
								_fontNormal));

						this.addSignature(2);
						_document.add(new Paragraph("\n"));
						String firmaClienteText = "FIRMA CLIENTE\n";
						_document.add(new Paragraph(firmaClienteText,
								_fontNormal));
						this.addSignature(1);


					} else {
						_document.add(new Paragraph(
								"Conforme - Firma Cliente:\n", _fontNormal));

						this.addSignature(1);
					}
				}
			}
		}
	}

	private void printHeaderDetail(int tipo) throws DocumentException {

		DecimalFormat df = new DecimalFormat("0.00");

		List<DTOLineaDeposito> tempList = new ArrayList<>(_deposito.Lineas.values());

		Collections
				.sort(tempList, new DTOLineaDeposito().new ArticuloComparator());

		if (tipo == ConstantsTypes.TIPO_DOCUMENTO_ALBARAN)
			for (DTOLineaDeposito linea : tempList) {
				if (linea.UnidadesFacturadas != 0) {
					String desc;
					if (linea.Descripcion.length() > 25)
						desc = linea.Descripcion.substring(0, 25);
					else
						desc = linea.Descripcion;

					String lineaText = padRight(linea.CodigoArticulo,
							10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesFacturadas),
									10)
							+ padLeft(df.format(linea.PVP), 10)
							+ padLeft(
									String.valueOf(df.format(linea.PVP
											* linea.UnidadesFacturadas)), 10)
							+ "\n";

					_document.add(new Paragraph(lineaText, _fontBold));

					if (linea.TotalAbono != 0) {
						String lineaAbonoText = padRight(
								linea.CodigoArticulo, 10)
								+ padRight(desc, 25)
								+ padLeft(String.valueOf(linea.UnidadesAbono),
										10)
								+ padLeft(df.format(linea.PVPAbono), 10)
								+ padLeft(String.valueOf(df
										.format(linea.TotalAbono)), 10) + "\n";

						_document.add(new Paragraph(lineaAbonoText, _fontBold));
					}
				} else {
					if (linea.TotalAbono != 0) {

						String desc;

						if (linea.Descripcion.length() > 25)
							desc = linea.Descripcion.substring(0, 25);
						else
							desc = linea.Descripcion;

						String lineaText = padRight(
								linea.CodigoArticulo, 10)
								+ padRight(desc, 25)
								+ padLeft(String.valueOf(linea.UnidadesAbono),
										10)
								+ padLeft(df.format(linea.PVPAbono), 10)
								+ padLeft(String.valueOf(df
										.format(linea.TotalAbono)), 10) + "\n";

						_document.add(new Paragraph(lineaText, _fontBold));
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

					String lineaText = padRight(linea.CodigoArticulo,
							10)
							+ padRight(desc, 25)
							+ padLeft(String.valueOf(linea.UnidadesRepuestas),
									10)
							+ padLeft(df.format(linea.PVPAnterior), 10) + "\n";

					_document.add(new Paragraph(lineaText, _fontBold));
				}
			}
		}
	}

	@Override
	protected void closePage() throws DocumentException {

		_document.add(new Paragraph("\n"));
		this.insertSeparators();
		this.insertSeparators();
		_document.close();

	}

	public void createAlbaran(String guid, boolean envioEdicards, DepositoModalidad modalidad) {

		try {
			_GUID = guid;
			_document = new Document();

			_pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
					+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_PDF + "/"
					+ "REC_A_" + _app.getUser().User + " " + _deposito.NumeroAlbaran
					+ "_" + this.getDateTimeFormat() + "_" + (envioEdicards ? "E" : "F") + ".pdf";

			_document.addTitle(_app.getUser().User + "_" + _deposito.NumeroAlbaran
					+ "_" + new Date(0));


				PdfWriter.getInstance(_document, new FileOutputStream(_pdfName));
		} catch (DocumentException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		try {
			_document.open();
			if (_deposito.Serie.equals(_app.getUser().SerialInvoiceA))
				printHeader();

			if (_deposito.Serie.equals(_app.getUser().SerialInvoiceB)) {
				String presupuestoText = "PRESUPUESTO: NUM "
						+ _app.getUser().User + "/"
						+ _deposito.NumeroAlbaran + "\n";

				_document.add(new Paragraph(presupuestoText, _fontBold));
			} else if (!_deposito.Pagado) {
				String albaranText = "ALBARAN: NUM " + _app.getUser().User
						+ "/" + _deposito.NumeroAlbaran + "   ASOCIADO A FACTURA\n";

				_document.add(new Paragraph(albaranText, _fontBold));
			} else {
				String albaranText = "ALBARAN ENTREGA: NUM "
						+ _app.getUser().User + "/"
						+ _deposito.NumeroAlbaran + "\n";

				_document.add(new Paragraph(albaranText, _fontBold));
			}

			this.printHeaderData(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			printHeaderFields(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			printHeaderDetail(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			printTotals(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);
			closePage();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void createDeposito(String guid, DepositoModalidad modalidad) {

		_GUID = guid;

		_document = new Document();

		_pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
				+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_PDF + "/"
				+ "D_" + _app.getUser().User + " " + _deposito.IdDeposito + "_"
				+ this.getDateTimeFormat() + ".pdf";

		_document.addTitle(_app.getUser().User + "_" + _deposito.IdDeposito
				+ "_" + new Date(0));

		try {
			_writer = PdfWriter.getInstance(_document, new FileOutputStream(
					_pdfName));
		} catch (DocumentException | FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		_document.open();

		try {

			printHeader();
			String depositoNum = "DEPOSITO: NUM " + _app.getUser().User + "/"
					+ _deposito.IdDeposito + "\n";

			_document.add(new Paragraph(depositoNum, _fontBold));
			this.printHeaderData(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			printHeaderFields(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			printHeaderDetail(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			printTotals(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			String firmaCliente = "Conforme - Firma Cliente:\n";
			_document.add(new Paragraph(firmaCliente, _fontNormal));
			this.addSignature(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);
			closePage();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
