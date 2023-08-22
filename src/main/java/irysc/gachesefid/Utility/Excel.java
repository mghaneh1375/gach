package irysc.gachesefid.Utility;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import static irysc.gachesefid.Utility.FileUtils.limboDir;
import static irysc.gachesefid.Utility.FileUtils.limboDir_dev;
import static irysc.gachesefid.Utility.StaticValues.DEV_MODE;
import static irysc.gachesefid.Utility.Utility.printException;


public class Excel {

    public static ArrayList<Row> read(String path) {

        try {
            FileInputStream file = new FileInputStream(new File((DEV_MODE) ? limboDir_dev + path : limboDir + path));

            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheetAt(0);
            ArrayList<Row> rows = new ArrayList<>();

            for (Row aSheet : sheet) rows.add(aSheet);

            file.close();
            return rows;

        } catch (Exception e) {
            System.out.println("Excel Reading have problems"+e);
        }

        return null;
    }

    private static int writeCells(Row row, int j, Object val) {

        if(val instanceof JSONObject) {
            for(String key : ((JSONObject) val).keySet()) {
                j = writeCells(row, j, ((JSONObject) val).get(key));
            }

            return j;
        }
        else
            row.createCell(j).setCellValue(String.valueOf(val));

        return j + 1;
    }

    private static int calcSize(JSONObject jsonObject) {

        int sum = 0;

        for(String key : jsonObject.keySet()) {
            if(jsonObject.get(key) instanceof JSONObject)
                sum += calcSize(jsonObject.getJSONObject(key));
            else
                sum++;
        }

        return sum;
    }

    private static void createHeader(XSSFSheet sheet, JSONObject jsonObject, int rowIdx, int j) {

        Row row = (sheet.getLastRowNum() == 0 || rowIdx > sheet.getLastRowNum())
                ? sheet.createRow(rowIdx) : sheet.getRow(rowIdx);

        for (String key : jsonObject.keySet()) {

            if(jsonObject.get(key) instanceof JSONObject) {
                int size = calcSize(jsonObject.getJSONObject(key));
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx , j, j + size - 1));
                row.createCell(j).setCellValue(key);
                createHeader(sheet, jsonObject.getJSONObject(key), rowIdx + 1, j);
                j += size;
            }
            else
                row.createCell(j++).setCellValue(key);
        }

    }

    public static ByteArrayInputStream write(JSONArray jsonArray) {

        if(jsonArray.length() == 0)
            return null;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Report_Data");

        JSONObject jsonObject = jsonArray.getJSONObject(0);
        createHeader(sheet, jsonObject, 0, 0);
        int startBody = sheet.getLastRowNum() + 1;


//        byte[] rgbB;
//        XSSFColor color;
//
//        try {
//            rgbB = Hex.decodeHex("ff0000");
//            color = new XSSFColor(rgbB);
//
//            XSSFCellStyle cellStyle = workbook.createCellStyle();
//            cellStyle.setFillForegroundColor(color);
//            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//            sheet.getRow(0).setRowStyle(cellStyle);
//        } catch (DecoderException e) {
//            printException(e);
//        }

        for(int i = 0; i < jsonArray.length(); i++) {

            Row row = sheet.createRow(startBody + i);
            jsonObject = jsonArray.getJSONObject(i);

            int j = 0;

            for (String key : jsonObject.keySet())
                j = writeCells(row, j, jsonObject.get(key));
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
        catch (Exception x) {
            printException(x);
        }

        return null;
    }

    public static Object getCellValue(Cell cell) {

        try {
            String str = cell.getStringCellValue();

            if(str.charAt(0) == '0')
                return str;

            try {
                return Integer.parseInt(str);
            }
            catch (Exception x) {
                return str;
            }
        }
        catch (Exception x) {
            double d = cell.getNumericCellValue();
            if ((d == Math.floor(d)) && !Double.isInfinite(d)) {
                return (int)d;
            }

            return d;
        }

    }

//    public static ByteArrayInputStream createQuiz() {
//
//        Font headerFont = workbook.createFont();
//        XSSFCellStyle cellStyle = workbook.createCellStyle();
//
//        cellStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
//        cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
//        cellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
//        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
//        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
//        headerFont.setFontHeightInPoints((short) 36);
//        cellStyle.setFont(headerFont);
//
//        cell.setCellStyle(cellStyle);
//
//    }
}
