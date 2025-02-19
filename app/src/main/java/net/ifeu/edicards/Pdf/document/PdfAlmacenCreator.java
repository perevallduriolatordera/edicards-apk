package net.ifeu.edicards.Pdf.document;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.google.zxing.BarcodeFormat;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.Totales;
import net.ifeu.edicards.DepositManagerExtension;
import net.ifeu.edicards.Pdf.pdfBase;
import net.ifeu.library.Barcodes.BarcodeGenerator;
import net.ifeu.library.Debugger.Debugger;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class PdfAlmacenCreator extends pdfBase implements IPdfDocumentGenerator{

    private static final int NUMBER_OF_COLUMNS_DETAIL = 7;
    private static final float[] COLUMNS_WIDTH_DETAIL = {5f, 10f, 25f, 10f, 10f, 10f, 30f}; // Sum of these values should be 100

    Deposito _deposito;

    public PdfAlmacenCreator(Deposito deposito, AppConfig app) {

        super(app);
        _deposito = deposito;

    }

    @Override
    protected void insertSeparators() throws DocumentException {
        //_document.add(new Paragraph(
        //        "---------------------------------------------------------------------"
        //                + "--------------------------------------- \n",
        //        _fontNormal));
        _document.add(new Paragraph("\n"));
    }

    private Paragraph getParagraph(String content, Font font) {
        Chunk chunk = new Chunk(content, font);
        Paragraph paragraph = new Paragraph(chunk);
        paragraph.setAlignment(Paragraph.ALIGN_CENTER);

        return paragraph;
    }


    private void printHeaderData() throws DocumentException {

        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("dd/MM/yyyy");
        String pago = "Pago: " + _deposito.PagoDescripcion
                + "\n";

        String text = "Fecha: " + formatter.format(_deposito.FechaDeposito)
                + "  Vendedor: " + _app.getUser().Name + "\n"
                + "Codigo Cliente: " + _deposito.Cliente.CodigoCliente
                + "  Nombre: " + _deposito.Nombre + "\n" + "Razón: "
                + _deposito.Razon + "\n" + "NIF: " + _deposito.NIF + "\n"
                + "Direccion: " + _deposito.Direccion1 + "\n" + "Cod. Postal: "
                + _deposito.CodigoPostal + "  Poblacion:  "
                + _deposito.Poblacion + "\n" + "Telefono 1: "
                + _deposito.Telefono1 + "  Telefono 2: " + _deposito.Telefono2
                + "  Mail:" + _deposito.Mail + "\n" + pago;

        Paragraph paragraph = new Paragraph(text, _fontNormal);
        paragraph.setAlignment(Element.ALIGN_LEFT);

        _document.add(paragraph);
    }

    private void printHeaderFields() throws DocumentException {

        this.insertSeparators();

        PdfPTable table = new PdfPTable(NUMBER_OF_COLUMNS_DETAIL);
        table.setWidthPercentage(100); // 100% of the available width

        // Set the relative widths of the columns
        table.setWidths(COLUMNS_WIDTH_DETAIL);

        PdfPCell cell = new PdfPCell();
        cell.addElement(getParagraph("PREPARADO", _fontBold));
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph("CÓDIGO", _fontBold));
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph("ARTÍCULO", _fontBold));
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph("UNIDADES", _fontBold));
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph("PRECIO", _fontBold));
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph("TOTAL", _fontBold));
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph("EAN", _fontBold));
        table.addCell(cell);

        _document.add(table);
    }

    private PdfPCell createEmptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.addElement(getParagraph("", _fontBold));
        cell.setBorder(0);
        return cell;
    }

    private void printTotalsFooter(String label, String  value) throws DocumentException {

        PdfPTable table = new PdfPTable(NUMBER_OF_COLUMNS_DETAIL);
        table.setWidthPercentage(100); // 100% of the available width

        // Set the relative widths of the columns
        table.setWidths(COLUMNS_WIDTH_DETAIL);

        table.addCell(this.createEmptyCell());
        table.addCell(this.createEmptyCell());
        table.addCell(this.createEmptyCell());
        table.addCell(this.createEmptyCell());

        PdfPCell cell = new PdfPCell();
        cell.addElement(getParagraph(label, _fontBold));
        cell.setBorder(0);
        table.addCell(cell);

        cell = new PdfPCell();
        cell.addElement(getParagraph(value, _fontNormal));
        cell.setBorder(0);
        table.addCell(cell);

        table.addCell(this.createEmptyCell());

        _document.add(table);

    }


    private void printTotals(boolean isTransferPayment, DepositoModalidad modalidad) throws DocumentException {
        DecimalFormat df = new DecimalFormat("0.00");

        try {
            _deposito.Calculate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if ((_deposito.Totales.DescuentoFinanciero != 0 || _deposito.Totales.DescuentoProntoPago != 0)
                && (_deposito.Serie.equals(_app.getUser().SerialInvoiceA))) {
            printTotalsFooter("TOTAL", df.format(_deposito.Totales.TotalBaseSinDte));

            if (_deposito.Totales.DescuentoProntoPago != 0) {
                this.printTotalsFooter("DTE. COMERCIAL:", df.format(_deposito.Totales.TotalDescuentoProntoPago));
            }

            if (_deposito.Totales.DescuentoFinanciero != 0) {
                this.printTotalsFooter("DTE. FINANCIERO:", df.format(_deposito.Totales.TotalDescuentoFinanciero));
            }
        }

        if (_deposito.Serie.equals(_app.getUser().SerialInvoiceA)) {
            this.printTotalsFooter("SUMA:", df.format(_deposito.Totales.TotalBase));

            this.printTotalsFooter("IMPUESTOS:", df
                    .format(_deposito.Totales.TotalIVA
                            + _deposito.Totales.TotalRecargo));
        }

        String total;

        if (_deposito.Serie.equals(_app.getUser().SerialInvoiceA)) {

            total = df.format(_deposito.Totales.Total);
       } else {
            try {
                total = df.format(_deposito.Totales.TotalBase);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        this.printTotalsFooter("TOTAL:", total);

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
            }
            String formaPagoText = "\nForma de pago: "
                    + _deposito.PagoDescripcion + "\n";

            _document.add(new Paragraph(formaPagoText, _fontBold));

            if (_deposito.Pagado) {

                _document.add(new Paragraph(
                        "Conforme   firma cliente:\n", _fontNormal));

                _document.add(new Paragraph("\n"));

                String pagadoText = "HE RECIBIDO DE "
                        + _deposito.Cliente.Nombre + " LA CANTIDAD DE "
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

                String firmaVendedorText = "FIRMA VENDEDOR: "
                        + _app.getUser().Name + "\n";
                _document.add(new Paragraph(firmaVendedorText,
                        _fontNormal));

                this.addSignature(ConstantsTypes.TIPO_DOCUMENTO_ALBARAN);

                String firmaClienteText = "FIRMA CLIENTE\n";

                _document.add(new Paragraph(firmaClienteText,
                        _fontNormal));

                this.addSignature(ConstantsTypes.TIPO_DOCUMENTO_DEPOSITO);

            } else {
                _document.add(new Paragraph(
                        "Conforme - Firma Cliente:\n", _fontNormal));
                this.addSignature(1);
            }
        }

        if (modalidad == DepositoModalidad.Edicards) {
            String pendienteEnvioText = "MERCANCIA PENDIENTE DE ENVIO"
                    + "\n";

            _document.add(new Paragraph(pendienteEnvioText,
                    _fontBoldExtra));
        }

        if (isTransferPayment) {

            this.insertSeparators();

            String transferText = "\nHACER TRANSFERENCIA EN UNO DE LOS SIGUIENTES NUMEROS DE CUENTA:\n"
                    + "\n"
                    + "BANCO SABADELL\n"
                    + "ES48 0081 0470 0500 0104 3307\n\n"
                    + "LA CAIXA\n"
                    + "ES08 2100 4652 0222 0002 2712\n\n"
                    + "BANCO SANTADER\n"
                    + "ES92 0075 1133 4405 0006 9237\n\n";

            Paragraph paragraph = new Paragraph(transferText, _fontNormal);
            paragraph.setAlignment(Element.ALIGN_CENTER);

            _document.add(paragraph);

            transferText = "Poner en el concepto: " + _deposito.Nombre + " y num de albaran " + _app.getUser().User + "/" + _deposito.NumeroAlbaran + "\n\n";

            paragraph = new Paragraph(transferText, _fontBold);
            paragraph.setAlignment(Element.ALIGN_CENTER);

            _document.add(paragraph);
        }
    }

    private void printHeaderDetail() throws DocumentException {

        DecimalFormat df = new DecimalFormat("0.00");
        List<LineaDeposito> tempList = new ArrayList<>(_deposito.Lineas.values());
        Collections
                .sort(tempList, new LineaDeposito().new ArticuloComparator());

        for (LineaDeposito linea : tempList) {
            if (linea.UnidadesFacturadas > 0) {

                PdfPTable table = new PdfPTable(NUMBER_OF_COLUMNS_DETAIL);
                table.setWidthPercentage(100); // 100% of the available width

                // Set the relative widths of the columns
                table.setWidths(COLUMNS_WIDTH_DETAIL);

                PdfPCell cell = new PdfPCell();
                cell.setCellEvent(new RectangleEvent());
                table.addCell(cell);

                cell = new PdfPCell();
                cell.addElement(getParagraph(linea.Articulo.CodigoArticulo, _fontNormal));
                table.addCell(cell);

                cell = new PdfPCell();
                cell.addElement(getParagraph(linea.Articulo.Descripcion, _fontNormal));
                table.addCell(cell);

                cell = new PdfPCell();
                cell.addElement(getParagraph(String.valueOf(linea.UnidadesFacturadas), _fontNormal));
                table.addCell(cell);

                cell = new PdfPCell();
                cell.addElement(getParagraph(currencyRound(linea.PVP), _fontNormal));
                table.addCell(cell);

                double totalLinea = ((linea.UnidadesFacturadas * linea.PVP)
                        - ((linea.UnidadesFacturadas * linea.PVP) * (linea.Descuento1 / 100)));

                cell = new PdfPCell();
                cell.addElement(getParagraph(currencyRound(totalLinea), _fontNormal));
                table.addCell(cell);

                cell = new PdfPCell();

                for (String barcode : getBarcodeList(linea.Articulo.EAN)) {
                    Image image = null;
                    try {
                        createBitmap(barcode);
                        if (!StringUtils.isEmpty(barcode)) {
                            image = Image.getInstance(getBarcodeFileName(barcode));
                            image.scaleToFit(image.getWidth() / 3, image.getHeight() / 3);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (image != null) {
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.addElement(image);
                        cell.addElement(getParagraph(linea.Articulo.EAN, _fontBold));
                    }
                }

                table.addCell(cell);
                _document.add(table);

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

    public void createAlbaran(String guid, boolean isTransferPayment, DepositoModalidad modalidad)
    {
        try {

            Debugger.Debug(_app, _app.getUser().User,"Generando albarán " + _deposito.NumeroAlbaran, null);
            _GUID = guid;

            _document = new Document();
            String tipoEnvio = modalidad == DepositoModalidad.Edicards ? "E" : "F";

            _pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
                    + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_PDF + "/"
                    + "AALM_" + _app.getUser().User + " " + _deposito.NumeroAlbaran
                    + "_" + this.getDateTimeFormat() + "_" + tipoEnvio + ".pdf";

            _document.addTitle(_app.getUser().User + "_" + _deposito.NumeroAlbaran
                    + "_" + new Date(0));

            PdfWriter.getInstance(_document, new FileOutputStream(_pdfName));
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

            printHeaderData();
            printHeaderFields();
            printHeaderDetail();
            printTotals(isTransferPayment, modalidad);
            printSignatureBoxesAlmacen();
            closePage();

        } catch (Exception e) {
            sendMailToMantenimiento(e, _app.getUser().User, _deposito.NumeroAlbaran, "albarán");
            throw new RuntimeException(e);
        } finally {
            {
                try {
                    Debugger.Debug(_app, _app.getUser().User,"Se ha generado el albarán " + _deposito.NumeroAlbaran, _pdfName);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void createDeposito(String guid, DepositoModalidad modalidad)
    {
        throw new NotImplementedException("Esta opción aún no ha sido implementada");
    }

    private List<String> getBarcodeList(String barcode) {
        return Arrays.asList(barcode.split(","));
    }


    private void createBitmap(String ean) {

        if (StringUtils.isEmpty(ean)) return;

        String barcodeName = getBarcodeFileName(ean);
        File barcodeFile = new File(barcodeName);
        Bitmap bitmap;

        if (barcodeFile.exists()) {
            bitmap = BitmapFactory.decodeFile(barcodeName);
        } else {
            bitmap = BarcodeGenerator.generateBarcode(ean, BarcodeFormat.EAN_13, 400, 50);
        }

        try (FileOutputStream out = new FileOutputStream(barcodeName)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    private String getBarcodeFileName(String ean) {
        return "/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_EAN + "/" + ean + ".bmp";
    }

    private void printSignatureBoxesAlmacen() throws DocumentException {

        if (_deposito.Serie.equals(_deposito.Serie.equals(_app.getUser().SerialInvoiceB)))
            return;

        this.insertSeparators();
        this.insertSeparators();
        this.insertSeparators();
        this.insertSeparators();
        this.insertSeparators();

        try {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100); // 100% of the available width

            // Set the relative widths of the columns
            float[] columnWidths = {25f, 25f, 25f, 25f}; // Sum of these values should be 100
            table.setWidths(columnWidths);

            PdfPCell cell = new PdfPCell();
            cell.addElement(getParagraph("PEDIDO PREPARADO POR\n\n\n\n\n\n\n\n", _fontBold));
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setCellEvent(new RectangleEvent());
            table.addCell(cell);

            cell = new PdfPCell();
            cell.addElement(getParagraph("FIRMA PREPARACIÓN\n\n\n\n\n\n\n\n", _fontBold));
            table.addCell(cell);

            cell = new PdfPCell();
            cell.setCellEvent(new RectangleEvent());
            table.addCell(cell);

            _document.add(table);

        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    static class RectangleEvent implements PdfPCellEvent {

        @Override
        public void cellLayout(PdfPCell pdfPCell, com.itextpdf.text.Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.TEXTCANVAS];
            Rectangle rect = new Rectangle(position.getLeft(), position.getBottom(), position.getRight(), position.getTop());

            // Draw a rectangle with a border of 2 units
            rect.setBorder(Rectangle.BOX);
            rect.setBorderWidth(1f);

            // Set the color of the rectangle
            rect.setBorderColor(BaseColor.BLACK);

            // Draw the rectangle on the canvas
            canvas.rectangle(rect);
        }
    }
}
