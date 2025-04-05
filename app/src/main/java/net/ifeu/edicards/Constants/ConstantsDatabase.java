package net.ifeu.edicards.Constants;

public class ConstantsDatabase {

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
    public static final String TABLE_EFECTIVO = "Efectivo";
    public static final String TABLE_CLIENTES_INFO = "ClientesInfo";
    public static final String TABLE_GASTOS_INFO = "GastosInfo";
    public static final String TABLE_INGRESOS = "Ingresos";
    public static final String TABLE_INGRESOS_DIARIOS = "IngresosDiarios";
    public static final String TABLE_GDPR = "GDPR";
    public static final String TABLE_LOGBOOK = "LogBook";
    public static final String TABLE_LOGBOOK_EXCEPTIONS = "LogBookExceptions";
    public static final String INDEX_DEPOSITO_CODIGOCLIENTE = "idx_Deposito_CodigoCliente";
    public static final String INDEX_DEPOSITO_NUMDOC = "idx_Deposito_NumDoc";
    public static final String INDEX_DEPOSITO_IDCLIENTE = "idx_Deposito_IdCliente";
    public static final String INDEX_DEPOSITO_FECHADEPOSITO = "idx_Deposito_FechaDeposito";
    public static final String INDEX_LINEADEPOSITO_IDDEPOSITO = "idx_LineaDeposito_IdDeposito";
    public static final String INDEX_LINEADEPOSITO_IDDEPOSITO_IDARTICULO = "idx_LineaDeposito_IdDep_idArt";
    public static final String INDEX_TARIFA_IDARTICULO_CODIGOTARIFA = "idx_Tarifa_IdArt_codTar";
    public static final String INDEX_PACTOS_IDARTICULO = "idx_Pactos_IdArticulo";
    public static final String INDEX_ARTICULOS_ACTIVO_TIPO = "idx_Articulos_Activo_Tipo";
    public static final String INDEX_LOGBOOK_FECHA  = "idx_LogBook_Fecha";
    public static String DATABASE_NAME = "Edicards.db";

    public static String DATABASE_RESTOREPOINT_NAME = "Edicards.db";

    public static String DATABASE_BACKUP_NAME = "Edicards_backup.db";
    public  static int DATABASE_VERSION = 2;


}
