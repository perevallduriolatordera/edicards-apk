package net.ifeu.edicards.Printing;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.DepositoModalidad;
import net.ifeu.edicards.DataTier.NTV.DepositoNTVDTO;
import net.ifeu.edicards.DepositManagerExtension;
import net.ifeu.edicards.Printer.PrintManager;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

/**
 * AsyncTask para ejecutar la impresión en un hilo de fondo
 * Previene que la UI se bloquee durante operaciones de Bluetooth e I/O
 */
public class PrintingAsyncTask extends AsyncTask<Void, String, PrintingAsyncTask.PrintingResult> {

	private ProgressDialog progressDialog;
	private Context context;
	private AppConfig appConfig;
	private Deposito deposito;
	private String GUID;
	private DepositoModalidad modalidad;
	private int copias;
	private PrintManager printManager;
	private PrintingAsyncTaskListener listener;
	private boolean printDeposito;

	public interface PrintingAsyncTaskListener {
		void onPrintingStarted();
		void onPrintingProgress(String message);
		void onPrintingSuccess();
		void onPrintingError(String error);
		void onPrintingCancelled();
	}

	public PrintingAsyncTask(Context context, AppConfig appConfig, Deposito deposito, String GUID,
							 DepositoModalidad modalidad, int copias, PrintManager printManager,
							 boolean printDeposito, PrintingAsyncTaskListener listener) {
		this.context = context;
		this.appConfig = appConfig;
		this.deposito = deposito;
		this.GUID = GUID;
		this.modalidad = modalidad;
		this.copias = copias;
		this.printManager = printManager;
		this.printDeposito = printDeposito;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		// Mostrar progress dialog
		progressDialog = new ProgressDialog(context);
		progressDialog.setTitle("Impresión");
		progressDialog.setMessage("Imprimiendo documentos...");
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
	protected PrintingResult doInBackground(Void... voids) {
		try {
			// El estado de la impresora ya fue verificado en el UI thread antes de lanzar este AsyncTask
			// No intentar verificar getStatus() aquí ya que causaría "Can't create handler inside thread"

			publishProgress("Iniciando impresión...");

			// Imprimir copias
				for (int i = 1; i <= copias; i++) {
					if (isCancelled()) {
						printManager.Release();
						return new PrintingResult(false, "Impresión cancelada");
					}

					publishProgress("Imprimiendo copia " + i + " de " + copias + "...");
					boolean result = false;

					if (deposito.isDeposito() && printDeposito) {
						try {
							result = printManager.printDeposito(deposito, appConfig, appConfig, GUID, modalidad);
						} catch (Exception e) {
							printManager.Release();
							return new PrintingResult(false, "Error al imprimir depósito: " + e.getMessage());
						}

						if (!result) {
							printManager.Release();
							return new PrintingResult(false, "No se pudo imprimir el depósito");
						}
					}

					result = false;
					if (deposito.isAlbaran()) {
						try {
							result = printManager.printAlbaran(deposito, context, appConfig, GUID,
									DepositManagerExtension.DataTier.isTransferPayment(deposito.FormaPago), modalidad);
						} catch (Exception e) {
							printManager.Release();
							return new PrintingResult(false, "Error al imprimir albarán: " + e.getMessage());
						}

						if (!result) {
							printManager.Release();
							return new PrintingResult(false, "No se pudo imprimir el albarán");
						}
					}
				}

			printManager.Release();
			return new PrintingResult(true, "Impresión completada");

		} catch (Exception e) {
			try {
				printManager.Release();
			} catch (Exception ex) {
				// Ignorar errores al liberar recursos
			}
			return new PrintingResult(false, "Error durante la impresión: " + e.getMessage());
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
	protected void onPostExecute(PrintingResult result) {
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
	protected void onCancelled(PrintingResult result) {
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

	/**
	 * Clase para almacenar el resultado de la impresión
	 */
	public static class PrintingResult {
		private boolean success;
		private String error;

		public PrintingResult(boolean success, String error) {
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
}
