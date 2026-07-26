package com.lowleveldesign.meetingscheduler.service;

import com.lowleveldesign.meetingscheduler.exception.InvalidBookingRequestException;
import com.lowleveldesign.meetingscheduler.exception.NoRoomAvailableException;
import com.lowleveldesign.meetingscheduler.model.Booking;
import com.lowleveldesign.meetingscheduler.model.Room;
import com.lowleveldesign.meetingscheduler.model.TimeSlot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, single-host implementation of {@link BookingService}.
 *
 * <p>Rooms are tried smallest-capacity-first among those large enough for the request
 * (best-fit), so a small meeting only spills into a larger room when every closer-fitting room is
 * already taken for that slot. Concurrency safety is delegated to each {@link Room}, which
 * guards its own booking calendar with its own lock -- this class holds no global lock, so
 * requests for different rooms proceed fully in parallel.
 */
public final class MeetingRoomBookingService implements BookingService {

    /** Rooms sorted ascending by capacity once, at construction time, for best-fit iteration. */
    private final List<Room> roomsByCapacityAscending;

    /** Maps a booking ID back to the room that holds it, for O(1) cancellation lookup. */
    private final Map<String, String> bookingIdToRoomId = new ConcurrentHashMap<>();
    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();

    public MeetingRoomBookingService(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("At least one room is required");
        }
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator.comparingInt(Room::getCapacity));
        this.roomsByCapacityAscending = sorted;
        for (Room room : rooms) {
            roomsById.put(room.getId(), room);
        }
    }

    @Override
    public String bookRoom(int attendeeCount, LocalDateTime start, LocalDateTime end) {
        if (attendeeCount <= 0) {
            throw new InvalidBookingRequestException("Attendee count must be positive");
        }
        // TimeSlot's constructor validates start/end itself.
        TimeSlot slot = new TimeSlot(start, end);

        for (Room room : roomsByCapacityAscending) {
            if (room.getCapacity() < attendeeCount) {
                continue;
            }
            Optional<Booking> booking = room.tryBook(attendeeCount, slot);
            if (booking.isPresent()) {
                Booking confirmed = booking.get();
                bookingIdToRoomId.put(confirmed.getId(), confirmed.getRoomId());
                return confirmed.getId();
            }
            // This room is either too small or booked for an overlapping slot -- try the next
            // best-fit candidate.
        }

        throw new NoRoomAvailableException(
                "No room available for " + attendeeCount + " attendees between " + start + " and " + end);
    }

    @Override
    public boolean cancelBooking(String bookingId) {
        String roomId = bookingIdToRoomId.remove(bookingId);
        if (roomId == null) {
            return false;
        }
        Room room = roomsById.get(roomId);
        return room != null && room.cancel(bookingId);
    }
}
