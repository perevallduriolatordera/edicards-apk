package net.ifeu.edicards;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.library.Controls.ButtonColor;

import java.util.ArrayList;

public class EstadoClientesDialog extends Activity {

	private AppConfig appConfig;
	private LinearLayout contenedorSinCoordenadas;
	private LinearLayout contenedorIncongruentes;
	private LinearLayout contenedorSinZona;

	// Límites geográficos de España
	private static final double LAT_MIN_PENINSULA = 34.0;
	private static final double LAT_MAX_PENINSULA = 44.0;
	private static final double LON_MIN_PENINSULA = -11.0;
	private static final double LON_MAX_PENINSULA = 5.0;
	private static final double LAT_MIN_CANARIAS = 27.0;
	private static final double LAT_MAX_CANARIAS = 30.0;
	private static final double LON_MIN_CANARIAS = -18.0;
	private static final double LON_MAX_CANARIAS = -13.0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_estado_clientes_dialog);

		// Hacer el diálogo más ancho (95% del ancho de pantalla)
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		android.view.Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		params.width = (int) (size.x * 0.95);
		params.height = (int) (size.y * 0.85);
		getWindow().setAttributes(params);

		appConfig = (AppConfig) this.getApplicationContext();

		contenedorSinCoordenadas = findViewById(R.id.contenedorSinCoordenadas);
		contenedorIncongruentes = findViewById(R.id.contenedorIncongruentes);
		contenedorSinZona = findViewById(R.id.contenedorSinZona);

		ButtonColor btnCerrar = findViewById(R.id.btnCerrarEstado);
		btnCerrar.setOnClickListener(v -> finish());

		cargarEstadoClientes();
	}

	private void cargarEstadoClientes() {
		try {
			cargarClientesSinCoordenadas();
			cargarClientesConCoordenadasIncongruentes();
			cargarClientesSinZona();
		} catch (Exception e) {
			Toast.makeText(this, "Error al cargar estado: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void cargarClientesSinCoordenadas() {
		contenedorSinCoordenadas.removeAllViews();

		try {
			Cursor cursor = appConfig.getDatabaseOperations().executeSentence(
				"SELECT CodigoCliente, Nombre, Direccion1, Poblacion FROM Clientes " +
				"WHERE Activo = 1 AND (Latitud IS NULL OR Latitud = 0 OR Longitud IS NULL OR Longitud = 0) " +
				"ORDER BY Nombre"
			);

			if (cursor != null && cursor.getCount() > 0) {
				TextView lblCount = findViewById(R.id.lblCountSinCoordenadas);
				lblCount.setText("(" + cursor.getCount() + " clientes)");

				cursor.moveToFirst();
				do {
					String codigo = cursor.getString(0);
					String nombre = cursor.getString(1);
					String direccion = cursor.getString(2);
					String poblacion = cursor.getString(3);

					agregarItemCliente(contenedorSinCoordenadas, codigo, nombre, direccion, poblacion, "SIN COORDENADAS");
				} while (cursor.moveToNext());
				cursor.close();
			} else {
				TextView lblCount = findViewById(R.id.lblCountSinCoordenadas);
				lblCount.setText("(0 clientes)");
				agregarMensajeVacio(contenedorSinCoordenadas, "Todos los clientes activos tienen coordenadas");
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error al cargar clientes sin coordenadas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void cargarClientesConCoordenadasIncongruentes() {
		contenedorIncongruentes.removeAllViews();
		ArrayList<ClienteProblematico> clientesIncongruentes = new ArrayList<>();

		try {
			Cursor cursor = appConfig.getDatabaseOperations().executeSentence(
				"SELECT CodigoCliente, Nombre, Direccion1, Poblacion, Latitud, Longitud FROM Clientes " +
				"WHERE Activo = 1 AND Latitud IS NOT NULL AND Latitud != 0 AND Longitud IS NOT NULL AND Longitud != 0 " +
				"ORDER BY Nombre"
			);

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				do {
					String codigo = cursor.getString(0);
					String nombre = cursor.getString(1);
					String direccion = cursor.getString(2);
					String poblacion = cursor.getString(3);
					double latitud = cursor.getDouble(4);
					double longitud = cursor.getDouble(5);

					String razonIncongruencia = validarCoordenadasEspana(latitud, longitud);
					if (razonIncongruencia != null) {
						// Coordenadas incongruentes
						ClienteProblematico cp = new ClienteProblematico();
						cp.codigo = codigo;
						cp.nombre = nombre;
						cp.direccion = direccion;
						cp.poblacion = poblacion;
						cp.razon = razonIncongruencia;
						clientesIncongruentes.add(cp);
					}
				} while (cursor.moveToNext());
				cursor.close();
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error al cargar coordenadas incongruentes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}

		TextView lblCount = findViewById(R.id.lblCountIncongruentes);
		lblCount.setText("(" + clientesIncongruentes.size() + " clientes)");

		if (clientesIncongruentes.isEmpty()) {
			agregarMensajeVacio(contenedorIncongruentes, "Todas las coordenadas son válidas para España");
		} else {
			for (ClienteProblematico cp : clientesIncongruentes) {
				agregarItemCliente(contenedorIncongruentes, cp.codigo, cp.nombre, cp.direccion, cp.poblacion, cp.razon);
			}
		}
	}

	private void cargarClientesSinZona() {
		contenedorSinZona.removeAllViews();

		try {
			Cursor cursor = appConfig.getDatabaseOperations().executeSentence(
				"SELECT CodigoCliente, Nombre, Direccion1, Poblacion FROM Clientes " +
				"WHERE Activo = 1 AND (IdZona IS NULL OR IdZona = 0) " +
				"ORDER BY Nombre"
			);

			if (cursor != null && cursor.getCount() > 0) {
				TextView lblCount = findViewById(R.id.lblCountSinZona);
				lblCount.setText("(" + cursor.getCount() + " clientes)");

				cursor.moveToFirst();
				do {
					String codigo = cursor.getString(0);
					String nombre = cursor.getString(1);
					String direccion = cursor.getString(2);
					String poblacion = cursor.getString(3);

					agregarItemCliente(contenedorSinZona, codigo, nombre, direccion, poblacion, "SIN ZONA ASIGNADA");
				} while (cursor.moveToNext());
				cursor.close();
			} else {
				TextView lblCount = findViewById(R.id.lblCountSinZona);
				lblCount.setText("(0 clientes)");
				agregarMensajeVacio(contenedorSinZona, "Todos los clientes activos tienen zona asignada");
			}
		} catch (Exception e) {
			Toast.makeText(this, "Error al cargar clientes sin zona: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	private void agregarItemCliente(LinearLayout contenedor, String codigo, String nombre, String direccion, String poblacion, String razon) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View itemView = inflater.inflate(R.layout.item_cliente_problematico, contenedor, false);

		TextView txtCodigo = itemView.findViewById(R.id.txtCodigoCliente);
		TextView txtNombre = itemView.findViewById(R.id.txtNombreCliente);
		TextView txtDireccion = itemView.findViewById(R.id.txtDireccionCliente);
		TextView txtRazon = itemView.findViewById(R.id.txtRazonProblema);

		txtCodigo.setText(codigo);
		txtNombre.setText(nombre);
		txtDireccion.setText((direccion != null ? direccion : "") + " - " + (poblacion != null ? poblacion : ""));
		txtRazon.setText(razon);

		contenedor.addView(itemView);
	}

	private void agregarMensajeVacio(LinearLayout contenedor, String mensaje) {
		TextView txtVacio = new TextView(this);
		txtVacio.setText(mensaje);
		txtVacio.setGravity(Gravity.CENTER);
		txtVacio.setPadding(16, 32, 16, 32);
		txtVacio.setTextColor(0xFF666666);
		txtVacio.setTextSize(14);
		contenedor.addView(txtVacio);
	}

	/**
	 * Valida que las coordenadas sean coherentes para España
	 * @return null si es válida, mensaje de error si es inválida
	 */
	private String validarCoordenadasEspana(Double latitud, Double longitud) {
		if (latitud == null || longitud == null) {
			return "Coordenadas nulas";
		}

		// Validación mundial básica
		if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
			return "Fuera del rango mundial";
		}

		// Verificar si las coordenadas están invertidas
		boolean posiblementeInvertidas = false;
		if (latitud >= LON_MIN_CANARIAS && latitud <= LON_MAX_PENINSULA &&
		    longitud >= LAT_MIN_CANARIAS && longitud <= LAT_MAX_PENINSULA) {
			posiblementeInvertidas = true;
		}

		// España Peninsular + Baleares
		boolean enPeninsula = (latitud >= LAT_MIN_PENINSULA && latitud <= LAT_MAX_PENINSULA) &&
		                      (longitud >= LON_MIN_PENINSULA && longitud <= LON_MAX_PENINSULA);

		// Islas Canarias
		boolean enCanarias = (latitud >= LAT_MIN_CANARIAS && latitud <= LAT_MAX_CANARIAS) &&
		                     (longitud >= LON_MIN_CANARIAS && longitud <= LON_MAX_CANARIAS);

		if (!enPeninsula && !enCanarias) {
			if (posiblementeInvertidas) {
				return "COORDENADAS INVERTIDAS (Lat=" + latitud + ", Lon=" + longitud + ")";
			}
			return "Fuera de España (Lat=" + latitud + ", Lon=" + longitud + ")";
		}

		return null; // Válida
	}

	// Clase auxiliar para almacenar clientes problemáticos
	private static class ClienteProblematico {
		String codigo;
		String nombre;
		String direccion;
		String poblacion;
		String razon;
	}
}
