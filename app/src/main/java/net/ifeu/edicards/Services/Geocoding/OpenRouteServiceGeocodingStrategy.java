package net.ifeu.edicards.Services.Geocoding;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import net.ifeu.edicards.Constants.ConstantsEndpoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Implementación de geocodificación usando OpenRouteService
 * Convierte direcciones en coordenadas (latitud, longitud)
 * Con retry logic y manejo mejorado de errores
 */
public class OpenRouteServiceGeocodingStrategy implements IGeocodingStrategy {

    private static final String TAG = "GeocodingStrategy";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1500; // 1.5 segundos entre reintentos

    // Caché en memoria para evitar llamadas duplicadas
    private static final Map<String, LatLng> geocodeCache = new HashMap<>();

    // Variables para sincronización de llamada asíncrona
    private JSONObject apiResponse = null;
    private Exception apiException = null;
    private int lastErrorCode = 0;

    @Override
    public LatLng geocodeAddress(String address, String city, String province, String postalCode) throws Exception {
        if (address == null || address.trim().isEmpty()) {
            Log.w(TAG, "Dirección vacía, no se puede geocodificar");
            return null;
        }

        // Construir clave de caché
        String cacheKey = buildCacheKey(address, city, province, postalCode);

        // Buscar en caché
        if (geocodeCache.containsKey(cacheKey)) {
            LatLng cached = geocodeCache.get(cacheKey);
            Log.d(TAG, "✓ Coordenadas obtenidas de caché: " + cacheKey);
            return cached;
        }

        // Construir dirección completa
        StringBuilder fullAddress = new StringBuilder();
        fullAddress.append(address);
        if (city != null && !city.isEmpty()) {
            fullAddress.append(", ").append(city);
        }
        if (province != null && !province.isEmpty()) {
            fullAddress.append(", ").append(province);
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            fullAddress.append(", ").append(postalCode);
        }

        Log.d(TAG, "Geocodificando: " + fullAddress.toString());

        // Llamar a ORS Geocoding API con retry logic
        LatLng result = callOrsGeocodingAPIWithRetry(fullAddress.toString());

        if (result != null) {
            // Guardar en caché
            geocodeCache.put(cacheKey, result);
            Log.d(TAG, "✓ Coordenadas obtenidas y cacheadas: " + result.toString());
        } else {
            Log.w(TAG, "✗ No se encontraron coordenadas para: " + fullAddress.toString());
        }

        return result;
    }

    @Override
    public String getName() {
        return "OpenRouteService";
    }

    /**
     * Llamada a ORS Geocoding API con retry logic para manejar errores 403 y 429
     */
    private LatLng callOrsGeocodingAPIWithRetry(String address) throws Exception {
        int attempt = 1;
        long delayMs = RETRY_DELAY_MS;

        while (attempt <= MAX_RETRY_ATTEMPTS) {
            try {
                Log.d(TAG, "Intento " + attempt + "/" + MAX_RETRY_ATTEMPTS + " - Geocodificando: " + address);

                LatLng result = callOrsGeocodingAPI(address);

                // Si tiene éxito, retornar
                if (result != null) {
                    return result;
                }

                // Si fue un error 403 y hay más intentos, reintentar con delay
                if (lastErrorCode == 403 && attempt < MAX_RETRY_ATTEMPTS) {
                    Log.w(TAG, "Error 403 en intento " + attempt + ". Esperando " + delayMs + "ms antes de reintentar...");
                    Thread.sleep(delayMs);
                    delayMs *= 2; // Exponential backoff: 1.5s -> 3s -> 6s
                    attempt++;
                    continue;
                }

                // Si fue otro error o último intento, retornar null
                return null;

            } catch (InterruptedException e) {
                // Si se interrumpe sleep, relanzar
                throw new Exception("Geocodificación interrumpida", e);
            } catch (Exception e) {
                // Si hay excepción y quedan intentos, reintentar
                if (attempt < MAX_RETRY_ATTEMPTS && (lastErrorCode == 403 || lastErrorCode == 429)) {
                    Log.w(TAG, "Error en intento " + attempt + ": " + e.getMessage() + ". Esperando antes de reintentar...");
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                    attempt++;
                    continue;
                }
                // Si no quedan intentos o error diferente, lanzar
                throw e;
            }
        }

        Log.e(TAG, "Agotados todos los intentos (" + MAX_RETRY_ATTEMPTS + ") para geocodificar: " + address);
        return null;
    }

    /**
     * Llamada sincrónica a ORS Geocoding API usando CountDownLatch
     */
    private LatLng callOrsGeocodingAPI(String address) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        try {
            if (ConstantsEndpoints.ORS_API_KEY == null || ConstantsEndpoints.ORS_API_KEY.isEmpty()) {
                throw new Exception("ORS_API_KEY no está configurada. Verifica local.properties o variables de entorno.");
            }

            // Decodificar API key si está en base64
            String apiKeyToUse = decodeApiKeyIfNeeded(ConstantsEndpoints.ORS_API_KEY);

            // URL encode de la dirección
            String encodedAddress = URLEncoder.encode(address, "UTF-8");

            String url = ConstantsEndpoints.ORS_GEOCODING_URL +
                    "?api_key=" + apiKeyToUse +
                    "&text=" + encodedAddress;

            Log.d(TAG, "URL completa (sin key): " + ConstantsEndpoints.ORS_GEOCODING_URL + "?api_key=***&text=" + encodedAddress);

            AndroidNetworking.get(url)
                    .setPriority(Priority.MEDIUM)
                    .addHeaders("User-Agent", "Edicards-Android/1.0")
                    .addHeaders("Accept", "application/json")
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            apiResponse = response;
                            apiException = null;
                            lastErrorCode = 0;
                            Log.d(TAG, "✓ Respuesta exitosa de ORS Geocoding");
                            latch.countDown();
                        }

                        @Override
                        public void onError(ANError error) {
                            apiResponse = null;
                            String errorMsg = error.getMessage() != null ? error.getMessage() : "Error desconocido";
                            lastErrorCode = error.getErrorCode();

                            String detailedError = "ORS Geocoding Error (" + lastErrorCode + "): " + errorMsg;
                            if (lastErrorCode == 401) {
                                detailedError = "Error de autenticación (401): Verifica que ORS_API_KEY sea válida";
                            } else if (lastErrorCode == 403) {
                                detailedError = "Error Forbidden (403): API Key puede tener restricciones o limit reached";
                            } else if (lastErrorCode == 429) {
                                detailedError = "Error de límite de rate (429): Demasiadas solicitudes a OpenRouteService";
                            } else if (lastErrorCode == -1) {
                                detailedError = "Error de conexión de red: " + errorMsg;
                            }

                            apiException = new Exception(detailedError);
                            Log.w(TAG, "⚠ ORS Error: " + detailedError);
                            latch.countDown();
                        }
                    });

            // Esperar respuesta (máximo 30 segundos)
            if (!latch.await(30, TimeUnit.SECONDS)) {
                lastErrorCode = 408; // Timeout
                throw new Exception("Timeout esperando respuesta de ORS Geocoding API (30 segundos)");
            }

            if (apiException != null) {
                throw apiException;
            }

            // Parsear respuesta
            if (apiResponse != null && apiResponse.has("features")) {
                JSONArray features = apiResponse.getJSONArray("features");

                if (features.length() > 0) {
                    JSONObject firstFeature = features.getJSONObject(0);

                    if (firstFeature.has("geometry")) {
                        JSONObject geometry = firstFeature.getJSONObject("geometry");

                        if (geometry.has("coordinates")) {
                            JSONArray coordinates = geometry.getJSONArray("coordinates");

                            if (coordinates.length() >= 2) {
                                // ORS retorna [longitude, latitude]
                                Double longitude = coordinates.getDouble(0);
                                Double latitude = coordinates.getDouble(1);

                                Log.d(TAG, "✓ Coordenadas parseadas: lat=" + latitude + ", lon=" + longitude);
                                lastErrorCode = 0;
                                return new LatLng(latitude, longitude);
                            }
                        }
                    }
                }
            }

            Log.w(TAG, "Respuesta vacía o sin features de ORS");
            return null;

        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrumpido: " + e.getMessage());
            throw new Exception("Geocodificación interrumpida", e);
        } catch (Exception e) {
            Log.e(TAG, "Error en geocodificación: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Construye una clave única para caché basada en la dirección
     */
    private String buildCacheKey(String address, String city, String province, String postalCode) {
        StringBuilder key = new StringBuilder();
        if (address != null) key.append(address);
        if (city != null) key.append("|").append(city);
        if (province != null) key.append("|").append(province);
        if (postalCode != null) key.append("|").append(postalCode);
        return key.toString();
    }

    /**
     * Limpia el caché (útil para testing o reset)
     */
    public static void clearCache() {
        geocodeCache.clear();
        Log.d(TAG, "Caché de geocodificación limpiado");
    }

    /**
     * Force re-geocodification by clearing coordinates in database
     */
    public static void forceReGeocoding(android.content.Context context) {
        try {
            net.ifeu.edicards.Application.AppConfig app = (net.ifeu.edicards.Application.AppConfig) context.getApplicationContext();
            app.getDatabaseOperations().executeSentence(
                "UPDATE Clientes SET Latitud = NULL, Longitud = NULL, FechaGeocodificacion = NULL"
            );
            clearCache();
            android.util.Log.i(TAG, "Re-geocodificación forzada - coordenadas limpiadas");
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error limpiando coordenadas: " + e.getMessage());
        }
    }

    /**
     * Retorna el tamaño del caché
     */
    public static int getCacheSize() {
        return geocodeCache.size();
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
                        Log.d(TAG, "✓ API key decodificada correctamente - ID: " + id + " (length: " + id.length() + ")");
                        return id;
                    } else {
                        Log.w(TAG, "JSON no tiene campo 'id'");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "No se pudo parsear JSON de API key decodificada: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "API key no está en base64 o error al decodificar: " + e.getMessage());
        }

        // Si no es base64 o no se pudo decodificar, retornar la clave original
        Log.w(TAG, "Usando API key original (length: " + apiKey.length() + ")");
        return apiKey;
    }
}
