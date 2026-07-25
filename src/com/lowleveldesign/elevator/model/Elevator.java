package com.lowleveldesign.elevator.model;

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
    }

    public int getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public ElevatorState getState() {
        return state;
    }

    public boolean isIdle() {
        return state == ElevatorState.IDLE;
    }

    /** Adds a stop (from a hall call or a destination call) to the schedule. */
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
     * Advances the elevator by one floor/step. Call repeatedly (e.g. by a
     * scheduler thread or simulation loop) until it returns to IDLE.
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

    private void openDoorAt(int floor) {
        state = ElevatorState.STOPPED;
        System.out.printf("Elevator %d stopping at floor %d%n", id, floor);
        door.open();
        door.close();
        state = (upStops.isEmpty() && downStops.isEmpty()) ? ElevatorState.IDLE : ElevatorState.MOVING;
    }

    /** Distance heuristic used by scheduling strategies to pick the best elevator. */
    public int distanceTo(int floor) {
        return Math.abs(currentFloor - floor);
    }

    public boolean hasPendingRequests() {
        return !upStops.isEmpty() || !downStops.isEmpty();
    }
}
