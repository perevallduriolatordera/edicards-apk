package net.ifeu.edicards.Services;

import android.content.Context;
import android.util.Log;

import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.Services.Geocoding.LatLng;

import java.util.ArrayList;

/**
 * Servicio unificado para optimización de rutas
 * Permite cambiar fácilmente entre Google Route Optimization API y OpenRouteService
 *
 * Configuración:
 * - Cambiar ConstantsEndpoints.ROUTE_OPTIMIZER_SERVICE entre "GOOGLE" y "ORS"
 *
 * Comparación:
 * - Google: Mejor optimización (95-98%), sin batching, €0.55/mes
 * - ORS: Buena optimización (85-90%), requiere batching, gratis
 */
public class UnifiedRouteOptimizer {

    private static final String TAG = "UnifiedRouteOptimizer";

    private Context context;
    private String googleApiKey;

    public UnifiedRouteOptimizer(Context context, String googleApiKey) {
        this.context = context;
        this.googleApiKey = googleApiKey;
    }

    /**
     * Optimiza ruta usando el servicio configurado en ConstantsEndpoints
     *
     * @param clientes Lista de clientes a visitar
     * @param baseLocation Ubicación de la base
     * @return Lista ordenada de clientes optimizada
     * @throws Exception Si hay error en la optimización
     */
    public ArrayList<RouteOptimizerService.RutaClienteData> optimizeRoute(
            ArrayList<Cliente> clientes,
            LatLng baseLocation) throws Exception {

        return optimizeRoute(clientes, baseLocation, null);
    }

    /**
     * Optimiza ruta usando el servicio configurado en ConstantsEndpoints
     *
     * @param clientes Lista de clientes a visitar
     * @param baseLocation Ubicación de la base
     * @param startingClient Cliente desde el que comenzar (null = comenzar desde base)
     * @return Lista ordenada de clientes optimizada
     * @throws Exception Si hay error en la optimización
     */
    public ArrayList<RouteOptimizerService.RutaClienteData> optimizeRoute(
            ArrayList<Cliente> clientes,
            LatLng baseLocation,
            Cliente startingClient) throws Exception {

        String service = ConstantsEndpoints.ROUTE_OPTIMIZER_SERVICE;

        Log.i(TAG, "╔═══════════════════════════════════════════════╗");
        Log.i(TAG, "║  UNIFIED ROUTE OPTIMIZER                     ║");
        Log.i(TAG, "║  Servicio seleccionado: " + service + "                  ║");
        Log.i(TAG, "║  Clientes a optimizar: " + clientes.size() + "               ║");
        Log.i(TAG, "╚═══════════════════════════════════════════════╝");

        if ("GOOGLE".equalsIgnoreCase(service)) {
            return optimizeWithGoogle(clientes, baseLocation);
        } else if ("ORS".equalsIgnoreCase(service)) {
            return optimizeWithORS(clientes, baseLocation, startingClient);
        } else {
            Log.w(TAG, "Servicio desconocido: " + service + ", usando Google por defecto");
            return optimizeWithGoogle(clientes, baseLocation);
        }
    }

    /**
     * Optimiza usando Google Maps Routes API con waypoint optimization
     */
    private ArrayList<RouteOptimizerService.RutaClienteData> optimizeWithGoogle(
            ArrayList<Cliente> clientes,
            LatLng baseLocation) throws Exception {

        Log.i(TAG, "→ Usando Google Maps Routes API (waypoint optimization)");

        GoogleMapsRouteOptimizer googleService =
            new GoogleMapsRouteOptimizer(context, googleApiKey);

        try {
            // Construir lista de waypoints (lat,lng como strings)
            ArrayList<String> waypoints = new ArrayList<>();

            // Agregar base
            waypoints.add(baseLocation.getLatitude() + "," + baseLocation.getLongitude());

            // Agregar clientes
            for (Cliente c : clientes) {
                if (c.Latitud != null && c.Longitud != null) {
                    waypoints.add(c.Latitud + "," + c.Longitud);
                }
            }

            Log.i(TAG, "Optimizando " + waypoints.size() + " waypoints con Google Maps");

            // Llamar al optimizador de Google Maps
            ArrayList<Integer> ordenOptimizado = googleService.optimizeRouteSynchronous(waypoints);

            if (ordenOptimizado == null || ordenOptimizado.isEmpty()) {
                Log.w(TAG, "⚠ Google devolvió resultado vacío, intentando fallback a ORS");
                return optimizeWithORS(clientes, baseLocation, null);
            }

            // Convertir orden optimizado a RutaClienteData
            ArrayList<RouteOptimizerService.RutaClienteData> resultado = new ArrayList<>();
            LatLng prevLocation = baseLocation;

            for (int i = 0; i < ordenOptimizado.size(); i++) {
                int idx = ordenOptimizado.get(i);

                // Saltar índice 0 (base)
                if (idx == 0) continue;

                // Obtener cliente (idx-1 porque 0 es base)
                if (idx - 1 >= clientes.size()) continue;

                Cliente cliente = clientes.get(idx - 1);

                RouteOptimizerService.RutaClienteData rutaCliente =
                    new RouteOptimizerService.RutaClienteData();

                rutaCliente.orden = resultado.size() + 1;
                rutaCliente.nif = cliente.NIF != null ? cliente.NIF : "";
                rutaCliente.razon = cliente.Razon != null ? cliente.Razon : "";
                rutaCliente.nombre = cliente.Nombre != null ? cliente.Nombre : "";
                rutaCliente.codigoCliente = cliente.CodigoCliente;
                rutaCliente.latitud = String.format("%.6f", cliente.Latitud);
                rutaCliente.longitud = String.format("%.6f", cliente.Longitud);
                rutaCliente.geolocalizationStatus = "✓ OK";

                // Calcular distancia aproximada
                LatLng currentLocation = new LatLng(cliente.Latitud, cliente.Longitud);
                double distKm = calcularDistanciaHaversine(prevLocation, currentLocation) / 1000.0;
                rutaCliente.distanciaKm = String.format("%.1f km", distKm);

                resultado.add(rutaCliente);
                prevLocation = currentLocation;
            }

            Log.i(TAG, "✓ Optimización con Google Maps completada exitosamente");
            return resultado;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error con Google Maps: " + e.getMessage());
            Log.w(TAG, "→ Intentando fallback a OpenRouteService...");
            return optimizeWithORS(clientes, baseLocation, null);
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
     * Optimiza usando OpenRouteService (fallback)
     */
    private ArrayList<RouteOptimizerService.RutaClienteData> optimizeWithORS(
            ArrayList<Cliente> clientes,
            LatLng baseLocation,
            Cliente startingClient) throws Exception {

        Log.i(TAG, "→ Usando OpenRouteService");

        RouteOptimizerService orsService = new RouteOptimizerService();

        ArrayList<RouteOptimizerService.RutaClienteData> result =
            orsService.optimizeRoute(clientes, baseLocation, startingClient);

        if (result != null && !result.isEmpty()) {
            Log.i(TAG, "✓ Optimización con ORS completada exitosamente");
        }

        return result;
    }
}
