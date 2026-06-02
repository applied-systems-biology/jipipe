# Investigation: External Server Process Management in JIPipe

**Date:** 2026-06-01  
**Status:** Investigation (no code changes)

---

## 1. Executive Summary

This report investigates how to add external server process management to JIPipe, supporting use cases like llama.cpp LLM servers, IPython/Jupyter servers, and other long-running external processes. The key challenges are: (1) coordinating server lifecycle across multiple JIPipe JVM instances, (2) ensuring servers only shut down when all consumers are gone, and (3) supporting all runtime configurations (IDE debug, launcher, Fiji plugin).

**Key finding:** JIPipe currently has **no infrastructure for persistent/shared external processes**. All external tool invocations follow a fire-and-forget pattern (spawn process, wait for completion, read output files). The existing `FileLocker`, `JIPipeAIServiceComponent`, and `JIPipeProcessArtifactEnvironment` patterns provide solid foundations to build upon, but significant new coordination infrastructure is required.

---

## 2. Current State Analysis

### 2.1 Application Lifecycle

| Aspect | Details |
|--------|---------|
| **Entry points** | `JIPipeLauncher.main()` (launcher), `JIPipeGUICommand.run()` (Fiji plugin), `JIPipeRunAlgorithmCommand.run()` (Fiji headless) |
| **Shutdown** | `JIPipe.exitLater()` saves settings, calls `JIPipeService.dispose()`, then **`Runtime.getRuntime().halt()`** after 500ms |
| **Shutdown hooks** | Only one exists (`JIPipeAIServiceComponent`, line 54) -- it will **never execute** during GUI exit because `halt()` bypasses shutdown hooks |
| **Service states** | `JIPipeServiceState` defines `ShuttingDown`/`Shutdown` but they are **never used** |
| **Multi-instance** | **No prevention or coordination**. Multiple JIPipe JVMs can run simultaneously with no awareness of each other |

**Critical constraint:** The use of `Runtime.getRuntime().halt()` in `JIPipe.exitLater()` (`JIPipe.java:446`) means any cleanup code must be invoked **before** `halt()`, not via shutdown hooks. The comment explains: *"Context introduces a shutdown hook that causes a deadlock"* -- this is a SciJava `Context` issue.

### 2.2 Process Management

JIPipe has a mature process execution infrastructure but it is entirely **synchronous and per-invocation**:

| Component | File | Purpose |
|-----------|------|---------|
| `ProcessUtils` | `jipipe-core/.../utils/ProcessUtils.java` | Central execution: `runProcess()` (blocking), `launchProcess()` (detached), `queryFast()` (one-shot) |
| `ExtendedExecutor` | `jipipe-core/.../utils/process/ExtendedExecutor.java` | Apache Commons Exec wrapper with PID tracking |
| `ProcessTree` | `jipipe-core/.../utils/process/ProcessTree.java` | Process wrapper with tree-aware `destroy()` |
| `RunCancellationWatchdog` | `jipipe-core/.../utils/process/RunCancellationWatchdog.java` | Polls `progressInfo.isCancelled()` every 500ms |
| `ProcessSidecarTask` | `jipipe-core/.../utils/process/ProcessSidecarTask.java` | Periodic monitoring hook (infrastructure exists but **unused**) |
| `ProcessUtils.killProcessTree()` | `jipipe-core/.../utils/ProcessUtils.java:289` | Kills process + all descendants (Windows: `taskkill /F /PID /T`, Unix: `ps` + BFS + `kill -9`) |

**Current pattern for every external tool call:**
1. Create temp directories for input/output
2. Write input data (files + `data-table.json`)
3. Spawn process via `ProcessUtils.runProcess()` (blocking)
4. Wait for completion
5. Read output from temp directories
6. Clean up

**What is missing:**
- No persistent/long-lived process management
- No port allocation or discovery
- No health checking of running processes
- No process pooling or reuse
- No socket-based IPC (all IPC is file-based)
- No inter-JVM coordination mechanism

### 2.3 Environment Configuration System

JIPipe has a sophisticated environment resolution chain that can be extended for server environments:

```
Node override → Project override → Application setting → Fallback (artifact default)
```

Key classes:
- `JIPipeEnvironment` -- abstract base (`JIPipeEnvironment.java`)
- `JIPipeArtifactEnvironment` -- adds artifact-based binary distribution (`JIPipeArtifactEnvironment.java`)
- `JIPipeProcessArtifactEnvironment` -- adds executable path + arguments + env vars (`JIPipeProcessArtifactEnvironment.java`)
- `JIPipeEnvironmentConfigurator` -- chain-of-responsibility resolution (`JIPipeEnvironmentConfigurator.java`)
- `PythonEnvironment`, `REnvironment`, `IlastikEnvironment`, etc. -- concrete implementations

The artifact system already handles OS/arch-specific binary distribution (e.g., `cn.ac.baai.bge_small_en:1.0.0-linux_amd64`). This is directly reusable for distributing server binaries like llama.cpp.

### 2.4 Service Component Pattern

`JIPipeAIServiceComponent` (`JIPipeAIServiceComponent.java`) is the closest existing pattern to a server manager:

- **Task queue serialization** via `JIPipeRunnableQueue` -- prevents concurrent state mutations
- **Status state machine**: `Unloaded → Loading → Idle ↔ Busy → Unloading → Unloaded` (or `Failed`)
- **Event system**: `StatusChangedEventEmitter` notifies UI
- **Async API**: `tryEmbed()` returns `CompletableFuture<float[]>`, auto-starts model if needed
- **Artifact resolution**: Downloads model artifacts on first use

However, it manages an **in-process** model (ONNX) or an external **HTTP API client** -- not a process that JIPipe spawns. The `JIPipeAPIEmbeddingAIModelRunner` assumes the server is already running elsewhere.

### 2.5 Directory and File Locations

| Directory | Path | Set By |
|-----------|------|--------|
| User profile | OS-specific (`~/.local/share/JIPipe/profiles/<version>` on Linux) | `PathUtils.getJIPipeUserDir()` |
| Shared dir | OS-specific (`~/.local/share/JIPipe/shared` on Linux) | `PathUtils.getJIPipeSharedDir()` |
| Artifacts | OS-specific (`~/.local/share/JIPipe/artifacts` on Linux) | `JIPipeArtifactsServiceComponent` |
| Temp dir | `java.io.tmpdir` or user-configured | `JIPipe.getTemporaryBaseDirectory()` |
| ImageJ dir | `imagej.dir` system property | `PathUtils.getImageJDir()` |

All paths support environment variable overrides (`JIPIPE_OVERRIDE_USER_DIR_BASE`, etc.) and CLI flags (`--profile-dir`).

**Existing file locking:** `FileLocker` (`FileLocker.java`) uses `java.nio.channels.FileLock` for inter-process mutual exclusion. Currently used for:
- Project temp directory locks
- Artifact repository locks
- Cleanup service coordination

### 2.6 Launch Configurations

| Mode | Entry | ImageJ Context | Exit Method |
|------|-------|----------------|-------------|
| IDE debug | `JIPipeGUICommand.main()` | `new ImageJ()` | Window close → `exitLater(0)` → `halt(0)` |
| Launcher GUI | `JIPipeLauncher.main()` → `GuiCommand` | `new ImageJ()` | Window close → `exitLater(0)` → `halt(0)` |
| Launcher headless | `JIPipeLauncher.main()` → `HeadlessRunCommand` | `new ImageJ()` | `halt(0)` after pipeline |
| Fiji plugin | Fiji menu → `JIPipeGUICommand.run()` | Existing Fiji context | Window close → `exitLater(0)` → `halt(0)` |
| Fiji headless | Fiji menu → `JIPipeRunAlgorithmCommand.run()` | Existing Fiji context | `finally` block cleanup |

**Key difference in production (Fiji):** JIPipe runs within an existing Fiji/ImageJ context. The JVM is shared with ImageJ and potentially other plugins. JIPipe cannot assume it controls the entire JVM lifecycle.

---

## 3. Technical Challenges

### 3.1 The `halt()` Problem

`JIPipe.exitLater()` uses `Runtime.getRuntime().halt()` instead of `System.exit()` to avoid a deadlock in SciJava's `Context` shutdown hook. This means:

- **Shutdown hooks do not run** -- the only existing one (`JIPipeAIServiceComponent` line 54) is dead code during GUI exit
- **No `finally` blocks execute** after `halt()` -- it terminates the JVM immediately
- **Signal handlers (`sun.misc.Signal`)** are also bypassed by `halt()`

**Implication:** Server cleanup must be explicitly invoked **before** `halt()` in the `exitLater()` method, not registered as a shutdown hook.

### 3.2 No Inter-JVM Coordination

There is currently zero mechanism for JIPipe JVM instances to communicate or coordinate:
- No application-level lock file
- No shared state directory
- No socket-based coordination
- No PID file tracking

The only cross-instance mechanism is `FileLocker` on the artifact repository, but it's used for brief mutual exclusion during artifact operations, not for long-lived coordination.

### 3.3 Fiji Plugin Mode Constraints

When running as a Fiji plugin:
- JIPipe shares a JVM with ImageJ and other plugins
- Closing the JIPipe window does **not** exit the JVM (ImageJ keeps running)
- Starting/stopping a server must be scoped to JIPipe's needs, not the entire JVM
- The server might be needed by non-JIPipe Fiji components (or vice versa)

### 3.4 Cross-Platform Portability

The solution must work on Windows, Linux, and macOS:
- `FileLock` behavior differs (advisory on Linux/macOS, mandatory on Windows)
- PID-based process detection works differently per OS
- Unix domain sockets require Java 16+ (available since JIPipe requires Java 21)
- Named pipes are not portable in Java

### 3.5 Orphaned Server Processes

If a JIPipe JVM crashes (or is killed with `kill -9`), no cleanup code runs. A shared server process could be left running with no consumers. The system must detect and clean up orphaned servers.

---

## 4. Proposed Architecture

### 4.1 Core Concept: Managed External Server

A **managed external server** is a long-running process (e.g., llama.cpp, IPython) that:
- Is spawned and owned by one or more JIPipe instances
- Communicates via HTTP/WebSocket on a local port
- Has a well-defined startup, health-check, and shutdown protocol
- Can be shared across JIPipe instances (with reference counting)
- Can be distributed as a JIPipe artifact (OS/arch-specific binary)

### 4.2 Server State Directory

Each server type gets a **state directory** in the JIPipe shared directory for inter-JVM coordination:

```
<JIPipeSharedDir>/servers/<server-type>/
├── server.lock          # FileLock for exclusive server start/stop operations
├── server.info          # PID:port:startedAt (service discovery)
├── refs/                # One file per consuming JVM (reference counting)
│   ├── 12345.ref        # Contains heartbeat timestamp
│   └── 67890.ref
└── config.json          # Server configuration snapshot
```

**Why `JIPipeSharedDir`?** Because it's version-independent (unlike the profile directory which is versioned) and specifically designed for data shared across JIPipe instances. It's accessible from all launch modes.

### 4.3 Server Lifecycle State Machine

```
                    ┌──────────────┐
                    │   NotRunning  │
                    └──────┬───────┘
                           │ acquire() called
                           ▼
                    ┌──────────────┐
              ┌────▶│   Starting   │
              │     └──────┬───────┘
              │            │ server responds to health check
              │            ▼
              │     ┌──────────────┐
              │     │    Running   │◀──── other JVMs discover via server.info
              │     └──────┬───────┘
              │            │ last ref released
              │            ▼
              │     ┌──────────────┐
              │     │  Stopping    │
              │     └──────┬───────┘
              │            │ server process terminated
              │            ▼
              │     ┌──────────────┐
              └─────│  NotRunning  │
                    └──────────────┘

              (also: Failed state from any transition)
```

### 4.4 Reference Counting Protocol

**Acquiring a server (each JVM):**

1. **Read `server.info`** -- check if a server is already running
2. **Validate** -- check PID via `ProcessHandle.of(pid).isAlive()` AND probe the port
3. **If orphaned** -- kill the orphaned process, clean up state directory
4. **If not running** -- acquire `server.lock`, start the server process, write `server.info`, release lock
5. **Register as a consumer** -- create `refs/<mypid>.ref` with current timestamp
6. **Start heartbeat** -- periodically update `refs/<mypid>.ref` timestamp (e.g., every 5 seconds)

**Releasing a server (each JVM on shutdown):**

1. **Delete `refs/<mypid>.ref`** -- unregister as a consumer
2. **Scan `refs/` directory** -- check each PID via `ProcessHandle.of(pid).isAlive()`
3. **If no live consumers remain** -- send SIGTERM/ctrl+C to the server process
4. **If other consumers exist** -- leave the server running

**Ownership model:**

| Server Type | Ownership | Shutdown Condition |
|-------------|-----------|-------------------|
| App-owned | Single JIPipe instance | Owning instance exits or explicitly stops it |
| Shared | All JIPipe instances | All consuming instances have exited or released |

### 4.5 Integration with Existing `exitLater()`

The server cleanup must be integrated into `JIPipe.exitLater()` **before** `halt()`:

```java
// In JIPipe.exitLater() -- proposed insertion point (before halt)
if (instance != null) {
    instance.getServerManager().releaseAll();
}
```

This ensures cleanup runs even though `halt()` will skip shutdown hooks.

For **Fiji plugin mode** (where `exitLater()` is not called on window close), the cleanup should be triggered by `JIPipeDesktopProjectWindow.dispose()` or a dedicated lifecycle callback.

### 4.6 Health Checking

Use the existing (but unused) `ProcessSidecarTask` infrastructure:

- `PeriodicProcessSidecarTask` already provides a periodic `tick()` callback
- A new `ServerHealthCheckSidecar` could periodically probe the server's HTTP health endpoint
- If the server becomes unresponsive, trigger a restart

Alternatively, for simpler integration, use a dedicated `ScheduledExecutorService` daemon thread that probes the server periodically.

### 4.7 Server Environment Configuration

Extend the existing environment system:

```
JIPipeEnvironment (abstract base)
  → JIPipeArtifactEnvironment (adds artifact-based binary distribution)
    → JIPipeServerEnvironment (NEW - adds server-specific config)
      → LlamaCppServerEnvironment (llama.cpp-specific)
      → IPythonServerEnvironment (IPython-specific)
      → JupyterServerEnvironment (Jupyter-specific)
```

`JIPipeServerEnvironment` would add:
- `port` (int, 0 = auto-select)
- `startupArgs` (expression-based arguments)
- `healthCheckEndpoint` (String, e.g., `/health`)
- `startupTimeoutMs` (long)
- `ownership` (enum: `AppOwned`, `Shared`)
- `autoStart` (boolean)

### 4.8 Server Manager Service Component

A new `JIPipeServerManagerServiceComponent` in `JIPipeService`:

- Manages all active server connections across the application
- Implements the reference counting protocol
- Provides `acquire(serverId, environment)` and `release(serverId)` API
- Handles orphan detection on startup
- Integrates with `JIPipeService.dispose()` for cleanup
- Uses `JIPipeRunnableQueue` for serialized lifecycle operations (following `JIPipeAIServiceComponent` pattern)

---

## 5. Detailed Technical Design

### 5.1 Inter-JVM Coordination: File-Based with ProcessHandle

**Recommended approach:** File-based coordination using the state directory pattern, with `ProcessHandle` for PID liveness checks.

**Rationale:**
- `FileLock` is already used in JIPipe and works across all platforms
- `ProcessHandle` (Java 9+) provides reliable cross-platform PID liveness checking
- No external library dependencies needed
- Unix domain sockets (Java 16+) are an option for Java 21 but add complexity for marginal benefit

**Not recommended:**
- Socket-based coordination (port conflicts, firewall issues)
- Shared memory (no cross-JVM atomics in Java, complexity)
- Named pipes (poor Java support)
- External coordination libraries (over-engineering)

### 5.2 Heartbeat Mechanism

Each JVM writes a heartbeat to its ref file every N seconds:

```
refs/12345.ref → {"pid": 12345, "lastHeartbeat": "2026-06-01T10:30:00Z", "jvmId": "..."}
```

When checking if a JVM is alive:
1. First check `ProcessHandle.of(pid).isAlive()` (fast, reliable)
2. Fallback: check heartbeat staleness (survives `ProcessHandle` edge cases on some OSes)
3. If both indicate dead, consider the JVM gone and clean up its ref file

**Why both?** `ProcessHandle` can sometimes return incorrect results on Windows when the process runs as a different user. The heartbeat provides a belt-and-suspenders check.

### 5.3 Port Allocation

For servers that need a dynamically allocated port:

1. Bind to port `0` (OS assigns a free port)
2. Write the assigned port to `server.info`
3. Other JVMs read `server.info` to discover the port

For servers with a configured fixed port:
1. Check if the port is already in use (try connecting to it)
2. If in use and the server.info matches, reuse it
3. If in use but no server.info (another application), report an error

**Utility to add:** `NetworkUtils.findFreePort()` -- bind a `ServerSocket` to port 0, get the assigned port, close the socket. This is the standard Java pattern.

### 5.4 Server Startup Protocol

```
JVM Instance A                          Server Process                   JVM Instance B
     │                                       │                               │
     │  1. Acquire server.lock               │                               │
     │  2. Read server.info → not found       │                               │
     │  3. Spawn process ──────────────────▶ │                               │
     │  4. Wait for health check              │                               │
     │                        ◀── HTTP 200 ── │                               │
     │  5. Write server.info                  │                               │
     │  6. Create refs/<pidA>.ref             │                               │
     │  7. Release server.lock                │                               │
     │  8. Start heartbeat thread             │                               │
     │                                       │                               │
     │                                       │    9. Read server.info ────────│
     │                                       │    10. Validate PID alive      │
     │                                       │    11. Probe port → OK         │
     │                                       │    12. Create refs/<pidB>.ref  │
     │                                       │    13. Start heartbeat thread  │
```

### 5.5 Server Shutdown Protocol

```
JVM Instance A exits                      Server Process                   JVM Instance B
     │                                       │                               │
     │  1. Delete refs/<pidA>.ref            │                               │
     │  2. Scan refs/ → only pidB.ref        │                               │
     │  3. pidB is alive → don't stop server │                               │
     │  4. halt(0)                           │                               │
     │                                       │                               │
     │                              (server keeps running)                  │
     │                                       │                               │
     │                                       │    5. Read server.info         │
     │                                       │    6. Server still healthy     │
     │                                       │    7. Continue using server    │
```

```
JVM Instance B exits                      Server Process
     │                                       │
     │  1. Delete refs/<pidB>.ref            │
     │  2. Scan refs/ → empty                │
     │  3. No live consumers → stop server   │
     │  4. Send SIGTERM ──────────────────▶ │
     │                        ◀── exits ──── │
     │  5. Delete server.info                │
     │  6. Clean up state directory          │
     │  7. halt(0)                           │
```

### 5.6 Orphan Detection and Cleanup

On every `acquire()` call (before starting a new server):

1. Read `server.info` -- get PID and port
2. Check `ProcessHandle.of(pid).isAlive()`:
   - If dead → orphaned. Kill the process tree (belt-and-suspenders), delete `server.info`, clean up refs
   - If alive → probe the port
3. Probe port:
   - If not responding → server is hung. Kill the process tree, delete `server.info`, clean up refs
   - If responding → server is healthy. Reuse it.

Also run orphan cleanup on JIPipe startup (in a service component `postprocess()`), to handle the case where JIPipe was previously killed and left a server running.

### 5.7 App-Owned vs Shared Servers

**App-owned** (e.g., a per-instance IPython kernel):
- State directory: `<JIPipeUserDir>/servers/<server-type>-<instance-uuid>/`
- Not visible to other JIPipe instances
- Shut down when the owning instance exits
- Simpler lifecycle: no ref counting needed, just start/stop

**Shared** (e.g., a llama.cpp server shared across instances):
- State directory: `<JIPipeSharedDir>/servers/<server-type>/`
- Visible to all JIPipe instances on the same machine
- Reference counting via `refs/` directory
- Only shut down when all consumers are gone

The `ownership` parameter in `JIPipeServerEnvironment` determines which path is used.

### 5.8 Integration Points

#### A. `JIPipeService` -- Add Server Manager Component

```java
// In JIPipeService constructor
serverManager = new JIPipeServerManagerServiceComponent(this);

// Add to components array
this.components = new JIPipeServiceComponent[] {
    ..., serverManager
};
```

#### B. `JIPipe.exitLater()` -- Add Server Cleanup

```java
public static void exitLater(int exitCode) {
    if (instance != null && instance.isAutosaveSettings()) {
        instance.getApplicationSettings().save();
    }
    // NEW: Release all server references
    if (instance != null) {
        instance.getServerManager().releaseAll();
    }
    if (instance != null) {
        instance.dispose();
    }
    Timer timer = new Timer(500, e -> {
        Runtime.getRuntime().halt(exitCode);
    });
    timer.start();
}
```

#### C. `JIPipeService.dispose()` -- Ensure Server Cleanup

```java
@Override
public void dispose() {
    super.dispose();
    // NEW: Release servers before unloading plugins
    getServerManager().releaseAll();
    // Unload all plugins (existing code)
    for (String activatedPluginId : getPlugins().getActivatedPlugins()) {
        ...
    }
}
```

#### D. `JIPipePlugin.dispose()` -- Per-Plugin Server Cleanup

Plugins that manage their own servers (e.g., an AI plugin managing llama.cpp) can register servers with the `JIPipeServerManagerServiceComponent` and they will be cleaned up automatically via `releaseAll()`. Alternatively, plugins can stop their servers in their own `dispose()` method.

#### E. Environment Pre/Post Hooks -- Per-Pipeline Server Management

`JIPipeEnvironment.runPreconfigure()` and `runPostprocessing()` can be used to auto-start/stop servers per pipeline run:

```java
public class LlamaCppServerEnvironment extends JIPipeServerEnvironment {
    @Override
    public void runPreconfigure(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {
        JIPipe.getInstance().getServerManager().acquire("llama-cpp", this);
    }

    @Override
    public void runPostprocessing(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {
        // Don't release here -- let the instance-level cleanup handle it
        // Or release if this is a per-pipeline server
    }
}
```

#### F. Algorithm Nodes -- Use Server During Execution

Algorithm nodes that need a server would resolve the environment and use the server's HTTP API:

```java
public class LlamaCppInferenceAlgorithm extends JIPipeIteratingAlgorithm {
    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        LlamaCppServerEnvironment env = getEnvironmentOrDefault(LlamaCppServerEnvironment.class);
        JIPipeManagedServer server = JIPipe.getInstance().getServerManager().acquire("llama-cpp", env);
        int port = server.getPort();
        // Make HTTP request to localhost:port
        // ...
    }
}
```

---

## 6. Use Case Designs

### 6.1 llama.cpp Server for LLM

**Server type:** Shared (expensive to start, benefits from reuse)  
**Communication:** HTTP API (OpenAI-compatible `/v1/chat/completions`, `/v1/embeddings`)  
**Configuration:**
- Model path (from artifact or local)
- GPU layers (`-ngl` flag)
- Context size (`-c` flag)
- Port (0 = auto, or fixed)

**State directory:** `<JIPipeSharedDir>/servers/llama-cpp/`

**Startup:** `llama-server -m <model> --port <port> -ngl <layers> -c <context>`  
**Health check:** `GET http://localhost:<port>/health` → 200 OK  
**Shutdown:** SIGTERM (graceful) or ctrl+C signal

**Integration with existing AI service:** The `JIPipeAPIEmbeddingAIModelRunner` could be extended to auto-start a llama.cpp server if configured, instead of requiring a pre-running external API.

### 6.2 IPython/Jupyter Server

**Server type:** App-owned or Shared (depends on use case)  
**Communication:** HTTP API (IPython kernel protocol via WebSocket, or REST API for Jupyter)  
**Configuration:**
- Python environment (`PythonEnvironment` reference)
- IPython/Jupyter installation (from artifact or pip)
- Port (0 = auto)
- Notebook directory (optional)

**State directory:** `<JIPipeSharedDir>/servers/ipython/` (shared) or `<JIPipeUserDir>/servers/ipython-<uuid>/` (app-owned)

**Startup:** `python -m IPython kernel --json` or `jupyter notebook --no-browser --port <port>`  
**Health check:** HTTP GET on the notebook server, or kernel info request  
**Shutdown:** SIGTERM or API request

**Benefit over current approach:** Currently every Python algorithm node spawns a new Python process per invocation. A persistent IPython kernel would eliminate Python startup overhead and allow state preservation between calls.

### 6.3 Generic External Process Server

**Server type:** Configurable (App-owned or Shared)  
**Communication:** HTTP, WebSocket, or custom protocol  
**Configuration:**
- Executable path (from artifact or local)
- Arguments (expression-based)
- Port argument pattern (how to pass the port to the process)
- Health check URL pattern
- Startup timeout

This could be a generic `JIPipeGenericServerEnvironment` that allows users to configure any HTTP-speaking server process.

---

## 7. Risk Analysis

### 7.1 Race Conditions

**Risk:** Two JVMs try to start the same server simultaneously.  
**Mitigation:** `FileLock` on `server.lock` provides mutual exclusion during the start operation. The lock is held only during the start sequence (not while the server runs).

**Risk:** A JVM exits between creating its ref file and starting the heartbeat.  
**Mitigation:** The `refs/<pid>.ref` file includes a creation timestamp. Other JVMs use `ProcessHandle` to verify PID liveness, not just the file's existence.

### 7.2 Stale State

**Risk:** Stale `server.info` or ref files after a JVM crash.  
**Mitigation:** Orphan detection on every `acquire()` call verifies the PID is alive AND the port is reachable. Stale files are cleaned up automatically.

**Risk:** Stale `FileLock` after a JVM crash.  
**Mitigation:** OS-level `FileLock` is released automatically when a process dies. This is guaranteed by the OS.

### 7.3 Port Conflicts

**Risk:** The dynamically allocated port is taken by another application between allocation and server startup.  
**Mitigation:** Use the server's own port allocation (let the server bind to port 0 and report back), or retry with a new port if binding fails.

### 7.4 Security

**Risk:** Malicious process connects to the server port.  
**Mitigation:** Bind to `127.0.0.1` (loopback only). Servers should not listen on public interfaces.

**Risk:** Another user on a shared machine starts a server that JIPipe connects to.  
**Mitigation:** The `server.info` file includes the PID. JIPipe can verify that the process at that PID is actually the expected server (via `ProcessHandle.info().command()` on supported platforms).

### 7.5 Fiji Plugin Mode

**Risk:** In Fiji plugin mode, ImageJ keeps running after JIPipe windows close. Server cleanup must not be tied to JVM shutdown.  
**Mitigation:** Clean up in `JIPipeDesktopProjectWindow.dispose()` and `JIPipeService.dispose()`, not in shutdown hooks.

**Risk:** Multiple JIPipe instances in the same Fiji JVM (if a user launches JIPipe twice from Fiji menu).  
**Mitigation:** The `JIPipeGUICommand.run()` already checks `JIPipe.isInstantiated()` and reuses the existing instance. This is unlikely but should be handled gracefully.

### 7.6 Performance

**Risk:** Heartbeat file writes every 5 seconds add I/O overhead.  
**Mitigation:** Writes are tiny (< 100 bytes) and to the local filesystem. Negligible impact.

**Risk:** Server startup time (especially for LLM model loading) could be 30+ seconds.  
**Mitigation:** Auto-start servers proactively (during splash screen or on first use with progress indicator). Cache the running server across pipeline runs.

---

## 8. Implementation Roadmap

### Phase 1: Core Infrastructure
- `JIPipeServerManagerServiceComponent` -- central server lifecycle manager
- `JIPipeManagedServer` -- represents a running server instance
- `JIPipeServerState` -- state directory coordination (file-based ref counting, PID tracking, orphan detection)
- Integration with `JIPipe.exitLater()` and `JIPipeService.dispose()`
- `NetworkUtils.findFreePort()` utility

### Phase 2: Server Environment Framework
- `JIPipeServerEnvironment` -- abstract base extending `JIPipeArtifactEnvironment`
- `JIPipeServerOwnership` enum (`AppOwned`, `Shared`)
- Environment configurator support for server environments
- Health check infrastructure (using `ProcessSidecarTask` or dedicated scheduler)

### Phase 3: Concrete Server Implementations
- `LlamaCppServerEnvironment` -- llama.cpp server
- `IPythonServerEnvironment` -- IPython kernel
- Algorithm nodes that use these environments

### Phase 4: UI Integration
- Server status panel in JIPipe desktop (showing running servers, their consumers)
- Server management in application settings
- Progress indicator during server startup

---

## 9. Alternative Approaches Considered

### 9.1 Coordination Server (Separate JVM)

A separate long-running coordination process that manages all servers. JVMs connect to it via Unix domain socket or TCP.

**Pros:** Survives `halt()`, clean lifecycle separation.  
**Cons:** Extra process to manage, bootstrapping problem (who starts the coordinator?), added complexity.  
**Verdict:** Over-engineered for the current requirements. The file-based approach is simpler and sufficient.

### 9.2 Unix Domain Socket Coordination

Use Java 21's built-in Unix domain socket support for real-time coordination between JVMs.

**Pros:** Immediate detection of JVM death (socket close), lower latency.  
**Cons:** Additional complexity, Windows 10+ only for Unix domain sockets, need to handle the "leader election" problem.  
**Verdict:** Consider as an optional enhancement in Phase 4, but not required for the core implementation.

### 9.3 SQLite Coordination Store

Use an SQLite database in the shared directory for coordination (JVM registration, heartbeat, server state).

**Pros:** Transactional guarantees, no custom locking code, well-understood concurrency model.  
**Cons:** Additional dependency (sqlite-jdbc), SQLite locking can be fragile on NFS, added complexity.  
**Verdict:** File-based coordination is simpler and JIPipe already has `FileLocker`. SQLite adds a dependency without compelling benefits.

### 9.4 No Shared Servers (App-Owned Only)

Each JIPipe instance starts its own server, no sharing.

**Pros:** Simpler lifecycle, no coordination needed.  
**Cons:** Wasteful of resources (especially GPU memory for LLMs), slower startup.  
**Verdict:** Keep as an option via the `AppOwned` ownership mode, but shared servers are a key requirement.

---

## 10. Key Files Reference

| File | Relevance |
|------|-----------|
| `jipipe-core/.../JIPipe.java:435-449` | `exitLater()` -- must integrate server cleanup here |
| `jipipe-core/.../api/service/JIPipeService.java:83-110` | Service component registration -- add `JIPipeServerManagerServiceComponent` |
| `jipipe-core/.../api/service/JIPipeService.java:114-127` | `dispose()` -- must release servers before plugin disposal |
| `jipipe-core/.../api/service/JIPipeServiceComponent.java` | Base class for new service component |
| `jipipe-core/.../api/service/components/JIPipeAIServiceComponent.java` | Pattern reference for task queue + status lifecycle |
| `jipipe-core/.../utils/ProcessUtils.java` | Process execution infrastructure (reuse for server startup) |
| `jipipe-core/.../utils/process/ProcessTree.java` | Process tree management (reuse for server shutdown) |
| `jipipe-core/.../utils/process/ProcessSidecarTask.java` | Health check infrastructure (currently unused, repurpose) |
| `jipipe-core/.../utils/FileLocker.java` | File-based locking (extend for server state directory) |
| `jipipe-core/.../utils/PathUtils.java:380-406` | `getJIPipeSharedDir()` -- base for server state directories |
| `jipipe-core/.../api/environments/JIPipeProcessArtifactEnvironment.java` | Pattern for executable-based artifact environments |
| `jipipe-core/.../api/environments/JIPipeEnvironmentConfigurator.java` | Environment resolution chain (extend for server environments) |
| `jipipe-core/.../api/ai/JIPipeAPIEmbeddingAIModelRunner.java` | HTTP API client pattern (extend for server health checks) |
| `jipipe-core/.../plugins/python/PythonEnvironment.java` | Python environment (base for IPython server environment) |
| `jipipe-core/.../utils/NetworkUtils.java` | Network utilities (add `findFreePort()` here) |
| `jipipe-desktop/.../JIPipeDesktopProjectWindow.java:179-184` | Window dispose -- alternative cleanup trigger for Fiji mode |
