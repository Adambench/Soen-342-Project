package ats.catalog;

import ats.domain.ClinicLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class LocationCatalog {
    private final Map<String, ClinicLocation> locations = new LinkedHashMap<>();

    public ClinicLocation findById(String locationId) {
        return locationId == null ? null : locations.get(locationId.trim().toUpperCase());
    }

    /** The first location added; used when a CSV row has no Location column. */
    public ClinicLocation getDefault() {
        return locations.values().iterator().next();
    }

    /** Accepts a location id or a location name (used by the CSV importer). */
    public ClinicLocation findByIdOrName(String value) {
        ClinicLocation byId = findById(value);
        if (byId != null) {
            return byId;
        }
        for (ClinicLocation loc : locations.values()) {
            if (loc.getName().equalsIgnoreCase(value.trim())) {
                return loc;
            }
        }
        return null;
    }

    public void add(ClinicLocation loc) {
        locations.put(loc.getLocationId(), loc);
    }

    public Collection<ClinicLocation> getAll() {
        return locations.values();
    }
}
