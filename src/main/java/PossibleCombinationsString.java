import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PossibleCombinationsString {
    int rows;
    int columns;
    String[][] originalMatrix;
    String[][] bestMatrix;
    int bestMatrixPoints = 0;
    HashMap<String, Boolean> preferableTimesMap;
    HashMap<String, Boolean> periodsBestMatrixHashMap = new HashMap<>();
    HashMap<String, String[][]> bestMatrixHashMap = new HashMap<>();
    HashMap<String, Integer> periodsPoints = new HashMap<>();
    HashMap<String, String> periodsAndShifts = new HashMap<>();
    HashMap<String, Integer> iterationsToAchieveBestValue = initializeIterationHashMap();

    List<List<String>> allCombinations;

    public void combinations(String periodAndShift, int i){
        this.bestMatrixPoints = 0;
        this.bestMatrix = new String[rows][columns];
//        System.out.println("Original Matrix:");
//        printMatrix(originalMatrix);

        this.allCombinations = generateColumnPermutations(columns);
        this.preferableTimesMap = preferableTimesMap;

        // Print or use the combinations as needed
        for (List<String> combination : allCombinations) {
            String[][] newMatrix = generateNewMatrix(originalMatrix, combination);
//            System.out.println("\nCombination:");
//            printMatrix(newMatrix);
            int points = calculatePoints(newMatrix, periodAndShift.split("_")[1]);

            if (points > this.bestMatrixPoints && verifyAbleMatrix(newMatrix, periodAndShift)) {
                this.bestMatrix = newMatrix;
                this.bestMatrixPoints = points;
                this.iterationsToAchieveBestValue.put(periodAndShift, i);
            }
            i++;
        }

        periodsAndShifts.putIfAbsent(periodAndShift, periodAndShift);
        addBestMatrix(bestMatrixHashMap, periodAndShift, bestMatrix);
        populateAllocatedClasses(bestMatrix, periodAndShift);
        addPeriodsPoints(periodsPoints, periodAndShift, bestMatrixPoints);
//        System.out.println("\nTotal number of combinations: " + allCombinations.size());
    }

    private static List<List<String>> generateColumnPermutations(int columns) {
        List<String> columnIndices = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            columnIndices.add(Integer.toString(i));
        }

        List<List<String>> allCombinations = new ArrayList<>();
        generatePermutations(columnIndices, 0, allCombinations);

        return allCombinations;
    }

    private static void generatePermutations(List<String> columns, int currentIndex, List<List<String>> permutations) {
        if (currentIndex == columns.size() - 1) {
            permutations.add(new ArrayList<>(columns));
        } else {
            for (int i = currentIndex; i < columns.size(); i++) {
                Collections.swap(columns, i, currentIndex);
                generatePermutations(columns, currentIndex + 1, permutations);
                Collections.swap(columns, i, currentIndex);
            }
        }
    }

    private static String[][] generateNewMatrix(String[][] originalMatrix, List<String> columnCombination) {
        int rows = originalMatrix.length;
        int columns = originalMatrix[0].length;

        String[][] newMatrix = new String[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                newMatrix[i][j] = originalMatrix[i][Integer.parseInt(columnCombination.get(j))];
            }
        }

        return newMatrix;
    }

    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int value : row) {
                System.out.print(value + "\t");
            }
            System.out.println();
        }
    }

    private int calculatePoints(String[][] matrix, String shift) {
        int points = 0;
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < this.columns; j++){
                if(!matrix[i][j].equals("-1")) {
                    if(preferableTimesMap.get(matrix[i][j].split("_")[0] + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, shift, retrieveClasstimeDay(j))) != null
                    && preferableTimesMap.get(matrix[i][j].split("_")[0] + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, shift, retrieveClasstimeDay(j)))) {
                        points++;
                    }
                }
            }
        }
        return points;
    }

    public static String retrieveClasstimeTime(int timePosition, String shift, String day){
        switch (timePosition) {
            case 0:
                return shift.equals("MATUTINO") || day.equals("Sábado") ? "07:00" : "18:30";
            case 1:
                return shift.equals("MATUTINO") || day.equals("Sábado") ? "07:50" : "19:20";
            case 2:
                return shift.equals("MATUTINO") || day.equals("Sábado") ? "08:40" : "20:25";
            case 3:
                return shift.equals("MATUTINO") || day.equals("Sábado") ? "09:45" : "21:15";
            case 4:
                return shift.equals("MATUTINO") || day.equals("Sábado") ? "10:35" : "22:05";
            case 5:
                return "11:25";
        }

        return "";
    }

    public static String retrieveClasstimeDay(int dayPosition){
        switch (dayPosition) {
            case 0:
                return "Segunda-Feira";
            case 1:
                return "Terça-Feira";
            case 2:
                return "Quarta-Feira";
            case 3:
                return "Quinta-Feira";
            case 4:
                return "Sexta-Feira";
            case 5:
            case 6:
                return "Sábado";
        }

        return "";
    }

    private void populateAllocatedClasses(String[][] matrix, String periodAndShift) {
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < this.columns; j++){
                boolean add = true;
                if(!matrix[i][j].equals("-1")){
                    add = false;
                }
                periodsBestMatrixHashMap.putIfAbsent(periodAndShift + "_" + matrix[i][j].split("_")[0] + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, periodAndShift.split("_")[1], retrieveClasstimeDay(j)), add);
            }
        }
    }

    private boolean verifyAbleMatrix(String[][] matrix, String periodAndShift) {
        for(String st: periodsAndShifts.keySet()){
            for(int i = 0; i < this.rows; i++){
                for(int j = 0; j < this.columns; j++){
                    if(!matrix[i][j].equals("-1")) {
                        if (!Objects.isNull(periodsBestMatrixHashMap.get(st + "_" + matrix[i][j].split("_")[0] + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, st.split("_")[1], retrieveClasstimeDay(j))))) {
                                if (!periodsBestMatrixHashMap.get(st + "_" + matrix[i][j].split("_")[0] + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, st.split("_")[1], retrieveClasstimeDay(j)))) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private void addPeriodsPoints(HashMap<String, Integer> periodsPoints, String periodAndShift, int point) {
        periodsPoints.putIfAbsent(periodAndShift, point);
    }

    private void addBestMatrix(HashMap<String, String[][]> bestMatrixHashMap, String periodAndShift, String[][] matrix) {
        bestMatrixHashMap.putIfAbsent(periodAndShift, matrix);
    }

    private HashMap<String, Integer> initializeIterationHashMap(){
        HashMap<String, Integer> iterationsToAchieveBestValue = new HashMap<>();
        iterationsToAchieveBestValue.put("1_MATUTINO", 0);
        iterationsToAchieveBestValue.put("2_MATUTINO", 0);
        iterationsToAchieveBestValue.put("3_MATUTINO", 0);
        iterationsToAchieveBestValue.put("4_MATUTINO", 0);
        iterationsToAchieveBestValue.put("5_MATUTINO", 0);
        iterationsToAchieveBestValue.put("6_MATUTINO", 0);
        iterationsToAchieveBestValue.put("7_MATUTINO", 0);
        iterationsToAchieveBestValue.put("8_MATUTINO", 0);
        iterationsToAchieveBestValue.put("9_MATUTINO", 0);
        iterationsToAchieveBestValue.put("10_MATUTINO", 0);
        iterationsToAchieveBestValue.put("1_NOTURNO", 0);
        iterationsToAchieveBestValue.put("2_NOTURNO", 0);
        iterationsToAchieveBestValue.put("3_NOTURNO", 0);
        iterationsToAchieveBestValue.put("4_NOTURNO", 0);
        iterationsToAchieveBestValue.put("5_NOTURNO", 0);
        iterationsToAchieveBestValue.put("6_NOTURNO", 0);
        iterationsToAchieveBestValue.put("7_NOTURNO", 0);
        iterationsToAchieveBestValue.put("8_NOTURNO", 0);
        iterationsToAchieveBestValue.put("9_NOTURNO", 0);
        iterationsToAchieveBestValue.put("10_NOTURNO", 0);
        return iterationsToAchieveBestValue;
    }

    public HashMap<String, String[][]> getBestAllocatedClasses() {
        return bestMatrixHashMap;
    }

    public int getTotalPoints() {
        int totalPoints = 0;
        for(Integer points : periodsPoints.values()){
            totalPoints = totalPoints + points;
        }
        return totalPoints;
    }
}

