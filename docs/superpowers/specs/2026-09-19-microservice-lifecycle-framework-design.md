# Microservice Lifecycle Framework Design

**Date:** 2026-09-19  
**Issue:** #1322 (search freezes) and general architectural improvement  
**Branch:** `1322-search-freezes`

## Problem

The AI search freeze (issue #1322) exposed a fundamental architectural flaw: the `JIPipeAIServiceComponent` uses a single `JIPipeQueuedRunnableExecutor` for **both** lifecycle management (load/unload model) **and** work execution (embed text). This conflation causes:

1. **Queue flooding** — `ensureEmbeddingsForEntries()` enqueues individual embed tasks alongside lifecycle tasks. 200 entries = 200 tasks clogging the serial queue.
2. **Deadlock risk** — Workarounds like `submitEmbedFunction` check model status inside a task but cannot trigger a load (that would enqueue behind itself on the serial queue).
3. **No selective cancellation** — When the ephemeral executor cancels the search worker, orphan embed tasks remain on the AI queue.
4. **Blocking on unstarted work** — An embed task can be dequeued before the model is loaded, forcing it to either fail or block the entire queue while polling.

The server service (`JIPipeServerServiceComponent`) has similar lifecycle patterns with its own state enum (`JIPipeServerState`), events, and ref-counted instances.

## Solution

A general microservice lifecycle framework that separates infrastructure lifecycle from work execution:

- **Microservices** manage their own lifecycle (start/stop) with a generic state model
- **Dependencies** between microservices are declared for lifecycle ordering
- **Service-aware executors** gate work dispatch on microservice readiness
- **No registry** — the framework is a library of interfaces and classes; calling code manages instances

## Design

### MicroserviceState

A single generic enum shared by all microservices:

```java
public enum MicroserviceState {
    Stopped,    // Not running, no resources held
    Starting,   // Resources being acquired / initialized
    Ready,      // Running and available for work
    Stopping,   // Resources being released
    Failed      // Startup or runtime failure (see detail string)
}
```

Key decisions:
- **No `Busy` state.** "Busy" is a transient condition during work execution, not a lifecycle state. The executor tracks whether work is running; the service stays `Ready` while processing work.
- **No `Idle` vs `Running` distinction.** Both map to `Ready`. The optional detail string conveys nuance (e.g., `"idle"` vs `"busy (3 active leases)"`).
- **`Failed` is terminal** until `start()` is called again. The detail string holds the error message.

State transitions:
```
Stopped --start()--> Starting --onStart success--> Ready
Starting --onStart failure--> Failed
Ready   --stop()-->  Stopping --onStop complete--> Stopped
Failed  --start()--> Starting (retry)
Any     --start()--> no-op if already Starting/Ready
Any     --stop()-->  no-op if already Stopped/Stopping
```

### Microservice Interface

```java
public interface Microservice {
    // Identity
    String getName();

    // Lifecycle
    void start();
    void stop();
    MicroserviceState getState();
    String getStateDetail();
    boolean isReady();

    // Dependencies
    List<Microservice> getDependencies();
    void addDependency(Microservice dependency);

    // Events
    MicroserviceStateChangeEventEmitter getStateChangeEventEmitter();
}
```

### AbstractMicroservice

Handles state transitions, dependency checking, and event emission. Subclasses implement the actual resource management:

```java
public abstract class AbstractMicroservice implements Microservice {
    private final String name;
    private volatile MicroserviceState state = MicroserviceState.Stopped;
    private volatile String stateDetail = "";
    private final List<Microservice> dependencies = new CopyOnWriteArrayList<>();
    private final MicroserviceStateChangeEventEmitter emitter = ...;

    // Public lifecycle — orchestrates state transitions
    public final synchronized void start() {
        // 1. If state is Starting or Ready, no-op
        // 2. Set state to Starting, fire event
        // 3. Check all dependencies are Ready; if not, start them first
        //    (dependency start failures → set Failed)
        //    If a dependency is Starting, wait for it to reach Ready or Failed
        //    (polling with timeout)
        // 4. Call onStart() — subclass does the actual work
        // 5. On success → set Ready, fire event; on failure → set Failed, fire event
    }

    public final synchronized void stop() {
        // 1. If state is Stopped or Stopping, no-op
        // 2. Set state to Stopping, fire event
        // 3. Call onStop() — subclass releases resources
        // 4. Set state to Stopped, fire event
    }

    public boolean isReady() {
        return state == MicroserviceState.Ready;
    }

    // Subclass hooks
    protected abstract void onStart() throws Exception;
    protected void onStop() throws Exception { }

    // Subclass can call this to set detail string
    protected void setStateDetail(String detail) { ... }

    // Subclass can call this to report failure during runtime
    protected void markFailed(String detail) { ... }
}
```

Key design decisions:
1. **`start()` and `stop()` are `final`** — they orchestrate state transitions and dependency resolution. Subclasses only implement `onStart()`/`onStop()`.
2. **Dependencies are started transitively.** When `aiService.start()` is called, it starts `serverService` and `artifactsService` first if they aren't Ready.
3. **`onStart()` throws Exception** — the base class catches it and sets `Failed`. No try/catch boilerplate in subclasses.
4. **`markFailed()`** — allows subclasses to report runtime failures (e.g., model crashes mid-operation).
5. **Thread safety** — state is `volatile`, dependencies use `CopyOnWriteArrayList`, `start()`/`stop()` are `synchronized`.
6. **Circular dependency detection** — `addDependency()` checks for cycles and rejects them.

### MicroserviceStateChangeEvent System

```
MicroserviceStateChangeEvent.java          (carries oldState, newState, source Microservice)
MicroserviceStateChangeListener.java       (onMicroserviceStateChanged(event))
MicroserviceStateChangeEventEmitter.java   (extends JIPipeEventEmitter)
```

Replaces both `JIPipeAIServiceComponent.StatusChangedEvent` and `JIPipeServerEvent`.

### ServiceAwareQueuedExecutor

Implements `JIPipeRunnableExecutor`. FIFO queue that only dispatches work when the service is `Ready`:

```java
public class ServiceAwareQueuedExecutor implements JIPipeRunnableExecutor {
    private final String name;
    private final Microservice service;
    private final Queue<JIPipeRunnableWorker> queue = new ArrayDeque<>();
    private JIPipeRunnableWorker currentlyRunningWorker = null;
    // ... event emitters (same as JIPipeQueuedRunnableExecutor)

    public ServiceAwareQueuedExecutor(String name, Microservice service) { ... }

    @Override
    public JIPipeRunnableWorker enqueue(JIPipeRunnable run) {
        // 1. Create worker, add to queue, fire enqueued event
        // 2. tryDequeue()
        return worker;
    }

    private void tryDequeue() {
        if (currentlyRunningWorker == null && !queue.isEmpty()) {
            if (!service.isReady()) {
                // Don't dispatch — subscribe to state changes;
                // when service becomes Ready, tryDequeue() is called again
                return;
            }
            currentlyRunningWorker = queue.remove();
            // Subscribe to worker finish/interrupt events
            currentlyRunningWorker.execute();
        }
    }

    // Worker completion callback
    private void onWorkerFinished(FinishedEvent event) {
        if (event.getWorker() == currentlyRunningWorker) {
            currentlyRunningWorker = null;
            tryDequeue();
        }
    }

    // Called when Microservice fires StateChanged → Ready
    private void onServiceReady() {
        tryDequeue();
    }

    @Override
    public void cancelAll() {
        // Remove all queued tasks (they haven't started)
        // Cancel currently running worker if any
    }
}
```

Key behaviors:
1. **Readiness gating** — `tryDequeue()` only dispatches when `service.isReady()`. If `Starting`, tasks pile up harmlessly. When service transitions to `Ready`, the state-change listener triggers `tryDequeue()`.
2. **No lifecycle tasks in the queue** — Load/unload are `Microservice.start()`/`stop()`, not queue tasks.
3. **Clean cancellation** — `cancelAll()` removes pending tasks before they start. No orphan tasks.
4. **Service goes down while work queued** — Tasks stay in queue. When service returns to `Ready`, they dispatch.

### ServiceAwareEphemeralExecutor

Implements `JIPipeRunnableExecutor`. Latest-wins with readiness gating:

```java
public class ServiceAwareEphemeralExecutor implements JIPipeRunnableExecutor {
    // Same fields as JIPipeEphemeralRunnableExecutor
    // Plus: Microservice reference + state-change listener

    @Override
    public JIPipeRunnableWorker enqueue(JIPipeRunnable run) {
        // 1. Cancel previous worker if running
        // 2. Set new worker as current
        // 3. If service.isReady(), execute immediately
        // 4. If not Ready, hold the worker — executes when service becomes Ready
        return worker;
    }

    private void onServiceReady() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.execute();
        }
    }
}
```

If the service isn't Ready, the latest task is held but not executed. When the service becomes Ready, it fires. If a newer task arrives before that, the held task is cancelled and replaced.

## AI Service Migration

### Before

```
JIPipeAIServiceComponent
  ├── JIPipeQueuedRunnableExecutor("AI Service")  ← mixed lifecycle + work tasks
  ├── LoadEmbeddingModelTask
  ├── UnloadEmbeddingModelTask
  ├── EmbedTextTask
  ├── EmbedFunctionTask<T>                         ← workaround for batch
  ├── startEmbeddingModelNow()
  ├── stopEmbeddingModelNow()
  ├── tryStartEmbeddingModel()
  ├── tryEmbed(text)
  ├── embedNow(text)
  └── submitEmbedFunction(fn)                      ← workaround
```

### After

```
JIPipeAIServiceComponent
  ├── EmbeddingModelService (extends AbstractMicroservice)
  │     ├── onStart()  → resolve artifact, acquire server lease or create direct runner
  │     ├── onStop()   → release lease / shut down runner
  │     └── embedNow(text) → delegate to lease/runner
  │
  ├── ServiceAwareQueuedExecutor("AI Embed", embeddingModelService)
  │     ← batch/sequential embed operations
  │
  ├── ServiceAwareEphemeralExecutor("AI Search", embeddingModelService)
  │     ← AI search (latest-wins)
  │
  ├── tryStartEmbeddingModel()  → embeddingModelService.start()
  ├── tryStopEmbeddingModel()   → embeddingModelService.stop()
  ├── embedNow(text)            → embeddingModelService.embedNow(text)
  ├── tryEmbed(text)            → enqueue on ServiceAwareQueuedExecutor
  └── submitEmbedFunction(fn)   → REMOVED
```

### EmbeddingModelService

```java
public class EmbeddingModelService extends AbstractMicroservice {
    private final JIPipeService jipipeService;
    private JIPipeServerLease<EmbeddingServerInstance> lease;
    private JIPipeEmbeddingAIModelRunner runner;
    private String modelId;

    public EmbeddingModelService(JIPipeService service) {
        super("Embedding Model");
        this.jipipeService = service;
        // Declare dependencies for lifecycle ordering.
        // Service components expose their managed microservices via getters
        // (e.g., getServerService().getServerMicroservice()).
        // If a component manages multiple microservices, it exposes the relevant one.
        addDependency(service.getServerService().getServerMicroservice());
        addDependency(service.getArtifacts().getArtifactsMicroservice());
    }

    private JIPipeService getService() { return jipipeService; }

    @Override
    protected void onStart() throws Exception {
        AIApplicationSettings settings = AIApplicationSettings.getInstance();
        EmbeddingModelEnvironment env = settings.getEmbeddingModelEnvironment();
        setStateDetail("Resolving model configuration...");
        if (env.isLoadFromArtifact()) {
            resolveAndApplyArtifactConfiguration(env);
        }
        if (env.getModelType() == EmbeddingModelType.LocalOnnx) {
            setStateDetail("Acquiring server instance...");
            lease = getService().getServerService().acquireLease(...);
            modelId = lease.getInstance().getEnvironment().deriveModelId();
        } else {
            setStateDetail("Starting direct runner...");
            runner = env.toRunner();
            runner.start();
            modelId = runner.getModelId();
        }
        setStateDetail("Ready (model: " + modelId + ")");
    }

    @Override
    protected void onStop() throws Exception {
        if (lease != null) { lease.close(); lease = null; }
        if (runner != null) { runner.shutdown(); runner = null; }
        modelId = null;
    }

    public float[] embedNow(String text) {
        if (!isReady()) throw new IllegalStateException("Not ready");
        if (lease != null && lease.isOpen()) return lease.getInstance().embed(text);
        if (runner != null) return runner.embed(text);
        throw new IllegalStateException("No model loaded");
    }

    public String getModelId() { return modelId; }
}
```

### Removed from AI service

| Removed | Why |
|---------|-----|
| `submitEmbedFunction()` | Workaround — executors handle readiness gating |
| `EmbedFunctionTask<T>` | Inner class for the workaround |
| `LoadEmbeddingModelTask` | Lifecycle is `microservice.start()` |
| `UnloadEmbeddingModelTask` | Lifecycle is `microservice.stop()` |
| `EmbedTextTask` | Simplified `DefaultJIPipeRunnable` on service-aware executor |
| `JIPipeQueuedRunnableExecutor("AI Service")` | Replaced by service-aware executors |
| `JIPipeAIModelRunnerStatus` enum | Replaced by `MicroserviceState` + detail |
| `StatusChangedEvent` / `StatusChangedEventEmitter` / `StatusChangedEventListener` | Replaced by `MicroserviceStateChangeEvent` |
| `waitForEmbeddingModelReady()` | Executors gate on readiness automatically |
| `submitEmbedFunction` workaround in `JIPipeAINodeDatabaseSearch` | Search tasks enqueue normally |

### Kept (API-compatible facades)

| Kept | Why |
|------|-----|
| `tryEmbed(text)` | Callers still use it; internally enqueues on `ServiceAwareQueuedExecutor` |
| `embedNow(text)` | Delegates to `EmbeddingModelService.embedNow()` |
| `tryStartEmbeddingModel()` / `tryStopEmbeddingModel()` | Delegate to `microservice.start()` / `stop()` |
| `getEmbeddingModelStatus()` | Returns `MicroserviceState` |
| `hasConfiguredAndReadyEmbeddingModel()` | Unchanged — checks settings |

## Server Service Migration

### Before

```
JIPipeServerServiceComponent
  ├── factories: Map<String, FactoryEntry>
  ├── activeInstances: Map<String, ManagedInstanceEntry>
  ├── PortManager
  ├── JIPipeQueuedRunnableExecutor("Server Lifecycle")
  ├── acquireLease(factoryId, envClass, env, ...)
  ├── releaseLease(lease)
  ├── releaseAll()
  └── JIPipeServerInstance (abstract)
        ├── state: JIPipeServerState (7 states)
        ├── start() / stop() / isHealthy()
        └── EmbeddingServerInstance (concrete, subprocess)
```

### After

```
JIPipeServerServiceComponent
  ├── factories: Map<String, FactoryEntry>           ← unchanged
  ├── activeInstances: Map<String, ManagedMicroserviceEntry>
  ├── PortManager                                     ← unchanged
  ├── acquireLease(...)                               ← delegates to instance.start()
  ├── releaseLease(lease)                             ← decrements ref count
  ├── releaseAll()                                    ← stops all instance microservices
  └── JIPipeServerInstance (extends AbstractMicroservice)
        ├── onStart()  → spawn process, wait for health check
        ├── onStop()   → kill process
        └── EmbeddingServerInstance (concrete)

  JIPipeServerState → REMOVED
  JIPipeServerEvent → REMOVED
```

### JIPipeServerInstance changes

```java
public abstract class JIPipeServerInstance<TEnv extends JIPipeEnvironment>
        extends AbstractMicroservice {

    @Override
    protected void onStart() throws Exception {
        setStateDetail("Starting on port " + port + "...");
        start();  // existing method — spawns process
        setStateDetail("Waiting for health check...");
        if (!waitForHealth()) {
            throw new ServerStartException(Reason.HealthCheckFailed, ...);
        }
        setStateDetail("Running on port " + port);
    }

    @Override
    protected void onStop() throws Exception {
        setStateDetail("Stopping...");
        stop();  // existing method — kills process
    }

    // Existing abstract methods stay:
    // protected abstract void start() throws Exception;
    // protected abstract void stop() throws Exception;
    // public abstract boolean isHealthy();
    // public abstract String getServerTypeId();
}
```

Note: There is a naming conflict — the existing `start()` and `stop()` methods on `JIPipeServerInstance` clash with `Microservice.start()` and `Microservice.stop()` (which are `final` on `AbstractMicroservice`). The existing methods will be renamed to `startProcess()` and `stopProcess()` (or similar), called from `onStart()` and `onStop()`.

### Removed from server service

| Removed | Why |
|---------|-----|
| `JIPipeServerState` enum | Replaced by `MicroserviceState` |
| `JIPipeServerEvent` / `EventEmitter` / `EventListener` | Replaced by `MicroserviceStateChangeEvent` |
| `JIPipeQueuedRunnableExecutor("Server Lifecycle")` | Lifecycle is `Microservice.start()`/`stop()` |
| `state` field in `JIPipeServerInstance` | Inherited from `AbstractMicroservice` |

### Kept

| Kept | Why |
|------|-----|
| `JIPipeServerLease` | Unchanged — ref-counted AutoCloseable lease |
| `JIPipeServerInstanceManager` / `JIPipeServerLeaseScope` | Unchanged |
| `PortManager` | Unchanged |
| `ProcessSupervisor` | Unchanged |
| `ServerInstanceFactory` | Unchanged |
| Factory registration via plugins | Unchanged |

## Testing Strategy

### Test structure

```
jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/
  AbstractMicroserviceTest.java
  MicroserviceDependencyTest.java
  ServiceAwareQueuedExecutorTest.java
  ServiceAwareEphemeralExecutorTest.java
```

### Test cases

**AbstractMicroserviceTest** — lifecycle correctness:
- `start()` transitions Stopped → Starting → Ready
- `start()` with failing `onStart()` transitions to Failed
- `stop()` transitions Ready → Stopping → Stopped
- `stop()` when Stopped is a no-op
- `start()` when already Ready is a no-op
- `start()` when already Starting is a no-op
- `markFailed()` transitions Ready → Failed with detail string
- `setStateDetail()` is reflected in `getStateDetail()`
- State change events are fired for every transition
- Concurrent `start()` from multiple threads is safe (only one wins)

**MicroserviceDependencyTest** — dependency resolution:
- `start()` starts dependencies first (transitive)
- If a dependency fails, the dependent goes to Failed
- If a dependency is already Ready, it's not re-started
- If a dependency is Starting, `start()` waits for it
- `stop()` does NOT stop dependencies (they may have other dependents)
- Circular dependencies are detected and rejected at `addDependency()` time

**ServiceAwareQueuedExecutorTest** — readiness gating:
- Tasks enqueue immediately but don't execute when service is not Ready
- Tasks execute when service transitions to Ready
- Tasks execute in FIFO order
- `cancelAll()` removes pending tasks without executing them
- `cancel(run)` removes a specific pending task
- `cancel(run)` cancels a running worker via interruption
- Worker completion triggers dispatch of next queued task
- Service going Stopping while tasks queued → tasks stay queued, not dispatched
- Service coming back to Ready → queued tasks dispatch
- Events (started, finished, interrupted, progress, enqueued) fire correctly
- Multiple tasks queued while service is Starting → all dispatch in order when Ready

**ServiceAwareEphemeralExecutorTest** — latest-wins + readiness:
- New task cancels previous running task
- Task doesn't execute when service is not Ready
- Task executes when service becomes Ready
- New task while service is Starting and previous task held → previous cancelled, new held
- `cancelAll()` cancels running worker and clears held task
- Superseded worker events are silently discarded

### Test infrastructure

Tests use plain JUnit 5 with no JIPipe initialization needed — the microservice framework has no dependency on `JIPipe.getInstance()` or SciJava context. A simple `TestService` inner class extends `AbstractMicroservice` with controllable behavior.

### Maven configuration

Add JUnit 5 to `jipipe-core/pom.xml` test scope:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

## File Inventory

### New files

```
jipipe-core/src/main/java/org/hkijena/jipipe/api/microservice/
  Microservice.java
  AbstractMicroservice.java
  MicroserviceState.java
  MicroserviceStateChangeEvent.java
  MicroserviceStateChangeListener.java
  MicroserviceStateChangeEventEmitter.java
  ServiceAwareQueuedExecutor.java
  ServiceAwareEphemeralExecutor.java

jipipe-core/src/test/java/org/hkijena/jipipe/api/microservice/
  AbstractMicroserviceTest.java
  MicroserviceDependencyTest.java
  ServiceAwareQueuedExecutorTest.java
  ServiceAwareEphemeralExecutorTest.java
```

### Deleted files

```
JIPipeServerState.java
JIPipeServerEvent.java
JIPipeServerEventEmitter.java
JIPipeServerEventListener.java
JIPipeAIModelRunnerStatus.java
```

### Modified files

- `jipipe-core/pom.xml` — add JUnit 5
- `JIPipeAIServiceComponent.java` — replace mixed queue with microservice + service-aware executors
- `JIPipeAINodeDatabaseSearch.java` — remove workarounds, use service-aware ephemeral executor
- `JIPipeServerInstance.java` — extend `AbstractMicroservice`
- `EmbeddingServerInstance.java` — rename `start()`/`stop()` to avoid clash with final lifecycle methods
- `JIPipeServerServiceComponent.java` — use `Microservice.start()`/`stop()`
- All AI monitor UI components — update to `MicroserviceStateChangeEvent`
- Any file referencing `JIPipeAIModelRunnerStatus` or `JIPipeServerState`

## Implementation Phases

1. **Framework + tests** — Create microservice interfaces, base class, executors, and tests. No existing code changes. Verify tests pass.

2. **Server service migration** — Migrate `JIPipeServerInstance` to extend `AbstractMicroservice`. Delete `JIPipeServerState` and related event classes. Update `JIPipeServerServiceComponent`. Compile and verify.

3. **AI service migration** — Create `EmbeddingModelService`. Replace mixed queue with service-aware executors. Delete old task classes and `JIPipeAIModelRunnerStatus`. Update AI monitor UI components. Compile and verify.

4. **AI search migration** — Refactor `JIPipeAINodeDatabaseSearch` to use `ServiceAwareEphemeralExecutor`. Remove `submitEmbedFunction` workaround and single-task pattern. Compile and verify.

5. **Cleanup** — Remove dead code, verify no remaining references to deleted types. Full `mvn compile` and `mvn test`.

## Relationship to SciJava

The framework is plain Java with no SciJava annotations or dependencies. SciJava remains the outer layer for plugin discovery and dependency injection. `JIPipeService` (a SciJava service) uses the framework internally — service components that need lifecycle management wrap `Microservice` instances. The framework does not register itself with SciJava or expose SciJava services.

Node-local services (created during pipeline runs) use the framework without any registration — the calling code (the node's `run()` method) creates and manages the `Microservice` instance directly.
