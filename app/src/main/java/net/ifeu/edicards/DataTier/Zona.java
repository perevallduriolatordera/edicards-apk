package net.ifeu.edicards.DataTier;

import java.util.ArrayList;
import java.util.List;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.content.ContentValues;
import android.database.Cursor;

public class Zona extends Persistent implements IPersistable {

	public long IdZona;
	public String NombreZona;
	public boolean Activa;

	@Override
	public void InitializePersistance(AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
	}

	@Override
	public void save() throws Exception {
		ContentValues values = new ContentValues();
		values.put("NombreZona", this.NombreZona);
		values.put("Activa", this.Activa ? 1 : 0);

		try {
			this.IdZona = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_ZONAS, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();
		values.put("NombreZona", this.NombreZona);
		values.put("Activa", this.Activa ? 1 : 0);

		String[] whereArgs = { String.valueOf(this.IdZona) };
		super.getDatabaseOperations().update(ConstantsDatabase.TABLE_ZONAS, values, "IdZona = ?", whereArgs);
	}

	/**
	 * Carga una zona por su ID
	 * @param idZona ID de la zona
	 * @return true si se encontró la zona, false si no
	 */
	public boolean setZonaById(long idZona) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
			ConstantsDatabase.TABLE_ZONAS,
			"IdZona",
			String.valueOf(idZona),
			false
		);

		if (cursor != null) {
			this.IdZona = cursor.getLong(cursor.getColumnIndex("IdZona"));
			this.NombreZona = cursor.getString(cursor.getColumnIndex("NombreZona"));
			this.Activa = cursor.getInt(cursor.getColumnIndex("Activa")) == 1;

			cursor.close();
			return true;
		}

		return false;
	}

	/**
	 * Obtiene todas las zonas activas
	 * @return Lista de zonas activas
	 */
	public ArrayList<Zona> getAllZonasActivas() throws Exception {
		ArrayList<Zona> zonas = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
			ConstantsDatabase.TABLE_ZONAS,
			"Activa",
			"1",
			false,
			"",
			"NombreZona"
		);

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				Zona zona = new Zona();
				zona.InitializePersistance(appConfig);
				zona.IdZona = cursor.getLong(cursor.getColumnIndex("IdZona"));
				zona.NombreZona = cursor.getString(cursor.getColumnIndex("NombreZona"));
				zona.Activa = cursor.getInt(cursor.getColumnIndex("Activa")) == 1;
				zonas.add(zona);
			} while (cursor.moveToNext());

			cursor.close();
		}

		return zonas;
	}

	/**
	 * Obtiene todas las zonas (activas e inactivas)
	 * @return Lista de todas las zonas
	 */
	public ArrayList<Zona> getAllZonas() throws Exception {
		ArrayList<Zona> zonas = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
			ConstantsDatabase.TABLE_ZONAS,
			"",
			"",
			false,
			"",
			"NombreZona"
		);

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				Zona zona = new Zona();
				zona.InitializePersistance(appConfig);
				zona.IdZona = cursor.getLong(cursor.getColumnIndex("IdZona"));
				zona.NombreZona = cursor.getString(cursor.getColumnIndex("NombreZona"));
				zona.Activa = cursor.getInt(cursor.getColumnIndex("Activa")) == 1;
				zonas.add(zona);
			} while (cursor.moveToNext());

			cursor.close();
		}

		return zonas;
	}

	/**
	 * Desactiva la zona (soft delete)
	 */
	public void desactivar() throws Exception {
		this.Activa = false;
		this.update();
	}

	/**
	 * Activa la zona
	 */
	public void activar() throws Exception {
		this.Activa = true;
		this.update();
	}

	/**
	 * Verifica si existe una zona con el nombre especificado (para evitar duplicados)
	 * @param nombreZona Nombre de la zona a verificar
	 * @return true si existe una zona con ese nombre, false si no
	 */
	public boolean existeZonaConNombre(String nombreZona) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
			ConstantsDatabase.TABLE_ZONAS,
			"NombreZona",
			nombreZona,
			true
		);

		if (cursor != null) {
			cursor.close();
			return true;
		}

		return false;
	}
}
