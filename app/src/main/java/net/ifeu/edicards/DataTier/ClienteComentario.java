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

public class ClienteComentario extends Persistent implements IPersistable {

	public long IdComentario;
	public String CodigoCliente = ConstantsTypes.EMPTY_STRING;
	public String Comentario = ConstantsTypes.EMPTY_STRING;
	public Date FechaCreacion;
	public String Usuario = ConstantsTypes.EMPTY_STRING;

	@SuppressLint("SimpleDateFormat")
	@Override
	public void save() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ContentValues values = new ContentValues();
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("Comentario", this.Comentario);
		values.put("FechaCreacion", formatter.format(this.FechaCreacion));
		values.put("Usuario", this.Usuario);

		try {
			this.IdComentario = super.getDatabaseOperations().insert(
				ConstantsDatabase.TABLE_CLIENTES_COMENTARIOS, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void update() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ContentValues values = new ContentValues();
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("Comentario", this.Comentario);
		values.put("FechaCreacion", formatter.format(this.FechaCreacion));
		values.put("Usuario", this.Usuario);

		String[] whereArgs = { String.valueOf(this.IdComentario) };

		super.getDatabaseOperations().update(
			ConstantsDatabase.TABLE_CLIENTES_COMENTARIOS, values, "IdComentario = ?", whereArgs);
	}

	/**
	 * Elimina el comentario de la base de datos
	 */
	public void delete() throws Exception {
		try {
			super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_CLIENTES_COMENTARIOS +
				" WHERE IdComentario = " + this.IdComentario);
		} catch (Exception e) {
			throw new Exception("Error eliminando comentario: " + e.getMessage());
		}
	}

	/**
	 * Carga un comentario por su ID
	 */
	@SuppressLint("SimpleDateFormat")
	public boolean setComentarioById(long idComentario) throws Exception {
		Cursor cursor = super.getDatabaseOperations().getFirstRecordFromField(
			ConstantsDatabase.TABLE_CLIENTES_COMENTARIOS, "IdComentario",
			String.valueOf(idComentario), false);

		if (cursor != null) {
			this.IdComentario = idComentario;
			this.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
			this.Comentario = cursor.getString(cursor.getColumnIndex("Comentario"));
			this.Usuario = cursor.getString(cursor.getColumnIndex("Usuario"));

			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String fechaStr = cursor.getString(cursor.getColumnIndex("FechaCreacion"));
			if (fechaStr != null && !fechaStr.isEmpty()) {
				this.FechaCreacion = formatter.parse(fechaStr);
			}

			cursor.close();
			return true;
		}

		return false;
	}

	/**
	 * Obtiene todos los comentarios de un cliente
	 */
	@SuppressLint("SimpleDateFormat")
	public ArrayList<ClienteComentario> getComentariosByCliente(String codigoCliente) throws Exception {
		ArrayList<ClienteComentario> comentarios = new ArrayList<>();

		Cursor cursor = super.getDatabaseOperations().getRecordsFromField(
			ConstantsDatabase.TABLE_CLIENTES_COMENTARIOS,
			"CodigoCliente", codigoCliente, true, "", "FechaCreacion DESC");

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			do {
				ClienteComentario comentario = Factory.build(ClienteComentario.class, this.appConfig);

				comentario.IdComentario = cursor.getLong(cursor.getColumnIndex("IdComentario"));
				comentario.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				comentario.Comentario = cursor.getString(cursor.getColumnIndex("Comentario"));
				comentario.Usuario = cursor.getString(cursor.getColumnIndex("Usuario"));

				String fechaStr = cursor.getString(cursor.getColumnIndex("FechaCreacion"));
				if (fechaStr != null && !fechaStr.isEmpty()) {
					try {
						comentario.FechaCreacion = formatter.parse(fechaStr);
					} catch (Exception e) {
						comentario.FechaCreacion = new Date();
					}
				}

				comentarios.add(comentario);
			} while (cursor.moveToNext());

			cursor.close();
		}

		return comentarios;
	}

	/**
	 * Cuenta los comentarios de un cliente
	 */
	public int getComentariosCount(String codigoCliente) {
		try {
			Cursor cursor = super.getDatabaseOperations().executeSentence(
				"SELECT COUNT(*) FROM " + ConstantsDatabase.TABLE_CLIENTES_COMENTARIOS +
				" WHERE CodigoCliente = '" + codigoCliente + "'");

			if (cursor != null && cursor.moveToFirst()) {
				int count = cursor.getInt(0);
				cursor.close();
				return count;
			}
		} catch (Exception e) {
			android.util.Log.e("ClienteComentario", "Error contando comentarios: " + e.getMessage());
		}
		return 0;
	}
}
