package net.ifeu.edicards.DataTier;

import android.content.Context;
import net.ifeu.edicards.AppConfig;

public interface IPersistable {

	public void InitializePersistance(AppConfig appConfig, Context context) throws Exception;
	public void ReleasePersistance() throws Exception;
	public void save() throws Exception;
	public void delete() throws Exception;
	public void update() throws Exception;
	public void clean() throws Exception;

}
