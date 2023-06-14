import controllers.TimetableController;
import entities.Timetable;
import services.TimetableServiceImpl;

import java.io.IOException;

public class TCC {
    public static void main(String[] args) throws IOException {
        TimetableController timetableController = new TimetableController(new TimetableServiceImpl());

        //Retrieving Preferable Timetable from UEMG XLSX
        Timetable preferableTimetable = timetableController.readTimetableXLSX("src/main/resources/disponibilidade_docentes_2_2023.xlsx");

    }
}
