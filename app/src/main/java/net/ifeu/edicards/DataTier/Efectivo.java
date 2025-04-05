package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Efectivo extends Persistent implements IPersistable {

	public long IdEfectivo;
	public double Efectivo;
	public Date UpdateDateIngreso;
	public Date UpdateDateEfectivo;

	@Override
	public void save() throws Exception {

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");

		ContentValues values = new ContentValues();
		values.put("Efectivo", this.Efectivo);
		values.put("UpdateDateIngreso", formatter.format(this.UpdateDateEfectivo));
		values.put("UpdateDateEfectivo", formatter.format(this.UpdateDateEfectivo));

		try {
			this.IdEfectivo = super.getDatabaseOperations().insert(
					ConstantsDatabase.TABLE_EFECTIVO, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void update() throws Exception {

		ContentValues values = new ContentValues();

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");

		values.put("Efectivo", this.Efectivo);
		values.put("UpdateDateIngreso", formatter.format(this.UpdateDateEfectivo));
		values.put("UpdateDateEfectivo", formatter.format(this.UpdateDateEfectivo));

		String[] whereArgs = { String.valueOf(this.IdEfectivo) };

		super.getDatabaseOperations().update(ConstantsDatabase.TABLE_EFECTIVO,
				values, "IdEfectivo = ?", whereArgs);
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				ConstantsDatabase.TABLE_EFECTIVO);
	}

	public void getEfectivo() throws Exception {

		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");

		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM " + ConstantsDatabase.TABLE_EFECTIVO);

		if (cursor != null) {
			cursor.moveToFirst();
			if (cursor.getCount() > 0) {
				this.IdEfectivo = Long.parseLong(cursor.getString(cursor
						.getColumnIndex("IdEfectivo")));
				this.Efectivo = Double.parseDouble(cursor.getString(cursor
						.getColumnIndex("Efectivo")));
				this.UpdateDateIngreso = formatter.parse(cursor.getString(cursor
						.getColumnIndex("UpdateDateIngreso")));
				this.UpdateDateEfectivo = formatter.parse(cursor.getString(cursor
						.getColumnIndex("UpdateDateEfectivo")));
			}

			cursor.close();
		} else {
			this.IdEfectivo = 1;
			this.Efectivo = 0;
			this.UpdateDateIngreso = new Date();
			this.UpdateDateEfectivo = new Date();
			this.save();
		}

	}

}
