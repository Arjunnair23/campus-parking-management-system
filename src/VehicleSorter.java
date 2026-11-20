
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * VehicleSorter.java  (Phase 3 - sorting)
 * Merge sort (on linked Nodes) and insertion sort (on an array copy).
 * Both sort a COPY, are stable, and count key comparisons.
 */
public class VehicleSorter {

    /** What to sort by. */
    public enum Key { SLOT, NUMBER, TIME }

    /** Sorted output plus how much work the algorithm did. */
    public static class SortResult {
        public final String algorithm;
        public final Key key;
        public final List<Vehicle> sorted;
        public final long comparisons;
        public final double micros;

        SortResult(String algorithm, Key key, List<Vehicle> sorted, long comparisons, double micros) {
            this.algorithm = algorithm;
            this.key = key;
            this.sorted = sorted;
            this.comparisons = comparisons;
            this.micros = micros;
        }
    }

    private long comparisons;

    public static Comparator<Vehicle> comparatorFor(Key key) {
        switch (key) {
            case NUMBER: return (a, b) -> a.getVehicleNumber().compareToIgnoreCase(b.getVehicleNumber());
            case TIME:   return (a, b) -> a.getEntryTime().compareTo(b.getEntryTime());
            case SLOT:
            default:     return (a, b) -> Integer.compare(a.getSlotNumber(), b.getSlotNumber());
        }
    }

    /** Parses "slot" / "number" / "time" (any case). Defaults to SLOT. */
    public static Key parseKey(String s) {
        if (s == null) return Key.SLOT;
        switch (s.trim().toLowerCase()) {
            case "number": return Key.NUMBER;
            case "time":   return Key.TIME;
            default:       return Key.SLOT;
        }
    }

    private int compare(Comparator<Vehicle> cmp, Vehicle a, Vehicle b) {
        comparisons++;
        return cmp.compare(a, b);
    }

    // ===== 1. MERGE SORT on a linked list =====

    public SortResult mergeSort(List<Vehicle> vehicles, Key key) {
        Comparator<Vehicle> cmp = comparatorFor(key);
        comparisons = 0;
        long start = System.nanoTime();

        Node<Vehicle> head = null, tail = null;
        for (Vehicle v : vehicles) {
            Node<Vehicle> n = new Node<>(v);
            if (head == null) head = tail = n;
            else { tail.next = n; tail = n; }
        }

        head = mergeSortNodes(head, cmp);

        List<Vehicle> out = new ArrayList<>();
        for (Node<Vehicle> t = head; t != null; t = t.next) out.add(t.data);

        double micros = (System.nanoTime() - start) / 1000.0;
        return new SortResult("Merge Sort", key, out, comparisons, micros);
    }

    private Node<Vehicle> mergeSortNodes(Node<Vehicle> head, Comparator<Vehicle> cmp) {
        if (head == null || head.next == null) return head;

        Node<Vehicle> slow = head, fast = head.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        Node<Vehicle> second = slow.next;
        slow.next = null;

        Node<Vehicle> left = mergeSortNodes(head, cmp);
        Node<Vehicle> right = mergeSortNodes(second, cmp);
        return merge(left, right, cmp);
    }

    private Node<Vehicle> merge(Node<Vehicle> a, Node<Vehicle> b, Comparator<Vehicle> cmp) {
        Node<Vehicle> dummy = new Node<>(null);
        Node<Vehicle> tail = dummy;

        while (a != null && b != null) {
            if (compare(cmp, a.data, b.data) <= 0) {
                tail.next = a;
                a = a.next;
            } else {
                tail.next = b;
                b = b.next;
            }
            tail = tail.next;
        }
        tail.next = (a != null) ? a : b;
        return dummy.next;
    }

    // ===== 2. INSERTION SORT on an array copy =====

    public SortResult insertionSort(List<Vehicle> vehicles, Key key) {
        Comparator<Vehicle> cmp = comparatorFor(key);
        comparisons = 0;
        long start = System.nanoTime();

        Vehicle[] arr = vehicles.toArray(new Vehicle[0]);
        for (int i = 1; i < arr.length; i++) {
            Vehicle current = arr[i];
            int j = i - 1;
            while (j >= 0 && compare(cmp, arr[j], current) > 0) {
                arr[j + 1] = arr[j];
                j--;
            }
            arr[j + 1] = current;
        }

        List<Vehicle> out = new ArrayList<>();
        for (Vehicle v : arr) out.add(v);

        double micros = (System.nanoTime() - start) / 1000.0;
        return new SortResult("Insertion Sort", key, out, comparisons, micros);
    }
}