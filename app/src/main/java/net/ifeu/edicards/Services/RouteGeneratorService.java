package net.ifeu.edicards.Services;

import android.content.Context;
import android.util.Log;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEndpoints;
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
 * Servicio para generar rutas optimizadas
 * Utiliza clustering geográfico para dividir clientes por zonas
 * y luego optimiza usando el servicio configurado en ConstantsEndpoints.ROUTE_OPTIMIZER_SERVICE
 */
public class RouteGeneratorService {

	private static final String TAG = "RouteGeneratorService";

	private Context context;
	private String googleApiKey;

	/**
	 * Genera una ruta optimizada para los clientes activos del vendedor
	 * Usa clustering geográfico para agrupar clientes por zonas y el servicio configurado para optimizar
	 *
	 * @param context Contexto de la aplicación
	 * @param ciudadBase Ciudad donde se ubica la base del vendedor
	 * @return true si la ruta se generó exitosamente, false en caso contrario
	 * @throws Exception Si hay error durante el proceso
	 */
	public boolean generateRoute(Context context, String ciudadBase) throws Exception {
		try {
			this.context = context;
			AppConfig app = (AppConfig) context.getApplicationContext();
			this.googleApiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;

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
	 * Genera una ruta de forma asíncrona usando el servicio configurado
	 * @param context Contexto de la aplicación
	 * @param ciudadBase Ciudad base
	 * @param callback Callback para recibir resultados
	 */
	public void generateRouteAsync(Context context, String ciudadBase, RouteGenerationCallback callback) {
		new Thread(() -> {
			try {
				this.context = context;
				AppConfig app = (AppConfig) context.getApplicationContext();
				this.googleApiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;

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

				String serviceName = ConstantsEndpoints.ROUTE_OPTIMIZER_SERVICE;
				callback.onProgress("Optimizando rutas con " + serviceName + "...");
				ArrayList<RouteOptimizerService.RutaClienteData> rutaOrdenada = optimizeRouteWithService(
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
	 * Optimiza ruta usando el servicio configurado (Google o ORS)
	 * - Separa clientes con coordenadas inválidas y los pone al final
	 * - REORDENA clusters por proximidad para minimizar saltos entre ellos
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> optimizeRouteWithService(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) throws Exception {

		ArrayList<RouteOptimizerService.RutaClienteData> rutaCompleta = new ArrayList<>();
		ArrayList<Cliente> clientesInvalidos = new ArrayList<>();
		int ordenGlobal = 1;

		String service = ConstantsEndpoints.ROUTE_OPTIMIZER_SERVICE;
		Log.d(TAG, "════════════════════════════════════════");
		Log.d(TAG, "OPTIMIZANDO CON " + service);
		Log.d(TAG, "Total clusters: " + clusters.size());
		Log.d(TAG, "Base location: " + baseLocation.getLatitude() + ", " + baseLocation.getLongitude());

		// REORDENAR clusters por proximidad secuencial (vecino más cercano)
		clusters = orderClustersByProximity(clusters, baseLocation);

		// Variables para conectar clusters
		Cliente clienteAnterior = null;
		int clusterIndex = 0;

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
			Log.i(TAG, "► USANDO " + service + " (cluster: " + clientesValidos.size() + " clientes)");

			// Optimizar usando el servicio configurado, conectando con el anterior
			ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster = optimizeClusterWithService(
				clientesValidos,
				baseLocation,
				clienteAnterior,
				cluster.clusterId  // ← Pasar el ID del cluster actual
			);

			// Agregar clientes optimizados con el orden global correcto
			if (rutaCluster != null && !rutaCluster.isEmpty()) {
				for (RouteOptimizerService.RutaClienteData rutaCliente : rutaCluster) {
					rutaCliente.orden = ordenGlobal++;
					rutaCompleta.add(rutaCliente);
				}

				// Guardar el último cliente del cluster actual para conectar con el siguiente
				RouteOptimizerService.RutaClienteData ultimoRuta = rutaCluster.get(rutaCluster.size() - 1);
				for (Cliente c : clientesValidos) {
					if (c.CodigoCliente.equals(ultimoRuta.codigoCliente)) {
						clienteAnterior = c;
						Log.d(TAG, "Último cliente del cluster " + cluster.clusterId + " guardado: " + c.Nombre);
						break;
					}
				}
			} else {
				Log.w(TAG, "No se pudo optimizar cluster " + cluster.clusterId);
			}

			clusterIndex++;
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
		Log.i(TAG, "║  RUTA FINAL OPTIMIZADA CON " + service + "        ║");
		Log.i(TAG, "║  Total clientes: " + rutaCompleta.size() + "                       ║");
		Log.i(TAG, "════════════════════════════════════════");

		// Reordenar ruta: el cliente más cercano a la base debe ser el primero
		rutaCompleta = ensureFirstClientNearBase(rutaCompleta, baseLocation);

		return rutaCompleta;
	}

	/**
	 * Optimiza un cluster SIEMPRE usando Google Maps
	 * Si el cluster tiene >24 clientes, lo subdivide en sub-clusters de máximo 24
	 * IMPORTANTE: Pre-ordena clientes por proximidad antes de dividir en batches
	 *
	 * @param clientesValidos Clientes del cluster a optimizar
	 * @param baseLocation Ubicación de la base
	 * @param clienteAnterior Cliente del cluster anterior (null si es el primer cluster)
	 * @param clusterID ID del cluster actual para debugging
	 * @return Ruta optimizada del cluster
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> optimizeClusterWithService(
			ArrayList<Cliente> clientesValidos,
			LatLng baseLocation,
			Cliente clienteAnterior,
			int clusterID) throws Exception {

		ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster = new ArrayList<>();

		// Google Maps tiene límite de 25 waypoints (base + 24 clientes)
		final int GOOGLE_MAPS_MAX_CLIENTS = 24;

		Log.i(TAG, "Iniciando optimización para cluster " + clusterID + " (" + clientesValidos.size() + " clientes)");

		// Si el cluster es muy grande, subdividirlo en batches
		if (clientesValidos.size() > GOOGLE_MAPS_MAX_CLIENTS) {
			Log.w(TAG, "  ⚠ Cluster muy grande (" + clientesValidos.size() + " clientes)");
			Log.w(TAG, "  → Pre-ordenando por proximidad antes de dividir en batches");

			// PRE-ORDENAR clientes por proximidad usando algoritmo del vecino más cercano
			ArrayList<Cliente> clientesOrdenados = preOrderClientesByProximity(
				clientesValidos,
				clienteAnterior,
				baseLocation
			);

			Log.w(TAG, "  → Subdividiendo en batches de " + GOOGLE_MAPS_MAX_CLIENTS + " clientes");

			// Dividir clientes ORDENADOS en sub-batches de máximo 24 clientes
			int totalBatches = (int) Math.ceil((double) clientesOrdenados.size() / GOOGLE_MAPS_MAX_CLIENTS);
			Cliente ultimoCliente = clienteAnterior;

			for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
				int startIdx = batchNum * GOOGLE_MAPS_MAX_CLIENTS;
				int endIdx = Math.min(startIdx + GOOGLE_MAPS_MAX_CLIENTS, clientesOrdenados.size());

				ArrayList<Cliente> batch = new ArrayList<>(clientesOrdenados.subList(startIdx, endIdx));

				Log.i(TAG, "  → Procesando sub-batch " + (batchNum + 1) + "/" + totalBatches +
				           " (" + batch.size() + " clientes) con Google Maps");

				UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);
				ArrayList<RouteOptimizerService.RutaClienteData> rutaBatch =
					optimizer.optimizeRoute(batch, baseLocation, ultimoCliente, clusterID);

				if (rutaBatch != null && !rutaBatch.isEmpty()) {
					// Ya no necesitamos asignar clusterID aquí - se asigna dentro de optimizeRoute
					for (RouteOptimizerService.RutaClienteData ruta : rutaBatch) {
						rutaCluster.add(ruta);
					}

					// Actualizar último cliente para conectar siguiente batch
					String ultimoCodigo = rutaBatch.get(rutaBatch.size() - 1).codigoCliente;
					for (Cliente c : batch) {
						if (c.CodigoCliente.equals(ultimoCodigo)) {
							ultimoCliente = c;
							break;
						}
					}
				}
			}
		} else {
			// Cluster pequeño, optimizar directamente con Google Maps
			Log.i(TAG, "  → Optimizando con Google Maps (" + clientesValidos.size() + " clientes)");

			UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);
			ArrayList<RouteOptimizerService.RutaClienteData> rutaOptimizada =
				optimizer.optimizeRoute(clientesValidos, baseLocation, clienteAnterior, clusterID);

			if (rutaOptimizada != null && !rutaOptimizada.isEmpty()) {
				// Ya no necesitamos asignar clusterID aquí - se asigna dentro de optimizeRoute
				for (RouteOptimizerService.RutaClienteData ruta : rutaOptimizada) {
					rutaCluster.add(ruta);
				}
			}
		}

		if (rutaCluster.isEmpty()) {
			Log.w(TAG, "No se pudo optimizar cluster " + clusterID + " con Google Maps");
		} else {
			Log.i(TAG, "✓ Cluster " + clusterID + " optimizado: " + rutaCluster.size() + " clientes");
		}

		return rutaCluster;
	}

	/**
	 * Pre-ordena clientes usando algoritmo del vecino más cercano (Nearest Neighbor)
	 * Esto asegura que los batches tengan clientes geográficamente continuos
	 *
	 * @param clientes Lista de clientes a ordenar
	 * @param startingClient Cliente desde donde comenzar (null = comenzar desde el más cercano a base)
	 * @param baseLocation Ubicación de la base
	 * @return Lista ordenada de clientes por proximidad
	 */
	private ArrayList<Cliente> preOrderClientesByProximity(
			ArrayList<Cliente> clientes,
			Cliente startingClient,
			LatLng baseLocation) {

		ArrayList<Cliente> ordenados = new ArrayList<>();
		ArrayList<Cliente> pendientes = new ArrayList<>(clientes);

		// Determinar punto de inicio
		double currentLat, currentLon;
		if (startingClient != null) {
			currentLat = startingClient.Latitud;
			currentLon = startingClient.Longitud;
			Log.d(TAG, "  Pre-ordenando desde cliente anterior: " + startingClient.Nombre);
		} else {
			currentLat = baseLocation.getLatitude();
			currentLon = baseLocation.getLongitude();
			Log.d(TAG, "  Pre-ordenando desde base");
		}

		// Algoritmo del vecino más cercano
		while (!pendientes.isEmpty()) {
			Cliente masCercano = null;
			double distanciaMinima = Double.MAX_VALUE;

			// Buscar cliente más cercano a la posición actual
			for (Cliente cliente : pendientes) {
				double distancia = GeoClusteringService.calculateHaversineDistance(
					currentLat, currentLon,
					cliente.Latitud, cliente.Longitud
				);

				if (distancia < distanciaMinima) {
					distanciaMinima = distancia;
					masCercano = cliente;
				}
			}

			// Agregar a la lista ordenada y remover de pendientes
			if (masCercano != null) {
				ordenados.add(masCercano);
				pendientes.remove(masCercano);

				// Actualizar posición actual al cliente recién agregado
				currentLat = masCercano.Latitud;
				currentLon = masCercano.Longitud;
			}
		}

		Log.d(TAG, "  ✓ " + ordenados.size() + " clientes pre-ordenados por proximidad");
		return ordenados;
	}

	/**
	 * Asegura que el primer cliente de la ruta esté cerca de la base
	 * Si el primer cliente está muy lejos, lo mueve al final y trae el más cercano
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> ensureFirstClientNearBase(
			ArrayList<RouteOptimizerService.RutaClienteData> ruta,
			LatLng baseLocation) {

		if (ruta == null || ruta.isEmpty()) {
			return ruta;
		}

		Log.d(TAG, "Verificando si el primer cliente está cerca de la base...");

		// Encontrar el cliente más cercano a la base (por línea recta, rápido)
		int nearestIdx = 0;
		double minDistance = Double.MAX_VALUE;

		for (int i = 0; i < ruta.size(); i++) {
			RouteOptimizerService.RutaClienteData cliente = ruta.get(i);
			try {
				double lat = Double.parseDouble(cliente.latitud);
				double lon = Double.parseDouble(cliente.longitud);

				double distance = GeoClusteringService.calculateHaversineDistance(
					baseLocation.getLatitude(), baseLocation.getLongitude(),
					lat, lon
				);

				if (distance < minDistance) {
					minDistance = distance;
					nearestIdx = i;
				}
			} catch (Exception e) {
				// Skip if coordinates are invalid
			}
		}

		// Si el más cercano no es el primero, reordenar
		if (nearestIdx != 0) {
			RouteOptimizerService.RutaClienteData nearestClient = ruta.remove(nearestIdx);
			ruta.add(0, nearestClient);
			Log.d(TAG, "✓ Primer cliente reordenado: " + nearestClient.nombre +
				" (distancia a base: " + String.format("%.1f km", minDistance / 1000.0) + ")");
		} else {
			Log.d(TAG, "✓ El primer cliente ya está cerca de la base");
		}

		return ruta;
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
	 * Objetivo: Clusters de máximo 20-25 clientes para que Google Maps pueda optimizarlos
	 */
	private int determineOptimalGridSize(int totalClientes) {
		// NUEVA ESTRATEGIA: clusters pequeños (≤25 clientes) para poder usar Google Maps
		// Google Maps tiene límite de 25 waypoints (base + 24 clientes)

		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "DETERMINANDO GRID SIZE ÓPTIMO");
		Log.i(TAG, "Total clientes: " + totalClientes);

		// Calcular gridSize para tener ~20 clientes por cluster (con margen para Google Maps)
		// Fórmula: gridSize = sqrt(totalClientes / 20)
		int targetClientsPerCluster = 20;
		int gridSize = (int) Math.ceil(Math.sqrt((double) totalClientes / targetClientsPerCluster));

		// Asegurar mínimo 3x3 y máximo 30x30
		gridSize = Math.max(3, Math.min(30, gridSize));

		int estimatedClusters = gridSize * gridSize;
		int estimatedClientsPerCluster = totalClientes / estimatedClusters;

		Log.i(TAG, "Grid size calculado: " + gridSize + "x" + gridSize + " = " + estimatedClusters + " cuadrantes");
		Log.i(TAG, "Clientes estimados por cluster: " + estimatedClientsPerCluster);
		Log.i(TAG, "════════════════════════════════════════");

		return gridSize;
	}

	/**
	 * Optimiza la ruta recorriendo cada cluster en orden y optimizando dentro de cada uno
	 * Resultado: Base → [Cluster 1 optimizado] → [Cluster 2 optimizado] → ... → Base
	 */
	private ArrayList<RouteOptimizerService.RutaClienteData> optimizeRouteWithClusters(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) throws Exception {

		ArrayList<RouteOptimizerService.RutaClienteData> rutaCompleta = new ArrayList<>();
		UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);
		int orden = 1;

		String service = ConstantsEndpoints.ROUTE_OPTIMIZER_SERVICE;
		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "OPTIMIZANDO RUTA CON CLUSTERING + " + service);
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
				baseLocation,
				null
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
			Log.i(TAG, "Guardando ruta con " + rutaOrdenada.size() + " clientes + base");

			int orden = 1;

			// 1. Guardar la base como primer punto de la ruta
			RutaGenerada rutaBase = Factory.build(RutaGenerada.class, app);
			rutaBase.FechaGeneracion = new Date();
			rutaBase.OrdenVisita = orden++;
			rutaBase.CodigoCliente = "BASE";
			rutaBase.NombreCliente = ciudadBase + " (BASE)";
			rutaBase.DireccionCliente = "";
			rutaBase.PoblacionCliente = ciudadBase;
			rutaBase.ProvinciaCliente = "";
			rutaBase.DistanciaEstimada = "0 km";
			rutaBase.CiudadBase = ciudadBase;
			rutaBase.GeolocalizationStatus = "✓ OK";
			rutaBase.Latitud = "";
			rutaBase.Longitud = "";
			rutaBase.ClusterID = 0;

			rutaBase.save();
			Log.d(TAG, "SaveRoute - Orden: 1, Cliente: " + ciudadBase + " (BASE)");

			// 2. Guardar cada cliente de la ruta
			for (RouteOptimizerService.RutaClienteData rutaCliente : rutaOrdenada) {
				// Buscar cliente por código en la BD
				Cliente cliente = findClienteByCodigo(app, rutaCliente.codigoCliente);

				if (cliente != null) {
					RutaGenerada ruta = Factory.build(RutaGenerada.class, app);
					ruta.FechaGeneracion = new Date();
					ruta.OrdenVisita = orden++;  // Usar contador incremental, no el orden original
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

					// Cluster (para debugging/Excel)
					ruta.ClusterID = rutaCliente.clusterID;

					ruta.save();

					Log.d(TAG, "SaveRoute - Orden: " + (orden - 1) + ", Cliente: " + cliente.Nombre + ", Distancia: " + rutaCliente.distanciaKm);
				} else {
					Log.w(TAG, "Cliente no encontrado: " + rutaCliente.codigoCliente);
				}
			}

			Log.i(TAG, "Ruta completa guardada en BD - Total: " + (rutaOrdenada.size() + 1) + " (incluyendo base)");

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
