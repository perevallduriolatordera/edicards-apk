package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_INGRESOS, "IdIngreso", idIngreso, false);
		
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
	
	
	public IngresoDiario getIngresosDiariosFromDate() throws Exception
	{
		Historico historico = Factory.build(Historico.class, appConfig);
		String date = historico.getDateOfLastMovement();

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_INGRESOS + " WHERE substr(Fecha,7)||substr(Fecha,1,2)||substr(Fecha,4,2) " +
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
						return ingreso;
									
				} while (cursor.moveToNext());
			}
			
			cursor.close();
		}
		
		return null;
	}

}
