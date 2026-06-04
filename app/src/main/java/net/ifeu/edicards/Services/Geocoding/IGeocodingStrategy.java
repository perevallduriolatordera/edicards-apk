package net.ifeu.edicards.Services.Geocoding;

/**
 * Interface que define una estrategia de geocodificación
 * Permite cambiar fácilmente entre diferentes proveedores de geocoding
 */
public interface IGeocodingStrategy {

    /**
     * Geocodifica una dirección y retorna las coordenadas
     *
     * @param address Dirección o calle principal
     * @param city Ciudad/Población
     * @param province Provincia
     * @param postalCode Código postal
     * @return LatLng con las coordenadas (latitude, longitude), o null si falla
     * @throws Exception Si hay error en la llamada a la API
     */
    LatLng geocodeAddress(String address, String city, String province, String postalCode) throws Exception;

    /**
     * Retorna el nombre de la estrategia para logging y debugging
     * @return Nombre de la estrategia (ej: "OpenRouteService", "GoogleGeocoding")
     */
    String getName();
}
