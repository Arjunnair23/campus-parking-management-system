import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ApiServer.java
 * -------------------------------------------------------
 * Runs the Campus Parking Management System as a local web
 * app: serves the static frontend from ./web and exposes a
 * JSON REST API over the existing ParkingSystem.
 *
 * Run it from the project ROOT folder (the one containing
 * src/, web/, lib/ and db.properties):
 *
 *   javac -d bin src/*.java
 *   java -cp "bin;lib/*" ApiServer
 *   then open http://localhost:8080 in a browser
 * -------------------------------------------------------
 */
public class ApiServer {

    private static ParkingSystem system;

    // ---- authentication state ----
    private static UserStore users;
    private static final long SESSION_MILLIS = 8L * 60 * 60 * 1000; // 8 hours
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    /** An active login: who it belongs to and when it expires. */
    private static class Session {
        final User user;
        final long expiresAt;
        Session(User user, long expiresAt) { this.user = user; this.expiresAt = expiresAt; }
    }
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        system = new ParkingSystem(10);
        try {
            users = new UserStore("db.properties");
        } catch (RuntimeException e) {
            System.err.println();
            System.err.println("Database error: " + e.getMessage());
            System.err.println("Server not started.");
            System.exit(1);
        }
        loadSampleData();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/parked", secured(ApiServer::handleParked));
        server.createContext("/api/queue", secured(ApiServer::handleQueue));
        server.createContext("/api/exits", secured(ApiServer::handleExits));
        server.createContext("/api/search", secured(ApiServer::handleSearch));
        server.createContext("/api/entry", secured(ApiServer::handleEntry));
        server.createContext("/api/exit", secured(ApiServer::handleExit));
        server.createContext("/api/expression", secured(ApiServer::handleExpression));
        server.createContext("/api/status", secured(ApiServer::handleStatus));
        server.createContext("/api/tree", secured(ApiServer::handleTree));
        server.createContext("/api/slot", secured(ApiServer::handleSlotSearch));
        server.createContext("/api/graph", secured(ApiServer::handleGraph));
        server.createContext("/api/sort", secured(ApiServer::handleSort));
        server.createContext("/api/find", secured(ApiServer::handleFind));
        server.createContext("/api/compare", secured(ApiServer::handleCompare));
        server.createContext("/api/batch", secured(ApiServer::handleBatch));
        server.createContext("/api/reserve", secured(ApiServer::handleReserve));
        // public auth endpoints
        server.createContext("/api/register", ApiServer::handleRegister);
        server.createContext("/api/login", ApiServer::handleLogin);
        // needs a valid login
        server.createContext("/api/logout", secured(ApiServer::handleLogout));
        server.createContext("/api/me", secured(ApiServer::handleMe));
        server.createContext("/", ApiServer::handleStatic);

        server.setExecutor(null);
        System.out.println("Campus Parking Management System running at http://localhost:8080");
        server.start();
    }

    private static void loadSampleData() {
        system.vehicleEntry("KA05AB1234", "Ravi Kumar", "CAR", "09:00");
        system.vehicleEntry("KA05CD5678", "Priya Singh", "BIKE", "09:05");
        system.vehicleEntry("KA05EF9012", "Dr. Anita Rao", "STAFF", "08:30");
        system.vehicleEntry("KA05GH3456", "Suresh Babu", "VISITOR", "09:10");
        system.vehicleEntry("KA05IJ7890", "Meena Iyer", "CAR", "09:15");
    }

    // ---------------- API handlers ----------------

    private static void handleParked(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        sendJson(ex, 200, JsonUtil.vehicleListToJson(system.getParkedVehicles()));
    }

    private static void handleQueue(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        sendJson(ex, 200, JsonUtil.vehicleListToJson(system.getWaitingVehicles()));
    }

    private static void handleExits(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        sendJson(ex, 200, JsonUtil.vehicleListToJson(system.getExitHistory()));
    }

    private static void handleStatus(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String json = "{\"occupied\":" + system.getOccupiedCount()
                + ",\"totalSlots\":" + system.getTotalSlots()
                + ",\"waiting\":" + system.getWaitingVehicles().size() + "}";
        sendJson(ex, 200, json);
    }

    private static void handleSearch(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String vehicleNumber = extractParam(ex.getRequestURI().getQuery(), "vehicleNumber");
        Vehicle v = (vehicleNumber == null || vehicleNumber.isBlank())
                ? null : system.searchVehicle(vehicleNumber);
        if (v == null) {
            sendJson(ex, 404, "{\"found\":false}");
        } else {
            sendJson(ex, 200, "{\"found\":true,\"vehicle\":" + JsonUtil.vehicleToJson(v) + "}");
        }
    }

    private static void handleEntry(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

        Map<String, String> data = JsonUtil.parseFlatObject(readBody(ex));
        String vehicleNumber = data.getOrDefault("vehicleNumber", "").trim();
        String ownerName = data.getOrDefault("ownerName", "").trim();
        String vehicleType = data.getOrDefault("vehicleType", "CAR").trim();

        if (vehicleNumber.isEmpty() || ownerName.isEmpty()) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"Vehicle number and owner name are required.\"}");
            return;
        }

        String entryTime = java.time.LocalTime.now().withNano(0).toString();
        ParkingSystem.Result r = system.vehicleEntry(vehicleNumber, ownerName, vehicleType, entryTime);
        sendJson(ex, r.success ? 200 : 409,
                "{\"success\":" + r.success + ",\"message\":\"" + JsonUtil.escape(r.message) + "\"}");
    }

    private static void handleExit(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

        Map<String, String> data = JsonUtil.parseFlatObject(readBody(ex));
        String vehicleNumber = data.getOrDefault("vehicleNumber", "").trim();
        ParkingSystem.Result r = system.vehicleExit(vehicleNumber);
        sendJson(ex, r.success ? 200 : 404,
                "{\"success\":" + r.success + ",\"message\":\"" + JsonUtil.escape(r.message) + "\"}");
    }

    private static void handleExpression(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

        Map<String, String> data = JsonUtil.parseFlatObject(readBody(ex));
        String expr = data.getOrDefault("expression", "");
        boolean balanced = ExpressionHandler.isBalanced(expr);
        sendJson(ex, 200, "{\"balanced\":" + balanced + "}");
    }

    // ---------------- Authentication ----------------

    /** Wraps a handler so it only runs for requests carrying a valid session token. */
    private static HttpHandler secured(HttpHandler inner) {
        return ex -> {
            if ("OPTIONS".equals(ex.getRequestMethod())) { inner.handle(ex); return; }
            if (currentSession(ex) == null) {
                withCors(ex);
                sendJson(ex, 401, "{\"success\":false,\"message\":\"Please log in.\"}");
                return;
            }
            inner.handle(ex);
        };
    }

    private static String bearerToken(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7).trim();
        return null;
    }

    /** Returns the live session for this request, or null if missing/expired. */
    private static Session currentSession(HttpExchange ex) {
        String token = bearerToken(ex);
        if (token == null) return null;
        Session s = sessions.get(token);
        if (s == null) return null;
        if (System.currentTimeMillis() > s.expiresAt) {
            sessions.remove(token);
            return null;
        }
        return s;
    }

    private static String newToken() {
        byte[] b = new byte[32];
        TOKEN_RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static void handleRegister(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

        Map<String, String> d = JsonUtil.parseFlatObject(readBody(ex));
        String error = users.register(d.get("fullName"), d.get("username"), d.get("password"));
        if (error != null) {
            boolean taken = error.contains("already taken");
            sendJson(ex, taken ? 409 : 400,
                    "{\"success\":false,\"message\":\"" + JsonUtil.escape(error) + "\"}");
            return;
        }
        sendJson(ex, 200, "{\"success\":true,\"message\":\"Account created. You can log in now.\"}");
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        withCors(ex);
        if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

        Map<String, String> d = JsonUtil.parseFlatObject(readBody(ex));
        User u = users.authenticate(d.get("username"), d.get("password"));
        if (u == null) {
            sendJson(ex, 401, "{\"success\":false,\"message\":\"Invalid username or password.\"}");
            return;
        }
        String token = newToken();
        sessions.put(token, new Session(u, System.currentTimeMillis() + SESSION_MILLIS));
        sendJson(ex, 200, "{\"success\":true,\"token\":\"" + token + "\",\"username\":\""
                + JsonUtil.escape(u.getUsername()) + "\",\"fullName\":\""
                + JsonUtil.escape(u.getFullName()) + "\"}");
    }

    private static void handleLogout(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"POST".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String token = bearerToken(ex);
        if (token != null) sessions.remove(token);
        sendJson(ex, 200, "{\"success\":true}");
    }

    private static void handleMe(HttpExchange ex) throws IOException {
        withCors(ex);
        Session s = currentSession(ex);
        sendJson(ex, 200, "{\"username\":\"" + JsonUtil.escape(s.user.getUsername())
                + "\",\"fullName\":\"" + JsonUtil.escape(s.user.getFullName()) + "\"}");
    }

        // ---------------- Phase 4A handlers (greedy reservation planner) ----------------

    /** GET /api/reserve?slots=2&requests=KA05AB1234 09:00 10:00;KA05CD5678 09:30 11:00 */
    private static void handleReserve(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String q = ex.getRequestURI().getQuery();

        int slots;
        try {
            slots = Integer.parseInt(String.valueOf(extractParam(q, "slots")).trim());
        } catch (NumberFormatException e) {
            slots = -1;
        }
        if (slots < 1 || slots > 5) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"Reserved slots must be a number from 1 to 5.\"}");
            return;
        }

        String raw = extractParam(q, "requests");
        java.util.List<ReservationScheduler.Request> reqs = new java.util.ArrayList<>();
        if (raw != null) {
            int lineNo = 0;
            for (String line : raw.split("[;\\r\\n]+")) {
                if (line.isBlank()) continue;
                lineNo++;
                if (lineNo > 100) {
                    sendJson(ex, 400, "{\"success\":false,\"message\":\"At most 100 bookings at a time.\"}");
                    return;
                }
                try {
                    reqs.add(ReservationScheduler.parse(line, reqs.size()));
                } catch (IllegalArgumentException e) {
                    sendJson(ex, 400, "{\"success\":false,\"message\":\"Booking " + lineNo + ": "
                            + JsonUtil.escape(e.getMessage()) + "\"}");
                    return;
                }
            }
        }
        if (reqs.isEmpty()) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"Enter at least one booking, one per line.\"}");
            return;
        }

        ReservationScheduler.Plan g = ReservationScheduler.scheduleGreedy(reqs, slots);
        ReservationScheduler.Plan f = ReservationScheduler.scheduleFcfs(reqs, slots);

        java.util.List<ReservationScheduler.Assignment> shown = new java.util.ArrayList<>(g.accepted);
        shown.sort(java.util.Comparator.<ReservationScheduler.Assignment>comparingInt(a -> a.slot)
                .thenComparingInt(a -> a.request.start));

        StringBuilder sb = new StringBuilder("{\"success\":true,\"slots\":" + slots
                + ",\"total\":" + reqs.size()
                + ",\"greedy\":{\"count\":" + g.acceptedCount() + ",\"accepted\":[");
        for (int i = 0; i < shown.size(); i++) {
            ReservationScheduler.Assignment a = shown.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"vehicle\":\"").append(JsonUtil.escape(a.request.vehicleNumber))
              .append("\",\"start\":\"").append(a.request.startText())
              .append("\",\"end\":\"").append(a.request.endText())
              .append("\",\"slot\":").append(a.slot).append("}");
        }
        sb.append("],\"rejected\":[");
        for (int i = 0; i < g.rejected.size(); i++) {
            ReservationScheduler.Request r = g.rejected.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"vehicle\":\"").append(JsonUtil.escape(r.vehicleNumber))
              .append("\",\"start\":\"").append(r.startText())
              .append("\",\"end\":\"").append(r.endText()).append("\"}");
        }
        sb.append("]},\"fcfs\":{\"count\":").append(f.acceptedCount()).append("}}");
        sendJson(ex, 200, sb.toString());
    }
    
        // ---------------- Phase 3C handlers (comparison) ----------------

    /** GET /api/compare -> benchmark of all four algorithms on generated data. */
    private static void handleCompare(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        java.util.Locale us = java.util.Locale.US;
        StringBuilder sb = new StringBuilder("{\"lookups\":" + AlgorithmComparison.BATCH_LOOKUPS + ",\"rows\":[");
        boolean first = true;
        for (AlgorithmComparison.Row r : AlgorithmComparison.benchmark(AlgorithmComparison.DEFAULT_SIZES)) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"n\":").append(r.n)
              .append(",\"mergeCmp\":").append(r.mergeCmp)
              .append(",\"mergeMicros\":").append(String.format(us, "%.1f", r.mergeMicros))
              .append(",\"insertionCmp\":").append(r.insertionCmp)
              .append(",\"insertionMicros\":").append(String.format(us, "%.1f", r.insertionMicros))
              .append(",\"linearAvg\":").append(String.format(us, "%.1f", r.linearAvg))
              .append(",\"binaryAvg\":").append(String.format(us, "%.1f", r.binaryAvg))
              .append(",\"batchLinear\":").append(r.batchLinear)
              .append(",\"batchSortBinary\":").append(r.batchSortBinary)
              .append("}");
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString());
    }

    /** GET /api/batch?numbers=KA05AB1234,KA05CD5678 -> sort once, then find each number both ways. */
    private static void handleBatch(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String raw = extractParam(ex.getRequestURI().getQuery(), "numbers");
        java.util.List<String> numbers = new java.util.ArrayList<>();
        if (raw != null) {
            for (String part : raw.split("[,\\s]+")) {
                if (!part.isBlank() && numbers.size() < 50) numbers.add(part.trim());
            }
        }
        if (numbers.isEmpty()) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"Enter one or more vehicle numbers, separated by commas.\"}");
            return;
        }
        AlgorithmComparison.BatchResult b = system.batchFind(numbers);
        StringBuilder sb = new StringBuilder("{\"success\":true,\"parked\":" + b.parkedCount
                + ",\"linearTotal\":" + b.linearTotal
                + ",\"sortCost\":" + b.sortCost
                + ",\"binaryTotal\":" + b.binaryTotal
                + ",\"binaryWithSort\":" + b.binaryWithSort()
                + ",\"items\":[");
        for (int i = 0; i < b.items.size(); i++) {
            AlgorithmComparison.BatchItem it = b.items.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"number\":\"").append(JsonUtil.escape(it.number))
              .append("\",\"found\":").append(it.found)
              .append(",\"slot\":").append(it.slot)
              .append(",\"linearCmp\":").append(it.linearCmp)
              .append(",\"binaryCmp\":").append(it.binaryCmp).append("}");
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString());
    }

        // ---------------- Phase 3 handlers (searching) ----------------

    /** GET /api/find?algo=linear|binary&number=KA05... -> search outcome + comparison counts. */
    private static void handleFind(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String q = ex.getRequestURI().getQuery();
        String number = extractParam(q, "number");
        if (number == null || number.isBlank()) {
            sendJson(ex, 400, "{\"success\":false,\"message\":\"Enter a vehicle number.\"}");
            return;
        }
        SearchAlgorithms.SearchResult r = system.findVehicle(extractParam(q, "algo"), number);
        String json = "{\"success\":true,"
                + "\"algorithm\":\"" + JsonUtil.escape(r.algorithm) + "\","
                + "\"found\":" + (r.vehicle != null) + ","
                + "\"position\":" + r.position + ","
                + "\"total\":" + r.total + ","
                + "\"comparisons\":" + r.comparisons + ","
                + "\"prepComparisons\":" + r.prepComparisons + ","
                + "\"micros\":" + String.format(java.util.Locale.US, "%.1f", r.micros)
                + (r.vehicle != null ? ",\"vehicle\":" + JsonUtil.vehicleToJson(r.vehicle) : "")
                + "}";
        sendJson(ex, 200, json);
    }

    // ---------------- Phase 3 handlers (sorting) ----------------

    /** GET /api/sort?algo=merge|insertion&by=slot|number|time -> sorted vehicles + comparison count. */
    private static void handleSort(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String q = ex.getRequestURI().getQuery();
        VehicleSorter.SortResult r = system.sortParked(extractParam(q, "algo"), extractParam(q, "by"));
        String json = "{\"algorithm\":\"" + JsonUtil.escape(r.algorithm) + "\","
                + "\"by\":\"" + r.key.name().toLowerCase() + "\","
                + "\"comparisons\":" + r.comparisons + ","
                + "\"micros\":" + String.format(java.util.Locale.US, "%.1f", r.micros) + ","
                + "\"vehicles\":" + JsonUtil.vehicleListToJson(r.sorted) + "}";
        sendJson(ex, 200, json);
    }

    // ---------------- Phase 2 handlers (BST + graph) ----------------

    /** GET /api/tree -> inorder / preorder / postorder traversals of the BST. */
    private static void handleTree(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String json = "{\"inorder\":" + JsonUtil.vehicleListToJson(system.getBstInorder())
                + ",\"preorder\":" + JsonUtil.vehicleListToJson(system.getBstPreorder())
                + ",\"postorder\":" + JsonUtil.vehicleListToJson(system.getBstPostorder()) + "}";
        sendJson(ex, 200, json);
    }

    /** GET /api/slot?slot=N -> BST lookup by slot number. */
    private static void handleSlotSearch(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String raw = extractParam(ex.getRequestURI().getQuery(), "slot");
        Vehicle v = null;
        try {
            if (raw != null) v = system.searchBySlotBST(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException e) {
            v = null;
        }
        if (v == null) {
            sendJson(ex, 404, "{\"found\":false}");
        } else {
            sendJson(ex, 200, "{\"found\":true,\"vehicle\":" + JsonUtil.vehicleToJson(v) + "}");
        }
    }

    /** GET /api/graph?start=Entrance -> zone adjacency list + BFS visit order. */
    private static void handleGraph(HttpExchange ex) throws IOException {
        withCors(ex);
        if (!"GET".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }
        String start = extractParam(ex.getRequestURI().getQuery(), "start");
        if (start == null || start.isBlank()) start = "Entrance";

        StringBuilder sb = new StringBuilder("{\"start\":\"" + JsonUtil.escape(start) + "\",\"adjacency\":{");
        boolean firstZone = true;
        for (Map.Entry<String, List<String>> e : system.getCampusGraph().getAdjacencyList().entrySet()) {
            if (!firstZone) sb.append(",");
            firstZone = false;
            sb.append("\"").append(JsonUtil.escape(e.getKey())).append("\":[");
            List<String> nbrs = e.getValue();
            for (int i = 0; i < nbrs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(JsonUtil.escape(nbrs.get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("},\"bfs\":[");
        List<String> order = system.bfsFromZone(start);
        for (int i = 0; i < order.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(JsonUtil.escape(order.get(i))).append("\"");
        }
        sb.append("]}");
        sendJson(ex, 200, sb.toString());
    }

    // ---------------- Static file serving ----------------

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";

        Path webRoot = Paths.get("web").normalize();
        Path filePath = Paths.get("web" + path).normalize();

        if (!filePath.startsWith(webRoot) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            byte[] msg = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            OutputStream os = ex.getResponseBody();
            os.write(msg);
            os.close();
            return;
        }

        byte[] bytes = Files.readAllBytes(filePath);
        ex.getResponseHeaders().set("Content-Type", guessContentType(filePath.toString()));
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    // ---------------- Helpers ----------------

    private static void withCors(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static String extractParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try {
                    return URLDecoder.decode(kv[1], "UTF-8");
                } catch (Exception e) {
                    return kv[1];
                }
            }
        }
        return null;
    }
}
