package ats.domain;

import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private static int nextId = 1;

    private final String appointmentId;
    private LocalDate date;
    private LocalTime time;
    private AppointmentStatus status = AppointmentStatus.REQUESTED;
    private LocalTime arrivalTime;
    private Patient patient;
    private HealthcareProfessional professional;
    private MedicalService service;
    private ClinicLocation location;
    private Availability slot;
    private Referral referral;

    public Appointment() {
        this.appointmentId = "A" + nextId++;
    }

    /** Sets patient, service and slot; professional, location, date and time come from the slot. */
    public void assignSlot(Patient p, MedicalService ms, Availability slot) {
        this.patient = p;
        this.service = ms;
        this.slot = slot;
        this.professional = slot.getProfessional();
        this.location = slot.getLocation();
        this.date = slot.getDate();
        this.time = slot.getStartTime();
    }

    public void linkReferral(Referral r) {
        this.referral = r;
    }

    public void book() {
        if (slot == null) {
            throw new BookingException("no slot held for this appointment");
        }
        if (service.isRequiresReferral() && referral == null) {
            throw new BookingException("referral required, appointment cannot be booked");
        }
        slot.reserve(this);
        patient.addAppointment(this);
        status = AppointmentStatus.BOOKED;
    }

    public boolean occursAt(LocalDate date, LocalTime time) {
        return status != AppointmentStatus.CANCELLED && slot != null && slot.covers(date, time);
    }

    public String getAppointmentId() { return appointmentId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public AppointmentStatus getStatus() { return status; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public Patient getPatient() { return patient; }
    public HealthcareProfessional getProfessional() { return professional; }
    public MedicalService getService() { return service; }
    public ClinicLocation getLocation() { return location; }
    public Availability getSlot() { return slot; }
    public Referral getReferral() { return referral; }

    @Override
    public String toString() {
        return String.format("%s: %s %s-%s, %s with %s at %s, service %s%s, status %s", appointmentId, date, time,
                slot.getEndTime(), patient.getFullName(), professional.getFullName(), location.getName(),
                service.getName(), referral == null ? "" : ", referral " + referral.getReferralId(), status);
    }
}
