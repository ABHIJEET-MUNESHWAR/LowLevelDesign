package com.lowleveldesign.elevator.controller;

import com.lowleveldesign.elevator.exception.InvalidBuildingConfigurationException;
import com.lowleveldesign.elevator.exception.InvalidFloorException;
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
        if (numberOfFloors <= 0) {
            throw new InvalidBuildingConfigurationException("A building must have at least one floor, but was " + numberOfFloors);
        }
        if (numberOfElevators <= 0) {
            throw new InvalidBuildingConfigurationException("A building must have at least one elevator, but was " + numberOfElevators);
        }
        if (capacityPerElevator <= 0) {
            throw new InvalidBuildingConfigurationException("Elevator capacity must be positive, but was " + capacityPerElevator);
        }
        this.numberOfFloors = numberOfFloors;
        this.controller = ElevatorController.getInstance(numberOfElevators, capacityPerElevator);
    }

    public int getNumberOfFloors() {
        return numberOfFloors;
    }

    public void validateFloor(int floor) {
        if (floor < 0 || floor >= numberOfFloors) {
            throw new InvalidFloorException(floor, numberOfFloors);
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

