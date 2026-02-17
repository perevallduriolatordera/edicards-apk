package net.ifeu.edicards.Excel;

import android.os.Environment;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.library.Csv.CsvCreator;
import net.ifeu.library.LogBook.LogBookStock;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.database.Cursor;
import net.ifeu.edicards.Constants.ConstantsDatabase;

public class LogBookCreator implements ILogCreator {

    private AppConfig _app;

    public LogBookCreator(AppConfig app) {
        this._app = app;
    }

    public void createExcel30Days() throws Exception {

        Date today = new Date();

        LogBookStock logBook = Factory.build(LogBookStock.class, _app);
        ArrayList<LogBookStock> trace = logBook.getLogBookLastPeriod(today);

        if (this.createExcel(trace)) {
            logBook.purge(today);
        } else {
            if (this.createCSV(trace)) {
                logBook.purge(today);
            } else {
                throw new RuntimeException("Ha sido imposible generar el fichero de trazabilidad de stock");
            }
        }
    }

    public void createCurrentExcel() throws Exception {

        LogBookStock logBook = Factory.build(LogBookStock.class, _app);
        ArrayList<LogBookStock> trace = logBook.getAllLogBook();

        if (!this.createExcel(trace)) {
            if (!this.createCSV(trace)) {
                throw new RuntimeException("Ha sido imposible generar el fichero de trazabilidad de stock");
            }
        }
    }

    // TEMPORAL: Generar Excel solo con operaciones del día actual
    // Se elimará en futuras versiones cuando se cambie la estrategia de trazabilidad
    public void createTodayExcel() throws Exception {

        LogBookStock logBook = Factory.build(LogBookStock.class, _app);
        ArrayList<LogBookStock> trace = logBook.getLogBookToday();
        ArrayList<LogBookStock> processedList = this.groupLogBooksByArticle(trace);

        if (!this.createExcel(processedList)) {
            if (!this.createCSV(processedList)) {
                throw new RuntimeException("Ha sido imposible generar el fichero de trazabilidad de stock");
            }
        }
    }

    // TEMPORAL: Agrupar LogBooks por cliente, fecha y artículo español
    // Suma valores de artículos españoles + chinos, mostrando siempre el español
    // Si hay distintos tipos de movimiento, usar el del artículo con StockInicial > 0
    private ArrayList<LogBookStock> groupLogBooksByArticle(ArrayList<LogBookStock> originalList) {
        Map<String, LogBookStock> groupedMap = new HashMap<>();

        for (LogBookStock logBook : originalList) {
            // Normalizar código a español
            String baseCode = LogBookStock.normalizeArticleCode(logBook.CodigoArticulo);
            String groupKey = logBook.CodigoCliente + "|" + logBook.Fecha + "|" + baseCode;

            if (groupedMap.containsKey(groupKey)) {
                // Ya existe, sumar valores
                LogBookStock existing = groupedMap.get(groupKey);
                existing.UnidadesDevueltas += logBook.UnidadesDevueltas;
                existing.UnidadesDefectuosas += logBook.UnidadesDefectuosas;
                existing.UnidadesRepuestas += logBook.UnidadesRepuestas;
                existing.UnidadesFacturadas += logBook.UnidadesFacturadas;
                existing.UnidadesIniciales += logBook.UnidadesIniciales;
                existing.UnidadesAbono += logBook.UnidadesAbono;
                existing.UnidadesDefectuosasAbono += logBook.UnidadesDefectuosasAbono;
                existing.StockInicial += logBook.StockInicial;
                existing.StockFinal += logBook.StockFinal;

                // Si tienen distinto TipoMovimiento, usar el del que tiene StockInicial > 0
                if (!existing.TipoMovimiento.equals(logBook.TipoMovimiento)) {
                    if (logBook.StockInicial > 0) {
                        existing.TipoMovimiento = logBook.TipoMovimiento;
                    }
                }
            } else {
                // Nueva entrada - crear con código español y descripción
                LogBookStock newEntry = Factory.build(LogBookStock.class, _app);
                newEntry.idLogBook = logBook.idLogBook;
                newEntry.Fecha = logBook.Fecha;
                newEntry.TipoMovimiento = logBook.TipoMovimiento;
                newEntry.CodigoCliente = logBook.CodigoCliente;
                newEntry.NombreCliente = logBook.NombreCliente;
                newEntry.CodigoArticulo = baseCode;
                newEntry.NombreArticulo = logBook.NombreArticulo;
                newEntry.StockInicial = logBook.StockInicial;
                newEntry.StockFinal = logBook.StockFinal;
                newEntry.UnidadesDevueltas = logBook.UnidadesDevueltas;
                newEntry.UnidadesDefectuosas = logBook.UnidadesDefectuosas;
                newEntry.UnidadesRepuestas = logBook.UnidadesRepuestas;
                newEntry.UnidadesFacturadas = logBook.UnidadesFacturadas;
                newEntry.UnidadesIniciales = logBook.UnidadesIniciales;
                newEntry.UnidadesAbono = logBook.UnidadesAbono;
                newEntry.UnidadesDefectuosasAbono = logBook.UnidadesDefectuosasAbono;

                groupedMap.put(groupKey, newEntry);
            }
        }

        ArrayList<LogBookStock> result = new ArrayList<>(groupedMap.values());

        // Ordenar por fecha
        Collections.sort(result, new Comparator<LogBookStock>() {
            @Override
            public int compare(LogBookStock a, LogBookStock b) {
                return a.Fecha.compareTo(b.Fecha);
            }
        });

        return result;
    }

    private boolean createExcel(ArrayList<LogBookStock> list) {

        if (list.size() == 0) return true;

        boolean result = true;

        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Trazabilidad " + _app.getUser().User);
            int rowCount = 0;

            Row row = sheet.createRow(0);

            this.createHeader(workbook, row);

            for (LogBookStock logBook : list) {
                row = sheet.createRow(++rowCount);
                this.createRow(row, logBook);
            }

            this.saveExcelFile(workbook);
        } catch (Exception e) {
            result = false;
        }
        return  result;
    }

    private boolean createCSV(ArrayList<LogBookStock> list) {

        if (list.size() == 0) return true;

        boolean result = true;

        try {

            SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
            String csvFilePath = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
                    + ConstantsFolders.FOLDER_LOGBOOK + "/" + ConstantsTypes.TRACE_TYPE_STOCK + "_" + _app.getUser().User + "_" + formatter.format(new Date()) + ".csv";

            CsvCreator csvCreator = new CsvCreator(csvFilePath, getCsvHeaders());

            for (LogBookStock logBook : list) {
                csvCreator.addLine(logBook.idLogBook, logBook.Fecha, logBook.CodigoCliente, logBook.NombreCliente,
                        logBook.CodigoArticulo, logBook.NombreArticulo, logBook.TipoMovimiento, logBook.UnidadesIniciales,
                        logBook.UnidadesRepuestas, logBook.UnidadesDevueltas, logBook.UnidadesFacturadas,
                        logBook.UnidadesAbono, logBook.StockInicial, logBook.StockFinal);
            }

            csvCreator.flush();
        } catch (Exception e) {
            result = false;
        }
        return  result;
    }

    private String[] getCsvHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("Identificador");
        headers.add("Fecha");
        headers.add("Codigo Cliente");
        headers.add("Nombre Cliente");
        headers.add("Código Artículo");
        headers.add("Nombre Articulo");
        headers.add("Tipo Movimiento");
        headers.add("Depósito inicial");
        headers.add("Depósito Final");
        headers.add("Unidades Contadas");
        headers.add("Unidades Facturadas");
        headers.add("Unidades Abonadas");
        headers.add("Unidades Stock Inicial");
        headers.add("Unidades Stock Final");

        String[] array = new String[headers.size()];

        return headers.toArray(array);

    }

    private void setCellHeaderStyle(Workbook wb, Cell cell) {

        Font font = wb.createFont();
        font.setColor(HSSFColor.WHITE.index);

        CellStyle cellStyle = wb.createCellStyle();
        cellStyle.setFont(font);

        cellStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
        cellStyle.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        cellStyle.setFillPattern((short) 1);

        //cellStyle.setShrinkToFit(true);

        cell.setCellStyle(cellStyle);
    }

    private void createHeader(Workbook wb, Row row) {

        int index = -1;

        Cell cell = row.createCell(++index);
        cell.setCellValue("Identificador");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Fecha");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Código cliente");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Nombre cliente");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Código artículo");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Nombre artículo");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Tipo movimiento");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Depósito Inicial");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Depósito Final");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Unidades Contadas");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Unidades Facturadas");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Unidades Abonadas");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Stock inicial");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Stock final");
        this.setCellHeaderStyle(wb, cell);

    }

    private void createRow(Row row, LogBookStock logBook) {

        int index = -1;

        Cell cell = row.createCell(++index);
        cell.setCellValue(logBook.idLogBook);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.Fecha);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.CodigoCliente);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.NombreCliente);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.CodigoArticulo);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.NombreArticulo);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.TipoMovimiento);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.UnidadesIniciales);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.UnidadesRepuestas);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.UnidadesDevueltas);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.UnidadesFacturadas);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.UnidadesAbono);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.StockInicial);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.StockFinal);

    }

    public void saveExcelFile(Workbook workbook) {

        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        String excelFilePath = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
                + ConstantsFolders.FOLDER_LOGBOOK +"/" + ConstantsTypes.TRACE_TYPE_STOCK + "_" + _app.getUser().User + "_" + formatter.format(new Date()) + ".xlsx";

        try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getNombreArticuloEspanol(String codigoEspanol) {
        try {
            Cursor cursor = _app.getDatabaseOperations().executeSentence(
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
            android.database.Cursor cursor = _app.getDatabaseOperations().executeSentence(
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
}
