package net.ifeu.edicards.Services;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Optimizador de rutas usando Google Route Optimization API (Fleet Routing)
 * API para optimización de 1 vehículo con múltiples paradas
 * Límite: 100 shipments por request
 * Mejor algoritmo TSP que Routes API
 */
public class GoogleFleetRoutingOptimizer {

	private static final String TAG = "GoogleFleetRouting";
	private static final String FLEET_ROUTING_API_URL = "https://routeoptimization.googleapis.com/v1:optimizeTours";
	private static final int MAX_SHIPMENTS_PER_REQUEST = 100;

	private Context context;
	private String apiKey;

	public GoogleFleetRoutingOptimizer(Context context, String apiKey) {
		this.context = context;
		this.apiKey = apiKey;
	}

	/**
	 * Optimiza ruta de forma síncrona usando Google Fleet Routing API
	 * Soporta hasta 100 clientes por batch
	 * Ruta LINEAL: no vuelve a la base
	 *
	 * @param waypoints Lista de coordenadas "lat,lng" (primero debe ser la base)
	 * @return Lista con el orden optimizado de índices
	 */
	public ArrayList<Integer> optimizeRouteSynchronous(ArrayList<String> waypoints) throws Exception {
		if (waypoints == null || waypoints.isEmpty()) {
			throw new Exception("Lista de waypoints vacía");
		}

		Log.i(TAG, "╔══════════════════════════════════════════╗");
		Log.i(TAG, "║  GOOGLE FLEET ROUTING API (Single Vehicle) ║");
		Log.i(TAG, "║  Total waypoints: " + waypoints.size() + "                   ║");
		Log.i(TAG, "╚══════════════════════════════════════════╝");

		// Si hay más de 100 clientes, dividir en batches
		if (waypoints.size() > MAX_SHIPMENTS_PER_REQUEST + 1) { // +1 por la base
			Log.w(TAG, "⚠ Más de 100 clientes, dividiendo en batches lineales");
			return optimizeRouteBatchedSync(waypoints);
		} else {
			ArrayList<Integer> result = callFleetRoutingAPISync(waypoints, null);
			Log.i(TAG, "✓ Optimización completada. Orden: " + result.toString());
			return result;
		}
	}

	/**
	 * Optimiza ruta dividida en batches LINEALES
	 * Batch 1: Base → Clientes → Último cliente
	 * Batch 2: Último cliente batch 1 → Clientes → Último cliente
	 * etc.
	 */
	private ArrayList<Integer> optimizeRouteBatchedSync(ArrayList<String> waypoints) throws Exception {
		String baseLocation = waypoints.get(0);
		ArrayList<Integer> combinedOrder = new ArrayList<>();
		int globalClientIndex = 1; // Empieza en 1 (0 es la base)
		String currentStartLocation = baseLocation;

		// Procesar en batches de máximo 100 clientes
		for (int i = 1; i < waypoints.size(); i += MAX_SHIPMENTS_PER_REQUEST) {
			int batchEnd = Math.min(i + MAX_SHIPMENTS_PER_REQUEST, waypoints.size());

			// Crear batch: punto de inicio + clientes del rango
			ArrayList<String> batch = new ArrayList<>();
			batch.add(currentStartLocation); // Punto de inicio (base o último cliente del batch anterior)
			for (int j = i; j < batchEnd; j++) {
				batch.add(waypoints.get(j));
			}

			int batchNumber = (i - 1) / MAX_SHIPMENTS_PER_REQUEST + 1;
			int totalBatches = (int) Math.ceil((double) (waypoints.size() - 1) / MAX_SHIPMENTS_PER_REQUEST);
			Log.i(TAG, "Procesando batch " + batchNumber + "/" + totalBatches +
				": clientes " + i + "-" + (batchEnd - 1) + " (" + (batch.size() - 1) + " clientes)");

			// Optimizar batch (sin retorno al inicio = LINEAL)
			ArrayList<Integer> batchResult = callFleetRoutingAPISync(batch, null);

			if (batchResult != null && !batchResult.isEmpty()) {
				// Convertir índices locales a globales
				for (Integer localIdx : batchResult) {
					if (localIdx > 0) { // Ignorar índice 0 (punto de inicio)
						int globalIdx = globalClientIndex + localIdx - 1;
						combinedOrder.add(globalIdx);
					}
				}

				// El último cliente de este batch será el punto de inicio del siguiente
				if (batchEnd < waypoints.size()) {
					int lastLocalIdx = batchResult.get(batchResult.size() - 1);
					if (lastLocalIdx > 0) {
						int lastGlobalIdx = globalClientIndex + lastLocalIdx - 1;
						currentStartLocation = waypoints.get(lastGlobalIdx);
						Log.d(TAG, "Siguiente batch empezará desde cliente " + lastGlobalIdx);
					}
				}
			}

			globalClientIndex += (batchEnd - i);
		}

		Log.i(TAG, "✓ Orden combinado de todos los batches: " + combinedOrder.size() + " clientes");
		return combinedOrder;
	}

	/**
	 * Llama a Google Fleet Routing API de forma síncrona
	 */
	private ArrayList<Integer> callFleetRoutingAPISync(
			ArrayList<String> waypoints,
			String endLocation) throws Exception {

		JSONObject requestBody = buildFleetRoutingRequest(waypoints, endLocation);

		Log.d(TAG, "POST a Fleet Routing API...");
		Log.d(TAG, "Shipments: " + (waypoints.size() - 1) + " clientes");

		HttpURLConnection connection = null;
		try {
			URL url = new URL(FLEET_ROUTING_API_URL + "?key=" + apiKey);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("X-Goog-FieldMask", "routes,metrics");
			connection.setDoOutput(true);

			// Enviar request
			byte[] requestBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);
			OutputStream os = connection.getOutputStream();
			os.write(requestBytes);
			os.flush();
			os.close();

			// Leer respuesta
			int responseCode = connection.getResponseCode();
			Scanner scanner;
			if (responseCode == 200) {
				scanner = new Scanner(connection.getInputStream(), "UTF-8");
			} else {
				scanner = new Scanner(connection.getErrorStream(), "UTF-8");
			}

			StringBuilder responseBody = new StringBuilder();
			while (scanner.hasNextLine()) {
				responseBody.append(scanner.nextLine());
			}
			scanner.close();

			if (responseCode != 200) {
				Log.e(TAG, "❌ ERROR en Fleet Routing API: " + responseCode);
				Log.e(TAG, "Response: " + responseBody.toString());
				throw new Exception("Fleet Routing API error: " + responseCode);
			}

			Log.i(TAG, "✅ Respuesta recibida de Fleet Routing API");

			// Parsear respuesta
			JSONObject response = new JSONObject(responseBody.toString());
			ArrayList<Integer> optimizedOrder = parseFleetRoutingResponse(response, waypoints.size());

			return optimizedOrder;

		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Construye JSON para Fleet Routing API
	 * Configura 1 vehículo con ruta LINEAL (sin retorno)
	 */
	private JSONObject buildFleetRoutingRequest(
			ArrayList<String> waypoints,
			String endLocation) throws Exception {

		JSONObject request = new JSONObject();

		// Modelo de optimización
		JSONObject model = new JSONObject();

		// 1. Definir shipments (paradas/clientes)
		JSONArray shipments = new JSONArray();
		for (int i = 1; i < waypoints.size(); i++) { // Empezar en 1 (saltar la base)
			String[] coords = waypoints.get(i).split(",");
			double lat = Double.parseDouble(coords[0]);
			double lng = Double.parseDouble(coords[1]);

			JSONObject shipment = new JSONObject();
			shipment.put("deliveries", new JSONArray().put(
				new JSONObject()
					.put("arrivalLocation", new JSONObject()
						.put("latitude", lat)
						.put("longitude", lng)
					)
			));
			shipments.put(shipment);
		}
		model.put("shipments", shipments);
		Log.d(TAG, "Shipments definidos: " + shipments.length());

		// 2. Definir vehículo (1 solo vehículo)
		JSONArray vehicles = new JSONArray();
		String[] baseCoords = waypoints.get(0).split(",");
		double baseLat = Double.parseDouble(baseCoords[0]);
		double baseLng = Double.parseDouble(baseCoords[1]);

		JSONObject vehicle = new JSONObject();
		vehicle.put("startLocation", new JSONObject()
			.put("latitude", baseLat)
			.put("longitude", baseLng)
		);

		// RUTA LINEAL: NO especificar endLocation para que no vuelva a la base
		if (endLocation != null) {
			String[] endCoords = endLocation.split(",");
			vehicle.put("endLocation", new JSONObject()
				.put("latitude", Double.parseDouble(endCoords[0]))
				.put("longitude", Double.parseDouble(endCoords[1]))
			);
			Log.d(TAG, "Configuración: Ruta a destino específico");
		} else {
			Log.d(TAG, "Configuración: Ruta LINEAL (sin retorno a la base)");
		}

		vehicles.put(vehicle);
		model.put("vehicles", vehicles);

		request.put("model", model);
		request.put("searchMode", "RETURN_BEST"); // Mejor optimización (más lento pero mejor calidad)

		Log.d(TAG, "Request JSON (primeros 1000 chars):\n" +
			request.toString(2).substring(0, Math.min(1000, request.toString(2).length())));

		return request;
	}

	/**
	 * Parsea respuesta de Fleet Routing API
	 * Extrae el orden de visita optimizado
	 */
	private ArrayList<Integer> parseFleetRoutingResponse(JSONObject response, int totalWaypoints) throws Exception {
		ArrayList<Integer> order = new ArrayList<>();

		if (!response.has("routes") || response.getJSONArray("routes").length() == 0) {
			throw new Exception("No se recibieron rutas en la respuesta");
		}

		JSONObject route = response.getJSONArray("routes").getJSONObject(0);

		if (!route.has("visits")) {
			Log.w(TAG, "⚠ No hay visits en la ruta, retornando orden secuencial");
			// Retornar orden secuencial como fallback
			for (int i = 0; i < totalWaypoints; i++) {
				order.add(i);
			}
			return order;
		}

		JSONArray visits = route.getJSONArray("visits");

		// Agregar índice 0 (punto de inicio)
		order.add(0);

		// Extraer índices de shipments visitados
		for (int i = 0; i < visits.length(); i++) {
			JSONObject visit = visits.getJSONObject(i);
			if (visit.has("shipmentIndex")) {
				// shipmentIndex + 1 porque los shipments empiezan en índice 0 pero corresponden a waypoint 1
				int shipmentIdx = visit.getInt("shipmentIndex");
				order.add(shipmentIdx + 1);
			}
		}

		Log.i(TAG, "Orden optimizado extraído: " + order.size() + " puntos");

		// Log de métricas si están disponibles
		if (response.has("metrics")) {
			JSONObject metrics = response.getJSONObject("metrics");
			if (metrics.has("travelDuration")) {
				String duration = metrics.getString("travelDuration");
				Log.i(TAG, "Duración estimada: " + duration);
			}
			if (metrics.has("travelDistanceMeters")) {
				double distanceKm = metrics.getDouble("travelDistanceMeters") / 1000.0;
				Log.i(TAG, "Distancia total: " + String.format("%.1f km", distanceKm));
			}
		}

		return order;
	}
}
