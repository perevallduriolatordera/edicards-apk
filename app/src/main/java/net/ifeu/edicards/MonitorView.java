package net.ifeu.edicards;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEvents;
import net.ifeu.edicards.DataTier.Contador;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.Excel.LogBookCreator;
import net.ifeu.edicards.Services.ParserMonitor;
import net.ifeu.edicards.Services.ServiceMonitor;
import net.ifeu.edicards.Services.ServiceWorker;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.Controls.LabelColor;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MonitorView extends Fragment {

	AppConfig _appConfig;
	public boolean SyncResult;

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
	
    private void fillDataMonitor(ServiceMonitor monitor) {
    	    	
    	if (monitor == null) {
    		monitor = new ServiceMonitor();
    		monitor.ParserMonitor = new ParserMonitor();
    	}
    	
    	if (monitor.ParserMonitor == null) {
    		monitor.ParserMonitor = new ParserMonitor();
    	}
    	
    	 
    	LinearLayout mainLinearLayout = (LinearLayout) this.getActivity().findViewById(R.id.mainLinearLayout);
    	mainLinearLayout.removeAllViews();
    	mainLinearLayout.setOrientation(LinearLayout.VERTICAL);
    	mainLinearLayout.setGravity(Gravity.CENTER);

    	ButtonColor sync = new ButtonColor(getActivity(), Color.RED);

    	sync.setText("Sincronización");
		int TEXT_SIZE_BUTTON = 18;
		sync.setTextSize(TEXT_SIZE_BUTTON);
		int BUTTONS_WIDTH = 100;
		sync.setWidth(BUTTONS_WIDTH);

    	final MonitorView that = this;
    	sync.setOnClickListener(arg0 -> {
			that.executeSync("Se está ejecutando la sincronización");
		});
    	
    	mainLinearLayout.addView(sync);

		ButtonColor logBookReport = new ButtonColor(getActivity(), Color.RED);

		logBookReport.setText("Enviar trazabilidad de stock");
		logBookReport.setTextSize(TEXT_SIZE_BUTTON);
		logBookReport.setWidth(BUTTONS_WIDTH);

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

		mainLinearLayout.addView(logBookReport);

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
			layout2.addView(this.createLabel("Fecha última copia", String.valueOf(backupDate), false, true));
		}

		layout.addView(layout2);
		mainLinearLayout.addView(layout);
    }
    
    private LinearLayout createLabel(String text, String value, boolean compress) {
    	
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

				if (that.SyncResult) {
					_appConfig.getMessageBox().Show("Sincronización",
							"El proceso de sincronizacón ha finalizado CORRECTAMENTE. Revise los indicadores para comprobar si se ha realizado correctamente",
							getActivity(), MessageBoxType.Information);
				} else {
					_appConfig.getMessageBox().Show("Sincronización",
							"El proceso de sincronizacón ha finalizado CON ERRORES. Revise los indicadores para comprobar si se ha realizado correctamente",
							getActivity(), MessageBoxType.Information);

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
}
