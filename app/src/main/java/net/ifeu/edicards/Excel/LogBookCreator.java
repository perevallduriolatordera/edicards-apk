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
import java.util.Date;
import java.util.List;

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

    private boolean createExcel(ArrayList<LogBookStock> list) {

        if (list.size() == 0) return false;

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

        if (list.size() == 0) return false;

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
}
