package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Tarifa extends Persistent implements IPersistable {

	public long IdTarifa;
	public String CodigoTarifa;
	public Date FechaIni;
	public Date FechaFin;
	public Articulo Articulo = Factory.build(Articulo.class, appConfig);
	public double PVP;
	public double Descuento1;
	public double Descuento2;

	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("CodigoTarifa", this.CodigoTarifa);
		values.put("FechaIni",formatter.format(this.FechaIni));
		values.put("FechaFin",formatter.format(this.FechaFin));
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("PVP", this.PVP);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		
		try {
			this.IdTarifa = super.getDatabaseOperations().insert(Constants.TABLE_TARIFAS, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);

		}
		
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_TARIFAS);
	}

	public boolean setTarifaByClienteArticulo(Cliente cliente, Articulo articulo) throws Exception
	{
		
		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_TARIFAS, "IdArticulo", String.valueOf(articulo.IdArticulo), false, "AND CodigoTarifa = '" + cliente.CodigoTarifa + "'",null);
		
		boolean founded = false;
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			if (cursor.getCount() > 0)
			{
			
				do {
					SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
					
					Date now = new Date();
					Date fechaIni = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("FechaIni")));
					Date fechaFin = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("FechaFin")));
					
					if ((now.after(fechaIni) ||  formatter.format(now).equals(formatter.format(fechaIni)))  && (now.before(fechaFin) ||  formatter.format(now).equals(formatter.format(fechaFin))))
					{
						founded = true;
						break;
					}
					
				} while (cursor.moveToNext());
			}
		}
		
		if (founded)
		{
			this.IdTarifa = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdTarifa")));
			this.CodigoTarifa = cursor.getString(cursor.getColumnIndex("CodigoTarifa"));
			
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			
			this.FechaIni = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("FechaIni")));
			this.FechaFin = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("FechaFin")));
			this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
			this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
			this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
			
			this.Articulo = articulo;
			cursor.close();
			return true;
		}
		
		if (cursor != null)
			cursor.close();
		
		return false ; 
	}
}
