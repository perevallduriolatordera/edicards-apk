package net.ifeu.edicards.Pdf;

import android.annotation.SuppressLint;
import android.os.Environment;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.Articulo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

public class PdfInventoryRecycled extends pdfBase {

	public PdfInventoryRecycled(AppConfig app) {
		super(app);
	}

	@SuppressLint("SimpleDateFormat")
	protected void printHeader() throws DocumentException {

		this.addLogo();

		String text;

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		text = "\nSTOCK RECICLADO CORRESPONDIENTE AL COMERCIAL: " + _app.getUser().User + " ( " +  _app.getUser().Name + " )\n" 
				+ "Fecha: " + formatter.format(new Date()) + "\n"; 

		Paragraph paragraph = new Paragraph(text, _fontBoldExtra);
		paragraph.setAlignment(Element.ALIGN_CENTER);

		_document.add(paragraph);

		this.insertSeparators();

	}

	

	private void printHeaderFields() throws DocumentException {

		this.insertSeparators();

		String text = padRight("CODIGO ARTICULO.", 30) + padRight("DESCRIPCION", 30)
				+ padLeft("UNIDADES.", 30) + padLeft("UNIDADES RECICLAJE.", 30) + "\n";

		_document.add(new Paragraph(text, _fontNormal));

		this.insertSeparators();
	}

	private void printHeaderDetail() {

		LinkedHashMap<String, Articulo> articulos;

        try {
			articulos = _app.getCache().getAllArticulos();
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
        
        for (Articulo art : articulos.values())
        {
			try {
				String lineaText = padRight(art.CodigoArticulo,
						30)
						+ padRight(art.Descripcion.length() > 28 ? art.Descripcion.substring(0,27) : art.Descripcion, 30)
						+ padLeft(String.valueOf(art.Stock),
								30)
						+ padLeft(String.valueOf(art.StockDefectuoso), 30)
						+ "\n";

				_document.add(new Paragraph(lineaText, _fontBold));
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean createInventory() throws FileNotFoundException,
			DocumentException {

		_document = new Document();

		_pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
				+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INVENTARIO + "/Reciclado_"
				+ _app.getUser().User + "_" 
				+ this.getDateTimeFormat() + ".pdf";

		_writer = PdfWriter.getInstance(_document, new FileOutputStream(
				_pdfName));
		_document.open();

		try {

			printHeader();
			printHeaderFields();
			printHeaderDetail();
			closePage();

		} catch (Exception e) {

			return false;
		}

		return true;
	}

}
