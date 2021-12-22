package net.ifeu.edicards.Printer;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.DataTier.Deposito;
import android.content.Context;

public interface IPrint {

	public boolean getStatus(Context context, AppConfig app,
			boolean showMessages) throws Exception;
	
	public boolean printAlbaran(Deposito deposito, Context context,
			AppConfig app, String guid, boolean isTransferPayment) throws Exception;
	
	public boolean printDeposito(Deposito deposito, Context context,
			AppConfig app, String guid) throws Exception;
	
	public void Release();
	
}
