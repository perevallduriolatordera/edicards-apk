package net.ifeu.edicards.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import net.ifeu.edicards.FichaClienteDialog;
import net.ifeu.edicards.R;

/**
 * BroadcastReceiver que se activa cuando se cumple la fecha/hora de un recordatorio
 * Muestra una notificación al usuario
 */
public class RecordatorioNotificationReceiver extends BroadcastReceiver {

	private static final String CHANNEL_ID = "recordatorios_channel";
	private static final String CHANNEL_NAME = "Recordatorios de Clientes";
	private static final String CHANNEL_DESCRIPTION = "Notificaciones de recordatorios de clientes";

	@Override
	public void onReceive(Context context, Intent intent) {
		// Obtener datos del recordatorio
		long idRecordatorio = intent.getLongExtra("idRecordatorio", -1);
		String codigoCliente = intent.getStringExtra("codigoCliente");
		String nombreCliente = intent.getStringExtra("nombreCliente");
		String tipoRecordatorio = intent.getStringExtra("tipoRecordatorio");
		String descripcion = intent.getStringExtra("descripcion");

		// Crear el canal de notificaciones (necesario para Android 8.0+)
		createNotificationChannel(context);

		// Crear intent para abrir la ficha del cliente al tocar la notificación
		Intent fichaIntent = new Intent(context, FichaClienteDialog.class);
		fichaIntent.putExtra("codigoCliente", codigoCliente);
		fichaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

		PendingIntent pendingIntent;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			pendingIntent = PendingIntent.getActivity(context, (int) idRecordatorio,
				fichaIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		} else {
			pendingIntent = PendingIntent.getActivity(context, (int) idRecordatorio,
				fichaIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		}

		// Construir la notificación
		String titulo = "Recordatorio: " + tipoRecordatorio;
		String texto = nombreCliente;
		if (descripcion != null && !descripcion.isEmpty()) {
			texto += "\n" + descripcion;
		}

		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
			.setSmallIcon(android.R.drawable.ic_dialog_info)
			.setContentTitle(titulo)
			.setContentText(texto)
			.setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.setVibrate(new long[]{0, 500, 200, 500});

		// Mostrar la notificación
		NotificationManager notificationManager =
			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null) {
			notificationManager.notify((int) idRecordatorio, builder.build());
		}
	}

	/**
	 * Crea el canal de notificaciones para Android 8.0 (API 26) en adelante
	 */
	private void createNotificationChannel(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				CHANNEL_NAME,
				NotificationManager.IMPORTANCE_HIGH
			);
			channel.setDescription(CHANNEL_DESCRIPTION);
			channel.enableVibration(true);
			channel.setVibrationPattern(new long[]{0, 500, 200, 500});

			NotificationManager notificationManager =
				context.getSystemService(NotificationManager.class);
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(channel);
			}
		}
	}
}
