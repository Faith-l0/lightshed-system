package za.co.lightshed.schedule;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a sample 7-day schedule. Higher stage = more outages per day. The slot times are
 * deterministic per town so repeated calls return the same schedule. (Sample logic: swap in
 * your real schedule data when you have it.)
 */
public class ScheduleGenerator {
    public static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private static final int SLOT_MINUTES = 150; // 2h30

    public record Slot(String start, String end) {}
    public record Day(String day, List<Slot> slots) {}
    public record Schedule(String town, String province, int stage, List<Day> days) {}

    /** Outages per day: stage 0 -> 0, 1-2 -> 1, 3-4 -> 2, 5-6 -> 3, 7-8 -> 4. */
    static int slotsPerDay(int stage) { return (stage + 1) / 2; }

    public Schedule generate(Place place, int stage) {
        int offsetHours = Math.floorMod((place.name() + place.province()).toLowerCase().hashCode(), 6);
        int perDay = slotsPerDay(stage);
        List<Day> days = new ArrayList<>();
        for (int d = 0; d < DAYS.length; d++) {
            List<Slot> slots = new ArrayList<>();
            for (int i = 0; i < perDay; i++) {
                int startMinutes = Math.floorMod((offsetHours + d * 2 + i * 6) * 60, 24 * 60);
                slots.add(new Slot(format(startMinutes), format((startMinutes + SLOT_MINUTES) % (24 * 60))));
            }
            days.add(new Day(DAYS[d], slots));
        }
        return new Schedule(place.name(), place.province(), stage, days);
    }

    private static String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
