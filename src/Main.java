import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
 
/**
 * Main.java
 * -------------------------------------------------------
 * Menu-driven demo of Phase 1: Linear Data Structures.
 * Loads sample input data, then lets you exercise every
 * required operation from the console.
 * -------------------------------------------------------
 */
public class Main {
 
    public static void main(String[] args) {
        // Small lot size (5) so it's easy to demo the waiting queue filling up
        ParkingSystem system = new ParkingSystem(10);
        Scanner sc = new Scanner(System.in);
 
        loadSampleData(system);
 
        int choice;
        do {
            printMenu();
            choice = readInt(sc);
 
            switch (choice) {
                case 1 -> {
                    System.out.print("Vehicle Number: ");
                    String num = sc.nextLine();
                    System.out.print("Owner Name: ");
                    String owner = sc.nextLine();
                    System.out.print("Vehicle Type (CAR/BIKE/STAFF/VISITOR): ");
                    String type = sc.nextLine();
                    system.vehicleEntry(num, owner, type, "10:00");
                }
                case 2 -> {
                    System.out.print("Vehicle Number to exit: ");
                    String num = sc.nextLine();
                    system.vehicleExit(num);
                }
                case 3 -> system.showParked();
                case 4 -> {
                    System.out.print("Vehicle Number to search: ");
                    String num = sc.nextLine();
                    Vehicle v = system.searchVehicle(num);
                    System.out.println(v != null ? v : "Vehicle not found.");
                }
                case 5 -> system.showWaitingQueue();
                case 6 -> system.showExitHistory();
                case 7 -> {
                    System.out.print("Enter expression to check brackets, e.g. ((a+b)*[c-d]): ");
                    String expr = sc.nextLine();
                    boolean balanced = ExpressionHandler.isBalanced(expr);
                    System.out.println("Balanced: " + balanced);
                }
                case 8 -> {
                    System.out.println("Inorder (sorted by slot):  " + system.getBstInorder());
                    System.out.println("Preorder:                  " + system.getBstPreorder());
                    System.out.println("Postorder:                 " + system.getBstPostorder());
                }
                case 9 -> {
                    System.out.print("Slot number to search (BST): ");
                    int slot = Integer.parseInt(sc.nextLine().trim());
                    Vehicle v = system.searchBySlotBST(slot);
                    System.out.println(v != null ? v : "No vehicle at slot " + slot + ".");
                }
                case 10 -> {
                    System.out.print("Start zone for BFS (e.g. Entrance): ");
                    String zone = sc.nextLine().trim();
                    if (zone.isEmpty()) zone = "Entrance";
                    System.out.println("BFS visit order from " + zone + ": " + system.bfsFromZone(zone));
                    System.out.println("Zone connections: " + system.getCampusGraph().getAdjacencyList());
                }
                case 11 -> {
                    System.out.print("Algorithm (merge/insertion): ");
                    String algo = sc.nextLine().trim();
                    System.out.print("Sort by (slot/number/time): ");
                    String key = sc.nextLine().trim();
                    VehicleSorter.SortResult r = system.sortParked(algo, key);
                    System.out.println(r.algorithm + " by " + r.key + "  |  comparisons: " + r.comparisons
                            + "  |  time: " + String.format("%.1f", r.micros) + " us");
                    for (Vehicle v : r.sorted) System.out.println("  " + v);
                }
                case 12 -> {
                    System.out.print("Algorithm (linear/binary): ");
                    String algo = sc.nextLine().trim();
                    System.out.print("Vehicle number to find: ");
                    String num = sc.nextLine().trim();
                    SearchAlgorithms.SearchResult r = system.findVehicle(algo, num);
                    System.out.println(r.algorithm + "  |  comparisons: " + r.comparisons
                            + (r.prepComparisons > 0 ? " (+" + r.prepComparisons + " to sort first)" : "")
                            + "  |  time: " + String.format("%.1f", r.micros) + " us");
                    System.out.println(r.vehicle != null
                            ? "Found at position " + r.position + " of " + r.total + ": " + r.vehicle
                            : "Not found among " + r.total + " parked vehicles.");
                }
                                case 13 -> {
                    System.out.println("Benchmark on generated data (comparison counts are exact; times vary):");
                    System.out.printf("%-6s | %-22s | %-22s | %-12s | %-12s | %-24s%n", "n",
                            "Merge sort cmp (us)", "Insertion sort cmp (us)", "Linear avg", "Binary avg",
                            AlgorithmComparison.BATCH_LOOKUPS + " lookups: linear vs sort+binary");
                    for (AlgorithmComparison.Row r : AlgorithmComparison.benchmark(AlgorithmComparison.DEFAULT_SIZES)) {
                        System.out.printf("%-6d | %-22s | %-22s | %-12.1f | %-12.1f | %-10d vs %-10d%n", r.n,
                                r.mergeCmp + " (" + Math.round(r.mergeMicros) + ")",
                                r.insertionCmp + " (" + Math.round(r.insertionMicros) + ")",
                                r.linearAvg, r.binaryAvg, r.batchLinear, r.batchSortBinary);
                    }
                    System.out.println("Theory: merge O(n log n), insertion O(n^2), linear O(n), binary O(log n).");
                }
                case 14 -> {
                    System.out.print("Vehicle numbers to find, separated by commas: ");
                    List<String> nums = new ArrayList<>();
                    for (String part : sc.nextLine().split(",")) if (!part.isBlank()) nums.add(part.trim());
                    AlgorithmComparison.BatchResult b = system.batchFind(nums);
                    for (AlgorithmComparison.BatchItem it : b.items) {
                        System.out.println("  " + it.number + " -> " + (it.found ? "slot " + it.slot : "not found")
                                + "  (linear " + it.linearCmp + " cmp, binary " + it.binaryCmp + " cmp)");
                    }
                    System.out.println("Linear total: " + b.linearTotal + " comparisons");
                    System.out.println("Sort once + binary: " + b.sortCost + " + " + b.binaryTotal
                            + " = " + b.binaryWithSort() + " comparisons");
                }

                                case 15 -> {
                    System.out.print("Number of reserved slots (1-5): ");
                    int k = Integer.parseInt(sc.nextLine().trim());
                    System.out.println("Enter bookings as: VEHICLE HH:MM HH:MM   (blank line to finish)");
                    List<ReservationScheduler.Request> reqs = new ArrayList<>();
                    while (true) {
                        String line = sc.nextLine();
                        if (line.isBlank()) break;
                        try {
                            reqs.add(ReservationScheduler.parse(line, reqs.size()));
                        } catch (IllegalArgumentException e) {
                            System.out.println("  Skipped: " + e.getMessage());
                        }
                    }
                    if (k < 1 || k > 5) { System.out.println("Slots must be between 1 and 5."); }
                    else {
                        ReservationScheduler.Plan g = ReservationScheduler.scheduleGreedy(reqs, k);
                        ReservationScheduler.Plan f = ReservationScheduler.scheduleFcfs(reqs, k);
                        System.out.println("Greedy (earliest finish first): accepted " + g.acceptedCount() + " of " + reqs.size());
                        for (ReservationScheduler.Assignment a : g.accepted)
                            System.out.println("  Reserved slot R" + a.slot + ": " + a.request.vehicleNumber
                                    + " " + a.request.startText() + "-" + a.request.endText());
                        for (ReservationScheduler.Request r : g.rejected)
                            System.out.println("  Rejected: " + r.vehicleNumber + " " + r.startText() + "-" + r.endText());
                        System.out.println("First-come-first-served would accept only " + f.acceptedCount() + " of " + reqs.size());
                    }
                }

                case 0 -> System.out.println("Exiting. Goodbye!");
                default -> System.out.println("Invalid choice, try again.");
            }
            System.out.println();
        } while (choice != 0);
 
        sc.close();
    }
 
    /** Requirement: "Defining project input data (list of elements/records)" */
    private static void loadSampleData(ParkingSystem system) {
        system.vehicleEntry("KA05AB1234", "Ravi Kumar", "CAR", "09:00");
        system.vehicleEntry("KA05CD5678", "Priya Singh", "BIKE", "09:05");
        system.vehicleEntry("KA05EF9012", "Dr. Anita Rao", "STAFF", "08:30");
        system.vehicleEntry("KA05GH3456", "Suresh Babu", "VISITOR", "09:10");
        system.vehicleEntry("KA05IJ7890", "Meena Iyer", "CAR", "09:15");
        // Lot (5 slots) is now full — the next entry should join the waiting queue
        system.vehicleEntry("KA05KL2468", "Arjun Nair", "CAR", "09:20");
        System.out.println();
    }
 
    private static void printMenu() {
        System.out.println("========== Campus Parking Management System ==========");
        System.out.println("1. Vehicle Entry");
        System.out.println("2. Vehicle Exit");
        System.out.println("3. Display All Parked Vehicles (traverse)");
        System.out.println("4. Search Vehicle by Number");
        System.out.println("5. Display Waiting Queue");
        System.out.println("6. Display Recently Exited (Stack)");
        System.out.println("7. Check Expression Bracket Balance (Stack demo)");
        System.out.println("---- Phase 2: Trees & Graphs ----");
        System.out.println("8. Show BST Traversals (inorder/preorder/postorder)");
        System.out.println("9. Search Vehicle by Slot (BST)");
        System.out.println("10. Graph BFS over Campus Zones");
        System.out.println("11. Sort Parked Vehicles (Merge/Insertion Sort)");
        System.out.println("12. Find Vehicle by Number (Search)");
        System.out.println("13. Benchmark: compare all algorithms");
        System.out.println("14. Batch Find (sort once, search many)");
        System.out.println("---- Phase 4: Algorithms ----");
        System.out.println("15. Reservation Planner (greedy)");
        System.out.println("0. Exit");
        System.out.print("Enter choice: ");
    }
 
    private static int readInt(Scanner sc) {
        int value = Integer.parseInt(sc.nextLine().trim());
        return value;
    }
}