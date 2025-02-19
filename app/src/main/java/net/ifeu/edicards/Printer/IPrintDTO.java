package net.ifeu.edicards.Printer;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.library.Errors.ResultResponse;

import android.content.Context;

public interface IPrintDTO {

	boolean getStatus(Context context, AppConfig app,
			boolean showMessages);
	
	ResultResponse printAlbaran(DTODeposito deposito, Context context,
								AppConfig app, String guid, boolean isTransferPayment,
								DepositoModalidad modalidad);
	
	boolean printDeposito(DTODeposito deposito, Context context,
			AppConfig app, String guid, DepositoModalidad modalidad);
	
	void Release();
}
