package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class RutaGenerada extends Persistent implements IPersistable {

	public long IdRuta;
	public Date FechaGeneracion;
	public int OrdenVisita;
	public String CodigoCliente;
	public String NombreCliente;
	public String DireccionCliente;
	public String PoblacionCliente;
	public String ProvinciaCliente;
	public String DistanciaEstimada;
	public String CiudadBase;

	// Geolocalización
	public String GeolocalizationStatus;  // "✓ OK", "⚠ SIN COORDS", "❌ INVÁLIDAS"
	public String Latitud;
	public String Longitud;

	// Cluster / Zona
	public int ClusterID;  // ID del cluster geográfico (se persiste en BD y se muestra en Excel)
	public String NombreZona;  // Nombre de la zona (para rutas por zonas, no se persiste en BD)

	@Override
	public void InitializePersistance(net.ifeu.edicards.Application.AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
	}

	@Override
	public void save() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		ContentValues values = new ContentValues();
		values.put("FechaGeneracion", formatter.format(this.FechaGeneracion));
		values.put("OrdenVisita", this.OrdenVisita);
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("NombreCliente", this.NombreCliente);
		values.put("DireccionCliente", this.DireccionCliente);
		values.put("PoblacionCliente", this.PoblacionCliente);
		values.put("ProvinciaCliente", this.ProvinciaCliente);
		values.put("DistanciaEstimada", this.DistanciaEstimada);
		values.put("CiudadBase", this.CiudadBase);
		values.put("GeolocalizationStatus", this.GeolocalizationStatus);
		values.put("Latitud", this.Latitud);
		values.put("Longitud", this.Longitud);
		values.put("ClusterID", this.ClusterID);

		try {
			this.IdRuta = super.getDatabaseOperations().insert(
				ConstantsDatabase.TABLE_RUTAS_GENERADAS, null, values);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void update() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		ContentValues values = new ContentValues();
		values.put("OrdenVisita", this.OrdenVisita);
		values.put("CodigoCliente", this.CodigoCliente);
		values.put("NombreCliente", this.NombreCliente);
		values.put("DireccionCliente", this.DireccionCliente);
		values.put("PoblacionCliente", this.PoblacionCliente);
		values.put("ProvinciaCliente", this.ProvinciaCliente);
		values.put("DistanciaEstimada", this.DistanciaEstimada);
		values.put("GeolocalizationStatus", this.GeolocalizationStatus);
		values.put("Latitud", this.Latitud);
		values.put("Longitud", this.Longitud);

		String[] whereArgs = {String.valueOf(this.IdRuta)};

		super.getDatabaseOperations().update(
			ConstantsDatabase.TABLE_RUTAS_GENERADAS, values,
			"IdRuta = ?", whereArgs);
	}

	public boolean hasRouteCurrentWeek() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date today = new Date();

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		Cursor cursor = super.getDatabaseOperations().executeSentence(
			"SELECT * FROM " + ConstantsDatabase.TABLE_RUTAS_GENERADAS +
			" WHERE substr(FechaGeneracion,1,4)||substr(FechaGeneracion,6,2)||substr(FechaGeneracion,9,2) " +
			"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

		boolean result = false;

		if (cursor != null) {
			result = cursor.getCount() > 0;
			cursor.close();
		}

		return result;
	}

	public ArrayList<RutaGenerada> getRutaCurrentWeek() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date today = new Date();

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		Cursor cursor = super.getDatabaseOperations().executeSentence(
			"SELECT * FROM " + ConstantsDatabase.TABLE_RUTAS_GENERADAS +
			" WHERE substr(FechaGeneracion,1,4)||substr(FechaGeneracion,6,2)||substr(FechaGeneracion,9,2) " +
			"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'" +
			" ORDER BY OrdenVisita ASC");

		ArrayList<RutaGenerada> list = new ArrayList<>();

		if (cursor != null) {
			cursor.moveToFirst();

			if (cursor.getCount() > 0) {
				do {
					RutaGenerada ruta = new RutaGenerada();
					ruta.InitializePersistance(appConfig);

					ruta.IdRuta = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdRuta")));
					ruta.FechaGeneracion = new Date();
					ruta.OrdenVisita = Integer.parseInt(cursor.getString(cursor.getColumnIndex("OrdenVisita")));
					ruta.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
					ruta.NombreCliente = cursor.getString(cursor.getColumnIndex("NombreCliente"));
					ruta.DireccionCliente = cursor.getString(cursor.getColumnIndex("DireccionCliente"));
					ruta.PoblacionCliente = cursor.getString(cursor.getColumnIndex("PoblacionCliente"));
					ruta.ProvinciaCliente = cursor.getString(cursor.getColumnIndex("ProvinciaCliente"));
					ruta.DistanciaEstimada = cursor.getString(cursor.getColumnIndex("DistanciaEstimada"));
					ruta.CiudadBase = cursor.getString(cursor.getColumnIndex("CiudadBase"));
					ruta.GeolocalizationStatus = cursor.getString(cursor.getColumnIndex("GeolocalizationStatus"));
					ruta.Latitud = cursor.getString(cursor.getColumnIndex("Latitud"));
					ruta.Longitud = cursor.getString(cursor.getColumnIndex("Longitud"));

					// Cargar ClusterID (con fallback a 0 si la columna no existe o es null)
					int clusterIdIndex = cursor.getColumnIndex("ClusterID");
					ruta.ClusterID = (clusterIdIndex >= 0 && !cursor.isNull(clusterIdIndex))
						? cursor.getInt(clusterIdIndex)
						: 0;

					list.add(ruta);
				} while (cursor.moveToNext());
			}

			cursor.close();
		}

		return list;
	}

	public void deleteCurrentWeekRoutes() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		Date today = new Date();

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		super.getDatabaseOperations().executeSentence(
			"DELETE FROM " + ConstantsDatabase.TABLE_RUTAS_GENERADAS +
			" WHERE substr(FechaGeneracion,1,4)||substr(FechaGeneracion,6,2)||substr(FechaGeneracion,9,2) " +
			"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
	}

	public int getRecordsCount() {
		return super.getDatabaseOperations().getRecordsCount(
			ConstantsDatabase.TABLE_RUTAS_GENERADAS);
	}
}
