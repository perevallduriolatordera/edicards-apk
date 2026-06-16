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
        return optimizeRoute(clientes, baseLocation, null);
    }

    /**
     * Optimiza una ruta para una lista de clientes usando algoritmo Nearest Neighbor
     * Versión con punto de inicio personalizado para conectar clusters
     *
     * @param clientes Lista de clientes con coordenadas ya geocodificadas
     * @param baseLocation Ubicación de la base del vendedor
     * @param startingClient Cliente desde el que comenzar la ruta (null = comenzar desde base)
     * @return Lista de RutaClienteData ordenada de forma optimizada, o null si falla
     * @throws Exception Si hay error en la llamada a ORS Matrix API
     */
    public ArrayList<RutaClienteData> optimizeRoute(ArrayList<Cliente> clientes, LatLng baseLocation, Cliente startingClient) throws Exception {
        if (clientes == null || clientes.isEmpty()) {
            Log.w(TAG, "Lista de clientes vacía");
            return null;
        }

        if (baseLocation == null) {
            Log.w(TAG, "Ubicación de base no disponible");
            return null;
        }

        Log.i(TAG, "Optimizando ruta para " + clientes.size() + " clientes");
        if (startingClient != null) {
            Log.i(TAG, "Punto de inicio personalizado: " + startingClient.Nombre);
        }

        try {
            // Dividir clientes en lotes de máximo 49 (50 incluyendo la base)
            // ORS Matrix API limita a 3500 rutas: sqrt(3500) ≈ 59, pero usamos 50 para seguridad
            // Matriz 50x50 = 2500 rutas
            final int MAX_LOCATIONS_PER_REQUEST = 49;
            ArrayList<RutaClienteData> rutaCompleta = new ArrayList<>();
            int orden = 1;

            // Si hay pocos clientes, procesarlos todos juntos
            if (clientes.size() <= MAX_LOCATIONS_PER_REQUEST) {
                // Determinar startingIndex basado en si hay startingClient
                int startingIndex = 0;
                ArrayList<Cliente> clientesOrdenados = clientes;

                if (startingClient != null) {
                    // Reordenar para que startingClient esté primero
                    clientesOrdenados = new ArrayList<>();
                    clientesOrdenados.add(startingClient);
                    for (Cliente c : clientes) {
                        if (!c.CodigoCliente.equals(startingClient.CodigoCliente)) {
                            clientesOrdenados.add(c);
                        }
                    }
                    startingIndex = 1; // Empezar desde primer cliente (no base)
                }

                return optimizeRouteBatch(clientesOrdenados, baseLocation, 0, startingIndex);
            }

            // Si hay muchos clientes, pre-ordenar por proximidad antes de dividir en lotes
            Log.i(TAG, "Pre-ordenando " + clientes.size() + " clientes por proximidad geográfica...");
            ArrayList<Cliente> clientesOrdenados = preOrderClientsByProximity(clientes, baseLocation);

            Log.i(TAG, "Dividiendo " + clientesOrdenados.size() + " clientes PRE-ORDENADOS en lotes de máximo " + MAX_LOCATIONS_PER_REQUEST);

            // Procesar lotes PRE-ORDENADOS (ahora están geográficamente agrupados)
            Cliente clienteAnterior = null;
            for (int i = 0; i < clientesOrdenados.size(); i += MAX_LOCATIONS_PER_REQUEST) {
                int fin = Math.min(i + MAX_LOCATIONS_PER_REQUEST, clientesOrdenados.size());
                ArrayList<Cliente> lote = new ArrayList<>(clientesOrdenados.subList(i, fin));

                // Si no es el primer lote, reordenar para conectar con el cliente anterior
                if (clienteAnterior != null && lote.size() > 1) {
                    lote = reordenarLoteParaConectar(lote, clienteAnterior);
                }

                Log.d(TAG, "Procesando lote " + ((i / MAX_LOCATIONS_PER_REQUEST) + 1) + ": clientes " + i + " a " + (fin - 1));

                // Pasar el orden inicial correcto para este lote
                // Si no hay cliente anterior (primer lote), empezar desde base (índice 0)
                // Si hay cliente anterior, empezar desde primer cliente del lote (índice 1, ya reordenado)
                int startingIndex = (clienteAnterior != null) ? 1 : 0;
                ArrayList<RutaClienteData> rutaLote = optimizeRouteBatch(lote, baseLocation, orden - 1, startingIndex);

                if (rutaLote != null) {
                    for (RutaClienteData ruta : rutaLote) {
                        rutaCompleta.add(ruta);
                        // Actualizar orden para el siguiente lote
                        orden = ruta.orden + 1;
                    }
                    // Guardar el último cliente procesado para conectar el siguiente lote
                    if (!rutaLote.isEmpty()) {
                        RutaClienteData ultimoRuta = rutaLote.get(rutaLote.size() - 1);
                        // Buscar el cliente correspondiente
                        for (Cliente c : lote) {
                            if (c.CodigoCliente.equals(ultimoRuta.codigoCliente)) {
                                clienteAnterior = c;
                                break;
                            }
                        }
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
     *
     * @param loteClientes Lista de clientes del lote
     * @param baseLocation Ubicación de la base
     * @param ordenInicial Número de orden inicial para este lote
     * @param startingLocationIndex Índice de location en donde comenzar el NN TSP (0 = base, 1+ = primer cliente del lote, etc.)
     */
    private ArrayList<RutaClienteData> optimizeRouteBatch(ArrayList<Cliente> loteClientes, LatLng baseLocation, int ordenInicial, int startingLocationIndex) throws Exception {
        try {
            // 1. Construir lista de coordenadas (incluye base al inicio)
            ArrayList<LatLng> locations = new ArrayList<>();
            locations.add(baseLocation); // Índice 0 = base

            Log.d(TAG, "=== INICIANDO OPTIMIZACIÓN DE LOTE ===");
            Log.d(TAG, "Base location (índice 0): " + baseLocation.toString());
            Log.d(TAG, "Punto de inicio para NN TSP: índice " + startingLocationIndex + (startingLocationIndex == 0 ? " (BASE)" : " (PRIMER CLIENTE DEL LOTE)"));

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

            // 3. Aplicar algoritmo de optimización de ruta (Nearest Neighbor + 2-Opt improvement)
            // Comenzar desde el índice especificado (base para primer lote, primer cliente para lotes posteriores)
            ArrayList<Integer> optimizedIndices = nearestNeighborTSP(distanceMatrix, startingLocationIndex);

            if (optimizedIndices == null || optimizedIndices.isEmpty()) {
                Log.e(TAG, "No se pudo calcular ruta optimizada para lote");
                return null;
            }

            // Aplicar mejora 2-opt para optimización local
            optimizedIndices = TwoOptRouteOptimizer.optimize(optimizedIndices, distanceMatrix);
            Log.i(TAG, "✓ Nearest Neighbor + 2-Opt completado");

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

                    // Validar geolocalización
                    rutaCliente.geolocalizationStatus = validateGeolocalization(cliente);
                    rutaCliente.latitud = cliente.Latitud != null ? String.format("%.6f", cliente.Latitud) : "NULL";
                    rutaCliente.longitud = cliente.Longitud != null ? String.format("%.6f", cliente.Longitud) : "NULL";

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

        // Ruta lineal: NO volver a la base
        Log.d(TAG, "Ruta NN lineal calculada: " + route.toString());
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
     * Reordena un lote para conectar inteligentemente con el cliente anterior
     * Busca el cliente del lote más cercano al cliente anterior y lo coloca al inicio
     */
    private ArrayList<Cliente> reordenarLoteParaConectar(ArrayList<Cliente> lote, Cliente clienteAnterior) {
        try {
            if (clienteAnterior.Latitud == null || clienteAnterior.Longitud == null ||
                lote.isEmpty()) {
                return lote;
            }

            // Calcular distancia desde el cliente anterior a cada cliente del lote
            int mejorIndice = 0;
            double mejorDistancia = Double.MAX_VALUE;

            LatLng locAnterior = new LatLng(clienteAnterior.Latitud, clienteAnterior.Longitud);

            for (int i = 0; i < lote.size(); i++) {
                Cliente cliente = lote.get(i);
                if (cliente.Latitud != null && cliente.Longitud != null) {
                    LatLng locActual = new LatLng(cliente.Latitud, cliente.Longitud);
                    double distancia = calcularDistanciaHaversine(locAnterior, locActual);

                    if (distancia < mejorDistancia) {
                        mejorDistancia = distancia;
                        mejorIndice = i;
                    }
                }
            }

            // Si el mejor cliente no es el primero, reordenar
            if (mejorIndice != 0) {
                Cliente clienteMejor = lote.remove(mejorIndice);
                lote.add(0, clienteMejor);
                Log.d(TAG, "Lote reordenado: cliente más cercano al anterior está a " + String.format("%.1f km", mejorDistancia / 1000.0));
            }

            return lote;

        } catch (Exception e) {
            Log.w(TAG, "Error reordenando lote: " + e.getMessage());
            return lote;
        }
    }

    /**
     * Calcula distancia Haversine entre dos puntos en metros
     */
    private double calcularDistanciaHaversine(LatLng loc1, LatLng loc2) {
        final int RADIO_TIERRA = 6371000; // metros
        double lat1 = Math.toRadians(loc1.getLatitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double deltaLat = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double deltaLon = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA * c;
    }

    /**
     * Valida si un cliente tiene geolocalización válida
     * Verifica:
     * - Coordenadas no nulas
     * - Latitud/Longitud dentro de rangos válidos
     * - No son valores por defecto (0, 0)
     *
     * @param cliente Cliente a validar
     * @return "✓ OK" si valida, "⚠ SIN COORDENADAS" si null/0, "❌ INVÁLIDAS" si fuera de rango
     */
    private String validateGeolocalization(Cliente cliente) {
        if (cliente.Latitud == null || cliente.Longitud == null) {
            return "⚠ SIN COORDS";
        }

        Double lat = cliente.Latitud;
        Double lon = cliente.Longitud;

        // Verificar si son valores por defecto
        if (lat == 0 && lon == 0) {
            return "⚠ SIN COORDS";
        }

        // Verificar rango válido (aproximadamente España: lat 36-43, lon -10 a 3)
        if (lat < 35 || lat > 44 || lon < -11 || lon > 4) {
            return "❌ INVÁLIDAS";
        }

        return "✓ OK";
    }

    /**
     * Calcula matriz de distancias usando Haversine (no requiere API)
     * Útil para pre-ordenamiento cuando hay muchas ubicaciones (> 60)
     *
     * @param locations Lista de ubicaciones
     * @return Matriz de distancias en metros
     */
    private double[][] calculateHaversineDistanceMatrix(ArrayList<LatLng> locations) {
        int size = locations.size();
        double[][] matrix = new double[size][size];

        Log.d(TAG, "Calculando matriz de distancias Haversine para " + size + " ubicaciones (local, sin API)");

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] = calcularDistanciaHaversine(locations.get(i), locations.get(j));
                }
            }
        }

        Log.d(TAG, "Matriz Haversine " + size + "x" + size + " calculada");
        return matrix;
    }

    /**
     * Pre-ordena clientes por proximidad geográfica usando Nearest Neighbor greedy
     * Esto agrupa geográficamente los clientes ANTES de dividir en lotes de 50
     * Minimiza los saltos entre lotes
     *
     * NOTA: Usa Haversine en lugar de ORS para evitar límite de 60 ubicaciones
     * Haversine es suficientemente exacto para pre-ordenamiento geográfico
     */
    private ArrayList<Cliente> preOrderClientsByProximity(ArrayList<Cliente> clientes, LatLng baseLocation) throws Exception {
        if (clientes.size() <= 1) {
            return clientes;
        }

        try {
            Log.i(TAG, "Pre-ordenando " + clientes.size() + " clientes usando distancia Haversine (local, sin API)");

            // 1. Construir lista de coordenadas (incluye base al inicio)
            ArrayList<LatLng> locations = new ArrayList<>();
            locations.add(baseLocation); // Índice 0 = base
            for (Cliente cliente : clientes) {
                if (cliente.Latitud != null && cliente.Longitud != null) {
                    locations.add(new LatLng(cliente.Latitud, cliente.Longitud));
                } else {
                    Log.w(TAG, "Cliente sin coordenadas en pre-ordenamiento: " + cliente.Nombre);
                }
            }

            // 2. Obtener matriz de distancias usando Haversine (local, sin API)
            double[][] distanceMatrix = calculateHaversineDistanceMatrix(locations);
            if (distanceMatrix == null) {
                Log.w(TAG, "No se pudo calcular matriz Haversine, usando orden original");
                return clientes;
            }

            Log.i(TAG, "Aplicando algoritmo Nearest Neighbor para pre-ordenamiento con Haversine...");

            // 3. Usar Nearest Neighbor desde la base para pre-ordenar
            ArrayList<Integer> orderedIndices = new ArrayList<>();
            boolean[] visited = new boolean[locations.size()];
            int currentIndex = 0; // Comenzar desde la base
            visited[0] = true;

            // Agregar índices de clientes ordenados por proximidad
            for (int i = 1; i < locations.size(); i++) {
                int nextIndex = -1;
                double minDistance = Double.MAX_VALUE;

                // Buscar el cliente no visitado más cercano
                for (int j = 1; j < locations.size(); j++) {
                    if (!visited[j] && distanceMatrix[currentIndex][j] < minDistance) {
                        minDistance = distanceMatrix[currentIndex][j];
                        nextIndex = j;
                    }
                }

                if (nextIndex >= 0) {
                    orderedIndices.add(nextIndex);
                    visited[nextIndex] = true;
                    currentIndex = nextIndex;

                    // Log cada 50 clientes
                    if (i % 50 == 0) {
                        Log.d(TAG, "Pre-ordenados " + i + " clientes, próximo a " + String.format("%.1f km", minDistance / 1000.0));
                    }
                } else {
                    Log.w(TAG, "Error: no se pudo encontrar siguiente cliente");
                    break;
                }
            }

            Log.i(TAG, "╔════════════════════════════════════════════╗");
            Log.i(TAG, "║  PRE-ORDENAMIENTO COMPLETADO EXITOSAMENTE ║");
            Log.i(TAG, "║  Total clientes pre-ordenados: " + orderedIndices.size() + "          ║");
            Log.i(TAG, "║  Método: Nearest Neighbor + Haversine      ║");
            Log.i(TAG, "║  Sin límite de API, cálculo local          ║");
            Log.i(TAG, "╚════════════════════════════════════════════╝");

            // 4. Reconstruir lista de clientes en nuevo orden
            ArrayList<Cliente> clientesOrdenados = new ArrayList<>();
            for (int idx : orderedIndices) {
                // Validar índice
                if (idx > 0 && idx <= clientes.size()) {
                    clientesOrdenados.add(clientes.get(idx - 1)); // -1 porque índice 0 es la base
                } else {
                    Log.w(TAG, "Índice fuera de rango en pre-ordenamiento: " + idx);
                }
            }

            return clientesOrdenados;

        } catch (Exception e) {
            Log.e(TAG, "Error en pre-ordenamiento por proximidad: " + e.getMessage());
            e.printStackTrace();
            // Retornar orden original en caso de error
            return clientes;
        }
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

        // Geolocalización
        public String geolocalizationStatus;  // "✓ OK", "⚠ SIN COORDENADAS", "❌ COORDENADAS INVÁLIDAS"
        public String latitud;                // Para referencia
        public String longitud;               // Para referencia
    }
}
