package com.lowleveldesign.elevatorlocus.engine;

import com.lowleveldesign.elevatorlocus.model.Request;
import com.lowleveldesign.elevatorlocus.model.StopEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Drives a single {@link Elevator} through a minute-by-minute simulation of a
 * batch of {@link Request}s.
 *
 * <p>Each request only becomes visible to the elevator at (or after) its own
 * request time, which is what lets the LOOK scheduler correctly ignore a hall
 * call that arrives after the elevator has already passed that floor.
 */
public class ElevatorSimulator {

    /** Default building: 20 floors numbered 1..20, car parked on floor 1. */
    public static final int MIN_FLOOR = 1;
    public static final int MAX_FLOOR = 20;
    public static final int START_FLOOR = 1;

    private final int minFloor;
    private final int maxFloor;
    private final int startFloor;

    public ElevatorSimulator() {
        this(START_FLOOR, MIN_FLOOR, MAX_FLOOR);
    }

    public ElevatorSimulator(int startFloor, int minFloor, int maxFloor) {
        this.startFloor = startFloor;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }

    /**
     * Runs the simulation to completion.
     *
     * @param requests the passenger requests to serve (any order)
     * @return the ordered list of stops the elevator made
     */
    public List<StopEvent> simulate(List<Request> requests) {
        List<Request> pending = new ArrayList<>(requests);
        pending.sort(Comparator.comparingInt(Request::getRequestTime));

        Elevator elevator = new Elevator(startFloor, minFloor, maxFloor);
        List<StopEvent> events = new ArrayList<>();

        int cursor = 0;
        int time = 0;
        // Upper bound on minutes: worst case is one full sweep per request plus
        // slack; guards against any unexpected non-termination.
        int safetyLimit = (requests.size() + 2) * (maxFloor - minFloor + 1) * 4 + 100;

        while (time <= safetyLimit) {
            while (cursor < pending.size() && pending.get(cursor).getRequestTime() <= time) {
                elevator.registerRequest(pending.get(cursor));
                cursor++;
            }

            if (!elevator.hasWork()) {
                if (cursor >= pending.size()) {
                    break; // nothing left to do and no future requests
                }
                time = pending.get(cursor).getRequestTime(); // fast-forward idle time
                continue;
            }

            StopEvent event = elevator.advance(time);
            if (event != null && !event.isEmpty()) {
                events.add(event);
            }
            time++;
        }
        return events;
    }
}
