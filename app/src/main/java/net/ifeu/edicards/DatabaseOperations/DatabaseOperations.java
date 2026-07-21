package net.ifeu.edicards.DatabaseOperations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.library.Database.DatabaseConnection;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;


public class DatabaseOperations {
	
	private DatabaseConnection _databaseConnection;
	private Context _context;
		
	public void openDB(Context context) throws Exception {
		_context = context;
		
		try {
			_databaseConnection = new DatabaseConnection(_context, ConstantsDatabase.DATABASE_NAME,ConstantsDatabase.DATABASE_VERSION);
			_databaseConnection.openDB();
		}
		catch (Exception e)
		{
			throw new Exception("Error abriendo objeto de conexión a base de datos. Motivo: " + e.getMessage());
		}
		
		// Creem l'estructura de base de dades en el cas de que sigui necessari.
		
		//if (!existsTable(ConstantsDatabase.TABLE_CLIENTES)) {
		if (false) {
			_databaseConnection.closeDB();
			boolean resultRestore = this.restoreDatabase(ConstantsDatabase.DATABASE_NAME);
			if (!resultRestore) {
				_databaseConnection = new DatabaseConnection(_context,ConstantsDatabase.DATABASE_NAME,ConstantsDatabase.DATABASE_VERSION);
				_databaseConnection.openDB();
				createDatabaseStructure();
			} else {

				try {

					_databaseConnection = new DatabaseConnection(_context,ConstantsDatabase.DATABASE_NAME,ConstantsDatabase.DATABASE_VERSION);
					_databaseConnection.openDB();
					this.createDatabaseStructureIfNecessary();
				}
				catch (Exception e)
				{
					throw new Exception("Error abriendo objeto de conexión a base de datos. Motivo: " + e.getMessage());
				}
			}
		} else {
			this.createDatabaseStructureIfNecessary();
		}

	}
	
	public void closeDB()
	{
		try {
			_databaseConnection.closeDB();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	public void createDatabaseStructure() throws Exception
	{
		dropTables();
		createTables();
		createIndexs();
		alterStructure();
	}
	
	public void createDatabaseStructureIfNecessary() throws Exception
	{
		createTables();
		createIndexs();
		alterStructure();
	}

	public boolean isOpen()
	{
		if (_databaseConnection == null)
			return false;

		return _databaseConnection.getDatabase().isOpen();
	}

	/**
	 * Obtiene la base de datos SQLite directamente
	 * @return SQLiteDatabase
	 */
	public android.database.sqlite.SQLiteDatabase getDatabase() {
		if (_databaseConnection != null) {
			return _databaseConnection.getDatabase();
		}
		return null;
	}

	private static void copyFile(FileInputStream fromFile, FileOutputStream toFile) throws IOException {
		FileChannel fromChannel = null;
		FileChannel toChannel = null;
		try {
			fromChannel = fromFile.getChannel();
			toChannel = toFile.getChannel();
			fromChannel.transferTo(0, fromChannel.size(), toChannel);
		} finally {
			try {
				if (fromChannel != null) {
					fromChannel.close();
				}
			} finally {
				if (toChannel != null) {
					toChannel.close();
				}
			}
		}
	}

	public boolean restoreDatabase(String databaseName) {
		String backupFileName = "/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_DB_BACKUP + "/" +
				databaseName;

		FileInputStream inputStreamNewDB;
		try {
			inputStreamNewDB = new FileInputStream(backupFileName);
		} catch (FileNotFoundException e1) {
			return false;
		}

		final File oldDB = _context.getDatabasePath(ConstantsDatabase.DATABASE_NAME);

		if (inputStreamNewDB != null) {
			try {
				copyFile((FileInputStream) inputStreamNewDB, new FileOutputStream(oldDB));
			} catch (IOException e) {
				return false;
			}
		} else {
			return false;
		}

		return true;
	}

	public void backupDatabase(String databaseName) throws IOException {
		final File dbFile = _context.getDatabasePath(ConstantsDatabase.DATABASE_NAME);
	    
	    FileInputStream fis = null;
		try {
			fis = new FileInputStream(dbFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	    String outFileName = "/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_DB_BACKUP + "/" +
				databaseName;
	    
	    final File currentDb = new File(outFileName);
	    currentDb.delete();

	    // Open the empty db as the output stream
	    OutputStream output = null;
		try {
			output = new FileOutputStream(outFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

	    // Transfer bytes from the inputfile to the outputfile
	    byte[] buffer = new byte[1024];
	    int length;
	    while ((length = fis.read(buffer))>0){
	        try {
				output.write(buffer, 0, length);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }

	    // Close the streams
	    output.flush();
	    output.close();
	    fis.close();
	}

	public Date getLastbackupDatabase() throws IOException {
		String outFileName = "/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_DB_BACKUP + "/" +
				ConstantsDatabase.DATABASE_NAME;

		final File currentDb = new File(outFileName);
		if (currentDb.exists())
			return new Date(currentDb.lastModified());
		else
			return null;
	}
	
	public boolean existsTable(String tableName)
	{
		Cursor cursor = _databaseConnection.getDatabase().rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '"+ tableName +"'", null);
	    if(cursor!=null) {
	        if(cursor.getCount()>0) {
	            cursor.close();
	            return true;
	        }
	         cursor.close();
	    }
	    return false;
	}

	public Cursor executeSentence(String sentence) throws Exception
	{
		Cursor cursor;
		cursor = _databaseConnection.getDatabase().rawQuery(sentence, null);

		boolean exists = cursor.moveToFirst();
		
		if (!exists) cursor.close();
		
		return exists ? cursor : null;
	}

	public Cursor getFirstRecordFromField(String table, String field, String filterText,boolean isText) throws Exception
	{
		Cursor cursor;
		
		if (isText)
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = '" + filterText + "'", null, null, null, null);
		else
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText , null, null, null, null);
		
		boolean exists = cursor.moveToFirst();
		
		return exists ? cursor : null;
	}
	
	public Cursor getRecordsFromField(String table, String field, String filterText, boolean isText, String otherConditions, String orderByField)
	{
		Cursor cursor = null;
		
		if (field.equals(ConstantsTypes.EMPTY_STRING) && otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, null, null, null,null, orderByField);
		else if (field.equals(ConstantsTypes.EMPTY_STRING) && !otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, "1 = 1 " + otherConditions, null, null, null, orderByField);
		
		else
			if (isText && otherConditions.equals(ConstantsTypes.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = '" + filterText + "'", null, null, null, orderByField );
			else if (isText && !otherConditions.equals(ConstantsTypes.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = '" + filterText + "' " + otherConditions, null, null, null, orderByField);
			else if (!isText && !otherConditions.equals(ConstantsTypes.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText + " " + otherConditions, null, null, null, orderByField);
			else if (!isText && otherConditions.equals(ConstantsTypes.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText, null, null, null, orderByField);
		
		return cursor;
	}

	public Cursor getRecordsFromFieldNumeric(String table, String field, String filterText, String otherConditions, String orderBy)
	{
		Cursor cursor = null;
		
		if (field.equals(ConstantsTypes.EMPTY_STRING) && otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, null, null, null, null, orderBy);
		else if (field.equals(ConstantsTypes.EMPTY_STRING) && !otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, "1 = 1 " + otherConditions, null, null, null, orderBy);
		else
			if (otherConditions.equals(ConstantsTypes.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText, null, null, null, orderBy);
			else if (!otherConditions.equals(ConstantsTypes.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText + " " + otherConditions, null, null, null, orderBy);
		return cursor;
	}
	
	public LinkedList<String> getStringArrayByField(String table, String fieldShow, String fieldFilter, String text, boolean onlyStartsWith, String otherConditions, String orderBy)
	{
		Cursor cursor = null;
		
		if (text.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, "1 = 1 " + otherConditions, null, null, null, orderBy);
		else if (onlyStartsWith && otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '" + text + "% '" + otherConditions, null, null, null, orderBy);
		else if (!onlyStartsWith && otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '%" + text + "%' " + otherConditions, null, null, null, orderBy);
		else if (onlyStartsWith && !otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '" + text + "%' " + otherConditions, null, null, null, orderBy);
		else if (!onlyStartsWith && !otherConditions.equals(ConstantsTypes.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '%" + text + "%' " + otherConditions, null, null, null, orderBy);	
		
		LinkedList<String> result = new LinkedList<>();
		
		if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			
			do {
				result.add(cursor.getString(0));
			}while (cursor.moveToNext());
		}
		
		cursor.close();
		return result; 
		
	}
	
	public int getRecordsCount(String table)
	{
		Cursor cursor = _databaseConnection.getDatabase().rawQuery("SELECT Count(*) FROM " + table, null);
		cursor.moveToFirst();
		int count = cursor.getInt(0);
		cursor.close();
		
		return count;
		
	}
	
	public long insert(String table, String nullColumnHack, ContentValues values) throws Exception
	{
	
		if (!_databaseConnection.getDatabase().isOpen())
			throw new Exception("La base de datos no está abierta.");
		
		try {
			long rowId = _databaseConnection.getDatabase().insert(table, nullColumnHack, values);
			
			if (rowId == -1)
				throw new Exception("Se ha producido un error al insertar en la base de datos en la tabla " + table);
			else
				return rowId;
			
		}
		
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void update(String table, ContentValues values, String where, String[] whereArgs) throws Exception
	{
		if (!_databaseConnection.getDatabase().isOpen())
			throw new Exception("La base de datos no está abierta.");
		
		try {
			_databaseConnection.getDatabase().update(table, values, where, whereArgs);
		}
		
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void createTables()
	{
		try {
			createClientesTable();
			createArticulosTable();
			createMovimientosAlmacenTable();
			createDepositosTable();
			createLineasDepositoTable();
			createTarifasTable();
			createPactosTable();
			createTiposIVATable();
			createFormasPagoTable();
			createGastosTable();
			createHistoricosTable();
			createLineasHistoricoTable();
			createContadoresTable();
			createEfectivoTable();
			createClientesInfoTable();
			createGastosInfoTable();
			createIngresosTable();
			createGDPRTable();
			createLogBookTable();
			createIngresosDiariosTable();
			createLogBookExceptionsTable();
			createCiudadVendedorTable();
			createRutasGeneradasTable();
			createZonasTable();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	
	private void createIndexs() {
		try {
			createIndex(ConstantsDatabase.INDEX_DEPOSITO_CODIGOCLIENTE, ConstantsDatabase.TABLE_DEPOSITOS, 
					 new ArrayList<>(Arrays.asList("CodigoCliente")));
			createIndex(ConstantsDatabase.INDEX_DEPOSITO_NUMDOC, ConstantsDatabase.TABLE_DEPOSITOS, 
					 new ArrayList<>(Arrays.asList("NumDoc")));
			createIndex(ConstantsDatabase.INDEX_DEPOSITO_IDCLIENTE, ConstantsDatabase.TABLE_DEPOSITOS, 
					 new ArrayList<>(Arrays.asList("IdCliente")));
			createIndex(ConstantsDatabase.INDEX_DEPOSITO_FECHADEPOSITO, ConstantsDatabase.TABLE_DEPOSITOS, 
					 new ArrayList<>(Arrays.asList("FechaDeposito")));
			createIndex(ConstantsDatabase.INDEX_LINEADEPOSITO_IDDEPOSITO, ConstantsDatabase.TABLE_LINEAS_DEPOSITO, 
					 new ArrayList<>(Arrays.asList("IdDeposito")));
			createIndex(ConstantsDatabase.INDEX_LINEADEPOSITO_IDDEPOSITO_IDARTICULO, ConstantsDatabase.TABLE_LINEAS_DEPOSITO, 
					 new ArrayList<>(Arrays.asList("IdDeposito","IdArticulo")));
			createIndex(ConstantsDatabase.INDEX_TARIFA_IDARTICULO_CODIGOTARIFA, ConstantsDatabase.TABLE_TARIFAS, 
					 new ArrayList<>(Arrays.asList("IdArticulo","CodigoTarifa")));
			createIndex(ConstantsDatabase.INDEX_PACTOS_IDARTICULO, ConstantsDatabase.TABLE_PACTOS, 
					 new ArrayList<>(Arrays.asList("IdArticulo")));
			createIndex(ConstantsDatabase.INDEX_ARTICULOS_ACTIVO_TIPO, ConstantsDatabase.TABLE_ARTICULOS, 
					 new ArrayList<>(Arrays.asList("Activo","Tipo")));
			createIndex(ConstantsDatabase.INDEX_LOGBOOK_FECHA, ConstantsDatabase.TABLE_LOGBOOK,
					new ArrayList<>(Arrays.asList("Fecha")));
			createIndex(ConstantsDatabase.INDEX_CIUDADVENDEDOR_USUARIO, ConstantsDatabase.TABLE_CIUDAD_VENDEDOR,
					new ArrayList<>(Arrays.asList("Usuario")));
			createIndex(ConstantsDatabase.INDEX_RUTAS_FECHA, ConstantsDatabase.TABLE_RUTAS_GENERADAS,
					new ArrayList<>(Arrays.asList("FechaGeneracion")));
			createIndex(ConstantsDatabase.INDEX_RUTAS_ORDEN, ConstantsDatabase.TABLE_RUTAS_GENERADAS,
					new ArrayList<>(Arrays.asList("OrdenVisita")));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void dropTables()
	{
		try {
			dropTable(ConstantsDatabase.TABLE_CLIENTES);
			dropTable(ConstantsDatabase.TABLE_ARTICULOS);
			dropTable(ConstantsDatabase.TABLE_MOVIMIENTOS_ALMACEN);
			dropTable(ConstantsDatabase.TABLE_DEPOSITOS);
			dropTable(ConstantsDatabase.TABLE_LINEAS_DEPOSITO);
			dropTable(ConstantsDatabase.TABLE_TARIFAS);
			dropTable(ConstantsDatabase.TABLE_PACTOS);
			dropTable(ConstantsDatabase.TABLE_TIPOS_IVA);
			dropTable(ConstantsDatabase.TABLE_FORMAS_PAGO);
			dropTable(ConstantsDatabase.TABLE_GASTOS);
			dropTable(ConstantsDatabase.TABLE_HISTORICOS);
			dropTable(ConstantsDatabase.TABLE_LINEAS_HISTORICO);
			dropTable(ConstantsDatabase.TABLE_CONTADORES);
			dropTable(ConstantsDatabase.TABLE_EFECTIVO);
			dropTable(ConstantsDatabase.TABLE_CLIENTES_INFO);
			dropTable(ConstantsDatabase.TABLE_GASTOS_INFO);
			dropTable(ConstantsDatabase.TABLE_INGRESOS);
			dropTable(ConstantsDatabase.TABLE_GDPR);
			dropTable(ConstantsDatabase.TABLE_LOGBOOK);
			dropTable(ConstantsDatabase.TABLE_INGRESOS_DIARIOS);
			dropTable(ConstantsDatabase.TABLE_LOGBOOK_EXCEPTIONS);
			dropTable(ConstantsDatabase.TABLE_ZONAS);

		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	
	public void deleteAllRecords(String table) throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().delete(table, null, null);
			} 
			catch (Exception e) {
				throw new Exception("Error borrando los registros de la tabla " + table + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error borrando los registros de la tabla " + table + " . Motivo: La Base de datos no ha podido ser abierta.");
		}
	}
	
	public int deleteRecordsByKeyValueString(String table, String key, String value) throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				return _databaseConnection.getDatabase().delete(table, key + " = ?", new String[] { value });
			} 
			catch (Exception e) {
				throw new Exception("Error borrando los registros de la tabla " + table + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error borrando los registros de la tabla " + table + " . Motivo: La Base de datos no ha podido ser abierta.");
		}
	}
	
	private void createIndex(String index, String table, ArrayList<String> fields) throws Exception {
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create index if not exists " + index + " on " + table + "(" + android.text.TextUtils.join(",", fields) + ")");
			} 
			catch (Exception e) {
				throw new Exception("Error creando índice " + index + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando indice " + index + " . Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void dropTable(String table) throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("drop table if exists " + table);
			} 
			catch (Exception e) {
				throw new Exception("Error eliminando la tabla " + table + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error eliminando la tabla " + table + " . Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createClientesTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_CLIENTES + " ( IdCliente integer primary key autoincrement, "
														  + "Activo integer, "
						                                  + "CodigoCliente text not null, "
						                                  + "NIF text not null, "
						                                  + "Telefono1 text, "
						                                  + "Telefono2 text, "
						                                  + "Razon text not null, "
						                                  + "Direccion1 text not null, "
						                                  + "Direccion2 text, " 
						                                  + "CodigoPostal text not null, "
						                                  + "Poblacion text not null, "
						                                  + "Provincia text not null, "
						                                  + "Fax text, "
						                                  + "Clave text, "
						                                  + "Mail text, "
						                                  + "Web text, " 
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real, "
						                                  + "DescuentoProntoPago real, "
						                                  + "DescuentoFinanciero real, "
						                                  + "Filiacion text, "
						                                  + "IdFormaPago integer, "
						                                  + "CodigoTarifa integer, "
						                                  + "Latitud real, "
						                                  + "Longitud real, "
						                                  + "FechaGeocodificacion datetime, "
														  + "Nombre text not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_CLIENTES + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_CLIENTES + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createDepositosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_DEPOSITOS + " ( IdDeposito integer primary key autoincrement, "
														  + "FechaDeposito date default CURRENT_DATE, "
														  + "Ejercicio text not null, "
														  + "IdCliente integer not null, "
						                                  + "CodigoCliente text not null, "
						                                  + "NIF text not null, "
						                                  + "Telefono1 text, "
						                                  + "Telefono2 text, "
						                                  + "Razon text not null, "
						                                  + "Direccion1 text not null, "
						                                  + "Direccion2 text, " 
						                                  + "Poblacion text not null, "
						                                  + "CodigoPostal text not null, "
						                                  + "Provincia text not null, "
						                                  + "Fax text, "
						                                  + "Clave text, "
						                                  + "Mail text, "
						                                  + "Web text, "
						                                  + "NumDoc text, " 
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real, "
						                                  + "IdTipoIVA integer, "
						                                  + "TipoDeposito text, "
														  + "Nombre text not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_DEPOSITOS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_DEPOSITOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createLineasDepositoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO + " ( IdLineaDeposito integer primary key autoincrement, "
														  + "IdDeposito integer not null, "
						                                  + "IdArticulo integer not null, "
						                                  + "UnidadesIniciales integer not null, "
						                                  + "PVP real, "
						                                  + "PVPAnterior real, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createHistoricosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_HISTORICOS + " ( IdHistorico integer primary key autoincrement, "
														  + "Fecha date default CURRENT_DATE, "
														  + "IdCliente integer not null, "
						                                  + "Serie text, "
						                                  + "NumeroAlbaran text, "
						                                  + "NombrePresentacion text, "
						                                  + "PoblacionPresentacion text, "
						                                  + "CodigoPostalPresentacion text, "
						                                  + "CantidadPagada real, "
						                                  + "Total real, "
						                                  + "GUID text, "
						                                  + "Serializacion text, "
														  + "Tipo integer);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_HISTORICOS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_HISTORICOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createLineasHistoricoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_LINEAS_HISTORICO + " ( IdLineaHistorico integer primary key autoincrement, "
														  + "IdHistorico integer not null, "
						                                  + "IdArticulo integer not null, "
						                                  + "Unidades integer not null, "
						                                  + "MovimientoStock integer not null, "
						                                  + "MovimientoStockDefectuosas integer not null, "
						                                  + "Tipo integer not null); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LINEAS_DEPOSITO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createTarifasTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_TARIFAS + " ( IdTarifa integer primary key autoincrement, "
														  + "CodigoTarifa text not null, "
														  + "IdArticulo integer not null, "
														  + "FechaIni date, "
														  + "FechaFin date, "
						                                  + "PVP real, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_TARIFAS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_TARIFAS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createPactosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_PACTOS + " ( IdPacto integer primary key autoincrement, "
														  + "IdArticulo integer not null, "
						                                  + "IdCliente integer not null, "
						                                  + "PVP real, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_PACTOS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_PACTOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
	}
	
	private void createArticulosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_ARTICULOS + " ( IdArticulo integer primary key autoincrement, "
						                                  + "CodigoArticulo text not null, "
						                                  + "Descripcion text not null, "
						                                  + "PVP real, "
						                                  + "Familia text not null, "
						                                  + "FamiliaCorta text, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real, "
						                                  + "TipoIVA text, "
						                                  + "Stock integer, "
						                                  + "StockDefectuoso integer, "
						                                  + "Activo integer, "
						                                  + "Tipo integer);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_ARTICULOS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_ARTICULOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createMovimientosAlmacenTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_MOVIMIENTOS_ALMACEN + " ( IdMovimiento integer primary key autoincrement, "
						                                  + "IdArticulo integer not null, "
						                                  + "Entradas integer not null, "
						                                  + "Salidas integer not null, "
						                                  + "Fecha date default CURRENT_DATE, "
						                                  + "TipoStock integer not null, "
						                                  + "Tipo integer not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_MOVIMIENTOS_ALMACEN + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_MOVIMIENTOS_ALMACEN + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createTiposIVATable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_TIPOS_IVA + " ( IdTipoIVA integer primary key autoincrement, "
						                                  + "Filiacion text not null, "
						                                  + "Articulo text not null, "
						                                  + "Fecha date not null, "
						                                  + "Descripcion,"
						                                  + "Impuesto text not null, "
						                                  + "Recargo real not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_TIPOS_IVA + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_TIPOS_IVA + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createFormasPagoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_FORMAS_PAGO + " ( IdFormaPago integer primary key autoincrement, "
						                                  + "CodigoFormaPago text not null, "
						                                  + "Descripcion text not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_FORMAS_PAGO + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_FORMAS_PAGO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createGastosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_GASTOS + " ( IdGasto integer primary key autoincrement, "
														  + "IdArticulo integer not null, "
														  + "Fecha date, "
						                                  + "Cantidad real); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_TARIFAS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_TARIFAS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createContadoresTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_CONTADORES + " ( IdContador integer primary key autoincrement, "
														  + "ContadorSerieA integer not null, "
						                                  + "ContadorSerieB integer not null); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_CONTADORES + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_CONTADORES + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}

	private void createEfectivoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_EFECTIVO + " ( IdEfectivo integer primary key autoincrement, "
						+ "Efectivo real not null, "
						+ "UpdateDateIngreso date not null, "
						+ "UpdateDateEfectivo date not null); ");

			}
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_EFECTIVO + ". Motivo: " + e.getMessage());
			}

		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_CONTADORES + ". Motivo: La Base de datos no ha podido ser abierta.");
		}

	}
	
	private void createClientesInfoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_CLIENTES_INFO + " ( IdCliente integer primary key not null , "
														  + "Representante text, " 
														  + "DniRepresentante text, " 
						                                  + "CCC text); ");
															
			} 
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_CLIENTES_INFO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createGastosInfoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_GASTOS_INFO + " ( IdGastoInfo integer primary key autoincrement , "
														  + "Fecha date, " 
						                                  + "Comentario text); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_GASTOS_INFO + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_GASTOS_INFO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createIngresosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_INGRESOS + " ( IdIngreso integer primary key autoincrement, "
														  + "Entidad text not null, "
														  + "Referencia text not null, "
														  + "Descripcion text not null, "
														  + "Fecha date, "
						                                  + "Cantidad real); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_INGRESOS + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_INGRESOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}

	private void createIngresosDiariosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_INGRESOS_DIARIOS + " ( IdIngreso integer primary key autoincrement, "
						+ "Fecha date, "
						+ "Ingresos real, "
						+ "Gastos real, "
						+ "Cantidad real); ");

			}
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_INGRESOS + ". Motivo: " + e.getMessage());
			}

		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_INGRESOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}

	}
	
	private void createGDPRTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_GDPR + " ( IdGDPR integer primary key autoincrement, "
														  + "CodigoCliente text not null); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_GDPR + ". Motivo: " + e.getMessage());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_GDPR + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}

	private void createLogBookTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_LOGBOOK + " ( IdLogBook integer primary key autoincrement, "
						+ "Fecha date default CURRENT_DATE," +
						  "TipoMovimiento text not null," +
						  "CodigoCliente text not null," +
						  "NombreCliente text not null," +
						  "CodigoArticulo text not null," +
						  "NombreArticulo text not null," +
						  "StockInicial integer not null," +
						  "StockFinal integer not null," +
						  "UnidadesDevueltas integer," +
						  "UnidadesDefectuosas integer," +
						  "UnidadesRepuestas integer," +
						  "UnidadesFacturadas integer," +
						  "UnidadesIniciales integer," +
						  "UnidadesAbono integer, " +
						  "UnidadesDefectuosasAbono integer); ");
			}
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LOGBOOK + ". Motivo: " + e.getMessage());
			}

		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LOGBOOK + ". Motivo: La Base de datos no ha podido ser abierta.");
		}

	}

	private void createLogBookExceptionsTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + ConstantsDatabase.TABLE_LOGBOOK_EXCEPTIONS + " ( IdLogBook integer primary key autoincrement, "
						+ "Fecha date default CURRENT_DATE,"
						+ "Label text not null,"
						+ " Message text not null); ");
			}
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LOGBOOK + ". Motivo: " + e.getMessage());
			}

		}
		else {
			throw new Exception("Error creando la tabla " + ConstantsDatabase.TABLE_LOGBOOK + ". Motivo: La Base de datos no ha podido ser abierta.");
		}

	}

	private void createCiudadVendedorTable() throws Exception {
		if (_databaseConnection.getDatabase().isOpen()) {
			try {
				_databaseConnection.getDatabase().execSQL(
					"create table if not exists " + ConstantsDatabase.TABLE_CIUDAD_VENDEDOR +
					" ( IdCiudadVendedor integer primary key autoincrement, " +
					"Usuario text not null, " +
					"CiudadBase text not null, " +
					"CodigoPostal text, " +
					"FechaCreacion date default CURRENT_DATE);"
				);
			} catch (Exception e) {
				throw new Exception("Error creando la tabla " +
					ConstantsDatabase.TABLE_CIUDAD_VENDEDOR + ". Motivo: " + e.getMessage());
			}
		} else {
			throw new Exception("Error creando la tabla " +
				ConstantsDatabase.TABLE_CIUDAD_VENDEDOR +
				". Motivo: La Base de datos no ha podido ser abierta.");
		}
	}

	private void createRutasGeneradasTable() throws Exception {
		if (_databaseConnection.getDatabase().isOpen()) {
			try {
				_databaseConnection.getDatabase().execSQL(
					"create table if not exists " + ConstantsDatabase.TABLE_RUTAS_GENERADAS +
					" ( IdRuta integer primary key autoincrement, " +
					"FechaGeneracion date default CURRENT_DATE, " +
					"OrdenVisita integer not null, " +
					"CodigoCliente text not null, " +
					"NombreCliente text not null, " +
					"DireccionCliente text, " +
					"PoblacionCliente text, " +
					"ProvinciaCliente text, " +
					"DistanciaEstimada text, " +
					"CiudadBase text not null, " +
					"GeolocalizationStatus text, " +
					"Latitud text, " +
					"Longitud text);"
				);
			} catch (Exception e) {
				throw new Exception("Error creando la tabla " +
					ConstantsDatabase.TABLE_RUTAS_GENERADAS + ". Motivo: " + e.getMessage());
			}
		} else {
			throw new Exception("Error creando la tabla " +
				ConstantsDatabase.TABLE_RUTAS_GENERADAS +
				". Motivo: La Base de datos no ha podido ser abierta.");
		}
	}

	private void createZonasTable() throws Exception {
		if (_databaseConnection.getDatabase().isOpen()) {
			try {
				_databaseConnection.getDatabase().execSQL(
					"create table if not exists " + ConstantsDatabase.TABLE_ZONAS +
					" ( IdZona integer primary key autoincrement, " +
					"NombreZona text not null, " +
					"Activa integer not null default 1);"
				);
			} catch (Exception e) {
				throw new Exception("Error creando la tabla " +
					ConstantsDatabase.TABLE_ZONAS + ". Motivo: " + e.getMessage());
			}
		} else {
			throw new Exception("Error creando la tabla " +
				ConstantsDatabase.TABLE_ZONAS +
				". Motivo: La Base de datos no ha podido ser abierta.");
		}
	}

	private void alterStructure()  {
		addGeocodingColumnsIfNecessary();

		if (_databaseConnection.getDatabase().isOpen())
		{
			// Añadir columna IdZona a tabla Clientes
			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_CLIENTES + " ADD COLUMN IdZona integer default null ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			// Añadir columna ClusterID a tabla RutasGeneradas
			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_RUTAS_GENERADAS + " ADD COLUMN ClusterID integer NOT NULL default 0 ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_LINEAS_HISTORICO + " ADD COLUMN PVP REAL default null ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_ARTICULOS + " ADD COLUMN StockPropio integer NOT NULL default 1 ");
			} catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_HISTORICOS + " ADD COLUMN ActualizarStock integer NOT NULL default 1 ");
			} catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_ARTICULOS + " ADD COLUMN EAN text  ");
			} catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_INGRESOS_DIARIOS + " ADD COLUMN FechaRegistro date default null ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}
				try {
					_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_RUTAS_GENERADAS + " ADD COLUMN GeolocalizationStatus text default null ");
				}
				catch (Exception e) {
					if (!e.getMessage().startsWith("duplicate column name"))
						throw new RuntimeException(e);
				}
			
				try {
					_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_RUTAS_GENERADAS + " ADD COLUMN Latitud text default null ");
				}
				catch (Exception e) {
					if (!e.getMessage().startsWith("duplicate column name"))
						throw new RuntimeException(e);
				}
			
				try {
					_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_RUTAS_GENERADAS + " ADD COLUMN Longitud text default null ");
				}
				catch (Exception e) {
					if (!e.getMessage().startsWith("duplicate column name"))
						throw new RuntimeException(e);
				}
		}
		else {
			throw new RuntimeException("Error añadiendo columnas. Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}

	// Agregar columnas de geocodificación a tabla Clientes
	private void addGeocodingColumnsIfNecessary() {
		if (_databaseConnection.getDatabase().isOpen()) {
			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_CLIENTES + " ADD COLUMN Latitud real default null ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_CLIENTES + " ADD COLUMN Longitud real default null ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + ConstantsDatabase.TABLE_CLIENTES + " ADD COLUMN FechaGeocodificacion datetime default null ");
			}
			catch (Exception e) {
				if (!e.getMessage().startsWith("duplicate column name"))
					throw new RuntimeException(e);
			}
		}
	}

}
