package net.ifeu.edicards.DataTier;

import java.util.HashMap;
import java.util.LinkedList;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class FormaPago extends Persistent implements IPersistable {

	public Long IdFormaPago;
	public String CodigoFormaPago;
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

		ContentValues values = new ContentValues();
		values.put("CodigoFormaPago", this.CodigoFormaPago);
		values.put("Descripcion", this.Descripcion);

		try {
			this.IdFormaPago = super.getDatabaseOperations().insert(
					Constants.TABLE_FORMAS_PAGO, null, values);
		} catch (Exception e) {
			throw e;
		}

	}

	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();

		values.put("IdFormaPago", this.IdFormaPago);
		values.put("CodigoFormaPago", this.CodigoFormaPago);
		values.put("Descripcion", this.Descripcion);

		String[] whereArgs = { String.valueOf(this.IdFormaPago) };

		super.getDatabaseOperations().update(Constants.TABLE_FORMAS_PAGO,
				values, "IdFormaPago = ?", whereArgs);
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				Constants.TABLE_FORMAS_PAGO);
	}

	@Override
	public void clean() throws Exception {
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_FORMAS_PAGO);
		
		cursor.close();
						
	}

	public boolean setFormaPagoById(String idFormaPago) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
				Constants.TABLE_FORMAS_PAGO, "IdFormaPago", idFormaPago, false);

		if (cursor != null) {
			this.IdFormaPago = Long.parseLong(idFormaPago);
			this.CodigoFormaPago = cursor.getString(cursor
					.getColumnIndex("CodigoFormaPago"));
			this.Descripcion = cursor.getString(cursor
					.getColumnIndex("Descripcion"));

			cursor.close();
			return true;
		}

		return false; // (cursor != null);
	}

	public boolean setFormaPagoByCode(String codigoFormaPago) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
				Constants.TABLE_FORMAS_PAGO, "CodigoFormaPago",
				codigoFormaPago, true);

		if (cursor != null) {
			this.IdFormaPago = Long.parseLong(cursor.getString(cursor
					.getColumnIndex("IdFormaPago")));
			this.CodigoFormaPago = codigoFormaPago;
			this.Descripcion = cursor.getString(cursor
					.getColumnIndex("Descripcion"));

			cursor.close();
			return true;
		}

		return false; // (cursor != null);
	}

	public String[] getAllFormasPagoDescripcion() throws Exception {
		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
				Constants.TABLE_FORMAS_PAGO, Constants.EMPTY_STRING,
				Constants.EMPTY_STRING, true, Constants.EMPTY_STRING,
				"Descripcion");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				int index = 0;
				String[] list = new String[cursor.getCount()];
				;
				do {
					FormaPago formaPago = new FormaPago();

					String descripcion = formaPago.Descripcion = cursor
							.getString(cursor.getColumnIndex("Descripcion"));
					list[index++] = descripcion;

				} while (cursor.moveToNext());

				cursor.close();
				return list;
			} else {
				cursor.close();
				return new String[] {};
			}
		} else
			return new String[] {};
	}

	public Cursor getAllFormasPagoCursor() throws Exception {
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT p.*,p.rowid as _id FROM " + Constants.TABLE_FORMAS_PAGO
						+ " p Order by p.Descripcion");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0)
				return cursor;
			else
				return null;
		} else
			return cursor;
	}

	public HashMap<String, FormaPago> getAllFormasPago() throws Exception {
		HashMap<String, FormaPago> list = new HashMap<String, FormaPago>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
				Constants.TABLE_FORMAS_PAGO, Constants.EMPTY_STRING,
				Constants.EMPTY_STRING, true, Constants.EMPTY_STRING,
				"Descripcion");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				do {
					FormaPago formaPago = new FormaPago();

					formaPago.IdFormaPago = Long.parseLong(cursor
							.getString(cursor.getColumnIndex("IdFormaPago")));
					formaPago.CodigoFormaPago = cursor.getString(cursor
							.getColumnIndex("CodigoFormaPago"));
					formaPago.Descripcion = cursor.getString(cursor
							.getColumnIndex("Descripcion"));

					list.put(formaPago.CodigoFormaPago, formaPago);

				} while (cursor.moveToNext());

				cursor.close();
				return list;
			} else {
				cursor.close();
				return list;
			}
		} else
			return list;

	}

	public LinkedList<String> getAllFormasPagoList() throws Exception {
		LinkedList<String> newList = new LinkedList<String>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
				Constants.TABLE_FORMAS_PAGO, Constants.EMPTY_STRING,
				Constants.EMPTY_STRING, true, Constants.EMPTY_STRING,
				"Descripcion");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {
				do {

					String item = cursor.getString(cursor
							.getColumnIndex("Descripcion"));
					newList.add(item);
				} while (cursor.moveToNext());
			}
		}

		if (cursor != null)
			cursor.close();

		return newList;
	}
}
