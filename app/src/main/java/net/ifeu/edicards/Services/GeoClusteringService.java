package net.ifeu.edicards.Services;

import android.util.Log;

import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.Services.Geocoding.LatLng;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de clustering geográfico para agrupar clientes por zonas
 * Divide el área en un grid de cuadrantes y agrupa clientes por proximidad geográfica
 */
public class GeoClusteringService {

    private static final String TAG = "GeoClusteringService";

    /**
     * Representa un cluster geográfico de clientes
     */
    public static class GeoCluster {
        public int clusterId;
        public ArrayList<Cliente> clientes;
        public double centerLat;
        public double centerLon;
        public double minLat;
        public double maxLat;
        public double minLon;
        public double maxLon;

        public GeoCluster(int clusterId) {
            this.clusterId = clusterId;
            this.clientes = new ArrayList<>();
        }

        public void calculateBounds() {
            if (clientes.isEmpty()) return;

            minLat = clientes.get(0).Latitud;
            maxLat = clientes.get(0).Latitud;
            minLon = clientes.get(0).Longitud;
            maxLon = clientes.get(0).Longitud;

            for (Cliente cliente : clientes) {
                minLat = Math.min(minLat, cliente.Latitud);
                maxLat = Math.max(maxLat, cliente.Latitud);
                minLon = Math.min(minLon, cliente.Longitud);
                maxLon = Math.max(maxLon, cliente.Longitud);
            }

            // Centro del cluster
            centerLat = (minLat + maxLat) / 2;
            centerLon = (minLon + maxLon) / 2;
        }

        public double distanceToPoint(double lat, double lon) {
            return calculateHaversineDistance(centerLat, centerLon, lat, lon);
        }

        public int size() {
            return clientes.size();
        }
    }

    /**
     * Agrupa clientes en clusters geográficos usando grid de cuadrantes
     *
     * @param clientes Lista de clientes a agrupar (deben tener coordenadas válidas)
     * @param gridSize Tamaño del grid (ej: 3 = 3x3 = 9 cuadrantes)
     * @return Lista de clusters ordenados por distancia a la base
     */
    public ArrayList<GeoCluster> clusterizeClients(ArrayList<Cliente> clientes, int gridSize, LatLng baseLocation) {
        if (clientes == null || clientes.isEmpty()) {
            Log.w(TAG, "Lista de clientes vacía para clustering");
            return new ArrayList<>();
        }

        Log.i(TAG, "════════════════════════════════════════");
        Log.i(TAG, "INICIANDO CLUSTERING GEOGRÁFICO");
        Log.i(TAG, "Total clientes: " + clientes.size());
        Log.i(TAG, "Tamaño grid: " + gridSize + "x" + gridSize + " (" + (gridSize * gridSize) + " cuadrantes)");

        // 1. Calcular bounding box de todos los clientes
        double minLat = clientes.get(0).Latitud;
        double maxLat = clientes.get(0).Latitud;
        double minLon = clientes.get(0).Longitud;
        double maxLon = clientes.get(0).Longitud;

        for (Cliente cliente : clientes) {
            minLat = Math.min(minLat, cliente.Latitud);
            maxLat = Math.max(maxLat, cliente.Latitud);
            minLon = Math.min(minLon, cliente.Longitud);
            maxLon = Math.max(maxLon, cliente.Longitud);
        }

        Log.d(TAG, "Bounding box calculado:");
        Log.d(TAG, "  Lat: " + minLat + " a " + maxLat + " (rango: " + (maxLat - minLat) + ")");
        Log.d(TAG, "  Lon: " + minLon + " a " + maxLon + " (rango: " + (maxLon - minLon) + ")");

        // 2. Calcular tamaño de cada celda del grid
        double latCellSize = (maxLat - minLat) / gridSize;
        double lonCellSize = (maxLon - minLon) / gridSize;

        Log.d(TAG, "Tamaño de celda:");
        Log.d(TAG, "  Lat: " + latCellSize);
        Log.d(TAG, "  Lon: " + lonCellSize);

        // 3. Crear mapa de clusters por coordenadas de grid
        Map<String, GeoCluster> clusterMap = new HashMap<>();
        int clusterId = 0;

        for (Cliente cliente : clientes) {
            // Calcular índice de grid
            int latIndex = (int) Math.floor((cliente.Latitud - minLat) / latCellSize);
            int lonIndex = (int) Math.floor((cliente.Longitud - minLon) / lonCellSize);

            // Asegurar que estén dentro de bounds
            latIndex = Math.min(latIndex, gridSize - 1);
            lonIndex = Math.min(lonIndex, gridSize - 1);

            String gridKey = latIndex + "," + lonIndex;

            if (!clusterMap.containsKey(gridKey)) {
                GeoCluster newCluster = new GeoCluster(clusterId++);
                clusterMap.put(gridKey, newCluster);
            }

            clusterMap.get(gridKey).clientes.add(cliente);
        }

        Log.i(TAG, "Clusters geográficos creados: " + clusterMap.size());

        // 4. Calcular centros y filtrar clusters vacíos
        ArrayList<GeoCluster> clusters = new ArrayList<>();
        for (GeoCluster cluster : clusterMap.values()) {
            if (!cluster.clientes.isEmpty()) {
                cluster.calculateBounds();
                clusters.add(cluster);
                Log.d(TAG, "Cluster " + cluster.clusterId + ": " + cluster.size() + " clientes, centro=(" +
                        String.format("%.4f", cluster.centerLat) + ", " + String.format("%.4f", cluster.centerLon) + ")");
            }
        }

        // 5. Ordenar clusters por distancia a la base (visita los más cercanos primero)
        clusters.sort(new Comparator<GeoCluster>() {
            @Override
            public int compare(GeoCluster c1, GeoCluster c2) {
                double dist1 = c1.distanceToPoint(baseLocation.getLatitude(), baseLocation.getLongitude());
                double dist2 = c2.distanceToPoint(baseLocation.getLatitude(), baseLocation.getLongitude());
                return Double.compare(dist1, dist2);
            }
        });

        Log.i(TAG, "Clusters ordenados por distancia a la base");
        for (int i = 0; i < clusters.size(); i++) {
            GeoCluster c = clusters.get(i);
            double distToBase = c.distanceToPoint(baseLocation.getLatitude(), baseLocation.getLongitude());
            Log.i(TAG, "  [" + i + "] Cluster " + c.clusterId + ": " + c.size() + " clientes, distancia a base: " +
                    String.format("%.1f km", distToBase / 1000.0));
        }

        Log.i(TAG, "════════════════════════════════════════");

        return clusters;
    }

    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     * Retorna distancia en metros
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return distance * 1000; // Retornar en metros
    }
}
