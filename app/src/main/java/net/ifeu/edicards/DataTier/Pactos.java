package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

public class Pactos extends Persistent implements IPersistable {

	public Long IdPacto;
	public Cliente Cliente = Factory.build(Cliente.class, appConfig);
	public Articulo Articulo = Factory.build(Articulo.class, appConfig);
	public double PVP;
	public double Descuento1;
	public double Descuento2;

	@Override
	public void save() throws Exception {
				
		ContentValues values = new ContentValues();
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("PVP", this.PVP);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		
		try {
			this.IdPacto = super.getDatabaseOperations().insert(Constants.TABLE_PACTOS, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_PACTOS);
	}

	public boolean setPactoByClienteArticulo(Cliente cliente, Articulo articulo)
	{
		
		boolean result = false;
		
		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_PACTOS, "IdArticulo", String.valueOf(articulo.IdArticulo),false, " AND IdCliente = " + cliente.IdCliente , null);
		
		if (cursor != null)
		{
			
			if (cursor.getCount() > 0)
			{
				cursor.moveToFirst();
				
					
				this.IdPacto = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdPacto")));
				this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
				this.Cliente = cliente;
				this.Articulo = articulo;
				
				result = true;
			
			}
			
		}
		
		if (cursor != null)
			cursor.close();
		
		return result ; 
	}
	
}
