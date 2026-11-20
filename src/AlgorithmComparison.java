import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * AlgorithmComparison.java  (Phase 3C - comparing performance)
 * -------------------------------------------------------
 * Puts numbers behind the theory:
 *
 *   sorting   : merge sort  O(n log n)   vs  insertion sort O(n^2)
 *   searching : linear      O(n)         vs  binary search  O(log n)
 *               (binary search needs the data sorted first)
 *
 * Two tools:
 *
 *  1. benchmark()  - runs all four algorithms on generated vehicle data of
 *                    growing size and reports comparison counts. Comparison
 *                    counts are exact and repeatable; times (microseconds)
 *                    vary a little from run to run.
 *
 *  2. batch()      - the real "sorting + searching together" workflow:
 *                    sort the parked vehicles ONCE, then find many vehicle
 *                    numbers with binary search, versus scanning the list
 *                    linearly for each one.
 * -------------------------------------------------------
 */
public class AlgorithmComparison {

    /** How many lookups the benchmark performs per data size. */
    public static final int BATCH_LOOKUPS = 50;

    public static final int[] DEFAULT_SIZES = {10, 100, 1000, 5000};

    // ---------------- benchmark ----------------

    public static class Row {
        public int n;
        public long mergeCmp, insertionCmp;
        public double mergeMicros, insertionMicros;
        public double linearAvg, binaryAvg;       // average comparisons for ONE lookup
        public long batchLinear;                  // total comparisons: BATCH_LOOKUPS linear searches
        public long batchSortBinary;              // total comparisons: sort once + BATCH_LOOKUPS binary searches
    }

    /** Generates n vehicles with unique, random-looking vehicle numbers (same seed -> same data). */
    public static List<Vehicle> randomVehicles(int n, long seed) {
        Random rnd = new Random(seed);
        Set<String> used = new HashSet<>();
        List<Vehicle> list = new ArrayList<>();
        while (list.size() < n) {
            String num = "KA" + String.format("%02d", rnd.nextInt(100))
                    + (char) ('A' + rnd.nextInt(26)) + (char) ('A' + rnd.nextInt(26))
                    + String.format("%04d", rnd.nextInt(10000));
            if (used.add(num)) {
                String time = String.format("%02d:%02d", 6 + rnd.nextInt(14), rnd.nextInt(60));
                list.add(new Vehicle(num, "Owner" + list.size(), "CAR", list.size() + 1, time));
            }
        }
        return list;
    }

    public static List<Row> benchmark(int[] sizes) {
        runOne(200, 1L); // warm-up, so the first row's timings are not dominated by JVM start-up
        List<Row> rows = new ArrayList<>();
        for (int n : sizes) rows.add(runOne(n, 2026L + n));
        return rows;
    }

    private static Row runOne(int n, long seed) {
        List<Vehicle> data = randomVehicles(n, seed);
        VehicleSorter sorter = new VehicleSorter();

        VehicleSorter.SortResult merge = sorter.mergeSort(data, VehicleSorter.Key.NUMBER);
        VehicleSorter.SortResult insertion = sorter.insertionSort(data, VehicleSorter.Key.NUMBER);

        Row r = new Row();
        r.n = n;
        r.mergeCmp = merge.comparisons;
        r.mergeMicros = merge.micros;
        r.insertionCmp = insertion.comparisons;
        r.insertionMicros = insertion.micros;

        // BATCH_LOOKUPS random existing vehicles, searched both ways
        Random rnd = new Random(seed ^ 0x5DEECE66DL);
        long linearTotal = 0, binaryTotal = 0;
        for (int i = 0; i < BATCH_LOOKUPS; i++) {
            String target = data.get(rnd.nextInt(n)).getVehicleNumber();
            linearTotal += SearchAlgorithms.linearSearch(data, target).comparisons;
            binaryTotal += SearchAlgorithms.binarySearch(merge.sorted, target).comparisons;
        }
        r.linearAvg = linearTotal / (double) BATCH_LOOKUPS;
        r.binaryAvg = binaryTotal / (double) BATCH_LOOKUPS;
        r.batchLinear = linearTotal;
        r.batchSortBinary = merge.comparisons + binaryTotal;
        return r;
    }

    // ---------------- batch lookup on the real parked vehicles ----------------

    public static class BatchItem {
        public String number;
        public boolean found;
        public int slot;
        public long linearCmp;
        public long binaryCmp;
    }

    public static class BatchResult {
        public final List<BatchItem> items = new ArrayList<>();
        public int parkedCount;
        public long linearTotal;     // comparisons if every number is found by a linear scan
        public long sortCost;        // comparisons to merge-sort the parked list once
        public long binaryTotal;     // comparisons for all the binary searches (after sorting)
        public long binaryWithSort() { return sortCost + binaryTotal; }
    }

    public static BatchResult batch(List<Vehicle> parked, List<String> numbers) {
        BatchResult result = new BatchResult();
        result.parkedCount = parked.size();

        VehicleSorter.SortResult sorted = new VehicleSorter().mergeSort(parked, VehicleSorter.Key.NUMBER);
        result.sortCost = sorted.comparisons;

        for (String num : numbers) {
            SearchAlgorithms.SearchResult lin = SearchAlgorithms.linearSearch(parked, num);
            SearchAlgorithms.SearchResult bin = SearchAlgorithms.binarySearch(sorted.sorted, num);

            BatchItem item = new BatchItem();
            item.number = num;
            item.found = lin.vehicle != null;
            item.slot = item.found ? lin.vehicle.getSlotNumber() : 0;
            item.linearCmp = lin.comparisons;
            item.binaryCmp = bin.comparisons;
            result.items.add(item);

            result.linearTotal += lin.comparisons;
            result.binaryTotal += bin.comparisons;
        }
        return result;
    }
}