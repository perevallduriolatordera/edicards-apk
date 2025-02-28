package net.ifeu.library.LogBook;

import android.content.ContentValues;
import android.database.Cursor;

import net.ifeu.edicards.Constants.ConstantsDatabase;
import net.ifeu.edicards.DataTier.Articulo;
import net.ifeu.edicards.DataTier.Deposito;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.LineaDeposito;
import net.ifeu.edicards.DataTier.Persistance.IPersistable;
import net.ifeu.edicards.DataTier.Persistance.Persistent;
import net.ifeu.library.Utils.DateTime.DateTimeUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class LogBookExceptions extends Persistent implements IPersistable, ITraceable {
    public long idLogBook;
    public String Fecha;
    public String Label;
    public String Message;
    private static LogBookExceptions instance;

    private static final int DAYS_BY_EXTRACT = -60;

    private LogBookExceptions() {}

    public static synchronized LogBookExceptions getInstance() {
        if (instance == null) {
            instance = new LogBookExceptions();
        }
        return instance;
    }

    private static String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");

        StackTraceElement[] stackTrace = e.getStackTrace();
        int limit = Math.min(stackTrace.length, 10); // Máximo 3 líneas del stack

        for (int i = 0; i < limit; i++) {
            sb.append("    at ").append(stackTrace[i]).append("\n");
        }

        return sb.toString();
    }

    public void setData(String label, String message)
    {
        this.Label = label;
        this.Message = message;
    }

    public void setData(String label, Exception e)
    {
        this.Label = label;
        this.Message = getStackTraceAsString(e);
    }

    @Override
    public void save() throws Exception {

        ContentValues values = new ContentValues();
        values.put("Message", this.Message);
        values.put("Label", this.Label);
        try {
            super.getDatabaseOperations().insert(ConstantsDatabase.TABLE_LOGBOOK_EXCEPTIONS, null , values);
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

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_LOGBOOK_EXCEPTIONS + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
                "BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "' ORDER BY Fecha DESC");

        boolean result = false;

        if (cursor != null)
        {
            cursor.moveToFirst();
            result = cursor.getCount() > 0;
            cursor.close();
        }

        return result;
    }

    public ArrayList<LogBookExceptions> getLogBookLastPeriod(Date today) throws Exception
    {
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("yyyyMMdd");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        calendar.add( Calendar.DAY_OF_YEAR, DAYS_BY_EXTRACT);
        Date firstDate = calendar.getTime();

        Cursor cursor = super.getDatabaseOperations().executeSentence("SELECT * FROM " + ConstantsDatabase.TABLE_LOGBOOK_EXCEPTIONS + " WHERE substr(Fecha,1,4)||substr(Fecha,6,2)||substr(Fecha,9,2) " +
                "BETWEEN '" + formatter.format(firstDate) + "' AND '" + formatter.format(today) + "' ORDER BY Fecha DESC");

        ArrayList<LogBookExceptions> list = new ArrayList<>();

        if (cursor != null)
        {
            cursor.moveToFirst();

            if (cursor.getCount() > 0)
            {
                do {

                    LogBookExceptions logBook = Factory.build(LogBookExceptions.class, appConfig);

                    logBook.idLogBook = Long.parseLong(cursor.getString(cursor.getColumnIndex("IdLogBook")));
                    logBook.Fecha = cursor.getString(cursor.getColumnIndex("Fecha"));
                    logBook.Message = cursor.getString(cursor.getColumnIndex("Message"));
                    logBook.Label = cursor.getString(cursor.getColumnIndex("Label"));

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

        Cursor cursor = super.getDatabaseOperations().executeSentence("DELETE FROM " + ConstantsDatabase.TABLE_LOGBOOK_EXCEPTIONS + " WHERE Fecha < '" + formatter.format(firstDate) + "'");

        if (cursor != null)
            cursor.close();
    }

}
