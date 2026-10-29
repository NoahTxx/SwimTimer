# Agent Instructions & Project Guidelines: SwimTimer

Welcome! This repository is an Android application built with Jetpack Compose, Kotlin Coroutines, and Room Database.

## Testing Guidelines

When developing features, fixing bugs, or refactoring code:
- **Comprehensive Testing Strategy:** Read [`docs/testing.md`](file:///C:/Users/noaht/StudioProjects/SwimTimer/docs/testing.md) for full architectural patterns, test layers, and conventions.
- **Run Unit, DB & UI Tests:** Run `./gradlew :app:testDebugUnitTest` and ensure all 46+ tests pass before committing changes.
- **Use Test Fakes:** Inject [`ISwimRepository`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/main/java/com/example/data/repository/SwimRepository.kt) with [`FakeSwimRepository`](file:///C:/Users/noaht/StudioProjects/SwimTimer/app/src/test/java/com/example/data/repository/FakeSwimRepository.kt) and `MonotonicClock` with `FakeClock` for fast, deterministic unit tests.
- **Compose UI Tests:** Add tests under `app/src/test/java/com/example/ui/` with Robolectric and Compose Test APIs (`createComposeRule`, `StateRestorationTester`).
