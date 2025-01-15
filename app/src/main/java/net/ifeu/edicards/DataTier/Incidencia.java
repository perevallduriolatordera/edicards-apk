package net.ifeu.edicards.DataTier;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Incidencia {

	public String Usuario;
	public Date Fecha;
	public IncidenciaType Tipo;
	public String Descripcion;
	public Map<String, String> Attachments;

	public Incidencia( String usuario, Date fecha, IncidenciaType tipo, String descripcion)
	{
		this.Usuario = usuario;
		this.Fecha = fecha;
		this.Tipo = tipo;
		this.Descripcion = descripcion;
		this.Attachments = new HashMap<>();
	}
	
	public void create(IIncidenciaCreator creator) throws Exception
	{
		creator.create(this);
	}

	public interface IIncidenciaCreator {
		void create(Incidencia incidencia);
	}

}


