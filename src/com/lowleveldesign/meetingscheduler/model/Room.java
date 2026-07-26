package com.lowleveldesign.meetingscheduler.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A meeting room. Each room owns its own lock and its own calendar of confirmed bookings, so
 * concurrent requests for different rooms never contend with each other, while concurrent
 * requests for the same room are safely serialized.
 *
 * <p>Bookings are kept in a {@link TreeMap} keyed by start time. Because bookings already held by
 * a room are guaranteed mutually non-overlapping, a new candidate slot can only possibly conflict
 * with the entry immediately at-or-before its start (the floor) or the entry immediately
 * at-or-after its start (the ceiling) -- so availability can be checked in O(log n) instead of
 * scanning every existing booking.
 */
public final class Room {

    private final String id;
    private final String name;
    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();
    /** Guarded exclusively by {@link #lock}. Keyed by booking start time. */
    private final TreeMap<LocalDateTime, Booking> bookings = new TreeMap<>();
    /**
     * Secondary index from booking ID to that booking's start time, so cancellation can locate an
     * entry in the calendar without scanning it. Guarded exclusively by {@link #lock} and always
     * updated in the same critical section as {@link #bookings}, so the two can never disagree.
     */
    private final Map<String, LocalDateTime> bookingIdToStart = new HashMap<>();

    /**
     * Creates a room with a fixed identity and capacity, and an empty booking calendar.
     *
     * @param id       the unique identifier of this room
     * @param name     the human-readable display name
     * @param capacity the maximum number of attendees this room can seat
     */
    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    /**
     * Returns this room's unique identifier.
     *
     * @return the room ID, used by the service to index rooms for cancellation lookups
     */
    public String getId() {
        return id;
    }

    /**
     * Returns this room's display name.
     *
     * @return the human-readable name shown in booking confirmations
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the maximum number of attendees this room can seat.
     *
     * <p>Read by the service to order and filter candidate rooms for best-fit selection. It is safe
     * to read without the lock because capacity is immutable for the life of the room.
     *
     * @return this room's capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Attempts to reserve this room for {@code slot} on behalf of {@code attendeeCount} people.
     *
     * <p>This is the atomic booking primitive of the whole system. The capacity check, the overlap
     * check, and the insertion into the calendar all happen while holding this room's lock, so the
     * classic check-then-act race is eliminated: if two threads request the same slot, exactly one
     * receives a booking and the other receives an empty result.
     *
     * <p>A failure to book is returned as an empty {@link Optional} rather than an exception,
     * because at the room level "already taken" is an ordinary outcome -- only the service, after
     * exhausting every candidate room, escalates it to an exception.
     *
     * @param attendeeCount the number of people to seat
     * @param slot          the desired half-open time range
     * @return the confirmed {@link Booking}, or {@link Optional#empty()} if this room is too small
     *         or is already booked for an overlapping slot
     */
    public Optional<Booking> tryBook(int attendeeCount, TimeSlot slot) {
        if (attendeeCount > capacity) {
            return Optional.empty();
        }
        lock.lock();
        try {
            if (hasOverlap(slot)) {
                return Optional.empty();
            }
            Booking booking = new Booking(UUID.randomUUID().toString(), id, name, capacity,
                    attendeeCount, slot);
            bookings.put(slot.getStart(), booking);
            bookingIdToStart.put(booking.getId(), slot.getStart());
            return Optional.of(booking);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancels a previously confirmed booking, freeing its slot for future requests.
     *
     * <p>Performed under this room's lock so a cancellation can never interleave with a concurrent
     * {@link #tryBook} and corrupt the calendar. The booking-ID index makes the lookup O(1) and the
     * removal O(log n), so cancellation never scans the calendar.
     *
     * @param bookingId the ID of the booking to remove
     * @return {@code true} if a matching booking existed and was removed, {@code false} otherwise
     */
    public boolean cancel(String bookingId) {
        lock.lock();
        try {
            LocalDateTime start = bookingIdToStart.remove(bookingId);
            if (start == null) {
                return false;
            }
            bookings.remove(start);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reports whether {@code slot} collides with any booking already held by this room.
     *
     * <p>Because existing bookings are mutually non-overlapping by construction, only two
     * candidates can possibly conflict: the booking starting at-or-before {@code slot} (the floor
     * entry) and the booking starting at-or-after it (the ceiling entry). Checking just those two
     * makes this O(log n) instead of a full O(n) scan of the calendar.
     *
     * <p>Must be called while holding {@link #lock}.
     *
     * @param slot the candidate time range
     * @return {@code true} if the slot conflicts with an existing booking, {@code false} if it is
     *         free
     */
    private boolean hasOverlap(TimeSlot slot) {
        LocalDateTime startKey = slot.getStart();

        Map.Entry<LocalDateTime, Booking> floor = bookings.floorEntry(startKey);
        if (floor != null && floor.getValue().getTimeSlot().overlaps(slot)) {
            return true;
        }

        Map.Entry<LocalDateTime, Booking> ceiling = bookings.ceilingEntry(startKey);
        return ceiling != null && ceiling.getValue().getTimeSlot().overlaps(slot);
    }

    /**
     * Renders the room's identity for logs and diagnostics.
     *
     * <p>Deliberately excludes the booking calendar, since reading it safely would require taking
     * the lock -- and a {@code toString} that blocks would be a hazard in debuggers and log
     * statements.
     *
     * @return a summary containing the room ID, name and capacity
     */
    @Override
    public String toString() {
        return "Room{id=" + id + ", name=" + name + ", capacity=" + capacity + '}';
    }
}
