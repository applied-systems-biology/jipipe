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
5. **Machine-readable tool scope replaces the broker's hardcoded id sets** (user ruling 2026-11-20, folding into C because C1/C2 edit exactly that code): "I am not a fan of the GLOBAL_TOOL_IDS system as it's used as of now. It breaks JIPipe registry style conventions on that registered items contain all information required for underlying operations to work with them. Currently the broker has precognition on global tool ids, but it is the tools themselves that should provide a machine-readable signature." C therefore deletes `GLOBAL_TOOL_IDS` and `EXCLUDED_TOOL_IDS` instead of growing them (section 4). The larger builder reform (intermediate schema builders carrying higher information content, replacing the hardcoded JSON string blocks) is LEDGERED as a separate sub-project, not folded here (section 9).

## 3. Verified ground (master 333ae2bb40)

- `MCPBrokerServer.GLOBAL_TOOL_IDS` = `{list_projects, list_algorithms, get_node_docs, get_job_status, cancel_job, search_nodes}` (`servers/mcp/MCPBrokerServer.java:122`). `withProjectArgument` (:331-353) injects a `project` property into every OTHER tool's published schema - into `properties` only, NOT into `required` (the schema already advertises it as optional while the call path enforces it: an inconsistency this sub-project fixes).
- `handleToolCall` (:374-420): missing `project` on a non-global tool returns the error "Missing required 'project' argument: ..."; a project-less GLOBAL call routes via `registry.findAnyByToolId` (the most-recently registered live instance that carries the tool, else the first) and forwards a null project key. `list_projects` is the aggregation exception (broker-side merge across all instances).
- `PROJECT_ARGUMENT_DESCRIPTION` (:138) is the injected property's description text.
- Instance dispatch (`JIPipeDesktopMCPIntegration.execute`, :491-509): `findProjectWindow(projectKey)` returns null for a null or unknown key (null short-circuits at `findProjectWindowOnEdt` :567), and the `AutomationContext` is then built with `project = null` - data operations return "No project loaded" today. The UI operations implement their own fallback chain via `AutomationOperationUtils.resolveDefaultProjectWindow` (`operations/AutomationOperationUtils.java:78-95`, package-private): bound-by-key, then the KeyboardFocusManager active-window owner chain, then the first open window.
- `JIPipe.getJIPipeVersion()` exists (`JIPipe.java:497`). The operation registry exposes `getIds()`/`getAll()` (`AutomationOperationRegistry.java:29-33`). Open project windows: `JIPipeDesktopProjectWindow.getOpenWindows()`.
- The broker test harness (`servers/mcp/MCPBrokerServerTest`) drives a real MCP client flow against stub instance endpoints; it contains `projectBoundToolWithoutProjectErrorsWithSchemaHint` (the behavior C2 replaces) and `publishedSchemaDeclaresProjectForProjectBoundToolsOnly` (the schema rule C1 changes).
- `create_project` already supports the no-open-window path internally (it falls back to the application context) and `open_project` already answers with "No project window is open: ... Create a project first (create_project)" - both correct op-side; only the broker gate is paradoxical.

## 4. C1: machine-readable tool scope (deletes the broker's precognition)

New enum `AutomationToolScope` (`org.hkijena.jipipe.api.automation`, beside `AutomationOperation` - the operation interface references it, so it lives above the endpoint layer; amended during implementation, plan-binding):

- `PROJECT_BOUND` (default): the tool acts on a project; the broker injects the `project` argument into the published schema and requires it (until C2 makes it optional-but-routable).
- `INSTANCE_GLOBAL`: callable without a project; the broker routes a project-less call to the most-recently registered live instance carrying the tool (the E2E ruling 2026-11-16 semantics, now declared by the tool). Members today: `list_algorithms`, `get_node_docs`, `get_job_status`, `cancel_job`, `search_nodes`, and the lifecycle tools `create_project`, `open_project`, `get_server_info`.
- `INSTANCE_GLOBAL_AGGREGATED`: callable without a project AND answered BY THE BROKER, which aggregates the responses of every live instance carrying the tool and rewrites the ids into routable form - routing to one instance would hide the others. Member today: `list_projects`. This is genuinely the operation's own semantics (the project list must span all instances), so it belongs in the signature, not in a broker-side id constant.
- `NOT_A_TOOL`: registered as an automation operation but excluded from the MCP `tools/list` aggregation. Member today: `list_operations` (the existing `EXCLUDED_TOOL_IDS` controller ruling, now declared by the operation).

`AutomationOperation` gains `default AutomationToolScope getToolScope() { return AutomationToolScope.PROJECT_BOUND; }` - the operation is the registered item and carries ALL information about itself (the registry convention). The eight non-default operations above override it; everything else keeps the default.

`MCPToolDescriptor` gains a `toolScope` field (serialized as the enum name string under `"tool-scope"`; ALWAYS emitted, matching the `"read-only"` style; absent on parse defaults to `PROJECT_BOUND`; an unknown value fails fast - mixed broker/instance versions are not a supported deployment, both ship in one build). The broker reads the scope from the descriptor it already holds - in `buildToolSpecification` (schema injection), in `handleToolCall` (routing), and in the `tools/list` aggregation (exclusion). `GLOBAL_TOOL_IDS`, `EXCLUDED_TOOL_IDS`, and `LIST_PROJECTS_TOOL_ID` as a scope source are DELETED; the `list_projects` aggregation code path remains (it is the broker answering `INSTANCE_GLOBAL_AGGREGATED`), selected by scope, not by id. The broker class Javadoc is rewritten to describe the declared-scope contract. Mixed broker/instance versions are not a supported deployment (both ship in one build); the absent-field default is documented in the descriptor.

## 5. C2: `project` optional on every tool (default routing)

Two coordinated changes:

- **Broker**: in `handleToolCall`, a missing `project` argument on a `PROJECT_BOUND` tool no longer errors - it takes the exact instance-global routing path (most-recent live instance carrying the tool, else the "no registered instance" error) and forwards a null project key. The injected schema description (`withProjectArgument`) changes to: "Optional. Omit to target the focused project window of the routed instance (an explicit error lists the candidates when several are open and none is focused). Pass '<instanceId>:<projectKey>' as returned by list_projects to target a specific window or instance - required when several JIPipe instances are running". The `required` array is not touched (it never contained the key).
- **Instance**: in `execute`, when `projectKey` is null the integration resolves a default window BEFORE building the context, on the EDT, with the chain: (1) the focused project window (the KeyboardFocusManager active-window owner chain, the same walk `resolveDefaultProjectWindow` uses); (2) if none is focused and exactly one project window is open, that window; (3) if none is focused and several are open, an error envelope: `error: "no focused project window and <n> projects are open: pass the 'project' argument with one of: <sessionId> (<name>), ..."`. The listing carries each open window's SESSION id and project name - the instance does not know its broker-assigned instance id, so it cannot render `<instanceId>:<projectKey>` form (reconciled during implementation; the plan pinned the session-id form). The context is built from the resolved window - `project` = its project, `projectKey` = its session id (so progress handles, events, and Ask prompts attribute to the resolved window, not to null). A null key that resolves NOTHING (no windows: unreachable at the broker today, defensive) keeps the current `project = null` behavior. Operations that do not read the project are unaffected by the binding.

Explicit `project` arguments behave exactly as today (registry lookup by id, unknown-id errors unchanged). The response envelopes are unchanged: an agent that omitted `project` and needs to know what it got asks `get_ui_state` (which reports the active window and tab).

## 6. C3: `get_server_info` (new instance-global read-only operation)

New operation in `ProjectOperations` (the project/lifecycle family), registered in `AutomationPlugin`, declared `INSTANCE_GLOBAL` (it must be callable without a project; its schema is published without the injected `project` property).

- Params: none (`emptyObjectSchema`).
- Response: `{ "jipipeVersion": <JIPipe.getJIPipeVersion()>, "projects": <open project window count>, "operations": <registered operation count via getIds().size()>, "serverTime": <epoch millis> }`.
- Classification: Query, `isReadOnly() = true`, no EDT requirement (plain reads; the window count reads a live list - guarded copy semantics as `getOpenWindows()` provides).
- Description: "Health and version probe: JIPipe version, open project count, registered operation count, and server time. Callable without a project".

## 7. Permissions and broker

`get_server_info`: Query, read-only, Allow by category default, `INSTANCE_GLOBAL` (no project). `create_project`/`open_project` keep Modify/Ask - with C2 the Ask prompt attributes to the resolved (focused) window when `project` is omitted, and to the named window otherwise. Unattended runs: the `--mcp-permissions allow=create_project,open_project` CLI override is documented in both ops' agent documentation as the bootstrap escape hatch. No other permission changes.

## 8. Testing policy (house rules: headless-safe, no Swing interaction tests)

- Scope signature: a registry-side test pins that the eight non-default operations declare their exact scope and that the descriptor round-trips it (`of(operation)` + JSON serialize/deserialize, absent-field defaults to `PROJECT_BOUND`, unknown string tolerated by `ignoreUnknown`).
- Broker harness (`MCPBrokerServerTest`, raw JSON-RPC against stub endpoints): stub descriptors carry the scope field; a project-less call on a `PROJECT_BOUND` tool ROUTES to the registered instance and forwards a null project key (the old error test flips); a project-less call with NO registered instance still errors with the available-projects listing; `INSTANCE_GLOBAL_AGGREGATED` still aggregates across instances and rewrites ids; `NOT_A_TOOL` stays out of `tools/list`; `create_project`/`open_project`/`get_server_info` publish WITHOUT the `project` property (schema test now drives off the declared scope, with stub descriptors for both scopes); `get_server_info` routes project-less and returns the stub envelope.
- Registry/schema validation tests pick up `get_server_info` automatically (schema shape, no-params, scope declaration).
- The instance-side focused/single/candidates resolution is window (Swing) code: no headless test per house policy - the user verifies by eye (focused window wins; multi-open without focus yields the candidate listing; explicit `project` unchanged).
- `get_server_info` response shape: headless test over the operation with a null-project context (version string non-empty, counts present, `serverTime` plausible) - no Swing needed for the registry/read parts it can see; window-count assertion is by-eye.

## 9. Out of scope (ledgered)

- **Schema builder reform** (ledgered 2026-11-20 as a charter addition, its own sub-project): today every operation hand-writes its JSON schema as a hardcoded string block (`AutomationSchemaUtils.parseJson(...)`). The reform introduces intermediate schema builders that produce the final MCP JSON AND carry higher information content (machine-readable parameter metadata) so the broker and other features are informed without parsing JSON strings - the same sloppy-design pattern the llm branch had to rectify. C does NOT do this sweep; C's descriptor scope field is the only step toward it that C carries.
- The MCP layer lifecycle (start/stop with the first/last window) - unchanged; a daemon-mode layer is a different charter.
- Per-tool opt-out from default routing ("strict mode") - the explicit `project` argument IS the strict mode.
- `close_project` (no such operation exists; no feedback item asks for it).
- Multi-instance fan-out of `get_server_info` (single-instance routing like the other instance-global tools).
