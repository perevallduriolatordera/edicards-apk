package net.ifeu.edicards.Services;

/**
 * Callback para generación asíncrona de rutas
 */
public interface RouteGenerationCallback {

    /**
     * Llamado cuando la ruta se genera exitosamente
     */
    void onRouteGenerated();

    /**
     * Llamado cuando hay error
     * @param error Mensaje de error
     */
    void onError(String error);

    /**
     * Llamado para actualizar el progreso
     * @param mensaje Mensaje de progreso
     */
    void onProgress(String mensaje);
}
