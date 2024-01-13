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
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.Pdf.pdfBase;
import net.ifeu.library.Barcodes.BarcodeGenerator;
import net.ifeu.library.Debugger.Debugger;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PdfAlmacenDeprecated extends pdfBase implements IPdfDocumentGenerator{
    Deposito _deposito;

    public PdfAlmacenDeprecated(Deposito deposito, AppConfig appConfig) {
        super(appConfig);
        _deposito = deposito;
    }

    @Override
    public boolean createAlbaran(String guid, boolean isTransferPayment) {

        try {

            Debugger.Debug(_app, _app.getUser().User,"Generando albarán " + _deposito.NumeroAlbaran, null);
            _GUID = guid;

            _document = new Document();
            String tipoEnvio = "E";

            _pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
                    + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_PDF + "/"
                    + "AALM_" + _app.getUser().User + " " + _deposito.NumeroAlbaran
                    + "_" + this.getDateTimeFormat() + "_" + tipoEnvio + ".pdf";

            _document.addTitle(_app.getUser().User + "_" + _deposito.IdDeposito
                    + "_" + new Date(0));

            PdfWriter.getInstance(_document, new FileOutputStream(_pdfName));
            _document.open();

            printHeader();
            printNumeroAlbaran();
            printHeaderDataAlmacen();
            printHeaderFieldsAlmacen();
            printHeaderDetailAlmacen();
            printSignatureBoxesAlmacen();
            closePage();

        } catch (Exception e) {
            sendMailToMantenimiento(e, _app.getUser().User, _deposito.NumeroAlbaran, "albarán");
            return false;
        } finally {
            {
                try {
                    Debugger.Debug(_app, _app.getUser().User,"Se ha generado el albarán " + _deposito.NumeroAlbaran, _pdfName);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }

    private void printHeaderDataAlmacen() throws DocumentException {

        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("dd/MM/yyyy");

        String text = "Fecha: " + formatter.format(_deposito.FechaDeposito)
                + "  Vendedor: " + _app.getUser().Name + "\n"
                + "Codigo Cliente: " + _deposito.Cliente.CodigoCliente
                + "  Nombre: " + _deposito.Nombre + "\n" + "Razón: "
                + _deposito.Razon + "\n" + "NIF: " + _deposito.NIF + "\n"
                + "Direccion: " + _deposito.Direccion1 + "\n" + "Cod. Postal: "
                + _deposito.CodigoPostal + "  Poblacion:  "
                + _deposito.Poblacion + "\n" + "Telefono 1: "
                + _deposito.Telefono1 + "  Telefono 2: " + _deposito.Telefono2
                + "  Mail:" + _deposito.Mail + "\n";

        _document.add(new Paragraph(text, _fontNormal));
    }

    private void printHeaderFieldsAlmacen() throws DocumentException {

        this.insertSeparators();

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100); // 100% of the available width

        // Set the relative widths of the columns
        float[] columnWidths = {15f, 10f, 35f, 10f, 30f}; // Sum of these values should be 100
        table.setWidths(columnWidths);

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
        cell.addElement(getParagraph("EAN", _fontBold));
        table.addCell(cell);

        _document.add(table);

        this.insertSeparators();
    }

    private void printSignatureBoxesAlmacen() throws DocumentException {

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

    private List<String> getBarcodeList(String barcode) {
        return Arrays.asList(barcode.split(","));
    }

    private void printHeaderDetailAlmacen() throws DocumentException {

        List<LineaDeposito> tempList = new ArrayList<>(_deposito.Lineas.values());
        Collections
                .sort(tempList, new LineaDeposito().new ArticuloComparator());

        for (LineaDeposito linea : tempList) {

            if (linea.UnidadesFacturadas > 0) {

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100); // 100% of the available width

                // Set the relative widths of the columns
                float[] columnWidths = {15f, 10f, 35f, 10f, 30f}; // Sum of these values should be 100
                table.setWidths(columnWidths);

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

    private Paragraph getParagraph(String content, Font font) {
        Chunk chunk = new Chunk(content, font);
        Paragraph paragraph = new Paragraph(chunk);
        paragraph.setAlignment(Paragraph.ALIGN_CENTER);

        return paragraph;
    }


    private String getBarcodeFileName(String ean) {
        return "/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_EAN + "/" + ean + ".bmp";
    }
    private void createBitmap(String ean) {

        if (StringUtils.isEmpty(ean)) return;

        String barcodeName = getBarcodeFileName(ean);
        File barcodeFile = new File(barcodeName);
        Bitmap bitmap;

        if (barcodeFile.exists()) {
            bitmap = BitmapFactory.decodeFile(barcodeName);
        } else {
            bitmap = BarcodeGenerator.generateBarcode(ean, BarcodeFormat.EAN_13, 400, 75);
        }

        try (FileOutputStream out = new FileOutputStream(barcodeName)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    private void printNumeroAlbaran() {
        if (_deposito.Serie.equals(_app.getUser().SerialInvoiceB)) {
            String presupuestoText = "PRESUPUESTO: NUM "
                    + _app.getUser().User + "/"
                    + _deposito.NumeroAlbaran + "\n";

            try {
                _document.add(new Paragraph(presupuestoText, _fontBold));
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        } else if (!_deposito.Pagado) {
            String albaranText = "ALBARAN: NUM " + _app.getUser().User
                    + "/" + _deposito.NumeroAlbaran + "\n";

            try {
                _document.add(new Paragraph(albaranText, _fontBold));
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        } else {
            String albaranText = "ALBARAN ENTREGA: NUM "
                    + _app.getUser().User + "/"
                    + _deposito.NumeroAlbaran + "\n";

            try {
                _document.add(new Paragraph(albaranText, _fontBold));
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean createDeposito(String guid) {
        return false;
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


