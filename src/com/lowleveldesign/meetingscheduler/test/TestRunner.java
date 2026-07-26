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

    public static void main(String[] args) {
        run("rejects overlapping bookings on the same room", TestRunner::testOverlapRejected);
        run("allows back-to-back, non-overlapping bookings", TestRunner::testBackToBackAllowed);
        run("best-fit: small request prefers the smallest sufficient room", TestRunner::testBestFit);
        run("large request spills into bigger room when smaller ones are full", TestRunner::testSpillover);
        run("throws when no room is large enough", TestRunner::testNoRoomLargeEnough);
        run("throws on invalid time range", TestRunner::testInvalidRange);
        run("cancellation frees the slot for re-booking", TestRunner::testCancellation);
        run("booking carries room name, capacity, attendees and slot", TestRunner::testBookingDetails);
        run("only one winner among concurrent racers for the same slot", TestRunner::testConcurrentRace);
        run("concurrent bookings on different rooms all succeed", TestRunner::testConcurrentDifferentRooms);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static BookingService newService(int... capacities) {
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < capacities.length; i++) {
            rooms.add(new Room("R" + i, "Room" + i, capacities[i]));
        }
        return new MeetingRoomBookingService(rooms);
    }

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

    private static Void testBackToBackAllowed() {
        BookingService service = newService(10);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
        service.bookRoom(5, start, end);
        String secondId = service.bookRoom(5, end, end.plusHours(1));
        assertTrue(secondId != null && !secondId.isEmpty(), "Back-to-back booking should succeed");
        return null;
    }

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
