---
title: Robolectric tests hang or run out of memory when the activity runs a continuous Compose frame loop
date: 2026-11-10
category: test-failures
module: app (Paper Glide game screen)
problem_type: test_failure
component: testing_framework
symptoms:
  - "testDebugUnitTest never finishes, or later test classes fail with UncaughtExceptionsBeforeTest caused by OutOfMemoryError: Java heap space"
  - Each test class passes when run alone but the full suite hangs
  - Raising the test heap only makes the hang last longer
root_cause: test_isolation
resolution_type: test_fix
severity: high
framework_version: robolectric 4.17, compose bom 2026.09.00
tags: [robolectric, compose, withframenanos, game-loop, paused-looper, hang, oom]
---

# Robolectric tests hang or run out of memory when the activity runs a continuous Compose frame loop

## Problem
`MainActivity` hosts `GameScreen`, whose `LaunchedEffect` runs `while (true) { withFrameNanos { ... } }` to drive the game. Any Robolectric test that idles the main looper while that activity is composed never returns.

## Symptoms
- The full unit-test run hangs; a thread dump shows `ShadowPausedLooper.idle()` repeatedly running `Choreographer.doFrame` under `ActivityController.setup()`.
- Earlier runs failed later test classes with `UncaughtExceptionsBeforeTest` wrapping `OutOfMemoryError`.

## What Didn't Work
- Raising `maxHeapSize` for the test JVM: the loop is unbounded, so more heap only delays the failure.

## Solution
- Tests that only check `onCreate` behaviour (requested orientation) use `Robolectric.buildActivity(MainActivity::class.java).create().get()` instead of `.setup()`, so the looper is never idled with the game composed (`app/src/test/java/com/paperglide/ManifestTest.kt`).
- UI tests use `createAndroidComposeRule<MainActivity>()` with `rule.mainClock.autoAdvance = false` and advance time by hand, so frames are produced only on request (`app/src/test/java/com/paperglide/ui/GameFlowTest.kt`, `TvFlowTest.kt`).

## Why This Works
`setup()` calls `visible()`, which idles the paused looper until no task is due. A frame loop always schedules the next Choreographer frame, so "idle" is never reached. `create()` stops before that idle, and the Compose test clock with auto-advance off decouples frame production from idling.

## Prevention
- Never call `setup()`, `resume()`/`visible()` or `ActivityScenario` idling on an activity whose composition runs an endless frame loop; drive it through the Compose test rule with a manual clock.

## Related Issues
- Plan: `docs/plans/2026-11-10-2051-feat-paper-glide-game-plan.md` (U1, U4).
