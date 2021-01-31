package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.ifeu.edicards.Constants.Constants;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class Ingresos extends Persistent implements IPersistable {

	public long IdIngreso;
	public Date Fecha;
	public String Entidad;
	public double Cantidad; 
	public String Referencia;
	public String Descripcion;
	
	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
	}
	
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
		
		Log.i("Ingreso save",formatter.format(this.Fecha));
		
		try {
			this.IdIngreso = super.getDatabaseOperations().insert(Constants.TABLE_INGRESOS, null , values);
		}
		catch (Exception e) {
			throw e;
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
		
		Log.i("Ingreso update",formatter.format(this.Fecha));
		
		String[] whereArgs = { String.valueOf(this.IdIngreso) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_INGRESOS, values, "IdGasto = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_INGRESOS);
	}
	
	public boolean setIngresoById(String idIngreso) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_INGRESOS, "IdIngreso", idIngreso, false);
		
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
		
		return false ; //(cursor != null);
	}
	
	
	public ArrayList<Ingresos> getIngresosOfThisWeek(Date today) throws Exception
	{
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(today);
		
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int daysToSubstract = 0;
		
		switch (dayOfWeek) {
		    case Calendar.SUNDAY:
		    	daysToSubstract = -6;
		    	break;
		    case Calendar.MONDAY:
		    	daysToSubstract = 0;
		    	break;
		    case Calendar.TUESDAY:
		    	daysToSubstract = -1;
		    	break;
		    case Calendar.WEDNESDAY:
		        daysToSubstract = -2;
		        break;
		    case Calendar.THURSDAY:
		    	daysToSubstract = -3;
		    	break;
		    case Calendar.FRIDAY:
		    	daysToSubstract = -4;
		    	break;
		    case Calendar.SATURDAY:
		    	daysToSubstract = -5;
		    	break;
		}
		
		calendar.add( Calendar.DAY_OF_YEAR, daysToSubstract);
		Date firstDate = calendar.getTime();
		
	//	Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_INGRESOS + " WHERE Fecha BETWEEN '" + 
	//			formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_INGRESOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
		
		//Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_INGRESOS); 
		
		ArrayList<Ingresos> list = new ArrayList<Ingresos>();
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				do {
					Long idIngreso = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdIngreso")));
					Ingresos ingreso = new Ingresos();
					ingreso.InitializePersistance(super.appConfig, super.context);
					
					if (ingreso.setIngresoById(String.valueOf(idIngreso)));
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
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(today);
		
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int daysToSubstract = 0;
		
		switch (dayOfWeek) {
		    case Calendar.SUNDAY:
		    	daysToSubstract = -6;
		    	break;
		    case Calendar.MONDAY:
		    	daysToSubstract = 0;
		    	break;
		    case Calendar.TUESDAY:
		    	daysToSubstract = -1;
		    	break;
		    case Calendar.WEDNESDAY:
		        daysToSubstract = -2;
		        break;
		    case Calendar.THURSDAY:
		    	daysToSubstract = -3;
		    	break;
		    case Calendar.FRIDAY:
		    	daysToSubstract = -4;
		    	break;
		    case Calendar.SATURDAY:
		    	daysToSubstract = -5;
		    	break;
		}
		
		calendar.add( Calendar.DAY_OF_YEAR, daysToSubstract);
		Date firstDate = calendar.getTime();
		
	//	Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_INGRESOS + " WHERE Fecha BETWEEN '" + 
	//			formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(Cantidad),0) as cantidadIngresada FROM " + Constants.TABLE_INGRESOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
		
		//Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_INGRESOS); 
				
		double cantidad = 0;
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("cantidadIngresada")));
				
			}
			
			cursor.close();
		}
		
		return cantidad ; 
	}
	
}
