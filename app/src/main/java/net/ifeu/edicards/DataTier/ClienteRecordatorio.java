package net.ifeu.edicards.DataTier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;

public class ClienteRecordatorio extends Persistent implements IPersistable {

	public long IdRecordatorio;
	public String CodigoCliente = ConstantsTypes.EMPTY_STRING;
	public String TipoRecordatorio = ConstantsTypes.EMPTY_STRING; // "Llamar", "Volver a pasar", "Otro"
	public String Descripcion = ConstantsTypes.EMPTY_STRING;
	public Date FechaRecordatorio;
	public String HoraRecordatorio = ConstantsTypes.EMPTY_STRING; // "HH:mm"
	public Date FechaCreacion;
	public String Usuario = ConstantsTypes.EMPTY_STRING;
	public boolean Completado = false;
	public Date FechaCompletado;

	@SuppressLint("SimpleDateFormat")
	@Override
	public void save() throws Exception {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ContentValues values = new ContentValues();
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("TipoRecordatorio", this.TipoRecordatorio);
		values.put("Descripcion", this.Descripcion);
		values.put("FechaRecordatorio", dateFormatter.format(this.FechaRecordatorio));
		values.put("HoraRecordatorio", this.HoraRecordatorio);
		values.put("FechaCreacion", datetimeFormatter.format(this.FechaCreacion));
		values.put("Usuario", this.Usuario);
		values.put("Completado", this.Completado ? 1 : 0);
		if (this.FechaCompletado != null) {
			values.put("FechaCompletado", datetimeFormatter.format(this.FechaCompletado));
		}

		try {
			this.IdRecordatorio = super.getDatabaseOperations().insert(
				ConstantsDatabase.TABLE_CLIENTES_RECORDATORIOS, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void update() throws Exception {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ContentValues values = new ContentValues();
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("TipoRecordatorio", this.TipoRecordatorio);
		values.put("Descripcion", this.Descripcion);
		values.put("FechaRecordatorio", dateFormatter.format(this.FechaRecordatorio));
		values.put("HoraRecordatorio", this.HoraRecordatorio);
		values.put("FechaCreacion", datetimeFormatter.format(this.FechaCreacion));
		values.put("Usuario", this.Usuario);
		values.put("Completado", this.Completado ? 1 : 0);
		if (this.FechaCompletado != null) {
			values.put("FechaCompletado", datetimeFormatter.format(this.FechaCompletado));
		} else {
			values.putNull("FechaCompletado");
		}

		String[] whereArgs = { String.valueOf(this.IdRecordatorio) };
		super.getDatabaseOperations().update(
			ConstantsDatabase.TABLE_CLIENTES_RECORDATORIOS, values,
			"IdRecordatorio = ?", whereArgs);
	}

	@Override
	public void delete() throws Exception {
		try {
			super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_CLIENTES_RECORDATORIOS +
				" WHERE IdRecordatorio = " + this.IdRecordatorio);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Obtiene todos los recordatorios de un cliente
	 */
	@SuppressLint("SimpleDateFormat")
	public ArrayList<ClienteRecordatorio> getRecordatoriosByCliente(String codigoCliente) throws Exception {
		ArrayList<ClienteRecordatorio> recordatorios = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
			ConstantsDatabase.TABLE_CLIENTES_RECORDATORIOS,
			"CodigoCliente", codigoCliente, true, "",
			"Completado ASC, FechaRecordatorio ASC, HoraRecordatorio ASC");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			do {
				ClienteRecordatorio recordatorio = Factory.build(ClienteRecordatorio.class, this.appConfig);

				recordatorio.IdRecordatorio = cursor.getLong(cursor.getColumnIndex("IdRecordatorio"));
				recordatorio.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				recordatorio.TipoRecordatorio = cursor.getString(cursor.getColumnIndex("TipoRecordatorio"));

				int descIndex = cursor.getColumnIndex("Descripcion");
				if (!cursor.isNull(descIndex)) {
					recordatorio.Descripcion = cursor.getString(descIndex);
				}

				String fechaRecStr = cursor.getString(cursor.getColumnIndex("FechaRecordatorio"));
				if (fechaRecStr != null && !fechaRecStr.isEmpty()) {
					try {
						recordatorio.FechaRecordatorio = dateFormatter.parse(fechaRecStr);
					} catch (Exception e) {
						recordatorio.FechaRecordatorio = new Date();
					}
				}

				recordatorio.HoraRecordatorio = cursor.getString(cursor.getColumnIndex("HoraRecordatorio"));

				String fechaCreStr = cursor.getString(cursor.getColumnIndex("FechaCreacion"));
				if (fechaCreStr != null && !fechaCreStr.isEmpty()) {
					try {
						recordatorio.FechaCreacion = datetimeFormatter.parse(fechaCreStr);
					} catch (Exception e) {
						recordatorio.FechaCreacion = new Date();
					}
				}

				int usuarioIndex = cursor.getColumnIndex("Usuario");
				if (!cursor.isNull(usuarioIndex)) {
					recordatorio.Usuario = cursor.getString(usuarioIndex);
				}

				recordatorio.Completado = cursor.getInt(cursor.getColumnIndex("Completado")) == 1;

				int fechaCompIndex = cursor.getColumnIndex("FechaCompletado");
				if (!cursor.isNull(fechaCompIndex)) {
					String fechaCompStr = cursor.getString(fechaCompIndex);
					if (fechaCompStr != null && !fechaCompStr.isEmpty()) {
						try {
							recordatorio.FechaCompletado = datetimeFormatter.parse(fechaCompStr);
						} catch (Exception e) {
							// Ignorar error de parseo
						}
					}
				}

				recordatorios.add(recordatorio);
			} while (cursor.moveToNext());

			cursor.close();
		}

		return recordatorios;
	}

	/**
	 * Obtiene todos los recordatorios pendientes (no completados)
	 */
	public ArrayList<ClienteRecordatorio> getRecordatoriosPendientes() throws Exception {
		ArrayList<ClienteRecordatorio> recordatorios = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations().executeSentence(
			"SELECT * FROM " + ConstantsDatabase.TABLE_CLIENTES_RECORDATORIOS +
			" WHERE Completado = 0 ORDER BY FechaRecordatorio ASC, HoraRecordatorio ASC");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat datetimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			do {
				ClienteRecordatorio recordatorio = Factory.build(ClienteRecordatorio.class, this.appConfig);

				recordatorio.IdRecordatorio = cursor.getLong(cursor.getColumnIndex("IdRecordatorio"));
				recordatorio.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				recordatorio.TipoRecordatorio = cursor.getString(cursor.getColumnIndex("TipoRecordatorio"));

				int descIndex = cursor.getColumnIndex("Descripcion");
				if (!cursor.isNull(descIndex)) {
					recordatorio.Descripcion = cursor.getString(descIndex);
				}

				String fechaRecStr = cursor.getString(cursor.getColumnIndex("FechaRecordatorio"));
				if (fechaRecStr != null && !fechaRecStr.isEmpty()) {
					try {
						recordatorio.FechaRecordatorio = dateFormatter.parse(fechaRecStr);
					} catch (Exception e) {
						recordatorio.FechaRecordatorio = new Date();
					}
				}

				recordatorio.HoraRecordatorio = cursor.getString(cursor.getColumnIndex("HoraRecordatorio"));

				String fechaCreStr = cursor.getString(cursor.getColumnIndex("FechaCreacion"));
				if (fechaCreStr != null && !fechaCreStr.isEmpty()) {
					try {
						recordatorio.FechaCreacion = datetimeFormatter.parse(fechaCreStr);
					} catch (Exception e) {
						recordatorio.FechaCreacion = new Date();
					}
				}

				int usuarioIndex = cursor.getColumnIndex("Usuario");
				if (!cursor.isNull(usuarioIndex)) {
					recordatorio.Usuario = cursor.getString(usuarioIndex);
				}

				recordatorio.Completado = false;

				recordatorios.add(recordatorio);
			} while (cursor.moveToNext());

			cursor.close();
		}

		return recordatorios;
	}
}
