import controllers.PrioritizationController;
import controllers.TimetableController;
import entities.*;
import enums.Shift;
import services.PrioritizationServiceImpl;
import services.TimetableServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;



public class TCC {
    public static void main(String[] args) throws IOException {
        TimetableController timetableController = new TimetableController(new TimetableServiceImpl());
        PrioritizationController prioritizationController = new PrioritizationController(new PrioritizationServiceImpl());
        HashMap<Object, Object> periodMap;
        HashMap<Object, String> professors = new HashMap<>();
        HashMap<Object, Object> subjectsPrioritizationMap = new HashMap<>();
        AtomicReference<HashMap<String, Professor>> professorHashMapRef = new AtomicReference<>(new HashMap<>());
        List<Timetable> timetableList = new ArrayList<>();
        List<ClassTime> allocatedClasses = new ArrayList<>();
        Timetable allocatedTimetable = new Timetable();
        HashMap<String, ProfessorExclusivity> professorsExclusivity = (HashMap<String, ProfessorExclusivity>) prioritizationController.readProfessorsExclusivity("src/main/resources/exclusividade_professores.xlsx");

        periodMap = (HashMap<Object, Object>) prioritizationController.readPeriodPrioritization("src/main/resources/priorizacao_horarios.xlsx");

        subjectsPrioritizationMap = (HashMap<Object, Object>) prioritizationController.readSubjectsPrioritization("src/main/resources/priorizacao_materias.xlsx");
        //Recuperando Matérias e priorizando cada uma
        List<Subject> subjectList = timetableController.readQDA("src/main/resources/QDA 11 05 2023.xlsx", periodMap, professors, subjectsPrioritizationMap);
        //Recupernado Horários Preferidos dos Professores
        List<Timetable> preferableTimetableList = timetableController.readTimetableXLSX("src/main/resources/disponibilidade_docentes_2_2023.xlsx", professors);

        // Criando a grade de horários vazia para os turnos MATUTINO e NOTURNO aplicando as restrições (5) (6) e (7)
        // Aos sábados, tanto para o turno matutino quanto para o noturno, podem ser alocados no máximo 6 horários de aula. (5)
        // Para o turno matutino, podem ser alocados 6 horários de aula por dia durante a semana. (6)
        // Para o turno noturno, podem ser alocados 5 horários de aula por noite durante a semana, excluindo-se o sábado. (7)
        for (Timetable timetable : preferableTimetableList) {
            timetableList.add(
                    timetable.getShift().equals(Shift.MATUTINO)
                            ? timetableController.createEmptyProfessorPreferableTimetableMatutino(timetable.getId(), timetable.getProfessorId(), timetable.getProfessor().getName(), timetable.getShift())
                            : timetableController.createEmptyProfessorPreferableTimetableNoturno(timetable.getId(), timetable.getProfessorId(), timetable.getProfessor().getName(), timetable.getShift()));
        }

        preferableTimetableList.add(
                timetableController.createEmptyProfessorPreferableTimetableMatutino(1000, 1000, "ESTÁGIO", Shift.MATUTINO));
        preferableTimetableList.add(
                timetableController.createEmptyProfessorPreferableTimetableMatutino(1000, 1000, "ESTÁGIO", Shift.NOTURNO));

        preferableTimetableList.stream().filter(timetable ->
                        timetable.getProfessor().getId().equals("1000")).collect(Collectors.toList())
                .stream().forEach(professor -> {
                    professor.getProfessor().setId("ESTAGIO");
                });
        subjectList.stream().forEach(
                subject ->
                {
                    professorHashMapRef.getAndUpdate(
                            map -> {
                                map.putIfAbsent(
                                        subject.getProfessor().getId() + "-" + subject.getShift().toString(),
                                        subject.getProfessor()
                                );
                                return map;
                            }
                    );
                }
        );

        preferableTimetableList.stream().forEach(
                timetable -> {
                    professorHashMapRef.getAndUpdate(
                            map -> {
                                if (map.containsKey(timetable.getProfessor().getId() + "-" + timetable.getShift().toString())) {
                                    map.get(timetable.getProfessor().getId() + "-" + timetable.getShift().toString())
                                            .setQtyPreferableTimes(timetable.getQtyPreferableTimes());
                                }
                                return map;
                            }
                    );
                }
        );

        subjectList.stream().forEach(
                subject -> {
                    subject.setProfessor(
                            professorHashMapRef.get().get(subject.getProfessor().getId() + "-" + subject.getShift().toString())
                    );
                }
        );


        HashMap<String, Professor> professorHashMap = rankProfessors(professorHashMapRef);

        preferableTimetableList.stream().forEach(
                timetable -> {
                    timetable.setProfessor(
                            professorHashMap.get(timetable.getProfessor().getId() + "-" + timetable.getShift().toString())
                    );
                }
        );

        for(Professor p: professorHashMapRef.get().values()) {
           if(!Objects.isNull(professorsExclusivity.get(p.getName().trim()))) {
               p.setOnlyUEMGProfessor(((ProfessorExclusivity) professorsExclusivity.get(p.getName().trim())).isOnlyUemgProfessor());
               p.setExclusiveToComputerEngineering(((ProfessorExclusivity) professorsExclusivity.get(p.getName().trim())).isExclusiveComputerEngineering());
           }
        }
        preferableTimetableList = preferableTimetableList.stream().filter(timetable -> !Objects.isNull(timetable.getProfessor())).collect(Collectors.toList());
        HashMap<String, Integer> iterationsToAchieveBestValue = allocateClassTimes(subjectList, periodMap, preferableTimetableList);
//        allocatedClasses = allocateClassTimes(subjectList, periodMap, preferableTimetableList);
        allocatedTimetable.setClasses(allocatedClasses);

        timetableController.exportGeneratedTimetable(preferableTimetableList, Shift.MATUTINO, iterationsToAchieveBestValue, ((HashMap<String, String>) periodMap.get("MATUTINO")).containsKey("2") ? 2 : 1);
        timetableController.exportGeneratedTimetable(preferableTimetableList, Shift.NOTURNO, iterationsToAchieveBestValue, ((HashMap<String, String>) periodMap.get("MATUTINO")).containsKey("2") ? 2 : 1);

//        timetableController.exportGeneratedTimetable(preferableTimetableList, Shift.MATUTINO, null, ((HashMap<String, String>) periodMap.get("MATUTINO")).containsKey("2") ? 2 : 1);
//        timetableController.exportGeneratedTimetable(preferableTimetableList, Shift.NOTURNO, null, ((HashMap<String, String>) periodMap.get("MATUTINO")).containsKey("2") ? 2 : 1);
    }

//    public static List<ClassTime> allocateClassTimes(List<Subject> subjectList, HashMap<Object, Object> periodMap, List<Timetable> preferableTimetable) {
    public static HashMap<String, Integer> allocateClassTimes(List<Subject> subjectList, HashMap<Object, Object> periodMap, List<Timetable> preferableTimetable) {
        Comparator<ClassTime> timetableComparator
                = (ct1, ct2) -> (int) ct1.getPrioritization() - ct2.getPrioritization();
        preferableTimetable.stream().forEach(timetable -> timetable.getClasses().sort(timetableComparator));
        // i = 0 TURNO MATUTINO
        // i = 1 TURNO NOTURNO
        List<Subject> currentSubjectList = new ArrayList<>();
        List<Subject> internshipsubjectList = subjectList.stream().filter(
                subject -> subject.getClassName().contains("Estágio")).collect(Collectors.toList());
        subjectList = subjectList.stream().filter(subject -> !subject.getClassName().contains("Estágio")).collect(Collectors.toList());
        Timetable currentTimetableList = new Timetable();
        String day = "Segunda-Feira";
        List<ClassTime> allocatedClasses = new ArrayList<>();
        LinkedHashMap<Object, Integer> daysAvailableClasses = new LinkedHashMap<>();
        HashMap<String, Shift> shifts = new HashMap<>();
        shifts = populateShifts(shifts);
        for (String st : shifts.keySet()) {
            daysAvailableClasses = emptyDaysAvailableClasses(daysAvailableClasses, shifts.get(st));
            for (String s : ((HashMap<String, String>) periodMap.get(shifts.get(st).toString())).keySet().stream().collect(Collectors.toList())) {
                HashMap<String, Shift> finalShifts = shifts;
                currentSubjectList = subjectList.stream().filter(sub -> sub.getPeriod() == Integer.parseInt(s) && sub.getShift().equals(finalShifts.get(st))).collect(Collectors.toList());
                currentSubjectList = allocationRank(currentSubjectList, preferableTimetable);
                List<ClassTime> classTimeListToAllocate = new ArrayList<>();

                for (Subject subject : currentSubjectList) {
                    HashMap<String, Shift> finalShifts1 = shifts;
                    currentTimetableList = preferableTimetable.stream().filter(timetable -> !Objects.isNull(timetable.getProfessor()) && timetable.getShift().equals(finalShifts1.get(st)) && timetable.getProfessor().getId().equals(subject.getProfessor().getId())).collect(Collectors.toList()).get(0);
                    fixPossibleWrongTime(currentTimetableList, shifts.get(st));
                    defineClassesToAllocate(
                            daysAvailableClasses,
                            classTimeListToAllocate,
                            currentTimetableList,
                            preferableTimetable,
                            subject,
                            subject.getNumbersOfLessons()
                    );
                    allocatedClasses.addAll(classTimeListToAllocate);
                    classTimeListToAllocate.clear();
                }
            }

        }

        List<ClassTime> originalClassTime = new ArrayList<>();
        for(ClassTime c : allocatedClasses){
            ClassTime newClassTime = ClassTime
                    .builder()
                    .dayOfTheWeek(c.getDayOfTheWeek())
                    .time(c.getTime())
                    .subject(
                            Subject.builder()
                                    .id(c.getSubject().getId())
                                    .className(c.getSubject().getClassName())
                                    .professor(c.getSubject().getProfessor())
                                    .numbersOfLessons(c.getSubject().getNumbersOfLessons())
                                    .shift(c.getSubject().getShift())
                                    .prioritization(c.getSubject().getPrioritization())
                                    .period(c.getSubject().getPeriod())
                                    .build()
                    )
                    .preferable(c.isPreferable())
                    .prioritization(c.getPrioritization())
                    .build();
            originalClassTime.add(newClassTime);
        }

        HashMap<String, Integer> iterationsToAchieveBestValue = new HashMap<>();
        for (String st : shifts.keySet()) {
            HashMap<String, Integer> iterations = new HashMap<>();
            iterations = iterationProcess(
                    originalClassTime.stream().filter(classTime -> classTime.getSubject().getShift().toString().equals(st)).collect(Collectors.toList()), periodMap, shifts.get(st),
                    preferableTimetable.stream().filter(timetable -> timetable.getShift().toString().equals(st.toString())).collect(Collectors.toList()));
            for (String s: iterations.keySet()){
                iterationsToAchieveBestValue.put(s, iterations.get(s));
            }
        }

        for (String st : shifts.keySet()) {
            for (String s : ((HashMap<String, String>) periodMap.get(shifts.get(st).toString())).keySet().stream().collect(Collectors.toList())) {
                HashMap<String, Shift> finalShifts = shifts;
                currentSubjectList = internshipsubjectList.stream().filter(sub -> sub.getPeriod() == Integer.parseInt(s) && sub.getShift().equals(finalShifts.get(st))).collect(Collectors.toList());
                List<ClassTime> classTimeListToAllocate = new ArrayList<>();

                for (Subject subject : currentSubjectList) {
                    HashMap<String, Shift> finalShifts1 = shifts;
                    currentTimetableList = preferableTimetable.stream().filter(
                            timetable -> !Objects.isNull(timetable.getProfessor())
                                    && timetable.getShift().equals(finalShifts1.get(st))
                                    && timetable.getProfessor().getId().equals(subject.getProfessor().getId())).collect(Collectors.toList()).get(0);
                    fixPossibleWrongTime(currentTimetableList, shifts.get(st));
                    defineClassesToAllocate(
                            daysAvailableClasses,
                            classTimeListToAllocate,
                            currentTimetableList,
                            preferableTimetable,
                            subject,
                            subject.getNumbersOfLessons()
                    );
                    allocatedClasses.addAll(classTimeListToAllocate);
                    classTimeListToAllocate.clear();
                }
            }
        }
//        return allocatedClasses;
        return iterationsToAchieveBestValue;
    }

    public static int allocateClasses(
            List<ClassTime> classTimeListToAllocate,
            Timetable currentTimetable,
            List<Timetable> preferableTimetable,
            Subject subject,
            int qtyClassesToAllocate,
            int currentClassTimeLength,
            String day) {
        int allocatedClasses = qtyClassesToAllocate;
        String currentTime = "";
        List<Integer> daysToAllocate = new ArrayList<>();

        while (allocatedClasses != 0) {
            currentTime = retrieveClasstimeFromAvailableClasses(currentClassTimeLength, subject.getShift().toString(), day);

            // Verifica se não foi alocada nenhuma aula e se o dia atual corresponde ao do horário
            String finalCurrentTime = currentTime;
            String finalDay = day;

            // 0 indica que ainda não foi alocado horário para o professor
            int validateProfessorFree = preferableTimetable.stream().filter(timetable ->
                    timetable.getProfessor().getId().equals(subject.getProfessor().getId())
                            && timetable.getShift().equals(subject.getShift())
                            && timetable.getClasses().stream().anyMatch(
                            classTime ->
                                    classTime.getDayOfTheWeek().equals(finalDay)
                                            && classTime.getTime().equals(finalCurrentTime)
                                            && !Objects.isNull(classTime.getSubject())
                    )
            ).collect(Collectors.toList()).size();

            int validateTimetableFree = preferableTimetable.stream().filter(timetable ->
                    timetable.getShift().equals(subject.getShift()) &&
                            timetable.getClasses().stream().anyMatch(
                                    classTime ->
                                            classTime.getDayOfTheWeek().equals(finalDay)
                                                    && classTime.getTime().equals(finalCurrentTime)
                                                    && !Objects.isNull(classTime.getSubject())
                                                    && classTime.getSubject().getPeriod() == subject.getPeriod()
                            )
            ).collect(Collectors.toList()).size();

            if (validateProfessorFree == 0 && validateTimetableFree == 0) {
                currentClassTimeLength--;
                allocatedClasses--;
                ClassTime cTime = currentTimetable.getClasses().stream().filter(classTime ->
                        classTime.getTime().equals(finalCurrentTime)
                                && classTime.getDayOfTheWeek().equals(finalDay)
                ).collect(Collectors.toList()).get(0);
                cTime.setSubject(subject);
                if (cTime.isPreferable()) {
                    currentTimetable.setPoints(currentTimetable.getPoints() + 1);
                }
                classTimeListToAllocate.add(cTime);
            } else {
                day = retrieveNextDay(day);
            }
        }

        return qtyClassesToAllocate - allocatedClasses;
    }

    public static LinkedHashMap<Object, Integer> emptyDaysAvailableClasses(LinkedHashMap<Object, Integer> daysAvailableClasses, Shift shift) {
        for (int i = 1; i <= 10; i++) {
            daysAvailableClasses.put(i + "-Segunda-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Terça-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Quarta-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Quinta-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Sexta-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Sábado", 6);

        }
        return daysAvailableClasses;
    }

    public static HashMap<String, Shift> populateShifts(HashMap<String, Shift> shiftsMap) {
        shiftsMap.putIfAbsent("MATUTINO", Shift.MATUTINO);
        shiftsMap.putIfAbsent("NOTURNO", Shift.NOTURNO);
        return shiftsMap;
    }


    // Cria o rank de alocação, onde os professores com mais horários disponíveis serão os últimos a serem alocados
    // Falta colocar a condição de 0, nesse caso, será o último professor a ser alocado, pois o mesmo não preencheu
    // a planilha.
    public static List<Subject> allocationRank(List<Subject> subjectList, List<Timetable> timetableList) {
        List<Subject> sortedSubjects = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            int finalI = i;
            List<Subject> newSubjectList = subjectList.stream()
                    .filter(subject -> subject.getPrioritization() == finalI).collect(Collectors.toList());
            Comparator<Subject> subjectComparator = Comparator
                    .comparingInt((Subject subject) -> subject.getProfessor().getPrioritizationLevel())
                    .thenComparingInt((Subject subject) -> subject.getProfessor().getPrioritizationByEducation());
            newSubjectList.sort(subjectComparator);
            sortedSubjects.addAll(newSubjectList);
        }

        return sortedSubjects;
    }

    public static void defineClassesToAllocate(
            LinkedHashMap<Object, Integer> daysAvailableClasses,
            List<ClassTime> classTimeListToAllocate,
            Timetable currentTimetableList,
            List<Timetable> preferableTimetable,
            Subject subject,
            int qtyClasses
    ) {
        String day = findBestDayToAllocate(daysAvailableClasses, qtyClasses, subject.getPeriod(), currentTimetableList);
        if (Objects.isNull(day)) {
            day = Integer.toString(subject.getPeriod()) + "-Segunda-Feira";
        }

        int qtyMissingClassesToAllocate = qtyClasses;
        int allocatedClasses = -1;
        int availableClasses = daysAvailableClasses.get(day);
        int allocatedForTheDay = 0;

        while (qtyMissingClassesToAllocate > 0) {
            //Os horários podem ser alocados em sequência no determinado dia sem problemas
            if (qtyClasses <= availableClasses) {
                //aloca tudo
                allocatedClasses = allocateClasses(
                        classTimeListToAllocate,
                        currentTimetableList,
                        preferableTimetable,
                        subject,
                        qtyClasses,
                        availableClasses,
                        day.split(Integer.toString(subject.getPeriod()) + "-")[1]);
                daysAvailableClasses.put(day, daysAvailableClasses.get(day) - allocatedClasses);
                return;
            }

            while (qtyMissingClassesToAllocate != 0) {
                //Quantidade de horários para ser alocados é menor que a quantidade disponível no dia
                //Dividir a quantidade por 2 até ser possível alocar a matéria em mais de 1 dia
                if (allocatedForTheDay == 0) {
                    qtyClasses = (qtyClasses / 2) + qtyClasses % 2;
                }

                if (qtyClasses == 0) {
                    qtyClasses = qtyMissingClassesToAllocate;
                }

                if (allocatedForTheDay == 1) {
                    qtyClasses = qtyMissingClassesToAllocate;
                }

                if (allocatedClasses != 0) {
                    day = findBestDayToAllocate(daysAvailableClasses, qtyClasses, subject.getPeriod(), currentTimetableList);
                }

                if (Objects.isNull(day)) {
                    allocatedForTheDay = 0;
                    continue;
                }

                if (!Objects.isNull(day)) {
                    availableClasses = daysAvailableClasses.get(day);
                    if (qtyClasses % 2 != 0) {
                        if (qtyClasses <= availableClasses) {
                            allocatedClasses = allocateClasses(
                                    classTimeListToAllocate,
                                    currentTimetableList,
                                    preferableTimetable,
                                    subject,
                                    qtyClasses,
                                    availableClasses,
                                    day.split(Integer.toString(subject.getPeriod()) + "-")[1]);
                        } else if (qtyClasses - 1 <= availableClasses) {
                            allocatedClasses = allocateClasses(
                                    classTimeListToAllocate,
                                    currentTimetableList,
                                    preferableTimetable,
                                    subject,
                                    qtyClasses - 1,
                                    availableClasses,
                                    day.split(Integer.toString(subject.getPeriod()) + "-")[1]);
                        }
                    } else {
                        if (qtyClasses <= availableClasses) {
                            allocatedClasses = allocateClasses(
                                    classTimeListToAllocate,
                                    currentTimetableList,
                                    preferableTimetable,
                                    subject,
                                    qtyClasses,
                                    availableClasses,
                                    day.split(Integer.toString(subject.getPeriod()) + "-")[1]);
                        }
                    }
                    if (allocatedClasses == 0) {
                        day = findBestDayToAllocate(daysAvailableClasses, qtyClasses, day, subject.getPeriod(), currentTimetableList);
                    } else {
                        qtyMissingClassesToAllocate = qtyMissingClassesToAllocate - allocatedClasses;
                        daysAvailableClasses.put(day, daysAvailableClasses.get(day) - allocatedClasses);

                        if (allocatedForTheDay == 1) {
                            qtyClasses = 0;
                            allocatedForTheDay = 0;
                        } else {
                            allocatedForTheDay = 1;
                        }

                    }
                }
            }
        }
        return;
    }

    public static String returnDayToAllocate(
            int qtyClasses, LinkedHashMap<Object, Integer> daysAvailableClasses, Subject subject) {
        for (Object st : daysAvailableClasses.keySet()) {
            if (daysAvailableClasses.get(st) >= qtyClasses
                    && st.toString().split("-")[0].equals(Integer.toString(subject.getPeriod()))) {
                return st.toString();
            }
        }

        return null;
    }

    //Encontra o melhor dia para alocar a matéria, caso não encontre, quer dizer que a matéria será dividida em mais
    //de 1 dia.
    public static String findBestDayToAllocate(
            LinkedHashMap<Object, Integer> daysAvailableClasses,
            int qtyClassesToAllocate,
            int period,
            Timetable currentTimetable) {
        for (Object o : daysAvailableClasses.keySet()) {
            if (qtyClassesToAllocate <= daysAvailableClasses.get(o)
                    && o.toString().split("-")[0].equals(Integer.toString(period))
                    && currentTimetable.getClasses().stream().filter(classTime ->
                        classTime.getDayOfTheWeek().split("-")[0].equals(o.toString().split("-")[1])
                        && !Objects.isNull(classTime.getSubject())).collect(Collectors.toList()).size() < daysAvailableClasses.get(o)) {
                return o.toString();
            }
        }
        return null;
    }

    public static String findBestDayToAllocate(
            LinkedHashMap<Object, Integer> daysAvailableClasses,
            int qtyClassesToAllocate,
            String dayToAvoid,
            int period,
            Timetable currentTimetable) {
        for (Object o : daysAvailableClasses.keySet()) {
            if (qtyClassesToAllocate <= daysAvailableClasses.get(o)
                && !o.toString().equals(dayToAvoid)
                && o.toString().split("-")[0].equals(Integer.toString(period))
                && currentTimetable.getClasses().stream().filter(classTime ->
                    classTime.getDayOfTheWeek().equals(o.toString().split("-")[1])
                    && !Objects.isNull(classTime.getSubject())).collect(Collectors.toList()).size() < daysAvailableClasses.get(o)) {
                return o.toString();
            }
        }
        return null;
    }


    public static String retrieveClasstimeFromAvailableClasses(int option, String shift, String day) {

        if (day.equals("Sábado")) {
            switch (option) {
                case 1:
                    return "11:25";
                case 2:
                    return "10:35";
                case 3:
                    return "09:45";
                case 4:
                    return "08:40";
                case 5:
                    return "07:50";
                case 6:
                    return "07:00";
            }
        } else {
            switch (option) {
                case 1:
                    return shift.equals("MATUTINO") ? "11:25" : "22:05";
                case 2:
                    return shift.equals("MATUTINO") ? "10:35" : "21:15";
                case 3:
                    return shift.equals("MATUTINO") ? "09:45" : "20:25";
                case 4:
                    return shift.equals("MATUTINO") ? "08:40" : "19:20";
                case 5:
                    return shift.equals("MATUTINO") ? "07:50" : "18:30";
                case 6:
                    return "07:00";
            }
        }

        return "";
    }

    public static String retrieveNextDay(String day) {
        switch (day) {
            case "Segunda-Feira":
                return "Terça-Feira";
            case "Terça-Feira":
                return "Quarta-Feira";
            case "Quarta-Feira":
                return "Quinta-Feira";
            case "Quinta-Feira":
                return "Sexta-Feira";
            case "Sexta-Feira":
                return "Sábado";
            case "Sábado":
                return "Segunda-Feira";
        }

        return "";
    }

    public static HashMap<String, Professor> rankProfessors(AtomicReference<HashMap<String, Professor>> professorHashMap) {
        int maxQtyClasses = 0;
        for (String s : professorHashMap.get().keySet()) {

            //Professores que nao marcaram nenhuma disposicao sao os ultimos a serem alocados
            if (professorHashMap.get().get(s).getQtyPreferableTimes() == 0) {
                professorHashMap.get().get(s).setPrioritizationLevel(101);
                continue;
            }

            if (s.contains("NOTURNO")) {
                maxQtyClasses = 30;
            } else {
                maxQtyClasses = 36;
            }

            if(professorHashMap.get().get(s).getName().contains("Vânia")) {
                String b = "";
            }

            //Professores que marcaram todas os dias da semana serao os ultimos a serem alocados
            if (professorHashMap.get().get(s).getQtyPreferableTimes() >= maxQtyClasses) {
                professorHashMap.get().get(s).setPrioritizationLevel(100);
                continue;
            }

            //Professores marcaram a quantidade minima de aula e sao priorizados corretamente
            if (professorHashMap.get().get(s).getQtyPreferableTimes() >= professorHashMap.get().get(s).getQtyClasses()) {
                professorHashMap.get().get(s).setPrioritizationLevel(
                        professorHashMap.get().get(s).getQtyPreferableTimes() - professorHashMap.get().get(s).getQtyClasses());
                continue;
            }

            //Professores marcaram menos do que quantidade minima de aula
            if (professorHashMap.get().get(s).getQtyPreferableTimes() >= professorHashMap.get().get(s).getQtyClasses()) {
                professorHashMap.get().get(s).setPrioritizationLevel(
                        100 - professorHashMap.get().get(s).getQtyPreferableTimes());

            }

        }

        for (String s : professorHashMap.get().keySet()) {

            //Professores que lecionam em outras instituicoes além da UEMG
            if (!professorHashMap.get().get(s).isOnlyUEMGProfessor()) {
                professorHashMap.get().get(s).setPrioritizationByEducation(0);
                continue;
            }

            //Professores que lecionam em outros cursos da UEMG
            if (!professorHashMap.get().get(s).isExclusiveToComputerEngineering()) {
                professorHashMap.get().get(s).setPrioritizationByEducation(1);
                continue;
            }

            //Professores possuem apenas 1 cargo e sao exclusivos da UEMG
            if (professorHashMap.get().get(s).isOnlyUEMGProfessor() && professorHashMap.get().get(s).isOnlyOneCharge()) {
                professorHashMap.get().get(s).setPrioritizationByEducation(2);
                continue;
            }

            //Professores possuem mais de 1 cargo e sao exclusivos da UEMG
            if (professorHashMap.get().get(s).isOnlyUEMGProfessor() && !professorHashMap.get().get(s).isOnlyOneCharge()) {
                professorHashMap.get().get(s).setPrioritizationByEducation(3);
            }
        }
        return professorHashMap.get();
    }

    public static HashMap<String, Integer> iterationProcess(List<ClassTime> allocatedClasses, HashMap<Object, Object> periodMap, Shift shift, List<Timetable> preferableTimetableList) {
        int columns = 6;
        int rows = 6;
        HashMap<String, Boolean> preferableTimesMap = new HashMap<>();
        HashMap<String, ClassTime> classTimeHashMap = new HashMap<>();
        HashMap<String, Subject> professorSubject = new HashMap<>();
        List<List<Integer>> periodsCombinations;
        int noturnoPoints = 0;
        int matutinoPoints = 0;

        for(ClassTime c: allocatedClasses) {
            classTimeHashMap.put(Integer.toString(c.getSubject().getPeriod()) + "_" + shift.toString() + "_" + c.getDayOfTheWeek() + "_" + c.getTime(), c);
        }

        periodsCombinations = possiblePeriods(periodMap, shift);
        retrievePreferableTimesMap(preferableTimesMap, preferableTimetableList);

//        List<Integer> teste = new ArrayList<>();
//        teste = periodsCombinations.get(0);
//        periodsCombinations.clear();
//        periodsCombinations.add(teste);
        HashMap<String, Integer> iterationsToAchieveBestValue = new HashMap<>();
        HashMap<String, Integer> bestIterationsToAchieveBestValue = new HashMap<>();
        int iterationCounter = 1;
        int iterationCombinations = 0;
        int bestPoints = 0;
        String periodAndShift = "";
        PossibleCombinationsString bestPossibleCombination = new PossibleCombinationsString();
        for(List<Integer> p: periodsCombinations){
            PossibleCombinationsString possibleCombinations = new PossibleCombinationsString();
            iterationCounter = 1;
            for(Integer period: p){
                periodAndShift = period + "_" + shift.toString();
                List<ClassTime> currentClassTimes = allocatedClasses.stream().filter(classTime ->
                        classTime.getSubject().getPeriod() == period && classTime.getSubject()
                                .getShift().equals(shift)).collect(Collectors.toList());

                String[][] matrix = generateSameNumberMatrix(rows, columns, currentClassTimes, professorSubject);

                possibleCombinations.setColumns(rows);
                possibleCombinations.setRows(columns);
                possibleCombinations.setOriginalMatrix(matrix);
                possibleCombinations.setPreferableTimesMap(preferableTimesMap);
                possibleCombinations.combinations(period + "_" + shift.toString(), iterationCounter);

//                timetableHashMap = addTimetablesToHashMap(preferableTimetableList);
                iterationsToAchieveBestValue.put(period + "_" + shift.toString(), possibleCombinations.getIterationsToAchieveBestValue().get(period + "_" + shift.toString()));
                iterationCounter = 1;
            }

            if (possibleCombinations.getTotalPoints() > bestPoints) {
                bestPoints = possibleCombinations.getTotalPoints();
                bestPossibleCombination = possibleCombinations;
                for(String s: iterationsToAchieveBestValue.keySet()) {
                    bestIterationsToAchieveBestValue.put(s, (iterationCombinations * 720) + iterationsToAchieveBestValue.get(s));
                }
            }

            HashMap<String, String[][]> newAllocatedClasses = possibleCombinations.getBestAllocatedClasses();

            HashMap<String, ClassTime> newAllocatedClassTime = new HashMap<>();

            if(shift.toString().equals("MATUTINO")) {
                if(matutinoPoints < possibleCombinations.getTotalPoints()) {
                    for(Timetable t : preferableTimetableList) {
                        for(ClassTime c : t.getClasses()) {
                            c.setSubject(null);
                        }
                    }

                    for (String st : newAllocatedClasses.keySet()) {
                        for (int i = 0; i < rows; i++) {
                            for (int j = 0; j < columns; j++) {
                                if (!newAllocatedClasses.get(st)[i][j].equals("-1") ) {
                                    String key = newAllocatedClasses.get(st)[i][j] + "_" + possibleCombinations.retrieveClasstimeDay(j) + "_" + possibleCombinations.retrieveClasstimeTime(i, shift.toString(), possibleCombinations.retrieveClasstimeDay(j));
                                    ClassTime classTime = ClassTime
                                            .builder()
                                            .subject(professorSubject.get(st + "_" + newAllocatedClasses.get(st)[i][j]))
                                            .time(possibleCombinations.retrieveClasstimeTime(i, shift.toString(), possibleCombinations.retrieveClasstimeDay(j)))
                                            .dayOfTheWeek(possibleCombinations.retrieveClasstimeDay(j))
                                            .build();
                                    newAllocatedClassTime.putIfAbsent(key, classTime);
                                }
                            }
                        }
                    }

                    for(Timetable t: preferableTimetableList) {
                        for(ClassTime c: newAllocatedClassTime.values().stream().filter(classTime -> classTime.getSubject().getProfessor().getId().equals(t.getProfessor().getId())).collect(Collectors.toList())) {
                            for(ClassTime ct: t.getClasses()) {
                                if(c.getTime().equals(ct.getTime()) && c.getDayOfTheWeek().equals(ct.getDayOfTheWeek())){
                                    String key = t.getProfessor().getId() + "_" + c.getSubject().getId() + "_" + c.getDayOfTheWeek() + "_" + c.getTime();
                                    if(Objects.isNull(newAllocatedClassTime.get(key))){
                                        ct.setSubject(null);
                                    } else {
                                        ct.setSubject(newAllocatedClassTime.get(key).getSubject());
                                    }
                                }
                            }
                        }
                    }
                    matutinoPoints = possibleCombinations.getTotalPoints();
                }
            } else {
                if(noturnoPoints < possibleCombinations.getTotalPoints()) {
                    for(Timetable t : preferableTimetableList) {
                        for(ClassTime c : t.getClasses()) {
                            c.setSubject(null);
                        }
                    }

                    for (String st : newAllocatedClasses.keySet()) {
                        for (int i = 0; i < rows; i++) {
                            for (int j = 0; j < columns; j++) {
                                if (!newAllocatedClasses.get(st)[i][j].equals("-1") ) {
                                    String key = newAllocatedClasses.get(st)[i][j] + "_" + possibleCombinations.retrieveClasstimeDay(j) + "_" + possibleCombinations.retrieveClasstimeTime(i, shift.toString(), possibleCombinations.retrieveClasstimeDay(j));
                                    ClassTime classTime = ClassTime
                                            .builder()
                                            .subject(professorSubject.get(st + "_" + newAllocatedClasses.get(st)[i][j]))
                                            .time(possibleCombinations.retrieveClasstimeTime(i, shift.toString(), possibleCombinations.retrieveClasstimeDay(j)))
                                            .dayOfTheWeek(possibleCombinations.retrieveClasstimeDay(j))
                                            .build();
                                    newAllocatedClassTime.putIfAbsent(key, classTime);
                                }
                            }
                        }
                    }

                    for(Timetable t: preferableTimetableList) {
                        for(ClassTime c: newAllocatedClassTime.values().stream().filter(classTime -> classTime.getSubject().getProfessor().getId().equals(t.getProfessor().getId())).collect(Collectors.toList())) {
                            for(ClassTime ct: t.getClasses()) {
                                if(c.getTime().equals(ct.getTime()) && c.getDayOfTheWeek().equals(ct.getDayOfTheWeek())){
                                    String key = t.getProfessor().getId() + "_" + c.getSubject().getId() + "_" + c.getDayOfTheWeek() + "_" + c.getTime();
                                    if(Objects.isNull(newAllocatedClassTime.get(key))){
                                        ct.setSubject(null);
                                    } else {
                                        ct.setSubject(newAllocatedClassTime.get(key).getSubject());
                                    }
                                }
                            }
                        }
                    }
                    noturnoPoints = possibleCombinations.getTotalPoints();
                }
            }
            System.out.println("Total de pontos sequência " + p.toString() + " " + shift.toString() + " : " + possibleCombinations.getTotalPoints());
            iterationCombinations++;
        }
        System.out.println("Maior pontuação " + shift.toString() + ": " + (shift.equals(Shift.MATUTINO) ? matutinoPoints : noturnoPoints));
        return bestIterationsToAchieveBestValue;
    }
    public static HashMap<String, Timetable> addTimetablesToHashMap(List<Timetable> timetableList) {
        HashMap<String, Timetable> timetableHashMap = new HashMap<>();

        for (Timetable t : timetableList) {
            timetableHashMap.putIfAbsent(t.getProfessorId() + "-" + t.getShift().toString(), t);
        }

        return timetableHashMap;
    }

    public static String[][] generateSameNumberMatrix(int rows, int columns, List<ClassTime> currentAllocatedClasses, HashMap<String, Subject> professorSubject) {
        String[][] matrix = new String[rows][columns];

        for (int j = 0; j < columns; j++) {
            for (int i = 0; i < rows; i++) {
                matrix[i][j] = "-1";
            }
        }

        Comparator<ClassTime> classTimeComparator
                = (ct1, ct2) -> retrieveDayPos(ct1.getDayOfTheWeek()) - retrieveDayPos(ct2.getDayOfTheWeek());
        currentAllocatedClasses.sort(classTimeComparator);

        String day = currentAllocatedClasses.get(0).getDayOfTheWeek();
        int column = 0;
        for (ClassTime c: currentAllocatedClasses) {
            int pos = retrievePositionFromTime(c.getTime());
            if(!c.getDayOfTheWeek().equals(day)){
                column++;
                day = c.getDayOfTheWeek();
            }
            matrix[pos][column] = c.getSubject().getProfessor().getId() + "_" + c.getSubject().getId();
            professorSubject.putIfAbsent(Integer.toString(c.getSubject().getPeriod()) + "_" + c.getSubject().getShift().toString() + "_" + c.getSubject().getProfessor().getId() + "_" + c.getSubject().getId(), c.getSubject());
        }

        return matrix;
    }

    public static void retrievePreferableTimesMap(HashMap<String, Boolean> preferableTimesMap, List<Timetable> preferableTimetableList) {
        for(Timetable t: preferableTimetableList) {
            for(ClassTime c: t.getClasses()) {
                String mapKey = t.getProfessor().getId() + "_" + c.getDayOfTheWeek() + "_" + c.getTime();
                Boolean preferable = false;
                if(c.isPreferable()){
                    preferable = true;
                }
                preferableTimesMap.putIfAbsent(mapKey, preferable);
            }
        }
    }

    public static int retrievePositionFromTime(String time){
        switch (time) {
            case "07:00":
            case "18:30":
                return 0;
            case "07:50":
            case "19:20":
                return 1;
            case "08:40":
            case "20:25":
                return 2;
            case "09:45":
            case "21:15":
                return 3;
            case "10:35":
            case "22:05":
                return 4;
            case "11:25":
                return 5;
        }

        return -1;
    }

    public static int retrieveDayPos(String day) {
        switch (day) {
            case "Segunda-Feira":
                return 0;
            case "Terça-Feira":
                return 1;
            case "Quarta-Feira":
                return 2;
            case "Quinta-Feira":
                return 3;
            case "Sexta-Feira":
                return 4;
            case "Sábado":
                return 5;
        }

        return -1;
    }

    public static List<List<Integer>> possiblePeriods(HashMap<Object, Object> periodMap, Shift shift) {
        int[] periods = new int[((HashMap<String, String>) periodMap.get(shift.toString())).size()];
        int i = 0;
        for(Object s: ((HashMap<String, String>) periodMap.get(shift.toString()))
                .keySet().stream().collect(Collectors.toList())){
            periods[i] = Integer.parseInt(s.toString());
            i++;
        }

        return PeriodsCombinations.generatePermutations(periods);
    }

    public static void fixPossibleWrongTime(Timetable currentTimetable, Shift shift){
        if(shift.equals(Shift.NOTURNO)){
            currentTimetable.getClasses().stream().filter(classTime -> classTime.getDayOfTheWeek().equals("Sábado"))
                    .collect(Collectors.toList()).stream().forEach(classTime -> classTime.setTime(convertTimes(classTime.getTime())));
        }
    }

    public static String convertTimes(String time){
        switch (time){
            case "07:00":
            case "18:30":
                return "07:00";
            case "07:50":
            case "19:20":
                return "07:50";
            case "08:40":
            case "20:25":
                return "08:40";
            case "09:45":
            case "21:15":
                return "09:45";
            case "10:35":
            case "22:05":
                return "10:35";
            case "11:25":
                return "11:25";
        }
        return "";
    }
}


