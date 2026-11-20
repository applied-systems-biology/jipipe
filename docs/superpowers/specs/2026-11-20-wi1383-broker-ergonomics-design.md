# Spec: #1383 sub-project C - broker ergonomics

Date: 2026-11-20
Status: DRAFT for user review
Work item: #1389 (child of #1383, MCP enhancements). Depends on: A/F/E merged, B open as !207 (file surface overlap: `AutomationPlugin` registration blocks and `JIPipeDesktopMCPIntegration`, disjoint regions). Branch base: post-E master `333ae2bb40`.
Source: user feedback items 6 and 12 of the #1383 charter. Design approved in chat (user, 2026-11-20).

## 1. Intent

A fresh MCP session must be able to create and open projects without satisfying a paradoxical `project` argument, every tool call should not have to pay the 47-character `<instanceId>:<projectKey>` toll, and there must be a real health/version tool instead of `list_projects` doubling as the heartbeat.

## 2. Confirmed decisions

1. **Design approved in chat** (user, 2026-11-20): the three-piece shape below. Rejected: per-op default resolution (duplicates the resolution chain in 40+ operations), and a new instance-level tool category (machinery for a distinction that vanishes once `project` is optional everywhere).
2. **Default routing semantics** (from item 6's own wording): omitted `project` routes to the FOCUSED project window; when none is focused and exactly one project window is open, that one; when several are open and none is focused, an explicit error listing the candidates. Explicit `project` always wins.
3. **No auto-allow for the lifecycle ops**: they stay Modify-class (Ask). Unattended runs use the existing `--mcp-permissions` CLI override, documented in the ops' agent documentation.
4. **Layer lifecycle out of scope**: the MCP layer starts with the first and stops with the last project window, so a truly window-less instance is unreachable by design; the zero-window bootstrap case does not arise at the broker (the broker answers "No registered JIPipe instance carries the tool"). Not changed here.

## 3. Verified ground (master 333ae2bb40)

- `MCPBrokerServer.GLOBAL_TOOL_IDS` = `{list_projects, list_algorithms, get_node_docs, get_job_status, cancel_job, search_nodes}` (`servers/mcp/MCPBrokerServer.java:122`). `withProjectArgument` (:331-353) injects a `project` property into every OTHER tool's published schema - into `properties` only, NOT into `required` (the schema already advertises it as optional while the call path enforces it: an inconsistency this sub-project fixes).
- `handleToolCall` (:374-420): missing `project` on a non-global tool returns the error "Missing required 'project' argument: ..."; a project-less GLOBAL call routes via `registry.findAnyByToolId` (the most-recently registered live instance that carries the tool, else the first) and forwards a null project key. `list_projects` is the aggregation exception (broker-side merge across all instances).
- `PROJECT_ARGUMENT_DESCRIPTION` (:138) is the injected property's description text.
- Instance dispatch (`JIPipeDesktopMCPIntegration.execute`, :491-509): `findProjectWindow(projectKey)` returns null for a null or unknown key (null short-circuits at `findProjectWindowOnEdt` :567), and the `AutomationContext` is then built with `project = null` - data operations return "No project loaded" today. The UI operations implement their own fallback chain via `AutomationOperationUtils.resolveDefaultProjectWindow` (`operations/AutomationOperationUtils.java:78-95`, package-private): bound-by-key, then the KeyboardFocusManager active-window owner chain, then the first open window.
- `JIPipe.getJIPipeVersion()` exists (`JIPipe.java:497`). The operation registry exposes `getIds()`/`getAll()` (`AutomationOperationRegistry.java:29-33`). Open project windows: `JIPipeDesktopProjectWindow.getOpenWindows()`.
- The broker test harness (`servers/mcp/MCPBrokerServerTest`) drives a real MCP client flow against stub instance endpoints; it contains `projectBoundToolWithoutProjectErrorsWithSchemaHint` (the behavior C2 replaces) and `publishedSchemaDeclaresProjectForProjectBoundToolsOnly` (the schema rule C1 changes).
- `create_project` already supports the no-open-window path internally (it falls back to the application context) and `open_project` already answers with "No project window is open: ... Create a project first (create_project)" - both correct op-side; only the broker gate is paradoxical.

## 4. C1: lifecycle ops join the global set

`GLOBAL_TOOL_IDS` gains `create_project` and `open_project` (stable string literals appended to the `Set.of(...)`). Consequences, all mechanical: their published schemas no longer carry the injected `project` property; `fallbackDescription` reports them as registry-global; project-less calls route to the most-recent instance carrying them. The `GLOBAL_TOOL_IDS` Javadoc is extended: the set now also holds the lifecycle tools, which act on the INSTANCE (its windows) rather than on an existing project. `open_project` keeps its op-side initiator error (with the `create_project` hint) - it stays honest when no window is open on the routed instance.

## 5. C2: `project` optional on every tool (default routing)

Two coordinated changes:

- **Broker**: in `handleToolCall`, a missing `project` argument on a NON-global tool no longer errors - it takes the exact global-tool routing path (most-recent live instance carrying the tool, else the "no registered instance" error) and forwards a null project key. The injected schema description (`PROJECT_ARGUMENT_DESCRIPTION` / `withProjectArgument`) changes to: "Optional. Omit to target the focused project window of the routed instance (an explicit error lists the candidates when several are open and none is focused). Pass '<instanceId>:<projectKey>' as returned by list_projects to target a specific window or instance - required when several JIPipe instances are running". The `required` array is not touched (it never contained the key).
- **Instance**: in `execute`, when `projectKey` is null the integration resolves a default window BEFORE building the context, on the EDT, with the chain: (1) the focused project window (the KeyboardFocusManager active-window owner chain, the same walk `resolveDefaultProjectWindow` uses); (2) if none is focused and exactly one project window is open, that window; (3) if none is focused and several are open, an error envelope: `error: "no focused project window and <n> projects are open: pass the 'project' argument with one of: <instanceId>:<projectKey> (<name>), ..."`. The context is built from the resolved window - `project` = its project, `projectKey` = its session id (so progress handles, events, and Ask prompts attribute to the resolved window, not to null). A null key that resolves NOTHING (no windows: unreachable at the broker today, defensive) keeps the current `project = null` behavior. Operations that do not read the project are unaffected by the binding.

Explicit `project` arguments behave exactly as today (registry lookup by id, unknown-id errors unchanged). The response envelopes are unchanged: an agent that omitted `project` and needs to know what it got asks `get_ui_state` (which reports the active window and tab).

## 6. C3: `get_server_info` (new global read-only operation)

New operation in `ProjectOperations` (the project/lifecycle family), registered in `AutomationPlugin` and added to `GLOBAL_TOOL_IDS` (it must be callable without a project; its schema is published without the injected `project` property).

- Params: none (`emptyObjectSchema`).
- Response: `{ "jipipeVersion": <JIPipe.getJIPipeVersion()>, "projects": <open project window count>, "operations": <registered operation count via getIds().size()>, "serverTime": <epoch millis> }`.
- Classification: Query, `isReadOnly() = true`, no EDT requirement (plain reads; the window count reads a live list - guarded copy semantics as `getOpenWindows()` provides).
- Description: "Health and version probe: JIPipe version, open project count, registered operation count, and server time. Callable without a project".

## 7. Permissions and broker

`get_server_info`: Query, read-only, Allow by category default, global (no project). `create_project`/`open_project` keep Modify/Ask - with C2 the Ask prompt attributes to the resolved (focused) window when `project` is omitted, and to the named window otherwise. Unattended runs: the `--mcp-permissions allow=create_project,open_project` CLI override is documented in both ops' agent documentation as the bootstrap escape hatch. No other permission changes.

## 8. Testing policy (house rules: headless-safe, no Swing interaction tests)

- Broker harness (`MCPBrokerServerTest`, raw JSON-RPC against stub endpoints): `projectBoundToolWithoutProjectErrorsWithSchemaHint` flips to the new contract - a project-less call on a project-bound tool ROUTES to the registered instance and forwards a null project key (the stub asserts the received `project-key` is null); a project-less call with NO registered instance still errors with the available-projects listing; `create_project`/`open_project`/`get_server_info` publish WITHOUT the `project` property (extends the schema test); `get_server_info` routes project-less and returns the stub envelope.
- Registry/schema validation tests pick up `get_server_info` automatically (schema shape, no-params, global classification).
- The instance-side focused/single/candidates resolution is window (Swing) code: no headless test per house policy - the user verifies by eye (focused window wins; multi-open without focus yields the candidate listing; explicit `project` unchanged).
- `get_server_info` response shape: headless test over the operation with a null-project context (version string non-empty, counts present, `serverTime` plausible) - no Swing needed for the registry/read parts it can see; window-count assertion is by-eye.

## 9. Out of scope (ledgered)

- The MCP layer lifecycle (start/stop with the first/last window) - unchanged; a daemon-mode layer is a different charter.
- Per-tool opt-out from default routing ("strict mode") - the explicit `project` argument IS the strict mode.
- `close_project` (no such operation exists; no feedback item asks for it).
- Multi-instance fan-out of `get_server_info` (single-instance routing like the other global tools).
