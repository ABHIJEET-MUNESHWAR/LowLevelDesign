package com.lowleveldesign.meetingscheduler.model;

import com.lowleveldesign.meetingscheduler.exception.InvalidBookingRequestException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable, half-open time range [start, end).
 *
 * <p>Half-open semantics mean a meeting ending at 10:00 does not overlap one starting at 10:00,
 * which matches how people actually book back-to-back meetings.
 */
public final class TimeSlot {

    private final LocalDateTime start;
    private final LocalDateTime end;

    /**
     * Creates a validated, immutable time range.
     *
     * <p>Validation lives in the constructor so an invalid slot can never exist anywhere in the
     * system -- every downstream method can assume {@code end > start} without re-checking.
     *
     * @param start the inclusive start of the range
     * @param end   the exclusive end of the range
     * @throws InvalidBookingRequestException if either bound is {@code null}, or if {@code end} is
     *                                        not strictly after {@code start} (which would make the
     *                                        meeting zero-length or backwards)
     */
    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new InvalidBookingRequestException("Start and end time must be provided");
        }
        if (!end.isAfter(start)) {
            throw new InvalidBookingRequestException("End time must be strictly after start time");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the inclusive start of this range.
     *
     * @return the instant at which the meeting begins
     */
    public LocalDateTime getStart() {
        return start;
    }

    /**
     * Returns the exclusive end of this range.
     *
     * @return the instant at which the meeting ends; the room is free again from this instant on
     */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * Reports whether this slot and {@code other} share any instant.
     *
     * <p>This is the single source of truth for "do two meetings conflict" -- {@code Room} relies on
     * it to enforce the no-double-booking invariant. Because both ranges are half-open, a slot
     * ending exactly when another begins does <em>not</em> overlap, so back-to-back meetings are
     * allowed.
     *
     * @param other the slot to compare against
     * @return {@code true} if the two ranges intersect, {@code false} if they are disjoint or
     *         merely adjacent
     */
    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    /**
     * Compares two slots by value: two slots are equal when both bounds match.
     *
     * @param o the object to compare against
     * @return {@code true} if {@code o} is a {@code TimeSlot} with the same start and end
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot that = (TimeSlot) o;
        return start.equals(that.start) && end.equals(that.end);
    }

    /**
     * Returns a hash consistent with {@link #equals(Object)}, so slots can safely be used as keys
     * in hash-based collections.
     *
     * @return a hash derived from both bounds
     */
    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    /**
     * Renders the range in half-open notation for logs and diagnostics.
     *
     * @return a string of the form {@code [start -> end)}
     */
    @Override
    public String toString() {
        return "[" + start + " -> " + end + ")";
    }
}
