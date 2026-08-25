package net.ifeu.edicards;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;
import net.ifeu.edicards.Services.RouteGeneratorService;
import net.ifeu.edicards.Services.RouteGenerationCallback;
import net.ifeu.edicards.Services.ExportToExcelService;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class RutasViewerFragment extends Fragment {

	public static final String ACTION_ZONA_ASIGNADA = "net.ifeu.edicards.ZONA_ASIGNADA";

	// Constantes para colorear items según antigüedad de visita
	private static final int DIAS_VISITA_MUY_RECIENTE = 15;  // Verde intenso
	private static final int DIAS_VISITA_RECIENTE = 30;      // Verde claro
	private static final int DIAS_VISITA_MEDIA = 60;         // Amarillo (2 meses)
	private static final int DIAS_VISITA_ANTIGUA = 90;       // Naranja (3 meses)
	// Más de 90 días = Rojo

	private AppConfig app;
	private ListView listViewRutas;
	private List<RutaGenerada> rutaActual;  // Lista original completa
	private List<RutaGenerada> rutaFiltrada;  // Lista filtrada por búsqueda
	private RutaAdapter adapter;
	private Button btnRegenerarRuta;
	private Button btnLimpiarCoordenadas;
	private Button btnExportarExcel;
	private Button btnGestionarZonas;
	private Button btnEstadoClientes;
	private TextView lblFechaGeneracion;
	private TextView lblTotalClientes;
	private android.widget.Spinner spinnerOrdenacion;
	private android.widget.EditText txtBuscador;
	private Button btnLimpiarBusqueda;
	private Button btnFiltroFavoritos;
	private boolean mostrarSoloFavoritos = false;

	// Google Maps
	private Button btnToggleMapa;
	private View mapContainer;
	private GoogleMap googleMap;
	private boolean mostrandoMapa = false;

	// Toggle controles
	private Button btnToggleControles;
	private View contenedorControles;
	private boolean controlesVisibles = true;

	private BroadcastReceiver zonaAsignadaReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Refrescar la lista de rutas cuando se asigna una zona
			// Asegurarse de ejecutar en el UI thread y que la Activity esté disponible
			if (getActivity() != null && isAdded()) {
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						cargarRutaActual(false);
					}
				});
			}
		}
	};

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
		btnEstadoClientes = (Button) getActivity().findViewById(R.id.btnEstadoClientes);
		lblFechaGeneracion = (TextView) getActivity().findViewById(R.id.lblFechaGeneracion);
		lblTotalClientes = (TextView) getActivity().findViewById(R.id.lblTotalClientes);
		spinnerOrdenacion = (android.widget.Spinner) getActivity().findViewById(R.id.spinnerOrdenacion);
		txtBuscador = (android.widget.EditText) getActivity().findViewById(R.id.txtBuscador);
		btnLimpiarBusqueda = (Button) getActivity().findViewById(R.id.btnLimpiarBusqueda);
		btnFiltroFavoritos = (Button) getActivity().findViewById(R.id.btnFiltroFavoritos);
		btnToggleControles = (Button) getActivity().findViewById(R.id.btnToggleControles);
		contenedorControles = getActivity().findViewById(R.id.contenedorControles);

		// Configurar buscador
		if (txtBuscador != null) {
			txtBuscador.addTextChangedListener(new android.text.TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					// No hacer nada
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					filtrarRutas(s.toString());
				}

				@Override
				public void afterTextChanged(android.text.Editable s) {
					// No hacer nada
				}
			});
		}

		// Botón limpiar búsqueda
		if (btnLimpiarBusqueda != null) {
			btnLimpiarBusqueda.setOnClickListener(view -> {
				if (txtBuscador != null) {
					txtBuscador.setText("");
				}
			});
		}

		// Botón filtro favoritos
		if (btnFiltroFavoritos != null) {
			btnFiltroFavoritos.setOnClickListener(view -> {
				mostrarSoloFavoritos = !mostrarSoloFavoritos;

				// Actualizar icono del botón
				if (mostrarSoloFavoritos) {
					btnFiltroFavoritos.setText("★");
					btnFiltroFavoritos.setTextColor(0xFFFFC107); // Amarillo/dorado
					btnFiltroFavoritos.setBackgroundColor(0xFFFFF8E1); // Fondo amarillo claro
				} else {
					btnFiltroFavoritos.setText("☆");
					btnFiltroFavoritos.setTextColor(0xFF9E9E9E); // Gris
					btnFiltroFavoritos.setBackgroundColor(0xFFFFFFFF); // Fondo blanco
				}

				// Aplicar filtro manteniendo el texto de búsqueda actual
				filtrarRutas(txtBuscador != null ? txtBuscador.getText().toString() : "");
			});
		}

		// Botón toggle controles (ocultar/mostrar)
		if (btnToggleControles != null) {
			btnToggleControles.setOnClickListener(view -> {
				controlesVisibles = !controlesVisibles;

				if (controlesVisibles) {
					// Mostrar controles
					contenedorControles.setVisibility(View.VISIBLE);
					btnToggleControles.setText("▼ Ocultar controles");
				} else {
					// Ocultar controles
					contenedorControles.setVisibility(View.GONE);
					btnToggleControles.setText("▲ Mostrar controles");
				}
			});
		}

		// Configurar Spinner de ordenación
		if (spinnerOrdenacion != null) {
			String[] opcionesOrdenacion = {
				"Orden de ruta",
				"Nombre (A-Z)",
				"Nombre (Z-A)",
				"Población (A-Z)",
				"Población (Z-A)",
				"Provincia (A-Z)",
				"Provincia (Z-A)",
				"Última visita (reciente primero)",
				"Última visita (antigua primero)"
			};

			android.widget.ArrayAdapter<String> adapterSpinner = new android.widget.ArrayAdapter<>(
				getActivity(),
				android.R.layout.simple_spinner_item,
				opcionesOrdenacion
			);
			adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinnerOrdenacion.setAdapter(adapterSpinner);

			spinnerOrdenacion.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
					ordenarRutaPor(position);
				}

				@Override
				public void onNothingSelected(android.widget.AdapterView<?> parent) {
					// No hacer nada
				}
			});
		}

		// Header clickable para refrescar
		LinearLayout headerRuta = (LinearLayout) getActivity().findViewById(R.id.headerRuta);
		if (headerRuta != null) {
			headerRuta.setOnClickListener(view -> {
				// Refrescar la lista
				cargarRutaActual(false);
				android.widget.Toast.makeText(getActivity(), "Lista actualizada", android.widget.Toast.LENGTH_SHORT).show();
			});
		}

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

		// Botón estado clientes
		if (btnEstadoClientes != null) {
			btnEstadoClientes.setOnClickListener(view -> {
				abrirEstadoClientes();
			});
		}

		// Inicializar mapa
		mapContainer = getActivity().findViewById(R.id.mapContainer);
		btnToggleMapa = (Button) getActivity().findViewById(R.id.btnToggleMapa);

		if (btnToggleMapa != null) {
			btnToggleMapa.setOnClickListener(view -> {
				toggleMapa();
			});
		}

		// Obtener el fragmento del mapa
		SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
		if (mapFragment != null) {
			mapFragment.getMapAsync(new OnMapReadyCallback() {
				@Override
				public void onMapReady(GoogleMap map) {
					googleMap = map;
					// Configurar el mapa
					googleMap.getUiSettings().setZoomControlsEnabled(true);
					googleMap.getUiSettings().setZoomGesturesEnabled(true);
					googleMap.getUiSettings().setScrollGesturesEnabled(true);
					// Si ya hay una ruta cargada, dibujarla
					if (rutaActual != null && !rutaActual.isEmpty()) {
						dibujarRutaEnMapa();
					}
				}
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

			android.util.Log.d("RutasViewerFragment", "rutaActual cargada con " + (rutaActual != null ? rutaActual.size() : 0) + " clientes");

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
			try {
				Cliente clienteModel = Factory.build(Cliente.class, app);
				for (RutaGenerada r : rutaActual) {
					Long idZona = clienteModel.getIdZona(r.CodigoCliente);
					if (idZona != null) {
						zonasUnicas.add(idZona);
					}
				}
			} catch (Exception e) {
				android.util.Log.w("RutasViewerFragment", "Error contando zonas: " + e.getMessage());
			}

			// Mostrar cantidad de zonas si hay más de 1
			if (zonasUnicas.size() > 1) {
				lblTotalClientes.setText("Total clientes: " + rutaActual.size() + " (" + zonasUnicas.size() + " zonas)");
			}

			// Verificar si hay clientes sin geocodificar
			verificarClientesSinGeocoding();

			// Inicializar lista filtrada con todos los clientes
			rutaFiltrada = new ArrayList<>(rutaActual);

			// Adapter personalizado
			adapter = new RutaAdapter(getActivity(), rutaFiltrada);
			// Limpiar adaptador anterior
		if (listViewRutas.getAdapter() != null) {
			listViewRutas.setAdapter(null);
		}
		listViewRutas.setAdapter(adapter);

		} catch (Exception e) {
			app.getMessageBox().Show("Error",
				"Error cargando ruta: " + e.getMessage(),
				getActivity(), MessageBoxType.Error);
		}
	}

	private void verificarClientesSinGeocoding() {
		try {
			Cliente cliente = Factory.build(Cliente.class, app);
			int clientesSinGeocoding = cliente.contarClientesSinGeocoding();

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

		// Crear copia ordenada por OrdenVisita para exportar
		ArrayList<RutaGenerada> rutaParaExportar = new ArrayList<>(rutaActual);
		java.util.Collections.sort(rutaParaExportar, new java.util.Comparator<RutaGenerada>() {
			@Override
			public int compare(RutaGenerada o1, RutaGenerada o2) {
				return Integer.compare(o1.OrdenVisita, o2.OrdenVisita);
			}
		});

		// Mostrar progress dialog
		final ProgressDialog progressDialog = new ProgressDialog(getActivity());
		progressDialog.setMessage("Exportando ruta a Excel...");
		progressDialog.setCancelable(false);
		progressDialog.show();

		// Ejecutar en background
		new Thread(() -> {
			try {
				ExportToExcelService exportService = new ExportToExcelService(getActivity());
				String rutaArchivo = exportService.exportRutasToExcel(rutaParaExportar);

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
	/**
	 * Clase auxiliar para almacenar información de última visita
	 */
	private static class InfoUltimaVisita {
		int dias;
		java.util.Date fecha;

		InfoUltimaVisita(int dias, java.util.Date fecha) {
			this.dias = dias;
			this.fecha = fecha;
		}
	}

	/**
	 * Obtiene información completa de la última visita al cliente
	 * Busca la fecha más reciente entre Depositos y Albaranes
	 * @param codigoCliente Código del cliente
	 * @return InfoUltimaVisita con días y fecha, o null si nunca se ha visitado
	 */
	private InfoUltimaVisita getInfoUltimaVisita(String codigoCliente) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

			// Buscar última fecha en Depositos y Albaranes usando el modelo
			Cliente clienteModel = Factory.build(Cliente.class, app);

			Long ultimoDepositoMillis = null;
			String fechaDepositoStr = clienteModel.getUltimaFechaDeposito(codigoCliente);
			if (fechaDepositoStr != null && !fechaDepositoStr.isEmpty()) {
				try {
					java.util.Date fecha = formatter.parse(fechaDepositoStr);
					ultimoDepositoMillis = fecha.getTime();
					android.util.Log.d("RutasViewerFragment", "FechaDeposito para " + codigoCliente + ": " + fecha);
				} catch (Exception e) {
					android.util.Log.w("RutasViewerFragment", "Error parseando FechaDeposito: " + fechaDepositoStr);
				}
			}

			Long ultimoAlbaranMillis = null;
			String fechaAlbaranStr = clienteModel.getUltimaFechaAlbaran(codigoCliente);
			if (fechaAlbaranStr != null && !fechaAlbaranStr.isEmpty()) {
				try {
					java.util.Date fecha = formatter.parse(fechaAlbaranStr);
					ultimoAlbaranMillis = fecha.getTime();
				} catch (Exception e) {
					android.util.Log.w("RutasViewerFragment", "Error parseando FechaAlbaran: " + fechaAlbaranStr);
				}
			}

			// Determinar la fecha más reciente
			Long ultimaVisitaMillis = null;
			if (ultimoDepositoMillis != null && ultimoAlbaranMillis != null) {
				ultimaVisitaMillis = Math.max(ultimoDepositoMillis, ultimoAlbaranMillis);
			} else if (ultimoDepositoMillis != null) {
				ultimaVisitaMillis = ultimoDepositoMillis;
			} else if (ultimoAlbaranMillis != null) {
				ultimaVisitaMillis = ultimoAlbaranMillis;
			}

			// Si nunca se ha visitado
			if (ultimaVisitaMillis == null) {
				return null;
			}

			// Calcular días transcurridos
			long ahora = System.currentTimeMillis();
			long diferencia = ahora - ultimaVisitaMillis;
			int dias = (int) (diferencia / (1000 * 60 * 60 * 24));

			return new InfoUltimaVisita(dias, new java.util.Date(ultimaVisitaMillis));

		} catch (Exception e) {
			android.util.Log.e("RutasViewerFragment", "Error calculando última visita: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Obtiene los días transcurridos desde la última visita al cliente
	 * Busca la fecha más reciente entre Depositos y Albaranes
	 * @param codigoCliente Código del cliente
	 * @return Días desde última visita, o -1 si nunca se ha visitado
	 */
	private int getDiasDesdeUltimaVisita(String codigoCliente) {
		InfoUltimaVisita info = getInfoUltimaVisita(codigoCliente);
		return info != null ? info.dias : -1;
	}

	/**
	 * Obtiene el color de fondo para un item según días desde última visita
	 * @param diasDesdeUltimaVisita Días transcurridos, o -1 si nunca visitado
	 * @return Color en formato ARGB
	 */
	private int getColorPorAntiguedadVisita(int diasDesdeUltimaVisita) {
		if (diasDesdeUltimaVisita < 0) {
			// Nunca visitado - Rojo intenso con transparencia
			return 0xFFFF4444;
		} else if (diasDesdeUltimaVisita <= DIAS_VISITA_MUY_RECIENTE) {
			// Menos de 15 días - Verde intenso
			return 0xFF4CAF50;
		} else if (diasDesdeUltimaVisita <= DIAS_VISITA_RECIENTE) {
			// 15-30 días - Verde claro
			return 0xFF81C784;
		} else if (diasDesdeUltimaVisita <= DIAS_VISITA_MEDIA) {
			// 30-60 días - Amarillo
			return 0xFFFFEB3B;
		} else if (diasDesdeUltimaVisita <= DIAS_VISITA_ANTIGUA) {
			// 60-90 días - Naranja
			return 0xFFFF9800;
		} else {
			// Más de 90 días - Rojo
			return 0xFFF44336;
		}
	}

	private class RutaAdapter extends BaseAdapter {
		private Context context;
		private List<RutaGenerada> rutas;

		public RutaAdapter(Context context, List<RutaGenerada> rutas) {
			this.context = context;
			this.rutas = rutas;
		}

		@Override
		public int getCount() {
			int count = rutas != null ? rutas.size() : 0;
			android.util.Log.d("RutaAdapter", "getCount() devuelve: " + count);
			return count;
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
			// IMPORTANTE: No cachear vistas para que siempre se recalcule el color
			// según la última visita al cliente
			LayoutInflater inflater = LayoutInflater.from(context);
			convertView = inflater.inflate(R.layout.item_ruta, parent, false);

			RutaGenerada ruta = rutas.get(position);

			TextView txtOrden = convertView.findViewById(R.id.txtOrden);
			TextView txtNombre = convertView.findViewById(R.id.txtNombre);
			TextView txtDireccion = convertView.findViewById(R.id.txtDireccion);
			TextView txtPoblacion = convertView.findViewById(R.id.txtPoblacion);
			TextView txtDistancia = convertView.findViewById(R.id.txtDistancia);
			TextView txtZona = convertView.findViewById(R.id.txtZona);
			TextView txtClienteNuevo = convertView.findViewById(R.id.txtClienteNuevo);
			TextView txtUltimaVisita = convertView.findViewById(R.id.txtUltimaVisita);
			TextView txtCoordenadas = convertView.findViewById(R.id.txtCoordenadas);
			android.widget.Button btnMoverArriba = convertView.findViewById(R.id.btnMoverArriba);
			android.widget.Button btnMoverAbajo = convertView.findViewById(R.id.btnMoverAbajo);
			android.widget.Button btnFavorito = convertView.findViewById(R.id.btnFavorito);
			android.widget.Button btnVerFicha = convertView.findViewById(R.id.btnVerFicha);

			txtOrden.setText(String.valueOf(ruta.OrdenVisita));

			// Configurar botones de reordenación
			final int currentPosition = position;
			boolean esClienteBase = (position == 0); // El primer cliente es la BASE

			// Botón mover arriba - deshabilitado para BASE y para el primero
			if (esClienteBase || position == 0) {
				btnMoverArriba.setEnabled(false);
				btnMoverArriba.setAlpha(0.3f);
			} else {
				btnMoverArriba.setEnabled(true);
				btnMoverArriba.setAlpha(1.0f);
				btnMoverArriba.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						moverRutaArriba(currentPosition);
					}
				});
			}

			// Botón mover abajo - deshabilitado para BASE y para el último
			if (esClienteBase || position == rutas.size() - 1) {
				btnMoverAbajo.setEnabled(false);
				btnMoverAbajo.setAlpha(0.3f);
			} else {
				btnMoverAbajo.setEnabled(true);
				btnMoverAbajo.setAlpha(1.0f);
				btnMoverAbajo.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						moverRutaAbajo(currentPosition);
					}
				});
			}
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

			// Obtener datos de ubicación del cliente desde el modelo
			try {
				Cliente clienteModel = Factory.build(Cliente.class, app);
				Cliente.DatosUbicacion ubicacion = clienteModel.getDatosUbicacion(ruta.CodigoCliente);

				if (ubicacion != null) {
					// Mostrar coordenadas en formato compacto si existen
					if (ubicacion.latitud != null && ubicacion.longitud != null &&
						ubicacion.latitud != 0 && ubicacion.longitud != 0) {
						txtCoordenadas.setText(String.format("📍 %.6f, %.6f", ubicacion.latitud, ubicacion.longitud));
						txtCoordenadas.setVisibility(android.view.View.VISIBLE);
					} else {
						txtCoordenadas.setVisibility(android.view.View.GONE);
					}

					// Mostrar zona si existe
					if (ubicacion.idZona != null && ubicacion.idZona > 0) {
						try {
							net.ifeu.edicards.DataTier.Zona zona = Factory.build(
								net.ifeu.edicards.DataTier.Zona.class, app);
							if (zona.setZonaById(ubicacion.idZona)) {
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
					txtCoordenadas.setVisibility(android.view.View.GONE);
					txtZona.setVisibility(android.view.View.GONE);
				}
			} catch (Exception e) {
				android.util.Log.w("RutasViewerFragment", "Error obteniendo datos ubicación: " + e.getMessage());
				txtCoordenadas.setVisibility(android.view.View.GONE);
				txtZona.setVisibility(android.view.View.GONE);
			}

			// Mostrar indicador de cliente nuevo si corresponde
			if (ruta.EsClienteNuevo) {
				txtClienteNuevo.setVisibility(android.view.View.VISIBLE);
				android.util.Log.d("RutaAdapter", "Cliente NUEVO mostrado: " + ruta.NombreCliente);
			} else {
				txtClienteNuevo.setVisibility(android.view.View.GONE);
			}

			// Configurar botón de favorito
			try {
				Cliente clienteModel = Factory.build(Cliente.class, app);
				final boolean favoritoActual = clienteModel.esFavorito(ruta.CodigoCliente);

				// Actualizar icono según el estado
				if (favoritoActual) {
					btnFavorito.setText("★"); // Estrella llena
					btnFavorito.setTextColor(0xFFFFC107); // Amarillo/dorado
				} else {
					btnFavorito.setText("☆"); // Estrella vacía
					btnFavorito.setTextColor(0xFF9E9E9E); // Gris
				}

				// Click para toggle favorito
				btnFavorito.setOnClickListener(new android.view.View.OnClickListener() {
					@Override
					public void onClick(android.view.View v) {
						toggleFavorito(ruta.CodigoCliente, !favoritoActual);
					}
				});

			} catch (Exception e) {
				android.util.Log.w("RutasViewerFragment", "Error leyendo estado favorito: " + e.getMessage());
				btnFavorito.setVisibility(android.view.View.GONE);
			}

			// Si es el cliente BASE (posición 0), no aplicar colores ni mostrar info de visita
			if (esClienteBase) {
				// Cliente BASE: fondo blanco, sin información de visitas
				convertView.setBackgroundColor(0xFFFFFFFF); // Blanco
				txtUltimaVisita.setText("🏠 PUNTO BASE");
				txtUltimaVisita.setBackgroundColor(0xFF9E9E9E); // Gris neutro
				txtUltimaVisita.setVisibility(android.view.View.VISIBLE);
			} else {
				// Clientes normales: calcular y aplicar color según antigüedad de visita
				InfoUltimaVisita infoVisita = getInfoUltimaVisita(ruta.CodigoCliente);
				int diasDesdeVisita = infoVisita != null ? infoVisita.dias : -1;
				int colorFondo = getColorPorAntiguedadVisita(diasDesdeVisita);

				// Aplicar color con transparencia para que el texto siga siendo legible
				convertView.setBackgroundColor((colorFondo & 0x00FFFFFF) | 0x30000000);  // 30% de opacidad

				// Configurar etiqueta de última visita
				if (infoVisita != null) {
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
					String fechaStr = sdf.format(infoVisita.fecha);
					String textoVisita;

					// MOSTRAR SIEMPRE LA FECHA para poder verificar
					if (diasDesdeVisita == 0) {
						textoVisita = "Última visita: Hoy (" + fechaStr + ")";
					} else if (diasDesdeVisita == 1) {
						textoVisita = "Última visita: Ayer (" + fechaStr + ")";
					} else {
						textoVisita = "Última visita: hace " + diasDesdeVisita + " días (" + fechaStr + ")";
					}

					// DEBUG: Log para verificar las fechas
					android.util.Log.d("RutasViewerFragment",
						"Cliente: " + ruta.NombreCliente +
						" | Código: " + ruta.CodigoCliente +
						" | Última visita: " + fechaStr +
						" | Días: " + diasDesdeVisita);

					txtUltimaVisita.setText(textoVisita);
					txtUltimaVisita.setBackgroundColor(colorFondo); // Mismo color que el fondo
					txtUltimaVisita.setVisibility(android.view.View.VISIBLE);
				} else {
					// Nunca visitado
					txtUltimaVisita.setText("⚠️ Cliente nunca visitado");
					txtUltimaVisita.setBackgroundColor(0xFFFF4444); // Rojo intenso
					txtUltimaVisita.setVisibility(android.view.View.VISIBLE);
				}
			}

			// Configurar botón Ver Ficha
			btnVerFicha.setOnClickListener(new android.view.View.OnClickListener() {
				@Override
				public void onClick(android.view.View v) {
					Intent intent = new Intent(getActivity(), FichaClienteDialog.class);
					intent.putExtra("codigoCliente", ruta.CodigoCliente);
					startActivity(intent);
				}
			});

			return convertView;
		}
	}

	/**
	 * Mueve una ruta una posición hacia arriba en el orden
	 */
	private void moverRutaArriba(int position) {
		if (position <= 0 || rutaActual == null) return;

		try {
			RutaGenerada ruta = rutaActual.get(position);
			RutaGenerada rutaAnterior = rutaActual.get(position - 1);

			// Intercambiar los valores de OrdenVisita
			int ordenTemp = ruta.OrdenVisita;
			ruta.OrdenVisita = rutaAnterior.OrdenVisita;
			rutaAnterior.OrdenVisita = ordenTemp;

			// Actualizar en la base de datos
			ruta.update();
			rutaAnterior.update();

			// Intercambiar en la lista local
			rutaActual.set(position, rutaAnterior);
			rutaActual.set(position - 1, ruta);

			// Notificar al adapter
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}

			// Actualizar el mapa si está visible
			if (mostrandoMapa && googleMap != null) {
				dibujarRutaEnMapa();
			}

			android.util.Log.i("RutasViewerFragment", "Ruta movida arriba: " + ruta.NombreCliente +
				" de posición " + ordenTemp + " a " + ruta.OrdenVisita);

		} catch (Exception e) {
			android.util.Log.e("RutasViewerFragment", "Error moviendo ruta arriba: " + e.getMessage());
			android.widget.Toast.makeText(getActivity(),
				"Error al reordenar: " + e.getMessage(),
				android.widget.Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Mueve una ruta una posición hacia abajo en el orden
	 */
	private void moverRutaAbajo(int position) {
		if (rutaActual == null || position >= rutaActual.size() - 1) return;

		try {
			RutaGenerada ruta = rutaActual.get(position);
			RutaGenerada rutaSiguiente = rutaActual.get(position + 1);

			// Intercambiar los valores de OrdenVisita
			int ordenTemp = ruta.OrdenVisita;
			ruta.OrdenVisita = rutaSiguiente.OrdenVisita;
			rutaSiguiente.OrdenVisita = ordenTemp;

			// Actualizar en la base de datos
			ruta.update();
			rutaSiguiente.update();

			// Intercambiar en la lista local
			rutaActual.set(position, rutaSiguiente);
			rutaActual.set(position + 1, ruta);

			// Notificar al adapter
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}

			// Actualizar el mapa si está visible
			if (mostrandoMapa && googleMap != null) {
				dibujarRutaEnMapa();
			}

			android.util.Log.i("RutasViewerFragment", "Ruta movida abajo: " + ruta.NombreCliente +
				" de posición " + ordenTemp + " a " + ruta.OrdenVisita);

		} catch (Exception e) {
			android.util.Log.e("RutasViewerFragment", "Error moviendo ruta abajo: " + e.getMessage());
			android.widget.Toast.makeText(getActivity(),
				"Error al reordenar: " + e.getMessage(),
				android.widget.Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Cambia el estado de favorito de un cliente
	 */
	private void toggleFavorito(String codigoCliente, boolean nuevoEstado) {
		try {
			Cliente cliente = Factory.build(Cliente.class, app);
			cliente.setEsFavorito(codigoCliente, nuevoEstado);

			// Actualizar la vista
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}

			String mensaje = nuevoEstado ? "Cliente marcado como favorito" : "Cliente desmarcado como favorito";
			android.widget.Toast.makeText(getActivity(), mensaje, android.widget.Toast.LENGTH_SHORT).show();

			android.util.Log.i("RutasViewerFragment",
				"Cliente " + codigoCliente + " - Favorito: " + nuevoEstado);

		} catch (Exception e) {
			android.util.Log.e("RutasViewerFragment", "Error actualizando favorito: " + e.getMessage());
			android.widget.Toast.makeText(getActivity(),
				"Error al actualizar favorito",
				android.widget.Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Filtra la lista de rutas según el texto de búsqueda y el filtro de favoritos
	 * Busca en nombre, dirección, población y provincia
	 */
	private void filtrarRutas(String textoBusqueda) {
		if (rutaActual == null) return;

		// Limpiar lista filtrada
		if (rutaFiltrada == null) {
			rutaFiltrada = new ArrayList<>();
		} else {
			rutaFiltrada.clear();
		}

		String busqueda = (textoBusqueda != null) ? textoBusqueda.toLowerCase().trim() : "";
		boolean hayTextoBusqueda = !busqueda.isEmpty();

		// Crear instancia de Cliente para consultas
		Cliente clienteModel = null;
		try {
			clienteModel = Factory.build(Cliente.class, app);
		} catch (Exception e) {
			android.util.Log.e("RutasViewerFragment", "Error creando modelo Cliente: " + e.getMessage());
			return;
		}

		// Filtrar rutas
		for (RutaGenerada ruta : rutaActual) {
			boolean pasaFiltroFavoritos = true;
			boolean pasaFiltroTexto = true;

			// Aplicar filtro de favoritos si está activo
			if (mostrarSoloFavoritos) {
				pasaFiltroFavoritos = clienteModel.esFavorito(ruta.CodigoCliente);
			}

			// Aplicar filtro de texto si hay búsqueda
			if (hayTextoBusqueda) {
				pasaFiltroTexto =
					(ruta.NombreCliente != null && ruta.NombreCliente.toLowerCase().contains(busqueda)) ||
					(ruta.DireccionCliente != null && ruta.DireccionCliente.toLowerCase().contains(busqueda)) ||
					(ruta.PoblacionCliente != null && ruta.PoblacionCliente.toLowerCase().contains(busqueda)) ||
					(ruta.ProvinciaCliente != null && ruta.ProvinciaCliente.toLowerCase().contains(busqueda)) ||
					(ruta.CodigoCliente != null && ruta.CodigoCliente.toLowerCase().contains(busqueda));
			}

			// Añadir a lista filtrada si pasa ambos filtros
			if (pasaFiltroFavoritos && pasaFiltroTexto) {
				rutaFiltrada.add(ruta);
			}
		}

		// Actualizar contador
		lblTotalClientes.setText("Mostrando: " + rutaFiltrada.size() + " de " + rutaActual.size() + " clientes");

		// Recrear adapter para forzar actualización
		adapter = new RutaAdapter(getActivity(), rutaFiltrada);
		listViewRutas.setAdapter(adapter);

		// Actualizar mapa si está visible
		if (mostrandoMapa && googleMap != null) {
			dibujarRutaEnMapa();
		}
	}

	/**
	 * Ordena la ruta actual según el criterio seleccionado
	 * @param criterio Índice del criterio (0=Orden ruta, 1=Nombre A-Z, 2=Nombre Z-A, etc.)
	 */
	private void ordenarRutaPor(int criterio) {
		if (rutaActual == null || rutaActual.isEmpty()) return;

		try {
			java.util.Collections.sort(rutaActual, (r1, r2) -> {
				switch (criterio) {
					case 0: // Orden de ruta (por OrdenVisita)
						return Integer.compare(r1.OrdenVisita, r2.OrdenVisita);

					case 1: // Nombre (A-Z)
						String nombre1 = r1.NombreCliente != null ? r1.NombreCliente : "";
						String nombre2 = r2.NombreCliente != null ? r2.NombreCliente : "";
						return nombre1.compareToIgnoreCase(nombre2);

					case 2: // Nombre (Z-A)
						String nombreZ1 = r1.NombreCliente != null ? r1.NombreCliente : "";
						String nombreZ2 = r2.NombreCliente != null ? r2.NombreCliente : "";
						return nombreZ2.compareToIgnoreCase(nombreZ1);

					case 3: // Población (A-Z)
						String pob1 = r1.PoblacionCliente != null ? r1.PoblacionCliente : "";
						String pob2 = r2.PoblacionCliente != null ? r2.PoblacionCliente : "";
						return pob1.compareToIgnoreCase(pob2);

					case 4: // Población (Z-A)
						String pobZ1 = r1.PoblacionCliente != null ? r1.PoblacionCliente : "";
						String pobZ2 = r2.PoblacionCliente != null ? r2.PoblacionCliente : "";
						return pobZ2.compareToIgnoreCase(pobZ1);

					case 5: // Provincia (A-Z)
						String prov1 = r1.ProvinciaCliente != null ? r1.ProvinciaCliente : "";
						String prov2 = r2.ProvinciaCliente != null ? r2.ProvinciaCliente : "";
						return prov1.compareToIgnoreCase(prov2);

					case 6: // Provincia (Z-A)
						String provZ1 = r1.ProvinciaCliente != null ? r1.ProvinciaCliente : "";
						String provZ2 = r2.ProvinciaCliente != null ? r2.ProvinciaCliente : "";
						return provZ2.compareToIgnoreCase(provZ1);

					case 7: // Última visita (reciente primero)
						InfoUltimaVisita info1 = getInfoUltimaVisita(r1.CodigoCliente);
						InfoUltimaVisita info2 = getInfoUltimaVisita(r2.CodigoCliente);
						int dias1 = info1 != null ? info1.dias : Integer.MAX_VALUE;
						int dias2 = info2 != null ? info2.dias : Integer.MAX_VALUE;
						return Integer.compare(dias1, dias2); // Menos días = más reciente

					case 8: // Última visita (antigua primero)
						InfoUltimaVisita infoA1 = getInfoUltimaVisita(r1.CodigoCliente);
						InfoUltimaVisita infoA2 = getInfoUltimaVisita(r2.CodigoCliente);
						int diasA1 = infoA1 != null ? infoA1.dias : Integer.MAX_VALUE;
						int diasA2 = infoA2 != null ? infoA2.dias : Integer.MAX_VALUE;
						return Integer.compare(diasA2, diasA1); // Más días = más antigua

					default:
						return 0;
				}
			});

			// Aplicar el filtro actual si existe
			if (txtBuscador != null && txtBuscador.getText().toString().trim().length() > 0) {
				filtrarRutas(txtBuscador.getText().toString());
			} else {
				rutaFiltrada = new ArrayList<>(rutaActual);
			}

			// Notificar al adapter que los datos han cambiado
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}

			// Actualizar el mapa si está visible
			if (mostrandoMapa && googleMap != null) {
				dibujarRutaEnMapa();
			}

			android.util.Log.i("RutasViewerFragment", "Lista ordenada por criterio: " + criterio);

		} catch (Exception e) {
			android.util.Log.e("RutasViewerFragment", "Error ordenando lista: " + e.getMessage());
		}
	}

	/**
	 * Muestra un diálogo con el estado de geocodificación de los clientes
	 */
	private void abrirEstadoClientes() {
		try {
			Intent intent = new Intent(getActivity(), EstadoClientesDialog.class);
			startActivity(intent);
		} catch (Exception e) {
			app.getMessageBox().Show("Error",
				"Error al abrir estado de clientes: " + e.getMessage(),
				getActivity(), MessageBoxType.Error);
			android.util.Log.e("RutasViewerFragment", "Error en abrirEstadoClientes", e);
		}
	}

	/**
	 * Alternar entre vista de lista y vista de mapa
	 */
	private void toggleMapa() {
		if (mostrandoMapa) {
			// Mostrar lista, ocultar mapa
			listViewRutas.setVisibility(View.VISIBLE);
			mapContainer.setVisibility(View.GONE);
			btnToggleMapa.setText("📍 Ver Mapa");
			mostrandoMapa = false;
		} else {
			// Mostrar mapa, ocultar lista
			listViewRutas.setVisibility(View.GONE);
			mapContainer.setVisibility(View.VISIBLE);
			btnToggleMapa.setText("📋 Ver Lista");
			mostrandoMapa = true;
			// Dibujar la ruta si hay datos y el mapa está listo
			if (googleMap != null && rutaActual != null && !rutaActual.isEmpty()) {
				dibujarRutaEnMapa();
			}
		}
	}

	/**
	 * Dibujar la ruta en el mapa de Google Maps
	 * Usa rutaFiltrada para mostrar solo los clientes visibles actualmente
	 */
	private void dibujarRutaEnMapa() {
		if (googleMap == null || rutaFiltrada == null || rutaFiltrada.isEmpty()) {
			return;
		}

		// Limpiar mapa
		googleMap.clear();

		ArrayList<LatLng> rutaPuntos = new ArrayList<>();
		LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

		// Añadir markers de clientes y construir polyline
		for (int i = 0; i < rutaFiltrada.size(); i++) {
			RutaGenerada ruta = rutaFiltrada.get(i);

			// Intentar obtener coordenadas de la ruta o del cliente
			Double lat = null;
			Double lon = null;

			// Primero intentar desde RutaGenerada
			if (ruta.Latitud != null && !ruta.Latitud.isEmpty() && !ruta.Latitud.equals("NULL")) {
				try {
					lat = Double.parseDouble(ruta.Latitud);
				} catch (NumberFormatException e) {
					// Ignorar
				}
			}

			if (ruta.Longitud != null && !ruta.Longitud.isEmpty() && !ruta.Longitud.equals("NULL")) {
				try {
					lon = Double.parseDouble(ruta.Longitud);
				} catch (NumberFormatException e) {
					// Ignorar
				}
			}

			// Si no hay coordenadas en RutaGenerada, obtenerlas de la tabla Clientes
			if (lat == null || lon == null) {
				try {
					Cliente clienteModel = Factory.build(Cliente.class, app);
					Double[] coords = clienteModel.getCoordenadas(ruta.CodigoCliente);
					if (coords != null) {
						lat = coords[0];
						lon = coords[1];
					}
				} catch (Exception e) {
					// Ignorar errores
				}
			}

			// Si tenemos coordenadas válidas, añadir marker y punto
			if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
				LatLng posicion = new LatLng(lat, lon);
				rutaPuntos.add(posicion);
				boundsBuilder.include(posicion);

				// Color del marker según orden
				float hue = BitmapDescriptorFactory.HUE_RED;
				if (i == 0) {
					hue = BitmapDescriptorFactory.HUE_GREEN; // Primer cliente en verde
				} else if (i == rutaFiltrada.size() - 1) {
					hue = BitmapDescriptorFactory.HUE_AZURE; // Último cliente en azul celeste (diferente al punto base)
				}

				// Añadir marker con número de orden
				googleMap.addMarker(new MarkerOptions()
					.position(posicion)
					.title("#" + ruta.OrdenVisita + " - " + ruta.NombreCliente)
					.snippet(ruta.DireccionCliente + ", " + ruta.PoblacionCliente)
					.icon(BitmapDescriptorFactory.defaultMarker(hue)));
			} else {
				// Log de depuración para puntos sin coordenadas
				android.util.Log.w("RutasViewerFragment",
					"⚠ Cliente #" + ruta.OrdenVisita + " (" + ruta.CodigoCliente + " - " + ruta.NombreCliente +
					") SIN COORDENADAS VÁLIDAS - lat=" + lat + ", lon=" + lon);
			}
		}

		// Dibujar polyline conectando los puntos
		if (rutaPuntos.size() > 1) {
			googleMap.addPolyline(new PolylineOptions()
				.addAll(rutaPuntos)
				.width(5f)
				.color(0xFF1976D2) // Azul
				.geodesic(true));

			// Ajustar zoom para mostrar toda la ruta
			try {
				LatLngBounds bounds = boundsBuilder.build();
				int padding = 100; // padding en pixels
				googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
			} catch (Exception e) {
				// Si falla, centrar en el primer punto
				if (!rutaPuntos.isEmpty()) {
					googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(rutaPuntos.get(0), 12f));
				}
			}
		} else if (rutaPuntos.size() == 1) {
			// Si solo hay un punto, centrarlo
			googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(rutaPuntos.get(0), 15f));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// Registrar el receiver para escuchar cuando se asigna una zona
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
			zonaAsignadaReceiver,
			new IntentFilter(ACTION_ZONA_ASIGNADA)
		);

		// Recargar la ruta al resumir (cuando vuelves del popup de zonas o asignación)
		cargarRutaActual(false);
	}

	@Override
	public void onPause() {
		super.onPause();
		// Desregistrar el receiver
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(zonaAsignadaReceiver);
	}
}
