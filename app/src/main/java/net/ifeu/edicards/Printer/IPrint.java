package net.ifeu.edicards.Printer;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;

import android.content.Context;

public interface IPrint {

	boolean getStatus(Context context, AppConfig app,
			boolean showMessages) throws Exception;
	
	boolean printAlbaran(Deposito deposito, Context context,
						 AppConfig app, String guid, boolean isTransferPayment,
						 DepositoModalidad modalidad) throws Exception;
	
	boolean printDeposito(Deposito deposito, Context context,
			AppConfig app, String guid, DepositoModalidad modalidad) throws Exception;
	
	void Release();
	
}
