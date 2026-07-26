package com.lowleveldesign.meetingscheduler.service;

import java.time.LocalDateTime;

/**
 * Facade for the meeting room booking system.
 */
public interface BookingService {

    /**
     * Books a room for {@code attendeeCount} people between {@code start} (inclusive) and
     * {@code end} (exclusive).
     *
     * @return the booking ID of the confirmed reservation
     * @throws com.lowleveldesign.meetingscheduler.exception.InvalidBookingRequestException if the
     *         request itself is malformed (bad capacity, end not after start)
     * @throws com.lowleveldesign.meetingscheduler.exception.NoRoomAvailableException if no room
     *         can satisfy the request, either because none is large enough or every large-enough
     *         room is already booked for the requested slot
     */
    String bookRoom(int attendeeCount, LocalDateTime start, LocalDateTime end);

    /** Cancels a previously confirmed booking. Returns true if a matching booking was found. */
    boolean cancelBooking(String bookingId);
}
