package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class TipoIVA extends Persistent implements IPersistable {

	public long IdTipoIVA;
	public String Filiacion;
	public String Articulo;
	public double Impuesto;
	public double Recargo;
	public Date Fecha;
	public String Descripcion;

	public void InitializePersistance(AppConfig appConfig, Context context)
			throws Exception {
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
		formatter = new SimpleDateFormat("dd/MM/yyyy");

		ContentValues values = new ContentValues();
		values.put("Filiacion", this.Filiacion);
		values.put("Articulo", this.Articulo);
		values.put("impuesto", this.Impuesto);
		values.put("Recargo", this.Recargo);
		values.put("Descripcion", this.Descripcion);
		values.put("Fecha", formatter.format(this.Fecha));

		try {
			this.IdTipoIVA = super.getDatabaseOperations().insert(
					Constants.TABLE_TIPOS_IVA, null, values);
		} catch (Exception e) {
			throw e;
		}

	}

	@Override
	public void clean() throws Exception {
		super.getDatabaseOperations().deleteAllRecords(
				Constants.TABLE_TIPOS_IVA);
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				Constants.TABLE_TIPOS_IVA);
	}

	public boolean setTipoIVAById(String idTipoIVA) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
				Constants.TABLE_TIPOS_IVA, "IdTipoIVA", idTipoIVA, false);

		if (cursor != null) {

			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("dd/MM/yyyy");

			this.IdTipoIVA = Long.parseLong(idTipoIVA);
			this.Filiacion = cursor.getString(cursor
					.getColumnIndex("Filiacion"));
			this.Articulo = cursor.getString(cursor.getColumnIndex("Articulo"));

			this.Fecha = (Date) formatter.parse(cursor.getString(cursor
					.getColumnIndex("Fecha")));
			this.Impuesto = Double.parseDouble(cursor.getString(cursor
					.getColumnIndex("Impuesto")));
			this.Descripcion = cursor.getString(cursor.getColumnIndex("Descripcion"));
			this.Recargo = Double.parseDouble(cursor.getString(cursor
					.getColumnIndex("Recargo")));

			cursor.close();
			return true;
		}

		return false; // (cursor != null);
	}

	public boolean setTipoIVAByClienteArticulo(Cliente cliente,
			Articulo articulo) throws Exception {

		// Cursor cursor =
		// super.getDatabaseOperations().getRecordsFromField(Constants.TABLE_TIPOS_IVA,
		// "Articulo", String.valueOf(articulo.TipoIVA), false,
		// "AND Filiacion = '" + cliente.Filiacion + "'",null);

		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM " + Constants.TABLE_TIPOS_IVA
						+ " WHERE Articulo='"
						+ String.valueOf(articulo.TipoIVA)
						+ "' AND Filiacion = '" + cliente.Filiacion + "'");

		boolean founded = false;

		if (cursor != null) {
			cursor.moveToFirst();

			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			Date last = (Date) formatter.parse("01/01/1900");

			if (cursor.getCount() > 0) {
				do {

					Date fecha = (Date) formatter.parse(cursor.getString(cursor
							.getColumnIndex("Fecha")));

					if (fecha.after(last)) {
						this.IdTipoIVA = Long.parseLong(cursor.getString(cursor
								.getColumnIndex("IdTipoIVA")));
						this.Articulo = cursor.getString(cursor
								.getColumnIndex("Articulo"));
						this.Filiacion = cursor.getString(cursor
								.getColumnIndex("Filiacion"));
						this.Fecha = fecha;
						this.Descripcion=cursor.getString(cursor.getColumnIndex("Descripcion"));
						this.Impuesto = Double.parseDouble(cursor
								.getString(cursor.getColumnIndex("Impuesto")));
						this.Recargo = Double.parseDouble(cursor
								.getString(cursor.getColumnIndex("Recargo")));

						founded = true;
					}

				} while (cursor.moveToNext());
			}
		}

		if (cursor != null)
			cursor.close();

		return founded;
	}

	public LinkedList<String> getFiliaciones() throws Exception
	{		
		LinkedList<String> newList = new LinkedList<String>();
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM TiposIVA");
		
		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {
				do {

					String item = cursor.getString(cursor.getColumnIndex("Filiacion")).trim() + " - " +
							cursor.getString(cursor.getColumnIndex("Descripcion")).trim();
					
					if (!newList.contains(item))
						newList.add(item);
				} while (cursor.moveToNext());
			}
		}

		if (cursor != null)
			cursor.close();
		
		return newList;
	}
	
	public LinkedHashMap<String, TipoIVA> getAllTiposIVA() throws Exception {
		
		LinkedHashMap<String, TipoIVA> list = new LinkedHashMap<String, TipoIVA>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
				Constants.TABLE_TIPOS_IVA, Constants.EMPTY_STRING,
				Constants.EMPTY_STRING, true, Constants.EMPTY_STRING,
				Constants.EMPTY_STRING);
		// Cursor cursor =
		// super.getDatabaseOperations().executeSentence("SELECT * FROM Articulos WHERE Activo = 1 AND Tipo = '"
		// + String.valueOf(tipo) + "'" +" ORDER BY Descripcion ASC");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				do {
					TipoIVA iva = new TipoIVA();

					SimpleDateFormat formatter = new SimpleDateFormat(
							"dd/MM/yyyy");
					Date fecha = (Date) formatter.parse(cursor.getString(cursor
							.getColumnIndex("Fecha")));

					iva.IdTipoIVA = Long.parseLong(cursor.getString(cursor
							.getColumnIndex("IdTipoIVA")));
					iva.Articulo = cursor.getString(cursor
							.getColumnIndex("Articulo"));
					iva.Filiacion = cursor.getString(cursor
							.getColumnIndex("Filiacion"));
					iva.Fecha = fecha;
					iva.Descripcion=cursor.getString(cursor.getColumnIndex("Descripcion"));
					iva.Impuesto = Double.parseDouble(cursor.getString(cursor
							.getColumnIndex("Impuesto")));
					iva.Recargo = Double.parseDouble(cursor.getString(cursor
							.getColumnIndex("Recargo")));

					list.put(String.valueOf(iva.IdTipoIVA), iva);

				} while (cursor.moveToNext());

				cursor.close();
				return list;
			} else
				return list;
		} else
			return list;

	}
	
	public String getFiliacionByCode(String code) throws Exception
	{
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM TiposIVA WHERE Filiacion = '" + code + "' ORDER BY Fecha DESC");
		
		String item = Constants.EMPTY_STRING;
		
		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {
				
				item = cursor.getString(cursor.getColumnIndex("Filiacion")).trim() + " - " +
						cursor.getString(cursor.getColumnIndex("Descripcion")).trim();
					
			}
		}

		if (cursor != null)
			cursor.close();
		
		return item;
	}
	
}
