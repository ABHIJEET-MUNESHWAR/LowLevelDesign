package com.lowleveldesign.elevator.exception;

/**
 * Thrown when a request is malformed - most notably a hall (external) call
 * raised with {@code Direction.IDLE}, which is meaningless since a passenger
 * pressing a hall button must be asking to go either UP or DOWN.
 */
public class InvalidRequestException extends ElevatorException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
