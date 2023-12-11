package net.ifeu.edicards.Pdf;

import android.os.Environment;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.Deposito;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

public class PdfAuthorization extends pdfBase{

	Deposito _deposito;
	
	public PdfAuthorization(Deposito deposito, String GUID, AppConfig app) {

		super (app);
		_deposito = deposito;
		_GUID = GUID;
		
	}

	protected void printHeader() throws DocumentException {

		this.addLogo();

		String text;

		text = "\nApreciados señores, \n\n"
				+
				// "Editora y Distribuidora de Tarjetas de Felicitacion\n" +
				// "Postales  Stickers  Llaveros\n" +
				// "Libros de Colorear  Gifts  Manualidades  Papel Fantasia\n" +
				"Para adaptarnos a las exigencias de la Ley 16/2009 de Servicios de Pago, del 13 de " +						
				"Noviembre de 2009, les solicitamos autorización expresa de la domiciliación de su banco de " + 							
				"las facturas a su nombre en su vencimiento.\n\n" +
				"Les agradeceríamos confirmen su número de cuenta, seguido de la firma del legal " +						
				"representante y sello de la empresa en la presente carta en prueba de su conformidad.\n\n";							


		Paragraph paragraph = new Paragraph(text, _fontNormal);
		paragraph.setAlignment(Element.ALIGN_LEFT);

		_document.add(paragraph);
		
		String text2 = "AUTORIZACIÓN DE DOMICILIACIÖN BANCANRIA\n";
		Paragraph paragraph2 = new Paragraph(text2, _fontUnderline);
		
		_document.add(paragraph2);

		//this.insertSeparators();

	}

	private void printHeaderData() throws DocumentException {

		String text = "\nPor la presente, Don " + _deposito.ClienteInfo.Representante
				+ "  con DNI " + _deposito.ClienteInfo.DniRepresentante  
				+ "  con poderes y en representación de " + _deposito.Razon 
				+ "  con CIF " + _deposito.NIF 
				+ "  autorizo a EDICIONES ESTER JAEN SL a domiciliar con cargo a la cuenta abajo señalada " 
				+ " todos los recibos originados de la relación comercial existente entre ambas partes.\n\n";
				

		_document.add(new Paragraph(text, _fontNormal));
		
		String text2 = "NÚMERO DE LA CUENTA\n";
		Paragraph paragraph2 = new Paragraph(text2, _fontUnderline);
		
		_document.add(paragraph2);
		
		String text3 = "\n" + _deposito.ClienteInfo.CCC + "\n\n";
		Paragraph paragraph3 = new Paragraph(text3, _fontBoldExtra);
		
		_document.add(paragraph3);
		

	}

	
	public boolean createAuthorization() throws FileNotFoundException,
			DocumentException {

		_document = new Document();

		_pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
				+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_AUTORIZACIONES + "/"
				+ "A_" + _app.getUser().User + " " + _deposito.CodigoCliente + "_"
				+ this.getDateTimeFormatLong() + ".pdf";

		_document.addTitle(_app.getUser().User + "_" + _deposito.ClienteInfo.CCC
				+ "_" + new Date(0));

		_writer = PdfWriter.getInstance(_document, new FileOutputStream(
				_pdfName));
		_document.open();

		try {

			printHeader();

			this.printHeaderData();
			
			String fecha = "Fecha: " + this.getDateTimeFormat() + "\n\n";

			_document.add(new Paragraph(fecha, _fontBold));
			
			String firmaCliente = "Firma Representante:\n";

			_document.add(new Paragraph(firmaCliente, _fontNormal));

			this.addSignature();
			// this.PrintBitmapSignature(context, PORT, SETTINGS, 150);

			closePage();

		} catch (Exception e) {

			return false;
		}

		return true;
	}


}
