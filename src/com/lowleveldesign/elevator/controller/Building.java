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

    /**
     * Creates a building and initializes its elevator bank. Configuration is
     * validated up front so a nonsensical setup fails here rather than
     * surfacing as a confusing error during dispatch.
     *
     * @param numberOfFloors      total floors, giving a valid range of {@code [0, numberOfFloors - 1]}
     * @param numberOfElevators   how many elevators serve this building
     * @param capacityPerElevator maximum passengers per elevator
     * @throws InvalidBuildingConfigurationException if any argument is not positive
     */
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

    /**
     * Returns how many floors this building has.
     *
     * @return the floor count; valid floors are {@code 0} through {@code count - 1}
     */
    public int getNumberOfFloors() {
        return numberOfFloors;
    }

    /**
     * Ensures a floor actually exists in this building. Centralizing the check
     * here keeps bounds logic in one place for every entry point.
     *
     * @param floor the floor to validate
     * @throws InvalidFloorException if the floor is negative or at/above the floor count
     */
    public void validateFloor(int floor) {
        if (floor < 0 || floor >= numberOfFloors) {
            throw new InvalidFloorException(floor, numberOfFloors);
        }
    }

    /**
     * Raises a hall call after validating the floor is within building bounds,
     * then delegates dispatch to the {@link ElevatorController}.
     *
     * @param floor     the floor the passenger is waiting on
     * @param direction the direction the passenger wants to travel
     * @return the elevator assigned to serve the call, so callers can register
     *         an arrival listener on it
     * @throws InvalidFloorException if the floor is outside the building
     */
    public Elevator submitHallRequest(int floor, Direction direction) {
        validateFloor(floor);
        return controller.submitHallRequest(floor, direction);
    }

    /**
     * Queues a destination floor for a passenger already inside a cabin, after
     * validating the floor is within building bounds.
     *
     * @param elevatorId       the elevator the passenger boarded
     * @param destinationFloor the floor they want to reach
     * @throws InvalidFloorException if the floor is outside the building
     */
    public void submitDestinationRequest(int elevatorId, int destinationFloor) {
        validateFloor(destinationFloor);
        controller.submitDestinationRequest(elevatorId, destinationFloor);
    }
}

