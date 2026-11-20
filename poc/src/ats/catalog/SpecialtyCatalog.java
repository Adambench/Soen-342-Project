package ats.catalog;

import ats.domain.Specialty;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class SpecialtyCatalog {
    private final Map<String, Specialty> specialties = new LinkedHashMap<>();

    public Specialty findOrCreate(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        return specialties.computeIfAbsent(key, k -> new Specialty(name.trim()));
    }

    public Specialty findByName(String name) {
        return name == null ? null : specialties.get(name.trim().toLowerCase(Locale.ROOT));
    }

    public Collection<Specialty> getAll() {
        return specialties.values();
    }
}
