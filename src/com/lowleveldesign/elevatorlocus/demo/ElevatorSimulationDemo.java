package com.lowleveldesign.elevatorlocus.demo;

/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
*/

import com.lowleveldesign.elevatorlocus.engine.ElevatorSimulator;
import com.lowleveldesign.elevatorlocus.io.RequestParser;
import com.lowleveldesign.elevatorlocus.model.Request;
import com.lowleveldesign.elevatorlocus.model.StopEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point that runs the single-elevator LOOK simulation.
 *
 * <p>Usage:
 * <ul>
 *   <li>{@code java ... ElevatorSimulationDemo <file>} - read requests from a file</li>
 *   <li>{@code java ... ElevatorSimulationDemo -}       - read requests from stdin</li>
 *   <li>{@code java ... ElevatorSimulationDemo}          - run the built-in sample</li>
 * </ul>
 * Each request line uses the format {@code T0, A, 5, UP, 10}.
 */
public class ElevatorSimulationDemo {

    public static void main(String[] args) throws IOException {
        List<String> lines;
        if (args.length >= 1 && !"-".equals(args[0])) {
            lines = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
        } else if (args.length >= 1) {
            lines = readStdin();
        } else {
            System.out.println("No input provided - running the built-in sample.\n");
            lines = new ArrayList<>(Arrays.asList(
                    "T0, A, 4, UP, 14",
                    "T3, B, 8, DOWN, 3",
                    "T7, C, 5, UP, 8",
                    "T8, D, 16, DOWN, 1"));
        }

        RequestParser parser = new RequestParser(ElevatorSimulator.MIN_FLOOR, ElevatorSimulator.MAX_FLOOR);
        List<String> errors = new ArrayList<>();
        List<Request> requests = parser.parseAll(lines, errors);

        if (!errors.isEmpty()) {
            System.err.println("Skipped " + errors.size() + " invalid request(s):");
            errors.forEach(e -> System.err.println("  - " + e));
            System.err.println();
        }

        List<StopEvent> events = new ElevatorSimulator().simulate(requests);
        for (StopEvent event : events) {
            event.toOutputLines().forEach(System.out::println);
        }
    }

    private static List<String> readStdin() throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
