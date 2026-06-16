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
import java.util.HashSet;
import java.util.Set;

/**
 * Servicio para generar rutas optimizadas usando OpenRouteService
 * Utiliza clustering geográfico para dividir clientes por zonas
 * y luego optimiza cada zona con TSP Nearest Neighbor
 */
public class RouteGeneratorService {

	private static final String TAG = "RouteGeneratorService";

	/**
	 * Genera una ruta optimizada para los clientes activos del vendedor usando Google Maps Routes API
	 * Usa clustering geográfico para agrupar clientes por zonas y Google Maps para optimizar cada cluster
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
			// Deduplicar clientes (puede haber duplicados por mismo CodigoCliente)
			clientesActivos = deduplicarClientes(clientesActivos);

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

			// 4. Optimizar ruta considerando clusters (usando ORS)
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
	 * Genera una ruta de forma asíncrona usando VROOM + ORS
	 * @param context Contexto de la aplicación
	 * @param ciudadBase Ciudad base
	 * @param callback Callback para recibir resultados
	 */
	public void generateRouteAsync(Context context, String ciudadBase, RouteGenerationCallback callback) {
		new Thread(() -> {
			try {
				AppConfig app = (AppConfig) context.getApplicationContext();

				callback.onProgress("Obteniendo clientes activos...");
				ArrayList<Cliente> clientesActivos = getClientesActivosConCoordenadas(app);

				if (clientesActivos == null || clientesActivos.isEmpty()) {
					callback.onError("No hay clientes activos con coordenadas");
					return;
				}

				clientesActivos = deduplicarClientes(clientesActivos);

				if (clientesActivos.size() < 2) {
					callback.onError("Mínimo 2 clientes requeridos");
					return;
				}

				callback.onProgress("Geocodificando ciudad base...");
				CiudadVendedor ciudad = Factory.build(CiudadVendedor.class, app);
				ciudad.load();

				IGeocodingStrategy geocodingStrategy = new OpenRouteServiceGeocodingStrategy();
				LatLng baseLocation = geocodingStrategy.geocodeAddress(
					ciudadBase, null, null, ciudad.CodigoPostal
				);

				if (baseLocation == null) {
					callback.onError("No se pudo geocodificar ciudad base");
					return;
				}

				callback.onProgress("Agrupando clientes por zonas...");
				GeoClusteringService clusteringService = new GeoClusteringService();
				int gridSize = determineOptimalGridSize(clientesActivos.size());
				ArrayList<GeoClusteringService.GeoCluster> clusters = clusteringService.clusterizeClients(
					clientesActivos, gridSize, baseLocation
				);

				if (clusters == null || clusters.isEmpty()) {
					callback.onError("Error creando clusters");
					return;
				}

				callback.onProgress("Optimizando rutas con VROOM...");
				ArrayList<RouteOptimizerService.RutaClienteData> rutaOrdenada = optimizeRouteWithVROOM(
					clusters, baseLocation
				);

				if (rutaOrdenada == null || rutaOrdenada.isEmpty()) {
					callback.onError("Error optimizando ruta");
					return;
				}

				callback.onProgress("Guardando ruta en BD...");
				saveRoute(app, rutaOrdenada, ciudadBase);

				Log.i(TAG, "Ruta generada exitosamente con VROOM");
				callback.onRouteGenerated();

			} catch (Exception e) {
				Log.e(TAG, "Error en generateRouteAsync: " + e.getMessage());
				callback.onError("Error: " + e.getMessage());
			}
		}).start();
	}

	/**
	 * Optimiza ruta usando VROOM + ORS
	 * - Todos los clusters: ORS Matrix + VROOM optimization
	 * - Separa clientes con coordenadas inválidas y los pone al final
	 * - REORDENA clusters por proximidad para minimizar saltos entre ellos
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> optimizeRouteWithVROOM(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) throws Exception {

		ArrayList<RouteOptimizerService.RutaClienteData> rutaCompleta = new ArrayList<>();
		ArrayList<Cliente> clientesInvalidos = new ArrayList<>();
		int ordenGlobal = 1;

		Log.d(TAG, "════════════════════════════════════════");
		Log.d(TAG, "OPTIMIZANDO CON VROOM + ORS");
		Log.d(TAG, "Total clusters: " + clusters.size());
		Log.d(TAG, "Base location: " + baseLocation.getLatitude() + ", " + baseLocation.getLongitude());

		// REORDENAR clusters por proximidad secuencial (vecino más cercano)
		clusters = orderClustersByProximity(clusters, baseLocation);

		for (GeoClusteringService.GeoCluster cluster : clusters) {
			// Separar clientes válidos e inválidos
			ArrayList<Cliente> clientesValidos = new ArrayList<>();
			ArrayList<Cliente> clientesInvalidosCluster = new ArrayList<>();

			Log.i(TAG, "Procesando cluster con " + cluster.clientes.size() + " clientes");

			for (Cliente cliente : cluster.clientes) {
				if (isValidCoordinate(cliente.Latitud, cliente.Longitud)) {
					clientesValidos.add(cliente);
				} else {
					clientesInvalidosCluster.add(cliente);
					Log.w(TAG, "    ✗ INVÁLIDO: " + cliente.Nombre + " (" + cliente.Latitud + ", " + cliente.Longitud + ")");
				}
			}

			// Agregar inválidos a la lista final para procesar después
			clientesInvalidos.addAll(clientesInvalidosCluster);

			if (clientesValidos.isEmpty()) {
				Log.w(TAG, "Cluster sin clientes válidos, saltando");
				continue;
			}

			Log.d(TAG, "Procesando cluster: " + clientesValidos.size() + " válidos, " + clientesInvalidosCluster.size() + " inválidos");
			Log.i(TAG, "► USANDO VROOM + ORS (cluster: " + clientesValidos.size() + " clientes)");

			// Usar VROOM para optimizar todos los clusters
			ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster = optimizeClusterWithVROOM(clientesValidos, baseLocation);

			// Agregar clientes optimizados con el orden global correcto
			if (rutaCluster != null && !rutaCluster.isEmpty()) {
				for (RouteOptimizerService.RutaClienteData rutaCliente : rutaCluster) {
					rutaCliente.orden = ordenGlobal++;
					rutaCompleta.add(rutaCliente);
				}
			} else {
				Log.w(TAG, "No se pudo optimizar cluster");
			}
		}

		// Agregar clientes con coordenadas inválidas al final
		if (!clientesInvalidos.isEmpty()) {
			Log.w(TAG, "");
			Log.w(TAG, "════ CLIENTES CON COORDENADAS INVÁLIDAS ════");
			Log.w(TAG, "Agregando " + clientesInvalidos.size() + " clientes al final");
			for (Cliente cliente : clientesInvalidos) {
				RouteOptimizerService.RutaClienteData rutaCliente = new RouteOptimizerService.RutaClienteData();
				rutaCliente.orden = ordenGlobal++;
				rutaCliente.codigoCliente = cliente.CodigoCliente;
				rutaCliente.nombre = cliente.Nombre + " (COORDENADAS INVÁLIDAS)";
				rutaCliente.distanciaKm = "N/A";
				rutaCompleta.add(rutaCliente);
				Log.w(TAG, "  - " + cliente.Nombre + " (" + cliente.Latitud + ", " + cliente.Longitud + ")");
			}
		}

		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "║  RUTA FINAL OPTIMIZADA CON VROOM        ║");
		Log.i(TAG, "║  Total clientes: " + rutaCompleta.size() + "                       ║");
		Log.i(TAG, "════════════════════════════════════════");

		return rutaCompleta;
	}

	/**
	 * Llamada síncrona a Google Maps
	 * Usa el método sincrónico de GoogleMapsRouteOptimizer
	 */
	/**
	 * Optimiza un cluster usando VROOM + ORS Matrix
	 * VROOM: Vehicle Routing Open-source Optimization Machine
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> optimizeClusterWithVROOM(
			ArrayList<Cliente> clientesValidos,
			LatLng baseLocation) throws Exception {

		ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster = new ArrayList<>();
		RouteOptimizerService optimizer = new RouteOptimizerService();

		Log.i(TAG, "Iniciando optimización VROOM+ORS para " + clientesValidos.size() + " clientes");

		ArrayList<RouteOptimizerService.RutaClienteData> rutaOptimizada = optimizer.optimizeRoute(
			clientesValidos,
			baseLocation
		);

		if (rutaOptimizada != null && !rutaOptimizada.isEmpty()) {
			for (RouteOptimizerService.RutaClienteData ruta : rutaOptimizada) {
				rutaCluster.add(ruta);
				Log.d(TAG, "  TSP: " + ruta.nombre + " -> " + ruta.latitud + ", " + ruta.longitud + " (" + ruta.distanciaKm + ")");
			}
		} else {
			Log.w(TAG, "No se pudo optimizar cluster con TSP");
		}

		return rutaCluster;
	}

	/**
	 * Reordena clusters usando algoritmo de vecino más cercano
	 * Minimiza los saltos entre clusters visitando los más cercanos secuencialmente
	 *
	 * @param clusters Lista original de clusters
	 * @param baseLocation Ubicación de la base
	 * @return Clusters reordenados por proximidad secuencial
	 */
	private ArrayList<GeoClusteringService.GeoCluster> orderClustersByProximity(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) {

		if (clusters == null || clusters.size() <= 1) {
			return clusters;
		}

		ArrayList<GeoClusteringService.GeoCluster> orderedClusters = new ArrayList<>();
		ArrayList<Integer> visitedIndexes = new ArrayList<>();

		Log.d(TAG, "════════════════════════════════════════");
		Log.d(TAG, "REORDENANDO CLUSTERS POR PROXIMIDAD");
		Log.d(TAG, "Total clusters a reordenar: " + clusters.size());

		// 1. Empezar con el cluster más cercano a la base
		int currentIdx = 0;
		double minDistToBase = Double.MAX_VALUE;
		for (int i = 0; i < clusters.size(); i++) {
			double distToBase = clusters.get(i).distanceToPoint(
					baseLocation.getLatitude(),
					baseLocation.getLongitude()
			);
			if (distToBase < minDistToBase) {
				minDistToBase = distToBase;
				currentIdx = i;
			}
		}

		// Agregar primer cluster
		orderedClusters.add(clusters.get(currentIdx));
		visitedIndexes.add(currentIdx);
		Log.d(TAG, "[1] Cluster inicial (más cercano a base): Cluster " +
				clusters.get(currentIdx).clusterId +
				" (" + clusters.get(currentIdx).size() + " clientes)");

		// 2. Algoritmo del vecino más cercano para clusters restantes
		double lastLat = clusters.get(currentIdx).centerLat;
		double lastLon = clusters.get(currentIdx).centerLon;

		while (visitedIndexes.size() < clusters.size()) {
			int nearestIdx = -1;
			double minDist = Double.MAX_VALUE;

			// Buscar cluster no visitado más cercano al último
			for (int i = 0; i < clusters.size(); i++) {
				if (visitedIndexes.contains(i)) {
					continue; // Ya visitado
				}

				GeoClusteringService.GeoCluster candidate = clusters.get(i);
				double dist = calculateDistance(
						lastLat, lastLon,
						candidate.centerLat, candidate.centerLon
				);

				if (dist < minDist) {
					minDist = dist;
					nearestIdx = i;
				}
			}

			if (nearestIdx == -1) {
				break; // No hay más clusters
			}

			// Agregar cluster más cercano
			orderedClusters.add(clusters.get(nearestIdx));
			visitedIndexes.add(nearestIdx);

			GeoClusteringService.GeoCluster nextCluster = clusters.get(nearestIdx);
			Log.d(TAG, "[" + (visitedIndexes.size()) + "] Cluster " + nextCluster.clusterId +
					" (" + nextCluster.size() + " clientes) - Distancia: " +
					String.format("%.1f km", minDist / 1000.0));

			// Actualizar posición para próxima iteración
			lastLat = nextCluster.centerLat;
			lastLon = nextCluster.centerLon;
		}

		Log.d(TAG, "════════════════════════════════════════");

		return orderedClusters;
	}

	/**
	 * Calcula distancia Haversine entre dos puntos en km
	 */
	private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		return GeoClusteringService.calculateHaversineDistance(lat1, lon1, lat2, lon2);
	}

	/**
	 * Determina el tamaño optimal del grid basado en cantidad de clientes
	 */
	private int determineOptimalGridSize(int totalClientes) {
		// Estrategia: máximo ~15-20 clientes por cluster
		// Para que Google Maps pueda manejar bien cada cluster

		Log.i(TAG, "DEBUG: determineOptimalGridSize recibió totalClientes = " + totalClientes);

		// Para <= 50 clientes: grid 2x2 = 4 cuadrantes (12-13 clientes/cluster)
		if (totalClientes <= 50) {
			Log.i(TAG, "DEBUG: Retornando gridSize=2 para " + totalClientes + " clientes");
			return 2;
		}
		// Para 51-150 clientes: grid 3x3 = 9 cuadrantes (16-17 clientes/cluster)
		if (totalClientes <= 150) {
			Log.i(TAG, "DEBUG: Retornando gridSize=3 para " + totalClientes + " clientes");
			return 3;
		}
		// Para 151-300 clientes: grid 5x5 = 25 cuadrantes (12 clientes/cluster)
		if (totalClientes <= 300) {
			Log.i(TAG, "DEBUG: Retornando gridSize=5 para " + totalClientes + " clientes");
			return 5;
		}
		// Para 301-500 clientes: grid 6x6 = 36 cuadrantes (13-14 clientes/cluster)
		if (totalClientes <= 500) {
			Log.i(TAG, "DEBUG: Retornando gridSize=6 para " + totalClientes + " clientes");
			return 6;
		}
		// Para > 500 clientes: grid 7x7 = 49 cuadrantes (10-15 clientes/cluster)
		Log.i(TAG, "DEBUG: Retornando gridSize=7 para " + totalClientes + " clientes (>500)");
		return 7;
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
	 * Obtiene los clientes activos y geocodifica automáticamente los que no tienen coordenadas
	 * Utiliza OpenRouteService para geocodificar direcciones
	 *
	 * @param app Configuración de la aplicación
	 * @return Lista de clientes activos con coordenadas (geocodificadas si era necesario)
	 * @throws Exception Si hay error en la base de datos o geocodificación
	 */
	private ArrayList<Cliente> getClientesActivosConCoordenadas(AppConfig app) throws Exception {
		ArrayList<Cliente> clientesActivos = new ArrayList<>();
		ArrayList<Cliente> clientesParaGeocoding = new ArrayList<>();

		try {
			// Obtener TODOS los clientes activos (con o sin coordenadas)
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT * FROM Clientes WHERE Activo = 1");

			Log.d(TAG, "Buscando todos los clientes activos...");

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

					// Verificar si tiene coordenadas válidas
					if (c.Latitud != null && c.Latitud != 0 && c.Longitud != null && c.Longitud != 0) {
						clientesActivos.add(c);
						Log.d(TAG, "✓ Cliente con coordenadas: " + c.Nombre + " (" + c.Latitud + ", " + c.Longitud + ")");
					} else {
						// Guardar para geocodificar después
						clientesParaGeocoding.add(c);
						Log.w(TAG, "⚠ Cliente SIN coordenadas: " + c.Nombre + " - será geocodificado");
					}

				} while (cursor.moveToNext());

				cursor.close();
			} else {
				Log.w(TAG, "No se encontró cursor o está vacío");
				if (cursor != null) {
					Log.w(TAG, "Cursor vacío - Count: " + cursor.getCount());
					cursor.close();
				}
			}

			// Geocodificar clientes sin coordenadas
			if (!clientesParaGeocoding.isEmpty()) {
				Log.i(TAG, "");
				Log.i(TAG, "════════════════════════════════════════");
				Log.i(TAG, "GEOCODIFICANDO " + clientesParaGeocoding.size() + " CLIENTES CON ORS");
				Log.i(TAG, "════════════════════════════════════════");

				IGeocodingStrategy geocodingStrategy = new OpenRouteServiceGeocodingStrategy();

				for (Cliente cliente : clientesParaGeocoding) {
					try {
						// Geocodificar usando dirección + población + código postal
						LatLng coordenadas = geocodingStrategy.geocodeAddress(
							cliente.Direccion1,
							cliente.Poblacion,
							cliente.Provincia,
							cliente.CodigoPostal
						);

						if (coordenadas != null && coordenadas.getLatitude() != null && coordenadas.getLongitude() != null) {
							cliente.Latitud = coordenadas.getLatitude();
							cliente.Longitud = coordenadas.getLongitude();
							clientesActivos.add(cliente);

							Log.i(TAG, "✓ Geocodificado: " + cliente.Nombre + " -> (" + cliente.Latitud + ", " + cliente.Longitud + ")");

							// Guardar coordenadas en la BD para futuras búsquedas
							try {
								String updateSQL = "UPDATE Clientes SET Latitud = " + cliente.Latitud +
													", Longitud = " + cliente.Longitud +
													" WHERE CodigoCliente = '" + cliente.CodigoCliente + "'";
								app.getDatabaseOperations().executeSentence(updateSQL);
								Log.d(TAG, "Coordenadas guardadas en BD para: " + cliente.Nombre);
							} catch (Exception e) {
								Log.w(TAG, "No se pudieron guardar coordenadas en BD: " + e.getMessage());
							}
						} else {
							Log.w(TAG, "✗ No se pudo geocodificar: " + cliente.Nombre);
						}
					} catch (Exception e) {
						Log.w(TAG, "Error geocodificando " + cliente.Nombre + ": " + e.getMessage());
					}

					// Rate limiting: esperar 100ms entre requests para no saturar ORS
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			Log.i(TAG, "");
			Log.i(TAG, "Total de clientes activos CON coordenadas: " + clientesActivos.size());

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

					// Geolocalización
					ruta.GeolocalizationStatus = rutaCliente.geolocalizationStatus;
					ruta.Latitud = rutaCliente.latitud;
					ruta.Longitud = rutaCliente.longitud;

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

	/**
	 * Deduplica clientes por CodigoCliente Y por coordenadas idénticas
	 * Si el mismo CodigoCliente aparece múltiples veces, mantiene solo la última instancia
	 * Si múltiples clientes tienen exactamente las mismas coordenadas, mantiene solo el primero
	 * También cuenta clientes con coordenadas inválidas (fuera de España)
	 *
	 * @param clientes Lista de clientes que puede contener duplicados
	 * @return Lista con duplicados removidos
	 */
	private ArrayList<Cliente> deduplicarClientes(ArrayList<Cliente> clientes) {
		if (clientes == null || clientes.isEmpty()) {
			return clientes;
		}

		int totalAntes = clientes.size();
		int coordinadasInvalidas = 0;
		int duplicadosPorCoordenadas = 0;

		// Paso 1: Deduplica por CodigoCliente (mantiene el último)
		java.util.LinkedHashMap<String, Cliente> clientesUnicos = new java.util.LinkedHashMap<>();

		for (Cliente cliente : clientes) {
			if (cliente.CodigoCliente != null && !cliente.CodigoCliente.isEmpty()) {
				// Contar coordenadas inválidas
				if (!isValidCoordinate(cliente.Latitud, cliente.Longitud)) {
					coordinadasInvalidas++;
				}
				// Si ya existe este código, será sobrescrito por la nueva ocurrencia (la última)
				clientesUnicos.put(cliente.CodigoCliente, cliente);
			}
		}

		// Paso 2: Deduplica por coordenadas idénticas (mantiene el primero)
		ArrayList<Cliente> resultado = new ArrayList<>();
		java.util.HashSet<String> coordinatasVistas = new java.util.HashSet<>();

		for (Cliente cliente : clientesUnicos.values()) {
			// Crear clave única para las coordenadas
			String coordKey = String.format("%.6f,%.6f", cliente.Latitud, cliente.Longitud);

			if (!coordinatasVistas.contains(coordKey)) {
				resultado.add(cliente);
				coordinatasVistas.add(coordKey);
			} else {
				duplicadosPorCoordenadas++;
				Log.w(TAG, "Cliente duplicado removido por coordenadas idénticas: " + cliente.Nombre +
					" (" + cliente.Latitud + ", " + cliente.Longitud + ")");
			}
		}

		int totalDespues = resultado.size();
		int duplicadosRemovidos = totalAntes - totalDespues;

		Log.i(TAG, "╔══════════════════════════════════════╗");
		Log.i(TAG, "║  DEDUPLICACIÓN DE CLIENTES          ║");
		Log.i(TAG, "║  Clientes antes: " + totalAntes + "                 ║");
		if (duplicadosRemovidos > 0) {
			Log.i(TAG, "║  Duplicados removidos: " + duplicadosRemovidos + "            ║");
		}
		if (duplicadosPorCoordenadas > 0) {
			Log.w(TAG, "║  Dup. por coordenadas: " + duplicadosPorCoordenadas + "           ║");
		}
		if (coordinadasInvalidas > 0) {
			Log.w(TAG, "║  COORDENADAS INVÁLIDAS: " + coordinadasInvalidas + "          ║");
		}
		Log.i(TAG, "║  Clientes únicos: " + totalDespues + "                 ║");
		Log.i(TAG, "╚══════════════════════════════════════╝");

		return resultado;
	}

	/**
	 * Valida si unas coordenadas corresponden a España (con margen de error)
	 * España: Latitud 35°N - 43°N, Longitud -10°W a 4°E
	 * Con margen extra para islas y GPS impreciso: 34°N - 44°N, -11°W a 5°E
	 * @param latitud Latitud a validar
	 * @param longitud Longitud a validar
	 * @return true si las coordenadas están dentro de España, false en caso contrario
	 */
	private boolean isValidCoordinate(double latitud, double longitud) {
		// Aceptar cualquier coordenada geográfica válida mundialmente
		boolean esValida = latitud >= -90 && latitud <= 90 &&
		                   longitud >= -180 && longitud <= 180;

		if (!esValida) {
			Log.w(TAG, "Coordenadas inválidas: lat=" + latitud + ", lng=" + longitud);
		}

		return esValida;
	}
}
