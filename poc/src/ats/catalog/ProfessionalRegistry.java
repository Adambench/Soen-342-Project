package ats.catalog;

import ats.domain.Availability;
import ats.domain.HealthcareProfessional;
import ats.domain.Specialty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfessionalRegistry {
    private final Map<String, HealthcareProfessional> professionals = new LinkedHashMap<>();
    private int nextId = 1;

    public HealthcareProfessional findById(String professionalId) {
        return professionalId == null ? null : professionals.get(professionalId.trim().toUpperCase());
    }

    /** Both criteria are optional (null): by id, else by specialty name, else everyone. */
    public List<HealthcareProfessional> findProfessionals(String professionalId, String specialty) {
        List<HealthcareProfessional> result = new ArrayList<>();
        if (professionalId != null) {
            HealthcareProfessional hp = findById(professionalId);
            if (hp != null && (specialty == null || hasSpecialtyNamed(hp, specialty))) {
                result.add(hp);
            }
            return result;
        }
        for (HealthcareProfessional hp : professionals.values()) {
            if (specialty == null || hasSpecialtyNamed(hp, specialty)) {
                result.add(hp);
            }
        }
        return result;
    }

    public HealthcareProfessional findOrCreate(String firstName, String lastName, String title, Specialty specialty) {
        for (HealthcareProfessional hp : professionals.values()) {
            if (hp.getFirstName().equalsIgnoreCase(firstName) && hp.getLastName().equalsIgnoreCase(lastName)) {
                hp.addSpecialty(specialty);
                return hp;
            }
        }
        HealthcareProfessional hp = new HealthcareProfessional("HP" + nextId++, firstName, lastName, title);
        hp.addSpecialty(specialty);
        professionals.put(hp.getProfessionalId(), hp);
        return hp;
    }

    public List<HealthcareProfessional> getAll() {
        return new ArrayList<>(professionals.values());
    }

    /** Helper used by the waiting list to resolve an availabilityId. */
    public Availability findAvailability(String availabilityId) {
        for (HealthcareProfessional hp : professionals.values()) {
            for (Availability av : hp.getAvailabilities()) {
                if (av.getAvailabilityId().equalsIgnoreCase(availabilityId)) {
                    return av;
                }
            }
        }
        return null;
    }

    private static boolean hasSpecialtyNamed(HealthcareProfessional hp, String name) {
        return hp.getSpecialties().stream().anyMatch(s -> s.getName().equalsIgnoreCase(name.trim()));
    }
}
