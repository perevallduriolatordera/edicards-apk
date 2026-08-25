package net.ifeu.edicards.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import net.ifeu.edicards.DataTier.ClienteRecordatorio;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Clase helper para programar y cancelar alarmas de recordatorios
 * Usa AlarmManager para programar notificaciones locales
 */
public class RecordatorioAlarmManager {

	private static final String TAG = "RecordatorioAlarmMgr";

	/**
	 * Programa una alarma para un recordatorio
	 */
	public static void programarAlarma(Context context, ClienteRecordatorio recordatorio, String nombreCliente) {
		try {
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			if (alarmManager == null) {
				Log.e(TAG, "AlarmManager no disponible");
				return;
			}

			// Crear el PendingIntent que se ejecutará cuando se cumpla la alarma
			Intent intent = new Intent(context, RecordatorioNotificationReceiver.class);
			intent.putExtra("idRecordatorio", recordatorio.IdRecordatorio);
			intent.putExtra("codigoCliente", recordatorio.CodigoCliente);
			intent.putExtra("nombreCliente", nombreCliente);
			intent.putExtra("tipoRecordatorio", recordatorio.TipoRecordatorio);
			intent.putExtra("descripcion", recordatorio.Descripcion);

			PendingIntent pendingIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				pendingIntent = PendingIntent.getBroadcast(context,
					(int) recordatorio.IdRecordatorio,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				pendingIntent = PendingIntent.getBroadcast(context,
					(int) recordatorio.IdRecordatorio,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			}

			// Calcular el timestamp para la alarma
			long triggerTime = calcularTiempoAlarma(recordatorio.FechaRecordatorio, recordatorio.HoraRecordatorio);

			// Programar la alarma
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				// API 23+: Usar setExactAndAllowWhileIdle para que funcione en Doze mode
				alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				// API 19+: Usar setExact
				alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
			} else {
				// API < 19: Usar set normal
				alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
			}

			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			Log.d(TAG, "Alarma programada para: " + sdf.format(new Date(triggerTime)) +
				" (ID: " + recordatorio.IdRecordatorio + ")");

		} catch (Exception e) {
			Log.e(TAG, "Error programando alarma: " + e.getMessage(), e);
		}
	}

	/**
	 * Cancela una alarma previamente programada
	 */
	public static void cancelarAlarma(Context context, long idRecordatorio) {
		try {
			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			if (alarmManager == null) {
				Log.e(TAG, "AlarmManager no disponible");
				return;
			}

			Intent intent = new Intent(context, RecordatorioNotificationReceiver.class);

			PendingIntent pendingIntent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				pendingIntent = PendingIntent.getBroadcast(context,
					(int) idRecordatorio,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				pendingIntent = PendingIntent.getBroadcast(context,
					(int) idRecordatorio,
					intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			}

			alarmManager.cancel(pendingIntent);
			pendingIntent.cancel();

			Log.d(TAG, "Alarma cancelada (ID: " + idRecordatorio + ")");

		} catch (Exception e) {
			Log.e(TAG, "Error cancelando alarma: " + e.getMessage(), e);
		}
	}

	/**
	 * Calcula el timestamp en milisegundos para la fecha y hora del recordatorio
	 */
	private static long calcularTiempoAlarma(Date fecha, String hora) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fecha);

		// Parsear la hora (formato "HH:mm")
		String[] parts = hora.split(":");
		int hours = Integer.parseInt(parts[0]);
		int minutes = Integer.parseInt(parts[1]);

		calendar.set(Calendar.HOUR_OF_DAY, hours);
		calendar.set(Calendar.MINUTE, minutes);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		long triggerTime = calendar.getTimeInMillis();
		long now = System.currentTimeMillis();

		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Log.d(TAG, "Calculando alarma:");
		Log.d(TAG, "  Fecha recibida: " + sdf.format(fecha));
		Log.d(TAG, "  Hora recibida: " + hora);
		Log.d(TAG, "  Tiempo calculado: " + sdf.format(new Date(triggerTime)));
		Log.d(TAG, "  Tiempo actual: " + sdf.format(new Date(now)));
		Log.d(TAG, "  Diferencia (ms): " + (triggerTime - now));

		// Verificar si la alarma está en el pasado
		if (triggerTime <= now) {
			Log.w(TAG, "ADVERTENCIA: La alarma está programada en el pasado!");
		}

		return triggerTime;
	}
}
