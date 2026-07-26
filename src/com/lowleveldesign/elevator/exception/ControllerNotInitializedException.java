package com.lowleveldesign.elevator.exception;

/**
 * Thrown when the {@code ElevatorController} singleton is accessed via the
 * no-arg {@code getInstance()} before it has been initialized with an
 * elevator count and capacity.
 */
public class ControllerNotInitializedException extends ElevatorException {

    private static final long serialVersionUID = 1L;

    public ControllerNotInitializedException() {
        super("ElevatorController has not been initialized. Call getInstance(elevatorCount, capacityPerElevator) first.");
    }
}
