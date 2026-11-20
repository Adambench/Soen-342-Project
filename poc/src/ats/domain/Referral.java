package ats.domain;

public class Referral {
    private static int nextId = 1;

    private final String referralId;
    private final String clinicalReason;
    private final String priority;
    private ReferralStatus status = ReferralStatus.OPEN;
    private final Patient patient;
    private final Specialty requestedSpecialty;

    public Referral(Patient patient, Specialty requestedSpecialty, String clinicalReason, String priority) {
        this.referralId = "R" + nextId++;
        this.patient = patient;
        this.requestedSpecialty = requestedSpecialty;
        this.clinicalReason = clinicalReason;
        this.priority = priority;
    }

    public boolean isOpen() {
        return status == ReferralStatus.OPEN;
    }

    public boolean matches(HealthcareProfessional hp) {
        return isOpen() && hp.hasSpecialty(requestedSpecialty);
    }

    public String getReferralId() { return referralId; }
    public String getClinicalReason() { return clinicalReason; }
    public String getPriority() { return priority; }
    public ReferralStatus getStatus() { return status; }
    public void setStatus(ReferralStatus status) { this.status = status; }
    public Patient getPatient() { return patient; }
    public Specialty getRequestedSpecialty() { return requestedSpecialty; }

    @Override
    public String toString() {
        return String.format("%s: %s, reason \"%s\", priority %s, %s", referralId, requestedSpecialty, clinicalReason, priority, status);
    }
}
