package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Utils.MessageBoxType;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class Gasto extends Persistent implements IPersistable {

	public long IdGasto;
	public Date Fecha;
	public Articulo Articulo = new Articulo();
	public double Cantidad; 
	public boolean IsNew;
	
	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
	}
	
	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("Cantidad", this.Cantidad);
		Log.i("Gasto save",formatter.format(this.Fecha));
		
		try {
			this.IdGasto = super.getDatabaseOperations().insert(Constants.TABLE_GASTOS, null , values);
		}
		catch (Exception e) {
			throw e;
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
		
		Log.i("Gasto update",formatter.format(this.Fecha));
		
		String[] whereArgs = { String.valueOf(this.IdGasto) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_GASTOS, values, "IdGasto = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_GASTOS);
	}
	
	public boolean setGastoById(String idGasto) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_GASTOS, "IdGasto", idGasto, false);
		
		if (cursor != null)
		{
			this.IdGasto = Long.parseLong(idGasto);
			
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");
			
			this.Fecha = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Cantidad")));
			
			Articulo articulo = new Articulo();
			
			try {
				articulo.InitializePersistance(super.appConfig, super.context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
			}
			
			if (articulo.setArticuloById(cursor.getString(cursor.getColumnIndex("IdArticulo"))))
				this.Articulo = articulo;
			
		    cursor.close();
			return true;
		}
		
		return false ; //(cursor != null);
	}
	
	public boolean setGastoByFechaArticulo(Date fecha, Articulo articuloSearch) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		Log.i("Gasto select",formatter.format(fecha));
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_GASTOS + " WHERE Fecha='" + 
				String.valueOf(formatter.format(fecha)) + "' AND IdArticulo = " + articuloSearch.IdArticulo);
		
		if (cursor != null)
		{
			this.IdGasto = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdGasto")));
			
			this.Fecha = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Cantidad")));
			
			Articulo articulo = new Articulo();
			
			try {
				articulo.InitializePersistance(super.appConfig, super.context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
			}
			
			if (articulo.setArticuloById(cursor.getString(cursor.getColumnIndex("IdArticulo"))))
				this.Articulo = articulo;
			
		    cursor.close();
			return true;
		}
		
		return false ; //(cursor != null);
	}
	
	public ArrayList<Gasto> getGastosByFecha(Date fecha) throws Exception
	{
		
		//Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_TIPOS_IVA, "Articulo", String.valueOf(articulo.TipoIVA), false, "AND Filiacion = '" + cliente.Filiacion + "'",null);
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_GASTOS + " WHERE Fecha='" + 
				String.valueOf(fecha) + "'");
		
		ArrayList<Gasto> list = new ArrayList<Gasto>();
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				do {
					Long idGasto = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdGasto")));
					Gasto gasto = new Gasto();
					gasto.InitializePersistance(super.appConfig, super.context);
					
					if (gasto.setGastoById(String.valueOf(idGasto)));
						list.add(gasto);
									
				} while (cursor.moveToNext());
			}	
		}
		
		if (cursor != null)
			cursor.close();
		
		return list ; 
	}
	
}
