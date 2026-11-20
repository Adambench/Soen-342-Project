package ats.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** Information Expert / Creator for waiting-list entries. */
public class WaitingList {
    private final List<WaitlistEntry> entries = new ArrayList<>();
    private final Function<String, HealthcareProfessional> professionalLookup;
    private final Function<String, Specialty> specialtyLookup;
    private final Function<String, Availability> availabilityLookup;

    public WaitingList(Function<String, HealthcareProfessional> professionalLookup,
                       Function<String, Specialty> specialtyLookup,
                       Function<String, Availability> availabilityLookup) {
        this.professionalLookup = professionalLookup;
        this.specialtyLookup = specialtyLookup;
        this.availabilityLookup = availabilityLookup;
    }

    public WaitlistEntry addEntry(Patient patient, String professionalId, String specialty,
                                  LocalDate dateFrom, LocalDate dateTo) {
        HealthcareProfessional hp = professionalId == null ? null : professionalLookup.apply(professionalId);
        Specialty s = specialty == null ? null : specialtyLookup.apply(specialty);
        WaitlistEntry entry = new WaitlistEntry(patient, hp, s, dateFrom, dateTo);
        entries.add(entry);
        assignInitialPriority(entry);
        return entry;
    }

    /** Highest-priority (lowest number) Waiting entry that matches the slot and has not declined it. */
    public WaitlistEntry findNextEligible(String availabilityId) {
        Availability slot = availabilityLookup.apply(availabilityId);
        if (slot == null) {
            throw new BookingException("unknown availability " + availabilityId);
        }
        return entries.stream()
                .filter(e -> e.matches(slot))
                .min(Comparator.comparingInt(WaitlistEntry::getPriority))
                .orElse(null);
    }

    /** New entries go to the back of the queue (first come, first served). */
    private void assignInitialPriority(WaitlistEntry entry) {
        int waiting = (int) entries.stream().filter(e -> e.getStatus() == WaitlistStatus.WAITING).count();
        entry.setPriority(waiting);
    }

    public WaitlistEntry findEntry(String entryId) {
        for (WaitlistEntry e : entries) {
            if (e.getEntryId().equalsIgnoreCase(entryId)) {
                return e;
            }
        }
        return null;
    }

    public List<WaitlistEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
