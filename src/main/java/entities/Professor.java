package entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Professor {
    private String id;
    private String name;
    private int prioritizationLevel = -1;
    private int qtyClasses = 0;
    private boolean onlyUEMGProfessor = false;

}
