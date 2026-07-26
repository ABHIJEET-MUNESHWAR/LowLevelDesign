package com.lowleveldesign.elevator.model;

import java.util.Objects;

/**
 * Represents a single request made either from a floor (EXTERNAL/hall call)
 * or from inside the cabin (INTERNAL/destination call).
 */
public class Request {

    private final int floor;
    private final Direction direction;
    private final RequestType type;

    private Request(int floor, Direction direction, RequestType type) {
        this.floor = floor;
        this.direction = direction;
        this.type = type;
    }

    public static Request externalRequest(int floor, Direction direction) {
        if (direction == Direction.IDLE) {
            throw new IllegalArgumentException("Hall request must be UP or DOWN");
        }
        return new Request(floor, direction, RequestType.EXTERNAL);
    }

    public static Request internalRequest(int destinationFloor) {
        return new Request(destinationFloor, Direction.IDLE, RequestType.INTERNAL);
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return floor == request.floor && direction == request.direction && type == request.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floor, direction, type);
    }

    @Override
    public String toString() {
        return type == RequestType.EXTERNAL
                ? String.format("HallCall(floor=%d, dir=%s)", floor, direction)
                : String.format("Destination(floor=%d)", floor);
    }
}
