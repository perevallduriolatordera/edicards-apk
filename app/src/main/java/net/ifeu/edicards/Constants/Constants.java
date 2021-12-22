package net.ifeu.edicards.Constants;

public final class Constants {

	public static final int CUSTOMER_FILTER_NAME = 0;
	public static final int CUSTOMER_FILTER_NIF = 1;
	public static final int CUSTOMER_FILTER_PHONE = 2;
	public static final int CUSTOMER_FILTER_CITY = 3;
	
	public static final int ARTICLE_FILTER_NAME = 0;
	
	public static final String TABLE_CLIENTES = "Clientes";
	public static final String TABLE_ARTICULOS = "Articulos";
	public static final String TABLE_MOVIMIENTOS_ALMACEN = "MovimientosAlmacen";
	public static final String TABLE_DEPOSITOS = "Depositos";
	public static final String TABLE_LINEAS_DEPOSITO = "LineasDeposito";
	public static final String TABLE_TARIFAS = "Tarifas";
	public static final String TABLE_PACTOS = "Pactos";
	public static final String TABLE_TIPOS_IVA = "TiposIVA";
	public static final String TABLE_FORMAS_PAGO = "FormasPago";
	public static final String TABLE_GASTOS = "Gastos";
	public static final String TABLE_HISTORICOS = "Historicos";
	public static final String TABLE_LINEAS_HISTORICO = "LineaHistorico";
	public static final String TABLE_CONTADORES = "Contadores";
	public static final String TABLE_CLIENTES_INFO = "ClientesInfo";
	public static final String TABLE_GASTOS_INFO = "GastosInfo";
	public static final String TABLE_INGRESOS = "Ingresos";
	public static final String TABLE_GDPR = "GDPR";
	
	public static final String INDEX_DEPOSITO_CODIGOCLIENTE = "idx_Deposito_CodigoCliente";
	public static final String INDEX_DEPOSITO_NUMDOC = "idx_Deposito_NumDoc";
	public static final String INDEX_DEPOSITO_IDCLIENTE = "idx_Deposito_IdCliente";
	public static final String INDEX_DEPOSITO_FECHADEPOSITO = "idx_Deposito_FechaDeposito";
	public static final String INDEX_LINEADEPOSITO_IDDEPOSITO = "idx_LineaDeposito_IdDeposito";
	public static final String INDEX_LINEADEPOSITO_IDDEPOSITO_IDARTICULO = "idx_LineaDeposito_IdDep_idArt";
	public static final String INDEX_TARIFA_IDARTICULO_CODIGOTARIFA = "idx_Tarifa_IdArt_codTar";
	public static final String INDEX_PACTOS_IDARTICULO = "idx_Pactos_IdArticulo";
	public static final String INDEX_ARTICULOS_ACTIVO_TIPO = "idx_Articulos_Activo_Tipo";
	
	
	
	public static final String TIPO_DEPOSITO_CONVENCIONAL = "1";
	public static final String TIPO_DEPOSITO_CAMPANA = "2";
	
	public static final String SERIE_A_VALUE = "1";
	public static final String SERIE_B_VALUE = "2";
	
	public static final String FOLDER_ROOT = "Edicards";
	public static final String FOLDER_FIRMAS = "Firmas";
	public static final String FOLDER_STOCK = "Stock";
	public static final String FOLDER_GASTOS = "Gastos";
	public static final String FOLDER_DEPOSITOS = "Depositos";
	public static final String FOLDER_ALBARANES = "Albaranes";
	public static final String FOLDER_TRACE = "Traza";
	public static final String FOLDER_JSON_PRINT = "Impresion";
	public static final String FOLDER_PDF = "PDF";
	public static final String FOLDER_INVENTARIO = "Inventario";
	public static final String FOLDER_INCIDENCIAS = "Incidencias";
	public static final String FOLDER_AUTORIZACIONES = "Autorizaciones";
	public static final String FOLDER_DB_BACKUP = "DBBackup";
	public static final String FOLDER_RECUENTO = "Recuento";
	public static final String FOLDER_DAILYSTOCK = "StockDiario";
	public static final String FOLDER_GDPR = "GDPR";
	public static final String FOLDER_SERVICES = "Servicios";
	
	public static final String FILE_FIRMAS = "Firma.jpg";
	public static final String FILE_FIRMA_COMERCIAL = "FirmaComercial.jpg";
	public static final String FILE_STOCK = "Stock.xml";
	public static final String FILE_RECUENTO = "Recuento.xml";
	public static final String FILE_DAILY_STOCK =  "StockDiario.xml";
	
	public static String DATABASE_NAME = "Edicards.db";
	public  static int DATABASE_VERSION = 2;
	
	public static final String EMPTY_STRING = "";
	
	public static final int REQUEST_SEARCH_CUSTOMER = 1;
	
	//public static final String NEW_LINE = System.getProperty("line.separator");
	public static final String NEW_LINE = String.format("\n");
	//public static final String NEW_LINE_MAIL =  "</BR>";
	
	public static final String NEW_CUSTOMER_CODE = "99";
	
	public static final String WS_FPAGO = "http://edicards.ddns.net:81/Dades.asmx/BuscarFPago";
	public static final String WS_FPAGO_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarFPagoZip";
	public static final String WS_TIPO_IVA = "http://edicards.ddns.net:81/Dades.asmx/BuscarIva";
	public static final String WS_TIPO_IVA_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarIvaZip";
	public static final String WS_ARTICULO = "http://edicards.ddns.net:81/Dades.asmx/BuscarArticles";
	public static final String WS_ARTICULO_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarArticlesZip";
	public static final String WS_ARTICULO_STOCK = "http://edicards.ddns.net:81/Dades.asmx/BuscarArticlesStock";
	public static final String WS_ARTICULO_STOCK_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarArticlesStockZip";
	public static final String WS_CLIENTES = "http://edicards.ddns.net:81/Dades.asmx/BuscarClients";
	public static final String WS_CLIENTES_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarClientsZip";
	public static final String WS_TARIFAS = "http://edicards.ddns.net:81/Dades.asmx/BuscarTarifes";
	public static final String WS_TARIFAS_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarTarifesZip";
	public static final String WS_PACTOS = "http://edicards.ddns.net:81/Dades.asmx/BuscarPactes";
	public static final String WS_PACTOS_ZIP ="http://edicards.ddns.net:81/Dades.asmx/BuscarPactesZip";
	public static final String WS_DEPOSITOS = "http://edicards.ddns.net:81/Dades.asmx/BuscarDiposits";
	public static final String WS_DEPOSITOS_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarDipositsZip";
	public static final String WS_TOTAL_DEPOSITOS = "http://edicards.ddns.net:81/Dades.asmx/DipositsPendents";
	public static final String WS_DEPOSITOS_PAGINACION = "http://edicards.ddns.net:81/Dades.asmx/DipositsBuscar";
	public static final String WS_TRASPASO_STOCK = "http://edicards.ddns.net:81/Dades.asmx/BuscarTraspasStock";
	public static final String WS_TRASPASO_STOCK_ZIP = "http://edicards.ddns.net:81/Dades.asmx/BuscarTraspasStockZip";
	public static final String WS_VALIDAR_TRASPASO = "http://edicards.ddns.net:81/Dades.asmx/ValidarTraspasStock";
	
	public static final String WS_ENVIAR_ARTICULOS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentArticles";
	public static final String WS_ENVIAR_GASTOS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentDespeses";
	public static final String WS_ENVIAR_DEPOSITOS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentDiposits";
	public static final String WS_ENVIAR_ALBARANES = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentAlbarans";
	public static final String WS_ENVIAR_STOCKS = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentStocks";
	//public static final String WS_ENVIAR_STOCK_DIARIO = "http://edicards.ddns.net:8808/ServeiDimoni.asmx/EnviamentStockFurgo";
	
	public static final int WS_PAGINACION = 500;
	public static final int WS_MAX_INTENTOS = 500;
	
	public static final int TIPO_HISTORICO_CLIENTE_EXISTENTE = 1;
	public static final int TIPO_HISTORICO_CLIENTE_NUEVO = 2;
	public static final int TIPO_HISTORICO_CLIENTE_BAJA = 3;
			
	public static final int TIPO_LINEA_HISTORICO_FACTURADAS = 1;
	public static final int TIPO_LINEA_HISTORICO_POTENCIADAS = 2;
	public static final int TIPO_LINEA_HISTORICO_BAJAS = 3;
	public static final int TIPO_LINEA_HISTORICO_DEFECTUOSAS = 4;
	public static final int TIPO_LINEA_HISTORICO_STOCK = 5;
	public static final int TIPO_LINEA_HISTORICO_UNIDADES_INICIALES = 6;
	
	public static final String MAIL_HOST = "smtp.gmail.com";
	public static final String MAIL_PORT = "465";
	public static final String MAIL_SPORT = "465";
	//public static final String MAIL_USER = "testtabletedi@gmail.com";
	//public static final String MAIL_PASSWORD = "tablet2013";
	
	public static final String MAIL_USER = "edicardssender@gmail.com";
	public static final String MAIL_PASSWORD = "ger0svk0";
	
	public static final String MAIL_SUBJECT = "Envio desde dispositivo movil";
	public static final String MAIL_TO = "testtabletedi@gmail.com";
	public static final String MAIL_TO_GDPR = "gdprtabletedi@gmail.com";
	public static final String MAIL_TO_INCIDENCIAS = "incidenciastabletedi@gmail.com";
	//public static final String MAIL_TO = "valldu@hotmail.com";
	public static final String MAIL_FROM = "testtabletedi@gmail.com";
	public static final String MAIL_BODY = "Enviado desde dispositivo movil";
	
//	public static final String MAIL_HOST = "smtp.edicards.com";
//	public static final String MAIL_PORT = "110";
//	public static final String MAIL_SPORT = "110";
//	public static final String MAIL_USER = "grupediciones@grupediciones.com";
//	public static final String MAIL_PAS	SWORD = "ge0000";
//	public static final S.tring MAIL_SUBJECT = "Envio desde dispositivo movil";
//	public static final String MAIL_TO = "valldu@hotmail.com";
//	public static final String MAIL_FROM = "grupediciones@grupediciones.com";
//	public static final String MAIL_BODY = "Enviado desde dispositivo movil";
	
	//public static final String MAIL_ADMINISTRACION = "valldu@hotmail.com";
	//public static final String MAIL_ADMINISTRACION_2 = "valldu@hotmail.com";

	public static final String MAIL_ADMINISTRACION = "comercial@edicards.com";
	public static final String MAIL_ADMINISTRACION_2 = "ester@edicards.com";
	public static final String MAIL_FACTURACION = "facturacion@edicards.com";
	public static final String MAIL_ENVIOS_EDICARDS = "almacenedicards@gmail.com";
	
	public static final String MANAGER_PASSWORD = "manager";
	
	public static final int MAXIMO_SIN_INGRESAR = 1200;
	
	public static final String PARSE_APP_ID = "jb1BQzgUAgGGnoHJ0b22RcGWZgLTRV6OLJgkav2M";
	public static final String PARSE_DEVELOPER_ID = "jHbKPdgOJz9tujwh2nztJgffn96QcgBIWrdWCmJY";
	public static final String PARSE_VERSION_CHANNEL = "Versiones";
	public static final String PARSE_VERSION_TEXT = "Hay disponible una nueva versión del aplicativo de Gestión " + 
			"Comercial Edicards. Descárgala cuando te sea posible.";
		

	public static final String FIRECLOUD_URL_TOKEN = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyB2-3jRoQOt3cCEOmFKEs2BVJAYkDCHrvI";
	public static final String FIRECLOUD_EMAIL = "edicardssender@gmail.com";
	public static final String FIRECLOUD_PASSWORD = "ger0svk0";
	public static final String FIRECLOUD_URL_DATABASE = "https://firestore.googleapis.com/v1/projects/edicards-stock/databases/(default)/documents/articulos";
	public static final String FIRECLOUD_URL_BASE = "https://firestore.googleapis.com/v1/";

	public static final String PREFILL_COMPANY = "E_ESTERJ";
	public static final String PREFILL_INVOICE_A = "A";
	public static final String PREFILL_INVOICE_B = "B";

	public static final int TIPO_DOCUMENTO_DEPOSITO = 1;
	public static final int TIPO_DOCUMENTO_ALBARAN = 2;
}
