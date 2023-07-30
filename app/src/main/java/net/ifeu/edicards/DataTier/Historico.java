package net.ifeu.edicards.DataTier;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.edicards.DepositManagerExtension;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.UUID;

public class Historico extends Persistent implements IPersistable {

	public Long IdHistorico;
	public Date Fecha;
	public Cliente Cliente = new Cliente();
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
		UUID uuid = UUID.randomUUID();
        GUID = uuid.toString();
	}
	
	@Override
	public void InitializePersistance(AppConfig appConfig, Context context) {
		super.InitializePersistance(appConfig, context);
	}
	
	@Override
	public void ReleasePersistance() throws Exception {
		super.ReleasePersistance();
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
			this.IdHistorico = super.getDatabaseOperations().insert(Constants.TABLE_HISTORICOS, null , values);
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
		
	    super.getDatabaseOperations().update(Constants.TABLE_HISTORICOS, values, "IdHistorico = ?", whereArgs);
	}
	
	public int getRecordsCount()
	{
		return super.getDatabaseOperations().getRecordsCount(Constants.TABLE_HISTORICOS);
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

		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
				"BETWEEN '" + dateStart + "' AND '" + dateEnd + "' ORDER BY NumeroAlbaran ASC");
		
		
		if (cursor != null)
		{
			cursor.moveToFirst();
			
			if (cursor.getCount() > 0)
			{
				
				do {
						
					Historico historico = new Historico();
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

					Cliente cliente = new Cliente();
					cliente.InitializePersistance(super.appConfig, super.context);
					
					if (cliente.setClienteById(cursor.getString(cursor.getColumnIndex("IdCliente"))))
						historico.Cliente = cliente;
					
					LineaHistorico linea = new LineaHistorico();
					linea.InitializePersistance(super.appConfig, super.context);

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
		
		Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT ifnull(sum(CantidadPagada),0) as CantidadPagada FROM " + Constants.TABLE_HISTORICOS + " WHERE substr(fecha,7)||substr(fecha,1,2)||substr(fecha,4,2) " +
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

	
	private void DeleteAllLines() throws Exception {

		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_LINEAS_HISTORICO + " WHERE IdHistorico = "
						+ this.IdHistorico);
		
		if (cursor != null)
			cursor.close();
	}

	@Override
	public void delete() throws Exception {

		this.DeleteAllLines();
		
		Cursor cursor = super.getDatabaseOperations().executeSentence(
				"DELETE FROM " + Constants.TABLE_HISTORICOS + " WHERE IdHistorico = "
						+ this.IdHistorico);
		
		if (cursor != null)
			cursor.close();
		
	}

	private boolean isDateInList(String date1, String date2, String dateValue)
	{
		String date1Formatted = date1.substring(6,10) + date1.substring(0,2) + date1.substring(3,5);
		String date2Formatted = date2.substring(6,10) + date2.substring(0,2) + date2.substring(3,5);
		String dateValueFormatted = dateValue.substring(6,10) + dateValue.substring(0,2) + dateValue.substring(3,5);

		boolean result = (dateValueFormatted.compareTo(date1Formatted) > 0 || dateValueFormatted.compareTo(date1Formatted) == 0) 
				&& (dateValueFormatted.compareTo(date2Formatted) < 0 || dateValueFormatted.compareTo(date2Formatted) == 0);

		return result;
	}

	public void saveChangesToHistorico(Deposito deposito) {
		this.InitializePersistance(appConfig, context);
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

		if (!deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE) && !deposito.Retirado)
			this.Tipo = Constants.TIPO_HISTORICO_CLIENTE_EXISTENTE;
		else if (deposito.CodigoCliente.equals(Constants.NEW_CUSTOMER_CODE))
			this.Tipo = Constants.TIPO_HISTORICO_CLIENTE_NUEVO;
		else if (deposito.Retirado)
			this.Tipo = Constants.TIPO_HISTORICO_CLIENTE_BAJA;

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

			LineaHistorico lineaHistorico = new LineaHistorico();
			lineaHistorico.InitializePersistance(appConfig, context);
			lineaHistorico.Historico = this;
			lineaHistorico.Articulo = linea.Articulo;

			if (linea.UnidadesFacturadas > 0) {

				lineaHistorico.Unidades = linea.UnidadesFacturadas;
				lineaHistorico.MovimientoStock = linea.UnidadesRepuestas;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_FACTURADAS;

			}

			if (linea.UnidadesInicialesFijas == 0 && linea.UnidadesRepuestas > 0) {
				lineaHistorico.Unidades = linea.UnidadesRepuestas;
				lineaHistorico.MovimientoStock = linea.Articulo.MovimientoStock;
				lineaHistorico.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_POTENCIADAS;

			}

			if (!linea.IsNew && linea.UnidadesRepuestas == 0) {
				lineaHistorico.Unidades = linea.UnidadesInicialesFijas; //
				lineaHistorico.MovimientoStock = 0;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_BAJAS;

			}

			if (linea.UnidadesDefectuosas > 0) {
				lineaHistorico.MovimientoStock = 0;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				// linea.UnidadesFacturadas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_DEFECTUOSAS;

			}

			if (linea.Articulo.MovimientoStock != 0 || linea.Articulo.MovimientoStockDefectuosas != 0) {

				lineaHistorico.Unidades = 0;
				lineaHistorico.MovimientoStock = linea.Articulo.MovimientoStock;
				lineaHistorico.MovimientoStockDefectuosas = linea.Articulo.MovimientoStockDefectuosas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_STOCK;

			}

			if (linea.UnidadesAbono > 0) {
				lineaHistorico.Unidades = linea.UnidadesAbono * -1;
				lineaHistorico.MovimientoStock = 0;
				lineaHistorico.MovimientoStockDefectuosas = 0;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_FACTURADAS;

			}

			if (linea.UnidadesInicialesFijas > 0) {
				lineaHistorico.Unidades = linea.UnidadesInicialesFijas;
				lineaHistorico.PVP = (float) linea.PVP;
				lineaHistorico.MovimientoStock =
						lineaHistorico.MovimientoStockDefectuosas = 0;

				// linea.UnidadesFacturadas;

				lineaHistorico.Tipo = Constants.TIPO_LINEA_HISTORICO_UNIDADES_INICIALES;

			}

			try {
				lineaHistorico.save();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

	}
				
}
