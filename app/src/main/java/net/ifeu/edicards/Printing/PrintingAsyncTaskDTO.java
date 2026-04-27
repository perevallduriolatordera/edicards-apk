package net.ifeu.edicards.Printing;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.app.Activity;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.DTODeposito;
import net.ifeu.edicards.DepositManagerExtension;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.library.Errors.ResultResponse;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

/**
 * AsyncTask para ejecutar la reimpresión (DTODeposito) en un hilo de fondo
 * Previene que la UI se bloquee durante operaciones de Bluetooth e I/O
 */
public class PrintingAsyncTaskDTO extends AsyncTask<Void, String, PrintingAsyncTaskDTO.PrintingResultDTO> {

	/**
	 * Clase para almacenar el resultado de la reimpresión
	 */
	public static class PrintingResultDTO {
		private boolean success;
		private String error;

		public PrintingResultDTO(boolean success, String error) {
			this.success = success;
			this.error = error;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getError() {
			return error;
		}
	}

	private ProgressDialog progressDialog;
	private Context context;
	private Activity activity;
	private AppConfig appConfig;
	private DTODeposito dto;
	private String GUID;
	private DepositoModalidad modalidad;
	private PrintManager printManager;
	private PrintingAsyncTaskDTOListener listener;
	private boolean printDeposito;

	public interface PrintingAsyncTaskDTOListener {
		void onPrintingStarted();
		void onPrintingProgress(String message);
		void onPrintingSuccess();
		void onPrintingError(String error);
		void onPrintingCancelled();
	}

	public PrintingAsyncTaskDTO(Context context, Activity activity, AppConfig appConfig, DTODeposito dto, String GUID,
							 DepositoModalidad modalidad, PrintManager printManager,
							 boolean printDeposito, PrintingAsyncTaskDTOListener listener) {
		this.context = context;
		this.activity = activity;
		this.appConfig = appConfig;
		this.dto = dto;
		this.GUID = GUID;
		this.modalidad = modalidad;
		this.printManager = printManager;
		this.printDeposito = printDeposito;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		// Mostrar progress dialog
		progressDialog = new ProgressDialog(context);
		progressDialog.setTitle("Reimpresión");
		progressDialog.setMessage("Reimprimiendo documentos...");
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(dialog -> {
			// Si el usuario cancela, detener la tarea
			cancel(true);
		});
		progressDialog.show();

		if (listener != null) {
			listener.onPrintingStarted();
		}
	}

	@Override
	protected PrintingResultDTO doInBackground(Void... voids) {
		try {
			// El estado de la impresora ya fue verificado en el UI thread antes de lanzar este AsyncTask
			// No intentar verificar getStatusDTO() aquí ya que causaría "Can't create handler inside thread"

			if (isCancelled()) {
				printManager.Release();
				return new PrintingResultDTO(false, "Reimpresión cancelada");
			}

				publishProgress("Reimprimiendo...");
				boolean result = false;

				if (dto.isDeposito() && printDeposito) {
					try {
						result = printManager.printDeposito(dto, appConfig, appConfig, GUID, modalidad);
					} catch (Exception e) {
						printManager.Release();
						return new PrintingResultDTO(false, "Error al reimprimir depósito: " + e.getMessage());
					}

					if (!result) {
						printManager.Release();
						return new PrintingResultDTO(false, "No se pudo reimprimir el depósito");
					}
				}

				result = false;
				if (dto.isAlbaran()) {
					try {
						ResultResponse resultResponse = printManager.printAlbaran(dto, activity, appConfig, GUID,
								DepositManagerExtension.DataTier.isTransferPayment(dto.PagoDescripcion), modalidad);
						if (resultResponse == null) {
							printManager.Release();
							return new PrintingResultDTO(false, "Error al reimprimir albarán: respuesta nula");
						}
						result = resultResponse.Success;
						if (!result) {
							printManager.Release();
							return new PrintingResultDTO(false, "No se pudo reimprimir el albarán: " + resultResponse.Message);
						}
					} catch (Exception e) {
						printManager.Release();
						return new PrintingResultDTO(false, "Error al reimprimir albarán: " + e.getMessage());
					}
				}

			printManager.Release();
			return new PrintingResultDTO(true, "Reimpresión completada");

		} catch (Exception e) {
			try {
				printManager.Release();
			} catch (Exception ex) {
				// Ignorar errores al liberar recursos
			}
			return new PrintingResultDTO(false, "Error durante la reimpresión: " + e.getMessage());
		}
	}

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.setMessage(values[0]);
		}
		if (listener != null) {
			listener.onPrintingProgress(values[0]);
		}
	}

	@Override
	protected void onPostExecute(PrintingResultDTO result) {
		super.onPostExecute(result);

		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		if (result.isSuccess()) {
			if (listener != null) {
				listener.onPrintingSuccess();
			}
		} else {
			if (listener != null) {
				listener.onPrintingError(result.getError());
			}
		}
	}

	@Override
	protected void onCancelled(PrintingResultDTO result) {
		super.onCancelled(result);

		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}

		try {
			printManager.Release();
		} catch (Exception e) {
			// Ignorar errores al liberar recursos
		}

		if (listener != null) {
			listener.onPrintingCancelled();
		}
	}
}
