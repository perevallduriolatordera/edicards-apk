package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Totales.Base;
import net.ifeu.library.LogBook.LogBook;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class Deposito extends Cliente implements IPersistable {

	public Long IdDeposito;
	public Date FechaDeposito;
	public String Ejercicio;
	public Cliente Cliente;
	public LinkedHashMap<String, LineaDeposito> Lineas = new LinkedHashMap<>();
	public Totales Totales = new Totales();
	public Totales TotalesDeposito = new Totales();
	public String Serie;
	public String NumeroAlbaran;
	public float DescuentoComercial = new Float(0);
	public float DescuentoFinanciero = new Float(0);
	public String PagoDescripcion;
	public boolean Pagado;
	public double CantidadPagada;
	public String NumDoc;
	public String TipoDeposito;
	public boolean Retirado;
	public boolean DatosFiscalesUpdated;
	public String MotivoRetirado = ConstantsTypes.EMPTY_STRING;
	public boolean CCCUpdated;
	public boolean IsNtvDeposit;

	@Override
	public void InitializePersistance(AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
		this.ClienteInfo = Factory.build(ClienteInfo.class, appConfig);
		this.Cliente = Factory.build(Cliente.class, appConfig);
	}
	@Override
	public void save() throws Exception {

		ContentValues values = new ContentValues();
		values.put("IdCliente", this.IdCliente);
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("Nombre", this.Nombre);
		values.put("NIF", this.NIF);
		values.put("Razon", this.Razon);
		values.put("Direccion1", this.Direccion1);
		values.put("Direccion2", this.Direccion2);
		values.put("CodigoPostal", this.CodigoPostal);
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
		values.put("NumDoc", this.NumDoc);
		values.put("TipoDeposito", this.TipoDeposito);

		if (this.FechaDeposito == null)
			this.FechaDeposito = new Date();

		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
		values.put("FechaDeposito", formatter.format(this.FechaDeposito));

		if (this.Ejercicio == null) {
			int year = Calendar.getInstance().get(Calendar.YEAR);
			values.put("Ejercicio", String.valueOf(year).substring(2));
			this.Ejercicio = String.valueOf(year).substring(2);
		} else {
			values.put("Ejercicio", this.Ejercicio);
		}

		try {
			this.IdDeposito = super.getDatabaseOperations().insert(
					ConstantsDatabase.TABLE_DEPOSITOS, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();

		values.put("IdDeposito", this.IdDeposito);
		values.put("IdCliente", this.IdCliente);
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("Nombre", this.Nombre);
		values.put("NIF", this.NIF);
		values.put("Razon", this.Razon);
		values.put("Direccion1", this.Direccion1);
		values.put("Direccion2", this.Direccion2);
		values.put("CodigoPostal", this.CodigoPostal);
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
		values.put("NumDoc", this.NumDoc);
		values.put("TipoDeposito", this.TipoDeposito);

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		values.put("FechaDeposito", formatter.format(new Date()));

		if (this.Ejercicio == null) {
			int year = Calendar.getInstance().get(Calendar.YEAR);
			values.put("Ejercicio", String.valueOf(year).substring(2));
			this.Ejercicio = String.valueOf(year).substring(2);
		} else {
			values.put("Ejercicio", this.Ejercicio);
		}

		String[] whereArgs = { String.valueOf(this.IdDeposito) };

		super.getDatabaseOperations().update(ConstantsDatabase.TABLE_DEPOSITOS, values,
				"IdDeposito = ?", whereArgs);
	}

	@Override
	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				ConstantsDatabase.TABLE_DEPOSITOS);
	}

	public boolean setDepositoById(String IdDeposito) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
				ConstantsDatabase.TABLE_DEPOSITOS, "IdDeposito", IdDeposito, true);

		if (cursor != null) {

			this.IdDeposito = Long.parseLong(cursor.getString(cursor
					.getColumnIndex("IdDeposito")));
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			this.FechaDeposito = formatter.parse(cursor.getString(cursor
					.getColumnIndex("FechaDeposito")));
			this.Ejercicio = cursor.getString(cursor
					.getColumnIndex("Ejercicio"));
			this.Clave = cursor.getString(cursor.getColumnIndex("Clave"));
			this.CodigoCliente = cursor.getString(cursor
					.getColumnIndex("CodigoCliente"));
			this.CodigoPostal = cursor.getString(cursor
					.getColumnIndex("CodigoPostal"));
			this.Descuento1 = Double.parseDouble(cursor.getString(cursor
					.getColumnIndex("Descuento1")));
			this.Descuento2 = Double.parseDouble(cursor.getString(cursor
					.getColumnIndex("Descuento2")));
			this.Direccion1 = cursor.getString(cursor
					.getColumnIndex("Direccion1"));
			this.Direccion2 = cursor.getString(cursor
					.getColumnIndex("Direccion2"));
			this.Fax = cursor.getString(cursor.getColumnIndex("Fax"));
			this.IdCliente = Long.parseLong(cursor.getString(cursor
					.getColumnIndex("IdCliente")));
			this.Mail = cursor.getString(cursor.getColumnIndex("Mail"));
			this.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
			this.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
			this.Poblacion = cursor.getString(cursor
					.getColumnIndex("Poblacion"));
			this.Provincia = cursor.getString(cursor
					.getColumnIndex("Provincia"));
			this.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
			this.Telefono1 = cursor.getString(cursor
					.getColumnIndex("Telefono1"));
			this.Telefono2 = cursor.getString(cursor
					.getColumnIndex("Telefono2"));
			this.Web = cursor.getString(cursor.getColumnIndex("Web"));
			this.NumDoc = cursor.getString(cursor.getColumnIndex("NumDoc"));
			this.TipoDeposito = cursor.getString(cursor
					.getColumnIndex("TipoDeposito"));

			Cliente cliente = Factory.build(Cliente.getClass(), appConfig);

			if (cliente.setClienteById(cursor.getString(cursor
					.getColumnIndex("IdCliente"))))
				this.Cliente = cliente;
			
			ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

			if (clienteInfo.setClienteInfoByCliente(cliente))
				this.Cliente.ClienteInfo = clienteInfo;
			
			LineaDeposito linea = Factory.build(LineaDeposito.class, appConfig);

			this.Lineas = linea
					.getLineasDepositosByDeposito(this);

			cursor.close();

		}

		return false;

	}

	public boolean setFirstDepositoByCliente(String codigoCliente) throws Exception {

		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
				ConstantsDatabase.TABLE_DEPOSITOS, "CodigoCliente", codigoCliente, true);

		if (cursor != null) {

			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				this.IdDeposito = Long.parseLong(cursor.getString(cursor
						.getColumnIndex("IdDeposito")));
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				this.FechaDeposito = formatter.parse(cursor.getString(cursor
						.getColumnIndex("FechaDeposito")));
				this.Ejercicio = (cursor.getString(cursor
						.getColumnIndex("Ejercicio")));
				this.Clave = cursor.getString(cursor.getColumnIndex("Clave"));
				this.CodigoCliente = cursor.getString(cursor
						.getColumnIndex("CodigoCliente"));
				this.CodigoPostal = cursor.getString(cursor
						.getColumnIndex("CodigoPostal"));
				this.Descuento1 = Double.parseDouble(cursor.getString(cursor
						.getColumnIndex("Descuento1")));
				this.Descuento2 = Double.parseDouble(cursor.getString(cursor
						.getColumnIndex("Descuento2")));
				this.Direccion1 = cursor.getString(cursor
						.getColumnIndex("Direccion1"));
				this.Direccion2 = cursor.getString(cursor
						.getColumnIndex("Direccion2"));
				this.Fax = cursor.getString(cursor.getColumnIndex("Fax"));
				this.IdCliente = Long.parseLong(cursor.getString(cursor
						.getColumnIndex("IdCliente")));
				this.Mail = cursor.getString(cursor.getColumnIndex("Mail"));
				this.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
				this.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
				this.Poblacion = cursor.getString(cursor
						.getColumnIndex("Poblacion"));
				this.Provincia = cursor.getString(cursor
						.getColumnIndex("Provincia"));
				this.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
				this.Telefono1 = cursor.getString(cursor
						.getColumnIndex("Telefono1"));
				this.Telefono2 = cursor.getString(cursor
						.getColumnIndex("Telefono2"));
				this.Web = cursor.getString(cursor.getColumnIndex("Web"));
				this.NumDoc = cursor.getString(cursor.getColumnIndex("NumDoc"));
				this.TipoDeposito = cursor.getString(cursor
						.getColumnIndex("TipoDeposito"));

				Cliente cliente = Factory.build(Cliente.class, appConfig);

				if (cliente.setClienteById(cursor.getString(cursor
						.getColumnIndex("IdCliente"))))
					this.Cliente = cliente;
				
				ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

				if (clienteInfo.setClienteInfoByCliente(cliente))
					this.Cliente.ClienteInfo = clienteInfo;
				
				LineaDeposito linea = Factory.build(LineaDeposito.class, appConfig);

				this.Lineas = linea
						.getLineasDepositosByDeposito(this);

				cursor.close();
				return true;
			}
		}

		return false;

	}
	
	public ArrayList<Deposito> getDepositosByCliente(String idCliente)
			throws Exception {
		ArrayList<Deposito> list = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations()
				.getRecordsFromFieldNumeric(ConstantsDatabase.TABLE_DEPOSITOS,
						"IdCliente", idCliente, ConstantsTypes.EMPTY_STRING, null);

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				do {
					Deposito deposito = Factory.build(Deposito.class, appConfig);

					deposito.IdDeposito = Long.parseLong(cursor
							.getString(cursor.getColumnIndex("IdDeposito")));
					SimpleDateFormat formatter = new SimpleDateFormat(
							"dd/MM/yyyy"); // please notice the capital M
					deposito.FechaDeposito = formatter.parse(cursor
							.getString(cursor.getColumnIndex("FechaDeposito")));
					deposito.Ejercicio = cursor.getString(cursor
							.getColumnIndex("Ejercicio"));
					deposito.Clave = cursor.getString(cursor
							.getColumnIndex("Clave"));
					deposito.CodigoCliente = cursor.getString(cursor
							.getColumnIndex("CodigoCliente"));
					deposito.CodigoPostal = cursor.getString(cursor
							.getColumnIndex("CodigoPostal"));
					deposito.Descuento1 = Double.parseDouble(cursor
							.getString(cursor.getColumnIndex("Descuento1")));
					deposito.Descuento2 = Double.parseDouble(cursor
							.getString(cursor.getColumnIndex("Descuento2")));
					deposito.Direccion1 = cursor.getString(cursor
							.getColumnIndex("Direccion1"));
					deposito.Direccion2 = cursor.getString(cursor
							.getColumnIndex("Direccion2"));
					deposito.Fax = cursor.getString(cursor
							.getColumnIndex("Fax"));
					deposito.IdCliente = Integer.parseInt(cursor
							.getString(cursor.getColumnIndex("IdCliente")));
					deposito.Mail = cursor.getString(cursor
							.getColumnIndex("Mail"));
					deposito.NIF = cursor.getString(cursor
							.getColumnIndex("NIF"));
					deposito.Nombre = cursor.getString(cursor
							.getColumnIndex("Nombre"));
					deposito.Poblacion = cursor.getString(cursor
							.getColumnIndex("Poblacion"));
					deposito.Provincia = cursor.getString(cursor
							.getColumnIndex("Provincia"));
					deposito.Razon = cursor.getString(cursor
							.getColumnIndex("Razon"));
					deposito.Telefono1 = cursor.getString(cursor
							.getColumnIndex("Telefono1"));
					deposito.Telefono2 = cursor.getString(cursor
							.getColumnIndex("Telefono2"));
					deposito.Web = cursor.getString(cursor
							.getColumnIndex("Web"));
					deposito.NumDoc = cursor.getString(cursor
							.getColumnIndex("NumDoc"));
					deposito.TipoDeposito = cursor.getString(cursor
							.getColumnIndex("TipoDeposito"));

					Cliente cliente = Factory.build(Cliente.class, appConfig);

					if (cliente.setClienteById(cursor.getString(cursor
							.getColumnIndex("IdCliente"))))
						deposito.Cliente = cliente;
					
					ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

					if (clienteInfo.setClienteInfoByCliente(cliente))
						this.Cliente.ClienteInfo = clienteInfo;

					LineaDeposito linea = Factory.build(LineaDeposito.class, appConfig);

					deposito.Lineas = linea
							.getLineasDepositosByDeposito(deposito);
					list.add(deposito);

				} while (cursor.moveToNext());

				cursor.close();
				return list;
			} else
				return list;
		} else
			return list;

	}
	
	public ArrayList<Deposito> getDepositosByCodigoCliente(String codigoCliente)
			throws Exception {
		ArrayList<Deposito> list = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations()
				.getRecordsFromField(ConstantsDatabase.TABLE_DEPOSITOS,
						"CodigoCliente", codigoCliente, true, ConstantsTypes.EMPTY_STRING, ConstantsTypes.EMPTY_STRING);
		

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				do {
					Deposito deposito = Factory.build(Deposito.class, appConfig);

					deposito.IdDeposito = Long.parseLong(cursor
							.getString(cursor.getColumnIndex("IdDeposito")));
					SimpleDateFormat formatter = new SimpleDateFormat(
							"dd/MM/yyyy"); // please notice the capital M
					deposito.FechaDeposito = formatter.parse(cursor
							.getString(cursor.getColumnIndex("FechaDeposito")));
					deposito.Ejercicio = cursor.getString(cursor
							.getColumnIndex("Ejercicio"));
					deposito.Clave = cursor.getString(cursor
							.getColumnIndex("Clave"));
					deposito.CodigoCliente = cursor.getString(cursor
							.getColumnIndex("CodigoCliente"));
					deposito.CodigoPostal = cursor.getString(cursor
							.getColumnIndex("CodigoPostal"));
					deposito.Descuento1 = Double.parseDouble(cursor
							.getString(cursor.getColumnIndex("Descuento1")));
					deposito.Descuento2 = Double.parseDouble(cursor
							.getString(cursor.getColumnIndex("Descuento2")));
					deposito.Direccion1 = cursor.getString(cursor
							.getColumnIndex("Direccion1"));
					deposito.Direccion2 = cursor.getString(cursor
							.getColumnIndex("Direccion2"));
					deposito.Fax = cursor.getString(cursor
							.getColumnIndex("Fax"));
					deposito.IdCliente = Integer.parseInt(cursor
							.getString(cursor.getColumnIndex("IdCliente")));
					deposito.Mail = cursor.getString(cursor
							.getColumnIndex("Mail"));
					deposito.NIF = cursor.getString(cursor
							.getColumnIndex("NIF"));
					deposito.Nombre = cursor.getString(cursor
							.getColumnIndex("Nombre"));
					deposito.Poblacion = cursor.getString(cursor
							.getColumnIndex("Poblacion"));
					deposito.Provincia = cursor.getString(cursor
							.getColumnIndex("Provincia"));
					deposito.Razon = cursor.getString(cursor
							.getColumnIndex("Razon"));
					deposito.Telefono1 = cursor.getString(cursor
							.getColumnIndex("Telefono1"));
					deposito.Telefono2 = cursor.getString(cursor
							.getColumnIndex("Telefono2"));
					deposito.Web = cursor.getString(cursor
							.getColumnIndex("Web"));
					deposito.NumDoc = cursor.getString(cursor
							.getColumnIndex("NumDoc"));
					deposito.TipoDeposito = cursor.getString(cursor
							.getColumnIndex("TipoDeposito"));

					Cliente cliente = Factory.build(Cliente.class, appConfig);

					if (cliente.setClienteById(cursor.getString(cursor
							.getColumnIndex("IdCliente"))))
						deposito.Cliente = cliente;
					
					ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

					if (clienteInfo.setClienteInfoByCliente(cliente))
						this.Cliente.ClienteInfo = clienteInfo;

					LineaDeposito linea = Factory.build(LineaDeposito.class, appConfig);

					deposito.Lineas = linea
							.getLineasDepositosByDeposito(deposito);
					list.add(deposito);

				} while (cursor.moveToNext());

				cursor.close();
				return list;
			} else
				return list;
		} else
			return list;

	}
	
	public ArrayList<Deposito> getDepositosToday()
			throws Exception {
		ArrayList<Deposito> list = new ArrayList<>();

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		String date = formatter.format(new Date());
		
		Cursor cursor = super.getDatabaseOperations()
				.getRecordsFromField(ConstantsDatabase.TABLE_DEPOSITOS,
						"FechaDeposito", date, true, ConstantsTypes.EMPTY_STRING, ConstantsTypes.EMPTY_STRING);

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				do {
					Deposito deposito = Factory.build(Deposito.class, appConfig);

					deposito.IdDeposito = Long.parseLong(cursor
							.getString(cursor.getColumnIndex("IdDeposito")));
					deposito.FechaDeposito = formatter.parse(cursor
							.getString(cursor.getColumnIndex("FechaDeposito")));
					deposito.Ejercicio = cursor.getString(cursor
							.getColumnIndex("Ejercicio"));
					deposito.Clave = cursor.getString(cursor
							.getColumnIndex("Clave"));
					deposito.CodigoCliente = cursor.getString(cursor
							.getColumnIndex("CodigoCliente"));
					deposito.CodigoPostal = cursor.getString(cursor
							.getColumnIndex("CodigoPostal"));
					deposito.Descuento1 = Double.parseDouble(cursor
							.getString(cursor.getColumnIndex("Descuento1")));
					deposito.Descuento2 = Double.parseDouble(cursor
							.getString(cursor.getColumnIndex("Descuento2")));
					deposito.Direccion1 = cursor.getString(cursor
							.getColumnIndex("Direccion1"));
					deposito.Direccion2 = cursor.getString(cursor
							.getColumnIndex("Direccion2"));
					deposito.Fax = cursor.getString(cursor
							.getColumnIndex("Fax"));
					deposito.IdCliente = Integer.parseInt(cursor
							.getString(cursor.getColumnIndex("IdCliente")));
					deposito.Mail = cursor.getString(cursor
							.getColumnIndex("Mail"));
					deposito.NIF = cursor.getString(cursor
							.getColumnIndex("NIF"));
					deposito.Nombre = cursor.getString(cursor
							.getColumnIndex("Nombre"));
					deposito.Poblacion = cursor.getString(cursor
							.getColumnIndex("Poblacion"));
					deposito.Provincia = cursor.getString(cursor
							.getColumnIndex("Provincia"));
					deposito.Razon = cursor.getString(cursor
							.getColumnIndex("Razon"));
					deposito.Telefono1 = cursor.getString(cursor
							.getColumnIndex("Telefono1"));
					deposito.Telefono2 = cursor.getString(cursor
							.getColumnIndex("Telefono2"));
					deposito.Web = cursor.getString(cursor
							.getColumnIndex("Web"));
					deposito.NumDoc = cursor.getString(cursor
							.getColumnIndex("NumDoc"));
					deposito.TipoDeposito = cursor.getString(cursor
							.getColumnIndex("TipoDeposito"));

					Cliente cliente = Factory.build(Cliente.class, appConfig);

					if (cliente.setClienteById(cursor.getString(cursor
							.getColumnIndex("IdCliente"))))
						deposito.Cliente = cliente;
					
					ClienteInfo clienteInfo = Factory.build(ClienteInfo.class, appConfig);

					if (clienteInfo.setClienteInfoByCliente(cliente))
						this.Cliente.ClienteInfo = clienteInfo;

					LineaDeposito linea = Factory.build(LineaDeposito.class, appConfig);

					deposito.Lineas = linea
							.getLineasDepositosByDeposito(deposito);
					list.add(deposito);

				} while (cursor.moveToNext());

				cursor.close();
				return list;
			} else
				return list;
		} else
			return list;

	}
	
	public void Calculate() throws Exception {
		Totales = new Totales();

		for (LineaDeposito linea : this.Lineas.values()) {

			if (linea.UnidadesFacturadas > 0 || linea.UnidadesAbono > 0) {
				// Calculamos el precio bruto inicial
				double bruto = round(linea.UnidadesFacturadas * linea.PVP
						+ linea.TotalAbono,3);
				
				double neto;

				if (linea.Descuento1 != 0)
					neto = round(bruto - (bruto * (linea.Descuento1 / 100)),3);
				else
					neto = bruto;

				@SuppressWarnings("unused")
				double dte = round(bruto - neto,3);

				TipoIVA iva = Factory.build(TipoIVA.class, appConfig);

				if (iva.setTipoIVAByClienteArticulo(this.Cliente,
						linea.Articulo)
						&& !super.appConfig.getUser().SerialInvoiceB
								.equals(this.Serie)) {

				
					double baseSinDte = neto;
					double base = bruto; //round(neto - descuento1 - descuento2,3);
					base = round(base, 3);
					baseSinDte = round(baseSinDte, 3);

					Base bases;

					if (this.Totales.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.Totales.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.BaseSinDte = round(bases.BaseSinDte + baseSinDte,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
					} else {
						bases = this.Totales.new Base();
						bases.Base = base;
						bases.BaseSinDte = baseSinDte;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.Totales.Bases.put(String.valueOf(iva.Impuesto),
								bases);
					}


				} else {
					double base = bruto;
					double baseSinDte = neto;
				
					Base bases;

					if (this.Totales.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.Totales.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.BaseSinDte = round(bases.BaseSinDte + baseSinDte,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
					} else {
						bases = this.Totales.new Base();
						bases.Base = base;
						bases.BaseSinDte = baseSinDte;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.Totales.Bases.put(String.valueOf(iva.Impuesto),
								bases);
					}

				}
			}
		}
		
		this.CalculateTotals(this.Totales);

	}

	public void CalculateDeposito() throws Exception {
		TotalesDeposito = new Totales();

		for (LineaDeposito linea : this.Lineas.values()) {

			if (linea.UnidadesRepuestas > 0) {
				// Calculamos el precio bruto inicial
				double bruto = round(linea.UnidadesRepuestas * linea.PVP,3);

				double neto;

				if (linea.Descuento1 != 0)
					neto = round(bruto - (bruto * (linea.Descuento1 / 100)),3);
				else
					neto = bruto;

				@SuppressWarnings("unused")
				double dte = round(bruto - neto,3);

				TipoIVA iva = Factory.build(TipoIVA.class, appConfig);

				if (iva.setTipoIVAByClienteArticulo(this.Cliente,
						linea.Articulo)
						&& !super.appConfig.getUser().SerialInvoiceB
								.equals(this.Serie)) {

					double base = bruto;

					Base bases;

					if (this.TotalesDeposito.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.TotalesDeposito.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
	
					} else {
						bases = this.TotalesDeposito.new Base();
						bases.Base = base;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.TotalesDeposito.Bases.put(
								String.valueOf(iva.Impuesto), bases);
					}

				} else {
					double base = bruto;

					Base bases;

					if (this.TotalesDeposito.Bases.containsKey(String
							.valueOf(iva.Impuesto))) {
						bases = this.TotalesDeposito.Bases.get(String
								.valueOf(iva.Impuesto));
						bases.Base = round(bases.Base + base,3);
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;
					} else {
						bases = this.TotalesDeposito.new Base();
						bases.Base = base;
						bases.IvaPerc = iva.Impuesto;
						bases.RecargoPerc = iva.Recargo;

						this.TotalesDeposito.Bases.put(
								String.valueOf(iva.Impuesto), bases);
					}

				}
			}
		}
		
		this.CalculateTotals(this.TotalesDeposito);

	}
	
	private void CalculateTotals(Totales totales)
	{
		for (Entry<String, Base> entry : totales.Bases.entrySet())
		{
			Base base = entry.getValue();
			
			double descuento1 = round(base.Base * (this.DescuentoComercial / 100),5);
			double descuento2 = round((base.Base - descuento1)
					* (this.DescuentoFinanciero / 100),5);
			double baseTipo = round(base.Base - descuento1 - descuento2,5);
			double totalIVA = round((baseTipo * base.IvaPerc) / 100,5);
			double totalRecargo = round((baseTipo * base.RecargoPerc) / 100,5);
			double total = round(baseTipo,2) + round(totalIVA,2) + round(totalRecargo,2);
			
			base.Descuento = round(descuento1 + descuento2 , 5);
			base.Iva = round(totalIVA,5);
			base.Recargo = round(totalRecargo,5);
			base.Total = round(total,2);
			
			totales.TotalBaseSinDte = round(totales.TotalBaseSinDte + base.BaseSinDte,5);
			totales.TotalBase = round(totales.TotalBase + baseTipo,5);
			totales.DescuentoFinanciero = this.DescuentoFinanciero;
			totales.DescuentoProntoPago = this.DescuentoComercial;
			totales.TotalDescuentoProntoPago = round(totales.TotalDescuentoProntoPago + descuento1,5);
			totales.TotalDescuentoFinanciero = round(totales.TotalDescuentoFinanciero + descuento2,5);
			
			totales.TotalIVA = round(totales.TotalIVA + totalIVA,5);
			totales.TotalRecargo = round(totales.TotalRecargo + totalRecargo,5);
			totales.Total = round(totales.Total + total,2);
			
		}
	}
	
	@Override
	public void clean() throws Exception {
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_DEPOSITOS);
		
		if (cursor != null)
			cursor.close();
		
		this.DeleteAllLinesDepositos();
	}
	
	private void DeleteAllLinesDepositos() throws Exception {
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO);
		
		if (cursor != null)
			cursor.close();
	}
	
	public void DeleteAllLines() throws Exception {
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO + " WHERE IdDeposito = "
						+ this.IdDeposito);
		
		if (cursor != null)
			cursor.close();

	}

	public void assingFromCliente(Cliente cliente) {
		
		this.Activo = cliente.Activo;
		this.CodigoCliente = cliente.CodigoCliente;
		this.Clave = cliente.Clave;
		this.CodigoPostal = cliente.CodigoPostal;
		this.Descuento1 = cliente.Descuento1;
		this.Descuento2 = cliente.Descuento2;
		this.Direccion1 = cliente.Direccion1;
		this.Direccion2 = cliente.Direccion2;
		this.Fax = cliente.Fax;
		this.IdCliente = cliente.IdCliente;
		this.Mail = cliente.Mail;
		this.NIF = cliente.NIF;
		this.Nombre = cliente.Nombre;
		this.Poblacion = cliente.Poblacion;
		this.Provincia = cliente.Provincia;
		this.Razon = cliente.Razon;
		this.Telefono1 = cliente.Telefono1;
		this.Telefono2 = cliente.Telefono2;
		this.Web = cliente.Web;
		this.FormaPago = cliente.FormaPago;
		this.Filiacion = cliente.Filiacion;
		this.ClienteInfo.Cliente = cliente;
		this.ClienteInfo.CCC = cliente.ClienteInfo.CCC;
		this.ClienteInfo.Representante = cliente.ClienteInfo.Representante;
		this.ClienteInfo.DniRepresentante = cliente.ClienteInfo.DniRepresentante;
		
		this.Cliente = cliente;

	}

	public void assingFromDeposito(Deposito deposito) {

		this.Activo = deposito.Activo;
		this.CodigoCliente = ConstantsTypes.EMPTY_STRING;
		this.IdCliente = 0;
		this.FechaDeposito = new Date();
		this.NIF = deposito.NIF;
		this.Razon = deposito.Razon;
		this.Direccion1 = deposito.Direccion1;
		this.Poblacion = deposito.Poblacion;
		this.Provincia = deposito.Provincia;
		this.CodigoPostal = deposito.CodigoPostal;
		this.Nombre = deposito.Nombre;

		this.Lineas = deposito.Lineas;
	}

	public boolean isAlbaran() {

		for (LineaDeposito linea : Lineas.values()) {
			if (linea.UnidadesFacturadas > 0 || linea.UnidadesAbono > 0)
				return true;
		}

		return false;
	}

	public boolean isDeposito() {

		for (LineaDeposito linea : Lineas.values()) {
			if (linea.UnidadesRepuestas > 0)
				return true;
		}

		return false;
	}

	public boolean isDepositoRetirado() {

		if (!isDeposito()) {
			for (LineaDeposito linea : Lineas.values()) {
				if (linea.UnidadesInicialesFijas > 0)
					return true;
			}
			
			if (Lineas.values().size() == 0) return true; 
		}

		return false;
	}
	
	public void RetirarDeposito() {
		for (LineaDeposito linea : Lineas.values()) {
			linea.UnidadesRepuestas=0;
		}
		
	}
	
	public Deposito CloneOnlyLineas()
	{
		Deposito deposito = Factory.build(Deposito.class, appConfig);
		
		for (LineaDeposito linea : Lineas.values()) {
			
			if (linea.UnidadesInicialesFijas > 0)
				deposito.Lineas.put(String.valueOf(linea.Articulo.CodigoArticulo), linea);
		}
		
		return deposito;
	}

	public boolean isDepositoUpdatedOnlyVentaDirecta() {

		boolean isOnlyVentaDirecta = false;

		for (LineaDeposito linea : Lineas.values()) {
			if (linea.UnidadesInicialesFijas != linea.UnidadesRepuestas)
				return false;

			if (linea.PVPAnterior != linea.PVPInicial && linea.UnidadesFacturadas > 0) {
				isOnlyVentaDirecta = true;
			}
		}

		return isOnlyVentaDirecta;

	}
	public boolean isDepositoUpdated() {
		for (LineaDeposito linea : Lineas.values()) {
			if ((linea.UnidadesInicialesFijas != linea.UnidadesRepuestas)
					|| (linea.PVPAnterior != linea.PVPInicial))
				return true;
		}

		return this.DatosFiscalesUpdated;

	}

	public boolean isDepositoConvencional() {
		return this.TipoDeposito.equals(ConstantsTypes.TIPO_DEPOSITO_CONVENCIONAL);
	}

	public DTODeposito getDTO() {
		DTODeposito dto = new DTODeposito();

		dto.Activo = this.Activo;
		dto.CantidadPagada = this.CantidadPagada;
		dto.Clave = this.Clave;
		dto.CodigoCliente = this.CodigoCliente;
		dto.CodigoPostal = this.CodigoPostal;
		dto.CodigoTarifa = this.CodigoTarifa;
		dto.Descuento1 = this.Descuento1;
		dto.Descuento2 = this.Descuento2;
		dto.DescuentoComercial = this.DescuentoComercial;
		dto.DescuentoFinanciero = this.DescuentoFinanciero;
		dto.DescuentoProntoPago = this.DescuentoProntoPago;
		dto.Direccion1 = this.Direccion1;
		dto.Direccion2 = this.Direccion2;
		dto.Ejercicio = this.Ejercicio;
		dto.Fax = this.Fax;
		dto.FechaDeposito = this.FechaDeposito;
		dto.Filiacion = this.Filiacion;
		dto.IdCliente = this.IdCliente;
		dto.IdDeposito = this.IdDeposito;
		dto.Mail = this.Mail;
		dto.NIF = this.NIF;
		dto.Nombre = this.Nombre;
		dto.NumDoc = this.NumDoc;
		dto.NumeroAlbaran = this.NumeroAlbaran;
		dto.Pagado = this.Pagado;
		dto.PagoDescripcion = this.PagoDescripcion;
		dto.Poblacion = this.Poblacion;
		dto.Provincia = this.Provincia;
		dto.Razon = this.Razon;
		dto.Serie = this.Serie;
		dto.Telefono1 = this.Telefono1;
		dto.Telefono2 = this.Telefono2;
		dto.TipoDeposito = this.TipoDeposito;
		dto.Web = this.Web;

		dto.Lineas.clear();

		for (LineaDeposito linea : this.Lineas.values()) {
			DTOLineaDeposito dtoLinea = new DTOLineaDeposito();

			dtoLinea.CodigoArticulo = linea.Articulo.CodigoArticulo;
			dtoLinea.DefectuosasAbono = linea.DefectuosasAbono;
			dtoLinea.Descripcion = linea.Articulo.Descripcion;
			dtoLinea.Familia = linea.Articulo.Familia;
			dtoLinea.Descuento1 = linea.Descuento1;
			dtoLinea.Descuento2 = linea.Descuento2;
			dtoLinea.IdArticulo = linea.IdArticulo;
			dtoLinea.IdDeposito = linea.IdDeposito;
			dtoLinea.IdLineaDeposito = linea.IdLineaDeposito;
			dtoLinea.IsNew = linea.IsNew;
			dtoLinea.IsVentaDirecta = linea.IsVentaDirecta;
			dtoLinea.PVP = linea.PVP;
			dtoLinea.PVPAbono = linea.PVPAbono;
			dtoLinea.PVPAnterior = linea.PVPAnterior;
			dtoLinea.PVPInicial = linea.PVPInicial;
			dtoLinea.StockInicial = linea.StockInicial;
			dtoLinea.TotalAbono = linea.TotalAbono;
			dtoLinea.UnidadesAbono = linea.UnidadesAbono;
			dtoLinea.UnidadesAnterior = linea.UnidadesAnterior;
			dtoLinea.UnidadesDefectuosas = linea.UnidadesDefectuosas;
			dtoLinea.UnidadesDevueltas = linea.UnidadesDevueltas;
			dtoLinea.UnidadesFacturadas = linea.UnidadesFacturadas;
			dtoLinea.UnidadesIniciales = linea.UnidadesIniciales;
			dtoLinea.UnidadesInicialesFijas = linea.UnidadesInicialesFijas;
			dtoLinea.UnidadesRepuestas = linea.UnidadesRepuestas;

			dto.Lineas.put(String.valueOf(dtoLinea.CodigoArticulo), dtoLinea);

		}

		return dto;

	}

	public void saveChangesToDeposito() {
		this.FechaDeposito = new Date();

		if (this.IdDeposito == null) {
			try {
				this.save();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else {
			try {
				this.DeleteAllLines();
				this.update();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		// Solo restamos stock, en el caso de que el deposito sea de tipo Furgoneta
		LogBook logBookTrace = Factory.build(LogBook.class, appConfig);

		for (LineaDeposito linea : this.Lineas.values()) {

			if (!this.isDepositoRetirado()) {

				if (linea.UnidadesRepuestas > 0) {
					try {
						linea.save();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

					if (appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Furgoneta) {
						linea.Articulo.Activo = true;

						if (linea.IsVentaDirecta) {
							int stockInicial = linea.Articulo.Stock;
							linea.Articulo.Stock = stockInicial + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
									- linea.UnidadesRepuestas
									- (linea.UnidadesFacturadas - (linea.UnidadesInicialesFijas - linea.UnidadesDevueltas));

							if (stockInicial != linea.Articulo.Stock) {
								logBookTrace.setData("VENTA DIRECTA CON UNIDADES REPUESTAS", this.Cliente.CodigoCliente,
										this.Cliente.Razon, linea.Articulo.CodigoArticulo, linea.Articulo.Descripcion,
										stockInicial, linea.Articulo.Stock, linea.UnidadesDevueltas, linea.UnidadesDefectuosas,
										linea.UnidadesRepuestas, linea.UnidadesFacturadas, linea.UnidadesInicialesFijas,
										linea.UnidadesAbono, linea.UnidadesDefectuosas);

								try {
									logBookTrace.save();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}

							}

							linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
									- linea.UnidadesDefectuosas - linea.UnidadesRepuestas
									- (linea.UnidadesFacturadas - linea.UnidadesInicialesFijas + linea.UnidadesDevueltas);

						} else {
							int stockInicial = linea.Articulo.Stock;
							linea.Articulo.Stock = stockInicial + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
									- linea.UnidadesRepuestas;

							if (stockInicial != linea.Articulo.Stock) {

								logBookTrace.setData("VENTA CONVENCIONAL (NO DIRECTA) CON UNIDADES REPUESTAS", this.Cliente.CodigoCliente,
										this.Cliente.Razon, linea.Articulo.CodigoArticulo, linea.Articulo.Descripcion,
										stockInicial, linea.Articulo.Stock, linea.UnidadesDevueltas, linea.UnidadesDefectuosas,
										linea.UnidadesRepuestas, linea.UnidadesFacturadas, linea.UnidadesInicialesFijas,
										linea.UnidadesAbono, linea.UnidadesDefectuosas);

								try {
									logBookTrace.save();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}

							linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
									- linea.UnidadesDefectuosas - linea.UnidadesRepuestas;
						}

						linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.UnidadesDefectuosas;

						linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
								+ linea.UnidadesDefectuosas;

						try {
							linea.Articulo.update();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				} else {

					if (appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Furgoneta) {

						linea.Articulo.Activo = true;

						if (linea.IsVentaDirecta) {
							int stockInicial = linea.Articulo.Stock;
							linea.Articulo.Stock = stockInicial - linea.UnidadesFacturadas;

							if (stockInicial != linea.Articulo.Stock) {

								logBookTrace.setData("VENTA DIRECTA SIN UNIDADES REPUESTAS", this.Cliente.CodigoCliente,
										this.Cliente.Razon, linea.Articulo.CodigoArticulo, linea.Articulo.Descripcion,
										stockInicial, linea.Articulo.Stock, linea.UnidadesDevueltas, linea.UnidadesDefectuosas,
										linea.UnidadesRepuestas, linea.UnidadesFacturadas, linea.UnidadesInicialesFijas,
										linea.UnidadesAbono, linea.UnidadesDefectuosas);

								try {
									logBookTrace.save();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}

							linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock - linea.UnidadesFacturadas;
						} else {
							int stockInicial = linea.Articulo.Stock;
							linea.Articulo.Stock = stockInicial + linea.UnidadesDevueltas - linea.UnidadesDefectuosas
									- linea.UnidadesRepuestas;


							if (stockInicial != linea.Articulo.Stock) {

								logBookTrace.setData("VENTA CONVENCIONAL (NO DIRECTA) CON UNIDADES REPUESTAS", this.Cliente.CodigoCliente,
										this.Cliente.Razon, linea.Articulo.CodigoArticulo, linea.Articulo.Descripcion,
										stockInicial, linea.Articulo.Stock, linea.UnidadesDevueltas, linea.UnidadesDefectuosas,
										linea.UnidadesRepuestas, linea.UnidadesFacturadas, linea.UnidadesInicialesFijas,
										linea.UnidadesAbono, linea.UnidadesDefectuosas);

								try {
									logBookTrace.save();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							}

							linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesDevueltas
									- linea.UnidadesDefectuosas - linea.UnidadesRepuestas;
						}

						linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.UnidadesDefectuosas;

						linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
								+ linea.UnidadesDefectuosas;

						try {
							linea.Articulo.update();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}

				if (linea.UnidadesAbono > 0 && appConfig.getWorkingArea().CurrentDepositoModalidad == DepositoModalidad.Furgoneta) {
					linea.Articulo.Activo = true;

					int stockInicial = linea.Articulo.Stock;
					linea.Articulo.Stock = stockInicial + linea.UnidadesAbono - linea.DefectuosasAbono;

					if (stockInicial != linea.Articulo.Stock) {

						logBookTrace.setData("VENTA ABONO", this.Cliente.CodigoCliente,
								this.Cliente.Razon, linea.Articulo.CodigoArticulo, linea.Articulo.Descripcion,
								stockInicial, linea.Articulo.Stock, linea.UnidadesDevueltas, linea.UnidadesDefectuosas,
								linea.UnidadesRepuestas, linea.UnidadesFacturadas, linea.UnidadesInicialesFijas,
								linea.UnidadesAbono, linea.UnidadesDefectuosas);

						try {
							logBookTrace.save();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}

					linea.Articulo.MovimientoStock = linea.Articulo.MovimientoStock + linea.UnidadesAbono
							- linea.DefectuosasAbono;

					linea.Articulo.StockDefectuoso = linea.Articulo.StockDefectuoso + linea.DefectuosasAbono;

					linea.Articulo.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas
							+ linea.Articulo.MovimientoStockDefectuosas + linea.DefectuosasAbono;

					try {
						linea.Articulo.update();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}

				}
			}
		}
	}

	private double round(double d, int decimalPlace) {
		BigDecimal bd = new BigDecimal(Double.toString(d));
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		return bd.doubleValue();
	}

}
