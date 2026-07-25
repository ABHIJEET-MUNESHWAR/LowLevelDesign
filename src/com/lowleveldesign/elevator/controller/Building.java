package com.lowleveldesign.elevator.controller;

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

    public ElevatorController getController() {
        return controller;
    }

    public void validateFloor(int floor) {
        if (floor < 0 || floor >= numberOfFloors) {
            throw new IllegalArgumentException("Floor " + floor + " out of building range [0, " + (numberOfFloors - 1) + "]");
        }
    }
}
