# General Instrumentation Layer Design

**Work item:** #1305  
**Branch:** `1305-instrumentation-layer` (in jipipe-3, off `master`)  
**Date:** 2026-08-13  

## Overview

A general-purpose, extensible instrumentation layer that exposes JIPipe project operations (queries, pipeline execution, modifications, search) via a WebSocket API for external automation and diagnostic tools. The layer extracts the underlying primitives currently embedded in the AI agent system (jipipe-2, branch `1290-ai-llm-integration`) into a standalone, AI-independent API. When the AI code is later merged onto this base, the AI agent will use the instrumentation API under the hood, and AI-specific commands/events will be registered as extensions to the same operation registry and event bus.

## Motivation

The AI agent system in jipipe-2 already has the capabilities needed for external automation: running pipelines, modifying projects, extracting information, and a WebSocket-based monitor (`AgentMonitorServer`). These capabilities are tightly coupled to the AI agent's internal architecture (stages, approvals, sessions, LLM functions). By extracting the underlying primitives into a standalone instrumentation layer, we:

1. Enable external tools (test harnesses, CI scripts, diagnostic dashboards) to control JIPipe without the AI agent
2. Provide a clean, typed Java API that the AI agent delegates to, eliminating duplicated logic
3. Allow the AI agent to register its own commands as extensions to the same infrastructure
4. Build the foundation on `master` (jipipe-3) before the AI code is merged, avoiding refactoring conflicts

## Architecture: Hybrid (Core API + Operation Registry)

The layer combines a typed Java facade with a JSON-based operation registry:

- **`InstrumentationAPI`** — typed Java methods for all built-in operations. The AI agent calls these directly.
- **`InstrumentationOperation`** — interface for JSON-in/JSON-out operations. The WebSocket server dispatches to these. Built-in operations wrap `InstrumentationAPI` methods; plugin operations are fully custom.
- **`InstrumentationOperationRegistry`** — holds all operations (built-in + plugin-registered). The WebSocket server and Java API both use the same underlying implementation.
- **`InstrumentationEventBus`** — publish-subscribe event system. The WebSocket server subscribes and broadcasts events to clients.
- **`InstrumentationJobManager`** — tracks async jobs (pipeline runs) with job IDs, progress, and completion notification via events.

### Data flow

```
External tool ──WebSocket──> InstrumentationServer ──> OperationRegistry ──> Operation
                                                                         │
                                                                         v
                                                                    InstrumentationAPI
                                                                         │
                                                                         v
                                                                    JIPipeProject / JIPipeGraph
                                                                         │
                                                                         v
                                                                    InstrumentationEventBus
                                                                         │
                                                                    <─────┘
                                                         InstrumentationServer ──> broadcast events ──> External tool

AI agent ──Java call──> InstrumentationAPI ──> JIPipeProject / JIPipeGraph
                                                         │
                                                         v
                                                    InstrumentationEventBus
```

## Package Structure

All code lives in `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/`:

```
api/instrumentation/
├── InstrumentationAPI.java                  # Typed Java facade
├── InstrumentationOperation.java            # Interface: getId(), getDescription(), execute(ctx, params)
├── AsyncInstrumentationOperation.java       # Extension for async ops (returns JobHandle)
├── InstrumentationOperationRegistry.java    # Registry of all operations
├── InstrumentationContext.java              # Context: project, workbench, progress, eventBus, jobManager
├── InstrumentationEventBus.java             # Event distribution
├── InstrumentationEvent.java                # Base event class
├── InstrumentationJob.java                  # Async job handle (id, status, progress, result)
├── InstrumentationJobStatus.java            # PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
├── InstrumentationJobManager.java           # Creates and tracks async jobs
├── InstrumentationRunEngine.java            # Core run logic (extracted from JIPipeDesktopQuickRun)
├── RunMode.java                             # Enum: UPDATE_CACHE, CACHE_INTERMEDIATE, etc.
├── InstrumentationServer.java               # WebSocket server (localhost, dispatches to registry)
├── InstrumentationProtocol.java             # Message type constants + DTO records
├── InstrumentationApplicationSettings.java  # Settings (enable, port, auto-start)
├── InstrumentationPlugin.java              # Registers built-in operations (extends JIPipePrepackagedDefaultJavaPlugin)
├── operations/
│   ├── QueryOperations.java                 # query_compartments, query_graph, query_node, query_data
│   ├── ExecutionOperations.java             # run_node, run_compartment, run_pipeline
│   ├── ModificationOperations.java          # add_node, remove_node, add_connection, etc.
│   ├── PipelineMapOperations.java           # get_pipeline_map, get_segment_detail, search_nodes
│   └── ProjectOperations.java               # list_projects, select_project, open_project, new_project
├── events/
│   ├── ProjectListChangedEvent.java
│   ├── ProjectChangedEvent.java
│   ├── NodeAddedEvent.java
│   ├── NodeRemovedEvent.java
│   ├── ConnectionChangedEvent.java
│   ├── ParameterChangedEvent.java
│   ├── CompartmentChangedEvent.java
│   ├── JobStartedEvent.java
│   ├── JobProgressEvent.java
│   └── JobCompletedEvent.java
└── pipeline_map/
    ├── PipelineMap.java                     # POJO (moved from api/ai/pipeline_map/)
    ├── SegmentNode.java
    ├── SegmentEdge.java
    └── SegmentStatus.java
```

## Core Interfaces

### InstrumentationOperation

```java
public interface InstrumentationOperation {
    String getId();
    String getDescription();
    JsonNode execute(InstrumentationContext ctx, JsonNode params) throws Exception;
}
```

### AsyncInstrumentationOperation

```java
public interface AsyncInstrumentationOperation extends InstrumentationOperation {
    InstrumentationJob executeAsync(InstrumentationContext ctx, JsonNode params) throws Exception;
}
```

Operations like `run_node`, `run_compartment`, `run_pipeline` implement this. The server detects async operations and returns a `job_started` response immediately, then broadcasts `job_progress` and `job_completed` events.

### InstrumentationContext

```java
public class InstrumentationContext {
    private final JIPipeProject project;
    private final JIPipeGraph graphOverride;  // null = use project graph; non-null = stage working graph
    private final JIPipeProgressInfo progressInfo;
    private final InstrumentationEventBus eventBus;
    private final InstrumentationJobManager jobManager;
    private final JIPipeDesktopProjectWorkbench workbench;  // null in headless mode

    // The graphOverride allows the AI agent's stage system to pass a stage's
    // working graph instead of the project graph. External WebSocket tools
    // always use the project graph (graphOverride is null).
    public JIPipeGraph getGraph() {
        return graphOverride != null ? graphOverride : (project != null ? project.getGraph() : null);
    }
}
```

### InstrumentationAPI

The API is stateless for project-scoped operations — each method takes an `InstrumentationContext` (which carries the project). Project management operations (`listProjects`, `selectProject`, `openProject`, `newProject`) are handled by the `InstrumentationServer`, which tracks open project windows. Key methods:

```java
// Project management
List<ProjectInfo> listProjects();
ProjectInfo selectProject(String projectId);
ProjectInfo openProject(Path path);
ProjectInfo newProject();

// Queries
JsonNode queryCompartments(InstrumentationContext ctx);
JsonNode queryGraph(InstrumentationContext ctx, String compartmentId);
JsonNode queryNode(InstrumentationContext ctx, String nodeId);
JsonNode queryData(InstrumentationContext ctx, String nodeId, String slotName, int limit);

// Execution (async)
InstrumentationJob runNode(InstrumentationContext ctx, String nodeId, RunMode mode);
InstrumentationJob runCompartment(InstrumentationContext ctx, String compartmentId);
InstrumentationJob runPipeline(InstrumentationContext ctx, RunMode mode);

// Modifications
String addNode(InstrumentationContext ctx, String compartmentId, String nodeTypeId, int x, int y);
void removeNode(InstrumentationContext ctx, String nodeId);
void addConnection(InstrumentationContext ctx, String source, String sourceSlot, String target, String targetSlot);
void removeConnection(InstrumentationContext ctx, String source, String sourceSlot, String target, String targetSlot);
void setParameter(InstrumentationContext ctx, String nodeId, String key, String value);
void setProjectMetadata(InstrumentationContext ctx, String name, String description);
String addCompartment(InstrumentationContext ctx, String name);
void renameCompartment(InstrumentationContext ctx, String compartmentId, String name);

// Pipeline map
JsonNode getPipelineMap(InstrumentationContext ctx);
JsonNode getSegmentDetail(InstrumentationContext ctx, String segmentId);

// Node search
JsonNode searchNodes(InstrumentationContext ctx, String query, int limit);
```

## WebSocket Protocol

Same JSON message pattern as the current `AgentMonitorServer`. All messages are JSON objects with a `"type"` field. Commands may include an optional `"requestId"` for response correlation.

### Commands (client to server)

| Command | Parameters | Response type | Async? |
|---|---|---|---|
| `list_projects` | — | `project_list` | No |
| `select_project` | `projectId` | `project_changed` | No |
| `open_project` | `path` | `project_changed` | No |
| `new_project` | — | `project_changed` | No |
| `query_compartments` | — | `operation_result` | No |
| `query_graph` | `compartmentId?` | `operation_result` | No |
| `query_node` | `nodeId` | `operation_result` | No |
| `query_data` | `nodeId, slotName, limit?` | `operation_result` | No |
| `run_node` | `nodeId, mode` | `job_started` | Yes |
| `run_compartment` | `compartmentId` | `job_started` | Yes |
| `run_pipeline` | `mode` | `job_started` | Yes |
| `add_node` | `compartmentId, nodeTypeId, x?, y?` | `operation_result` | No |
| `remove_node` | `nodeId` | `operation_result` | No |
| `add_connection` | `source, sourceSlot, target, targetSlot` | `operation_result` | No |
| `remove_connection` | `source, sourceSlot, target, targetSlot` | `operation_result` | No |
| `set_parameter` | `nodeId, key, value` | `operation_result` | No |
| `set_project_metadata` | `name?, description?` | `operation_result` | No |
| `add_compartment` | `name` | `operation_result` | No |
| `rename_compartment` | `compartmentId, name` | `operation_result` | No |
| `get_pipeline_map` | — | `operation_result` | No |
| `get_segment_detail` | `segmentId` | `operation_result` | No |
| `search_nodes` | `query, limit?` | `operation_result` | No |
| `list_operations` | — | `operation_list` | No |
| `get_job_status` | `jobId` | `operation_result` | No |
| `cancel_job` | `jobId` | `operation_result` | No |

### Events (server to client)

| Event | Fields | Trigger |
|---|---|---|
| `project_list` | `projects: [...]` | `list_projects` command or window open/close |
| `project_changed` | `projectId, name` | `select_project`, `open_project`, `new_project` |
| `operation_result` | `requestId?, data: {...}` | Synchronous operation completes |
| `operation_list` | `operations: [{id, description, async}]` | `list_operations` command |
| `node_added` | `compartmentId, nodeId, nodeTypeId, name` | `add_node` operation |
| `node_removed` | `nodeId` | `remove_node` operation |
| `connection_changed` | `source, sourceSlot, target, targetSlot, action` | `add_connection`/`remove_connection` |
| `parameter_changed` | `nodeId, key, value` | `set_parameter` operation |
| `compartment_changed` | `compartmentId, name, action` | `add_compartment`/`rename_compartment` |
| `job_started` | `jobId, operation, description, requestId?` | Async operation starts |
| `job_progress` | `jobId, progress, message` | Async operation reports progress |
| `job_completed` | `jobId, status, result?, error?` | Async operation finishes |
| `error` | `requestId?, message` | Any operation fails |

## Run Architecture

### RunMode enum

```java
public enum RunMode {
    UPDATE_CACHE,              // Run to target node, cache only target output
    CACHE_INTERMEDIATE,        // Run to target node, cache all intermediate outputs
    UPDATE_PREDECESSOR_CACHE,  // Run to predecessors of target (skip target itself)
    PIPELINE_CACHE,            // Run entire pipeline, cache in memory only
    PIPELINE_FILESYSTEM,       // Run entire pipeline, write to output folder
    PIPELINE_DISCARD           // Run entire pipeline, discard results
}
```

### InstrumentationRunEngine

Extracts the core run logic from `JIPipeDesktopQuickRun` (which stays as a thin desktop wrapper). The run engine:

1. Creates `JIPipeGraphRunConfiguration` with settings appropriate for the `RunMode`
2. Creates a `JIPipeGraphRun` (existing core API class in `api/run/`)
3. Handles predecessor finding (which nodes to run vs. skip based on cache availability)
4. Handles cache clearing for target nodes
5. Runs the `JIPipeGraphRun`
6. Reports progress through `InstrumentationJob`

**Configuration mapping:**

| RunMode | storeToCache | storeToDisk | storeIntermediate | excludeSelected |
|---|---|---|---|---|
| UPDATE_CACHE | true | false | false | false |
| CACHE_INTERMEDIATE | true | false | true | false |
| UPDATE_PREDECESSOR_CACHE | true | false | false | true |
| PIPELINE_CACHE | true | false | false | false |
| PIPELINE_FILESYSTEM | true | true | false | false |
| PIPELINE_DISCARD | false | false | false | false |

The predecessor-finding logic (checking cache availability, finding nodes that need re-execution) is currently in `JIPipeDesktopQuickRun.findPredecessorsWithoutCache()` and moves to `InstrumentationRunEngine`.

## Event System

### InstrumentationEventBus

Simple publish-subscribe:

```java
public class InstrumentationEventBus {
    <T extends InstrumentationEvent> void subscribe(Class<T> eventType, Consumer<T> listener);
    void publish(InstrumentationEvent event);
}
```

### Event hierarchy

```
InstrumentationEvent
├── ProjectListChangedEvent     // project window opened/closed
├── ProjectChangedEvent         // selected project changed
├── NodeAddedEvent              // node added to graph
├── NodeRemovedEvent            // node removed from graph
├── ConnectionChangedEvent      // connection added/removed
├── ParameterChangedEvent       // parameter value changed
├── CompartmentChangedEvent     // compartment added/removed/renamed
├── JobStartedEvent             // async job started
├── JobProgressEvent            // async job progress update
└── JobCompletedEvent           // async job finished (success or failure)
```

### Event sources

- **Modification operations** publish `NodeAddedEvent`, `NodeRemovedEvent`, etc. after successful modifications
- **`InstrumentationJobManager`** publishes `JobStartedEvent`, `JobProgressEvent`, `JobCompletedEvent`
- **JIPipe project window lifecycle** publishes `ProjectListChangedEvent`

### Event broadcasting

The `InstrumentationServer` subscribes to the `InstrumentationEventBus` for the currently selected project and serializes each event as JSON to all connected WebSocket clients. When the selected project changes, the server unsubscribes from the old project's event bus and subscribes to the new one.

## Job Manager

### InstrumentationJob

```java
public class InstrumentationJob {
    private final String id;                    // "job-{uuid-prefix}"
    private final String operation;             // e.g. "run_node"
    private final String description;           // human-readable
    private volatile InstrumentationJobStatus status;  // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private volatile double progress;           // 0.0 - 1.0
    private volatile String message;
    private volatile JsonNode result;           // result data on completion
    private volatile String error;              // error message on failure
    private final Instant createdAt;
    private volatile Instant completedAt;
    private final Thread thread;                // for cancellation

    void updateProgress(double progress, String message);
    void complete(JsonNode result);
    void fail(String error);
    void cancel();
}
```

### Async flow

1. Client sends command (e.g., `run_node`)
2. Server finds operation in registry, detects it implements `AsyncInstrumentationOperation`
3. Server calls `operation.executeAsync(ctx, params)` which:
   - Creates a `InstrumentationJob` via `ctx.getJobManager().createJob("run_node", "Update cache: NodeName")`
   - Spawns a background thread that runs the pipeline
   - Returns the `InstrumentationJob` immediately
4. Server sends `job_started` event to client
5. During execution, progress is reported via `job.updateProgress()` → server broadcasts `job_progress`
6. On completion: `job.complete(result)` → server broadcasts `job_completed` with status `COMPLETED`
7. On failure: `job.fail(error)` → server broadcasts `job_completed` with status `FAILED`

### Cancellation

`cancel_job` command interrupts the job's thread. The pipeline run checks `Thread.interrupted()` and aborts gracefully.

## Server Lifecycle

### Startup

The `InstrumentationServer` is started via CLI flag:

```bash
# Default port (8780)
./jipipe --instrumentation

# Custom port
./jipipe --instrumentation 9100
```

Lifecycle:
1. `GuiCommand` parses `--instrumentation [port]` flag
2. After JIPipe initializes and first project window opens, `InstrumentationServer` is created and started
3. Server binds to `127.0.0.1:{port}` (configurable via system property `jipipe.instrumentation.bind`)
4. Server registers as a project window listener to track open/close events
5. Server broadcasts `project_list` to connected clients when windows change

### Settings (InstrumentationApplicationSettings)

| Setting | Type | Default | Description |
|---|---|---|---|
| `enable-instrumentation` | boolean | false | Enable instrumentation server |
| `port` | int | 8780 | WebSocket port |
| `auto-start` | boolean | false | Start server when JIPipe launches |

### Multiple projects

The server tracks all open project windows. Clients:
1. Call `list_projects` to receive all open projects
2. Call `select_project` to subscribe to that project's event bus
3. All subsequent commands operate on the selected project
4. Events from the selected project are broadcast to the client

### Multiple clients

The server supports multiple simultaneous WebSocket connections. Each connection has its own selected project. Events from a project are broadcast to all clients that have that project selected.

### Desktop integration

- Status bar indicator shows server status (running/stopped, port, connected clients)
- Menu item: Tools > Instrumentation Server (start/stop)
- Settings panel in application settings

## Extensibility

### Plugin registration

Plugins register operations via the `JIPipeDefaultJavaPlugin` convenience method:

```java
// In any JIPipeJavaPlugin subclass register() method:
protected void registerInstrumentationOperation(String id, InstrumentationOperation op) {
    getJIPipe().getInstrumentationOperationRegistry().register(id, op);
}
```

### Built-in operations

Registered by `InstrumentationPlugin` (extends `JIPipePrepackagedDefaultJavaPlugin`). This plugin is part of jipipe-core and is always loaded.

### AI-registered operations (when AI is merged)

The AI plugin registers additional operations:

| Operation | Description |
|---|---|
| `send_message` | Send a message to the AI agent |
| `create_session` | Create a new AI chat session |
| `delete_session` | Delete an AI chat session |
| `switch_mode` | Switch the AI agent's mode |
| `respond_user_input` | Respond to an AI user input request |
| `respond_stage_approval` | Respond to an AI stage approval request |
| `search_pipeline` | Semantic search over pipeline segments (uses embeddings) |
| `search_knowledge` | Search the knowledge database |
| `search_api` | Search the API database |
| `get_session_diagnostic` | Get AI session diagnostic info |

AI-specific events (message_added, state_changed, tool_call, llm_response, etc.) are published on the `InstrumentationEventBus` as custom event types and broadcast to WebSocket clients.

## AI Agent Adaptation (jipipe-2)

When the instrumentation layer is merged into jipipe-2's AI branch (`1290-ai-llm-integration`):

### Replaced components

| Current component | Replaced by |
|---|---|
| `AgentMonitorServer` | `InstrumentationServer` |
| `ProjectQueryService` | `InstrumentationAPI` query methods |
| `AgentMonitorProtocol` | `InstrumentationProtocol` |

### Refactored components

| Current component | Change |
|---|---|
| `RunFunctions` | Delegates to `InstrumentationAPI.runNode()` etc. User confirmation stays in AI code. |
| `StageFunctions` | Uses `InstrumentationAPI` for graph operations (addNode, addConnection, etc.) via `InstrumentationContext` with `graphOverride` set to the stage's working graph. Stages/approvals stay AI-specific. |
| `PipelineMapFunctions` | Map building delegates to `InstrumentationAPI.getPipelineMap()`. Semantic search stays as AI-registered operation. |
| `JIPipeDesktopQuickRun` | Core run logic extracted to `InstrumentationRunEngine`. Desktop wrapper stays as thin UI adapter. |

### What stays AI-specific

- Stages and approvals (`PipelineStage`, `StageItem`, `StageReviewResult`, approval flow)
- Agent sessions (`AgentSession`, `AgentSessionConfig`, agentic loop)
- Protocol engine (state machine, `ProtocolFunctions`)
- LLM client (`LLMClient`, streaming, retry logic)
- Knowledge/API database (embedding search, knowledge DB)
- Permissions (`PermissionSet`)
- User input requests (`ChatInputRequest`, `CompletableFuture<ChatInputResult>`)
- User confirmation flow for pipeline runs

### Error handling

Commands that require a selected project (all query, execution, and modification commands) return an error if no project is selected:

```json
{"type": "error", "requestId": "req-1", "message": "No project selected. Call list_projects and select_project first."}
```

Unknown command types return:

```json
{"type": "error", "requestId": "req-1", "message": "Unknown command: <type>"}
```

Operation exceptions are caught and returned as errors with the exception message.

## Security

- **Bind address:** `127.0.0.1` (localhost only, configurable via system property `jipipe.instrumentation.bind`)
- **Authentication:** None (same as current `AgentMonitorServer`)
- **Rationale:** Local automation tools; no remote access needed

## Dependencies

The instrumentation layer uses only existing jipipe-core dependencies:
- **Jackson** — JSON serialization (already used throughout)
- **java-websocket** — WebSocket server (already used by `AgentMonitorServer`)
- **JIPipeGraphRun** — core pipeline execution (already in `api/run/`)

No new external dependencies are required.
