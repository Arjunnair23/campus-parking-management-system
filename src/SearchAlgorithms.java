import java.util.List;

/**
 * SearchAlgorithms.java  (Phase 3 - searching)
 * -------------------------------------------------------
 * Two ways to find a parked vehicle by its vehicle number:
 *
 *   1. Linear search  - checks records one by one from the start.
 *                       Works on unsorted data. O(n).
 *   2. Binary search  - repeatedly halves the search range.
 *                       REQUIRES the data to be sorted by vehicle
 *                       number first (Phase 3A merge sort does that).
 *                       O(log n) once the data is sorted.
 *
 * Both count how many records they compared, so the two can be
 * compared with real numbers (Phase 3C).
 * -------------------------------------------------------
 */
public class SearchAlgorithms {

    /** Outcome of one search. */
    public static class SearchResult {
        public final String algorithm;
        public final Vehicle vehicle;        // null if not found
        public final int position;           // 1-based position in the searched list, 0 if not found
        public final int total;              // how many records were searched
        public final long comparisons;       // comparisons made by the search itself
        public final double micros;
        public long prepComparisons;         // comparisons spent sorting first (binary search only)

        SearchResult(String algorithm, Vehicle vehicle, int position, int total,
                     long comparisons, double micros) {
            this.algorithm = algorithm;
            this.vehicle = vehicle;
            this.position = position;
            this.total = total;
            this.comparisons = comparisons;
            this.micros = micros;
        }
    }

    /** Linear search: scan from the first record until a match is found. */
    public static SearchResult linearSearch(List<Vehicle> vehicles, String vehicleNumber) {
        long start = System.nanoTime();
        long comparisons = 0;
        Vehicle found = null;
        int position = 0;
        String target = vehicleNumber == null ? "" : vehicleNumber.trim();

        for (int i = 0; i < vehicles.size(); i++) {
            comparisons++;
            if (vehicles.get(i).getVehicleNumber().equalsIgnoreCase(target)) {
                found = vehicles.get(i);
                position = i + 1;
                break;
            }
        }
        double micros = (System.nanoTime() - start) / 1000.0;
        return new SearchResult("Linear Search", found, position, vehicles.size(), comparisons, micros);
    }

    /**
     * Binary search. The list MUST already be sorted by vehicle number
     * (case-insensitive, ascending) - otherwise the result is unreliable.
     */
    public static SearchResult binarySearch(List<Vehicle> sortedByNumber, String vehicleNumber) {
        long start = System.nanoTime();
        long comparisons = 0;
        Vehicle found = null;
        int position = 0;
        String target = vehicleNumber == null ? "" : vehicleNumber.trim();

        int low = 0, high = sortedByNumber.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            comparisons++;
            int cmp = sortedByNumber.get(mid).getVehicleNumber().compareToIgnoreCase(target);
            if (cmp == 0) {
                found = sortedByNumber.get(mid);
                position = mid + 1;
                break;
            } else if (cmp < 0) {
                low = mid + 1;     // target is in the right half
            } else {
                high = mid - 1;    // target is in the left half
            }
        }
        double micros = (System.nanoTime() - start) / 1000.0;
        return new SearchResult("Binary Search", found, position, sortedByNumber.size(), comparisons, micros);
    }
}