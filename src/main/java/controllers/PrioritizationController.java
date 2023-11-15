package controllers;

import entities.ProfessorExclusivity;
import services.PrioritizationServiceImpl;

import java.io.IOException;
import java.util.Map;

public class PrioritizationController {
    private PrioritizationServiceImpl prioritizationService;

    public PrioritizationController(PrioritizationServiceImpl prioritizationService) {
        this.prioritizationService = prioritizationService;
    }

    public Map<Object, Object> readPeriodPrioritization(String xlsxPath) throws IOException {
        return prioritizationService.readPeriodPrioritization(xlsxPath);
    }

    public Map<Object, Object> readSubjectsPrioritization(String xlsxPath) throws IOException {
        return prioritizationService.readSubjectsPrioritization(xlsxPath);
    }

    public Map<String, ProfessorExclusivity> readProfessorsExclusivity(String xlsxPath) throws IOException {
        return prioritizationService.readProfessorsExclusivity(xlsxPath);
    }
}
