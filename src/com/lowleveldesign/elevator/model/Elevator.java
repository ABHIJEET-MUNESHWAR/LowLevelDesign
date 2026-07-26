package com.lowleveldesign.elevator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A single elevator cabin. Uses the LOOK/SCAN algorithm: it keeps moving in
 * its current direction serving all pending stops in that direction before
 * reversing, which avoids starvation and minimizes direction changes.
 */
public class Elevator {

    private final int id;
    private final int capacity;
    private int currentFloor;
    private Direction direction;
    private ElevatorState state;
    private final Door door;
    private final Display display;

    // Pending stops above/below the current floor, kept sorted for LOOK scheduling.
    private final TreeSet<Integer> upStops;
    private final TreeSet<Integer> downStops;

    // Observers notified when doors open at a floor, e.g. so a "passenger"
    // can raise a destination request only once they've actually boarded.
    private final List<ElevatorListener> listeners;

    /**
     * Creates an elevator parked and idle at floor 0.
     *
     * @param id       unique identifier within the building's elevator bank
     * @param capacity maximum number of passengers the cabin can hold
     */
    public Elevator(int id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.state = ElevatorState.IDLE;
        this.door = new Door();
        this.display = new Display();
        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * Registers an observer to be notified whenever this elevator opens its
     * doors. Used to raise a destination request only once a passenger has
     * actually boarded.
     *
     * @param listener the callback to invoke on each arrival
     */
    public void addListener(ElevatorListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns this elevator's identifier within the building's bank.
     *
     * @return the elevator id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the maximum number of passengers this cabin can hold.
     *
     * @return the passenger capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the floor the elevator is currently at.
     *
     * @return the current floor
     */
    public int getCurrentFloor() {
        return currentFloor;
    }

    /**
     * Returns the direction the elevator is currently travelling, which
     * scheduling strategies use to decide whether a new request is "on the way".
     *
     * @return UP, DOWN, or {@link Direction#IDLE} when parked
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns the elevator's current activity.
     *
     * @return IDLE, MOVING or STOPPED
     */
    public ElevatorState getState() {
        return state;
    }

    /**
     * Convenience check for whether this elevator is free to be dispatched
     * without interrupting an existing trip.
     *
     * @return {@code true} if the elevator has no pending work
     */
    public boolean isIdle() {
        return state == ElevatorState.IDLE;
    }

    /**
     * Queues a floor for this elevator to stop at, from either a hall call or a
     * destination call. Stops are split into ascending/descending sets so the
     * LOOK algorithm can serve everything in the current direction first. If the
     * elevator is already parked at the requested floor, the doors open
     * immediately instead of queuing anything.
     *
     * @param floor the floor to stop at
     */
    public void addStop(int floor) {
        if (floor == currentFloor && state != ElevatorState.MOVING) {
            openDoorAt(floor);
            return;
        }
        if (floor > currentFloor) {
            upStops.add(floor);
        } else if (floor < currentFloor) {
            downStops.add(floor);
        }
        if (direction == Direction.IDLE) {
            direction = floor > currentFloor ? Direction.UP : Direction.DOWN;
        }
        state = ElevatorState.MOVING;
    }

    /**
     * Advances the elevator by a single floor, implementing one tick of the
     * LOOK/SCAN algorithm: continue in the current direction serving every
     * queued stop, then reverse if the opposite queue still has work, and
     * finally settle into {@link ElevatorState#IDLE} once both queues drain.
     * Call repeatedly (from a simulation loop or a scheduler thread) until the
     * elevator reports no pending requests.
     */
    public void step() {
        if (direction == Direction.UP) {
            if (!upStops.isEmpty()) {
                currentFloor++;
                display.update(id, currentFloor, direction);
                if (upStops.contains(currentFloor)) {
                    upStops.remove(currentFloor);
                    openDoorAt(currentFloor);
                }
            }
            if (upStops.isEmpty()) {
                direction = downStops.isEmpty() ? Direction.IDLE : Direction.DOWN;
            }
        } else if (direction == Direction.DOWN) {
            if (!downStops.isEmpty()) {
                currentFloor--;
                display.update(id, currentFloor, direction);
                if (downStops.contains(currentFloor)) {
                    downStops.remove(currentFloor);
                    openDoorAt(currentFloor);
                }
            }
            if (downStops.isEmpty()) {
                direction = upStops.isEmpty() ? Direction.IDLE : Direction.UP;
            }
        }

        if (upStops.isEmpty() && downStops.isEmpty()) {
            direction = Direction.IDLE;
            state = ElevatorState.IDLE;
        }
    }

    /**
     * Performs the arrival routine at a floor: halt, cycle the doors, notify
     * observers that boarding is now possible, then resume or go idle
     * depending on whether stops remain.
     *
     * @param floor the floor being served
     */
    private void openDoorAt(int floor) {
        state = ElevatorState.STOPPED;
        System.out.printf("Elevator %d stopping at floor %d%n", id, floor);
        door.open();
        door.close();
        // Notify observers (e.g. a boarding passenger) only now that the door
        // has actually opened - this is the earliest a destination request
        // for this stop can legitimately be raised.
        for (ElevatorListener listener : listeners) {
            listener.onDoorOpened(id, floor);
        }
        state = (upStops.isEmpty() && downStops.isEmpty()) ? ElevatorState.IDLE : ElevatorState.MOVING;
    }

    /**
     * Distance heuristic used by scheduling strategies to rank candidate
     * elevators for a request.
     *
     * @param floor the floor being requested
     * @return the absolute number of floors between here and {@code floor}
     */
    public int distanceTo(int floor) {
        return Math.abs(currentFloor - floor);
    }

    /**
     * Indicates whether this elevator still has work queued, used to drive the
     * simulation loop and to skip idle cars during {@code stepAll()}.
     *
     * @return {@code true} if any stop remains in either direction queue
     */
    public boolean hasPendingRequests() {
        return !upStops.isEmpty() || !downStops.isEmpty();
    }
}
