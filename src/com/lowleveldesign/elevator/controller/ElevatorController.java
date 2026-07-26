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

    /**
     * Private to enforce the Singleton — obtain the controller via
     * {@link #getInstance(int, int)}.
     *
     * @param elevatorCount       how many elevators to create, numbered from 1
     * @param capacityPerElevator passenger capacity for each elevator
     */
    private ElevatorController(int elevatorCount, int capacityPerElevator) {
        this.elevators = new ArrayList<>();
        for (int i = 1; i <= elevatorCount; i++) {
            elevators.add(new Elevator(i, capacityPerElevator));
        }
        this.schedulingStrategy = new NearestElevatorStrategy();
    }

    /**
     * Returns the shared controller, creating and initializing it on first
     * call. Subsequent calls return the existing instance and ignore the
     * arguments, since a building has exactly one elevator bank.
     *
     * @param elevatorCount       elevators to create on first initialization
     * @param capacityPerElevator capacity per elevator on first initialization
     * @return the singleton controller
     */
    public static synchronized ElevatorController getInstance(int elevatorCount, int capacityPerElevator) {
        if (instance == null) {
            instance = new ElevatorController(elevatorCount, capacityPerElevator);
        }
        return instance;
    }

    /**
     * Returns the already-initialized controller, letting any component reach
     * it without holding a {@link Building} reference or re-supplying config.
     *
     * @return the singleton controller
     * @throws ControllerNotInitializedException if {@link #getInstance(int, int)}
     *                                           has not been called yet
     */
    public static synchronized ElevatorController getInstance() {
        if (instance == null) {
            throw new ControllerNotInitializedException();
        }
        return instance;
    }

    /**
     * Swaps the dispatch algorithm at runtime — the extension point of the
     * Strategy pattern, allowing zoning, destination dispatch or load-aware
     * policies without modifying this class.
     *
     * @param strategy the algorithm to use for subsequent hall calls
     */
    public void setSchedulingStrategy(SchedulingStrategy strategy) {
        this.schedulingStrategy = strategy;
    }

    /**
     * Handles a hall call: builds the request, asks the configured strategy
     * which elevator should serve it, and queues the pickup floor on that car.
     *
     * @param floor     the floor the passenger is waiting on
     * @param direction the direction they want to travel
     * @return the elevator assigned to serve the call, so callers can register
     *         an {@link com.lowleveldesign.elevator.model.ElevatorListener} and
     *         raise their destination request only once it actually arrives
     * @throws com.lowleveldesign.elevator.exception.InvalidRequestException if the direction is IDLE
     * @throws NoElevatorAvailableException if the strategy selects no elevator
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

    /**
     * Handles a destination call from a passenger already inside a cabin. No
     * scheduling decision is needed because the elevator is already fixed.
     *
     * @param elevatorId       the elevator the passenger boarded
     * @param destinationFloor the floor they want to reach
     * @throws ElevatorNotFoundException if no elevator has the given id
     */
    public void submitDestinationRequest(int elevatorId, int destinationFloor) {
        Elevator elevator = getElevator(elevatorId);
        Request request = Request.internalRequest(destinationFloor);
        System.out.printf("Elevator %d received %s%n", elevatorId, request);
        elevator.addStop(destinationFloor);
    }

    /**
     * Looks up an elevator by id.
     *
     * @param elevatorId the id to find
     * @return the matching elevator
     * @throws ElevatorNotFoundException if no elevator has that id
     */
    public Elevator getElevator(int elevatorId) {
        return elevators.stream()
                .filter(e -> e.getId() == elevatorId)
                .findFirst()
                .orElseThrow(() -> new ElevatorNotFoundException(elevatorId));
    }

    /**
     * Returns every elevator in the building's bank, e.g. for reporting final
     * positions or feeding a scheduling strategy.
     *
     * @return the list of managed elevators
     */
    public List<Elevator> getElevators() {
        return elevators;
    }

    /**
     * Advances the whole bank by one simulation tick, stepping only elevators
     * that still have pending stops so idle cars stay put.
     */
    public void stepAll() {
        for (Elevator elevator : elevators) {
            if (elevator.hasPendingRequests()) {
                elevator.step();
            }
        }
    }

    /**
     * Reports whether any elevator still has work outstanding, used as the
     * termination condition for a simulation loop.
     *
     * @return {@code true} while at least one elevator has pending stops
     */
    public boolean anyElevatorBusy() {
        return elevators.stream().anyMatch(Elevator::hasPendingRequests);
    }
}
