package net.ifeu.edicards;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.Constants.ConstantsTypes;
import net.ifeu.library.Controls.ButtonColor;
import net.ifeu.library.LogBook.LogBookStock;
import net.ifeu.library.Utils.MessageBox.MessageBoxType;
import net.ifeu.library.Utils.Screen.ScreenManager;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.os.Environment;

public class LogBookPreviewActivity extends Activity {

    private AppConfig _appConfig;
    private ListView _listView;
    private List<LogBookRowData> _data;
    private static final int TEXT_SIZE_BUTTON = 16;
    private static final int BUTTON_MARGIN = 25;

    public static class LogBookRowData {
        public String idLogBook;
        public String fecha;
        public String codigoCliente;
        public String nombreCliente;
        public String codigoArticulo;
        public String nombreArticulo;
        public String tipoMovimiento;
        public String depositoInicial;
        public String depositoFinal;
        public String unidadesContadas;
        public String unidadesFacturadas;
        public String unidadesAbonadas;
        public String stockInicial;
        public String stockFinal;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logbook_preview);

        _appConfig = (AppConfig) this.getApplicationContext();

        android.view.WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.FILL_PARENT;
        params.width = ScreenManager.getScreenSizeByPercentage(this.getWindowManager(), 0.95f).getWidth();
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        _listView = (ListView) findViewById(R.id.logbookListView);

        createButtons();
        loadExcelData();
    }

    private void createButtons() {
        LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.buttonLinearLayout);
        if (buttonLayout == null) return;

        buttonLayout.removeAllViews();
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        ButtonColor sendButton = new ButtonColor(this, Color.GREEN);
        sendButton.setText("Enviar");
        sendButton.setTextSize(TEXT_SIZE_BUTTON);
        params.setMargins(0, 0, BUTTON_MARGIN, 0);
        sendButton.setLayoutParams(params);
        sendButton.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        ButtonColor cancelButton = new ButtonColor(this, Color.RED);
        cancelButton.setText("Cancelar");
        cancelButton.setTextSize(TEXT_SIZE_BUTTON);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, BUTTON_MARGIN, 0);
        cancelButton.setLayoutParams(params);
        cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        buttonLayout.addView(sendButton);
        buttonLayout.addView(cancelButton);
    }

    private void loadExcelData() {
        _data = new ArrayList<>();

        try {
            String directory = Environment.getExternalStorageDirectory().toString() + "/" +
                    ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_LOGBOOK;

            File dir = new File(directory);
            File[] files = dir.listFiles();

            if (files == null || files.length == 0) {
                _appConfig.getMessageBox().Show("Vista previa de trazabilidad",
                    "No se encontró ningún archivo de trazabilidad",
                    this, MessageBoxType.Error);
                finish();
                return;
            }

            File excelFile = null;
            for (File file : files) {
                if (file.getName().contains(ConstantsTypes.TRACE_TYPE_STOCK) &&
                    file.getName().endsWith(".xlsx")) {
                    if (excelFile == null || file.lastModified() > excelFile.lastModified()) {
                        excelFile = file;
                    }
                }
            }

            if (excelFile == null) {
                _appConfig.getMessageBox().Show("Vista previa de trazabilidad",
                    "No se encontró el archivo Excel de trazabilidad",
                    this, MessageBoxType.Error);
                finish();
                return;
            }

            FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                LogBookRowData rowData = new LogBookRowData();
                rowData.idLogBook = getCellValue(row.getCell(0));
                rowData.fecha = getCellValue(row.getCell(1));
                rowData.codigoCliente = getCellValue(row.getCell(2));
                rowData.nombreCliente = getCellValue(row.getCell(3));
                rowData.codigoArticulo = getCellValue(row.getCell(4));
                rowData.nombreArticulo = getCellValue(row.getCell(5));
                rowData.tipoMovimiento = getCellValue(row.getCell(6));
                rowData.depositoInicial = getCellValue(row.getCell(7));
                rowData.depositoFinal = getCellValue(row.getCell(8));
                rowData.unidadesContadas = getCellValue(row.getCell(9));
                rowData.unidadesFacturadas = getCellValue(row.getCell(10));
                rowData.unidadesAbonadas = getCellValue(row.getCell(11));
                rowData.stockInicial = getCellValue(row.getCell(12));
                rowData.stockFinal = getCellValue(row.getCell(13));

                _data.add(rowData);
            }

            fis.close();

            ArrayAdapter<LogBookRowData> adapter = new ArrayAdapter<LogBookRowData>(
                    this, R.layout.list_item_logbook, _data) {
                @NonNull
                @Override
                public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (convertView == null) {
                        convertView = inflater.inflate(R.layout.list_item_logbook, parent, false);
                    }

                    LogBookRowData data = getItem(position);

                    TextView tv;
                    tv = (TextView) convertView.findViewById(R.id.itemLogBookId);
                    tv.setText(data.idLogBook);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookFecha);
                    tv.setText(data.fecha);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookCodigoCliente);
                    tv.setText(data.codigoCliente);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookNombreCliente);
                    tv.setText(data.nombreCliente);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookCodigoArticulo);
                    tv.setText(data.codigoArticulo);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookNombreArticulo);
                    tv.setText(data.nombreArticulo);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookTipoMovimiento);
                    tv.setText(data.tipoMovimiento);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookDepositoInicial);
                    tv.setText(data.depositoInicial);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookDepositoFinal);
                    tv.setText(data.depositoFinal);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookUnidadesContadas);
                    tv.setText(data.unidadesContadas);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookUnidadesFacturadas);
                    tv.setText(data.unidadesFacturadas);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookUnidadesAbonadas);
                    tv.setText(data.unidadesAbonadas);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookStockInicial);
                    tv.setText(data.stockInicial);

                    tv = (TextView) convertView.findViewById(R.id.itemLogBookStockFinal);
                    tv.setText(data.stockFinal);

                    return convertView;
                }
            };

            _listView.setAdapter(adapter);

        } catch (Exception e) {
            _appConfig.getMessageBox().Show("Vista previa de trazabilidad",
                "Error al cargar los datos: " + e.getMessage(),
                this, MessageBoxType.Error);
            finish();
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case Cell.CELL_TYPE_NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case Cell.CELL_TYPE_BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}
