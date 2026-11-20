import java.util.List;
 
/**
 * ParkingSystem.java
 * -------------------------------------------------------
 * Coordinates the three linear data structures into one
 * working parking workflow:
 *   - ParkedVehicleList (linked list) -> currently parked
 *   - LinkedQueue                     -> waiting vehicles
 *   - LinkedStack                     -> recently exited vehicles
 *
 * Phase 2: vehicleEntry / vehicleExit now ALSO return a
 * Result object so the REST API (ApiServer) can serialize
 * the outcome as JSON. They still print to the console, so
 * the Phase-1 console menu (Main) works exactly as before
 * (Main simply ignores the returned Result).
 * -------------------------------------------------------
 */
public class ParkingSystem {
 
    /** Simple outcome record shared by the console menu and the REST API. */
    public static class Result {
        public final boolean success;
        public final String message;
        public final Vehicle vehicle;      // primary vehicle (parked / exited)
        public final Vehicle movedVehicle; // auto-parked from queue (exit flow only)
 
        public Result(boolean success, String message, Vehicle vehicle, Vehicle movedVehicle) {
            this.success = success;
            this.message = message;
            this.vehicle = vehicle;
            this.movedVehicle = movedVehicle;
        }
    }
 
    private ParkedVehicleList parkedList;
    private LinkedQueue<Vehicle> waitingQueue;
    private LinkedStack<Vehicle> exitStack;
 
    // Phase 2: BST index (keyed by slot number) kept alongside parkedList,
    // and a graph model of the campus parking zones.
    private VehicleBST slotIndex;
    private ParkingGraph campusGraph;
 
    private final int totalSlots;
    private boolean[] slotOccupied;
 
    public ParkingSystem(int totalSlots) {
        this.totalSlots = totalSlots;
        this.parkedList = new ParkedVehicleList();
        this.waitingQueue = new LinkedQueue<>();
        this.exitStack = new LinkedStack<>();
        this.slotOccupied = new boolean[totalSlots + 1]; // 1-indexed
        this.slotIndex = new VehicleBST();
        this.campusGraph = ParkingGraph.buildCampusLayout();
    }
 
    /** Finds the first free slot, or -1 if the lot is full. */
    private int findFreeSlot() {
        for (int i = 1; i <= totalSlots; i++) {
            if (!slotOccupied[i]) return i;
        }
        return -1;
    }
 
    private boolean isWaiting(String vehicleNumber) {
        for (Vehicle v : waitingQueue.toList()) {
            if (v.getVehicleNumber().equalsIgnoreCase(vehicleNumber)) return true;
        }
        return false;
    }
 
    /**
     * Vehicle entry: checks duplicates, assigns a slot or queues the vehicle.
     * Prints to console (Phase 1 behaviour) AND returns a Result (Phase 2 API).
     */
    public synchronized Result vehicleEntry(String vehicleNumber, String ownerName, String vehicleType, String entryTime) {
        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            String msg = "Rejected: vehicle number is required.";
            System.out.println(msg);
            return new Result(false, msg, null, null);
        }
        vehicleNumber = vehicleNumber.trim();
        if (parkedList.exists(vehicleNumber) || isWaiting(vehicleNumber)) {
            String msg = "Rejected: Vehicle " + vehicleNumber + " is already parked or waiting.";
            System.out.println(msg);
            return new Result(false, msg, null, null);
        }
 
        int slot = findFreeSlot();
        if (slot == -1) {
            // Lot full -> vehicle joins waiting queue (temporary slot 0 placeholder)
            Vehicle waiting = new Vehicle(vehicleNumber, ownerName, vehicleType, 0, entryTime);
            waitingQueue.enqueue(waiting);
            String msg = "Lot full. Vehicle " + vehicleNumber + " added to waiting queue.";
            System.out.println(msg);
            return new Result(true, msg, waiting, null);
        }
 
        slotOccupied[slot] = true;
        Vehicle v = new Vehicle(vehicleNumber, ownerName, vehicleType, slot, entryTime);
        parkedList.insert(v);
        slotIndex.insert(v); // keep the BST index in sync
        String msg = "Vehicle " + vehicleNumber + " parked at Slot " + slot + ".";
        System.out.println(msg);
        return new Result(true, msg, v, null);
    }
 
    /**
     * Vehicle exit: removes from parked list, frees slot, pushes to exit stack,
     * pulls next from queue. Prints to console AND returns a Result.
     */
    public synchronized Result vehicleExit(String vehicleNumber) {
        Vehicle removed = parkedList.delete(vehicleNumber);
        if (removed == null) {
            String msg = "Vehicle " + vehicleNumber + " not found among parked vehicles.";
            System.out.println(msg);
            return new Result(false, msg, null, null);
        }
 
        slotOccupied[removed.getSlotNumber()] = false;
        exitStack.push(removed);
        slotIndex.delete(removed.getSlotNumber()); // keep the BST index in sync
        String msg = "Vehicle " + vehicleNumber + " exited from Slot " + removed.getSlotNumber() + ".";
        System.out.println(msg);
 
        // Auto-assign the freed slot to the next waiting vehicle, if any
        Vehicle moved = null;
        if (!waitingQueue.isEmpty()) {
            Vehicle next = waitingQueue.dequeue();
            int slot = findFreeSlot();
            slotOccupied[slot] = true;
            moved = new Vehicle(next.getVehicleNumber(), next.getOwnerName(),
                    next.getVehicleType(), slot, next.getEntryTime());
            parkedList.insert(moved);
            slotIndex.insert(moved); // keep the BST index in sync
            String moveMsg = "Vehicle " + moved.getVehicleNumber() +
                    " moved from waiting queue to Slot " + slot + ".";
            System.out.println(moveMsg);
            msg = msg + " " + moveMsg;
        }
        return new Result(true, msg, removed, moved);
    }
 
    public void showParked() { parkedList.traverse(); }
    public void showWaitingQueue() { waitingQueue.display(); }
    public void showExitHistory() { exitStack.display(); }
 
    public synchronized Vehicle searchVehicle(String vehicleNumber) {
        return parkedList.search(vehicleNumber);
    }
 
    // ---- Getters for the REST API ----
 
    public synchronized List<Vehicle> getParkedVehicles() {
        return parkedList.toList();
    }
 
    public synchronized List<Vehicle> getWaitingVehicles() {
        return waitingQueue.toList();
    }
 
    public synchronized List<Vehicle> getExitHistory() {
        return exitStack.toList();
    }
 
    public int getOccupiedCount() {
        return parkedList.getSize();
    }
 
    public int getWaitingCount() {
        return waitingQueue.getSize();
    }
 
    public int getExitCount() {
        return exitStack.getSize();
    }
 
    public int getAvailableCount() {
        return totalSlots - parkedList.getSize();
    }
 
    public int getTotalSlots() {
        return totalSlots;
    }
        // ---- Phase 3C: comparison + batch lookup ----

    /** Sorts the parked vehicles once, then finds every given vehicle number (binary vs linear cost). */
    public synchronized AlgorithmComparison.BatchResult batchFind(List<String> numbers) {
        return AlgorithmComparison.batch(parkedList.toList(), numbers);
    }
        // ---- Phase 3: searching ----

    /**
     * Finds a parked vehicle by number. algo: "linear" or "binary".
     * Binary search first merge-sorts a copy by vehicle number (the cost of that
     * is reported separately as prepComparisons).
     */
    public synchronized SearchAlgorithms.SearchResult findVehicle(String algo, String vehicleNumber) {
        List<Vehicle> snapshot = parkedList.toList();
        if ("binary".equalsIgnoreCase(algo)) {
            VehicleSorter.SortResult sorted = new VehicleSorter().mergeSort(snapshot, VehicleSorter.Key.NUMBER);
            SearchAlgorithms.SearchResult r = SearchAlgorithms.binarySearch(sorted.sorted, vehicleNumber);
            r.prepComparisons = sorted.comparisons;
            return r;
        }
        return SearchAlgorithms.linearSearch(snapshot, vehicleNumber);
    }

    // ---- Phase 3: sorting ----

    /** Sorts a copy of the parked vehicles. algo: "merge" or "insertion"; key: "slot", "number" or "time". */
    public synchronized VehicleSorter.SortResult sortParked(String algo, String key) {
        VehicleSorter sorter = new VehicleSorter();
        VehicleSorter.Key k = VehicleSorter.parseKey(key);
        List<Vehicle> snapshot = parkedList.toList();
        if ("insertion".equalsIgnoreCase(algo)) return sorter.insertionSort(snapshot, k);
        return sorter.mergeSort(snapshot, k);
    }
 
    // ---- Phase 2: BST index (binary tree + BST + traversals) ----
 
    /** O(log n) average-case lookup by slot number, via the BST index. */
    public synchronized Vehicle searchBySlotBST(int slotNumber) {
        return slotIndex.search(slotNumber);
    }
 
    public synchronized List<Vehicle> getBstInorder() { return slotIndex.inorder(); }
    public synchronized List<Vehicle> getBstPreorder() { return slotIndex.preorder(); }
    public synchronized List<Vehicle> getBstPostorder() { return slotIndex.postorder(); }
 
    // ---- Phase 2: campus zone graph + BFS ----
 
    public ParkingGraph getCampusGraph() { return campusGraph; }
 
    /** BFS over the campus zone graph, starting from the given zone (defaults to "Entrance"). */
    public List<String> bfsFromZone(String startZone) {
        return campusGraph.bfs(startZone);
    }
}