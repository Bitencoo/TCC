package services;

import entities.ClassTime;
import entities.Professor;
import entities.Subject;
import entities.Timetable;
import enums.Shift;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TimetableServiceImpl implements TimetableService {
    @Override
    public List<Timetable> readPreferableTimetableFromExcel(String xlsxPath) throws IOException {
        File myFile = new File("src/main/resources/disponibilidade_docentes_2_2023.xlsx");
        FileInputStream fis = new FileInputStream(myFile);

        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);

        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);

        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();
        int beginningRow = 2;
        String professor = "";
        int professorIdIncrementor = 1;
        int timetableIdIncrementor = 1;
        int day = 0;
        Timetable currentTimetable = null;
        Timetable timetable = null;
        String line = "";

        List<Timetable> timetableList = new ArrayList<>();

        for(int i = beginningRow; i < mySheet.getLastRowNum(); i++) {
            //Estou na linha Agora basta ler das colunas 4 até 11, 20 até 26 e depois 26 até 28
            //Fazer dois laços de repetição
            Row row = mySheet.getRow(i);
            line = "";

            if (row != null) {
                if (row.getCell(0) != null && row.getCell(0).getCellType() != CellType.BLANK && !row.getCell(0).getStringCellValue().equals("NOME")) {
                    professor = row.getCell(0).getStringCellValue();
                    System.out.println(professor);
                    System.out.println("Hora   S T Q Q S S");
                    currentTimetable = createEmptyProfessorPreferableTimetableMatutino(timetableIdIncrementor, professorIdIncrementor, professor, Shift.MATUTINO);

                    timetableIdIncrementor++;
                    professorIdIncrementor++;
                    day = -1;
                    timetableList.add(currentTimetable);
                }
                currentTimetable = processTimetableShifts(currentTimetable, Shift.MATUTINO, row, day, line, false);
            }
        }

        for(int i = beginningRow; i < mySheet.getLastRowNum(); i++) {
            //Estou na linha Agora basta ler das colunas 4 até 11, 20 até 26 e depois 26 até 28
            //Fazer dois laços de repetição
            Row row = mySheet.getRow(i);
            line = "";
            if (row != null) {
                if (row.getCell(0) != null && row.getCell(0).getCellType() != CellType.BLANK && !row.getCell(0).getStringCellValue().equals("NOME")) {
                    professor = row.getCell(0).getStringCellValue();
                    System.out.println(professor);
                    System.out.println("Hora   S T Q Q S S");
                    currentTimetable = createEmptyProfessorPreferableTimetableNoturno(timetableIdIncrementor, professorIdIncrementor, professor, Shift.NOTURNO);
                    timetableIdIncrementor++;
                    professorIdIncrementor++;
                    day = -1;
                    timetableList.add(currentTimetable);
                }
                currentTimetable = processTimetableShifts(currentTimetable, Shift.NOTURNO, row, day, line, false);
                currentTimetable = processTimetableShifts(currentTimetable, Shift.NOTURNO, row, day, line, true);

            }
        }

        return timetableList;
    }

    //Reads QDA file to get shifts, professors and subjects
    public List<Subject> readQDA(String xlsxPath, Map<Object, Object> periodPrioritization) throws IOException {
        File myFile = new File(xlsxPath);
        FileInputStream fis = new FileInputStream(myFile);

        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);

        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);

        // Get iterator to all the rows in current sheet
        Iterator<Row> rowIterator = mySheet.iterator();

        List<Subject> subjectList = new ArrayList<>();
        int professorId = 0;
        Subject subject = null;
        for(int i = 1; i < mySheet.getLastRowNum(); i++) {
            Row row = mySheet.getRow(i);
            if(row.getCell(0) != null && !row.getCell(0).getCellType().equals(CellType.BLANK)) {
                professorId++;
            }
            if(row.getCell(10).getStringCellValue().equals("Engenharia da Computação")) {
                int period = Integer.parseInt(row.getCell(13).getStringCellValue().replace("º", "").trim());
                String rowPeriodSubject = row.getCell(13).getStringCellValue().replace("º", "").trim();
                boolean isSubjectThisSemester = Objects.isNull(((HashMap<String, String>)periodPrioritization.get("MATUTINO")).get(row.getCell(13).getStringCellValue().replace("º", "").trim()));
                if (!isSubjectThisSemester) {
                    subject = Subject
                            .builder()
                            .className(row.getCell(14).getStringCellValue())
                            .numbersOfLessons(4)
                            .period(period)
                            .shift(row.getCell(12).getStringCellValue().contains("Not") ? Shift.NOTURNO : Shift.MATUTINO)
                            .prioritization(period % 2 == 0
                                    ? Integer.parseInt(((HashMap<String, String>) periodPrioritization.get("MATUTINO")).get(row.getCell(13).getStringCellValue().replace("º", "").trim()))
                                    : Integer.parseInt(((HashMap<String, String>) periodPrioritization.get("NOTURNO")).get(row.getCell(13).getStringCellValue().replace("º", "").trim())))
                            .professor(
                                    Professor
                                            .builder()
                                            .name(row.getCell(1).getStringCellValue())
                                            .onlyUEMGProfessor(false)
                                            .id(String.valueOf(professorId))
                                            .build()
                            )
                            .build();
                }
            }
            if(subject != null){
                subjectList.add(subject);
            }
            subject = null;
        }
        Comparator<Subject> subjectComparator
                = (s1, s2) -> (int) s1.getPrioritization() - s2.getPrioritization();
        subjectList.sort(subjectComparator);
        subjectList = subjectList.stream().sorted((s1, s2) -> s1.getShift().toString().compareTo(s2.getShift().toString())).collect(Collectors.toList());
        return subjectList;
    }

    public String returnXorO(String availability) {
        if(availability.isBlank()){
            return " - ";
        }
        return " " + availability + " ";
    }

    public Timetable createEmptyProfessorPreferableTimetableMatutino(int timetableIdIncrementor, int professorIdIncrementor, String professorName, Shift shift){
        Timetable timetable = new Timetable();
        timetable.setId(timetableIdIncrementor);
        timetable.setProfessorId(professorIdIncrementor);
        timetable.setShift(shift);
        timetable.setProfessor(
                Professor
                        .builder()
                        .id(String.valueOf(professorIdIncrementor))
                        .name(professorName)
                        .build()
        );
        timetable.setClasses(new ArrayList<ClassTime>());

        for(int i = 0; i < 6; i++){
            for(int j = 0; j < 6; j++){
                ClassTime classTime = ClassTime
                        .builder()
                        .time(retrieveClasstimeTime(i, shift.toString()))
                        .dayOfTheWeek(retrieveClasstimeDay(j))
                        .build();
                timetable.getClasses().add(classTime);
            }
        }

        return timetable;

    }

    public Timetable createEmptyProfessorPreferableTimetableNoturno(int timetableIdIncrementor, int professorIdIncrementor, String professorName, Shift shift){
        Timetable timetable = new Timetable();
        timetable.setId(timetableIdIncrementor);
        timetable.setProfessorId(professorIdIncrementor);
        timetable.setShift(shift);
        timetable.setProfessor(
                Professor
                        .builder()
                        .id(String.valueOf(professorIdIncrementor))
                        .name(professorName)
                        .build()
        );
        timetable.setClasses(new ArrayList<ClassTime>());

        for(int i = 0; i < 5; i++){
            for(int j = 0; j < 5; j++){
                ClassTime classTime = ClassTime
                        .builder()
                        .time(retrieveClasstimeTime(i, shift.toString()))
                        .dayOfTheWeek(retrieveClasstimeDay(j))
                        .build();
                timetable.getClasses().add(classTime);
            }
        }

        for(int i = 0; i < 6; i++){
            ClassTime classTime = ClassTime
                    .builder()
                    .time(retrieveClasstimeTime(i, Shift.MATUTINO.toString()))
                    .dayOfTheWeek(retrieveClasstimeDay(5))
                    .build();
            timetable.getClasses().add(classTime);
        }

        return timetable;
    }

    public String retrieveClasstimeTime(int timePosition, String shift){
        switch (timePosition) {
            case 0:
                return shift.equals("MATUTINO") ? "07:00" : "18:30";
            case 1:
                return shift.equals("MATUTINO") ? "07:50" : "19:20";
            case 2:
                return shift.equals("MATUTINO") ? "08:40" : "20:25";
            case 3:
                return shift.equals("MATUTINO") ? "09:45" : "21:15";
            case 4:
                return shift.equals("MATUTINO") ? "10:35" : "22:05";
            case 5:
                return "11:25";
        }

        return "";
    }

    public int retrievePositionFromTime(String time){
        switch (time) {
            case "7:0":
            case "18:30":
                return 0;
            case "7:50":
            case "19:20":
                return 1;
            case "8:40":
            case "20:25":
                return 2;
            case "9:45":
            case "21:15":
                return 3;
            case "10:35":
            case "22:05":
                return 4;
            case "11:25":
                return 5;
        }

        return -1;
    }

    public String retrieveClasstimeDay(int dayPosition){
        switch (dayPosition) {
            case 0:
                return "Segunda-Feira";
            case 1:
                return "Terça-Feira";
            case 2:
                return "Quarta-Feira";
            case 3:
                return "Quinta-Feira";
            case 4:
                return "Sexta-Feira";
            case 5:
            case 6:
                return "Sábado";
        }

        return "";
    }

    public String convertHoursToString(String hours) {
        int hour = Integer.parseInt(hours.split(":")[0]);

        if (hour < 10){
            hours = "0" + hours;
        }

        if (hours.equals("22:5")){
            hours = "22:05";
        }

        if (hours.equals("07:0")){
            hours = "07:00";
        }
        return hours;
    }

    public Timetable processTimetableShifts(Timetable currentTimetable, Shift shift, Row row, int day, String line, boolean saturday) {

        int[] beginningColumnByShifts = new int[] {4, 20, 26};
        int[] endingColumnByShifts = new int[] {11, 26, 28};
        String currentTime = "";
        int beginningcolumn = saturday ? beginningColumnByShifts[2] : shift.equals(Shift.MATUTINO) ? beginningColumnByShifts[0] : beginningColumnByShifts[1];
        int endingColumn = saturday ? endingColumnByShifts[2] : shift.equals(Shift.MATUTINO) ? endingColumnByShifts[0] : endingColumnByShifts[1];

        //Verifying and reading to memory if there is a professor


        for (int test = beginningcolumn; test < endingColumn; test++) {
            Cell cell = row.getCell(test);
            if (cell != null) {
                switch (cell.getCellType()) {
                    case STRING:
                        line = line + returnXorO(cell.getStringCellValue());

                        if(row.getCell(test).getStringCellValue().toUpperCase().equals("X")){
                            int finalTest = test;
                            String finalCurrentTime = currentTime;
                            String finalLine = line;
                            currentTimetable.getClasses().stream()
                                    .filter(t -> t.getDayOfTheWeek()
                                            .equals(saturday ? retrieveClasstimeDay(6) : retrieveClasstimeDay(finalTest - beginningcolumn - 1))
                                            && t.getTime().equals(convertHoursToString(finalLine.trim().split(" ")[0])))
                                    .collect(Collectors.toList()).get(0).setPreferable(true);

                        }
                        break;
                    case BLANK:
                        if (!line.contains("INTERVALO")) {
                            line = line + returnXorO(cell.getStringCellValue());
                        }
                        break;
                    case NUMERIC:
                        line = line + returnXorO(String.valueOf(cell.getDateCellValue().getHours()) + ":" + String.valueOf(cell.getDateCellValue().getMinutes()));
                        currentTime = String.valueOf(cell.getDateCellValue().getHours()) + ":" + String.valueOf(cell.getDateCellValue().getMinutes());
                        break;
                    default:
                        break;
                }

            }
        }
        if (day == 5) {
            day = 0;
        } else {
            day++;
        }
        System.out.println(line);


        return currentTimetable;
    }

}
