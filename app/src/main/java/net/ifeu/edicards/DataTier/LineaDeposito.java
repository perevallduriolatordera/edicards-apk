package net.ifeu.edicards.DataTier;

import java.util.Comparator;
import java.util.LinkedHashMap;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Utils.MessageBoxType;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class LineaDeposito extends Persistent implements IPersistable {

	public long IdLineaDeposito;
	public long IdDeposito;
	public long IdArticulo;
	public Deposito Deposito = new Deposito();
	public Articulo Articulo = new Articulo();
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
	public boolean IsVentaDirecta;
	public double PVPInicial;
	public int UnidadesAbono;
	public int DefectuosasAbono;
	public double PVPAbono;
	public double TotalAbono;
	
	@Override
	public void InitializePersistance(AppConfig appConfig, Context context) throws Exception {
		// TODO Auto-generated method stub
		super.InitializePersistance(appConfig, context);
	}
	
	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
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
			this.IdLineaDeposito = super.getDatabaseOperations().insert(Constants.TABLE_LINEAS_DEPOSITO, null , values);
		}
		catch (Exception e) {
			throw e;
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
		
	    super.getDatabaseOperations().update(Constants.TABLE_LINEAS_DEPOSITO, values, "IdLineaDeposito = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_LINEAS_DEPOSITO);
	}
	
	
	public LinkedHashMap<String, LineaDeposito> getLineasDepositosByDeposito(Deposito deposito) throws Exception
	{
			LinkedHashMap<String,LineaDeposito> list = new LinkedHashMap<String,LineaDeposito>();
			
			Cursor cursor = super.getDatabaseOperations().getRecordsFromFieldNumeric(Constants.TABLE_LINEAS_DEPOSITO, "IdDeposito", String.valueOf(deposito.IdDeposito), Constants.EMPTY_STRING, null);
			
			if (cursor != null)
			{
				cursor.moveToFirst();
				
				if (cursor.getCount() > 0)
				{
					
					do {
						Log.i("getLineasDepositoByDeposito","Entro");
						Articulo articulo = new Articulo();
						
						try {
							articulo.InitializePersistance(super.appConfig, super.context);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
						}
						
						LineaDeposito linea = new LineaDeposito();
						
						linea.Deposito = deposito; 
						linea.IdLineaDeposito = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdLineaDeposito")));
						linea.IdArticulo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdArticulo")));
						linea.IdDeposito = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdDeposito")));
						linea.UnidadesIniciales = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesIniciales")));
						linea.UnidadesRepuestas = linea.UnidadesIniciales;
						linea.UnidadesInicialesFijas = linea.UnidadesIniciales;
						linea.UnidadesFacturadas = linea.UnidadesInicialesFijas;
						
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
					
					cursor.close();
					return list;
					}
				else
				{
					cursor.close();
					return list;
				}
			}
			else
				return list;

	}
	
	public double getPVP(Cliente cliente, Articulo articulo) throws Exception
    {
    	double pvp = articulo.PVP;
    	
    	Tarifa tarifa = new Tarifa();
    	
    	try {
			tarifa.InitializePersistance(super.appConfig, super.context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			super.appConfig.getMessageBox()
					.Show("Error", e.getMessage().toString(),
							super.context,
							MessageBoxType.Error);
		}
    	
    	
    	if (tarifa.setTarifaByClienteArticulo(cliente, articulo));
    		if (tarifa.PVP != 0)
    		{
    			Log.i("LineaDeposito", "Assigno pvp de tarifa. PVP = " + String.valueOf(tarifa.PVP));
    			pvp = tarifa.PVP;
    		}
    	
    	Pactos pactos = new Pactos();
    	
    	try {
    		pactos.InitializePersistance(super.appConfig, super.context.getApplicationContext());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			super.appConfig.getMessageBox()
					.Show("Error", e.getMessage().toString(),
							super.context,
							MessageBoxType.Error);
		}
    	
    	if (pactos.setPactoByClienteArticulo(cliente, articulo));
    		if (pactos.PVP != 0)
    			pvp = pactos.PVP;
    	
    	return pvp;
    	
    }
    
    public double getDte(Cliente cliente, Articulo articulo) throws Exception
    {
    	double dte = articulo.Descuento1;
    	
    	Tarifa tarifa = new Tarifa();
    	
    	try {
			tarifa.InitializePersistance(super.appConfig, super.context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			super.appConfig.getMessageBox()
					.Show("Error", e.getMessage().toString(),
							super.context,
							MessageBoxType.Error);
		}
    	
    	
    	if (tarifa.setTarifaByClienteArticulo(cliente, articulo));
    		if (tarifa.Descuento1 != 0)
    		{
    			Log.i("LineaDeposito", "Assigno dte de tarifa. DTE = " + String.valueOf(tarifa.Descuento1));
    			dte = tarifa.Descuento1;
    		}
    	
    	Pactos pactos = new Pactos();
    	
    	try {
    		pactos.InitializePersistance(appConfig, super.context);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			super.appConfig.getMessageBox()
			.Show("Error", e.getMessage().toString(),
					super.context,
					MessageBoxType.Error);
		}
    	
    	if (pactos.setPactoByClienteArticulo(cliente, articulo));
			if (pactos.Descuento1 != 0)
				dte = pactos.Descuento1;
	
    	return dte;
    	
    }
    
    public Boolean existArticuloInDeposito(String IdDeposito, String IdArticulo) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_LINEAS_DEPOSITO + " WHERE IdDeposito = " +
				IdDeposito + " AND IdArticulo = " + IdArticulo);
				
		if (cursor != null)
		{
			cursor.moveToFirst();
			int count = cursor.getCount();
			cursor.close();
			return (count  > 0);
		}
		else
			return false;
	}
    
	public LineaMovimientos getMovimientos() {
		
		int total = 0;
		int totalAlmacen = 0;
		int totalDefectuoso = 0;
		int totalDeposito = 0;
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
