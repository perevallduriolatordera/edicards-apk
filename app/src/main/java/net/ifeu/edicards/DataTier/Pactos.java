package net.ifeu.edicards.DataTier;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Utils.MessageBoxType;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class Pactos extends Persistent implements IPersistable {

	public Long IdPacto;
	public Cliente Cliente = new Cliente();
	public Articulo Articulo = new Articulo();
	public double PVP;
	public double Descuento1;
	public double Descuento2;
	
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
				
		ContentValues values = new ContentValues();
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("IdArticulo", this.Articulo.IdArticulo);
		values.put("PVP", this.PVP);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		
		try {
			this.IdPacto = super.getDatabaseOperations().insert(Constants.TABLE_PACTOS, null , values);
		}
		catch (Exception e) {
			throw e;
		}
		
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_PACTOS);
	}
	
	public boolean setPactoById(String idPacto) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_TIPOS_IVA, "IdPacto", idPacto, false);
		
		if (cursor != null)
		{
			this.IdPacto = Long.parseLong(idPacto);
			this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
			this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
			this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
			
			Cliente cliente = new Cliente();
			
			try {
				cliente.InitializePersistance(super.appConfig, super.context);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				super.appConfig.getMessageBox().Show("Error", e.getMessage().toString(), super.appConfig, MessageBoxType.Error);
			}
			
			if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente"))))
				this.Cliente = cliente;
			
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
	
	public boolean setPactoByClienteArticulo(Cliente cliente, Articulo articulo) throws Exception
	{
		
		boolean result = false;
		
		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_PACTOS, "IdArticulo", String.valueOf(articulo.IdArticulo),false, " AND IdCliente = " + cliente.IdCliente , null);
		
		if (cursor != null)
		{
			
			if (cursor.getCount() > 0)
			{
				cursor.moveToFirst();
				
					
				this.IdPacto = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdPacto")));
				this.PVP = Double.parseDouble(cursor.getString(cursor.getColumnIndex("PVP")));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
				this.Cliente = cliente;
				this.Articulo = articulo;
				
				result = true;
			
			}
			
		}
		
		if (cursor != null)
			cursor.close();
		
		return result ; //(cursor != null);
	}
	
}
