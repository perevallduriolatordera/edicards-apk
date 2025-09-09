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

	public LineaMovimientos getMovimientos() {

		int total;
		int totalAlmacen;
		int totalDefectuoso = 0;
		int totalDeposito;
		DTOLineaDeposito linea = this;

		if (linea.UnidadesRepuestas > 0) {

			if (linea.IsVentaDirecta) {
				total =  linea.UnidadesDevueltas - linea.UnidadesDefectuosas
						- linea.UnidadesRepuestas
						- (linea.UnidadesFacturadas - (linea.UnidadesInicialesFijas - linea.UnidadesDevueltas));
			} else {
				total = linea.UnidadesDevueltas - linea.UnidadesDefectuosas
						- linea.UnidadesRepuestas;
			}
			totalDefectuoso = linea.UnidadesDefectuosas;

		} else {

			if (linea.IsVentaDirecta) {
				total = linea.UnidadesFacturadas;

			} else {
				total = linea.UnidadesDevueltas - linea.UnidadesDefectuosas
						- linea.UnidadesRepuestas;
			}
		}

		if (linea.UnidadesAbono > 0) {
			total = total + linea.UnidadesAbono - linea.DefectuosasAbono;
			totalDefectuoso = totalDefectuoso + linea.DefectuosasAbono;
		}

		totalAlmacen = linea.UnidadesRepuestas - linea.UnidadesDevueltas;
		totalDeposito = linea.UnidadesInicialesFijas - linea.UnidadesDevueltas + linea.UnidadesRepuestas;

		LineaMovimientos movimientos = new LineaMovimientos();
		movimientos.Total = total;
		movimientos.TotalDefectuoso = linea.UnidadesDefectuosas;
		movimientos.TotalMovimientoDeposito = totalAlmacen;
		movimientos.TotalAbono = linea.UnidadesAbono;
		movimientos.TotalAbonoDefectuoso = linea.DefectuosasAbono;
		movimientos.TotalDeposito = totalDeposito;

		movimientos.TotalVentaDirecta = linea.UnidadesFacturadas - (linea.UnidadesInicialesFijas
				- linea.UnidadesDevueltas);

		movimientos.TotalVentaDeposito = (linea.UnidadesInicialesFijas
				- linea.UnidadesDevueltas) + linea.UnidadesAbono;

		return movimientos;
	}


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
