package com.lowleveldesign.elevator.model;

/**
 * Models the elevator cabin door. Kept intentionally simple (no timers) so the
 * behaviour is deterministic and easy to reason about / test.
 */
public class Door {

    private DoorState state;

    public Door() {
        this.state = DoorState.CLOSED;
    }

    public void open() {
        state = DoorState.OPEN;
        System.out.println("  Door opened");
    }

    public void close() {
        state = DoorState.CLOSED;
        System.out.println("  Door closed");
    }

    public DoorState getState() {
        return state;
    }
}
