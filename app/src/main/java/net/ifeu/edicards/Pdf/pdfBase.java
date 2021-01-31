package net.ifeu.edicards.Pdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.R;
import net.ifeu.edicards.Constants.Constants;

public class pdfBase {
	protected Document _document;
	protected PdfWriter _writer;
	protected String _pdfName;
	protected Context _context;
	protected AppConfig _app;
	protected String _GUID;
	
	protected Font _fontBold = new Font(Font.FontFamily.COURIER, 7, Font.BOLD);
	protected Font _fontNormal = new Font(Font.FontFamily.COURIER, 7, Font.NORMAL);
	protected Font _fontUnderline  = new Font(Font.FontFamily.COURIER, 7, Font.UNDERLINE + Font.BOLD);
	protected Font _fontBoldExtra  = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);
	
	protected static final int BUFFER_IO_SIZE = 8000;

	public static Map<String, String> getInfo(String src) throws IOException {
		PdfReader reader = new PdfReader(src);
		return reader.getInfo();
	}
	
	public pdfBase (Context context, AppConfig appConfig) {
		this._context = context;
		this._app = appConfig;
	}
	
	protected void addLogo() throws FileNotFoundException, DocumentException {

		Drawable myImage = _context.getResources().getDrawable(
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
			// TODO Auto-generated catch block
			_app.getErrorTrace().Send(_app.getUser().User, e);
		}

	}
	
	protected void closePage() throws DocumentException {

		_document.add(new Paragraph("\n"));

		//this.insertSeparators();
		//this.insertSeparators();

		_document.close();

	}
	
	protected void addSignature() throws MalformedURLException,
		IOException, DocumentException {

		String prefix = Constants.EMPTY_STRING;
		
		prefix = "C_";
		
		String imageFile = Environment.getExternalStorageDirectory().toString()
				+ "/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_FIRMAS
				+ "/" + prefix + _GUID + ".png";
		
		Log.i("PdfCreator", imageFile);
		
		// Image image = Image.getInstance(imageFile);
		// image.scaleToFit(image.getWidth() / 1, image.getHeight() / 1);
		// _document.add(image);
		
		try {
		
			// Drawable drawable = Drawable.createFromPath(imageFile);
			// Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			Bitmap bitmap = this.loadImageFromFile(imageFile);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_app.getErrorTrace().Send(_app.getUser().User, e);
		}
	
	}
	
	protected void addSignature(int tipo) throws MalformedURLException,
		IOException, DocumentException {

		String prefix = Constants.EMPTY_STRING;
		
		prefix = tipo == 1 ? "C_" : "V_";
		
		String imageFile = Environment.getExternalStorageDirectory().toString()
				+ "/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_FIRMAS
				+ "/" + prefix + _GUID + ".png";
		
		Log.i("PdfCreator", imageFile);
		
		// Image image = Image.getInstance(imageFile);
		// image.scaleToFit(image.getWidth() / 1, image.getHeight() / 1);
		// _document.add(image);
		
		try {
		
			// Drawable drawable = Drawable.createFromPath(imageFile);
			// Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			Bitmap bitmap = this.loadImageFromFile(imageFile);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			byte[] bitmapdata = stream.toByteArray();
			try {
				Image bgImage = Image.getInstance(bitmapdata);
				// bgImage.setAbsolutePosition(5f, 400f);
				bgImage.scaleToFit(bgImage.getWidth() / 7,
						bgImage.getHeight() / 7);
		
				_document.add(bgImage);
				
				bitmap.recycle();
				bitmap = null;
				System.gc();
		
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			_app.getErrorTrace().Send(_app.getUser().User, e);
		}

	}

	protected boolean isMadeInSpain(String codigoPostal) {
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
	
	protected void insertSeparators() throws DocumentException {
		_document.add(new Paragraph(
				"_____________________________________________________________________"
			  + "_____________________________________________________ \n",
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
	
	protected String getDateTimeFormat(Date fecha) {
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
		return format.format(fecha);
	}
	
	protected Bitmap loadImageFromFile(final String file) {
		try {
			// Addresses bug in SDK :
			// http://groups.google.com/group/android-developers/browse_thread/thread/4ed17d7e48899b26/
			BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(file), BUFFER_IO_SIZE);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(baos,
					BUFFER_IO_SIZE);
			copy(bis, bos);
			bos.flush();
			
			Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0,
					baos.size());
			
			return bitmap;
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

}
