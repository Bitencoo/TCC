import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PeriodsCombinations {
    public static List<List<Integer>> generatePermutations(int[] array) {
        List<List<Integer>> result = new ArrayList<>();
        permute(array, 0, result);
        return result;
    }

    private static void permute(int[] array, int start, List<List<Integer>> result) {
        if (start == array.length - 1) {
            result.add(Arrays.asList(Arrays.stream(array).boxed().toArray(Integer[]::new)));
            return;
        }

        for (int i = start; i < array.length; i++) {
            swap(array, start, i);
            permute(array, start + 1, result);
            swap(array, start, i); // backtrack
        }
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
