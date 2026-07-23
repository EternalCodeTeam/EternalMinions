# EternalMinions Project Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a buildable standalone EternalMinions Paper project with convention plugins in `buildSrc`, a public API module, and a runtime plugin module.

**Architecture:** The API module owns immutable public minion contracts and a lifecycle-safe provider. The plugin module is the composition root, owns the in-memory registry and configuration, and exposes only the API service. Rendering, persistence, scheduling, and world mutation remain explicit future boundaries instead of empty implementations.

**Tech Stack:** Java 21, Gradle Kotlin DSL, Paper API, plugin-yml, Shadow, Okaeri Configs, JUnit 5, AssertJ.

---

### Task 1: Gradle project and buildSrc

**Files:**
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `buildSrc/build.gradle.kts`
- Create: `buildSrc/src/main/kotlin/Versions.kt`
- Create: `buildSrc/src/main/kotlin/eternalminions-java.gradle.kts`
- Create: `buildSrc/src/main/kotlin/eternalminions-java-test.gradle.kts`
- Create: `buildSrc/src/main/kotlin/eternalminions-repositories.gradle.kts`
- Create: `eternalminions-api/build.gradle.kts`
- Create: `eternalminions-plugin/build.gradle.kts`

- [x] Define the two modules and Java 21 convention plugins.
- [x] Configure Paper, Okaeri, JUnit, plugin-yml and Shadow dependencies centrally.
- [x] Copy the existing trusted Gradle wrapper from EternalCombat and verify `gradlew projects` lists both modules.

### Task 2: Public API contracts using TDD

**Files:**
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/EternalMinionsApi.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/EternalMinionsProvider.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionId.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionRendererType.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionPosition.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionSnapshot.java`
- Create: `eternalminions-api/src/main/java/com/eternalcode/minions/minion/MinionService.java`
- Test: `eternalminions-api/src/test/java/com/eternalcode/minions/EternalMinionsProviderTest.java`
- Test: `eternalminions-api/src/test/java/com/eternalcode/minions/minion/MinionIdTest.java`

- [x] Write provider and identifier tests before production types.
- [x] Run API tests and confirm compilation fails because the contracts do not exist.
- [x] Implement the minimal immutable contracts and lifecycle-safe provider.
- [x] Run API tests and confirm they pass.

### Task 3: Runtime registry and plugin bootstrap using TDD

**Files:**
- Create: `eternalminions-plugin/src/test/java/com/eternalcode/minions/minion/MinionRegistryTest.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/minion/MinionRegistry.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/ConfigService.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/config/MinionsConfig.java`
- Create: `eternalminions-plugin/src/main/java/com/eternalcode/minions/EternalMinionsPlugin.java`

- [x] Write tests for unique registration, owner lookup and removal.
- [x] Run plugin tests and confirm compilation fails because `MinionRegistry` is absent.
- [x] Implement a small registry with immutable outward-facing collections.
- [x] Add Okaeri configuration containing renderer distances, scheduler budget and offline limits.
- [x] Compose the registry and provider in the plugin lifecycle without static Bukkit access.
- [x] Run plugin tests and confirm they pass.

### Task 4: Documentation and full verification

**Files:**
- Create: `README.md`
- Create: `.gitignore`

- [x] Document module boundaries and explicitly list renderer, scheduler, persistence and world-action work as later milestones.
- [x] Run `gradlew clean build` and confirm both modules compile, all tests pass, and the shaded plugin JAR is created.
- [x] Inspect the generated plugin description and final file tree for accidental copied template names.
