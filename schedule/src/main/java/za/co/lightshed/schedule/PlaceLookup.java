package za.co.lightshed.schedule;

import java.util.Optional;

/** Asks the Place-name service whether a town exists. */
public interface PlaceLookup {
    /** @return the properly-cased place, or empty if the province or town is unknown */
    Optional<Place> find(String province, String town) throws ServiceUnavailableException;
}
