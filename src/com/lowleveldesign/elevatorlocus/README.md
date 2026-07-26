# Single Elevator Simulation (LOCUS)

A time-driven simulation of **one elevator** in a **20-floor** building
(floors `1..20`, car parked on floor `1`). It schedules movement with the
**LOOK** algorithm to minimise passenger wait time and travel distance.

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
