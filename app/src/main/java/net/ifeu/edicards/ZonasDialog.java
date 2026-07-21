package net.ifeu.edicards;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Zona;
import net.ifeu.edicards.Services.ZonaManager;
import net.ifeu.library.Controls.ButtonColor;

import java.util.ArrayList;

public class ZonasDialog extends Activity {

	private AppConfig appConfig;
	private ZonaManager zonaManager;
	private LinearLayout zonasListLayout;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_zonas_dialog);

		// Hacer el diálogo más ancho (90% del ancho de pantalla)
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		android.view.Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		params.width = (int) (size.x * 0.9);
		getWindow().setAttributes(params);

		appConfig = (AppConfig) this.getApplicationContext();
		zonaManager = new ZonaManager(appConfig);

		zonasListLayout = findViewById(R.id.zonasListLayout);

		ButtonColor btnNuevaZona = findViewById(R.id.btnNuevaZona);
		ButtonColor btnCerrar = findViewById(R.id.btnCerrar);

		btnNuevaZona.setOnClickListener(v -> mostrarDialogNuevaZona());
		btnCerrar.setOnClickListener(v -> finish());

		cargarZonas();
	}

	private void cargarZonas() {
		try {
			zonasListLayout.removeAllViews();

			ArrayList<Zona> zonas = zonaManager.obtenerTodasLasZonas();

			if (zonas.isEmpty()) {
				TextView txtVacio = new TextView(this);
				txtVacio.setText("No hay zonas creadas. Haga clic en '+ Nueva Zona' para crear una.");
				txtVacio.setGravity(Gravity.CENTER);
				txtVacio.setPadding(16, 32, 16, 32);
				zonasListLayout.addView(txtVacio);
			} else {
				for (Zona zona : zonas) {
					agregarItemZona(zona);
				}
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error al cargar zonas: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void agregarItemZona(Zona zona) {
		try {
			LayoutInflater inflater = LayoutInflater.from(this);
			View itemView = inflater.inflate(R.layout.item_zona, zonasListLayout, false);

			TextView txtNombreZona = itemView.findViewById(R.id.txtNombreZona);
			TextView txtNumClientes = itemView.findViewById(R.id.txtNumClientes);
			ButtonColor btnVerClientes = itemView.findViewById(R.id.btnVerClientesZona);
		ButtonColor btnEditar = itemView.findViewById(R.id.btnEditarZona);
			ButtonColor btnActivar = itemView.findViewById(R.id.btnActivarZona);
			ButtonColor btnDesactivar = itemView.findViewById(R.id.btnDesactivarZona);
			ButtonColor btnBorrar = itemView.findViewById(R.id.btnBorrarZona);

			// Configurar nombre y estado
			String nombreConEstado = zona.NombreZona;
			if (!zona.Activa) {
				nombreConEstado += " [INACTIVA]";
			}
			txtNombreZona.setText(nombreConEstado);

			// Obtener número de clientes
			int numClientes = zonaManager.obtenerNumeroClientesEnZona(zona.IdZona);
			txtNumClientes.setText("(" + numClientes + " clientes)");

			// Configurar botones
			btnEditar.changeAspect(this, R.color.Orange, getResources().getDrawable(android.R.drawable.ic_menu_edit));
			btnActivar.changeAspect(this, R.color.Green, getResources().getDrawable(android.R.drawable.ic_input_add));
			btnDesactivar.changeAspect(this, R.color.Orange, getResources().getDrawable(android.R.drawable.ic_menu_close_clear_cancel));
			btnBorrar.changeAspect(this, R.color.Red, getResources().getDrawable(android.R.drawable.ic_menu_delete));

			// Mostrar/ocultar botones según estado
			if (zona.Activa) {
				btnActivar.setVisibility(android.view.View.GONE);
				btnDesactivar.setVisibility(android.view.View.VISIBLE);
			} else {
				btnActivar.setVisibility(android.view.View.VISIBLE);
				btnDesactivar.setVisibility(android.view.View.GONE);
			}

			// Configurar listeners
			btnVerClientes.setOnClickListener(v -> abrirAsignacionClientes());
		btnEditar.setOnClickListener(v -> mostrarDialogEditarZona(zona));
			btnActivar.setOnClickListener(v -> mostrarConfirmacionActivar(zona));
			btnDesactivar.setOnClickListener(v -> mostrarConfirmacionDesactivar(zona));
			btnBorrar.setOnClickListener(v -> mostrarConfirmacionBorrar(zona, numClientes));

			zonasListLayout.addView(itemView);

		} catch (Exception e) {
			Toast.makeText(this, "Error al agregar zona: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void mostrarDialogNuevaZona() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Nueva Zona");

		final EditText input = new EditText(this);
		input.setHint("Nombre de la zona");
		builder.setView(input);

		builder.setPositiveButton("Crear", (dialog, which) -> {
			String nombreZona = input.getText().toString().trim();

			if (nombreZona.isEmpty()) {
				Toast.makeText(this, "El nombre de la zona no puede estar vacío", Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				zonaManager.crearZona(nombreZona);
				Toast.makeText(this, "Zona creada: " + nombreZona, Toast.LENGTH_SHORT).show();
				cargarZonas();
			} catch (Exception e) {
				Toast.makeText(this, "Error al crear zona: " + e.getMessage(), Toast.LENGTH_LONG).show();
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}

	private void mostrarDialogEditarZona(Zona zona) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Editar Zona");

		final EditText input = new EditText(this);
		input.setText(zona.NombreZona);
		input.setSelection(zona.NombreZona.length());
		builder.setView(input);

		builder.setPositiveButton("Guardar", (dialog, which) -> {
			String nuevoNombre = input.getText().toString().trim();

			if (nuevoNombre.isEmpty()) {
				Toast.makeText(this, "El nombre de la zona no puede estar vacío", Toast.LENGTH_SHORT).show();
				return;
			}

			try {
				zonaManager.actualizarNombreZona(zona.IdZona, nuevoNombre);
				Toast.makeText(this, "Zona actualizada", Toast.LENGTH_SHORT).show();
				cargarZonas();
			} catch (Exception e) {
				Toast.makeText(this, "Error al actualizar zona: " + e.getMessage(), Toast.LENGTH_LONG).show();
			}
		});

		builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

		builder.show();
	}

	private void mostrarConfirmacionDesactivar(Zona zona) {
		new AlertDialog.Builder(this)
			.setTitle("Desactivar Zona")
			.setMessage("¿Está seguro de que desea desactivar la zona '" + zona.NombreZona + "'?")
			.setPositiveButton("Desactivar", (dialog, which) -> {
				try {
					zonaManager.desactivarZona(zona.IdZona);
					Toast.makeText(this, "Zona desactivada", Toast.LENGTH_SHORT).show();
					cargarZonas();
				} catch (Exception e) {
					Toast.makeText(this, "Error al desactivar zona: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			})
			.setNegativeButton("Cancelar", null)
			.show();
	}

	private void mostrarConfirmacionActivar(Zona zona) {
		new AlertDialog.Builder(this)
			.setTitle("Activar Zona")
			.setMessage("¿Está seguro de que desea activar la zona '" + zona.NombreZona + "'?")
			.setPositiveButton("Activar", (dialog, which) -> {
				try {
					zonaManager.activarZona(zona.IdZona);
					Toast.makeText(this, "Zona activada", Toast.LENGTH_SHORT).show();
					cargarZonas();
				} catch (Exception e) {
					Toast.makeText(this, "Error al activar zona: " + e.getMessage(), Toast.LENGTH_LONG).show();
				}
			})
			.setNegativeButton("Cancelar", null)
			.show();
	}

	private void mostrarConfirmacionBorrar(Zona zona, int numClientes) {
		if (numClientes > 0) {
			new AlertDialog.Builder(this)
				.setTitle("No se puede borrar")
				.setMessage("La zona '" + zona.NombreZona + "' tiene " + numClientes + " cliente(s) asignado(s). " +
					"Debe reasignar o eliminar los clientes antes de borrar la zona.")
				.setPositiveButton("Entendido", null)
				.show();
		} else {
			new AlertDialog.Builder(this)
				.setTitle("Borrar Zona")
				.setMessage("¿Está seguro de que desea BORRAR PERMANENTEMENTE la zona '" + zona.NombreZona + "'?\n\n" +
					"Esta acción no se puede deshacer.")
				.setPositiveButton("Borrar", (dialog, which) -> {
					try {
						zonaManager.eliminarZona(zona.IdZona);
						Toast.makeText(this, "Zona borrada permanentemente", Toast.LENGTH_SHORT).show();
						cargarZonas();
					} catch (Exception e) {
						Toast.makeText(this, "Error al borrar zona: " + e.getMessage(), Toast.LENGTH_LONG).show();
					}
				})
				.setNegativeButton("Cancelar", null)
				.show();
		}
	}

	private void abrirAsignacionClientes() {
		Intent intent = new Intent(this, AsignarClientesZonaDialog.class);
		startActivity(intent);
	}
}
