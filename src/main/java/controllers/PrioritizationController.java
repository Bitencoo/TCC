package controllers;

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
}
