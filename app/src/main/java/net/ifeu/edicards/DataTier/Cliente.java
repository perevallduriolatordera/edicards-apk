package net.ifeu.edicards.DataTier;

import java.util.LinkedList;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class Cliente extends Persistent implements IPersistable {

	public long IdCliente;
	public boolean Activo;
	public String CodigoCliente;
	public String Nombre;
	public String NIF;
	public String Razon;
	public String Direccion1;
	public String Direccion2;
	public String CodigoPostal;
	public String Poblacion;
	public String Provincia;
	public String Telefono1;
	public String Telefono2;
	public String Fax;
	public String Clave;
	public String Mail; 
	public String Web;
	public double Descuento1;
	public double Descuento2;
	public double DescuentoProntoPago;
	public double DescuentoFinanciero;
	public String Filiacion;
	public FormaPago formaPago = new FormaPago();
	public String CodigoTarifa;
	public ClienteInfo ClienteInfo;
	
	public String NIFPrevious;
	public String RazonPrevious;
	public String NombrePrevious;
	public String DireccionPrevious;
	public String PoblacionPrevious;
	public String CodigoPostalPrevious;
	public String ProvinciaPrevious;
	public String Telefono1Previous;
	public String Telefono2Previous;
	public String FaxPrevious;
	public String MailPrevious;
	public String CCCPrevious;
	
	
	public Cliente() {
		// TODO Auto-generated constructor stub
		this.ClienteInfo = new ClienteInfo();
	}

	@Override
	public void InitializePersistance(AppConfig appConfig, Context context) {
		super.InitializePersistance(appConfig, context);
	}

	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
	}
	
	@Override
	public void save() throws Exception {
		
		ContentValues values = new ContentValues();
		values.put("Activo", this.Activo ? "1" : "2");
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("Nombre", this.Nombre);
		values.put("NIF", this.NIF);
		values.put("Razon",this.Razon);
		values.put("Direccion1", this.Direccion1);
		values.put("Direccion2", this.Direccion2);
		values.put("CodigoPostal",this.CodigoPostal);
		values.put("Poblacion", this.Poblacion);
		values.put("Provincia", this.Provincia);
		values.put("Telefono1", this.Telefono1);
		values.put("Telefono2", this.Telefono2);
		values.put("Fax", this.Fax);
		values.put("Clave", this.Clave);
		values.put("Mail", this.Mail);
		values.put("Web", this.Web);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		values.put("DescuentoProntoPago", this.DescuentoProntoPago);
		values.put("DescuentoFinanciero", this.DescuentoFinanciero);
		values.put("Filiacion", this.Filiacion);
		values.put("IdFormaPago", this.formaPago.IdFormaPago);
		values.put("CodigoTarifa", this.CodigoTarifa);

		try {
			this.IdCliente = super.getDatabaseOperations().insert(Constants.TABLE_CLIENTES, null , values);
			
			this.ClienteInfo.InitializePersistance(appConfig, context);
			this.ClienteInfo.save();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void update() throws Exception {
		
		ContentValues values = new ContentValues();
		
		values.put("Activo", this.Activo ? "1" : "2");
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("Nombre", this.Nombre);
		values.put("NIF", this.NIF);
		values.put("Razon",this.Razon);
		values.put("Direccion1", this.Direccion1);
		values.put("Direccion2", this.Direccion2);
		values.put("CodigoPostal",this.CodigoPostal);
		values.put("Poblacion", this.Poblacion);
		values.put("Provincia", this.Provincia);
		values.put("Telefono1", this.Telefono1);
		values.put("Telefono2", this.Telefono2);
		values.put("Fax", this.Fax);
		values.put("Clave", this.Clave);
		values.put("Mail", this.Mail);
		values.put("Web", this.Web);
		values.put("Descuento1", this.Descuento1);
		values.put("Descuento2", this.Descuento2);
		values.put("DescuentoProntoPago", this.DescuentoProntoPago);
		values.put("DescuentoFinanciero", this.DescuentoFinanciero);
		values.put("Filiacion", this.Filiacion);
		values.put("IdFormaPago", this.formaPago.IdFormaPago);
		values.put("CodigoTarifa", this.CodigoTarifa);
		
		String[] whereArgs = { String.valueOf(this.IdCliente) }; 
		
			super.getDatabaseOperations().update(Constants.TABLE_CLIENTES, values, "IdCliente = ?", whereArgs);
	    
	    this.ClienteInfo.InitializePersistance(appConfig, context);
	    
	    if (this.ClienteInfo.ExistsClienteInfoByCliente(this))
	    	this.ClienteInfo.update();
	    else
	    	this.ClienteInfo.save();
	}	
	
	
	public LinkedList<String> getClientesByFilter(String text,int filter,boolean onlyStartsWith) throws Exception
	{		
		LinkedList<String> list = new LinkedList<>();
		LinkedList<String> finalList = new LinkedList<>();
		
		switch (filter)
		{
		    
			case Constants.CUSTOMER_FILTER_NAME :list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente","Nombre",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case Constants.CUSTOMER_FILTER_NIF:list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente", "NIF",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case Constants.CUSTOMER_FILTER_PHONE:list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente", "Telefono1",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case Constants.CUSTOMER_FILTER_CITY:list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente", "Poblacion", text, onlyStartsWith, "AND Activo = 1", "Nombre");break;
		}
		
		for (String value: list)
		{
			Cliente cliente = new Cliente();
			cliente.InitializePersistance(super.appConfig, super.context);
			
			String finalValue;
			if (cliente.setClienteByCodigo(value))
			{
				finalValue = value + " --- " + cliente.Nombre + " --- " + cliente.Poblacion + " --- " + cliente.Direccion1;
				finalList.add(finalValue);
			}
		}
		return finalList;
	}
	
	public LinkedList<String> getClientesNameByFilter(String text,int filter,boolean onlyStartsWith) throws Exception
	{		
		LinkedList<String> list = new LinkedList<>();
		LinkedList<String> finalList = new LinkedList<>();
		
		switch (filter)
		{
		    
			case Constants.CUSTOMER_FILTER_NAME :list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente","Nombre",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case Constants.CUSTOMER_FILTER_NIF:list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente", "NIF",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case Constants.CUSTOMER_FILTER_PHONE:list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente", "Telefono1",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case Constants.CUSTOMER_FILTER_CITY:list = super.getDatabaseOperations().getStringArrayByField(Constants.TABLE_CLIENTES, "CodigoCliente", "Poblacion", text, onlyStartsWith, "AND Activo = 1", "Nombre");break;
		}
		
		for (String value: list)
		{
			Cliente cliente = new Cliente();
			cliente.InitializePersistance(super.appConfig, super.context);
			
			String finalValue;
			if (cliente.setClienteByCodigo(value))
			{
				finalValue = cliente.Nombre;
				finalList.add(finalValue);
			}
		}
		return finalList;
	}
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_CLIENTES);
	}
	
	public boolean setClienteByName(String name) throws Exception
	{
		
		name = name.replace("'", "''");
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_CLIENTES, "Nombre", name, true);
		
		if (cursor != null)
		{
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			
			if (this.Activo) {
				this.Clave = cursor.getString(cursor.getColumnIndex("Clave"));
				this.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				this.CodigoPostal= cursor.getString(cursor.getColumnIndex("CodigoPostal"));
				this.Direccion1= cursor.getString(cursor.getColumnIndex("Direccion1"));
				this.Direccion2= cursor.getString(cursor.getColumnIndex("Direccion2"));
				this.Fax = cursor.getString(cursor.getColumnIndex("Fax"));
				this.IdCliente = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdCliente")));
				this.Mail= cursor.getString(cursor.getColumnIndex("Mail"));
				this.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
				this.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
				this.Poblacion= cursor.getString(cursor.getColumnIndex("Poblacion"));
				this.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));
				this.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
				this.Telefono1= cursor.getString(cursor.getColumnIndex("Telefono1"));
				this.Telefono2= cursor.getString(cursor.getColumnIndex("Telefono2"));
				this.Web= cursor.getString(cursor.getColumnIndex("Web"));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
				this.DescuentoProntoPago = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoProntoPago")));
				this.DescuentoFinanciero = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoFinanciero")));
				this.CodigoTarifa = cursor.getString(cursor.getColumnIndex("CodigoTarifa"));
				this.Filiacion = cursor.getString(cursor.getColumnIndex("Filiacion"));
				
				// Forma de Pago
				FormaPago formaPago = new FormaPago();
				formaPago.InitializePersistance(super.appConfig, super.context);

				if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
					this.formaPago = formaPago;
				
				ClienteInfo clienteInfo = new ClienteInfo();
				clienteInfo.InitializePersistance(super.appConfig, super.context);

				if (clienteInfo.setClienteInfoByCliente(this))
					this.ClienteInfo = clienteInfo;

			    cursor.close();
			    
				return true; 
			}
			else {
				cursor.close();
				return false;
			}
			
		}
		
		return false ; 
	}
	
	public boolean setClienteById(String IdCliente) throws Exception
	{
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_CLIENTES, "IdCliente", IdCliente, false);
		
		if (cursor != null)
		{
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			
			if (this.Activo) {
				this.IdCliente = Long.parseLong(IdCliente);
				this.Clave = cursor.getString(cursor.getColumnIndex("Clave"));
				this.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				this.CodigoPostal= cursor.getString(cursor.getColumnIndex("CodigoPostal"));
				this.Direccion1= cursor.getString(cursor.getColumnIndex("Direccion1"));
				this.Direccion2= cursor.getString(cursor.getColumnIndex("Direccion2"));
				this.Fax = cursor.getString(cursor.getColumnIndex("Fax"));
				this.IdCliente = Integer.parseInt(cursor.getString(cursor.getColumnIndex("IdCliente")));
				this.Mail= cursor.getString(cursor.getColumnIndex("Mail"));
				this.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
				this.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
				this.Poblacion= cursor.getString(cursor.getColumnIndex("Poblacion"));
				this.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));
				this.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
				this.Telefono1= cursor.getString(cursor.getColumnIndex("Telefono1"));
				this.Telefono2= cursor.getString(cursor.getColumnIndex("Telefono2"));
				this.Web= cursor.getString(cursor.getColumnIndex("Web"));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
				this.DescuentoProntoPago = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoProntoPago")));
				this.DescuentoFinanciero = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoFinanciero")));
				this.CodigoTarifa = cursor.getString(cursor.getColumnIndex("CodigoTarifa"));
				this.Filiacion = cursor.getString(cursor.getColumnIndex("Filiacion"));
				
				// Forma de Pago
				FormaPago formaPago = new FormaPago();
				formaPago.InitializePersistance(super.appConfig, super.context);

				if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
					this.formaPago = formaPago;
				
				// ClienteInfo
				ClienteInfo clienteInfo = new ClienteInfo();
				clienteInfo.InitializePersistance(super.appConfig, super.context);

				if (clienteInfo.setClienteInfoByCliente(this))
					this.ClienteInfo = clienteInfo;

			    cursor.close();
				return true;
			} else {
				cursor.close();
				return false;
			}
			
		}
		
		return false ; 
	}

	public boolean setClienteByCodigoStatus(String codigoCliente) throws Exception
	{
		if (codigoCliente == null)
			return false;
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_CLIENTES, "CodigoCliente", codigoCliente, true);
		
		if (cursor != null)
		{
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			this.IdCliente = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdCliente")));
			this.Clave = cursor.getString(cursor.getColumnIndex("Clave"));
			this.CodigoCliente = codigoCliente;
			this.CodigoPostal= cursor.getString(cursor.getColumnIndex("CodigoPostal"));
			this.Direccion1= cursor.getString(cursor.getColumnIndex("Direccion1"));
			this.Direccion2= cursor.getString(cursor.getColumnIndex("Direccion2"));
			this.Fax = cursor.getString(cursor.getColumnIndex("Fax"));
			this.Mail= cursor.getString(cursor.getColumnIndex("Mail"));
			this.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
			this.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
			this.Poblacion= cursor.getString(cursor.getColumnIndex("Poblacion"));
			this.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));
			this.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
			this.Telefono1= cursor.getString(cursor.getColumnIndex("Telefono1"));
			this.Telefono2= cursor.getString(cursor.getColumnIndex("Telefono2"));
			this.Web= cursor.getString(cursor.getColumnIndex("Web"));
			this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
			this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
			this.DescuentoProntoPago = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoProntoPago")));
			this.DescuentoFinanciero = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoFinanciero")));
			this.CodigoTarifa = cursor.getString(cursor.getColumnIndex("CodigoTarifa"));
			this.Filiacion = cursor.getString(cursor.getColumnIndex("Filiacion"));
			
			// Forma de Pago
			FormaPago formaPago = new FormaPago();
			formaPago.InitializePersistance(super.appConfig, super.context);

			if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
				this.formaPago = formaPago;
			
			// ClienteInfo
			ClienteInfo clienteInfo = new ClienteInfo();
			clienteInfo.InitializePersistance(super.appConfig, super.context);

			if (clienteInfo.setClienteInfoByCliente(this))
				this.ClienteInfo = clienteInfo;
		
		    cursor.close();
			return true;
		} else {
			return false;
		}
			
	}
	
	public boolean setClienteByCodigo(String codigoCliente) throws Exception
	{
		if (codigoCliente == null)
			return false;
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_CLIENTES, "CodigoCliente", codigoCliente, true);
		
		if (cursor != null)
		{
			this.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
			
			if (this.Activo) {
				
				this.IdCliente = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdCliente")));
				this.Clave = cursor.getString(cursor.getColumnIndex("Clave"));
				this.CodigoCliente = codigoCliente;
				this.CodigoPostal= cursor.getString(cursor.getColumnIndex("CodigoPostal"));
				this.Direccion1= cursor.getString(cursor.getColumnIndex("Direccion1"));
				this.Direccion2= cursor.getString(cursor.getColumnIndex("Direccion2"));
				this.Fax = cursor.getString(cursor.getColumnIndex("Fax"));
				this.Mail= cursor.getString(cursor.getColumnIndex("Mail"));
				this.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
				this.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
				this.Poblacion= cursor.getString(cursor.getColumnIndex("Poblacion"));
				this.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));
				this.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
				this.Telefono1= cursor.getString(cursor.getColumnIndex("Telefono1"));
				this.Telefono2= cursor.getString(cursor.getColumnIndex("Telefono2"));
				this.Web= cursor.getString(cursor.getColumnIndex("Web"));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Descuento2")));
				this.DescuentoProntoPago = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoProntoPago")));
				this.DescuentoFinanciero = Double.parseDouble(cursor.getString(cursor.getColumnIndex("DescuentoFinanciero")));
				this.CodigoTarifa = cursor.getString(cursor.getColumnIndex("CodigoTarifa"));
				this.Filiacion = cursor.getString(cursor.getColumnIndex("Filiacion"));
				
				// Forma de Pago
				FormaPago formaPago = new FormaPago();
				formaPago.InitializePersistance(super.appConfig, super.context);

				if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
					this.formaPago = formaPago;
				
				// ClienteInfo
				ClienteInfo clienteInfo = new ClienteInfo();
				clienteInfo.InitializePersistance(super.appConfig, super.context);

				if (clienteInfo.setClienteInfoByCliente(this))
					this.ClienteInfo = clienteInfo;

			    cursor.close();
				return true;
			} else {
				cursor.close();
				return false;
			}
			
		}
		
		return false ; 
	}
	
	public boolean hasGDPRSigned() throws Exception {
		
		boolean result;
		if (this.CodigoCliente == null || this.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE))
			return true;

		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(Constants.TABLE_GDPR, "CodigoCliente", this.CodigoCliente, true);
		result = (cursor != null);
		if (cursor != null) cursor.close();
				
		return result;
		
	}
	
	public void setGDPRSigned() {
		
		ContentValues values = new ContentValues();
		
		values.put("CodigoCliente", this.CodigoCliente);
		
		try {
			 super.getDatabaseOperations().insert(Constants.TABLE_GDPR, null , values);
			
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	              

}
