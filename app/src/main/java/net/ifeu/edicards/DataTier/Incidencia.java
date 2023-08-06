package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsFolders;

import android.os.Environment;

public class Incidencia {

	public String Usuario;
	public Date Fecha;
	public IncidenciaType Tipo;
	public String Descripcion;

	public Incidencia( String usuario, Date fecha, IncidenciaType tipo, String descripcion)
	{
		this.Usuario = usuario;
		this.Fecha = fecha;
		this.Tipo = tipo;
		this.Descripcion = descripcion;
	}
	
	public void create() throws Exception
	{	
		String text;
		String tipoInc = ConstantsTypes.EMPTY_STRING;
		String prefix = ConstantsTypes.EMPTY_STRING;
		
		switch (this.Tipo)
		{
			case ClienteNuevo: tipoInc = "Cliente Nuevo"; prefix = "N"; break;
			case BajaCliente: tipoInc = " Baja de cliente"; prefix = "B"; break;
			case DatosFiscales: tipoInc = "Datos fiscales modificados"; prefix = "D"; break;
			case AlbaranAnulado: tipoInc = "Albarán anulado"; prefix = "A"; break;
			case AlbaranAnuladoDesdeEdicards: tipoInc = "Albarán anulado"; prefix = "E"; break;
			case CuentaCorriente: tipoInc = "Cuenta Corriente modificada"; prefix = "C"; break;
			case Filiacion: tipoInc = "Filiación de Cliente modificada"; prefix = "F"; break;
			case Ingreso: tipoInc = "Ingreso realizado por comercial"; prefix = "I"; break;
		}
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm");
		
		text = "COMERCIAL: " + this.Usuario + ConstantsTypes.NEW_LINE +
				"FECHA - HORA: " + formatter.format(this.Fecha) + ConstantsTypes.NEW_LINE +
				"TIPO INCIDENCIA: " + tipoInc + ConstantsTypes.NEW_LINE +
				"DESCRIPCIÓN: " + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE + this.Descripcion + ConstantsTypes.NEW_LINE +
				ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE +
				"Este mensaje se ha generado automáticamente desde el dispositivo móvil.";

		String fileName = Environment.getExternalStorageDirectory().getPath() + "/"
				+ ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_INCIDENCIAS + "/" + prefix + "_" + this.getDateTimeFormat() + ".txt";
		
		net.ifeu.library.IO.IOUtils.writeAllText(text, fileName);
		
	}
	
	private String getDateTimeFormat()
	{
		 SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		 return format.format(new Date());
		 
	}
	
}


