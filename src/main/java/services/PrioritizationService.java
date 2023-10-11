package services;

import java.io.IOException;
import java.util.HashMap;

public interface PrioritizationService {
    void applyPrioritization();
    HashMap<Object, Object> readPeriodPrioritization(String xlsxPath) throws IOException;
    HashMap<Object, Object> readSubjectsPrioritization(String xlsxPath) throws IOException;
}
