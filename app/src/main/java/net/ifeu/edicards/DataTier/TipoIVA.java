package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.content.ContentValues;
import android.database.Cursor;

public class TipoIVA extends Persistent implements IPersistable {

	public long IdTipoIVA;
	public String Filiacion;
	public String Articulo;
	public double Impuesto;
	public double Recargo;
	public Date Fecha;
	public String Descripcion;

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
					ConstantsDatabase.TABLE_TIPOS_IVA, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void clean() throws Exception {
		super.getDatabaseOperations().deleteAllRecords(
				ConstantsDatabase.TABLE_TIPOS_IVA);
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				ConstantsDatabase.TABLE_TIPOS_IVA);
	}

	public boolean setTipoIVAByClienteArticulo(Cliente cliente,
			Articulo articulo) throws Exception {

		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM " + ConstantsDatabase.TABLE_TIPOS_IVA
						+ " WHERE Articulo='"
						+ articulo.TipoIVA
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
		LinkedList<String> newList = new LinkedList<>();
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM TiposIVA GROUP BY Filiacion,Descripcion");
		
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

	public String getFiliacionByCode(String code) throws Exception
	{
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM TiposIVA WHERE Filiacion = '" + code + "' ORDER BY Fecha DESC");
		
		String item = ConstantsTypes.EMPTY_STRING;
		
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
