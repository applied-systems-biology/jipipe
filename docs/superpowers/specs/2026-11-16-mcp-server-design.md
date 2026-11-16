# MCP server - design

Date: 2026-11-16
Work item: #1369
Related: #1290 (AI LLM integration, shared server system), #1348 (release strategy, context only)

JIPipe replaces its rudimentary WebSocket instrumentation API with a proper MCP (Model
Context Protocol) server so AI agents (Claude Code, Cursor, and future JIPipe-internal
consumers such as the in-app AI chat, documentation generation, and the website) can drive
JIPipe: explore projects, nodes, and docs, read and edit graphs, run pipelines and cache
operations, explore data and errors, and capture documentation-grade screenshots.

The operation layer built for this is the reusable core: MCP is the first transport, not
the only consumer. The inter-instance IPC introduced here is intentionally designed to be
reused when GUI and run processes are separated later.

## Confirmed decisions

- Full replacement: the MCP server plus a clean operation layer replaces the
  instrumentation API in this work item. The in-app AI agent migrates to the new layer;
  the WebSocket instrumentation is deleted.
- Transport: MCP Streamable HTTP via the official MCP Java SDK, embedded in a broker
  process, endpoint `http://127.0.0.1:<port>/mcp`.
- Broker topology: one shared broker process serves all running JIPipe instances
  (elected-host rejected: closing the wrong instance must not kill the endpoint). The
  broker is spawned on demand, refcounted, and uses the stable port from application
  settings, so desktop users configure MCP in their agent once.
- v1 scope: foundation (server, permissions, project targeting, statusbar UI, logging)
  plus the docs-grade core tool set. Deferred: browse-style UI interaction, opening or
  creating projects, per-data-type rich visualization tools.
- Permission defaults: read-only tools Allow, mutating and execution tools Ask.
- Localhost security: optional access token (setting, default off).
- Ask prompts originate from the MCP statusbar button. The project window notification
  inbox is not in active use anymore and is not used for this.

## Non-goals (v1)

- No browse-style UI interaction (clicking controls, typing text, opening menus). The
  work item lists it as a future feature; it is a follow-up work item.
- No open/create/close project operations (work item: "default: ask"). Follow-up.
- No MCP `resources`/`prompts` capabilities; tools only.
- No persisted permission grants; "Allow always" is per project and dies with the
  instance.
- No headless/run-process separation yet; the IPC layer only prepares for it.

## 1. Architecture overview

```
MCP clients (Claude Code, Cursor, ...)
        |  MCP Streamable HTTP, 127.0.0.1:<port>/mcp, optional Bearer token
        v
+----------------------------------------------------------+
| MCP broker (headless JVM, spawned on demand)             |
|  - official MCP Java SDK (McpServer.sync) + Jetty        |
|  - instance registry: projects, capabilities, op ports   |
|  - router: tool call -> owning instance                  |
|  - SharedServerProcess base: discovery file, PID         |
|    monitors, refcounted exit (1290 pattern)              |
+---------------------------+------------------------------+
                            | internal HTTP/JSON (127.0.0.1, ephemeral instance port)
                            v
+----------------------------------------------------------+
| JIPipe instance (GUI JVM), one per running JIPipe        |
|  - Automation API (ex InstrumentationAPI): operations,   |
|    registry, context; the reusable core                  |
|  - Instance endpoint (JDK HttpServer, /op/<id>):         |
|    enforces permissions, executes operations             |
|  - Permission service (Allow/Ask/Deny/Hide, grants)      |
|  - Broker client (spawn if needed, register, update)     |
|  - Desktop UI (statusbar button, popup, settings, log)   |
+----------------------------------------------------------+
```

Packages:

- `org.hkijena.jipipe.api.automation`: `JIPipeAutomationAPI`, `AutomationOperation`,
  `AutomationOperationRegistry`, `AutomationContext`, permission service; operations in
  `api/automation/operations/`.
- `org.hkijena.jipipe.api.automation.endpoint`: `AutomationInstanceEndpoint` (JDK
  `com.sun.net.httpserver.HttpServer` on 127.0.0.1, ephemeral port),
  `MCPBrokerClient` (spawn, register, keep project list current).
- `org.hkijena.jipipe.servers.mcp`: `MCPServerProcess` (extends the ported
  `SharedServerProcess`), `MCPBrokerServer` (MCP SDK + Jetty), `MCPInstanceRegistry`,
  routing, shared DTOs.
- `org.hkijena.jipipe.plugins.mcp`: plugin registration, desktop UI
  (`JIPipeDesktopMCPStatusControl`), `MCPApplicationSettings`.

## 2. Broker process

Follows the LLM proxy of the #1290 branch (`LLMProxyProcess`, `LLMProxyClient`,
`SharedServerProcess`, `SharedServerDiscoveryFile`) exactly.

Lifecycle:

1. A JIPipe instance with MCP enabled runs its broker client: read the discovery file
   `<user-dir>/mcp/discovery.json` (JSON: pid, port, started-at, version); if the PID is
   alive and `/health` answers, connect; otherwise delete the stale file and spawn
   `java @argfile ... MCPServerProcess --discovery-file <path> --parent-pid <pid>
   --port <settings-port>` (same classpath via `java.class.path`, same argfile mechanism,
   same 30 s health poll deadline as `LLMProxyClient.spawnProxy`).
2. The instance registers (see section 3) and re-registers whenever its set of open
   projects changes; it unregisters on MCP disable and on exit.
3. Broker exit conditions (identical to `SharedServerProcess`): parent PID dead and no
   instances; or the last instance disappeared after a grace period.
4. Port policy: the configured port from application settings (default 8781) is preferred
   so agent configurations never change. If it is occupied, the broker falls back to
   `PortManager.assignFreePort()`, logs loudly, and the instance UI shows the actual URL.
   The optional token protects against accidentally talking to a wrong broker.

The broker is stateless with respect to projects: it aggregates registrations and routes.
MCP sessions live in the broker (SDK session management).

## 3. JIPipe instance side

- `MCPBrokerClient` mirrors `LLMProxyClient`: ensureConnected, register, unregister,
  health check, all JSON over `HttpURLConnection` to 127.0.0.1.
- `AutomationInstanceEndpoint` is a small JDK `HttpServer` bound to 127.0.0.1 on an
  ephemeral port. Routes: `POST /op/<operation-id>` (execute), `GET /health`. The instance
  generates a random secret at endpoint start and hands it to the broker inside the
  registration payload; the broker must present it on every `/op/...` call so other local
  users cannot drive instances directly.
- Registration payload: instance id (UUID), JIPipe PID, endpoint port, endpoint secret,
  application version, and the project list (key, display name, path). Registration is
  refreshed when projects open or close.
- Operations execute through `JIPipeAutomationAPI`; Swing-touching operations are
  marshalled to the EDT and the endpoint thread waits for completion.

## 4. Operation layer

- `AutomationOperation` keeps `getId()`, `getDescription()`, `execute(ctx)` and gains:
  - `getInputSchema()`: a declared JSON schema for the arguments (instrumentation
    operations parsed ad-hoc JSON; MCP requires declared schemas).
  - annotations: `category` (query, modify, execute, render, meta), `readOnly` (feeds
    permission defaults and MCP `readOnlyHint`/`destructiveHint`), and an agent-facing
    documentation string.
- `AutomationOperationRegistry` remains plugin-extensible
  (`registerAutomationOperation(id, op)`) so plugins can contribute operations later
  (for example per-data-type visualization tools).
- `AutomationContext` resolves the target project per call from the `project` argument
  (no session-scoped `select_project`): the instance endpoint maps the project id to the
  open `JIPipeDesktopProjectWindow` and builds the context (graph override, event bus,
  job manager as today).
- Project ids are broker-minted `<instanceId>:<projectKey>` strings; `list_projects` is
  aggregated across all registered instances.
- The in-app AI agent re-points from `InstrumentationAPI` statics to
  `JIPipeAutomationAPI` (same call shapes). Its function-call surface can later be
  generated from the same registry and schemas.
- `list_operations` is dropped as a tool (MCP `tools/list` is that); the registry keeps
  it as an internal query.

## 5. Tool catalog (v1)

Name = operation id. Defaults: A = Allow, Ask = Ask. "migrated" = exists as an
instrumentation operation today, migrated with a schema added.

| Group | Tools | Default |
|---|---|---|
| Projects | `list_projects` (migrated, aggregated) | A |
| Query | `query_compartments`, `query_graph`, `query_node` (migrated), `get_project_info` | A |
| Graph map | `get_pipeline_map`, `get_segment_detail`, `search_nodes` (migrated) | A |
| Algorithms | `list_algorithms`, `get_node_docs` (node registry, `@JIPipeDocumentation`, taxonomy) | A |
| Data | `query_data` (migrated), `describe_data` (type, dimensions, image preview), `export_data` (writes to the export/temp directory, returns path) | A |
| Screenshots | `screenshot_canvas`, `screenshot_minimap`, `screenshot_ui` (named UI surface), `screenshot_window` (optional fake titlebar) | A |
| Errors/logs | `get_errors` (validation and last-run errors), `get_log` (progress log tail/filter) | A |
| Modify | `add_node`, `remove_node`, `add_connection`, `remove_connection`, `set_parameter`, `set_project_metadata`, `add_compartment`, `rename_compartment` (migrated), `arrange_nodes` (reuses the canvas layout code) | Ask |
| Execute | `run_node`, `run_compartment`, `run_pipeline` (migrated, return job ids), `get_job_status`, `cancel_job`, `update_cache` (scope node/compartment/pipeline; mode full/intermediate/predecessors), `create_ro_crate` (migrated) | Ask |

That is 35 tools: 20 migrated with schemas added (the current 21 operations minus
`list_operations`), and 15 new (project info, algorithm docs, data describe/export,
screenshots, errors/logs, jobs, cache, arrange). Execution and long-running modify tools
are asynchronous via the existing job system and report through MCP progress
notifications.

## 6. Permissions

Per tool, four states:

- Allow: executes silently (default for read-only).
- Ask: a prompt bubble anchored to the MCP statusbar button of the target project window
  (same statusbar pattern as the AI control and author button). Answers: Allow once,
  Allow always (this project), Deny. The agent blocks on the tool call until answered;
  there is no timeout by default. If the window is minimized, the bubble shows when it is
  restored; the request is listed in the popup as pending.
- Deny: refused with an agent-readable message ("operation denied by the user; do not
  retry this operation"). Deny also applies when the user answers Deny to an Ask.
- Hide: the instance never registers the tool, so it never appears in `tools/list`.

- "Allow always" grants are in-memory, per project, per instance; they are not persisted
  into projects or settings and die with the instance. The statusbar popup lists active
  grants with a revoke button.
- CLI override (session-only, not persisted): `--mcp-permissions allow=<id,id...>,deny=<id...>`.
- Precedence: CLI override > user per-tool setting > category default (read = Allow,
  modify/execute = Ask).

## 7. Security

- Broker and instance endpoints bind 127.0.0.1 only.
- Optional token (checkbox in settings, default off): the broker requires
  `Authorization: Bearer <token>` on `/mcp`. The token is generated when the token option
  is first enabled and persisted at `<user-dir>/mcp/token` with 0600 permissions until
  the user regenerates it (so broker restarts do not invalidate agent configurations); it
  is copyable from the settings page and the statusbar popup (one paste into
  `claude mcp add --header "Authorization: Bearer ..."`).
- Instance endpoints require the registration secret on every `/op/...` call (section 3).

## 8. Desktop UI

- Statusbar button `JIPipeDesktopMCPStatusControl` in the project window, gated behind
  `AIApplicationSettings.enableAI` (same place as the AI control). Flat button, icon plus
  short state text ("MCP", "MCP: query_graph"), spinner while busy.
- Popup menu: copy server URL (plus token when enabled), pause/resume, open MCP settings,
  open operation log, revoke "Allow always" grants, pending Ask requests.
- Pause: the instance unregisters and stops its endpoint; the broker keeps running for
  other instances. Stop (settings) disables MCP entirely.
- Dynamic updates: the button label reflects the last operation ("query_graph .
  my-project"); the work item calls for this.
- Operation log: the instance records every call (tool, project, caller session, outcome,
  duration) using the progress-log store; the broker keeps a ring buffer
  (`ServerLogRingBuffer` pattern) for routing and lifecycle events. The popup log view
  shows both; broker entries are fetched via `/control/logs` like the LLM proxy.
- Settings section "MCP" in application settings: enable MCP, port (8781), auto-start,
  token on/off and regenerate, per-tool permission table (Allow/Ask/Deny/Hide).

## 9. Screenshot system

Documentation-grade: deterministic, high-resolution, independent of the user's theme and
window manager.

- `screenshot_canvas` / `screenshot_minimap`: off-screen `BufferedImage` rendering through
  `JIPipeDesktopGraphCanvasPaintManager` (the same painter the floating map uses;
  `paintMiniMap` already accepts a scale and layer set). A `scale` parameter defaults to
  2x. No screen interaction is needed.
- `screenshot_ui`: a curated catalog of named UI surfaces per project window (graph
  toolbar, node configuration panel, settings sections, and similar). Never-shown
  components are realized off-screen exactly like the launcher's `ScreenshotRenderer`
  (off-screen `JFrame` at -4000/-4000, `printAll`); visible components go through
  `ScreenImage`.
- `screenshot_window`: window capture plus a programmatically drawn fake titlebar
  (KDE 6 Breeze style). The title text is sized via `FontMetrics` (never assumed);
  window controls are vector-drawn; only pure gap constants are hard-coded.
- All screenshot tools return MCP image content (base64 PNG) and accept an optional
  `save_to` path.

## 10. Migration and removal

Delete:

- `InstrumentationServer` (WebSocket), `InstrumentationProtocol`, session state and the
  `select_project` flow, `InstrumentationApplicationSettings`, `InstrumentationPlugin`,
  the `--instrumentation` flag and the `--ai-agent-monitor` alias in `GuiCommand`,
  `InstrumentationServerTest`.

Move and upgrade:

- The 21 operations from `api/instrumentation/operations/` to
  `api/automation/operations/` (package rename, schemas and annotations added).
- `InstrumentationAPI` becomes `JIPipeAutomationAPI`; all call sites (in-app AI agent,
  error debugging, agent monitor) re-point.

Port from the #1290 branch (jipipe-5 `1290-ai-llm-integration`):

- `SharedServerProcess`, `SharedServerDiscoveryFile`, `SharedInstanceRegistry`,
  `SharedInstanceRegistration` plus their tests. If #1290 merges to master first, reuse
  directly; otherwise port and deduplicate at merge time.

CLI: `--mcp [port]` replaces `--instrumentation [port]`; `--mcp-permissions` is new.

Docs: the AGENTS.md "Instrumentation System" section is rewritten for the automation/MCP
architecture.

## 11. Testing

Per the project test policy: non-trivial logic only, no Swing interaction or UI structure
tests (the user verifies UI by eye).

Tested (pure logic, headless-safe):

- Registry to MCP tool-spec conversion, Hide filtering, union tool list across instances.
- Routing: project id to instance resolution, dead-instance re-route, no-capable-instance
  error.
- Permission precedence (CLI override > user setting > category default), grant lifecycle
  (allow-always scoping, revoke), Ask denial outcome.
- Argument schema validation (malformed arguments rejected).
- Discovery file write/read/cleanup and PID monitor behavior (port
  `TestSharedServerDiscoveryFile`).
- Broker client state machine (connect/spawn/reconnect) against a stub endpoint.

Not tested by policy: screenshot rendering output, statusbar/popup/settings UI structure,
bubble behavior.

## 12. Dependencies and distribution

- `io.modelcontextprotocol.sdk:mcp` (Apache 2.0, latest stable at implementation time)
  plus an embedded Jetty servlet stack (Jetty 12, Java 21 compatible) for the SDK's
  Streamable HTTP servlet transport (`HttpServletStreamableServerTransportProvider`,
  endpoint `/mcp`).
- Added to `jipipe-core/pom.xml`, `dist/dist-info.json`, regenerated build scripts
  (`dist/generate-dist-scripts.py`), and the `jipipe-distribution-files` repository if
  bundled there.
- The broker is spawned with the instance classpath, so no separate artifact is needed.
- Server identity: serverInfo name `jipipe`, version from the build; capabilities:
  `tools` with `listChanged` (the tool list updates as instances join or leave).

## 13. Error handling

- Broker unreachable at tool call time: agent gets a clear MCP error ("JIPipe MCP server
  is not running; enable it in settings or start JIPipe with --mcp").
- Configured port occupied: fallback to an auto-assigned port, loud log entry, the
  statusbar popup shows the actual URL.
- Instance dies between registration and a tool call: the broker's PID monitor removes
  it; a call targeting it returns an error listing the still-available projects.
- Broker dies while instances run: instances detect failed health checks, delete the
  stale discovery file, and respawn (same `ensureConnected` path as the LLM proxy).
- Ask prompt: no timeout; the tool call blocks until the user answers. The agent may
  cancel its call, in which case the pending request is dropped and logged.
- Broker to instance HTTP timeouts: generous defaults (execution tools are long-running
  and report through job progress; the endpoint acknowledges execution start quickly and
  results are polled via `get_job_status` semantics).

## 14. Coordination

- #1290 introduces the shared server system this design ports. The MCP work must either
  build on its merge or port the four shared classes and their tests, deduplicating at
  merge time. The registration/discovery/argfile conventions follow the LLM proxy
  verbatim to keep the two systems symmetric.
- #1348 (release strategy) affects how the feature reaches users but not this design.
