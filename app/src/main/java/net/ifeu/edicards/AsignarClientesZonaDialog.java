package net.ifeu.edicards;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Zona;
import net.ifeu.edicards.Services.ZonaManager;
import net.ifeu.library.Controls.ButtonColor;

import java.util.ArrayList;

public class AsignarClientesZonaDialog extends Activity {

	private AppConfig appConfig;
	private ZonaManager zonaManager;
	private ListView listViewClientes;
	private Spinner spinnerFiltroZona;
	private EditText txtFiltroPoblacion;
	private Spinner spinnerAsignacionMasiva;
	private TextView txtContadorClientes;

	private ArrayList<Cliente> todosClientes;
	private ArrayList<Cliente> clientesFiltrados;
	private ArrayList<Zona> zonas;
	private ClienteZonaAdapter adapter;

	private Long zonaFiltroSeleccionada = null; // null = todas las zonas
	private String poblacionFiltro = ""; // filtro de población

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_asignar_clientes_zona);

		// Hacer el diálogo más ancho (95% del ancho de pantalla)
		android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
		android.view.Display display = getWindowManager().getDefaultDisplay();
		android.graphics.Point size = new android.graphics.Point();
		display.getSize(size);
		params.width = (int) (size.x * 0.95);
		getWindow().setAttributes(params);

		appConfig = (AppConfig) this.getApplicationContext();
		zonaManager = new ZonaManager(appConfig);

		listViewClientes = findViewById(R.id.listViewClientes);
		spinnerFiltroZona = findViewById(R.id.spinnerFiltroZona);
		txtFiltroPoblacion = findViewById(R.id.txtFiltroPoblacion);
		spinnerAsignacionMasiva = findViewById(R.id.spinnerAsignacionMasiva);
		txtContadorClientes = findViewById(R.id.txtContadorClientes);
		ButtonColor btnCerrar = findViewById(R.id.btnCerrar);
		ButtonColor btnLimpiarFiltro = findViewById(R.id.btnLimpiarFiltro);
		ButtonColor btnAsignarTodos = findViewById(R.id.btnAsignarTodos);

		btnCerrar.setOnClickListener(v -> finish());
		btnLimpiarFiltro.setOnClickListener(v -> limpiarFiltroPoblacion());
		btnAsignarTodos.setOnClickListener(v -> asignarTodosFiltrados());

		// Listener para filtro de población
		txtFiltroPoblacion.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				poblacionFiltro = s.toString().trim();
				aplicarFiltros();
			}

			@Override
			public void afterTextChanged(Editable s) {}
		});

		cargarDatos();
	}

	private void cargarDatos() {
		try {
			// Cargar todas las zonas
			zonas = zonaManager.obtenerTodasLasZonas();

			// Cargar todos los clientes
			Cliente clienteFactory = Factory.build(Cliente.class, appConfig);
			todosClientes = clienteFactory.getAllClientes();
			clientesFiltrados = new ArrayList<>(todosClientes);

			// Configurar spinner de filtro
			configurarFiltroZona();

			// Configurar spinner de asignación masiva
			configurarSpinnerAsignacionMasiva();

			// Configurar lista de clientes
			adapter = new ClienteZonaAdapter();
			listViewClientes.setAdapter(adapter);

			actualizarContador();

		} catch (Exception e) {
			Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	private void configurarFiltroZona() {
		ArrayList<String> nombresZonas = new ArrayList<>();
		nombresZonas.add("Todas las zonas");
		nombresZonas.add("Sin asignar");

		for (Zona zona : zonas) {
			String nombre = zona.NombreZona;
			if (!zona.Activa) {
				nombre += " [INACTIVA]";
			}
			nombresZonas.add(nombre);
		}

		ArrayAdapter<String> adapterFiltro = new ArrayAdapter<>(
			this, android.R.layout.simple_spinner_item, nombresZonas);
		adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerFiltroZona.setAdapter(adapterFiltro);

		spinnerFiltroZona.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				establecerFiltroZona(position);
				aplicarFiltros();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
	}

	private void configurarSpinnerAsignacionMasiva() {
		ArrayList<String> nombresZonas = new ArrayList<>();
		nombresZonas.add("-- Seleccionar zona --");

		for (Zona zona : zonas) {
			if (zona.Activa) {  // Solo zonas activas
				nombresZonas.add(zona.NombreZona);
			}
		}

		ArrayAdapter<String> adapterAsignacion = new ArrayAdapter<>(
			this, android.R.layout.simple_spinner_item, nombresZonas);
		adapterAsignacion.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAsignacionMasiva.setAdapter(adapterAsignacion);
	}

	private void establecerFiltroZona(int position) {
		if (position == 0) {
			// Todas las zonas
			zonaFiltroSeleccionada = null;
		} else if (position == 1) {
			// Sin asignar
			zonaFiltroSeleccionada = Long.valueOf(-1); // Usamos -1 para indicar "sin asignar"
		} else {
			// Zona específica
			Zona zonaSeleccionada = zonas.get(position - 2);
			zonaFiltroSeleccionada = zonaSeleccionada.IdZona;
		}
	}

	private void aplicarFiltros() {
		clientesFiltrados.clear();

		for (Cliente cliente : todosClientes) {
			boolean pasaFiltroZona = false;
			boolean pasaFiltroPoblacion = false;

			// Filtro de zona
			if (zonaFiltroSeleccionada == null) {
				// Todas las zonas
				pasaFiltroZona = true;
			} else if (zonaFiltroSeleccionada == -1) {
				// Sin asignar
				pasaFiltroZona = (cliente.IdZona == null);
			} else {
				// Zona específica
				pasaFiltroZona = (cliente.IdZona != null && cliente.IdZona.equals(zonaFiltroSeleccionada));
			}

			// Filtro de población
			if (poblacionFiltro.isEmpty()) {
				pasaFiltroPoblacion = true;
			} else {
				String poblacionCliente = (cliente.Poblacion != null ? cliente.Poblacion : "").toLowerCase();
				pasaFiltroPoblacion = poblacionCliente.contains(poblacionFiltro.toLowerCase());
			}

			// Solo añadir si pasa ambos filtros
			if (pasaFiltroZona && pasaFiltroPoblacion) {
				clientesFiltrados.add(cliente);
			}
		}

		adapter.notifyDataSetChanged();
		actualizarContador();
	}

	private void limpiarFiltroPoblacion() {
		txtFiltroPoblacion.setText("");
		poblacionFiltro = "";
		aplicarFiltros();
	}

	private void asignarTodosFiltrados() {
		int posicionZona = spinnerAsignacionMasiva.getSelectedItemPosition();

		if (posicionZona == 0) {
			Toast.makeText(this, "Por favor, selecciona una zona", Toast.LENGTH_SHORT).show();
			return;
		}

		if (clientesFiltrados.isEmpty()) {
			Toast.makeText(this, "No hay clientes filtrados para asignar", Toast.LENGTH_SHORT).show();
			return;
		}

		// Encontrar la zona activa correspondiente
		Zona zonaSeleccionada = null;
		int countActivas = 0;
		for (Zona zona : zonas) {
			if (zona.Activa) {
				countActivas++;
				if (countActivas == posicionZona) {
					zonaSeleccionada = zona;
					break;
				}
			}
		}

		if (zonaSeleccionada == null) {
			Toast.makeText(this, "Error al obtener la zona seleccionada", Toast.LENGTH_SHORT).show();
			return;
		}

		// Confirmar con el usuario
		final Zona zonaFinal = zonaSeleccionada;
		String mensaje = "¿Asignar " + clientesFiltrados.size() + " clientes a la zona '"
			+ zonaFinal.NombreZona + "'?";

		boolean confirmar = appConfig.getMessageBox().ShowWithResult(
			"Confirmar asignación masiva",
			mensaje,
			this,
			net.ifeu.library.Utils.MessageBox.MessageBoxType.Question
		);

		if (confirmar) {
			int asignados = 0;
			int errores = 0;

			for (Cliente cliente : clientesFiltrados) {
				try {
					zonaManager.asignarClienteAZona(cliente.IdCliente, zonaFinal.IdZona);
					cliente.IdZona = zonaFinal.IdZona;
					asignados++;
				} catch (Exception e) {
					errores++;
				}
			}

			String resultado = asignados + " clientes asignados correctamente";
			if (errores > 0) {
				resultado += "\n" + errores + " errores";
			}

			Toast.makeText(this, resultado, Toast.LENGTH_LONG).show();

			// Resetear spinner de asignación masiva
			spinnerAsignacionMasiva.setSelection(0);

			// Actualizar la vista
			adapter.notifyDataSetChanged();

			// Notificar que se han asignado zonas para refrescar la pantalla de rutas
			Intent intent = new Intent(RutasViewerFragment.ACTION_ZONA_ASIGNADA);
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
		}
	}

	private void actualizarContador() {
		txtContadorClientes.setText("Total: " + clientesFiltrados.size() + " clientes");
	}

	private class ClienteZonaAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return clientesFiltrados.size();
		}

		@Override
		public Object getItem(int position) {
			return clientesFiltrados.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = LayoutInflater.from(AsignarClientesZonaDialog.this)
					.inflate(R.layout.item_cliente_zona, parent, false);

				holder = new ViewHolder();
				holder.txtCodigoCliente = convertView.findViewById(R.id.txtCodigoCliente);
				holder.txtNombreCliente = convertView.findViewById(R.id.txtNombreCliente);
				holder.txtPoblacionCliente = convertView.findViewById(R.id.txtPoblacionCliente);
				holder.spinnerZona = convertView.findViewById(R.id.spinnerZona);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			Cliente cliente = clientesFiltrados.get(position);

			// Configurar datos del cliente
			holder.txtCodigoCliente.setText(cliente.CodigoCliente);
			holder.txtNombreCliente.setText(cliente.Nombre);

			String poblacion = (cliente.Poblacion != null ? cliente.Poblacion : "") +
				(cliente.Provincia != null ? ", " + cliente.Provincia : "");
			holder.txtPoblacionCliente.setText(poblacion.trim());

			// Configurar spinner de zona
			configurarSpinnerZona(holder.spinnerZona, cliente);

			return convertView;
		}

		private void configurarSpinnerZona(Spinner spinner, final Cliente cliente) {
			ArrayList<String> nombresZonas = new ArrayList<>();
			nombresZonas.add("Sin asignar");

			for (Zona zona : zonas) {
				if (zona.Activa) {  // Solo zonas activas en el spinner
					nombresZonas.add(zona.NombreZona);
				}
			}

			ArrayAdapter<String> adapterZona = new ArrayAdapter<>(
				AsignarClientesZonaDialog.this,
				android.R.layout.simple_spinner_item,
				nombresZonas);
			adapterZona.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapterZona);

			// Seleccionar la zona actual del cliente
			int selectedPosition = 0;
			if (cliente.IdZona != null) {
				for (int i = 0; i < zonas.size(); i++) {
					if (zonas.get(i).Activa && zonas.get(i).IdZona == cliente.IdZona) {
						selectedPosition = i + 1; // +1 porque "Sin asignar" está en posición 0
						break;
					}
				}
			}
			spinner.setSelection(selectedPosition);

			// Listener para cambios en la zona
			spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					try {
						Long nuevaIdZona = null;

						if (position > 0) {
							// Encontrar la zona activa correspondiente
							int zonaActivaIndex = -1;
							int countActivas = 0;
							for (int i = 0; i < zonas.size(); i++) {
								if (zonas.get(i).Activa) {
									countActivas++;
									if (countActivas == position) {
										zonaActivaIndex = i;
										break;
									}
								}
							}
							if (zonaActivaIndex >= 0) {
								nuevaIdZona = zonas.get(zonaActivaIndex).IdZona;
							}
						}

						// Solo actualizar si realmente cambió
						if ((cliente.IdZona == null && nuevaIdZona != null) ||
							(cliente.IdZona != null && !cliente.IdZona.equals(nuevaIdZona))) {

							zonaManager.asignarClienteAZona(cliente.IdCliente, nuevaIdZona);
							cliente.IdZona = nuevaIdZona;

							String mensaje = nuevaIdZona == null
								? "Cliente desasignado de zona"
								: "Cliente asignado a zona";
							Toast.makeText(AsignarClientesZonaDialog.this, mensaje, Toast.LENGTH_SHORT).show();

							// Notificar que se ha asignado una zona para refrescar la pantalla de rutas
							Intent intent = new Intent(RutasViewerFragment.ACTION_ZONA_ASIGNADA);
							LocalBroadcastManager.getInstance(AsignarClientesZonaDialog.this).sendBroadcast(intent);
						}

					} catch (Exception e) {
						Toast.makeText(AsignarClientesZonaDialog.this,
							"Error al asignar zona: " + e.getMessage(),
							Toast.LENGTH_LONG).show();
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {}
			});
		}

		class ViewHolder {
			TextView txtCodigoCliente;
			TextView txtNombreCliente;
			TextView txtPoblacionCliente;
			Spinner spinnerZona;
		}
	}
}
