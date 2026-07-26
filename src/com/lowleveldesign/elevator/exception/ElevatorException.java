package com.lowleveldesign.elevator.exception;

/**
 * Base type for all elevator-system failures. Extending {@link RuntimeException}
 * keeps the API uncluttered (these represent programming/usage errors and
 * unsatisfiable requests rather than recoverable I/O conditions), while a
 * shared supertype lets callers catch every elevator failure with a single
 * {@code catch (ElevatorException e)} when they don't care about the specific
 * cause.
 */
public class ElevatorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ElevatorException(String message) {
        super(message);
    }
}
