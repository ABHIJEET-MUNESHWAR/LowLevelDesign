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

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    /** True if this slot and {@code other} share any instant. */
    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot that = (TimeSlot) o;
        return start.equals(that.start) && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "[" + start + " -> " + end + ")";
    }
}
