package net.ifeu.edicards.Pdf;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.R;
import net.ifeu.library.Debugger.Debugger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class pdfBase {
	protected Document _document;
	protected PdfWriter _writer;
	protected String _pdfName;
	protected AppConfig _app;
	protected String _GUID;
	
	protected Font _fontBold = new Font(Font.FontFamily.COURIER, 7, Font.BOLD);
	protected Font _fontNormal = new Font(Font.FontFamily.COURIER, 7, Font.NORMAL);
	protected Font _fontUnderline  = new Font(Font.FontFamily.COURIER, 7, Font.UNDERLINE + Font.BOLD);
	protected Font _fontBoldExtra  = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
	
	protected static final int BUFFER_IO_SIZE = 8000;

	public pdfBase (AppConfig appConfig) {
		this._app = appConfig;
	}

	protected void printHeader() throws DocumentException {

		this.addLogo();
		String text;
		text = "\nGRUP EDICIONES ESTER JAEN SL   NIF: B-61806808\n"
				+ "Pol. Ind Pla de la Bruguera    C/Solsones, 68   --   08211 Castellar del Valles  (Spain)\n"
				+ "Telfs: 902007753  937143823    Tel. Internacional +34 937143823     Fax.902007754\n"
				+ "e-mail: edicards@edicards.com    Web: www.edicards.com\n\n";

		Paragraph paragraph = new Paragraph(text, _fontNormal);
		paragraph.setAlignment(Element.ALIGN_CENTER);

		_document.add(paragraph);
		this.insertSeparators();

	}

	protected boolean sendMailToMantenimiento(Exception e, String user, String id, String tipo) {

		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			String title = "Error generando pdf del " + tipo + " num " + id + " del comercial " + user + ":";
			Debugger.Debug(_app, _app.getUser().User, title + "\n\n" + sw, null);

		} catch (Exception exc) {
			return false;
		}

		return true;
	}


	protected void addLogo() {

		Drawable myImage = _app.getResources().getDrawable(
				R.drawable.edicardsprint);

		Bitmap bitmap = ((BitmapDrawable) myImage).getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte[] bitmapdata = stream.toByteArray();
		try {
			Image bgImage = Image.getInstance(bitmapdata);
			// bgImage.setAbsolutePosition(5f, 400f);
			bgImage.scaleToFit(bgImage.getWidth() / 5, bgImage.getHeight() / 5);

			_document.add(bgImage);

		} catch (Exception e) {
		}

	}
	
	protected void closePage() throws DocumentException {
		_document.add(new Paragraph("\n"));
		_document.close();
	}
	
	protected void addSignature() {

		String prefix;
		prefix = "C_";
		
		String imageFile = Environment.getExternalStorageDirectory().toString()
				+ "/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS
				+ "/" + prefix + _GUID + ".png";

		try {

			Bitmap bitmap = this.loadImageFromFile(imageFile);
			if (bitmap == null) return;
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] bitmapdata = stream.toByteArray();
			try {
				Image bgImage = Image.getInstance(bitmapdata);
				// bgImage.setAbsolutePosition(5f, 400f);
				bgImage.scaleToFit(bgImage.getWidth() / 7,
						bgImage.getHeight() / 7);
		
				_document.add(bgImage);
				System.gc();
		
			} catch (Exception e) {
			}
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void addSignature(int tipo) {

		String prefix;
		prefix = tipo == 1 ? "C_" : "V_";
		
		String imageFile = Environment.getExternalStorageDirectory().toString()
				+ "/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_FIRMAS
				+ "/" + prefix + _GUID + ".png";
		try {

			Bitmap bitmap = this.loadImageFromFile(imageFile);
			if (bitmap == null) return;

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] bitmapdata = stream.toByteArray();
			try {
				Image bgImage = Image.getInstance(bitmapdata);
				bgImage.scaleToFit(bgImage.getWidth() / 7,
						bgImage.getHeight() / 7);

				PdfPTable table = new PdfPTable(1);
				table.setWidthPercentage(100);

				float[] columnWidths = {100f};
				table.setWidths(columnWidths);

				PdfPCell cell = new PdfPCell();
				cell.setBorder(0);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				cell.addElement(bgImage);

				table.addCell(cell);
				_document.add(table);
				
				bitmap.recycle();
				System.gc();
		
			} catch (Exception e) {
			}
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	protected void insertSeparators() throws DocumentException {
		_document.add(new Paragraph(
				"\n",
				_fontNormal));
	}
	
	@SuppressLint("SimpleDateFormat")
	protected static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}
	
	@SuppressLint("SimpleDateFormat")
	protected static String padLeft(String s, int n) {
		return String.format("%1$" + n + "s", s);
	}
	
	
	protected String getDateTimeFormatLong() {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
		return format.format(new Date());
	
	}
	
	protected String getDateTimeFormat() {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
		return format.format(new Date());
	
	}

	protected String getDateTimeFormatForFilename() {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy_hhmmss");
		return format.format(new Date());

	}
	
	protected String getDateTimeFormat(Date fecha) {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
		return format.format(fecha);
	}
	
	protected Bitmap loadImageFromFile(final String file) {
		try {
			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(file), BUFFER_IO_SIZE);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(baos,
					BUFFER_IO_SIZE);
			copy(bis, bos);
			bos.flush();
			
			return BitmapFactory.decodeByteArray(baos.toByteArray(), 0,
					baos.size());

		} catch (IOException e) {
			return null;
		}
	}

	private void copy(final InputStream bis, final OutputStream baos)
			throws IOException {
		byte[] buf = new byte[256];
		int l;
		while ((l = bis.read(buf)) >= 0)
			baos.write(buf, 0, l);
	}

	protected String currencyRound(double value) {
		// Create a DecimalFormat object with the desired format
		DecimalFormat decimalFormat = new DecimalFormat("0.00");

		// Use the format method to round the double value
		String roundedValue = decimalFormat.format(value);

		return roundedValue;

	}

}
