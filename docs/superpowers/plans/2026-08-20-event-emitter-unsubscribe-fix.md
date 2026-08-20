# Fix JIPipeEventEmitter.unsubscribe() Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix `JIPipeEventEmitter.unsubscribe()` so it actually removes subscribers, eliminating a codebase-wide memory leak where 44 `unsubscribe()` call sites are silent no-ops.

**Architecture:** Remove the never-populated `listenerSubscriberMap`. Add a `matchesListener(Listener)` method to the `Subscriber` interface. Rewrite `unsubscribe()` to iterate `subscribers` under a write lock and remove matching entries by identity comparison. Remove dead fields (`emittingDepth`, `copyOnWriteActive`). Add comprehensive tests.

**Tech Stack:** Java 25, JUnit 5, AssertJ, Maven

## Global Constraints

- All changes are in `jipipe-core` module
- The `Subscriber` interface is public and nested in `JIPipeEventEmitter` — adding a method is a breaking API change, but all implementations are nested classes in the same file
- Thread safety via `StampedLock` must be preserved — `unsubscribe()` must acquire the write lock
- Identity comparison (`==`) must be used for listener matching, consistent with the original `IdentityHashMap` design
- No external dependencies may be added
- Existing callers of `unsubscribe()` must work without modification

---

### Task 1: Create test infrastructure and failing tests

**Files:**
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/events/JIPipeEventEmitterTest.java`
- Read: `jipipe-core/src/main/java/org/hkijena/jipipe/api/events/JIPipeEventEmitter.java`

**Interfaces:**
- Consumes: `JIPipeEventEmitter` (abstract class), `JIPipeEvent` (marker interface), `Subscriber` (nested interface)
- Produces: `JIPipeEventEmitterTest` — test class that validates subscribe/unsubscribe/emit behavior

- [ ] **Step 1: Write the failing tests**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/events/JIPipeEventEmitterTest.java`:

```java
package org.hkijena.jipipe.api.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JIPipeEventEmitterTest {

    private static class TestEvent extends AbstractJIPipeEvent {
        private final String message;

        TestEvent(String message) {
            super(null);
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    private interface TestEventListener {
        void onTestEvent(TestEvent event);
    }

    private static class TestEventEmitter extends JIPipeEventEmitter<TestEvent, TestEventListener> {
        @Override
        protected void call(TestEventListener listener, TestEvent event) {
            listener.onTestEvent(event);
        }
    }

    private static class RecordingListener implements TestEventListener {
        final List<String> messages = new ArrayList<>();

        @Override
        public void onTestEvent(TestEvent event) {
            messages.add(event.getMessage());
        }
    }

    @Test
    void subscribeAndEmit_deliversToListener() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).containsExactly("hello");
    }

    @Test
    void unsubscribe_removesStrongSubscriber() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void unsubscribe_removesWeakSubscriber() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribeWeak(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void unsubscribe_withoutSubscribe_isNoOp() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void unsubscribe_withWrongListener_doesNothing() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listenerA = new RecordingListener();
        RecordingListener listenerB = new RecordingListener();

        emitter.subscribe(listenerA);
        emitter.unsubscribe(listenerB);
        emitter.emit(new TestEvent("hello"));

        assertThat(listenerA.messages).containsExactly("hello");
        assertThat(listenerB.messages).isEmpty();
    }

    @Test
    void unsubscribe_doesNotAffectOtherListeners() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listenerA = new RecordingListener();
        RecordingListener listenerB = new RecordingListener();

        emitter.subscribe(listenerA);
        emitter.subscribe(listenerB);
        emitter.unsubscribe(listenerA);
        emitter.emit(new TestEvent("hello"));

        assertThat(listenerA.messages).isEmpty();
        assertThat(listenerB.messages).containsExactly("hello");
    }

    @Test
    void unsubscribeAll_removesAllMatchingSubscribers() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.subscribe(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void subscribeLambda_isNotAffectedByUnsubscribe() {
        TestEventEmitter emitter = new TestEventEmitter();
        List<String> received = new ArrayList<>();
        RecordingListener listener = new RecordingListener();

        emitter.subscribeLambda((e, event) -> received.add(event.getMessage()));
        emitter.subscribe(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(received).containsExactly("hello");
        assertThat(listener.messages).isEmpty();
    }

    @Test
    void emit_afterUnsubscribe_doesNotCallUnsubscribedListener() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.emit(new TestEvent("first"));
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("second"));

        assertThat(listener.messages).containsExactly("first");
    }

    @Test
    void dispose_clearsAllSubscribers() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.dispose();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> emitter.emit(new TestEvent("hello"))
        );
    }

    @Test
    void gc_removesDeadWeakSubscribers() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribeWeak(listener);
        listener = null;
        System.gc();
        emitter.gc();
        emitter.emit(new TestEvent("hello"));
        // Should not throw or fail
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeEventEmitterTest -Dsurefire.useFile=false`
Expected: `unsubscribe_removesStrongSubscriber`, `unsubscribe_removesWeakSubscriber`, `unsubscribe_doesNotAffectOtherListeners`, `unsubscribeAll_removesAllMatchingSubscribers`, and `emit_afterUnsubscribe_doesNotCallUnsubscribedListener` FAIL. Other tests PASS.

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/api/events/JIPipeEventEmitterTest.java
git commit -m "Add failing tests for JIPipeEventEmitter unsubscribe (#1314)"
```

---

### Task 2: Fix unsubscribe() and remove dead code

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/events/JIPipeEventEmitter.java`

**Interfaces:**
- Consumes: `JIPipeEventEmitterTest` (from Task 1)
- Produces: Fixed `JIPipeEventEmitter` with working `unsubscribe()`, `Subscriber.matchesListener()`, no `listenerSubscriberMap`, no dead fields

- [ ] **Step 1: Implement the fix**

In `JIPipeEventEmitter.java`, make the following changes:

**Remove the `listenerSubscriberMap` field (line 36):**
```java
// DELETE this line:
private final Map<Listener, Subscriber<Event, Listener>> listenerSubscriberMap = new IdentityHashMap<>();
```

**Remove dead fields (lines 37-38):**
```java
// DELETE these lines:
private final AtomicInteger emittingDepth = new AtomicInteger(0);
private final AtomicBoolean copyOnWriteActive = new AtomicBoolean();
```

**Remove unused imports:**
```java
// DELETE these imports:
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
```

**Rewrite `unsubscribe()` (lines 85-90):**
```java
public void unsubscribe(Listener listener) {
    long stamp = stampedLock.writeLock();
    try {
        subscribers.removeIf(s -> s.matchesListener(listener));
    } finally {
        stampedLock.unlock(stamp);
    }
}
```

**Update `dispose()` (lines 138-142) — remove `listenerSubscriberMap.clear()`:**
```java
@Override
public void dispose() {
    disposed = true;
    subscribers.clear();
}
```

**Add `matchesListener` method to the `Subscriber` interface (after line 149):**
```java
public interface Subscriber<Event extends JIPipeEvent, Listener> {
    void call(JIPipeEventEmitter<Event, Listener> emitter, Event event);

    boolean isPresent();

    boolean requestGCImmediatelyAfterCall();

    boolean matchesListener(Listener listener);
}
```

**Implement `matchesListener` in `StrongObjectSubscriber` (after line 172):**
```java
@Override
public boolean matchesListener(Listener listener) {
    return this.listener == listener;
}
```

**Implement `matchesListener` in `WeakObjectSubscriber` (after line 198):**
```java
@Override
public boolean matchesListener(Listener listener) {
    return this.listener.get() == listener;
}
```

**Implement `matchesListener` in `LambdaSubscriber` (after line 226):**
```java
@Override
public boolean matchesListener(Listener listener) {
    return false;
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeEventEmitterTest -Dsurefire.useFile=false`
Expected: All 12 tests PASS

- [ ] **Step 3: Compile the full project to verify no breakage**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 4: Run the full test suite**

Run: `mvn test -pl jipipe-core -Dsurefire.useFile=false`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/events/JIPipeEventEmitter.java
git commit -m "Fix unsubscribe() by iterating subscribers and matching by identity (#1314)

Remove the never-populated listenerSubscriberMap. Add matchesListener()
to the Subscriber interface so unsubscribe() can find and remove matching
subscribers by identity comparison. Remove dead emittingDepth and
copyOnWriteActive fields."
```

---

### Task 3: Create design spec document

**Files:**
- Create: `docs/superpowers/specs/2026-08-20-event-emitter-unsubscribe-fix-design.md`

- [ ] **Step 1: Write the design spec**

```markdown
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
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/specs/2026-08-20-event-emitter-unsubscribe-fix-design.md
git commit -m "Add design spec for event emitter unsubscribe fix (#1314)"
```
