package controllers;

import entities.Subject;
import entities.Timetable;
import services.TimetableService;
import services.TimetableServiceImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TimetableController {

    private TimetableServiceImpl timetableService;
    public TimetableController(TimetableServiceImpl timetableService){
        this.timetableService = timetableService;
    }
    public List<Timetable> readTimetableXLSX(String xlsxPath) {
        try {
            return timetableService.readPreferableTimetableFromExcel(xlsxPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Subject> readQDA(String xlsxPath, Map<Object, Object> periodPrioritization) {
        try {
            List<Subject> subjectList =
                    timetableService.readQDA(xlsxPath, periodPrioritization);
            return subjectList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
