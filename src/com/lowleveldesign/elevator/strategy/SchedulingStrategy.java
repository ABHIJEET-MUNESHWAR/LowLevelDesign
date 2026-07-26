package com.lowleveldesign.elevator.strategy;

import com.lowleveldesign.elevator.model.Elevator;
import com.lowleveldesign.elevator.model.Request;

import java.util.List;

/**
 * Strategy pattern: decides which elevator should serve a given hall request.
 * Allows swapping dispatch algorithms (nearest car, zoning, destination
 * dispatch, etc.) without changing the controller.
 */
public interface SchedulingStrategy {

    /**
     * Chooses which elevator should serve the given request. Implementations
     * decide the policy (nearest car, zoning, destination dispatch, load
     * balancing, ...) without the controller needing to change.
     *
     * @param elevators all elevators in the building's bank
     * @param request   the request needing an assignment
     * @return the elevator that should serve it, or {@code null} if none can
     *         (the controller converts this into a
     *         {@code NoElevatorAvailableException})
     */
    Elevator selectElevator(List<Elevator> elevators, Request request);
}
