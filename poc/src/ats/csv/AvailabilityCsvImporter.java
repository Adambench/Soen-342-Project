package ats.csv;

import ats.catalog.LocationCatalog;
import ats.catalog.ProfessionalRegistry;
import ats.catalog.SpecialtyCatalog;
import ats.domain.Availability;
import ats.domain.BookingException;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.Specialty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure Fabrication: reads availabilities from a CSV file.
 * Required columns: First Name, Last Name, Title, Specialty, Date, Start, End.
 * Optional column: Location (id or name); other columns (e.g. Status from an export) are ignored.
 */
public class AvailabilityCsvImporter {
    static final String[] REQUIRED = {"First Name", "Last Name", "Title", "Specialty", "Date", "Start", "End"};

    private final ProfessionalRegistry professionals;
    private final SpecialtyCatalog specialties;
    private final LocationCatalog locations;
    private final List<String> lastErrors = new ArrayList<>();

    public AvailabilityCsvImporter(ProfessionalRegistry professionals, SpecialtyCatalog specialties, LocationCatalog locations) {
        this.professionals = professionals;
        this.specialties = specialties;
        this.locations = locations;
    }

    /** Returns the number of rows imported; rejected rows are listed in getLastErrors(). */
    public int importFrom(String filePath) {
        lastErrors.clear();
        Path path = Paths.get(filePath);
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BookingException("cannot read file " + path.toAbsolutePath());
        }
        if (lines.isEmpty()) {
            throw new BookingException("invalid CSV format: file is empty");
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> header = CsvUtil.parseLine(lines.get(0).replace("﻿", ""));
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        for (String name : REQUIRED) {
            if (!col.containsKey(name.toLowerCase(Locale.ROOT))) {
                throw new BookingException("invalid CSV format: missing column \"" + name + "\"");
            }
        }
        Integer locationCol = col.get("location");

        int imported = 0;
        for (int n = 1; n < lines.size(); n++) {
            String line = lines.get(n);
            if (line.isBlank()) {
                continue;
            }
            int lineNo = n + 1;
            try {
                List<String> f = CsvUtil.parseLine(line);
                String first = get(f, col, "first name");
                String last = get(f, col, "last name");
                String title = get(f, col, "title");
                String specialtyName = get(f, col, "specialty");
                LocalDate date = LocalDate.parse(get(f, col, "date"));
                LocalTime start = LocalTime.parse(get(f, col, "start"));
                LocalTime end = LocalTime.parse(get(f, col, "end"));
                if (first.isEmpty() || last.isEmpty() || specialtyName.isEmpty()) {
                    throw new BookingException("name and specialty are required");
                }
                ClinicLocation loc = locations.getDefault();
                if (locationCol != null && locationCol < f.size() && !f.get(locationCol).isBlank()) {
                    loc = locations.findByIdOrName(f.get(locationCol));
                    if (loc == null) {
                        throw new BookingException("unknown location \"" + f.get(locationCol) + "\"");
                    }
                }
                Specialty s = specialties.findOrCreate(specialtyName);
                HealthcareProfessional hp = professionals.findOrCreate(first, last, title, s);
                Availability av = hp.addAvailability(date, start, end, loc);
                if (av == null) {
                    throw new BookingException("overlaps an existing slot of " + hp.getFullName());
                }
                imported++;
            } catch (DateTimeParseException e) {
                lastErrors.add("line " + lineNo + ": bad date or time (" + e.getParsedString() + ")");
            } catch (BookingException | IndexOutOfBoundsException e) {
                lastErrors.add("line " + lineNo + ": " + (e.getMessage() == null ? "missing fields" : e.getMessage()));
            }
        }
        return imported;
    }

    public List<String> getLastErrors() {
        return lastErrors;
    }

    private static String get(List<String> fields, Map<String, Integer> col, String name) {
        return fields.get(col.get(name)).trim();
    }
}
