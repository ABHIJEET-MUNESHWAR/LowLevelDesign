package com.lowleveldesign.elevator.model;

public enum ElevatorState {
    /** No pending stops; the elevator is parked and available for dispatch. */
    IDLE,
    /** Travelling between floors with at least one stop still queued. */
    MOVING,
    /** Halted at a floor with its doors cycling for boarding/alighting. */
    STOPPED
}
