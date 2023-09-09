package services;

import entities.Subject;
import entities.Timetable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface TimetableService {
    List<Timetable> readPreferableTimetableFromExcel(String xlsxPath) throws IOException;
    List<Subject> readQDA(String xlsxPath, Map<Object, Object> periodPrioritization) throws IOException;

}
