package com.lowleveldesign.meetingscheduler.exception;

/**
 * Thrown when no room can satisfy a booking request -- either no room is large enough for the
 * requested capacity, or every room that is large enough is already booked for the requested
 * time slot.
 */
public class NoRoomAvailableException extends RuntimeException {

    /**
     * Creates the exception describing the request that could not be satisfied.
     *
     * @param message a description including the requested attendee count and time range, so the
     *                caller can report or retry with different parameters
     */
    public NoRoomAvailableException(String message) {
        super(message);
    }
}
