import controllers.PrioritizationController;
import controllers.TimetableController;
import entities.Subject;
import entities.Timetable;
import services.PrioritizationServiceImpl;
import services.TimetableServiceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TCC {
    public static void main(String[] args) throws IOException {
        TimetableController timetableController = new TimetableController(new TimetableServiceImpl());
        PrioritizationController prioritizationController = new PrioritizationController(new PrioritizationServiceImpl());
        Map<Object, Object> periodMap;

        //Recuperando Priorização dos períodos MATUTINO e NOTURNO
        periodMap = prioritizationController.readPeriodPrioritization("src/main/resources/priorizacao_horarios.xlsx");

        //Recuperando Matérias e priorizando cada uma
        List<Subject> subjectList = timetableController.readQDA("src/main/resources/QDA 11 05 2023.xlsx", periodMap);
        //Retrieving Preferable Timetable from UEMG XLSX
        List<Timetable> timetableList = timetableController.readTimetableXLSX("src/main/resources/disponibilidade_docentes_2_2023.xlsx");

        String a = "";
    }
}
