package net.ifeu.edicards.DataTier;

import java.util.Comparator;

import com.google.gson.annotations.Expose;

public class DTOLineaDeposito {
	
	@Expose
	public long IdLineaDeposito;
	@Expose
	public long IdDeposito;
	@Expose
	public long IdArticulo;
	@Expose
	public String CodigoArticulo;
	@Expose
	public String Descripcion;
	@Expose
	public String Familia;
	@Expose
	public int StockInicial;
	@Expose
	public int UnidadesIniciales;
	@Expose
	public int UnidadesInicialesFijas;
	@Expose
	public int UnidadesDevueltas;
	@Expose
	public int UnidadesFacturadas;
	@Expose
	public int UnidadesDefectuosas;
	@Expose
	public int UnidadesRepuestas;
	@Expose
	public double PVP;
	@Expose
	public double Descuento1;
	@Expose
	public double Descuento2;
	@Expose
	public double PVPAnterior;
	@Expose
	public int UnidadesAnterior;
	@Expose
	public boolean IsNew;
	@Expose
	public boolean IsVentaDirecta;
	@Expose
	public double PVPInicial;
	@Expose
	public int UnidadesAbono;
	@Expose
	public int DefectuosasAbono;
	@Expose
	public double PVPAbono;
	@Expose
	public double TotalAbono;
	
	 public class ArticuloComparator implements Comparator<DTOLineaDeposito> {
		    @Override
		    public int compare(DTOLineaDeposito linea1, DTOLineaDeposito linea2) {
		    	
		    	if ((linea1.Familia == null) || (linea2.Familia == null))
		    		return 0;
		    	else
		    		return linea1.Familia.compareTo(linea2.Familia);
		    }
		}

}
