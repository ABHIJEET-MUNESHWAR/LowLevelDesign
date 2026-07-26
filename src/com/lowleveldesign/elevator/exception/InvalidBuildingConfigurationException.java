package com.lowleveldesign.elevator.exception;

/**
 * Thrown when a building is constructed with a nonsensical configuration,
 * such as a non-positive number of floors, elevators, or elevator capacity.
 * Failing fast at construction avoids far more confusing failures later
 * during dispatch.
 */
public class InvalidBuildingConfigurationException extends ElevatorException {

    private static final long serialVersionUID = 1L;

    public InvalidBuildingConfigurationException(String message) {
        super(message);
    }
}
