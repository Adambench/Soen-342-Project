package ats.domain;

import java.time.LocalDate;
import java.time.LocalTime;

public class Activity {
    private final LocalDate date;
    private final LocalTime time;
    private final String userId;
    private final String description;

    public Activity(String userId, String description) {
        this.date = LocalDate.now();
        this.time = LocalTime.now().withNano(0);
        this.userId = userId;
        this.description = description;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getUserId() { return userId; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return date + " " + time + "  " + userId + "  " + description;
    }
}
