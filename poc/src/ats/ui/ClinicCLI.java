package ats.ui;

import ats.catalog.LocationCatalog;
import ats.catalog.MedicalServiceCatalog;
import ats.catalog.PatientRegistry;
import ats.catalog.ProfessionalRegistry;
import ats.controller.AvailabilityController;
import ats.controller.BookingController;
import ats.controller.PatientController;
import ats.controller.WaitlistController;
import ats.domain.Appointment;
import ats.domain.Availability;
import ats.domain.BookingException;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.MedicalService;
import ats.domain.MonthlyPattern;
import ats.domain.OfferResponse;
import ats.domain.Patient;
import ats.domain.RecurrencePattern;
import ats.domain.Referral;
import ats.domain.WaitlistEntry;
import ats.domain.WeeklyPattern;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/** Command-line UI. It only reads input, calls a controller, and prints the result (Model-View Separation). */
public class ClinicCLI {
    private final PatientController patientController;
    private final BookingController bookingController;
    private final AvailabilityController availabilityController;
    private final WaitlistController waitlistController;
    // read-only lookups used to list ids on screen
    private final PatientRegistry patients;
    private final ProfessionalRegistry professionals;
    private final MedicalServiceCatalog services;
    private final LocationCatalog locations;
    private final Scanner in = new Scanner(System.in, "UTF-8");

    public ClinicCLI(PatientController patientController, BookingController bookingController,
                     AvailabilityController availabilityController, WaitlistController waitlistController,
                     PatientRegistry patients, ProfessionalRegistry professionals,
                     MedicalServiceCatalog services, LocationCatalog locations) {
        this.patientController = patientController;
        this.bookingController = bookingController;
        this.availabilityController = availabilityController;
        this.waitlistController = waitlistController;
        this.patients = patients;
        this.professionals = professionals;
        this.services = services;
        this.locations = locations;
    }

    public void run() {
        System.out.println("=== Appointment Tracking System (PoC) ===");
        while (true) {
            System.out.println();
            System.out.println(" 1  Import availabilities from CSV");
            System.out.println(" 2  Export availabilities to CSV");
            System.out.println(" 3  Make availability");
            System.out.println(" 4  Search availability");
            System.out.println(" 5  Book an appointment");
            System.out.println(" 6  Register patient");
            System.out.println(" 7  Search patient");
            System.out.println(" 8  Add patient to waiting list");
            System.out.println(" 9  Offer a freed slot to the waiting list");
            System.out.println(" 10 List professionals, services, locations, patients");
            System.out.println(" 0  Exit");
            String choice = ask("Choice");
            if (choice == null || choice.equals("0")) {
                System.out.println("Bye.");
                return;
            }
            try {
                switch (choice) {
                    case "1": importCsv(); break;
                    case "2": exportCsv(); break;
                    case "3": makeAvailability(); break;
                    case "4": searchAvailability(); break;
                    case "5": bookAppointment(); break;
                    case "6": registerPatient(); break;
                    case "7": searchPatient(); break;
                    case "8": addToWaitingList(); break;
                    case "9": offerSlot(); break;
                    case "10": listReferenceData(); break;
                    default: System.out.println("Unknown choice.");
                }
            } catch (BookingException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (DateTimeParseException e) {
                System.out.println("Error: use dates as YYYY-MM-DD and times as HH:MM (" + e.getParsedString() + ")");
            } catch (NumberFormatException e) {
                System.out.println("Error: a number was expected.");
            } catch (InputEnded e) {
                System.out.println("Bye.");
                return;
            }
        }
    }

    // ---------------------------------------------------------------- PoC features

    private void importCsv() {
        String path = askOr("CSV file to import", "data/availabilities.csv");
        int n = availabilityController.importAvailabilities(path);
        System.out.println(n + " availabilities imported.");
        for (String err : availabilityController.getLastImportErrors()) {
            System.out.println("  rejected " + err);
        }
    }

    private void exportCsv() {
        String path = askOr("CSV file to write", "export/availabilities_export.csv");
        int n = availabilityController.exportAvailabilities(path);
        System.out.println(n + " availabilities exported to " + path);
    }

    private void makeAvailability() {
        String hp = askRequired("Professional id (e.g. HP1)");
        String loc = askOr("Location id", locations.getDefault().getLocationId());
        LocalDate start = LocalDate.parse(askRequired("Start date (YYYY-MM-DD)"));
        LocalDate end = LocalDate.parse(askRequired("End date (YYYY-MM-DD)"));
        RecurrencePattern pattern;
        String kind = askOr("Repeat: W = day(s) of week, M = monthly", "W").toUpperCase(Locale.ROOT);
        if (kind.startsWith("M")) {
            pattern = new MonthlyPattern(Integer.parseInt(askRequired("Day of month (1-31)")));
        } else {
            pattern = new WeeklyPattern(parseDays(askRequired("Day(s) of week, e.g. MON or MON,WED")));
        }
        LocalTime from = LocalTime.parse(askRequired("Start time (HH:MM)"));
        LocalTime to = LocalTime.parse(askRequired("End time (HH:MM)"));
        int minutes = Integer.parseInt(askOr("Slot length in minutes", "30"));
        List<Availability> created = availabilityController.makeAvailability(hp, loc, start, end, pattern, from, to, minutes);
        System.out.println(created.size() + " availabilities created (" + pattern + ")"
                + (availabilityController.getLastSkipped() > 0
                ? ", " + availabilityController.getLastSkipped() + " skipped because they overlap existing slots" : "") + ".");
        if (!created.isEmpty()) {
            System.out.println("  first: " + created.get(0));
            System.out.println("  last:  " + created.get(created.size() - 1));
        }
    }

    private void searchAvailability() {
        System.out.println("Leave a criterion blank to ignore it.");
        String hp = askOptional("Professional id");
        String specialty = askOptional("Specialty (e.g. Cardiology)");
        LocalDate dateFrom = optDate(askOptional("From date (YYYY-MM-DD)"));
        LocalDate dateTo = optDate(askOptional("To date (YYYY-MM-DD)"));
        LocalTime timeFrom = optTime(askOptional("From time (HH:MM)"));
        LocalTime timeTo = optTime(askOptional("To time (HH:MM)"));
        List<Availability> open = availabilityController.searchAvailability(hp, specialty, dateFrom, dateTo, timeFrom, timeTo);
        if (open.isEmpty()) {
            System.out.println("No availability. The patient can be added to the waiting list (option 8).");
            return;
        }
        System.out.println(open.size() + " open availabilities:");
        open.forEach(av -> System.out.println("  " + av));
    }

    private void bookAppointment() {
        String patientId = askOptional("Patient id (blank to search)");
        if (patientId == null) {
            searchPatient();
            patientId = askRequired("Patient id");
        }
        System.out.println("Medical services:");
        services.getAll().forEach(ms -> System.out.println("  " + ms));
        String serviceId = askRequired("Service id");
        String hp = askRequired("Professional id (option 4 shows who is free)");
        String loc = askOr("Location id", locations.getDefault().getLocationId());
        LocalDate date = LocalDate.parse(askRequired("Date (YYYY-MM-DD)"));
        LocalTime time = LocalTime.parse(askRequired("Time (HH:MM)"));

        // UC4 Check availability and constraints
        if (!bookingController.checkAvailability(patientId, hp, serviceId, loc, date, time)) {
            List<Availability> proposed = bookingController.getProposedSlots();
            if (proposed.isEmpty()) {
                System.out.println("Slot unavailable and no other free slot for this professional. "
                        + "The patient can be added to the waiting list (option 8).");
                return;
            }
            System.out.println("Slot unavailable. Next available slots:");
            for (int i = 0; i < proposed.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + proposed.get(i));
            }
            String pick = askOptional("Pick a number (blank to cancel)");
            if (pick == null) {
                System.out.println("Booking cancelled, nothing saved.");
                return;
            }
            Availability chosen = proposed.get(Integer.parseInt(pick) - 1);
            bookingController.selectSlot(chosen.getDate(), chosen.getStartTime());
            date = chosen.getDate();
            time = chosen.getStartTime();
        }
        System.out.println("Slot held: " + bookingController.getHeldSlot());

        // UC5 Verify referral requirement
        List<Referral> referrals = bookingController.verifyReferralRequirement(patientId, serviceId, hp);
        if (referrals.isEmpty()) {
            System.out.println("No referral required.");
        } else {
            System.out.println("Valid referrals:");
            referrals.forEach(r -> System.out.println("  " + r));
            bookingController.selectReferral(askOr("Referral id", referrals.get(0).getReferralId()));
        }

        // UC3 Book appointment
        Appointment a = bookingController.bookAppointment(patientId, hp, serviceId, loc, date, time);
        System.out.println("Booked: " + a);
    }

    // ---------------------------------------------------------------- patients and waiting list

    private void registerPatient() {
        String first = askRequired("First name");
        String last = askRequired("Last name");
        LocalDate dob = LocalDate.parse(askRequired("Date of birth (YYYY-MM-DD)"));
        String phone = askRequired("Phone");
        String ins = askOptional("Insurance (RAMQ) number, blank if none");
        Patient p = patientController.registerPatient(first, last, dob, phone, ins);
        System.out.println("Registered: " + p);
    }

    private void searchPatient() {
        String criteria = askRequired("Search patient (insurance no., birth date, phone, or name)");
        List<Patient> found = patientController.searchPatient(criteria);
        if (found.isEmpty()) {
            System.out.println("No patient found. Register the patient with option 6.");
        }
        found.forEach(p -> System.out.println("  " + p));
    }

    private void addToWaitingList() {
        String patientId = askRequired("Patient id");
        System.out.println("Give at least one of: professional, specialty, date range.");
        String hp = askOptional("Professional id");
        String specialty = askOptional("Specialty");
        LocalDate from = optDate(askOptional("From date (YYYY-MM-DD)"));
        LocalDate to = optDate(askOptional("To date (YYYY-MM-DD)"));
        WaitlistEntry e = waitlistController.addToWaitingList(patientId, hp, specialty, from, to);
        System.out.println("Added: " + e);
    }

    private void offerSlot() {
        String availabilityId = askRequired("Freed availability id (e.g. AV3)");
        WaitlistEntry e = waitlistController.offerAppointment(availabilityId);
        System.out.println("Offer this slot by phone to: " + e);
        String answer = askRequired("Did the patient accept? (Y/N)").toUpperCase(Locale.ROOT);
        OfferResponse response = answer.startsWith("Y") ? OfferResponse.ACCEPTED : OfferResponse.DECLINED;
        waitlistController.recordOfferResponse(e.getEntryId(), availabilityId, response);
        if (response == OfferResponse.ACCEPTED) {
            System.out.println("Accepted. Book it with option 5 for patient " + e.getPatient().getPatientId()
                    + " at " + availabilityId + ".");
        } else {
            System.out.println("Declined. The patient stays on the waiting list for other slots.");
        }
    }

    private void listReferenceData() {
        System.out.println("Professionals:");
        for (HealthcareProfessional hp : professionals.getAll()) {
            System.out.println("  " + hp);
        }
        System.out.println("Medical services:");
        for (MedicalService ms : services.getAll()) {
            System.out.println("  " + ms);
        }
        System.out.println("Locations:");
        for (ClinicLocation loc : locations.getAll()) {
            System.out.println("  " + loc);
        }
        System.out.println("Patients:");
        for (Patient p : patients.getAll()) {
            System.out.println("  " + p + (p.getReferrals().isEmpty() ? "" : "  referrals: " + p.getReferrals()));
        }
    }

    // ---------------------------------------------------------------- input helpers

    private static class InputEnded extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!in.hasNextLine()) {
            throw new InputEnded();
        }
        String line = in.nextLine().trim();
        if (System.console() == null) {
            System.out.println(line); // echo when input is piped from a file, so the transcript is readable
        }
        return line;
    }

    private String askRequired(String prompt) {
        String v = ask(prompt);
        if (v.isEmpty()) {
            throw new BookingException(prompt + " is required");
        }
        return v;
    }

    private String askOptional(String prompt) {
        String v = ask(prompt);
        return v.isEmpty() ? null : v;
    }

    private String askOr(String prompt, String defaultValue) {
        String v = ask(prompt + " [" + defaultValue + "]");
        return v.isEmpty() ? defaultValue : v;
    }

    private static LocalDate optDate(String s) {
        return s == null ? null : LocalDate.parse(s);
    }

    private static LocalTime optTime(String s) {
        return s == null ? null : LocalTime.parse(s);
    }

    private static Set<DayOfWeek> parseDays(String text) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String part : text.split("[,\\s]+")) {
            String p = part.trim().toUpperCase(Locale.ROOT);
            if (p.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (DayOfWeek d : DayOfWeek.values()) {
                if (d.name().startsWith(p) && p.length() >= 2) {
                    days.add(d);
                    matched = true;
                }
            }
            if (!matched) {
                throw new BookingException("unknown day \"" + part + "\" (use MON, TUE, WED, THU, FRI, SAT, SUN)");
            }
        }
        if (days.isEmpty()) {
            throw new BookingException("give at least one day of the week");
        }
        return days;
    }
}
