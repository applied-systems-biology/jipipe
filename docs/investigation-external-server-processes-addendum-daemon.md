# Addendum: Dedicated Server Manager Architecture

**Date:** 2026-06-01  
**Status:** Investigation (no code changes)  
**Extends:** `investigation-external-server-processes.md`

---

## 1. Executive Summary

This addendum evaluates an alternative to the direct file-based coordination approach described in the main report: a **dedicated server manager** that acts as an intermediary between JIPipe instances and external processes. The manager runs as a lightweight daemon process, owns all server lifecycle decisions, and provides a library API for concrete implementations.

**Key insight:** A dedicated daemon process completely sidesteps the `Runtime.getRuntime().halt()` problem from the main report. The daemon survives JIPipe crashes, can detect client disconnection immediately (not via stale heartbeat files), and serves as a single source of truth for all coordination. The tradeoff is a bootstrapping problem (starting the daemon) and the overhead of an additional JVM process.

**Recommendation:** Adopt a **hybrid architecture** where the server manager is a library that can operate in two modes: **embedded** (in-process, for simple app-owned servers) and **daemon** (separate JVM, for shared servers that must survive client crashes). This gives the best of both worlds.

---

## 2. Problem with the File-Based Approach

The main report proposes file-based coordination using `server.info`, `refs/`, and `FileLocker`. While portable and dependency-free, this approach has fundamental limitations:

### 2.1 The `halt()` Problem Is Not Fully Solved

`JIPipe.exitLater()` calls `Runtime.getRuntime().halt()`, which:
- Bypasses shutdown hooks
- Bypasses `finally` blocks
- Bypasses signal handlers

The main report proposes integrating cleanup **before** `halt()` in `exitLater()`. This works for **graceful** shutdowns but fails for:
- JVM crashes (segfault, out-of-memory)
- `kill -9` from the user or OS
- OS shutdown (no time for cleanup)
- Fiji plugin mode where closing JIPipe doesn't call `exitLater()` at all

In these cases, the JIPipe process is simply gone. The `refs/<pid>.ref` file remains, and no heartbeat updates occur. Other JIPipe instances (or future instances) must detect the stale ref through:
- `ProcessHandle.of(pid).isAlive()` -- reliable but requires another instance to trigger the check
- Heartbeat staleness -- requires waiting for a timeout (e.g., 10 seconds of no update)

**Neither provides immediate, reliable cleanup after a crash.**

### 2.2 No Independent Validation

With file-based coordination, there is no entity that can:
- Proactively detect that a server process has become unresponsive
- Restart a crashed server before any JIPipe instance requests it
- Validate that a server's configuration matches what consumers expect
- Enforce resource limits (e.g., "only one GPU server at a time")

Every check is reactive -- it happens when a JIPipe instance tries to `acquire()` or `release()` a server. Between those calls, nobody is watching.

### 2.3 Race Conditions in Decentralized Design

File-based coordination is decentralized -- any JIPipe instance can start, stop, or check the server. While `FileLocker` provides mutual exclusion during individual operations, the overall protocol has race condition windows:

- Instance A reads `server.info`, sees no server, and starts one
- Instance B reads `server.info` at the same time, also sees no server, and starts another
- Both try to write `server.info`; one wins, the other's server is orphaned

The `server.lock` mitigates this but requires careful lock scope management. A centralized manager eliminates this class of bugs entirely.

---

## 3. Dedicated Server Manager Architecture

### 3.1 Core Concept

A **server manager** is a lightweight Java process (daemon) that:

1. **Owns all external server processes** -- it spawns them, monitors them, and terminates them
2. **Manages client relationships** -- JIPipe instances connect to the manager and declare which servers they need
3. **Applies reference counting** -- a server is only stopped when no clients need it and the idle timeout expires
4. **Performs independent validation** -- health checks, configuration validation, resource enforcement
5. **Survives client crashes** -- if a JIPipe process dies, the manager detects the disconnection and cleans up
6. **Acts as a library** -- concrete implementations (llama.cpp, IPython) depend on the manager library, not on JIPipe

### 3.2 Dual-Mode Operation

The manager library operates in two modes:

| Mode | When | Server Ownership | Coordination |
|------|------|-----------------|--------------|
| **Embedded** | Simple cases, app-owned servers, no sharing needed | In-process | Direct method calls |
| **Daemon** | Shared servers, crash resilience required | Separate JVM | HTTP/localhost or Unix socket |

**Embedded mode** is used when:
- A server is app-owned (not shared between instances)
- Crash resilience is not critical (e.g., a per-session IPython kernel)
- Minimizing resource overhead is important (no extra JVM)

**Daemon mode** is used when:
- A server is shared between multiple JIPipe instances
- Crash resilience is required (e.g., expensive-to-start LLM server)
- Independent validation is needed
- The server must outlive any individual JIPipe instance

The mode selection is transparent to the calling code -- the library API is identical in both modes.

### 3.3 Architecture Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                        JIPipe Instance A                         │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ ServerManager (embedded client)                            │  │
│  │   ├── acquire("llama-cpp", env) ─── HTTP POST ──────────┐ │  │
│  │   ├── release("llama-cpp") ──────── HTTP DELETE ────────┤ │  │
│  │   └── getStatus() ───────────────── HTTP GET ───────────┤ │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
┌──────────────────────────────────────────────────────────────────┐
│                        JIPipe Instance B                         │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ ServerManager (embedded client)                            │  │
│  │   ├── acquire("llama-cpp", env) ─── HTTP POST ──────────┐ │  │
│  │   └── release("llama-cpp") ──────── HTTP DELETE ────────┤ │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTP on localhost:PORT
                           │ (or Unix domain socket)
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    Server Manager Daemon                         │
│                    (lightweight JVM process)                     │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ HTTP API                                                   │  │
│  │   POST   /api/v1/servers/{id}/acquire   (register client) │  │
│  │   DELETE /api/v1/servers/{id}/release   (unregister)      │  │
│  │   GET    /api/v1/servers/{id}/status    (health + state)  │  │
│  │   GET    /api/v1/servers                (list all)        │  │
│  │   POST   /api/v1/servers/{id}/restart   (restart server)  │  │
│  │   WS     /api/v1/events                (real-time events) │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Process Supervisor                                         │  │
│  │   ├── Spawns server processes via ProcessBuilder           │  │
│  │   ├── Monitors via ProcessHandle.onExit()                  │  │
│  │   ├── Health checks via HTTP/TCP probing                   │  │
│  │   ├── Restart policy (on-failure, always, never)           │  │
│  │   └── Resource limits (max concurrent servers)             │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Client Tracker                                             │  │
│  │   ├── Tracks connected JIPipe instances (by PID)           │  │
│  │   ├── Detects disconnection (TCP keepalive / read timeout) │  │
│  │   ├── Reference counting per server                        │  │
│  │   └── Auto-release on client disconnect                    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Daemon Lifecycle                                           │  │
│  │   ├── PID file at <sharedDir>/manager/daemon.pid           │  │
│  │   ├── Connection info at <sharedDir>/manager/daemon.json   │  │
│  │   ├── Idle timeout (auto-stop after N minutes no clients)  │  │
│  │   ├── Graceful shutdown on SIGTERM                         │  │
│  │   └── Orphan cleanup on startup                            │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐                     │
│  │ llama.cpp server │  │ IPython kernel   │  (managed procs)   │
│  │ PID: 45678       │  │ PID: 45679       │                     │
│  │ Port: 18081      │  │ Port: 18082      │                     │
│  └──────────────────┘  └──────────────────┘                     │
└──────────────────────────────────────────────────────────────────┘
```

### 3.4 The Manager as a Library

The server manager is not just a daemon -- it is **a library that concrete implementations depend on**. This is the key design principle.

```
┌─────────────────────────────────────────────────────────────┐
│                  jipipe-server-manager                       │
│                  (Maven module: library)                     │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ API layer (interfaces)                                │  │
│  │   ServerManager        -- acquire/release/status      │  │
│  │   ManagedServer        -- server lifecycle handle     │  │
│  │   ServerDefinition     -- how to start a server       │  │
│  │   ServerHealthCheck    -- how to verify it's running  │  │
│  │   ServerClient         -- represents a consumer       │  │
│  │   ServerEvent          -- lifecycle events            │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Core implementation                                   │  │
│  │   EmbeddedServerManager   -- in-process mode          │  │
│  │   DaemonClientManager     -- connects to daemon       │  │
│  │   DaemonServerManager     -- the daemon itself        │  │
│  │   ProcessSupervisor       -- manages child procs      │  │
│  │   StateDirectoryManager   -- file-based state         │  │
│  │   ClientTracker           -- reference counting       │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Daemon launcher                                      │  │
│  │   DaemonMain               -- entry point             │  │
│  │   DaemonHttpServer         -- REST API                │  │
│  │   DaemonProcessManager     -- owns all servers        │  │
│  │   DaemonClientTracker      -- tracks JIPipe instances │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
          ▲                ▲                ▲
          │                │                │
    ┌─────┴──────┐  ┌─────┴──────┐  ┌─────┴──────┐
    │ llama-cpp  │  │  ipython   │  │  generic   │
    │ server     │  │  server    │  │  server    │
    │ impl       │  │  impl      │  │  impl      │
    └────────────┘  └────────────┘  └────────────┘
    (depends on      (depends on      (depends on
     manager API)     manager API)     manager API)
```

**Each concrete implementation provides:**

1. **`ServerDefinition`** -- how to construct the command line, environment variables, and working directory for the server process
2. **`ServerHealthCheck`** -- how to verify the server is alive (HTTP endpoint, TCP port probe, custom protocol)
3. **`ServerConfiguration`** -- serializable configuration (port, model path, GPU layers, etc.)
4. **Optional: `ServerProxy`** -- a client that knows how to communicate with the specific server type

Example for llama.cpp:

```java
public class LlamaCppServerDefinition implements ServerDefinition {
    private Path executablePath;
    private Path modelPath;
    private int gpuLayers;
    private int contextSize;
    
    @Override
    public List<String> getCommandLine(int port) {
        return List.of(
            executablePath.toString(),
            "--port", String.valueOf(port),
            "--model", modelPath.toString(),
            "--ngl", String.valueOf(gpuLayers),
            "--ctx-size", String.valueOf(contextSize)
        );
    }
    
    @Override
    public ServerHealthCheck getHealthCheck() {
        return new HttpHealthCheck("/health", 200);
    }
}
```

### 3.5 Java-Based Services (In-Process Mode)

The user mentioned "if it's Java like Spring it just stays in Java." This refers to the case where the "server" is itself a Java process that could run **inside** the manager daemon rather than as a separate native process.

The manager library supports this via a different `ServerDefinition` subtype:

```java
public interface JavaServerDefinition extends ServerDefinition {
    void start(JavaServerContext context) throws Exception;
    void stop() throws Exception;
    boolean isHealthy();
}
```

When a `JavaServerDefinition` is used with the daemon, the server runs as a thread **within** the daemon JVM. When used in embedded mode, it runs within the JIPipe JVM.

**Use case example:** A Java-based LLM gateway that uses DJL (Deep Java Library) or ONNX Runtime to run inference directly in Java, without needing a separate llama.cpp process. This could be a Spring-based service or a plain Java service -- the manager library doesn't care.

**Spring integration (if desired):** The daemon process itself could optionally be a Spring Boot application. This makes sense if the daemon hosts Java-based services that benefit from Spring's dependency injection, configuration management, and actuator endpoints. However, this adds significant startup time and memory overhead. See Section 6 for the tradeoff analysis.

---

## 4. Daemon Lifecycle

### 4.1 Daemon Bootstrap (Starting the Daemon)

When a JIPipe instance needs a shared server, the library follows this sequence:

```
JIPipe Instance                        Daemon Process                  File System
     │                                       │                            │
     │  1. Check daemon.json ──────────────────────────────────────────▶ │
     │  2. Read: port=18080, pid=12345      ◀────────────────────────── │
     │  3. ProcessHandle.of(12345).isAlive()                              │
     │     → true                                                         │
     │  4. HTTP GET http://localhost:18080/api/v1/health                  │
     │     → 200 OK (daemon is alive)                                    │
     │  5. POST /api/v1/servers/llama-cpp/acquire                        │
     │                                       │                            │
     │                         (if daemon not running)                    │
     │  3b. ProcessHandle.of(12345).isAlive()                             │
     │     → false (or no daemon.json)                                    │
     │  4b. Acquire bootstrap lock ────────────────────────────────────▶ │
     │  5b. Start daemon process ──────────▶ │                           │
     │  6b. Wait for daemon.json to appear ────────────────────────────▶ │
     │  7b. Read port from daemon.json      ◀────────────────────────── │
     │  8b. Release bootstrap lock ────────────────────────────────────▶ │
     │  9b. POST /api/v1/servers/llama-cpp/acquire                       │
```

**Bootstrap lock:** A `FileLocker` on `<sharedDir>/manager/bootstrap.lock` prevents two JIPipe instances from starting the daemon simultaneously.

**Daemon startup sequence:**
1. Acquire bootstrap lock
2. Write PID to `daemon.pid`
3. Start HTTP server on a free port (port 0, OS assigns)
4. Write connection info to `daemon.json`: `{"pid": 12345, "port": 18080, "startedAt": "..."}`
5. Release bootstrap lock
6. Enter main event loop

### 4.2 Client Registration and Reference Counting

When a JIPipe instance acquires a server:

```
JIPipe Instance                        Daemon Process
     │                                       │
     │  POST /api/v1/servers/llama-cpp/acquire
     │  Body: {"clientPid": 67890, "config": {...}}
     │                                       │
     │                        ┌──────────────┤
     │                        │ 1. If server not running:
     │                        │    a. Spawn llama-cpp process
     │                        │    b. Wait for health check
     │                        │ 2. Register client 67890
     │                        │    for server "llama-cpp"
     │                        │ 3. Start keepalive monitor
     │                        │    for client 67890
     │                        └──────────────┤
     │                                       │
     │  Response: 200 OK                     │
     │  {"port": 18081, "status": "running", "serverPid": 45678}
     │                                       │
```

**Keepalive mechanism:** The daemon monitors client liveness via two complementary approaches:

1. **TCP connection tracking:** Each client maintains a long-lived HTTP connection or WebSocket to the daemon. When the connection drops (client crash), the daemon immediately knows.

2. **PID monitoring:** The daemon periodically checks `ProcessHandle.of(clientPid).isAlive()`. This catches cases where the TCP connection is kept alive by the OS despite the process being dead.

**On client disconnect (detected by either method):**
1. Remove the client from the reference count for all its servers
2. If no clients remain for a server, start the idle timeout (e.g., 5 minutes)
3. If the idle timeout expires with no new clients, stop the server

### 4.3 Graceful Daemon Shutdown

The daemon can be shut down in several ways:

| Trigger | Behavior |
|---------|----------|
| **No clients + idle timeout** | Auto-shutdown after N minutes with zero connected clients |
| **SIGTERM / SIGINT** | Stop all managed servers gracefully, then exit |
| **All JIPipe instances exited** | Detected via PID monitoring, triggers idle timeout |
| **Explicit shutdown API** | `POST /api/v1/daemon/shutdown` (requires authentication or local-only access) |

**Shutdown sequence:**
1. Stop accepting new client connections
2. Send SIGTERM to all managed server processes
3. Wait up to 10 seconds for each server to exit
4. Force-kill any remaining server processes (via `ProcessUtils.killProcessTree()`)
5. Delete `daemon.json` and `daemon.pid`
6. Exit

### 4.4 Crash Recovery

**If the daemon crashes:**
1. JIPipe instances detect the daemon is gone (HTTP request fails, TCP connection drops)
2. They follow the bootstrap sequence (Section 4.1) to start a new daemon
3. The new daemon discovers orphaned server processes by:
   - Scanning the state directory for server records
   - Checking if the recorded PIDs are alive
   - Probing the recorded ports
4. Orphaned servers are either adopted (if they match current needs) or terminated

**If a JIPipe instance crashes:**
1. The daemon detects the TCP connection drop or PID death
2. It automatically releases the client's references
3. If no other clients need a server, the idle timeout begins
4. No stale files or heartbeat checking needed -- the daemon knows immediately

**If both crash simultaneously:**
1. On next JIPipe startup, the new instance discovers orphaned server processes
2. The state directory contains the server records from the previous daemon
3. The new instance starts a new daemon, which adopts or terminates orphans
4. If no JIPipe instance ever starts again, the server processes will eventually be cleaned up by the OS (on restart) or by a system-level cleanup script

---

## 5. Comparison: File-Based vs Daemon Approach

| Aspect | File-Based (Main Report) | Daemon (This Addendum) |
|--------|--------------------------|----------------------|
| **Crash detection** | Delayed (heartbeat timeout, next `acquire()` call) | Immediate (TCP disconnect, PID monitoring) |
| **Crash cleanup** | Relies on next JIPipe instance to check | Daemon handles it proactively |
| **`halt()` resilience** | Fragile (cleanup must be before `halt()`) | Robust (daemon is separate process) |
| **Race conditions** | Possible during concurrent start/stop | Eliminated (single coordinator) |
| **Independent validation** | None (reactive only) | Full (daemon monitors servers proactively) |
| **Complexity** | Low (file-based, no IPC) | Medium (HTTP API, daemon lifecycle) |
| **Dependencies** | None (uses existing `FileLocker`) | HTTP server library (see Section 6) |
| **Resource overhead** | Minimal (no extra process) | One lightweight JVM (~30-50 MB) |
| **Bootstrapping** | N/A (decentralized) | Need to start daemon on first use |
| **Debuggability** | Inspect files on disk | HTTP API (`curl localhost:PORT/api/v1/servers`) |
| **Fiji plugin mode** | Same as other modes | Same (daemon is independent of Fiji) |
| **In-process Java services** | Not supported | Supported (run inside daemon JVM) |
| **Third-party integration** | Must implement file protocol | Just use HTTP API |

### Verdict

The daemon approach is strictly superior for **shared servers** that need crash resilience and independent validation. The file-based approach is acceptable for **app-owned servers** where simplicity is preferred.

The recommended **hybrid architecture** uses both:
- **Embedded mode** (file-based) for app-owned, per-instance servers
- **Daemon mode** for shared, cross-instance servers

---

## 6. Technology Choices

### 6.1 HTTP Server for the Daemon

The daemon needs an HTTP server for its REST API. Three options are evaluated:

#### Option A: Pure JDK `com.sun.net.httpserver`

| Aspect | Rating |
|--------|--------|
| Dependencies | **Zero** (JDK-bundled) |
| JAR size | **0 KB** |
| Startup time | **~50ms** |
| Memory | **~25-35 MB** (total JVM) |
| WebSocket | **No** (would need separate library) |
| Maturity | Stable since JDK 1.6 |

**Pros:** Zero footprint, no classpath conflicts, fastest startup.  
**Cons:** No WebSocket (need long-polling for events), thread-per-connection model (mitigated by virtual threads in Java 21), `com.sun` namespace is not officially supported but practically stable.

#### Option B: Javalin

| Aspect | Rating |
|--------|--------|
| Dependencies | ~28 (includes Jetty, Jackson, SLF4J) |
| JAR size | ~768 KB (framework) + ~10 MB (with Jetty) |
| Startup time | **~800ms** |
| Memory | **~80-120 MB** (total JVM) |
| WebSocket | **Yes** (built-in) |
| Maturity | Active, well-maintained |

**Pros:** Clean API, WebSocket support, virtual threads, OpenAPI, well-documented.  
**Cons:** Significant transitive dependencies (Jetty ecosystem), higher memory, slower startup. JIPipe already has Jackson and SLF4J, so some dependencies overlap.

#### Option C: Jetty Embedded (without Javalin)

| Aspect | Rating |
|--------|--------|
| Dependencies | ~8 (servlet API, Jetty core) |
| JAR size | ~3.2 MB |
| Startup time | **~400ms** |
| Memory | **~60-80 MB** (total JVM) |
| WebSocket | **Yes** (built-in) |
| Maturity | Very mature (Eclipse project) |

**Pros:** Full-featured, WebSocket, HTTP/2, virtual threads, no high-level framework overhead.  
**Cons:** Verbose API, more code to write than Javalin.

### Recommendation

**Start with Option A (Pure JDK)** for the MVP. The daemon's API surface is small (6-8 endpoints, one WebSocket channel). The lack of WebSocket can be worked around with Server-Sent Events (SSE), which `com.sun.net.httpserver` supports trivially. Virtual threads in Java 21 eliminate the thread-per-connection concern.

**Upgrade to Option B (Javalin) or C (Jetty)** if:
- WebSocket becomes a hard requirement (e.g., real-time streaming of server logs)
- The API grows beyond simple CRUD operations
- You want automatic OpenAPI documentation generation

**Spring Boot is not recommended** for the daemon. A minimal Spring Boot app uses 120-200 MB RSS and takes 1-2 seconds to start. This is too heavy for a background daemon that should start in <500ms. Spring Framework (without Boot) is lighter but still heavier than pure JDK for what amounts to a simple REST API. However, if the daemon hosts Java-based services that already use Spring, it could make sense to embed the manager within a Spring application (see Section 6.3).

### 6.2 IPC Transport

| Transport | Latency | Portability | Recommendation |
|-----------|---------|-------------|----------------|
| **HTTP on localhost** | ~50-200 µs | All platforms | **Primary** -- simplest, debuggable |
| **Unix domain socket** | ~5-10 µs | Linux/macOS/Win10+ | **Future optimization** |
| **WebSocket on localhost** | ~50-200 µs | All platforms | **For event streaming** |

**Recommended: HTTP on localhost as primary transport.**

Rationale:
- Works on all platforms without configuration
- Debuggable with `curl`, browsers, and any HTTP client
- JIPipe already has `java.net.http.HttpClient` available
- Port conflicts are avoided by using OS-assigned ports (port 0)
- Firewall concerns are minimal (loopback only, `127.0.0.1`)

**Unix domain socket as future optimization:**
- Java 21 has built-in support (`StandardProtocolFamily.UNIX`)
- Lower latency, no port allocation, filesystem-based access control
- Not yet needed -- optimize later if latency becomes an issue

### 6.3 Spring Integration (When Appropriate)

The user mentioned "if it's Java like Spring it just stays in Java." This is valid for a specific use case: **when the managed service is itself a Java application.**

**Scenario:** A Java-based LLM inference service using DJL or ONNX Runtime, packaged as a Spring Boot application, runs inside the daemon JVM.

**How it works:**
1. The daemon starts a Spring `ApplicationContext` within its JVM
2. The Spring context contains the inference service beans
3. The daemon's HTTP API proxies requests to the Spring-managed services
4. No external process is needed -- the "server" is Java code running in-process

**This is only appropriate when:**
- The service is Java-based and can run within the daemon JVM
- The service benefits from Spring's DI, configuration, and lifecycle management
- Memory overhead is acceptable (Spring Boot adds ~100 MB)

**For native process management (llama.cpp, IPython, etc.), Spring is not needed and adds unnecessary overhead.**

### 6.4 Dependency Strategy

The server manager library should have **minimal dependencies** to avoid conflicts with JIPipe's existing classpath. The library's dependency footprint depends on which mode is used:

| Mode | Required Dependencies |
|------|-----------------------|
| **Embedded (client)** | None beyond JDK + Jackson (already in JIPipe) |
| **Embedded (server)** | JDK `HttpServer` (built-in) + Jackson |
| **Daemon client** | JDK `HttpClient` (built-in) + Jackson |
| **Daemon server** | JDK `HttpServer` (built-in) + Jackson |

**The only external dependency is Jackson**, which is already available in JIPipe. This means the manager library can be a near-zero-cost addition to JIPipe's classpath.

If Javalin/Jetty is added later for WebSocket support, those dependencies only affect the daemon's classpath (a separate JVM), not JIPipe's classpath.

---

## 7. Project Structure

### 7.1 Maven Module Layout

```
jipipe-2/
├── contrib/
│   ├── jipipe-ro-crate-java-2.1.0/     (existing library)
│   └── jipipe-server-manager/           (NEW)
│       ├── pom.xml                      (parent POM)
│       ├── jipipe-server-manager-api/   (interfaces, no impl)
│       │   ├── pom.xml
│       │   └── src/main/java/
│       │       └── org/hkijena/jipipe/svm/
│       │           ├── ServerManager.java
│       │           ├── ManagedServer.java
│       │           ├── ServerDefinition.java
│       │           ├── ServerHealthCheck.java
│       │           ├── ServerClient.java
│       │           ├── ServerEvent.java
│       │           ├── ServerConfiguration.java
│       │           └── JavaServerDefinition.java
│       ├── jipipe-server-manager-core/  (implementation)
│       │   ├── pom.xml
│       │   └── src/main/java/
│       │       └── org/hkijena/jipipe/svm/core/
│       │           ├── EmbeddedServerManager.java
│       │           ├── DaemonClientManager.java
│       │           ├── DaemonServerManager.java
│       │           ├── DaemonMain.java
│       │           ├── DaemonHttpServer.java
│       │           ├── ProcessSupervisor.java
│       │           ├── StateDirectoryManager.java
│       │           ├── ClientTracker.java
│       │           └── HttpHealthCheck.java
│       └── jipipe-server-manager-test/  (unit tests)
│           ├── pom.xml
│           └── src/test/java/
│               └── org/hkijena/jipipe/svm/
│                   ├── EmbeddedServerManagerTest.java
│                   ├── DaemonIntegrationTest.java
│                   └── CrashRecoveryTest.java
├── jipipe-core/                        (depends on -api module)
├── plugins/
│   ├── jipipe-plugin-ai/               (uses library for llama.cpp)
│   └── jipipe-plugin-python/           (uses library for IPython)
└── pom.xml
```

### 7.2 Dependency Graph

```
jipipe-server-manager-api  (zero external deps)
         ▲
         │
jipipe-server-manager-core (depends on -api + Jackson)
         ▲
         │
    ┌────┴─────┐
    │          │
jipipe-core   (daemon launcher, server environments)
    ▲
    │
plugins/      (concrete server definitions)
```

**Key principle:** `jipipe-core` depends only on `jipipe-server-manager-api` (interfaces). The `jipipe-server-manager-core` (implementation) is loaded at runtime. Concrete implementations in plugins depend on the API module.

### 7.3 Following Existing Patterns

The project structure follows the existing `jipipe-ro-crate-java-2.1.0` pattern in `contrib/`:
- Standalone Maven module with its own POM
- Independent from `jipipe-core` (api module) or minimally dependent (core module)
- Consumed by `jipipe-core` as a dependency
- Has its own test infrastructure

---

## 8. REST API Specification

### 8.1 Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/health` | Daemon health check |
| `GET` | `/api/v1/servers` | List all managed servers |
| `POST` | `/api/v1/servers/{id}/acquire` | Acquire a server (start if needed) |
| `DELETE` | `/api/v1/servers/{id}/release` | Release a server (stop if last client) |
| `GET` | `/api/v1/servers/{id}/status` | Get server status and health |
| `POST` | `/api/v1/servers/{id}/restart` | Restart a server |
| `WS` | `/api/v1/events` | Real-time event stream |
| `POST` | `/api/v1/daemon/shutdown` | Graceful daemon shutdown |

### 8.2 Request/Response Examples

**Acquire a server:**
```http
POST /api/v1/servers/llama-cpp/acquire HTTP/1.1
Content-Type: application/json

{
  "clientPid": 67890,
  "definition": {
    "type": "process",
    "commandLine": ["/path/to/llama-server", "--port", "{port}", "--model", "/path/to/model.gguf"],
    "workingDirectory": "/tmp",
    "environmentVariables": {},
    "healthCheck": {"type": "http", "path": "/health", "expectedStatus": 200},
    "startupTimeoutMs": 30000,
    "portArg": "{port}"
  },
  "config": {
    "ownership": "shared"
  }
}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "serverId": "llama-cpp",
  "status": "running",
  "port": 18081,
  "serverPid": 45678,
  "refCount": 2,
  "startedAt": "2026-06-01T10:30:00Z"
}
```

**Release a server:**
```http
DELETE /api/v1/servers/llama-cpp/release HTTP/1.1
Content-Type: application/json

{
  "clientPid": 67890
}
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "serverId": "llama-cpp",
  "status": "running",
  "refCount": 1,
  "message": "Server still in use by 1 other client(s)"
}
```

**Server status:**
```http
GET /api/v1/servers/llama-cpp/status HTTP/1.1
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "serverId": "llama-cpp",
  "status": "running",
  "healthStatus": "healthy",
  "port": 18081,
  "serverPid": 45678,
  "refCount": 2,
  "clients": [
    {"pid": 67890, "acquiredAt": "2026-06-01T10:30:05Z", "lastSeen": "2026-06-01T10:35:00Z"},
    {"pid": 67891, "acquiredAt": "2026-06-01T10:31:00Z", "lastSeen": "2026-06-01T10:35:02Z"}
  ],
  "startedAt": "2026-06-01T10:30:00Z",
  "uptimeMs": 300000
}
```

### 8.3 Event Stream (WebSocket or SSE)

For real-time notifications (server started, server stopped, health changed, client connected/disconnected), the daemon provides an event stream.

**WebSocket (preferred if available):**
```
WS ws://localhost:18080/api/v1/events

→ {"type": "server.started", "serverId": "llama-cpp", "port": 18081}
→ {"type": "client.connected", "serverId": "llama-cpp", "clientPid": 67891}
→ {"type": "health.degraded", "serverId": "llama-cpp", "message": "Health check timeout"}
→ {"type": "client.disconnected", "serverId": "llama-cpp", "clientPid": 67890}
→ {"type": "server.stopping", "serverId": "llama-cpp", "reason": "no clients"}
```

**SSE fallback (if no WebSocket):**
```
GET /api/v1/events
Accept: text/event-stream

data: {"type": "server.started", "serverId": "llama-cpp", "port": 18081}

data: {"type": "client.connected", "serverId": "llama-cpp", "clientPid": 67891}
```

---

## 9. Integration with JIPipe

### 9.1 Service Component

A new `JIPipeServerManagerServiceComponent` wraps the library's `ServerManager` interface:

```java
public class JIPipeServerManagerServiceComponent extends JIPipeServiceComponent {
    private final ServerManager serverManager;
    
    public JIPipeServerManagerServiceComponent(JIPipeService service) {
        super(service);
        // Choose mode based on whether shared servers are configured
        this.serverManager = ServerManagerFactory.create(
            PathUtils.getJIPipeSharedDir().resolve("manager"),
            ServerManagerMode.Auto  // embedded if no sharing, daemon if sharing
        );
    }
    
    public ManagedServer acquire(String serverId, ServerDefinition definition) {
        return serverManager.acquire(serverId, definition);
    }
    
    public void release(String serverId) {
        serverManager.release(serverId, ProcessHandle.current().pid());
    }
    
    public void releaseAll() {
        serverManager.releaseAll(ProcessHandle.current().pid());
    }
}
```

### 9.2 Integration with `exitLater()`

```java
public static void exitLater(int exitCode) {
    if (instance != null && instance.isAutosaveSettings()) {
        instance.getApplicationSettings().save();
    }
    // Release all server references
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

**Crucially:** Even if `releaseAll()` is never called (crash, `kill -9`), the daemon detects the client's disappearance via TCP disconnect and PID monitoring, and automatically cleans up. This is the daemon approach's primary advantage.

### 9.3 Environment Integration

`JIPipeServerEnvironment` extends `JIPipeArtifactEnvironment` and provides the bridge between JIPipe's environment system and the server manager:

```java
public abstract class JIPipeServerEnvironment extends JIPipeArtifactEnvironment {
    private ServerOwnership ownership = ServerOwnership.Shared;
    private int port = 0; // 0 = auto
    private long startupTimeoutMs = 30000;
    private long idleTimeoutMs = 300000; // 5 minutes
    
    public abstract ServerDefinition toServerDefinition();
    
    @Override
    public void runPreconfigure(JIPipeGraphRun run, JIPipeProgressInfo progressInfo) {
        JIPipeServerManagerServiceComponent mgr = 
            JIPipe.getInstance().getServerManager();
        ManagedServer server = mgr.acquire(getServerId(), toServerDefinition());
        // Store the server reference for algorithm nodes to use
        run.setMetadata(getServerId() + ".server", server);
    }
}
```

Concrete implementation:
```java
public class LlamaCppServerEnvironment extends JIPipeServerEnvironment {
    private Path modelPath;
    private int gpuLayers = 0;
    
    @Override
    public ServerDefinition toServerDefinition() {
        return ProcessServerDefinition.builder()
            .executablePath(getExecutablePath())
            .arguments("--port", "{port}", "--model", modelPath.toString(), 
                       "--ngl", String.valueOf(gpuLayers))
            .healthCheck(HttpHealthCheck.of("/health", 200))
            .startupTimeoutMs(startupTimeoutMs)
            .build();
    }
    
    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, 
                                                JIPipeProgressInfo progressInfo) {
        setExecutablePath(artifact.getLocalPath().resolve("llama-server"));
        setModelPath(artifact.getLocalPath().resolve("model.gguf"));
    }
}
```

### 9.4 Daemon Startup in Production (Fiji Plugin Mode)

In Fiji plugin mode, the daemon must be started without disrupting the existing Fiji JVM:

1. The library detects that shared server mode is needed
2. It launches the daemon as a **separate JVM process** using `ProcessBuilder`
3. The daemon's classpath is constructed from JIPipe's JARs (already available in Fiji's `plugins/` directory)
4. Alternatively, the daemon can be a shaded (fat) JAR containing only the manager library

**Launching the daemon:**
```java
String javaHome = System.getProperty("java.home");
String classpath = buildDaemonClasspath(); // JIPipe JARs + manager library
ProcessBuilder pb = new ProcessBuilder(
    javaHome + "/bin/java",
    "-cp", classpath,
    "-Xmx128m",              // Limit daemon memory
    "org.hkijena.jipipe.svm.core.DaemonMain",
    "--state-dir", stateDir.toString(),
    "--idle-timeout", "300"  // 5 minutes
);
pb.directory(stateDir.toFile());
Process daemon = pb.start();
```

**For Fiji production mode**, the classpath is constructed from:
- `ImageJ.app/jars/` -- core Java libraries
- `ImageJ.app/plugins/JIPipe/` -- JIPipe JARs including the manager library

This is the same classpath that ImageJ's launcher uses (via `--pass-classpath --full-classpath`).

---

## 10. Risk Analysis (Addendum)

### 10.1 Bootstrapping Problem

**Risk:** Who starts the daemon? What if the bootstrap fails?  
**Mitigation:** The first JIPipe instance that needs a shared server starts the daemon. The `FileLocker` on `bootstrap.lock` prevents duplicate starts. If the daemon fails to start (port conflict, JVM crash), the library falls back to **embedded mode** with a warning. The file-based coordination from the main report serves as the fallback.

### 10.2 Daemon Resource Overhead

**Risk:** The daemon JVM consumes memory even when idle.  
**Mitigation:** The daemon auto-shuts down after a configurable idle timeout (default: 5 minutes with no clients). Memory is limited via `-Xmx128m`. When using pure JDK `HttpServer`, idle memory is ~25-35 MB.

### 10.3 Daemon Crash

**Risk:** The daemon itself crashes, leaving managed server processes orphaned.  
**Mitigation:** 
1. The daemon writes server state to the state directory (`<sharedDir>/manager/servers/`) before starting each server
2. On restart, the new daemon reads the state directory and adopts or terminates orphaned servers
3. JIPipe instances detect daemon failure (HTTP connection refused) and start a new daemon
4. The daemon should be simple enough to be very stable -- its only job is process management and HTTP serving

### 10.4 Security

**Risk:** An unauthorized process connects to the daemon's HTTP API.  
**Mitigation:**
1. Bind to `127.0.0.1` only (loopback, not externally accessible)
2. Verify client PIDs via `ProcessHandle` (reject connections from non-existent PIDs)
3. Optional: require a shared secret token in API requests (generated at daemon startup, written to `daemon.json`, readable only by the current user via OS file permissions)
4. On Unix, Unix domain sockets provide filesystem-based access control

### 10.5 Version Compatibility

**Risk:** JIPipe instances with different versions of the manager library connect to the same daemon.  
**Mitigation:** The daemon's `/api/v1/health` endpoint reports its API version. Client libraries check compatibility and refuse to connect to incompatible daemons. The daemon auto-shuts down if the last client disconnects and a new daemon with the updated version is started.

---

## 11. Updated Implementation Roadmap

### Phase 1: Manager Library (API + Core)
- Create `jipipe-server-manager` Maven module with `api` and `core` sub-modules
- Implement `ServerDefinition`, `ServerHealthCheck`, `ManagedServer` interfaces
- Implement `EmbeddedServerManager` (in-process mode)
- Implement `ProcessSupervisor` (spawn, monitor, kill process trees)
- Implement `HttpHealthCheck` and `TcpHealthCheck`
- Unit tests

### Phase 2: Daemon Mode
- Implement `DaemonMain` (entry point)
- Implement `DaemonHttpServer` (REST API using JDK `HttpServer`)
- Implement `DaemonProcessManager` (owns all server processes)
- Implement `ClientTracker` (TCP-based client monitoring)
- Implement `StateDirectoryManager` (persistent state for crash recovery)
- Implement `DaemonClientManager` (JIPipe-side client that connects to daemon)
- Integration tests (start daemon, acquire server, release, crash recovery)

### Phase 3: JIPipe Integration
- Add `JIPipeServerManagerServiceComponent` to `JIPipeService`
- Add `JIPipeServerEnvironment` extending `JIPipeArtifactEnvironment`
- Integrate `releaseAll()` into `JIPipe.exitLater()` and `JIPipeService.dispose()`
- Add `NetworkUtils.findFreePort()` utility
- Daemon launcher (start daemon from JIPipe with correct classpath)

### Phase 4: Concrete Implementations
- `LlamaCppServerEnvironment` (llama.cpp server)
- `IPythonServerEnvironment` (IPython kernel)
- `JupyterServerEnvironment` (Jupyter notebook server)
- `GenericServerEnvironment` (user-configurable HTTP server)

### Phase 5: UI and Polish
- Server status panel in JIPipe desktop
- Server management in application settings
- Progress indicator during server startup
- WebSocket event stream for real-time status updates
- Optional: Javalin migration for WebSocket support

---

## 12. Key Files Reference (Addendum)

| File | Relevance |
|------|-----------|
| `contrib/jipipe-ro-crate-java-2.1.0/pom.xml` | Pattern for standalone library module |
| `jipipe-core/pom.xml` | Will add dependency on `jipipe-server-manager-api` |
| `jipipe-launcher/pom.xml` | Pattern for daemon classpath construction |
| `jipipe-core/.../JIPipe.java:435-449` | `exitLater()` -- integration point for `releaseAll()` |
| `jipipe-core/.../api/service/JIPipeService.java:83-110` | Add `JIPipeServerManagerServiceComponent` |
| `jipipe-core/.../api/service/components/JIPipeAIServiceComponent.java` | Pattern for task queue + lifecycle management |
| `jipipe-core/.../utils/ProcessUtils.java` | Reuse for spawning daemon process |
| `jipipe-core/.../utils/FileLocker.java` | Reuse for bootstrap lock |
| `jipipe-core/.../utils/PathUtils.java:380-406` | `getJIPipeSharedDir()` -- base for daemon state |
| `jipipe-core/.../api/ai/JIPipeAPIEmbeddingAIModelRunner.java` | Pattern for HTTP client with retries |
| `jipipe-core/.../plugins/artifacts/ArtifactsPlugin.java:86-94` | Pattern for registering artifact environments |
