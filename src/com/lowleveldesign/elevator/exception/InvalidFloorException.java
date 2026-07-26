package com.lowleveldesign.elevator.exception;

/**
 * Thrown when a requested floor lies outside the building's valid range,
 * e.g. a negative floor or one at/above the building's floor count.
 */
public class InvalidFloorException extends ElevatorException {

    private static final long serialVersionUID = 1L;

    /**
     * Builds the message itself so every throw site reports the valid range
     * consistently.
     *
     * @param floor          the rejected floor
     * @param numberOfFloors the building's floor count, defining the valid range
     */
    public InvalidFloorException(int floor, int numberOfFloors) {
        super("Floor " + floor + " is out of building range [0, " + (numberOfFloors - 1) + "]");
    }
}
