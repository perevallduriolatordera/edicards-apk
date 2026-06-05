package net.ifeu.edicards.Services;

import android.os.Environment;
import android.util.Log;

import net.ifeu.edicards.DataTier.RutaGenerada;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Servicio para exportar rutas a formato Excel
 */
public class ExportToExcelService {

	private static final String TAG = "ExportToExcelService";

	/**
	 * Exporta lista de rutas a archivo Excel
	 *
	 * @param rutas Lista de RutaGenerada a exportar
	 * @return Ruta completa del archivo creado, o null si falla
	 * @throws Exception Si hay error durante la exportación
	 */
	public String exportRutasToExcel(List<RutaGenerada> rutas) throws Exception {
		if (rutas == null || rutas.isEmpty()) {
			Log.w(TAG, "Lista de rutas vacía, no se puede exportar");
			return null;
		}

		try {
			// Crear carpeta si no existe
			File rutasFolder = createRutasFolder();
			if (rutasFolder == null) {
				Log.e(TAG, "No se pudo crear carpeta Rutas");
				return null;
			}

			// Generar nombre archivo con timestamp
			String fileName = generateFileName();
			File outputFile = new File(rutasFolder, fileName);

			// Crear workbook
			Workbook workbook = new HSSFWorkbook();
			Sheet sheet = workbook.createSheet("Ruta Optimizada");

			// Crear encabezados
			createHeaders(sheet, workbook);

			// Agregar datos
			int rowNum = 1;
			for (RutaGenerada ruta : rutas) {
				Row row = sheet.createRow(rowNum++);
				fillRow(row, ruta);
			}

			// Ajustar ancho de columnas
			autoResizeColumns(sheet);

			// Guardar archivo
			FileOutputStream fos = new FileOutputStream(outputFile);
			workbook.write(fos);
			fos.close();
			workbook.close();

			Log.i(TAG, "Archivo Excel creado exitosamente: " + outputFile.getAbsolutePath());
			return outputFile.getAbsolutePath();

		} catch (Exception e) {
			Log.e(TAG, "Error exportando a Excel: " + e.getMessage());
			throw e;
		}
	}

	/**
	 * Crea la carpeta Rutas dentro de la carpeta edicards
	 * Estructura: /storage/emulated/0/edicards/Rutas/
	 */
	private File createRutasFolder() {
		try {
			// Obtener carpeta raíz de almacenamiento externo
			File sdCard = Environment.getExternalStorageDirectory();

			// Crear carpeta edicards
			File ediardsFolder = new File(sdCard, "edicards");
			if (!ediardsFolder.exists()) {
				if (!ediardsFolder.mkdirs()) {
					Log.e(TAG, "No se pudo crear carpeta edicards");
					return null;
				}
			}

			// Crear carpeta Rutas dentro de edicards
			File rutasFolder = new File(ediardsFolder, "Rutas");
			if (!rutasFolder.exists()) {
				if (!rutasFolder.mkdirs()) {
					Log.e(TAG, "No se pudo crear carpeta Rutas");
					return null;
				}
				Log.i(TAG, "Carpeta Rutas creada en: " + rutasFolder.getAbsolutePath());
			}

			return rutasFolder;
		} catch (Exception e) {
			Log.e(TAG, "Error creando carpeta Rutas: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Genera nombre de archivo con formato: RutaYYYYMMDD_HHMMSS.xls
	 */
	private String generateFileName() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
		String timestamp = sdf.format(new Date());
		return "Ruta" + timestamp + ".xls";
	}

	/**
	 * Crea encabezados de la tabla
	 */
	private void createHeaders(Sheet sheet, Workbook workbook) {
		Row headerRow = sheet.createRow(0);

		// Crear estilo para encabezados
		CellStyle headerStyle = workbook.createCellStyle();
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);
		headerFont.setColor((short) 255); // Blanco
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor((short) 64); // Azul oscuro
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Nombres de columnas
		String[] headers = {
			"Orden",
			"Código Cliente",
			"NIF",
			"Razón Social",
			"Nombre",
			"Distancia (km)",
			"Fecha Generación"
		};

		for (int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}
	}

	/**
	 * Rellena una fila con datos de una ruta
	 */
	private void fillRow(Row row, RutaGenerada ruta) {
		int cellNum = 0;

		// Orden
		Cell cellOrden = row.createCell(cellNum++);
		cellOrden.setCellValue(ruta.Orden != null ? ruta.Orden : "");

		// Código Cliente
		Cell cellCodigo = row.createCell(cellNum++);
		cellCodigo.setCellValue(ruta.CodigoCliente != null ? ruta.CodigoCliente : "");

		// NIF
		Cell cellNif = row.createCell(cellNum++);
		cellNif.setCellValue(ruta.NIF != null ? ruta.NIF : "");

		// Razón Social
		Cell cellRazon = row.createCell(cellNum++);
		cellRazon.setCellValue(ruta.Razon != null ? ruta.Razon : "");

		// Nombre
		Cell cellNombre = row.createCell(cellNum++);
		cellNombre.setCellValue(ruta.Nombre != null ? ruta.Nombre : "");

		// Distancia
		Cell cellDistancia = row.createCell(cellNum++);
		cellDistancia.setCellValue(ruta.DistanciaKm != null ? ruta.DistanciaKm : "");

		// Fecha Generación
		Cell cellFecha = row.createCell(cellNum++);
		cellFecha.setCellValue(ruta.FechaGeneracion != null ? ruta.FechaGeneracion.toString() : "");
	}

	/**
	 * Ajusta automáticamente el ancho de las columnas
	 */
	private void autoResizeColumns(Sheet sheet) {
		for (int i = 0; i < 7; i++) {
			sheet.autoSizeColumn(i);
		}
	}
}
