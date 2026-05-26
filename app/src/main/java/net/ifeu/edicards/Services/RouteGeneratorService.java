package net.ifeu.edicards.Services;

import android.content.Context;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.error.ANError;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsEndpoints;
import net.ifeu.edicards.DataTier.CiudadVendedor;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class RouteGeneratorService {

	private static final String TAG = "RouteGeneratorService";
	private JSONObject apiResponse = null;
	private Exception apiException = null;

	public boolean generateRoute(Context context, String ciudadBase) throws Exception {
		try {
			AppConfig app = (AppConfig) context.getApplicationContext();

			// 1. Obtener clientes activos
			ArrayList<Cliente> clientesActivos = getClientesActivos(app);

			if (clientesActivos == null || clientesActivos.isEmpty()) {
				Log.w(TAG, "No hay clientes activos para generar ruta");
				return false;
			}

			if (clientesActivos.size() < 2) {
				Log.w(TAG, "Mínimo 2 clientes requeridos para generar ruta");
				return false;
			}

			// 2. Obtener código postal del vendedor
			CiudadVendedor ciudad = Factory.build(CiudadVendedor.class, app);
			ciudad.load();
			String codigoPostal = ciudad.CodigoPostal != null ? ciudad.CodigoPostal : "";

			// 3. Construir JSON de clientes
			JSONArray clientesJSON = buildClientesJSON(clientesActivos);

			// 4. Construir prompt para ChatGPT
			String prompt = buildChatGPTPrompt(ciudadBase, codigoPostal, clientesJSON);

			// 5. Llamar a ChatGPT API
			JSONObject response = callChatGPTAPI(prompt);

			if (response == null) {
				Log.e(TAG, "Error: respuesta nula de API");
				return false;
			}

			// 6. Parsear respuesta
			ArrayList<RutaClienteData> rutaOrdenada = parseRouteResponse(response);

			if (rutaOrdenada == null || rutaOrdenada.isEmpty()) {
				Log.e(TAG, "Error: no se pudo parsear la respuesta de ChatGPT");
				return false;
			}

			// 7. Guardar ruta en BD
			saveRoute(app, rutaOrdenada, ciudadBase);

			Log.i(TAG, "Ruta generada exitosamente con " + rutaOrdenada.size() + " clientes");
			return true;

		} catch (Exception e) {
			Log.e(TAG, "Error generando ruta: " + e.getMessage());
			return false;
		}
	}

	private ArrayList<Cliente> getClientesActivos(AppConfig app) throws Exception {
		ArrayList<Cliente> clientesActivos = new ArrayList<>();

		try {
			Cliente cliente = Factory.build(Cliente.class, app);

			// Ejecutar sentencia SQL para obtener clientes activos
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT * FROM Clientes WHERE Activo = 1 ORDER BY Nombre ASC");

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();

				do {
					Cliente c = Factory.build(Cliente.class, app);
					c.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
					c.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
					c.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
					c.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
					c.Direccion1 = cursor.getString(cursor.getColumnIndex("Direccion1"));
					c.Direccion2 = cursor.getString(cursor.getColumnIndex("Direccion2"));
					c.Poblacion = cursor.getString(cursor.getColumnIndex("Poblacion"));
					c.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));
					c.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));

					clientesActivos.add(c);
				} while (cursor.moveToNext());

				cursor.close();
			}

		} catch (Exception e) {
			Log.e(TAG, "Error obteniendo clientes activos: " + e.getMessage());
			throw e;
		}

		return clientesActivos;
	}

	private JSONArray buildClientesJSON(ArrayList<Cliente> clientes) throws Exception {
		JSONArray array = new JSONArray();

		for (Cliente c : clientes) {
			JSONObject obj = new JSONObject();
			obj.put("nif", c.NIF != null ? c.NIF : "");
			obj.put("razon", c.Razon != null ? c.Razon : "");
			obj.put("nombre", c.Nombre != null ? c.Nombre : "");
			obj.put("direccion", c.Direccion1 != null ? c.Direccion1 : "");
			obj.put("poblacion", c.Poblacion != null ? c.Poblacion : "");
			obj.put("provincia", c.Provincia != null ? c.Provincia : "");
			obj.put("codigo_postal", c.CodigoPostal != null ? c.CodigoPostal : "");

			array.put(obj);
		}

		return array;
	}

	private String buildChatGPTPrompt(String ciudadBase, String codigoPostal, JSONArray clientesJSON) {
		StringBuilder prompt = new StringBuilder();

		prompt.append("Eres un experto en optimización de rutas logísticas. Tu objetivo es crear la ruta ");
		prompt.append("MÁS EFICIENTE para un vendedor que debe visitar a todos sus clientes.\n\n");

		prompt.append("Ciudad base del vendedor: ").append(ciudadBase);
		if (codigoPostal != null && !codigoPostal.isEmpty()) {
			prompt.append(" (CP: ").append(codigoPostal).append(")");
		}
		prompt.append("\n\n");

		prompt.append("Lista de clientes a visitar (en formato JSON):\n");
		prompt.append(clientesJSON.toString()).append("\n\n");

		prompt.append("REQUISITOS ESTRICTOS:\n");
		prompt.append("1. La ruta debe ser CIRCULAR: comenzar y terminar en ").append(ciudadBase).append("\n");
		prompt.append("2. Minimizar la distancia total recorrida\n");
		prompt.append("3. Agrupar clientes por proximidad geográfica\n");
		prompt.append("4. Considerar carreteras principales de España\n");
		if (codigoPostal != null && !codigoPostal.isEmpty()) {
			prompt.append("5. Usar el código postal ").append(codigoPostal).append(" como referencia de ubicación exacta\n\n");
		} else {
			prompt.append("\n");
		}

		prompt.append("RESPONDE ÚNICAMENTE con un JSON válido en este formato exacto (sin texto adicional):\n");
		prompt.append("{\n");
		prompt.append("  \"ruta\": [\n");
		prompt.append("    {\"orden\": 1, \"nif\": \"...\", \"razon\": \"...\", \"distancia_km\": \"...km\"},\n");
		prompt.append("    {\"orden\": 2, \"nif\": \"...\", \"razon\": \"...\", \"distancia_km\": \"...km\"}\n");
		prompt.append("  ],\n");
		prompt.append("  \"distancia_total_km\": \"número\",\n");
		prompt.append("  \"tiempo_estimado_horas\": \"número\"\n");
		prompt.append("}\n");

		return prompt.toString();
	}


	private JSONObject callChatGPTAPI(String prompt) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);

		try {
			JSONObject requestBody = new JSONObject();
			requestBody.put("model", ConstantsEndpoints.CHATGPT_MODEL);

			JSONArray messages = new JSONArray();
			JSONObject message = new JSONObject();
			message.put("role", "user");
			message.put("content", prompt);
			messages.put(message);

			requestBody.put("messages", messages);
			requestBody.put("temperature", 0.3);
			requestBody.put("max_tokens", 2000);

			AndroidNetworking.post(ConstantsEndpoints.CHATGPT_API_URL)
				.addHeaders("Authorization", "Bearer " + ConstantsEndpoints.CHATGPT_API_KEY)
				.addHeaders("Content-Type", "application/json")
				.addStringBody(requestBody.toString())
				.setPriority(Priority.MEDIUM)
				.build()
				.getAsJSONObject(new JSONObjectRequestListener() {
					@Override
					public void onResponse(JSONObject response) {
						apiResponse = response;
						apiException = null;
						latch.countDown();
					}

					@Override
					public void onError(ANError error) {
						apiResponse = null;
						apiException = new Exception("API Error: " + error.getMessage());
						Log.e(TAG, "ChatGPT API Error: " + error.getErrorDetail());
						latch.countDown();
					}
				});

			// Esperar respuesta (máximo 60 segundos)
			latch.await();

			if (apiException != null) {
				throw apiException;
			}

			return apiResponse;

		} catch (Exception e) {
			Log.e(TAG, "Error llamando a ChatGPT API: " + e.getMessage());
			throw e;
		}
	}

	private ArrayList<RutaClienteData> parseRouteResponse(JSONObject response) throws Exception {
		ArrayList<RutaClienteData> ruta = new ArrayList<>();

		try {
			// Extraer el mensaje de respuesta
			JSONArray choices = response.getJSONArray("choices");
			if (choices.length() == 0) {
				throw new Exception("Sin opciones en respuesta");
			}

			JSONObject choice = choices.getJSONObject(0);
			JSONObject message = choice.getJSONObject("message");
			String content = message.getString("content");

			// Parsear el JSON de la ruta
			JSONObject rutaJSON = new JSONObject(content);
			JSONArray rutaArray = rutaJSON.getJSONArray("ruta");

			for (int i = 0; i < rutaArray.length(); i++) {
				JSONObject item = rutaArray.getJSONObject(i);

				RutaClienteData rutaCliente = new RutaClienteData();
				rutaCliente.orden = item.getInt("orden");
				rutaCliente.nif = item.optString("nif", "");
				rutaCliente.razon = item.optString("razon", "");
				rutaCliente.distanciaKm = item.optString("distancia_km", "");

				ruta.add(rutaCliente);
			}

			Log.i(TAG, "Ruta parseada exitosamente: " + ruta.size() + " paradas");
			return ruta;

		} catch (Exception e) {
			Log.e(TAG, "Error parseando respuesta: " + e.getMessage());
			throw e;
		}
	}

	private void saveRoute(AppConfig app, ArrayList<RutaClienteData> rutaOrdenada, String ciudadBase) throws Exception {
		try {
			RutaGenerada rutaGenerada = Factory.build(RutaGenerada.class, app);

			// Eliminar rutas anteriores de esta semana
			rutaGenerada.deleteCurrentWeekRoutes();

			// Guardar cada cliente de la ruta
			for (RutaClienteData rutaCliente : rutaOrdenada) {
				// Buscar cliente por NIF en la BD
				Cliente cliente = findClienteByNIF(app, rutaCliente.nif);

				if (cliente != null) {
					RutaGenerada ruta = Factory.build(RutaGenerada.class, app);
					ruta.FechaGeneracion = new Date();
					ruta.OrdenVisita = rutaCliente.orden;
					ruta.CodigoCliente = cliente.CodigoCliente;
					ruta.NombreCliente = cliente.Nombre;
					ruta.DireccionCliente = cliente.Direccion1;
					ruta.PoblacionCliente = cliente.Poblacion;
					ruta.ProvinciaCliente = cliente.Provincia;
					ruta.DistanciaEstimada = rutaCliente.distanciaKm;
					ruta.CiudadBase = ciudadBase;

					ruta.save();

					Log.d(TAG, "Ruta guardada: " + rutaCliente.orden + ". " + cliente.Nombre);
				} else {
					Log.w(TAG, "Cliente no encontrado: " + rutaCliente.nif);
				}
			}

			Log.i(TAG, "Ruta completa guardada en BD");

		} catch (Exception e) {
			Log.e(TAG, "Error guardando ruta: " + e.getMessage());
			throw e;
		}
	}

	private Cliente findClienteByNIF(AppConfig app, String nif) throws Exception {
		try {
			android.database.Cursor cursor = app.getDatabaseOperations().executeSentence(
				"SELECT * FROM Clientes WHERE NIF = '" + nif + "' AND Activo = 1 LIMIT 1");

			if (cursor != null && cursor.getCount() > 0) {
				cursor.moveToFirst();

				Cliente cliente = Factory.build(Cliente.class, app);
				cliente.IdCliente = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdCliente")));
				cliente.CodigoCliente = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
				cliente.NIF = cursor.getString(cursor.getColumnIndex("NIF"));
				cliente.Razon = cursor.getString(cursor.getColumnIndex("Razon"));
				cliente.Nombre = cursor.getString(cursor.getColumnIndex("Nombre"));
				cliente.Direccion1 = cursor.getString(cursor.getColumnIndex("Direccion1"));
				cliente.Direccion2 = cursor.getString(cursor.getColumnIndex("Direccion2"));
				cliente.Poblacion = cursor.getString(cursor.getColumnIndex("Poblacion"));
				cliente.CodigoPostal = cursor.getString(cursor.getColumnIndex("CodigoPostal"));
				cliente.Provincia = cursor.getString(cursor.getColumnIndex("Provincia"));

				cursor.close();
				return cliente;
			}

			if (cursor != null) {
				cursor.close();
			}

		} catch (Exception e) {
			Log.e(TAG, "Error buscando cliente por NIF: " + e.getMessage());
		}

		return null;
	}

	// Clase interna para datos de ruta
	private static class RutaClienteData {
		public int orden;
		public String nif;
		public String razon;
		public String distanciaKm;
	}
}
