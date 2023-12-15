package net.ifeu.edicards.Pdf.incident;

import android.os.Environment;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Incidencia;
import net.ifeu.edicards.Pdf.pdfBase;
import net.ifeu.library.IO.IOUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class IncidentPdfCreator extends pdfBase implements Incidencia.IIncidenciaCreator {

    public IncidentPdfCreator(AppConfig appConfig) {
        super(appConfig);
    }

    @Override
    public void create(Incidencia incidencia) {

        _document = new Document();

        String text;
        String tipoInc = ConstantsTypes.EMPTY_STRING;
        String prefix = ConstantsTypes.EMPTY_STRING;

        switch (incidencia.Tipo) {
            case ClienteNuevo:
                tipoInc = "Cliente Nuevo";
                prefix = "N";
                break;
            case BajaCliente:
                tipoInc = " Baja de cliente";
                prefix = "B";
                break;
            case DatosFiscales:
                tipoInc = "Datos fiscales modificados";
                prefix = "D";
                break;
            case AlbaranAnulado:
                tipoInc = "Albarán anulado";
                prefix = "A";
                break;
            case AlbaranAnuladoDesdeEdicards:
                tipoInc = "Albarán anulado";
                prefix = "E";
                break;
            case CuentaCorriente:
                tipoInc = "Cuenta Corriente modificada";
                prefix = "C";
                break;
            case Filiacion:
                tipoInc = "Filiación de Cliente modificada";
                prefix = "F";
                break;
            case Ingreso:
                tipoInc = "Ingreso realizado por comercial";
                prefix = "I";
                break;
        }

        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm");

        text = "COMERCIAL: " + incidencia.Usuario + ConstantsTypes.NEW_LINE +
                "FECHA - HORA: " + formatter.format(incidencia.Fecha) + ConstantsTypes.NEW_LINE +
                "TIPO INCIDENCIA: " + tipoInc + ConstantsTypes.NEW_LINE +
                "DESCRIPCIÓN: " + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE + incidencia.Descripcion + ConstantsTypes.NEW_LINE +
                ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE +
                "Este mensaje se ha generado automáticamente desde el dispositivo móvil.";

        _pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
                + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INCIDENCIAS + "/" + prefix + "_" + this.getDateTimeFormatForFilename() + ".pdf";


        _document.addTitle(_app.getUser().User + "_"
                + "Incidencia" + "_" + new Date(0));

        try {
            _writer = PdfWriter.getInstance(_document, new FileOutputStream(
                    _pdfName));
            _document.open();

            Paragraph paragraph = new Paragraph(text, this._fontBold);
            paragraph.setAlignment(Element.ALIGN_LEFT);
            _document.add(paragraph);

            String attachments = "\nADJUNTOS\n";
            Paragraph paragraphAttachments = new Paragraph(attachments, this._fontBoldExtra);
            paragraphAttachments.setAlignment(Element.ALIGN_LEFT);

            _document.add(paragraphAttachments);

            for (Map.Entry<String, String> set : incidencia.Attachments.entrySet()) {
                Image image = null;
                try {
                    if (!StringUtils.isEmpty(set.getValue())) {

                        String metadataText = "\n\n" + set.getKey() + "\n\n";
                        Paragraph paragraphItem = new Paragraph(metadataText, this._fontBold);
                        paragraphItem.setAlignment(Element.ALIGN_LEFT);
                        _document.add(paragraphItem);

                        image = Image.getInstance(set.getValue());
                        image.scaleToFit(image.getWidth() / 10, image.getHeight() / 10);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (image != null) {
                    _document.add(image);
                }
            }

        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            this.closePage();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }

        // ESBORREM ELS FITXERS TEMPORALS DELS ADJUNTS
        for (Map.Entry<String, String> set : incidencia.Attachments.entrySet()) {
            IOUtils.deleteFile(set.getValue());
        }

    }

}


