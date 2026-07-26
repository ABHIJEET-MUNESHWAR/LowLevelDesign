package com.lowleveldesign.meetingscheduler.exception;

/** Thrown when a booking request is malformed: bad capacity, or end time not after start time. */
public class InvalidBookingRequestException extends RuntimeException {

    /**
     * Creates the exception describing why the request was rejected.
     *
     * @param message a description of the specific validation that failed, such as a non-positive
     *                attendee count or an end time that is not after the start time
     */
    public InvalidBookingRequestException(String message) {
        super(message);
    }
}
