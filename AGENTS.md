# JIPipe Development Guide

## Project Overview

JIPipe is a graphical batch processing language for image analysis built on Java 21 with Maven as the build system. It
uses a monolithic architecture with internal code separation rather than user-facing plugins.

## Build System

### Prerequisites

- **Java 21** (required)
- **Maven 3.8+**
- ImageJ/Fiji integration (for full functionality)

### Build Commands

```bash
# Clean and compile all modules
mvn clean compile

# Package the application
mvn package

# Run tests (only contrib module has tests configured)
mvn test -pl contrib/jipipe-ro-crate-java-2.1.0

# Generate Javadoc
mvn javadoc:javadoc

# Build with ImageJ integration (recommended for development)
mvn clean compile

# NOTE: Do NOT use -Dimagej.enabled=true - it is a legacy flag that is not
# applicable for modern SciJava-based ImageJ and is no longer used.
```

## Project Structure

### Main Modules

- **`jipipe-core/`** - Core framework and plugin system
- **`jipipe-desktop/`** - Desktop application UI
- **`jipipe-cli/`** - Command-line interface

### Plugin System (Internal)

The project uses a Java-based plugin architecture for internal code organization:

#### Key Classes:

- **[`JIPipeJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeJavaPlugin.java:29)** - Main plugin
  interface
- **[`JIPipeDefaultJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java:101)** - Base
  implementation with convenient registration methods
- **[
  `JIPipePrepackagedDefaultJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/JIPipePrepackagedDefaultJavaPlugin.java:30)** -
  Internal plugin with preconfigured authors

#### Core Plugins (in jipipe-core):

- [`CorePlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/core/CorePlugin.java:60) - Essential algorithms
  and data types
- [
  `StandardParametersPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/parameters/StandardParametersPlugin.java:110) -
  Parameter system
- [`ExpressionPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/expressions/ExpressionPlugin.java:48) -
  Expression evaluation
- And many others...

### Plugin Directories (Code Organization)

The `plugins/` directory contains additional modules for functionality separation:

- File system data sources
- Python integration
- Plotting capabilities
- Data environments
- etc.

### Node taxonomy (6.0.0)

Since 6.0.0, node categorization no longer uses the legacy `JIPipeNodeTypeCategory` / `menuPath` system (removed
entirely). Nodes are assigned to a **function tree** + **framework** + **spatial dimensions** + **tags** (see the
`api/nodes/taxonomy/` package):

- **Functions** - `org.hkijena.jipipe.api.nodes.taxonomy.functions`: a tree of 13 top-level branches (Images, Tables,
  Data sources, Tracking, …) with 48 leaves. Assign a node with
  `@ConfigureJIPipeNode(function = {SomeLeafFunction.class, OptionalSecondaryFunction.class})` - the first entry is the
  primary function, the rest are secondary (analogous to former menu aliases).
- **Frameworks** - `org.hkijena.jipipe.api.nodes.taxonomy.frameworks` (e.g. `CoreFramework`):
  `@ConfigureJIPipeNode(framework = CoreFramework.class)`.
- **Spatial dimensions / tags** - `@ConfigureJIPipeNode(spatialDimensions = {SPATIAL_2D})` (constants of
  `JIPipeNodeTaxonomySpatialDimension`, a closed set: 2D/3D) and `@JIPipeNodeTaxonomyTags("…")`.

Rules:

- Function/framework IDs are registration IDs and are persisted in saved projects - use **stable string literals**,
  never code-derived values (the same rule as node/data-type IDs, below).
- Every registered node **must** have a primary function. This is enforced by the branch gate
  `TaxonomyFullValidationTest` (jipipe-launcher; runs the production validation over the entire registry and fails on
  any unassigned node) and by the per-module `TaxonomyConversionReportTest` gates (run with
  `-Djipipe.taxonomy.report.module=<java-package-prefix>`).
- Conversion decision record: `utils/taxonomy-conversion/conversion-map.md`. Design spec:
  `docs/superpowers/specs/2026-10-08-node-taxonomy-redesign-design.md`.

## Development Patterns

### Creating a New Algorithm Node

1. **Create algorithm class** extending [
   `JIPipeGraphNode`](jipipe-core/src/main/java/org/hkijena/jipipe/api/nodes/JIPipeGraphNode.java)
2. **Register in plugin** using convenient methods:

```java
// In your JIPipeDefaultJavaPlugin subclass:
public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
    // Register algorithm with icon
    registerNodeType("my-algorithm-id", MyAlgorithmNode.class, 
        ResourceUtils.getPluginResource("algorithm-icon.png"));
    
    // Or use advanced registration with dependencies
    registerNodeType(new JIPipeJavaNodeRegistrationTask(
        "my-algorithm-id", MyAlgorithmNode.class, this, icon));
}
```

### Adding New Data Types

```java
// Register data type with import/export operations
registerDatatype("my-data-type", MyData.class, icon,
    new MyImportOperation(), 
    new MyExportOperation(),
    new MyDisplayOperation());
```

### Plugin Registration Methods

The [`JIPipeDefaultJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeDefaultJavaPlugin.java:101) provides
convenient methods for:

- **Node registration**: `registerNodeType()`, `registerAnnotationNodeType()`
- **Data types**: `registerDatatype()`, `registerDatatypeOperation()`
- **Parameters**: `registerParameterType()`, `registerEnumParameterType()`
- **Expressions**: `registerExpressionFunction()`, `registerTableColumnOperation()`
- **UI components**: `registerMenuExtension()`, `registerGraphEditorToolBarButtonExtension()`
- **Examples**: `registerNodeExample()`, `registerNodeTemplate()`

### Keyboard Shortcuts

Every user-triggerable UI action (graph editor tools, context actions, menu accelerators, dock actions) is a
`JIPipeShortcutAction` with a stable string-literal id (persisted in saved keymap profiles: never code-derived).
Register actions in the plugin's `register()` via
[`JIPipeShortcutRegistry`](jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutRegistry.java)
(`JIPipeShortcutAction.builder(id, name, category)`; idempotent: skip already-registered ids). The core catalog
lives in `JIPipeShortcutActions` (registered by `StandardSettingsPlugin`). Bindings resolve user override >
default; the Keyboard settings section (`JIPipeDesktopKeyboardSection`) edits overrides. Never read
`KeyStroke`s from actions or tooltips: render for display through
`JIPipeShortcutRegistry.formatChord`/`formatVariants` and pass the pre-rendered text to
`TooltipUtils.createTooltipWithShortcutText`.
The per-window command bar lists the registry's actions as entries (including unassigned ones); its trigger action is
`org.hkijena.jipipe.command-bar.open` (defaults Ctrl+P / Ctrl+Shift+P). A chord prefix opens the bar in chord mode via
the dispatcher handoff; window/tab switching are ephemeral bar entries (not registry actions, not bindable). The list
mode (grayed unavailable / available only / everything) is the `registry:command-bar` setting; recently fired commands
rank first on an empty query.

## Testing

### Current Test Infrastructure

Only the [`contrib/jipipe-ro-crate-java-2.1.0`](contrib/jipipe-ro-crate-java-2.1.0/pom.xml) module has JUnit 5 tests
configured.

```bash
# Run all available tests
mvn test

# Run specific test class
mvn test -Dtest=MyTestClass
```

### Adding Tests

For new modules, add JUnit 5 dependencies and create `src/test/java` structure following the contrib module pattern.

### Test Policy: Do Not Test Trivial Things (IMPORTANT)

**Tests cover non-trivial behavior and contracts - not the re-verification of
obvious code or UI structure** (ruling 2026-10-20; extends the "no Swing
interaction tests" rule - the user verifies the Swing UI by eye in the app:
"I test it myself", interaction tests were removed as "just messy").

- **Swing UI structure: no tests.** Do NOT assert low-level component
  structure - tab titles/order, component visibility or enabled toggles,
  border/padding/insets, section collapse state, cell-renderer details,
  dialog layout. If a UI change looks right in the code, it is done; the
  user checks the running app and reports what is wrong.
- **Trivial code: no tests.** No tests for getters/setters, pure
  delegation, simple if/else, or constants - and no test whose assertion
  just restates the implementation ("set X, assert X == X").
- **What DOES warrant a test:** non-trivial logic - algorithms, data
  transformations, serialization round-trips, ID stability, ordering/
  concurrency contracts, idempotency (generators are byte-stable),
  fail-closed guards, registry/ship-set contracts (e.g.
  `SystemPatternTemplatesTest` count + deserialization pins, taxonomy
  validation gates).
- **Heuristic:** if deleting the test would not make it harder to catch a
  regression the user actually cares about, do not write it. When in doubt,
  skip the test.

### Plugin-Aware Registry Tests: Presence, Not Exact Counts (IMPORTANT)

**Tests that assert on a LIVE service-component registry must be designed
aware that plugins contribute to that registry** (ruling 2026-10-22: "All
tests should be designed aware of plugins").

- **Never pin an exact registry size** for anything a plugin can register
  into - nodes, datatypes, taxonomy functions/frameworks, expressions,
  parameters, menu items, ... A test of the form
  `assertEquals(N, JIPipe.getNodes().getFunctions().all().size())` breaks the
  moment any dev adds one taxonomy member through their plugin, even though
  the core set is intact. (This is what happened to
  `TaxonomyPluginTest.allTaxonomyRegistered`, pinned at 89 while the registry
  held 90.)
- **Assert presence instead:** every expected ID (a curated list of the
  core's stable string-literal IDs) is registered, and any structural
  invariants hold (valid parent chains, unique top-level hues, ...). Extra
  IDs contributed by plugins are legitimate, not failures.
- **Exact counts are still correct for single-source ship sets:** artifacts
  generated in one place whose membership is fully controlled by core (e.g. the bundled `system-templates.json` +
  pattern files in
  `SystemPatternTemplatesTest`). The distinction is *who can add entries*:
  a generator output has one owner; a service registry has many.

### ImageJ Legacy Patcher Constraint (IMPORTANT)

The ImageJ2 legacy patcher (`net.imagej.patcher.LegacyInjector`) bytecode-patches ImageJ1 (`ij.*`) classes for ImageJ1/2
cross-compatibility. **Patching is only possible while the affected classes are not yet initialized.**

- Any test that touches `ij.*` classes (e.g. `new ij.process.AutoThresholder()`, `ShortProcessor`, `IJ.*`) loads them
  **unpatched** if the patcher has not run yet.
- If a *later* test then creates the SciJava context (`JIPipe.ensureInstance()` → `new net.imagej.ImageJ()`),
  `LegacyService` fails with `No _hooks field found in ij.IJ` - **an order-dependent failure** (passes locally in one
  order, fails in CI in another).
- **Protection already in place:** [
  `ImageJLegacyPatcherLauncherSessionListener`](jipipe-core/src/test/java/org/hkijena/jipipe/ImageJLegacyPatcherLauncherSessionListener.java)
  is a JUnit 5 `LauncherSessionListener` (registered via `jipipe-core/src/test/resources/META-INF/services/`) that calls
  `LegacyInjector.preinit()` at launcher-session start, before any test class is loaded. Do not remove it or its
  service-file registration.
- **Rules for new tests:**
    - Do NOT call `LegacyInjector.preinit()` yourself, and do NOT add `--add-opens`/javaagent workarounds - the listener
      already handles this for the whole suite.
    - If you add a test that initializes ImageJ1 classes *before* any `JIPipe.ensureInstance()` test runs, keep the
      listener intact; if you observe `No _hooks field found in ij.IJ`, the listener registration or ordering is
      broken - check the service file first.
    - When adding a new module with tests that use `ij.*` or `JIPipe.ensureInstance()`, mirror the jipipe-core setup:
      `junit-platform-launcher` test dependency + a `LauncherSessionListener` service registration (or extend the core
      listener's module).
    - Regression guard: [
      `ImageJ1ClassLoadingOrderTest`](jipipe-core/src/test/java/org/hkijena/jipipe/utils/threshold/ImageJ1ClassLoadingOrderTest.java)
      loads ImageJ1 classes before `JIPipe.ensureInstance()` to catch ordering regressions.
- **Never "fix" this by excluding the failing test** - the underlying patcher ordering is the actual contract.

## IDE Development

### Main Class for Debugging

- **Desktop**: [`JIPipeDesktopMain`](jipipe-desktop/src/main/java/org/hkijena/jipipe/desktop/JIPipeDesktopMain.java)
- **CLI**: Available in jipipe-cli module

### Required VM Options (for ImageJ integration)

When debugging in IDE, add these VM arguments:

```
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.desktop/javax.swing=ALL-UNNAMED
```

## Code Style Guidelines

### Git Branch Naming

Follow GitLab branch naming conventions: `<issue-id>-<name-in-kebab-case>`

Examples:

- `1310-cellpose-42-models-actionable-errors`
- `1308-ro-crate-improvements`

### No Em Dashes or En Dashes (IMPORTANT)

Do **not** use em dashes (U+2014) or en dashes (U+2013) in code, Javadoc, comments, or
user-facing strings. Use a regular hyphen, a colon, a comma, or a period instead. This keeps
searches and diffs predictable and avoids AI-sounding typography. (The existing codebase uses
plain hyphens in prose, e.g. `- The codebase has ...`; follow that.)

### Never Assume Text Sizes - Measure Them (CRITICAL)

Never hardcode pixel widths, heights, offsets, or gaps that depend on rendered text. Text
dimensions vary with font, font size, scaling, Look and Feel, language, and content, so any
assumed size clips or misplaces text. Always measure:

- Position or size text-dependent components from the component's own preferred size, or from
  `FontMetrics`/`SwingUtilities.computeStringWidth` measured against the actual string and font.
- In custom cell renderers (tree/list), text changes via `JLabel.setText()` only queue an
  asynchronous revalidate, and `CellRendererPane.validate()` no-ops while the row container is
  still flagged valid - so the layout would render the PREVIOUS row's measured bounds. After
  changing any text in a reused renderer row, force a synchronous re-layout:
  `panel.invalidate(); panel.validate();` (see `JIPipeDesktopFindPanel.FindTreeCellRenderer`).
- Fixed-size struts/padding around text are allowed only as gaps (they do not encode a text
  size); anything that must enclose or align with text (badges, chips, columns, dialogs) must
  derive its size from measurement.
- Audit rule for reviews: in new UI code, flag `setPreferredSize`/`new Dimension`/fixed-width
  arithmetic on or near text-bearing components unless the value is measured or a pure gap.

### General Principles

1. **Follow existing patterns** - The codebase has consistent patterns for plugin registration and algorithm
   implementation
2. **Use the provided convenience methods** from `JIPipeDefaultJavaPlugin` rather than direct registry access when
   possible
3. **Implement proper metadata** - All plugins should define name, description, authors, etc.
4. **Handle dependencies gracefully** - Use the scheduling system for node registration

### Registered IDs Must Be Stable String Literals (IMPORTANT)

Registration IDs (node types, data types, parameter types, expression functions, ...) are **persisted in saved projects
and pipelines** (e.g. as `field-class-id` in project JSON). They must never be derived from code:

- **Never** use `SomeClass.class.getCanonicalName()`, `.getName()`, or any other code-derived value as a registration
  ID. A rename/refactor would silently break every saved project that uses the ID.
- **Always** use an explicit string literal ID (e.g. `"ij1-threshold-local-auto2d-16u:method"`).
- **Exception:** IDs for third-party features that are integrated automatically (e.g. discovered ImageJ plugins) may be
  generated from the third-party's own identifiers - never from JIPipe's Java code.
- If you inherit a legacy code-derived ID, freeze its current value as a string literal (projects contain it verbatim)
  and keep it unchanged.

### Plugin Development Best Practices

1. **Extend `JIPipePrepackagedDefaultJavaPlugin`** for internal core plugins (preconfigured authors)
2. **Extend `JIPipeDefaultJavaPlugin`** for custom development
3. **Use proper resource loading** via [
   `ResourceUtils`](jipipe-core/src/main/java/org/hkijena/jipipe/utils/ResourceUtils.java)
4. **Follow the registration pattern**: implement `register()` method, call superclass methods in `postprocess()`

### Important Notes

- The JSON-based plugin system ([
  `JIPipeJsonPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeJsonPlugin.java)) is **unused** - the project
  pivoted to Java-only plugins for internal code organization
- Plugin management is kept away from users - it's purely for development organization and authorship tracking
- All core functionality uses the [
  `JIPipeJavaPlugin`](jipipe-core/src/main/java/org/hkijena/jipipe/JIPipeJavaPlugin.java:29) interface system

## Dependencies

### Key External Libraries

- **ImageJ/Fiji** - Image processing foundation
- **Jackson** - JSON serialization/deserialization
- **SciJava** - Plugin framework and context injection
- **SLF4J** - Logging
- **JGraphT** - Graph algorithms for data flow

### Internal Dependencies

The project uses a multi-module Maven structure with careful dependency management. Core plugins depend on each other in
a specific order during initialization.

## MCP server / automation layer

JIPipe exposes its automation operations to external agents over MCP (Model Context Protocol). A shared broker
(servers/mcp) fronts per-instance HTTP endpoints. The old WebSocket-based instrumentation API
(`api/instrumentation`) was removed in this change and replaced by this layer.

### Architecture

- **Operation layer** (`api/automation`): [`AutomationOperation`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperation.java)
  is the operation interface: `getId()`, `getDescription()`, `getInputSchema()` (JSON schema mirroring what
  `execute()` parses, `additionalProperties:false`), `getCategory()`/`isReadOnly()`, and
  `execute(AutomationContext, JsonNode)`. [`AsyncAutomationOperation`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AsyncAutomationOperation.java)
  adds `executeAsync` for long-running operations (returns an `InstrumentationJob` immediately).
  [`AutomationOperationRegistry`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationRegistry.java)
  is the ConcurrentHashMap-backed registry; it validates parameters against the input schema before dispatch.
  [`AutomationContext`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationContext.java)
  carries project, project key, progress, event bus, and job manager; `ctx.getGraph()` (not
  `ctx.getProject().getGraph()`) respects the graph override mechanism used by the AI agent's stage system.
  [`JIPipeAutomationAPI`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java)
  holds the typed static methods that operation implementations delegate to. Registration is plugin-extensible
  (any `JIPipeJavaPlugin` can register operations).
- **Instance endpoint** (`api/automation/endpoint`): [`AutomationInstanceEndpoint`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/AutomationInstanceEndpoint.java)
  serves the global operation registry on 127.0.0.1, authenticated by a per-endpoint secret
  (`X-JIPipe-Endpoint-Secret` header, constant-time compare). Errors travel in the JSON envelope as
  `ok:false` with a code of `unauthorized|hidden|denied|unknown|invalid|failed`; operations with a null or
  non-object input schema are refused (fail-closed).
- **Broker client lifecycle**: [`MCPBrokerClient`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/endpoint/MCPBrokerClient.java)
  connects to a running broker via the discovery file `<JIPipe user dir>/mcp/discovery.json` (PID liveness
  check plus `GET /health`), deletes stale files, and otherwise spawns a broker ([`MCPServerProcess`](jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPServerProcess.java),
  SharedServerProcess-based) with a 30 second health poll. The GUI launcher starts the client on the first
  project window when `--mcp [port]` is given (or `MCPApplicationSettings.autoStart` is set) and shuts it down
  when the last window closes; the lifecycle runs on a background executor, never on the EDT. Registration
  (`POST /control/register`) carries project entries (project key = project window session id) and tool
  descriptors built from the global operation registry.
- **Broker** (`servers/mcp`): [`MCPBrokerServer`](jipipe-core/src/main/java/org/hkijena/jipipe/servers/mcp/MCPBrokerServer.java)
  serves streamable HTTP MCP on `/mcp` plus the control routes `POST /control/register`,
  `POST /control/unregister`, and `GET /control/logs` (`GET /health` is open). Default port 8781 via
  [`MCPApplicationSettings`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/settings/MCPApplicationSettings.java)
  (an occupied port falls back to a free one). Optional bearer token (`require-token` setting): the shared
  token file `<JIPipe user dir>/mcp/token` is generated once and presented as `Authorization: Bearer` on
  `/mcp` and `/control/*`.
- **Permissions** (`api/automation/permissions`): [`AutomationPermissionService`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/permissions/AutomationPermissionService.java)
  resolves the [`AutomationPermissionState`](jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/permissions/AutomationPermissionState.java)
  of each operation: Allow, Ask, Deny, Hide. Resolution order: Allow-always grant (per project, in-memory),
  CLI override, user state (settings permission map), then the category default
  (`isReadOnly() ? Allow : Ask`). Ask shows a bubble anchored to the MCP statusbar button of the target
  project window; the wait is bounded at 300 seconds so a paused UI cannot hold endpoint threads forever
  (deny on timeout). CLI `--mcp-permissions allow=<ids> deny=<ids>` overrides are session-only and
  fail-closed (the most restrictive state wins).
- **Desktop integration** (`org.hkijena.jipipe.desktop.app.automation`): [`JIPipeDesktopMCPIntegration`](jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPIntegration.java)
  dispatches operations on the EDT, owns the ask bubble, the operation log, and the screenshot source;
  [`JIPipeDesktopMCPStatusControl`](jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/automation/JIPipeDesktopMCPStatusControl.java)
  is the statusbar button and popup.
- **Tool catalog**: `docs/superpowers/specs/2026-11-16-mcp-server-design.md`.

### Adding a New Operation

1. Create a static inner class implementing `AutomationOperation` (or `AsyncAutomationOperation` for
   long-running tasks) in an operations group file under `api/automation/operations/`
2. Implement `getId()` (stable snake_case string literal, persisted in permission settings and CLI
   overrides), `getDescription()`, `getInputSchema()` (mirroring what `execute()` parses,
   `additionalProperties:false`), `getCategory()`/`isReadOnly()`, and `execute(AutomationContext, JsonNode)`
3. Register it in [`AutomationPlugin.register()`](jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java)
   via `registerAutomationOperation(id, operation)`; MCP exposure is automatic (`list_operations` itself is
   excluded from the tool catalog)
4. Fire events via `ctx.getEventBus().publish(...)` so connected clients are notified of changes

## Progress system (6.0.0)

Progress logging is store-based, in the `api/progress` package: [`JIPipeProgressLog`](jipipe-core/src/main/java/org/hkijena/jipipe/api/progress/JIPipeProgressLog.java)
is the store interface (silent, stdout, and file-backed NDJSON implementations); record types are `JIPipeProgressRecord`,
`JIPipeProgressNode`/`JIPipeProgressNodeKind`/`JIPipeProgressNodeState`, and `JIPipeLogLevel`. Memory is bounded: only the
node index and a tail of recent records are held, and random-range reads use a checkpoint index. [`JIPipeProgressInfo`](jipipe-core/src/main/java/org/hkijena/jipipe/api/JIPipeProgressInfo.java)
is a handle over the store: `log()` emits an INFO record, `debug()` a DEBUG (10) record - use `debug()` for per-item/per-pair
logging (hidden by the INFO-level UI filter and text render), `log()` for per-run messages. Consumers receive coalesced
`ProgressBatchEvent`s (at most one per 100 ms plus a final empty batch, where `fromLine > toLine`). Logs live as
`logs/<run-uuid>.ndjson` in the project/global temp directory - never in the output folder; `log.txt` in output folders is a
streamed compat render of the store.

## Distribution Build Scripts

The ZIP build scripts (`dist/zip/build.sh`, `dist/zip/build-release.sh`, `dist/zip/build-no-dependencies.sh`) are
**generated files** - do not edit them directly.

### How to add or update an external dependency

1. Add the dependency to [`dist/dist-info.json`](dist/dist-info.json) under the `"dependencies"` object, mapping the jar
   filename to its Maven Central download URL
2. Run `python3 dist/generate-dist-scripts.py` (from the `dist/` directory) to regenerate all build scripts
3. If the dependency is also needed in the `jipipe-distribution-files` repo, add the jar to
   `scripts/plugin-dependencies/` in that repo

### Key files

- **[`dist/dist-info.json`](dist/dist-info.json)** - Source of truth for external dependencies, contrib modules, and
  JIPipe modules
- **[`dist/generate-dist-scripts.py`](dist/generate-dist-scripts.py)** - Generator script that produces the build shell
  scripts from `dist-info.json`