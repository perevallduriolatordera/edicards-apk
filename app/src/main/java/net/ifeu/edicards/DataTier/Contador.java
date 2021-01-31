package net.ifeu.edicards.DataTier;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class Contador extends Persistent implements IPersistable {

	public long IdContador;
	public int ContadorSerieA;
	public int ContadorSerieB;

	@Override
	public void InitializePersistance(AppConfig appConfig, Context context) throws Exception {
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
		values.put("ContadorSerieA", this.ContadorSerieA);
		values.put("ContadorSerieB", this.ContadorSerieB);

		try {
			this.IdContador = super.getDatabaseOperations().insert(
					Constants.TABLE_CONTADORES, null, values);
		} catch (Exception e) {
			throw e;
		}

	}

	@Override
	public void update() throws Exception {

		ContentValues values = new ContentValues();

		values.put("IdContador", this.IdContador);
		values.put("ContadorSerieA", this.ContadorSerieA);
		values.put("ContadorSerieB", this.ContadorSerieB);

		String[] whereArgs = { String.valueOf(this.IdContador) };

		super.getDatabaseOperations().update(Constants.TABLE_CONTADORES,
				values, "IdContador = ?", whereArgs);
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
				Constants.TABLE_CONTADORES);
	}

	public void getContadores() throws Exception {
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT * FROM " + Constants.TABLE_CONTADORES);

		if (cursor != null) {
			cursor.moveToFirst();
			if (cursor.getCount() > 0) {
				this.IdContador = Long.parseLong(cursor.getString(cursor
						.getColumnIndex("IdContador")));
				this.ContadorSerieA = Integer.parseInt(cursor.getString(cursor
						.getColumnIndex("ContadorSerieA")));
				this.ContadorSerieB = Integer.parseInt(cursor.getString(cursor
						.getColumnIndex("ContadorSerieB")));
			}

			cursor.close();
		}

	}

}
