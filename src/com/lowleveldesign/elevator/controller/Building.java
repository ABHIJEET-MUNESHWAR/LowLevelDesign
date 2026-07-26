package com.lowleveldesign.elevator.controller;

import com.lowleveldesign.elevator.model.Direction;
import com.lowleveldesign.elevator.model.Elevator;

/**
 * Represents the building housing the elevator bank. Kept minimal - mainly
 * validates floor bounds and owns the {@link ElevatorController}.
 */
public class Building {

    private final int numberOfFloors;
    private final ElevatorController controller;

    public Building(int numberOfFloors, int numberOfElevators, int capacityPerElevator) {
        this.numberOfFloors = numberOfFloors;
        this.controller = ElevatorController.getInstance(numberOfElevators, capacityPerElevator);
    }

    public int getNumberOfFloors() {
        return numberOfFloors;
    }

    public void validateFloor(int floor) {
        if (floor < 0 || floor >= numberOfFloors) {
            throw new IllegalArgumentException("Floor " + floor + " out of building range [0, " + (numberOfFloors - 1) + "]");
        }
    }

    /** Validates the floor is within building bounds before dispatching the hall call. */
    public Elevator submitHallRequest(int floor, Direction direction) {
        validateFloor(floor);
        return controller.submitHallRequest(floor, direction);
    }

    /** Validates the destination floor is within building bounds before queuing it. */
    public void submitDestinationRequest(int elevatorId, int destinationFloor) {
        validateFloor(destinationFloor);
        controller.submitDestinationRequest(elevatorId, destinationFloor);
    }
}

