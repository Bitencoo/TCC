import controllers.PrioritizationController;
import controllers.TimetableController;
import entities.ClassTime;
import entities.Subject;
import entities.Timetable;
import enums.Shift;
import services.PrioritizationServiceImpl;
import services.TimetableServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TCC {
    public static void main(String[] args) throws IOException {
        TimetableController timetableController = new TimetableController(new TimetableServiceImpl());
        PrioritizationController prioritizationController = new PrioritizationController(new PrioritizationServiceImpl());
        HashMap<Object, Object> periodMap;
        HashMap<Object, String> professors = new HashMap<>();
        HashMap<Object, Object> subjectsPrioritizationMap = new HashMap<>();

        List<Timetable> timetableList = new ArrayList<>();
        List<ClassTime> allocatedClasses = new ArrayList<>();
        Timetable allocatedTimetable = new Timetable();

        //Recuperando Priorização dos períodos MATUTINO e NOTURNO
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
        for(Timetable timetable: preferableTimetableList) {
            timetableList.add(
                    timetable.getShift().equals(Shift.MATUTINO)
                            ? timetableController.createEmptyProfessorPreferableTimetableMatutino(timetable.getId(), timetable.getProfessorId(), timetable.getProfessor().getName(), timetable.getShift())
                            : timetableController.createEmptyProfessorPreferableTimetableNoturno(timetable.getId(), timetable.getProfessorId(), timetable.getProfessor().getName(), timetable.getShift()));
        }

        allocatedClasses = allocateClassTimes(subjectList, periodMap, preferableTimetableList);
        allocatedTimetable.setClasses(allocatedClasses);
        timetableController.exportGeneratedTimetable(preferableTimetableList);
    }

    public static List<ClassTime> allocateClassTimes(List<Subject> subjectList, HashMap<Object, Object> periodMap, List<Timetable> preferableTimetable) {
        Comparator<ClassTime> timetableComparator
                = (ct1, ct2) -> (int) ct1.getPrioritization() - ct2.getPrioritization();
        preferableTimetable.stream().forEach(timetable -> timetable.getClasses().sort(timetableComparator));
        // i = 0 TURNO MATUTINO
        // i = 1 TURNO NOTURNO
        List<Subject> currentSubjectList = new ArrayList<>();
        Timetable  currentTimetableList = new Timetable();
        String day = "Segunda-Feira";
        List<ClassTime> allocatedClasses = new ArrayList<>();
        LinkedHashMap<Object, Integer> daysAvailableClasses = new LinkedHashMap<>();
        HashMap<String, Shift> shifts = new HashMap<>();
        shifts = populateShifts(shifts);
        daysAvailableClasses = emptyDaysAvailableClasses(daysAvailableClasses, Shift.MATUTINO);
        /*for(String shiftKey: shifts.keySet()){
            daysAvailableClasses = emptyDaysAvailableClasses(daysAvailableClasses, shifts.get(shiftKey));
        }*/
        for(String s : ((HashMap<String, String>) periodMap.get("MATUTINO")).keySet().stream().collect(Collectors.toList())) {
            currentSubjectList = subjectList.stream().filter(sub -> sub.getPeriod() == Integer.parseInt(s) && sub.getShift().equals(Shift.MATUTINO)).collect(Collectors.toList());
            currentSubjectList = allocationRank(currentSubjectList, preferableTimetable);
            List<ClassTime> classTimeListToAllocate = new ArrayList<>();

            for (Subject subject: currentSubjectList){
                currentTimetableList = preferableTimetable.stream().filter(timetable -> timetable.getShift().equals(Shift.MATUTINO) && timetable.getProfessor().getId().equals(subject.getProfessor().getId())).collect(Collectors.toList()).get(0);
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
        return allocatedClasses;
    }

    public static int allocateClasses(
            List<ClassTime> classTimeListToAllocate,
            Timetable currentTimetable,
            List<Timetable> preferableTimetable,
            Subject subject,
            int qtyClassesToAllocate,
            int currentClassTimeLength,
            String day){
        int allocatedClasses = qtyClassesToAllocate;
        String currentTime = "";
        List<Integer> daysToAllocate = new ArrayList<>();

        while(allocatedClasses != 0){
            currentTime = retrieveClasstimeFromAvailableClasses(currentClassTimeLength, subject.getShift().toString());

            // Verifica se não foi alocada nenhuma aula e se o dia atual corresponde ao do horário
            String finalCurrentTime = currentTime;
            String finalDay = day;

            // 0 indica que ainda não foi alocado horário para o professor
            int validateProfessorFree = preferableTimetable.stream().filter(timetable ->
                    timetable.getProfessor().getId().equals(subject.getProfessor().getId())
                            && timetable.getClasses().stream().anyMatch(
                            classTime ->
                                    classTime.getDayOfTheWeek().equals(finalDay)
                                            && classTime.getTime().equals(finalCurrentTime)
                                            && !Objects.isNull(classTime.getSubject())
                    )
            ).collect(Collectors.toList()).size();

            int validateTimetableFree = preferableTimetable.stream().filter(timetable ->
                    timetable.getClasses().stream().anyMatch(
                            classTime ->
                                    classTime.getDayOfTheWeek().equals(finalDay)
                                            && classTime.getTime().equals(finalCurrentTime)
                                            && !Objects.isNull(classTime.getSubject())
                                            && classTime.getSubject().getPeriod() == subject.getPeriod()
                    )
            ).collect(Collectors.toList()).size();

            if(validateProfessorFree == 0 && validateTimetableFree == 0){
                currentClassTimeLength--;
                allocatedClasses--;
                ClassTime cTime = currentTimetable.getClasses().stream().filter(classTime ->
                        classTime.getTime().equals(finalCurrentTime)
                        && classTime.getDayOfTheWeek().equals(finalDay)
                        ).collect(Collectors.toList()).get(0);
                cTime.setSubject(subject);
                if(cTime.isPreferable()){
                    currentTimetable.setPoints(currentTimetable.getPoints() + 1);
                }
                classTimeListToAllocate.add(cTime);
            } else {
                day = retrieveNextDay(day);
            }
        }

        return qtyClassesToAllocate - allocatedClasses;
    }

    public static LinkedHashMap<Object, Integer> emptyDaysAvailableClasses(LinkedHashMap<Object, Integer> daysAvailableClasses, Shift shift){
        for(int i = 1; i <= 10; i++){
            daysAvailableClasses.put(i + "-Segunda-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Terça-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Quarta-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Quinta-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Sexta-Feira", shift.equals(Shift.MATUTINO) ? 6 : 5);
            daysAvailableClasses.put(i + "-Sábado", 6);

        }
        return daysAvailableClasses;
    }

    public static HashMap<String, Shift> populateShifts(HashMap<String, Shift> shiftsMap){
        shiftsMap.putIfAbsent("MATUTINO", Shift.MATUTINO);
        shiftsMap.putIfAbsent("NOTURNO", Shift.NOTURNO);
        return shiftsMap;
    }


    // Cria o rank de alocação, onde os professores com mais horários disponíveis serão os últimos a serem alocados
    // Falta colocar a condição de 0, nesse caso, será o último professor a ser alocado, pois o mesmo não preencheu
    // a planilha.
    public static List<Subject> allocationRank(List<Subject> subjectList, List<Timetable> timetableList) {
        Comparator<Timetable> timetableComparator
                = (t1, t2) -> (int) t1.getQtyPreferableTimes() - t2.getQtyPreferableTimes();
        timetableList.sort(timetableComparator);

        Comparator<Subject> subjectComparator
                = (s1, s2) -> s1.getPrioritization() - s2.getPrioritization();
        subjectList.sort(subjectComparator);

        return subjectList;
    }

    public static void defineClassesToAllocate(
            LinkedHashMap<Object, Integer> daysAvailableClasses,
            List<ClassTime> classTimeListToAllocate,
            Timetable  currentTimetableList,
            List<Timetable> preferableTimetable,
            Subject subject,
            int qtyClasses
    ){
        String day = findBestDayToAllocate(daysAvailableClasses, qtyClasses, subject.getPeriod());

        if(Objects.isNull(day)){
            day = Integer.toString(subject.getPeriod()) + "-Segunda-Feira";
        }

        int qtyMissingClassesToAllocate = qtyClasses;
        int allocatedClasses = -1;
        int availableClasses = daysAvailableClasses.get(day);

        while(qtyMissingClassesToAllocate > 0){
            //Os horários podem ser alocados em sequência no determinado dia sem problemas
            if(qtyClasses <= availableClasses){
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

            while(qtyClasses != 0) {
                //Quantidade de horários para ser alocados é menor que a quantidade disponível no dia
                //Dividir a quantidade por 2 até ser possível alocar a matéria em mais de 1 dia
                qtyClasses = (qtyMissingClassesToAllocate / 2) + qtyMissingClassesToAllocate % 2;

                if(allocatedClasses != 0){
                    day = findBestDayToAllocate(daysAvailableClasses, qtyClasses, subject.getPeriod());
                }

                if(!Objects.isNull(day)){
                    availableClasses = daysAvailableClasses.get(day);
                    if(qtyClasses % 2 != 0) {
                        if(qtyClasses <= availableClasses){
                            allocatedClasses =  allocateClasses(
                                    classTimeListToAllocate,
                                    currentTimetableList,
                                    preferableTimetable,
                                    subject,
                                    qtyClasses,
                                    availableClasses,
                                    day.split(Integer.toString(subject.getPeriod()) + "-")[1]);
                        } else if(qtyClasses - 1 <= availableClasses) {
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
                        if(qtyClasses <= availableClasses) {
                            allocatedClasses =  allocateClasses(
                                    classTimeListToAllocate,
                                    currentTimetableList,
                                    preferableTimetable,
                                    subject,
                                    qtyClasses,
                                    availableClasses,
                                    day.split(Integer.toString(subject.getPeriod()) + "-")[1]);
                        }
                    }
                    if(allocatedClasses == 0){
                        day = findBestDayToAllocate(daysAvailableClasses, qtyClasses, day, subject.getPeriod());
                    } else {
                        qtyMissingClassesToAllocate = qtyMissingClassesToAllocate - allocatedClasses;
                        daysAvailableClasses.put(day, daysAvailableClasses.get(day) - allocatedClasses);
                    }
                }
            }
        }
        return;
    }

    public static String returnDayToAllocate(
            int qtyClasses, LinkedHashMap<Object, Integer> daysAvailableClasses, Subject subject){
        for(Object st : daysAvailableClasses.keySet()){
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
            int period){
        for(Object o: daysAvailableClasses.keySet()){
            if(qtyClassesToAllocate <= daysAvailableClasses.get(o) && o.toString().split("-")[0].equals(Integer.toString(period))){
                return o.toString();
            }
        }
        return null;
    }

    public static String findBestDayToAllocate(
            LinkedHashMap<Object, Integer> daysAvailableClasses,
            int qtyClassesToAllocate,
            String dayToAvoid,
            int period){
        for(Object o: daysAvailableClasses.keySet()){
            if(qtyClassesToAllocate <= daysAvailableClasses.get(o) && !o.toString().equals(dayToAvoid) && o.toString().split("-")[0].equals(Integer.toString(period))){
                return o.toString();
            }
        }
        return null;
    }


    public static String retrieveClasstimeFromAvailableClasses(int option, String shift){
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

        return "";
    }

    public static String retrieveNextDay(String day){
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

}


