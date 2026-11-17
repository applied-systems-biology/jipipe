# MCP Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the WebSocket instrumentation API with an MCP (Model Context Protocol) server: a spawned, refcounted broker process serving Streamable HTTP, routing tool calls to JIPipe instances that execute operations through a renamed, schema-declaring automation layer with a per-tool permission system.

**Architecture:** Five blocks (spec sections 1-3): broker process (`servers/mcp`, MCP Java SDK + Jetty, ported `SharedServerProcess` lifecycle), instance endpoint (`api/automation/endpoint`, JDK HttpServer on an ephemeral 127.0.0.1 port), automation operation layer (renamed `api/instrumentation` core with JSON schemas and annotations), permission service (Allow/Ask/Deny/Hide, statusbar-button Ask flow), desktop UI (statusbar control, settings). MCP clients talk only to the broker; the broker routes by broker-minted project id `<instanceId>:<projectKey>`.

**Tech Stack:** Java 21, Maven, official MCP Java SDK (`io.modelcontextprotocol.sdk:mcp`) + Jetty 12 ee10 servlet stack (new dependencies), JDK `com.sun.net.httpserver.HttpServer` for instance endpoints, Jackson, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-16-mcp-server-design.md` (read together with this plan; the spec is authoritative on behavior).

## Global Constraints

- No em dashes (U+2014) or en dashes (U+2013) in code, comments, Javadoc, or strings.
- Operation ids are stable string literals, snake_case (they become MCP tool names and are API surface).
- The automation plugin's registration id stays the literal `"org.hkijena.jipipe:instrumentation"` (persisted reference; class rename only).
- All server sockets bind `127.0.0.1` only.
- Never hardcode text-dependent pixel sizes; measure with `FontMetrics` (fake titlebar).
- Test policy: no Swing interaction or UI-structure tests; no tests whose assertion restates the implementation. Run tests with `mvn -q test -pl jipipe-core -Dtest=<Class>`, compile with `mvn -q compile -pl jipipe-core`.
- Commit messages: conventional commits, end with `(#1369)`.
- The #1290 branch checkout `/data/src/jipipe-5` (branch `1290-ai-llm-integration`) is the reference for ported shared-server classes and the LLM proxy spawn/client patterns; port files, do not link across checkouts.

## Review Focus

Failure modes the spec implies but unit tests do not fully exercise:

1. **Long-running run tools must not hold a broker HTTP response open.** A person expects `run_pipeline` to return a job id at once and to poll `get_job_status`. Pinned in Task 6: broker test asserts `tools/call` on an async-classified tool returns an envelope containing `job-id` while a stub job runs.
2. **Project window closed between `list_projects` and the tool call.** A person expects a clear error naming still-available projects, not a stack trace. Pinned in Task 6 (routing test, unknown project id).
3. **Hidden tool called directly against an instance endpoint.** A person expects refusal even though the broker never advertised it. Pinned in Task 5 (endpoint test, hidden op).
4. **Two instances racing to spawn the broker.** A person expects the loser to detect the live broker via discovery file + health and reuse it. Pinned in Task 7 (client test with pre-existing discovery file).
5. **Wrong or missing Bearer token.** A person expects 401 before any tool listing or call. Pinned in Task 6 (token test).
6. **Screenshot result size.** A person expects `save_to` to return a path instead of inlining a multi-MB base64 image. Pinned in Task 12 (envelope test).

---

### Task 1: Port shared server infrastructure from #1290

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/SharedServerDiscoveryFile.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/SharedServerProcess.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/SharedInstanceRegistry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/SharedInstanceRegistration.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/servers/TestSharedServerDiscoveryFile.java`

**Interfaces:**
- Consumes: nothing new (`PortManager` exists in the same package).
- Produces: `SharedServerDiscoveryFile.write(Path, long pid, int port, String startedAt)`, `.read(Path) -> DiscoveryData`, `.delete(Path)`, `.isPidAlive(long)`; `SharedServerProcess(Path discoveryFile, long parentPid, int port, SharedInstanceRegistry<?> registry)` with abstract `startServer()`, `stopServer()`, `getRegistry()` and final `run()` (discovery write, shutdown hook, parent + instance PID monitors); `SharedInstanceRegistry<T extends SharedInstanceRegistration>` with `register/unregister/getInstanceCount/getAll/removeDeadInstances`.

- [ ] **Step 1: Copy the four main classes and the test verbatim** from `/data/src/jipipe-5/jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/` and `/data/src/jipipe-5/jipipe-core/src/test/java/org/hkijena/jipipe/api/servers/TestSharedServerDiscoveryFile.java` into the same packages in this repo. Package names already match.

- [ ] **Step 2: Run the ported test**

Run: `mvn -q test -pl jipipe-core -Dtest=TestSharedServerDiscoveryFile`
Expected: PASS

- [ ] **Step 3: Compile**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/servers/Shared*.java jipipe-core/src/test/java/org/hkijena/jipipe/api/servers/TestSharedServerDiscoveryFile.java
git commit -m "feat(#1369): port shared server infrastructure from #1290"
```

### Task 2: Rename instrumentation core to the automation layer

**Files:**
- Rename (git mv + class renames). Source package `org.hkijena.jipipe.api.instrumentation`, target package `org.hkijena.jipipe.api.automation`:

| From (api/instrumentation) | To (api/automation) |
|---|---|
| `InstrumentationAPI` | `JIPipeAutomationAPI` |
| `InstrumentationOperation` | `AutomationOperation` |
| `InstrumentationOperationRegistry` | `AutomationOperationRegistry` |
| `InstrumentationContext` | `AutomationContext` |
| `events/` (all) | `automation/events/` |
| `pipeline_map/` (all) | `automation/pipeline_map/` |
| `operations/` (all six files) | `automation/operations/` |

- Rename: `api/service/components/JIPipeInstrumentationServiceComponent.java` -> `JIPipeAutomationServiceComponent` (field/method names follow).
- Rename: `plugins/instrumentation/InstrumentationPlugin.java` -> `plugins/automation/AutomationPlugin.java` (class name only; **keep** `getId()` returning the literal `"org.hkijena.jipipe:instrumentation"` and keep the plugin description text updated to mention the automation layer).
- Rename: `JIPipeDefaultJavaPlugin.registerInstrumentationOperation(...)` -> `registerAutomationOperation(...)` (line ~1092) and its call sites.
- Update references to the renamed types in: `api/service/JIPipeService.java`, `api/progress/JIPipeProgressNodeUtils.java`, `jipipe-launcher/.../GuiCommand.java`.
- Keep untouched in `api/instrumentation/` until Task 14: `InstrumentationServer.java`, `InstrumentationProtocol.java`, `InstrumentationApplicationSettings.java`, and `InstrumentationServerTest.java`. Update their imports to the renamed types so everything compiles.

**Interfaces:**
- Consumes: existing `InstrumentationOperation` interface shape: `String getId()`, `String getDescription()`, `JsonNode execute(AutomationContext ctx, JsonNode params) throws Exception`.
- Produces: `JIPipeAutomationAPI` (same static methods, new name), `AutomationOperationRegistry` with the same register/get contract, `registerAutomationOperation(String id, AutomationOperation operation)` on `JIPipeDefaultJavaPlugin`, `JIPipeAutomationServiceComponent.getOperationRegistry()`.

- [ ] **Step 1: Perform the renames** with `git mv` per the table, rename classes and every reference (compiler errors are the checklist). Do not change behavior.

- [ ] **Step 2: Compile and run the surviving instrumentation test**

Run: `mvn -q compile -pl jipipe-core && mvn -q test -pl jipipe-core -Dtest=InstrumentationServerTest`
Expected: BUILD SUCCESS, test PASS

- [ ] **Step 3: Verify no stragglers**

Run: `grep -rln "InstrumentationAPI\|InstrumentationOperation\b\|InstrumentationContext" jipipe-core/src/main/java jipipe-launcher/src/main/java | grep -v "api/instrumentation"`
Expected: no output

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(#1369): rename instrumentation core to automation layer"
```

### Task 3: Operation metadata: JSON schemas, categories, validation

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperation.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationCategory.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtils.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationRegistry.java` (add validating dispatch)
- Modify: all 20 operation classes under `api/automation/operations/` (add schema + annotations)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationSchemaUtilsTest.java`, `.../AutomationRegistryValidationTest.java`

**Interfaces:**
- Consumes: Task 2's `AutomationOperation`, `AutomationOperationRegistry`.
- Produces:
  - `AutomationOperation` additions: `JsonNode getInputSchema()` (abstract), `default AutomationOperationCategory getCategory()`, `default boolean isReadOnly()`, `default String getAgentDocumentation()` (defaults to `getDescription()`).
  - `enum AutomationOperationCategory { Query, Modify, Execute, Render, Meta }`.
  - `AutomationSchemaUtils.validate(JsonNode schema, JsonNode args) -> List<String>` (errors; supports `type` among object/string/integer/number/boolean/array, `required`, `properties`, `enum`, `additionalProperties:false`; unknown keywords ignored).
  - `AutomationOperationRegistry.execute(String id, AutomationContext ctx, JsonNode params)` that validates against the op schema first and throws `IllegalArgumentException` listing the errors without invoking the operation.

- [ ] **Step 1: Write the failing tests.** `AutomationSchemaUtilsTest`: rejects missing required property, wrong type, value outside `enum`, unknown property when `additionalProperties:false`; accepts a valid object. `AutomationRegistryValidationTest`: register a stub op whose `execute` throws `AssertionError("must not be called")` with schema `{"type":"object","required":["x"],"properties":{"x":{"type":"integer"}}}`; assert `execute("stub", ctx, {})` throws `IllegalArgumentException` and `execute("stub", ctx, {"x":1})` invokes it (second stub records the call).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaUtilsTest,AutomationRegistryValidationTest'`
Expected: FAIL (classes missing)

- [ ] **Step 3: Implement the interface additions, enum, validator, and validating dispatch.**

- [ ] **Step 4: Declare schemas + annotations on all 20 migrated operations.** Rule: read each operation's `execute` param parsing and mirror it exactly in `getInputSchema()` (same property names, correct types, `additionalProperties:false`); set `readOnly=true` + category `Query` for the QueryOperations and PipelineMapOperations and `ProjectOperations.ListProjects`; category `Modify`/`Execute` with `readOnly=false` for ModificationOperations, ExecutionOperations, `ROCrateOperations.CreateROCrate`; `ListOperations` is `Meta`/`readOnly=true`. Worked example for `add_node`: schema `{"type":"object","required":["compartment","node-type"],"properties":{"compartment":{"type":"string"},"node-type":{"type":"string"},"x":{"type":"number"},"y":{"type":"number"}},"additionalProperties":false}`.

- [ ] **Step 5: Run tests and compile**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationSchemaUtilsTest,AutomationRegistryValidationTest' && mvn -q compile -pl jipipe-core`
Expected: PASS, BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(#1369): declare input schemas and annotations on automation operations"
```

### Task 4: Permission service

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/permissions/AutomationPermissionState.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/permissions/AutomationPermissionService.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/permissions/AutomationPermissionServiceTest.java`

**Interfaces:**
- Consumes: Task 3 (`AutomationOperationCategory`, `isReadOnly()`).
- Produces:
  - `enum AutomationPermissionState { Allow, Ask, Deny, Hide }`.
  - `AutomationPermissionService`: `void setUserState(String operationId, AutomationPermissionState state)`, `void setCliOverride(AutomationPermissionState state, Collection<String> operationIds)`, `AutomationPermissionState resolve(AutomationOperation operation, String projectKey)`, `void grantAlways(String operationId, String projectKey)`, `void revokeAllGrants()`, `List<AutomationGrant> getGrants()` where `record AutomationGrant(String operationId, String projectKey)`.

- [ ] **Step 1: Write the failing test.** Cases: read-only op with no config resolves `Allow`; non-read-only resolves `Ask`; `setUserState(Deny)` wins over default; `setCliOverride(Allow, [id])` wins over `setUserState(Deny)`; after `grantAlways(id, "p1")`, `resolve(op, "p1")` returns `Allow` while `resolve(op, "p2")` stays `Ask`; `revokeAllGrants()` restores `Ask`; `resolve` returns `Hide` when user state is `Hide`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationPermissionServiceTest`
Expected: FAIL (class missing)

- [ ] **Step 3: Implement.** Resolution order: "Allow always" grant -> CLI override -> user state -> category default (`isReadOnly() ? Allow : Ask`). Grants are in-memory only.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationPermissionServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/permissions jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/permissions
git commit -m "feat(#1369): automation permission service with grants and CLI override"
```

### Task 5: Instance operation endpoint

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/AutomationInstanceEndpoint.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/AutomationEndpointHandlers.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPInstanceRegistration.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPProjectEntry.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/endpoint/AutomationInstanceEndpointTest.java`

**Interfaces:**
- Consumes: Task 1 (`SharedInstanceRegistration`), Task 3 (registry validation), Task 4 (`AutomationPermissionService`).
- Produces:
  - `MCPProjectEntry` Jackson DTO: `key`, `name`, `path` (kebab-case JSON keys).
  - `MCPInstanceRegistration extends SharedInstanceRegistration`: Jackson fields `endpointPort:int`, `endpointSecret:String`, `appVersion:String`, `projects:List<MCPProjectEntry>`.
  - `interface AutomationEndpointHandlers { JsonNode execute(String operationId, String projectKey, JsonNode params) throws Exception; boolean isRegistered(String operationId); boolean isHidden(String operationId); }` (the GUI wiring implements project resolution and EDT marshalling; tests use stubs).
  - `interface AskPromptHandler { boolean ask(String operationId, String projectKey); }` (true = allow once).
  - `interface OperationCallListener { void onCall(String operationId, String projectKey, String outcome, long durationMs); }` (the GUI wiring records into the operation log; the endpoint records nothing itself when the listener is a no-op).
  - `AutomationInstanceEndpoint(AutomationOperationRegistry registry, AutomationPermissionService permissions, AutomationEndpointHandlers handlers, AskPromptHandler askHandler, OperationCallListener callListener)`, `start()`, `stop()`, `int getBoundPort()`, `String getSecret()`.
  - HTTP surface: `GET /health` -> 200 `ok`; `POST /op/<operationId>` with header `X-JIPipe-Endpoint-Secret`, body `{"project-key": "...", "params": {...}}`, response `{"ok":true,"result":...}` or `{"ok":false,"code":"unauthorized|hidden|denied|unknown|invalid|failed","error":"..."}` with HTTP 200 (the envelope carries the error semantics).

- [ ] **Step 1: Write the failing test.** Start endpoint with port 0, stub handlers (one registered op returning `{"echo":true}`, one hidden). Use `java.net.http.HttpClient`. Assert: `/health` 200; `/op/echo` with correct secret and valid params -> `ok:true`; wrong secret -> `code:"unauthorized"`; unknown op -> `code:"unknown"`; hidden op -> `code:"hidden"`; op configured `Deny` in the permission service -> `code:"denied"` and `askHandler` never invoked; op configured `Ask` with `askHandler` returning true -> executes, returning false -> `code:"denied"`; params violating the op schema -> `code:"invalid"`; handler throwing -> `code:"failed"` with the message; the `OperationCallListener` recorded one entry per dispatch with the matching outcome.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationInstanceEndpointTest`
Expected: FAIL (classes missing)

- [ ] **Step 3: Implement** on `com.sun.net.httpserver.HttpServer` bound to `127.0.0.1:0`; generate the secret with a secure random 32-byte base64url string; validate params via `AutomationSchemaUtils.validate` before invoking the registry; notify the `OperationCallListener` after every dispatch (outcome one of `ok|denied|unknown|hidden|invalid|failed`, duration in ms); run the registry dispatch on the calling (HTTP executor) thread - EDT marshalling belongs to the GUI wiring, not the endpoint.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationInstanceEndpointTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/endpoint
git commit -m "feat(#1369): instance operation endpoint with secret and permission enforcement"
```

### Task 6: MCP broker: dependencies, registry, server, process

**Files:**
- Modify: `jipipe-core/pom.xml`, `dist/dist-info.json` (regenerate scripts)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPInstanceRegistry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServer.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPServerProcess.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServerTest.java`

**Interfaces:**
- Consumes: Tasks 1, 3, 5.
- Produces:
  - `MCPInstanceRegistry extends SharedInstanceRegistry<MCPInstanceRegistration>`: `Set<String> aggregateToolIds()`, `MCPInstanceRegistration findByProjectId(String projectId)` for ids of the form `<instanceId>:<projectKey>` (returns null when instance unknown; `NoSuchElementException`-style error envelope when instance known but projectKey absent).
  - `MCPBrokerServer(int port, MCPInstanceRegistry registry, String bearerToken /*nullable*/)`: `start()`, `stop()`, routes `GET /health`; `POST /control/register`, `POST /control/unregister` (Jackson `MCPInstanceRegistration`); `GET /control/logs` (list of `ServerLogEntry` from a `ServerLogRingBuffer`); MCP endpoint at `/mcp` via `HttpServletStreamableServerTransportProvider` + `McpServer.sync(...)`, serverInfo name `"jipipe"`, version `org.hkijena.jipipe.JIPipe.getVersion()` (or the build version accessor used elsewhere), capabilities `tools(listChanged=true)`. Tool specs are rebuilt from the registry: name = operation id, description = `getAgentDocumentation()`, `inputSchema` = op schema, `readOnlyHint` = `isReadOnly()`. `tools/call` splits the `project` argument, resolves the instance, and forwards `POST http://127.0.0.1:<endpointPort>/op/<id>` (header secret, body `{"project-key": k, "params": <args minus "project">}`); results whose JSON contains `image-base64` + `mime` are wrapped as MCP image content, everything else as JSON text content.
  - `MCPServerProcess extends SharedServerProcess` with `main(String[] args)` parsing `--discovery-file <path>`, `--parent-pid <long>`, `--port <int>` (0 or occupied -> `PortManager.assignFreePort()` + a loud stderr/log line), `--token-file <path>` (optional; file content trimmed = bearer token).

- [ ] **Step 1: Add dependencies.** Resolve the latest stable version: `curl -s https://repo1.maven.org/maven2/io/modelcontextprotocol/sdk/mcp/maven-metadata.xml | grep -o '<latest>[^<]*'`. Add `io.modelcontextprotocol.sdk:mcp` and the Jetty 12 ee10 servlet stack (`org.eclipse.jetty:jetty-server`, `org.eclipse.jetty.ee10:jetty-ee10-servlet`) to `jipipe-core/pom.xml`. Run `mvn -q dependency:list -pl jipipe-core | grep -E 'mcp|jetty|servlet'` and enumerate every newly added runtime jar into `dist/dist-info.json` `dependencies` (`<artifact>-<version>.jar` -> `https://repo1.maven.org/maven2/<group-as-path>/<artifact>/<version>/<artifact>-<version>.jar`). Regenerate: `cd dist && python3 generate-dist-scripts.py`. Commit.

```bash
git add jipipe-core/pom.xml dist/dist-info.json dist/zip
git commit -m "build(#1369): add MCP Java SDK and Jetty dependencies"
```

- [ ] **Step 2: Write the failing broker test.** Use JDK `HttpClient` + a stub instance endpoint (a second `AutomationInstanceEndpoint` from Task 5 or a raw `HttpServer` stub implementing `/op`): construct `MCPInstanceRegistry`, build a registration for the stub (tool id `echo_test` in the stub registry), start `MCPBrokerServer` on port 0, POST `/control/register`, then: `GET /health` -> 200; MCP `tools/list` (a JSON-RPC POST to `/mcp` with `{"jsonrpc":"2.0","id":1,"method":"tools/list"}` after an `initialize` handshake) contains `echo_test` with `readOnlyHint`; `tools/call` with `{"name":"echo_test","arguments":{"project":"<instanceId>:p1"}}` returns the stub result; unknown project id -> error listing available project ids; `Authorization: Bearer wrong` with a token configured -> 401; no token configured -> open access.

- [ ] **Step 3: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=MCPBrokerServerTest`
Expected: FAIL (classes missing)

- [ ] **Step 4: Implement** `MCPInstanceRegistry`, `MCPBrokerServer` (Jetty `Server` + servlet handler at `/mcp`, plain `HttpHandler`-style servlets for `/control/*`, `/health`; token check filter before all routes when configured), `MCPServerProcess`. Job-classified tools (category `Execute`, excluding `get_job_status`/`cancel_job`): the instance endpoint's op returns `{"job-id": ...}` immediately; the broker passes it through unchanged (spec Review Focus 1: the response must not wait for job completion).

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=MCPBrokerServerTest`
Expected: PASS

- [ ] **Step 6: Wire job progress to MCP progress notifications.** In the pinned SDK version, locate the server-side progress notification API (search the SDK sources for `notifyToolProgress` or a `notifications/progress` sender on `McpSyncServer`/`McpAsyncServer`). When a `tools/call` request carries `_meta.progressToken` and the forwarded op returns a `job-id`, subscribe to the job manager's progress for that job and forward progress (current/total/message); stop on completion. If the pinned SDK version exposes no server-side progress sender, record that in the MR description and keep the job-id + `get_job_status` polling path as the only reporting channel (the tool result already contains `job-id`).

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp jipipe-core/src/test/java/org/hkijena/jipipe/servers/mcp
git commit -m "feat(#1369): MCP broker server and process with project routing"
```

### Task 7: Broker client, application settings, CLI, instance lifecycle

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPBrokerClient.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/settings/MCPApplicationSettings.java`
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/GuiCommand.java`
- Modify: the settings registration site of `InstrumentationApplicationSettings` (locate with `grep -rn "InstrumentationApplicationSettings" jipipe-core/src/main/java --include=*.java` and mirror it for the new class)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/endpoint/MCPBrokerClientTest.java`, `.../McpCliOptionsTest.java`

**Interfaces:**
- Consumes: Tasks 5, 6; `SharedServerDiscoveryFile`; the LLM proxy reference `LLMProxyClient.ensureConnected/spawnProxy/registerWithProxy` in `/data/src/jipipe-5`.
- Produces:
  - `MCPBrokerClient`: `synchronized void ensureConnected(JIPipeProgressInfo progressInfo)` (discovery file -> PID alive -> GET `/health`; else delete stale file and `spawnBroker`), `void register()` / `void unregister()` (POST `/control/register|unregister` with `MCPInstanceRegistration`), `void updateProjects(List<MCPProjectEntry>)`, `boolean isBrokerAlive()`, `Path getDiscoveryFilePath()` (`<JIPipe user dir>/mcp/discovery.json`), constructor-injectable `Spawner` functional interface `Process spawn(List<String> command)` so tests never spawn a real JVM; the production spawner mirrors `LLMProxyClient.spawnProxy` (same `java.class.path`, `@argfile`, `--discovery-file`, `--parent-pid`, `--port`, 30 s health poll, stdout drain to progress).
  - `MCPApplicationSettings` singleton (`getInstance()` pattern like `InstrumentationApplicationSettings`): `boolean isEnable()` (default false), `int getPort()` (default 8781), `boolean isAutoStart()`, `boolean isRequireToken()`, persisted per-tool permission map accessor for Task 13's table.
  - `GuiCommand.parseMcpArgs(List<String> args) -> McpCliOptions` where `record McpCliOptions(boolean requested, Integer port, List<String> allowIds, List<String> denyIds)` parsing `--mcp [port]` and `--mcp-permissions allow=a,b deny=c` (overrides applied non-persistently to the `AutomationPermissionService`).
  - Lifecycle wiring in `GuiCommand.WindowWatcher`: on first `onWindowOpened`, if `--mcp` requested or `MCPApplicationSettings.getInstance().isAutoStart()`: start the `AutomationInstanceEndpoint`, apply CLI overrides, `MCPBrokerClient.ensureConnected` + `register`; on every window open/close `updateProjects(...)`; on last window closed `unregister()` + stop endpoint before `JIPipe.exitLater(0)`. The `--instrumentation`/`--ai-agent-monitor` flags are removed in Task 14, not here (they keep compiling until then).

- [ ] **Step 1: Write the failing tests.** `MCPBrokerClientTest`: pre-write a discovery file pointing at a stub broker `HttpServer` (records `/control/register`), `ensureConnected` connects without spawning (spawner throws if called), `register()` hits the stub with the expected JSON fields; discovery file with dead PID + spawner stub returning a process that writes a fresh discovery file and serves `/health` -> client spawns and connects; dead-PID + spawn failure -> `RuntimeException` within the 30 s deadline. `McpCliOptionsTest`: `--mcp` alone, `--mcp 9000`, `--mcp-permissions allow=a,b deny=c`, and a non-MCP argument list yields `requested=false`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='MCPBrokerClientTest,McpCliOptionsTest'`
Expected: FAIL

- [ ] **Step 3: Implement** the client, settings singleton, `parseMcpArgs` (pure static, testable), and the `WindowWatcher` wiring.

- [ ] **Step 4: Run tests to verify they pass and compile both modules**

Run: `mvn -q test -pl jipipe-core -Dtest='MCPBrokerClientTest,McpCliOptionsTest' && mvn -q compile -pl jipipe-core,jipipe-launcher -am`
Expected: PASS, BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): broker client, MCP settings, and --mcp CLI lifecycle"
```

### Task 8: Info operations: project info, algorithm catalog, node docs

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` (inner classes like the existing group files)
- Modify: `plugins/automation/AutomationPlugin.java` (register `get_project_info`, `list_algorithms`, `get_node_docs`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/InfoOperationsTest.java`

**Interfaces:**
- Consumes: Tasks 2-3 (`AutomationContext.getProject()`, node registry `JIPipe.getNodes()`, `@JIPipeDocumentation` annotations, taxonomy).
- Produces: ops `get_project_info` (metadata, compartment summary, cache stats), `list_algorithms` (registry catalog filtered by optional `query` string, returns id/name/description/functions), `get_node_docs` (requires `node-id`; returns name, description, parameter tree with per-parameter docs, taxonomy functions). Schemas per the Task 3 rules, `readOnly=true`, category `Query`.

- [ ] **Step 1: Write the failing test.** Initialize the JIPipe context the way existing core tests do; `get_node_docs` on any id from `JIPipe.getNodes().getRegisteredNodeInfos()` that has parameters returns a non-empty `parameters` array and non-empty `name`; `get_node_docs` with an unknown id returns a JSON `error` field; `list_algorithms` with `query` matching a substring of a known node name returns at least that node and every returned entry has non-empty `id`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=InfoOperationsTest`
Expected: FAIL

- [ ] **Step 3: Implement** the three operations + registration + schemas.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=InfoOperationsTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): project info, algorithm catalog, and node docs operations"
```

### Task 9: Data operations: describe and export

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/DataOperations.java`
- Modify: `plugins/automation/AutomationPlugin.java` (register `describe_data`, `export_data`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/DataOperationsTest.java`

**Interfaces:**
- Consumes: Task 8 patterns; existing `query_data` logic for slot/cache resolution; existing datatype export operations.
- Produces: `describe_data` (`compartment`, `node`, `slot` required; returns data type id, display name, row/count info where applicable, and for image types a small PNG preview as the `image-base64`/`mime` envelope from the spec) and `export_data` (additionally `format` and optional `target-dir`, default the project's export/temp directory; returns `{"saved-to": path}`; refuses paths outside the project/export directories). Both `readOnly=true`, category `Query`.

- [ ] **Step 1: Write the failing test.** Unknown node/slot combinations produce a JSON `error` field; `export_data` with a `target-dir` outside the allowed roots is refused with an `error` mentioning the allowed roots (this guard is the tested logic; the datatype delegation itself is existing infrastructure).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=DataOperationsTest`
Expected: FAIL

- [ ] **Step 3: Implement** the two operations + registration + schemas, reusing `query_data`'s cache resolution.

- [ ] **Step 4: Run test to verify it passes and compile**

Run: `mvn -q test -pl jipipe-core -Dtest=DataOperationsTest && mvn -q compile -pl jipipe-core`
Expected: PASS, BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): describe and export data operations"
```

### Task 10: Execution operations: cache, jobs, arrange

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/CacheOperations.java`
- Modify: `api/automation/operations/ExecutionOperations.java` (add `get_job_status`, `cancel_job`)
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ArrangeOperations.java`
- Modify: `plugins/automation/AutomationPlugin.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/CacheOperationsTest.java`

**Interfaces:**
- Consumes: the cache/run services the desktop cache actions invoke (locate via the "Update cache" UI action classes and reuse them, do not reimplement), `AutomationContext` job manager access, existing graph layout code (locate via `grep -rn "arrange\|autoLayout\|layout" jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor --include=*.java | head`; reuse it; only if nothing exists, implement a simple layered layout in the operation).
- Produces:
  - `update_cache`: params `scope` (`node`|`compartment`|`pipeline`), `mode` (`full`|`intermediate`|`predecessors`), plus scope selectors (`compartment`, `node` when scope != pipeline); returns `{"job-id": ...}` (async via the job manager). Schema enum-constrains `scope`/`mode`.
  - `get_job_status` (`job-id` -> state/progress), `cancel_job` (`job-id`) in `ExecutionOperations`, both `readOnly=false` for cancel, `readOnly=true` for status.
  - `arrange_nodes` (`compartment` required): triggers the layout, returns the new node positions map.

- [ ] **Step 1: Write the failing test.** Invalid `scope`/`mode` values are rejected by schema validation (`code:"invalid"` semantics via `IllegalArgumentException`); `update_cache` with valid args on a stub context returns a `job-id` field (stub the cache service behind a small interface `CacheTrigger` injected into the operation so the test needs no real cache).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=CacheOperationsTest`
Expected: FAIL

- [ ] **Step 3: Implement** the three operation groups + registration + schemas.

- [ ] **Step 4: Run test to verify it passes and compile**

Run: `mvn -q test -pl jipipe-core -Dtest=CacheOperationsTest && mvn -q compile -pl jipipe-core`
Expected: PASS, BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): cache, job, and arrange operations"
```

### Task 11: Error and log operations

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/DiagnosticsOperations.java`
- Modify: `plugins/automation/AutomationPlugin.java` (register `get_errors`, `get_log`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/DiagnosticsOperationsTest.java`

**Interfaces:**
- Consumes: the project validation/error panel data model (locate the classes the errors panel reads), `api/progress` store (`JIPipeProgressLog` memory-backed implementation; `log()` = INFO records, `debug()` = DEBUG level 10 records).
- Produces: `get_errors` (`compartment` optional filter; returns validation errors and, when a run happened, last-run errors with node/compartment references), `get_log` (`tail:int` default 200, `level` optional `INFO`|`DEBUG`, `node` optional filter; returns records newest-last). Both `readOnly=true`, category `Query`.

- [ ] **Step 1: Write the failing test.** Feed a memory-backed `JIPipeProgressLog` a mix of INFO and DEBUG records; `get_log` with `level:"INFO"` excludes DEBUG records; `tail:2` returns exactly the last two; `get_errors` on a project with a deliberately invalid graph (e.g. a node with a missing required input) returns at least one error entry with a node reference.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=DiagnosticsOperationsTest`
Expected: FAIL

- [ ] **Step 3: Implement** the two operations + registration + schemas.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=DiagnosticsOperationsTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): error and progress log diagnostics operations"
```

### Task 12: Screenshot operations

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ScreenshotOperations.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/utils/FakeTitlebarPainter.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationResults.java`
- Modify: `plugins/automation/AutomationPlugin.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/utils/FakeTitlebarPainterTest.java`, `.../automation/AutomationResultsTest.java`

**Interfaces:**
- Consumes: `JIPipeDesktopGraphCanvasPaintManager.paintMiniMap(...)` and the canvas painter for off-screen rendering; move `ScreenshotRenderer` from `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/screenshotui/ScreenshotRenderer.java` to `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/utils/ScreenshotRenderer.java` (update the launcher's import; the launcher depends on core) and use its off-screen realization (`JFrame` at -4000/-4000 + `printAll`) for never-shown components; `ScreenImage` for visible-window capture.
- Produces:
  - `AutomationResults` (result-envelope helpers used by ops and by the Task 6 broker): `static ObjectNode image(BufferedImage img, int scale)` (base64 PNG + `mime`), `static ObjectNode savedTo(Path p)`, `static ObjectNode choose(BufferedImage img, String saveTo)`: returns `savedTo` when `saveTo` is non-null (no inline image), else `image`.
  - Ops (all `readOnly=true`, category `Render`, schema params: `compartment` where relevant, `scale:number` default 2, `save-to:string` optional): `screenshot_canvas`, `screenshot_minimap`, `screenshot_ui` (`surface` string from a catalog), `screenshot_window` (`titlebar:boolean` default true). The GUI-side handler supplies the live component/canvas lookups (same handler-extension point pattern as Task 5; operations receive a `ScreenshotSource` interface so tests can pass fake sources).
  - `FakeTitlebarPainter.layout(String title, FontMetrics metrics, int width) -> FakeTitlebarLayout` (record with title x/y, button rects) and `paint(Graphics2D, FakeTitlebarLayout, String title)`: KDE 6 Breeze style, all text positions from `metrics`, buttons as vector shapes right-aligned with fixed gap constants only.

- [ ] **Step 1: Write the failing tests.** `FakeTitlebarPainterTest` (headless-safe via `BufferedImage.createGraphics()`): a title wider than half the bar is ellipsized (painted width never exceeds the reserved text rect); button rects are right-aligned with equal gaps and stay inside the bar; `layout` never returns negative coordinates at width 100. `AutomationResultsTest`: `choose(img, null)` has `image-base64` + `mime`; `choose(img, "/x/y.png")` has `saved-to` and no `image-base64`.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='FakeTitlebarPainterTest,AutomationResultsTest'`
Expected: FAIL

- [ ] **Step 3: Implement** the envelope helpers, the painter, and the four operations + registration + schemas (off-screen rendering through the paint manager; no `Robot` in canvas/minimap paths; `screenshot_window` uses `ScreenImage` on visible windows and the off-screen realization otherwise).

- [ ] **Step 4: Run tests to verify they pass and compile**

Run: `mvn -q test -pl jipipe-core -Dtest='FakeTitlebarPainterTest,AutomationResultsTest' && mvn -q compile -pl jipipe-core`
Expected: PASS, BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): documentation-grade screenshot operations with fake titlebar"
```

### Task 13: Desktop UI: statusbar control, Ask bubble, settings

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPStatusControl.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPOperationLogUI.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPSettingsSection.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java` (`initializeStatusBar()`, add after the AI control at the `isEnableAI()` gate, line ~641)
- Modify: `plugins/automation/AutomationPlugin.java` (register the settings section the way existing application-settings sections are registered; locate an existing example with `grep -rln "JIPipeDesktopApplicationSettingsUI\|SettingsSheet" jipipe-core/src/main/java | head`)
- Modify: the GUI wiring point from Task 7 so `AutomationEndpointHandlers` resolves the target `JIPipeDesktopProjectWindow` by project key, marshals to the EDT, and implements `AskPromptHandler` by raising the statusbar bubble.

**Interfaces:**
- Consumes: Tasks 4-7, 12; the statusbar patterns of `JIPipeDesktopAIStatusControl` (flat button, `UIUtils.makeButtonFlat`, reloadable popup menu, `SpinnerIcon` busy state).
- Produces: no public API consumed by other tasks; behavioral contract only:
  - Button: states off/starting/running/paused; label shows the last operation ("MCP: query_graph"); spinner while a call is in flight.
  - Popup: copy server URL (+ token when enabled), pause/resume, open MCP settings, open operation log, revoke grants (lists `AutomationGrant`s with per-entry revoke), pending Ask requests.
  - Ask bubble: anchored below the button, shows operation id + project name, answers Allow once / Allow always (this project) / Deny; answering unblocks the waiting endpoint thread (`CountDownLatch`).
  - Settings section "MCP": enable, port (8781), auto-start, require-token + regenerate + copy, per-tool permission table (dropdown Allow/Ask/Deny/Hide per operation id from the registry).

- [ ] **Step 1: Implement** the control, bubble, log window (list of operation-log records from the instance log store + broker `/control/logs`), and settings section; wire the workbench and the endpoint handlers.

- [ ] **Step 2: Compile and run the full core test suite**

Run: `mvn -q compile -pl jipipe-core && mvn -q test -pl jipipe-core`
Expected: BUILD SUCCESS, all tests PASS

- [ ] **Step 3: Commit** (no automated UI tests per test policy; the user verifies by eye)

```bash
git add -A
git commit -m "feat(#1369): MCP statusbar control, ask bubble, and settings section"
```

### Task 14: Remove instrumentation, clean dependencies, update docs

**Files:**
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/api/instrumentation/` (remaining: `InstrumentationServer.java`, `InstrumentationProtocol.java`, `InstrumentationApplicationSettings.java`)
- Delete: `jipipe-core/src/test/java/org/hkijena/jipipe/api/instrumentation/InstrumentationServerTest.java`
- Modify: `jipipe-launcher/src/main/java/org/hkijena/jipipe/launcher/commands/GuiCommand.java` (remove the `--instrumentation`/`--ai-agent-monitor` parsing block and the static server field; only `--mcp` lifecycle remains)
- Modify: `jipipe-core/pom.xml`, `dist/dist-info.json` (remove `Java-WebSocket-1.5.7.jar` after confirming no other `org.java_websocket` imports: `grep -rln "org.java_websocket" jipipe-core jipipe-desktop jipipe-launcher` must be empty first), regenerate scripts
- Modify: `AGENTS.md` ("Instrumentation System" section -> automation/MCP description: packages, launch flags `--mcp`, permissions, tool catalog pointer to the spec)
- Verify: `grep -rn "instrumentation" jipipe-core/src/main/java jipipe-launcher/src/main/java --include=*.java` returns only the plugin id literal in `AutomationPlugin` and comments that deliberately reference the history.

**Interfaces:**
- Consumes: all previous tasks; nothing may reference deleted classes.

- [ ] **Step 1: Delete the instrumentation remnants and the flag parsing; migrate any remaining reference.**

- [ ] **Step 2: Remove the Java-WebSocket dependency** (pom, dist-info.json, `cd dist && python3 generate-dist-scripts.py`) if and only if the grep shows no other users.

- [ ] **Step 3: Rewrite the AGENTS.md section** for the automation/MCP architecture (keep it terse: packages, lifecycle, flags, permission model, where operations are registered).

- [ ] **Step 4: Full verification**

Run: `mvn -q clean compile -pl jipipe-core -am && mvn -q test -pl jipipe-core`
Expected: BUILD SUCCESS, all tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1369): remove WebSocket instrumentation API and update docs"
```

### Task 15: End-to-end verification against a live JIPipe instance

**Files:**
- Create (throwaway, not committed): `/tmp/opencode/mcp-e2e/` (verification scripts)

**Interfaces:**
- Consumes: all previous tasks; `/data/JIPipe/` contains existing JIPipe projects and data for testing.
- Produces: a written verification report as the task result (no committed artifacts).

- [ ] **Step 1: Build the runtime classpath** (`mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/opencode/mcp-e2e/cp.txt -pl jipipe-core,jipipe-desktop -am`) and assemble the module classpath (jipipe-core/target/classes, jipipe-desktop/target/classes, imagej launcher shims per the AGENTS.md VM options: `--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/javax.swing=ALL-UNNAMED`).

- [ ] **Step 2: Launch the desktop app** with `--mcp 8790` on the available display, wait for `<user-dir>/mcp/discovery.json`, then open an existing project from `/data/JIPipe/` (pass it as the remaining argument or open via the UI).

- [ ] **Step 3: Drive the MCP endpoint with a raw JSON-RPC script** (Python `requests` or curl): initialize (capture `mcp-session-id`), `notifications/initialized`, `tools/list` (assert the new ops and migrated ops appear), `tools/call` on `list_projects`, `query_graph` and `get_node_docs` against the opened project, `screenshot_window`/`screenshot_canvas` (assert valid PNG bytes via base64 decode), `run_compartment` on a small compartment with an `Ask` default and verify the statusbar bubble flow manually, `get_job_status` until completion.

- [ ] **Step 4: Verify the negative paths against the live instance**: call with a wrong Bearer token after enabling the token setting, call a tool after pausing MCP from the popup, confirm the statusbar label updates and the operation log records entries.

- [ ] **Step 5: Report findings** in the session (what worked, what failed, screenshots of the statusbar/bubble states); file follow-up work items for anything broken that is out of scope to fix here.
