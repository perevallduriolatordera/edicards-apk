package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.ifeu.edicards.Constants.Constants;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class GastosInfo extends Persistent implements IPersistable {

	public long IdGastoInfo;
	public Date Fecha;
	public String Comentario = Constants.EMPTY_STRING;
	public Boolean IsNew;
	
	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
	}
	
	@SuppressLint("SimpleDateFormat")
	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Comentario", this.Comentario);
		
		Log.i("Gasto Info save",formatter.format(this.Fecha));
		Log.i("Gasto Info save",this.Comentario);
		
		try {
			this.IdGastoInfo = super.getDatabaseOperations().insert(Constants.TABLE_GASTOS_INFO, null , values);
			Log.i("GastosInfo", "El save s'ha efectuat correctament");
		}
		catch (Exception e) {
			Log.i("GastosInfo", "Error Save: " + e.getMessage());
			throw e;
		}
		
	}
	
	@Override
	public void update() throws Exception {
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		
		values.put("IdGastoInfo", this.IdGastoInfo);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Comentario", this.Comentario);
		
		Log.i("Gasto Info update",formatter.format(this.Fecha));
		
		String[] whereArgs = { String.valueOf(this.IdGastoInfo) }; 
		
	    super.getDatabaseOperations().update(Constants.TABLE_GASTOS_INFO, values, "IdGastoInfo = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_GASTOS_INFO);
	}
	
	public boolean setGastoInfoById(String idGastoInfo) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_GASTOS_INFO, "IdGastoInfo", idGastoInfo, false);
		
		if (cursor != null)
		{
			this.IdGastoInfo = Long.parseLong(idGastoInfo);
			
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");
			
			this.Fecha = (Date) formatter.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
			this.Comentario = cursor.getString(cursor.getColumnIndex("Comentario"));
			
		    cursor.close();
			return true;
		}
		
		return false ; //(cursor != null);
	}
	
	public GastosInfo getGastoInfoByFecha(Date fecha) throws Exception
	{
		
		//Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_TIPOS_IVA, "Articulo", String.valueOf(articulo.TipoIVA), false, "AND Filiacion = '" + cliente.Filiacion + "'",null);
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_GASTOS_INFO + " WHERE Fecha='" + 
				String.valueOf(formatter.format(fecha)) + "'");
		
		GastosInfo gastoInfo = new GastosInfo();
		Boolean result = false;
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				
				Long idGastoInfo = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdGastoInfo")));
				gastoInfo.InitializePersistance(super.appConfig, super.context);
				
				result = gastoInfo.setGastoInfoById(String.valueOf(idGastoInfo));
			}
			
			cursor.close();
		}
		
		
		
		gastoInfo.IsNew = !result;
		return gastoInfo ; 
	}
	
}
