package com.lowleveldesign.elevator.controller;

import com.lowleveldesign.elevator.exception.ControllerNotInitializedException;
import com.lowleveldesign.elevator.exception.ElevatorNotFoundException;
import com.lowleveldesign.elevator.exception.NoElevatorAvailableException;
import com.lowleveldesign.elevator.model.Direction;
import com.lowleveldesign.elevator.model.Elevator;
import com.lowleveldesign.elevator.model.Request;
import com.lowleveldesign.elevator.strategy.NearestElevatorStrategy;
import com.lowleveldesign.elevator.strategy.SchedulingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton facade that owns all elevators in a building and dispatches
 * incoming hall/destination requests to the elevator chosen by the
 * configured {@link SchedulingStrategy}.
 */
public class ElevatorController {

    private static ElevatorController instance;

    private final List<Elevator> elevators;
    private SchedulingStrategy schedulingStrategy;

    private ElevatorController(int elevatorCount, int capacityPerElevator) {
        this.elevators = new ArrayList<>();
        for (int i = 1; i <= elevatorCount; i++) {
            elevators.add(new Elevator(i, capacityPerElevator));
        }
        this.schedulingStrategy = new NearestElevatorStrategy();
    }

    public static synchronized ElevatorController getInstance(int elevatorCount, int capacityPerElevator) {
        if (instance == null) {
            instance = new ElevatorController(elevatorCount, capacityPerElevator);
        }
        return instance;
    }

    public static synchronized ElevatorController getInstance() {
        if (instance == null) {
            throw new ControllerNotInitializedException();
        }
        return instance;
    }

    public void setSchedulingStrategy(SchedulingStrategy strategy) {
        this.schedulingStrategy = strategy;
    }

    /**
     * External hall call, e.g. a passenger pressing UP/DOWN on a floor.
     * Returns the elevator chosen to serve it, so callers can (for example)
     * register an {@link com.lowleveldesign.elevator.model.ElevatorListener}
     * to raise a destination request only once that elevator actually opens
     * its doors at the pickup floor.
     */
    public Elevator submitHallRequest(int floor, Direction direction) {
        Request request = Request.externalRequest(floor, direction);
        Elevator chosen = schedulingStrategy.selectElevator(elevators, request);
        if (chosen == null) {
            throw new NoElevatorAvailableException("No elevator could be assigned to serve " + request);
        }
        System.out.printf("Dispatching hall call %s -> Elevator %d%n", request, chosen.getId());
        chosen.addStop(floor);
        return chosen;
    }

    /** Internal destination call, e.g. a passenger pressing a floor button inside the cabin. */
    public void submitDestinationRequest(int elevatorId, int destinationFloor) {
        Elevator elevator = getElevator(elevatorId);
        Request request = Request.internalRequest(destinationFloor);
        System.out.printf("Elevator %d received %s%n", elevatorId, request);
        elevator.addStop(destinationFloor);
    }

    public Elevator getElevator(int elevatorId) {
        return elevators.stream()
                .filter(e -> e.getId() == elevatorId)
                .findFirst()
                .orElseThrow(() -> new ElevatorNotFoundException(elevatorId));
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    /** Advances every elevator that still has pending stops by one step. */
    public void stepAll() {
        for (Elevator elevator : elevators) {
            if (elevator.hasPendingRequests()) {
                elevator.step();
            }
        }
    }

    public boolean anyElevatorBusy() {
        return elevators.stream().anyMatch(Elevator::hasPendingRequests);
    }
}
