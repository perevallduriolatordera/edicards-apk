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
			setFixedColumnWidths(sheet);

			// Guardar archivo
			FileOutputStream fos = new FileOutputStream(outputFile);
			workbook.write(fos);
			fos.close();

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
		headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
		headerFont.setColor((short) 255); // Blanco
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor((short) 64); // Azul oscuro
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Nombres de columnas
		String[] headers = {
			"Orden",
			"Codigo Cliente",
			"Nombre Cliente",
			"Direccion",
			"Poblacion",
			"Provincia",
			"Distancia (km)",
			"Fecha Generacion"
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
		cellOrden.setCellValue(ruta.OrdenVisita);

		// Codigo Cliente
		Cell cellCodigo = row.createCell(cellNum++);
		cellCodigo.setCellValue(ruta.CodigoCliente != null ? ruta.CodigoCliente : "");

		// Nombre Cliente
		Cell cellNombre = row.createCell(cellNum++);
		cellNombre.setCellValue(ruta.NombreCliente != null ? ruta.NombreCliente : "");

		// Direccion
		Cell cellDireccion = row.createCell(cellNum++);
		cellDireccion.setCellValue(ruta.DireccionCliente != null ? ruta.DireccionCliente : "");

		// Poblacion
		Cell cellPoblacion = row.createCell(cellNum++);
		cellPoblacion.setCellValue(ruta.PoblacionCliente != null ? ruta.PoblacionCliente : "");

		// Provincia
		Cell cellProvincia = row.createCell(cellNum++);
		cellProvincia.setCellValue(ruta.ProvinciaCliente != null ? ruta.ProvinciaCliente : "");

		// Distancia
		Cell cellDistancia = row.createCell(cellNum++);
		cellDistancia.setCellValue(ruta.DistanciaEstimada != null ? ruta.DistanciaEstimada : "");

		// Fecha Generacion
		Cell cellFecha = row.createCell(cellNum++);
		cellFecha.setCellValue(ruta.FechaGeneracion != null ? ruta.FechaGeneracion.toString() : "");
	}

	/**
	 * Establece anchos fijos para las columnas
	 * Evita usar autoSizeColumn() que depende de AWT (no disponible en Android)
	 */
	private void setFixedColumnWidths(Sheet sheet) {
		// Establecer anchos fijos para cada columna (en unidades de 1/256 de ancho de carácter)
		sheet.setColumnWidth(0, 8 * 256);    // Orden
		sheet.setColumnWidth(1, 15 * 256);   // Codigo Cliente
		sheet.setColumnWidth(2, 20 * 256);   // Nombre Cliente
		sheet.setColumnWidth(3, 25 * 256);   // Direccion
		sheet.setColumnWidth(4, 15 * 256);   // Poblacion
		sheet.setColumnWidth(5, 15 * 256);   // Provincia
		sheet.setColumnWidth(6, 15 * 256);   // Distancia
		sheet.setColumnWidth(7, 18 * 256);   // Fecha Generacion
	}
}
