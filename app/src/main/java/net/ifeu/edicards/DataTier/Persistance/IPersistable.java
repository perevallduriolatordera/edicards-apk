package net.ifeu.edicards.DataTier.Persistance;

import android.content.Context;
import net.ifeu.edicards.Application.AppConfig;

public interface IPersistable {

	void InitializePersistance(AppConfig appConfig, Context context) throws Exception;
	void ReleasePersistance() throws Exception;
	void save() throws Exception;
	void delete() throws Exception;
	void update() throws Exception;
	void clean() throws Exception;
}
