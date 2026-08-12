package net.ifeu.edicards.Services;

import android.content.Context;
import android.util.Log;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.DataTier.CiudadVendedor;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;
import net.ifeu.edicards.DataTier.Zona;
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
	private static final double MAX_JUMP_KM = 10.0; // Salto máximo permitido entre clientes consecutivos
	private static final double MAX_DISTANCE_FROM_BASE_KM = 1000.0; // Distancia máxima desde la base para incluir en ruta (excluye coordenadas erróneas)

	private Context context;
	private String googleApiKey;
	private ArrayList<Cliente> clientesLejanos; // Clientes fuera del radio MAX_DISTANCE_FROM_BASE_KM

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

			// 2.5. Filtrar clientes por distancia máxima a la base (excluir coordenadas erróneas)
			clientesActivos = filterClientesByDistanceFromBase(clientesActivos, baseLocation);

			if (clientesActivos.isEmpty()) {
				Log.e(TAG, "No hay clientes con coordenadas válidas para optimizar");
				return false;
			}

			// 2.6. Pre-ordenar clientes geográficamente (ruta más lineal)
			clientesActivos = preOrdenarClientesGeograficamente(clientesActivos, baseLocation);

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

				// Filtrar clientes excluyendo coordenadas inválidas
				callback.onProgress("Filtrando clientes con coordenadas inválidas...");
				clientesActivos = filterClientesByDistanceFromBase(clientesActivos, baseLocation);

				if (clientesActivos.isEmpty()) {
					callback.onError("No hay clientes con coordenadas válidas para optimizar");
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

		// Aplicar optimización 2-opt para eliminar cruces
		orderedClusters = optimize2Opt(orderedClusters, baseLocation);

		return orderedClusters;
	}

	/**
	 * Optimiza ruta de clusters usando algoritmo 2-opt
	 * Elimina cruces en la ruta intercambiando segmentos
	 *
	 * @param clusters Lista de clusters ordenados
	 * @param baseLocation Ubicación de la base
	 * @return Lista optimizada sin cruces
	 */
	private ArrayList<GeoClusteringService.GeoCluster> optimize2Opt(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) {

		if (clusters == null || clusters.size() <= 2) {
			return clusters;
		}

		Log.d(TAG, "════════════════════════════════════════");
		Log.d(TAG, "APLICANDO OPTIMIZACIÓN 2-OPT A CLUSTERS");

		ArrayList<GeoClusteringService.GeoCluster> route = new ArrayList<>(clusters);
		boolean improved = true;
		int iterations = 0;
		final int MAX_ITERATIONS = 100;

		double initialDistance = calculateTotalRouteDistance(route, baseLocation);
		Log.d(TAG, "Distancia inicial: " + String.format("%.1f km", initialDistance / 1000.0));

		while (improved && iterations < MAX_ITERATIONS) {
			improved = false;
			iterations++;

			for (int i = 0; i < route.size() - 1; i++) {
				for (int j = i + 2; j < route.size(); j++) {
					// Calcular distancia actual
					double distBefore = 0;
					if (i == 0) {
						distBefore += calculateDistance(
							baseLocation.getLatitude(), baseLocation.getLongitude(),
							route.get(i).centerLat, route.get(i).centerLon
						);
					} else {
						distBefore += calculateDistance(
							route.get(i - 1).centerLat, route.get(i - 1).centerLon,
							route.get(i).centerLat, route.get(i).centerLon
						);
					}
					distBefore += calculateDistance(
						route.get(i).centerLat, route.get(i).centerLon,
						route.get(i + 1).centerLat, route.get(i + 1).centerLon
					);
					distBefore += calculateDistance(
						route.get(j - 1).centerLat, route.get(j - 1).centerLon,
						route.get(j).centerLat, route.get(j).centerLon
					);

					// Calcular distancia después de intercambiar
					double distAfter = 0;
					if (i == 0) {
						distAfter += calculateDistance(
							baseLocation.getLatitude(), baseLocation.getLongitude(),
							route.get(j).centerLat, route.get(j).centerLon
						);
					} else {
						distAfter += calculateDistance(
							route.get(i - 1).centerLat, route.get(i - 1).centerLon,
							route.get(j).centerLat, route.get(j).centerLon
						);
					}
					distAfter += calculateDistance(
						route.get(j).centerLat, route.get(j).centerLon,
						route.get(i + 1).centerLat, route.get(i + 1).centerLon
					);
					distAfter += calculateDistance(
						route.get(j - 1).centerLat, route.get(j - 1).centerLon,
						route.get(i).centerLat, route.get(i).centerLon
					);

					// Si mejora, intercambiar segmento
					if (distAfter < distBefore) {
						// Invertir segmento entre i+1 y j
						int left = i + 1;
						int right = j;
						while (left < right) {
							GeoClusteringService.GeoCluster temp = route.get(left);
							route.set(left, route.get(right));
							route.set(right, temp);
							left++;
							right--;
						}
						improved = true;
						Log.d(TAG, "  → Mejora encontrada: intercambio [" + i + "-" + j + "] reduce " +
							String.format("%.1f km", (distBefore - distAfter) / 1000.0));
					}
				}
			}
		}

		double finalDistance = calculateTotalRouteDistance(route, baseLocation);
		double improvement = initialDistance - finalDistance;

		Log.d(TAG, "Distancia final: " + String.format("%.1f km", finalDistance / 1000.0));
		Log.d(TAG, "Mejora total: " + String.format("%.1f km", improvement / 1000.0) +
			" (" + String.format("%.1f%%", (improvement / initialDistance) * 100) + ")");
		Log.d(TAG, "Iteraciones: " + iterations);
		Log.d(TAG, "════════════════════════════════════════");

		return route;
	}

	/**
	 * Calcula distancia total de una ruta de clusters
	 */
	private double calculateTotalRouteDistance(
			ArrayList<GeoClusteringService.GeoCluster> route,
			LatLng baseLocation) {

		if (route == null || route.isEmpty()) {
			return 0;
		}

		double totalDist = 0;

		// Distancia de base al primer cluster
		totalDist += calculateDistance(
			baseLocation.getLatitude(), baseLocation.getLongitude(),
			route.get(0).centerLat, route.get(0).centerLon
		);

		// Distancia entre clusters
		for (int i = 0; i < route.size() - 1; i++) {
			totalDist += calculateDistance(
				route.get(i).centerLat, route.get(i).centerLon,
				route.get(i + 1).centerLat, route.get(i + 1).centerLon
			);
		}

		// Distancia del último cluster a la base
		totalDist += calculateDistance(
			route.get(route.size() - 1).centerLat, route.get(route.size() - 1).centerLon,
			baseLocation.getLatitude(), baseLocation.getLongitude()
		);

		return totalDist;
	}

	/**
	 * Calcula distancia Haversine entre dos puntos en km
	 */
	private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		return GeoClusteringService.calculateHaversineDistance(lat1, lon1, lat2, lon2);
	}

	/**
	 * Determina el tamaño optimal del grid basado en cantidad de clientes
	 * Objetivo: Clusters de ~90 clientes para aprovechar Google Fleet Routing (límite 100)
	 */
	private int determineOptimalGridSize(int totalClientes) {
		// ESTRATEGIA: Aprovechar al máximo Google Fleet Routing API
		// - Google puede optimizar hasta 100 clientes por request con algoritmo TSP superior
		// - Clusters grandes = menos saltos entre clusters, Google optimiza las transiciones
		// - Con 400 clientes y clusters de 90 → ~4-5 clusters → solo 4-5 saltos entre clusters

		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "DETERMINANDO GRID SIZE ÓPTIMO");
		Log.i(TAG, "Total clientes: " + totalClientes);

		// Calcular gridSize para tener ~90 clientes por cluster (aprovechar límite de 100)
		// Esto permite que Google Fleet Routing vea mucho más contexto y optimice mejor
		// Fórmula: gridSize = sqrt(totalClientes / 90)
		int targetClientsPerCluster = 90;
		int gridSize = (int) Math.ceil(Math.sqrt((double) totalClientes / targetClientsPerCluster));

		// Asegurar mínimo 3x3 y máximo 20x20
		gridSize = Math.max(3, Math.min(20, gridSize));

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
		Log.i(TAG, "Estrategia: Permutaciones exhaustivas + 2-opt");

		// Encontrar mejor orden de clusters probando todas las permutaciones + 2-opt
		ArrayList<GeoClusteringService.GeoCluster> clustersVisitados = findBestClusterOrder(clusters, baseLocation);

		// Optimizar cada cluster en el orden calculado
		// Mantener track de la ubicación actual (empieza en base, luego último cliente visitado)
		LatLng currentLocation = baseLocation;
		for (int i = 0; i < clustersVisitados.size(); i++) {
			GeoClusteringService.GeoCluster cluster = clustersVisitados.get(i);
			Log.i(TAG, "");
			Log.i(TAG, "Procesando Cluster " + cluster.clusterId + " (" + (i + 1) + "/" + clustersVisitados.size() + ")");
			Log.i(TAG, "  Clientes: " + cluster.size());
			Log.i(TAG, "  Centro: (" + String.format("%.4f", cluster.centerLat) + ", " +
					String.format("%.4f", cluster.centerLon) + ")");

			// Optimizar clientes desde ubicación actual (base para cluster 1, último cliente para demás)
			ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster = optimizer.optimizeRoute(
				cluster.clientes,
				currentLocation,
				null,
				cluster.clusterId  // ← Pasar el ID del cluster
			);

			if (rutaCluster != null && !rutaCluster.isEmpty()) {
			// Recalcular distancia del PRIMER cliente del cluster desde currentLocation
			RouteOptimizerService.RutaClienteData primerCliente = rutaCluster.get(0);
			double lat = Double.parseDouble(primerCliente.latitud);
			double lon = Double.parseDouble(primerCliente.longitud);
			LatLng primerClienteLocation = new LatLng(lat, lon);
			double distKm = calculateDistance(
				currentLocation.getLatitude(), currentLocation.getLongitude(),
				primerClienteLocation.getLatitude(), primerClienteLocation.getLongitude()
			) / 1000.0;
			primerCliente.distanciaKm = String.format("%.1f km", distKm);

			if (i > 0) {
				Log.i(TAG, "  → Distancia desde último cliente del cluster anterior: " +
					String.format("%.1f km", distKm));
			}

				// Validar saltos dentro del cluster
				int saltosGrandes = validarSaltosEnRuta(rutaCluster, MAX_JUMP_KM);
				if (saltosGrandes > 0) {
					Log.w(TAG, "  ⚠ Cluster " + cluster.clusterId + " tiene " + saltosGrandes + " saltos > " + MAX_JUMP_KM + " km");
				}

				// MEJORA: Si hay un siguiente cluster, reordenar para terminar en el cliente
				// más cercano al siguiente cluster (minimizar salto entre clusters)
				if (i < clustersVisitados.size() - 1) {
					GeoClusteringService.GeoCluster siguienteCluster = clustersVisitados.get(i + 1);
					optimizarConexionEntreCluster(rutaCluster, siguienteCluster);
				}

				// Ajustar números de orden para que sean secuenciales en la ruta global
				for (RouteOptimizerService.RutaClienteData ruta : rutaCluster) {
					ruta.orden = orden++;
					rutaCompleta.add(ruta);
				}
			// Actualizar currentLocation al último cliente de este cluster
			RouteOptimizerService.RutaClienteData ultimoCliente = rutaCluster.get(rutaCluster.size() - 1);
			double ultimoLat = Double.parseDouble(ultimoCliente.latitud);
			double ultimoLon = Double.parseDouble(ultimoCliente.longitud);
			currentLocation = new LatLng(ultimoLat, ultimoLon);

				Log.i(TAG, "  ✓ Cluster " + cluster.clusterId + " optimizado: " + rutaCluster.size() + " clientes");
			} else {
				Log.w(TAG, "  ✗ No se pudo optimizar cluster " + cluster.clusterId);
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
	 * Pre-ordena los clientes geográficamente para crear una ruta más lineal
	 * Usa ordenación angular desde la base (barrido en sentido horario)
	 * Esto evita zigzags y mejora la eficiencia del clustering
	 */
	private ArrayList<Cliente> preOrdenarClientesGeograficamente(
			ArrayList<Cliente> clientes,
			LatLng baseLocation) {

		Log.i(TAG, "");
		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "PRE-ORDENACIÓN GEOGRÁFICA");
		Log.i(TAG, "Ordenando " + clientes.size() + " clientes desde la base");

		final double baseLat = baseLocation.getLatitude();
		final double baseLon = baseLocation.getLongitude();

		// Ordenar por ángulo polar desde la base (barrido en sentido horario)
		ArrayList<Cliente> clientesOrdenados = new ArrayList<>(clientes);
		clientesOrdenados.sort((c1, c2) -> {
			// Calcular ángulo desde la base a cada cliente
			double angulo1 = Math.atan2(c1.Latitud - baseLat, c1.Longitud - baseLon);
			double angulo2 = Math.atan2(c2.Latitud - baseLat, c2.Longitud - baseLon);

			// Si están en el mismo ángulo (~misma dirección), ordenar por distancia
			if (Math.abs(angulo1 - angulo2) < 0.1) {
				double dist1 = GeoClusteringService.calculateHaversineDistance(
					baseLat, baseLon, c1.Latitud, c1.Longitud);
				double dist2 = GeoClusteringService.calculateHaversineDistance(
					baseLat, baseLon, c2.Latitud, c2.Longitud);
				return Double.compare(dist1, dist2);
			}

			return Double.compare(angulo1, angulo2);
		});

		Log.i(TAG, "✓ Clientes ordenados en barrido angular desde base");
		Log.i(TAG, "════════════════════════════════════════");

		return clientesOrdenados;
	}

	/**
	 * Valida que no haya saltos mayores al límite especificado en una ruta
	 * @return Número de saltos que exceden el límite
	 */
	private int validarSaltosEnRuta(ArrayList<RouteOptimizerService.RutaClienteData> ruta, double maxKm) {
		int saltosGrandes = 0;

		for (int i = 1; i < ruta.size(); i++) {
			String distStr = ruta.get(i).distanciaKm.replace(" km", "").replace(",", ".");
			try {
				double distancia = Double.parseDouble(distStr);
				if (distancia > maxKm) {
					saltosGrandes++;
					Log.d(TAG, "    → Salto grande: " + ruta.get(i - 1).nombre +
						" → " + ruta.get(i).nombre + " (" + distancia + " km)");
				}
			} catch (Exception e) {
				// Ignorar distancias inválidas
			}
		}

		return saltosGrandes;
	}

	/**
	 * Optimiza la conexión entre el cluster actual y el siguiente
	 * Reordena los clientes del cluster actual para que el último sea el más cercano
	 * al centro del siguiente cluster (minimiza el salto entre clusters)
	 */
	private void optimizarConexionEntreCluster(
			ArrayList<RouteOptimizerService.RutaClienteData> rutaCluster,
			GeoClusteringService.GeoCluster siguienteCluster) {

		if (rutaCluster == null || rutaCluster.size() < 2) {
			return; // No hay nada que optimizar
		}

		// Buscar el cliente más cercano al centro del siguiente cluster
		int indiceMasCercano = -1;
		double distanciaMinima = Double.MAX_VALUE;

		for (int i = 0; i < rutaCluster.size(); i++) {
			RouteOptimizerService.RutaClienteData cliente = rutaCluster.get(i);
			try {
				double lat = Double.parseDouble(cliente.latitud);
				double lon = Double.parseDouble(cliente.longitud);

				double distancia = GeoClusteringService.calculateHaversineDistance(
					lat, lon,
					siguienteCluster.centerLat, siguienteCluster.centerLon
				);

				if (distancia < distanciaMinima) {
					distanciaMinima = distancia;
					indiceMasCercano = i;
				}
			} catch (Exception e) {
				// Ignorar clientes con coordenadas inválidas
			}
		}

		// Si encontramos un cliente más cercano y NO es el último, reordenar
		if (indiceMasCercano >= 0 && indiceMasCercano != rutaCluster.size() - 1) {
			RouteOptimizerService.RutaClienteData clienteMasCercano = rutaCluster.remove(indiceMasCercano);
			rutaCluster.add(clienteMasCercano); // Mover al final

			Log.d(TAG, "  → Optimizado: moviendo '" + clienteMasCercano.nombre +
				"' al final (distancia al siguiente cluster: " +
				String.format("%.1f", distanciaMinima / 1000) + " km)");
		}
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
			// Obtener SOLO clientes activos CON zona asignada (con o sin coordenadas)
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT * FROM Clientes WHERE Activo = 1 AND IdZona IS NOT NULL AND IdZona != 0");

			Log.d(TAG, "Buscando clientes activos con zona asignada...");

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
						// Cliente sin coordenadas - se excluye de la ruta
						clientesParaGeocoding.add(c);
						Log.w(TAG, "❌ Cliente EXCLUIDO (sin coordenadas): " + c.Nombre);
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

			// Mostrar resumen de clientes excluidos
			if (!clientesParaGeocoding.isEmpty()) {
				Log.w(TAG, "");
				Log.w(TAG, "════════════════════════════════════════");
				Log.w(TAG, "CLIENTES EXCLUIDOS (sin coordenadas): " + clientesParaGeocoding.size());
				Log.w(TAG, "════════════════════════════════════════");
				Log.w(TAG, "Para geocodificar estos clientes, ejecute una sincronización desde el menú principal.");
				Log.w(TAG, "La geocodificación automática ocurre durante la importación de datos.");
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
	 * Filtra clientes excluyendo los que tienen coordenadas inválidas
	 * Verifica:
	 * 1. Coordenadas fuera de rango válido (lat: -90/90, lon: -180/180)
	 * 2. Coordenadas demasiado lejanas de la base (>1000km = probablemente erróneas)
	 *
	 * @param clientes Lista de todos los clientes
	 * @param baseLocation Ubicación de la base
	 * @return Lista de clientes con coordenadas válidas
	 */
	private ArrayList<Cliente> filterClientesByDistanceFromBase(
			ArrayList<Cliente> clientes,
			LatLng baseLocation) {

		ArrayList<Cliente> clientesValidos = new ArrayList<>();
		clientesLejanos = new ArrayList<>();

		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "FILTRANDO CLIENTES CON COORDENADAS INVÁLIDAS");
		Log.i(TAG, "Criterios: rango válido + distancia máxima " + MAX_DISTANCE_FROM_BASE_KM + " km");

		for (Cliente cliente : clientes) {
			if (cliente.Latitud == null || cliente.Longitud == null) {
				clientesLejanos.add(cliente);
				Log.w(TAG, "❌ Cliente EXCLUIDO (sin coordenadas): " + cliente.Nombre);
				continue;
			}

			// 1. Verificar si las coordenadas están en rango válido
			if (cliente.Latitud < -90 || cliente.Latitud > 90 ||
			    cliente.Longitud < -180 || cliente.Longitud > 180) {
				clientesLejanos.add(cliente);
				Log.w(TAG, "❌ Cliente EXCLUIDO (coordenadas fuera de rango): " + cliente.Nombre +
					" (" + cliente.Latitud + ", " + cliente.Longitud + ")");
				continue;
			}

			// 2. Verificar si las coordenadas son 0,0 (inválidas)
			if ((cliente.Latitud == 0 && cliente.Longitud == 0) ||
			    (Math.abs(cliente.Latitud) < 0.001 && Math.abs(cliente.Longitud) < 0.001)) {
				clientesLejanos.add(cliente);
				Log.w(TAG, "❌ Cliente EXCLUIDO (coordenadas 0,0): " + cliente.Nombre);
				continue;
			}

			// 3. Calcular distancia a la base
			double distanciaMetros = GeoClusteringService.calculateHaversineDistance(
				baseLocation.getLatitude(), baseLocation.getLongitude(),
				cliente.Latitud, cliente.Longitud
			);
			double distanciaKm = distanciaMetros / 1000.0;

			// 4. Excluir si está demasiado lejos (coordenadas probablemente erróneas)
			if (distanciaKm > MAX_DISTANCE_FROM_BASE_KM) {
				clientesLejanos.add(cliente);
				Log.w(TAG, "❌ Cliente EXCLUIDO (>1000km de la base): " + cliente.Nombre +
					" - " + String.format("%.0f", distanciaKm) + " km" +
					" (" + cliente.Latitud + ", " + cliente.Longitud + ")");
			} else {
				clientesValidos.add(cliente);
			}
		}

		Log.i(TAG, "Clientes VÁLIDOS (para ruta): " + clientesValidos.size());
		Log.i(TAG, "Clientes EXCLUIDOS (inválidos): " + clientesLejanos.size());
		Log.i(TAG, "════════════════════════════════════════");

		return clientesValidos;
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

					// Cluster / Zona (para debugging/Excel)
					ruta.ClusterID = rutaCliente.clusterID;
					ruta.NombreZona = rutaCliente.nombreZona;

					ruta.save();

					Log.d(TAG, "SaveRoute - Orden: " + (orden - 1) + ", Cliente: " + cliente.Nombre + ", Distancia: " + rutaCliente.distanciaKm);
				} else {
					Log.w(TAG, "Cliente no encontrado: " + rutaCliente.codigoCliente);
				}
			}

			// 3. Agregar clientes lejanos al final (excluidos de la optimización)
			if (clientesLejanos != null && !clientesLejanos.isEmpty()) {
				Log.i(TAG, "");
				Log.i(TAG, "════════════════════════════════════════");
				Log.i(TAG, "AGREGANDO CLIENTES LEJANOS AL FINAL");
				Log.i(TAG, "Total clientes excluidos: " + clientesLejanos.size());
				Log.i(TAG, "════════════════════════════════════════");

				for (Cliente cliente : clientesLejanos) {
					RutaGenerada ruta = Factory.build(RutaGenerada.class, app);
					ruta.FechaGeneracion = new Date();
					ruta.OrdenVisita = orden++;
					ruta.CodigoCliente = cliente.CodigoCliente;
					ruta.NombreCliente = cliente.Nombre;
					ruta.DireccionCliente = cliente.Direccion1;
					ruta.PoblacionCliente = cliente.Poblacion;
					ruta.ProvinciaCliente = cliente.Provincia;
					ruta.DistanciaEstimada = "N/A";
					ruta.CiudadBase = ciudadBase;

					// Geolocalización - marcar como coordenadas inválidas
					ruta.GeolocalizationStatus = "❌ INVÁLIDAS";
					ruta.Latitud = (cliente.Latitud != null) ? String.format("%.6f", cliente.Latitud) : "0";
					ruta.Longitud = (cliente.Longitud != null) ? String.format("%.6f", cliente.Longitud) : "0";

					// Cluster especial para clientes excluidos
					ruta.ClusterID = 999;

					ruta.save();

					Log.d(TAG, "Cliente excluido agregado al final: " + cliente.Nombre +
						" (" + ruta.Latitud + ", " + ruta.Longitud + ")");
				}
			}

			int totalGuardados = rutaOrdenada.size() + 1 + (clientesLejanos != null ? clientesLejanos.size() : 0);
			Log.i(TAG, "Ruta completa guardada en BD - Total: " + totalGuardados +
				" (" + rutaOrdenada.size() + " optimizados + " +
				(clientesLejanos != null ? clientesLejanos.size() : 0) + " excluidos)");

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

		// Deduplica solo por CodigoCliente (mantiene el último)
		java.util.LinkedHashMap<String, Cliente> clientesUnicos = new java.util.LinkedHashMap<>();

		for (Cliente cliente : clientes) {
			if (cliente.CodigoCliente != null && !cliente.CodigoCliente.isEmpty()) {
				// Si ya existe este código, será sobrescrito por la nueva ocurrencia (la última)
				clientesUnicos.put(cliente.CodigoCliente, cliente);
			}
		}

		ArrayList<Cliente> resultado = new ArrayList<>(clientesUnicos.values());
		int totalDespues = resultado.size();
		int duplicadosRemovidos = totalAntes - totalDespues;

		if (duplicadosRemovidos > 0) {
			Log.i(TAG, "Deduplicación: " + totalAntes + " → " + totalDespues + " clientes (" + duplicadosRemovidos + " duplicados por código)");
		}

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
	private boolean isValidCoordinate(Double latitud, Double longitud) {
		// Verificar que no sean null primero
		if (latitud == null || longitud == null) {
			return false;
		}

		// Aceptar cualquier coordenada geográfica válida mundialmente
		boolean esValida = latitud >= -90 && latitud <= 90 &&
		                   longitud >= -180 && longitud <= 180;

		if (!esValida) {
			Log.w(TAG, "Coordenadas inválidas: lat=" + latitud + ", lng=" + longitud);
		}

		return esValida;
	}

	/**
	 * Ordena zonas por distancia a la base (de menor a mayor distancia)
	 * Calcula la distancia promedio de los clientes de cada zona a la base
	 */
	private void ordenarZonasPorDistancia(AppConfig app, ArrayList<Zona> zonas, LatLng baseLocation) {
		// Clase auxiliar para almacenar zona con su distancia
		class ZonaConDistancia {
			Zona zona;
			double distanciaPromedio;

			ZonaConDistancia(Zona zona, double distancia) {
				this.zona = zona;
				this.distanciaPromedio = distancia;
			}
		}

		ArrayList<ZonaConDistancia> zonasConDistancia = new ArrayList<>();

		// Calcular distancia promedio para cada zona
		for (Zona zona : zonas) {
			try {
				ArrayList<Cliente> clientesZona = getClientesPorZona(app, zona.IdZona);

				if (clientesZona == null || clientesZona.isEmpty()) {
					// Si no tiene clientes, poner al final
					zonasConDistancia.add(new ZonaConDistancia(zona, Double.MAX_VALUE));
					continue;
				}

				// Calcular distancia promedio de los clientes de esta zona a la base
				double sumaDistancias = 0;
				int clientesValidos = 0;

				for (Cliente cliente : clientesZona) {
					if (cliente.Latitud != null && cliente.Longitud != null) {
						double distancia = calculateDistance(
							baseLocation.getLatitude(), baseLocation.getLongitude(),
							cliente.Latitud, cliente.Longitud
						);
						sumaDistancias += distancia;
						clientesValidos++;
					}
				}

				double distanciaPromedio = clientesValidos > 0 ? (sumaDistancias / clientesValidos) : Double.MAX_VALUE;
				zonasConDistancia.add(new ZonaConDistancia(zona, distanciaPromedio));

				Log.d(TAG, "  Zona '" + zona.NombreZona + "': " + clientesValidos + " clientes, distancia promedio = " +
					String.format("%.2f", distanciaPromedio) + " km");

			} catch (Exception e) {
				Log.e(TAG, "Error calculando distancia para zona " + zona.NombreZona + ": " + e.getMessage());
				zonasConDistancia.add(new ZonaConDistancia(zona, Double.MAX_VALUE));
			}
		}

		// Ordenar por distancia promedio (menor a mayor)
		java.util.Collections.sort(zonasConDistancia, new java.util.Comparator<ZonaConDistancia>() {
			@Override
			public int compare(ZonaConDistancia z1, ZonaConDistancia z2) {
				return Double.compare(z1.distanciaPromedio, z2.distanciaPromedio);
			}
		});

		// Reemplazar lista original con zonas ordenadas
		zonas.clear();
		for (ZonaConDistancia zcd : zonasConDistancia) {
			zonas.add(zcd.zona);
		}

		Log.i(TAG, "");
		Log.i(TAG, "  ✓ Zonas ordenadas por distancia:");
		for (int i = 0; i < zonasConDistancia.size(); i++) {
			ZonaConDistancia zcd = zonasConDistancia.get(i);
			String distStr = zcd.distanciaPromedio == Double.MAX_VALUE ? "SIN CLIENTES" :
				String.format("%.2f km", zcd.distanciaPromedio);
			Log.i(TAG, "    " + (i + 1) + ". " + zcd.zona.NombreZona + " - " + distStr);
		}
	}

	/**
	 * Valida que las coordenadas sean coherentes para España
	 * España peninsular + Baleares: Lat 35°-44°N, Lon -10° a 5°E
	 * Canarias: Lat 27°-30°N, Lon -18° a -13°W
	 * @param latitud Latitud a validar
	 * @param longitud Longitud a validar
	 * @return null si es válida, mensaje de error si es inválida
	 */
	private String validarCoordenadasEspana(Double latitud, Double longitud) {
		if (latitud == null || longitud == null) {
			return "Coordenadas nulas";
		}

		// Validación mundial básica
		if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
			return "Fuera del rango mundial (lat: " + latitud + ", lon: " + longitud + ")";
		}

		// Verificar si las coordenadas están invertidas (común error)
		// Si la "latitud" está en rango de longitud de España y viceversa
		boolean posiblementeInvertidas = false;
		if (latitud >= -18 && latitud <= 5 && longitud >= 27 && longitud <= 44) {
			posiblementeInvertidas = true;
		}

		// España Peninsular + Baleares: Lat 34-44°N, Lon -11° a 5°E
		boolean enPeninsula = (latitud >= 34.0 && latitud <= 44.0) &&
		                      (longitud >= -11.0 && longitud <= 5.0);

		// Islas Canarias: Lat 27-30°N, Lon -18° a -13°W
		boolean enCanarias = (latitud >= 27.0 && latitud <= 30.0) &&
		                     (longitud >= -18.0 && longitud <= -13.0);

		if (!enPeninsula && !enCanarias) {
			if (posiblementeInvertidas) {
				return "Coordenadas INVERTIDAS (lat=" + latitud + ", lon=" + longitud + ") - revisar";
			}
			return "Fuera de España (lat=" + latitud + ", lon=" + longitud + ")";
		}

		return null; // Válida
	}

	/**
	 * Genera todas las permutaciones de una lista de clusters
	 * Con 4-5 clusters: 24-120 permutaciones (factible)
	 */
	private ArrayList<ArrayList<GeoClusteringService.GeoCluster>> generatePermutations(
			ArrayList<GeoClusteringService.GeoCluster> clusters) {

		ArrayList<ArrayList<GeoClusteringService.GeoCluster>> result = new ArrayList<>();
		permute(clusters, 0, result);
		return result;
	}

	private void permute(ArrayList<GeoClusteringService.GeoCluster> arr, int index,
			ArrayList<ArrayList<GeoClusteringService.GeoCluster>> result) {

		if (index == arr.size() - 1) {
			result.add(new ArrayList<>(arr));
			return;
		}

		for (int i = index; i < arr.size(); i++) {
			// Swap
			GeoClusteringService.GeoCluster temp = arr.get(index);
			arr.set(index, arr.get(i));
			arr.set(i, temp);

			permute(arr, index + 1, result);

			// Swap back
			temp = arr.get(index);
			arr.set(index, arr.get(i));
			arr.set(i, temp);
		}
	}

	/**
	 * Encuentra el mejor orden de clusters probando todas las permutaciones
	 * y aplicando 2-opt sobre la mejor
	 */
	private ArrayList<GeoClusteringService.GeoCluster> findBestClusterOrder(
			ArrayList<GeoClusteringService.GeoCluster> clusters,
			LatLng baseLocation) {

		Log.i(TAG, "");
		Log.i(TAG, "════════════════════════════════════════");
		Log.i(TAG, "OPTIMIZANDO ORDEN DE CLUSTERS");
		Log.i(TAG, "Total clusters: " + clusters.size());

		// Generar todas las permutaciones
		ArrayList<ArrayList<GeoClusteringService.GeoCluster>> permutations =
			generatePermutations(new ArrayList<>(clusters));

		Log.i(TAG, "Permutaciones a evaluar: " + permutations.size());

		// Evaluar cada permutación
		ArrayList<GeoClusteringService.GeoCluster> bestOrder = null;
		double bestDistance = Double.MAX_VALUE;

		for (ArrayList<GeoClusteringService.GeoCluster> permutation : permutations) {
			double distance = calculateTotalRouteDistance(permutation, baseLocation);
			if (distance < bestDistance) {
				bestDistance = distance;
				bestOrder = new ArrayList<>(permutation);
			}
		}

		Log.i(TAG, "Mejor orden (permutaciones): " + String.format("%.1f km", bestDistance / 1000.0));

		// Aplicar 2-opt sobre el mejor orden para eliminar cruces
		Log.i(TAG, "Aplicando 2-opt para eliminar cruces...");
		bestOrder = optimize2Opt(bestOrder, baseLocation);

		double finalDistance = calculateTotalRouteDistance(bestOrder, baseLocation);
		Log.i(TAG, "Distancia final (después de 2-opt): " + String.format("%.1f km", finalDistance / 1000.0));
		Log.i(TAG, "Mejora total: " + String.format("%.1f km", (bestDistance - finalDistance) / 1000.0));
		Log.i(TAG, "════════════════════════════════════════");

		return bestOrder;
	}

	/**
	 * Genera rutas optimizadas por zonas (cada zona se optimiza independientemente)
	 * @param context Contexto de la aplicación
	 * @param ciudadBase Ciudad base del vendedor
	 * @param callback Callback para reportar progreso
	 */
	public void generateRouteByZonesAsync(Context context, String ciudadBase, RouteGenerationCallback callback) {
		new Thread(() -> {
			try {
				this.context = context;
				AppConfig app = (AppConfig) context.getApplicationContext();
				this.googleApiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;

				// 1. Obtener ubicación de la base
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

				// 2. Obtener zonas activas
				callback.onProgress("Obteniendo zonas activas...");
				net.ifeu.edicards.Services.ZonaManager zonaManager = new net.ifeu.edicards.Services.ZonaManager(app);
				ArrayList<net.ifeu.edicards.DataTier.Zona> zonas = zonaManager.obtenerZonasActivas();

				if (zonas.isEmpty()) {
					callback.onError("No hay zonas activas configuradas");
					return;
				}

				Log.i(TAG, "Generando rutas para " + zonas.size() + " zonas");

				ArrayList<RouteOptimizerService.RutaClienteData> rutaCompleta = new ArrayList<>();
				int ordenGlobal = 1;

				// 3. Procesar cada zona independientemente
				for (net.ifeu.edicards.DataTier.Zona zona : zonas) {
					callback.onProgress("Procesando zona: " + zona.NombreZona + "...");
					Log.i(TAG, "═══════════════════════════════════════");
					Log.i(TAG, "PROCESANDO ZONA: " + zona.NombreZona);

					// Obtener clientes de esta zona con coordenadas válidas
					ArrayList<Cliente> clientesZona = getClientesPorZona(app, zona.IdZona);

					if (clientesZona.isEmpty()) {
						Log.w(TAG, "Zona '" + zona.NombreZona + "' no tiene clientes asignados, saltando");
						continue;
					}

					Log.i(TAG, "Clientes en zona: " + clientesZona.size());

					// Filtrar por distancia desde base
					clientesZona = filterClientesByDistanceFromBase(clientesZona, baseLocation);

					if (clientesZona.isEmpty()) {
						Log.w(TAG, "Zona '" + zona.NombreZona + "' no tiene clientes con coordenadas válidas");
						continue;
					}

					// Optimizar clientes de esta zona usando Google Fleet Routing
					UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);
					ArrayList<RouteOptimizerService.RutaClienteData> rutaZona = optimizer.optimizeRoute(
						clientesZona,
						baseLocation,
						null,
						(int) zona.IdZona
					);

					if (rutaZona == null || rutaZona.isEmpty()) {
						Log.w(TAG, "No se pudo optimizar la zona '" + zona.NombreZona + "'");
						continue;
					}

					// Agregar información de zona y orden global
					for (RouteOptimizerService.RutaClienteData cliente : rutaZona) {
						cliente.ordenVisita = ordenGlobal++;
						cliente.nombreZona = zona.NombreZona;
						cliente.clusterId = (int) zona.IdZona;
						rutaCompleta.add(cliente);
					}

					Log.i(TAG, "Zona '" + zona.NombreZona + "' optimizada: " + rutaZona.size() + " clientes");
				}

				// 4. EXCLUIR clientes sin zona asignada del cálculo de rutas
				// Los clientes sin zona asignada NO se incluyen en la ruta
				callback.onProgress("Verificando zonas asignadas...");
				Log.i(TAG, "═══════════════════════════════════════");
				Log.i(TAG, "CLIENTES SIN ZONA EXCLUIDOS DEL CÁLCULO");

				ArrayList<Cliente> clientesSinZona = getClientesSinZona(app);
				if (!clientesSinZona.isEmpty()) {
					Log.i(TAG, "Clientes sin zona asignada (excluidos): " + clientesSinZona.size());
					// NO se procesan ni se añaden a la ruta
				}

				// CÓDIGO ANTERIOR COMENTADO - Ya no se procesan clientes sin zona
				/*
				if (!clientesSinZona.isEmpty()) {
					Log.i(TAG, "Clientes sin zona: " + clientesSinZona.size());

					clientesSinZona = filterClientesByDistanceFromBase(clientesSinZona, baseLocation);

					if (!clientesSinZona.isEmpty()) {
						UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);
						ArrayList<RouteOptimizerService.RutaClienteData> rutaSinZona = optimizer.optimizeRoute(
							clientesSinZona,
							baseLocation,
							null,
							0
						);

						if (rutaSinZona != null && !rutaSinZona.isEmpty()) {
							for (RouteOptimizerService.RutaClienteData cliente : rutaSinZona) {
								cliente.ordenVisita = ordenGlobal++;
								cliente.nombreZona = "Sin asignar";
								cliente.clusterId = 0;
								rutaCompleta.add(cliente);
							}
							Log.i(TAG, "Clientes sin zona optimizados: " + rutaSinZona.size());
						}
					}
				}
				*/

				if (rutaCompleta.isEmpty()) {
					callback.onError("No se pudieron generar rutas para ninguna zona");
					return;
				}

				// 5. Guardar ruta en BD
				callback.onProgress("Guardando ruta en BD...");
				saveRoute(app, rutaCompleta, ciudadBase);

				Log.i(TAG, "═══════════════════════════════════════");
				Log.i(TAG, "Rutas por zonas generadas exitosamente: " + rutaCompleta.size() + " clientes");
				callback.onRouteGenerated();

			} catch (Exception e) {
				Log.e(TAG, "Error en generateRouteByZonesAsync: " + e.getMessage());
				e.printStackTrace();
				callback.onError("Error: " + e.getMessage());
			}
		}).start();
	}

	/**
	 * Obtiene clientes activos con coordenadas válidas para una zona específica
	 */
	private ArrayList<Cliente> getClientesPorZona(AppConfig app, long idZona) throws Exception {
		ArrayList<Cliente> clientes = new ArrayList<>();

		// Primero contar cuántos clientes hay en la zona (sin filtro de coordenadas)
		android.database.Cursor cursorTotal = app.getDatabaseOperations().executeSentence(
			"SELECT COUNT(*) FROM Clientes WHERE Activo = 1 AND IdZona = " + idZona
		);
		int totalEnZona = 0;
		if (cursorTotal != null && cursorTotal.moveToFirst()) {
			totalEnZona = cursorTotal.getInt(0);
			cursorTotal.close();
		}

		// Diagnóstico: ver qué valores tienen las coordenadas de TODOS los clientes de esta zona
		android.database.Cursor cursorDiag = app.getDatabaseOperations().executeSentence(
			"SELECT CodigoCliente, Nombre, Latitud, Longitud FROM Clientes WHERE Activo = 1 AND IdZona = " + idZona
		);
		if (cursorDiag != null && cursorDiag.getCount() > 0) {
			cursorDiag.moveToFirst();
			Log.d(TAG, "  === DIAGNÓSTICO: TODOS los clientes en zona " + idZona + " ===");
			int conCoords = 0;
			int sinCoords = 0;
			do {
				String codigo = cursorDiag.getString(0);
				String nombre = cursorDiag.getString(1);
				Double latDbl = cursorDiag.isNull(2) ? null : cursorDiag.getDouble(2);
				Double lonDbl = cursorDiag.isNull(3) ? null : cursorDiag.getDouble(3);

				if (latDbl != null && latDbl != 0.0 && lonDbl != null && lonDbl != 0.0) {
					conCoords++;
					Log.d(TAG, "    ✓ " + codigo + " (" + nombre + "): Lat=" + latDbl + ", Lon=" + lonDbl);
				} else {
					sinCoords++;
					String razon = (latDbl == null || lonDbl == null) ? "NULL" : "CERO";
					Log.d(TAG, "    ✗ " + codigo + " (" + nombre + "): Lat=" + latDbl + ", Lon=" + lonDbl + " [" + razon + "]");
				}
			} while (cursorDiag.moveToNext());
			Log.d(TAG, "  === RESUMEN: " + conCoords + " con coords válidas, " + sinCoords + " sin coords ===");
			cursorDiag.close();
		}

		// Intentar query más simple: solo filtrar por zona y activo, luego filtrar coordenadas en código
		android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
			"SELECT * FROM Clientes WHERE Activo = 1 AND IdZona = " + idZona
		);

		Log.d(TAG, "  Total clientes en zona: " + totalEnZona + " | Recuperados sin filtro coords: " + (cursor != null ? cursor.getCount() : 0));

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			int conCoordenadas = 0;
			int sinCoordenadas = 0;
			int coordenadasInvalidas = 0;
			do {
				Cliente cliente = Factory.build(Cliente.class, app);
				if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente")))) {
					// Verificar que existan coordenadas
					if (cliente.Latitud != null && cliente.Latitud != 0.0 && cliente.Longitud != null && cliente.Longitud != 0.0) {
						// Validar que las coordenadas sean válidas para España
						String validacion = validarCoordenadasEspana(cliente.Latitud, cliente.Longitud);
						if (validacion == null) {
							// Coordenadas válidas
							clientes.add(cliente);
							conCoordenadas++;
						} else {
							// Coordenadas inválidas
							coordenadasInvalidas++;
							Log.w(TAG, "    ✗ Cliente " + cliente.CodigoCliente + " (" + cliente.Nombre + "): " + validacion);
						}
					} else {
						sinCoordenadas++;
					}
				}
			} while (cursor.moveToNext());
			cursor.close();
			Log.d(TAG, "  Después de filtrar: Válidas=" + conCoordenadas + " | Sin coords=" + sinCoordenadas + " | Inválidas=" + coordenadasInvalidas);
		}

		return deduplicarClientes(clientes);
	}

	/**
	 * Obtiene clientes activos con coordenadas válidas que NO tienen zona asignada
	 */
	private ArrayList<Cliente> getClientesSinZona(AppConfig app) throws Exception {
		ArrayList<Cliente> clientes = new ArrayList<>();

		android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
			"SELECT * FROM Clientes WHERE Activo = 1 AND (IdZona IS NULL OR IdZona = 0)" +
			" AND Latitud IS NOT NULL AND Latitud != 0 AND Longitud IS NOT NULL AND Longitud != 0"
		);

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			do {
				Cliente cliente = Factory.build(Cliente.class, app);
				if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente")))) {
					clientes.add(cliente);
				}
			} while (cursor.moveToNext());
			cursor.close();
		}

		return deduplicarClientes(clientes);
	}

	/**
	 * Genera ruta agrupada por zonas de forma asíncrona
	 * - Procesa cada zona como un lote separado
	 * - Los clientes sin zona se procesan al final
	 * - En la visualización se pueden ver agrupados por zona
	 *
	 * @param app Contexto de aplicación
	 * @param ciudadBase Ciudad base del vendedor
	 * @param callback Callback para reportar progreso y resultados
	 */
	public void generateRouteByZonesAsync(AppConfig app, String ciudadBase, RouteGenerationCallback callback) {
		new Thread(() -> {
			try {
				this.context = app.getApplicationContext();
				this.googleApiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;

				callback.onProgress("Obteniendo zonas activas...");

				// 1. Obtener todas las zonas activas
				Zona zona = Factory.build(Zona.class, app);
				ArrayList<Zona> zonasActivas = zona.getAllZonasActivas();

				Log.i(TAG, "══════════════════════════════════════════");
				Log.i(TAG, "  GENERACIÓN DE RUTA POR ZONAS");
				Log.i(TAG, "  Total zonas activas: " + zonasActivas.size());
				Log.i(TAG, "══════════════════════════════════════════");

				// 2. Geocodificar ciudad base
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

				// 3. ORDENAR ZONAS POR DISTANCIA A LA BASE
				callback.onProgress("Ordenando zonas por proximidad a la base...");
				Log.i(TAG, "");
				Log.i(TAG, "═══ ORDENANDO ZONAS POR DISTANCIA ═══");

				ordenarZonasPorDistancia(app, zonasActivas, baseLocation);

				ArrayList<RouteOptimizerService.RutaClienteData> rutaCompleta = new ArrayList<>();
				int ordenGlobal = 1;
				Cliente clienteAnterior = null;

				// 4. Procesar cada zona como un lote (ya ordenadas por distancia)
				for (int i = 0; i < zonasActivas.size(); i++) {
					Zona zonaActual = zonasActivas.get(i);

					callback.onProgress("Procesando zona " + (i + 1) + "/" + zonasActivas.size() + ": " + zonaActual.NombreZona);
					Log.i(TAG, "");
					Log.i(TAG, "═══ PROCESANDO ZONA: " + zonaActual.NombreZona + " ═══");

					// Obtener clientes de esta zona
					ArrayList<Cliente> clientesZona = getClientesPorZona(app, zonaActual.IdZona);

					if (clientesZona == null || clientesZona.isEmpty()) {
						Log.w(TAG, "  No hay clientes en zona: " + zonaActual.NombreZona);
						continue;
					}

					Log.i(TAG, "  Clientes en zona: " + clientesZona.size());

					// Filtrar clientes por distancia válida
					clientesZona = filterClientesByDistanceFromBase(clientesZona, baseLocation);

					if (clientesZona.isEmpty()) {
						Log.w(TAG, "  No hay clientes válidos en zona: " + zonaActual.NombreZona);
						continue;
					}

					// Optimizar clientes de esta zona
					callback.onProgress("Optimizando ruta para zona: " + zonaActual.NombreZona + " (" + clientesZona.size() + " clientes)");

					UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);

					// Usar el IdZona como clusterID para que se visualicen agrupados
					int clusterID = (int) zonaActual.IdZona;

					ArrayList<RouteOptimizerService.RutaClienteData> rutaZona =
						optimizer.optimizeRoute(clientesZona, baseLocation, clienteAnterior, clusterID);

					if (rutaZona != null && !rutaZona.isEmpty()) {
						// Agregar clientes optimizados con orden global correcto
						for (RouteOptimizerService.RutaClienteData rutaCliente : rutaZona) {
							rutaCliente.orden = ordenGlobal++;
							// Guardar nombre de zona en el objeto para visualización
							rutaCliente.nombreZona = zonaActual.NombreZona;
							rutaCompleta.add(rutaCliente);
						}

						// Guardar último cliente para conectar con siguiente zona
						RouteOptimizerService.RutaClienteData ultimoRuta = rutaZona.get(rutaZona.size() - 1);
						for (Cliente c : clientesZona) {
							if (c.CodigoCliente.equals(ultimoRuta.codigoCliente)) {
								clienteAnterior = c;
								break;
							}
						}

						Log.i(TAG, "  ✓ Zona optimizada: " + rutaZona.size() + " clientes");
					}
				}

				// 5. EXCLUIR clientes sin zona del cálculo de rutas
				// Los clientes sin zona asignada NO se incluyen en la ruta
				callback.onProgress("Verificando zonas asignadas...");
				Log.i(TAG, "");
				Log.i(TAG, "═══ CLIENTES SIN ZONA EXCLUIDOS DEL CÁLCULO ═══");

				ArrayList<Cliente> clientesSinZona = getClientesSinZona(app);

				if (clientesSinZona != null && !clientesSinZona.isEmpty()) {
					Log.i(TAG, "  Clientes sin zona asignada (excluidos): " + clientesSinZona.size());
				// NO se procesan ni se añaden a la ruta
				/* CÓDIGO ANTERIOR COMENTADO - Ya no se procesan clientes sin zona

					clientesSinZona = filterClientesByDistanceFromBase(clientesSinZona, baseLocation);

					if (!clientesSinZona.isEmpty()) {
						UnifiedRouteOptimizer optimizer = new UnifiedRouteOptimizer(context, googleApiKey);

						// Usar un clusterID especial para clientes sin zona (999999)
						int clusterSinZona = 999999;

						ArrayList<RouteOptimizerService.RutaClienteData> rutaSinZona =
							optimizer.optimizeRoute(clientesSinZona, baseLocation, clienteAnterior, clusterSinZona);

						if (rutaSinZona != null && !rutaSinZona.isEmpty()) {
							for (RouteOptimizerService.RutaClienteData rutaCliente : rutaSinZona) {
								rutaCliente.orden = ordenGlobal++;
								rutaCliente.nombreZona = "SIN ZONA";
								rutaCompleta.add(rutaCliente);
							}

							Log.i(TAG, "  ✓ Clientes sin zona optimizados: " + rutaSinZona.size());
						}
					}
				}
				*/
			}

				if (rutaCompleta.isEmpty()) {
					callback.onError("No se pudo generar ninguna ruta");
					return;
				}

				// 5. Asegurar que el primer cliente esté cerca de la base
				rutaCompleta = ensureFirstClientNearBase(rutaCompleta, baseLocation);

				// 6. Guardar ruta en BD
				callback.onProgress("Guardando ruta en BD...");
				saveRoute(app, rutaCompleta, ciudadBase);

				Log.i(TAG, "");
				Log.i(TAG, "══════════════════════════════════════════");
				Log.i(TAG, "  ✓ RUTA GENERADA POR ZONAS EXITOSAMENTE");
				Log.i(TAG, "  Total clientes: " + rutaCompleta.size());
				Log.i(TAG, "  Total zonas procesadas: " + zonasActivas.size());
				Log.i(TAG, "══════════════════════════════════════════");

				callback.onRouteGenerated();

			} catch (Exception e) {
				Log.e(TAG, "Error en generateRouteByZonesAsync: " + e.getMessage());
				e.printStackTrace();
				callback.onError("Error: " + e.getMessage());
			}
		}).start();
	}
}
