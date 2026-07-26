package com.lowleveldesign.elevatorlocus.model;

import com.lowleveldesign.elevatorlocus.exception.InvalidRequestException;

/**
 * An immutable elevator request raised by a single passenger.
 *
 * <p>A request bundles the hall call (the passenger stands on
 * {@code sourceFloor} and presses {@code direction}) together with the
 * destination-floor button they will press once on board. Modelling both in one
 * object keeps the simulator simple while still honouring the rule that the
 * destination is only "known" to the elevator after the passenger boards.
 */
public final class Request {

    private final int requestTime;
    private final String passengerName;
    private final int sourceFloor;
    private final Direction direction;
    private final int destinationFloor;

    /**
     * @param requestTime      minute at which the passenger presses the hall
     *                         button (0 for the first request); must be &ge; 0
     * @param passengerName    non-blank passenger identifier
     * @param sourceFloor      floor the passenger is waiting on
     * @param direction        requested hall direction ({@link Direction#UP} or
     *                         {@link Direction#DOWN})
     * @param destinationFloor floor the passenger wants to reach; must be
     *                         consistent with {@code direction}
     * @param minFloor         lowest valid floor (inclusive)
     * @param maxFloor         highest valid floor (inclusive)
     * @throws InvalidRequestException if any field is out of range or the
     *                                 direction and destination are inconsistent
     */
    public Request(int requestTime, String passengerName, int sourceFloor,
                   Direction direction, int destinationFloor,
                   int minFloor, int maxFloor) {
        if (requestTime < 0) {
            throw new InvalidRequestException("Request time must be >= 0 but was " + requestTime);
        }
        if (passengerName == null || passengerName.trim().isEmpty()) {
            throw new InvalidRequestException("Passenger name must not be blank");
        }
        if (direction != Direction.UP && direction != Direction.DOWN) {
            throw new InvalidRequestException("Request direction must be UP or DOWN but was " + direction);
        }
        validateFloor(sourceFloor, minFloor, maxFloor, "Current floor");
        validateFloor(destinationFloor, minFloor, maxFloor, "Destination floor");
        if (sourceFloor == destinationFloor) {
            throw new InvalidRequestException(
                    "Destination floor must differ from current floor (" + sourceFloor + ")");
        }
        if (direction == Direction.UP && destinationFloor < sourceFloor) {
            throw new InvalidRequestException(
                    "Direction UP but destination " + destinationFloor + " is below source " + sourceFloor);
        }
        if (direction == Direction.DOWN && destinationFloor > sourceFloor) {
            throw new InvalidRequestException(
                    "Direction DOWN but destination " + destinationFloor + " is above source " + sourceFloor);
        }
        this.requestTime = requestTime;
        this.passengerName = passengerName.trim();
        this.sourceFloor = sourceFloor;
        this.direction = direction;
        this.destinationFloor = destinationFloor;
    }

    private static void validateFloor(int floor, int minFloor, int maxFloor, String label) {
        if (floor < minFloor || floor > maxFloor) {
            throw new InvalidRequestException(
                    label + " " + floor + " is outside the valid range [" + minFloor + ", " + maxFloor + "]");
        }
    }

    public int getRequestTime() {
        return requestTime;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public int getSourceFloor() {
        return sourceFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    @Override
    public String toString() {
        return String.format("T%d %s %d %s %d",
                requestTime, passengerName, sourceFloor, direction, destinationFloor);
    }
}
