package asia.leadsgen.psp.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Created by Duy Anh on 4/4/19.
 */

public class ExcelProductWriterUtil {
	
	private static String[] columnsProduct = {"Product Name", "Front Image Url", "Back Image Url", "Color", "Size", "SKU"};
	
	public static void write(List<Map> allProduct, String fileName) throws IOException {
		
		// Create a Workbook
        Workbook workbook = new XSSFWorkbook();
        
        // Create a Sheet
        Sheet sheetAllProduct = workbook.createSheet("All Products in Campaign");
        
        // Create a Font for styling header cells
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        
        // Create a CellStyle with the font
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        
        // Create a Row
        Row headerRow = sheetAllProduct.createRow(0);
        
        // Creating cells
        for (int i = 0; i < columnsProduct.length; i++) {
        	Cell cell = headerRow.createCell(i);
        	cell.setCellValue(columnsProduct[i]);
        	cell.setCellStyle(headerCellStyle);
        }
        
        int rowNum = 1;
        for (Map map : allProduct) {
        	Row row = sheetAllProduct.createRow(rowNum++);
        	row.createCell(0).setCellValue(ParamUtil.getString(map, "product_name"));
        	row.createCell(1).setCellValue(ParamUtil.getString(map, "front_image"));
        	row.createCell(2).setCellValue(ParamUtil.getString(map, "back_image"));
        	row.createCell(3).setCellValue(ParamUtil.getString(map, "color_name"));
        	row.createCell(4).setCellValue(ParamUtil.getString(map, "product_size"));
        	row.createCell(5).setCellValue(ParamUtil.getString(map, "sku"));
        }
        
        // Resize all columns to fit the content size
        for(int i = 0; i < columnsProduct.length; i++) {
        	sheetAllProduct.autoSizeColumn(i);
        }
        
        // Write the output to a file
        FileOutputStream fileOut = new FileOutputStream(fileName + ".xlsx");
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
	}

}
