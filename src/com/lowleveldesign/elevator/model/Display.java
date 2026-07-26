package com.lowleveldesign.elevator.model;

/**
 * Simple in-cabin/floor display that always reflects the elevator's latest
 * known floor and direction.
 */
public class Display {

    private int currentFloor;
    private Direction direction;

    /**
     * Refreshes the indicator with the elevator's latest position and heading,
     * and echoes it so the simulation is observable.
     *
     * @param elevatorId   the elevator this display belongs to
     * @param currentFloor the floor the elevator is now at
     * @param direction    the direction it is now travelling
     */
    public void update(int elevatorId, int currentFloor, Direction direction) {
        this.currentFloor = currentFloor;
        this.direction = direction;
        System.out.printf("  [Elevator %d Display] Floor: %d | Direction: %s%n", elevatorId, currentFloor, direction);
    }

    /**
     * Returns the floor currently shown on the indicator.
     *
     * @return the last floor passed to {@link #update(int, int, Direction)}
     */
    public int getCurrentFloor() {
        return currentFloor;
    }

    /**
     * Returns the direction arrow currently shown on the indicator.
     *
     * @return the last direction passed to {@link #update(int, int, Direction)}
     */
    public Direction getDirection() {
        return direction;
    }
}
