import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReservationScheduler.java  (Phase 4A - greedy technique)
 * -------------------------------------------------------
 * PROBLEM
 *   The campus has a few RESERVED parking slots (for events, VIPs, visitors).
 *   People send booking requests, each with a start and an end time.
 *   One slot can hold only one booking at a time (bookings may touch: one
 *   ending at 11:00 and the next starting at 11:00 is fine).
 *   Goal: accept as MANY bookings as possible.
 *
 * GREEDY CHOICE  (interval scheduling, earliest finish time first)
 *   1. Sort the requests by END time, earliest first.
 *   2. Take each request in that order. Give it to the reserved slot that
 *      became free most recently but is still free by the request's start
 *      ("best fit"). If no slot is free in time, reject the request.
 *
 *   Why it works: finishing early leaves the most room for later requests,
 *   so picking the request that ends first never hurts the final count.
 *   Time: O(n log n) for the sort + O(n * k) to place n requests into k slots.
 *
 * BASELINE
 *   scheduleFcfs() handles requests in the order they START and gives each to
 *   the first free slot. It is simple and "fair" but can accept far fewer
 *   bookings - the comparison shows why the greedy choice matters.
 * -------------------------------------------------------
 */
public class ReservationScheduler {

    /** One booking request. Times are minutes since midnight; the booking is [start, end). */
    public static class Request {
        public final String vehicleNumber;
        public final int start;
        public final int end;
        final int order; // position in the input, used to keep sorting deterministic

        Request(String vehicleNumber, int start, int end, int order) {
            this.vehicleNumber = vehicleNumber;
            this.start = start;
            this.end = end;
            this.order = order;
        }

        public String startText() { return clock(start); }
        public String endText() { return clock(end); }
    }

    /** An accepted request and the reserved slot (1..k) it was given. */
    public static class Assignment {
        public final Request request;
        public final int slot;
        Assignment(Request request, int slot) { this.request = request; this.slot = slot; }
    }

    public static class Plan {
        public final List<Assignment> accepted = new ArrayList<>();
        public final List<Request> rejected = new ArrayList<>();
        public int acceptedCount() { return accepted.size(); }
    }

    // ---------------- parsing ----------------

    private static final Pattern LINE = Pattern.compile(
            "^([A-Za-z0-9_-]{1,20})\\s+(\\d{1,2}):(\\d{2})\\s+(\\d{1,2}):(\\d{2})$");

    /**
     * Parses one line "VEHICLE HH:MM HH:MM", e.g. "KA05AB1234 09:00 11:30".
     * @throws IllegalArgumentException with a readable message if the line is invalid
     */
    public static Request parse(String line, int order) {
        Matcher m = LINE.matcher(line == null ? "" : line.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("expected 'VEHICLE HH:MM HH:MM' (for example KA05AB1234 09:00 11:30)");
        }
        int sh = Integer.parseInt(m.group(2)), sm = Integer.parseInt(m.group(3));
        int eh = Integer.parseInt(m.group(4)), em = Integer.parseInt(m.group(5));
        if (sh > 23 || eh > 23 || sm > 59 || em > 59) {
            throw new IllegalArgumentException("time must be between 00:00 and 23:59");
        }
        int start = sh * 60 + sm, end = eh * 60 + em;
        if (end <= start) {
            throw new IllegalArgumentException("end time must be after start time");
        }
        return new Request(m.group(1), start, end, order);
    }

    // ---------------- the greedy algorithm ----------------

    /** Earliest-finish-time greedy over {@code slots} reserved slots. */
    public static Plan scheduleGreedy(List<Request> requests, int slots) {
        List<Request> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.<Request>comparingInt(r -> r.end)
                .thenComparingInt(r -> r.start)
                .thenComparingInt(r -> r.order));

        int[] freeAt = new int[slots + 1];            // freeAt[s] = minute slot s becomes free (index 0 unused)
        for (int s = 1; s <= slots; s++) freeAt[s] = -1;

        Plan plan = new Plan();
        for (Request r : sorted) {
            int best = 0;
            for (int s = 1; s <= slots; s++) {
                // slot is usable if it is free by r.start; prefer the one freed latest (best fit)
                if (freeAt[s] <= r.start && (best == 0 || freeAt[s] > freeAt[best])) best = s;
            }
            if (best == 0) {
                plan.rejected.add(r);
            } else {
                freeAt[best] = r.end;
                plan.accepted.add(new Assignment(r, best));
            }
        }
        return plan;
    }

    /** Baseline: first come (earliest start) first served, first free slot. */
    public static Plan scheduleFcfs(List<Request> requests, int slots) {
        List<Request> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.<Request>comparingInt(r -> r.start).thenComparingInt(r -> r.order));

        int[] freeAt = new int[slots + 1];
        for (int s = 1; s <= slots; s++) freeAt[s] = -1;

        Plan plan = new Plan();
        for (Request r : sorted) {
            int chosen = 0;
            for (int s = 1; s <= slots && chosen == 0; s++) {
                if (freeAt[s] <= r.start) chosen = s;
            }
            if (chosen == 0) {
                plan.rejected.add(r);
            } else {
                freeAt[chosen] = r.end;
                plan.accepted.add(new Assignment(r, chosen));
            }
        }
        return plan;
    }

    static String clock(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}