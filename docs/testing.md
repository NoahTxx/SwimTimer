# Testing Strategy & Guidelines for SwimTimer

This document details the testing architecture, testing layers, best practices, and execution commands for the **SwimTimer** application.

---

## 1. Testing Architecture Overview

SwimTimer uses a **JVM-first, decoupled testing architecture**:
- **Pure Jetpack Compose & ViewModel:** Business logic in `StopwatchViewModel` and UI components are decoupled from Android platform hardware.
- **Dependency Injection via Interfaces & Fakes:** `ISwimRepository` provides a clean contract implemented by `SwimRepository` (production Room DB) and `FakeSwimRepository` (in-memory Flow state).
- **Time Virtualization:** The timing engine uses `MonotonicClock` with `FakeClock`, allowing millisecond-accurate simulated runs without waiting for real wall-clock time.
- **Fast JVM Execution with Robolectric:** Both Room SQLite tests and Jetpack Compose UI tests execute directly on the JVM with Robolectric 4.17 in seconds, without requiring a physical device or emulator.

---

## 2. Test Suites & Layers

### A. Business Logic & Domain Unit Tests
- **Locations:**
  - [`app/src/test/java/com/example/ui/StopwatchViewModelTest.kt`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/test/java/com/example/ui/StopwatchViewModelTest.kt)
  - [`app/src/test/java/com/example/domain/PersonalBestEvaluatorTest.kt`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/test/java/com/example/domain/PersonalBestEvaluatorTest.kt)
- **Scope:**
  - Timer states (`IDLE`, `RUNNING`, `FINISHED`, `COUNTDOWN`).
  - Mass start (`onStartAll`) vs. single-lane start (`onStartSingleLane`).
  - Lap splits, finish times, and hundredths rounding.
  - Lane addition and swimmer assignment.
  - Multi-lane session persistence and lap verification.
  - Personal Best (PB) calculations across all strokes (Freestyle, Medley/Lagen legs, favorite stroke sync).

### B. In-Memory Room SQLite Tests
- **Location:** [`app/src/test/java/com/example/data/db/AppDatabaseTest.kt`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/test/java/com/example/data/db/AppDatabaseTest.kt)
- **Scope:**
  - In-memory database initialization (`Room.inMemoryDatabaseBuilder`).
  - Swimmer CRUD and personal bests (PBs).
  - Bulk group renaming and clearing.
  - Relational `SessionWithLaps` queries and cascading deletes.

### C. Compose UI Behavior & Navigation Tests
- **Location:** [`app/src/test/java/com/example/ui/ComposeUiBehaviorTest.kt`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/test/java/com/example/ui/ComposeUiBehaviorTest.kt)
- **Scope:**
  - Semantic matcher verifications (`assertIsDisplayed`, `assertExists`).
  - Dialog dismissals and click interactions (`StrokeRateCalculatorDialog`, `SwimPaceCalculatorDialog`).
  - State restoration testing across configuration changes with `StateRestorationTester`.
  - Tab navigation and back-handling logic (`MainAppContent`).

### D. Math & Swim Timing Algorithms
- **Location:** [`app/src/test/java/com/example/ExampleRobolectricTest.kt`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/test/java/com/example/ExampleRobolectricTest.kt)
- **Scope:**
  - Time formatting (`TimeFormatter.formatMillis`, `formatDelta`).
  - Hundredths rounding guarantees (ensuring lap sums equal total split).
  - Individual medley (IM / Lagen) leg orders and distances.

---

## 3. Running Tests

### Run All Unit, Database & UI Tests
To run all local tests on the JVM:
```bash
./gradlew :app:testDebugUnitTest
```

### Run a Specific Test Class
```bash
./gradlew :app:testDebugUnitTest --tests com.example.ui.StopwatchViewModelTest
./gradlew :app:testDebugUnitTest --tests com.example.data.db.AppDatabaseTest
./gradlew :app:testDebugUnitTest --tests com.example.ui.ComposeUiBehaviorTest
```

### View HTML Test Reports
After running tests, inspect the generated HTML report:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 4. Writing New Tests

1. **Always use fakes over mocks:** Prefer `FakeSwimRepository` and `FakeClock`.
2. **ViewModel tests with Coroutines:**
   - Use `StandardTestDispatcher` and `Dispatchers.setMain(testDispatcher)`.
   - Clean up tickers by calling `viewModel.onResetAll()` in `@After` to avoid runaway infinite loops in tests.
3. **Compose UI Tests:**
   - Use `createComposeRule()` with `SwimTimerTheme`.
   - Verify state restoration using `StateRestorationTester`.
