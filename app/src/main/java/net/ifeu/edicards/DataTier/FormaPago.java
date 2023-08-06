package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import java.util.HashMap;

public class FormaPago extends Persistent implements IPersistable {

	public Long IdFormaPago;
	public String CodigoFormaPago;
	public String Descripcion;

	@Override
	public void save() throws Exception {

		ContentValues values = new ContentValues();
		values.put("CodigoFormaPago", this.CodigoFormaPago);
		values.put("Descripcion", this.Descripcion);

		try {
			this.IdFormaPago = super.getDatabaseOperations().insert(
					ConstantsDatabase.TABLE_FORMAS_PAGO, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();

		values.put("IdFormaPago", this.IdFormaPago);
		values.put("CodigoFormaPago", this.CodigoFormaPago);
		values.put("Descripcion", this.Descripcion);

		String[] whereArgs = { String.valueOf(this.IdFormaPago) };

		super.getDatabaseOperations().update(ConstantsDatabase.TABLE_FORMAS_PAGO,
				values, "IdFormaPago = ?", whereArgs);
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				ConstantsDatabase.TABLE_FORMAS_PAGO);
	}

	@Override
	public void clean() throws Exception {
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_FORMAS_PAGO);
		
		cursor.close();
						
	}

	public boolean setFormaPagoById(String idFormaPago) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
				ConstantsDatabase.TABLE_FORMAS_PAGO, "IdFormaPago", idFormaPago, false);

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
				ConstantsDatabase.TABLE_FORMAS_PAGO, "CodigoFormaPago",
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

	public HashMap<String, FormaPago> getAllFormasPago() {
		HashMap<String, FormaPago> list = new HashMap<>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
				ConstantsDatabase.TABLE_FORMAS_PAGO, ConstantsTypes.EMPTY_STRING,
				ConstantsTypes.EMPTY_STRING, true, ConstantsTypes.EMPTY_STRING,
				"Descripcion");

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {

				do {
					FormaPago formaPago = Factory.build(FormaPago.class, appConfig);

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
}
