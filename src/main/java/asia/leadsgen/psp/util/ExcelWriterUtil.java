package asia.leadsgen.psp.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import asia.leadsgen.psp.obj.Base;

public class ExcelWriterUtil {

    private static String[] columnsSummary = {"Product Name", "Color", "Size", "Quantity"};
    
    private static String[] columnsDetail = {"CPI", "Product Name", "Color", "Size", "Quantity"};
    
    private static List<Base> bases =  new ArrayList<>();
    
    static {
    	bases.add(new Base("Basic Tee", "Gildan", "G200"));
    	bases.add(new Base("Premium Tee", "Gildan", "G640"));
    	bases.add(new Base("V-Neck", "Gildan", "G64V"));
    	bases.add(new Base("Long Sleeve", "Gildan", "G240"));
    	bases.add(new Base("Hoodie", "Gildan", "G185"));
    	bases.add(new Base("Tank Top", "Gildan", "G520"));
    	bases.add(new Base("Women's Basic", "Gildan", "G500L"));
    	bases.add(new Base("Women's Premium", "Gildan", "G640L"));
    	bases.add(new Base("Women's V-Neck", "Gildan", "G500VL"));
    	bases.add(new Base("Women's Long Sleeve", "Gildan  ", "G189  "));
    	bases.add(new Base("Women's Hoodie", "", ""));
    	bases.add(new Base("Women's Tank", "Gildan", "G645RL"));
    }

    public static void write(List<Map> fulfillmentDetail, String fileName) throws IOException, InvalidFormatException {

        // Create a Workbook
        Workbook workbook = new XSSFWorkbook();
        CreationHelper createHelper = workbook.getCreationHelper();

        // Create a Sheet
        Sheet sheetDescrpition = workbook.createSheet("Product Descrpition");
        Sheet sheetSummary = workbook.createSheet("Order Summary");
        Sheet sheetDetail = workbook.createSheet("CPI Detail");

        // Create a Font for styling header cells
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        // Create a CellStyle with the font
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        // Create a Row
        Row headerRow2 = sheetSummary.createRow(0);
        Row headerRow3 = sheetDetail.createRow(0);

        // Creating cells
        for(int i = 0; i < columnsSummary.length; i++) {
            Cell cell = headerRow2.createCell(i);
            cell.setCellValue(columnsSummary[i]);
            cell.setCellStyle(headerCellStyle);
        }
        
        for(int i = 0; i < columnsDetail.length; i++) {
            Cell cell = headerRow3.createCell(i);
            cell.setCellValue(columnsDetail[i]);
            cell.setCellStyle(headerCellStyle);
        }

        // Cell Style for formatting Date
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));

        // Create Other rows and cells with base data
        int rowNum = 0;
        for(Base base: bases) {
            Row row = sheetDescrpition.createRow(rowNum++);
            
            Cell cell = row.createCell(0);
            cell.setCellValue(base.getName());
            cell.setCellStyle(headerCellStyle);
            
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(base.getType());
            
            Cell cell3 = row.createCell(2);
            cell3.setCellValue(base.getCode());
            
        }
        
        int size = fulfillmentDetail.size();
        int count = 0;
        int rowNum2 = 1;
        for (Map map : fulfillmentDetail) {
        	Row row2 = sheetSummary.createRow(rowNum2++);
            row2.createCell(0).setCellValue(ParamUtil.getString(map, AppParams.S_PRODUCT_NAME));
            row2.createCell(1).setCellValue(ParamUtil.getString(map, AppParams.S_COLOR_NAME));
            row2.createCell(2).setCellValue(ParamUtil.getString(map, AppParams.S_SIZE));
            row2.createCell(3).setCellValue(ParamUtil.getInt(map, AppParams.N_QUANTITY));
            count++;
            if (count == size - 1) {
				break;
			}
		}
        
        int rowNum3 = 1;
        int temp = 0;
        for (Map map : fulfillmentDetail) {
        	Row row3 = sheetDetail.createRow(rowNum3++);
        	row3.createCell(0).setCellValue(ParamUtil.getString(map, AppParams.S_FULFILLMENT_ID));
            row3.createCell(1).setCellValue(ParamUtil.getString(map, AppParams.S_PRODUCT_NAME));
            row3.createCell(2).setCellValue(ParamUtil.getString(map, AppParams.S_COLOR_NAME));
            row3.createCell(3).setCellValue(ParamUtil.getString(map, AppParams.S_SIZE));
            row3.createCell(4).setCellValue(ParamUtil.getInt(map, AppParams.N_QUANTITY));
            temp++;
            if (temp == size - 1) {
				break;
			}
		}
        
        Row rowSumSheet2 = sheetSummary.createRow(size + 1);
        Cell cell0 = rowSumSheet2.createCell(0);
        cell0.setCellValue("Sum");
        cell0.setCellStyle(headerCellStyle);
        Cell cell3 = rowSumSheet2.createCell(3);
        cell3.setCellStyle(headerCellStyle);
        cell3.setCellValue(ParamUtil.getInt(fulfillmentDetail.get(size-1), AppParams.N_QUANTITY));
        
        sheetSummary.addMergedRegion(new CellRangeAddress(size + 1,size + 1,0,2));
        
        Row rowSumSheet3 = sheetDetail.createRow(size + 1);
        Cell cell0Sheet3 = rowSumSheet3.createCell(0);
        cell0Sheet3.setCellValue("Sum");
        cell0Sheet3.setCellStyle(headerCellStyle);
        Cell cell4Sheet3 = rowSumSheet3.createCell(4);
        cell4Sheet3.setCellStyle(headerCellStyle);
        cell4Sheet3.setCellValue(ParamUtil.getInt(fulfillmentDetail.get(size-1), AppParams.N_QUANTITY));
        
        sheetDetail.addMergedRegion(new CellRangeAddress(size + 1,size + 1,0,3));
        
        // Resize all columns to fit the content size
        for(int i = 0; i < columnsDetail.length; i++) {
        	sheetDetail.autoSizeColumn(i);
            sheetSummary.autoSizeColumn(i);
            sheetDescrpition.autoSizeColumn(i);
        }

        // Write the output to a file
        FileOutputStream fileOut = new FileOutputStream(fileName + ".xlsx");
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }

}