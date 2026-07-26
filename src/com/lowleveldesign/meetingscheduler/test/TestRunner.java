package com.lowleveldesign.meetingscheduler.test;

import com.lowleveldesign.meetingscheduler.exception.InvalidBookingRequestException;
import com.lowleveldesign.meetingscheduler.exception.NoRoomAvailableException;
import com.lowleveldesign.meetingscheduler.model.Booking;
import com.lowleveldesign.meetingscheduler.model.Room;
import com.lowleveldesign.meetingscheduler.model.TimeSlot;
import com.lowleveldesign.meetingscheduler.service.BookingService;
import com.lowleveldesign.meetingscheduler.service.MeetingRoomBookingService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free correctness and concurrency test suite (no JUnit required). Run via
 * {@code java com.lowleveldesign.meetingscheduler.test.TestRunner}; exits non-zero on failure.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Runs every test in the suite and prints a pass/fail line per test plus a final tally.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        run("rejects overlapping bookings on the same room", TestRunner::testOverlapRejected);
        run("allows back-to-back, non-overlapping bookings", TestRunner::testBackToBackAllowed);
        run("best-fit: small request prefers the smallest sufficient room", TestRunner::testBestFit);
        run("large request spills into bigger room when smaller ones are full", TestRunner::testSpillover);
        run("throws when no room is large enough", TestRunner::testNoRoomLargeEnough);
        run("throws on invalid time range", TestRunner::testInvalidRange);
        run("cancellation frees the slot for re-booking", TestRunner::testCancellation);
        run("booking carries room name, capacity, attendees and slot", TestRunner::testBookingDetails);
        run("best-fit boundary: exact-capacity and off-by-one requests", TestRunner::testCapacityBoundary);
        run("only one winner among concurrent racers for the same slot", TestRunner::testConcurrentRace);
        run("concurrent bookings on different rooms all succeed", TestRunner::testConcurrentDifferentRooms);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Executes a single test, records the outcome, and prints its result line.
     *
     * <p>Catches {@link Throwable} rather than {@link Exception} so an {@link AssertionError} from
     * a failed assertion is reported as a normal test failure instead of aborting the whole run.
     *
     * @param name the human-readable test description to print
     * @param test the test body; returning normally means pass, throwing means fail
     */
    private static void run(String name, Callable<Void> test) {
        try {
            test.call();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " -- " + t.getMessage());
        }
    }

    /**
     * Fails the current test if {@code condition} is false.
     *
     * @param condition the condition that must hold
     * @param message   the failure description reported when it does not
     */
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Fails the current test if {@code actual} does not equal {@code expected}, reporting both
     * values so the failure line is self-explanatory.
     *
     * @param expected the value the test requires
     * @param actual   the value produced by the code under test
     * @param message  the failure description
     */
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    /**
     * Builds a booking service over rooms with the given capacities, named {@code Room0..RoomN} in
     * the order supplied.
     *
     * @param capacities the capacity of each room to create
     * @return a ready-to-use service for a single test
     */
    private static BookingService newService(int... capacities) {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < capacities.length; i++) {
            rooms.add(new Room("R" + i, "Room" + i, capacities[i]));
        }
        return new MeetingRoomBookingService(rooms);
    }

    /**
     * Verifies the core invariant: once a room is booked, a second request whose range intersects
     * the first is rejected rather than double-booked.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testOverlapRejected() {
        BookingService service = newService(10);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        service.bookRoom(5, start, end);
        try {
            service.bookRoom(5, start.plusMinutes(30), end.plusMinutes(30));
            throw new AssertionError("Expected NoRoomAvailableException for overlapping slot");
        } catch (NoRoomAvailableException expected) {
            // expected
        }
        return null;
    }

    /**
     * Verifies that half-open ranges are honoured: a meeting starting exactly when another ends
     * is not treated as an overlap, so back-to-back bookings on one room both succeed.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testBackToBackAllowed() {
        BookingService service = newService(10);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        service.bookRoom(5, start, end);
        String secondId = service.bookRoom(5, end, end.plusHours(1));
        assertTrue(secondId != null && !secondId.isEmpty(), "Back-to-back booking should succeed");
        return null;
    }

    /**
     * Verifies best-fit ordering. Three identical overlapping requests must consume the rooms from
     * smallest to largest, proving the smallest sufficient room is always preferred; a fourth
     * request then fails because the inventory is exhausted for that slot.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testBestFit() {
        BookingService service = newService(6, 12, 20);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        // 4 attendees should fit and take the 6-capacity room, leaving 12 and 20 free.
        service.bookRoom(4, start, end);
        // A second, overlapping request for 4 attendees must now spill to the 12-capacity room
        // (proving the 6-capacity room, not the 12, was chosen first).
        service.bookRoom(4, start, end);
        // A third overlapping request for 4 must spill to the 20-capacity room.
        String thirdId = service.bookRoom(4, start, end);
        assertTrue(thirdId != null, "Third overlapping booking should still find the largest room");
        // A fourth, identical request should now fail -- all three rooms are occupied.
        try {
            service.bookRoom(4, start, end);
            throw new AssertionError("Expected rejection once all rooms are occupied for this slot");
        } catch (NoRoomAvailableException expected) {
            // expected
        }
        return null;
    }

    /**
     * Verifies the requirement that a group may occupy a larger room than it strictly needs: with
     * the smaller rooms taken, 16 attendees are successfully placed in the 20-person room.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testSpillover() {
        BookingService service = newService(6, 12, 20);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        service.bookRoom(4, start, end);  // takes the 6-room
        service.bookRoom(10, start, end); // takes the 12-room
        // 16 attendees don't fit in 6 or 12 anyway, but this also proves overflow into 20 works.
        String id = service.bookRoom(16, start, end);
        assertTrue(id != null, "16 attendees should be able to use the 20-capacity room");
        return null;
    }

    /**
     * Verifies that a request exceeding every room's capacity is rejected even when the whole
     * inventory is completely free -- capacity, not availability, is the limiting factor here.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testNoRoomLargeEnough() {
        BookingService service = newService(6, 12);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        try {
            service.bookRoom(50, start, end);
            throw new AssertionError("Expected NoRoomAvailableException: no room is large enough");
        } catch (NoRoomAvailableException expected) {
            // expected
        }
        return null;
    }

    /**
     * Verifies that a malformed range (end before start) is rejected as an invalid request rather
     * than reported as a lack of availability -- the two failure modes stay distinct.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testInvalidRange() {
        BookingService service = newService(6);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 9, 0);
        try {
            service.bookRoom(4, start, end);
            throw new AssertionError("Expected InvalidBookingRequestException for end before start");
        } catch (InvalidBookingRequestException expected) {
            // expected
        }
        return null;
    }

    /**
     * Verifies that cancelling a booking genuinely releases its slot (the same range can then be
     * booked again), and that cancelling an unknown ID reports failure instead of throwing.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testCancellation() {
        BookingService service = newService(6);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        String id = service.bookRoom(4, start, end);
        assertTrue(service.cancelBooking(id), "Cancellation of an existing booking should succeed");
        String rebookedId = service.bookRoom(4, start, end);
        assertTrue(rebookedId != null, "Slot should be re-bookable after cancellation");
        assertTrue(!service.cancelBooking("does-not-exist"), "Cancelling an unknown ID should return false");
        return null;
    }

    /**
     * The confirmation printed on a successful booking is built entirely from the {@code Booking}
     * receipt, so this asserts the receipt actually carries every field that gets printed: room
     * name, room capacity, attendee count, and the start/end of the slot.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testBookingDetails() {
        Room room = new Room("R1", "Falcon", 20);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        Booking booking = room.tryBook(16, new TimeSlot(start, end))
                .orElseThrow(() -> new AssertionError("Booking should have succeeded"));

        assertEquals("Falcon", booking.getRoomName(), "Room name should be on the booking");
        assertEquals(20, booking.getRoomCapacity(), "Room capacity should be on the booking");
        assertEquals(16, booking.getAttendeeCount(), "Attendee count should be on the booking");
        assertEquals(start, booking.getTimeSlot().getStart(), "Start time should be on the booking");
        assertEquals(end, booking.getTimeSlot().getEnd(), "End time should be on the booking");
        return null;
    }

    /**
     * Exercises the binary search that locates the first sufficiently large room, including its
     * boundaries: a request equal to the smallest capacity, a request one over it, a request equal
     * to the largest capacity, and a request one over the largest (which must find nothing).
     *
     * <p>Duplicate capacities are included because a lower-bound search must land on the
     * <em>first</em> room of an equal-capacity run, not an arbitrary one.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     */
    private static Void testCapacityBoundary() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);

        // Exactly the smallest capacity must fit the smallest room.
        assertTrue(newService(4, 8, 8, 15).bookRoom(4, start, end) != null,
                "A request equal to the smallest capacity should fit the smallest room");
        // One over the smallest must skip it and land on the 8-capacity room.
        assertTrue(newService(4, 8, 8, 15).bookRoom(5, start, end) != null,
                "A request just over the smallest capacity should use the next size up");
        // Exactly the largest capacity must still be bookable.
        assertTrue(newService(4, 8, 8, 15).bookRoom(15, start, end) != null,
                "A request equal to the largest capacity should fit the largest room");
        // One over the largest capacity must find nothing at all.
        try {
            newService(4, 8, 8, 15).bookRoom(16, start, end);
            throw new AssertionError("A request over the largest capacity should be rejected");
        } catch (NoRoomAvailableException expected) {
            // expected
        }

        // With duplicate capacities, two 8-person requests should occupy both 8-capacity rooms
        // before spilling into the 15, proving the search lands on the first of the equal run.
        BookingService service = newService(4, 8, 8, 15);
        service.bookRoom(8, start, end);
        service.bookRoom(8, start, end);
        service.bookRoom(8, start, end); // spills into the 15
        try {
            service.bookRoom(8, start, end);
            throw new AssertionError("Expected rejection once both 8-rooms and the 15-room are taken");
        } catch (NoRoomAvailableException expected) {
            // expected
        }
        return null;
    }

    /**
     * Verifies the critical concurrency guarantee: when many threads request the same room and the
     * same slot simultaneously, exactly one succeeds and the rest are cleanly rejected. A result
     * other than one would mean the overlap-check-then-insert sequence is not atomic.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     * @throws Exception if a worker thread fails or the pool is interrupted
     */
    private static Void testConcurrentRace() throws Exception {
        BookingService service = newService(10);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);

        int threadCount = 30;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    service.bookRoom(4, start, end);
                    successCount.incrementAndGet();
                } catch (NoRoomAvailableException ignored) {
                    // expected for losers of the race
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Exactly one concurrent racer should win the same slot");
        return null;
    }

    /**
     * Verifies that per-room locking does not serialize unrelated work: with as many equally-sized
     * rooms as threads, every concurrent request for the same slot succeeds, each landing in its
     * own room.
     *
     * @return always {@code null}; the method signature satisfies {@link Callable}
     * @throws Exception if a worker thread fails or the pool is interrupted
     */
    private static Void testConcurrentDifferentRooms() throws Exception {
        int roomCount = 8;
        int[] capacities = new int[roomCount];
        Arrays.fill(capacities, 10);
        BookingService service = newService(capacities);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);

        ExecutorService pool = Executors.newFixedThreadPool(roomCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < roomCount; i++) {
            tasks.add(() -> {
                service.bookRoom(4, start, end);
                successCount.incrementAndGet();
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(roomCount, successCount.get(), "Each thread should get its own room, all succeeding");
        return null;
    }
}
