# Elevator System — Low Level Design

A low level design of a multi-elevator (elevator bank) system in Java, modelling
hall calls, destination calls, dispatch scheduling and cabin movement using the
LOOK/SCAN algorithm.

## Table of Contents

1. [Package Structure](#package-structure)
2. [Folder Details](#folder-details)
   - [model](#model)
   - [strategy](#strategy)
   - [controller](#controller)
   - [demo](#demo)
3. [Flow of the System](#flow-of-the-system)
4. [Design Patterns Used](#design-patterns-used)
5. [Extending the Design](#extending-the-design)
   - [More Scheduling Strategies](#more-scheduling-strategies)
   - [Multithreading](#multithreading)
   - [Concurrency](#concurrency)
   - [Parallelism](#parallelism)

## Package Structure

```
com.lowleveldesign.elevator
├── model
│   ├── Direction.java
│   ├── RequestType.java
│   ├── Request.java
│   ├── DoorState.java
│   ├── Door.java
│   ├── ElevatorState.java
│   ├── Display.java
│   └── Elevator.java
├── strategy
│   ├── SchedulingStrategy.java
│   └── NearestElevatorStrategy.java
├── controller
│   ├── ElevatorController.java
│   └── Building.java
└── demo
    └── ElevatorSystemDemo.java
```

## Folder Details

### model

Contains the core domain entities — the "nouns" of the system, with no
knowledge of scheduling or dispatch logic.

| Class | Responsibility |
|---|---|
| `Direction` | Enum: `UP`, `DOWN`, `IDLE`. Direction of travel for an elevator or a hall request. |
| `RequestType` | Enum: `EXTERNAL` (hall call from a floor) vs `INTERNAL` (destination call from inside the cabin). |
| `Request` | Immutable value object representing a single call. Built via factory methods `externalRequest(floor, direction)` and `internalRequest(floor)` so invalid combinations (e.g. a hall call with `IDLE` direction) can't be constructed. |
| `DoorState` | Enum: `OPEN`, `CLOSED`. |
| `Door` | Models the cabin door. Deliberately simple (no timers/threads) so behaviour is deterministic and testable. |
| `ElevatorState` | Enum: `IDLE`, `MOVING`, `STOPPED` — the elevator's current activity. |
| `Display` | Cosmetic component reflecting an elevator's latest floor/direction (simulates the floor/cabin indicator panel). |
| `Elevator` | The core cabin. Maintains two `TreeSet<Integer>` queues (`upStops`, `downStops`) and implements the **LOOK/SCAN** algorithm via `step()`, moving one floor at a time and opening the door whenever it reaches a pending stop. |

### strategy

Contains the dispatch/scheduling logic — decoupled from the `Elevator` and
`ElevatorController` so new algorithms can be dropped in without touching
either.

| Class | Responsibility |
|---|---|
| `SchedulingStrategy` | Interface: `selectElevator(List<Elevator>, Request)`. |
| `NearestElevatorStrategy` | Default implementation. Prefers idle elevators (ranked by distance), then elevators already travelling toward the request in the same direction, and penalizes elevators moving away/opposite so they're only chosen as a last resort. |

### controller

Contains the orchestration layer — the "brain" that owns all elevators and
exposes the public API a building's panels would call into.

| Class | Responsibility |
|---|---|
| `ElevatorController` | Singleton. Owns the list of `Elevator`s, receives `submitHallRequest`/`submitDestinationRequest` calls, delegates elevator selection to the configured `SchedulingStrategy`, and advances all elevators each simulation tick via `stepAll()`. |
| `Building` | Thin wrapper representing the building: validates floor bounds and lazily initializes the `ElevatorController` singleton with the elevator count/capacity. |

### demo

| Class | Responsibility |
|---|---|
| `ElevatorSystemDemo` | `main()` entry point. Builds a `Building`, submits a couple of hall calls and destination calls, then loops calling `stepAll()` until every elevator is idle — printing floor-by-floor movement, door events, and the final state of each elevator. |

## Flow of the System

1. **Building setup** — `Building` is constructed with floor count, elevator
   count and per-elevator capacity. It initializes the `ElevatorController`
   singleton, which in turn creates the `Elevator` instances (each starting
   `IDLE` at floor 0).
2. **Hall call** — A passenger presses UP/DOWN on a floor →
   `ElevatorController.submitHallRequest(floor, direction)` creates an
   `EXTERNAL` `Request` and asks the `SchedulingStrategy` to pick the best
   `Elevator`. The chosen elevator adds the floor to its `upStops`/`downStops`
   queue via `addStop()`, and its `direction` flips from `IDLE` if needed.
3. **Destination call** — Once inside, a passenger presses a floor button →
   `ElevatorController.submitDestinationRequest(elevatorId, floor)` creates an
   `INTERNAL` `Request` and adds it directly to that elevator's stop queue
   (no scheduling decision needed — the cabin is fixed).
4. **Movement (LOOK/SCAN)** — Each simulation tick, `ElevatorController.stepAll()`
   calls `Elevator.step()` on every busy elevator. An elevator moving `UP`
   keeps incrementing its floor and serving every stop in `upStops` before
   reversing to `downStops` (and vice versa) — this avoids starvation and
   minimizes needless direction reversals.
5. **Arrival** — When the current floor matches a pending stop, the elevator
   transitions to `STOPPED`, opens the `Door`, closes it, and resumes if more
   stops remain, or goes `IDLE` if the queues are empty.
6. **Termination** — The demo loop calls `stepAll()` until
   `ElevatorController.anyElevatorBusy()` returns `false`, meaning all
   elevators have served every request and returned to `IDLE`.

```
Passenger → Building → ElevatorController → SchedulingStrategy → Elevator.addStop()
                                                                        │
                                                              ElevatorController.stepAll()
                                                                        │
                                                              Elevator.step() (LOOK) → Door open/close → Display
```

## Design Patterns Used

| Pattern | Where | Why |
|---|---|---|
| **Singleton** | `ElevatorController` | There should be exactly one controller coordinating all elevators in a building — duplicate controllers could double-dispatch the same hall call or lose track of elevator state. `getInstance(...)` guarantees a single shared coordination point, analogous to a single physical dispatch panel. |
| **Strategy** | `SchedulingStrategy` / `NearestElevatorStrategy` | Elevator dispatch algorithms vary widely in practice (nearest car, zoning, destination-dispatch, load balancing). Extracting selection logic behind an interface lets `ElevatorController` remain unchanged while algorithms are swapped via `setSchedulingStrategy()` — open for extension, closed for modification. |
| **Static Factory Method** | `Request.externalRequest(...)` / `Request.internalRequest(...)` | A hall call must always have a real direction (`UP`/`DOWN`), while a destination call has none. Private constructor + named factories prevent illegal states (e.g. an `EXTERNAL` request with `IDLE` direction) from ever being constructed, which a public constructor with optional/nullable fields would not guarantee. |
| **Facade** | `ElevatorController` | Hides the complexity of elevator selection, stop-queue management, and stepping behind two simple entry points (`submitHallRequest`, `submitDestinationRequest`) that callers (hall panels, cabin panels, the demo) use without knowing internal scheduling details. |
| **State (implicit, via enums)** | `ElevatorState`, `DoorState`, `Direction` | Rather than a full State pattern with polymorphic classes, lightweight enums represent the finite states of an elevator/door. This keeps the object graph simple while still making illegal states (e.g. "moving" with no direction) easy to guard against, appropriate since transition logic is small and centralized in `Elevator`. |
| **Immutable Value Object** | `Request` | Once a request is created it never changes — floor, direction and type are `final`. Immutability makes requests safe to pass around, hash/compare (`equals`/`hashCode`), and reason about without defensive copying. |

## Extending the Design

### More Scheduling Strategies

Because dispatch logic is isolated behind `SchedulingStrategy`, new
algorithms are additive — no existing class needs modification:

- **Zoning**: assign elevators to floor ranges (e.g. low-rise/high-rise) and
  filter candidates by zone before ranking by distance.
- **Destination Dispatch**: change `submitHallRequest` to accept the
  passenger's actual destination up front (common in modern high-rises) and
  group passengers heading to nearby floors into the same cabin to reduce
  stops.
- **Load-aware dispatch**: extend `Elevator` with a current passenger count
  and have the strategy skip/deprioritize elevators near `capacity`.
- **Estimated Time of Arrival (ETA) strategy**: instead of raw floor distance,
  estimate travel time factoring in pending stops ahead of the requested
  floor, door-open dwell time, etc.

Adding one of these only requires a new class implementing
`SchedulingStrategy` and calling
`elevatorController.setSchedulingStrategy(new YourStrategy())` — the
`Elevator` and `ElevatorController` classes require no changes.

### Multithreading

Currently, `ElevatorController.stepAll()` advances every elevator
sequentially on a single thread (suitable for a deterministic simulation/demo).
To model elevators as independent physical machines:

- Give each `Elevator` its own thread (or run each on a scheduled
  `ExecutorService` task) so cabins move simultaneously and independently
  rather than in lock-step within `stepAll()`.
- Replace the synchronous `step()` polling loop with each elevator running
  its own loop: sleep for a fixed tick interval, then `step()`, repeating
  until idle, decoupling elevator speed from the demo loop.
- Hall/destination request submission would happen from separate "passenger"
  threads (or a request-generating simulator) calling into the shared
  `ElevatorController` concurrently.

### Concurrency

Once multiple threads submit requests and/or drive elevators independently,
shared mutable state must be protected:

- `ElevatorController.elevators` (list) and `schedulingStrategy` (reference)
  should be guarded — e.g. use a `CopyOnWriteArrayList` for elevators (rarely
  mutated, frequently read) and `volatile`/synchronized accessors for the
  active strategy so a strategy swap is visible across dispatch threads.
- `Elevator`'s internal `upStops`/`downStops` `TreeSet`s are not thread-safe.
  If a hall-call thread calls `addStop()` while the elevator's own thread is
  concurrently running `step()`, this must be protected — either by making
  `Elevator` synchronize its own mutating methods (`addStop`, `step`) or by
  funneling all mutations through a per-elevator queue/mailbox (e.g. a
  `BlockingQueue<Request>`) that only the elevator's own thread drains,
  avoiding shared-state locking entirely.
- The scheduling decision in `NearestElevatorStrategy.selectElevator()` reads
  elevator state (`getDirection()`, `getCurrentFloor()`) that could be
  changing concurrently; a consistent snapshot (e.g. reading fields once into
  locals, or having `Elevator` expose an immutable state snapshot object)
  avoids torn reads during ranking.
- `ElevatorController.getInstance()` is already `synchronized` for safe lazy
  singleton initialization, but if request submission becomes hot-path/high
  frequency, consider double-checked locking or eager initialization to avoid
  contention on every call.

### Parallelism

With multithreading and concurrency handled, true parallel execution becomes
possible:

- **Independent elevator movement**: since each `Elevator`'s `step()` only
  touches its own state, elevators can genuinely run in parallel across CPU
  cores (e.g. via a thread pool sized to the number of elevators) with no
  cross-elevator locking needed — only the shared `ElevatorController` list
  lookups need synchronization.
- **Parallel strategy evaluation**: `NearestElevatorStrategy.selectElevator()`
  currently ranks elevators sequentially; for very large elevator banks this
  loop could use `parallelStream()` to compute costs concurrently before
  reducing to the minimum, since each elevator's cost calculation is
  independent and side-effect free.
- **Simulation scaling**: for load-testing the dispatch algorithm against
  many simulated passengers, request generation and elevator stepping could
  run on separate thread pools, with the `ElevatorController` acting as the
  synchronization boundary between them.
