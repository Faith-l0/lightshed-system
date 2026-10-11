package za.co.lightshed.placename;

import java.util.List;

/** In-memory store of cleaned places with simple queries. */
public class PlaceNameService {
    private final List<Place> places;

    public PlaceNameService(List<Place> places) { this.places = List.copyOf(places); }

    public List<String> provinces() { return PlaceNameCleaner.PROVINCES; }

    public List<Place> all() { return places; }

    /** Towns in a province (case-insensitive); empty if the province is unknown or has none. */
    public List<Place> townsIn(String province) {
        return places.stream().filter(p -> p.province().equalsIgnoreCase(province.trim())).toList();
    }

    public boolean isKnownProvince(String province) {
        return PlaceNameCleaner.PROVINCES.stream().anyMatch(p -> p.equalsIgnoreCase(province.trim()));
    }
}
