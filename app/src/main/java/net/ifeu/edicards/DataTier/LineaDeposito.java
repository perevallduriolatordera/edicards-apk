package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import java.util.Comparator;
import java.util.LinkedHashMap;

public class LineaDeposito extends Persistent implements IPersistable {

	public long IdLineaDeposito;
	public long IdDeposito;
	public long IdArticulo;
	public Deposito Deposito;
	public Articulo Articulo;
	public int StockInicial;
	public int UnidadesIniciales;
	public int UnidadesInicialesFijas;
	public int UnidadesDevueltas;
	public int UnidadesFacturadas;
	public int UnidadesDefectuosas;
	public int UnidadesRepuestas;
	public double PVP;
	public double Descuento1;
	public double Descuento2;
	public double PVPAnterior;
	public int UnidadesAnterior;
	public boolean IsNew;
	public boolean IsSelected;
	public boolean IsVentaDirecta;
	public double PVPInicial;
	public int UnidadesAbono;
	public int DefectuosasAbono;
	public double PVPAbono;
	public double TotalAbono;

	@Override
	public void InitializePersistance(AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
		this.Deposito = Factory.build(Deposito.class, appConfigParam);
		this.Articulo = Factory.build(Articulo.class, appConfigParam);
	}
	@Override
	public void save() throws Exception {
		
		ContentValues values = new ContentValues();
		values.put("IdDeposito", this.Deposito.IdDeposito);
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("UnidadesIniciales", this.UnidadesIniciales);
		values.put("PVP", this.PVP);
		values.put("PVPAnterior", this.PVPAnterior);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
	
		try {
			this.IdLineaDeposito = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_LINEAS_DEPOSITO, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();
		
		values.put("IdDeposito", this.IdDeposito);
		values.put("IdArticulo", this.IdArticulo);
		values.put("UnidadesIniciales", this.UnidadesIniciales);
		values.put("PVP", this.PVP);
		values.put("PVPAnterior", this.PVPAnterior);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		
		String[] whereArgs = { String.valueOf(this.IdLineaDeposito) }; 
		
	    super.getDatabaseOperations().update(ConstantsDatabase.TABLE_LINEAS_DEPOSITO, values, "IdLineaDeposito = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(ConstantsDatabase.TABLE_LINEAS_DEPOSITO);
	}
	
	
	public LinkedHashMap<String, LineaDeposito> getLineasDepositosByDeposito(Deposito deposito) throws Exception
	{
			LinkedHashMap<String,LineaDeposito> list = new LinkedHashMap<>();
			
			Cursor cursor = super.getDatabaseOperations().getRecordsFromFieldNumeric(ConstantsDatabase.TABLE_LINEAS_DEPOSITO, "IdDeposito", String.valueOf(deposito.IdDeposito), ConstantsTypes.EMPTY_STRING, null);
			
			if (cursor != null)
			{
				cursor.moveToFirst();
				
				if (cursor.getCount() > 0)
				{
					
					do {
						Articulo articulo = Factory.build(Articulo.class, appConfig);
						LineaDeposito linea = Factory.build(LineaDeposito.class, appConfig);
						
						linea.Deposito = deposito; 
						linea.IdLineaDeposito = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdLineaDeposito")));
						linea.IdArticulo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdArticulo")));
						linea.IdDeposito = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdDeposito")));
						linea.UnidadesIniciales = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesIniciales")));
						linea.UnidadesRepuestas = linea.UnidadesIniciales;
						linea.UnidadesInicialesFijas = linea.UnidadesIniciales;
						linea.UnidadesFacturadas = 0;
						
						linea.UnidadesAnterior = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesIniciales")));
						linea.IsNew = false;
						
						if (articulo.setArticuloById(cursor.getString(cursor.getColumnIndex("IdArticulo"))))
						{
							linea.Articulo = articulo;
							linea.StockInicial = articulo.Stock;
							linea.PVPAnterior = getPVP(deposito.Cliente, articulo);
							
							linea.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVPAnterior")));
							linea.PVPAbono = linea.PVP;
							
							if (linea.PVP == 0)
								linea.PVP = getPVP(deposito.Cliente, articulo);
							
							linea.PVPInicial = linea.PVP;
							
						}
						
																		
						list.put(linea.Articulo.CodigoArticulo, linea);
						
					} while (cursor.moveToNext());

				}
				cursor.close();
				return list;
			}
			else
				return list;

	}
	
	public double getPVP(Cliente cliente, Articulo articulo) throws Exception {
		double pvp = articulo.PVP;

		Tarifa tarifa = Factory.build(Tarifa.class, appConfig);

		if (tarifa.setTarifaByClienteArticulo(cliente, articulo)) {
			if (tarifa.PVP != 0) {
				pvp = tarifa.PVP;
			}
		}
    	Pactos pactos = Factory.build(Pactos.class, appConfig);

    	if (pactos.setPactoByClienteArticulo(cliente, articulo)) {
			if (pactos.PVP != 0)
				pvp = pactos.PVP;
		}
    	return pvp;
    	
    }
    
    public double getDte(Cliente cliente, Articulo articulo) throws Exception {
		double dte = articulo.Descuento1;

		Tarifa tarifa = Factory.build(Tarifa.class, appConfig);

		if (tarifa.setTarifaByClienteArticulo(cliente, articulo)) {
			if (tarifa.Descuento1 != 0)
				dte = tarifa.Descuento1;
		}

    	Pactos pactos = Factory.build(Pactos.class, appConfig);
    	
    	if (pactos.setPactoByClienteArticulo(cliente, articulo)) {
			if (pactos.Descuento1 != 0)
				dte = pactos.Descuento1;
		}
    	return dte;
    	
    }

	public LineaMovimientos getMovimientos() {
		
		int total;
		int totalAlmacen;
		int totalDefectuoso = 0;
		int totalDeposito;
		LineaDeposito linea = this;
		
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
    
    public class ArticuloComparator implements Comparator<LineaDeposito> {
	    @Override
	    public int compare(LineaDeposito linea1, LineaDeposito linea2) {
	    	
	    	if ((linea1.Articulo == null) || (linea2.Articulo == null))
	    		return 0;
	    	else if ((linea1.Articulo.CodigoArticulo == null) || (linea2.Articulo.CodigoArticulo == null))
	    		return 0;
	    	else
	    		return linea1.Articulo.Familia.compareTo(linea2.Articulo.Familia);
	    }
	}
}
