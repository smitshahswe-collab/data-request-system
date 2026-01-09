package com.example.datarequestsystem.model;

public enum Status {
    RECEIVED,
    IN_REVIEW,
    COMPLETED,
    REJECTED;

    public boolean canTransitionTo(Status newStatus) {
        return switch (this) {
            case RECEIVED -> newStatus == IN_REVIEW || newStatus == REJECTED;
            case IN_REVIEW -> newStatus == COMPLETED || newStatus == REJECTED;
            case COMPLETED, REJECTED -> false;
        };
    }
}