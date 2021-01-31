package net.ifeu.library.Database;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseConnection {
    static final String TAG = "DBAdapter";
    
    final Context _context;

    private DatabaseHelper _dbHelper;
    private SQLiteDatabase _database;
    private String _databaseName;
    private int _databaseVersion;
    
    public SQLiteDatabase getDatabase() {
    	return _database;
    }
    
    public String getDatabaseName() {
    	return _databaseName;
    }
    
    public void setDatabaseName(String value)
    {
    	_databaseName = value;
    }
    
    public int getDatabaseVersion() {
    	return _databaseVersion;
    }
    
    public void DatabaseVersion(int value) {
    	_databaseVersion = value;
    }
    
    
    public DatabaseConnection(Context ctx, String databaseName, int databaseVersion)
    {
    	 _databaseName = databaseName;
         _databaseVersion = databaseVersion;
         
        this._context = ctx;
        _dbHelper = new DatabaseHelper(_context);
       
        Log.i("Database",String.valueOf(_databaseVersion));
    }

    private class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            super(context, _databaseName, null, _databaseVersion);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            onCreate(db);
        }

		@Override
		public void onCreate(SQLiteDatabase arg0) {
			// TODO Auto-generated method stub
			
		}
    }

    //---opens the database---
    public DatabaseConnection openDB() throws SQLException 
    {
    	if (_database == null)
    		_database = _dbHelper.getWritableDatabase();
        return this;
    }

    //---closes the database---
    public void closeDB() 
    {
    	_database.close();
        _dbHelper.close();
    }

}
