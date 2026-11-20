package ats.domain;

import java.time.LocalDate;

/** Polymorphism / Protected Variations: a new repetition rule is a new class. */
public interface RecurrencePattern {
    boolean occursOn(LocalDate date);
}
