package net.ifeu.edicards;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.Constants.ConstantsEvents;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Efectivo;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.Excel.ILogCreator;
import net.ifeu.edicards.Excel.LogBookCreator;
import net.ifeu.edicards.Excel.LogBookExceptionsCreator;
import net.ifeu.edicards.Services.ParserMonitor;
import net.ifeu.edicards.Services.ServiceMonitor;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.edicards.Services.ImportResult;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Performance.CpuInfo;
import net.ifeu.library.Performance.MemoryInfo;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.ScreenManager;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MonitorView extends Fragment {

	AppConfig _appConfig;
	public ImportResult SyncResult;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.activity_monitor, container,
				false);
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		_appConfig = (AppConfig) this.getActivity().getApplicationContext();
    	ServiceMonitor monitor = _appConfig.getWorkingArea().Monitor;

		CountDownTimer countDownTimer = new CountDownTimer(100000, 5000) {
			@Override
			public void onTick(long millisUntilFinished) {
				fillDataMonitor(monitor);
			}

			@Override
			public void onFinish() {
			}
		};

		// Start the timer
		countDownTimer.start();
    	
		this.fillDataMonitor(monitor);
	}
	
	private void showWaiting(String message) {
		LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
    	mainLinearLayout.setGravity(Gravity.CENTER);
    	
    	LinearLayout layout1 = new LinearLayout(this.getActivity());
		layout1.setOrientation(LinearLayout.VERTICAL);
		layout1.setGravity(Gravity.CENTER);
    	    	   		 
    	layout1.addView(this.createLabelWaiting("Espere unos instantes..." ,message));
    	mainLinearLayout.addView(layout1);
	}

	private LinearLayout fillPerformanceSummaryMonitor() {
		double memoryUsage = MemoryInfo.getMemoryUsage(_appConfig);
		int cpuUsage = CpuInfo.getCpuUsageSinceLastCall();

		LinearLayout layout = new LinearLayout(this.getActivity());
		layout.removeAllViews();
		layout.setOrientation(LinearLayout.HORIZONTAL);

		layout.addView(this.createLabel("Uso de CPU",String.valueOf(cpuUsage) + " %", false));
		layout.addView(this.createLabel("Uso de memoria" ,String.valueOf(roundTo2Decimals(memoryUsage) + " %"), false));

		return layout;
	}
	
    private void fillDataMonitor(ServiceMonitor monitor) {
    	    	
    	if (monitor == null) {
    		monitor = new ServiceMonitor();
    		monitor.ParserMonitor = new ParserMonitor();
    	}
    	
    	if (monitor.ParserMonitor == null) {
    		monitor.ParserMonitor = new ParserMonitor();
    	}
    	
    	 
    	LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.mainLinearLayout);
		if (mainLinearLayout == null) {
			return;
		}

    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
    	mainLinearLayout.setGravity(Gravity.CENTER);

    	this.createButtonsHeader();

		LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	layout.setLayoutParams(params);

    	LinearLayout layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		
		// Totales forma de pago
		
    	layout2.addView(this.createLabel("Formas de pago nuevas" ,String.valueOf(monitor.ParserMonitor.FormaPagoSaveCounter), false));
    	layout2.addView(this.createLabel("Formas de pago actualizadas" , String.valueOf(monitor.ParserMonitor.FormaPagoUpdateCounter), false));
    	
    	// Totales pactos
    	
    	layout2.addView(this.createLabel("pactos nuevos" , String.valueOf(monitor.ParserMonitor.PactoSaveCounter), false));
    	layout2.addView(this.createLabel("pactos actualizados" , String.valueOf(monitor.ParserMonitor.PactoUpdateCounter), false));
    	
    	layout.addView(layout2);
    	
    	layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		
    	// Totales iva
    	
    	layout2.addView(this.createLabel("tipos de iva nuevos",String.valueOf(monitor.ParserMonitor.IvaSaveCounter), false));
    	
    	// Totales tarifa
    	
    	layout2.addView(this.createLabel("tipos de tarifa nuevos" , String.valueOf(monitor.ParserMonitor.TarifaSaveCounter), false));
		
    	// Totales articulos
    	
    	layout2.addView(this.createLabel("artículos nuevos" , String.valueOf(monitor.ParserMonitor.ArticuloSaveCounter), false));
    	layout2.addView(this.createLabel("artículos actualizados" , String.valueOf(monitor.ParserMonitor.ArticuloUpdateCounter), false));
    	
    	layout.addView(layout2);
    	
    	layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);

    	// Totales clientes
    	
    	layout2.addView(this.createLabel("clientes nuevos" , String.valueOf(monitor.ParserMonitor.ClienteSaveCounter), false));
    	layout2.addView(this.createLabel("clientes actualizados" , String.valueOf(monitor.ParserMonitor.ClienteUpdateCounter), false));

    	// Totales depósitos
    	
    	layout2.addView(this.createLabel("depósitos nuevos" , String.valueOf(monitor.ParserMonitor.DepositoSaveCounter), false));
    	layout2.addView(this.createLabel("depósitos actualizados" , String.valueOf(monitor.ParserMonitor.DepositoUpdateCounter), false));
    	
    	layout.addView(layout2);
    	
    	layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		
		// Totales pdf enviados
		
    	layout2.addView(this.createLabel("pdf enviados" , String.valueOf(monitor.PdfSend), false));
    	
    	// Totales autorizaciones enviadas
    	
    	layout2.addView(this.createLabel("autorizaciones enviadas" ,String.valueOf(monitor.AutorizacionesSend), false));
		    	
    	// Totales GDPR enviados
		
    	layout2.addView(this.createLabel("documentos GDPR enviados" , String.valueOf(monitor.GDPRSend), false));
    	
    	// Totales incidencias enviadas
		
    	layout2.addView(this.createLabel("incidencias enviadas" , String.valueOf(monitor.IncidenciasSend), false));
    	
    	layout.addView(layout2);
    	
    	layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);
		
    	// Totales articulos enviados
		
    	layout2.addView(this.createLabel("artículos enviados" , String.valueOf(monitor.ArticulosSend), false));
    	
    	// Totales gastos enviados
		
    	layout2.addView(this.createLabel("gastos enviados" , String.valueOf(monitor.GastosSend), false));
    	
    	// Totales inventario enviados
    	
    	layout2.addView(this.createLabel("documentos de inventario enviados" , String.valueOf(monitor.InventarioSend), true));
		
    	// Totales recuento enviados
    	
    	layout2.addView(this.createLabel("documentos de recuento enviados" , String.valueOf(monitor.RecuentoSend), false));

		layout.addView(layout2);
    	layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);

		// Totales de albaranes a clientes enviados

		layout2.addView(this.createLabel("albaranes a clientes enviados" , String.valueOf(monitor.EnviosClienteSend), false));

		// Totales albaranes enviados
		
    	layout2.addView(this.createLabel("albaranes enviados" , String.valueOf(monitor.AlbaranesSend), false));
		
    	// Totales depósitos enviados
		
    	layout2.addView(this.createLabel("depósitos enviados" , String.valueOf(monitor.DepositosSend), false));
    	layout.addView(layout2);
		layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);

		// contadores de facturas

		Contador contador = Factory.build(Contador.class, _appConfig);
		try {
			contador.getContadores();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		layout2.addView(this.createLabel("contador tipo A" , String.valueOf(contador.ContadorSerieA), false));
		layout2.addView(this.createLabel("contador tipo B" , String.valueOf(contador.ContadorSerieB), false));

		// Fecha de última modificación
		Date lastModified;
		try {
			lastModified = this._appConfig.getDatabaseOperations().getLastbackupDatabase();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (lastModified != null) {
			DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			String backupDate = formatter.format(lastModified);
			layout2.addView(this.createLabel("Fecha backup", String.valueOf(backupDate), false, true));
		}

		layout.addView(layout2);

		layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);

		layout2.addView(this.createLabel("ficheros pendientes de enviar", ServiceWorker.getFilesFromDirectories().stream().reduce((total, item)-> total.concat(item + "\n")).orElse(""), false));

		layout2 = new LinearLayout(this.getActivity());
		layout2.removeAllViews();
		layout2.setOrientation(LinearLayout.HORIZONTAL);

		layout.addView(fillPerformanceSummaryMonitor());

		mainLinearLayout.addView(layout);
    }

	private void createButtonsHeader() {

		int TEXT_SIZE_BUTTON = 16;
		int BUTTON_MARGIN = 25;
		final MonitorView that = this;
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

		LinearLayout layout = (LinearLayout) this.getActivity().findViewById(R.id.buttonLinearLayout);
		if (layout == null) return;

		layout.removeAllViews();
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.setGravity(Gravity.CENTER);

		ButtonColor sync = new ButtonColor(getActivity(), Color.RED);

		sync.setText("Sincronización");
		params.setMargins(0, 0, BUTTON_MARGIN, 0);
		sync.setLayoutParams(params);
		sync.setTextSize(TEXT_SIZE_BUTTON);

		sync.setOnClickListener(arg0 -> {
			that.executeSync("Se está ejecutando la sincronización");
		});

		layout.addView(sync);

		ButtonColor logBookReport = new ButtonColor(getActivity(), Color.RED);

		logBookReport.setText("Enviar trazabilidad de stock");
		logBookReport.setTextSize(TEXT_SIZE_BUTTON);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, BUTTON_MARGIN, 0);
		logBookReport.setLayoutParams(params);

		logBookReport.setOnClickListener(view -> {

			that.showWaiting("Generando reporte de trazabilidad de stock");
			LogBookCreator logBookCreator = new LogBookCreator(_appConfig);
			try {
				logBookCreator.createExcel30Days();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			that.executeSync("Enviar trazabilidad de stock");
		});

		layout.addView(logBookReport);

		ButtonColor createBackup = new ButtonColor(getActivity(), Color.RED);

		createBackup.setText("Crear copia de seguridad");
		createBackup.setTextSize(TEXT_SIZE_BUTTON);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, BUTTON_MARGIN, 0);
		createBackup.setLayoutParams(params);

		createBackup.setOnClickListener(view -> {

			try {
				_appConfig.getDatabaseOperations().backupDatabase(ConstantsDatabase.DATABASE_RESTOREPOINT_NAME);
			} catch (IOException e) {
				_appConfig.getMessageBox().Show("Creando backup de la base de datos", "Se ha producido un error generando la base de datos", getActivity(), MessageBoxType.Error);
			}
			_appConfig.getMessageBox().Show("Creando backup de la base de datos", "La base de datos se ha generado correctamente", getActivity(), MessageBoxType.Information);
		});

		layout.addView(createBackup);

		ButtonColor restoreBackup = new ButtonColor(getActivity(), Color.RED);

		restoreBackup.setText("Restaurar copia de seguridad");
		restoreBackup.setTextSize(TEXT_SIZE_BUTTON);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, BUTTON_MARGIN, 0);
		restoreBackup.setLayoutParams(params);

		restoreBackup.setOnClickListener(view -> {

			String restorePassword;
			do {
				restorePassword = _appConfig.getMessageBox().InputBox("Restaurar base de datos",
						"Introduzca la contraseña para restaurar la base de datos. Si la operación de realiza con éxito, la app será reiniciada.", getActivity());
			} while (restorePassword.trim().equals(ConstantsTypes.EMPTY_STRING));

			if (!restorePassword.equals(getPasswordForRestore())) {
				_appConfig.getMessageBox().Show("Restaurando backup de la base de datos", "Se ha introducido una contraseña no correcte. Vuelva a intentarlo" ,getActivity(), MessageBoxType.Error);
				return;
			}

			boolean result = _appConfig.getDatabaseOperations().restoreDatabase(ConstantsDatabase.DATABASE_RESTOREPOINT_NAME);
			if (!result)
				_appConfig.getMessageBox().Show("Restaurando backup de la base de datos", "Se ha producido un error restaurando la base de datos" ,getActivity(), MessageBoxType.Error);
			else {
				_appConfig.getMessageBox().Show("Restaurando backup de la base de datos", "La base de datos se ha restaurado correctamente.", getActivity(), MessageBoxType.Information);
				System.exit(0);
			}

		});

		layout.addView(restoreBackup);

		ButtonColor logBookExceptionsReport = new ButtonColor(getActivity(), Color.RED);

		logBookExceptionsReport.setText("Enviar Errores a soporte");
		logBookExceptionsReport.setTextSize(TEXT_SIZE_BUTTON);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, BUTTON_MARGIN, 0);
		logBookExceptionsReport.setLayoutParams(params);

		logBookExceptionsReport.setOnClickListener(view -> {

			that.showWaiting("Generando reporte de errores para soporte");
			ILogCreator logBookCreator = new LogBookExceptionsCreator(_appConfig);
			try {
				logBookCreator.createExcel30Days();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			that.executeSync("Enviar trazabilidad de errores a soporte");
		});

		//layout.addView(logBookExceptionsReport);

		ButtonColor initializeEfectivoButton = new ButtonColor(getActivity(), Color.RED);

		initializeEfectivoButton.setText("Inicializar Ingresos Efectivo");
		initializeEfectivoButton.setTextSize(TEXT_SIZE_BUTTON);
		params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, BUTTON_MARGIN, 0);
		initializeEfectivoButton.setLayoutParams(params);

		initializeEfectivoButton.setOnClickListener(view -> {

			String restorePassword;
			do {
				restorePassword = _appConfig.getMessageBox().InputBox("Restaurar base de datos",
						"Introduzca la contraseña para inicializar los ingresos de efectivo. Si la operación de realiza con éxito, la app será reiniciada.", getActivity());
			} while (restorePassword.trim().equals(ConstantsTypes.EMPTY_STRING));

			if (!restorePassword.equals(getPasswordForRestore())) {
				_appConfig.getMessageBox().Show("Inicializando ingresos de efectivo", "Se ha introducido una contraseña no correcte. Vuelva a intentarlo" ,getActivity(), MessageBoxType.Error);
				return;
			}

			initializeEfectivo();
		});

		layout.addView(initializeEfectivoButton);

	}

    private LinearLayout createLabel(String text, String value, boolean compress) {
    	
	 	LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this.getActivity());
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width= ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.2f).getWidth();
		params.height= ScreenManager.getScreenSizeByPercentage(getActivity().getWindowManager(), 0.15f).getWidth();
    	card.setLayoutParams(params);
    	card.setPadding(20, 20, 20, 20);
    	
    	LinearLayout layout1 = new LinearLayout(this.getActivity());
		layout1.setOrientation(LinearLayout.VERTICAL);
		
		LabelColor label = new LabelColor(this.getActivity(), Color.BLUE, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(compress ? 12 : 18);
		
		
    	LabelColor label2 = new LabelColor(this.getActivity(), Color.BLACK, true, Gravity.CENTER);
		label2.setText(String.valueOf(value));
		label2.setTextSize(30);
    	
    	layout1.addView(label);
    	layout1.addView(label2);
    	
    	card.addView(layout1);
    	
    	layout.addView(card);
    	
    	return layout;
    }

	private LinearLayout createLabel(String text, String value, boolean compress, boolean compressContent) {

		LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);

		android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);

		RelativeLayout card = new RelativeLayout(this.getActivity());
		card.setBackgroundResource(R.drawable.card_background);
		card.setGravity(Gravity.CENTER);

		params.width=225;
		params.height=120;
		card.setLayoutParams(params);
		card.setPadding(20, 20, 20, 20);

		LinearLayout layout1 = new LinearLayout(this.getActivity());
		layout1.setOrientation(LinearLayout.VERTICAL);

		LabelColor label = new LabelColor(this.getActivity(), Color.BLUE, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(compress ? 12 : 18);


		LabelColor label2 = new LabelColor(this.getActivity(), Color.BLACK, true, Gravity.CENTER);
		label2.setText(String.valueOf(value));
		label2.setTextSize(compressContent ? 18 : 30);

		layout1.addView(label);
		layout1.addView(label2);

		card.addView(layout1);

		layout.addView(card);

		return layout;
	}
    
    private LinearLayout createLabelWaiting(String text, String value) {
    	
	 	LinearLayout layout = new LinearLayout(this.getActivity());
		layout.setOrientation(LinearLayout.HORIZONTAL);
		
    	android.widget.LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT);
    	
		RelativeLayout card = new RelativeLayout(this.getActivity());
    	card.setBackgroundResource(R.drawable.card_background);
    	card.setGravity(Gravity.CENTER);
    	    	
    	params.width=900;
    	params.height=600;
    	card.setLayoutParams(params);
    	card.setPadding(20, 20, 20, 20);
    	
    	LinearLayout layout1 = new LinearLayout(this.getActivity());
		layout1.setOrientation(LinearLayout.VERTICAL);
		
		LabelColor label = new LabelColor(this.getActivity(), Color.BLUE, true, Gravity.CENTER);
		label.setText(text.toUpperCase());
		label.setTextSize(48);
		
		
    	LabelColor label2 = new LabelColor(this.getActivity(), Color.BLACK, true, Gravity.CENTER);
		label2.setText(String.valueOf(value));
		label2.setTextSize(72);
    	
    	layout1.addView(label);
    	layout1.addView(label2);
    	
    	card.addView(layout1);
    	
    	layout.addView(card);
    	
    	return layout;
    }

    private void executeSync(String message) {
		try {
			final MonitorView that = this;
			that.showWaiting(message);

			final Context context = _appConfig;
			_appConfig.getWorkingArea().UpgradeDataPost = true;

			boolean result = _appConfig.getMessageBox().ShowWithResult("Sincronización",
					"Desea resincronizar los datos con la central. Si sigue adelante, espere unos instantes hasta que reciba la notificación de que el proceso ha terminado",
					getActivity(), MessageBoxType.Information);

			if (!result) {
				_appConfig = (AppConfig) that.getActivity().getApplicationContext();
				that.fillDataMonitor(_appConfig.getWorkingArea().Monitor);
				return;
			}


			final ServiceWorker worker = new ServiceWorker();

			Thread syncThread = new Thread() {

				@Override
				public void run() {

					try {

						that.SyncResult = worker.RunImport(context, false);
						worker.RunExport(context);
						_appConfig = (AppConfig) that.getActivity().getApplicationContext();
						_appConfig.getWorkingArea().Monitor = worker.Monitor();

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

			};

			syncThread.start();
			try {
				syncThread.join();

				_appConfig = (AppConfig) that.getActivity().getApplicationContext();
				_appConfig.getWorkingArea().Monitor = worker.Monitor();
				that.fillDataMonitor(_appConfig.getWorkingArea().Monitor);

				if (that.SyncResult.isSuccess()) {
					_appConfig.getMessageBox().Show("Sincronización",
							"El proceso de sincronizacón ha finalizado CORRECTAMENTE. Revise los indicadores para comprobar si se ha realizado correctamente",
							getActivity(), MessageBoxType.Information);
				} else {
					StringBuilder errorMsg = new StringBuilder("El proceso de sincronizacón ha finalizado CON ERRORES:\n\n");
					for (String error : that.SyncResult.getErrorMessages()) {
						errorMsg.append("• ").append(error).append("\n");
					}
					errorMsg.append("\nRevise los indicadores para más detalles.");
					
					_appConfig.getMessageBox().Show("Sincronización",
							errorMsg.toString(),
							getActivity(), MessageBoxType.Error);

				}

				_appConfig.getCache().invalidate();
				_appConfig.getMediator().notify(ConstantsEvents.EVENT_STOCK_CHANGED, null);

			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private double roundTo2Decimals(double val) {
		DecimalFormat df2 = new DecimalFormat("0.00");
		return Double.parseDouble(df2.format(val).replace(",", "."));
	}

	private String getPasswordForRestore() {
		Calendar calendar = Calendar.getInstance();
		int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
		int currentYear = calendar.get(Calendar.YEAR);
		return String.valueOf(dayOfYear) + currentYear;
	}

	private void initializeEfectivo() {
		Efectivo efectivo = Factory.build(Efectivo.class, _appConfig);
		try {
			efectivo.getEfectivo();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		efectivo.Efectivo = 0;
		efectivo.UpdateDateEfectivo = new Date();
		efectivo.UpdateDateIngreso = new Date();
		try {
			efectivo.update();
			_appConfig.getMessageBox().Show("Inicializando ingresos de efectivo", "Se ha inicializado el ingreso de efectivo correctamente" ,getActivity(), MessageBoxType.Error);
		} catch (Exception e) {
			_appConfig.getMessageBox().Show("Inicializando ingresos de efectivo", "Se ha producido un error inicializando el ingreso de efectivo" ,getActivity(), MessageBoxType.Error);
		}
	}
}
