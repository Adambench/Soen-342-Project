package ats.domain;

import java.time.LocalDate;
import java.time.LocalTime;

/** One bookable slot of a healthcare professional at a clinic location. */
public class Availability {
    private static int nextId = 1;

    private final String availabilityId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;
    private final HealthcareProfessional professional;
    private final ClinicLocation location;
    private Appointment appointment;

    Availability(HealthcareProfessional professional, ClinicLocation location,
                 LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.availabilityId = "AV" + nextId++;
        this.professional = professional;
        this.location = location;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isFree() {
        return status == AvailabilityStatus.AVAILABLE;
    }

    /** True when the slot is AVAILABLE and fits the (optional) criteria; null means "any". */
    public boolean isOpenWithin(LocalDate dateFrom, LocalDate dateTo, LocalTime timeFrom, LocalTime timeTo) {
        return isFree()
                && (dateFrom == null || !date.isBefore(dateFrom))
                && (dateTo == null || !date.isAfter(dateTo))
                && (timeFrom == null || !startTime.isBefore(timeFrom))
                && (timeTo == null || !endTime.isAfter(timeTo));
    }

    public boolean covers(LocalDate date, LocalTime time) {
        return this.date.equals(date) && !time.isBefore(startTime) && time.isBefore(endTime);
    }

    public boolean overlaps(LocalDate date, LocalTime start, LocalTime end) {
        return this.date.equals(date) && start.isBefore(endTime) && end.isAfter(startTime);
    }

    public void reserve(Appointment a) {
        if (!isFree()) {
            throw new BookingException("slot " + availabilityId + " is already booked");
        }
        this.appointment = a;
        this.status = AvailabilityStatus.BOOKED;
    }

    public String getAvailabilityId() { return availabilityId; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public AvailabilityStatus getStatus() { return status; }
    public HealthcareProfessional getProfessional() { return professional; }
    public ClinicLocation getLocation() { return location; }
    public Appointment getAppointment() { return appointment; }

    @Override
    public String toString() {
        return String.format("%-5s %s %s-%s  %s (%s) at %s  [%s]", availabilityId, date, startTime, endTime,
                professional.getFullName(), professional.getSpecialtyNames(), location.getLocationId(), status);
    }
}
