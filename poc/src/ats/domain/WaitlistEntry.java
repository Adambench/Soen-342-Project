package ats.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class WaitlistEntry {
    private static int nextId = 1;

    private final String entryId;
    private final LocalDateTime requestedAt = LocalDateTime.now();
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private int priority;
    private WaitlistStatus status = WaitlistStatus.WAITING;
    private final Set<String> declinedSlotIds = new HashSet<>();
    private final Patient patient;
    private final HealthcareProfessional professional; // 0..1
    private final Specialty specialty;                 // 0..1

    public WaitlistEntry(Patient patient, HealthcareProfessional professional, Specialty specialty,
                         LocalDate dateFrom, LocalDate dateTo) {
        this.entryId = "W" + nextId++;
        this.patient = patient;
        this.professional = professional;
        this.specialty = specialty;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    /** True when this Waiting entry wants the given free slot and has not declined it already. */
    public boolean matches(Availability slot) {
        return status == WaitlistStatus.WAITING
                && slot.isFree()
                && !declinedSlotIds.contains(slot.getAvailabilityId())
                && (professional == null || slot.getProfessional() == professional)
                && (specialty == null || slot.getProfessional().hasSpecialty(specialty))
                && (dateFrom == null || !slot.getDate().isBefore(dateFrom))
                && (dateTo == null || !slot.getDate().isAfter(dateTo));
    }

    public void markAccepted() {
        status = WaitlistStatus.ACCEPTED;
    }

    public void markDeclined(String availabilityId) {
        declinedSlotIds.add(availabilityId);
    }

    public String getEntryId() { return entryId; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDate getDateFrom() { return dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public WaitlistStatus getStatus() { return status; }
    public Patient getPatient() { return patient; }
    public HealthcareProfessional getProfessional() { return professional; }
    public Specialty getSpecialty() { return specialty; }

    @Override
    public String toString() {
        return String.format("%s: %s (tel %s), %s%s, %s to %s, priority %d, %s", entryId, patient.getFullName(),
                patient.getPhoneNumber(), professional == null ? "any professional" : professional.getFullName(),
                specialty == null ? "" : ", " + specialty, dateFrom == null ? "any date" : dateFrom,
                dateTo == null ? "any date" : dateTo, priority, status);
    }
}
