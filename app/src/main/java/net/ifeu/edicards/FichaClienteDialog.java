package net.ifeu.edicards;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.ClienteComentario;
import net.ifeu.edicards.DataTier.ClienteInfo;
import net.ifeu.edicards.DataTier.ClienteRecordatorio;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Historico;
import net.ifeu.edicards.DataTier.LineaHistorico;
import net.ifeu.edicards.Services.RecordatorioAlarmManager;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class FichaClienteDialog extends Activity {

	private AppConfig appConfig;
	private String codigoCliente;
	private Cliente cliente;
	private ClienteInfo clienteInfo;
	private Deposito depositoActual;
	private Historico albaranActual;

	// Views de datos básicos
	private TextView txtNombreCliente;
	private TextView txtCodigoCliente;
	private TextView txtDireccion;
	private TextView txtPoblacion;
	private TextView txtTelefono;
	private TextView txtEmail;
	private TextView txtCCC;

	// Views de depósito
	private TextView txtInfoDeposito;
	private Button btnIrADeposito;

	// Views de albarán
	private TextView txtInfoAlbaran;
	private Button btnIrAAlbaran;

	// Views de recordatorios
	private ListView listViewRecordatorios;
	private Button btnAgregarRecordatorio;
	private RecordatoriosAdapter recordatoriosAdapter;
	private ArrayList<ClienteRecordatorio> recordatorios;

	// Views de comentarios
	private ListView listViewComentarios;
	private Button btnAgregarComentario;
	private ComentariosAdapter comentariosAdapter;
	private ArrayList<ClienteComentario> comentarios;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_ficha_cliente);

		// Hacer el diálogo más ancho
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		android.view.Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		params.width = (int) (size.x * 0.95);
		params.height = (int) (size.y * 0.9);
		getWindow().setAttributes(params);

		appConfig = (AppConfig) this.getApplicationContext();

		// Obtener código de cliente del intent
		codigoCliente = getIntent().getStringExtra("codigoCliente");
		if (codigoCliente == null || codigoCliente.isEmpty()) {
			Toast.makeText(this, "Error: Código de cliente no válido", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		inicializarVistas();
		cargarDatosCliente();
	}

	private void inicializarVistas() {
		// Cabecera
		txtNombreCliente = findViewById(R.id.txtNombreCliente);
		txtCodigoCliente = findViewById(R.id.txtCodigoCliente);

		// Datos básicos
		txtDireccion = findViewById(R.id.txtDireccion);
		txtPoblacion = findViewById(R.id.txtPoblacion);
		txtTelefono = findViewById(R.id.txtTelefono);
		txtEmail = findViewById(R.id.txtEmail);
		txtCCC = findViewById(R.id.txtCCC);

		// Depósito
		txtInfoDeposito = findViewById(R.id.txtInfoDeposito);
		btnIrADeposito = findViewById(R.id.btnIrADeposito);
		btnIrADeposito.setOnClickListener(v -> irADeposito());

		// Albarán
		txtInfoAlbaran = findViewById(R.id.txtInfoAlbaran);
		btnIrAAlbaran = findViewById(R.id.btnIrAAlbaran);
		btnIrAAlbaran.setOnClickListener(v -> irAAlbaran());

		// Recordatorios
		listViewRecordatorios = findViewById(R.id.listViewRecordatorios);
		btnAgregarRecordatorio = findViewById(R.id.btnAgregarRecordatorio);
		btnAgregarRecordatorio.setOnClickListener(v -> mostrarDialogNuevoRecordatorio());

		// Comentarios
		listViewComentarios = findViewById(R.id.listViewComentarios);
		btnAgregarComentario = findViewById(R.id.btnAgregarComentario);
		btnAgregarComentario.setOnClickListener(v -> mostrarDialogNuevoComentario());

		// Botón cerrar
		Button btnCerrar = findViewById(R.id.btnCerrar);
		btnCerrar.setOnClickListener(v -> finish());
	}

	private void cargarDatosCliente() {
		try {
			// Cargar cliente
			cliente = Factory.build(Cliente.class, appConfig);
			if (!cliente.setClienteByCodigo(codigoCliente)) {
				Toast.makeText(this, "Cliente no encontrado", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}

			// Cargar info adicional del cliente
			clienteInfo = Factory.build(ClienteInfo.class, appConfig);
			clienteInfo.setClienteInfoByCliente(cliente);

			cargarDatosBasicos();
			cargarInfoDeposito();
			cargarInfoAlbaran();
			cargarRecordatorios();
			cargarComentarios();

		} catch (Exception e) {
			Toast.makeText(this, "Error cargando datos: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error: " + e.getMessage());
		}
	}

	private void cargarDatosBasicos() {
		txtNombreCliente.setText(cliente.Nombre);
		txtCodigoCliente.setText("Código: " + cliente.CodigoCliente);

		String direccion = cliente.Direccion1;
		if (cliente.Direccion2 != null && !cliente.Direccion2.isEmpty()) {
			direccion += " " + cliente.Direccion2;
		}
		txtDireccion.setText("📍 " + direccion);

		txtPoblacion.setText("🏙️ " + cliente.Poblacion + " (" + cliente.CodigoPostal + "), " + cliente.Provincia);

		String telefono = "";
		if (cliente.Telefono1 != null && !cliente.Telefono1.isEmpty()) {
			telefono = cliente.Telefono1;
			if (cliente.Telefono2 != null && !cliente.Telefono2.isEmpty()) {
				telefono += " / " + cliente.Telefono2;
			}
		}
		txtTelefono.setText("📞 " + (telefono.isEmpty() ? "Sin teléfono" : telefono));

		txtEmail.setText("✉️ " + (cliente.Mail != null && !cliente.Mail.isEmpty() ? cliente.Mail : "Sin email"));

		txtCCC.setText("🏦 " + (clienteInfo.CCC != null && !clienteInfo.CCC.isEmpty() ? clienteInfo.CCC : "Sin CCC"));
	}

	private void cargarInfoDeposito() {
		try {
			depositoActual = Factory.build(Deposito.class, appConfig);
			boolean tieneDeposito = depositoActual.setFirstDepositoByCliente(cliente.CodigoCliente);

			if (tieneDeposito) {
				SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
				String fecha = formatter.format(depositoActual.FechaDeposito);
				txtInfoDeposito.setText("Depósito del " + fecha + "\nDoc: " + depositoActual.NumDoc);
				btnIrADeposito.setVisibility(android.view.View.VISIBLE);
			} else {
				txtInfoDeposito.setText("Sin depósito actual");
				btnIrADeposito.setVisibility(android.view.View.GONE);
				depositoActual = null;
			}
		} catch (Exception e) {
			txtInfoDeposito.setText("Error cargando depósito");
			btnIrADeposito.setVisibility(android.view.View.GONE);
			depositoActual = null;
			android.util.Log.e("FichaClienteDialog", "Error depósito: " + e.getMessage());
		}
	}

	private void cargarInfoAlbaran() {
		try {
			// Usar el método del modelo para obtener el último albarán
			albaranActual = cliente.getUltimoAlbaran(codigoCliente);

			if (albaranActual != null) {
				SimpleDateFormat displayFormatter = new SimpleDateFormat("dd/MM/yyyy");
				String fecha = displayFormatter.format(albaranActual.Fecha);
				String serieStr = (albaranActual.Serie != null && !albaranActual.Serie.isEmpty())
					? albaranActual.Serie + "-" : "";
				txtInfoAlbaran.setText("Albarán del " + fecha + "\nNº " + serieStr + albaranActual.NumeroAlbaran);
				btnIrAAlbaran.setVisibility(android.view.View.VISIBLE);
			} else {
				txtInfoAlbaran.setText("Sin albaranes registrados");
				btnIrAAlbaran.setVisibility(android.view.View.GONE);
			}
		} catch (Exception e) {
			txtInfoAlbaran.setText("Error cargando albarán");
			btnIrAAlbaran.setVisibility(android.view.View.GONE);
			albaranActual = null;
			android.util.Log.e("FichaClienteDialog", "Error albarán: " + e.getMessage(), e);
		}
	}

	private void cargarRecordatorios() {
		try {
			ClienteRecordatorio recordatorioModel = Factory.build(ClienteRecordatorio.class, appConfig);
			recordatorios = recordatorioModel.getRecordatoriosByCliente(codigoCliente);

			recordatoriosAdapter = new RecordatoriosAdapter();
			listViewRecordatorios.setAdapter(recordatoriosAdapter);

		} catch (Exception e) {
			Toast.makeText(this, "Error cargando recordatorios: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error recordatorios: " + e.getMessage());
		}
	}

	private void cargarComentarios() {
		try {
			ClienteComentario comentarioModel = Factory.build(ClienteComentario.class, appConfig);
			comentarios = comentarioModel.getComentariosByCliente(codigoCliente);

			comentariosAdapter = new ComentariosAdapter();
			listViewComentarios.setAdapter(comentariosAdapter);

		} catch (Exception e) {
			Toast.makeText(this, "Error cargando comentarios: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error comentarios: " + e.getMessage());
		}
	}

	private void irADeposito() {
		try {
			if (depositoActual == null) {
				Toast.makeText(this, "No hay depósito disponible para este cliente",
					Toast.LENGTH_SHORT).show();
				return;
			}

			// Cargar el depósito en el WorkingArea para que DepositView lo pueda mostrar
			appConfig.getWorkingArea().CurrentDeposito = depositoActual;

			Intent intent = new Intent(this, DepositView.class);
			startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(this, "Error abriendo depósito: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error: " + e.getMessage(), e);
		}
	}

	private void irAAlbaran() {
		try {
			if (albaranActual == null) {
				Toast.makeText(this, "No hay albarán disponible para este cliente",
					Toast.LENGTH_SHORT).show();
				return;
			}

			// Cargar el albarán en el WorkingArea para que AlbaranView lo pueda mostrar
			appConfig.getWorkingArea().CurrentHistorico = albaranActual;

			Intent intent = new Intent(this, AlbaranView.class);
			startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(this, "Error abriendo albarán: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error: " + e.getMessage(), e);
		}
	}

	private void mostrarDialogNuevoRecordatorio() {
		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nuevo_recordatorio, null);

		Spinner spinnerTipo = dialogView.findViewById(R.id.spinnerTipoRecordatorio);
		EditText editDescripcion = dialogView.findViewById(R.id.editDescripcionRecordatorio);
		Button btnFecha = dialogView.findViewById(R.id.btnFechaRecordatorio);
		Button btnHora = dialogView.findViewById(R.id.btnHoraRecordatorio);
		Button btnGuardar = dialogView.findViewById(R.id.btnGuardarRecordatorio);
		Button btnCancelar = dialogView.findViewById(R.id.btnCancelarRecordatorio);

		// Configurar spinner con tipos
		String[] tipos = {"Llamar", "Volver a pasar", "Otro"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipos);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerTipo.setAdapter(adapter);

		// Variables para fecha y hora
		// Inicializar con la fecha de hoy pero con hora en 00:00:00
		java.util.Calendar calInicial = java.util.Calendar.getInstance();
		calInicial.set(Calendar.HOUR_OF_DAY, 0);
		calInicial.set(Calendar.MINUTE, 0);
		calInicial.set(Calendar.SECOND, 0);
		calInicial.set(Calendar.MILLISECOND, 0);

		final Date[] fechaSeleccionada = {calInicial.getTime()};
		final String[] horaSeleccionada = {"09:00"};

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		btnFecha.setText(dateFormat.format(fechaSeleccionada[0]));
		btnHora.setText(horaSeleccionada[0]);

		// DatePicker
		btnFecha.setOnClickListener(v -> {
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(fechaSeleccionada[0]);

			new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
				java.util.Calendar newCal = java.util.Calendar.getInstance();
				newCal.set(Calendar.YEAR, year);
				newCal.set(Calendar.MONTH, month);
				newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				newCal.set(Calendar.HOUR_OF_DAY, 0);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				newCal.set(Calendar.MILLISECOND, 0);
				fechaSeleccionada[0] = newCal.getTime();
				btnFecha.setText(dateFormat.format(fechaSeleccionada[0]));
			}, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
			   cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
		});

		// TimePicker
		btnHora.setOnClickListener(v -> {
			String[] parts = horaSeleccionada[0].split(":");
			int hour = Integer.parseInt(parts[0]);
			int minute = Integer.parseInt(parts[1]);

			TimePickerDialog timePickerDialog = new TimePickerDialog(
				this,
				android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
				(view, hourOfDay, minuteOfHour) -> {
					horaSeleccionada[0] = String.format("%02d:%02d", hourOfDay, minuteOfHour);
					btnHora.setText(horaSeleccionada[0]);
				},
				hour,
				minute,
				android.text.format.DateFormat.is24HourFormat(this)
			);
			timePickerDialog.show();
		});

		AlertDialog dialog = new AlertDialog.Builder(this)
			.setView(dialogView)
			.create();

		btnGuardar.setOnClickListener(v -> {
			String tipo = spinnerTipo.getSelectedItem().toString();
			String descripcion = editDescripcion.getText().toString().trim();

			if (tipo.equals("Otro") && descripcion.isEmpty()) {
				Toast.makeText(this, "Debes escribir una descripción para 'Otro'",
					Toast.LENGTH_SHORT).show();
				return;
			}

			// Validar que la fecha/hora no esté en el pasado
			Calendar testCal = Calendar.getInstance();
			testCal.setTime(fechaSeleccionada[0]);
			String[] parts = horaSeleccionada[0].split(":");
			testCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
			testCal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
			testCal.set(Calendar.SECOND, 0);
			testCal.set(Calendar.MILLISECOND, 0);

			long triggerTime = testCal.getTimeInMillis();
			long now = System.currentTimeMillis();

			// Log de debug
			SimpleDateFormat debugFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			android.util.Log.d("FichaClienteDialog", "Validación:");
			android.util.Log.d("FichaClienteDialog", "  Trigger: " + debugFormat.format(new Date(triggerTime)));
			android.util.Log.d("FichaClienteDialog", "  Ahora:   " + debugFormat.format(new Date(now)));
			android.util.Log.d("FichaClienteDialog", "  Diferencia (seg): " + ((triggerTime - now) / 1000));

			// Permitir un margen de 10 segundos para evitar falsos negativos
			if (triggerTime < (now - 10000)) {
				Toast.makeText(this, "La fecha y hora del recordatorio debe ser futura",
					Toast.LENGTH_LONG).show();
				return;
			}

			guardarRecordatorio(tipo, descripcion, fechaSeleccionada[0], horaSeleccionada[0]);
			dialog.dismiss();
		});

		btnCancelar.setOnClickListener(v -> dialog.dismiss());

		dialog.show();
	}

	private void guardarRecordatorio(String tipo, String descripcion, Date fecha, String hora) {
		try {
			// Log de debugging
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			android.util.Log.d("FichaClienteDialog", "Guardando recordatorio:");
			android.util.Log.d("FichaClienteDialog", "  Fecha recibida: " + sdf.format(fecha));
			android.util.Log.d("FichaClienteDialog", "  Hora recibida: " + hora);

			ClienteRecordatorio recordatorio = Factory.build(ClienteRecordatorio.class, appConfig);
			recordatorio.CodigoCliente = codigoCliente;
			recordatorio.TipoRecordatorio = tipo;
			recordatorio.Descripcion = descripcion;
			recordatorio.FechaRecordatorio = fecha;
			recordatorio.HoraRecordatorio = hora;
			recordatorio.FechaCreacion = new Date();
			recordatorio.Usuario = appConfig.getUser().Name;
			recordatorio.Completado = false;
			recordatorio.save();

			android.util.Log.d("FichaClienteDialog", "  ID guardado: " + recordatorio.IdRecordatorio);
			android.util.Log.d("FichaClienteDialog", "  Fecha en objeto: " + sdf.format(recordatorio.FechaRecordatorio));
			android.util.Log.d("FichaClienteDialog", "  Hora en objeto: " + recordatorio.HoraRecordatorio);

			// Programar notificación con AlarmManager
			RecordatorioAlarmManager.programarAlarma(this, recordatorio, cliente.Nombre);

			Toast.makeText(this, "Recordatorio guardado", Toast.LENGTH_SHORT).show();
			cargarRecordatorios();

		} catch (Exception e) {
			Toast.makeText(this, "Error guardando recordatorio: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error: " + e.getMessage());
		}
	}

	private void completarRecordatorio(ClienteRecordatorio recordatorio) {
		try {
			recordatorio.Completado = true;
			recordatorio.FechaCompletado = new Date();
			recordatorio.update();

			// Cancelar notificación programada
			RecordatorioAlarmManager.cancelarAlarma(this, recordatorio.IdRecordatorio);

			Toast.makeText(this, "Recordatorio completado", Toast.LENGTH_SHORT).show();
			cargarRecordatorios();

		} catch (Exception e) {
			Toast.makeText(this, "Error completando recordatorio: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error: " + e.getMessage());
		}
	}

	private void editarRecordatorio(ClienteRecordatorio recordatorio) {
		// Similar a crear, pero pre-rellenado
		View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nuevo_recordatorio, null);

		Spinner spinnerTipo = dialogView.findViewById(R.id.spinnerTipoRecordatorio);
		EditText editDescripcion = dialogView.findViewById(R.id.editDescripcionRecordatorio);
		Button btnFecha = dialogView.findViewById(R.id.btnFechaRecordatorio);
		Button btnHora = dialogView.findViewById(R.id.btnHoraRecordatorio);
		Button btnGuardar = dialogView.findViewById(R.id.btnGuardarRecordatorio);
		Button btnCancelar = dialogView.findViewById(R.id.btnCancelarRecordatorio);

		// Configurar spinner
		String[] tipos = {"Llamar", "Volver a pasar", "Otro"};
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipos);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerTipo.setAdapter(adapter);

		// Pre-seleccionar tipo
		for (int i = 0; i < tipos.length; i++) {
			if (tipos[i].equals(recordatorio.TipoRecordatorio)) {
				spinnerTipo.setSelection(i);
				break;
			}
		}

		editDescripcion.setText(recordatorio.Descripcion);

		final Date[] fechaSeleccionada = {recordatorio.FechaRecordatorio};
		final String[] horaSeleccionada = {recordatorio.HoraRecordatorio};

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		btnFecha.setText(dateFormat.format(fechaSeleccionada[0]));
		btnHora.setText(horaSeleccionada[0]);

		// DatePicker
		btnFecha.setOnClickListener(v -> {
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(fechaSeleccionada[0]);

			new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
				java.util.Calendar newCal = java.util.Calendar.getInstance();
				newCal.set(Calendar.YEAR, year);
				newCal.set(Calendar.MONTH, month);
				newCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				newCal.set(Calendar.HOUR_OF_DAY, 0);
				newCal.set(Calendar.MINUTE, 0);
				newCal.set(Calendar.SECOND, 0);
				newCal.set(Calendar.MILLISECOND, 0);
				fechaSeleccionada[0] = newCal.getTime();
				btnFecha.setText(dateFormat.format(fechaSeleccionada[0]));
			}, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
			   cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
		});

		// TimePicker
		btnHora.setOnClickListener(v -> {
			String[] parts = horaSeleccionada[0].split(":");
			int hour = Integer.parseInt(parts[0]);
			int minute = Integer.parseInt(parts[1]);

			TimePickerDialog timePickerDialog = new TimePickerDialog(
				this,
				android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
				(view, hourOfDay, minuteOfHour) -> {
					horaSeleccionada[0] = String.format("%02d:%02d", hourOfDay, minuteOfHour);
					btnHora.setText(horaSeleccionada[0]);
				},
				hour,
				minute,
				android.text.format.DateFormat.is24HourFormat(this)
			);
			timePickerDialog.show();
		});

		AlertDialog dialog = new AlertDialog.Builder(this)
			.setView(dialogView)
			.create();

		btnGuardar.setOnClickListener(v -> {
			String tipo = spinnerTipo.getSelectedItem().toString();
			String descripcion = editDescripcion.getText().toString().trim();

			if (tipo.equals("Otro") && descripcion.isEmpty()) {
				Toast.makeText(this, "Debes escribir una descripción para 'Otro'",
					Toast.LENGTH_SHORT).show();
				return;
			}

			// Validar que la fecha/hora no esté en el pasado
			Calendar testCal = Calendar.getInstance();
			testCal.setTime(fechaSeleccionada[0]);
			String[] parts = horaSeleccionada[0].split(":");
			testCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
			testCal.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
			testCal.set(Calendar.SECOND, 0);
			testCal.set(Calendar.MILLISECOND, 0);

			long triggerTime = testCal.getTimeInMillis();
			long now = System.currentTimeMillis();

			// Log de debug
			SimpleDateFormat debugFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			android.util.Log.d("FichaClienteDialog", "Validación:");
			android.util.Log.d("FichaClienteDialog", "  Trigger: " + debugFormat.format(new Date(triggerTime)));
			android.util.Log.d("FichaClienteDialog", "  Ahora:   " + debugFormat.format(new Date(now)));
			android.util.Log.d("FichaClienteDialog", "  Diferencia (seg): " + ((triggerTime - now) / 1000));

			// Permitir un margen de 10 segundos para evitar falsos negativos
			if (triggerTime < (now - 10000)) {
				Toast.makeText(this, "La fecha y hora del recordatorio debe ser futura",
					Toast.LENGTH_LONG).show();
				return;
			}

			try {
				recordatorio.TipoRecordatorio = tipo;
				recordatorio.Descripcion = descripcion;
				recordatorio.FechaRecordatorio = fechaSeleccionada[0];
				recordatorio.HoraRecordatorio = horaSeleccionada[0];
				recordatorio.update();

				// Reprogramar notificación
				RecordatorioAlarmManager.cancelarAlarma(this, recordatorio.IdRecordatorio);
				RecordatorioAlarmManager.programarAlarma(this, recordatorio, cliente.Nombre);

				Toast.makeText(this, "Recordatorio actualizado", Toast.LENGTH_SHORT).show();
				cargarRecordatorios();
				dialog.dismiss();

			} catch (Exception e) {
				Toast.makeText(this, "Error actualizando: " + e.getMessage(),
					Toast.LENGTH_SHORT).show();
			}
		});

		btnCancelar.setOnClickListener(v -> dialog.dismiss());

		dialog.show();
	}

	private void eliminarRecordatorio(ClienteRecordatorio recordatorio) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Eliminar Recordatorio");
		builder.setMessage("¿Estás seguro de que deseas eliminar este recordatorio?");

		builder.setPositiveButton("Eliminar", (dialog, which) -> {
			try {
				// Cancelar notificación programada
				RecordatorioAlarmManager.cancelarAlarma(this, recordatorio.IdRecordatorio);

				recordatorio.delete();

				Toast.makeText(this, "Recordatorio eliminado", Toast.LENGTH_SHORT).show();
				cargarRecordatorios();

			} catch (Exception e) {
				Toast.makeText(this, "Error eliminando: " + e.getMessage(),
					Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}

	private void mostrarDialogNuevoComentario() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Nuevo Comentario");

		final EditText input = new EditText(this);
		input.setHint("Escribe tu comentario aquí");
		input.setLines(4);
		input.setMinLines(3);
		builder.setView(input);

		builder.setPositiveButton("Guardar", (dialog, which) -> {
			String textoComentario = input.getText().toString().trim();
			if (!textoComentario.isEmpty()) {
				guardarComentario(textoComentario);
			} else {
				Toast.makeText(this, "El comentario no puede estar vacío",
					Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}

	private void guardarComentario(String texto) {
		try {
			ClienteComentario comentario = Factory.build(ClienteComentario.class, appConfig);
			comentario.CodigoCliente = codigoCliente;
			comentario.Comentario = texto;
			comentario.FechaCreacion = new Date();
			comentario.Usuario = appConfig.getUser().Name;
			comentario.save();

			Toast.makeText(this, "Comentario guardado", Toast.LENGTH_SHORT).show();
			cargarComentarios();

		} catch (Exception e) {
			Toast.makeText(this, "Error guardando comentario: " + e.getMessage(),
				Toast.LENGTH_SHORT).show();
			android.util.Log.e("FichaClienteDialog", "Error: " + e.getMessage());
		}
	}

	private void editarComentario(ClienteComentario comentario) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Editar Comentario");

		final EditText input = new EditText(this);
		input.setText(comentario.Comentario);
		input.setLines(4);
		input.setMinLines(3);
		input.setSelection(comentario.Comentario.length());
		builder.setView(input);

		builder.setPositiveButton("Guardar", (dialog, which) -> {
			String textoComentario = input.getText().toString().trim();
			if (!textoComentario.isEmpty()) {
				try {
					comentario.Comentario = textoComentario;
					comentario.update();
					Toast.makeText(this, "Comentario actualizado", Toast.LENGTH_SHORT).show();
					cargarComentarios();
				} catch (Exception e) {
					Toast.makeText(this, "Error actualizando: " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
				}
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}

	private void eliminarComentario(ClienteComentario comentario) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Eliminar Comentario");
		builder.setMessage("¿Estás seguro de que deseas eliminar este comentario?");

		builder.setPositiveButton("Eliminar", (dialog, which) -> {
			try {
				comentario.delete();
				Toast.makeText(this, "Comentario eliminado", Toast.LENGTH_SHORT).show();
				cargarComentarios();
			} catch (Exception e) {
				Toast.makeText(this, "Error eliminando: " + e.getMessage(),
					Toast.LENGTH_SHORT).show();
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}

	// Adapter para la lista de recordatorios
	private class RecordatoriosAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return recordatorios != null ? recordatorios.size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return recordatorios.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(FichaClienteDialog.this);
				convertView = inflater.inflate(R.layout.item_recordatorio, parent, false);
			}

			ClienteRecordatorio recordatorio = recordatorios.get(position);

			TextView txtTipo = convertView.findViewById(R.id.txtTipoRecordatorio);
			TextView txtFechaHora = convertView.findViewById(R.id.txtFechaHoraRecordatorio);
			TextView txtDescripcion = convertView.findViewById(R.id.txtDescripcionRecordatorio);
			Button btnCompletar = convertView.findViewById(R.id.btnCompletarRecordatorio);
			Button btnEditar = convertView.findViewById(R.id.btnEditarRecordatorio);
			Button btnEliminar = convertView.findViewById(R.id.btnEliminarRecordatorio);

			// Tipo de recordatorio
			txtTipo.setText(recordatorio.TipoRecordatorio);

			// Fecha y hora
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			String fechaHora = dateFormat.format(recordatorio.FechaRecordatorio) + " " + recordatorio.HoraRecordatorio;
			txtFechaHora.setText(fechaHora);

			// Descripción (si existe)
			if (recordatorio.Descripcion != null && !recordatorio.Descripcion.isEmpty()) {
				txtDescripcion.setText(recordatorio.Descripcion);
				txtDescripcion.setVisibility(View.VISIBLE);
			} else {
				txtDescripcion.setVisibility(View.GONE);
			}

			// Si está completado, cambiar estilo
			if (recordatorio.Completado) {
				txtTipo.setTextColor(0xFF9E9E9E); // Gris
				txtFechaHora.setTextColor(0xFF9E9E9E);
				txtDescripcion.setTextColor(0xFF9E9E9E);
				btnCompletar.setVisibility(View.GONE);
			} else {
				txtTipo.setTextColor(0xFF1976D2); // Azul
				txtFechaHora.setTextColor(0xFF666666);
				txtDescripcion.setTextColor(0xFF424242);
				btnCompletar.setVisibility(View.VISIBLE);
			}

			// Configurar botones
			btnCompletar.setOnClickListener(v -> completarRecordatorio(recordatorio));
			btnEditar.setOnClickListener(v -> editarRecordatorio(recordatorio));
			btnEliminar.setOnClickListener(v -> eliminarRecordatorio(recordatorio));

			return convertView;
		}
	}

	// Adapter para la lista de comentarios
	private class ComentariosAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return comentarios != null ? comentarios.size() : 0;
		}

		@Override
		public Object getItem(int position) {
			return comentarios.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(FichaClienteDialog.this);
				convertView = inflater.inflate(R.layout.item_comentario, parent, false);
			}

			ClienteComentario comentario = comentarios.get(position);

			TextView txtFecha = convertView.findViewById(R.id.txtFechaComentario);
			TextView txtComentario = convertView.findViewById(R.id.txtComentario);
			TextView txtUsuario = convertView.findViewById(R.id.txtUsuarioComentario);
			Button btnEditar = convertView.findViewById(R.id.btnEditarComentario);
			Button btnEliminar = convertView.findViewById(R.id.btnEliminarComentario);

			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
			txtFecha.setText(formatter.format(comentario.FechaCreacion));
			txtComentario.setText(comentario.Comentario);
			txtUsuario.setText("Por: " + comentario.Usuario);

			btnEditar.setOnClickListener(v -> editarComentario(comentario));
			btnEliminar.setOnClickListener(v -> eliminarComentario(comentario));

			return convertView;
		}
	}
}
