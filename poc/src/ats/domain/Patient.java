package ats.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Patient {
    private final String patientId;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String phoneNumber;
    private final String insuranceNumber;
    private final String insuranceType;
    private final List<Appointment> appointments = new ArrayList<>();
    private final List<Referral> referrals = new ArrayList<>();

    public Patient(String patientId, String firstName, String lastName, LocalDate dateOfBirth,
                   String phoneNumber, String insuranceNumber, String insuranceType) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.insuranceNumber = insuranceNumber;
        this.insuranceType = insuranceType;
    }

    /** Insurance number, date of birth, phone number, or partial first and/or last name. */
    public boolean matches(String criteria) {
        String c = criteria.trim().toLowerCase(Locale.ROOT);
        if (c.isEmpty()) {
            return false;
        }
        String digits = c.replaceAll("[^0-9]", "");
        return c.equalsIgnoreCase(insuranceNumber == null ? "" : insuranceNumber)
                || (dateOfBirth != null && c.equals(dateOfBirth.toString()))
                || (!digits.isEmpty() && digits.length() >= 7 && phoneNumber != null
                    && phoneNumber.replaceAll("[^0-9]", "").contains(digits))
                || getFullName().toLowerCase(Locale.ROOT).contains(c)
                || firstName.toLowerCase(Locale.ROOT).startsWith(c)
                || lastName.toLowerCase(Locale.ROOT).startsWith(c);
    }

    public boolean hasConflict(LocalDate date, LocalTime time) {
        for (Appointment a : appointments) {
            if (a.occursAt(date, time)) {
                return true;
            }
        }
        return false;
    }

    public void addAppointment(Appointment a) {
        appointments.add(a);
    }

    public Referral getReferral(String referralId) {
        for (Referral r : referrals) {
            if (r.getReferralId().equalsIgnoreCase(referralId)) {
                return r;
            }
        }
        return null;
    }

    public List<Referral> getOpenReferralsFor(HealthcareProfessional hp) {
        return referrals.stream().filter(r -> r.matches(hp)).collect(Collectors.toList());
    }

    public void addReferral(Referral r) {
        referrals.add(r);
    }

    public String getPatientId() { return patientId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getInsuranceNumber() { return insuranceNumber; }
    public String getInsuranceType() { return insuranceType; }
    public List<Appointment> getAppointments() { return Collections.unmodifiableList(appointments); }
    public List<Referral> getReferrals() { return Collections.unmodifiableList(referrals); }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return String.format("%-4s %-22s born %s  tel %s  ins. %s", patientId, getFullName(), dateOfBirth, phoneNumber,
                insuranceNumber == null || insuranceNumber.isEmpty() ? "-" : insuranceNumber);
    }
}
