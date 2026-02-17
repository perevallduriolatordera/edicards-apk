package net.ifeu.library.LogBook;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class LogBookStock extends Persistent implements IPersistable, ITraceable {

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

    public static String normalizeArticleCode(String codigoArticulo) {
        return codigoArticulo != null && codigoArticulo.startsWith("CH") 
               ? codigoArticulo.substring(2) 
               : codigoArticulo;
    }

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

        // Calcular stock total (suma del artículo actual + homólogo)
        String baseCode = normalizeArticleCode(codigoArticulo);  // Normalizar a código español
        String spanishCode = baseCode;
        String chineseCode = "CH" + baseCode;

        // Obtener stock actual del artículo homólogo (chino si recibimos español, viceversa)
        int stockHomologo = getStockActualArticulo(
            codigoArticulo.startsWith("CH") ? spanishCode : chineseCode
        );

        // Stock total = parámetros recibidos + stock actual del homólogo
        this.StockInicial = stockInicial + stockHomologo;
        this.StockFinal = stockFinal + stockHomologo;
        this.UnidadesDevueltas = unidadesDevueltas;
        this.UnidadesDefectuosas = unidadesDefectuosas;
        this.UnidadesRepuestas = unidadesRepuestas;
        this.UnidadesFacturadas = unidadesFacturadas;
        this.UnidadesIniciales = unidadesIniciales;
        this.UnidadesAbono = unidadesAbono;
        this.UnidadesDefectuosasAbono = unidadesDefectuosasAbono;
    }

    // Sobrecarga: setData que recibe el stock del homólogo como parámetro (evita consulta a BD)
    public void setData(String tipoMovimiento, String codigoCliente, String nombreCliente,
                     String codigoArticulo, String nombreArticulo,
                     int stockInicial, int stockFinal, int unidadesDevueltas,
                     int unidadesDefectuosas, int unidadesRepuestas, int unidadesFacturadas,
                     int unidadesIniciales, int unidadesAbono, int unidadesDefectuosasAbono,
                     int stockHomologoInicial)
    {
        this.TipoMovimiento = tipoMovimiento;
        this.CodigoCliente = codigoCliente;
        this.NombreCliente = nombreCliente;
        this.CodigoArticulo = codigoArticulo;
        this.NombreArticulo = nombreArticulo;

        // Stock total = parámetros recibidos + stock inicial del homólogo (pasado como parámetro)
        this.StockInicial = stockInicial + stockHomologoInicial;
        this.StockFinal = stockFinal + stockHomologoInicial;
        this.UnidadesDevueltas = unidadesDevueltas;
        this.UnidadesDefectuosas = unidadesDefectuosas;
        this.UnidadesRepuestas = unidadesRepuestas;
        this.UnidadesFacturadas = unidadesFacturadas;
        this.UnidadesIniciales = unidadesIniciales;
        this.UnidadesAbono = unidadesAbono;
        this.UnidadesDefectuosasAbono = unidadesDefectuosasAbono;
    }

    // Tercera sobrecarga: setData para valores YA TOTALES (no suma nada)
    public void setDataWithTotals(String tipoMovimiento, String codigoCliente, String nombreCliente,
                     String codigoArticulo, String nombreArticulo,
                     int stockInicialTotal, int stockFinalTotal, int unidadesDevueltas,
                     int unidadesDefectuosas, int unidadesRepuestas, int unidadesFacturadas,
                     int unidadesIniciales, int unidadesAbono, int unidadesDefectuosasAbono)
    {
        this.TipoMovimiento = tipoMovimiento;
        this.CodigoCliente = codigoCliente;
        this.NombreCliente = nombreCliente;
        this.CodigoArticulo = codigoArticulo;
        this.NombreArticulo = nombreArticulo;

        // Los valores ya son TOTALES, no sumar nada
        this.StockInicial = stockInicialTotal;
        this.StockFinal = stockFinalTotal;
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

        try {
            ContentValues values = new ContentValues();
            values.put("TipoMovimiento", this.TipoMovimiento);
            values.put("NombreCliente", this.NombreCliente);
            values.put("CodigoCliente", this.CodigoCliente);
            values.put("CodigoArticulo", this.CodigoArticulo);
            values.put("NombreArticulo", this.NombreArticulo);
            values.put("StockInicial", this.StockInicial);
            values.put("StockFinal", this.StockFinal);
            values.put("UnidadesDevueltas", this.UnidadesDevueltas);
            values.put("UnidadesDefectuosas", this.UnidadesDefectuosas);
            values.put("UnidadesRepuestas", this.UnidadesRepuestas);
            values.put("UnidadesFacturadas", this.UnidadesFacturadas);
            values.put("UnidadesIniciales", this.UnidadesIniciales);
            values.put("UnidadesAbono", this.UnidadesAbono);
            values.put("UnidadesDefectuosasAbono", this.UnidadesDefectuosasAbono);

            super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_LOGBOOK, null, values);
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

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_LOGBOOK + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
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

    public ArrayList<LogBookStock> getLogBookLastPeriod(Date today) throws Exception
    {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        calendar.add( Calendar.DAY_OF_YEAR, DAYS_BY_EXTRACT);
        Date firstDate = calendar.getTime();

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_LOGBOOK + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
                "BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "'");

        ArrayList<LogBookStock> list = new ArrayList<>();

        if (cursor != null)
        {
            cursor.moveToFirst();

            if (cursor.getCount() > 0)
            {
                do {

                    LogBookStock logBook = Factory.build(LogBookStock.class, appConfig);

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

    private String getNombreArticuloEspanol(String codigoEspanol) {
        try {
            Cursor cursor = super.getDatabaseOperations().executeSentence(
                "SELECT Descripcion FROM " + ConstantsDatabase.TABLE_ARTICULOS + 
                " WHERE CodigoArticulo = '" + codigoEspanol + "' AND Activo = 1");
            
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                String descripcion = cursor.getString(cursor.getColumnIndex("Descripcion"));
                cursor.close();
                return descripcion;
            }
            
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            // Si hay error, devolver null
        }
        return null;
    }
    
    private int getStockActualArticulo(String codigoArticulo) {
        try {
            Cursor cursor = super.getDatabaseOperations().executeSentence(
                "SELECT Stock FROM " + ConstantsDatabase.TABLE_ARTICULOS + 
                " WHERE CodigoArticulo = '" + codigoArticulo + "' AND Activo = 1");
            
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0) {
                int stock = cursor.getInt(cursor.getColumnIndex("Stock"));
                cursor.close();
                return stock;
            }
            
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            // Si hay error, devolver 0
        }
        return 0;
    }

    public ArrayList<LogBookStock> getAllLogBook() throws Exception
    {
        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_LOGBOOK + " ORDER BY Fecha ASC");

        ArrayList<LogBookStock> list = new ArrayList<>();

        if (cursor != null)
        {
            cursor.moveToFirst();

            if (cursor.getCount() > 0)
            {
                do {

                    LogBookStock logBook = Factory.build(LogBookStock.class, appConfig);

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

    // TEMPORAL: Obtener solo LogBooks del día actual
    // Se eliminará en futuras versiones cuando se cambie la estrategia de trazabilidad
    public ArrayList<LogBookStock> getLogBookToday() throws Exception
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String today = formatter.format(new Date());

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_LOGBOOK +
                " WHERE substr(Fecha,1,10) = '" + today + "' ORDER BY Fecha ASC");

        ArrayList<LogBookStock> list = new ArrayList<>();

        if (cursor != null)
        {
            cursor.moveToFirst();

            if (cursor.getCount() > 0)
            {
                do {

                    LogBookStock logBook = Factory.build(LogBookStock.class, appConfig);

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

        Cursor cursor = super.getDatabaseOperations().executeSentence("DELETE FROM " + ConstantsDatabase.TABLE_LOGBOOK + " WHERE Fecha < '" + formatter.format(firstDate) + "'");

        if (cursor != null)
            cursor.close();
    }

}
