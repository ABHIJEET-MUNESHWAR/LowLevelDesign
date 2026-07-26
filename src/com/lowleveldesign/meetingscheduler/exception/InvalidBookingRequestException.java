package com.lowleveldesign.meetingscheduler.exception;

/** Thrown when a booking request is malformed: bad capacity, or end time not after start time. */
public class InvalidBookingRequestException extends RuntimeException {

    public InvalidBookingRequestException(String message) {
        super(message);
    }
}
