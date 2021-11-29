package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.UUID;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Utils.MessageBoxType;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class Historico extends Persistent implements IPersistable {

	public Long IdHistorico;
	public Date Fecha;
	public Cliente Cliente = new Cliente();
	public LinkedHashMap<String,LineaHistorico> Lineas = new LinkedHashMap<String,LineaHistorico>();
	public double Total;
	public double CantidadPagada;
	public String Serie;
	public String NumeroAlbaran;
	public String NombrePresentacion;
	public String PoblacionPresentacion;
	public String CodigoPostalPresentacion;
	public int Tipo;
	public String GUID;
	public String Serializacion;
	public boolean ActualizarStock;
	
	public Historico()
	{
		UUID uuid = UUID.randomUUID();
        GUID = uuid.toString();
	}
	
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
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("CantidadPagada", this.CantidadPagada);
		values.put("Total", this.Total);
		values.put("Serie", this.Serie);
		values.put("NumeroAlbaran", this.NumeroAlbaran);
		values.put("NombrePresentacion", this.NombrePresentacion);
		values.put("PoblacionPresentacion", this.PoblacionPresentacion);
		values.put("CodigoPostalPresentacion",this.CodigoPostalPresentacion);
		values.put("Tipo",this.Tipo);
		values.put("GUID", this.GUID);
		values.put("Serializacion", this.Serializacion);
		values.put("ActualizarStock", this.ActualizarStock ? 1 : 0);
	
		try {
			this.IdHistorico = super.getDatabaseOperations().insert(Constants.TABLE_HISTORICOS, null , values);
		}
		catch (Exception e) {
			throw e;
		}
		
	}
	
	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		values.put("IdHistorico", this.IdHistorico);
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("CantidadPagada", this.CantidadPagada);
		values.put("Total", this.Total);
		values.put("Serie", this.Serie);
		values.put("NumeroAlbaran", this.NumeroAlbaran);
		values.put("NombrePresentacion", this.NombrePresentacion);
		values.put("PoblacionPresentacion", this.PoblacionPresentacion);
		values.put("CodigoPostalPresentacion",this.CodigoPostalPresentacion);
		values.put("Tipo",this.Tipo);
		values.put("GUID", this.GUID);
		values.put("Serializacion", this.Serializacion);
		values.put("ActualizarStock", this.ActualizarStock ? 1 : 0);
		
		String[] whereArgs = { String.valueOf(this.IdHistorico) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_HISTORICOS, values, "IdHistorico = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_HISTORICOS);
	}
	
	public boolean setHistoricoById(String IdHistorico) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_HISTORICOS, "IdHistorico", IdHistorico, false);
		
		if (cursor != null) {
			
			this.IdHistorico = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdHistorico")));
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy"); //please notice the capital M
			this.Fecha = formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Serie = cursor.getString(cursor.getColumnIndex("Serie"));
			this.NumeroAlbaran = cursor.getString(cursor.getColumnIndex("NumeroAlbaran"));
			this.NombrePresentacion = cursor.getString(cursor.getColumnIndex("NombrePresentacion"));
			this.PoblacionPresentacion = cursor.getString(cursor.getColumnIndex("PoblacionPresentacion"));
			this.CodigoPostalPresentacion = cursor.getString(cursor.getColumnIndex("CodigoPostalPresentacion"));
			this.CantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));
			this.Total = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Total")));
			this.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
			this.GUID = cursor.getString(cursor.getColumnIndex("Tipo"));
			this.Serializacion = cursor.getString(cursor.getColumnIndex("Serializacion"));
			this.ActualizarStock = cursor.getInt(cursor.getColumnIndex("ActualizarStock")) == 1 ? true : false;
			
			Cliente cliente = new Cliente();
			
			try {
				cliente.InitializePersistance(super.appConfig, super.context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
			}
			
			if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente"))))
				this.Cliente = cliente;
			
			LineaHistorico linea = new LineaHistorico();
			
			try {
				linea.InitializePersistance(super.appConfig, super.context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
			}
			
			this.Lineas = linea.getLineasHistoricoByHistorico(this);
			cursor.close();
		}
		
		return false;
			
	}
	
	public ArrayList<Historico> getHistoricosBetweenDates(Date fecha1, Date fecha2) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		SimpleDateFormat formatterSQL;
		formatterSQL = new SimpleDateFormat("yyyyMMdd");
		
		SimpleDateFormat formatterSpain;
		formatterSpain = new SimpleDateFormat("MM/dd/yyyy");
		
		Log.i("Report", "Fecha Inicial: " + formatter.format(fecha1));
		Log.i("Report", "Fecha Final: " + formatter.format(fecha2));
		
		ArrayList<Historico> list = new ArrayList<Historico>();
		
		String dateStart = formatterSQL.format(fecha1);
		String dateEnd = formatterSQL.format(fecha2);
		
		
//		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_HISTORICOS + " WHERE Fecha BETWEEN '" +
//				dateStart + "' AND '" + dateEnd + "' ORDER BY NumeroAlbaran ASC");
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + dateStart + "' AND '" + dateEnd + "' ORDER BY NumeroAlbaran ASC");
		
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				
				do {
						
					Historico historico = new Historico();
					historico.IdHistorico = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdHistorico")));
					historico.Fecha = formatterSpain.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
					Log.i("Historico Entre Fechas",cursor.getString(cursor.getColumnIndex("Fecha")));
					historico.Serie = cursor.getString(cursor.getColumnIndex("Serie"));
					historico.NumeroAlbaran = cursor.getString(cursor.getColumnIndex("NumeroAlbaran"));
					historico.NombrePresentacion = cursor.getString(cursor.getColumnIndex("NombrePresentacion"));
					historico.PoblacionPresentacion = cursor.getString(cursor.getColumnIndex("PoblacionPresentacion"));
					historico.CodigoPostalPresentacion = cursor.getString(cursor.getColumnIndex("CodigoPostalPresentacion"));
					historico.CantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));
					historico.Total = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Total")));
					historico.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
					historico.GUID = cursor.getString(cursor.getColumnIndex("GUID"));
					historico.Serializacion = cursor.getString(cursor.getColumnIndex("Serializacion"));
					historico.ActualizarStock = (cursor.getInt(cursor.getColumnIndex("ActualizarStock")) == 1 ? true : false);

					Cliente cliente = new Cliente();
					
					try {
						cliente.InitializePersistance(super.appConfig, super.context);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
					}
					
					if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente"))))
						historico.Cliente = cliente;
					
					LineaHistorico linea = new LineaHistorico();
						
					try {
						linea.InitializePersistance(super.appConfig, super.context);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
					}
					
					historico.Lineas = linea.getLineasHistoricoByHistorico(historico);
					
					if (this.isDateInList(formatter.format(fecha1),formatter.format(fecha2),cursor.getString(cursor.getColumnIndex("Fecha"))))
						list.add(historico);
					
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
	
	public ArrayList<Historico> getHistoricosOfThisWeek(Date today) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");
		
		SimpleDateFormat formatterSpain;
		formatterSpain = new SimpleDateFormat("MM/dd/yyyy");
		
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
		
		ArrayList<Historico> list = new ArrayList<Historico>();
		
		//Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_HISTORICOS + " WHERE Fecha BETWEEN '" +
		//		formatter.format(firstDate) + "' AND '" + formatter.format(today) + "' ORDER BY NumeroAlbaran ASC");
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "' ORDER BY NumeroAlbaran ASC");
		
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				
				do {
						
					Historico historico = new Historico();
					historico.IdHistorico = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdHistorico")));
					historico.Fecha = formatterSpain.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
					Log.i("Historico Entre Fechas",cursor.getString(cursor.getColumnIndex("Fecha")));
					historico.Serie = cursor.getString(cursor.getColumnIndex("Serie"));
					historico.NumeroAlbaran = cursor.getString(cursor.getColumnIndex("NumeroAlbaran"));
					historico.NombrePresentacion = cursor.getString(cursor.getColumnIndex("NombrePresentacion"));
					historico.PoblacionPresentacion = cursor.getString(cursor.getColumnIndex("PoblacionPresentacion"));
					historico.CodigoPostalPresentacion = cursor.getString(cursor.getColumnIndex("CodigoPostalPresentacion"));
					historico.CantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));
					historico.Total = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Total")));
					historico.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
					historico.GUID = cursor.getString(cursor.getColumnIndex("GUID"));
					historico.Serializacion = cursor.getString(cursor.getColumnIndex("Serializacion"));
					historico.ActualizarStock = (cursor.getInt(cursor.getColumnIndex("ActualizarStock"))) == 1 ? true : false;

					Cliente cliente = new Cliente();
					
					try {
						cliente.InitializePersistance(super.appConfig, super.context);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
					}
					
					if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente"))))
						historico.Cliente = cliente;
					
					LineaHistorico linea = new LineaHistorico();
					
					try {
						linea.InitializePersistance(super.appConfig, super.context);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
					}
					
					historico.Lineas = linea.getLineasHistoricoByHistorico(historico);
					
					list.add(historico);
					
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

	public ArrayList<String> getCantidadPagadaOfThisWeekList(Date today) throws Exception
	{

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
		
		
		Historico historico = new Historico();
		historico.InitializePersistance(this.appConfig, this.appConfig);

		ArrayList<Historico> list = historico.getHistoricosBetweenDates(
				firstDate, today);

		ArrayList<String> listString = new ArrayList<String>();
		
		for (Historico hist : list) {

			listString.add(hist.Fecha + " -- " + hist.CantidadPagada);
		}

		return listString;

	}
	
	public double getCantidadPagadaOfThisWeek(Date today) throws Exception
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
		
		//Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_HISTORICOS + " WHERE Fecha BETWEEN '" +
		//		formatter.format(firstDate) + "' AND '" + formatter.format(today) + "' ORDER BY NumeroAlbaran ASC");
		
		double cantidadPagada = 0;
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(CantidadPagada),0) as CantidadPagada FROM " + Constants.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
		
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				cantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));
								
				cursor.close();
				return cantidadPagada;
			}
			else
			{
				cursor.close();
				return 0;
			}
		}
		else
			return 0;

	}

	
	private void DeleteAllLines() throws Exception {
		
		Log.i("Historico","Borrar lineas: " + this.IdHistorico);
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_LINEAS_HISTORICO + " WHERE IdHistorico = "
						+ this.IdHistorico);
		
		if (cursor != null)
			cursor.close();
	}
	
	private void DeleteAllLines(Long id) throws Exception {
		
		Log.i("Historico","Borrar lineas: " + id);
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_LINEAS_HISTORICO + " WHERE IdHistorico = "
						+ id);
		
		if (cursor != null)
			cursor.close();
	}
	
	@Override
	public void delete() throws Exception {
		
		Log.i("Historico","Borrar cabecera: " + this.IdHistorico);
		
		this.DeleteAllLines();
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_HISTORICOS + " WHERE IdHistorico = "
						+ this.IdHistorico);
		
		if (cursor != null)
			cursor.close();
		
	}
	
	public void deleteFromGUID(String guid) throws Exception {
	
		Log.i("Historico","Borrar cabecera: " + guid);
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_HISTORICOS + " WHERE GUID = "
						+ guid);
		
		if (cursor != null)
			cursor.close();
		
		this.DeleteAllLines(this.getIdFromGUID(guid));
	}
	
	private Long getIdFromGUID(String guid) throws Exception {
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_HISTORICOS, "GUID", guid, false);
		
		if (cursor != null) {
			
			Long result = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdHistorico"))); 
			cursor.close();
			
			return result;
		}
		
		return Long.MIN_VALUE;
 
	}
	
	private boolean isDateInList(String date1, String date2, String dateValue)
	{
		
		Log.i("IsDateInList old",date1);
		Log.i("IsDateInList old ",date2);
		Log.i("IsDateInList old",dateValue);
	
		String date1Formatted = date1.substring(6,10) + date1.substring(0,2) + date1.substring(3,5);
		String date2Formatted = date2.substring(6,10) + date2.substring(0,2) + date2.substring(3,5);
		String dateValueFormatted = dateValue.substring(6,10) + dateValue.substring(0,2) + dateValue.substring(3,5);
		
		Log.i("IsDateInList new",date1Formatted);
		Log.i("IsDateInList new",date2Formatted);
		Log.i("IsDateInList new",dateValueFormatted);
		
		boolean result = (dateValueFormatted.compareTo(date1Formatted) > 0 || dateValueFormatted.compareTo(date1Formatted) == 0) 
				&& (dateValueFormatted.compareTo(date2Formatted) < 0 || dateValueFormatted.compareTo(date2Formatted) == 0);
		
		Log.i("IsDateInList result",String.valueOf(result));
		
		return result;
	}
				
}
