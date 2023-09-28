import controllers.PrioritizationController;
import controllers.TimetableController;
import entities.ClassTime;
import entities.Subject;
import entities.Timetable;
import enums.Shift;
import services.PrioritizationServiceImpl;
import services.TimetableServiceImpl;

import java.io.IOException;
import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;

public class TCC {
    public static void main(String[] args) throws IOException {
        TimetableController timetableController = new TimetableController(new TimetableServiceImpl());
        PrioritizationController prioritizationController = new PrioritizationController(new PrioritizationServiceImpl());
        HashMap<Object, Object> periodMap;
        HashMap<Object, String> professors = new HashMap<>();

        List<Timetable> timetableList = new ArrayList<>();
        List<ClassTime> allocatedClasses = new ArrayList<>();
        Timetable allocatedTimetable = new Timetable();

        //Recuperando Priorização dos períodos MATUTINO e NOTURNO
        periodMap = (HashMap<Object, Object>) prioritizationController.readPeriodPrioritization("src/main/resources/priorizacao_horarios.xlsx");

        //Recuperando Matérias e priorizando cada uma
        List<Subject> subjectList = timetableController.readQDA("src/main/resources/QDA 11 05 2023.xlsx", periodMap, professors);
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
        String b = "";
        allocatedTimetable.setClasses(allocatedClasses);
        timetableController.exportGeneratedTimetable(preferableTimetableList);
    }

    // Verificando restrições (1) e (2)
    // Um professor não pode estar presente em mais de uma sala de aula ao mesmo tempo. (1)
    // Alunos não podem estar em mais de uma sala de aula ao mesmo tempo. (2)
    public static boolean onlyOneClassAtATime(String classString, HashMap<String, Boolean> allocatedClasses) {
        return allocatedClasses.containsKey(classString);
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
        int dayNumber = 0;
        int currentDayNumber = 0;
        int sumDayNumber = 0;
        int qtyOpenClasses = 6;
        int currentClassTimeLength = 0;
        int currentClassTimeAfter = 0;
        List<ClassTime> allocatedClasses = new ArrayList<>();
        LinkedHashMap<Object, Integer> daysAvailableClasses = new LinkedHashMap<>();
        daysAvailableClasses = emptyDaysAvailableClasses(daysAvailableClasses, Shift.MATUTINO);
        for(String s : ((HashMap<String, String>) periodMap.get("MATUTINO")).keySet().stream().collect(Collectors.toList())) {
            currentSubjectList = subjectList.stream().filter(sub -> sub.getPeriod() == Integer.parseInt(s) && sub.getShift().equals(Shift.MATUTINO)).collect(Collectors.toList());
            currentSubjectList = allocationRank(currentSubjectList, preferableTimetable);
            //Como se fosse as matérias do período
            List<ClassTime> classTimeListToAllocate = new ArrayList<>();

            for (Subject subject: currentSubjectList){
                day = "Segunda-Feira";
                currentTimetableList = preferableTimetable.stream().filter(timetable -> timetable.getShift().equals(Shift.MATUTINO) && timetable.getProfessor().getId().equals(subject.getProfessor().getId())).collect(Collectors.toList()).get(0);
                int classtimeLength = subject.getNumbersOfLessons();
                currentClassTimeLength = classtimeLength;
                qtyOpenClasses = daysAvailableClasses.get(Integer.toString(subject.getPeriod()) + "-" + day);
                currentDayNumber = 0;
                //dayNumber = 0;
                while(currentClassTimeLength != 0) {
                    day = currentTimetableList.getClasses().get(currentDayNumber).getDayOfTheWeek();
                    day = Integer.toString(subject.getPeriod()) + "-" + day;
                    if (qtyOpenClasses == 0) {
                        //Aqui só precisa mudar o dia e recuperar a quantidade de horários vagos naquele dia
                        currentDayNumber = currentDayNumber + 6;
                        if (currentDayNumber > 35){
                            currentDayNumber = 35;
                        }
                        qtyOpenClasses = daysAvailableClasses.get(Integer.toString(subject.getPeriod()) + "-" + currentTimetableList.getClasses().get(currentDayNumber).getDayOfTheWeek());
                    } else {
                        if (qtyOpenClasses != 0 && qtyOpenClasses < currentClassTimeLength) {
                            //Há mais aulas para serem alocadas do que horários disponíveis para o dia em questão
                            //Sei que precisa dividir as aulas em mais de 1 dia
                            //Verifico se as aulas restantes são pares para garantir pelo menos 2 alocadas no mesmo dia
                            //Se for ímpar, verifico se é apenas 1 aula
                            if (currentClassTimeLength % 2 == 0) {
                                //Sei que são 4 horários
                                //Alocar 2 horários seguidos
                                //Diminuir os horários livres e a quantidade de aulas atuais para serem alocadas
                                if (qtyOpenClasses >= 2) {
                                    for(Object st : daysAvailableClasses.keySet()){
                                        if (daysAvailableClasses.get(st) >= 2 && st.toString().split("-")[0].equals(Integer.toString(subject.getPeriod()))) {
                                            day = st.toString();
                                            break;
                                        }
                                    }
                                    currentClassTimeAfter = allocateClasses(
                                            classTimeListToAllocate,
                                            currentTimetableList,
                                            preferableTimetable,
                                            subject,
                                            2,
                                            //dayNumber,
                                            currentClassTimeLength,
                                            day);
                                    currentClassTimeLength = currentClassTimeLength - currentClassTimeAfter;
                                    daysAvailableClasses.put(day, daysAvailableClasses.get(day) - currentClassTimeAfter);
                                    //sumDayNumber = 2;
                                    //dayNumber = dayNumber + sumDayNumber;
                                    qtyOpenClasses = qtyOpenClasses - 2;
                                    //sumDayNumber = 0;
                                    currentDayNumber = currentDayNumber + 2;
                                } else {
                                    //Trocar dia
                                    currentDayNumber = currentDayNumber + 6;
                                    if (currentDayNumber == 35){
                                        currentDayNumber = 35;
                                    }
                                    qtyOpenClasses = daysAvailableClasses.get(Integer.toString(subject.getPeriod()) + "-" + currentTimetableList.getClasses().get(currentDayNumber).getDayOfTheWeek());
                                }
                            } else {
                                if (currentClassTimeLength <= qtyOpenClasses) {
                                    for(Object st : daysAvailableClasses.keySet()){
                                        if (daysAvailableClasses.get(st) >= currentClassTimeLength && st.toString().split("-")[0].equals(Integer.toString(subject.getPeriod()))) {
                                            day = st.toString();
                                            break;
                                        }
                                    }
                                    currentClassTimeAfter = allocateClasses(
                                            classTimeListToAllocate,
                                            currentTimetableList,
                                            preferableTimetable,
                                            subject,
                                            currentClassTimeLength,
                                            //dayNumber,
                                            currentClassTimeLength,
                                            day);
                                    //sumDayNumber = qty;
                                    //dayNumber = dayNumber + sumDayNumber;
                                    currentClassTimeLength = currentClassTimeLength - currentClassTimeAfter;
                                    daysAvailableClasses.put(day, daysAvailableClasses.get(day) - currentClassTimeAfter);
                                    qtyOpenClasses = qtyOpenClasses - currentClassTimeAfter;
                                    //sumDayNumber = 0;
                                    dayNumber = dayNumber + currentClassTimeAfter;
                                } else {
                                    for(Object st : daysAvailableClasses.keySet()){
                                        if (daysAvailableClasses.get(st) >= qtyOpenClasses && st.toString().split("-")[0].equals(Integer.toString(subject.getPeriod()))) {
                                            day = st.toString();
                                            break;
                                        }
                                    }
                                    currentClassTimeAfter = allocateClasses(
                                            classTimeListToAllocate,
                                            currentTimetableList,
                                            preferableTimetable,
                                            subject,
                                            qtyOpenClasses,
                                            //dayNumber,
                                            currentClassTimeLength,
                                            day);
                                    //sumDayNumber = 1;
                                    qtyOpenClasses = 0;
                                    dayNumber++;
                                    currentClassTimeLength = currentClassTimeLength - currentClassTimeAfter;
                                    daysAvailableClasses.put(day, daysAvailableClasses.get(day) - currentClassTimeAfter);
                                }
                            }
                        } else {
                            //Já sei que posso alocar todas as aulas de uma vez, pois o número de horários livres é
                            //menor do que o número de aulas restantes da matéria
                            for(Object st : daysAvailableClasses.keySet()){
                                if (daysAvailableClasses.get(st) >= currentClassTimeLength && st.toString().split("-")[0].equals(Integer.toString(subject.getPeriod()))) {
                                    day = st.toString();
                                    break;
                                }
                            }

                            currentClassTimeAfter = allocateClasses(
                                    classTimeListToAllocate,
                                    currentTimetableList,
                                    preferableTimetable,
                                    subject,
                                    currentClassTimeLength,
                                   // dayNumber,
                                    currentClassTimeLength,
                                    day);
                            //sumDayNumber = 1;
                            currentClassTimeLength = currentClassTimeLength - currentClassTimeAfter;
                            daysAvailableClasses.put(day, daysAvailableClasses.get(day) - currentClassTimeAfter);
                            qtyOpenClasses = qtyOpenClasses - currentClassTimeAfter;
                            dayNumber++;
                        }
                        //dayNumber = dayNumber + sumDayNumber;
                    }
                }
            }
            String a = "";
            allocatedClasses.addAll(classTimeListToAllocate);
        }

        return allocatedClasses;
    }

    public static int allocateClasses(
            List<ClassTime> classTimeListToAllocate,
            Timetable currentTimetable,
            List<Timetable> preferableTimetable,
            Subject subject,
            int qtyClassesToAllocate,
            //int dayNumber,
            int currentClassTimeLength,
            String day){
        int dayNumber = 0;
        int hasEnoughConsecutiveClasses = 0;
        int counterClassSequence = 0;
        int currentNumberOfLessons = 0;
        int allocatedClasses = 0;
        String previousDay = "Segunda-Feira";
        String currentTime = "";
        List<Integer> daysToAllocate = new ArrayList<>();

        if(subject.getClassName().contains("Introdução")) {
            String g = "";
        }
        while(daysToAllocate.size() != qtyClassesToAllocate){
            if (dayNumber == 36){
                int b = 0;
            }
            if (dayNumber < 35) {
                previousDay = currentTimetable.getClasses().get(dayNumber + 1).getDayOfTheWeek();
            }
            currentTime = currentTimetable.getClasses().get(dayNumber).getTime();
            day = currentTimetable.getClasses().get(dayNumber).getDayOfTheWeek();


            // Muda o dia atual caso necessário
            /*if(!currentTimetable.getClasses().get(currentDay).getDayOfTheWeek().equals(day)) {
                dayNumber++;
                day = currentTimetable.getClasses().get(dayNumber).getDayOfTheWeek();

                // Aloca 2 horários em seguida para o dia em questão
                if(counterClassSequence >= 2) {
                    currentTimetable.getClasses().get(dayNumber).setSubject(subject);
                    currentTimetable.getClasses().get(dayNumber).setSubject(subject);
                    classTimeListToAllocate = classTimeListToAllocate.subList(classTimeListToAllocate.size() - 2, classTimeListToAllocate.size());
                    currentNumberOfLessons = currentNumberOfLessons - 2;
                }
            }*/

            // Verifica se não foi alocada nenhuma aula ainda para o determinado horário
            if(!Objects.isNull(currentTimetable.getClasses().get(dayNumber).getSubject())) {
                counterClassSequence = 0;
            }

            // Verifica se não foi alocada nenhuma aula e se o dia atual corresponde ao do horário
            String finalCurrentTime = currentTime;
            String finalDay = day;
            int finalDayNumber = dayNumber;

            if (day.equals("Quarta-Feira") && subject.getClassName().equals("Cálculo III")) {
                String a = "";
            }

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
            //Verificar se o professor já está dando aula nesse horário NOK
            /*preferableTimetable.stream().filter(timetable ->
                    timetable.getProfessor().getId() == subject.getProfessor().getId()
                    && timetable.getClasses().stream().anyMatch(
                            classTime ->
                                    classTime.getDayOfTheWeek().equals(finalDay)
                                    && classTime.getTime().equals(finalCurrentTime)
                                    && Objects.isNull(classTime.getSubject())
                    )
                    ).collect(Collectors.toList());
            */
            //Verificar se o horário do período já está alocado NOK


            /*int validation = preferableTimetable.stream()
                    .filter(timetable -> timetable.getClasses().stream()
                            .anyMatch(classTime ->
                                    !Objects.isNull(classTime.getSubject()) && classTime.getTime().equals(finalCurrentTime) && classTime.getDayOfTheWeek().equals(finalDay) && classTime.getSubject().getPeriod() == subject.getPeriod()))
                    .collect(Collectors.toList()).size();
            *//*boolean validate = preferableTimetable.stream()
                    .anyMatch(timetable -> timetable.getClasses().stream()
                            .anyMatch(classTime -> classTime.getTime().equals(finalCurrentTime)
                                    && classTime.getDayOfTheWeek().equals(finalDay)
                                    && !Objects.isNull(classTime.getSubject())
                                    && timetable.getClasses().get(finalDayNumber).getSubject().getPeriod() == subject.getPeriod()));
            */
            if(validateProfessorFree == 0 && validateTimetableFree == 0){
                //Primeiro horário do dia ou é só um horário avulso que precisa alocar
                if(currentTime.equals("07:00") || currentTime.equals("18:30") || qtyClassesToAllocate == 1) {
                    hasEnoughConsecutiveClasses++;
                    daysToAllocate.add(dayNumber);
                    dayNumber++;
                    continue;
                }

                //Já sei que tem que alocar mais de 1 horário e não é o primeiro horário das aulas
                //Logo, precisa ser alocado 2 ou mais horários EM SEGUIDA
                //Se o meu próximo horário for o mesmo dia do horário atual, pode alocar
                if (previousDay.equals(day)){
                    hasEnoughConsecutiveClasses++;
                    daysToAllocate.add(dayNumber);
                    dayNumber++;
                    continue;
                }

                //Se o dia do horário anterior for igual o do horário atual e o tamanho do daysToAllocate for maior
                // ou igual a 1 significa que já aloquei 1 horário pra essa aula, logo posso alocar a próxima sem
                // problemas
                if (currentTimetable.getClasses().get(dayNumber - 1).getDayOfTheWeek().equals(day)
                        && daysToAllocate.size() >= 1) {
                    hasEnoughConsecutiveClasses++;
                    daysToAllocate.add(dayNumber);
                    dayNumber++;
                    continue;
                }

                hasEnoughConsecutiveClasses = 0;
                daysToAllocate.clear();
            } else {
                hasEnoughConsecutiveClasses = 0;
                daysToAllocate.clear();
            }

            dayNumber++;
        }

        if (hasEnoughConsecutiveClasses == qtyClassesToAllocate) {
            daysToAllocate.stream().forEach(dayn -> {
                currentTimetable.getClasses().get(dayn).setSubject(subject);
                classTimeListToAllocate.add(currentTimetable.getClasses().get(dayn));
            });

            allocatedClasses = daysToAllocate.size();
        }

        return allocatedClasses;
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

    public static void applyConstraints(List<Timetable> timetableList) {

    }

    // Cria o rank de alocação, onde os professores com mais horários disponíveis serão os últimos a serem alocados
    // Falta colocar a condição de 0, nesse caso, será o último professor a ser alocado, pois o mesmo não preencheu
    // a planilha.
    public static List<Subject> allocationRank(List<Subject> subjectList, List<Timetable> timetableList) {
        Comparator<Timetable> timetableComparator
                = (t1, t2) -> (int) t2.getQtyPreferableTimes() - t1.getQtyPreferableTimes();
        timetableList.sort(timetableComparator);

        subjectList.stream().forEach(
                subject -> {
                    subject.setPrioritization(timetableList.stream().filter(timetable -> timetable.getProfessor().getId().equals(subject.getProfessor().getId())).collect(Collectors.toList()).get(0).getProfessorId());
                });

        Comparator<Subject> subjectComparator
                = (s1, s2) -> s2.getPrioritization() - s1.getPrioritization();
        subjectList.sort(subjectComparator);

        return subjectList;
    }

}


