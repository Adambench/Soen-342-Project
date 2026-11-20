# Appointment Tracking System: Proof of Concept (Iteration II)

A command-line Java application for the PoC features in the Iteration II handout:

| Menu | Feature |
|---|---|
| 1 | **Import from CSV.** Loads availabilities. The system starts with none. |
| 2 | **Export to CSV.** Writes the whole availability database. The file can be imported again. |
| 3 | **Make availability.** Professional, date range, repetition (day(s) of week or monthly), start/end time, slot length. |
| 4 | **Search availability.** Optional criteria: professional, specialty, date range, time range. |
| 5 | **Book an appointment** for a patient at the clinic or on the phone. Covers UC3, UC4 and UC5. |
| 6, 7 | Register patient, search patient |
| 8, 9 | Add patient to waiting list, offer a freed slot to the waiting list |
| 10 | List professionals, services, locations and patients (shows the ids to type) |

## Run

You need Java 17 or newer (`java -version`).

```
cd poc
./run.sh                    # macOS / Linux
run.bat                     # Windows
```

Without the scripts:

```
cd poc
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ats.Main
```

Add `--demo-patients` to start with two patients: Émilie Lavoie (P1), who has an open Cardiology referral (R1), and Olivier Côté (P2). This lets you demo the referral check without registering anyone first.

## Scripted demo

This runs every feature, including the failure cases:

```
cd poc
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ats.Main --demo-patients < demo/demo_input.txt
```

It does the following, in order:
1. Imports `data/availabilities.csv` (the 8 sample rows from the handout).
2. Searches Cardiology slots.
3. Makes "Dr. Tremblay every Monday in November, 9:00–12:00": 28 slots are created and 2 are skipped because they overlap imported slots.
4. Books P1 with Cardiology using referral R1.
5. Tries the same slot for P2: the slot is taken and the next one is proposed, then the booking fails because P2 has no referral.
6. Books P2 in Family Medicine.
7. Shows a patient scheduling conflict, a service not offered, and invalid search criteria.
8. Adds P2 to the waiting list, then offers a new slot to the list.
9. Exports to `export/demo_export.csv`, then tries a missing file.

## Design

The code follows the design class diagram (`artifacts/iteration2/Class_Diagram_*.png`). It uses the same packages, class names and method names:

- `ui`: `ClinicCLI`. It only reads input, calls a controller and prints the result.
- `controller`: `PatientController`, `BookingController`, `AvailabilityController`, `WaitlistController` (GRASP Controller).
- `catalog`: `PatientRegistry`, `ProfessionalRegistry`, `SpecialtyCatalog`, `MedicalServiceCatalog`, `LocationCatalog`, `ActivityLog`.
- `csv`: `AvailabilityCsvImporter`, `AvailabilityCsvExporter` (Pure Fabrication).
- `domain`: `Patient`, `HealthcareProfessional`, `Availability`, `Appointment`, `Referral`, `MedicalService`, `Specialty`, `ClinicLocation`, `RecurrencePattern` (`WeeklyPattern`, `MonthlyPattern`), `WaitingList`, `WaitlistEntry`, `Activity` and the enums.

Booking uses the system operations in the SSDs: `checkAvailability`, then `selectSlot` if the slot is taken, then `verifyReferralRequirement`, then `selectReferral` if needed, then `bookAppointment`.

Notes:
- **Persistence:** data is kept in memory only, as the handout allows for this iteration.
- **Reference data:** locations (L1, L2) and medical services (S1–S5) are created at startup in `Main`, because the PoC has no screens for them.
- **CSV format:** CSV rows without a `Location` column use the default location L1. The export adds `Location` and `Status` columns; the importer reads them back and ignores `Status`.
- **Resources:** resource allocation (rooms, equipment, roles) is left for Iteration III, as the handout suggests.
