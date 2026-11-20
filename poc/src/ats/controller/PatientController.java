package ats.controller;

import ats.catalog.PatientRegistry;
import ats.domain.Patient;

import java.time.LocalDate;
import java.util.List;

/** UC1 Register patient, UC2 Search patient. */
public class PatientController {
    private final PatientRegistry patients;

    public PatientController(PatientRegistry patients) {
        this.patients = patients;
    }

    public Patient registerPatient(String firstName, String lastName, LocalDate dateOfBirth,
                                   String phoneNumber, String insuranceNumber) {
        return patients.createPatient(firstName, lastName, dateOfBirth, phoneNumber, insuranceNumber);
    }

    public List<Patient> searchPatient(String criteria) {
        return patients.search(criteria);
    }
}
