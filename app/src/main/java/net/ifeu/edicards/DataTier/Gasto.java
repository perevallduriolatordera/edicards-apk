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

public class Gasto extends Persistent implements IPersistable {

	public long IdGasto;
	public Date Fecha;
	public Articulo Articulo = Factory.build(Articulo.class, appConfig);
	public double Cantidad; 
	public boolean IsNew;

	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("Cantidad", this.Cantidad);

		try {
			this.IdGasto = super.getDatabaseOperations().insert(Constants.TABLE_GASTOS, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void update() throws Exception {
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		
		values.put("IdGasto", this.IdGasto);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("Cantidad", this.Cantidad);

		String[] whereArgs = { String.valueOf(this.IdGasto) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_GASTOS, values, "IdGasto = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_GASTOS);
	}

	public boolean setGastoByFechaArticulo(Date fecha, Articulo articuloSearch) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_GASTOS + " WHERE Fecha='" +
				formatter.format(fecha) + "' AND IdArticulo = " + articuloSearch.IdArticulo);
		
		if (cursor != null)
		{
			this.IdGasto = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdGasto")));
			
			this.Fecha = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Cantidad")));
			
			Articulo articulo = Factory.build(Articulo.class, appConfig);

			if (articulo.setArticuloById(cursor.getString(cursor.getColumnIndex("IdArticulo"))))
				this.Articulo = articulo;
			
		    cursor.close();
			return true;
		}
		
		return false ; 
	}
}
