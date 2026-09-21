# Repository Guidelines

## Project Structure & Module Organization

This is a single NeoForge Minecraft mod module. Java code lives in `src/main/java/com/invasion`, organized by responsibility: core registration and configuration are at the package root, invasion/Nexus behavior is under `nexus`, and optional integrations are under `compat`. Mod assets, translations, sounds, recipes, tags, loot tables, and metadata live in `src/main/resources` (`assets/invmod`, `data/invmod`, and `META-INF`). The `nexus/test` package contains in-game debugging helpers, not a separate automated test suite.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper and Java 25:

- `./gradlew build` compiles the mod, processes resources, and creates the distributable JAR in `build/libs`.
- `./gradlew runClient` launches a development Minecraft client.
- `./gradlew runServer` launches a development server without a GUI.
- `./gradlew clean build` removes prior outputs before rebuilding when diagnosing stale artifacts.

There is currently no conventional unit-test source set; use a successful build and focused in-game verification for behavior changes.

## Coding Style & Naming Conventions

Follow the surrounding Java style: four-space indentation, UTF-8 source, descriptive `PascalCase` classes, `camelCase` methods and fields, and uppercase `CONSTANT_CASE` constants. Keep packages lowercase and place new code in the package owning its behavior. Preserve existing NeoForge registration and event patterns. Keep JSON/resource identifiers lowercase with underscores. Run the build as the formatting and compilation check; no separate formatter or linter is configured.

## Testing Guidelines

For gameplay or client changes, run `./gradlew build`, then exercise the affected path in `runClient` or `runServer` (especially Nexus waves, configuration, compatibility hooks, and resource loading). Include reproduction steps and expected/observed behavior in review notes. Add automated tests only when they fit the existing Gradle/NeoForge setup; otherwise keep diagnostic helpers scoped to `nexus/test`.

## Commit & Pull Request Guidelines

Recent commits use short, imperative descriptions, such as `Add mob config search` and `Prevent friendly fire between configured invasion mobs`. Follow that convention and keep each commit focused. Pull requests should explain the player-visible or maintenance impact, identify affected systems/resources, include verification commands and in-game steps, and attach screenshots or logs for UI, rendering, or crash-related changes. Call out compatibility or configuration changes explicitly.

## Configuration & Compatibility

Check `gradle.properties` before changing Minecraft/NeoForge or optional integration versions. Keep optional mod integrations isolated under `compat` and avoid making them required at runtime. Do not commit generated `build` outputs or local run data.
