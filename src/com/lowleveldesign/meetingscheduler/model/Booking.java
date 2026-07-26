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

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getRoomCapacity() {
        return roomCapacity;
    }

    public int getAttendeeCount() {
        return attendeeCount;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    @Override
    public String toString() {
        return "Booking{id=" + id + ", room=" + roomName + ", capacity=" + roomCapacity
                + ", attendees=" + attendeeCount + ", slot=" + timeSlot + '}';
    }
}
