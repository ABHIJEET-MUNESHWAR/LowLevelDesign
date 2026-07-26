package com.lowleveldesign.elevatorlocus.test;

import com.lowleveldesign.elevatorlocus.engine.ElevatorSimulator;
import com.lowleveldesign.elevatorlocus.io.RequestParser;
import com.lowleveldesign.elevatorlocus.model.Request;
import com.lowleveldesign.elevatorlocus.model.StopEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lightweight (dependency-free) verification harness that runs every sample from
 * the problem statement and compares the produced output against the expected
 * output. Exits with a non-zero status if any sample fails.
 */
public class SampleTestRunner {

    private static int failures = 0;

    public static void main(String[] args) {
        check("Sample 1 - basic single request",
                Arrays.asList(
                        "T0, A, 5, UP, 10"),
                Arrays.asList(
                        "5 A IN",
                        "10 A OUT"));

        check("Sample 2 - requests in the same direction",
                Arrays.asList(
                        "T0, A, 5, UP, 12",
                        "T1, B, 3, UP, 8"),
                Arrays.asList(
                        "3 B IN",
                        "5 A IN",
                        "8 B OUT",
                        "12 A OUT"));

        check("Sample 3 - opposite direction, delayed down request",
                Arrays.asList(
                        "T0, A, 2, UP, 10",
                        "T5, B, 8, DOWN, 4"),
                Arrays.asList(
                        "2 A IN",
                        "10 A OUT",
                        "8 B IN",
                        "4 B OUT"));

        check("Sample 4 - opposite direction without delay",
                Arrays.asList(
                        "T0, A, 15, DOWN, 6",
                        "T2, B, 4, UP, 12"),
                Arrays.asList(
                        "4 B IN",
                        "12 B OUT",
                        "15 A IN",
                        "6 A OUT"));

        check("Sample 5 - opposite direction, delayed up request",
                Arrays.asList(
                        "T0, A, 15, DOWN, 6",
                        "T6, B, 4, UP, 12"),
                Arrays.asList(
                        "15 A IN",
                        "6 A OUT",
                        "4 B IN",
                        "12 B OUT"));

        check("Sample 6 - multiple opposite requests",
                Arrays.asList(
                        "T0, A, 4, UP, 14",
                        "T3, B, 8, DOWN, 3",
                        "T7, C, 5, UP, 8",
                        "T8, D, 16, DOWN, 1"),
                Arrays.asList(
                        "4 A IN",
                        "14 A OUT",
                        "16 D IN",
                        "8 B IN",
                        "3 B OUT",
                        "1 D OUT",
                        "5 C IN",
                        "8 C OUT"));

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL SAMPLES PASSED");
        } else {
            System.out.println(failures + " SAMPLE(S) FAILED");
            System.exit(1);
        }
    }

    private static void check(String name, List<String> input, List<String> expected) {
        RequestParser parser = new RequestParser(ElevatorSimulator.MIN_FLOOR, ElevatorSimulator.MAX_FLOOR);
        List<Request> requests = parser.parseAll(input, new ArrayList<>());

        List<String> actual = new ArrayList<>();
        for (StopEvent event : new ElevatorSimulator().simulate(requests)) {
            actual.addAll(event.toOutputLines());
        }

        if (actual.equals(expected)) {
            System.out.println("[PASS] " + name);
        } else {
            failures++;
            System.out.println("[FAIL] " + name);
            System.out.println("   expected: " + expected);
            System.out.println("   actual:   " + actual);
        }
    }
}
