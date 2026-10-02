package net.ifeu.edicards.Services.Geocoding;

import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import net.ifeu.edicards.Constants.ConstantsEndpoints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Estrategia de geocodificación usando Google Maps Geocoding API
 * https://developers.google.com/maps/documentation/geocoding
 *
 * Con retry logic y manejo mejorado de errores
 */
public class GoogleMapsGeocodingStrategy implements IGeocodingStrategy {

    private static final String TAG = "GoogleMapsGeocoding";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1500; // 1.5 segundos entre reintentos

    @Override
    public String getName() {
        return "GoogleMaps";
    }

    @Override
    public LatLng geocodeAddress(String address, String city, String province, String postalCode) throws Exception {

        // Construir dirección completa
        StringBuilder fullAddress = new StringBuilder();

        if (address != null && !address.trim().isEmpty()) {
            fullAddress.append(address.trim());
        }

        if (city != null && !city.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(city.trim());
        }

        if (province != null && !province.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(province.trim());
        }

        if (postalCode != null && !postalCode.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(postalCode.trim());
        }

        if (fullAddress.length() == 0) {
            throw new Exception("No se proporcionó ninguna parte de la dirección para geocodificar");
        }

        Log.d(TAG, "Geocodificando dirección completa: " + fullAddress.toString());

        // Llamar a Google Maps Geocoding API con retry logic
        LatLng result = callGoogleGeocodingAPIWithRetry(fullAddress.toString());

        if (result != null) {
            Log.i(TAG, "✓ Geocodificación exitosa: " + result.getLatitude() + ", " + result.getLongitude());
        } else {
            Log.w(TAG, "✗ No se pudo geocodificar la dirección");
        }

        return result;
    }

    /**
     * Llamada a Google Maps Geocoding API con retry logic para manejar errores temporales
     */
    private LatLng callGoogleGeocodingAPIWithRetry(String address) throws Exception {
        int attempt = 1;
        long delayMs = RETRY_DELAY_MS;
        int lastErrorCode = 0;

        while (attempt <= MAX_RETRY_ATTEMPTS) {
            try {
                Log.d(TAG, "Intento " + attempt + "/" + MAX_RETRY_ATTEMPTS + " - Geocodificando: " + address);

                LatLng result = callGoogleGeocodingAPI(address);

                if (result != null) {
                    if (attempt > 1) {
                        Log.i(TAG, "✓ Geocodificación exitosa en intento " + attempt);
                    }
                    return result;
                }

                // Si result es null pero no hubo excepción, la dirección no se encontró
                if (lastErrorCode == 0) {
                    Log.w(TAG, "Dirección no encontrada en Google Maps");
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error en geocodificación: " + e.getMessage());

                // Si es el último intento, lanzar la excepción
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    throw e;
                }

                // Reintentar con errores temporales (rate limit, timeout, etc)
                if (e.getMessage() != null &&
                    (e.getMessage().contains("429") ||
                     e.getMessage().contains("Timeout") ||
                     e.getMessage().contains("conexión"))) {

                    Log.w(TAG, "Error temporal detectado. Reintentando en " + delayMs + "ms...");
                    Thread.sleep(delayMs);
                    delayMs *= 2; // Backoff exponencial
                } else {
                    // Error no recuperable
                    throw e;
                }
            }

            attempt++;
        }

        Log.e(TAG, "Agotados todos los intentos (" + MAX_RETRY_ATTEMPTS + ") para geocodificar: " + address);
        throw new Exception("No se pudo geocodificar la dirección después de " + MAX_RETRY_ATTEMPTS + " intentos");
    }

    /**
     * Llamada individual a Google Maps Geocoding API
     */
    private LatLng callGoogleGeocodingAPI(String address) throws Exception {

        // Validar que la API key esté configurada
        if (ConstantsEndpoints.GOOGLE_MAPS_API_KEY == null || ConstantsEndpoints.GOOGLE_MAPS_API_KEY.isEmpty()) {
            throw new Exception("GOOGLE_MAPS_API_KEY no está configurada. Verifica local.properties o variables de entorno.");
        }

        String apiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;

        // URL encode de la dirección
        String encodedAddress;
        try {
            encodedAddress = URLEncoder.encode(address, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Exception("Error al codificar dirección: " + e.getMessage());
        }

        // Construir URL
        String url = ConstantsEndpoints.GOOGLE_GEOCODING_URL +
                     "?address=" + encodedAddress +
                     "&key=" + apiKey;

        Log.d(TAG, "URL completa (sin key): " + ConstantsEndpoints.GOOGLE_GEOCODING_URL + "?address=" + encodedAddress + "&key=***");

        // Usar CountDownLatch para convertir la llamada asíncrona en síncrona
        final CountDownLatch latch = new CountDownLatch(1);
        final JSONObject[] apiResponse = {null};
        final Exception[] apiException = {null};
        final int[] lastErrorCode = {0};

        // Hacer petición GET a Google Maps Geocoding API
        AndroidNetworking.get(url)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        apiResponse[0] = response;
                        latch.countDown();
                    }

                    @Override
                    public void onError(ANError error) {
                        apiResponse[0] = null;
                        String errorMsg = error.getMessage() != null ? error.getMessage() : "Error desconocido";
                        lastErrorCode[0] = error.getErrorCode();

                        String detailedError = "Google Geocoding Error (" + lastErrorCode[0] + "): " + errorMsg;
                        if (lastErrorCode[0] == 401) {
                            detailedError = "Error de autenticación (401): Verifica que GOOGLE_MAPS_API_KEY sea válida";
                        } else if (lastErrorCode[0] == 403) {
                            detailedError = "Error Forbidden (403): API Key puede tener restricciones";
                        } else if (lastErrorCode[0] == 429) {
                            detailedError = "Error de límite de rate (429): Demasiadas solicitudes a Google Maps";
                        } else if (lastErrorCode[0] == -1) {
                            detailedError = "Error de conexión de red: " + errorMsg;
                        }

                        apiException[0] = new Exception(detailedError);
                        Log.w(TAG, "⚠ Google Maps Error: " + detailedError);
                        latch.countDown();
                    }
                });

        // Esperar respuesta (máximo 30 segundos)
        if (!latch.await(30, TimeUnit.SECONDS)) {
            lastErrorCode[0] = 408; // Timeout
            throw new Exception("Timeout esperando respuesta de Google Geocoding API (30 segundos)");
        }

        if (apiException[0] != null) {
            throw apiException[0];
        }

        // Parsear respuesta
        if (apiResponse[0] != null) {
            String status = apiResponse[0].optString("status", "");

            if ("OK".equals(status)) {
                JSONArray results = apiResponse[0].optJSONArray("results");

                if (results != null && results.length() > 0) {
                    JSONObject firstResult = results.getJSONObject(0);

                    if (firstResult.has("geometry")) {
                        JSONObject geometry = firstResult.getJSONObject("geometry");

                        if (geometry.has("location")) {
                            JSONObject location = geometry.getJSONObject("location");

                            if (location.has("lat") && location.has("lng")) {
                                Double latitude = location.getDouble("lat");
                                Double longitude = location.getDouble("lng");

                                // Verificar que las coordenadas sean válidas
                                if (latitude != null && longitude != null &&
                                    latitude != 0.0 && longitude != 0.0) {

                                    Log.d(TAG, "✓ Coordenadas obtenidas de Google Maps: " + latitude + ", " + longitude);
                                    return new LatLng(latitude, longitude);
                                }
                            }
                        }
                    }
                }

                Log.w(TAG, "Respuesta OK pero sin coordenadas válidas");
                return null;

            } else if ("ZERO_RESULTS".equals(status)) {
                Log.w(TAG, "No se encontraron resultados para: " + address);
                return null;

            } else if ("OVER_QUERY_LIMIT".equals(status)) {
                throw new Exception("Error 429: Límite de consultas excedido en Google Maps");

            } else if ("REQUEST_DENIED".equals(status)) {
                String errorMessage = apiResponse[0].optString("error_message", "Sin detalles");
                throw new Exception("Request denied: " + errorMessage + ". Verifica la API key y sus restricciones.");

            } else if ("INVALID_REQUEST".equals(status)) {
                throw new Exception("Request inválido: la dirección está mal formada");

            } else {
                throw new Exception("Status desconocido de Google Maps: " + status);
            }
        }

        throw new Exception("No se recibió respuesta de Google Maps Geocoding API");
    }
}
