package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CiudadVendedor extends Persistent implements IPersistable {

	public long IdCiudadVendedor;
	public String Usuario;
	public String CiudadBase;
	public String CodigoPostal;
	public Date FechaCreacion;

	@Override
	public void InitializePersistance(AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
	}

	@Override
	public void save() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		ContentValues values = new ContentValues();
		values.put("Usuario", this.Usuario);
		values.put("CiudadBase", this.CiudadBase);
		values.put("CodigoPostal", this.CodigoPostal);
		values.put("FechaCreacion", formatter.format(new Date()));

		try {
			this.IdCiudadVendedor = super.getDatabaseOperations().insert(
				ConstantsDatabase.TABLE_CIUDAD_VENDEDOR, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		ContentValues values = new ContentValues();
		values.put("Usuario", this.Usuario);
		values.put("CiudadBase", this.CiudadBase);
		values.put("CodigoPostal", this.CodigoPostal);

		String[] whereArgs = {String.valueOf(this.IdCiudadVendedor)};

		super.getDatabaseOperations().update(
			ConstantsDatabase.TABLE_CIUDAD_VENDEDOR, values,
			"IdCiudadVendedor = ?", whereArgs);
	}

	public boolean exists() throws Exception {
		String usuario = appConfig.getUser().User;

		Cursor cursor = super.getDatabaseOperations().executeSentence(
			"SELECT * FROM " + ConstantsDatabase.TABLE_CIUDAD_VENDEDOR +
			" WHERE Usuario = '" + usuario + "'");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.close();
			return true;
		}

		if (cursor != null) {
			cursor.close();
		}

		return false;
	}

	public void load() throws Exception {
		String usuario = appConfig.getUser().User;

		Cursor cursor = super.getDatabaseOperations().executeSentence(
			"SELECT * FROM " + ConstantsDatabase.TABLE_CIUDAD_VENDEDOR +
			" WHERE Usuario = '" + usuario + "'");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			this.IdCiudadVendedor = Long.parseLong(
				cursor.getString(cursor.getColumnIndex("IdCiudadVendedor")));
			this.Usuario = cursor.getString(cursor.getColumnIndex("Usuario"));
			this.CiudadBase = cursor.getString(cursor.getColumnIndex("CiudadBase"));
			this.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
			try {
				this.FechaCreacion = formatter.parse(
					cursor.getString(cursor.getColumnIndex("FechaCreacion")));
			} catch (Exception e) {
				this.FechaCreacion = new Date();
			}

			cursor.close();
		}
	}

	public String getCiudadBase() {
		return this.CiudadBase;
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
			ConstantsDatabase.TABLE_CIUDAD_VENDEDOR);
	}
}
