package za.co.lightshed.placename;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Cleans raw "Town,Province" rows: trims whitespace, normalises case and province aliases,
 * drops invalid rows (blank fields, unknown province, non-alphabetic names) and removes duplicates.
 */
public class PlaceNameCleaner {

    public static final List<String> PROVINCES = List.of(
            "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
            "Mpumalanga", "North West", "Northern Cape", "Western Cape");

    private static final Map<String, String> PROVINCE_LOOKUP = new HashMap<>();
    static {
        for (String p : PROVINCES) PROVINCE_LOOKUP.put(key(p), p);
        PROVINCE_LOOKUP.put(key("KZN"), "KwaZulu-Natal");
        PROVINCE_LOOKUP.put(key("Kwazulu Natal"), "KwaZulu-Natal");
        PROVINCE_LOOKUP.put(key("North-West"), "North West");
    }

    /** Result of a cleaning run, including how many rows were rejected. */
    public record Result(List<Place> places, int rejected, int duplicates) {}

    public Result clean(Reader reader) throws IOException {
        Set<Place> unique = new TreeSet<>(Comparator
                .comparing(Place::province).thenComparing(Place::name));
        int rejected = 0, duplicates = 0;
        try (BufferedReader br = new BufferedReader(reader)) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                Optional<Place> place = cleanRow(line);
                if (place.isEmpty()) { rejected++; continue; }
                if (!unique.add(place.get())) duplicates++;
            }
        }
        return new Result(new ArrayList<>(unique), rejected, duplicates);
    }

    Optional<Place> cleanRow(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 2) return Optional.empty();
        String name = titleCase(parts[0]);
        String province = PROVINCE_LOOKUP.get(key(parts[1]));
        if (name.isEmpty() || province == null) return Optional.empty();
        if (!name.matches("[\\p{L}][\\p{L} .'-]*")) return Optional.empty();
        return Optional.of(new Place(name, province));
    }

    private static String key(String s) {
        return s.trim().toLowerCase().replaceAll("[\\s-]+", " ");
    }

    static String titleCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String word : s.trim().toLowerCase().split("\\s+")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
