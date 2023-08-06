package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import android.content.ContentValues;
import android.database.Cursor;

public class Ingresos extends Persistent implements IPersistable {

	public long IdIngreso;
	public Date Fecha;
	public String Entidad;
	public double Cantidad; 
	public String Referencia;
	public String Descripcion;

	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Entidad", this.Entidad);
		values.put("Cantidad", this.Cantidad);
		values.put("Referencia", this.Referencia);
		values.put("Descripcion", this.Descripcion);

		try {
			this.IdIngreso = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_INGRESOS, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void update() throws Exception {
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		ContentValues values = new ContentValues();
		
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Entidad", this.Entidad);
		values.put("Cantidad", this.Cantidad);
		values.put("Referencia", this.Referencia);
		values.put("Descripcion", this.Descripcion);

		String[] whereArgs = { String.valueOf(this.IdIngreso) }; 
		
	    super.getDatabaseOperations().update(ConstantsDatabase.TABLE_INGRESOS, values, "IdGasto = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(ConstantsDatabase.TABLE_INGRESOS);
	}
	
	public boolean setIngresoById(String idIngreso) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_INGRESOS, "IdIngreso", idIngreso, false);
		
		if (cursor != null)
		{
			this.IdIngreso = Long.parseLong(idIngreso);
			
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("MM/dd/yyyy");
			
			this.Fecha = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Cantidad")));
			this.Entidad = cursor.getString(cursor.getColumnIndex("Entidad"));
			this.Referencia = cursor.getString(cursor.getColumnIndex("Referencia"));
			this.Descripcion = cursor.getString(cursor.getColumnIndex("Descripcion"));
			
		    cursor.close();
			return true;
		}
		
		return false ; 
	}
	
	
	public ArrayList<Ingresos> getIngresosOfThisWeek(Date today) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_INGRESOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

		ArrayList<Ingresos> list = new ArrayList<>();
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				do {
					Long idIngreso = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdIngreso")));
					Ingresos ingreso = Factory.build(Ingresos.class, appConfig);
					
					if (ingreso.setIngresoById(String.valueOf(idIngreso)))
						list.add(ingreso);
									
				} while (cursor.moveToNext());
			}
			
			cursor.close();
		}
		
		return list ; 
	}
	
	public double getTotalIngresosThisWeek(Date today) throws Exception
	{
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");
		

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(Cantidad),0) as cantidadIngresada FROM " + ConstantsDatabase.TABLE_INGRESOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

				
		double cantidad = 0;
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
				cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("cantidadIngresada")));

			cursor.close();
		}
		
		return cantidad ; 
	}
	
}
