package entities;

import enums.Shift;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Timetable {
    private int id;
    private int professorId;
    private int qtyPreferableTimes;
    private Shift shift;
    private Professor professor;
    private List<ClassTime> classes;
}
