package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
		values.put("UpdateDateIngreso", formatter.format(this.UpdateDateIngreso));
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
		values.put("UpdateDateIngreso", formatter.format(this.UpdateDateIngreso));
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

	public boolean checkUpdateToday() {
		// Obtener las fechas actuales y las de efectivo (sin horas)
		Calendar currentCalendar = Calendar.getInstance();
		currentCalendar.setTime(new Date());
		currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
		currentCalendar.set(Calendar.MINUTE, 0);
		currentCalendar.set(Calendar.SECOND, 0);
		currentCalendar.set(Calendar.MILLISECOND, 0);

		Calendar updateCalendarIngreso = Calendar.getInstance();
		updateCalendarIngreso.setTime(this.UpdateDateIngreso);
		updateCalendarIngreso.set(Calendar.HOUR_OF_DAY, 0);
		updateCalendarIngreso.set(Calendar.MINUTE, 0);
		updateCalendarIngreso.set(Calendar.SECOND, 0);
		updateCalendarIngreso.set(Calendar.MILLISECOND, 0);

		Calendar updateCalendarEfectivo = Calendar.getInstance();
		updateCalendarEfectivo.setTime(this.UpdateDateEfectivo);
		updateCalendarEfectivo.set(Calendar.HOUR_OF_DAY, 0);
		updateCalendarEfectivo.set(Calendar.MINUTE, 0);
		updateCalendarEfectivo.set(Calendar.SECOND, 0);
		updateCalendarEfectivo.set(Calendar.MILLISECOND, 0);

		if (updateCalendarIngreso.getTime().equals(currentCalendar.getTime())) return true;
		if (updateCalendarEfectivo.getTime().equals(currentCalendar.getTime())) return true;

		return false;
	}

}
