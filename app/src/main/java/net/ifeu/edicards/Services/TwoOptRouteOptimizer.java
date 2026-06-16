package net.ifeu.edicards.Services;

import android.util.Log;

import java.util.ArrayList;

/**
 * Optimizador de rutas usando algoritmo 2-opt
 * Mejora sobre Nearest Neighbor: 10-20% más eficiente
 * Sin dependencias externas - puro Java
 */
public class TwoOptRouteOptimizer {

    private static final String TAG = "TwoOptRouteOptimizer";

    /**
     * Aplica mejora 2-opt a una ruta existente
     * El algoritmo intercambia pares de aristas para reducir distancia total
     *
     * @param route Ruta inicial (típicamente de Nearest Neighbor)
     * @param distanceMatrix Matriz de distancias
     * @return Ruta mejorada
     */
    public static ArrayList<Integer> optimize(ArrayList<Integer> route, double[][] distanceMatrix) {
        if (route == null || route.size() < 4) {
            Log.d(TAG, "Ruta muy pequeña, no aplicar 2-opt");
            return route;
        }

        long startTime = System.currentTimeMillis();
        ArrayList<Integer> improved = new ArrayList<>(route);
        boolean mejorado = true;
        int iteraciones = 0;
        // Escalar máximo de iteraciones según tamaño de ruta
        int maxIteraciones = Math.min(500, improved.size() * 10); // Más agresivo para rutas pequeñas

        Log.d(TAG, "=== INICIANDO 2-OPT OPTIMIZATION ===");
        Log.d(TAG, "Ruta inicial: " + improved.size() + " nodos, máx iteraciones: " + maxIteraciones);

        // Calcular distancia inicial
        double distanciaInicial = calcularDistanciaTotal(improved, distanceMatrix);
        Log.d(TAG, "Distancia inicial: " + String.format("%.0f metros (%.1f km)", distanciaInicial, distanciaInicial / 1000.0));

        // Iterar hasta que no haya mejoras o alcance max iteraciones
        while (mejorado && iteraciones < maxIteraciones) {
            mejorado = false;
            iteraciones++;

            // Probar todos los pares de aristas
            for (int i = 0; i < improved.size() - 3; i++) {
                for (int k = i + 2; k < improved.size() - 1; k++) {
                    // Calcular mejora si invertimos segmento [i+1, k]
                    double delta = calcularDeltaMejora(improved, distanceMatrix, i, k);

                    if (delta < -0.1) { // Si hay mejora significativa (más de 0.1m)
                        // Invertir segmento
                        invertirSegmento(improved, i + 1, k);
                        mejorado = true;
                        // No hacer break - continuar buscando mejoras en esta iteración
                    }
                }
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        double distanciaFinal = calcularDistanciaTotal(improved, distanceMatrix);
        double mejora = (distanciaInicial - distanciaFinal) / distanciaInicial * 100.0;

        Log.i(TAG, "=== 2-OPT COMPLETADO ===");
        Log.i(TAG, "Iteraciones: " + iteraciones);
        Log.i(TAG, "Tiempo: " + elapsedTime + "ms");
        Log.i(TAG, "Distancia inicial: " + String.format("%.0f m", distanciaInicial));
        Log.i(TAG, "Distancia final: " + String.format("%.0f m", distanciaFinal));
        Log.i(TAG, "✓ Mejora: " + String.format("%.1f%% (%d metros)", mejora, (long)(distanciaInicial - distanciaFinal)));

        return improved;
    }

    /**
     * Calcula el cambio en distancia si se invierte el segmento [i, k]
     */
    private static double calcularDeltaMejora(ArrayList<Integer> route, double[][] distanceMatrix, int i, int k) {
        int n = route.size();

        // Nodos actuales
        int a = route.get(i);
        int b = route.get(i + 1);
        int c = route.get(k);
        int d = route.get((k + 1) % n);

        // Distancia actual: a -> b ... c -> d
        double distanciaActual = distanceMatrix[a][b] + distanceMatrix[c][d];

        // Distancia después de invertir: a -> c ... b -> d
        double distanciaNew = distanceMatrix[a][c] + distanceMatrix[b][d];

        return distanciaNew - distanciaActual;
    }

    /**
     * Invierte el segmento de la ruta entre i y k (inclusive)
     */
    private static void invertirSegmento(ArrayList<Integer> route, int i, int k) {
        while (i < k) {
            int temp = route.get(i);
            route.set(i, route.get(k));
            route.set(k, temp);
            i++;
            k--;
        }
    }

    /**
     * Calcula la distancia total de una ruta
     */
    private static double calcularDistanciaTotal(ArrayList<Integer> route, double[][] distanceMatrix) {
        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            int from = route.get(i);
            int to = route.get(i + 1);
            if (from >= 0 && from < distanceMatrix.length && to >= 0 && to < distanceMatrix.length) {
                total += distanceMatrix[from][to];
            }
        }
        return total;
    }
}
