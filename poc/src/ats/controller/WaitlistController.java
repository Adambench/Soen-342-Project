package ats.controller;

import ats.catalog.ActivityLog;
import ats.catalog.PatientRegistry;
import ats.catalog.ProfessionalRegistry;
import ats.domain.BookingException;
import ats.domain.HealthcareProfessional;
import ats.domain.OfferResponse;
import ats.domain.Patient;
import ats.domain.WaitingList;
import ats.domain.WaitlistEntry;

import java.time.LocalDate;
import java.util.List;

/** Add patient to waiting list, Offer appointment to waitlisted patient (follows the interaction diagrams). */
public class WaitlistController {
    private final PatientRegistry patients;
    private final ProfessionalRegistry professionals;
    private final WaitingList waitingList;
    private final ActivityLog activityLog;
    private final String userId;

    public WaitlistController(PatientRegistry patients, ProfessionalRegistry professionals,
                              WaitingList waitingList, ActivityLog activityLog, String userId) {
        this.patients = patients;
        this.professionals = professionals;
        this.waitingList = waitingList;
        this.activityLog = activityLog;
        this.userId = userId;
    }

    public WaitlistEntry addToWaitingList(String patientId, String professionalId, String specialty,
                                          LocalDate dateFrom, LocalDate dateTo) {
        if (!validateRequest(professionalId, specialty, dateFrom, dateTo)) {
            throw new BookingException("invalid request: give a professional, a specialty or a date range (dateFrom not after dateTo)");
        }
        Patient patient = patients.findById(patientId);
        if (patient == null) {
            throw new BookingException("patient not found (register the patient first)");
        }
        List<HealthcareProfessional> found = professionals.findProfessionals(professionalId, specialty);
        if (found.isEmpty()) {
            throw new BookingException("no matching professional");
        }
        for (HealthcareProfessional hp : found) {
            if (!hp.getOpenAvailabilities(dateFrom, dateTo, null, null).isEmpty()) {
                throw new BookingException("slots available: book an appointment instead");
            }
        }
        WaitlistEntry entry = waitingList.addEntry(patient, professionalId, specialty, dateFrom, dateTo);
        activityLog.record(userId, "ADD_TO_WAITLIST", entry.getEntryId(), patient.getPatientId());
        return entry;
    }

    /** Returns the highest-priority eligible entry for a freed slot (the patient to phone). */
    public WaitlistEntry offerAppointment(String availabilityId) {
        WaitlistEntry entry = waitingList.findNextEligible(availabilityId);
        if (entry == null) {
            throw new BookingException("no eligible waitlisted patient for " + availabilityId);
        }
        activityLog.record(userId, "OFFER_APPOINTMENT", entry.getEntryId(), availabilityId);
        return entry;
    }

    public void recordOfferResponse(String entryId, String availabilityId, OfferResponse response) {
        WaitlistEntry entry = waitingList.findEntry(entryId);
        if (entry == null) {
            throw new BookingException("unknown waitlist entry " + entryId);
        }
        if (response == OfferResponse.ACCEPTED) {
            entry.markAccepted();
            activityLog.record(userId, "OFFER_ACCEPTED", entryId, availabilityId);
        } else {
            entry.markDeclined(availabilityId);
            activityLog.record(userId, "OFFER_DECLINED", entryId, availabilityId);
        }
    }

    private boolean validateRequest(String professionalId, String specialty, LocalDate dateFrom, LocalDate dateTo) {
        boolean somethingGiven = professionalId != null || specialty != null || dateFrom != null || dateTo != null;
        boolean rangeOk = dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
        return somethingGiven && rangeOk;
    }
}
