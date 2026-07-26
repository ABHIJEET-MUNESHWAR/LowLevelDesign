package com.lowleveldesign.meetingscheduler.exception;

/**
 * Thrown when a {@code BookingService} is configured with an unusable room inventory -- currently,
 * a {@code null} or empty room list. Kept distinct from {@link InvalidBookingRequestException}
 * (which describes a bad individual booking request) because this is a setup-time configuration
 * error rather than a per-request failure, and callers may want to handle the two very
 * differently (e.g., fail fast at startup vs. return an HTTP 4xx per request).
 */
public class InvalidRoomConfigurationException extends RuntimeException {

    /**
     * Creates the exception describing the configuration problem.
     *
     * @param message a description of what made the room inventory unusable
     */
    public InvalidRoomConfigurationException(String message) {
        super(message);
    }
}
