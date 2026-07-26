package com.lowleveldesign.meetingscheduler.service;

import java.time.LocalDateTime;

/**
 * Facade for the meeting room booking system.
 */
public interface BookingService {

    /**
     * Books a room for {@code attendeeCount} people between {@code start} (inclusive) and
     * {@code end} (exclusive), and reports the confirmed details.
     *
     * <p>Implementations choose the smallest room that can seat the request and is free for the
     * whole range, so a small meeting only occupies a large room when nothing closer-fitting is
     * available. Bookings never overlap on the same room.
     *
     * @param attendeeCount the number of people to seat; must be positive
     * @param start         the inclusive start of the desired meeting
     * @param end           the exclusive end of the desired meeting; must be after {@code start}
     * @return the booking ID of the confirmed reservation
     * @throws com.lowleveldesign.meetingscheduler.exception.InvalidBookingRequestException if the
     *         request itself is malformed (bad capacity, end not after start)
     * @throws com.lowleveldesign.meetingscheduler.exception.NoRoomAvailableException if no room
     *         can satisfy the request, either because none is large enough or every large-enough
     *         room is already booked for the requested slot
     */
    String bookRoom(int attendeeCount, LocalDateTime start, LocalDateTime end);

    /**
     * Cancels a previously confirmed booking, releasing its slot so the room can be booked again
     * for that time range.
     *
     * @param bookingId the ID returned by {@link #bookRoom}
     * @return {@code true} if a matching booking was found and cancelled, {@code false} if the ID
     *         is unknown or the booking was already cancelled
     */
    boolean cancelBooking(String bookingId);
}
