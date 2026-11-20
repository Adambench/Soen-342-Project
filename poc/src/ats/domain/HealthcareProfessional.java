package ats.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HealthcareProfessional {
    private final String professionalId;
    private final String firstName;
    private final String lastName;
    private final String title;
    private String professionalInfo = "";
    private final Set<Specialty> specialties = new LinkedHashSet<>();
    private final Set<ClinicLocation> locations = new LinkedHashSet<>();
    private final List<Availability> availabilities = new ArrayList<>();

    public HealthcareProfessional(String professionalId, String firstName, String lastName, String title) {
        this.professionalId = professionalId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
    }

    public boolean practisesAt(ClinicLocation loc) {
        return locations.contains(loc);
    }

    /** A professional offers a service when they have the service's specialty. */
    public boolean offers(MedicalService ms) {
        return hasSpecialty(ms.getSpecialty());
    }

    public boolean hasSpecialty(Specialty s) {
        return specialties.contains(s);
    }

    /** Creator: the professional creates and owns its slots. Returns null when the slot overlaps an existing one. */
    public Availability addAvailability(LocalDate date, LocalTime start, LocalTime end, ClinicLocation loc) {
        if (!start.isBefore(end)) {
            throw new BookingException("start time must be before end time");
        }
        for (Availability av : availabilities) {
            if (av.overlaps(date, start, end)) {
                return null;
            }
        }
        Availability av = new Availability(this, loc, date, start, end);
        availabilities.add(av);
        locations.add(loc);
        return av;
    }

    public Availability findFreeSlot(ClinicLocation loc, LocalDate date, LocalTime time) {
        for (Availability av : availabilities) {
            if (av.getLocation() == loc && av.covers(date, time) && av.isFree()) {
                return av;
            }
        }
        return null;
    }

    public List<Availability> getNextFreeSlots(ClinicLocation loc, LocalDate date, LocalTime time, int max) {
        return availabilities.stream()
                .filter(av -> av.getLocation() == loc && av.isFree())
                .filter(av -> av.getDate().isAfter(date)
                        || (av.getDate().equals(date) && !av.getStartTime().isBefore(time)))
                .sorted(Comparator.comparing(Availability::getDate).thenComparing(Availability::getStartTime))
                .limit(max)
                .collect(Collectors.toList());
    }

    public List<Availability> getOpenAvailabilities(LocalDate dateFrom, LocalDate dateTo, LocalTime timeFrom, LocalTime timeTo) {
        return availabilities.stream()
                .filter(av -> av.isOpenWithin(dateFrom, dateTo, timeFrom, timeTo))
                .collect(Collectors.toList());
    }

    public void addSpecialty(Specialty s) { specialties.add(s); }
    public void addLocation(ClinicLocation loc) { locations.add(loc); }

    public String getProfessionalId() { return professionalId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getTitle() { return title; }
    public String getProfessionalInfo() { return professionalInfo; }
    public void setProfessionalInfo(String info) { this.professionalInfo = info; }
    public Set<Specialty> getSpecialties() { return Collections.unmodifiableSet(specialties); }
    public Set<ClinicLocation> getLocations() { return Collections.unmodifiableSet(locations); }
    public List<Availability> getAvailabilities() { return Collections.unmodifiableList(availabilities); }

    public String getFullName() {
        return (title == null || title.isEmpty() ? "" : title + " ") + firstName + " " + lastName;
    }

    public String getSpecialtyNames() {
        return specialties.stream().map(Specialty::getName).collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return String.format("%-5s %-28s %-22s slots: %d", professionalId, getFullName(), getSpecialtyNames(), availabilities.size());
    }
}
