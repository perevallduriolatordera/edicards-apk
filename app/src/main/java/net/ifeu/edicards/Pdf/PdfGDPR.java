package net.ifeu.edicards.Pdf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import android.content.Context;
import android.os.Environment;
import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Cliente;

public class PdfGDPR extends pdfBase {

	private final Cliente _cliente;

	public PdfGDPR(Cliente cliente, String GUID, Context context, AppConfig app) {
		super(context, app);
		_cliente = cliente;
		_GUID = GUID;
	}

	private void printHeader() throws DocumentException {

		this.addLogo();

		String text;

		text = "CONSENTIMIENTO EXPRESO PARA EL TRATAMIENTO DE DATOS DE CARÁCTER PERSONAL\n\n";

		Paragraph paragraph = new Paragraph(text, this._fontBoldExtra);
		paragraph.setAlignment(Element.ALIGN_CENTER);

		_document.add(paragraph);
		
	}

	private void printBody() throws DocumentException {

		String text = "\nDe conformidad con lo establecido en el REGLAMENTO (UE) 2016/679 de protección de datos "
				+ "de carácter personal, le informamos que los datos que usted nos facilite serán incorporados al "
				+ "sistema de tratamiento titularidad de la empresa Ediciones Ester Jaen S.L con NIF B 61816808	y domicilio social en C/Solsones, 68, de Castellar del Valles (BCN)	C.P. (08211), con la finalidad de (por ejemplo COMUNICACIÓN DE EVENTOS, "
				+ "PUBLICIDAD, PROSPECCIÓN COMERCIAL; GESTIÓN DE CLIENTES, CONTABLE, FISCAL Y ADMINISTRATIVA).\n\n";

		_document.add(new Paragraph(text, _fontNormal));
		
		String text2 = "Mediante la firma del presente documento, " + this._cliente.Razon + " , con CIF/NIF " + this._cliente.NIF + "," + this._cliente.Direccion1 + " , " +  this._cliente.Poblacion + " , " + this._cliente.Provincia + " , C.P.(" + this._cliente.CodigoPostal + "), da su consentimiento expreso para el tratamiento de sus datos con la finalidad mencionada. Sus datos serán conservados durante el plazo estrictamente necesario y serán borrados cuando haya transcurrido un tiempo sin hacer uso de los mismos. Se procederá a tratar los datos de manera lícita, leal, transparente, adecuada, pertinente, limitada, exacta y actualizada. Sus datos podrán ser objeto de tratamiento por terceros  (serán encargados del tratamiento destinatarios de sus datos con una finalidad contractual lícita, por ejemplo nuestra empresa de mantenimiento informático o nuestra gestoría para facturación y contabilidad) exigiendo el mismo nivel de derechos, obligaciones y responsabilidades establecidas en la ley. " +
						"Mientras no nos comunique lo contrario, entenderemos que sus datos no han sido modificados y que usted se compromete a notificarnos cualquier variación. " + 
						"De acuerdo con los derechos que le confiere la normativa vigente podrá ejercer los derechos de acceso, rectificación, limitación de tratamiento, supresión, portabilidad y oposición al tratamiento de sus datos de carácter personal así como revocar el consentimiento prestado, dirigiendo su petición a la dirección postal indicada o al correo electrónico XXX@XXXX.COM y podrá dirigirse a la Autoridad de Control competente para presentar la reclamación que considere oportuna.\n\n "; 

		Paragraph paragraph2 = new Paragraph(text2, this._fontNormal);
		_document.add(paragraph2);
		
		String text3 = "Nombre, NIF y firma \n\n";
		
		Paragraph paragraph3 = new Paragraph(text3, this._fontNormal);
		_document.add(paragraph3);
		
		String text4 = this._cliente.Razon + " (" + this._cliente.NIF + ")\n\n";
		
		Paragraph paragraph4 = new Paragraph(text4, this._fontNormal);
		_document.add(paragraph4);
		
	}

	public boolean createGDPR() throws FileNotFoundException,
			DocumentException {

		_document = new Document();

		_pdfName = Environment.getExternalStorageDirectory().getPath() + "/"
				+ Constants.FOLDER_ROOT + "/" + Constants.FOLDER_GDPR + "/"
				+ _cliente.CodigoCliente 
				+ ".pdf";

		_document.addTitle(_app.getUser().User + "_" + _cliente.CodigoCliente
				+ "_" + new Date(0));

		_writer = PdfWriter.getInstance(_document, new FileOutputStream(
				_pdfName));
		_document.open();

		try {

			this.printHeader();

			this.printBody();
			
			this.addSignature();

			this.closePage();

		} catch (Exception e) {

			return false;
		}

		return true;
	}

}
