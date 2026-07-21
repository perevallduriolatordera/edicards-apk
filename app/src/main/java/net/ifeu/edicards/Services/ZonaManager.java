package net.ifeu.edicards.Services;

import android.content.Context;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Zona;

import java.util.ArrayList;

/**
 * Servicio para gestionar zonas de rutas
 */
public class ZonaManager {

	private AppConfig appConfig;

	public ZonaManager(AppConfig appConfig) {
		this.appConfig = appConfig;
	}

	/**
	 * Crea una nueva zona
	 * @param nombreZona Nombre de la zona
	 * @return La zona creada
	 * @throws Exception Si el nombre ya existe o hay error en la BD
	 */
	public Zona crearZona(String nombreZona) throws Exception {
		// Validar que el nombre no esté vacío
		if (nombreZona == null || nombreZona.trim().isEmpty()) {
			throw new Exception("El nombre de la zona no puede estar vacío");
		}

		// Verificar que no exista una zona con el mismo nombre
		Zona zonaExistente = Factory.build(Zona.class, appConfig);
		if (zonaExistente.existeZonaConNombre(nombreZona.trim())) {
			throw new Exception("Ya existe una zona con el nombre '" + nombreZona + "'");
		}

		// Crear la zona
		Zona nuevaZona = Factory.build(Zona.class, appConfig);
		nuevaZona.NombreZona = nombreZona.trim();
		nuevaZona.Activa = true;
		nuevaZona.save();

		return nuevaZona;
	}

	/**
	 * Actualiza el nombre de una zona
	 * @param idZona ID de la zona a actualizar
	 * @param nuevoNombre Nuevo nombre
	 * @throws Exception Si hay error en la BD
	 */
	public void actualizarNombreZona(long idZona, String nuevoNombre) throws Exception {
		if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
			throw new Exception("El nombre de la zona no puede estar vacío");
		}

		Zona zona = Factory.build(Zona.class, appConfig);
		if (!zona.setZonaById(idZona)) {
			throw new Exception("No se encontró la zona con ID " + idZona);
		}

		// Verificar que no exista otra zona con el mismo nombre
		Zona zonaExistente = Factory.build(Zona.class, appConfig);
		if (zonaExistente.existeZonaConNombre(nuevoNombre.trim())) {
			// Verificar que no sea la misma zona
			if (zonaExistente.IdZona != idZona) {
				throw new Exception("Ya existe otra zona con el nombre '" + nuevoNombre + "'");
			}
		}

		zona.NombreZona = nuevoNombre.trim();
		zona.update();
	}

	/**
	 * Desactiva una zona (soft delete)
	 * @param idZona ID de la zona a desactivar
	 * @throws Exception Si hay error en la BD
	 */
	public void desactivarZona(long idZona) throws Exception {
		Zona zona = Factory.build(Zona.class, appConfig);
		if (!zona.setZonaById(idZona)) {
			throw new Exception("No se encontró la zona con ID " + idZona);
		}

		zona.desactivar();
	}

	/**
	 * Elimina permanentemente una zona (solo si no tiene clientes asignados)
	 * @param idZona ID de la zona a eliminar
	 * @throws Exception Si hay error en la BD o si la zona tiene clientes asignados
	 */
	public void eliminarZona(long idZona) throws Exception {
		// Verificar que la zona existe
		Zona zona = Factory.build(Zona.class, appConfig);
		if (!zona.setZonaById(idZona)) {
			throw new Exception("No se encontró la zona con ID " + idZona);
		}

		// Verificar que no tiene clientes asignados
		int numClientes = obtenerNumeroClientesEnZona(idZona);
		if (numClientes > 0) {
			throw new Exception("No se puede eliminar la zona '" + zona.NombreZona + "' porque tiene " + numClientes + " cliente(s) asignado(s)");
		}

		// Eliminar zona de la base de datos
		appConfig.getDatabaseOperations().deleteRecordsByKeyValueString(
			net.ifeu.edicards.Constants.ConstantsDatabase.TABLE_ZONAS,
			"IdZona",
			String.valueOf(idZona)
		);
	}

	/**
	 * Activa una zona
	 * @param idZona ID de la zona a activar
	 * @throws Exception Si hay error en la BD
	 */
	public void activarZona(long idZona) throws Exception {
		Zona zona = Factory.build(Zona.class, appConfig);
		if (!zona.setZonaById(idZona)) {
			throw new Exception("No se encontró la zona con ID " + idZona);
		}

		zona.activar();
	}

	/**
	 * Obtiene todas las zonas activas
	 * @return Lista de zonas activas
	 * @throws Exception Si hay error en la BD
	 */
	public ArrayList<Zona> obtenerZonasActivas() throws Exception {
		Zona zona = Factory.build(Zona.class, appConfig);
		return zona.getAllZonasActivas();
	}

	/**
	 * Obtiene todas las zonas (activas e inactivas)
	 * @return Lista de todas las zonas
	 * @throws Exception Si hay error en la BD
	 */
	public ArrayList<Zona> obtenerTodasLasZonas() throws Exception {
		Zona zona = Factory.build(Zona.class, appConfig);
		return zona.getAllZonas();
	}

	/**
	 * Asigna un cliente a una zona
	 * @param idCliente ID del cliente
	 * @param idZona ID de la zona (null para desasignar)
	 * @throws Exception Si hay error en la BD
	 */
	public void asignarClienteAZona(long idCliente, Long idZona) throws Exception {
		Cliente cliente = Factory.build(Cliente.class, appConfig);
		if (!cliente.setClienteById(String.valueOf(idCliente))) {
			throw new Exception("No se encontró el cliente con ID " + idCliente);
		}

		// Si se especifica una zona, verificar que existe y está activa
		if (idZona != null) {
			Zona zona = Factory.build(Zona.class, appConfig);
			if (!zona.setZonaById(idZona)) {
				throw new Exception("No se encontró la zona con ID " + idZona);
			}
			if (!zona.Activa) {
				throw new Exception("La zona '" + zona.NombreZona + "' no está activa");
			}
		}

		cliente.IdZona = idZona;
		cliente.update();
	}

	/**
	 * Obtiene una zona por su ID
	 * @param idZona ID de la zona
	 * @return La zona o null si no existe
	 * @throws Exception Si hay error en la BD
	 */
	public Zona obtenerZonaPorId(long idZona) throws Exception {
		Zona zona = Factory.build(Zona.class, appConfig);
		if (zona.setZonaById(idZona)) {
			return zona;
		}
		return null;
	}

	/**
	 * Obtiene el número de clientes asignados a una zona
	 * @param idZona ID de la zona
	 * @return Número de clientes
	 * @throws Exception Si hay error en la BD
	 */
	public int obtenerNumeroClientesEnZona(long idZona) throws Exception {
		// Realizar consulta SQL para contar clientes con IdZona = idZona
		String query = "SELECT COUNT(*) FROM Clientes WHERE IdZona = " + idZona;
		android.database.Cursor cursor = appConfig.getDatabaseOperations().executeSentence(query);

		int count = 0;
		if (cursor != null) {
			count = cursor.getInt(0);
			cursor.close();
		}

		return count;
	}
}
