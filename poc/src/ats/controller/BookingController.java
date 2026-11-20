package ats.controller;

import ats.catalog.LocationCatalog;
import ats.catalog.MedicalServiceCatalog;
import ats.catalog.PatientRegistry;
import ats.catalog.ProfessionalRegistry;
import ats.domain.Appointment;
import ats.domain.Availability;
import ats.domain.BookingException;
import ats.domain.ClinicLocation;
import ats.domain.HealthcareProfessional;
import ats.domain.MedicalService;
import ats.domain.Patient;
import ats.domain.Referral;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use-case controller for UC3 Book appointment (includes UC4 Check availability and UC5 Verify referral).
 * Follows the SSDs: checkAvailability -> [selectSlot] -> verifyReferralRequirement -> [selectReferral] -> bookAppointment.
 */
public class BookingController {
    private final PatientRegistry patients;
    private final ProfessionalRegistry professionals;
    private final MedicalServiceCatalog services;
    private final LocationCatalog locations;

    private Patient currentPatient;
    private MedicalService currentService;
    private List<Availability> proposedSlots = new ArrayList<>();
    private Availability heldSlot;
    private Referral selectedReferral;

    public BookingController(PatientRegistry patients, ProfessionalRegistry professionals,
                             MedicalServiceCatalog services, LocationCatalog locations) {
        this.patients = patients;
        this.professionals = professionals;
        this.services = services;
        this.locations = locations;
    }

    /** UC4. True when the requested slot is free and is now held; false when it is taken (see getProposedSlots()). */
    public boolean checkAvailability(String patientId, String professionalId, String serviceId,
                                     String locationId, LocalDate date, LocalTime time) {
        Patient p = require(patients.findById(patientId), "unknown patient " + patientId);
        HealthcareProfessional hp = require(professionals.findById(professionalId), "unknown professional " + professionalId);
        MedicalService ms = require(services.findById(serviceId), "unknown medical service " + serviceId);
        ClinicLocation loc = require(locations.findById(locationId), "unknown location " + locationId);

        currentPatient = p;
        currentService = ms;
        heldSlot = null;
        selectedReferral = null;
        proposedSlots = new ArrayList<>();

        if (!hp.practisesAt(loc) || !hp.offers(ms)) {
            throw new BookingException("service not offered by this professional at this location");
        }
        if (p.hasConflict(date, time)) {
            throw new BookingException("scheduling conflict for patient");
        }
        Availability slot = hp.findFreeSlot(loc, date, time);
        if (slot != null) {
            heldSlot = slot;
            return true;
        }
        proposedSlots = hp.getNextFreeSlots(loc, date, time, 10).stream()
                .filter(av -> !p.hasConflict(av.getDate(), av.getStartTime()))
                .limit(5)
                .collect(Collectors.toList());
        return false;
    }

    public List<Availability> getProposedSlots() {
        return Collections.unmodifiableList(proposedSlots);
    }

    public Availability getHeldSlot() {
        return heldSlot;
    }

    public void selectSlot(LocalDate date, LocalTime time) {
        for (Availability av : proposedSlots) {
            if (av.covers(date, time)) {
                heldSlot = av;
                return;
            }
        }
        throw new BookingException("the chosen date and time is not one of the proposed slots");
    }

    /** UC5. Empty list = no referral required. Throws when a referral is required but none is valid. */
    public List<Referral> verifyReferralRequirement(String patientId, String serviceId, String professionalId) {
        Patient p = require(patients.findById(patientId), "unknown patient " + patientId);
        MedicalService ms = require(services.findById(serviceId), "unknown medical service " + serviceId);
        HealthcareProfessional hp = require(professionals.findById(professionalId), "unknown professional " + professionalId);
        if (!ms.isRequiresReferral()) {
            return Collections.emptyList();
        }
        List<Referral> valid = p.getOpenReferralsFor(hp);
        if (valid.isEmpty()) {
            throw new BookingException(p.getReferrals().isEmpty()
                    ? "referral required, appointment cannot be booked"
                    : "referral not valid for this service");
        }
        return valid;
    }

    public void selectReferral(String referralId) {
        Referral r = currentPatient == null ? null : currentPatient.getReferral(referralId);
        if (r == null || heldSlot == null || !r.matches(heldSlot.getProfessional())) {
            throw new BookingException("referral " + referralId + " is not one of the valid referrals");
        }
        selectedReferral = r;
    }

    /**
     * UC3. Creates the Appointment (Creator) for the held slot and books it.
     * Resource allocation is checked here in the SSD; resource classes are left for Iteration III.
     */
    public Appointment bookAppointment(String patientId, String professionalId, String serviceId,
                                       String locationId, LocalDate date, LocalTime time) {
        boolean holdsRequestedSlot = heldSlot != null && currentPatient != null
                && currentPatient.getPatientId().equalsIgnoreCase(patientId)
                && heldSlot.getProfessional().getProfessionalId().equalsIgnoreCase(professionalId)
                && heldSlot.getLocation().getLocationId().equalsIgnoreCase(locationId)
                && heldSlot.covers(date, time);
        if (!holdsRequestedSlot && !checkAvailability(patientId, professionalId, serviceId, locationId, date, time)) {
            throw new BookingException("slot unavailable");
        }
        if (currentService.isRequiresReferral() && selectedReferral == null) {
            throw new BookingException("referral required: run verifyReferralRequirement and selectReferral first");
        }
        Appointment a = new Appointment();
        a.assignSlot(currentPatient, currentService, heldSlot);
        if (selectedReferral != null) {
            a.linkReferral(selectedReferral);
        }
        a.book();
        clear();
        return a;
    }

    private void clear() {
        currentPatient = null;
        currentService = null;
        heldSlot = null;
        selectedReferral = null;
        proposedSlots = new ArrayList<>();
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new BookingException(message);
        }
        return value;
    }
}
