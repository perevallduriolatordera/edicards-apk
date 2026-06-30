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
 * Optimizador de rutas usando Google Maps Routes API
 * Implementación asíncrona con callbacks (sin dependencias externas)
 */
public class GoogleMapsRouteOptimizer {

    private static final String TAG = "GoogleMapsRouteOptimizer";
    private static final String GOOGLE_MAPS_ROUTES_API_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

    private Context context;
    private String apiKey;

    public GoogleMapsRouteOptimizer(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
    }

    /**
     * Optimiza ruta de forma asíncrona usando Google Maps Routes API
     * Google Maps tiene límite de 25 waypoints por request (origin + 24 intermedios máximo)
     */
    public void optimizeRoute(ArrayList<String> waypoints, RouteOptimizationCallback callback) {
        if (waypoints == null || waypoints.isEmpty()) {
            callback.onError("Lista de waypoints vacía");
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "=== GOOGLE MAPS ROUTES API ===");
                Log.d(TAG, "Waypoints totales: " + waypoints.size());
                Log.d(TAG, "Base location (origen y destino): " + waypoints.get(0));

                // Usar método sincrónico para obtener la ruta optimizada
                ArrayList<Integer> result = optimizeRouteSynchronous(waypoints);

                if (result != null && !result.isEmpty()) {
                    Log.d(TAG, "Resultado de optimización recibido: " + result.toString());
                    callback.onRouteOptimized(null);
                } else {
                    callback.onError("No se recibió orden optimizado de Google Maps");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                e.printStackTrace();
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Optimiza ruta dividida en batches (síncrono dentro del thread)
     * Google Maps permite máximo 25 waypoints por request
     * Retorna el orden optimizado
     */
    public ArrayList<Integer> optimizeRouteBatchedSync(ArrayList<String> waypoints) throws Exception {
        final int MAX_WAYPOINTS_PER_BATCH = 25;
        String baseLocation = waypoints.get(0); // Origen/destino (siempre el mismo)
        ArrayList<Integer> combinedOrder = new ArrayList<>();
        int globalClientIndex = 1; // Indice global (empieza en 1 porque 0 es la base)

        // Procesar clientes en batches de máximo 24 (base + 24 clientes = 25 waypoints)
        for (int i = 1; i < waypoints.size(); i += (MAX_WAYPOINTS_PER_BATCH - 1)) {
            int batchEnd = Math.min(i + (MAX_WAYPOINTS_PER_BATCH - 1), waypoints.size());

            // Crear batch: siempre incluye la base + clientes del rango
            ArrayList<String> batch = new ArrayList<>();
            batch.add(baseLocation);
            for (int j = i; j < batchEnd; j++) {
                batch.add(waypoints.get(j));
            }

            Log.d(TAG, "Procesando batch: " + i + "-" + (batchEnd - 1) + " de " + waypoints.size() + " (" + batch.size() + " waypoints)");

            ArrayList<Integer> batchResult = callGoogleMapsRoutesAPISync(batch);

            if (batchResult != null && !batchResult.isEmpty()) {
                // Convertir índices locales (del batch) a índices globales
                for (Integer localIdx : batchResult) {
                    if (localIdx > 0) { // Ignorar índice 0 (base)
                        combinedOrder.add(globalClientIndex + localIdx - 1);
                    }
                }
            }

            globalClientIndex += (batchEnd - i);
        }

        Log.d(TAG, "Orden combinado de todos los batches: " + combinedOrder.toString());
        return combinedOrder;
    }

    /**
     * Método sincrónico para obtener la ruta optimizada
     * Devuelve el orden de los waypoints optimizado por Google Maps
     */
    public ArrayList<Integer> optimizeRouteSynchronous(ArrayList<String> waypoints) throws Exception {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new Exception("Lista de waypoints vacía");
        }

        Log.i(TAG, "╔══════════════════════════════════════════╗");
        Log.i(TAG, "║  OPTIMIZANDO RUTA CON GOOGLE MAPS       ║");
        Log.i(TAG, "║  Total waypoints: " + waypoints.size() + "                    ║");
        Log.i(TAG, "╚══════════════════════════════════════════╝");

        if (waypoints.size() > 25) {
            Log.w(TAG, "⚠ Más de 25 waypoints, dividiendo en batches de máximo 25");
            ArrayList<Integer> result = optimizeRouteBatchedSync(waypoints);
            Log.i(TAG, "✓ Optimización completada (batches). Orden: " + result.toString());
            return result;
        } else {
            ArrayList<Integer> result = callGoogleMapsRoutesAPISync(waypoints);
            Log.i(TAG, "✓ Optimización completada. Orden: " + result.toString());
            return result;
        }
    }

    /**
     * Llama a Google Maps Routes API de forma síncrona (ejecutar en thread)
     */
    private ArrayList<Integer> callGoogleMapsRoutesAPISync(ArrayList<String> waypoints) throws Exception {
        JSONObject requestBody = buildRequestJSON(waypoints);

        Log.d(TAG, "POST a Google Maps Routes API...");
        Log.d(TAG, "URL: " + GOOGLE_MAPS_ROUTES_API_URL);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(GOOGLE_MAPS_ROUTES_API_URL + "?key=" + apiKey);
            Log.d(TAG, "URL completa: " + url.toString());
            Log.d(TAG, "API Key (primeros 10 chars): " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Goog-Api-Key", apiKey);
            // Google Maps Routes API requiere especificar qué campos queremos en la respuesta
            // Cuando usamos optimizeWaypointOrder, necesitamos solicitar routes.optimized_intermediate_waypoint_index
            connection.setRequestProperty("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.optimized_intermediate_waypoint_index");
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setUseCaches(false);

            // Enviar request
            String requestBodyStr = requestBody.toString();
            Log.d(TAG, "Tamaño del request: " + requestBodyStr.length() + " bytes");
            Log.d(TAG, "Request body (primeros 300 chars):\n" + requestBodyStr.substring(0, Math.min(300, requestBodyStr.length())));
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBodyStr.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Leer respuesta
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            StringBuilder responseBody = new StringBuilder();
            java.io.InputStream inputStream = null;

            try {
                // Si es error, leer desde error stream, sino desde input stream
                if (responseCode >= 400) {
                    inputStream = connection.getErrorStream();
                    Log.w(TAG, "Error HTTP " + responseCode + ", usando error stream");
                } else {
                    inputStream = connection.getInputStream();
                }

                if (inputStream != null) {
                    try (Scanner scanner = new Scanner(inputStream)) {
                        scanner.useDelimiter("\\A");
                        if (scanner.hasNext()) {
                            responseBody.append(scanner.next());
                        }
                    }
                } else {
                    Log.w(TAG, "No hay input stream disponible");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error leyendo respuesta: " + e.getMessage());
                e.printStackTrace();
            }

            String responseBodyStr = responseBody.toString();
            if (!responseBodyStr.isEmpty()) {
                Log.d(TAG, "Response body (primeros 500 chars):\n" + responseBodyStr.substring(0, Math.min(500, responseBodyStr.length())));
            } else {
                Log.w(TAG, "Response body vacío");
            }

            if (responseCode != 200) {
                Log.e(TAG, "❌ ERROR en respuesta Google Maps API: " + responseCode);
                Log.e(TAG, "Response error: " + responseBodyStr);
                throw new Exception("Google Maps API returned: " + responseCode + " - " + responseBodyStr);
            }

            Log.i(TAG, "✅ Respuesta recibida de Google Maps (código 200)");

            // Parsear respuesta
            JSONObject response = new JSONObject(responseBodyStr);
            ArrayList<Integer> optimizedOrder = parseGoogleMapsResponse(response);

            Log.i(TAG, "✓ Ruta optimizada por Google Maps - Orden: " + optimizedOrder.toString());
            return optimizedOrder;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Construye JSON para Google Maps Routes API
     * Ruta LINEAL: Base → Clientes (sin retorno)
     * El último cliente será el destino (NO vuelve a la base)
     */
    private JSONObject buildRequestJSON(ArrayList<String> waypoints) throws Exception {
        JSONObject request = new JSONObject();

        // Origen (base - primer waypoint)
        JSONObject origin = new JSONObject();
        String[] firstCoords = waypoints.get(0).split(",");
        double originLat = Double.parseDouble(firstCoords[0]);
        double originLng = Double.parseDouble(firstCoords[1]);

        origin.put("location", new JSONObject()
            .put("latLng", new JSONObject()
                .put("latitude", originLat)
                .put("longitude", originLng)
            )
        );
        request.put("origin", origin);
        Log.d(TAG, "Origen (Base): lat=" + originLat + ", lng=" + originLng);

        // Destino (último waypoint - NO vuelve a la base para ruta LINEAL)
        JSONObject destination = new JSONObject();
        String[] lastCoords = waypoints.get(waypoints.size() - 1).split(",");
        double destLat = Double.parseDouble(lastCoords[0]);
        double destLng = Double.parseDouble(lastCoords[1]);

        destination.put("location", new JSONObject()
            .put("latLng", new JSONObject()
                .put("latitude", destLat)
                .put("longitude", destLng)
            )
        );
        request.put("destination", destination);
        Log.d(TAG, "Destino (Último cliente - Ruta LINEAL): lat=" + destLat + ", lng=" + destLng);

        // Intermedios (todos los clientes EXCEPTO el último, que es el destino)
        if (waypoints.size() > 2) {
            JSONArray intermediates = new JSONArray();
            // Iterar desde 1 (después de la base) hasta size-1 (antes del último)
            for (int i = 1; i < waypoints.size() - 1; i++) {
                String[] coords = waypoints.get(i).split(",");
                double lat = Double.parseDouble(coords[0]);
                double lng = Double.parseDouble(coords[1]);
                JSONObject waypoint = new JSONObject();
                waypoint.put("location", new JSONObject()
                    .put("latLng", new JSONObject()
                        .put("latitude", lat)
                        .put("longitude", lng)
                    )
                );
                intermediates.put(waypoint);
            }
            request.put("intermediates", intermediates);
            Log.d(TAG, "Intermedios (Clientes): " + (waypoints.size() - 2) + " puntos");
        } else {
            Log.d(TAG, "Sin intermedios (ruta directa base → último cliente)");
        }

        // Opciones
        request.put("optimizeWaypointOrder", true);
        // NOTA: optimize_waypoint_order NO funciona con TRAFFIC_AWARE_OPTIMAL
        // Usar TRAFFIC_AWARE en su lugar
        request.put("routingPreference", "TRAFFIC_AWARE");
        request.put("travelMode", "DRIVE");

        Log.d(TAG, "Configuración: optimizeWaypointOrder=true, TRAFFIC_AWARE_OPTIMAL, DRIVE");
        Log.d(TAG, "Request JSON completo:\n" + request.toString(2));

        return request;
    }

    /**
     * Parsea respuesta de Google Maps
     * Busca el campo optimized_intermediate_waypoint_index que contiene el orden optimizado
     */
    private ArrayList<Integer> parseGoogleMapsResponse(JSONObject response) throws Exception {
        ArrayList<Integer> optimizedOrder = new ArrayList<>();

        Log.d(TAG, "Parseando respuesta de Google Maps Routes API");

        if (!response.has("routes")) {
            Log.w(TAG, "Response no contiene campo 'routes'");
            Log.w(TAG, "Campos disponibles en response: " + response.keys());
            return optimizedOrder;
        }

        JSONArray routes = response.getJSONArray("routes");
        Log.d(TAG, "Rutas encontradas: " + routes.length());

        if (routes.length() > 0) {
            JSONObject route = routes.getJSONObject(0);

            // Google Maps Routes API retorna optimized_intermediate_waypoint_index cuando se usa optimizeWaypointOrder
            if (route.has("optimized_intermediate_waypoint_index")) {
                JSONArray optimizedIndices = route.getJSONArray("optimized_intermediate_waypoint_index");
                Log.d(TAG, "Optimized waypoint indices: " + optimizedIndices.length());

                // Los índices están en orden optimizado (empezando desde 0 = primer intermedio)
                // Necesitamos convertir a índices globales (1-based, porque 0 es la base)
                for (int i = 0; i < optimizedIndices.length(); i++) {
                    int waypointIdx = optimizedIndices.getInt(i);
                    // waypointIdx es 0-based entre los intermedios, convertir a índice global
                    int globalIdx = waypointIdx + 1;
                    optimizedOrder.add(globalIdx);
                    Log.d(TAG, "  Posición " + i + ": waypoint índice " + waypointIdx + " → global " + globalIdx);
                }

                Log.d(TAG, "✓ Orden optimizado completo: " + optimizedOrder.toString());
            } else {
                Log.w(TAG, "Route no contiene 'optimized_intermediate_waypoint_index'");
                Log.d(TAG, "Campos en route: " + route.keys());
            }
        } else {
            Log.w(TAG, "No hay rutas en respuesta");
        }

        return optimizedOrder;
    }
}
