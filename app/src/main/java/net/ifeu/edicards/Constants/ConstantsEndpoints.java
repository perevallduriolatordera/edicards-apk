package net.ifeu.edicards.Constants;

public class ConstantsEndpoints {

    public static final String WS_FPAGO = "http://edicards.ddns.net:81/Dades.asmx/BuscarFPago";
    public static final String WS_FPAGO_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarFPagoZip";
    public static final String WS_TIPO_IVA = "http://edicards.ddns.net:81/Dades.asmx/BuscarIva";
    public static final String WS_TIPO_IVA_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarIvaZip";
    public static final String WS_ARTICULO = "http://edicards.ddns.net:81/Dades.asmx/BuscarArticles";
    public static final String WS_ARTICULO_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarArticlesZip";
    public static final String WS_CLIENTES = "http://edicards.ddns.net:81/Dades.asmx/BuscarClients";
    public static final String WS_CLIENTES_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarClientsZip";
    public static final String WS_TARIFAS = "http://edicards.ddns.net:81/Dades.asmx/BuscarTarifes";
    public static final String WS_TARIFAS_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarTarifesZip";
    public static final String WS_PACTOS = "http://edicards.ddns.net:81/Dades.asmx/BuscarPactes";
    public static final String WS_PACTOS_ZIP ="http://edicards.ddns.net:81/Dades.asmx/BuscarPactesZip";
    public static final String WS_DEPOSITOS_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarDipositsZip";
    public static final String WS_TOTAL_DEPOSITOS = "http://edicards.ddns.net:81/Dades.asmx/DipositsPendents";
    public static final String WS_DEPOSITOS_PAGINACION = "http://edicards.ddns.net:81/Dades.asmx/DipositsBuscar";
    public static final String WS_TRASPASO_STOCK = "http://edicards.ddns.net:81/Dades.asmx/BuscarTraspasStock";
    public static final String WS_VALIDAR_TRASPASO = "http://edicards.ddns.net:81/Dades.asmx/ValidarTraspasStock";

    public static final String WS_ENVIAR_ARTICULOS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentArticles";
    public static final String WS_ENVIAR_GASTOS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentDespeses";
    public static final String WS_ENVIAR_DEPOSITOS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentDiposits";
    public static final String WS_ENVIAR_ALBARANES = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentAlbarans";
    public static final String WS_ENVIAR_STOCKS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentStocks";
    //public static final String WS_ENVIAR_STOCK_DIARIO = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentStockFurgo";

    // OpenAI ChatGPT API
    // IMPORTANTE: La API key se obtiene desde variable de entorno CHATGPT_API_KEY
    // NO hardcodear la clave en el código por razones de seguridad
    public static final String CHATGPT_API_URL = "https://api.openai.com/v1/chat/completions";
    public static final String CHATGPT_API_KEY = System.getenv("CHATGPT_API_KEY") != null ?
        System.getenv("CHATGPT_API_KEY") : "";
    public static final String CHATGPT_MODEL = "gpt-3.5-turbo";

    // OpenRouteService API
    // IMPORTANTE: La API key se obtiene desde BuildConfig (inyectada desde local.properties en build.gradle)
    // Guardada en base64 con formato JSON: {"org":"...","id":"...","h":"..."}
    // La estrategia de geocoding extrae automáticamente el campo "id"
    public static String ORS_API_KEY = null; // Se inicializa en AppConfig
    public static final String ORS_GEOCODING_URL = "https://api.openrouteservice.org/geocode/search";
    public static final String ORS_MATRIX_URL = "https://api.openrouteservice.org/v2/matrix/driving-car";

    // Google Maps Routes API
    // Inyectada desde local.properties en build.gradle
    public static String GOOGLE_MAPS_API_KEY = null; // Se inicializa en AppConfig
    public static final String GOOGLE_MAPS_ROUTES_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

    // Google Route Optimization API
    // Optimización de rutas profesional con algoritmo OR-Tools
    // Pricing: $10 USD por 1000 requests (~€0.55/mes para uso típico)
    // Documentación: https://developers.google.com/maps/documentation/route-optimization
    public static final String GOOGLE_ROUTE_OPTIMIZATION_URL = "https://routes.googleapis.com/v2:optimizeTours";

    // Configuración: Seleccionar servicio de optimización de rutas
    // Opciones: "GOOGLE" (Google Maps Routes API con waypoint optimization), "ORS" (OpenRouteService)
    public static final String ROUTE_OPTIMIZER_SERVICE = "GOOGLE";

    public static final int WS_PAGINACION = 500;
    public static final int WS_MAX_INTENTOS = 500;

}
