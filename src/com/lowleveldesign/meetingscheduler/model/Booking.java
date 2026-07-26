package com.lowleveldesign.meetingscheduler.model;

import java.time.LocalDateTime;

/**
 * An immutable receipt for a confirmed reservation. Returned to the caller as proof that a room
 * was reserved for a given slot; carries the booking ID that identifies it for later lookup or
 * cancellation.
 */
public final class Booking {

    private final String id;
    private final String roomId;
    private final String roomName;
    private final int roomCapacity;
    private final int attendeeCount;
    private final TimeSlot timeSlot;
    private final LocalDateTime bookedAt;

    /**
     * Creates a confirmed booking receipt.
     *
     * <p>Called only by {@link Room#tryBook} while the room's lock is held and the slot has been
     * proven free, so an instance of this class always represents a genuinely reserved slot. The
     * room's name and capacity are copied in (rather than holding a {@code Room} reference) so the
     * receipt is self-describing and can be logged or printed without touching shared room state.
     *
     * @param id            the unique booking identifier returned to the caller
     * @param roomId        the identifier of the reserved room
     * @param roomName      the display name of the reserved room
     * @param roomCapacity  the maximum occupancy of the reserved room
     * @param attendeeCount the number of people the booking was made for
     * @param timeSlot      the reserved half-open time range
     */
    public Booking(String id, String roomId, String roomName, int roomCapacity, int attendeeCount,
                   TimeSlot timeSlot) {
        this.id = id;
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomCapacity = roomCapacity;
        this.attendeeCount = attendeeCount;
        this.timeSlot = timeSlot;
        this.bookedAt = LocalDateTime.now();
    }

    /**
     * Returns the unique booking identifier.
     *
     * @return the ID handed back to the caller, used to look up or cancel this booking later
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the identifier of the reserved room.
     *
     * @return the room ID, used by the service to route a cancellation to the owning room
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Returns the display name of the reserved room.
     *
     * @return the human-readable room name shown in the booking confirmation
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Returns the maximum occupancy of the reserved room.
     *
     * <p>Reported alongside the attendee count so it is visible when a smaller meeting was placed
     * in a larger room (for example 16 attendees in a 20-person room).
     *
     * @return the capacity of the reserved room
     */
    public int getRoomCapacity() {
        return roomCapacity;
    }

    /**
     * Returns how many people this booking was made for.
     *
     * @return the requested attendee count, always at most {@link #getRoomCapacity()}
     */
    public int getAttendeeCount() {
        return attendeeCount;
    }

    /**
     * Returns the reserved time range.
     *
     * @return the half-open {@code [start, end)} slot held by this booking
     */
    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    /**
     * Returns when this booking was created.
     *
     * <p>This is the wall-clock time the reservation was made, not the time of the meeting itself;
     * it is useful for auditing and for ordering competing bookings after the fact.
     *
     * @return the creation timestamp of this receipt
     */
    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    /**
     * Renders the booking for logs and diagnostics.
     *
     * @return a summary containing the ID, room name, room capacity, attendee count and slot
     */
    @Override
    public String toString() {
        return "Booking{id=" + id + ", room=" + roomName + ", capacity=" + roomCapacity
                + ", attendees=" + attendeeCount + ", slot=" + timeSlot + '}';
    }
}
