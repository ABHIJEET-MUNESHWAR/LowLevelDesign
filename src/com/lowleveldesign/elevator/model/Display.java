package com.lowleveldesign.elevator.model;

/**
 * Simple in-cabin/floor display that always reflects the elevator's latest
 * known floor and direction.
 */
public class Display {

    public void update(int elevatorId, int currentFloor, Direction direction) {
        System.out.printf("  [Elevator %d Display] Floor: %d | Direction: %s%n", elevatorId, currentFloor, direction);
    }
}
