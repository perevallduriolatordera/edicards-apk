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
import android.widget.ListView;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.CiudadVendedor;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;
import net.ifeu.edicards.Services.RouteGeneratorService;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;

import java.text.SimpleDateFormat;
import java.util.List;

public class RutasViewerFragment extends Fragment {

	private AppConfig app;
	private ListView listViewRutas;
	private List<RutaGenerada> rutaActual;
	private Button btnRegenerarRuta;
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
		lblFechaGeneracion = (TextView) getActivity().findViewById(R.id.lblFechaGeneracion);
		lblTotalClientes = (TextView) getActivity().findViewById(R.id.lblTotalClientes);

		// Cargar ruta
		cargarRutaActual();

		// Botón regenerar con password
		btnRegenerarRuta.setOnClickListener(view -> {
			solicitarPasswordYRegenerar();
		});
	}

	private void cargarRutaActual() {
		try {
			RutaGenerada ruta = Factory.build(RutaGenerada.class, app);
			rutaActual = ruta.getRutaCurrentWeek();

			if (rutaActual == null || rutaActual.isEmpty()) {
				app.getMessageBox().Show("Rutas",
					"No hay ruta generada para esta semana",
					getActivity(), MessageBoxType.Information);
				return;
			}

			// Actualizar UI
			SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
			lblFechaGeneracion.setText("Generada: " + formatter.format(rutaActual.get(0).FechaGeneracion));
			lblTotalClientes.setText("Total clientes: " + rutaActual.size());

			// Adapter personalizado
			RutaAdapter adapter = new RutaAdapter(getActivity(), rutaActual);
			listViewRutas.setAdapter(adapter);

		} catch (Exception e) {
			app.getMessageBox().Show("Error",
				"Error cargando ruta: " + e.getMessage(),
				getActivity(), MessageBoxType.Error);
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
						cargarRutaActual();
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

			TextView txtOrden = (TextView) convertView.findViewById(R.id.txtOrden);
			TextView txtNombre = (TextView) convertView.findViewById(R.id.txtNombre);
			TextView txtDireccion = (TextView) convertView.findViewById(R.id.txtDireccion);
			TextView txtPoblacion = (TextView) convertView.findViewById(R.id.txtPoblacion);
			TextView txtDistancia = (TextView) convertView.findViewById(R.id.txtDistancia);

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
				txtDistancia.setText(ruta.DistanciaEstimada + " km");
			} else {
				txtDistancia.setText("");
			}

			return convertView;
		}
	}
}
