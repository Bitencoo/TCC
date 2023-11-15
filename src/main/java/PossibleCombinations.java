import enums.Shift;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PossibleCombinations {
    int rows;
    int columns;
    int[][] originalMatrix;
    int[][] bestMatrix;
    int bestMatrixPoints = 0;
    HashMap<String, Boolean> preferableTimesMap;
    HashMap<String, Boolean> periodsBestMatrixHashMap = new HashMap<>();
    HashMap<String, int[][]> bestMatrixHashMap = new HashMap<>();
    HashMap<String, Integer> periodsPoints = new HashMap<>();
    HashMap<String, String> periodsAndShifts = new HashMap<>();

    List<List<Integer>> allCombinations;

    public void combinations(String periodAndShift){
        this.bestMatrixPoints = 0;
        this.bestMatrix = new int[rows][columns];
//        System.out.println("Original Matrix:");
//        printMatrix(originalMatrix);

        this.allCombinations = generateColumnPermutations(columns);
        this.preferableTimesMap = preferableTimesMap;

        // Print or use the combinations as needed
        for (List<Integer> combination : allCombinations) {
            int[][] newMatrix = generateNewMatrix(originalMatrix, combination);
//            System.out.println("\nCombination:");
//            printMatrix(newMatrix);
            int points = calculatePoints(newMatrix, periodAndShift.split("_")[1]);

            if (points > this.bestMatrixPoints && verifyAbleMatrix(newMatrix, periodAndShift)) {
                this.bestMatrix = newMatrix;
                this.bestMatrixPoints = points;
            }
        }

        periodsAndShifts.putIfAbsent(periodAndShift, periodAndShift);
        addBestMatrix(bestMatrixHashMap, periodAndShift, bestMatrix);
        populateAllocatedClasses(bestMatrix, periodAndShift);
        addPeriodsPoints(periodsPoints, periodAndShift, bestMatrixPoints);
//        System.out.println("\nTotal number of combinations: " + allCombinations.size());
    }

    private static List<List<Integer>> generateColumnPermutations(int columns) {
        List<Integer> columnIndices = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            columnIndices.add(i);
        }

        List<List<Integer>> allCombinations = new ArrayList<>();
        generatePermutations(columnIndices, 0, allCombinations);

        return allCombinations;
    }

    private static void generatePermutations(List<Integer> columns, int currentIndex, List<List<Integer>> permutations) {
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

    private static int[][] generateNewMatrix(int[][] originalMatrix, List<Integer> columnCombination) {
        int rows = originalMatrix.length;
        int columns = originalMatrix[0].length;

        int[][] newMatrix = new int[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                newMatrix[i][j] = originalMatrix[i][columnCombination.get(j)];
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

    private int calculatePoints(int[][] matrix, String shift) {
        int points = 0;
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < this.columns; j++){
                if(matrix[i][j] != -1 && preferableTimesMap.get(Integer.toString(matrix[i][j]) + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, shift, retrieveClasstimeDay(j)))) {
                    points++;
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

    private void populateAllocatedClasses(int[][] matrix, String periodAndShift) {
        for(int i = 0; i < this.rows; i++){
            for(int j = 0; j < this.columns; j++){
                boolean add = true;
                if(matrix[i][j] != -1){
                    add = false;
                }
                periodsBestMatrixHashMap.putIfAbsent(periodAndShift + "_" + Integer.toString(matrix[i][j]) + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, periodAndShift.split("_")[1], retrieveClasstimeDay(j)), add);
            }
        }
    }

    private boolean verifyAbleMatrix(int[][] matrix, String periodAndShift) {
        if(matrix[0][5] == 103){
            int a = 0;
        }
        for(String st: periodsAndShifts.keySet()){
            for(int i = 0; i < this.rows; i++){
                for(int j = 0; j < this.columns; j++){
                    if(matrix[i][j] != -1) {
                        if (!Objects.isNull(periodsBestMatrixHashMap.get(st + "_" + Integer.toString(matrix[i][j]) + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, st.split("_")[1], retrieveClasstimeDay(j))))) {
                            if (!periodsBestMatrixHashMap.get(st + "_" + Integer.toString(matrix[i][j]) + "_" + retrieveClasstimeDay(j) + "_" + retrieveClasstimeTime(i, st.split("_")[1], retrieveClasstimeDay(j)))) {
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

    private void addBestMatrix(HashMap<String, int[][]> bestMatrixHashMap, String periodAndShift, int[][] matrix) {
        bestMatrixHashMap.putIfAbsent(periodAndShift, matrix);
    }

    public HashMap<String, int[][]> getBestAllocatedClasses() {
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

