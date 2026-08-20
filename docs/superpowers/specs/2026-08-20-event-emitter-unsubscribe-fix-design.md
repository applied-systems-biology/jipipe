# JIPipeEventEmitter.unsubscribe() Fix Design

**Work item:** #1314
**Branch:** `1314-fix-event-emitter-unsubscribe` (off `master`)
**Date:** 2026-08-20

## Overview

Fix `JIPipeEventEmitter.unsubscribe()` which was a silent no-op due to a `listenerSubscriberMap` that was declared but never populated. Every `unsubscribe()` call across the codebase was a no-op, causing widespread memory leaks.

## Root cause

`listenerSubscriberMap` (an `IdentityHashMap`) was declared on line 36 but never written to. The `subscribe()`, `subscribeWeak()`, and `subscribeLambda()` methods all create subscriber objects and add them to the `subscribers` list via `addSubscriber()`, but never populate the map. `unsubscribe()` looked up the map, always got null, and did nothing.

## Impact

- 44 `unsubscribe()` call sites across the codebase are no-ops
- Disposed UI components retain event subscriptions forever (memory leak)
- Removed graph nodes retain subscriptions to slot/parameter events (memory leak)
- Recycled parameter editors retain subscriptions to old parameter sources (stale callbacks)

## Fix

- Removed `listenerSubscriberMap` entirely
- Added `matchesListener(Listener)` to the `Subscriber` interface
  - `StrongObjectSubscriber`: identity comparison with its strong listener reference
  - `WeakObjectSubscriber`: identity comparison with the referent of its `WeakReference`
  - `LambdaSubscriber`: always returns false (no listener object to match)
- Rewrote `unsubscribe()` to iterate `subscribers` under a write lock and remove matching entries via `removeIf`
- Removed dead fields `emittingDepth` and `copyOnWriteActive` (declared but never read)

## Why remove the map instead of populating it?

Populating the map for `subscribeWeak()` would require a strong reference to the listener as the map key, defeating the purpose of weak subscriptions (the listener could never be GC'd). Iterating the subscribers list and matching by identity handles both strong and weak cases correctly without this problem.

## Regression safety

- Thread safety: `unsubscribe()` acquires the write lock, consistent with `addSubscriber()` and `removeSubscriber()`
- Unsubscribe during emit: `emit()` copies the list under a read lock, so modifications during iteration are safe
- Double unsubscribe: `removeIf` handles gracefully (nothing to remove)
- GC of weak subscribers: `gc()` still works as before
- Lambda subscribers: `matchesListener()` returns false, so `unsubscribe()` does not affect them
