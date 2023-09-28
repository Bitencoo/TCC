package services;

import entities.Subject;
import entities.Timetable;
import enums.Shift;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface TimetableService {
    List<Timetable> readPreferableTimetableFromExcel(String xlsxPath, HashMap<Object, String> professors) throws IOException;

    List<Subject> readQDA(String xlsxPath, Map<Object, Object> periodPrioritization, HashMap<Object, String> professors) throws IOException;

    Timetable createEmptyProfessorPreferableTimetableMatutino(int timetableIdIncrementor, int professorIdIncrementor, String professorName, Shift shift);

    Timetable createEmptyProfessorPreferableTimetableNoturno(int timetableIdIncrementor, int professorIdIncrementor, String professorName, Shift shift);

    void exportGeneratedTimetable(List<Timetable> generatedTimetables) throws IOException;
}
