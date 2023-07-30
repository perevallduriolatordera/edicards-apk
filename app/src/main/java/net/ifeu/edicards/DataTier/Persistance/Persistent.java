package net.ifeu.edicards.DataTier.Persistance;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DatabaseOperations.DatabaseOperations;
import android.content.Context;

public abstract class Persistent implements IPersistable {

	DatabaseOperations _databaseOperations;
	
	public AppConfig appConfig;
	public Context context;
	
	public DatabaseOperations getDatabaseOperations()
	{
		return _databaseOperations;
	}

	@Override
	public void InitializePersistance(AppConfig appConfigParam, Context contextParam) {
		appConfig = appConfigParam;
		context = contextParam;

		_databaseOperations = appConfig.getDatabaseOperations();

		try {
			if (!_databaseOperations.isOpen())
				_databaseOperations.openDB(context);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void ReleasePersistance() throws Exception 
	{
		_databaseOperations.closeDB();
	}

	@Override
	public void save() throws Exception
	{
		
	}
	
	@Override
	public void delete()  throws Exception
	{
		
	}
	
	@Override
	public void update()  throws Exception
	{
		
	}
	
	@Override
	public void clean()  throws Exception
	{
		
	}

	
	
}
