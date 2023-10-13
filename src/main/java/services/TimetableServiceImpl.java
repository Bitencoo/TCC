package services;

import entities.ClassTime;
import entities.Professor;
import entities.Subject;
import entities.Timetable;
import enums.Shift;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TimetableServiceImpl implements TimetableService {
    @Override
    public List<Timetable> readPreferableTimetableFromExcel(String xlsxPath, HashMap<Object, String> professors) throws IOException {
        File myFile = new File("src/main/resources/disponibilidade_docentes_2_2023.xlsx");
        FileInputStream fis = new FileInputStream(myFile);

        // Finds the workbook instance for XLSX file
        XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);

        // Return first sheet from the XLSX workbook
        XSSFSheet mySheet = myWorkBook.getSheetAt(0);

        int beginningRow = 2;
        String professor = "";
        int timetableIdIncrementor = 1;
        int day = 0;
        Timetable currentTimetable = null;
        String line = "";

        List<Timetable> timetableList = new ArrayList<>();

        for(int i = beginningRow; i < mySheet.getLastRowNum(); i++) {
            //Estou na linha Agora basta ler das colunas 4 até 11, 20 até 26 e depois 26 até 28
            Row row = mySheet.getRow(i);
            line = "";

            if (row != null) {
                if (row.getCell(0) != null && row.getCell(0).getCellType() != CellType.BLANK && !row.getCell(0).getStringCellValue().equals("NOME")) {
                    professor = row.getCell(0).getStringCellValue().toUpperCase().trim().replace(" ", "");
                    currentTimetable = null;
                    if(professors.containsKey(professor)) {
                        System.out.println(professor);
                        System.out.println("Hora   S T Q Q S S");
                        currentTimetable = createEmptyProfessorPreferableTimetableMatutino(timetableIdIncrementor, Integer.parseInt(professors.get(professor)), professor, Shift.MATUTINO);

                        timetableIdIncrementor++;
                        day = -1;
                        timetableList.add(currentTimetable);
                    }

                }
                if(!Objects.isNull(currentTimetable)) {
                    currentTimetable = processTimetableShifts(currentTimetable, Shift.MATUTINO, row, day, line, false);
                }
            }
        }
        currentTimetable = null;
        for(int i = beginningRow; i < mySheet.getLastRowNum(); i++) {
            Row row = mySheet.getRow(i);
            line = "";
            if (row != null) {
                if (row.getCell(0) != null && row.getCell(0).getCellType() != CellType.BLANK && !row.getCell(0).getStringCellValue().equals("NOME")) {
                    professor = row.getCell(0).getStringCellValue().toUpperCase().trim().replace(" ", "");
                    currentTimetable = null;
                    if(professors.containsKey(professor)){
                        System.out.println(professor);
                        System.out.println("Hora   S T Q Q S S");
                        currentTimetable = createEmptyProfessorPreferableTimetableNoturno(timetableIdIncrementor, Integer.parseInt(professors.get(professor)), professor, Shift.NOTURNO);
                        timetableIdIncrementor++;
                        day = -1;
                        timetableList.add(currentTimetable);
                    }
                }
                if(!Objects.isNull(currentTimetable)) {
                    currentTimetable = processTimetableShifts(currentTimetable, Shift.NOTURNO, row, day, line, false);
                    currentTimetable = processTimetableShifts(currentTimetable, Shift.NOTURNO, row, day, line, true);
                }

            }
        }

        return timetableList;
    }

    //Reads QDA file to get shifts, professors and subjects
    public List<Subject> readQDA(String xlsxPath, Map<Object, Object> periodPrioritization, HashMap<Object, String> professors, HashMap<Object, Object> subjectsPrioritizationMap) throws IOException {
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
        HashMap<String, Professor> professorsMap = new HashMap<>();
        int qtyClasses = 0;
        for(int i = 1; i < mySheet.getLastRowNum(); i++) {
            Row row = mySheet.getRow(i);
            if(row.getCell(0) != null && !row.getCell(0).getCellType().equals(CellType.BLANK)) {
                professorId++;
            }
            if( !row.getCell(14).getStringCellValue().contains("Estágio")
                    && !row.getCell(13).getStringCellValue().isBlank()
            ) {
                if(row.getCell(14).getStringCellValue().contains("Optativa III")
                && row.getCell(1).getStringCellValue().contains("Erasmo")) {
                    continue;
                }
                int period = Integer.parseInt(row.getCell(13).getStringCellValue().split("º")[0].trim());
                boolean isSubjectThisSemester = Objects.isNull(((HashMap<String, String>)periodPrioritization.get("MATUTINO")).get(row.getCell(13).getStringCellValue().replace("º", "").trim()));
                if (!isSubjectThisSemester) {
                    Professor professor = Professor
                            .builder()
                            .name(row.getCell(1).getStringCellValue())
                            .onlyUEMGProfessor(false)
                            .id(String.valueOf(professorId))
                            .build();
                    professors.putIfAbsent(professor.getName().trim().toUpperCase().replace(" ", ""), Integer.toString(professorId));
                    professorsMap.putIfAbsent(professor.getId(), professor);
                    if(row.getCell(10).getStringCellValue().equals("Engenharia da Computação")) {


                        if(!Objects.isNull(row.getCell(8))){
                            switch(row.getCell(8).getCellType()){
                                case NUMERIC:
                                    qtyClasses = (int ) row.getCell(8).getNumericCellValue();
                                    break;
                                case STRING:
                                    if(row.getCell(8).getStringCellValue().contains("+")){
                                        qtyClasses = Integer.parseInt(
                                                row.getCell(8).getStringCellValue().split("\\+")[0]) +
                                                Integer.parseInt(
                                                        row.getCell(8).getStringCellValue().split("\\+")[1]);
                                    } else {
                                        qtyClasses = Integer.parseInt(row.getCell(8).getStringCellValue());
                                    }
                                    break;
                                default:
                                    qtyClasses = 0;
                                    break;
                            }
                        }

                        if(professorsMap.get(professor.getId()).getQtyClasses() == 0){
                            professorsMap
                                    .get(professor.getId()).setQtyClasses(qtyClasses);
                        }

                        int numberOfLessons = (int) (row.getCell(16).getNumericCellValue() + row.getCell(17).getNumericCellValue());
                        String subjectName = row.getCell(14).getStringCellValue().replace("I-", "I").replace("- (", "I").split("\\(")[0].trim();
                        if(row.getCell(14).getStringCellValue().replace("I-", "I").replace("- (", "I").split("\\(")[0].trim().contains("Metodologia Cientifica para Computação")){
                            subjectName = "Metodologia Cientifica para Computação";
                        }

                        if(subjectName.contains("Optativa")){
                            subjectName = subjectName.split("-")[0].trim();
                        }

                        subject = Subject
                                .builder()
                                .className(row.getCell(14).getStringCellValue())
                                .numbersOfLessons(numberOfLessons)
                                .period(period)
                                .shift(row.getCell(12).getStringCellValue().contains("Not") ? Shift.NOTURNO : Shift.MATUTINO)
                                .prioritization(
                                        subjectsPrioritizationMap.containsKey(subjectName) ?
                                                (int) Double.parseDouble(subjectsPrioritizationMap.get(subjectName).toString())
                                                : -1)
                        .professor(
                                        professor
                                )
                                .build();
                    }
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

        subjectList.stream().forEach(subject1 -> {
            subject1.getProfessor().setQtyClasses(professorsMap.get(subject1.getProfessor().getId()).getQtyClasses());
        });
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
        timetable.setQtyPreferableTimes(0);
        timetable.setPoints(0);
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
                        .prioritization(j)
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
        timetable.setQtyPreferableTimes(0);
        timetable.setPoints(0);
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
                        .prioritization(j)
                        .build();
                timetable.getClasses().add(classTime);
            }
        }

        for(int i = 0; i < 6; i++){
            ClassTime classTime = ClassTime
                    .builder()
                    .time(retrieveClasstimeTime(i, Shift.MATUTINO.toString()))
                    .dayOfTheWeek(retrieveClasstimeDay(5))
                    .prioritization(5)
                    .build();
            timetable.getClasses().add(classTime);
        }

        return timetable;
    }

    @Override
    public void exportGeneratedTimetable(List<Timetable> generatedTimetables, Shift shift) throws IOException {
        FileInputStream templateFile = new FileInputStream("src/main/java/generated/timetables/horarios_template.xlsx");
        Workbook workbook = WorkbookFactory.create(templateFile);
        templateFile.close();

        // Create a cell style that wraps text
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true); // Enable text wrapping
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        String cellValue = workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue();
        workbook.getSheetAt(0).getRow(1).getCell(0)
                .setCellValue(
                        cellValue.replace("[INDICAR SEMESTRE E ANO]", "1 SEMESTRE DE 2023 ")
                                .replace("[INDICAR TURNO]", shift.toString())
                );
        // Create a sheet within the workbook
        Sheet sheet = workbook.getSheetAt(0);

        // Define the file path where the Excel file will be saved
        String filePath = "src/main/java/generated/timetables/horarios_" + shift.toString() + ".xlsx";

        HashMap<Object, Integer> dayColumn = new HashMap<>();
        dayColumn.put("Segunda-Feira", 1);
        dayColumn.put("Terça-Feira", 2);
        dayColumn.put("Quarta-Feira", 3);
        dayColumn.put("Quinta-Feira", 4);
        dayColumn.put("Sexta-Feira", 5);
        dayColumn.put("Sábado", 7);

        HashMap<Object, Integer> hourRow = new HashMap<>();
        hourRow.put("07:00", 6);
        hourRow.put("07:50", 7);
        hourRow.put("08:40", 8);
        hourRow.put("09:45", 10);
        hourRow.put("10:35", 11);
        hourRow.put("11:25", 12);
        hourRow.put("18:30", 6);
        hourRow.put("19:20", 7);
        hourRow.put("20:25", 8);
        hourRow.put("21:15", 10);
        hourRow.put("22:05", 11);

        HashMap<Object, String> periods = new HashMap<>();
        periods.put("1", "1º");
        periods.put("3", "3º");
        periods.put("5", "5º");
        periods.put("7", "7º");
        periods.put("9", "9º");
        int period = 0;
        int rowLine = 4;
        int columnLine = 0;
        int countPeriod = 0;
        Row row = sheet.getRow(rowLine);

        row.getCell(columnLine).setCellValue(periods.get(Integer.toString(period) + " " + row.getCell(columnLine).getStringCellValue()));

        rowLine = 6;
        columnLine++;
        for(Timetable timetable: generatedTimetables){
            for(period = 1; period <= 9; period = period + 2){
                int finalPeriod = period;
                for(ClassTime c: timetable.getClasses().stream().filter(
                        classTime -> !Objects.isNull(classTime.getSubject())
                                && classTime.getSubject().getPeriod() == finalPeriod
                                && classTime.getSubject().getShift().equals(shift)).collect(Collectors.toList())) {
                    columnLine = dayColumn.get(c.getDayOfTheWeek());

                    rowLine = hourRow.get(c.getTime()) + (countPeriod * 16);
                    sheet.getRow(rowLine).getCell(columnLine).setCellValue(c.getSubject().getClassName() + "\n" +
                            timetable.getProfessor().getName());
                    // Auto-adjust the row height
                    sheet.getRow(rowLine).setHeight((short)-1);
                    // Autosize the column width
                    sheet.autoSizeColumn(columnLine);
                }
                countPeriod++;
            }
            countPeriod = 0;
        }

        // Write the workbook data to a file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }

        // Close the workbook
        workbook.close();
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
                            ClassTime ct = currentTimetable.getClasses().stream()
                                    .filter(t -> t.getDayOfTheWeek()
                                            .equals(saturday ? retrieveClasstimeDay(6) : retrieveClasstimeDay(finalTest - beginningcolumn - 1))
                                            && t.getTime().equals(convertHoursToString(finalLine.trim().split(" ")[0])))
                                    .collect(Collectors.toList()).get(0);
                            ct.setPreferable(true);
                            currentTimetable.setQtyPreferableTimes(currentTimetable.getQtyPreferableTimes() + 1);

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
