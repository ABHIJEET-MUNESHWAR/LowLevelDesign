package com.lowleveldesign.elevator.model;

/**
 * Models the elevator cabin door. Kept intentionally simple (no timers) so the
 * behaviour is deterministic and easy to reason about / test.
 */
public class Door {

    private DoorState state;

    /** Creates a door in the safe default state: closed. */
    public Door() {
        this.state = DoorState.CLOSED;
    }

    /**
     * Opens the door so passengers can board or alight. Called when the
     * elevator arrives at a requested stop.
     */
    public void open() {
        state = DoorState.OPEN;
        System.out.println("  Door opened");
    }

    /**
     * Closes the door so the elevator may safely resume travel.
     */
    public void close() {
        state = DoorState.CLOSED;
        System.out.println("  Door closed");
    }

    /**
     * Returns whether the door is currently open or closed.
     *
     * @return the current door state
     */
    public DoorState getState() {
        return state;
    }
}
