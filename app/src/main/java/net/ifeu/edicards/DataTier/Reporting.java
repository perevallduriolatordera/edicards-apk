package net.ifeu.edicards.DataTier;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import net.ifeu.edicards.Constants.ConstantsTypes;

public class Reporting {
	
	public LinkedHashMap<String,ArrayList<Historico>> Agrupado = new LinkedHashMap<>();
	public LinkedHashMap<String,Potenciado> Potenciados = new LinkedHashMap<>();
	public LinkedHashMap<String,Vendido> Vendidos = new LinkedHashMap<>();
	public LinkedHashMap<String,Retirado> Retirados = new LinkedHashMap<>();
	public LinkedHashMap<String,Defectuoso> Defectuosos = new LinkedHashMap<>();
	public Totales totales = new Totales();
	
	public class Potenciado
	{
		public Articulo articulo = new Articulo();
		public int unidades;
	}
	
	public class Vendido
	{
		public Articulo articulo = new Articulo();
		public int unidades;
	}
	
	public class Retirado
	{
		public Articulo articulo = new Articulo();
		public int unidades;
	}
	
	public class Defectuoso
	{
		public Articulo articulo = new Articulo();
		public int unidades;
	}
	
	public class Totales
	{
		public int Nuevos;
		public int Retirados;
		public int Visitas;
		public String InicialSerieA = ConstantsTypes.EMPTY_STRING;
		public String FinalSeriaA = ConstantsTypes.EMPTY_STRING;
		public String InicialSerieB = ConstantsTypes.EMPTY_STRING;
		public String FinalSerieB = ConstantsTypes.EMPTY_STRING;
		public double TotalSerieA;
		public double TotalSerieB;
		public double CantidadPagadaSerieA;
		public double CantidadPagadaSerieB;
		public int NumeroSerieA;
		public int NumeroSerieB;
		public double Facturado;
	}
}
