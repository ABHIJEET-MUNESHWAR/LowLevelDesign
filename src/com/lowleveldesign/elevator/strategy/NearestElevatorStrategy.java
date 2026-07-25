package com.lowleveldesign.elevator.strategy;

import com.lowleveldesign.elevator.model.Direction;
import com.lowleveldesign.elevator.model.Elevator;
import com.lowleveldesign.elevator.model.ElevatorState;
import com.lowleveldesign.elevator.model.Request;

import java.util.List;

/**
 * Picks the elevator that can reach the requested floor with the least cost:
 * 1. Idle elevators are preferred, ranked by distance.
 * 2. Otherwise, an elevator already moving toward the request in the same
 *    direction and not yet past the requested floor is preferred.
 * 3. Falls back to the closest elevator overall.
 */
public class NearestElevatorStrategy implements SchedulingStrategy {

    @Override
    public Elevator selectElevator(List<Elevator> elevators, Request request) {
        Elevator best = null;
        int bestCost = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            int cost = computeCost(elevator, request);
            if (cost < bestCost) {
                bestCost = cost;
                best = elevator;
            }
        }
        return best;
    }

    private int computeCost(Elevator elevator, Request request) {
        int distance = elevator.distanceTo(request.getFloor());

        if (elevator.getState() == ElevatorState.IDLE) {
            return distance;
        }

        boolean sameDirection = elevator.getDirection() == request.getDirection();
        boolean movingTowardsRequest =
                (elevator.getDirection() == Direction.UP && elevator.getCurrentFloor() <= request.getFloor()) ||
                (elevator.getDirection() == Direction.DOWN && elevator.getCurrentFloor() >= request.getFloor());

        if (sameDirection && movingTowardsRequest) {
            return distance; // best case, no detour
        }
        // Moving away or in opposite direction: penalize so idle/aligned cars win.
        return distance + 1000;
    }
}
