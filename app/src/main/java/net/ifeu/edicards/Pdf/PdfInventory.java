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
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.MovimientosAlmacen;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

public class PdfInventory extends pdfBase {

	public PdfInventory(AppConfig app) {
		super(app);
	}

	@SuppressLint("SimpleDateFormat")
	protected void printHeader() throws DocumentException {

		this.addLogo();

		String text;

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		text = "\nSTOCK CORRESPONDIENTE AL COMERCIAL: " + _app.getUser().User + " ( " +  _app.getUser().Name + " )\n" 
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

	@SuppressWarnings("deprecation")
	private void printHeaderDetail() {

		LinkedHashMap<String, Articulo> articulos;

		try {
			articulos = _app.getCache().getAllArticulos();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		MovimientosAlmacen movimientos = Factory.build(MovimientosAlmacen.class, _app);

		Date now = new Date();
		LinkedHashMap<String,ArrayList<MovimientosAlmacen>> movs;
		
		try {
			 
			movs = movimientos.getMovimientosByMonth(2000 + (now.getYear() % 100), 
						now.getMonth() + 1);
		 } catch (Exception e1) {
			throw new RuntimeException(e1);
		  }



        for (Articulo art : articulos.values())
        {
			try {
				String lineaText = padRight(art.CodigoArticulo,
						30)
						+ padRight(art.Descripcion, 30)
						+ padLeft(String.valueOf(art.Stock),
								30)
						+ padLeft(String.valueOf(art.StockDefectuoso), 30)
						+ "\n";

				_document.add(new Paragraph(lineaText, _fontBold));
				
				
				if (movs.containsKey(art.CodigoArticulo))
				{
					ArrayList<MovimientosAlmacen> array = movs.get(art.CodigoArticulo);
					
					StringBuilder movsText = new StringBuilder(ConstantsTypes.EMPTY_STRING);

					for (MovimientosAlmacen movArticulo : array) {
						if (movArticulo.Entradas != 0 || movArticulo.Salidas != 0) {
							String auxText = this.getDateTimeFormat(movArticulo.Fecha)
									+ (movArticulo.Tipo == 1 ? " Inventario: " : " Camión: ")
									+ (movArticulo.Entradas != 0 ? movArticulo.Entradas + " unidades entradas " : ConstantsTypes.EMPTY_STRING)
									+ (movArticulo.Salidas != 0 ? movArticulo.Salidas + " unidades sacadas " : ConstantsTypes.EMPTY_STRING)
									+ (movArticulo.TipoStock == 1 ? " de unidades en buen estado " : " de unidades de reciclaje ")
									+ "\n";


							movsText.append(padRight(ConstantsTypes.EMPTY_STRING,
									15)).append(padRight(auxText, 60));

						}
					}
					_document.add(new Paragraph(movsText.toString(), _fontNormal));
					_document.add(new Paragraph(ConstantsTypes.EMPTY_STRING, _fontBold));
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        
		
		}

	}

	public boolean createInventory() throws FileNotFoundException,
			DocumentException {

		_document = new Document();

		_pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
				+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INVENTARIO + "/"
				+ _app.getUser().User + "_" 
				+ this.getDateTimeFormat() + ".pdf";

		_writer = PdfWriter.getInstance(_document, new FileOutputStream(
				_pdfName));
		_document.open();

		try {

			printHeader();

			printHeaderFields();

			printHeaderDetail();

			// this.PrintBitmapSignature(context, PORT, SETTINGS, 150);

			closePage();

		} catch (Exception e) {

			return false;
		}

		return true;
	}

}
