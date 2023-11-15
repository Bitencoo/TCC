package entities;

import enums.Shift;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subject {
    private String className = "A definir";
    private int id;
    private int numbersOfLessons = 0;
    private int period = 0;
    private int prioritization = -1;
    private Shift shift;
    private Professor professor;
}
