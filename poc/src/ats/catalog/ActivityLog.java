package ats.catalog;

import ats.domain.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure Fabrication: one place that records activities for accountability. */
public class ActivityLog {
    private final List<Activity> activities = new ArrayList<>();

    public void record(String userId, String action, String subjectId, String relatedId) {
        activities.add(new Activity(userId, action + " " + subjectId + (relatedId == null ? "" : " " + relatedId)));
    }

    public List<Activity> getActivities() {
        return Collections.unmodifiableList(activities);
    }
}
