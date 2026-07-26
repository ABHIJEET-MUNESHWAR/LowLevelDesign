package com.lowleveldesign.elevator.exception;

/**
 * Thrown when no elevator can be assigned to serve a request - e.g. the
 * elevator bank is empty, or a custom {@code SchedulingStrategy} declined to
 * select one (every car out of service or out of zone). Without this, a
 * strategy returning {@code null} would surface much later as an opaque
 * {@link NullPointerException} far from the real cause.
 */
public class NoElevatorAvailableException extends ElevatorException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message description of the request that could not be assigned
     */
    public NoElevatorAvailableException(String message) {
        super(message);
    }
}
