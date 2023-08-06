package net.ifeu.edicards.DataTier;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.content.ContentValues;
import android.database.Cursor;

public class ClienteInfo extends Persistent implements IPersistable {
	
	public Cliente Cliente;
	public String CCC;
	public String Representante;
	public String DniRepresentante;
	
	public ClienteInfo()
	{
		this.CCC = ConstantsTypes.EMPTY_STRING;
		this.Representante = ConstantsTypes.EMPTY_STRING;
		this.DniRepresentante = ConstantsTypes.EMPTY_STRING;
	}
	
	public void assignCCC(String codigoBanco, String codigoAgencia, String digitoControl, String numeroCuenta, String iban)
	{
		if (!codigoBanco.equals(ConstantsTypes.EMPTY_STRING) && !codigoAgencia.equals(ConstantsTypes.EMPTY_STRING) &&
			!digitoControl.equals(ConstantsTypes.EMPTY_STRING) && !numeroCuenta.equals(ConstantsTypes.EMPTY_STRING) &&
			!iban.equals(ConstantsTypes.EMPTY_STRING))
			
			this.CCC = iban + "-" + codigoBanco + "-" + codigoAgencia + "-" + digitoControl + "-" + numeroCuenta; 
		else
			this.CCC = ConstantsTypes.EMPTY_STRING;
	}

	@Override
	public void save() throws Exception {
		
		if (this.Cliente != null) {
			ContentValues values = new ContentValues();
			values.put("IdCliente", this.Cliente.IdCliente);
			values.put("CCC", this.CCC);
			values.put("Representante", this.Representante);
			values.put("DniRepresentante", this.DniRepresentante);
			
			
			try {
				super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_CLIENTES_INFO, null , values);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}	
		}
		
	}
	
	@Override
	public void update() throws Exception {
		
		ContentValues values = new ContentValues();
		
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("CCC", this.CCC);
		values.put("Representante", this.Representante);
		values.put("DniRepresentante", this.DniRepresentante);
		
		String[] whereArgs = { String.valueOf(this.Cliente.IdCliente) }; 
		
	    super.getDatabaseOperations().update(ConstantsDatabase.TABLE_CLIENTES_INFO, values, "IdCliente = ?", whereArgs);
	}	
	
	public boolean setClienteInfoByCliente(Cliente cliente) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_CLIENTES_INFO, "IdCliente", String.valueOf(cliente.IdCliente), false);
		
		if (cursor != null)
		{
			
			this.CCC = cursor.getString(cursor.getColumnIndex("CCC"));
			this.Representante = cursor.getString(cursor.getColumnIndex("Representante"));
			this.DniRepresentante = cursor.getString(cursor.getColumnIndex("DniRepresentante"));
			
			// Cliente
			this.Cliente = cliente;
		
		    cursor.close();
			return true;
		} else {
			return false;
		}
	}
	
	public boolean ExistsClienteInfoByCliente(Cliente cliente) throws Exception
	{
		
		boolean result;
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_CLIENTES_INFO, "IdCliente", String.valueOf(cliente.IdCliente), false);
		
		if (cursor != null)
		{		
		    cursor.close();
			result = true;
		} else {
			result = false;
		}
		
		return result;
	}
	
}
