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
import java.util.HashMap;
import java.util.List;

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
        else if(val instanceof JSONArray) {

            for(int z = 0; z < ((JSONArray)val).length(); z++) {

                JSONObject tmp = ((JSONArray)val).getJSONObject(z);
                tmp.remove("نام");

                for(String key : tmp.keySet())
                    j = writeCells(row, j, tmp.get(key));
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
            else if(jsonObject.get(key) instanceof JSONArray) {
                int size = 0;

                Row newRow = sheet.createRow(rowIdx + 1);
                int p = j;

                for(int z = 0; z < jsonObject.getJSONArray(key).length(); z++) {

                    JSONObject tmp = jsonObject.getJSONArray(key).getJSONObject(z);

                    int localSize = calcSize(tmp) - 1;
                    size += localSize;
                    sheet.addMergedRegion(new CellRangeAddress(rowIdx + 1, rowIdx + 1, p, p + localSize - 1));
                    newRow.createCell(p).setCellValue(tmp.getString("نام"));
                    tmp.remove("نام");

                    createHeader(sheet, jsonObject.getJSONArray(key).getJSONObject(z), rowIdx + 2, p);

                    p += localSize;
                }

                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx , j, j + size - 1));
                row.createCell(j).setCellValue(key);

                j += size;
            }
            else
                row.createCell(j++).setCellValue(key);
        }

    }

    private static void createHeader2(XSSFSheet sheet, JSONObject jsonObject,
                                      int rowIdx, int j, List<String> header,
                                      XSSFCellStyle cellStyle
    ) {

        Row row = (sheet.getLastRowNum() == 0 || rowIdx > sheet.getLastRowNum())
                ? sheet.createRow(rowIdx) : sheet.getRow(rowIdx);

        for (String key : header) {

            if(jsonObject.get(key) instanceof JSONObject) {
                int size = calcSize(jsonObject.getJSONObject(key));
                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx , j, j + size - 1));
                row.createCell(j).setCellValue(key);
                createHeader(sheet, jsonObject.getJSONObject(key), rowIdx + 1, j);
                j += size;
            }
            else if(jsonObject.get(key) instanceof JSONArray) {
                int size = 0;

                Row newRow = sheet.createRow(rowIdx + 1);
                int p = j;

                for(int z = 0; z < jsonObject.getJSONArray(key).length(); z++) {

                    JSONObject tmp = jsonObject.getJSONArray(key).getJSONObject(z);

                    int localSize = calcSize(tmp) - 1;
                    size += localSize;
                    sheet.addMergedRegion(new CellRangeAddress(rowIdx + 1, rowIdx + 1, p, p + localSize - 1));
                    Cell cell = newRow.createCell(p);
                    cell.setCellStyle(cellStyle);
                    cell.setCellValue(tmp.getString("نام"));
                    tmp.remove("نام");

                    createHeader(sheet, jsonObject.getJSONArray(key).getJSONObject(z), rowIdx + 2, p);

                    p += localSize;
                }

                sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx , j, j + size - 1));
                Cell cell = row.createCell(j);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(key);

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

    public static ByteArrayInputStream write2(JSONArray jsonArray, List<String> header) {

        if(jsonArray.length() == 0)
            return null;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Report_Data");

        Font headerFont = workbook.createFont();
        XSSFCellStyle cellStyle = workbook.createCellStyle();

        cellStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);
        cellStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);
        cellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        cellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        headerFont.setFontHeightInPoints((short) 13);
        cellStyle.setFont(headerFont);

        JSONObject jsonObject = jsonArray.getJSONObject(0);
        createHeader2(sheet, jsonObject, 0, 0, header, cellStyle);
        int startBody = sheet.getLastRowNum() + 1;


        for(int i = 0; i < jsonArray.length(); i++) {

            Row row = sheet.createRow(startBody + i);
            jsonObject = jsonArray.getJSONObject(i);

            int j = 0;

            for (String key : header)
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

}
