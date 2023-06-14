package controllers;

import entities.Timetable;
import services.TimetableService;

import java.io.IOException;

public class TimetableController {

    private TimetableService timetableService;
    public TimetableController(TimetableService timetableService){
        this.timetableService = timetableService;
    }
    public Timetable readTimetableXLSX(String xlsxPath) {
        try {
            timetableService.readPreferableTimetableFromExcel(xlsxPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
