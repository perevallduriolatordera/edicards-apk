package net.ifeu.edicards.DataTier;

import java.util.HashMap;

public class Totales
{
	public HashMap<String,Base> Bases = new HashMap<String, Base>();
	public double TotalBase;
	public double TotalBaseSinDte;
	public double DescuentoProntoPago;
	public double TotalDescuentoProntoPago;
	public double DescuentoFinanciero;
	public double TotalDescuentoFinanciero;
	public double TotalIVA;
	public double TotalRecargo;
	public double Total;
	
	public class Base {
	
		public double Base;
		public double BaseSinDte;
		public double Iva;
		public double IvaPerc;
		public double Recargo;
		public double RecargoPerc;
		public double Descuento;
		public double Total;
	}
}
