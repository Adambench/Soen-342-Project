package ats.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/** Repeats on the given day(s) of the week, e.g. every Monday and Wednesday. */
public class WeeklyPattern implements RecurrencePattern {
    private final Set<DayOfWeek> daysOfWeek;

    public WeeklyPattern(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = EnumSet.copyOf(daysOfWeek);
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return daysOfWeek.contains(date.getDayOfWeek());
    }

    @Override
    public String toString() {
        return "every " + daysOfWeek;
    }
}
