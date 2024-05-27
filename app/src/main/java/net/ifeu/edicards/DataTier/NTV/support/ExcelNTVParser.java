package net.ifeu.edicards.DataTier.NTV.support;

import net.ifeu.edicards.DataTier.NTV.DepositoNTVDTO;
import net.ifeu.edicards.DataTier.NTV.DepositoNTVLineaDTO;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExcelNTVParser {

    public static DepositoNTVDTO parseExcelFile(String filePath) {

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(filePath));
        } catch (FileNotFoundException e) {
            return null;
        }

        Workbook workbook = null;
        try {
             workbook = WorkbookFactory.create(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }

        Sheet sheet = workbook.getSheetAt(0);

        // Assuming fixed positions for data in the example
        Row clientRow = sheet.getRow(1);
        String idCliente = clientRow.getCell(1).getStringCellValue().split(" ")[0]; // Extract idCliente

        Map<String, DepositoNTVLineaDTO> lineas = new LinkedHashMap<String, DepositoNTVLineaDTO>();
        for (int i = 4; i <= sheet.getLastRowNum()-1; i++) { // Data starts from row 6
            Row row = sheet.getRow(i);
            if (row == null) continue;

            boolean containsPhoto;
            String codigoArticulo = "";

            try {
                codigoArticulo = row.getCell(0).getStringCellValue();
                containsPhoto = false;
            } catch (Exception e) {
                codigoArticulo = row.getCell(2).getStringCellValue();
                containsPhoto = true;
            }

            int cantidad = Integer.parseInt(row.getCell(!containsPhoto ? 4 : 5).getStringCellValue());
            double precio = Double.parseDouble(row.getCell(!containsPhoto ? 5 : 6).getStringCellValue().replace(",","."));
            double dto1 = Double.parseDouble(row.getCell(!containsPhoto ? 6 : 7).getStringCellValue().replace(",","."));
            double dto2 = Double.parseDouble(row.getCell(!containsPhoto ? 7 : 8).getStringCellValue().replace(",","."));

            DepositoNTVLineaDTO linea = new DepositoNTVLineaDTO(codigoArticulo, cantidad, precio, dto1, dto2);
            lineas.put(linea.codigoArticulo, linea);
        }

        return new DepositoNTVDTO(idCliente, lineas);
    }
}