package net.ifeu.library.LogBook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class LogBook extends Persistent implements IPersistable {

    public long idLogBook;
    public String Fecha;
    public String TipoMovimiento;
    public String CodigoCliente;
    public String NombreCliente;
    public String CodigoArticulo;
    public String NombreArticulo;
    public int StockInicial;
    public int StockFinal;
    public int UnidadesDevueltas;
    public int UnidadesDefectuosas;
    public int UnidadesRepuestas;
    public int UnidadesFacturadas;
    public int UnidadesIniciales;
    public int UnidadesAbono;
    public int UnidadesDefectuosasAbono;

    private static final int DAYS_BY_EXTRACT = -60;

    public void setData(String tipoMovimiento, String codigoCliente, String nombreCliente,
                     String codigoArticulo, String nombreArticulo,
                     int stockInicial, int stockFinal, int unidadesDevueltas,
                     int unidadesDefectuosas, int unidadesRepuestas, int unidadesFacturadas,
                     int unidadesIniciales, int unidadesAbono, int unidadesDefectuosasAbono)
    {
        this.TipoMovimiento = tipoMovimiento;
        this.CodigoCliente = codigoCliente;
        this.NombreCliente = nombreCliente;
        this.CodigoArticulo = codigoArticulo;
        this.NombreArticulo = nombreArticulo;
        this.StockInicial = stockInicial;
        this.StockFinal = stockFinal;
        this.UnidadesDevueltas = unidadesDevueltas;
        this.UnidadesDefectuosas = unidadesDefectuosas;
        this.UnidadesRepuestas = unidadesRepuestas;
        this.UnidadesFacturadas = unidadesFacturadas;
        this.UnidadesIniciales = unidadesIniciales;
        this.UnidadesAbono = unidadesAbono;
        this.UnidadesDefectuosasAbono = unidadesDefectuosasAbono;
    }

    @Override
    public void save() throws Exception {

        Articulo articuloHomonimo = Factory.build(Articulo.class, appConfig);

        boolean isCH = false;
        boolean isFound;
        if (this.CodigoArticulo.startsWith("CH")) {
            isFound = articuloHomonimo.setArticuloByCodigo(this.CodigoArticulo.substring(2));
            isCH = true;
        } else {
            isFound = articuloHomonimo.setArticuloByCodigo("CH" + this.CodigoArticulo);
        }

        //PVT 03-03-2023
        if (isFound) {
            if (isCH) {
                this.CodigoArticulo = articuloHomonimo.CodigoArticulo;
                this.NombreArticulo = articuloHomonimo.Descripcion;
                this.StockInicial += articuloHomonimo.Stock;
                this.StockFinal += articuloHomonimo.Stock;
            } else {
                this.StockInicial += articuloHomonimo.Stock;
                this.StockFinal += articuloHomonimo.Stock;
            }
        }

        ContentValues values = new ContentValues();
        values.put("TipoMovimiento", this.TipoMovimiento);
        values.put("NombreCliente", this.NombreCliente);
        values.put("CodigoCliente", this.CodigoCliente);
        values.put("CodigoArticulo", this.CodigoArticulo);
        values.put("NombreArticulo", this.NombreArticulo);
        values.put("StockInicial", this.StockInicial);
        values.put("StockFinal", this.StockFinal);
        values.put("UnidadesDevueltas",this.UnidadesDevueltas);
        values.put("UnidadesDefectuosas", this.UnidadesDefectuosas);
        values.put("UnidadesRepuestas", this.UnidadesRepuestas);
        values.put("UnidadesFacturadas",this.UnidadesFacturadas);
        values.put("UnidadesIniciales", this.UnidadesIniciales);
        values.put("UnidadesAbono", this.UnidadesAbono);
        values.put("UnidadesDefectuosasAbono", this.UnidadesDefectuosasAbono);

        try {
            super.getDatabaseOperations().insert(Constants.TABLE_LOGBOOK, null , values);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public boolean hasLogBookCurrentWeek() throws Exception
    {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyyMMdd");
        Date today = new Date();

        Date firstDate = DateTimeUtils.getFirstDayOfCurrentWeek(today);

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_LOGBOOK + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
                "BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

        boolean result = false;

        if (cursor != null)
        {
            cursor.moveToFirst();
            result = cursor.getCount() > 0;
            cursor.close();
        }

        return result;
    }

    public ArrayList<LogBook> getLogBookLastPeriod(Date today) throws Exception
    {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        calendar.add( Calendar.DAY_OF_YEAR, DAYS_BY_EXTRACT);
        Date firstDate = calendar.getTime();

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_LOGBOOK + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
                "BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

        ArrayList<LogBook> list = new ArrayList<>();

        if (cursor != null)
        {
            cursor.moveToFirst();

            if (cursor.getCount() > 0)
            {
                do {

                    LogBook logBook = Factory.build(LogBook.class, appConfig);

                    logBook.idLogBook = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdLogBook")));
                    logBook.Fecha = cursor.getString(cursor.getColumnIndex("Fecha"));
                    logBook.TipoMovimiento = cursor.getString(cursor.getColumnIndex("TipoMovimiento"));
                    logBook.NombreCliente  = cursor.getString(cursor.getColumnIndex("NombreCliente"));
                    logBook.CodigoCliente  = cursor.getString(cursor.getColumnIndex("CodigoCliente"));
                    logBook.NombreArticulo  = cursor.getString(cursor.getColumnIndex("NombreArticulo"));
                    logBook.CodigoArticulo  = cursor.getString(cursor.getColumnIndex("CodigoArticulo"));
                    logBook.StockInicial = Integer.parseInt(cursor.getString(cursor.getColumnIndex("StockInicial")));
                    logBook.StockFinal = Integer.parseInt(cursor.getString(cursor.getColumnIndex("StockFinal")));
                    logBook.UnidadesDevueltas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesDevueltas")));
                    logBook.UnidadesDefectuosas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesDefectuosas")));
                    logBook.UnidadesRepuestas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesRepuestas")));
                    logBook.UnidadesFacturadas = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesFacturadas")));
                    logBook.UnidadesIniciales = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesIniciales")));
                    logBook.UnidadesAbono = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesAbono")));
                    logBook.UnidadesDefectuosasAbono = Integer.parseInt(cursor.getString(cursor.getColumnIndex("UnidadesDefectuosasAbono")));

                    list.add(logBook);

                } while (cursor.moveToNext());
            }

            cursor.close();
        }

        return list ;
    }

    public void purge(Date today) throws Exception
    {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyy-MM-dd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        calendar.add( Calendar.DAY_OF_YEAR, DAYS_BY_EXTRACT);
        Date firstDate = calendar.getTime();

        Cursor cursor = super.getDatabaseOperations().executeSentence("DELETE FROM " + Constants.TABLE_LOGBOOK + " WHERE Fecha < '" + formatter.format(firstDate) + "'");

        if (cursor != null)
            cursor.close();
    }

}
