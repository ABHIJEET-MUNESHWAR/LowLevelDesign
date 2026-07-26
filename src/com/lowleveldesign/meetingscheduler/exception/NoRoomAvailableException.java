package com.lowleveldesign.meetingscheduler.exception;

/**
 * Thrown when no room can satisfy a booking request -- either no room is large enough for the
 * requested capacity, or every room that is large enough is already booked for the requested
 * time slot.
 */
public class NoRoomAvailableException extends RuntimeException {

    public NoRoomAvailableException(String message) {
        super(message);
    }
}
