package net.ifeu.edicards.Excel;

import android.os.Environment;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.library.Csv.CsvCreator;
import net.ifeu.library.LogBook.LogBookExceptions;
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

public class LogBookExceptionsCreator implements ILogCreator {

    private static final int MAX_EXCEL_ROWS = 5000;

    private AppConfig _app;

    public LogBookExceptionsCreator(AppConfig app) {
        this._app = app;
    }

    public void createExcel30Days() throws Exception {

        Date today = new Date();

        LogBookExceptions logBook = Factory.build(LogBookExceptions.class, _app);
        ArrayList<LogBookExceptions> trace = logBook.getLogBookLastPeriod(today);

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

    private boolean hasEnoughMemorySpace() {
        Runtime rt = Runtime.getRuntime();
        long free = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        return free > 50 * 1024 * 1024; // 50 MB
    }

    private boolean createExcel(ArrayList<LogBookExceptions> list) {

        if (list.size() == 0) return true;
        if (!hasEnoughMemorySpace()) return true;
        if (list.size() > MAX_EXCEL_ROWS) return true;

        boolean result = true;

        try {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Trazabilidad " + _app.getUser().User);
            int rowCount = 0;

            Row row = sheet.createRow(0);

            this.createHeader(workbook, row);

            for (LogBookExceptions logBook : list) {
                row = sheet.createRow(++rowCount);
                this.createRow(row, logBook);
            }

            this.saveExcelFile(workbook);
        } catch (Exception e) {
            result = false;
        }
        return  result;
    }

    private boolean createCSV(ArrayList<LogBookExceptions> list) {

        if (list.size() == 0) return true;
        if (!hasEnoughMemorySpace()) return true;
        if (list.size() > MAX_EXCEL_ROWS) return true;

        boolean result = true;

        try {

            SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
            String csvFilePath = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
                    + ConstantsFolders.FOLDER_LOGBOOK + "/" + ConstantsTypes.TRACE_TYPE_EXCEPTION + "_" + _app.getUser().User + "_" + formatter.format(new Date()) + ".csv";

            CsvCreator csvCreator = new CsvCreator(csvFilePath, getCsvHeaders());

            for (LogBookExceptions logBook : list) {
                csvCreator.addLine(logBook.idLogBook, logBook.Fecha, logBook.Message);
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
        headers.add("Etiqueta");
        headers.add("Mensaje");

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
        cell.setCellValue("Etiqueta");
        this.setCellHeaderStyle(wb, cell);

        cell = row.createCell(++index);
        cell.setCellValue("Mensaje");
        this.setCellHeaderStyle(wb, cell);
    }

    private void createRow(Row row, LogBookExceptions logBook) {

        int index = -1;

        Cell cell = row.createCell(++index);
        cell.setCellValue(logBook.idLogBook);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.Fecha);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.Label);

        cell = row.createCell(++index);
        cell.setCellValue(logBook.Message);
    }

    public void saveExcelFile(Workbook workbook) {

        SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
        String excelFilePath = Environment.getExternalStorageDirectory().toString() + "/" + ConstantsFolders.FOLDER_ROOT + "/"
                + ConstantsFolders.FOLDER_LOGBOOK + "/" + ConstantsTypes.TRACE_TYPE_EXCEPTION + "_" + _app.getUser().User + "_" + formatter.format(new Date()) + ".xlsx";

        try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
            workbook.write(outputStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
