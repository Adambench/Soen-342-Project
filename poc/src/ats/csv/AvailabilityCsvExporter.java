package ats.csv;

import ats.catalog.ProfessionalRegistry;
import ats.domain.Availability;
import ats.domain.BookingException;
import ats.domain.HealthcareProfessional;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure Fabrication: writes the entire availability database to CSV, in a format the importer reads back. */
public class AvailabilityCsvExporter {
    private final ProfessionalRegistry professionals;

    public AvailabilityCsvExporter(ProfessionalRegistry professionals) {
        this.professionals = professionals;
    }

    public int exportTo(String filePath) {
        List<Availability> all = new ArrayList<>();
        for (HealthcareProfessional hp : professionals.getAll()) {
            all.addAll(hp.getAvailabilities());
        }
        if (all.isEmpty()) {
            throw new BookingException("nothing to export: there are no availabilities in the system");
        }
        all.sort(Comparator.comparing(Availability::getDate).thenComparing(Availability::getStartTime)
                .thenComparing(av -> av.getProfessional().getLastName()));
        Path path = Paths.get(filePath);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
                out.println("First Name,Last Name,Title,Specialty,Date,Start,End,Location,Status");
                for (Availability av : all) {
                    HealthcareProfessional hp = av.getProfessional();
                    String specialty = hp.getSpecialties().isEmpty() ? "" : hp.getSpecialties().iterator().next().getName();
                    out.println(String.join(",",
                            CsvUtil.quote(hp.getFirstName()), CsvUtil.quote(hp.getLastName()), CsvUtil.quote(hp.getTitle()),
                            CsvUtil.quote(specialty), av.getDate().toString(), av.getStartTime().toString(),
                            av.getEndTime().toString(), CsvUtil.quote(av.getLocation().getLocationId()), av.getStatus().name()));
                }
            }
        } catch (IOException e) {
            throw new BookingException("cannot write file " + path.toAbsolutePath());
        }
        return all.size();
    }
}
