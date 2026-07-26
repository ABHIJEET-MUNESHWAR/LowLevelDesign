package com.lowleveldesign.elevator.test;

import com.lowleveldesign.elevator.controller.Building;
import com.lowleveldesign.elevator.controller.ElevatorController;
import com.lowleveldesign.elevator.exception.ControllerNotInitializedException;
import com.lowleveldesign.elevator.exception.ElevatorException;
import com.lowleveldesign.elevator.exception.ElevatorNotFoundException;
import com.lowleveldesign.elevator.exception.InvalidBuildingConfigurationException;
import com.lowleveldesign.elevator.exception.InvalidFloorException;
import com.lowleveldesign.elevator.exception.InvalidRequestException;
import com.lowleveldesign.elevator.exception.NoElevatorAvailableException;
import com.lowleveldesign.elevator.model.Direction;
import com.lowleveldesign.elevator.model.Door;
import com.lowleveldesign.elevator.model.Display;
import com.lowleveldesign.elevator.model.Elevator;
import com.lowleveldesign.elevator.model.ElevatorState;
import com.lowleveldesign.elevator.model.Request;
import com.lowleveldesign.elevator.model.RequestType;
import com.lowleveldesign.elevator.strategy.NearestElevatorStrategy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Dependency-free correctness test suite for the elevator system (no JUnit
 * required, consistent with {@code com.lowleveldesign.meetingscheduler.test.TestRunner}).
 * Run via {@code java com.lowleveldesign.elevator.test.TestRunner}; exits non-zero on failure.
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Runs the full suite, printing a PASS/FAIL line per test and exiting
     * non-zero if any fail so it can be used directly in CI.
     *
     * @param args unused
     * @throws Exception if the reflective singleton reset fails
     */
    public static void main(String[] args) throws Exception {
        // --- Request ---
        run("externalRequest rejects IDLE direction", TestRunner::testExternalRequestRejectsIdle);
        run("externalRequest stores floor/direction/type", TestRunner::testExternalRequestFields);
        run("internalRequest stores floor and IDLE direction", TestRunner::testInternalRequestFields);
        run("equals/hashCode treat identical requests as equal", TestRunner::testRequestEquality);
        run("toString distinguishes hall calls from destinations", TestRunner::testRequestToString);

        // --- Door / Display (supporting model objects) ---
        run("door starts CLOSED and reflects open()/close()", TestRunner::testDoorLifecycle);
        run("display reflects the most recent update()", TestRunner::testDisplayReflectsLastUpdate);

        // --- Elevator core (LOOK/SCAN) ---
        run("new elevator starts IDLE at floor 0", TestRunner::testElevatorInitialState);
        run("addStop at current floor while not moving opens door immediately", TestRunner::testAddStopAtCurrentFloorOpensImmediately);
        run("step() serves multiple UP stops in ascending (LOOK) order", TestRunner::testStepServesStopsInLookOrder);
        run("elevator reverses direction instead of going idle when the other queue has stops", TestRunner::testElevatorReversesDirection);
        run("elevator returns to IDLE once all stops are served", TestRunner::testElevatorBecomesIdle);
        run("distanceTo returns absolute floor distance", TestRunner::testDistanceTo);
        run("hasPendingRequests reflects queued stops", TestRunner::testHasPendingRequests);
        run("listener is notified only on arrival, not on intermediate floors", TestRunner::testListenerFiresOnlyOnArrival);
        run("destination requested only after listener fires (no premature drop-off)", TestRunner::testNoDropOffBeforeBoarding);

        // --- Scheduling strategy ---
        run("strategy prefers the closest IDLE elevator", TestRunner::testStrategyPrefersClosestIdle);
        run("strategy prefers an elevator already heading toward the request", TestRunner::testStrategyPrefersAlignedElevator);
        run("strategy penalizes an elevator moving away from the request", TestRunner::testStrategyPenalizesElevatorMovingAway);

        // --- ElevatorController (singleton + facade) ---
        run("getInstance() without args throws before initialization", TestRunner::testGetInstanceBeforeInitThrows);
        run("getInstance() returns the same shared instance", TestRunner::testGetInstanceReturnsSharedInstance);
        run("submitHallRequest dispatches to and returns the chosen elevator", TestRunner::testSubmitHallRequestDispatches);
        run("submitDestinationRequest queues a stop on the target elevator", TestRunner::testSubmitDestinationRequest);
        run("submitDestinationRequest with unknown elevator id throws", TestRunner::testSubmitDestinationRequestUnknownId);
        run("stepAll only advances elevators with pending requests", TestRunner::testStepAllOnlyAdvancesBusyElevators);
        run("setSchedulingStrategy swaps the dispatch algorithm", TestRunner::testSetSchedulingStrategy);

        // --- Building ---
        run("Building.validateFloor rejects out-of-range floors", TestRunner::testBuildingValidateFloorRejectsOutOfRange);
        run("Building.submitHallRequest rejects out-of-range floor", TestRunner::testBuildingSubmitHallRequestValidates);
        run("Building.submitDestinationRequest rejects out-of-range floor", TestRunner::testBuildingSubmitDestinationRequestValidates);

        // --- Custom exceptions ---
        run("Building constructor rejects invalid configuration", TestRunner::testBuildingRejectsInvalidConfiguration);
        run("NoElevatorAvailableException when strategy selects no elevator", TestRunner::testNoElevatorAvailableWhenStrategyDeclines);
        run("all elevator exceptions share the ElevatorException supertype", TestRunner::testAllExceptionsShareCommonSupertype);

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Executes one test in isolation with a fresh controller singleton,
     * recording the outcome. Failures are caught so a single broken test never
     * aborts the rest of the suite.
     *
     * @param name human-readable description shown in the output
     * @param test the test body
     */
    private static void run(String name, Callable<Void> test) {
        try {
            resetControllerSingleton();
            test.call();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " -- " + t.getMessage());
        }
    }

    /**
     * Fails the current test unless the condition holds.
     *
     * @param condition the condition that must be true
     * @param message   explanation reported when it isn't
     */
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Fails the current test unless the two values are equal, reporting both
     * so the mismatch is obvious.
     *
     * @param expected the value the test requires
     * @param actual   the value produced
     * @param message  explanation reported on mismatch
     */
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    /**
     * ElevatorController is a Singleton, which makes it awkward to unit test in
     * isolation - each test needs a fresh instance. Rather than adding a
     * production-only "reset" method purely for tests, reflection is used to
     * clear the private static instance between test cases.
     *
     * @throws Exception if the field cannot be accessed reflectively
     */
    private static void resetControllerSingleton() throws Exception {
        Field field = ElevatorController.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    // ---------------------------------------------------------------- Request

    private static Void testExternalRequestRejectsIdle() {
        try {
            Request.externalRequest(5, Direction.IDLE);
            throw new AssertionError("Expected InvalidRequestException for IDLE hall request");
        } catch (InvalidRequestException expected) {
            // expected
        }
        return null;
    }

    private static Void testExternalRequestFields() {
        Request request = Request.externalRequest(5, Direction.UP);
        assertEquals(5, request.getFloor(), "floor");
        assertEquals(Direction.UP, request.getDirection(), "direction");
        assertEquals(RequestType.EXTERNAL, request.getType(), "type");
        return null;
    }

    private static Void testInternalRequestFields() {
        Request request = Request.internalRequest(7);
        assertEquals(7, request.getFloor(), "floor");
        assertEquals(Direction.IDLE, request.getDirection(), "destination requests carry no direction");
        assertEquals(RequestType.INTERNAL, request.getType(), "type");
        return null;
    }

    private static Void testRequestEquality() {
        Request a = Request.externalRequest(5, Direction.UP);
        Request b = Request.externalRequest(5, Direction.UP);
        Request c = Request.externalRequest(5, Direction.DOWN);
        assertTrue(a.equals(b), "identical hall requests should be equal");
        assertEquals(a.hashCode(), b.hashCode(), "equal requests must share a hash code");
        assertTrue(!a.equals(c), "requests differing by direction must not be equal");
        return null;
    }

    private static Void testRequestToString() {
        assertEquals("HallCall(floor=3, dir=UP)", Request.externalRequest(3, Direction.UP).toString(), "hall call toString");
        assertEquals("Destination(floor=9)", Request.internalRequest(9).toString(), "destination toString");
        return null;
    }

    // ------------------------------------------------------------- Door/Display

    private static Void testDoorLifecycle() {
        Door door = new Door();
        assertEquals(com.lowleveldesign.elevator.model.DoorState.CLOSED, door.getState(), "door should start closed");
        door.open();
        assertEquals(com.lowleveldesign.elevator.model.DoorState.OPEN, door.getState(), "door should be open after open()");
        door.close();
        assertEquals(com.lowleveldesign.elevator.model.DoorState.CLOSED, door.getState(), "door should be closed after close()");
        return null;
    }

    private static Void testDisplayReflectsLastUpdate() {
        Display display = new Display();
        display.update(1, 4, Direction.UP);
        assertEquals(4, display.getCurrentFloor(), "display floor");
        assertEquals(Direction.UP, display.getDirection(), "display direction");
        display.update(1, 5, Direction.DOWN);
        assertEquals(5, display.getCurrentFloor(), "display should reflect the latest update");
        assertEquals(Direction.DOWN, display.getDirection(), "display should reflect the latest update");
        return null;
    }

    // ---------------------------------------------------------------- Elevator

    private static Void testElevatorInitialState() {
        Elevator elevator = new Elevator(1, 8);
        assertEquals(0, elevator.getCurrentFloor(), "initial floor");
        assertEquals(Direction.IDLE, elevator.getDirection(), "initial direction");
        assertEquals(ElevatorState.IDLE, elevator.getState(), "initial state");
        assertTrue(elevator.isIdle(), "isIdle() should be true initially");
        assertTrue(!elevator.hasPendingRequests(), "no pending requests initially");
        assertEquals(8, elevator.getCapacity(), "capacity");
        return null;
    }

    private static Void testAddStopAtCurrentFloorOpensImmediately() {
        Elevator elevator = new Elevator(1, 8);
        List<Integer> arrivals = new ArrayList<>();
        elevator.addListener((id, floor) -> arrivals.add(floor));

        elevator.addStop(0); // already at floor 0 and not moving

        assertEquals(Collections.singletonList(0), arrivals, "door should open immediately for a same-floor stop");
        assertTrue(!elevator.hasPendingRequests(), "nothing should be queued for a same-floor stop");
        assertEquals(ElevatorState.IDLE, elevator.getState(), "elevator should settle back to IDLE");
        return null;
    }

    private static Void testStepServesStopsInLookOrder() {
        Elevator elevator = new Elevator(1, 8);
        List<Integer> arrivals = new ArrayList<>();
        elevator.addListener((id, floor) -> arrivals.add(floor));

        elevator.addStop(5);
        elevator.addStop(2);
        elevator.addStop(8);

        while (elevator.hasPendingRequests()) {
            elevator.step();
        }

        assertEquals(Arrays.asList(2, 5, 8), arrivals, "LOOK algorithm should serve ascending stops in order while moving UP");
        assertEquals(8, elevator.getCurrentFloor(), "final floor should be the last stop served");
        return null;
    }

    private static Void testElevatorReversesDirection() {
        Elevator elevator = new Elevator(1, 8);
        List<Integer> arrivals = new ArrayList<>();
        elevator.addListener((id, floor) -> arrivals.add(floor));

        elevator.addStop(5);  // sets direction UP (was IDLE), queued in upStops
        elevator.addStop(-3); // behind the elevator - queued in downStops, direction stays UP

        assertEquals(Direction.UP, elevator.getDirection(), "should still be heading UP toward the first stop");

        while (elevator.hasPendingRequests()) {
            elevator.step();
        }

        assertEquals(Arrays.asList(5, -3), arrivals, "should finish the UP stop, then reverse to serve the DOWN stop");
        assertEquals(-3, elevator.getCurrentFloor(), "final floor after reversal");
        return null;
    }

    private static Void testElevatorBecomesIdle() {
        Elevator elevator = new Elevator(1, 8);
        elevator.addStop(3);
        while (elevator.hasPendingRequests()) {
            elevator.step();
        }
        assertEquals(Direction.IDLE, elevator.getDirection(), "direction should return to IDLE");
        assertEquals(ElevatorState.IDLE, elevator.getState(), "state should return to IDLE");
        assertTrue(elevator.isIdle(), "isIdle() should report true once queues are drained");
        return null;
    }

    private static Void testDistanceTo() {
        Elevator elevator = new Elevator(1, 8);
        elevator.addStop(6);
        elevator.step(); // currentFloor -> 1
        assertEquals(5, elevator.distanceTo(6), "distance from floor 1 to floor 6");
        assertEquals(1, elevator.distanceTo(0), "distance from floor 1 to floor 0");
        return null;
    }

    private static Void testHasPendingRequests() {
        Elevator elevator = new Elevator(1, 8);
        assertTrue(!elevator.hasPendingRequests(), "no stops queued yet");
        elevator.addStop(4);
        assertTrue(elevator.hasPendingRequests(), "a stop was queued");
        while (elevator.hasPendingRequests()) {
            elevator.step();
        }
        assertTrue(!elevator.hasPendingRequests(), "queue drained after serving the stop");
        return null;
    }

    private static Void testListenerFiresOnlyOnArrival() {
        Elevator elevator = new Elevator(1, 8);
        List<Integer> arrivals = new ArrayList<>();
        elevator.addListener((id, floor) -> arrivals.add(floor));

        elevator.addStop(3);
        elevator.step(); // floor 1 - not a stop
        assertTrue(arrivals.isEmpty(), "listener must not fire on intermediate floors");
        elevator.step(); // floor 2 - not a stop
        assertTrue(arrivals.isEmpty(), "listener must not fire on intermediate floors");
        elevator.step(); // floor 3 - the requested stop
        assertEquals(Collections.singletonList(3), arrivals, "listener should fire exactly once, on arrival");
        return null;
    }

    /**
     * Regression test for the pickup-before-dropoff bug: a destination must
     * never be queued (and therefore never served) before the passenger's
     * hall-call pickup floor has actually been reached.
     */
    private static Void testNoDropOffBeforeBoarding() {
        Elevator elevator = new Elevator(1, 8);
        List<Integer> arrivals = new ArrayList<>();
        elevator.addListener((id, floor) -> {
            arrivals.add(floor);
            if (floor == 8) {
                elevator.addStop(2); // destination only requested after boarding at 8
            }
        });

        elevator.addStop(8); // hall pickup

        while (elevator.hasPendingRequests()) {
            elevator.step();
        }

        assertEquals(Arrays.asList(8, 2), arrivals,
                "pickup at floor 8 must be served before the destination floor 2 is ever queued or reached");
        return null;
    }

    // --------------------------------------------------------------- Strategy

    private static Void testStrategyPrefersClosestIdle() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        Elevator near = new Elevator(1, 8);
        Elevator far = new Elevator(2, 8);
        far.addStop(9);
        while (far.hasPendingRequests()) {
            far.step();
        }
        // "far" ends idle at floor 9; "near" idle at floor 0. Request at floor 2 should prefer "near".
        Request request = Request.externalRequest(2, Direction.UP);

        Elevator chosen = strategy.selectElevator(Arrays.asList(near, far), request);

        assertEquals(near.getId(), chosen.getId(), "closest idle elevator should be chosen");
        return null;
    }

    private static Void testStrategyPrefersAlignedElevator() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        Elevator idleFar = new Elevator(1, 8);
        idleFar.addStop(20);
        while (idleFar.hasPendingRequests()) {
            idleFar.step();
        }
        // idleFar ends IDLE at floor 20.

        Elevator movingAligned = new Elevator(2, 8);
        movingAligned.addStop(10); // currently at 0, heading UP toward 10

        Request request = Request.externalRequest(5, Direction.UP); // between 0 and 10

        Elevator chosen = strategy.selectElevator(Arrays.asList(idleFar, movingAligned), request);

        assertEquals(movingAligned.getId(), chosen.getId(),
                "an elevator already moving toward the request in the same direction should win over a farther idle one");
        return null;
    }

    private static Void testStrategyPenalizesElevatorMovingAway() {
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();

        Elevator movingAway = new Elevator(1, 8);
        movingAway.addStop(10); // at floor 0 heading UP, away from a request below it

        Elevator idleCloser = new Elevator(2, 8);
        idleCloser.addStop(3);
        while (idleCloser.hasPendingRequests()) {
            idleCloser.step();
        }
        // idleCloser ends IDLE at floor 3.

        Request request = Request.externalRequest(1, Direction.DOWN); // below movingAway's current floor and opposite direction

        Elevator chosen = strategy.selectElevator(Arrays.asList(movingAway, idleCloser), request);

        assertEquals(idleCloser.getId(), chosen.getId(),
                "an idle elevator should be preferred over one moving away/opposite direction");
        return null;
    }

    // -------------------------------------------------------------- Controller

    private static Void testGetInstanceBeforeInitThrows() {
        try {
            ElevatorController.getInstance();
            throw new AssertionError("Expected ControllerNotInitializedException before any getInstance(count, capacity) call");
        } catch (ControllerNotInitializedException expected) {
            // expected
        }
        return null;
    }

    private static Void testGetInstanceReturnsSharedInstance() {
        ElevatorController first = ElevatorController.getInstance(2, 8);
        ElevatorController second = ElevatorController.getInstance();
        ElevatorController third = ElevatorController.getInstance(5, 20); // args ignored once created
        assertTrue(first == second, "getInstance() must return the same shared instance");
        assertTrue(first == third, "getInstance(count, capacity) must not create a second instance");
        assertEquals(2, third.getElevators().size(), "elevator count from the first initialization must stick");
        return null;
    }

    private static Void testSubmitHallRequestDispatches() {
        ElevatorController controller = ElevatorController.getInstance(2, 8);
        Elevator chosen = controller.submitHallRequest(3, Direction.UP);
        assertTrue(chosen != null, "a hall request must return the elevator chosen to serve it");
        assertTrue(chosen.hasPendingRequests(), "chosen elevator should have the floor queued");
        return null;
    }

    private static Void testSubmitDestinationRequest() {
        ElevatorController controller = ElevatorController.getInstance(2, 8);
        Elevator elevator = controller.getElevator(1);
        controller.submitDestinationRequest(1, 6);
        assertTrue(elevator.hasPendingRequests(), "destination stop should be queued on elevator 1");
        return null;
    }

    private static Void testSubmitDestinationRequestUnknownId() {
        ElevatorController controller = ElevatorController.getInstance(2, 8);
        try {
            controller.submitDestinationRequest(99, 4);
            throw new AssertionError("Expected ElevatorNotFoundException for an unknown elevator id");
        } catch (ElevatorNotFoundException expected) {
            // expected
        }
        return null;
    }

    private static Void testStepAllOnlyAdvancesBusyElevators() {
        ElevatorController controller = ElevatorController.getInstance(2, 8);
        controller.submitDestinationRequest(1, 5); // only elevator 1 is busy
        Elevator elevator1 = controller.getElevator(1);
        Elevator elevator2 = controller.getElevator(2);

        controller.stepAll();

        assertEquals(1, elevator1.getCurrentFloor(), "busy elevator should advance one floor");
        assertEquals(0, elevator2.getCurrentFloor(), "idle elevator must not move");
        return null;
    }

    private static Void testSetSchedulingStrategy() {
        ElevatorController controller = ElevatorController.getInstance(2, 8);
        final boolean[] usedCustomStrategy = {false};
        controller.setSchedulingStrategy((elevators, request) -> {
            usedCustomStrategy[0] = true;
            return elevators.get(0);
        });

        Elevator chosen = controller.submitHallRequest(4, Direction.UP);

        assertTrue(usedCustomStrategy[0], "controller must delegate elevator selection to the configured strategy");
        assertEquals(controller.getElevator(1).getId(), chosen.getId(), "custom strategy's chosen elevator should be used");
        return null;
    }

    // ----------------------------------------------------------------- Building

    private static Void testBuildingValidateFloorRejectsOutOfRange() {
        Building building = new Building(10, 1, 4);
        try {
            building.validateFloor(10); // valid range is [0, 9]
            throw new AssertionError("Expected InvalidFloorException for floor at/above numberOfFloors");
        } catch (InvalidFloorException expected) {
            // expected
        }
        try {
            building.validateFloor(-1);
            throw new AssertionError("Expected InvalidFloorException for a negative floor");
        } catch (InvalidFloorException expected) {
            // expected
        }
        return null;
    }

    private static Void testBuildingSubmitHallRequestValidates() {
        Building building = new Building(10, 1, 4);
        try {
            building.submitHallRequest(15, Direction.UP);
            throw new AssertionError("Expected InvalidFloorException for an out-of-range hall request");
        } catch (InvalidFloorException expected) {
            // expected
        }
        return null;
    }

    private static Void testBuildingSubmitDestinationRequestValidates() {
        Building building = new Building(10, 1, 4);
        try {
            building.submitDestinationRequest(1, -5);
            throw new AssertionError("Expected InvalidFloorException for an out-of-range destination");
        } catch (InvalidFloorException expected) {
            // expected
        }
        return null;
    }

    // ---------------------------------------------------------------- Exceptions

    private static Void testBuildingRejectsInvalidConfiguration() {
        try {
            new Building(0, 2, 8);
            throw new AssertionError("Expected InvalidBuildingConfigurationException for zero floors");
        } catch (InvalidBuildingConfigurationException expected) {
            // expected
        }
        try {
            new Building(10, 0, 8);
            throw new AssertionError("Expected InvalidBuildingConfigurationException for zero elevators");
        } catch (InvalidBuildingConfigurationException expected) {
            // expected
        }
        try {
            new Building(10, 2, -1);
            throw new AssertionError("Expected InvalidBuildingConfigurationException for negative capacity");
        } catch (InvalidBuildingConfigurationException expected) {
            // expected
        }
        return null;
    }

    private static Void testNoElevatorAvailableWhenStrategyDeclines() {
        ElevatorController controller = ElevatorController.getInstance(2, 8);
        controller.setSchedulingStrategy((elevators, request) -> null); // e.g. all cars out of service
        try {
            controller.submitHallRequest(4, Direction.UP);
            throw new AssertionError("Expected NoElevatorAvailableException when the strategy selects nothing");
        } catch (NoElevatorAvailableException expected) {
            // expected
        }
        return null;
    }

    /**
     * All elevator failures share a common supertype, so callers that don't
     * care about the specific cause can catch just ElevatorException.
     */
    private static Void testAllExceptionsShareCommonSupertype() {
        Building building = new Building(10, 1, 4);
        try {
            building.validateFloor(99);
            throw new AssertionError("Expected an ElevatorException subtype");
        } catch (ElevatorException expected) {
            // expected - InvalidFloorException is an ElevatorException
        }
        try {
            Request.externalRequest(1, Direction.IDLE);
            throw new AssertionError("Expected an ElevatorException subtype");
        } catch (ElevatorException expected) {
            // expected - InvalidRequestException is an ElevatorException
        }
        assertTrue(new ElevatorException("x") instanceof RuntimeException,
                "ElevatorException should be unchecked so the API stays uncluttered");
        return null;
    }
}
