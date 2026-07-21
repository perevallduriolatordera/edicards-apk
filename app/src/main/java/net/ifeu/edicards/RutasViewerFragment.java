package net.ifeu.edicards;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.CiudadVendedor;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;
import net.ifeu.edicards.Services.RouteGeneratorService;
import net.ifeu.edicards.Services.RouteGenerationCallback;
import net.ifeu.edicards.Services.ExportToExcelService;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.text.SimpleDateFormat;
import java.util.List;

public class RutasViewerFragment extends Fragment {

	private AppConfig app;
	private ListView listViewRutas;
	private List<RutaGenerada> rutaActual;
	private Button btnRegenerarRuta;
	private Button btnLimpiarCoordenadas;
	private Button btnExportarExcel;
	private Button btnGestionarZonas;
	private Button btnEstadoGeocodificacion;
	private TextView lblFechaGeneracion;
	private TextView lblTotalClientes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                          Bundle savedInstanceState) {
		return inflater.inflate(R.layout.activity_rutas_viewer, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		app = (AppConfig) getActivity().getApplicationContext();

		// Inicializar componentes
		listViewRutas = (ListView) getActivity().findViewById(R.id.listViewRutas);
		btnRegenerarRuta = (Button) getActivity().findViewById(R.id.btnRegenerarRuta);
		btnExportarExcel = (Button) getActivity().findViewById(R.id.btnExportarExcel);
		btnGestionarZonas = (Button) getActivity().findViewById(R.id.btnGestionarZonas);
		btnLimpiarCoordenadas = (Button) getActivity().findViewById(R.id.btnLimpiarCoordenadas);
		btnEstadoGeocodificacion = (Button) getActivity().findViewById(R.id.btnEstadoGeocodificacion);
		lblFechaGeneracion = (TextView) getActivity().findViewById(R.id.lblFechaGeneracion);
		lblTotalClientes = (TextView) getActivity().findViewById(R.id.lblTotalClientes);

		// Cargar ruta (sin mostrar mensajes en initialization, ya que pueden causar WindowLeaked)
		cargarRutaActual(false);

		// Botón regenerar con password
		btnRegenerarRuta.setOnClickListener(view -> {
			solicitarPasswordYRegenerar();
		});

		// Botón exportar a Excel
		if (btnExportarExcel != null) {
			btnExportarExcel.setOnClickListener(view -> {
				exportarRutaAExcel();
			});
		}

		// Botón gestionar zonas
		if (btnGestionarZonas != null) {
			btnGestionarZonas.setOnClickListener(view -> {
				abrirGestionZonas();
			});
		}

		// Botón limpiar coordenadas (solo visible en debug/testing)
		if (btnLimpiarCoordenadas != null) {
			btnLimpiarCoordenadas.setOnClickListener(view -> {
				limpiarCoordenadasYRegeocordificar();
			});
		}

		// Botón estado geocodificación
		if (btnEstadoGeocodificacion != null) {
			btnEstadoGeocodificacion.setOnClickListener(view -> {
				mostrarEstadoGeocodificacion();
			});
		}
	}

	private void cargarRutaActual() {
		cargarRutaActual(true);
	}

	private void cargarRutaActual(boolean mostrarMensajes) {
		try {
			RutaGenerada ruta = Factory.build(RutaGenerada.class, app);
			rutaActual = ruta.getRutaCurrentWeek();

			if (rutaActual == null || rutaActual.isEmpty()) {
				if (mostrarMensajes) {
					app.getMessageBox().Show("Rutas",
						"No hay ruta generada para esta semana",
						getActivity(), MessageBoxType.Information);
				}
				return;
			}

			// Ordenar por OrdenVisita para asegurar que aparezcan en el orden correcto
			java.util.Collections.sort(rutaActual, new java.util.Comparator<RutaGenerada>() {
				@Override
				public int compare(RutaGenerada o1, RutaGenerada o2) {
					return Integer.compare(o1.OrdenVisita, o2.OrdenVisita);
				}
			});

			// Actualizar UI
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			lblFechaGeneracion.setText("Generada: " + formatter.format(rutaActual.get(0).FechaGeneracion));
			lblTotalClientes.setText("Total clientes: " + rutaActual.size());

			// Contar zonas únicas en la ruta
			java.util.HashSet<Long> zonasUnicas = new java.util.HashSet<>();
			for (RutaGenerada r : rutaActual) {
				try {
					android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
						"SELECT IdZona FROM Clientes WHERE CodigoCliente = '" + r.CodigoCliente + "' LIMIT 1");
					if (cursor != null && cursor.getCount() > 0) {
						cursor.moveToFirst();
						int idZonaIndex = cursor.getColumnIndex("IdZona");
						if (idZonaIndex >= 0 && !cursor.isNull(idZonaIndex)) {
							zonasUnicas.add(cursor.getLong(idZonaIndex));
						}
						cursor.close();
					}
				} catch (Exception e) {
					// Ignorar errores de zona
				}
			}

			// Mostrar cantidad de zonas si hay más de 1
			if (zonasUnicas.size() > 1) {
				lblTotalClientes.setText("Total clientes: " + rutaActual.size() + " (" + zonasUnicas.size() + " zonas)");
			}

			// Verificar si hay clientes sin geocodificar
			verificarClientesSinGeocoding();

			// Adapter personalizado
			RutaAdapter adapter = new RutaAdapter(getActivity(), rutaActual);
			listViewRutas.setAdapter(adapter);

		} catch (Exception e) {
			app.getMessageBox().Show("Error",
				"Error cargando ruta: " + e.getMessage(),
				getActivity(), MessageBoxType.Error);
		}
	}

	private void verificarClientesSinGeocoding() {
		try {
			// Contar clientes activos sin geocodificación (solo informativo)
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT COUNT(*) FROM Clientes WHERE Activo = 1 AND (Latitud IS NULL OR Longitud IS NULL)");

			int clientesSinGeocoding = 0;
			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();
				clientesSinGeocoding = cursor.getInt(0);
				cursor.close();
			}

			if (clientesSinGeocoding > 0) {
				android.util.Log.i("RutasViewerFragment", "Hay " + clientesSinGeocoding +
					" cliente(s) activo(s) sin geocodificación. Se incluirán en la ruta cuando se geocodifiquen.");
			}

		} catch (Exception e) {
			android.util.Log.w("RutasViewerFragment", "Error verificando clientes sin geocoding: " + e.getMessage());
		}
	}

	private void solicitarPasswordYRegenerar() {
		String password = app.getMessageBox().InputBox("Regenerar Ruta",
			"Introduzca la contraseña de manager", getActivity());

		if (password == null || password.isEmpty()) {
			return;
		}

		if (password.equals(ConstantsTypes.MANAGER_PASSWORD)) {
			regenerarRuta();
		} else {
			app.getMessageBox().Show("Error",
				"La contraseña introducida no es correcta",
				getActivity(), MessageBoxType.Error);
		}
	}

	private void limpiarCoordenadasYRegeocordificar() {
		// Confirmar con el usuario
		boolean confirm = app.getMessageBox().ShowWithResult("Limpiar Coordenadas",
			"¿Deseas limpiar todas las coordenadas geocodificadas?\n\n" +
			"Se borrarán las coordenadas de todos los clientes y se re-geocodificarán en la próxima sincronización.",
			getActivity(), MessageBoxType.Information);

		if (!confirm) {
			return;
		}

		// Mostrar progress dialog
		final ProgressDialog progressDialog = new ProgressDialog(getActivity());
		progressDialog.setMessage("Limpiando coordenadas...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		// Ejecutar en background
		new Thread(() -> {
			try {
				// Limpiar coordenadas
				net.ifeu.edicards.Services.Geocoding.OpenRouteServiceGeocodingStrategy.forceReGeocoding(getActivity());

				getActivity().runOnUiThread(() -> {
					progressDialog.dismiss();
					app.getMessageBox().Show("Éxito",
						"Coordenadas limpiadas correctamente.\n\n" +
						"En la próxima sincronización se re-geocodificarán todos los clientes.",
						getActivity(), MessageBoxType.Ok);
				});

			} catch (Exception e) {
				getActivity().runOnUiThread(() -> {
					progressDialog.dismiss();
					app.getMessageBox().Show("Error",
						"Error limpiando coordenadas: " + e.getMessage(),
						getActivity(), MessageBoxType.Error);
				});
			}
		}).start();
	}

	private void regenerarRuta() {
		// Mostrar progress dialog
		final ProgressDialog progressDialog = new ProgressDialog(getActivity());
		progressDialog.setMessage("Generando nueva ruta...\nEsto puede tardar 30-60 segundos");
		progressDialog.setCancelable(false);
		progressDialog.show();

		// Ejecutar en background
		new Thread(() -> {
			try {
				// Obtener ciudad base
				CiudadVendedor ciudad = Factory.build(CiudadVendedor.class, app);
				ciudad.load();

				if (ciudad.CiudadBase == null || ciudad.CiudadBase.isEmpty()) {
					getActivity().runOnUiThread(() -> {
						progressDialog.dismiss();
						app.getMessageBox().Show("Error",
							"Ciudad base no configurada",
							getActivity(), MessageBoxType.Error);
					});
					return;
				}

				// Generar ruta con Google Maps Routes API
				RouteGeneratorService service = new RouteGeneratorService();
				String googleMapsApiKey = ConstantsEndpoints.GOOGLE_MAPS_API_KEY;

				if (googleMapsApiKey == null || googleMapsApiKey.isEmpty()) {
					getActivity().runOnUiThread(() -> {
						progressDialog.dismiss();
						app.getMessageBox().Show("Error",
							"Google Maps API Key no está configurada",
							getActivity(), MessageBoxType.Error);
					});
				} else {
					service.generateRouteByZonesAsync(app, ciudad.CiudadBase, new RouteGenerationCallback() {
						@Override
						public void onProgress(String message) {
							getActivity().runOnUiThread(() -> {
								progressDialog.setMessage("Generando ruta: " + message);
							});
						}

						@Override
						public void onRouteGenerated() {
							getActivity().runOnUiThread(() -> {
								progressDialog.dismiss();
								app.getMessageBox().Show("Éxito",
									"Ruta regenerada correctamente con Google Maps",
									getActivity(), MessageBoxType.Ok);
								cargarRutaActual(true);
							});
						}

						@Override
						public void onError(String errorMsg) {
							getActivity().runOnUiThread(() -> {
								progressDialog.dismiss();
								app.getMessageBox().Show("Error",
									"No se pudo regenerar la ruta: " + errorMsg,
									getActivity(), MessageBoxType.Error);
							});
						}
					});
				}

			} catch (Exception e) {
				getActivity().runOnUiThread(() -> {
					progressDialog.dismiss();
					app.getMessageBox().Show("Error",
						"Error: " + e.getMessage(),
						getActivity(), MessageBoxType.Error);
				});
			}
		}).start();
	}

	private void exportarRutaAExcel() {
		if (rutaActual == null || rutaActual.isEmpty()) {
			app.getMessageBox().Show("Error",
				"No hay ruta para exportar",
				getActivity(), MessageBoxType.Error);
			return;
		}

		// Mostrar progress dialog
		final ProgressDialog progressDialog = new ProgressDialog(getActivity());
		progressDialog.setMessage("Exportando ruta a Excel...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		// Ejecutar en background
		new Thread(() -> {
			try {
				ExportToExcelService exportService = new ExportToExcelService(getActivity());
				String rutaArchivo = exportService.exportRutasToExcel(rutaActual);

				// Actualizar UI en main thread
				getActivity().runOnUiThread(() -> {
					progressDialog.dismiss();

					if (rutaArchivo != null) {
						app.getMessageBox().Show("Éxito",
							"Ruta exportada correctamente a:\n" + rutaArchivo,
							getActivity(), MessageBoxType.Ok);
					} else {
						app.getMessageBox().Show("Error",
							"No se pudo exportar la ruta",
							getActivity(), MessageBoxType.Error);
					}
				});

			} catch (Exception e) {
				getActivity().runOnUiThread(() -> {
					progressDialog.dismiss();
					app.getMessageBox().Show("Error",
						"Error exportando a Excel: " + e.getMessage(),
						getActivity(), MessageBoxType.Error);
				});
			}
		}).start();
	}

	/**
	 * Abre el dialog de gestión de zonas
	 */
	private void abrirGestionZonas() {
		Intent intent = new Intent(getActivity(), ZonasDialog.class);
		startActivity(intent);
	}

	// Adapter personalizado para ListView
	private class RutaAdapter extends BaseAdapter {
		private Context context;
		private List<RutaGenerada> rutas;

		public RutaAdapter(Context context, List<RutaGenerada> rutas) {
			this.context = context;
			this.rutas = rutas;
		}

		@Override
		public int getCount() {
			return rutas.size();
		}

		@Override
		public Object getItem(int position) {
			return rutas.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				convertView = inflater.inflate(R.layout.item_ruta, parent, false);
			}

			RutaGenerada ruta = rutas.get(position);

			TextView txtOrden = convertView.findViewById(R.id.txtOrden);
			TextView txtNombre = convertView.findViewById(R.id.txtNombre);
			TextView txtDireccion = convertView.findViewById(R.id.txtDireccion);
			TextView txtPoblacion = convertView.findViewById(R.id.txtPoblacion);
			TextView txtDistancia = convertView.findViewById(R.id.txtDistancia);
			TextView txtZona = convertView.findViewById(R.id.txtZona);

			// Elementos de coordenadas
			LinearLayout layoutCoordenadas = convertView.findViewById(R.id.layoutCoordenadas);
			TextView txtLatitud = convertView.findViewById(R.id.txtLatitud);
			TextView txtLongitud = convertView.findViewById(R.id.txtLongitud);

			txtOrden.setText(String.valueOf(ruta.OrdenVisita));
			txtNombre.setText(ruta.NombreCliente);
			txtDireccion.setText(ruta.DireccionCliente);

			String poblacionProvincia = "";
			if (ruta.PoblacionCliente != null && !ruta.PoblacionCliente.isEmpty()) {
				poblacionProvincia = ruta.PoblacionCliente;
				if (ruta.ProvinciaCliente != null && !ruta.ProvinciaCliente.isEmpty()) {
					poblacionProvincia += ", " + ruta.ProvinciaCliente;
				}
			}
			txtPoblacion.setText(poblacionProvincia);

			if (ruta.DistanciaEstimada != null && !ruta.DistanciaEstimada.isEmpty()) {
				txtDistancia.setText(ruta.DistanciaEstimada);
			} else {
				txtDistancia.setText("- km");
			}

			// Obtener coordenadas y zona del cliente desde la BD
			try {
				android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
					"SELECT Latitud, Longitud, IdZona FROM Clientes WHERE CodigoCliente = '" + ruta.CodigoCliente + "' LIMIT 1");

				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					Double latitud = cursor.getDouble(0);
					Double longitud = cursor.getDouble(1);

					// Obtener zona si existe
					int idZonaIndex = cursor.getColumnIndex("IdZona");
					Long idZona = null;
					if (idZonaIndex >= 0 && !cursor.isNull(idZonaIndex)) {
						idZona = cursor.getLong(idZonaIndex);
					}
					cursor.close();

					// Mostrar coordenadas si existen
					if (latitud != null && longitud != null && latitud != 0 && longitud != 0) {
						txtLatitud.setText(String.format("%.6f", latitud));
						txtLongitud.setText(String.format("%.6f", longitud));
						layoutCoordenadas.setVisibility(android.view.View.VISIBLE);
					} else {
						layoutCoordenadas.setVisibility(android.view.View.GONE);
					}

					// Mostrar zona si existe
					if (idZona != null && idZona > 0) {
						try {
							net.ifeu.edicards.DataTier.Zona zona = net.ifeu.edicards.DataTier.Factories.Factory.build(
								net.ifeu.edicards.DataTier.Zona.class, app);
							if (zona.setZonaById(idZona)) {
								txtZona.setText(zona.NombreZona);
								txtZona.setVisibility(android.view.View.VISIBLE);
							} else {
								txtZona.setVisibility(android.view.View.GONE);
							}
						} catch (Exception e) {
							txtZona.setVisibility(android.view.View.GONE);
						}
					} else {
						txtZona.setVisibility(android.view.View.GONE);
					}
				} else {
					layoutCoordenadas.setVisibility(android.view.View.GONE);
					txtZona.setVisibility(android.view.View.GONE);
					if (cursor != null) cursor.close();
				}
			} catch (Exception e) {
				android.util.Log.w("RutasViewerFragment", "Error obteniendo coordenadas: " + e.getMessage());
				layoutCoordenadas.setVisibility(android.view.View.GONE);
				txtZona.setVisibility(android.view.View.GONE);
			}

			// Click para expandir/contraer coordenadas
			convertView.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					if (layoutCoordenadas.getVisibility() == android.view.View.VISIBLE) {
						layoutCoordenadas.setVisibility(android.view.View.GONE);
					} else if (layoutCoordenadas.getVisibility() == android.view.View.GONE) {
						layoutCoordenadas.setVisibility(android.view.View.VISIBLE);
					}
				}
			});

			return convertView;
		}
	}

	/**
	 * Muestra un diálogo con el estado de geocodificación de los clientes
	 */
	private void mostrarEstadoGeocodificacion() {
		try {
			// Crear diálogo
			final android.app.Dialog dialog = new android.app.Dialog(getActivity());
			dialog.setContentView(R.layout.dialog_estado_geocodificacion);
			dialog.setTitle("Estado de Geocodificación");

			// Configurar tamaño del diálogo
			android.view.Window window = dialog.getWindow();
			if (window != null) {
				window.setLayout(
					(int) (getResources().getDisplayMetrics().widthPixels * 0.95),
					(int) (getResources().getDisplayMetrics().heightPixels * 0.85)
				);
			}

			// Obtener estadísticas
			net.ifeu.edicards.DataTier.Cliente clienteHelper = net.ifeu.edicards.DataTier.Factories.Factory.build(
				net.ifeu.edicards.DataTier.Cliente.class, app);

			int totalClientes = clienteHelper.getTotalClientes();
			int clientesGeocodificados = clienteHelper.getClientesGeocodificados();
			int clientesPendientes = totalClientes - clientesGeocodificados;
			double porcentaje = totalClientes > 0 ? (clientesGeocodificados * 100.0 / totalClientes) : 0;

			// Actualizar textos
			TextView txtTotalClientes = dialog.findViewById(R.id.txtTotalClientes);
			TextView txtClientesGeocodificados = dialog.findViewById(R.id.txtClientesGeocodificados);
			TextView txtClientesPendientes = dialog.findViewById(R.id.txtClientesPendientes);
			TextView txtPorcentaje = dialog.findViewById(R.id.txtPorcentaje);
			TextView txtMensajePendientes = dialog.findViewById(R.id.txtMensajePendientes);
			ListView listViewClientesPendientes = dialog.findViewById(R.id.listViewClientesPendientes);

			txtTotalClientes.setText(String.valueOf(totalClientes));
			txtClientesGeocodificados.setText(String.valueOf(clientesGeocodificados));
			txtClientesPendientes.setText(String.valueOf(clientesPendientes));
			txtPorcentaje.setText(String.format("%.1f%%", porcentaje));

			// Obtener lista de clientes pendientes
			if (clientesPendientes > 0) {
				java.util.ArrayList<net.ifeu.edicards.DataTier.Cliente> clientesPendientesList =
					clienteHelper.getClientesPendientesGeocodificacion();

				// Crear adapter para la lista
				BaseAdapter adapter = new BaseAdapter() {
					@Override
					public int getCount() {
						return clientesPendientesList.size();
					}

					@Override
					public Object getItem(int position) {
						return clientesPendientesList.get(position);
					}

					@Override
					public long getItemId(int position) {
						return position;
					}

					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						if (convertView == null) {
							convertView = LayoutInflater.from(getActivity()).inflate(
								R.layout.item_cliente_pendiente, parent, false);
						}

						net.ifeu.edicards.DataTier.Cliente cliente = clientesPendientesList.get(position);

						TextView txtCodigoCliente = convertView.findViewById(R.id.txtCodigoCliente);
						TextView txtNombreCliente = convertView.findViewById(R.id.txtNombreCliente);
						TextView txtDireccion = convertView.findViewById(R.id.txtDireccion);
						TextView txtPoblacion = convertView.findViewById(R.id.txtPoblacion);
						TextView txtMotivo = convertView.findViewById(R.id.txtMotivo);

						txtCodigoCliente.setText(cliente.CodigoCliente);
						txtNombreCliente.setText(cliente.Nombre);
						txtDireccion.setText(cliente.Direccion1 != null ? cliente.Direccion1 : "Sin dirección");

						String poblacion = "";
						if (cliente.Poblacion != null && !cliente.Poblacion.isEmpty()) {
							poblacion = cliente.Poblacion;
						}
						if (cliente.CodigoPostal != null && !cliente.CodigoPostal.isEmpty()) {
							poblacion += (poblacion.isEmpty() ? "" : ", ") + cliente.CodigoPostal;
						}
						txtPoblacion.setText(poblacion.isEmpty() ? "Sin población" : poblacion);

						// Determinar motivo
						String motivo;
						if (cliente.Latitud == null || cliente.Longitud == null) {
							motivo = "Sin coordenadas (null)";
						} else if (cliente.Latitud == 0 || cliente.Longitud == 0) {
							motivo = "Coordenadas = 0";
						} else if (cliente.Latitud < -90 || cliente.Latitud > 90 ||
						          cliente.Longitud < -180 || cliente.Longitud > 180) {
							motivo = "Coordenadas inválidas (fuera de rango)";
						} else {
							motivo = "Pendiente";
						}
						txtMotivo.setText(motivo);

						return convertView;
					}
				};

				listViewClientesPendientes.setAdapter(adapter);
				listViewClientesPendientes.setVisibility(View.VISIBLE);
				txtMensajePendientes.setVisibility(View.GONE);
			} else {
				listViewClientesPendientes.setVisibility(View.GONE);
				txtMensajePendientes.setVisibility(View.VISIBLE);
			}

			// Botón cerrar
			Button btnCerrar = dialog.findViewById(R.id.btnCerrar);
			btnCerrar.setOnClickListener(v -> dialog.dismiss());

			dialog.show();

		} catch (Exception e) {
			app.getMessageBox().Show("Error",
				"Error al cargar estado de geocodificación: " + e.getMessage(),
				getActivity(), MessageBoxType.Error);
			android.util.Log.e("RutasViewerFragment", "Error en mostrarEstadoGeocodificacion", e);
		}
	}
}
