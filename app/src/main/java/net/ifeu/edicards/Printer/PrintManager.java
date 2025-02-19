package net.ifeu.edicards.Printer;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.library.Errors.ResultResponse;

import android.content.Context;

public class PrintManager {

	IPrint _print = null;
	IPrintDTO _printDTO = null;

	public boolean getStatus(Context context, AppConfig app,
			boolean showMessages) {

		PrintDocumentsStar star = new PrintDocumentsStar();

		boolean result = star.getStatus(context, app, showMessages);

		if (!result) {
			PrintDocumentsWoosim woosim = new PrintDocumentsWoosim();
			result = woosim.getStatus(context, app, showMessages);

			if (result)
				_print = woosim;
		} else {
			_print = star;
		}

		return result;

	}
	
	public boolean getStatusDTO(Context context, AppConfig app,
			boolean showMessages) {

		PrintDTODocumentsStar star = new PrintDTODocumentsStar();

		boolean result = star.getStatus(context, app, showMessages);

		if (!result) {
			PrintDTODocumentsWoosim woosim = new PrintDTODocumentsWoosim();
			result = woosim.getStatus(context, app, showMessages);

			if (result)
				_printDTO = woosim;
		} else {
			_printDTO = star;
		}

		return result;

	}

	public boolean printAlbaran(Deposito deposito, Context context,
								AppConfig app, String guid, boolean isTransferPayment, DepositoModalidad modalidad) throws Exception {
		if (_print == null)
			throw new Exception("No se ha encontrado impresora");

		return _print.printAlbaran(deposito, context, app, guid, isTransferPayment, modalidad);

	}

	public boolean printDeposito(Deposito deposito, Context context,
			AppConfig app, String guid, DepositoModalidad modalidad) throws Exception {

		if (_print == null)
			throw new Exception("No se ha encontrado impresora");

		return _print.printDeposito(deposito, context, app, guid, modalidad);
	}
	
	public ResultResponse printAlbaran(DTODeposito deposito, Context context,
									   AppConfig app, String guid, boolean isTransferPayment,
									   DepositoModalidad modalidad) throws Exception {
		if (_printDTO == null)
			throw new Exception("No se ha encontrado impresora");

		return _printDTO.printAlbaran(deposito, context, app, guid, isTransferPayment, modalidad);

	}

	public boolean printDeposito(DTODeposito deposito, Context context,
			AppConfig app, String guid, DepositoModalidad modalidad) throws Exception {

		if (_printDTO == null)
			throw new Exception("No se ha encontrado impresora");

		return _printDTO.printDeposito(deposito, context, app, guid, modalidad);
	}
	
	public void Release ()
	{
		if (_printDTO != null)
			_printDTO.Release();
			
	}
}
