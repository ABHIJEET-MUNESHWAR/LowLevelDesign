package com.lowleveldesign.elevator.exception;

/**
 * Thrown when a destination request references an elevator id that does not
 * exist in the building's elevator bank.
 */
public class ElevatorNotFoundException extends ElevatorException {

    private static final long serialVersionUID = 1L;

    public ElevatorNotFoundException(int elevatorId) {
        super("No elevator exists with id " + elevatorId);
    }
}
