package net.ifeu.edicards.DataTier;

import java.util.Date;
import java.util.LinkedList;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.edicards.Pdf.incident.IncidentPdfCreator;

import android.content.ContentValues;
import android.database.Cursor;

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
	public String CodigoTarifa;
	public ClienteInfo ClienteInfo;
	public FormaPago FormaPago;
	public Double Latitud;
	public Double Longitud;
	public Date FechaGeocodificacion;
	public Long IdZona;
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


	@Override
	public void InitializePersistance(AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
		this.ClienteInfo = Factory.build(ClienteInfo.class, appConfig);
		this.FormaPago = Factory.build(FormaPago.class, appConfig);
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
		values.put("IdFormaPago", this.FormaPago.IdFormaPago);
		values.put("CodigoTarifa", this.CodigoTarifa);
		values.put("Latitud", this.Latitud);
		values.put("Longitud", this.Longitud);
		if (this.FechaGeocodificacion != null) {
			values.put("FechaGeocodificacion", this.FechaGeocodificacion.getTime());
		}
		if (this.IdZona != null) {
			values.put("IdZona", this.IdZona);
		}

		try {
			this.IdCliente = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_CLIENTES, null , values);

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
		values.put("IdFormaPago", this.FormaPago.IdFormaPago);
		values.put("CodigoTarifa", this.CodigoTarifa);
		values.put("Latitud", this.Latitud);
		values.put("Longitud", this.Longitud);
		if (this.FechaGeocodificacion != null) {
			values.put("FechaGeocodificacion", this.FechaGeocodificacion.getTime());
		}
		if (this.IdZona != null) {
			values.put("IdZona", this.IdZona);
		}

		String[] whereArgs = { String.valueOf(this.IdCliente) }; 
		
		super.getDatabaseOperations().update(ConstantsDatabase.TABLE_CLIENTES, values, "IdCliente = ?", whereArgs);

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
		    
			case ConstantsTypes.CUSTOMER_FILTER_NAME :list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente","Nombre",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case ConstantsTypes.CUSTOMER_FILTER_NIF:list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", "NIF",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case ConstantsTypes.CUSTOMER_FILTER_PHONE:list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", "Telefono1",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case ConstantsTypes.CUSTOMER_FILTER_CITY:list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", "Poblacion", text, onlyStartsWith, "AND Activo = 1", "Nombre");break;
		}
		
		for (String value: list)
		{
			Cliente cliente = Factory.build(Cliente.class, appConfig);

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
		    
			case ConstantsTypes.CUSTOMER_FILTER_NAME :list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente","Nombre",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case ConstantsTypes.CUSTOMER_FILTER_NIF:list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", "NIF",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case ConstantsTypes.CUSTOMER_FILTER_PHONE:list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", "Telefono1",text,onlyStartsWith,"AND Activo = 1","Nombre");break;
			case ConstantsTypes.CUSTOMER_FILTER_CITY:list = super.getDatabaseOperations().getStringArrayByField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", "Poblacion", text, onlyStartsWith, "AND Activo = 1", "Nombre");break;
		}
		
		for (String value: list)
		{
			Cliente cliente = Factory.build(Cliente.class, appConfig);
			
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
		return super.getDatabaseOperations().getRecordsCount(ConstantsDatabase.TABLE_CLIENTES);
	}
	
	public boolean setClienteByName(String name) throws Exception
	{
		
		name = name.replace("'", "''");
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_CLIENTES, "Nombre", name, true);
		
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
				FormaPago formaPago = Factory.build(FormaPago.class, appConfig);

				if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
					this.FormaPago = formaPago;
				
				ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

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
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_CLIENTES, "IdCliente", IdCliente, false);
		
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
				FormaPago formaPago = Factory.build(FormaPago.class, appConfig);

				if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
					this.FormaPago = formaPago;
				
				// ClienteInfo
				ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

				if (clienteInfo.setClienteInfoByCliente(this))
					this.ClienteInfo = clienteInfo;

				// IdZona
				int zonaColumnIndex = cursor.getColumnIndex("IdZona");
				if (zonaColumnIndex != -1 && !cursor.isNull(zonaColumnIndex)) {
					this.IdZona = cursor.getLong(zonaColumnIndex);
				}

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
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", codigoCliente, true);
		
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
			FormaPago formaPago = Factory.build(FormaPago.class, appConfig);

			if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
				this.FormaPago = formaPago;
			
			// ClienteInfo
			ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

			if (clienteInfo.setClienteInfoByCliente(this))
				this.ClienteInfo = clienteInfo;

			// IdZona
			int zonaColumnIndex = cursor.getColumnIndex("IdZona");
			if (zonaColumnIndex != -1 && !cursor.isNull(zonaColumnIndex)) {
				this.IdZona = cursor.getLong(zonaColumnIndex);
			}

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
		
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_CLIENTES, "CodigoCliente", codigoCliente, true);
		
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
				FormaPago formaPago = Factory.build(FormaPago.class, appConfig);

				if (formaPago.setFormaPagoById(cursor.getString(cursor.getColumnIndex("IdFormaPago"))))
					this.FormaPago = formaPago;
				
				// ClienteInfo
				ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

				if (clienteInfo.setClienteInfoByCliente(this))
					this.ClienteInfo = clienteInfo;

				// IdZona
				int zonaColumnIndex = cursor.getColumnIndex("IdZona");
				if (zonaColumnIndex != -1 && !cursor.isNull(zonaColumnIndex)) {
					this.IdZona = cursor.getLong(zonaColumnIndex);
				}

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
		if (this.CodigoCliente == null || this.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE))
			return true;

		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(ConstantsDatabase.TABLE_GDPR, "CodigoCliente", this.CodigoCliente, true);
		result = (cursor != null);
		if (cursor != null) cursor.close();
				
		return result;
		
	}
	
	public void setGDPRSigned() {
		
		ContentValues values = new ContentValues();
		
		values.put("CodigoCliente", this.CodigoCliente);
		
		try {
			 super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_GDPR, null , values);
			
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	public void CheckIfNewFiliacion(String filiacion, String codigoCliente) throws Exception {
		try {
			Cliente cliente = Factory.build(Cliente.class, appConfig);
			boolean clienteLoaded = cliente.setClienteByCodigo(codigoCliente);

			if (!clienteLoaded) {
				return;
			}

			// Normalizar valores null a cadena vacía para comparación segura
			String filiacionActual = (cliente.Filiacion != null) ? cliente.Filiacion : "";
			String filiacionNueva = (filiacion != null) ? filiacion : "";

			if (!filiacionActual.equals(filiacionNueva)) {
				cliente.Filiacion = filiacionNueva;
				cliente.update();

				String text = "Datos de filiacion del cliente: " + ConstantsTypes.NEW_LINE + ConstantsTypes.NEW_LINE
						+ "CODIGO CLIENTE: " + cliente.CodigoCliente + ConstantsTypes.NEW_LINE + "NOMBRE DEL CLIENTE: "
						+ cliente.Nombre + ConstantsTypes.NEW_LINE + "CODIGO FILIACION ANTERIOR: " + filiacionActual
						+ ConstantsTypes.NEW_LINE + "CODIGO FILIACION NUEVA: " + filiacionNueva
						+ ConstantsTypes.NEW_LINE;

				try {
					Incidencia incidencia = new Incidencia(appConfig.getUser().User, new Date(), IncidenciaType.Filiacion,
							text);
					incidencia.create(new IncidentPdfCreator(appConfig));
				} catch (Exception pdfException) {
					throw pdfException;
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Obtiene todos los clientes activos de la base de datos
	 * @return ArrayList de clientes
	 * @throws Exception Si hay error en la BD
	 */
	public java.util.ArrayList<Cliente> getAllClientes() throws Exception {
		java.util.ArrayList<Cliente> clientes = new java.util.ArrayList<>();

		Cursor cursor = super.getDatabaseOperations().executeSentence(
			"SELECT * FROM " + ConstantsDatabase.TABLE_CLIENTES + " WHERE Activo = '1' ORDER BY Nombre ASC");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {
				do {
					Cliente cliente = Factory.build(Cliente.class, appConfig);

					cliente.IdCliente = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdCliente")));
					cliente.Activo = cursor.getString(cursor.getColumnIndex("Activo")).equals("1");
					cliente.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
					cliente.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
					cliente.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
					cliente.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
					cliente.Direccion1 = cursor.getString(cursor.getColumnIndex("Direccion1"));
					cliente.Direccion2 = cursor.getString(cursor.getColumnIndex("Direccion2"));
					cliente.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));
					cliente.Poblacion = cursor.getString(cursor.getColumnIndex("Poblacion"));
					cliente.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));
					cliente.Telefono1 = cursor.getString(cursor.getColumnIndex("Telefono1"));
					cliente.Telefono2 = cursor.getString(cursor.getColumnIndex("Telefono2"));

					// IdZona (puede ser null)
					int idZonaIndex = cursor.getColumnIndex("IdZona");
					if (idZonaIndex >= 0 && !cursor.isNull(idZonaIndex)) {
						cliente.IdZona = cursor.getLong(idZonaIndex);
					}

					clientes.add(cliente);
				} while (cursor.moveToNext());
			}

			cursor.close();
		}

		return clientes;
	}

	/**
	 * Obtiene el total de clientes en la base de datos
	 * @return Número total de clientes
	 */
	public int getTotalClientes() {
		int total = 0;
		Cursor cursor = null;
		try {
			String query = "SELECT COUNT(*) as total FROM Clientes";
			cursor = super.getDatabaseOperations().getDatabase().rawQuery(query, null);
			if (cursor != null && cursor.moveToFirst()) {
				total = cursor.getInt(0);
			}
		} catch (Exception e) {
			android.util.Log.e("Cliente", "Error al obtener total de clientes", e);
		} finally {
			if (cursor != null) cursor.close();
		}
		return total;
	}

	/**
	 * Obtiene el número de clientes que tienen coordenadas válidas
	 * @return Número de clientes geocodificados
	 */
	public int getClientesGeocodificados() {
		int total = 0;
		Cursor cursor = null;
		try {
			String query = "SELECT COUNT(*) as total FROM Clientes " +
			              "WHERE Latitud IS NOT NULL AND Longitud IS NOT NULL " +
			              "AND Latitud != 0 AND Longitud != 0 " +
			              "AND Latitud BETWEEN -90 AND 90 " +
			              "AND Longitud BETWEEN -180 AND 180";
			cursor = super.getDatabaseOperations().getDatabase().rawQuery(query, null);
			if (cursor != null && cursor.moveToFirst()) {
				total = cursor.getInt(0);
			}
		} catch (Exception e) {
			android.util.Log.e("Cliente", "Error al obtener clientes geocodificados", e);
		} finally {
			if (cursor != null) cursor.close();
		}
		return total;
	}

	/**
	 * Obtiene la lista de clientes pendientes de geocodificar
	 * @return ArrayList de clientes sin coordenadas válidas
	 */
	public java.util.ArrayList<Cliente> getClientesPendientesGeocodificacion() {
		java.util.ArrayList<Cliente> clientes = new java.util.ArrayList<>();
		Cursor cursor = null;

		try {
			String query = "SELECT * FROM Clientes " +
			              "WHERE Latitud IS NULL OR Longitud IS NULL " +
			              "OR Latitud = 0 OR Longitud = 0 " +
			              "OR Latitud < -90 OR Latitud > 90 " +
			              "OR Longitud < -180 OR Longitud > 180 " +
			              "ORDER BY Nombre";

			cursor = super.getDatabaseOperations().getDatabase().rawQuery(query, null);

			if (cursor != null && cursor.moveToFirst()) {
				do {
					Cliente cliente = new Cliente();
					cliente.IdCliente = cursor.getLong(cursor.getColumnIndex("IdCliente"));
					cliente.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
					cliente.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
					cliente.Direccion1 = cursor.getString(cursor.getColumnIndex("Direccion1"));
					cliente.Poblacion = cursor.getString(cursor.getColumnIndex("Poblacion"));
					cliente.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));
					cliente.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));

					int latitudIndex = cursor.getColumnIndex("Latitud");
					if (latitudIndex >= 0 && !cursor.isNull(latitudIndex)) {
						cliente.Latitud = cursor.getDouble(latitudIndex);
					}

					int longitudIndex = cursor.getColumnIndex("Longitud");
					if (longitudIndex >= 0 && !cursor.isNull(longitudIndex)) {
						cliente.Longitud = cursor.getDouble(longitudIndex);
					}

					clientes.add(cliente);
				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			android.util.Log.e("Cliente", "Error al obtener clientes pendientes", e);
		} finally {
			if (cursor != null) cursor.close();
		}

		return clientes;
	}


}
