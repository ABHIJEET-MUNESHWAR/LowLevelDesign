package com.lowleveldesign.elevator.demo;

/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
*/

import com.lowleveldesign.elevator.controller.Building;
import com.lowleveldesign.elevator.controller.ElevatorController;
import com.lowleveldesign.elevator.model.Direction;
import com.lowleveldesign.elevator.model.Elevator;
import com.lowleveldesign.elevator.strategy.NearestElevatorStrategy;

/**
 * Simple simulation demonstrating hall calls and destination calls being
 * dispatched and served across a bank of elevators using the LOOK algorithm.
 */
public class ElevatorSystemDemo {

    /**
     * Runs the simulation: raises three hall calls, wires each passenger's
     * destination request to fire only once their elevator arrives, then steps
     * the bank until every elevator is idle and prints a summary.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        Building building = new Building(10, 2, 8); // 10 floors, 2 elevators, capacity 8
        System.out.printf("Building has %d floors%n", building.getNumberOfFloors());

        // ElevatorController is a singleton; once Building has initialized it,
        // any other component can fetch the same instance without needing a
        // Building reference or re-supplying elevator count/capacity.
        ElevatorController controller = ElevatorController.getInstance();
        controller.setSchedulingStrategy(new NearestElevatorStrategy()); // pluggable dispatch algorithm

        // Passenger on floor 3 wants to go up; passenger on floor 8 wants to go down.
        // Routed through Building so floor bounds are validated before dispatch.
        Elevator elevatorForFloor3 = building.submitHallRequest(3, Direction.UP);
        Elevator elevatorForFloor8 = building.submitHallRequest(8, Direction.DOWN);

        // A destination can only be requested once the passenger has actually
        // boarded, i.e. once the assigned elevator opens its doors at their
        // pickup floor - never before. Registering a listener enforces that
        // ordering instead of submitting the destination call up front.
        elevatorForFloor3.addListener((elevatorId, floor) -> {
            if (floor == 3) {
                building.submitDestinationRequest(elevatorId, 7);
            }
        });
        elevatorForFloor8.addListener((elevatorId, floor) -> {
            if (floor == 8) {
                building.submitDestinationRequest(elevatorId, 2);
            }
        });

        int steps = 0;
        while (controller.anyElevatorBusy() && steps < 50) {
            controller.stepAll();
            steps++;
        }

        System.out.println("\nSimulation complete in " + steps + " steps.");
        controller.getElevators().forEach(e ->
                System.out.printf("Elevator %d final floor: %d, capacity: %d, state: %s%n",
                        e.getId(), e.getCurrentFloor(), e.getCapacity(), e.getState()));
    }
}
