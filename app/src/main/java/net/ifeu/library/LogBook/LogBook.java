package net.ifeu.library.LogBook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;

import net.ifeu.edicards.AppConfig;
import net.ifeu.edicards.Constants.Constants;
import net.ifeu.edicards.DataTier.IPersistable;
import net.ifeu.edicards.DataTier.Persistent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    public void InitializePersistance(AppConfig appConfig, Context context) throws Exception {
        // TODO Auto-generated method stub
        super.InitializePersistance(appConfig, context);
    }

    @Override
    public void ReleasePersistance() throws Exception {
        super.ReleasePersistance();
    }

    @Override
    public void save() throws Exception {

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
            throw e;
        }

    }

    public static String getTodayFileFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(new Date());
    }

    private static String getTodayFilename() {

        String formattedDate = getTodayFileFormat();
        String filename = Constants.FILE_LOGBOOLK.replace("{0}", formattedDate);

        return Environment.getExternalStorageDirectory()
                .toString()
                + "/"
                + Constants.FOLDER_ROOT
                + "/"
                + Constants.FOLDER_LOGBOOK
                + "/"
                + filename;
    }

    private static String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(new Date());
    }

    public boolean hasLogBookCurrentWeek() throws Exception
    {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyyMMdd");
        Date today = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToSubstract = 0;

        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                daysToSubstract = -6;
                break;
            case Calendar.MONDAY:
                daysToSubstract = 0;
                break;
            case Calendar.TUESDAY:
                daysToSubstract = -1;
                break;
            case Calendar.WEDNESDAY:
                daysToSubstract = -2;
                break;
            case Calendar.THURSDAY:
                daysToSubstract = -3;
                break;
            case Calendar.FRIDAY:
                daysToSubstract = -4;
                break;
            case Calendar.SATURDAY:
                daysToSubstract = -5;
                break;
        }

        calendar.add( Calendar.DAY_OF_YEAR, daysToSubstract);
        Date firstDate = calendar.getTime();

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + Constants.TABLE_LOGBOOK + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
                "BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

        ArrayList<LogBook> list = new ArrayList<LogBook>();

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

        ArrayList<LogBook> list = new ArrayList<LogBook>();

        if (cursor != null)
        {
            cursor.moveToFirst();

            if (cursor.getCount() > 0)
            {
                do {

                    LogBook logBook = new LogBook();
                    logBook.InitializePersistance(super.appConfig, super.context);

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

        Cursor cursor = super.getDatabaseOperations().executeSentence("DELETE FROM " + Constants.TABLE_LOGBOOK + " WHERE Fecha < '" + formatter.format(today) + "'");

        if (cursor != null)
            cursor.close();
    }

}
