package services;

import entities.Subject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PrioritizationServiceImpl implements PrioritizationService {
    @Override
    public void applyPrioritization() {
    }

    @Override
    public HashMap<Object, Object> readPeriodPrioritization(String xlsxPath) throws IOException {
        File myFile = new File(xlsxPath);
        FileInputStream fis = new FileInputStream(myFile);

        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);

        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);

        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();

        List<Subject> subjectList = new ArrayList<>();
        int professorId = 0;
        HashMap<Object, Object> periodMap = new HashMap<>();
        for(int j = 0; j < 2; j++){
            Row periodIdentification = mySheet.getRow(0);
            periodMap.put(periodIdentification.getCell(j).getStringCellValue(), new HashMap<String, String>());
            for(int i = 1; i < mySheet.getLastRowNum(); i++){
                Row row = mySheet.getRow(i);
                switch(row.getCell(j).getCellType()){
                    case STRING -> ((HashMap<String,String>) periodMap.get(periodIdentification.getCell(j).getStringCellValue()))
                            .put(row.getCell(j).getStringCellValue(), String.valueOf(i)  );
                    case NUMERIC -> ((HashMap<String,String>) periodMap.get(periodIdentification.getCell(j).getStringCellValue()))
                            .put(String.valueOf(row.getCell(j).getNumericCellValue()).replace(".0", ""), String.valueOf(i));
                }
            }
        }

        return periodMap;
    }
}
