import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ParkingGraph.java
 * -------------------------------------------------------
 * Models the campus parking layout as a graph: the entrance
 * and each vehicle-type zone are nodes, and edges represent
 * the walkable/driveable connections between them.
 *
 * Covers:
 *   - "Introducing basic graph representation (adjacency list/matrix)"
 *   - "Performing simple traversal (BFS or DFS - one approach)"
 *
 * BFS is implemented using this project's own LinkedQueue
 * (from Phase 1) rather than java.util.Queue, since BFS is
 * naturally a queue-driven algorithm - this keeps the graph
 * traversal consistent with the rest of the project.
 * -------------------------------------------------------
 */
public class ParkingGraph {

    // adjacency list: zone name -> list of directly connected zone names
    private final Map<String, List<String>> adjacencyList = new LinkedHashMap<>();

    public void addZone(String zone) {
        adjacencyList.putIfAbsent(zone, new ArrayList<>());
    }

    /** Adds an undirected connection between two zones (both ways walkable). */
    public void addConnection(String zoneA, String zoneB) {
        addZone(zoneA);
        addZone(zoneB);
        adjacencyList.get(zoneA).add(zoneB);
        adjacencyList.get(zoneB).add(zoneA);
    }

    /** Breadth-First Search from a starting zone; returns the order zones were visited in. */
    public List<String> bfs(String start) {
        List<String> visitOrder = new ArrayList<>();
        if (!adjacencyList.containsKey(start)) return visitOrder;

        Set<String> visited = new HashSet<>();
        LinkedQueue<String> queue = new LinkedQueue<>(); // reusing our own Phase-1 queue

        queue.enqueue(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.dequeue();
            visitOrder.add(current);

            for (String neighbor : adjacencyList.get(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.enqueue(neighbor);
                }
            }
        }
        return visitOrder;
    }

    public Map<String, List<String>> getAdjacencyList() {
        return adjacencyList;
    }

    /**
     * Builds the default campus layout:
     *   Entrance -- ZoneA-Car   -- ZoneC-Staff
     *   Entrance -- ZoneB-Bike  -- ZoneD-Visitor -- ZoneC-Staff
     */
    public static ParkingGraph buildCampusLayout() {
        ParkingGraph graph = new ParkingGraph();
        graph.addConnection("Entrance", "ZoneA-Car");
        graph.addConnection("Entrance", "ZoneB-Bike");
        graph.addConnection("ZoneA-Car", "ZoneC-Staff");
        graph.addConnection("ZoneB-Bike", "ZoneD-Visitor");
        graph.addConnection("ZoneC-Staff", "ZoneD-Visitor");
        return graph;
    }
}