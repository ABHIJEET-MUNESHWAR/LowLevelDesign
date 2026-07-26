package com.lowleveldesign.meetingscheduler.model;

import java.time.LocalDateTime;
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

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    /**
     * Attempts to reserve this room for {@code slot} on behalf of {@code attendeeCount} people.
     * Returns an empty {@link Optional} if the room is too small or already booked for an
     * overlapping slot; otherwise returns the newly created, immutable {@link Booking}.
     *
     * <p>Thread-safe: the overlap check and the insertion happen atomically while holding this
     * room's lock, so two threads racing for the same slot can never both succeed.
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
            return Optional.of(booking);
        } finally {
            lock.unlock();
        }
    }

    /** Cancels a previously confirmed booking, freeing its slot. Returns true if it existed. */
    public boolean cancel(String bookingId) {
        lock.lock();
        try {
            LocalDateTime toRemove = null;
            for (Map.Entry<LocalDateTime, Booking> entry : bookings.entrySet()) {
                if (entry.getValue().getId().equals(bookingId)) {
                    toRemove = entry.getKey();
                    break;
                }
            }
            if (toRemove == null) {
                return false;
            }
            bookings.remove(toRemove);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /** Must be called while holding {@link #lock}. */
    private boolean hasOverlap(TimeSlot slot) {
        LocalDateTime startKey = slot.getStart();

        Map.Entry<LocalDateTime, Booking> floor = bookings.floorEntry(startKey);
        if (floor != null && floor.getValue().getTimeSlot().overlaps(slot)) {
            return true;
        }

        Map.Entry<LocalDateTime, Booking> ceiling = bookings.ceilingEntry(startKey);
        return ceiling != null && ceiling.getValue().getTimeSlot().overlaps(slot);
    }

    @Override
    public String toString() {
        return "Room{id=" + id + ", name=" + name + ", capacity=" + capacity + '}';
    }
}
