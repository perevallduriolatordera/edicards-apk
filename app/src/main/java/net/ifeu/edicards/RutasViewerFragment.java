package net.ifeu.edicards;

import android.app.ProgressDialog;
import android.content.Context;
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
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.CiudadVendedor;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;
import net.ifeu.edicards.Services.RouteGeneratorService;
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
		btnLimpiarCoordenadas = (Button) getActivity().findViewById(R.id.btnLimpiarCoordenadas);
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

		// Botón limpiar coordenadas (solo visible en debug/testing)
		if (btnLimpiarCoordenadas != null) {
			btnLimpiarCoordenadas.setOnClickListener(view -> {
				limpiarCoordenadasYRegeocordificar();
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

				// Generar ruta
				RouteGeneratorService service = new RouteGeneratorService();
				boolean success = service.generateRoute(app, ciudad.CiudadBase);

				// Actualizar UI en main thread
				getActivity().runOnUiThread(() -> {
					progressDialog.dismiss();

					if (success) {
						app.getMessageBox().Show("Éxito",
							"Ruta regenerada correctamente",
							getActivity(), MessageBoxType.Ok);
						cargarRutaActual(true);
					} else {
						app.getMessageBox().Show("Error",
							"No se pudo regenerar la ruta. Verifique conexión a internet.",
							getActivity(), MessageBoxType.Error);
					}
				});

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
				ExportToExcelService exportService = new ExportToExcelService();
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

			// Obtener coordenadas del cliente desde la BD
			try {
				android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
					"SELECT Latitud, Longitud FROM Clientes WHERE CodigoCliente = '" + ruta.CodigoCliente + "' LIMIT 1");

				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					Double latitud = cursor.getDouble(0);
					Double longitud = cursor.getDouble(1);
					cursor.close();

					if (latitud != null && longitud != null && latitud != 0 && longitud != 0) {
						txtLatitud.setText(String.format("%.6f", latitud));
						txtLongitud.setText(String.format("%.6f", longitud));
						layoutCoordenadas.setVisibility(android.view.View.VISIBLE);
					} else {
						layoutCoordenadas.setVisibility(android.view.View.GONE);
					}
				} else {
					layoutCoordenadas.setVisibility(android.view.View.GONE);
					if (cursor != null) cursor.close();
				}
			} catch (Exception e) {
				android.util.Log.w("RutasViewerFragment", "Error obteniendo coordenadas: " + e.getMessage());
				layoutCoordenadas.setVisibility(android.view.View.GONE);
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
}
