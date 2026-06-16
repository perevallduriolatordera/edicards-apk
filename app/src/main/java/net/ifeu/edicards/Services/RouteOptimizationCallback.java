package net.ifeu.edicards.Services;

import java.util.ArrayList;

/**
 * Callback para operaciones asíncronas de optimización de rutas
 */
public interface RouteOptimizationCallback {

    /**
     * Llamado cuando la optimización de ruta finaliza exitosamente
     * @param rutaOrdenada Lista de RutaClienteData optimizados
     */
    void onRouteOptimized(ArrayList<RouteOptimizerService.RutaClienteData> rutaOrdenada);

    /**
     * Llamado cuando hay error en la optimización
     * @param error Mensaje de error
     */
    void onError(String error);
}
