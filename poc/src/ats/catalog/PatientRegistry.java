package ats.catalog;

import ats.domain.BookingException;
import ats.domain.Patient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatientRegistry {
    private final Map<String, Patient> patients = new LinkedHashMap<>();
    private int nextId = 1;

    public Patient createPatient(String firstName, String lastName, LocalDate dateOfBirth,
                                 String phoneNumber, String insuranceNumber) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new BookingException("first and last name are required");
        }
        if (insuranceNumber != null && !insuranceNumber.isBlank()) {
            for (Patient p : patients.values()) {
                if (insuranceNumber.equalsIgnoreCase(p.getInsuranceNumber())) {
                    throw new BookingException("a patient with insurance number " + insuranceNumber + " already exists (" + p.getPatientId() + ")");
                }
            }
        }
        String type = insuranceNumber == null || insuranceNumber.isBlank() ? "Uninsured" : "RAMQ";
        Patient p = new Patient("P" + nextId++, firstName.trim(), lastName.trim(), dateOfBirth, phoneNumber, insuranceNumber, type);
        patients.put(p.getPatientId(), p);
        return p;
    }

    public Patient findById(String patientId) {
        return patientId == null ? null : patients.get(patientId.trim().toUpperCase());
    }

    public List<Patient> search(String criteria) {
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients.values()) {
            if (p.matches(criteria)) {
                result.add(p);
            }
        }
        return result;
    }

    public Collection<Patient> getAll() {
        return patients.values();
    }
}
