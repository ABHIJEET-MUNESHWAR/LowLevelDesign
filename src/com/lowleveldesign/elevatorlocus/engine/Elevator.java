package com.lowleveldesign.elevatorlocus.engine;

import com.lowleveldesign.elevatorlocus.model.Direction;
import com.lowleveldesign.elevatorlocus.model.Request;
import com.lowleveldesign.elevatorlocus.model.StopEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A single elevator that schedules its own movement using the LOOK algorithm
 * (a practical variant of SCAN): it keeps travelling in its current direction,
 * serving every compatible call on the way, and only reverses once there is no
 * further work ahead of it.
 *
 * <p>The elevator tracks three kinds of pending work:
 * <ul>
 *   <li><b>Up hall calls</b> and <b>down hall calls</b> - passengers waiting on
 *       a floor who have pressed the external UP/DOWN button but not yet
 *       boarded.</li>
 *   <li><b>Car calls</b> - destination-floor buttons pressed by passengers who
 *       are already on board.</li>
 * </ul>
 *
 * <p>A passenger's destination only becomes a car call at the moment they board,
 * which naturally enforces the rule that the destination button is pressed from
 * inside the elevator.
 */
public class Elevator {

    private final int minFloor;
    private final int maxFloor;

    private int currentFloor;
    private Direction direction = Direction.IDLE;

    private final List<Request>[] upHallCalls;
    private final List<Request>[] downHallCalls;
    private final List<Request>[] carCalls;

    @SuppressWarnings("unchecked")
    public Elevator(int startFloor, int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentFloor = startFloor;
        int size = maxFloor + 1;
        this.upHallCalls = new List[size];
        this.downHallCalls = new List[size];
        this.carCalls = new List[size];
        for (int f = 0; f < size; f++) {
            upHallCalls[f] = new ArrayList<>();
            downHallCalls[f] = new ArrayList<>();
            carCalls[f] = new ArrayList<>();
        }
    }

    /** Registers a passenger waiting on a floor as a pending hall call. */
    public void registerRequest(Request request) {
        if (request.getDirection() == Direction.UP) {
            upHallCalls[request.getSourceFloor()].add(request);
        } else {
            downHallCalls[request.getSourceFloor()].add(request);
        }
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    /**
     * @return {@code true} while any passenger is still waiting to be picked up
     *         or is on board heading to a destination.
     */
    public boolean hasWork() {
        for (int f = minFloor; f <= maxFloor; f++) {
            if (!upHallCalls[f].isEmpty() || !downHallCalls[f].isEmpty() || !carCalls[f].isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Advances the elevator by exactly one minute.
     *
     * <p>In that minute the elevator either <em>stops</em> at the current floor
     * to let passengers off/on, or <em>moves</em> one floor toward its next
     * target. Direction selection and reversals are resolved without consuming
     * time - only the physical stop or move costs a minute, matching the
     * problem's timing model.
     *
     * @param time the current simulation minute
     * @return the {@link StopEvent} produced if the elevator stopped this
     *         minute, or {@code null} if it spent the minute moving
     */
    public StopEvent advance(int time) {
        if (direction == Direction.IDLE) {
            direction = chooseInitialDirection();
        }
        // Resolve reversals with no elapsed time: if there is nothing to do in
        // the current direction (and no reason to stop here), flip around.
        while (!shouldStop(currentFloor) && !hasRequestAhead(direction)) {
            Direction reversed = direction.reverse();
            if (!hasRequestAhead(reversed)) {
                break; // work is only at the current floor; shouldStop will handle it
            }
            direction = reversed;
        }

        if (shouldStop(currentFloor)) {
            return serviceStop(time);
        }
        currentFloor += direction.step();
        return null;
    }

    /**
     * When idle, aim at the nearest pending call. Ties and same-floor calls fall
     * back to the direction the passenger there wants to travel.
     */
    private Direction chooseInitialDirection() {
        int nearest = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int f = minFloor; f <= maxFloor; f++) {
            boolean pending = !upHallCalls[f].isEmpty() || !downHallCalls[f].isEmpty() || !carCalls[f].isEmpty();
            if (pending) {
                int distance = Math.abs(f - currentFloor);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = f;
                }
            }
        }
        if (nearest < 0) {
            return Direction.IDLE;
        }
        if (nearest > currentFloor) {
            return Direction.UP;
        }
        if (nearest < currentFloor) {
            return Direction.DOWN;
        }
        return !upHallCalls[currentFloor].isEmpty() ? Direction.UP : Direction.DOWN;
    }

    /**
     * Decides whether the elevator should stop at {@code floor} given its
     * current direction. It stops for: a car call here, a hall call in the
     * direction of travel, or - at a turnaround point where nothing lies ahead -
     * an opposite-direction hall call it is about to reverse for.
     */
    private boolean shouldStop(int floor) {
        if (!carCalls[floor].isEmpty()) {
            return true;
        }
        if (direction == Direction.UP) {
            if (!upHallCalls[floor].isEmpty()) {
                return true;
            }
            return !downHallCalls[floor].isEmpty() && !hasRequestAbove(floor);
        }
        if (direction == Direction.DOWN) {
            if (!downHallCalls[floor].isEmpty()) {
                return true;
            }
            return !upHallCalls[floor].isEmpty() && !hasRequestBelow(floor);
        }
        return false;
    }

    /** Performs the drop-offs then pickups at the current floor. */
    private StopEvent serviceStop(int time) {
        List<String> alighting = new ArrayList<>();
        for (Request passenger : carCalls[currentFloor]) {
            alighting.add(passenger.getPassengerName());
        }
        carCalls[currentFloor].clear();

        List<Request> boardingRequests = selectBoardingRequests();
        List<String> boarding = new ArrayList<>();
        for (Request passenger : boardingRequests) {
            boarding.add(passenger.getPassengerName());
            carCalls[passenger.getDestinationFloor()].add(passenger);
        }
        return new StopEvent(time, currentFloor, alighting, boarding);
    }

    /**
     * Chooses which waiting passengers board at the current floor: those going
     * the elevator's way, or - at a turnaround - those waiting to go the other
     * way, in which case the elevator commits to reversing.
     */
    private List<Request> selectBoardingRequests() {
        List<Request> boarding = new ArrayList<>();
        if (direction == Direction.UP) {
            if (!upHallCalls[currentFloor].isEmpty()) {
                boarding.addAll(upHallCalls[currentFloor]);
                upHallCalls[currentFloor].clear();
            } else if (!downHallCalls[currentFloor].isEmpty()) {
                boarding.addAll(downHallCalls[currentFloor]);
                downHallCalls[currentFloor].clear();
                direction = Direction.DOWN;
            }
        } else if (direction == Direction.DOWN) {
            if (!downHallCalls[currentFloor].isEmpty()) {
                boarding.addAll(downHallCalls[currentFloor]);
                downHallCalls[currentFloor].clear();
            } else if (!upHallCalls[currentFloor].isEmpty()) {
                boarding.addAll(upHallCalls[currentFloor]);
                upHallCalls[currentFloor].clear();
                direction = Direction.UP;
            }
        }
        return boarding;
    }

    private boolean hasRequestAhead(Direction dir) {
        if (dir == Direction.UP) {
            return hasRequestAbove(currentFloor);
        }
        if (dir == Direction.DOWN) {
            return hasRequestBelow(currentFloor);
        }
        return false;
    }

    private boolean hasRequestAbove(int floor) {
        for (int f = floor + 1; f <= maxFloor; f++) {
            if (!upHallCalls[f].isEmpty() || !downHallCalls[f].isEmpty() || !carCalls[f].isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRequestBelow(int floor) {
        for (int f = floor - 1; f >= minFloor; f--) {
            if (!upHallCalls[f].isEmpty() || !downHallCalls[f].isEmpty() || !carCalls[f].isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
