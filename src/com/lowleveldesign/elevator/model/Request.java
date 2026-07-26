package com.lowleveldesign.elevator.model;

import com.lowleveldesign.elevator.exception.InvalidRequestException;

import java.util.Objects;

/**
 * Represents a single request made either from a floor (EXTERNAL/hall call)
 * or from inside the cabin (INTERNAL/destination call).
 */
public class Request {

    private final int floor;
    private final Direction direction;
    private final RequestType type;

    /**
     * Private to force construction through the named factory methods, which
     * guarantee that only valid floor/direction/type combinations exist.
     *
     * @param floor     the floor this request refers to
     * @param direction the travel direction for a hall call, or {@link Direction#IDLE} for a destination call
     * @param type      whether this is an EXTERNAL (hall) or INTERNAL (destination) request
     */
    private Request(int floor, Direction direction, RequestType type) {
        this.floor = floor;
        this.direction = direction;
        this.type = type;
    }

    /**
     * Creates a hall (external) call — a passenger pressing UP or DOWN on a
     * floor, requesting to be picked up.
     *
     * @param floor     the floor the passenger is waiting on
     * @param direction the direction the passenger wants to travel; must be UP or DOWN
     * @return a new EXTERNAL request
     * @throws InvalidRequestException if {@code direction} is {@link Direction#IDLE},
     *                                 since a hall call must express an intended direction
     */
    public static Request externalRequest(int floor, Direction direction) {
        if (direction == Direction.IDLE) {
            throw new InvalidRequestException("A hall request must specify direction UP or DOWN, but was IDLE");
        }
        return new Request(floor, direction, RequestType.EXTERNAL);
    }

    /**
     * Creates a destination (internal) call — a passenger already inside the
     * cabin pressing a floor button. Such requests carry no direction, because
     * the direction is implied by the elevator's current position.
     *
     * @param destinationFloor the floor the passenger wants to travel to
     * @return a new INTERNAL request
     */
    public static Request internalRequest(int destinationFloor) {
        return new Request(destinationFloor, Direction.IDLE, RequestType.INTERNAL);
    }

    /**
     * Returns the floor this request refers to — the pickup floor for a hall
     * call, or the drop-off floor for a destination call.
     *
     * @return the requested floor
     */
    public int getFloor() {
        return floor;
    }

    /**
     * Returns the direction the passenger intends to travel.
     *
     * @return UP or DOWN for a hall call; {@link Direction#IDLE} for a destination call
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns whether this request originated outside the cabin (a hall call)
     * or inside it (a destination call).
     *
     * @return the request type
     */
    public RequestType getType() {
        return type;
    }

    /**
     * Two requests are equal when their floor, direction and type all match,
     * so duplicate button presses can be de-duplicated in a {@code Set}.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is an identical request
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return floor == request.floor && direction == request.direction && type == request.type;
    }

    /**
     * Hash consistent with {@link #equals(Object)}, so requests behave
     * correctly as keys in hash-based collections.
     *
     * @return a hash derived from floor, direction and type
     */
    @Override
    public int hashCode() {
        return Objects.hash(floor, direction, type);
    }

    /**
     * Human-readable form used in dispatch logs, rendered differently for hall
     * calls and destination calls so the two are easy to distinguish.
     *
     * @return e.g. {@code HallCall(floor=3, dir=UP)} or {@code Destination(floor=7)}
     */
    @Override
    public String toString() {
        return type == RequestType.EXTERNAL
                ? String.format("HallCall(floor=%d, dir=%s)", floor, direction)
                : String.format("Destination(floor=%d)", floor);
    }
}
