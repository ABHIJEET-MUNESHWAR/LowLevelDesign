/**
 * Meeting Room Booking System -- low level design.
 *
 * <h2>Design in one line</h2>
 * Each {@code Room} owns its own lock and its own sorted set of bookings, so booking is
 * "best-fit room selection + per-room mutual exclusion" -- rooms are booked independently and
 * in parallel, while a single room's booking list is always mutated atomically.
 *
 * <h2>Package map</h2>
 * <ul>
 *   <li>{@code model}     -- value objects: {@code TimeSlot} (a half-open time range), {@code Booking}
 *                            (an immutable receipt), and {@code Room} (capacity + its own lock and
 *                            booking calendar).</li>
 *   <li>{@code exception} -- {@code NoRoomAvailableException}, {@code InvalidBookingRequestException},
 *                            and {@code InvalidRoomConfigurationException}, all unchecked so
 *                            callers opt in to handling them.</li>
 *   <li>{@code service}   -- {@code BookingService} facade and its single implementation,
 *                            {@code MeetingRoomBookingService}.</li>
 *   <li>{@code test}      -- dependency-free correctness and concurrency tests
 *                            ({@code TestRunner}).</li>
 * </ul>
 *
 * <h2>The decisions worth defending</h2>
 * <ol>
 *   <li><b>Locking is per-room, not global.</b> A single {@code ReentrantLock} guards each room's
 *       booking calendar. Two threads booking two different rooms never contend; two threads racing
 *       for the same room are serialized so the "no overlap" invariant can never be violated by a
 *       lost-update race.</li>
 *   <li><b>Best-fit room selection.</b> Candidate rooms (capacity &gt;= requested) are tried smallest
 *       first. This is exactly the rule in the prompt: a 16-person request may fall through to a
 *       20-person room if every smaller room is already booked for that slot, but it will not be
 *       handed the 20-person room while a closer-fitting room is still free.</li>
 *   <li><b>Bookings are keyed and searched by start time.</b> {@code Room} keeps a
 *       {@code TreeMap&lt;LocalDateTime, Booking&gt;}. Because existing bookings in a room are always
 *       mutually non-overlapping (an invariant maintained by construction), a new request can only
 *       ever conflict with the floor entry (the booking starting at-or-before the request) or the
 *       ceiling entry (the next booking starting at-or-after the request) -- an O(log n) check
 *       instead of an O(n) scan.</li>
 *   <li><b>Booking IDs are the linearization point.</b> A booking ID (UUID) is minted only after a
 *       slot is confirmed free and inserted while still holding the room's lock, so a returned ID is
 *       always backed by a real, non-overlapping reservation.</li>
 *   <li><b>Single-host, not distributed.</b> In-memory state and {@code ReentrantLock} are sufficient
 *       and intentional -- there is no cross-process coordination requirement here. Moving to
 *       multiple hosts would require replacing the per-room lock with a per-room DB row lock /
 *       compare-and-swap on a persisted calendar.</li>
 * </ol>
 *
 * <p>Run {@code MeetingSchedulerDemo} for a worked scenario (including a concurrent race for the
 * same room/slot) and {@code test.TestRunner} for the correctness and concurrency suite.
 */
package com.lowleveldesign.meetingscheduler;
