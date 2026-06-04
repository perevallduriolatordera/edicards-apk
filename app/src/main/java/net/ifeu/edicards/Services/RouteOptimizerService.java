package net.ifeu.edicards.Services;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.Services.Geocoding.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para optimización de rutas usando OpenRouteService
 * Calcula la ruta más eficiente para una lista de clientes
 */
public class RouteOptimizerService {

    private static final String TAG = "RouteOptimizerService";

    // Variables para sincronización de llamada asíncrona
    private JSONObject apiResponse = null;
    private Exception apiException = null;

    /**
     * Optimiza una ruta para una lista de clientes usando algoritmo Nearest Neighbor
     *
     * @param clientes Lista de clientes con coordenadas ya geocodificadas
     * @param baseLocation Ubicación de la base del vendedor
     * @return Lista de RutaClienteData ordenada de forma optimizada, o null si falla
     * @throws Exception Si hay error en la llamada a ORS Matrix API
     */
    public ArrayList<RutaClienteData> optimizeRoute(ArrayList<Cliente> clientes, LatLng baseLocation) throws Exception {
        if (clientes == null || clientes.isEmpty()) {
            Log.w(TAG, "Lista de clientes vacía");
            return null;
        }

        if (baseLocation == null) {
            Log.w(TAG, "Ubicación de base no disponible");
            return null;
        }

        Log.i(TAG, "Optimizando ruta para " + clientes.size() + " clientes");

        try {
            // Dividir clientes en lotes de máximo 49 (50 incluyendo la base)
            // ORS Matrix API limita a 3500 rutas: sqrt(3500) ≈ 59, pero usamos 50 para seguridad
            // Matriz 50x50 = 2500 rutas
            final int MAX_LOCATIONS_PER_REQUEST = 49;
            ArrayList<RutaClienteData> rutaCompleta = new ArrayList<>();
            int orden = 1;

            // Si hay pocos clientes, procesarlos todos juntos
            if (clientes.size() <= MAX_LOCATIONS_PER_REQUEST) {
                return optimizeRouteBatch(clientes, baseLocation, 0);
            }

            // Si hay muchos clientes, dividir en lotes
            Log.i(TAG, "Dividiendo " + clientes.size() + " clientes en lotes de máximo " + MAX_LOCATIONS_PER_REQUEST);

            for (int i = 0; i < clientes.size(); i += MAX_LOCATIONS_PER_REQUEST) {
                int fin = Math.min(i + MAX_LOCATIONS_PER_REQUEST, clientes.size());
                ArrayList<Cliente> lote = new ArrayList<>(clientes.subList(i, fin));

                Log.d(TAG, "Procesando lote " + ((i / MAX_LOCATIONS_PER_REQUEST) + 1) + ": clientes " + i + " a " + (fin - 1));

                // Pasar el orden inicial correcto para este lote
                ArrayList<RutaClienteData> rutaLote = optimizeRouteBatch(lote, baseLocation, orden - 1);

                if (rutaLote != null) {
                    for (RutaClienteData ruta : rutaLote) {
                        rutaCompleta.add(ruta);
                        // Actualizar orden para el siguiente lote
                        orden = ruta.orden + 1;
                    }
                } else {
                    Log.w(TAG, "No se pudo optimizar lote " + ((i / MAX_LOCATIONS_PER_REQUEST) + 1));
                }
            }

            // RESUMEN FINAL DE TODA LA RUTA (incluyendo lotes)
            Log.i(TAG, "╔══════════════════════════════════════════╗");
            Log.i(TAG, "║  RUTA OPTIMIZADA COMPLETA              ║");
            Log.i(TAG, "║  Total clientes: " + rutaCompleta.size() + "                    ║");

            double distanciaTotalCompleta = 0;
            for (RutaClienteData ruta : rutaCompleta) {
                String distStr = ruta.distanciaKm.replace(" km", "");
                try {
                    distanciaTotalCompleta += Double.parseDouble(distStr);
                } catch (NumberFormatException e) {
                    // Ignorar
                }
            }
            Log.i(TAG, "║  Distancia total: " + String.format("%.1f km", distanciaTotalCompleta) + "           ║");
            Log.i(TAG, "╚══════════════════════════════════════════╝");

            return rutaCompleta;

        } catch (Exception e) {
            Log.e(TAG, "Error optimizando ruta: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Optimiza una ruta para un lote de clientes (máximo 99 clientes)
     */
    private ArrayList<RutaClienteData> optimizeRouteBatch(ArrayList<Cliente> loteClientes, LatLng baseLocation, int ordenInicial) throws Exception {
        try {
            // 1. Construir lista de coordenadas (incluye base al inicio)
            ArrayList<LatLng> locations = new ArrayList<>();
            locations.add(baseLocation); // Índice 0 = base

            Log.d(TAG, "=== INICIANDO OPTIMIZACIÓN DE LOTE ===");
            Log.d(TAG, "Base location (índice 0): " + baseLocation.toString());

            for (Cliente cliente : loteClientes) {
                if (cliente.Latitud != null && cliente.Longitud != null) {
                    locations.add(new LatLng(cliente.Latitud, cliente.Longitud));
                    Log.d(TAG, "Cliente [" + (locations.size() - 1) + "]: " + cliente.Nombre + " -> (" + cliente.Latitud + ", " + cliente.Longitud + ")");
                } else {
                    Log.w(TAG, "Cliente sin coordenadas válidas: " + cliente.Nombre);
                }
            }

            Log.d(TAG, "Lote: localizaciones totales (incluida base): " + locations.size());

            // 2. Obtener matriz de distancias desde ORS
            double[][] distanceMatrix = getDistanceMatrix(locations);

            if (distanceMatrix == null) {
                Log.e(TAG, "No se pudo obtener matriz de distancias para lote");
                return null;
            }

            // 3. Aplicar algoritmo Nearest Neighbor TSP para optimizar ruta
            ArrayList<Integer> optimizedIndices = nearestNeighborTSP(distanceMatrix, 0);

            if (optimizedIndices == null || optimizedIndices.isEmpty()) {
                Log.e(TAG, "No se pudo calcular ruta optimizada para lote");
                return null;
            }

            // 4. Construir resultado con RutaClienteData
            ArrayList<RutaClienteData> rutaOrdenada = new ArrayList<>();

            for (int i = 0; i < optimizedIndices.size(); i++) {
                int clienteIndex = optimizedIndices.get(i);

                // Índice 0 es la base, saltarlo
                if (clienteIndex == 0) {
                    continue;
                }

                // Obtener cliente (índice en clientes es clienteIndex - 1)
                int realClienteIndex = clienteIndex - 1;
                if (realClienteIndex >= 0 && realClienteIndex < loteClientes.size()) {
                    Cliente cliente = loteClientes.get(realClienteIndex);

                    RutaClienteData rutaCliente = new RutaClienteData();
                    rutaCliente.orden = ordenInicial + rutaOrdenada.size() + 1;
                    rutaCliente.nif = cliente.NIF != null ? cliente.NIF : "";
                    rutaCliente.razon = cliente.Razon != null ? cliente.Razon : "";
                    rutaCliente.nombre = cliente.Nombre != null ? cliente.Nombre : "";
                    rutaCliente.codigoCliente = cliente.CodigoCliente;

                    // Calcular distancia desde punto anterior
                    int prevIndex = i > 0 ? optimizedIndices.get(i - 1) : 0;
                    double distancia = distanceMatrix[prevIndex][clienteIndex];
                    double distanciaKilometros = distancia / 1000.0;
                    rutaCliente.distanciaKm = String.format("%.1f km", distanciaKilometros); // Convertir metros a km

                    rutaOrdenada.add(rutaCliente);

                    // Obtener nombre del punto anterior para debugging
                    String prevName = "BASE";
                    if (prevIndex > 0 && prevIndex - 1 < loteClientes.size()) {
                        prevName = loteClientes.get(prevIndex - 1).Nombre;
                    }

                    Log.d(TAG, "Orden " + rutaCliente.orden + ": " + cliente.Nombre);
                    Log.d(TAG, "  - Punto anterior: [" + prevIndex + "] = " + prevName);
                    Log.d(TAG, "  - Punto actual: [" + clienteIndex + "] = " + cliente.Nombre);
                    Log.d(TAG, "  - DISTANCIA DESDE ORS: " + String.format("%.0f metros = %.1f km", distancia, distanciaKilometros));
                    Log.d(TAG, "  - Distancia formateada para UI: " + rutaCliente.distanciaKm);

                    // Validar distancias sospechosas
                    if (distancia == Double.MAX_VALUE) {
                        Log.w(TAG, "  ⚠ ADVERTENCIA: Distancia es NULL en matriz ORS (ruta imposible?)");
                    } else if (distancia == 0) {
                        Log.w(TAG, "  ⚠ ADVERTENCIA: Distancia es 0 metros (misma ubicación?)");
                    }
                }
            }

            // RESUMEN FINAL DE LA RUTA OPTIMIZADA
            Log.i(TAG, "=== RESUMEN RUTA OPTIMIZADA ===");
            Log.i(TAG, "Total clientes en ruta: " + rutaOrdenada.size());

            double distanciaTotal = 0;
            for (RutaClienteData ruta : rutaOrdenada) {
                // Extraer valor numérico de "XX.X km"
                String distStr = ruta.distanciaKm.replace(" km", "");
                try {
                    double distKm = Double.parseDouble(distStr);
                    distanciaTotal += distKm;
                    Log.i(TAG, String.format("  %d. %s - %s", ruta.orden, ruta.nombre, ruta.distanciaKm));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "No se pudo parsear distancia: " + ruta.distanciaKm);
                }
            }
            Log.i(TAG, "Distancia total ruta: " + String.format("%.1f km", distanciaTotal));
            Log.i(TAG, "=== FIN RESUMEN ===");

            return rutaOrdenada;

        } catch (Exception e) {
            Log.e(TAG, "Error optimizando lote: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtiene la matriz de distancias desde ORS Matrix API
     *
     * @param locations Lista de coordenadas
     * @return Matriz de distancias en metros (o null si falla)
     */
    private double[][] getDistanceMatrix(ArrayList<LatLng> locations) throws Exception {
        if (locations.size() < 2) {
            Log.w(TAG, "Se necesitan al menos 2 localizaciones");
            return null;
        }

        // ORS Matrix API limita a 3500 rutas (pares de distancia)
        // Máximo seguro: √3500 ≈ 59, pero usamos 60 para ser conservador
        if (locations.size() > 60) {
            throw new Exception("ORS Matrix API limita a 60 localizaciones máximo para evitar exceder 3500 rutas. Tienes: " + locations.size());
        }

        final CountDownLatch latch = new CountDownLatch(1);

        try {
            // Validar que todas las ubicaciones tengan coordenadas válidas
            for (int i = 0; i < locations.size(); i++) {
                LatLng loc = locations.get(i);
                if (loc.getLatitude() == null || loc.getLongitude() == null) {
                    throw new Exception("Localización " + i + " tiene coordenadas nulas");
                }
                if (loc.getLatitude() < -90 || loc.getLatitude() > 90 ||
                    loc.getLongitude() < -180 || loc.getLongitude() > 180) {
                    throw new Exception("Localización " + i + " tiene coordenadas inválidas: lat=" +
                        loc.getLatitude() + ", lon=" + loc.getLongitude());
                }
            }

            // Construir JSON request
            JSONObject requestBody = new JSONObject();

            // Locations en formato [[lon, lat], [lon, lat], ...]
            JSONArray locationsArray = new JSONArray();
            for (LatLng location : locations) {
                JSONArray coord = new JSONArray();
                coord.put(location.getLongitude());
                coord.put(location.getLatitude());
                locationsArray.put(coord);
            }

            requestBody.put("locations", locationsArray);
            JSONArray metrics = new JSONArray();
            metrics.put("distance");
            requestBody.put("metrics", metrics);
            requestBody.put("units", "m"); // metros

            Log.d(TAG, "Llamando a ORS Matrix API con " + locations.size() + " localizaciones");
            Log.d(TAG, "Request body: " + requestBody.toString());

            // Decodificar API key si está en base64
            String apiKeyToUse = decodeApiKeyIfNeeded(ConstantsEndpoints.ORS_API_KEY);

            AndroidNetworking.post(ConstantsEndpoints.ORS_MATRIX_URL)
                    .addHeaders("Authorization", apiKeyToUse)
                    .addHeaders("Content-Type", "application/json")
                    .addJSONObjectBody(requestBody)
                    .setPriority(Priority.MEDIUM)
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            apiResponse = response;
                            apiException = null;
                            Log.d(TAG, "Respuesta recibida de ORS Matrix");
                            latch.countDown();
                        }

                        @Override
                        public void onError(ANError error) {
                            apiResponse = null;
                            String errorMsg = error.getMessage() != null ? error.getMessage() : "Error desconocido";
                            int statusCode = error.getErrorCode();

                            String detailedError = "ORS Matrix Error (" + statusCode + "): " + errorMsg;
                            if (statusCode == 400) {
                                detailedError = "Error de solicitud malformada (400): Verifica que todas las ubicaciones tengan coordenadas válidas. " + errorMsg;
                            } else if (statusCode == 401) {
                                detailedError = "Error de autenticación (401): Verifica ORS_API_KEY";
                            } else if (statusCode == 429) {
                                detailedError = "Error de límite de rate (429): Demasiadas solicitudes a OpenRouteService";
                            } else if (statusCode == -1) {
                                detailedError = "Error de conexión de red: " + errorMsg;
                            }

                            apiException = new Exception(detailedError);
                            Log.e(TAG, "ORS Matrix Error: " + detailedError);
                            if (error.getErrorBody() != null) {
                                Log.e(TAG, "Error body: " + error.getErrorBody());
                            }
                            latch.countDown();
                        }
                    });

            // Esperar respuesta (máximo 30 segundos)
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new Exception("Timeout esperando respuesta de ORS Matrix API (30 segundos)");
            }

            if (apiException != null) {
                throw apiException;
            }

            // Parsear respuesta
            if (apiResponse != null && apiResponse.has("distances")) {
                JSONArray distances = apiResponse.getJSONArray("distances");

                Log.d(TAG, "Respuesta ORS Matrix completa: " + apiResponse.toString());
                Log.d(TAG, "Número de filas: " + distances.length());

                // Convertir JSONArray a double[][]
                double[][] matrix = new double[distances.length()][];

                for (int i = 0; i < distances.length(); i++) {
                    JSONArray row = distances.getJSONArray(i);
                    matrix[i] = new double[row.length()];

                    // Log detallado de cada fila
                    StringBuilder rowDebug = new StringBuilder();
                    rowDebug.append("Fila [").append(i).append("]: ");

                    for (int j = 0; j < row.length(); j++) {
                        // Validar que no sea null antes de convertir
                        if (row.isNull(j)) {
                            Log.w(TAG, "Distancia null en [" + i + "][" + j + "]");
                            matrix[i][j] = Double.MAX_VALUE; // Usar valor muy grande en lugar de null
                            rowDebug.append("[").append(j).append("]=NULL ");
                        } else {
                            matrix[i][j] = row.getDouble(j);
                            rowDebug.append("[").append(j).append("]=").append(String.format("%.0f", matrix[i][j])).append("m ");
                        }
                    }
                    Log.d(TAG, rowDebug.toString());
                }

                Log.d(TAG, "Matriz de distancias parseada: " + matrix.length + "x" + (matrix.length > 0 ? matrix[0].length : 0));

                // Debug: mostrar resumen de distancias
                if (matrix.length > 0) {
                    Log.d(TAG, "RESUMEN MATRIZ DE DISTANCIAS (en metros):");
                    for (int i = 0; i < Math.min(5, matrix.length); i++) {
                        for (int j = 0; j < Math.min(5, matrix[i].length); j++) {
                            double dist = matrix[i][j];
                            if (dist == Double.MAX_VALUE) {
                                Log.d(TAG, String.format("  [%d][%d] = NULL", i, j));
                            } else {
                                Log.d(TAG, String.format("  [%d][%d] = %.0f metros = %.1f km", i, j, dist, dist/1000.0));
                            }
                        }
                    }
                }

                return matrix;
            }

            Log.w(TAG, "Respuesta vacía o sin distances de ORS");
            if (apiResponse != null) {
                Log.w(TAG, "Respuesta ORS: " + apiResponse.toString());
            }
            return null;

        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrumpido: " + e.getMessage());
            throw new Exception("Cálculo de distancias interrumpido", e);
        } catch (Exception e) {
            Log.e(TAG, "Error en ORS Matrix: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Algoritmo Nearest Neighbor para resolver el problema del vendedor viajero (TSP)
     * Comienza en startIndex y siempre selecciona el nodo más cercano no visitado
     *
     * @param distanceMatrix Matriz de distancias
     * @param startIndex Índice inicial (usualmente 0 para la base)
     * @return Lista de índices en orden optimizado
     */
    private ArrayList<Integer> nearestNeighborTSP(double[][] distanceMatrix, int startIndex) {
        ArrayList<Integer> route = new ArrayList<>();
        boolean[] visited = new boolean[distanceMatrix.length];

        int currentIndex = startIndex;
        route.add(currentIndex);
        visited[currentIndex] = true;

        // Visitar todos los nodos
        for (int i = 1; i < distanceMatrix.length; i++) {
            int nearestIndex = -1;
            double nearestDistance = Double.MAX_VALUE;

            // Encontrar el nodo más cercano no visitado
            for (int j = 0; j < distanceMatrix.length; j++) {
                if (!visited[j]) {
                    double distance = distanceMatrix[currentIndex][j];
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestIndex = j;
                    }
                }
            }

            if (nearestIndex != -1) {
                route.add(nearestIndex);
                visited[nearestIndex] = true;
                currentIndex = nearestIndex;
            }
        }

        // Volver a la base (startIndex)
        route.add(startIndex);

        Log.d(TAG, "Ruta NN calculada: " + route.toString());
        return route;
    }

    /**
     * Decodifica la API key si está en base64
     */
    private String decodeApiKeyIfNeeded(String apiKey) {
        try {
            // Intentar decodificar de base64
            byte[] decodedBytes = android.util.Base64.decode(apiKey, android.util.Base64.DEFAULT);
            String decodedString = new String(decodedBytes, "UTF-8");

            // Si empieza con '{', probablemente es JSON (base64 decodificado)
            if (decodedString.startsWith("{")) {
                try {
                    JSONObject json = new JSONObject(decodedString);
                    // Retornar el valor de "id" si existe
                    if (json.has("id")) {
                        String id = json.getString("id");
                        Log.d(TAG, "API key decodificada correctamente de base64");
                        return id;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "No se pudo parsear JSON de API key decodificada");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "API key no está en base64 o error al decodificar: " + e.getMessage());
        }

        // Si no es base64 o no se pudo decodificar, retornar la clave original
        return apiKey;
    }

    /**
     * Clase interna para datos de ruta optimizada
     */
    public static class RutaClienteData {
        public int orden;
        public String nif;
        public String razon;
        public String nombre;
        public String codigoCliente;
        public String distanciaKm;
    }
}
