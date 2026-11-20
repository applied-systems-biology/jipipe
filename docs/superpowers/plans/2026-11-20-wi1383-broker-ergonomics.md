# #1383 Sub-project C - Broker Ergonomics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the broker's hardcoded tool knowledge with a machine-readable scope declared by each operation, make `project` optional everywhere with focused-window default routing, and add the `get_server_info` health probe.

**Architecture:** New `AutomationToolScope` enum on `AutomationOperation` (default `PROJECT_BOUND`), copied onto `MCPToolDescriptor` at registration; the broker reads scope from descriptors (or local metadata on the legacy path) and `GLOBAL_TOOL_IDS`/`EXCLUDED_TOOL_IDS` are deleted from the live path. Missing `project` routes like the instance-global tools and the instance resolves focused -> single-open -> candidate-listing error. New `get_server_info` operation.

**Tech Stack:** Java 21, Jackson databind, JUnit 5 in jipipe-core, MCP SDK (sync server), Maven.

**Spec:** `docs/superpowers/specs/2026-11-20-wi1383-broker-ergonomics-design.md`

## Global Constraints

- Commit format `type(#1383): subject`; explicit `git add` of exactly the touched files; no push.
- No em dashes or en dashes anywhere (code, Javadoc, comments, strings).
- Branch `1383-broker-ergonomics` (from master `333ae2bb40`; spec commits `48a9fbf385`, `4868dab75e`).
- Registered ids stay stable string literals; the scope enum values are code, NOT ids (nothing persists them in projects).
- Wire format: the descriptor's new field serializes as `"tool-scope"` (kebab-case, matching the existing `"read-only"`); `AutomationToolScope` values serialize as their enum names (`"PROJECT_BOUND"`, `"INSTANCE_GLOBAL"`, `"INSTANCE_GLOBAL_AGGREGATED"`, `"NOT_A_TOOL"`).
- Classification: `get_server_info` = Query, read-only, `requiresEdt() = false` (a health probe must not queue behind a busy EDT).
- House test policy: headless-safe only; the instance-side window resolution is user-verified by eye (NavigateToNode precedent).
- Test command per task: `mvn -q test -pl jipipe-core -Dtest=<TaskTests>,MCPBrokerServerTest,MCPToolDescriptorTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` (skip classes that do not exist yet). Builds are sequential - one slot; wait out concurrent mvn, never kill. Full-suite verification uses `mvn -q clean test -pl jipipe-core` (standing ruling: branch switches leave stale bytecode).

## Review Focus

Uncovered-by-spec failure modes and the task that pins each with a test:

1. **A stale/legacy registration without the `tool-scope` field must behave as PROJECT_BOUND** (old instances ship no field; the field initializer is the default). Pinned in Task 1 (descriptor deserialization without the field -> `PROJECT_BOUND`).
2. **The list_projects aggregation must survive the scope refactor byte-compatibly** - same merged, id-rewritten envelope as before. Pinned in Task 2 (existing `listProjectsAggregatesInstancesAndReturnsRoutableIds` stays green with the fixture now carrying `INSTANCE_GLOBAL_AGGREGATED`).
3. **`NOT_A_TOOL` exclusion must move to scope without letting `list_operations` leak into `tools/list`**. Pinned in Task 2 (fixture with `NOT_A_TOOL` stays absent from the tool list).
4. **Default routing must never override an explicit `project`** - explicit ids resolve exactly as today (registry lookup, unknown-id errors unchanged). Pinned in Task 2 (explicit-project call forwards the named key; unknown id still errors with the available-projects listing).
5. **`get_server_info` must not fail when no window is open** (headless dispatch, the window-count read is best-effort). Pinned in Task 4 (null-project context response shape; `projects` present and >= 0).

---

### Task 1: `AutomationToolScope` + operation declarations + descriptor field

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationToolScope.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperation.java` (new default method)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPToolDescriptor.java` (field + copy in `of`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ProjectOperations.java` (`ListProjects` :961, `ListOperations` :1012, `CreateProject` :337, `OpenProject` :562)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ExecutionOperations.java` (`GetJobStatus` :174, `CancelJob` :230)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java` (`ListAlgorithms` :318, `GetNodeDocs` :382)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java` (`SearchNodes` :87)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/endpoint/MCPToolDescriptorTest.java` (create)

**Interfaces:**
- Produces (Tasks 2-4 consume): enum `org.hkijena.jipipe.api.automation.AutomationToolScope` with values `PROJECT_BOUND`, `INSTANCE_GLOBAL`, `INSTANCE_GLOBAL_AGGREGATED`, `NOT_A_TOOL`; `AutomationOperation.getToolScope()` default `PROJECT_BOUND`; `MCPToolDescriptor.getToolScope()` (JSON `"tool-scope"`, always serialized, defaults to `PROJECT_BOUND` when absent on parse).

- [ ] **Step 1: Write the failing tests** (`MCPToolDescriptorTest`)

```java
@Test void scopeDefaultsToProjectBound()          // new MCPToolDescriptor() -> getToolScope() == PROJECT_BOUND
@Test void ofCopiesTheOperationScope()            // stub op overriding INSTANCE_GLOBAL -> descriptor carries it
@Test void jsonRoundTripPreservesScope()          // serialize descriptor with INSTANCE_GLOBAL_AGGREGATED -> field "tool-scope"=="INSTANCE_GLOBAL_AGGREGATED" -> deserialize -> same
@Test void absentFieldParsesAsProjectBound()      // JSON without "tool-scope" -> PROJECT_BOUND (the legacy-registration contract)
@Test void unknownFieldsAreTolerated()            // ignoreUnknown holds (extra field in JSON does not break parsing)
```

Stub operation: a minimal anonymous/inline `AutomationOperation` (id "test", schema `{"type":"object"}`, execute throws) overriding only `getToolScope()`.

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=MCPToolDescriptorTest` -> COMPILATION ERROR (enum/method/field missing).

- [ ] **Step 3: Implement the enum + default + overrides + descriptor field**

- Enum `AutomationToolScope` in `org.hkijena.jipipe.api.automation`: each constant Javadoc'd with its routing semantics (PROJECT_BOUND: schema gets the injected project argument; INSTANCE_GLOBAL: project-less calls route to the most-recent live instance carrying the tool, the E2E ruling 2026-11-16 semantics now declared by the tool; INSTANCE_GLOBAL_AGGREGATED: answered BY THE BROKER by aggregating every live instance carrying the tool and rewriting ids to routable form; NOT_A_TOOL: registered operation excluded from `tools/list`).
- `AutomationOperation.getToolScope()` default `PROJECT_BOUND`, Javadoc referencing the registry convention (the operation carries all information about itself).
- Overrides, exactly: `ListProjects` -> `INSTANCE_GLOBAL_AGGREGATED`; `ListOperations` -> `NOT_A_TOOL`; `GetJobStatus`, `CancelJob`, `ListAlgorithms`, `GetNodeDocs`, `SearchNodes`, `CreateProject`, `OpenProject` -> `INSTANCE_GLOBAL`.
- `MCPToolDescriptor`: field `private AutomationToolScope toolScope = AutomationToolScope.PROJECT_BOUND;` + `@JsonGetter("tool-scope")`/`@JsonSetter("tool-scope")` pair + `of(operation)` copies `operation.getToolScope()`. Class Javadoc's serialized-fields list gains the field.

- [ ] **Step 4: Run to verify pass** - same command -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationToolScope.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperation.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPToolDescriptor.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ProjectOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ExecutionOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/InfoOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/PipelineMapOperations.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/endpoint/MCPToolDescriptorTest.java
git commit -m "feat(#1383): declare the automation tool scope on operations and descriptors"
```

### Task 2: broker reads the scope (delete `GLOBAL_TOOL_IDS`; optional `project` routing)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServer.java` (`GLOBAL_TOOL_IDS` :122 deleted; `LIST_PROJECTS_TOOL_ID` :131 deleted or reduced to the aggregation envelope field; `PROJECT_ARGUMENT_DESCRIPTION` :138 reworded; `withProjectArgument` :331 scope-driven; `fallbackDescription` :349 scope-driven; `buildToolSpecification` both overloads :296/:313 capture the scope; `handleToolCall` :374 takes the scope and reroutes the missing-project case)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPInstanceRegistry.java` (`EXCLUDED_TOOL_IDS` :30 stays ONLY for the legacy id path; `aggregateToolDescriptors` :89 filters `NOT_A_TOOL` by scope; `aggregateToolIds` :71 keeps the id-based exclusion with a legacy Javadoc)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPToolDescriptor.java` (only if the aggregation needs a null-scope guard)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServerTest.java` (fixtures gain `"tool-scope"`; one test flips; new tests)

**Interfaces:**
- Consumes: Task 1's `getToolScope()` on descriptor and operation.
- Produces: broker behavior - `PROJECT_BOUND` tools get the injected (now optional) project argument; missing project routes to the most-recent live instance carrying the tool with a null forwarded key; `INSTANCE_GLOBAL_AGGREGATED` answers broker-side (aggregation, id rewrite); `NOT_A_TOOL` never appears in `tools/list`.

- [ ] **Step 1: Update the fixtures and flip/write the tests** (`MCPBrokerServerTest`)

Fixture changes in `registerInstanceWithDescriptors()`: `get_node_docs`, `search_nodes`, `get_job_status` fixtures gain `put("tool-scope", "INSTANCE_GLOBAL")`; ADD `list_projects` fixture (`"tool-scope": "INSTANCE_GLOBAL_AGGREGATED"`, the envelope the stub returns stays as the aggregation test needs); ADD `list_operations` fixture (`"tool-scope": "NOT_A_TOOL"`). `echo_test`/`render_image_test` stay WITHOUT the field (the legacy default contract).

```java
// FLIP: projectBoundToolWithoutProjectErrorsWithSchemaHint becomes:
@Test void projectBoundToolWithoutProjectRoutesWithNullKey()
// callTool("echo_test", {}) -> the stub records project-key == null (the forwarded body has no project key)
// and the call SUCCEEDS (the stub envelope comes back)

@Test void projectBoundToolWithoutProjectAndNoInstanceErrors()  // no registration -> error mentions no instance carries the tool
@Test void explicitProjectStillResolvesByName()                  // callTool("echo_test", {project: "<INSTANCE_ID>:<key>"}) -> stub records THAT key
@Test void explicitUnknownProjectStillErrors()                   // unknown id -> "Unknown instance in project id" error unchanged
@Test void notAToolStaysOutOfToolsList()                         // list_operations absent from tools/list (existing loop asserts)
@Test void lifecycleToolsPublishWithoutProjectArgument()         // create_project/open_project/get_server_info style: covered via the INSTANCE_GLOBAL fixtures (docs/search/jobs) having no project property
```

Existing tests that must stay green: `listProjectsAggregatesInstancesAndReturnsRoutableIds` (fixture now carries the scope), `globalToolWithoutProjectRoutesToRegisteredInstance`, `jobToolsRouteWithoutProject`, `registrationDescriptorsDriveToolsList`, `publishedSchemaDeclaresProjectForProjectBoundToolsOnly` (reworded assertions only if it hardcodes the removed constant semantics - the schema facts it asserts hold).

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=MCPBrokerServerTest` -> the flipped test FAILS (still the old "Missing required 'project' argument" error), `notAToolStaysOutOfToolsList` FAILS (list_operations currently excluded by the id set -> wait: it IS excluded today; this test guards the refactor - it must PASS before AND after; if it passes pre-refactor, it is the regression guard, not a red test; note the actual red set in the report).

- [ ] **Step 3: Implement the broker changes**

- `withProjectArgument(Map schema, AutomationToolScope scope)`: inject the project property only for `PROJECT_BOUND`; description text exactly: "Optional. Omit to target the focused project window of the routed instance (an explicit error lists the candidates when several are open and none is focused). Pass '<instanceId>:<projectKey>' as returned by list_projects to target a specific window or instance - required when several JIPipe instances are running".
- `fallbackDescription(String operationId, AutomationToolScope scope)`: project-bound wording keeps "the optional 'project' argument selects..."; non-project-bound wording keeps the existing registry-global sentence.
- Both `buildToolSpecification` overloads resolve the scope (descriptor: `descriptor.getToolScope()`; legacy: `operationMetadata != null && operationMetadata.get(operationId) != null ? ...getToolScope() : PROJECT_BOUND`) and pass it to `handleToolCall(operationId, scope, exchange, request)` (captured in the lambda).
- `handleToolCall`: missing/blank project -> if scope is `INSTANCE_GLOBAL`/`INSTANCE_GLOBAL_AGGREGATED`/`NOT_A_TOOL`(unreachable)/`PROJECT_BOUND` -> ALL take the instance-global routing path (`registry.findAnyByToolId`, else the no-instance error) and forward a null key; the aggregation branch fires when the resolved scope is `INSTANCE_GLOBAL_AGGREGATED` (replacing the `LIST_PROJECTS_TOOL_ID` id check). Explicit project path unchanged.
- Delete `GLOBAL_TOOL_IDS`; `PROJECT_ARGUMENT_DESCRIPTION` replaced by the new text; class Javadoc rewritten (the declared-scope contract replaces the hardcoded-set paragraph; keep the E2E 2026-11-16 and 2026-11-17 history references).
- `MCPInstanceRegistry.aggregateToolDescriptors`: skip descriptors with `getToolScope() == NOT_A_TOOL` (replace the `EXCLUDED_TOOL_IDS` loop there); `aggregateToolIds`: keep `EXCLUDED_TOOL_IDS` with the legacy Javadoc ("tool-ids-only registrations carry no scope; this id-based exclusion is the legacy path and is only active when no registration carries descriptors").
- Legacy `buildToolSpecification(String)`/`aggregateToolIds` path keeps working (operationMetadata may null-guard `getToolScope`).

- [ ] **Step 4: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=MCPBrokerServerTest,MCPToolDescriptorTest` -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServer.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPInstanceRegistry.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServerTest.java
git commit -m "feat(#1383): derive tool routing from the declared scope; make project optional at the broker"
```

### Task 3: instance-side default routing (focused -> single -> candidates)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java` (`execute` :491-509; new private resolution helper beside `findProjectWindowOnEdt` :567)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ProjectOperations.java` (`CreateProject`/`OpenProject` `getAgentDocumentation()` gain the permission-override note)

**Interfaces:**
- Consumes: `JIPipeDesktopProjectWindow.getOpenWindows()` (:277, unmodifiable live set), `KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow()` owner-chain walk (the `resolveDefaultProjectWindow` pattern, `AutomationOperationUtils.java:78-95`), `JsonUtils.getObjectMapper()` for the error envelope.
- Produces: a null `projectKey` resolves to a window (context built with that window's project AND its session id as the key) or the error envelope `{"error": "no focused project window and <n> projects are open: pass the 'project' argument with one of: <sessionId> (<name>), ..."}`. Explicit keys: unchanged behavior.

- [ ] **Step 1: Implement the resolution in `execute`**

When `projectKey == null`: resolve on the EDT (`callOnEdt`): (1) walk the active-window owner chain for a `JIPipeDesktopProjectWindow`; (2) else if `getOpenWindows()` has exactly one, that one; (3) else if more than one and none focused, return the candidates error (the listing carries each window's `getSessionId()` and project metadata name); (4) else (zero windows) keep today's `project = null` context. On success the context uses the resolved window's project and session id (progress handles, events, and Ask prompts attribute to the resolved window). A non-null key goes through `findProjectWindow` exactly as today. Javadoc on the helper documents the chain and the spec reference.

- [ ] **Step 2: Agent documentation for the lifecycle ops**

`CreateProject`/`OpenProject` `getAgentDocumentation()` append: "Unattended runs without a user to answer the permission prompt: start the application with --mcp-permissions allow=create_project,open_project (the CLI override)". No behavior change.

- [ ] **Step 3: Run the untouched-suite check**

Run: `mvn -q test -pl jipipe-core -Dtest=MCPBrokerServerTest,MCPToolDescriptorTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest`
Expected: PASS (no new tests - the resolution is window code, user-verified by eye per house policy; the suite proves no regression).

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ProjectOperations.java
git commit -m "feat(#1383): resolve the focused project window when the project argument is omitted"
```

### Task 4: `get_server_info` operation

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ProjectOperations.java` (new `GetServerInfo` class after `ListOperations` :1012)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java:68` (register after `open_project`)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectOperationsTest.java` (extend; the file exists from sub-project F)

**Interfaces:**
- Consumes: `JIPipe.getJIPipeVersion()` (`JIPipe.java:497`), `JIPipe.getAutomation().getOperationRegistry().getIds()`, `JIPipeDesktopProjectWindow.getOpenWindows()`.
- Produces: operation id `get_server_info`, no params (`emptyObjectSchema`), `INSTANCE_GLOBAL`, Query, read-only, `requiresEdt() = false`. Response: `{ "jipipeVersion": str, "projects": int >= 0, "operations": int > 0, "serverTime": epoch millis }`.

- [ ] **Step 1: Write the failing test** (in `ProjectOperationsTest`, modeled on its existing helpers)

```java
@Test void serverInfoReportsVersionCountsAndTime() // execute over a null-project context (headless)
    // jipipeVersion non-blank; operations >= 1; projects >= 0 (headless: 0); serverTime within 60s of now
```

- [ ] **Step 2: Run to verify failure** - `mvn -q test -pl jipipe-core -Dtest=ProjectOperationsTest` -> FAIL (no such operation/class).

- [ ] **Step 3: Implement `GetServerInfo`** in `ProjectOperations`: `getId()` "get_server_info"; `getToolScope()` `INSTANCE_GLOBAL`; Query/read-only/EDT-false; `emptyObjectSchema`; `execute` ignores ctx/params and builds the response node; the `projects` count reads `getOpenWindows().size()` inside a try/catch (best-effort, documented: a health probe never throws). Description: "Health and version probe: JIPipe version, open project count, registered operation count, and server time. Callable without a project". Register in `AutomationPlugin` after `open_project`.

- [ ] **Step 4: Run to verify pass** - `mvn -q test -pl jipipe-core -Dtest=ProjectOperationsTest,AutomationRegistryValidationTest,AutomationOperationsSchemaTest` -> PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/ProjectOperations.java \
  jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
  jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/operations/ProjectOperationsTest.java
git commit -m "feat(#1383): add the get_server_info health probe"
```

### Task 5: Full-suite verification

- [ ] **Step 1: Run the whole jipipe-core suite** - `mvn -q clean test -pl jipipe-core` (clean per the standing branch-switch ruling).
Expected: 0 failures, 0 errors; skips only the known environment-dependent classes; zero failures in new or touched test classes.

- [ ] **Step 2: Record the numbers** in the SDD workspace ledger (`.superpowers/sdd/2026-11-20-wi1383-broker-ergonomics/progress.md`): suite counts, any deferred minors, the by-eye list for the user (focused-window routing with several projects open; the candidates error; explicit `project` unchanged).

No commit (ledger path is git-ignored).
