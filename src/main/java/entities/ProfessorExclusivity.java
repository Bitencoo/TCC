package entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfessorExclusivity {
    private boolean onlyUemgProfessor;
    private boolean exclusiveComputerEngineering;
}
