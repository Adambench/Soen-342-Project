package ats.controller;

import ats.catalog.LocationCatalog;
import ats.catalog.ProfessionalRegistry;
import ats.csv.AvailabilityCsvExporter;
import ats.csv.AvailabilityCsvImporter;
import ats.domain.Availability;
import ats.domain.BookingException;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.RecurrencePattern;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Make availability, Search availability, Import / Export availabilities (CSV). */
public class AvailabilityController {
    private final ProfessionalRegistry professionals;
    private final LocationCatalog locations;
    private final AvailabilityCsvImporter importer;
    private final AvailabilityCsvExporter exporter;
    private int lastSkipped;

    public AvailabilityController(ProfessionalRegistry professionals, LocationCatalog locations,
                                  AvailabilityCsvImporter importer, AvailabilityCsvExporter exporter) {
        this.professionals = professionals;
        this.locations = locations;
        this.importer = importer;
        this.exporter = exporter;
    }

    /** Example: Nov 1-30, WeeklyPattern(MONDAY), 09:00-12:00, 30 min -> 30 slots. Overlapping slots are skipped. */
    public List<Availability> makeAvailability(String professionalId, String locationId, LocalDate startDate,
                                               LocalDate endDate, RecurrencePattern pattern, LocalTime startTime,
                                               LocalTime endTime, int slotMinutes) {
        HealthcareProfessional hp = professionals.findById(professionalId);
        ClinicLocation loc = locations.findById(locationId);
        if (hp == null || loc == null) {
            throw new BookingException("unknown professional or location");
        }
        if (startDate.isAfter(endDate) || !startTime.isBefore(endTime) || slotMinutes <= 0) {
            throw new BookingException("invalid date or time range");
        }
        List<Availability> created = new ArrayList<>();
        lastSkipped = 0;
        boolean anyDate = false;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (!pattern.occursOn(d)) {
                continue;
            }
            anyDate = true;
            for (LocalTime s = startTime; !s.plusMinutes(slotMinutes).isAfter(endTime) && s.plusMinutes(slotMinutes).isAfter(s);
                 s = s.plusMinutes(slotMinutes)) {
                Availability av = hp.addAvailability(d, s, s.plusMinutes(slotMinutes), loc);
                if (av == null) {
                    lastSkipped++;
                } else {
                    created.add(av);
                }
            }
        }
        if (!anyDate) {
            throw new BookingException("no date in the range matches the pattern");
        }
        return created;
    }

    public int getLastSkipped() {
        return lastSkipped;
    }

    /** All criteria optional (null). Returns open slots sorted by date and start; empty when none. */
    public List<Availability> searchAvailability(String professionalId, String specialty, LocalDate dateFrom,
                                                 LocalDate dateTo, LocalTime timeFrom, LocalTime timeTo) {
        if (!validateCriteria(dateFrom, dateTo, timeFrom, timeTo)) {
            throw new BookingException("invalid criteria (dateFrom after dateTo, or timeFrom not before timeTo)");
        }
        List<HealthcareProfessional> found = professionals.findProfessionals(professionalId, specialty);
        if (found.isEmpty()) {
            throw new BookingException("no matching professional");
        }
        List<Availability> open = new ArrayList<>();
        for (HealthcareProfessional hp : found) {
            open.addAll(hp.getOpenAvailabilities(dateFrom, dateTo, timeFrom, timeTo));
        }
        sortByDateAndStartTime(open);
        return open;
    }

    public int importAvailabilities(String filePath) {
        return importer.importFrom(filePath);
    }

    public List<String> getLastImportErrors() {
        return importer.getLastErrors();
    }

    public int exportAvailabilities(String filePath) {
        return exporter.exportTo(filePath);
    }

    private boolean validateCriteria(LocalDate dateFrom, LocalDate dateTo, LocalTime timeFrom, LocalTime timeTo) {
        return (dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo))
                && (timeFrom == null || timeTo == null || timeFrom.isBefore(timeTo));
    }

    private void sortByDateAndStartTime(List<Availability> s) {
        s.sort(Comparator.comparing(Availability::getDate).thenComparing(Availability::getStartTime));
    }
}
