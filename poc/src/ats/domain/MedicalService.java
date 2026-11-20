package ats.domain;

public class MedicalService {
    private final String serviceId;
    private final String name;
    private final boolean requiresReferral;
    private final boolean coveredByRAMQ;
    private final Specialty specialty;

    public MedicalService(String serviceId, String name, boolean requiresReferral, boolean coveredByRAMQ, Specialty specialty) {
        this.serviceId = serviceId;
        this.name = name;
        this.requiresReferral = requiresReferral;
        this.coveredByRAMQ = coveredByRAMQ;
        this.specialty = specialty;
    }

    public String getServiceId() { return serviceId; }
    public String getName() { return name; }
    public boolean isRequiresReferral() { return requiresReferral; }
    public boolean isCoveredByRAMQ() { return coveredByRAMQ; }
    public Specialty getSpecialty() { return specialty; }

    @Override
    public String toString() {
        return serviceId + " " + name + " (" + specialty + (requiresReferral ? ", referral required" : "") + ")";
    }
}
