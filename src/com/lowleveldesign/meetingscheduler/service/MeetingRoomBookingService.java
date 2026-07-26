package com.lowleveldesign.meetingscheduler.service;

import com.lowleveldesign.meetingscheduler.exception.InvalidBookingRequestException;
import com.lowleveldesign.meetingscheduler.exception.InvalidRoomConfigurationException;
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

    /**
     * The capacities of {@link #roomsByCapacityAscending}, in the same order. Kept as a parallel
     * primitive array so the first room large enough for a request can be found with a binary
     * search instead of skipping over the too-small rooms one by one.
     */
    private final int[] capacitiesAscending;

    /** Maps a booking ID back to the room that holds it, for O(1) cancellation lookup. */
    private final Map<String, String> bookingIdToRoomId = new ConcurrentHashMap<>();
    private final Map<String, Room> roomsById = new ConcurrentHashMap<>();

    /**
     * Creates a booking service over a fixed inventory of rooms.
     *
     * <p>The rooms are sorted by capacity once here rather than on every request, so the hot path
     * of {@link #bookRoom} is a plain iteration in best-fit order. The two index maps are populated
     * up front so cancellation never has to scan the whole inventory.
     *
     * @param rooms the rooms this service can allocate; copied defensively, so later changes to the
     *              caller's list do not affect the service
     * @throws InvalidRoomConfigurationException if {@code rooms} is {@code null} or empty, since a
     *                                            booking service with no inventory could never
     *                                            succeed
     */
    public MeetingRoomBookingService(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            throw new InvalidRoomConfigurationException("At least one room is required");
        }
        List<Room> sorted = new ArrayList<>(rooms);
        sorted.sort(Comparator.comparingInt(Room::getCapacity));
        this.roomsByCapacityAscending = sorted;
        this.capacitiesAscending = new int[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            capacitiesAscending[i] = sorted.get(i).getCapacity();
        }
        for (Room room : rooms) {
            roomsById.put(room.getId(), room);
        }
    }

    /**
     * Books the best-fitting available room for the requested capacity and time range, and prints
     * the confirmation.
     *
     * <p>Candidate rooms are tried smallest-capacity-first, so a request only spills into a larger
     * room when every closer-fitting room is already occupied for that slot -- this is what allows
     * a 16-person meeting to use a 20-person room while still preferring a 16-person one.
     *
     * <p>No global lock is taken: each candidate room is probed through its own lock, so requests
     * for different rooms proceed in parallel while requests for the same room are serialized.
     *
     * <p>The first room large enough is found by binary search over the capacity-sorted inventory,
     * so rooms that are too small are never visited; only genuinely eligible rooms are probed.
     *
     * @param attendeeCount the number of people to seat; must be positive
     * @param start         the inclusive start of the desired meeting
     * @param end           the exclusive end of the desired meeting; must be after {@code start}
     * @return the ID of the confirmed booking
     * @throws InvalidBookingRequestException if the attendee count is not positive, or the time
     *                                        range is null or not strictly increasing
     * @throws NoRoomAvailableException       if no room is large enough, or every large-enough room
     *                                        is already booked for an overlapping slot
     */
    @Override
    public String bookRoom(int attendeeCount, LocalDateTime start, LocalDateTime end) {
        if (attendeeCount <= 0) {
            throw new InvalidBookingRequestException("Attendee count must be positive");
        }
        // TimeSlot's constructor validates start/end itself.
        TimeSlot slot = new TimeSlot(start, end);

        for (int i = firstRoomFitting(attendeeCount); i < roomsByCapacityAscending.size(); i++) {
            Room room = roomsByCapacityAscending.get(i);
            Optional<Booking> booking = room.tryBook(attendeeCount, slot);
            if (booking.isPresent()) {
                Booking confirmed = booking.get();
                bookingIdToRoomId.put(confirmed.getId(), confirmed.getRoomId());
                printConfirmation(confirmed);
                return confirmed.getId();
            }
            // This room is booked for an overlapping slot -- try the next best-fit candidate.
        }

        throw new NoRoomAvailableException(
                "No room available for " + attendeeCount + " attendees between " + start + " and " + end);
    }

    /**
     * Finds the index of the smallest room that can seat {@code attendeeCount}.
     *
     * <p>A binary search (lower bound) over the capacity-sorted inventory, so a request for a large
     * meeting does not have to walk past every small room. Returns the inventory size when no room
     * is large enough, which makes the caller's loop body run zero times and fall straight through
     * to the "no room available" outcome.
     *
     * @param attendeeCount the number of people to seat
     * @return the index of the first sufficiently large room, or the room count if there is none
     */
    private int firstRoomFitting(int attendeeCount) {
        int low = 0;
        int high = capacitiesAscending.length;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (capacitiesAscending[mid] < attendeeCount) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
    }

    /**
     * Cancels a confirmed booking and frees its slot for future requests.
     *
     * <p>Uses the booking-ID index to jump straight to the owning room instead of scanning every
     * room's calendar. The index entry is removed first, so a repeated cancellation of the same ID
     * returns {@code false} rather than doing the work twice.
     *
     * @param bookingId the ID returned by {@link #bookRoom}
     * @return {@code true} if a matching booking was found and cancelled, {@code false} if the ID
     *         is unknown or was already cancelled
     */
    @Override
    public boolean cancelBooking(String bookingId) {
        String roomId = bookingIdToRoomId.remove(bookingId);
        if (roomId == null) {
            return false;
        }
        Room room = roomsById.get(roomId);
        return room != null && room.cancel(bookingId);
    }

    /**
     * Prints the details of a confirmed booking: room name, room capacity, attendee count, and the
     * start and end time.
     *
     * <p>Two deliberate details. It is called after the room's lock has already been released, so
     * console I/O never happens while holding a lock. And the entire line is assembled into a
     * single string before being written, so confirmations printed by concurrent threads cannot
     * interleave mid-line.
     *
     * @param booking the confirmed booking whose details should be reported
     */
    private void printConfirmation(Booking booking) {
        String message = "Booking confirmed"
                + " | Room: " + booking.getRoomName()
                + " | Capacity: " + booking.getRoomCapacity()
                + " | Attendees: " + booking.getAttendeeCount()
                + " | Start: " + booking.getTimeSlot().getStart()
                + " | End: " + booking.getTimeSlot().getEnd()
                + " | Booking Id: " + booking.getId();
        System.out.println(message);
    }
}
