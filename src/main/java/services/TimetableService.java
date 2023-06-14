package services;

import entities.Timetable;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface TimetableService {
    Timetable readPreferableTimetableFromExcel(String xlsxPath) throws IOException;
}
