package net.ifeu.edicards.Services;

import android.content.Context;
import android.util.Log;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.CiudadVendedor;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;
import net.ifeu.edicards.Services.Geocoding.IGeocodingStrategy;
import net.ifeu.edicards.Services.Geocoding.LatLng;
import net.ifeu.edicards.Services.Geocoding.OpenRouteServiceGeocodingStrategy;

import java.util.ArrayList;
import java.util.Date;

/**
 * Servicio para generar rutas optimizadas usando OpenRouteService
 * Utiliza clustering geográfico para dividir clientes por zonas
 * y luego optimiza cada zona con TSP Nearest Neighbor
 */
public class RouteGeneratorService {

	private static final String TAG = "RouteGeneratorService";

	/**
	 * Genera una ruta optimizada para los clientes activos del vendedor
	 * Usa clustering geográfico para agrupar clientes por zonas
	 *
	 * @param context Contexto de la aplicación
	 * @param ciudadBase Ciudad donde se ubica la base del vendedor
	 * @return true si la ruta se generó exitosamente, false en caso contrario
	 * @throws Exception Si hay error durante el proceso
	 */
	public boolean generateRoute(Context context, String ciudadBase) throws Exception {
		try {
			AppConfig app = (AppConfig) context.getApplicationContext();

			// Debug: Contar TODOS los clientes activos
			android.database.Cursor cursorTodos = app.getDatabaseOperations().executeSentence(
				"SELECT COUNT(*) FROM Clientes WHERE Activo = 1");
			int totalActivos = 0;
			if (cursorTodos != null && cursorTodos.getCount() > 0) {
				cursorTodos.moveToFirst();
				totalActivos = cursorTodos.getInt(0);
				cursorTodos.close();
			}
			Log.i(TAG, "Total clientes activos en BD: " + totalActivos);

			// Debug: Contar clientes con coordenadas
			android.database.Cursor cursorConCoordenadas = app.getDatabaseOperations().executeSentence(
				"SELECT COUNT(*) FROM Clientes WHERE Activo = 1 AND Latitud IS NOT NULL AND Latitud != 0 AND Longitud IS NOT NULL AND Longitud != 0");
			int clientesConCoordenadas = 0;
			if (cursorConCoordenadas != null && cursorConCoordenadas.getCount() > 0) {
				cursorConCoordenadas.moveToFirst();
				clientesConCoordenadas = cursorConCoordenadas.getInt(0);
				cursorConCoordenadas.close();
			}
			Log.i(TAG, "Clientes activos con coordenadas válidas: " + clientesConCoordenadas);

			// 1. Obtener clientes activos con coordenadas geocodificadas
			ArrayList<Cliente> clientesActivos = getClientesActivosConCoordenadas(app);

			if (clientesActivos == null || clientesActivos.isEmpty()) {
				Log.w(TAG, "No hay clientes activos con coordenadas para generar ruta");
				return false;
			}

			if (clientesActivos.size() < 2) {
				Log.w(TAG, "Mínimo 2 clientes con coordenadas requeridos para generar ruta (actual: " + clientesActivos.size() + ")");
				return false;
			}

			// 2. Obtener ubicación de la base del vendedor
			CiudadVendedor ciudad = Factory.build(CiudadVendedor.class, app);
			ciudad.load();

			// Geocodificar ubicación de la base si no está ya en caché
			IGeocodingStrategy geocodingStrategy = new OpenRouteServiceGeocodingStrategy();
			LatLng baseLocation = geocodingStrategy.geocodeAddress(
				ciudadBase,
				null,
				null,
				ciudad.CodigoPostal
			);

			if (baseLocation == null) {
				Log.e(TAG, "No se pudo geocodificar la ciudad base: " + ciudadBase);
				return false;
			}

			Log.d(TAG, "Ciudad base geocodificada: " + baseLocation.toString());

			// 3. Clustering geográfico de clientes
			GeoClusteringService clusteringService = new GeoClusteringService();
			int gridSize = determineOptimalGridSize(clientesActivos.size());
			ArrayList<GeoClusteringService.GeoCluster> clusters = clusteringService.clusterizeClients(
				clientesActivos,
				gridSize,
				baseLocation
			);

			if (clusters == null || clusters.isEmpty()) {
				Log.e(TAG, "Error: no se pudieron crear clusters geográficos");
				return false;
			}

			// 4. Optimizar ruta considerando clusters
			ArrayList<RouteOptimizerService.RutaClienteData> rutaOrdenada = optimizeRouteWithClusters(
				clusters,
				baseLocation
			);

			if (rutaOrdenada == null || rutaOrdenada.isEmpty()) {
				Log.e(TAG, "Error: no se pudo optimizar la ruta con clusters");
				return false;
			}

			// 5. Guardar ruta en BD
			saveRoute(app, rutaOrdenada, ciudadBase);

			Log.i(TAG, "Ruta generada exitosamente con " + rutaOrdenada.size() + " clientes en " + clusters.size() + " clusters");
			return true;

		} catch (Exception e) {
			Log.e(TAG, "Error generando ruta: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Determina el tamaño optimal del grid basado en cantidad de clientes
	 */
	private int determineOptimalGridSize(int totalClientes) {
		// Para <= 50 clientes: grid 2x2 = 4 cuadrantes
		if (totalClientes <= 50) {
			return 2;
		}
		// Para 51-150 clientes: grid 3x3 = 9 cuadrantes
		if (totalClientes <= 150) {
			return 3;
		}
		// Para > 150 clientes: grid 4x4 = 16 cuadrantes
		return 4;
	}

	/**
	 * Optimiza la ruta recorriendo cada cluster en orden y optimizando dentro de cada uno
	 * Resultado: Base → [Cluster 1 optimizado] → [Cluster 2 optimizado] → ... → Base
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> optimizeRouteWithClusters(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) throws Exception {

		ArrayList<RouteOptimizerService.RutaClienteData> rutaCompleta = new ArrayList<>();
		RouteOptimizerService optimizer = new RouteOptimizerService();
		int orden = 1;

		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "OPTIMIZANDO RUTA CON CLUSTERING");
		Log.i(TAG, "Total clusters: " + clusters.size());

		// Recorrer cada cluster en orden
		for (int i = 0; i < clusters.size(); i++) {
			GeoClusteringService.GeoCluster cluster = clusters.get(i);
			Log.i(TAG, "");
			Log.i(TAG, "Procesando Cluster " + (i + 1) + " de " + clusters.size());
			Log.i(TAG, "  Clientes: " + cluster.size());
			Log.i(TAG, "  Centro: (" + String.format("%.4f", cluster.centerLat) + ", " +
					String.format("%.4f", cluster.centerLon) + ")");

			// Optimizar clientes dentro de este cluster
			ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster = optimizer.optimizeRoute(
				cluster.clientes,
				baseLocation
			);

			if (rutaCluster != null && !rutaCluster.isEmpty()) {
				// Ajustar números de orden para que sean secuenciales en la ruta global
				for (RouteOptimizerService.RutaClienteData ruta : rutaCluster) {
					ruta.orden = orden++;
					rutaCompleta.add(ruta);
				}
				Log.i(TAG, "  ✓ Cluster " + (i + 1) + " optimizado: " + rutaCluster.size() + " clientes");
			} else {
				Log.w(TAG, "  ✗ No se pudo optimizar cluster " + (i + 1));
			}
		}

		// Resumen final
		Log.i(TAG, "");
		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "║ RUTA FINAL OPTIMIZADA POR CLUSTERS   ║");
		Log.i(TAG, "║ Total clientes: " + rutaCompleta.size() + "                       ║");

		double distanciaTotalCompleta = 0;
		for (RouteOptimizerService.RutaClienteData ruta : rutaCompleta) {
			String distStr = ruta.distanciaKm.replace(" km", "");
			try {
				distanciaTotalCompleta += Double.parseDouble(distStr);
			} catch (NumberFormatException e) {
				// Ignorar
			}
		}
		Log.i(TAG, "║ Distancia total: " + String.format("%.1f km", distanciaTotalCompleta) + "             ║");
		Log.i(TAG, "════════════════════════════════════════");

		return rutaCompleta;
	}

	/**
	 * Obtiene los clientes activos que tienen coordenadas
	 * NO ordena por nombre para permitir que el algoritmo de clustering y optimización funcione correctamente
	 *
	 * @param app Configuración de la aplicación
	 * @return Lista de clientes activos con coordenadas
	 * @throws Exception Si hay error en la base de datos
	 */
	private ArrayList<Cliente> getClientesActivosConCoordenadas(AppConfig app) throws Exception {
		ArrayList<Cliente> clientesActivos = new ArrayList<>();

		try {
			// Obtener clientes activos CON coordenadas válidas (> 0)
			// NO ORDENAR por nombre, dejar que clustering y optimización ordenen
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT * FROM Clientes WHERE Activo = 1 AND Latitud IS NOT NULL AND Latitud != 0 AND Longitud IS NOT NULL AND Longitud != 0");

			Log.d(TAG, "Buscando clientes activos con coordenadas válidas");

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();

				do {
					Cliente c = Factory.build(Cliente.class, app);
					c.IdCliente = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdCliente")));
					c.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
					c.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
					c.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
					c.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
					c.Direccion1 = cursor.getString(cursor.getColumnIndex("Direccion1"));
					c.Direccion2 = cursor.getString(cursor.getColumnIndex("Direccion2"));
					c.Poblacion = cursor.getString(cursor.getColumnIndex("Poblacion"));
					c.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));
					c.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));
					c.Latitud = cursor.getDouble(cursor.getColumnIndex("Latitud"));
					c.Longitud = cursor.getDouble(cursor.getColumnIndex("Longitud"));

					clientesActivos.add(c);
					Log.d(TAG, "Cliente cargado: " + c.Nombre + " (" + c.Latitud + ", " + c.Longitud + ")");

				} while (cursor.moveToNext());

				cursor.close();
			} else {
				Log.w(TAG, "No se encontró cursor o está vacío");
				if (cursor != null) {
					Log.w(TAG, "Cursor vacío - Count: " + cursor.getCount());
					cursor.close();
				}
			}

			Log.i(TAG, "Total de clientes activos con coordenadas: " + clientesActivos.size());

		} catch (Exception e) {
			Log.e(TAG, "Error obteniendo clientes activos: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

		return clientesActivos;
	}

	/**
	 * Guarda la ruta optimizada en la base de datos
	 *
	 * @param app Configuración de la aplicación
	 * @param rutaOrdenada Lista de clientes en orden optimizado
	 * @param ciudadBase Ciudad base del vendedor
	 * @throws Exception Si hay error en la base de datos
	 */
	private void saveRoute(AppConfig app, ArrayList<RouteOptimizerService.RutaClienteData> rutaOrdenada, String ciudadBase) throws Exception {
		try {
			RutaGenerada rutaGenerada = Factory.build(RutaGenerada.class, app);

			// Eliminar rutas anteriores de esta semana
			rutaGenerada.deleteCurrentWeekRoutes();

			// Log de debugging
			Log.i(TAG, "Guardando ruta con " + rutaOrdenada.size() + " clientes");

			// Guardar cada cliente de la ruta
			for (RouteOptimizerService.RutaClienteData rutaCliente : rutaOrdenada) {
				// Buscar cliente por código en la BD
				Cliente cliente = findClienteByCodigo(app, rutaCliente.codigoCliente);

				if (cliente != null) {
					RutaGenerada ruta = Factory.build(RutaGenerada.class, app);
					ruta.FechaGeneracion = new Date();
					ruta.OrdenVisita = rutaCliente.orden;
					ruta.CodigoCliente = cliente.CodigoCliente;
					ruta.NombreCliente = cliente.Nombre;
					ruta.DireccionCliente = cliente.Direccion1;
					ruta.PoblacionCliente = cliente.Poblacion;
					ruta.ProvinciaCliente = cliente.Provincia;
					ruta.DistanciaEstimada = rutaCliente.distanciaKm;
					ruta.CiudadBase = ciudadBase;

					ruta.save();

					Log.d(TAG, "SaveRoute - Orden: " + rutaCliente.orden + ", Cliente: " + cliente.Nombre + ", Distancia: " + rutaCliente.distanciaKm);
				} else {
					Log.w(TAG, "Cliente no encontrado: " + rutaCliente.codigoCliente);
				}
			}

			Log.i(TAG, "Ruta completa guardada en BD - Total: " + rutaOrdenada.size());

		} catch (Exception e) {
			Log.e(TAG, "Error guardando ruta: " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Busca un cliente por su código en la base de datos
	 *
	 * @param app Configuración de la aplicación
	 * @param codigoCliente Código único del cliente
	 * @return Objeto Cliente si lo encuentra, null en caso contrario
	 * @throws Exception Si hay error en la base de datos
	 */
	private Cliente findClienteByCodigo(AppConfig app, String codigoCliente) throws Exception {
		try {
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT * FROM Clientes WHERE CodigoCliente = '" + codigoCliente + "' AND Activo = 1 LIMIT 1");

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();

				Cliente cliente = Factory.build(Cliente.class, app);
				cliente.IdCliente = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdCliente")));
				cliente.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				cliente.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
				cliente.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
				cliente.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
				cliente.Direccion1 = cursor.getString(cursor.getColumnIndex("Direccion1"));
				cliente.Direccion2 = cursor.getString(cursor.getColumnIndex("Direccion2"));
				cliente.Poblacion = cursor.getString(cursor.getColumnIndex("Poblacion"));
				cliente.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));
				cliente.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));

				cursor.close();
				return cliente;
			}

			if (cursor != null) {
				cursor.close();
			}

		} catch (Exception e) {
			Log.e(TAG, "Error buscando cliente por código: " + e.getMessage());
		}

		return null;
	}
}
