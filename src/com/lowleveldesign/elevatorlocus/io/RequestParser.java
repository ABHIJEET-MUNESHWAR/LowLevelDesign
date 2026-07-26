package com.lowleveldesign.elevatorlocus.io;

import com.lowleveldesign.elevatorlocus.exception.InvalidRequestException;
import com.lowleveldesign.elevatorlocus.model.Direction;
import com.lowleveldesign.elevatorlocus.model.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the comma-separated request format into validated {@link Request}
 * objects, e.g. {@code "T0, A, 5, UP, 10"}.
 *
 * <p>Fields, in order: request time ({@code T<n>}), passenger name, current
 * floor, direction ({@code UP}/{@code DOWN}) and destination floor.
 */
public class RequestParser {

    private final int minFloor;
    private final int maxFloor;

    public RequestParser(int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }

    /**
     * Parses a single request line.
     *
     * @param line the raw line
     * @return the parsed and validated request
     * @throws InvalidRequestException if the line is malformed or invalid
     */
    public Request parse(String line) {
        if (line == null) {
            throw new InvalidRequestException("Request line must not be null");
        }
        String[] parts = line.split(",");
        if (parts.length != 5) {
            throw new InvalidRequestException(
                    "Expected 5 comma-separated fields but got " + parts.length + ": \"" + line + "\"");
        }
        int time = parseTime(parts[0].trim());
        String name = parts[1].trim();
        int currentFloor = parseFloor(parts[2].trim(), "current floor");
        Direction direction = parseDirection(parts[3].trim());
        int destinationFloor = parseFloor(parts[4].trim(), "destination floor");
        return new Request(time, name, currentFloor, direction, destinationFloor, minFloor, maxFloor);
    }

    /**
     * Parses every non-blank line, collecting per-line failures so that one bad
     * request does not abort the whole batch.
     *
     * @param lines  the raw input lines
     * @param errors a sink to which human-readable parse errors are appended
     * @return the successfully parsed requests
     */
    public List<Request> parseAll(List<String> lines, List<String> errors) {
        List<Request> requests = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            try {
                requests.add(parse(line));
            } catch (InvalidRequestException ex) {
                errors.add(ex.getMessage());
            }
        }
        return requests;
    }

    private int parseTime(String token) {
        if (token.length() < 2 || (token.charAt(0) != 'T' && token.charAt(0) != 't')) {
            throw new InvalidRequestException("Time must look like 'T<minutes>' but was \"" + token + "\"");
        }
        try {
            return Integer.parseInt(token.substring(1).trim());
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Invalid time value \"" + token + "\"");
        }
    }

    private int parseFloor(String token, String label) {
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            throw new InvalidRequestException("Invalid " + label + " \"" + token + "\"");
        }
    }

    private Direction parseDirection(String token) {
        try {
            Direction direction = Direction.valueOf(token.toUpperCase());
            if (direction != Direction.UP && direction != Direction.DOWN) {
                throw new IllegalArgumentException();
            }
            return direction;
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Direction must be UP or DOWN but was \"" + token + "\"");
        }
    }
}
