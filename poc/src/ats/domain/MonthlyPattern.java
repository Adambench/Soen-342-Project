package ats.domain;

import java.time.LocalDate;

/** Repeats once a month on the given day of the month, e.g. the 15th. */
public class MonthlyPattern implements RecurrencePattern {
    private final int dayOfMonth;

    public MonthlyPattern(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new BookingException("day of month must be between 1 and 31");
        }
        this.dayOfMonth = dayOfMonth;
    }

    @Override
    public boolean occursOn(LocalDate date) {
        return date.getDayOfMonth() == dayOfMonth;
    }

    @Override
    public String toString() {
        return "monthly on day " + dayOfMonth;
    }
}
