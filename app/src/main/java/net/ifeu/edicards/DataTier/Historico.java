package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;

public class Historico extends Persistent implements IPersistable {

	public Long IdHistorico;
	public Date Fecha;
	public Cliente Cliente;
	public LinkedHashMap<String,LineaHistorico> Lineas = new LinkedHashMap<>();
	public double Total;
	public double CantidadPagada;
	public String Serie;
	public String NumeroAlbaran;
	public String NombrePresentacion;
	public String PoblacionPresentacion;
	public String CodigoPostalPresentacion;
	public int Tipo;
	public String GUID;
	public String Serializacion;
	public boolean ActualizarStock;

	public Historico()
	{
        GUID = UUID.randomUUID().toString();
	}
	@Override
	public void InitializePersistance(AppConfig appConfigParam) {
		super.InitializePersistance(appConfigParam);
		this.Cliente = Factory.build(Cliente.class, appConfigParam);
	}

	@Override
	public void save() throws Exception {
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		ContentValues values = new ContentValues();
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("CantidadPagada", this.CantidadPagada);
		values.put("Total", this.Total);
		values.put("Serie", this.Serie);
		values.put("NumeroAlbaran", this.NumeroAlbaran);
		values.put("NombrePresentacion", this.NombrePresentacion);
		values.put("PoblacionPresentacion", this.PoblacionPresentacion);
		values.put("CodigoPostalPresentacion",this.CodigoPostalPresentacion);
		values.put("Tipo",this.Tipo);
		values.put("GUID", this.GUID);
		values.put("Serializacion", this.Serializacion);
		values.put("ActualizarStock", this.ActualizarStock ? 1 : 0);
	
		try {
			this.IdHistorico = super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_HISTORICOS, null , values);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	
	@Override
	public void update() throws Exception {
		ContentValues values = new ContentValues();
		
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		values.put("IdHistorico", this.IdHistorico);
		values.put("IdCliente", this.Cliente.IdCliente);
		values.put("Fecha",formatter.format(this.Fecha));
		values.put("CantidadPagada", this.CantidadPagada);
		values.put("Total", this.Total);
		values.put("Serie", this.Serie);
		values.put("NumeroAlbaran", this.NumeroAlbaran);
		values.put("NombrePresentacion", this.NombrePresentacion);
		values.put("PoblacionPresentacion", this.PoblacionPresentacion);
		values.put("CodigoPostalPresentacion",this.CodigoPostalPresentacion);
		values.put("Tipo",this.Tipo);
		values.put("GUID", this.GUID);
		values.put("Serializacion", this.Serializacion);
		values.put("ActualizarStock", this.ActualizarStock ? 1 : 0);
		
		String[] whereArgs = { String.valueOf(this.IdHistorico) }; 
		
	    super.getDatabaseOperations().update(ConstantsDatabase.TABLE_HISTORICOS, values, "IdHistorico = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(ConstantsDatabase.TABLE_HISTORICOS);
	}

	public ArrayList<Historico> getHistoricosBetweenDates(Date fecha1, Date fecha2) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("MM/dd/yyyy");
		
		SimpleDateFormat formatterSQL;
		formatterSQL = new SimpleDateFormat("yyyyMMdd");
		
		SimpleDateFormat formatterSpain;
		formatterSpain = new SimpleDateFormat("MM/dd/yyyy");

		ArrayList<Historico> list = new ArrayList<>();
		
		String dateStart = formatterSQL.format(fecha1);
		String dateEnd = formatterSQL.format(fecha2);

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + dateStart + "' AND '" + dateEnd + "' ORDER BY NumeroAlbaran ASC");
		
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				
				do {
						
					Historico historico = Factory.build(Historico.class, appConfig);
					historico.IdHistorico = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdHistorico")));
					historico.Fecha = formatterSpain.parse(cursor.getString(cursor.getColumnIndex("Fecha")));
					historico.Serie = cursor.getString(cursor.getColumnIndex("Serie"));
					historico.NumeroAlbaran = cursor.getString(cursor.getColumnIndex("NumeroAlbaran"));
					historico.NombrePresentacion = cursor.getString(cursor.getColumnIndex("NombrePresentacion"));
					historico.PoblacionPresentacion = cursor.getString(cursor.getColumnIndex("PoblacionPresentacion"));
					historico.CodigoPostalPresentacion = cursor.getString(cursor.getColumnIndex("CodigoPostalPresentacion"));
					historico.CantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));
					historico.Total = Double.parseDouble(cursor.getString(cursor.getColumnIndex("Total")));
					historico.Tipo = Integer.parseInt(cursor.getString(cursor.getColumnIndex("Tipo")));
					historico.GUID = cursor.getString(cursor.getColumnIndex("GUID"));
					historico.Serializacion = cursor.getString(cursor.getColumnIndex("Serializacion"));
					historico.ActualizarStock = (cursor.getInt(cursor.getColumnIndex("ActualizarStock")) == 1);

					Cliente cliente = Factory.build(Cliente.class, appConfig);
					
					if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente"))))
						historico.Cliente = cliente;
					
					LineaHistorico linea = Factory.build(LineaHistorico.class, appConfig);

					historico.Lineas = linea.getLineasHistoricoByHistorico(historico);
					
					if (this.isDateInList(formatter.format(fecha1),formatter.format(fecha2),cursor.getString(cursor.getColumnIndex("Fecha"))))
						list.add(historico);
					
				} while (cursor.moveToNext());

			}
			cursor.close();
		}
		return list;

	}

	public double getCantidadPagadaOfThisWeek(Date today) throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");

		Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

		double cantidadPagada;
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(CantidadPagada),0) as CantidadPagada FROM " + ConstantsDatabase.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");
		
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				cantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));
								
				cursor.close();
				return cantidadPagada;
			}
			else
			{
				cursor.close();
				return 0;
			}
		}
		else
			return 0;

	}

	public Optional<Double> getCantidadPagadaToday() throws Exception
	{
		SimpleDateFormat formatter;
		formatter = new SimpleDateFormat("yyyyMMdd");
		Date today = new Date();

		double cantidadPagada;

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(CantidadPagada),0) as CantidadPagada FROM " + ConstantsDatabase.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + formatter.format(today) + "' AND '" + formatter.format(today) + "'");


		if (cursor != null)
		{
			cursor.moveToFirst();

			if (cursor.getCount() > 0)
			{
				cantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));

				cursor.close();
				return Optional.of(cantidadPagada);
			}
			else
			{
				cursor.close();
				return Optional.empty();
			}
		}
		else
			return Optional.empty();

	}

	public Date getDateOfLastMovement() throws Exception {
		// Consulta para obtener la fecha más reciente con movimientos
		String latestDateQuery = "SELECT fecha FROM " + ConstantsDatabase.TABLE_HISTORICOS +
				" ORDER BY substr(fecha,7) || substr(fecha,4,2) || substr(fecha,1,2) DESC LIMIT 1";

		Cursor latestDateCursor = super.getDatabaseOperations().executeSentence(latestDateQuery);

		if (latestDateCursor != null && latestDateCursor.moveToFirst()) {
			String fechaStr = latestDateCursor.getString(0);  // Suponiendo formato dd/MM/yy
			latestDateCursor.close();

			// Convertir la fecha recuperada a Date con formato YYYYMMDD
			try {
				// Primero interpretar el formato original de la fecha en la base de datos
				SimpleDateFormat originalFormat = new SimpleDateFormat("MM/dd/yy");
				Date parsedDate = originalFormat.parse(fechaStr);

				return parsedDate;

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return null;
		}
		return null;
	}
	public String getDateOfLastMovementFormatted() throws Exception {
		// Consulta para obtener la fecha más reciente con movimientos
		String latestDateQuery = "SELECT fecha FROM " + ConstantsDatabase.TABLE_HISTORICOS +
				" WHERE fecha != strftime('%d/%m/%Y', 'now', 'localtime')" +
				" ORDER BY substr(fecha,7) || substr(fecha,4,2) || substr(fecha,1,2) DESC LIMIT 1";

		Cursor latestDateCursor = super.getDatabaseOperations().executeSentence(latestDateQuery);

		if (latestDateCursor != null && latestDateCursor.moveToFirst()) {
			String fechaStr = latestDateCursor.getString(0);  // Suponiendo formato dd/MM/yy
			latestDateCursor.close();

			// Convertir la fecha recuperada a Date con formato YYYYMMDD
			try {
				// Primero interpretar el formato original de la fecha en la base de datos
				SimpleDateFormat originalFormat = new SimpleDateFormat("MM/dd/yy");
				Date parsedDate = originalFormat.parse(fechaStr);

				// Convertir al formato YYYYMMDD
				SimpleDateFormat targetFormat = new SimpleDateFormat("yyyyMMdd");
				String formattedDate = targetFormat.format(parsedDate);

				return formattedDate;

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return ConstantsTypes.EMPTY_STRING;
		}
        return ConstantsTypes.EMPTY_STRING;
    }

	public Optional<Double> getCantidadPagadaLastDay() throws Exception
	{

		String latestDate = getDateOfLastMovementFormatted();
		double cantidadPagada;

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(CantidadPagada),0) as CantidadPagada FROM " + ConstantsDatabase.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + latestDate + "' AND '" + latestDate + "'");

		if (cursor != null)
		{
			cursor.moveToFirst();

			if (cursor.getCount() > 0)
			{
				cantidadPagada = Double.parseDouble(cursor.getString(cursor.getColumnIndex("CantidadPagada")));

				cursor.close();
				return Optional.of(cantidadPagada);
			}
			else
			{
				cursor.close();
				return Optional.empty();
			}
		}
		else
			return Optional.empty();
	}

	
	private void DeleteAllLines() throws Exception {

		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_LINEAS_HISTORICO + " WHERE IdHistorico = "
						+ this.IdHistorico);
		
		if (cursor != null)
			cursor.close();
	}

	@Override
	public void delete() throws Exception {

		this.DeleteAllLines();
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + ConstantsDatabase.TABLE_HISTORICOS + " WHERE IdHistorico = "
						+ this.IdHistorico);
		
		if (cursor != null)
			cursor.close();
		
	}

	private boolean isDateInList(String date1, String date2, String dateValue)
	{
		String date1Formatted = date1.substring(6,10) + date1.substring(0,2) + date1.substring(3,5);
		String date2Formatted = date2.substring(6,10) + date2.substring(0,2) + date2.substring(3,5);
		String dateValueFormatted = dateValue.substring(6,10) + dateValue.substring(0,2) + dateValue.substring(3,5);

		return (dateValueFormatted.compareTo(date1Formatted) > 0 || dateValueFormatted.compareTo(date1Formatted) == 0)
				&& (dateValueFormatted.compareTo(date2Formatted) < 0 || dateValueFormatted.compareTo(date2Formatted) == 0);
	}

	public void saveChangesToHistorico(Deposito deposito) {
		this.Cliente = deposito.Cliente;
		this.NombrePresentacion = deposito.Nombre;
		this.PoblacionPresentacion = deposito.Poblacion;
		this.CodigoPostalPresentacion = deposito.CodigoPostal;

		this.Serie = deposito.Serie;
		this.NumeroAlbaran = deposito.NumeroAlbaran;

		this.Fecha = new Date();

		try {
			deposito.Calculate();
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}

		this.Total = deposito.Totales.TotalBase;
		this.CantidadPagada = deposito.CantidadPagada;

		if (!deposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE) && !deposito.Retirado)
			this.Tipo = ConstantsTypes.TIPO_HISTORICO_CLIENTE_EXISTENTE;
		else if (deposito.CodigoCliente.equals(ConstantsTypes.NEW_CUSTOMER_CODE))
			this.Tipo = ConstantsTypes.TIPO_HISTORICO_CLIENTE_NUEVO;
		else if (deposito.Retirado)
			this.Tipo = ConstantsTypes.TIPO_HISTORICO_CLIENTE_BAJA;

		this.ActualizarStock = appConfig.getWorkingArea().CurrentDepositoModalidad != DepositoModalidad.Edicards;

		// Serializamos el objeto deposito a JSON
		try {
			this.Serializacion = deposito.getDTO().serialize();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		try {
			this.save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		appConfig.getWorkingArea().CurrentHistorico = this;

		for (LineaDeposito linea : deposito.Lineas.values()) {

			LineaHistorico lineaHistorico = Factory.build(LineaHistorico.class, appConfig);
			lineaHistorico.Historico = this;
			lineaHistorico.Articulo = linea.Articulo;

			if (linea.UnidadesFacturadas > 0) {
				saveLineaHistorico(linea, linea.UnidadesFacturadas, linea.UnidadesRepuestas,
						0, ConstantsTypes.TIPO_LINEA_HISTORICO_FACTURADAS, 0);

			}

			if (linea.UnidadesInicialesFijas == 0 && linea.UnidadesRepuestas > 0) {
				saveLineaHistorico(linea, linea.UnidadesRepuestas, linea.Articulo.MovimientoStock,
						linea.Articulo.MovimientoStockDefectuosas, ConstantsTypes.TIPO_LINEA_HISTORICO_POTENCIADAS, 0);
			}

			if (!linea.IsNew && linea.UnidadesRepuestas == 0) {
				saveLineaHistorico(linea, linea.UnidadesInicialesFijas, 0,
						0, ConstantsTypes.TIPO_LINEA_HISTORICO_BAJAS, 0);
			}

			if (linea.UnidadesDefectuosas > 0) {
				saveLineaHistorico(linea, 0,0,0,
						ConstantsTypes.TIPO_LINEA_HISTORICO_DEFECTUOSAS, 0);
			}

			if (linea.Articulo.MovimientoStock != 0 || linea.Articulo.MovimientoStockDefectuosas != 0) {

				saveLineaHistorico(linea, 0, linea.Articulo.MovimientoStock,
						linea.Articulo.MovimientoStockDefectuosas, ConstantsTypes.TIPO_LINEA_HISTORICO_STOCK, 0);
			}

			if (linea.UnidadesAbono > 0) {

				saveLineaHistorico(linea, linea.UnidadesAbono * -1, 0,
						0, ConstantsTypes.TIPO_LINEA_HISTORICO_FACTURADAS, 0);
			}

			if (linea.UnidadesInicialesFijas > 0) {

				saveLineaHistorico(linea, linea.UnidadesInicialesFijas, 0,
						0, ConstantsTypes.TIPO_LINEA_HISTORICO_UNIDADES_INICIALES, (float) linea.PVP);
			}

		}

	}

	private void saveLineaHistorico(LineaDeposito linea, int unidades, int movimientoStock,
									int movimientoStockDefectouso, int tipo, float pvp) {

		LineaHistorico lineaHistorico = Factory.build(LineaHistorico.class, appConfig);
		lineaHistorico.Historico = this;
		lineaHistorico.Articulo = linea.Articulo;

		lineaHistorico.Unidades = unidades;
		lineaHistorico.MovimientoStock = movimientoStock;
		lineaHistorico.MovimientoStockDefectuosas = movimientoStockDefectouso;
		lineaHistorico.PVP = pvp;

		lineaHistorico.Tipo = tipo;
		try {
			lineaHistorico.save();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
				
}


