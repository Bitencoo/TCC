package controllers;

import entities.Subject;
import entities.Timetable;
import enums.Shift;
import services.TimetableService;
import services.TimetableServiceImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableController {

    private TimetableService timetableService;
    public TimetableController(TimetableService timetableService){
        this.timetableService = timetableService;
    }
    public List<Timetable> readTimetableXLSX(String xlsxPath, HashMap<Object, String> professors) {
        try {
            return timetableService.readPreferableTimetableFromExcel(xlsxPath, professors);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Subject> readQDA(String xlsxPath, Map<Object, Object> periodPrioritization, HashMap<Object, String> professors, HashMap<Object, Object> subjectsPrioritizationMap) {
        try {
            List<Subject> subjectList =
                    timetableService.readQDA(xlsxPath, periodPrioritization, professors, subjectsPrioritizationMap);
            return subjectList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Timetable createEmptyProfessorPreferableTimetableMatutino(int timetableIdIncrementor, int professorIdIncrementor, String professorName, Shift shift){
        return timetableService.createEmptyProfessorPreferableTimetableMatutino(timetableIdIncrementor, professorIdIncrementor, professorName, shift);
    }
    public Timetable createEmptyProfessorPreferableTimetableNoturno(int timetableIdIncrementor, int professorIdIncrementor, String professorName, Shift shift){
        return timetableService.createEmptyProfessorPreferableTimetableNoturno(timetableIdIncrementor, professorIdIncrementor, professorName, shift);
    }

    public void exportGeneratedTimetable(List<Timetable> timetables) throws IOException {
        timetableService.exportGeneratedTimetable(timetables);
    }


    }
