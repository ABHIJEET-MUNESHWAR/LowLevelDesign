# Single Elevator Simulation (LOCUS)

A time-driven simulation of **one elevator** in a **20-floor** building
(floors `1..20`, car parked on floor `1`). It schedules movement with the
**LOOK** algorithm to minimise passenger wait time and travel distance.

## Problem Statement




Problem Statement

Design and implement a program that simulates an elevator in a building with multiple floors that efficiently manages elevator movements to minimize passenger wait times and travel distances.


The program must fulfill the following requirements -


Number of elevators - 1


Number of floors - 20, with floor numbers starting with 1

Elevator requests
The elevator is on floor 1 before the requests start coming in.  
A user can request the elevator to go up or down by pressing the UP or DOWN button outside the elevator.  
If the elevator is already moving in the requested direction and will pass the requested floor at the time, it should stop and pick up the passenger. If the elevator is already moving in the requested direction and has already passed the requested floor, it will not pick up the passenger.  
When picked up, a passenger can request a destination floor by manually pressing the destination floor button on the panel inside the elevator (buttons 1 until 20). Assume that a passenger can press the destination floor exactly once. Repressing the same floor is not going to nullify the request.  
For simplicity, assume that the elevator has unlimited capacity.  
Time interval constraints
The elevator takes 1 min to move from one floor to the next, irrespective of direction.  
The elevator stops at any floor for 1 min.


















Input and Output Samples
Input Format
Each request is provided in the following comma-separated format:
1.  Time at which the request is made by the passenger. e.g. T0 is 0th min when the first request comes in. T5 is 5 mins after the first request. This is important to note considering the other requests coming from different floors at different times and lift movement time between floors is 1 min and stoppage time is 1 min.
2.  Name of the passenger
3.  Current floor of the passenger (C, where 1 ≤ C ≤ 20)
4.  Desired direction relative to the current floor: UP or DOWN
5.  Destination floor (D, where 1 ≤ D ≤ 20)


Output Format
Each output line consists of the following space-separated values:
1.  Floor at which the elevator stops (F, where 1 ≤ F ≤ 20)
2.  Passenger name(s) followed by their action: IN (boarding) or OUT (alighting)


Sample 1 (Basic single request)
Input
T0, A, 5, UP, 10

1-2-3-4-5
5-6-7-8-9-10
Output
5 A IN
10 A OUT


Sample 2 (Requests in the same direction)
Input
T0, A, 5, UP, 12
T1, B, 3, UP, 8

1-2-3 ----- 2 mins (1 1-2, 1 2-3 ) > 1st mins
Output
3 B IN
5 A IN
8 B OUT
12 A OUT







Sample 3 (Requests in the opposite direction, delayed down request)
Input
T0, A, 2, UP, 10
T5, B, 8, DOWN, 4

Output
2 A IN
10 A OUT
8 B IN
4 B OUT




Sample 4 (Requests in the opposite direction but without delay of any request)
Input
T0, A, 15, DOWN, 6
T2, B, 4, UP, 12

Output
4 B IN
12 B OUT
15 A IN
6 A OUT


Sample 5 (Requests in the opposite direction, delayed up request)
Input
T0, A, 15, DOWN, 6
T6, B, 4, UP, 12

Output
15 A IN
6 A OUT
4 B IN
12 B OUT

Sample 6 (Multiple opposite requests)
Input
T0, A, 4, UP, 14
T3, B, 8, DOWN, 3
T7, C, 5, UP, 8
T8, D, 16, DOWN, 1



Output
4 A IN
14 A OUT
16 D IN
8 B IN
3 B OUT
1 D OUT
5 C IN
8 C OUT





Expectations from the candidate
Functional Completeness:Accurately implements elevator logic covering all rules, timing, and multiple requests with correct output.
Code Design & Structure: Writes clean, modular code with clear organization and separation of concerns.
Data Structures & Logic: Uses appropriate and efficient data structures with clear and effective logic.
Input Validation & Error Handling: Validates inputs properly and handles errors gracefully without crashes.
Scalability and Extensibility: Writes flexible code that can be easily extended to add new features.

## Problem rules modelled

- One elevator, 20 floors, starts on floor 1.
- A passenger raises a hall call (`UP`/`DOWN`) from a floor; if the elevator is
  already travelling that way and has **not yet passed** the floor, it stops and
  picks them up. If it has already passed, they are served on a later pass.
- The destination button is only "known" once the passenger **boards**.
- Moving between adjacent floors takes **1 minute**; each stop takes **1 minute**.

## Timing model

Every simulated minute the elevator performs exactly one physical act — a
**stop** (1 min to let passengers off/on) or a **move** (1 min to the next
floor). Choosing a direction and reversing at a turnaround are instantaneous.
Because a hall call is only registered at its request time, the scheduler
naturally ignores calls that arrive after the car has already passed the floor.

## Package layout

| Package | Responsibility |
|---------|----------------|
| `model`     | `Direction`, `Action`, `Request` (validated), `StopEvent` |
| `engine`    | `Elevator` (LOOK scheduling) and `ElevatorSimulator` (time loop) |
| `io`        | `RequestParser` for the `T0, A, 5, UP, 10` input format |
| `exception` | `InvalidRequestException` |
| `demo`      | `ElevatorSimulationDemo` runnable entry point |
| `test`      | `SampleTestRunner` — dependency-free checks for all 6 samples |

## Input / output format

Input line: `T<time>, <name>, <currentFloor>, <UP|DOWN>, <destination>`
Output line: `<floor> <name(s)> <IN|OUT>` (drop-offs before pickups at a floor).

## Build & run

```bash
# from the repository root
javac --release 8 -d out/elevatorlocus $(find src/com/lowleveldesign/elevatorlocus -name '*.java')

# run all sample checks
java -cp out/elevatorlocus com.lowleveldesign.elevatorlocus.test.SampleTestRunner

# run the built-in sample
java -cp out/elevatorlocus com.lowleveldesign.elevatorlocus.demo.ElevatorSimulationDemo

# read requests from a file, or from stdin with "-"
java -cp out/elevatorlocus com.lowleveldesign.elevatorlocus.demo.ElevatorSimulationDemo requests.txt
```

On Windows PowerShell, gather sources with:
`Get-ChildItem -Recurse src\com\lowleveldesign\elevatorlocus -Filter *.java | % FullName`

## Extensibility

- Building size and start floor are constructor parameters on
  `ElevatorSimulator` / `Elevator` — not hard-coded into the logic.
- The stop/dispatch decision lives in `Elevator`; an alternative strategy (e.g.
  SCAN, or multi-car dispatch) can reuse the same `Request`/`StopEvent` model.
- Input parsing is isolated in `RequestParser`, so alternative input sources or
  formats can be added without touching the scheduler.
