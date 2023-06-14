package entities;

import lombok.Data;

import java.util.List;

@Data
public class Timetable {
    private int id;
    private int professorId;
    private String shift;
    private Professor professor;
    private List<ClassTime> classes;
}
