package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;

public class GastosInfo extends Persistent implements IPersistable {

	public long IdGastoInfo;
	public Date Fecha;
	public String Comentario = ConstantsTypes.EMPTY_STRING;
	public Boolean IsNew;

	@SuppressLint("SimpleDateFormat")
	@Override
	public void save() throws Exception {
				
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Comentario", this.Comentario);
		
		try {
			this.IdGastoInfo = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_GASTOS_INFO, null , values);
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
		
		values.put("IdGastoInfo", this.IdGastoInfo);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("Comentario", this.Comentario);

		String[] whereArgs = { String.valueOf(this.IdGastoInfo) }; 
		
	    super.getDatabaseOperations().update(ConstantsDatabase.TABLE_GASTOS_INFO, values, "IdGastoInfo = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(ConstantsDatabase.TABLE_GASTOS_INFO);
	}
	
	public boolean setGastoInfoById(String idGastoInfo) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_GASTOS_INFO, "IdGastoInfo", idGastoInfo, false);
		
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
		
		return false ; 
	}
	
	public GastosInfo getGastoInfoByFecha(Date fecha) throws Exception
	{
		
		//Cursor cursor = super.getDatabaseOperations().getRecordsFromField(ConstantsDatabase.TABLE_TIPOS_IVA, "Articulo", String.valueOf(articulo.TipoIVA), false, "AND Filiacion = '" + cliente.Filiacion + "'",null);
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_GASTOS_INFO + " WHERE Fecha='" +
				formatter.format(fecha) + "'");
		
		GastosInfo gastoInfo = Factory.build(GastosInfo.class, appConfig);
		boolean result = false;
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				Long idGastoInfo = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdGastoInfo")));
				result = gastoInfo.setGastoInfoById(String.valueOf(idGastoInfo));
			}
			
			cursor.close();
		}
		
		
		
		gastoInfo.IsNew = !result;
		return gastoInfo ; 
	}
	
}
