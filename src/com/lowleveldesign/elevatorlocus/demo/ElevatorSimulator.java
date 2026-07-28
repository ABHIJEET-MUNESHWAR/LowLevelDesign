package com.lowleveldesign.elevatorlocus.demo;/*
    Created using IntelliJ IDEA
    Author: Abhijeet Ashok Muneshwar
    Date:   28-07-2026
    Time:   10:10 pm
*/

import java.util.*;

public class ElevatorSimulator {

    enum Direction {UP, DOWN, IDLE}

    static class Request {
        int       time;
        String    name;
        int       currentFloor;
        Direction direction;
        int       destFloor;

        public Request(String input) {
            String[] parts = input.replaceAll("\\s+", "").split(",");
            this.time = Integer.parseInt(parts[0].substring(1));
            this.name = parts[1];
            this.currentFloor = Integer.parseInt(parts[2]);
            this.direction = Direction.valueOf(parts[3].toUpperCase());
            this.destFloor = Integer.parseInt(parts[4]);
        }
    }

    static class Passenger {
        String name;
        int    destFloor;

        Passenger(String name, int destFloor) {
            this.name = name;
            this.destFloor = destFloor;
        }
    }

    public static void main(String[] args) {
        List<String> inputs = Arrays.asList(
                "T0, A, 4, UP, 14",
                "T3, B, 8, DOWN, 3",
                "T7, C, 5, UP, 8",
                "T8, D, 16, DOWN, 1"
        );

        List<Request> pendingRequests = new ArrayList<>();
        for (String line : inputs) {
            pendingRequests.add(new Request(line));
        }

        simulate(pendingRequests);
    }

    public static void simulate(List<Request> requests) {
        int       currentFloor = 1;
        int       currentTime  = 0;
        Direction currentDir   = Direction.IDLE;

        List<Request>   waitingRequests = new ArrayList<>();
        List<Passenger> insideElevator  = new ArrayList<>();

        while (!requests.isEmpty() || !waitingRequests.isEmpty() || !insideElevator.isEmpty()) {

            // 1. Ingest newly arrived requests up to current time
            Iterator<Request> reqIter = requests.iterator();
            while (reqIter.hasNext()) {
                Request r = reqIter.next();
                if (r.time <= currentTime) {
                    waitingRequests.add(r);
                    reqIter.remove();
                }
            }

            // 2. Determine elevator movement direction if IDLE
            if (currentDir == Direction.IDLE) {
                if (!waitingRequests.isEmpty()) {
                    Request closest = getClosestRequest(waitingRequests, currentFloor);
                    if (closest.currentFloor > currentFloor) currentDir = Direction.UP;
                    else if (closest.currentFloor < currentFloor) currentDir = Direction.DOWN;
                    else currentDir = closest.direction;
                } else if (!requests.isEmpty()) {
                    currentTime = requests.get(0).time;
                    continue;
                } else {
                    break; // Done
                }
            }

            // 3. Handle Pickups and Drop-offs at current floor
            List<String> events = new ArrayList<>();

            // Drop-off
            Iterator<Passenger> passIter = insideElevator.iterator();
            while (passIter.hasNext()) {
                Passenger p = passIter.next();
                if (p.destFloor == currentFloor) {
                    events.add(p.name + " OUT");
                    passIter.remove();
                }
            }

            // Pick-up
            Iterator<Request> waitIter = waitingRequests.iterator();
            while (waitIter.hasNext()) {
                Request r = waitIter.next();
                if (r.currentFloor == currentFloor) {
                    // Board if elevator moving same way OR if elevator was empty/heading to pick up
                    if (currentDir == Direction.IDLE || r.direction == currentDir || insideElevator.isEmpty()) {
                        events.add(r.name + " IN");
                        insideElevator.add(new Passenger(r.name, r.destFloor));
                        currentDir = r.direction; // Align direction with passenger request
                        waitIter.remove();
                    }
                }
            }

            // Stoppage time accounting
            if (!events.isEmpty()) {
                for (String event : events) {
                    System.out.println(currentFloor + " " + event);
                }
                currentTime += 1; // 1-minute stop delay
            }

            // 4. Direction decision for next move
            boolean hasTargetsAbove = hasTasksInDirection(Direction.UP, currentFloor, waitingRequests, insideElevator);
            boolean hasTargetsBelow = hasTasksInDirection(Direction.DOWN, currentFloor, waitingRequests, insideElevator);

            if (currentDir == Direction.UP && !hasTargetsAbove) {
                currentDir = hasTargetsBelow ? Direction.DOWN : Direction.IDLE;
            } else if (currentDir == Direction.DOWN && !hasTargetsBelow) {
                currentDir = hasTargetsAbove ? Direction.UP : Direction.IDLE;
            }

            // Move elevator 1 floor
            if (currentDir == Direction.UP) {
                currentFloor++;
                currentTime++;
            } else if (currentDir == Direction.DOWN) {
                currentFloor--;
                currentTime++;
            }
        }
    }

    private static Request getClosestRequest(List<Request> reqs, int currentFloor) {
        return reqs.stream()
                .min(Comparator.comparingInt(r -> Math.abs(r.currentFloor - currentFloor)))
                .orElse(reqs.get(0));
    }

    private static boolean hasTasksInDirection(Direction dir, int floor, List<Request> waiting, List<Passenger> inside) {
        if (dir == Direction.UP) {
            return inside.stream().anyMatch(p -> p.destFloor > floor) ||
                    waiting.stream().anyMatch(r -> r.currentFloor > floor);
        } else {
            return inside.stream().anyMatch(p -> p.destFloor < floor) ||
                    waiting.stream().anyMatch(r -> r.currentFloor < floor);
        }
    }
}