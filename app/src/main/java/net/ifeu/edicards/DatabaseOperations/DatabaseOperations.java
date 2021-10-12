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
import java.util.LinkedList;

import net.ifeu.edicards.Constants.Constants;
import net.ifeu.library.Database.DatabaseConnection;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;


public class DatabaseOperations {
	
	private DatabaseConnection _databaseConnection;
	private Context _context;
		
	public void openDB(Context context) throws Exception {
		_context = context;
		
		try {
			_databaseConnection = new DatabaseConnection(_context,Constants.DATABASE_NAME,Constants.DATABASE_VERSION);
			_databaseConnection.openDB();
		}
		catch (Exception e)
		{
			throw new Exception("Error abriendo objeto de conexión a base de datos. Motivo: " + e.getMessage().toString());
		}
		
		// Creem l'estructura de base de dades en el cas de que sigui necessari.
		
		if (!existsTable(Constants.TABLE_CLIENTES)) {
			_databaseConnection.closeDB();
			boolean resultRestore = this.restoreDatabase();
			if (!resultRestore) {
				_databaseConnection = new DatabaseConnection(_context,Constants.DATABASE_NAME,Constants.DATABASE_VERSION);
				_databaseConnection.openDB();
				createDatabaseStructure();
			} else {

				try {

					_databaseConnection = new DatabaseConnection(_context,Constants.DATABASE_NAME,Constants.DATABASE_VERSION);
					_databaseConnection.openDB();
					this.createDatabaseStructureIfNecessary();
				}
				catch (Exception e)
				{
					throw new Exception("Error abriendo objeto de conexión a base de datos. Motivo: " + e.getMessage().toString());
				}
			}
		} else {
			this.createDatabaseStructureIfNecessary();
		}

	}
	
	public void closeDB() throws Exception 
	{
		try {
			_databaseConnection.closeDB();
		}
		catch (Exception e) {
			throw e;
		}
		
	}
	
	public void beginTransaction() {
		_databaseConnection.getDatabase().beginTransaction();
	}
	
	public void commit() {
		_databaseConnection.getDatabase().setTransactionSuccessful();
	}
	
	public void endTransaction() {
		_databaseConnection.getDatabase().endTransaction();
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
	
	public void ResetConnection() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			_databaseConnection.getDatabase().close();
			this.openDB(_context);
		}
		
	}
	
	public boolean isOpen() throws Exception
	{
		if (_databaseConnection == null)
			return false;
		
		return _databaseConnection.getDatabase().isOpen();
	}
	
	/*public boolean backupDatabase(File file) {
	    File from = _context.getDatabasePath(Constants.DATABASE_NAME);
	    File to = file;
	    try {
	        IOUtils.copyFile(from, to);
	        return true;
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	       Log.e("backup", "Error backuping up database: " + e.getMessage(), e);
	    }
	    return false;
	}*/

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

	public boolean restoreDatabase() {
		String backupFileName = "/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_DB_BACKUP + "/" +
				Constants.DATABASE_NAME;

		FileInputStream inputStreamNewDB = null;
		try {
			inputStreamNewDB = new FileInputStream(backupFileName);
		} catch (FileNotFoundException e1) {
			return false;
		}

		final File oldDB = _context.getDatabasePath(Constants.DATABASE_NAME);

		if (inputStreamNewDB != null) {
			try {
				copyFile((FileInputStream) inputStreamNewDB, new FileOutputStream(oldDB));
			} catch (IOException e) {
				Log.d("Database", "ex for is of restore: " + e);
				return false;
			}
		} else {
			Log.d("Database", "Restore - file does not exists");
			return false;
		}

		return true;
	}

	public void backupDatabase() throws IOException {
		final File dbFile = _context.getDatabasePath(Constants.DATABASE_NAME);
	    
	    FileInputStream fis = null;
		try {
			fis = new FileInputStream(dbFile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	    String outFileName = "/sdcard/" + Constants.FOLDER_ROOT + "/" + Constants.FOLDER_DB_BACKUP + "/" +                            
				Constants.DATABASE_NAME;
	    
	    final File currentDb = new File(outFileName);
	    currentDb.delete();

	    // Open the empty db as the output stream
	    OutputStream output = null;
		try {
			output = new FileOutputStream(outFileName);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    // Transfer bytes from the inputfile to the outputfile
	    byte[] buffer = new byte[1024];
	    int length;
	    while ((length = fis.read(buffer))>0){
	        try {
				output.write(buffer, 0, length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }

	    // Close the streams
	    output.flush();
	    output.close();
	    fis.close();
	}
	
	public boolean existsTable(String tableName) throws Exception
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
	
	public boolean existsIndex(String indexName) throws Exception
	{
		Cursor cursor = _databaseConnection.getDatabase().rawQuery("select DISTINCT tbl_name from sqlite_master where name = '"+ indexName +"' and type='index'", null);
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
		Cursor cursor = null;
		
		cursor = _databaseConnection.getDatabase().rawQuery(sentence, null);
		
		Log.i("Database", "Execute sentence:" + sentence + Constants.NEW_LINE +
				"Total: " + cursor.getCount());
		
		boolean exists = cursor.moveToFirst();
		
		if (!exists) cursor.close();
		
		return exists ? cursor : null;
		
		/*if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			return cursor;
		}
		else
			cursor.close();
			return null;*/
	}
	
	public String getCodeById(String table, String Codigo, String IdField, Long value)
	{
		Cursor cursor = null;
		
		cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, IdField + " = " + value , null, null, null, null);
		
		if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			
			String code = cursor.getString(cursor.getColumnIndex(Codigo));
			
			cursor.close();
			return code;
		}
		
		return null;
		
	}
	
	public Long getIdByCode(String table, String Codigo, String IdField, Long value)
	{
		Cursor cursor = null;
		
		cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, Codigo + " = " + value , null, null, null, null);
		
		if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			
			Long code = Long.parseLong(cursor.getString(cursor.getColumnIndex(Codigo)));
			
			cursor.close();
			
			return code;
		}
		
		return null;
		
	}
	
	public Cursor getFirstRecordFromField(String table, String field, String filterText,boolean isText) throws Exception
	{
		Cursor cursor = null;
		
		if (isText)
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = '" + filterText + "'", null, null, null, null);
		else
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText , null, null, null, null);
		
		boolean exists = cursor.moveToFirst();
		
		return exists ? cursor : null;
		
		/*if (cursor.getCount() > 0)
		{
			cursor.moveToFirst();
			return cursor;
		}
		else
			return null;*/
		
	}
	
	public Cursor getRecordsFromField(String table, String field, String filterText, boolean isText, String otherConditions, String orderByField) throws Exception
	{
		Cursor cursor = null;
		
		if (field.equals(Constants.EMPTY_STRING) && otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, null, null, null,null, orderByField);
		else if (field.equals(Constants.EMPTY_STRING) && !otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, "1 = 1 " + otherConditions, null, null, null, orderByField);
		
		else
			if (isText && otherConditions.equals(Constants.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = '" + filterText + "'", null, null, null, orderByField );
			else if (isText && !otherConditions.equals(Constants.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = '" + filterText + "' " + otherConditions, null, null, null, orderByField);
			else if (!isText && !otherConditions.equals(Constants.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText + " " + otherConditions, null, null, null, orderByField);
			else if (!isText && otherConditions.equals(Constants.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText, null, null, null, orderByField);
		
		return cursor;
	}
	
	public Cursor getRecordsFromFieldNumeric(String table, String field, String filterText, String otherConditions, String orderBy) throws Exception
	{
		Cursor cursor = null;
		
		if (field.equals(Constants.EMPTY_STRING) && otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, null, null, null, null, orderBy);
		else if (field.equals(Constants.EMPTY_STRING) && !otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, "1 = 1 " + otherConditions, null, null, null, orderBy);
		else
			if (otherConditions.equals(Constants.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText, null, null, null, orderBy);
			else if (!otherConditions.equals(Constants.EMPTY_STRING))
				cursor = _databaseConnection.getDatabase().query(table, new String[] {"*"}, field + " = " + filterText + " " + otherConditions, null, null, null, orderBy);
		return cursor;
	}
	
	public LinkedList<String> getStringArrayByField(String table, String fieldShow, String fieldFilter, String text, boolean onlyStartsWith, String otherConditions, String orderBy) throws Exception
	{
		Cursor cursor = null;
		
		if (text.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, "1 = 1 " + otherConditions, null, null, null, orderBy);
		else if (onlyStartsWith && otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '" + text + "% '" + otherConditions, null, null, null, orderBy);
		else if (!onlyStartsWith && otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '%" + text + "%' " + otherConditions, null, null, null, orderBy);
		else if (onlyStartsWith && !otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '" + text + "%' " + otherConditions, null, null, null, orderBy);
		else if (!onlyStartsWith && !otherConditions.equals(Constants.EMPTY_STRING))
			cursor = _databaseConnection.getDatabase().query(table, new String[] {fieldShow, fieldFilter}, fieldFilter + " like '%" + text + "%' " + otherConditions, null, null, null, orderBy);	
		
		LinkedList<String> result = new LinkedList<String>();
		
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
			Log.i("DatabaseOperations","Error en insert: " + e.getMessage());
			throw e;
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
			Log.e("Database",e.getMessage().toString());
			throw e;
		}
	}
	
	public void execute(String sentence) throws Exception
	{
		if (!_databaseConnection.getDatabase().isOpen())
			throw new Exception("La base de datos no está abierta.");
		
		try {
			_databaseConnection.getDatabase().execSQL(sentence);
		}
		
		catch (Exception e) {
			throw e;
		}
	}
	
	private void createTables() throws Exception
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
			createClientesInfoTable();
			createGastosInfoTable();
			createIngresosTable();
			createGDPRTable();
		}
		catch (Exception e) {
			throw e;
		}
		
	}
	
	private void createIndexs() throws Exception {
		try {
			createIndex(Constants.INDEX_DEPOSITO_CODIGOCLIENTE, Constants.TABLE_DEPOSITOS, 
					 new ArrayList<String>(Arrays.asList("CodigoCliente")));
			createIndex(Constants.INDEX_DEPOSITO_NUMDOC, Constants.TABLE_DEPOSITOS, 
					 new ArrayList<String>(Arrays.asList("NumDoc")));
			createIndex(Constants.INDEX_DEPOSITO_IDCLIENTE, Constants.TABLE_DEPOSITOS, 
					 new ArrayList<String>(Arrays.asList("IdCliente")));
			createIndex(Constants.INDEX_DEPOSITO_FECHADEPOSITO, Constants.TABLE_DEPOSITOS, 
					 new ArrayList<String>(Arrays.asList("FechaDeposito")));
			createIndex(Constants.INDEX_LINEADEPOSITO_IDDEPOSITO, Constants.TABLE_LINEAS_DEPOSITO, 
					 new ArrayList<String>(Arrays.asList("IdDeposito")));
			createIndex(Constants.INDEX_LINEADEPOSITO_IDDEPOSITO_IDARTICULO, Constants.TABLE_LINEAS_DEPOSITO, 
					 new ArrayList<String>(Arrays.asList("IdDeposito","IdArticulo")));
			createIndex(Constants.INDEX_TARIFA_IDARTICULO_CODIGOTARIFA, Constants.TABLE_TARIFAS, 
					 new ArrayList<String>(Arrays.asList("IdArticulo","CodigoTarifa")));
			createIndex(Constants.INDEX_PACTOS_IDARTICULO, Constants.TABLE_PACTOS, 
					 new ArrayList<String>(Arrays.asList("IdArticulo")));
			createIndex(Constants.INDEX_ARTICULOS_ACTIVO_TIPO, Constants.TABLE_ARTICULOS, 
					 new ArrayList<String>(Arrays.asList("Activo","Tipo")));
		}
		catch (Exception e) {
			throw e;
		}
	}
	
	private void dropTables() throws Exception
	{
		try {
			dropTable(Constants.TABLE_CLIENTES);
			dropTable(Constants.TABLE_ARTICULOS);
			dropTable(Constants.TABLE_MOVIMIENTOS_ALMACEN);
			dropTable(Constants.TABLE_DEPOSITOS);
			dropTable(Constants.TABLE_LINEAS_DEPOSITO);
			dropTable(Constants.TABLE_TARIFAS);
			dropTable(Constants.TABLE_PACTOS);
			dropTable(Constants.TABLE_TIPOS_IVA); 
			dropTable(Constants.TABLE_FORMAS_PAGO);
			dropTable(Constants.TABLE_GASTOS);
			dropTable(Constants.TABLE_HISTORICOS);
			dropTable(Constants.TABLE_LINEAS_HISTORICO);
			dropTable(Constants.TABLE_CONTADORES);
			dropTable(Constants.TABLE_CLIENTES_INFO);
			dropTable(Constants.TABLE_GASTOS_INFO);
			dropTable(Constants.TABLE_INGRESOS);
			dropTable(Constants.TABLE_GDPR);
			
		}
		catch (Exception e) {
			throw e;
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
				throw new Exception("Error borrando los registros de la tabla " + table + ". Motivo: " + e.getMessage().toString());
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
				throw new Exception("Error borrando los registros de la tabla " + table + ". Motivo: " + e.getMessage().toString());
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
				throw new Exception("Error creando índice " + index + ". Motivo: " + e.getMessage().toString());
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
				throw new Exception("Error eliminando la tabla " + table + ". Motivo: " + e.getMessage().toString());
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
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_CLIENTES + " ( IdCliente integer primary key autoincrement, "
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
														  + "Nombre text not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_CLIENTES + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_CLIENTES + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createDepositosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_DEPOSITOS + " ( IdDeposito integer primary key autoincrement, "
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
				throw new Exception("Error creando la tabla " + Constants.TABLE_DEPOSITOS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_DEPOSITOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createLineasDepositoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_LINEAS_DEPOSITO + " ( IdLineaDeposito integer primary key autoincrement, "
														  + "IdDeposito integer not null, "
						                                  + "IdArticulo integer not null, "
						                                  + "UnidadesIniciales integer not null, "
						                                  + "PVP real, "
						                                  + "PVPAnterior real, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_LINEAS_DEPOSITO + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_LINEAS_DEPOSITO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createHistoricosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_HISTORICOS + " ( IdHistorico integer primary key autoincrement, "
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
				throw new Exception("Error creando la tabla " + Constants.TABLE_HISTORICOS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_HISTORICOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createLineasHistoricoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_LINEAS_HISTORICO + " ( IdLineaHistorico integer primary key autoincrement, "
														  + "IdHistorico integer not null, "
						                                  + "IdArticulo integer not null, "
						                                  + "Unidades integer not null, "
						                                  + "MovimientoStock integer not null, "
						                                  + "MovimientoStockDefectuosas integer not null, "
						                                  + "Tipo integer not null); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_LINEAS_DEPOSITO + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_LINEAS_DEPOSITO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createTarifasTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_TARIFAS + " ( IdTarifa integer primary key autoincrement, "
														  + "CodigoTarifa text not null, "
														  + "IdArticulo integer not null, "
														  + "FechaIni date, "
														  + "FechaFin date, "
						                                  + "PVP real, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_TARIFAS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_TARIFAS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createPactosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_PACTOS + " ( IdPacto integer primary key autoincrement, "
														  + "IdArticulo integer not null, "
						                                  + "IdCliente integer not null, "
						                                  + "PVP real, "
						                                  + "Descuento1 real, "
						                                  + "Descuento2 real); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_PACTOS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_PACTOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
	}
	
	private void createArticulosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_ARTICULOS + " ( IdArticulo integer primary key autoincrement, "
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
				throw new Exception("Error creando la tabla " + Constants.TABLE_ARTICULOS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_ARTICULOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createMovimientosAlmacenTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_MOVIMIENTOS_ALMACEN + " ( IdMovimiento integer primary key autoincrement, "
						                                  + "IdArticulo integer not null, "
						                                  + "Entradas integer not null, "
						                                  + "Salidas integer not null, "
						                                  + "Fecha date default CURRENT_DATE, "
						                                  + "TipoStock integer not null, "
						                                  + "Tipo integer not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_MOVIMIENTOS_ALMACEN + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_MOVIMIENTOS_ALMACEN + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createTiposIVATable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_TIPOS_IVA + " ( IdTipoIVA integer primary key autoincrement, "
						                                  + "Filiacion text not null, "
						                                  + "Articulo text not null, "
						                                  + "Fecha date not null, "
						                                  + "Descripcion,"
						                                  + "Impuesto text not null, "
						                                  + "Recargo real not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_TIPOS_IVA + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_TIPOS_IVA + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createFormasPagoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_FORMAS_PAGO + " ( IdFormaPago integer primary key autoincrement, "
						                                  + "CodigoFormaPago text not null, "
						                                  + "Descripcion text not null);");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_FORMAS_PAGO + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_FORMAS_PAGO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createGastosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_GASTOS + " ( IdGasto integer primary key autoincrement, "
														  + "IdArticulo integer not null, "
														  + "Fecha date, "
						                                  + "Cantidad real); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_TARIFAS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_TARIFAS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createContadoresTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_CONTADORES + " ( IdContador integer primary key autoincrement, "
														  + "ContadorSerieA integer not null, "
						                                  + "ContadorSerieB integer not null); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_CONTADORES + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_CONTADORES + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createClientesInfoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_CLIENTES_INFO + " ( IdCliente integer primary key not null , "
														  + "Representante text, " 
														  + "DniRepresentante text, " 
						                                  + "CCC text); ");
															
			} 
			catch (Exception e) {
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_CLIENTES_INFO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createGastosInfoTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_GASTOS_INFO + " ( IdGastoInfo integer primary key autoincrement , "
														  + "Fecha date, " 
						                                  + "Comentario text); ");
															
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_GASTOS_INFO + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_GASTOS_INFO + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createIngresosTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_INGRESOS + " ( IdIngreso integer primary key autoincrement, "
														  + "Entidad text not null, "
														  + "Referencia text not null, "
														  + "Descripcion text not null, "
														  + "Fecha date, "
						                                  + "Cantidad real); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_INGRESOS + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_INGRESOS + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void createGDPRTable() throws Exception
	{
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("create table if not exists " + Constants.TABLE_GDPR + " ( IdGDPR integer primary key autoincrement, "
														  + "CodigoCliente text not null); ");
				
			} 
			catch (Exception e) {
				throw new Exception("Error creando la tabla " + Constants.TABLE_GDPR + ". Motivo: " + e.getMessage().toString());
			}
			
		}
		else {
			throw new Exception("Error creando la tabla " + Constants.TABLE_GDPR + ". Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
	private void alterStructure() throws Exception {
		if (_databaseConnection.getDatabase().isOpen())
		{
			try {
				_databaseConnection.getDatabase().execSQL("alter table " + Constants.TABLE_LINEAS_HISTORICO + " ADD COLUMN PVP REAL default null ");
			} 
			catch (Exception e) {
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + Constants.TABLE_ARTICULOS + " ADD COLUMN StockPropio integer NOT NULL default 1 ");
			} catch (Exception e) {
			}

			try {
				_databaseConnection.getDatabase().execSQL("alter table " + Constants.TABLE_HISTORICOS + " ADD COLUMN ActualizarStock integer NOT NULL default 1 ");
			} catch (Exception e) {
			}
			
		}
		else {
			throw new Exception("Error añadiendo columnas. Motivo: La Base de datos no ha podido ser abierta.");
		}
		
	}
	
}
