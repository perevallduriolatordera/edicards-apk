package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;

public class IngresoDiario extends Persistent implements IPersistable {

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
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_INGRESOS_DIARIOS, "IdIngreso", idIngreso, false);
		
		if (cursor != null)
		{
			this.IdIngreso = Long.parseLong(idIngreso);
			
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("MM/dd/yyyy");
			
			this.Fecha = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Cantidad")));
			this.Ingresos = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Ingresos")));
			this.Gastos = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Gastos")));
			
		    cursor.close();
			return true;
		}
		
		return false ; 
	}
	

	public static Optional<Double> getIngresosDiariosFromDate(AppConfig appConfig) {
		Historico historico = Factory.build(Historico.class, appConfig);
        try {
            return historico.getCantidadPagadaLastDay();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

	public static Date getDateOfLastMovement(AppConfig appConfig) {
		Historico historico = Factory.build(Historico.class, appConfig);
		try {
			return historico.getDateOfLastMovement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ArrayList<IngresoDiario> getIngresosDiariosFromDate() throws Exception
	{
		Historico historico = Factory.build(Historico.class, appConfig);
		String date = historico.getDateOfLastMovementFormatted();

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_INGRESOS_DIARIOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + date + "' AND '" + date + "'");

		ArrayList<IngresoDiario> list = new ArrayList<>();
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				do {
					Long idIngreso = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdIngreso")));
					IngresoDiario ingreso = Factory.build(IngresoDiario.class, appConfig);
					
					if (ingreso.setIngresoById(String.valueOf(idIngreso)))
						list.add(ingreso);
									
				} while (cursor.moveToNext());
			}
			
			cursor.close();
		}

		return list ;
	}

	public ArrayList<IngresoDiario> getIngresosDiariosFromToday() throws Exception
	{
		Historico historico = Factory.build(Historico.class, appConfig);
		String date = historico.getDateFormatted(new Date());

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_INGRESOS_DIARIOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + date + "' AND '" + date + "'");

		ArrayList<IngresoDiario> list = new ArrayList<>();

		if (cursor != null)
		{
			cursor.moveToFirst();

			if (cursor.getCount() > 0)
			{
				do {
					Long idIngreso = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdIngreso")));
					IngresoDiario ingreso = Factory.build(IngresoDiario.class, appConfig);

					if (ingreso.setIngresoById(String.valueOf(idIngreso)))
						list.add(ingreso);

				} while (cursor.moveToNext());
			}

			cursor.close();
		}

		return list ;
	}


	public ArrayList<IngresoDiario> getListIngresosOfThisWeek(Date today) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_INGRESOS_DIARIOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

		ArrayList<IngresoDiario> list = new ArrayList<>();

		if (cursor != null)
		{
			cursor.moveToFirst();

			if (cursor.getCount() > 0)
			{
				do {
					Long idIngreso = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdIngreso")));
					IngresoDiario ingreso = Factory.build(IngresoDiario.class, appConfig);

					if (ingreso.setIngresoById(String.valueOf(idIngreso)))
						list.add(ingreso);

				} while (cursor.moveToNext());
			}

			cursor.close();
		}

		return list;
	}

	public double getQuantityIngresosOfDate(Date date) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(Cantidad),0) as cantidadIngresada FROM " + ConstantsDatabase.TABLE_INGRESOS_DIARIOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
				"BETWEEN '" + formatter.format(date) + "' AND '" + formatter.format(date) + "'");


		double cantidad = 0;
		if (cursor != null) {
			do {
				cursor.moveToFirst();

				if (cursor.getCount() > 0) {
					cantidad = Double.parseDouble(cursor.getString(cursor.getColumnIndex("cantidadIngresada")));
				}

			} while (cursor.moveToNext());

			cursor.close();
		}

		return cantidad ;
	}

}
