# Addendum 2: Daemon Instantiation -- How the Manager Process Is Started

**Date:** 2026-06-01  
**Status:** Investigation (no code changes)  
**Extends:** `investigation-external-server-processes-addendum-daemon.md`

---

## 1. Problem Statement

The previous addendum describes a daemon process but leaves the instantiation mechanism vague. This document clarifies **exactly how the daemon JVM process is spawned** from within a running JIPipe instance, under the following constraints:

1. **The same JVM runtime that runs JIPipe is used** -- no separate JRE installation or version
2. **Must be OS-independent** -- same mechanism works on Windows, Linux, and macOS
3. **Must work in all launch configurations** -- IDE debug, jipipe-launcher, Fiji plugin

---

## 2. Core Mechanism: `java.home` + `java.class.path`

The daemon is spawned using `ProcessBuilder` with two JDK-standard system properties that are **guaranteed to be available** in every JVM:

| Property | Meaning | Example (Linux) | Example (Windows) |
|----------|---------|-----------------|-------------------|
| `java.home` | Root of the JRE/JDK running the current JVM | `/usr/lib/jvm/java-21-openjdk-amd64` | `C:\Program Files\Java\jdk-21` |
| `java.class.path` | The full runtime classpath of the current JVM | `jipipe-core.jar:plugins/...` | `jipipe-core.jar;plugins\...` |

Both properties are set by the JVM at startup and reflect the actual runtime environment. They work identically whether JIPipe was launched by:
- **IDE debug** (Maven constructs the classpath)
- **jipipe-launcher** (ImageJ native launcher constructs the classpath)
- **Fiji plugin** (ImageJ native launcher constructs the classpath)

### 2.1 Spawning the Daemon

```java
public class DaemonLauncher {

    public static Process spawnDaemon(Path stateDir, String... extraArgs) throws IOException {
        // 1. Locate the Java executable
        Path javaHome = Path.of(System.getProperty("java.home"));
        String javaExec = SystemUtils.IS_OS_WINDOWS
                ? javaHome.resolve("bin").resolve("javaw.exe").toString()
                : javaHome.resolve("bin").resolve("java").toString();

        // 2. Inherit the current classpath
        String classpath = System.getProperty("java.class.path");

        // 3. Build command line
        List<String> command = new ArrayList<>();
        command.add(javaExec);
        command.add("-cp");
        command.add(classpath);
        command.add("-Xmx128m");                    // limit daemon memory
        command.add("-Djipipe.daemon=true");        // mark as daemon process
        command.add("org.hkijena.jipipe.svm.core.DaemonMain");
        command.add("--state-dir");
        command.add(stateDir.toAbsolutePath().toString());
        command.addAll(Arrays.asList(extraArgs));

        // 4. Spawn the process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(stateDir.resolve("daemon.log").toFile());
        return pb.start();
    }
}
```

**Why this is OS-independent:**
- `java.home` is a JVM-standard property, always available
- `java.class.path` uses the platform-specific separator (`:` on Unix, `;` on Windows) automatically
- `javaw.exe` on Windows avoids a console window; `java` on Unix is the standard executable
- `ProcessBuilder` is cross-platform by design

### 2.2 Why `java.class.path` Works in All Configurations

This is the critical question. Does `java.class.path` contain all JIPipe JARs regardless of how JIPipe was launched?

| Launch Mode | Who Sets `java.class.path` | Content |
|-------------|---------------------------|---------|
| **IDE debug** | Maven/IDE | All JARs from `mvn compile` + dependencies |
| **jipipe-launcher** | ImageJ native launcher (`--pass-classpath --full-classpath`) | All JARs from `ImageJ.app/jars/` + `ImageJ.app/plugins/` |
| **Fiji plugin** | ImageJ native launcher (Fiji startup) | All JARs from `Fiji.app/jars/` + `Fiji.app/plugins/` |

In all three cases, the classpath includes the `jipipe-server-manager-core` JAR (or its classes), because that module is a dependency of `jipipe-core` (which is a dependency of all launch configurations).

**Edge case: modular classpath.** Some IDEs and launchers use the module path (`-p`) instead of the classpath (`-cp`). In this case, `java.class.path` may be empty. However:
- ImageJ/Fiji does NOT use modules (it's a classpath-based application)
- Maven Surefire (testing) uses classpath
- The ImageJ native launcher uses `-cp`, not `-p`

**Edge case: custom classloaders.** If JIPipe uses custom classloaders (e.g., SciJava's `PluginService`), some classes may not be on `java.class.path`. However, the daemon main class (`DaemonMain`) is in `jipipe-server-manager-core`, which is a direct dependency of `jipipe-core` and always on the classpath.

**Verification approach:** At daemon startup, `DaemonMain` should verify it can load its own classes and log the classpath. If the classpath is incomplete, the daemon can fall back to scanning the ImageJ directory for JARs (see Section 3).

### 2.3 `javaw.exe` vs `java.exe` on Windows

| Executable | Console Window | Use Case |
|------------|---------------|----------|
| `java.exe` | Yes (visible) | Debugging, development |
| `javaw.exe` | No (headless) | Production daemon |

For the daemon, `javaw.exe` is preferred because:
- No console window appears (users would find it confusing)
- The daemon writes its own log file (`daemon.log`)
- The daemon has no stdin/stdout interaction

If `javaw.exe` is not found (rare -- it should always exist in a JDK/JRE), fall back to `java.exe`.

---

## 3. Fallback: ImageJ Directory Scanning

If `java.class.path` is empty or incomplete (unlikely but defensive), the daemon launcher can construct the classpath by scanning the ImageJ directory -- the same approach the ImageJ native launcher uses internally:

```java
public static String buildClasspathFromImageJDir() {
    Path imageJDir = Path.of(ij.Prefs.getImageJDir());
    List<Path> jars = new ArrayList<>();
    
    // Scan jars/ directory (core libraries)
    Path jarsDir = imageJDir.resolve("jars");
    if (Files.isDirectory(jarsDir)) {
        try (var stream = Files.walk(jarsDir)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                  .forEach(jars::add);
        } catch (IOException e) { /* log */ }
    }
    
    // Scan plugins/ directory (ImageJ plugins including JIPipe)
    Path pluginsDir = imageJDir.resolve("plugins");
    if (Files.isDirectory(pluginsDir)) {
        try (var stream = Files.walk(pluginsDir)) {
            stream.filter(p -> p.toString().endsWith(".jar"))
                  .forEach(jars::add);
        } catch (IOException e) { /* log */ }
    }
    
    return jars.stream()
               .map(Path::toAbsolutePath)
               .map(Path::toString)
               .collect(Collectors.joining(File.pathSeparator));
}
```

This is only needed as a fallback for the Fiji plugin case where `java.class.path` might be non-standard. In practice, the ImageJ native launcher always sets `java.class.path` correctly.

---

## 4. Daemon Bootstrap Sequence

### 4.1 Full Sequence Diagram

```
JIPipe Instance A                    File System                    Daemon Process
     │                                   │                              │
     │  1. acquire("llama-cpp", ...)     │                              │
     │                                   │                              │
     │  2. Read daemon.json ────────────▶│                              │
     │     → not found                   │                              │
     │                                   │                              │
     │  3. Acquire bootstrap.lock ──────▶│                              │
     │     (FileLocker on                │                              │
     │      <sharedDir>/manager/         │                              │
     │      bootstrap.lock)              │                              │
     │                                   │                              │
     │  4. Re-check daemon.json ────────▶│  (another instance might     │
     │     → still not found             │   have started the daemon    │
     │                                   │   while we waited for lock)  │
     │                                   │                              │
     │  5. Spawn daemon ─────────────────────────────────────────────▶ │
     │     ProcessBuilder(               │                              │
     │       java -cp <classpath>        │                              │
     │       DaemonMain                  │                              │
     │       --state-dir <dir>)          │                              │
     │                                   │                              │
     │                                   │              6. Daemon starts│
     │                                   │                 HTTP server  │
     │                                   │              on port 0       │
     │                                   │                              │
     │                                   │  7. Write daemon.json ──────│
     │                                   │     {pid: 45678,             │
     │                                   │      port: 18080,            │
     │                                   │      startedAt: "..."}       │
     │                                   │                              │
     │  8. Release bootstrap.lock ──────▶│                              │
     │                                   │                              │
     │  9. Wait for daemon.json ────────▶│                              │
     │     → found (port 18080)          │                              │
     │                                   │                              │
     │  10. HTTP GET :18080/health ──────────────────────────────────▶ │
     │      → 200 OK ◀─────────────────────────────────────────────── │
     │                                   │                              │
     │  11. POST /servers/llama-cpp/acquire ────────────────────────▶ │
     │      → 200 {port: 18081} ◀──────────────────────────────────── │
     │                                   │                              │
     │  12. Use llama-cpp on port 18081  │                              │
```

### 4.2 Concurrent Bootstrap (Two JIPipe Instances Start Simultaneously)

```
JIPipe A                              File System                    JIPipe B
     │                                   │                              │
     │  1. Read daemon.json ────────────▶│◀──── 1. Read daemon.json ───│
     │     → not found                   │     → not found              │
     │                                   │                              │
     │  2. Acquire bootstrap.lock ──────▶│                              │
     │     → SUCCESS                     │                              │
     │                                   │     2. Acquire bootstrap.lock│
     │                                   │     → BLOCKED (waiting)      │
     │  3. Spawn daemon ──────────────────────────────────────────────│
     │                                   │                              │
     │  4. daemon.json written ─────────▶│                              │
     │                                   │                              │
     │  5. Release bootstrap.lock ──────▶│                              │
     │                                   │     3. Lock acquired!        │
     │                                   │                              │
     │                                   │     4. Read daemon.json ─────│
     │                                   │     → FOUND (pid: 45678)     │
     │                                   │                              │
     │                                   │     5. ProcessHandle.of(45678)│
     │                                   │     → alive!                 │
     │                                   │                              │
     │                                   │     6. Release bootstrap.lock│
     │                                   │                              │
     │                                   │     7. Connect to daemon     │
```

The `FileLocker` on `bootstrap.lock` ensures exactly one JIPipe instance starts the daemon. The second instance waits, then discovers the already-running daemon.

### 4.3 Daemon Already Running (Common Case)

```
JIPipe Instance B                  File System                    Daemon Process
     │                                   │                              │
     │  1. Read daemon.json ────────────▶│                              │
     │     → found (pid: 45678,          │                              │
     │           port: 18080)            │                              │
     │                                   │                              │
     │  2. ProcessHandle.of(45678)       │                              │
     │     .isAlive() → true             │                              │
     │                                   │                              │
     │  3. HTTP GET :18080/health ───────────────────────────────────▶ │
     │     → 200 OK                      │                              │
     │                                   │                              │
     │  4. POST /servers/llama-cpp/acquire ────────────────────────▶ │
     │     (no daemon spawn needed)      │                              │
```

No bootstrap lock needed. The instance just connects to the existing daemon.

---

## 5. Daemon Self-Identification and Safety

### 5.1 How the Daemon Knows It's a Daemon

The daemon is launched with `-Djipipe.daemon=true`. This system property serves multiple purposes:

1. **Prevents recursive daemon spawning:** If the daemon itself tries to acquire a server, the library detects `jipipe.daemon=true` and uses embedded mode instead of trying to spawn another daemon.

2. **Prevents JIPipe initialization:** The daemon should NOT initialize the full JIPipe stack (`JIPipeService`, plugins, ImageJ context). It only needs the server manager library. The `-Djipipe.daemon=true` flag tells `DaemonMain` to skip JIPipe bootstrapping.

3. **Enables lightweight startup:** The daemon only loads:
   - `jipipe-server-manager-api` classes
   - `jipipe-server-manager-core` classes  
   - Jackson (for JSON serialization)
   - JDK `HttpServer` (for the REST API)
   - **NOT** ImageJ, SciJava, or any JIPipe plugins

### 5.2 Daemon Main Class

```java
public class DaemonMain {
    public static void main(String[] args) {
        // 1. Parse arguments
        Path stateDir = parseStateDir(args);
        int idleTimeoutMinutes = parseIdleTimeout(args);
        
        // 2. Verify we're running as a daemon
        if (!"true".equals(System.getProperty("jipipe.daemon"))) {
            System.err.println("This class must be launched as a daemon.");
            System.exit(1);
        }
        
        // 3. Write PID file (for crash detection by future instances)
        long pid = ProcessHandle.current().pid();
        Files.writeString(stateDir.resolve("daemon.pid"), String.valueOf(pid));
        
        // 4. Start HTTP server on auto-assigned port
        HttpServer server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),  // port 0 = OS assigns
            50  // backlog
        );
        // ... register handlers ...
        server.start();
        
        // 5. Write connection info
        int port = server.getAddress().getPort();
        String info = String.format(
            "{\"pid\":%d,\"port\":%d,\"startedAt\":\"%s\"}",
            pid, port, Instant.now()
        );
        Files.writeString(stateDir.resolve("daemon.json"), info);
        
        // 6. Register shutdown hook (daemon uses graceful shutdown, NOT halt())
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Stop all managed servers
            processManager.stopAll();
            // Clean up state files
            Files.deleteIfExists(stateDir.resolve("daemon.json"));
            Files.deleteIfExists(stateDir.resolve("daemon.pid"));
        }));
        
        // 7. Enter idle timeout loop
        while (true) {
            Thread.sleep(idleTimeoutMinutes * 60 * 1000L);
            if (clientTracker.getClientCount() == 0) {
                logger.info("Idle timeout reached, shutting down daemon");
                System.exit(0);  // triggers shutdown hook
            }
        }
    }
}
```

**Critical difference from JIPipe:** The daemon uses `System.exit(0)` (which runs shutdown hooks), **NOT** `Runtime.getRuntime().halt()`. The daemon has no SciJava `Context` and therefore no deadlock problem. This means the daemon's shutdown hooks **always execute**, providing reliable cleanup.

### 5.3 Signal Handling

The daemon should handle SIGTERM (from OS shutdown, user `kill`, or JIPipe sending a shutdown signal):

```java
// Using sun.misc.Signal (available in all JDKs, though unofficial)
Signal.handle(new Signal("TERM"), signal -> {
    logger.info("Received SIGTERM, shutting down gracefully");
    // Shutdown hook will run because we use System.exit, not halt
    System.exit(0);
});
```

On Windows, SIGTERM is sent when the process is terminated via Task Manager or `taskkill` (without `/F`). With `/F`, the process is force-killed and no signal handler runs -- but the OS releases all resources (ports, file locks, processes).

---

## 6. Daemon-Client Communication

### 6.1 Connection Lifecycle

Each JIPipe instance maintains a **persistent HTTP connection** (or WebSocket) to the daemon. This serves two purposes:

1. **API communication** -- acquire/release/status requests
2. **Liveness detection** -- if the connection drops, the daemon knows the client is dead

```
JIPipe Instance                         Daemon Process
     │                                       │
     │  ──── TCP connection established ───▶ │  (kept alive via HTTP keep-alive
     │                                       │   or WebSocket ping/pong)
     │                                       │
     │  POST /servers/llama-cpp/acquire ───▶ │
     │  ◀─── 200 OK ─────────────────────── │
     │                                       │
     │  ... (hours pass, server in use) ...  │
     │                                       │
     │  JIPipe crashes / killed / halt()     │
     │  ╳                                    │
     │  (connection drops)                   │
     │                                       │
     │                          ◀─── TCP RST │  (daemon detects disconnect
     │                                       │   via read() returning -1 or
     │                                       │   IOException on write)
     │                                       │
     │                                       │  → Auto-release client refs
     │                                       │  → Start idle timer if no
     │                                       │    remaining clients
```

### 6.2 Implementation Options

**Option A: HTTP Keep-Alive (Simplest)**

JIPipe instances make regular HTTP requests with `Connection: keep-alive`. The daemon monitors idle connections. If no request arrives within a timeout (e.g., 30 seconds), the daemon considers the client dead.

- **Pro:** Works with any HTTP server library
- **Con:** Requires periodic polling (heartbeat requests) from the client

**Option B: WebSocket (Recommended)**

JIPipe instances open a WebSocket connection to `/api/v1/events`. The connection stays open for the lifetime of the JIPipe instance. The daemon uses TCP keepalive to detect disconnection.

- **Pro:** Immediate detection, no polling, bidirectional events
- **Con:** Requires WebSocket support (available via JDK `HttpClient` on client side, needs server-side support)

**Option C: Long-Polling with Client PID**

JIPipe instances periodically call `GET /api/v1/heartbeat?clientPid=<pid>`. The daemon also independently verifies client PIDs via `ProcessHandle.of(pid).isAlive()`.

- **Pro:** No persistent connection needed, works with any HTTP server
- **Con:** Detection delay equals the heartbeat interval

**Recommendation:** Use **Option C (Long-Polling with PID)** for the MVP. It's the simplest, requires no WebSocket, and works with the pure JDK `HttpServer`. The daemon also proactively checks client PIDs via `ProcessHandle`, so even if a client stops sending heartbeats (crash), the daemon detects it within seconds.

```java
// In the daemon's client tracker
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.scheduleAtFixedRate(() -> {
    for (ClientInfo client : clients.values()) {
        if (!ProcessHandle.of(client.getPid()).map(ProcessHandle::isAlive).orElse(false)) {
            // Client process is dead -- release its server references
            releaseAllForClient(client.getPid());
            clients.remove(client.getPid());
        }
    }
}, 0, 5, TimeUnit.SECONDS);  // Check every 5 seconds
```

### 6.3 Heartbeat from JIPipe to Daemon

JIPipe instances send a heartbeat to the daemon every N seconds:

```java
// In JIPipe's DaemonClientManager
ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();
heartbeat.scheduleAtFixedRate(() -> {
    try {
        HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(URI.create(daemonUrl + "/api/v1/heartbeat?clientPid=" + myPid))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.discarding()
        );
    } catch (Exception e) {
        // Daemon might be down -- try to reconnect or restart
        handleDaemonUnreachable();
    }
}, 0, 10, TimeUnit.SECONDS);  // Every 10 seconds
```

---

## 7. Integration with `exitLater()`

### 7.1 Graceful Release Before `halt()`

When JIPipe shuts down gracefully (via `exitLater()`), it must notify the daemon before calling `halt()`:

```java
public static void exitLater(int exitCode) {
    if (instance != null && instance.isAutosaveSettings()) {
        instance.getApplicationSettings().save();
    }
    // Release all server references via daemon API
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

`releaseAll()` sends `DELETE /api/v1/servers/{id}/release` for each server the instance is using. This is a synchronous HTTP call that completes in milliseconds.

### 7.2 Crash Scenario (No `exitLater()` Called)

If JIPipe crashes or is force-killed:
1. The heartbeat stops arriving at the daemon
2. The daemon's `ProcessHandle` check detects the JIPipe PID is dead
3. The daemon auto-releases all server references for that client
4. If no other clients need a server, the idle timer starts

**No special code is needed in JIPipe for this case.** The daemon handles it independently.

### 7.3 Fiji Plugin Mode

In Fiji plugin mode, closing the JIPipe window does NOT call `exitLater()` or `halt()`. Instead:
- `JIPipeDesktopProjectWindow.dispose()` is called
- The JVM (Fiji) keeps running

The server manager must handle this differently:

```java
// In JIPipeDesktopProjectWindow.dispose()
if (JIPipe.isInstantiated()) {
    JIPipe.getInstance().getServerManager().releaseAll();
}
```

Or better: the server manager should be lifecycle-aware and register a listener for project window close events.

---

## 8. State Directory Location

The daemon's state directory must be:
1. Accessible from all JIPipe instances (shared, not per-profile)
2. In a well-known location
3. Writable by the current user

**Recommended location:** `<JIPipeSharedDir>/manager/`

| OS | Path |
|----|------|
| Linux | `~/.local/share/JIPipe/shared/manager/` |
| Windows | `%APPDATA%\JIPipe\shared\manager\` |
| macOS | `~/Library/Application Support/JIPipe/shared/manager/` |

Overridable via `JIPIPE_OVERRIDE_SHARED_DIR` environment variable.

**State directory contents:**

```
<JIPipeSharedDir>/manager/
├── bootstrap.lock        # FileLocker for daemon startup mutual exclusion
├── daemon.pid            # Daemon process PID (for crash detection)
├── daemon.json           # Connection info: {pid, port, startedAt}
├── daemon.log            # Daemon log output (stdout/stderr redirect)
└── servers/              # Per-server state (optional, for crash recovery)
    ├── llama-cpp.json    # {pid, port, config, clients}
    └── ipython.json
```

---

## 9. Complete Flow: From JIPipe to Server in Use

### 9.1 First JIPipe Instance, No Daemon Running

```
1. JIPipe starts (IDE / launcher / Fiji plugin)
2. JIPipeService initializes, ServerManagerServiceComponent created
3. User opens a pipeline that uses LlamaCppServerEnvironment
4. Algorithm node calls: serverManager.acquire("llama-cpp", definition)
5. DaemonClientManager reads <sharedDir>/manager/daemon.json → not found
6. DaemonClientManager acquires bootstrap.lock
7. DaemonClientManager spawns daemon via ProcessBuilder:
     java -cp <java.class.path> -Xmx128m -Djipipe.daemon=true
          org.hkijena.jipipe.svm.core.DaemonMain --state-dir <sharedDir>/manager
8. Daemon starts, binds HTTP to 127.0.0.1:0 (OS assigns port 18080)
9. Daemon writes daemon.json: {pid: 45678, port: 18080}
10. DaemonClientManager reads daemon.json → port 18080
11. DaemonClientManager releases bootstrap.lock
12. DaemonClientManager verifies daemon health: GET :18080/api/v1/health → 200 OK
13. DaemonClientManager registers client: POST :18080/api/v1/clients/register {pid: 67890}
14. DaemonClientManager acquires server:
      POST :18080/api/v1/servers/llama-cpp/acquire {definition: ...}
15. Daemon spawns llama-server process, waits for health check
16. Daemon returns: {status: "running", port: 18081}
17. Algorithm node uses llama.cpp via HTTP on port 18081
18. DaemonClientManager starts heartbeat thread (10s interval)
```

### 9.2 Second JIPipe Instance, Daemon Already Running

```
1. JIPipe instance B starts
2. User opens a pipeline that uses LlamaCppServerEnvironment
3. Algorithm node calls: serverManager.acquire("llama-cpp", definition)
4. DaemonClientManager reads daemon.json → found (port 18080, pid 45678)
5. DaemonClientManager checks ProcessHandle.of(45678).isAlive() → true
6. DaemonClientManager verifies daemon health: GET :18080/health → 200 OK
7. DaemonClientManager registers client: POST :18080/api/v1/clients/register {pid: 67891}
8. DaemonClientManager acquires server:
     POST :18080/api/v1/servers/llama-cpp/acquire {definition: ...}
9. Daemon checks: llama-cpp already running, same config → reuse
   OR: different config → stop old server, start new one (version upgrade)
10. Daemon returns: {status: "running", port: 18081}
11. Algorithm node uses same llama.cpp server on port 18081
12. DaemonClientManager starts heartbeat thread
```

### 9.3 First Instance Crashes

```
1. JIPipe instance A crashes (segfault, kill -9, halt())
2. Heartbeat from instance A stops
3. Daemon's PID monitor (5s interval):
     ProcessHandle.of(67890).isAlive() → false
4. Daemon auto-releases all server refs for client 67890
5. Daemon checks: client 67891 (instance B) still alive → keep llama-cpp running
6. JIPipe instance B continues using llama-cpp uninterrupted
```

### 9.4 Both Instances Exit Gracefully

```
1. JIPipe instance B: exitLater() → releaseAll() →
     DELETE :18080/api/v1/servers/llama-cpp/release {clientPid: 67891}
2. Daemon: ref count for llama-cpp → 0, start idle timer (5 minutes)
3. JIPipe instance A: exitLater() → releaseAll() →
     DELETE :18080/api/v1/servers/llama-cpp/release (already released)
4. Daemon: idle timer expires, stop llama-cpp server process
5. Daemon: no clients → start daemon idle timer
6. Daemon: daemon idle timer expires (5 minutes no clients) → System.exit(0)
7. Daemon shutdown hook: clean up daemon.json, daemon.pid
```

---

## 10. Edge Cases and Failure Modes

### 10.1 Daemon Crashes

If the daemon crashes (JVM error, OS kill):
1. Managed server processes are orphaned (their parent PID is gone)
2. JIPipe instances detect daemon failure (heartbeat HTTP request fails)
3. Next JIPipe instance to need a server triggers bootstrap (Section 4)
4. New daemon reads server state files, discovers orphans:
   - Checks if each server PID is alive + port is reachable
   - Adopts or terminates orphans based on current needs
5. If no JIPipe instance restarts the daemon, orphaned server processes continue running
   until manually killed or OS shutdown

**Mitigation:** The daemon writes server state to `<sharedDir>/manager/servers/*.json` on every state change. This allows crash recovery.

### 10.2 Daemon Port Conflict

If another application is using the port the daemon selected:
- The daemon uses port 0 (OS assigns), so this should never happen
- If it does happen (extremely rare), the daemon logs the error and exits
- The JIPipe instance detects daemon failure and retries with a new spawn

### 10.3 Classpath Too Long (Windows)

On Windows, the maximum command line length is ~32,767 characters. JIPipe's classpath with all plugins could exceed this limit.

**Mitigation:** Use a classpath manifest JAR (a small JAR whose `MANIFEST.MF` contains `Class-Path:` entries pointing to all other JARs). This is a standard Java pattern:

```java
// Create a temporary manifest JAR that references all classpath entries
Path manifestJar = stateDir.resolve("daemon-classpath.jar");
Manifest manifest = new Manifest();
Attributes attrs = manifest.getMainAttributes();
attrs.put(Attributes.Name.MAIN_CLASS, "org.hkijena.jipipe.svm.core.DaemonMain");
attrs.put(Attributes.Name.CLASS_PATH, classpathEntriesAsString);
try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(manifestJar), manifest)) {
    // Empty JAR -- only the manifest matters
}

// Spawn with just the manifest JAR
ProcessBuilder pb = new ProcessBuilder(
    javaExec, "-jar", manifestJar.toAbsolutePath().toString(), ...
);
```

This reduces the command line to: `java -jar daemon-classpath.jar --state-dir ...`

### 10.4 Fiji Mode: JIPipe Not Started via ImageJ Launcher

In some Fiji configurations, JIPipe might be started via a different mechanism (e.g., Jython script, other SciJava plugins). The `java.class.path` property still contains the full classpath because the ImageJ native launcher sets it at JVM startup, regardless of how JIPipe is triggered within the running Fiji instance.

### 10.5 Multiple Daemons (Different JIPipe Versions)

If two versions of JIPipe are installed and running simultaneously, they might each try to start a daemon. The daemon state directory is under `JIPipeSharedDir`, which is **version-independent** by design. This means both versions would share the same daemon.

**Problem:** The daemon's classpath comes from the JIPipe version that started it. If version A starts the daemon, version B's `DaemonMain` class might be different.

**Mitigation:** The `daemon.json` file includes the JIPipe version and the daemon API version. When a JIPipe instance connects, it checks compatibility. If incompatible:
1. Request the daemon to shut down (via API)
2. Wait for it to stop
3. Start a new daemon with the current version's classpath

This is a rare edge case (two JIPipe versions running simultaneously) and can be handled with a simple version check.

---

## 11. Summary: What Changed vs the Previous Addendum

| Aspect | Previous Addendum | This Addendum |
|--------|-------------------|---------------|
| **Daemon spawning** | Vague ("ProcessBuilder") | Explicit: `java.home` + `java.class.path` via `ProcessBuilder` |
| **Classpath source** | Not specified | `System.getProperty("java.class.path")` with ImageJ directory scan fallback |
| **OS independence** | Not addressed | Detailed: `javaw.exe` on Windows, `java` on Unix, `File.pathSeparator` |
| **Windows classpath length** | Not addressed | Manifest JAR workaround |
| **Daemon self-identification** | Not addressed | `-Djipipe.daemon=true` system property |
| **Shutdown behavior** | "Graceful" | Explicit: daemon uses `System.exit()` (not `halt()`), shutdown hooks always run |
| **Client liveness** | "TCP keepalive" | Concrete: `ProcessHandle` polling (5s) + HTTP heartbeat (10s) |
| **Fiji plugin mode** | Briefly mentioned | Explicit cleanup in `JIPipeDesktopProjectWindow.dispose()` |
| **Version compatibility** | Not addressed | `daemon.json` includes version, incompatible daemons are restarted |
