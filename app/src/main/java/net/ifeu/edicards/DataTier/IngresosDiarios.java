package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class IngresosDiarios extends Persistent implements IPersistable {

	public long IdIngreso;
	public Date Fecha;
	public double Cantidad; 
	public double Ingresos;
	public double Gastos;

	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Ingresos", this.Ingresos);
		values.put("Cantidad", this.Cantidad);
		values.put("Gastos", this.Gastos);

		try {
			this.IdIngreso = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_INGRESOS_DIARIOS, null , values);
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
		values.put("Ingresos", this.Ingresos);
		values.put("Cantidad", this.Cantidad);
		values.put("Gastos", this.Gastos);
		String[] whereArgs = { String.valueOf(this.IdIngreso) }; 
		
	    super.getDatabaseOperations().update(ConstantsDatabase.TABLE_INGRESOS_DIARIOS, values, "IdGasto = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(ConstantsDatabase.TABLE_INGRESOS_DIARIOS);
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
			this.Ingresos = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Ingresos")));
			this.Gastos = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Gastos")))
			
		    cursor.close();
			return true;
		}
		
		return false ; 
	}
	
	
	public IngresosDiarios getIngresosDiariosFromYesterday(Date today) throws Exception
	{

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(today);
		calendar.add(Calendar.DATE, -1);
		Date yesterday = calendar.getTime();

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(yesterday);

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_INGRESOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(yesterday) + "' AND '" + formatter.format(yesterday) + "'");

		ArrayList<IngresosDiarios> list = new ArrayList<>();
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				do {
					Long idIngreso = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdIngreso")));
					IngresosDiarios ingreso = Factory.build(IngresosDiarios.class, appConfig);
					
					if (ingreso.setIngresoById(String.valueOf(idIngreso)))
						return ingreso;
									
				} while (cursor.moveToNext());
			}
			
			cursor.close();
		}
		
		return null;
	}

}
