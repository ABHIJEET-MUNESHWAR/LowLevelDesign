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
    Elevator selectElevator(List<Elevator> elevators, Request request);
}
