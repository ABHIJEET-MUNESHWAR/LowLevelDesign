package com.lowleveldesign.elevator.model;

public enum DoorState {
    /** Door is open; passengers may board or alight and the cabin must not move. */
    OPEN,
    /** Door is closed; the cabin is safe to travel. */
    CLOSED
}
