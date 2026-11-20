import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
/**
 * JsonUtil.java
 * -------------------------------------------------------
 * Small hand-written JSON helper so the project needs no
 * external libraries (no Gson/Jackson download required -
 * everything compiles with the plain JDK).
 * Only handles the flat, string/number-only shapes this
 * project actually sends and receives.
 * -------------------------------------------------------
 */
public class JsonUtil {
 
    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
 
    public static String vehicleToJson(Vehicle v) {
        return "{"
                + "\"vehicleNumber\":\"" + escape(v.getVehicleNumber()) + "\","
                + "\"ownerName\":\"" + escape(v.getOwnerName()) + "\","
                + "\"vehicleType\":\"" + escape(v.getVehicleType()) + "\","
                + "\"slotNumber\":" + v.getSlotNumber() + ","
                + "\"entryTime\":\"" + escape(v.getEntryTime()) + "\""
                + "}";
    }
 
    public static String vehicleListToJson(List<Vehicle> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(vehicleToJson(list.get(i)));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
 
    /** Parses a flat JSON object of string fields, e.g. {"a":"b","c":"d"}, into a Map. */
    public static Map<String, String> parseFlatObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) return map;
        String body = json.trim();
        if (body.startsWith("{")) body = body.substring(1);
        if (body.endsWith("}")) body = body.substring(0, body.length() - 1);
 
        int i = 0;
        int n = body.length();
        while (i < n) {
            while (i < n && (body.charAt(i) == ',' || Character.isWhitespace(body.charAt(i)))) i++;
            if (i >= n || body.charAt(i) != '"') break;
 
            int keyStart = ++i;
            while (i < n && body.charAt(i) != '"') i++;
            String key = body.substring(keyStart, i);
            i++; // closing quote
 
            while (i < n && (body.charAt(i) == ':' || Character.isWhitespace(body.charAt(i)))) i++;
 
            String value;
            if (i < n && body.charAt(i) == '"') {
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < n && body.charAt(i) != '"') {
                    if (body.charAt(i) == '\\' && i + 1 < n) {
                        sb.append(body.charAt(i + 1));
                        i += 2;
                    } else {
                        sb.append(body.charAt(i));
                        i++;
                    }
                }
                value = sb.toString();
                i++; // closing quote
            } else {
                int valStart = i;
                while (i < n && body.charAt(i) != ',') i++;
                value = body.substring(valStart, i).trim();
            }
            map.put(key, value);
        }
        return map;
    }
}
 