package net.ifeu.edicards.Printer;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.DataTier.DTODeposito;
import android.content.Context;

public interface IPrintDTO {

	public boolean getStatus(Context context, AppConfig app,
			boolean showMessages);
	
	public boolean printAlbaran(DTODeposito deposito, Context context,
			AppConfig app, String guid, boolean isTransferPayment);
	
	public boolean printDeposito(DTODeposito deposito, Context context,
			AppConfig app, String guid);
	
	public void Release();
}
