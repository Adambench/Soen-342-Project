package ats;

import ats.catalog.ActivityLog;
import ats.catalog.LocationCatalog;
import ats.catalog.MedicalServiceCatalog;
import ats.catalog.PatientRegistry;
import ats.catalog.ProfessionalRegistry;
import ats.catalog.SpecialtyCatalog;
import ats.controller.AvailabilityController;
import ats.controller.BookingController;
import ats.controller.PatientController;
import ats.controller.WaitlistController;
import ats.csv.AvailabilityCsvExporter;
import ats.csv.AvailabilityCsvImporter;
import ats.domain.ClinicLocation;
import ats.domain.MedicalService;
import ats.domain.Patient;
import ats.domain.Referral;
import ats.domain.WaitingList;
import ats.ui.ClinicCLI;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Wires the objects together and starts the CLI.
 * The system starts with no availabilities (load them with "Import from CSV").
 * Reference data (locations, medical services) is created here because the PoC has no screens for it.
 * Run with --demo-patients to also add two patients and a referral for demonstrating UC5.
 */
public class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        PatientRegistry patients = new PatientRegistry();
        ProfessionalRegistry professionals = new ProfessionalRegistry();
        SpecialtyCatalog specialties = new SpecialtyCatalog();
        MedicalServiceCatalog services = new MedicalServiceCatalog();
        LocationCatalog locations = new LocationCatalog();
        ActivityLog activityLog = new ActivityLog();

        locations.add(new ClinicLocation("L1", "Clinique Centre-Ville", "1000 rue Sainte-Catherine O, Montreal"));
        locations.add(new ClinicLocation("L2", "Clinique Rosemont", "2500 boul. Rosemont, Montreal"));
        services.add(new MedicalService("S1", "General consultation", false, true, specialties.findOrCreate("Family Medicine")));
        services.add(new MedicalService("S2", "Primary care assessment", false, true, specialties.findOrCreate("Primary Care")));
        services.add(new MedicalService("S3", "Cardiology consultation", true, true, specialties.findOrCreate("Cardiology")));
        services.add(new MedicalService("S4", "Dermatology consultation", true, false, specialties.findOrCreate("Dermatology")));
        services.add(new MedicalService("S5", "Orthopedic evaluation", true, true, specialties.findOrCreate("Orthopedics")));

        for (String arg : args) {
            if (arg.equals("--demo-patients")) {
                Patient emilie = patients.createPatient("Émilie", "Lavoie", LocalDate.of(1999, 1, 1), "514-555-0101", "LAVE99010101");
                emilie.addReferral(new Referral(emilie, specialties.findOrCreate("Cardiology"), "Palpitations", "High"));
                patients.createPatient("Olivier", "Côté", LocalDate.of(1985, 6, 15), "514-555-0102", "COTO85061502");
            }
        }

        AvailabilityCsvImporter importer = new AvailabilityCsvImporter(professionals, specialties, locations);
        AvailabilityCsvExporter exporter = new AvailabilityCsvExporter(professionals);
        WaitingList waitingList = new WaitingList(professionals::findById, specialties::findByName, professionals::findAvailability);

        new ClinicCLI(
                new PatientController(patients),
                new BookingController(patients, professionals, services, locations),
                new AvailabilityController(professionals, locations, importer, exporter),
                new WaitlistController(patients, professionals, waitingList, activityLog, "staff"),
                patients, professionals, services, locations
        ).run();
    }
}
