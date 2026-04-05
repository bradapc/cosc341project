package com.example.orchard;

import com.google.firebase.Timestamp;

public class Shift {
    private String userId;
    private Timestamp startTime;
    private Timestamp endTime;
    private boolean active;

    public Shift() {} // Required for Firestore

    public Shift(String userId, Timestamp startTime) {
        this.userId = userId;
        this.startTime = startTime;
        this.active = true;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
