package com.lowleveldesign.elevator.demo;

/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
*/

import com.lowleveldesign.elevator.controller.Building;
import com.lowleveldesign.elevator.controller.ElevatorController;
import com.lowleveldesign.elevator.model.Direction;

/**
 * Simple simulation demonstrating hall calls and destination calls being
 * dispatched and served across a bank of elevators using the LOOK algorithm.
 */
public class ElevatorSystemDemo {

    public static void main(String[] args) {
        Building building = new Building(10, 2, 8); // 10 floors, 2 elevators, capacity 8
        ElevatorController controller = building.getController();

        // Passenger on floor 3 wants to go up; passenger on floor 8 wants to go down.
        controller.submitHallRequest(3, Direction.UP);
        controller.submitHallRequest(8, Direction.DOWN);

        // Once picked up, passengers choose their destination floors.
        controller.submitDestinationRequest(1, 7);
        controller.submitDestinationRequest(2, 2);

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
