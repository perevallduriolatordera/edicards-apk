package net.ifeu.edicards.Services;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import net.ifeu.edicards.Application.AppConfig;
import net.ifeu.edicards.DataTier.Cliente;
import net.ifeu.edicards.DataTier.ClienteComentario;
import net.ifeu.edicards.DataTier.ClienteRecordatorio;
import net.ifeu.edicards.DataTier.Factories.Factory;
import net.ifeu.edicards.DataTier.RutaGenerada;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
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
	private Context context;
	private String appVersion = "N/A";
	private AppConfig appConfig;
	private List<RutaGenerada> todasLasRutas; // Para calcular cuartiles

	// Estilos de celdas para colores según cuartiles (más claros para Excel)
	private CellStyle styleCuartil1;   // Verde claro - 25% más recientes
	private CellStyle styleCuartil2;   // Naranja claro - siguiente 25%
	private CellStyle styleCuartil3;   // Naranja oscuro - siguiente 25%
	private CellStyle styleCuartil4;   // Rojo claro - 25% más antiguos
	private CellStyle styleSinVisita;  // Rojo claro - nunca visitado

	/**
	 * Constructor que recibe el contexto para obtener la versión de la app
	 */
	public ExportToExcelService(Context context) {
		this.context = context;
		this.appVersion = getAppVersion();
		this.appConfig = (AppConfig) context.getApplicationContext();
	}

	/**
	 * Obtiene la versión de la aplicación
	 */
	private String getAppVersion() {
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return pInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Error obteniendo versión de la app: " + e.getMessage());
			return "N/A";
		}
	}

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

		// Guardar referencia a todas las rutas para calcular cuartiles
		this.todasLasRutas = rutas;

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

			// Inicializar estilos de colores
			initializeStyles(workbook);

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
	 * Inicializa los estilos de color para las celdas según cuartiles de antigüedad
	 * Colores más claros para mejor legibilidad en Excel
	 */
	private void initializeStyles(Workbook workbook) {
		// Verde claro - Cuartil 1 (25% más recientes)
		styleCuartil1 = workbook.createCellStyle();
		styleCuartil1.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
		styleCuartil1.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Naranja claro - Cuartil 2 (siguiente 25%)
		styleCuartil2 = workbook.createCellStyle();
		styleCuartil2.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		styleCuartil2.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Naranja oscuro - Cuartil 3 (siguiente 25%)
		styleCuartil3 = workbook.createCellStyle();
		styleCuartil3.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
		styleCuartil3.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Rojo claro - Cuartil 4 (25% más antiguos)
		styleCuartil4 = workbook.createCellStyle();
		styleCuartil4.setFillForegroundColor(IndexedColors.ROSE.getIndex());
		styleCuartil4.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Rojo claro - Sin visitas (NULL)
		styleSinVisita = workbook.createCellStyle();
		styleSinVisita.setFillForegroundColor(IndexedColors.ROSE.getIndex());
		styleSinVisita.setFillPattern(CellStyle.SOLID_FOREGROUND);
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
		headerFont.setColor(IndexedColors.WHITE.getIndex()); // Blanco
		headerStyle.setFont(headerFont);
		headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); // Azul oscuro
		headerStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		// Nombres de columnas (solo hasta la columna L - Recordatorios)
		String[] headers = {
			"Orden",
			"Cluster",
			"Zona",
			"Codigo Cliente",
			"Nombre Cliente",
			"Direccion",
			"Poblacion",
			"Provincia",
			"Ultima Visita",
			"Distancia (km)",
			"Comentarios",
			"Recordatorios"
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
		// Determinar el estilo de color para TODA la fila basado en cuartiles
		CellStyle rowStyle = getStyleForVisitAge(ruta.FechaUltimaVisita);

		int cellNum = 0;

		// Orden
		Cell cellOrden = row.createCell(cellNum++);
		cellOrden.setCellValue(ruta.OrdenVisita);
		cellOrden.setCellStyle(rowStyle);

		// Cluster
		Cell cellCluster = row.createCell(cellNum++);
		cellCluster.setCellValue(ruta.ClusterID);
		cellCluster.setCellStyle(rowStyle);

		// Zona
		Cell cellZona = row.createCell(cellNum++);
		cellZona.setCellValue(ruta.NombreZona != null ? ruta.NombreZona : "");
		cellZona.setCellStyle(rowStyle);

		// Codigo Cliente
		Cell cellCodigo = row.createCell(cellNum++);
		cellCodigo.setCellValue(ruta.CodigoCliente != null ? ruta.CodigoCliente : "");
		cellCodigo.setCellStyle(rowStyle);

		// Nombre Cliente
		Cell cellNombre = row.createCell(cellNum++);
		cellNombre.setCellValue(ruta.NombreCliente != null ? ruta.NombreCliente : "");
		cellNombre.setCellStyle(rowStyle);

		// Direccion
		Cell cellDireccion = row.createCell(cellNum++);
		cellDireccion.setCellValue(ruta.DireccionCliente != null ? ruta.DireccionCliente : "");
		cellDireccion.setCellStyle(rowStyle);

		// Poblacion
		Cell cellPoblacion = row.createCell(cellNum++);
		cellPoblacion.setCellValue(ruta.PoblacionCliente != null ? ruta.PoblacionCliente : "");
		cellPoblacion.setCellStyle(rowStyle);

		// Provincia
		Cell cellProvincia = row.createCell(cellNum++);
		cellProvincia.setCellValue(ruta.ProvinciaCliente != null ? ruta.ProvinciaCliente : "");
		cellProvincia.setCellStyle(rowStyle);

		// Ultima Visita
		Cell cellUltimaVisita = row.createCell(cellNum++);
		if (ruta.FechaUltimaVisita != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
			cellUltimaVisita.setCellValue(dateFormat.format(ruta.FechaUltimaVisita));
		} else {
			cellUltimaVisita.setCellValue("Sin visitas");
		}
		cellUltimaVisita.setCellStyle(rowStyle);

		// Distancia
		Cell cellDistancia = row.createCell(cellNum++);
		cellDistancia.setCellValue(ruta.DistanciaEstimada != null ? ruta.DistanciaEstimada : "");
		cellDistancia.setCellStyle(rowStyle);

		// Comentarios (concatenados con " - ")
		Cell cellComentarios = row.createCell(cellNum++);
		String comentarios = obtenerComentariosCliente(ruta.CodigoCliente);
		cellComentarios.setCellValue(comentarios);
		cellComentarios.setCellStyle(rowStyle);

		// Recordatorios (concatenados con " - ")
		Cell cellRecordatorios = row.createCell(cellNum++);
		String recordatorios = obtenerRecordatoriosCliente(ruta.CodigoCliente);
		cellRecordatorios.setCellValue(recordatorios);
		cellRecordatorios.setCellStyle(rowStyle);

		// Columnas M, N, O, P... eliminadas (Geo Status, Latitud, Longitud, Fecha Generacion, Version App)
	}

	/**
	 * Determina el estilo de color basado en cuartiles de antigüedad de visita
	 * Sistema idéntico al de RutasViewerFragment
	 */
	private CellStyle getStyleForVisitAge(Date fechaUltimaVisita) {
		// EXCEPCIÓN: Nunca visitado (NULL) siempre en rojo claro
		if (fechaUltimaVisita == null) {
			return styleSinVisita;
		}

		// Calcular días desde última visita
		long diff = new Date().getTime() - fechaUltimaVisita.getTime();
		int diasDesdeVisita = (int) (diff / (24 * 60 * 60 * 1000));

		// Obtener todos los días de visita (excluyendo NULL y BASE)
		java.util.List<Integer> diasVisitas = new java.util.ArrayList<>();
		for (RutaGenerada ruta : todasLasRutas) {
			// Saltar el cliente BASE (OrdenVisita = 1)
			if (ruta.OrdenVisita == 1) continue;

			if (ruta.FechaUltimaVisita != null) {
				long diffRuta = new Date().getTime() - ruta.FechaUltimaVisita.getTime();
				int diasRuta = (int) (diffRuta / (24 * 60 * 60 * 1000));
				diasVisitas.add(diasRuta);
			}
		}

		// Si no hay datos, usar color neutro
		if (diasVisitas.isEmpty()) {
			return styleCuartil2; // Amarillo como neutro
		}

		// Ordenar de menor a mayor (más reciente a más antiguo)
		java.util.Collections.sort(diasVisitas);

		// Calcular cuartiles
		int totalClientes = diasVisitas.size();
		int q1Index = totalClientes / 4;
		int q2Index = totalClientes / 2;
		int q3Index = (3 * totalClientes) / 4;

		int q1 = q1Index < diasVisitas.size() ? diasVisitas.get(q1Index) : 0;
		int q2 = q2Index < diasVisitas.size() ? diasVisitas.get(q2Index) : 0;
		int q3 = q3Index < diasVisitas.size() ? diasVisitas.get(q3Index) : 0;

		// Asignar estilo según cuartil
		if (diasDesdeVisita <= q1) {
			return styleCuartil1; // Verde - 25% más recientes
		} else if (diasDesdeVisita <= q2) {
			return styleCuartil2; // Naranja claro - siguiente 25%
		} else if (diasDesdeVisita <= q3) {
			return styleCuartil3; // Naranja oscuro - siguiente 25%
		} else {
			return styleCuartil4; // Rojo claro - 25% más antiguos
		}
	}

	/**
	 * Obtiene todos los comentarios de un cliente concatenados con " - "
	 */
	private String obtenerComentariosCliente(String codigoCliente) {
		try {
			ClienteComentario comentarioModel = Factory.build(ClienteComentario.class, appConfig);
			java.util.ArrayList<ClienteComentario> comentarios = comentarioModel.getComentariosByCliente(codigoCliente);

			if (comentarios == null || comentarios.isEmpty()) {
				return "";
			}

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < comentarios.size(); i++) {
				sb.append(comentarios.get(i).Comentario);
				if (i < comentarios.size() - 1) {
					sb.append(" - ");
				}
			}
			return sb.toString();

		} catch (Exception e) {
			Log.e(TAG, "Error obteniendo comentarios: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Obtiene todos los recordatorios de un cliente concatenados con " - "
	 */
	private String obtenerRecordatoriosCliente(String codigoCliente) {
		try {
			ClienteRecordatorio recordatorioModel = Factory.build(ClienteRecordatorio.class, appConfig);
			java.util.ArrayList<ClienteRecordatorio> recordatorios = recordatorioModel.getRecordatoriosByCliente(codigoCliente);

			if (recordatorios == null || recordatorios.isEmpty()) {
				return "";
			}

			StringBuilder sb = new StringBuilder();
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
			for (int i = 0; i < recordatorios.size(); i++) {
				ClienteRecordatorio rec = recordatorios.get(i);
				String fecha = dateFormat.format(rec.FechaRecordatorio);
				sb.append(rec.TipoRecordatorio).append(" (").append(fecha).append(")");
				if (rec.Descripcion != null && !rec.Descripcion.isEmpty()) {
					sb.append(": ").append(rec.Descripcion);
				}
				if (i < recordatorios.size() - 1) {
					sb.append(" - ");
				}
			}
			return sb.toString();

		} catch (Exception e) {
			Log.e(TAG, "Error obteniendo recordatorios: " + e.getMessage());
			return "";
		}
	}

	/**
	 * Establece anchos fijos para las columnas
	 * Evita usar autoSizeColumn() que depende de AWT (no disponible en Android)
	 */
	private void setFixedColumnWidths(Sheet sheet) {
		// Establecer anchos fijos para cada columna (en unidades de 1/256 de ancho de carácter)
		sheet.setColumnWidth(0, 8 * 256);    // Orden
		sheet.setColumnWidth(1, 10 * 256);   // Cluster
		sheet.setColumnWidth(2, 15 * 256);   // Zona
		sheet.setColumnWidth(3, 15 * 256);   // Codigo Cliente
		sheet.setColumnWidth(4, 25 * 256);   // Nombre Cliente
		sheet.setColumnWidth(5, 30 * 256);   // Direccion
		sheet.setColumnWidth(6, 18 * 256);   // Poblacion
		sheet.setColumnWidth(7, 15 * 256);   // Provincia
		sheet.setColumnWidth(8, 15 * 256);   // Ultima Visita
		sheet.setColumnWidth(9, 15 * 256);   // Distancia (km)
		sheet.setColumnWidth(10, 40 * 256);  // Comentarios (más ancho)
		sheet.setColumnWidth(11, 40 * 256);  // Recordatorios (más ancho)
		// Columnas 12+ eliminadas
	}
}
