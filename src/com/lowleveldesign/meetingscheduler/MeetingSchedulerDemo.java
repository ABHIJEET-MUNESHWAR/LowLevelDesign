package com.lowleveldesign.meetingscheduler;

import com.lowleveldesign.meetingscheduler.exception.NoRoomAvailableException;
import com.lowleveldesign.meetingscheduler.model.Room;
import com.lowleveldesign.meetingscheduler.service.BookingService;
import com.lowleveldesign.meetingscheduler.service.MeetingRoomBookingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Runnable worked example: best-fit selection, overlap rejection, and a concurrent race. */
public final class MeetingSchedulerDemo {

    public static void main(String[] args) throws Exception {
        List<Room> rooms = Arrays.asList(
                new Room("R1", "Falcon (Small)", 6),
                new Room("R2", "Eagle (Medium)", 12),
                new Room("R3", "Hawk (Large)", 20));
        BookingService bookingService = new MeetingRoomBookingService(rooms);

        LocalDateTime start = LocalDateTime.of(2026, 1, 5, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 5, 11, 0);

        // 1. Best-fit: 4 attendees fits the small room, so it should be chosen over medium/large.
        String booking1 = bookingService.bookRoom(4, start, end);
        System.out.println("Booked small room for 4 attendees -> booking id: " + booking1);

        // 2. Same slot, small room is now taken. 5 attendees should spill into the medium room.
        String booking2 = bookingService.bookRoom(5, start, end);
        System.out.println("Booked medium room for 5 attendees -> booking id: " + booking2);

        // 3. Overlapping slot, capacity 16: small (taken/too small) and medium (taken) are out,
        //    so this spills all the way to the 20-person large room -- exactly the "16 members can
        //    use a 20 member room" requirement.
        String booking3 = bookingService.bookRoom(16, start.plusMinutes(30), end.plusMinutes(30));
        System.out.println("Booked large room for 16 attendees -> booking id: " + booking3);

        // 4. A non-overlapping slot on the small room should succeed even though the room was
        //    "booked" earlier -- proves overlap detection is slot-aware, not room-wide.
        String booking4 = bookingService.bookRoom(3, end, end.plusHours(1));
        System.out.println("Booked small room for next hour -> booking id: " + booking4);

        // 5. Every room is now full for [start, end.plusMinutes(30)) at this capacity -> rejected.
        try {
            bookingService.bookRoom(25, start, end);
            System.out.println("ERROR: expected NoRoomAvailableException");
        } catch (NoRoomAvailableException e) {
            System.out.println("Correctly rejected: " + e.getMessage());
        }

        // 6. Cancel a booking and confirm the slot is bookable again.
        boolean cancelled = bookingService.cancelBooking(booking2);
        System.out.println("Cancelled booking2: " + cancelled);
        String booking5 = bookingService.bookRoom(5, start, end);
        System.out.println("Re-booked medium room after cancellation -> booking id: " + booking5);

        concurrentRaceDemo();
    }

    /**
     * Fires many concurrent requests for the *same* room and the *same* time slot. Only one
     * should ever win; every other thread must receive a clean rejection with no corruption of
     * the room's booking calendar.
     */
    private static void concurrentRaceDemo() throws Exception {
        System.out.println("\n--- Concurrent race for one room/slot ---");
        List<Room> rooms = Arrays.asList(new Room("R1", "Contested Room", 10));
        BookingService bookingService = new MeetingRoomBookingService(rooms);

        LocalDateTime start = LocalDateTime.of(2026, 2, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 10, 0);

        int threadCount = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();

        List<Callable<Void>> tasks = new java.util.ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    bookingService.bookRoom(4, start, end);
                    successCount.incrementAndGet();
                } catch (NoRoomAvailableException e) {
                    rejectedCount.incrementAndGet();
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

        System.out.println("Successful bookings: " + successCount.get() + " (expected 1)");
        System.out.println("Rejected bookings:   " + rejectedCount.get() + " (expected " + (threadCount - 1) + ")");
    }
}
