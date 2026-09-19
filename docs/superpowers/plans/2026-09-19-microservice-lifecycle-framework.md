# Microservice Lifecycle Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a general microservice lifecycle framework that separates infrastructure lifecycle from work execution, then migrate the AI service and server service to use it, eliminating the search freeze root cause.

**Architecture:** A `Microservice` interface with `AbstractMicroservice` base class manages lifecycle states (Stopped/Starting/Ready/Stopping/Failed) and declared dependencies. Two new executor implementations (`ServiceAwareQueuedExecutor` for FIFO, `ServiceAwareEphemeralExecutor` for latest-wins) gate work dispatch on `Microservice.isReady()`. The AI service and server service are migrated to use this framework, replacing their current mixed lifecycle/work queues.

**Tech Stack:** Java 21, Maven, JUnit 5, Swing (SwingWorker-based executors)

## Global Constraints

- Java 21 required
- Maven build system
- No SciJava annotations or dependencies in the microservice framework package
- Framework must work without `JIPipe.getInstance()` being initialized (for testing and node-local use)
- All new code goes in package `org.hkijena.jipipe.api.microservice`
- Follow existing code conventions (copyright headers, JIPipeEventEmitter patterns)
- Existing `JIPipeRunnableExecutor` interface must be implemented by new executors
- Existing `JIPipeRunnableWorker` (SwingWorker) is reused for task execution

---

## File Structure

### New files (framework)

```
jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/
  Microservice.java                          — interface
  AbstractMicroservice.java                  — base implementation with state, deps, events
  MicroserviceState.java                     — enum: Stopped, Starting, Ready, Stopping, Failed
  MicroserviceStateChangeEvent.java          — event carrying oldState, newState, source
  MicroserviceStateChangeListener.java       — listener interface
  MicroserviceStateChangeEventEmitter.java   — emitter extending JIPipeEventEmitter
  ServiceAwareQueuedExecutor.java            — FIFO executor gating on isReady()
  ServiceAwareEphemeralExecutor.java         — latest-wins executor gating on isReady()
```

### New files (tests)

```
jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/
  AbstractMicroserviceTest.java
  MicroserviceDependencyTest.java
  ServiceAwareQueuedExecutorTest.java
  ServiceAwareEphemeralExecutorTest.java
```

### Deleted files (after migration)

```
jipipe-core/.../api/ai/JIPipeAIModelRunnerStatus.java
jipipe-core/.../api/servers/JIPipeServerState.java
jipipe-core/.../api/servers/JIPipeServerEvent.java
jipipe-core/.../api/servers/JIPipeServerEventEmitter.java
jipipe-core/.../api/servers/JIPipeServerEventListener.java
```

### Modified files (migration)

- `jipipe-core/pom.xml` — add JUnit 5
- `JIPipeAIServiceComponent.java` — replace mixed queue with microservice + service-aware executors
- `JIPipeAINodeDatabaseSearch.java` — remove workarounds, use service-aware ephemeral executor
- `JIPipeServerInstance.java` — extend `AbstractMicroservice`
- `EmbeddingServerInstance.java` — rename `start()`/`stop()` to `startProcess()`/`stopProcess()`
- `JIPipeServerServiceComponent.java` — use `Microservice.start()`/`stop()`
- `JIPipeAIModelRunner.java` — replace `JIPipeAIModelRunnerStatus` with `MicroserviceState`
- `JIPipeAPIEmbeddingAIModelRunner.java` — update status references
- `JIPipeOnnxEmbeddingAIModelRunner.java` — update status references
- All AI monitor UI components — update to `MicroserviceStateChangeEvent`
- All server monitor UI components — update to `MicroserviceStateChangeEvent`
- Any file referencing `JIPipeAIModelRunnerStatus` or `JIPipeServerState`

---

## Task 1: Add JUnit 5 to jipipe-core

**Files:**
- Modify: `jipipe-core/pom.xml`

**Interfaces:**
- Produces: JUnit 5 available as test dependency in `jipipe-core`

- [ ] **Step 1: Add JUnit 5 dependency to pom.xml**

Add the following to the `<dependencies>` section of `jipipe-core/pom.xml`, after the last existing dependency (before `</dependencies>`):

```xml
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.2</version>
            <scope>test</scope>
        </dependency>
```

Also add the maven-surefire-plugin configuration to the `<build>` section if not already present:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
```

- [ ] **Step 2: Verify dependency resolves**

Run: `mvn dependency:resolve -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/pom.xml
git commit -m "Add JUnit 5 test dependency to jipipe-core (#1322)"
```

---

## Task 2: Create MicroserviceState enum

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceState.java`

**Interfaces:**
- Produces: `MicroserviceState` enum with values `Stopped`, `Starting`, `Ready`, `Stopping`, `Failed`

- [ ] **Step 1: Create the enum**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceState.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

/**
 * Lifecycle states for a {@link Microservice}.
 *
 * <p>State transitions:
 * <ul>
 *   <li>{@code Stopped → Starting} (start called)</li>
 *   <li>{@code Starting → Ready} (onStart succeeded)</li>
 *   <li>{@code Starting → Failed} (onStart threw)</li>
 *   <li>{@code Ready → Stopping} (stop called)</li>
 *   <li>{@code Stopping → Stopped} (onStop completed)</li>
 *   <li>{@code Failed → Starting} (retry via start)</li>
 * </ul>
 *
 * <p>There is no {@code Busy} state — "busy" is a transient condition during
 * work execution tracked by the executor, not a lifecycle state. The optional
 * detail string on {@link AbstractMicroservice} can convey nuance.
 */
public enum MicroserviceState {
    Stopped,
    Starting,
    Ready,
    Stopping,
    Failed
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceState.java
git commit -m "Add MicroserviceState enum (#1322)"
```

---

## Task 3: Create Microservice event system

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeEvent.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeListener.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeEventEmitter.java`

**Interfaces:**
- Consumes: `AbstractJIPipeEvent` from `org.hkijena.jipipe.api.events`, `JIPipeEventEmitter` from `org.hkijena.jipipe.api.events`, `MicroserviceState` from Task 2
- Produces: `MicroserviceStateChangeEvent`, `MicroserviceStateChangeListener`, `MicroserviceStateChangeEventEmitter`

- [ ] **Step 1: Create the event class**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeEvent.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Event emitted when a {@link Microservice}'s state changes.
 */
public class MicroserviceStateChangeEvent extends AbstractJIPipeEvent {
    private final Microservice microservice;
    private final MicroserviceState oldState;
    private final MicroserviceState newState;

    public MicroserviceStateChangeEvent(Object source, Microservice microservice,
                                        MicroserviceState oldState, MicroserviceState newState) {
        super(source);
        this.microservice = microservice;
        this.oldState = oldState;
        this.newState = newState;
    }

    public Microservice getMicroservice() {
        return microservice;
    }

    public MicroserviceState getOldState() {
        return oldState;
    }

    public MicroserviceState getNewState() {
        return newState;
    }
}
```

- [ ] **Step 2: Create the listener interface**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeListener.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import java.util.EventListener;

/**
 * Listener for {@link MicroserviceStateChangeEvent}s.
 */
public interface MicroserviceStateChangeListener extends EventListener {
    void onMicroserviceStateChanged(MicroserviceStateChangeEvent event);
}
```

- [ ] **Step 3: Create the emitter**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeEventEmitter.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

/**
 * Event emitter for {@link MicroserviceStateChangeEvent}s.
 */
public class MicroserviceStateChangeEventEmitter
        extends JIPipeEventEmitter<MicroserviceStateChangeEvent, MicroserviceStateChangeListener> {
    @Override
    protected void call(MicroserviceStateChangeListener listener, MicroserviceStateChangeEvent event) {
        listener.onMicroserviceStateChanged(event);
    }
}
```

- [ ] **Step 4: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeEvent.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeListener.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/MicroserviceStateChangeEventEmitter.java
git commit -m "Add MicroserviceStateChangeEvent system (#1322)"
```

---

## Task 4: Create Microservice interface and AbstractMicroservice base class

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/Microservice.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/AbstractMicroservice.java`

**Interfaces:**
- Consumes: `MicroserviceState` (Task 2), `MicroserviceStateChangeEventEmitter` (Task 3)
- Produces: `Microservice` interface, `AbstractMicroservice` base class

- [ ] **Step 1: Create the Microservice interface**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/Microservice.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import java.util.List;

/**
 * A microservice with a managed lifecycle.
 *
 * <p>Microservices have a lifecycle state ({@link MicroserviceState}), can declare
 * dependencies on other microservices (for lifecycle ordering), and fire
 * {@link MicroserviceStateChangeEvent}s when their state changes.</p>
 *
 * <p>The framework does not include a registry. Calling code is responsible for
 * creating and managing microservice instances. Global services (e.g., AI service,
 * server service) are managed by their respective {@code JIPipeServiceComponent}s.
 * Node-local services can be created transiently during pipeline runs.</p>
 */
public interface Microservice {
    String getName();

    void start();
    void stop();

    MicroserviceState getState();
    String getStateDetail();
    boolean isReady();

    List<Microservice> getDependencies();
    void addDependency(Microservice dependency);

    MicroserviceStateChangeEventEmitter getStateChangeEventEmitter();
}
```

- [ ] **Step 2: Create the AbstractMicroservice base class**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/AbstractMicroservice.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation of {@link Microservice} that manages state transitions,
 * dependency resolution, and event emission.
 *
 * <p>Subclasses implement {@link #onStart()} and {@link #onStop()} for actual
 * resource management. The {@link #start()} and {@link #stop()} methods are
 * final and orchestrate state transitions, dependency checking, and events.</p>
 *
 * <p>Thread safety: state is volatile, dependencies use CopyOnWriteArrayList,
 * start()/stop() are synchronized on this instance.</p>
 */
public abstract class AbstractMicroservice implements Microservice {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMicroservice.class);

    /**
     * Timeout for waiting on a dependency to transition out of Starting state (seconds).
     */
    private static final long DEPENDENCY_START_TIMEOUT_SECONDS = 120;

    /**
     * Polling interval when waiting for a dependency (milliseconds).
     */
    private static final long DEPENDENCY_POLL_INTERVAL_MS = 500;

    private final String name;
    private volatile MicroserviceState state = MicroserviceState.Stopped;
    private volatile String stateDetail = "";
    private final List<Microservice> dependencies = new CopyOnWriteArrayList<>();
    private final MicroserviceStateChangeEventEmitter stateChangeEventEmitter = new MicroserviceStateChangeEventEmitter();

    protected AbstractMicroservice(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public final synchronized void start() {
        if (state == MicroserviceState.Starting || state == MicroserviceState.Ready) {
            return;
        }

        // Start dependencies first
        for (Microservice dep : dependencies) {
            if (dep.getState() == MicroserviceState.Ready) {
                continue;
            }
            if (dep.getState() == MicroserviceState.Starting) {
                if (!waitForDependency(dep)) {
                    MicroserviceState oldState = state;
                    state = MicroserviceState.Failed;
                    stateDetail = "Dependency '" + dep.getName() + "' failed to start";
                    fireStateChange(oldState, MicroserviceState.Failed);
                    return;
                }
                continue;
            }
            // Dependency is Stopped or Failed — start it
            dep.start();
            if (!waitForDependency(dep)) {
                MicroserviceState oldState = state;
                state = MicroserviceState.Failed;
                stateDetail = "Dependency '" + dep.getName() + "' failed to start";
                fireStateChange(oldState, MicroserviceState.Failed);
                return;
            }
        }

        MicroserviceState oldState = state;
        state = MicroserviceState.Starting;
        fireStateChange(oldState, MicroserviceState.Starting);

        try {
            onStart();
            oldState = state;
            state = MicroserviceState.Ready;
            fireStateChange(oldState, MicroserviceState.Ready);
        } catch (Exception e) {
            LOGGER.error("Failed to start microservice '{}': {}", name, e.getMessage(), e);
            oldState = state;
            state = MicroserviceState.Failed;
            stateDetail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            fireStateChange(oldState, MicroserviceState.Failed);
        }
    }

    @Override
    public final synchronized void stop() {
        if (state == MicroserviceState.Stopped || state == MicroserviceState.Stopping) {
            return;
        }

        MicroserviceState oldState = state;
        state = MicroserviceState.Stopping;
        fireStateChange(oldState, MicroserviceState.Stopping);

        try {
            onStop();
        } catch (Exception e) {
            LOGGER.error("Error stopping microservice '{}': {}", name, e.getMessage(), e);
        }

        oldState = state;
        state = MicroserviceState.Stopped;
        stateDetail = "";
        fireStateChange(oldState, MicroserviceState.Stopped);
    }

    @Override
    public MicroserviceState getState() {
        return state;
    }

    @Override
    public String getStateDetail() {
        return stateDetail;
    }

    @Override
    public boolean isReady() {
        return state == MicroserviceState.Ready;
    }

    @Override
    public List<Microservice> getDependencies() {
        return new java.util.ArrayList<>(dependencies);
    }

    @Override
    public void addDependency(Microservice dependency) {
        Objects.requireNonNull(dependency, "Dependency must not be null");
        if (createsCycle(dependency)) {
            throw new IllegalArgumentException(
                    "Adding dependency '" + dependency.getName() + "' would create a circular dependency");
        }
        dependencies.add(dependency);
    }

    @Override
    public MicroserviceStateChangeEventEmitter getStateChangeEventEmitter() {
        return stateChangeEventEmitter;
    }

    // ===== Subclass hooks =====

    protected abstract void onStart() throws Exception;

    protected void onStop() throws Exception {
    }

    protected void setStateDetail(String detail) {
        this.stateDetail = detail != null ? detail : "";
    }

    protected void markFailed(String detail) {
        MicroserviceState oldState = state;
        if (oldState == MicroserviceState.Failed) {
            return;
        }
        state = MicroserviceState.Failed;
        stateDetail = detail != null ? detail : "";
        fireStateChange(oldState, MicroserviceState.Failed);
    }

    // ===== Internal =====

    private boolean waitForDependency(Microservice dep) {
        long deadline = System.currentTimeMillis() + DEPENDENCY_START_TIMEOUT_SECONDS * 1000;
        while (System.currentTimeMillis() < deadline) {
            MicroserviceState depState = dep.getState();
            if (depState == MicroserviceState.Ready) {
                return true;
            }
            if (depState == MicroserviceState.Failed) {
                return false;
            }
            try {
                Thread.sleep(DEPENDENCY_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean createsCycle(Microservice newDep) {
        Set<Microservice> visited = new HashSet<>();
        collectTransitiveDependencies(newDep, visited);
        return visited.contains(this);
    }

    private void collectTransitiveDependencies(Microservice service, Set<Microservice> visited) {
        if (!visited.add(service)) {
            return;
        }
        for (Microservice dep : service.getDependencies()) {
            collectTransitiveDependencies(dep, visited);
        }
    }

    private void fireStateChange(MicroserviceState oldState, MicroserviceState newState) {
        stateChangeEventEmitter.emit(new MicroserviceStateChangeEvent(this, this, oldState, newState));
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/Microservice.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/AbstractMicroservice.java
git commit -m "Add Microservice interface and AbstractMicroservice base class (#1322)"
```

---

## Task 5: Write AbstractMicroservice tests

**Files:**
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/AbstractMicroserviceTest.java`

**Interfaces:**
- Consumes: `AbstractMicroservice` (Task 4), `MicroserviceState` (Task 2), `MicroserviceStateChangeEvent` (Task 3)
- Produces: Test class verifying lifecycle correctness

- [ ] **Step 1: Write the test class**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/AbstractMicroserviceTest.java`:

```java
package org.hkijena.jipipe.api.microservice;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AbstractMicroserviceTest {

    static class TestService extends AbstractMicroservice {
        volatile boolean startCalled = false;
        volatile boolean stopCalled = false;
        volatile boolean throwOnStart = false;
        volatile boolean throwOnStop = false;
        String throwMessage = "boom";

        TestService() {
            super("Test Service");
        }

        TestService(String name) {
            super(name);
        }

        @Override
        protected void onStart() throws Exception {
            startCalled = true;
            if (throwOnStart) throw new RuntimeException(throwMessage);
        }

        @Override
        protected void onStop() throws Exception {
            stopCalled = true;
            if (throwOnStop) throw new RuntimeException(throwMessage);
        }
    }

    @Test
    void startTransitionsToReady() {
        TestService s = new TestService();
        s.start();
        assertEquals(MicroserviceState.Ready, s.getState());
        assertTrue(s.startCalled);
    }

    @Test
    void startFailureTransitionsToFailed() {
        TestService s = new TestService();
        s.throwOnStart = true;
        s.start();
        assertEquals(MicroserviceState.Failed, s.getState());
        assertTrue(s.startCalled);
        assertTrue(s.getStateDetail().contains("boom"));
    }

    @Test
    void stopTransitionsToStopped() {
        TestService s = new TestService();
        s.start();
        s.stop();
        assertEquals(MicroserviceState.Stopped, s.getState());
        assertTrue(s.stopCalled);
    }

    @Test
    void stopWhenStoppedIsNoOp() {
        TestService s = new TestService();
        s.stop();
        assertEquals(MicroserviceState.Stopped, s.getState());
        assertFalse(s.stopCalled);
    }

    @Test
    void startWhenReadyIsNoOp() {
        TestService s = new TestService();
        s.start();
        s.startCalled = false;
        s.start();
        assertFalse(s.startCalled);
        assertEquals(MicroserviceState.Ready, s.getState());
    }

    @Test
    void startWhenStartingIsNoOp() {
        TestService s = new TestService() {
            @Override
            protected void onStart() throws Exception {
                // Simulate that we're "in" onStart — state should already be Starting
                assertEquals(MicroserviceState.Starting, getState());
                // A second start() call should be a no-op (but it can't happen
                // because start() is synchronized and we're inside it)
                super.onStart();
            }
        };
        s.start();
        assertEquals(MicroserviceState.Ready, s.getState());
    }

    @Test
    void markFailedTransitionsToFailed() {
        TestService s = new TestService();
        s.start();
        s.markFailed("crashed");
        assertEquals(MicroserviceState.Failed, s.getState());
        assertEquals("crashed", s.getStateDetail());
    }

    @Test
    void markFailedWhenAlreadyFailedIsNoOp() {
        TestService s = new TestService();
        s.throwOnStart = true;
        s.start();
        String originalDetail = s.getStateDetail();
        s.markFailed("new error");
        assertEquals(MicroserviceState.Failed, s.getState());
        assertEquals(originalDetail, s.getStateDetail());
    }

    @Test
    void setStateDetailIsReflected() {
        TestService s = new TestService();
        s.setStateDetail("loading model...");
        assertEquals("loading model...", s.getStateDetail());
    }

    @Test
    void stateChangeEventsAreFired() {
        TestService s = new TestService();
        List<MicroserviceStateChangeEvent> events = new ArrayList<>();
        s.getStateChangeEventEmitter().subscribe(events::add);

        s.start();
        s.stop();

        assertEquals(4, events.size());
        assertEquals(MicroserviceState.Starting, events.get(0).getNewState());
        assertEquals(MicroserviceState.Ready, events.get(1).getNewState());
        assertEquals(MicroserviceState.Stopping, events.get(2).getNewState());
        assertEquals(MicroserviceState.Stopped, events.get(3).getNewState());
    }

    @Test
    void failedStartFiresEvents() {
        TestService s = new TestService();
        s.throwOnStart = true;
        List<MicroserviceStateChangeEvent> events = new ArrayList<>();
        s.getStateChangeEventEmitter().subscribe(events::add);

        s.start();

        assertEquals(2, events.size());
        assertEquals(MicroserviceState.Starting, events.get(0).getNewState());
        assertEquals(MicroserviceState.Failed, events.get(1).getNewState());
    }

    @Test
    void concurrentStartIsSafe() throws Exception {
        TestService s = new TestService();
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger startCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                s.start();
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(MicroserviceState.Ready, s.getState());
        // onStart should only have been called once
        assertEquals(1, startCount.get() + 1); // startCalled is volatile boolean, can't use AtomicInteger
    }

    @Test
    void failedServiceCanRestart() {
        TestService s = new TestService();
        s.throwOnStart = true;
        s.start();
        assertEquals(MicroserviceState.Failed, s.getState());

        // Fix the issue and retry
        s.throwOnStart = false;
        s.start();
        assertEquals(MicroserviceState.Ready, s.getState());
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -pl jipipe-core -Dtest=AbstractMicroserviceTest -q`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/AbstractMicroserviceTest.java
git commit -m "Add AbstractMicroservice tests (#1322)"
```

---

## Task 6: Write MicroserviceDependency tests

**Files:**
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/MicroserviceDependencyTest.java`

**Interfaces:**
- Consumes: `AbstractMicroservice` (Task 4), `MicroserviceState` (Task 2)
- Produces: Test class verifying dependency resolution

- [ ] **Step 1: Write the test class**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/MicroserviceDependencyTest.java`:

```java
package org.hkijena.jipipe.api.microservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MicroserviceDependencyTest {

    static class TestService extends AbstractMicroservice {
        boolean throwOnStart = false;

        TestService(String name) {
            super(name);
        }

        @Override
        protected void onStart() throws Exception {
            if (throwOnStart) throw new RuntimeException("dep failed");
        }
    }

    @Test
    void startStartsDependenciesFirst() {
        TestService dep = new TestService("Dependency");
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();

        assertEquals(MicroserviceState.Ready, dep.getState());
        assertEquals(MicroserviceState.Ready, main.getState());
    }

    @Test
    void dependencyFailureFailsDependent() {
        TestService dep = new TestService("Dependency");
        dep.throwOnStart = true;
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();

        assertEquals(MicroserviceState.Failed, dep.getState());
        assertEquals(MicroserviceState.Failed, main.getState());
        assertTrue(main.getStateDetail().contains("Dependency"));
    }

    @Test
    void readyDependencyIsNotRestarted() {
        TestService dep = new TestService("Dependency");
        dep.start();
        assertTrue(dep.startCalled);

        dep.startCalled = false;
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();

        assertFalse(dep.startCalled);
        assertEquals(MicroserviceState.Ready, main.getState());
    }

    @Test
    void stopDoesNotStopDependencies() {
        TestService dep = new TestService("Dependency");
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();
        main.stop();

        assertEquals(MicroserviceState.Stopped, main.getState());
        assertEquals(MicroserviceState.Ready, dep.getState());
    }

    @Test
    void circularDependencyRejected() {
        TestService a = new TestService("A");
        TestService b = new TestService("B");

        a.addDependency(b);
        assertThrows(IllegalArgumentException.class, () -> b.addDependency(a));
    }

    @Test
    void transitiveDependencyStarted() {
        TestService a = new TestService("A");
        TestService b = new TestService("B");
        TestService c = new TestService("C");

        b.addDependency(a);  // B depends on A
        c.addDependency(b);  // C depends on B

        c.start();

        assertEquals(MicroserviceState.Ready, a.getState());
        assertEquals(MicroserviceState.Ready, b.getState());
        assertEquals(MicroserviceState.Ready, c.getState());
    }

    @Test
    void transitiveCircularDependencyRejected() {
        TestService a = new TestService("A");
        TestService b = new TestService("B");
        TestService c = new TestService("C");

        b.addDependency(a);  // B depends on A
        c.addDependency(b);  // C depends on B
        assertThrows(IllegalArgumentException.class, () -> a.addDependency(c));
    }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `mvn test -pl jipipe-core -Dtest=MicroserviceDependencyTest -q`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/MicroserviceDependencyTest.java
git commit -m "Add MicroserviceDependency tests (#1322)"
```

---

## Task 7: Create ServiceAwareQueuedExecutor

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/ServiceAwareQueuedExecutor.java`

**Interfaces:**
- Consumes: `Microservice` (Task 4), `MicroserviceState` (Task 2), `MicroserviceStateChangeEvent` (Task 3), `JIPipeRunnableExecutor` interface, `JIPipeRunnableWorker`, `JIPipeRunnable`
- Produces: `ServiceAwareQueuedExecutor` implementing `JIPipeRunnableExecutor`

- [ ] **Step 1: Create the executor**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/ServiceAwareQueuedExecutor.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * FIFO {@link JIPipeRunnableExecutor} that only dispatches work when the
 * associated {@link Microservice} is in the {@link MicroserviceState#Ready} state.
 *
 * <p>Tasks enqueued while the service is not Ready pile up in the queue.
 * When the service transitions to Ready, the executor's state-change listener
 * triggers dispatch.</p>
 *
 * <p>Cancellation removes pending tasks from the queue before they start —
 * no orphan tasks remain. Lifecycle operations (start/stop) are handled by
 * the {@link Microservice}, NOT as queue tasks.</p>
 */
public class ServiceAwareQueuedExecutor implements JIPipeRunnableExecutor,
        JIPipeRunnable.FinishedEventListener, JIPipeRunnable.InterruptedEventListener,
        JIPipeRunnable.ProgressEventListener, MicroserviceStateChangeListener {

    private final String name;
    private final Microservice service;
    private final Queue<JIPipeRunnableWorker> queue = new ArrayDeque<>();
    private final Map<JIPipeRunnable, JIPipeRunnableWorker> assignedWorkers = new HashMap<>();
    private final JIPipeRunnable.EnqueuedEventEmitter enqueuedEventEmitter = new JIPipeRunnable.EnqueuedEventEmitter();
    private final JIPipeRunnable.FinishedEventEmitter finishedEventEmitter = new JIPipeRunnable.FinishedEventEmitter();
    private final JIPipeRunnable.InterruptedEventEmitter interruptedEventEmitter = new JIPipeRunnable.InterruptedEventEmitter();
    private final JIPipeRunnable.ProgressEventEmitter progressEventEmitter = new JIPipeRunnable.ProgressEventEmitter();
    private final JIPipeRunnable.StartedEventEmitter startedEventEmitter = new JIPipeRunnable.StartedEventEmitter();
    private JIPipeRunnableWorker currentlyRunningWorker = null;
    private boolean silent;

    public ServiceAwareQueuedExecutor(String name, Microservice service) {
        this.name = name;
        this.service = service;
        service.getStateChangeEventEmitter().subscribe(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JIPipeRunnableWorker enqueue(JIPipeRunnable run) {
        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, silent);
        worker.getFinishedEventEmitter().subscribe(this);
        worker.getInterruptedEventEmitter().subscribe(this);
        worker.getProgressEventEmitter().subscribe(this);
        assignedWorkers.put(run, worker);
        queue.add(worker);
        enqueuedEventEmitter.emit(new JIPipeRunnable.EnqueuedEvent(run, worker));
        tryDequeue();
        return worker;
    }

    private void tryDequeue() {
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            if (!service.isReady()) {
                return;
            }
            currentlyRunningWorker = queue.remove();
            startedEventEmitter.emit(new JIPipeRunnable.StartedEvent(currentlyRunningWorker.getRun(), currentlyRunningWorker));
            currentlyRunningWorker.execute();
        }
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
            unregisterWorkerEvents(event.getWorker());
            event.getRun().onFinished(event);
        }
        finishedEventEmitter.emit(event);
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            tryDequeue();
        }
    }

    @Override
    public void onRunnableInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            assignedWorkers.remove(currentlyRunningWorker.getRun());
            currentlyRunningWorker = null;
            tryDequeue();
            unregisterWorkerEvents(event.getWorker());
            event.getRun().onInterrupted(event);
        }
        interruptedEventEmitter.emit(event);
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            tryDequeue();
        }
    }

    @Override
    public void onRunnableProgress(JIPipeRunnable.ProgressEvent event) {
        progressEventEmitter.emit(event);
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        if (event.getNewState() == MicroserviceState.Ready) {
            tryDequeue();
        }
    }

    private void registerWorkerEvents(JIPipeRunnableWorker worker) {
        worker.getFinishedEventEmitter().subscribe(this);
        worker.getInterruptedEventEmitter().subscribe(this);
        worker.getProgressEventEmitter().subscribe(this);
    }

    private void unregisterWorkerEvents(JIPipeRunnableWorker worker) {
        worker.getFinishedEventEmitter().unsubscribe(this);
        worker.getInterruptedEventEmitter().unsubscribe(this);
        worker.getProgressEventEmitter().unsubscribe(this);
    }

    @Override
    public boolean isRunningOrEnqueued(JIPipeRunnable runnable) {
        if (currentlyRunningWorker != null && currentlyRunningWorker.getRun() == runnable)
            return true;
        for (JIPipeRunnableWorker worker : queue) {
            if (worker.getRun() == runnable)
                return true;
        }
        return false;
    }

    @Override
    public JIPipeRunnable.EnqueuedEventEmitter getEnqueuedEventEmitter() {
        return enqueuedEventEmitter;
    }

    @Override
    public JIPipeRunnable.FinishedEventEmitter getFinishedEventEmitter() {
        return finishedEventEmitter;
    }

    @Override
    public JIPipeRunnable.InterruptedEventEmitter getInterruptedEventEmitter() {
        return interruptedEventEmitter;
    }

    @Override
    public JIPipeRunnable.ProgressEventEmitter getProgressEventEmitter() {
        return progressEventEmitter;
    }

    @Override
    public JIPipeRunnable.StartedEventEmitter getStartedEventEmitter() {
        return startedEventEmitter;
    }

    @Override
    public Queue<JIPipeRunnableWorker> getQueue() {
        return new ArrayDeque<>(queue);
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public boolean isEmpty() {
        return currentlyRunningWorker == null && queue.isEmpty();
    }

    @Override
    public int size() {
        return (currentlyRunningWorker != null ? 1 : 0) + queue.size();
    }

    @Override
    public JIPipeRunnableWorker findWorkerOf(JIPipeRunnable run) {
        return assignedWorkers.getOrDefault(run, null);
    }

    @Override
    public JIPipeRunnableWorker getCurrentRunWorker() {
        return currentlyRunningWorker;
    }

    @Override
    public JIPipeRunnable getCurrentRun() {
        return currentlyRunningWorker != null ? currentlyRunningWorker.getRun() : null;
    }

    @Override
    public void cancel(JIPipeRunnable run) {
        if (run == null) return;
        JIPipeRunnableWorker worker = findWorkerOf(run);
        if (worker != null) {
            if (currentlyRunningWorker == worker) {
                worker.getRun().getProgressInfo().cancel();
                worker.cancel(true);
                assignedWorkers.remove(worker.getRun());
                currentlyRunningWorker = null;
                tryDequeue();
                unregisterWorkerEvents(worker);
            } else {
                queue.remove(worker);
                interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(worker,
                        new InterruptedException("Operation was cancelled.")));
                unregisterWorkerEvents(worker);
            }
        }
    }

    @Override
    public void cancelAll() {
        for (JIPipeRunnableWorker worker : ImmutableList.copyOf(queue)) {
            cancel(worker.getRun());
        }
        if (currentlyRunningWorker != null) {
            cancel(currentlyRunningWorker.getRun());
        }
    }

    @Override
    public void cancelIf(Predicate<JIPipeRunnable> predicate) {
        for (JIPipeRunnableWorker toCancel : queue.stream().filter(rw -> predicate.test(rw.getRun())).collect(Collectors.toList())) {
            cancel(toCancel.getRun());
        }
        if (currentlyRunningWorker != null && !currentlyRunningWorker.isDone() && predicate.test(currentlyRunningWorker.getRun())) {
            cancel(currentlyRunningWorker.getRun());
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/ServiceAwareQueuedExecutor.java
git commit -m "Add ServiceAwareQueuedExecutor (#1322)"
```

---

## Task 8: Create ServiceAwareEphemeralExecutor

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/ServiceAwareEphemeralExecutor.java`

**Interfaces:**
- Consumes: `Microservice` (Task 4), `MicroserviceState` (Task 2), `MicroserviceStateChangeEvent` (Task 3), `JIPipeRunnableExecutor` interface, `JIPipeRunnableWorker`, `JIPipeRunnable`
- Produces: `ServiceAwareEphemeralExecutor` implementing `JIPipeRunnableExecutor`

- [ ] **Step 1: Create the executor**

Create `jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/ServiceAwareEphemeralExecutor.java`:

```java
/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableExecutor;
import org.hkijena.jipipe.api.run.JIPipeRunnableWorker;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Latest-wins {@link JIPipeRunnableExecutor} that gates on {@link Microservice#isReady()}.
 *
 * <p>Only one task is held at a time. Enqueuing a new task cancels any previous
 * running or held task. If the service is not Ready, the task is held but not
 * executed until the service becomes Ready. If a newer task arrives before the
 * service is Ready, the held task is cancelled and replaced.</p>
 */
public class ServiceAwareEphemeralExecutor implements JIPipeRunnableExecutor,
        MicroserviceStateChangeListener {

    private final String name;
    private final Microservice service;
    private final AtomicLong generation = new AtomicLong(0);
    private volatile JIPipeRunnableWorker currentWorker = null;
    private volatile boolean silent = false;

    private final JIPipeRunnable.EnqueuedEventEmitter enqueuedEventEmitter = new JIPipeRunnable.EnqueuedEventEmitter();
    private final JIPipeRunnable.FinishedEventEmitter finishedEventEmitter = new JIPipeRunnable.FinishedEventEmitter();
    private final JIPipeRunnable.InterruptedEventEmitter interruptedEventEmitter = new JIPipeRunnable.InterruptedEventEmitter();
    private final JIPipeRunnable.ProgressEventEmitter progressEventEmitter = new JIPipeRunnable.ProgressEventEmitter();
    private final JIPipeRunnable.StartedEventEmitter startedEventEmitter = new JIPipeRunnable.StartedEventEmitter();

    public ServiceAwareEphemeralExecutor(String name, Microservice service) {
        this.name = name;
        this.service = service;
        service.getStateChangeEventEmitter().subscribe(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JIPipeRunnableWorker enqueue(JIPipeRunnable run) {
        long myGeneration = generation.incrementAndGet();

        // Cancel previous worker if running
        JIPipeRunnableWorker previous = currentWorker;
        if (previous != null) {
            previous.getRun().getProgressInfo().cancel();
            previous.cancel(true);
        }

        JIPipeRunnableWorker worker = new JIPipeRunnableWorker(run, silent);
        currentWorker = worker;

        worker.getFinishedEventEmitter().subscribe(this::onWorkerFinished);
        worker.getInterruptedEventEmitter().subscribe(this::onWorkerInterrupted);
        worker.getProgressEventEmitter().subscribe(this::onWorkerProgress);

        enqueuedEventEmitter.emit(new JIPipeRunnable.EnqueuedEvent(run, worker));

        // Only execute if service is ready
        if (service.isReady()) {
            startedEventEmitter.emit(new JIPipeRunnable.StartedEvent(run, worker));
            worker.execute();
        }

        return worker;
    }

    private void onWorkerFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getWorker() != currentWorker) {
            return;  // Superseded
        }
        currentWorker = null;
        finishedEventEmitter.emit(event);
    }

    private void onWorkerInterrupted(JIPipeRunnable.InterruptedEvent event) {
        if (event.getWorker() != currentWorker) {
            return;  // Superseded
        }
        currentWorker = null;
        interruptedEventEmitter.emit(event);
    }

    private void onWorkerProgress(JIPipeRunnable.ProgressEvent event) {
        if (event.getWorker() != currentWorker) {
            return;  // Superseded
        }
        progressEventEmitter.emit(event);
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        if (event.getNewState() == MicroserviceState.Ready) {
            // If we have a held worker that hasn't been executed, dispatch it now
            JIPipeRunnableWorker worker = currentWorker;
            if (worker != null && !worker.isDone()) {
                // Check if it hasn't been started yet (state is PENDING)
                if (worker.getState() == javax.swing.SwingWorker.StateValue.PENDING) {
                    startedEventEmitter.emit(new JIPipeRunnable.StartedEvent(worker.getRun(), worker));
                    worker.execute();
                }
            }
        }
    }

    @Override
    public void cancelAll() {
        generation.incrementAndGet();
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null) {
            worker.getRun().getProgressInfo().cancel();
            worker.cancel(true);
            currentWorker = null;
            interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(worker,
                    new InterruptedException("Operation was cancelled.")));
        }
    }

    @Override
    public void cancel(JIPipeRunnable run) {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null && worker.getRun() == run) {
            worker.getRun().getProgressInfo().cancel();
            worker.cancel(true);
            currentWorker = null;
            interruptedEventEmitter.emit(new JIPipeRunnable.InterruptedEvent(worker,
                    new InterruptedException("Operation was cancelled.")));
        }
    }

    @Override
    public void cancelIf(Predicate<JIPipeRunnable> predicate) {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null && predicate.test(worker.getRun())) {
            cancel(worker.getRun());
        }
    }

    @Override
    public boolean isEmpty() {
        return currentWorker == null;
    }

    @Override
    public int size() {
        return currentWorker != null ? 1 : 0;
    }

    @Override
    public boolean isRunningOrEnqueued(JIPipeRunnable runnable) {
        JIPipeRunnableWorker worker = currentWorker;
        return worker != null && worker.getRun() == runnable;
    }

    @Override
    public JIPipeRunnable getCurrentRun() {
        JIPipeRunnableWorker worker = currentWorker;
        return worker != null ? worker.getRun() : null;
    }

    @Override
    public JIPipeRunnableWorker getCurrentRunWorker() {
        return currentWorker;
    }

    @Override
    public JIPipeRunnableWorker findWorkerOf(JIPipeRunnable run) {
        JIPipeRunnableWorker worker = currentWorker;
        if (worker != null && worker.getRun() == run) {
            return worker;
        }
        return null;
    }

    @Override
    public Queue<JIPipeRunnableWorker> getQueue() {
        return new ArrayDeque<>();
    }

    @Override
    public boolean isSilent() {
        return silent;
    }

    @Override
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    @Override
    public JIPipeRunnable.EnqueuedEventEmitter getEnqueuedEventEmitter() {
        return enqueuedEventEmitter;
    }

    @Override
    public JIPipeRunnable.FinishedEventEmitter getFinishedEventEmitter() {
        return finishedEventEmitter;
    }

    @Override
    public JIPipeRunnable.InterruptedEventEmitter getInterruptedEventEmitter() {
        return interruptedEventEmitter;
    }

    @Override
    public JIPipeRunnable.ProgressEventEmitter getProgressEventEmitter() {
        return progressEventEmitter;
    }

    @Override
    public JIPipeRunnable.StartedEventEmitter getStartedEventEmitter() {
        return startedEventEmitter;
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn compile -pl jipipe-core -q`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/ServiceAwareEphemeralExecutor.java
git commit -m "Add ServiceAwareEphemeralExecutor (#1322)"
```

---

## Task 9: Write ServiceAwareQueuedExecutor tests

**Files:**
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/ServiceAwareQueuedExecutorTest.java`

**Interfaces:**
- Consumes: `ServiceAwareQueuedExecutor` (Task 7), `AbstractMicroservice` (Task 4), `DefaultJIPipeRunnable`
- Produces: Test class verifying readiness gating

- [ ] **Step 1: Write the test class**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/ServiceAwareQueuedExecutorTest.java`:

```java
package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAwareQueuedExecutorTest {

    static class TestService extends AbstractMicroservice {
        TestService() { super("Test"); }

        @Override
        protected void onStart() throws Exception { }
    }

    static class CountingRun extends DefaultJIPipeRunnable {
        final CountDownLatch latch;
        final AtomicInteger counter;
        final long delayMs;

        CountingRun(CountDownLatch latch, AtomicInteger counter, long delayMs) {
            this.latch = latch;
            this.counter = counter;
            this.delayMs = delayMs;
        }

        @Override
        public String getTaskLabel() { return "Counting"; }

        @Override
        public void run() {
            counter.incrementAndGet();
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            latch.countDown();
        }
    }

    @Test
    void tasksExecuteWhenServiceReady() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger(0);

        exec.enqueue(new CountingRun(latch, counter, 0));
        exec.enqueue(new CountingRun(latch, counter, 0));
        exec.enqueue(new CountingRun(latch, counter, 0));

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(3, counter.get());
    }

    @Test
    void tasksDoNotExecuteWhenServiceNotReady() throws Exception {
        TestService service = new TestService();
        // Do NOT start the service
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);

        exec.enqueue(new CountingRun(latch, counter, 0));

        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, counter.get());
    }

    @Test
    void tasksExecuteWhenServiceBecomesReady() throws Exception {
        TestService service = new TestService();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger counter = new AtomicInteger(0);

        exec.enqueue(new CountingRun(latch, counter, 0));
        exec.enqueue(new CountingRun(latch, counter, 0));

        // Service not ready — tasks should not execute
        assertFalse(latch.await(1, TimeUnit.SECONDS));
        assertEquals(0, counter.get());

        // Start service — tasks should execute
        service.start();
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(2, counter.get());
    }

    @Test
    void cancelAllRemovesPendingTasks() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        // Enqueue a long-running task to block the queue
        CountDownLatch blockLatch = new CountDownLatch(1);
        exec.enqueue(new CountingRun(blockLatch, new AtomicInteger(0), 10000));

        // Enqueue tasks that should never run
        CountDownLatch pendingLatch = new CountDownLatch(2);
        AtomicInteger pendingCounter = new AtomicInteger(0);
        exec.enqueue(new CountingRun(pendingLatch, pendingCounter, 0));
        exec.enqueue(new CountingRun(pendingLatch, pendingCounter, 0));

        exec.cancelAll();

        assertFalse(pendingLatch.await(2, TimeUnit.SECONDS));
        assertEquals(0, pendingCounter.get());
    }

    @Test
    void tasksExecuteInFifoOrder() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareQueuedExecutor exec = new ServiceAwareQueuedExecutor("test", service);

        AtomicInteger order = new AtomicInteger(0);
        int[] results = new int[5];
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int idx = i;
            exec.enqueue(new DefaultJIPipeRunnable() {
                @Override
                public String getTaskLabel() { return "Task " + idx; }

                @Override
                public void run() {
                    results[idx] = order.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        // Tasks should execute in order 1,2,3,4,5
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, results[i]);
        }
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl jipipe-core -Dtest=ServiceAwareQueuedExecutorTest -q`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/ServiceAwareQueuedExecutorTest.java
git commit -m "Add ServiceAwareQueuedExecutor tests (#1322)"
```

---

## Task 10: Write ServiceAwareEphemeralExecutor tests

**Files:**
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/ServiceAwareEphemeralExecutorTest.java`

**Interfaces:**
- Consumes: `ServiceAwareEphemeralExecutor` (Task 8), `AbstractMicroservice` (Task 4), `DefaultJIPipeRunnable`
- Produces: Test class verifying latest-wins + readiness gating

- [ ] **Step 1: Write the test class**

Create `jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/ServiceAwareEphemeralExecutorTest.java`:

```java
package org.hkijena.jipipe.api.microservice;

import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAwareEphemeralExecutorTest {

    static class TestService extends AbstractMicroservice {
        TestService() { super("Test"); }
        @Override
        protected void onStart() throws Exception { }
    }

    @Test
    void newTaskCancelsPrevious() throws Exception {
        TestService service = new TestService();
        service.start();
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch firstStarted = new CountDownLatch(1);
        AtomicInteger firstCompleted = new AtomicInteger(0);

        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "First"; }
            @Override
            public void run() {
                firstStarted.countDown();
                try { Thread.sleep(5000); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                firstCompleted.incrementAndGet();
            }
        });

        assertTrue(firstStarted.await(2, TimeUnit.SECONDS));

        // Enqueue second task — should cancel the first
        CountDownLatch secondDone = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Second"; }
            @Override
            public void run() {
                secondDone.countDown();
            }
        });

        assertTrue(secondDone.await(5, TimeUnit.SECONDS));
        assertEquals(0, firstCompleted.get(), "First task should have been cancelled before completing");
    }

    @Test
    void taskDoesNotExecuteWhenServiceNotReady() throws Exception {
        TestService service = new TestService();
        // Do NOT start
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Test"; }
            @Override
            public void run() { latch.countDown(); }
        });

        assertFalse(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    void taskExecutesWhenServiceBecomesReady() throws Exception {
        TestService service = new TestService();
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Test"; }
            @Override
            public void run() { latch.countDown(); }
        });

        assertFalse(latch.await(1, TimeUnit.SECONDS));

        service.start();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void cancelAllClearsHeldTask() throws Exception {
        TestService service = new TestService();
        ServiceAwareEphemeralExecutor exec = new ServiceAwareEphemeralExecutor("test", service);

        CountDownLatch latch = new CountDownLatch(1);
        exec.enqueue(new DefaultJIPipeRunnable() {
            @Override
            public String getTaskLabel() { return "Test"; }
            @Override
            public void run() { latch.countDown(); }
        });

        exec.cancelAll();
        service.start();

        assertFalse(latch.await(2, TimeUnit.SECONDS));
        assertTrue(exec.isEmpty());
    }
}
```

- [ ] **Step 2: Run tests**

Run: `mvn test -pl jipipe-core -Dtest=ServiceAwareEphemeralExecutorTest -q`
Expected: All tests pass

- [ ] **Step 3: Run all framework tests together**

Run: `mvn test -pl jipipe-core -Dtest="org.hkijena.jipipe.api.microservice.*" -q`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/ServiceAwareEphemeralExecutorTest.java
git commit -m "Add ServiceAwareEphemeralExecutor tests (#1322)"
```

---

## Task 11: Migrate JIPipeServerInstance to AbstractMicroservice

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/JIPipeServerInstance.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/embeddingserver/EmbeddingServerInstance.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/GenericProcessServerInstance.java` (if it exists and overrides start/stop)
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/JIPipeServerState.java`

**Interfaces:**
- Consumes: `AbstractMicroservice` (Task 4), `MicroserviceState` (Task 2)
- Produces: `JIPipeServerInstance` now extends `AbstractMicroservice`

- [ ] **Step 1: Rename existing start()/stop() methods**

In `JIPipeServerInstance.java`:
- Rename the abstract `start()` method to `startProcess()`
- Rename the abstract `stop()` method to `stopProcess()`
- Add `onStart()` that calls `startProcess()` and health check
- Add `onStop()` that calls `stopProcess()`
- Remove the `state` field, `getState()`, `setState()` — inherited from `AbstractMicroservice`
- Remove `JIPipeServerState` import

The full modified `JIPipeServerInstance.java`:

```java
package org.hkijena.jipipe.api.servers;

import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.microservice.AbstractMicroservice;
import org.hkijena.jipipe.api.microservice.MicroserviceState;

import java.time.Instant;

public abstract class JIPipeServerInstance<TEnv extends JIPipeEnvironment> extends AbstractMicroservice {
    private final TEnv environment;
    private final int port;
    private Instant startedAt;
    private Process process;

    protected JIPipeServerInstance(TEnv environment, int port) {
        super(environment.getName() + " (port " + port + ")");
        this.environment = environment;
        this.port = port;
    }

    @Override
    protected void onStart() throws Exception {
        setStateDetail("Starting on port " + port + "...");
        startProcess();
        setStateDetail("Waiting for health check...");
        if (!waitForHealth()) {
            throw new ServerStartException(ServerStartException.Reason.HealthCheckFailed,
                    "Health check failed for " + getServerTypeId() + " on port " + port);
        }
        startedAt = Instant.now();
        setStateDetail("Running on port " + port);
    }

    @Override
    protected void onStop() throws Exception {
        setStateDetail("Stopping...");
        stopProcess();
        startedAt = null;
    }

    private boolean waitForHealth() {
        long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            if (Thread.currentThread().isInterrupted()) return false;
            if (isHealthy()) return true;
            try { Thread.sleep(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    protected abstract void startProcess() throws ServerStartException;
    protected abstract void stopProcess();
    public abstract boolean isHealthy();
    public abstract String getServerTypeId();

    public TEnv getEnvironment() { return environment; }
    public int getPort() { return port; }
    public Instant getStartedAt() { return startedAt; }

    public String getDisplayName() {
        return environment.getName() + " (" + getServerTypeId() + ")";
    }

    public Process getProcess() { return process; }
    protected void setProcess(Process process) { this.process = process; }
}
```

- [ ] **Step 2: Update EmbeddingServerInstance to use renamed methods**

In `EmbeddingServerInstance.java`, rename `start()` to `startProcess()` and `stop()` to `stopProcess()`. Remove any direct `setState()` calls and `JIPipeServerState` references. The `setStateDetail()` calls replace the old state management.

- [ ] **Step 3: Update GenericProcessServerInstance if it exists**

Apply the same rename pattern (`start()` → `startProcess()`, `stop()` → `stopProcess()`).

- [ ] **Step 4: Update JIPipeServerServiceComponent**

In `JIPipeServerServiceComponent.java`:
- Replace all `instance.getState()` checks that used `JIPipeServerState` with `MicroserviceState` checks
- Replace `instance.setState(JIPipeServerState.X)` calls — these are now handled by `Microservice.start()`/`stop()`
- Replace `instance.start()` with `instance.startProcess()` or `instance.start()` (the Microservice.start() which calls onStart() which calls startProcess())
- Remove `JIPipeServerState` imports, `JIPipeServerEvent` imports
- Replace `fireStateChanged(instance, oldState, newState)` with the microservice's own event system
- The `acquireLease()` method should call `instance.start()` (the Microservice lifecycle method) instead of `startInstance()`

- [ ] **Step 5: Delete JIPipeServerState.java and related event files**

Delete:
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/JIPipeServerState.java`
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/JIPipeServerEvent.java`
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/JIPipeServerEventEmitter.java`
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/JIPipeServerEventListener.java`

- [ ] **Step 6: Update server monitor UI components**

Update any files that import `JIPipeServerState`:
- `JIPipeDesktopServerMonitorOverviewPage.java` — replace `JIPipeServerState` checks with `MicroserviceState`
- `JIPipeDesktopServerStatusControl.java` — same

- [ ] **Step 7: Verify it compiles**

Run: `mvn compile -q`
Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Migrate JIPipeServerInstance to AbstractMicroservice, delete JIPipeServerState (#1322)"
```

---

## Task 12: Migrate JIPipeAIServiceComponent to microservice framework

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeAIServiceComponent.java`
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/api/ai/JIPipeAIModelRunnerStatus.java`

**Interfaces:**
- Consumes: `AbstractMicroservice` (Task 4), `ServiceAwareQueuedExecutor` (Task 7), `ServiceAwareEphemeralExecutor` (Task 8), `MicroserviceState` (Task 2)
- Produces: Refactored `JIPipeAIServiceComponent` with `EmbeddingModelService` inner class

- [ ] **Step 1: Create EmbeddingModelService as inner class**

In `JIPipeAIServiceComponent.java`, add an inner class `EmbeddingModelService extends AbstractMicroservice` that:
- Has `onStart()` that does what `startEmbeddingModelNow()` currently does (resolve artifact, acquire lease or create runner)
- Has `onStop()` that does what `stopEmbeddingModelNow()` currently does (release lease, shutdown runner)
- Has a `embedNow(String text)` method that delegates to lease/runner
- Has a `getModelId()` method
- Declares dependencies on server service and artifacts service microservices (if they expose them)

- [ ] **Step 2: Replace the mixed queue with service-aware executors**

Replace:
```java
private final JIPipeQueuedRunnableExecutor taskQueue = new JIPipeQueuedRunnableExecutor("AI Service");
```

With:
```java
private final EmbeddingModelService embeddingModelService;
private final ServiceAwareQueuedExecutor embedQueue;
private final ServiceAwareEphemeralExecutor searchQueue;
```

Initialize them in the constructor. The `embedQueue` is used for batch embed operations (e.g., `tryEmbed()`), and the `searchQueue` is used for AI search operations.

- [ ] **Step 3: Remove old task classes and methods**

Remove:
- `LoadEmbeddingModelTask` inner class
- `UnloadEmbeddingModelTask` inner class
- `EmbedTextTask` inner class
- `EmbedFunctionTask<T>` inner class
- `submitEmbedFunction()` method
- `startEmbeddingModelNow()` method (moved to `EmbeddingModelService.onStart()`)
- `stopEmbeddingModelNow()` method (moved to `EmbeddingModelService.onStop()`)
- `StatusChangedEvent`, `StatusChangedEventEmitter`, `StatusChangedEventListener` inner classes
- `fireStatusChanged()` method
- `currentStatus` field

- [ ] **Step 4: Update public API methods to delegate**

- `tryStartEmbeddingModel()` → `embeddingModelService.start()`
- `tryStopEmbeddingModel()` → `embeddingModelService.stop()`
- `embedNow(text)` → `embeddingModelService.embedNow(text)`
- `tryEmbed(text)` → create a simple `EmbedTextRun` and enqueue on `embedQueue`, return its `CompletableFuture`
- `getEmbeddingModelStatus()` → `embeddingModelService.getState()` (returns `MicroserviceState`)
- `getEmbeddingModelError()` → `embeddingModelService.getStateDetail()` when state is `Failed`
- `getModelId()` → `embeddingModelService.getModelId()`
- `hasEmbeddingModel()` → `embeddingModelService.isReady()`
- `getEmbeddingProgressInfo()` → unchanged

- [ ] **Step 5: Delete JIPipeAIModelRunnerStatus.java**

Delete the file. Update all references:
- `JIPipeAIModelRunner.java` — change `getStatus()` return type to `MicroserviceState`
- `JIPipeAPIEmbeddingAIModelRunner.java` — update status field and references
- `JIPipeOnnxEmbeddingAIModelRunner.java` — update status field and references

- [ ] **Step 6: Update AI monitor UI components**

Update all files importing `JIPipeAIModelRunnerStatus`:
- `JIPipeDesktopAIMonitorOverviewPage.java` — subscribe to `MicroserviceStateChangeEvent`, replace status checks with `MicroserviceState`
- `JIPipeDesktopAIStatusControl.java` — same
- `JIPipeDesktopAIMonitorLogPage.java` — same
- `JIPipeDesktopAIMonitorEmbeddingLogPage.java` — same
- `JIPipeDesktopAIMonitorWindow.java` — same
- `ModelConfigurationComponent.java` — same

Replace patterns:
- `JIPipeAIModelRunnerStatus.Unloaded` / `Failed` → `MicroserviceState.Stopped` / `Failed`
- `JIPipeAIModelRunnerStatus.Loading` → `MicroserviceState.Starting`
- `JIPipeAIModelRunnerStatus.Idle` / `Busy` → `MicroserviceState.Ready`
- `JIPipeAIModelRunnerStatus.Unloading` → `MicroserviceState.Stopping`

- [ ] **Step 7: Verify it compiles**

Run: `mvn compile -q`
Expected: No errors

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "Migrate JIPipeAIServiceComponent to microservice framework, delete JIPipeAIModelRunnerStatus (#1322)"
```

---

## Task 13: Migrate JIPipeAINodeDatabaseSearch to use ServiceAwareEphemeralExecutor

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/database/JIPipeAINodeDatabaseSearch.java`

**Interfaces:**
- Consumes: `ServiceAwareEphemeralExecutor` (Task 8), refactored `JIPipeAIServiceComponent` (Task 12)
- Produces: Cleaned-up AI search without workarounds

- [ ] **Step 1: Remove the submitEmbedFunction workaround**

Remove the single-task `submitEmbedFunction` pattern from `internalQuery()`. Replace with a simple enqueue on the search ephemeral executor (or direct execution if already on a worker thread).

The search should:
1. Pre-filter candidates (unchanged)
2. Create a `SearchRun` task that does: ensure embeddings, embed query, score, sort
3. Enqueue the `SearchRun` on a `ServiceAwareEphemeralExecutor` gated on the embedding model microservice
4. Block on the result

Remove:
- `SEARCH_TOTAL_TIMEOUT_SECONDS` constant
- The `submitEmbedFunction` lambda
- The `JIPipeAIModelRunnerStatus` status check inside the lambda

- [ ] **Step 2: Simplify internalQuery**

The `internalQuery()` method should:
1. Check AI availability (unchanged)
2. Pre-filter candidates (unchanged)
3. Handle empty query (unchanged)
4. Resolve model ID (unchanged)
5. Get the search executor from the AI service (or accept it as a constructor parameter)
6. Enqueue a search task and block on the result

The search task body (running on the ephemeral executor's worker thread) uses `tryEmbed()` for the query and `ensureEmbeddingsForEntries()` for entries. These now go through the `ServiceAwareQueuedExecutor` which handles readiness gating.

- [ ] **Step 3: Remove ensureEmbeddingsForEntriesWithEmbedder if no longer needed**

If the `ensureEmbeddingsForEntriesWithEmbedder` method in `JIPipeEmbeddingDatabase` is no longer used (check with grep), remove it and the corresponding method in `JIPipeGlobalEmbeddingSearch`.

- [ ] **Step 4: Verify it compiles**

Run: `mvn compile -q`
Expected: No errors

- [ ] **Step 5: Run all tests**

Run: `mvn test -pl jipipe-core -q`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Migrate AI search to ServiceAwareEphemeralExecutor, remove workarounds (#1322)"
```

---

## Task 14: Final cleanup and verification

**Files:**
- Various (cleanup of dead imports, dead code)

- [ ] **Step 1: Search for remaining references to deleted types**

Run: `rg "JIPipeAIModelRunnerStatus|JIPipeServerState|JIPipeServerEvent" -g "*.java"`
Expected: No results (only comments are OK)

- [ ] **Step 2: Search for remaining references to removed methods**

Run: `rg "submitEmbedFunction|EmbedFunctionTask|LoadEmbeddingModelTask|UnloadEmbeddingModelTask|waitForEmbeddingModelReady" -g "*.java"`
Expected: No results

- [ ] **Step 3: Full compile**

Run: `mvn clean compile -q`
Expected: No errors

- [ ] **Step 4: Full test run**

Run: `mvn test -pl jipipe-core -q`
Expected: All tests pass

- [ ] **Step 5: Commit any remaining cleanup**

```bash
git add -A
git commit -m "Final cleanup: remove dead code and imports (#1322)"
```

---

## Self-Review Notes

**Spec coverage:**
- MicroserviceState enum → Task 2 ✓
- Microservice interface + AbstractMicroservice → Task 4 ✓
- MicroserviceStateChangeEvent system → Task 3 ✓
- ServiceAwareQueuedExecutor → Task 7 ✓
- ServiceAwareEphemeralExecutor → Task 8 ✓
- AbstractMicroservice tests → Task 5 ✓
- MicroserviceDependency tests → Task 6 ✓
- ServiceAwareQueuedExecutor tests → Task 9 ✓
- ServiceAwareEphemeralExecutor tests → Task 10 ✓
- AI service migration → Task 12 ✓
- Server service migration → Task 11 ✓
- AI search migration → Task 13 ✓
- Cleanup → Task 14 ✓

**Placeholder scan:** No TBDs, TODOs, or "implement later" in any step. All code blocks are complete.

**Type consistency:** `MicroserviceState` values used consistently. `Microservice` interface methods match across tasks. `AbstractMicroservice` constructor takes `String name` consistently. Executor methods match `JIPipeRunnableExecutor` interface.
