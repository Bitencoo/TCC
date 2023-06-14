package services;

import entities.Timetable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

public class TimetableServiceImpl implements TimetableService {
    @Override
    public Timetable readPreferableTimetableFromExcel(String xlsxPath) throws IOException {
        File myFile = new File("src/main/resources/disponibilidade_docentes_2_2023.xlsx");
        FileInputStream fis = new FileInputStream(myFile);

        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);

        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);

        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        int beginningRow = 2;
        int beginningColumn = 5;
        int endingColumn = 11;
        int auxColumn = 0;
        String course = "";

        for(int i = beginningRow; i < mySheet.getLastRowNum(); i++) {
            Row row = mySheet.getRow(i);
            String line = "";
            course = "";

            if(row != null){
                if(row.getCell(3) != null && row.getCell(3).getCellType() != CellType.BLANK && row.getCell(3).getStringCellValue().contains("Computação")){
                    course = row.getCell(3).getStringCellValue();
                    auxColumn = auxColumn + 1;
                }

                if(auxColumn < 8){
                    for(int j = beginningColumn; j <= endingColumn; j++){
                        Cell cell = row.getCell(j);

                        if(cell != null){
                            switch (cell.getCellType()) {
                                case STRING:
                                    line = line + returnXorO(cell.getStringCellValue());
                                    break;
                                case BLANK:
                                    line = line + returnXorO(cell.getStringCellValue());
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }


                if (auxColumn > 0){
                    auxColumn = auxColumn++;
                }

                if(auxColumn == 8){
                    auxColumn = 0;
                }
                System.out.println(line);
            }
        }

        return null;
    }

    public String returnXorO(String availability) {
        if(availability.isBlank()){
            return "-";
        }
        return availability;
    }
}
