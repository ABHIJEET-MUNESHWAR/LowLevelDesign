package com.lowleveldesign.elevator.demo;

/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
*/

import com.lowleveldesign.elevator.controller.Building;
import com.lowleveldesign.elevator.controller.ElevatorController;
import com.lowleveldesign.elevator.model.Direction;
import com.lowleveldesign.elevator.model.Elevator;

/**
 * Simple simulation demonstrating hall calls and destination calls being
 * dispatched and served across a bank of elevators using the LOOK algorithm.
 */
public class ElevatorSystemDemo {

    public static void main(String[] args) {
        Building building = new Building(10, 2, 8); // 10 floors, 2 elevators, capacity 8
        ElevatorController controller = building.getController();

        // Passenger on floor 3 wants to go up; passenger on floor 8 wants to go down.
        Elevator elevatorForFloor3 = controller.submitHallRequest(3, Direction.UP);
        Elevator elevatorForFloor8 = controller.submitHallRequest(8, Direction.DOWN);
        Elevator elevatorForFloor2 = controller.submitHallRequest(2, Direction.UP);

        // A destination can only be requested once the passenger has actually
        // boarded, i.e. once the assigned elevator opens its doors at their
        // pickup floor - never before. Registering a listener enforces that
        // ordering instead of submitting the destination call up front.
        elevatorForFloor3.addListener((elevatorId, floor) -> {
            if (floor == 3) {
                controller.submitDestinationRequest(elevatorId, 7);
            }
        });
        elevatorForFloor8.addListener((elevatorId, floor) -> {
            if (floor == 8) {
                controller.submitDestinationRequest(elevatorId, 2);
            }
        });
        elevatorForFloor2.addListener((elevatorId, floor) -> {
            if (floor == 2) {
                controller.submitDestinationRequest(elevatorId, 5);
            }
        });

        int steps = 0;
        while (controller.anyElevatorBusy() && steps < 50) {
            controller.stepAll();
            steps++;
        }

        System.out.println("\nSimulation complete in " + steps + " steps.");
        controller.getElevators().forEach(e ->
                System.out.printf("Elevator %d final floor: %d, state: %s%n",
                        e.getId(), e.getCurrentFloor(), e.getState()));
    }
}
