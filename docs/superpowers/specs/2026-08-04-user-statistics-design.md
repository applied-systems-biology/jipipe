# User Statistics Feature Design

**Work Item:** [#1304](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1304)
**Date:** 2026-08-04
**Scope:** JIPipe-side implementation only (capture, storage, UI, reporting). The companion web service is a separate project.

## Purpose

JIPipe requires capture of user statistics to fill out DFG KPI forms for NFDI4BIOIMAGE. The system must give users the option to not send statistics or limit them to their preferred level, while nudging them to enable as many as possible. "Fun" statistics gameify the collection to reduce user aversion.

## Architecture Overview

A new `StatisticsPlugin` (JIPipePrepackagedDefaultJavaPlugin) registers all statistics-related components. A new `JIPipeStatisticsServiceComponent` (JIPipeServiceComponent) owns the lifecycle: loads/saves `statistics.json`, runs the daily reporting timer, and provides the API for incrementing counters.

Statistics items follow a registry-based pattern (like nodes, data types, parameters). Each item implements `JIPipeStatisticsItem` and is registered with `JIPipeStatisticsRegistry`. This makes the system extensible — future plugins can register their own items, and the deferred project statistics panel can query the same registry without code duplication.

### Initialization Flow

1. `JIPipeService` constructor creates `JIPipeStatisticsServiceComponent` (alongside other service components)
2. `JIPipeServiceDefaultInitializer.runInitialization()` registers plugins — `StatisticsPlugin.register()` registers the privacy level enum, settings sheet, Tools menu item, and all v1 statistics items
3. Settings reload loads `privacyLevel`, `serverUrl`, `enabled`, `showFirstTimePrompt` from `settings.json`
4. `JIPipeStatisticsServiceComponent.postprocess()` loads `statistics.json`, subscribes to `JIPipeRunnableQueue` events, starts the daily reporting timer

## Component Design

### 1. Plugin: `StatisticsPlugin`

- **Package:** `org.hkijena.jipipe.plugins.statistics`
- **Extends:** `JIPipePrepackagedDefaultJavaPlugin`
- **Annotation:** `@Plugin(type = JIPipeJavaPlugin.class)`
- **isCorePlugin():** `true`
- **Dependency ID:** `org.hkijena.jipipe:statistics`
- **register():**
  - `registerEnumParameterType("statistics-privacy-level", StatisticsPrivacyLevel.class, "Statistics privacy level", "Controls what usage statistics JIPipe collects and sends")`
  - `registerApplicationSettingsSheet(new JIPipeStatisticsApplicationSettings())`
  - `registerMenuExtension(ShowStatisticsTool.class)`
  - Register all v1 statistics items via `JIPipe.getService().getStatistics().getRegistry().registerItem(...)`

### 2. Service Component: `JIPipeStatisticsServiceComponent`

- **Package:** `org.hkijena.jipipe.api.service.components`
- **Extends:** `JIPipeServiceComponent`
- **Registered in:** `JIPipeService` constructor
- **Accessible via:** `JIPipe.getService().getStatistics()`

**Responsibilities:**
- Owns `JIPipeStatisticsRegistry` (the item registry)
- Loads `statistics.json` on `postprocess()` — restores item values, reads machine ID, timestamps
- Creates machine ID (UUID) on first launch if none exists
- Subscribes to `JIPipeRunnableQueue` events for workflow run counting
- Starts daily reporting timer (1-hour tick, checks if 24h elapsed since last send)
- Provides debounced save (`saveLater()` — 250ms `javax.swing.Timer`)
- Provides `sendNow()` for manual/immediate sends
- Provides `getMachineId()`, `rerollMachineId()`

**postprocess() sequence:**
1. Load `statistics.json` (or initialize with new machine ID + `firstLaunchTimestamp`)
2. Restore each registered item's state via `item.deserialize(jsonNode)`
3. Subscribe to `JIPipeRunnableQueue.getInstance().getFinishedEventEmitter()` for workflow run counting
4. Start the daily reporting timer

### 3. Statistics Items Registry

**Interface: `JIPipeStatisticsItem`**
```java
public interface JIPipeStatisticsItem {
    String getId();
    String getName();
    String getDescription();
    JIPipeStatisticsItemCategory getCategory();
    StatisticsPrivacyLevel getRequiredPrivacyLevel();
    JsonNode serialize();
    void deserialize(JsonNode node);
    void reset();

    default void initialize(JIPipeStatisticsServiceComponent service) {}
}
```

The `initialize()` default method is called during the service component's `postprocess()`. Items that need event subscriptions override it to register listeners. Passive items leave it as the default no-op.

**Category enum: `JIPipeStatisticsItemCategory`** — `Machine`, `Usage`, `Fun`

**Registry: `JIPipeStatisticsRegistry`**
- `Map<String, JIPipeStatisticsItem> registeredItems`
- `registerItem(JIPipeStatisticsItem)` — registers an item
- `getItemsForLevel(StatisticsPrivacyLevel)` — returns items whose required level <= given level
- `getItemsByCategory()` — groups items for UI display

**Item behavior types:**
- **Passive items** (Machine ID, OS, RAM, version, GPU, recent projects count): compute value on demand in `serialize()`, ignore `deserialize()`
- **Persistent items** (counters, accumulators): store accumulated state, use `serialize()`/`deserialize()` for persistence

### 4. V1 Statistics Items

| Item | ID | Category | Required Level | Type |
|------|----|----------|---------------|------|
| Machine ID | `machine-id` | Machine | Installation | String (UUID) |
| Operating system | `operating-system` | Machine | Installation | String |
| Total RAM (MB) | `total-ram-mb` | Machine | Installation | int |
| JIPipe version | `jipipe-version` | Machine | Installation | String |
| GPU/VRAM info | `gpu-info` | Machine | Everything | String |
| Workflow runs | `workflow-runs` | Usage | RoughProjects | int (counter) |
| Recent projects count | `recent-projects-count` | Usage | RoughProjects | int |
| RO-Crates created | `ro-crates-created` | Usage | RoughProjects | int (counter) |
| Node move distance | `node-move-distance` | Fun | Everything | double (accumulated) |
| Longest node width | `longest-node-width` | Fun | Everything | int (max) |
| Top 5 popular nodes | `popular-nodes` | Fun | Everything | Map<String,Int> |
| Largest project (nodes) | `largest-project-nodes` | Fun | Everything | int (max) |
| Largest project (compartments) | `largest-project-compartments` | Fun | Everything | int (max) |
| Noodle score (min/avg/max) | `noodle-score` | Fun | Everything | 3 doubles |

### 5. Privacy Levels

**Enum: `StatisticsPrivacyLevel`** (registered via `registerEnumParameterType`)

| Level | Value | Description |
|-------|-------|-------------|
| None | 0 | No statistics collected or sent |
| Installation | 1 | Machine ID + JIPipe version (sent once on first consent) |
| ActiveInstallation | 2 | + daily ping (proves installation is still active) |
| RoughProjects | 3 | + project count, workflow runs, RO-Crates created |
| Everything | 4 | + all fun statistics (DEFAULT) |

### 6. Settings

**Settings sheet: `JIPipeStatisticsApplicationSettings`**
- **Extends:** `JIPipeDefaultApplicationsSettingsSheet`
- **Category:** `General`
- **ID:** `org.hkijena.jipipe:statistics`

**Fields (all standard parameter types, no custom editors):**
- `enabled` — boolean, default `true` (master on/off switch)
- `privacyLevel` — `StatisticsPrivacyLevel`, default `Everything`
- `serverUrl` — String, default `https://jipipe.hki-jena.de/statistics/api/v1/statistics`
- `showFirstTimePrompt` — boolean, default `true` (set to `false` after first prompt is shown)

**Machine ID** is NOT in settings — it is stored in `statistics.json` and managed by the service component. It is displayed in the Tools > Statistics viewer dialog with a re-roll button.

### 7. Storage & Concurrency

**File:** `<JIPipe user dir>/statistics.json`

**Format:**
```json
{
  "machineId": "550e8400-e29b-41d4-a716-446655440000",
  "firstLaunchTimestamp": "2024-01-10T08:00:00",
  "lastSentTimestamp": "2024-01-15T10:30:00",
  "items": {
    "workflow-runs": 42,
    "node-move-distance": 12345.6,
    "popular-nodes": {"import-image": 10, "threshold": 5},
    "noodle-score": {"min": 12.0, "avg": 45.5, "max": 120.0}
  }
}
```

**Concurrency strategy** (multiple JIPipe instances share the same file):

1. **Separate lock file:** `statistics.lock` (stable file, never replaced)
2. **Read-modify-write cycle** for counter updates:
   - Acquire `FileLock` on `statistics.lock` (via `FileChannel`)
   - Re-read `statistics.json` from disk (to get latest state from other instances)
   - Merge: update only dirty items in the loaded JSON (other instances may have updated other items)
   - Write to `statistics.json.tmp`, then `Files.move(tmp, statistics.json, ATOMIC_MOVE)`
   - Release lock
3. **Debounced saves:** 250ms `javax.swing.Timer` (following `JIPipeApplicationSettingsServiceComponent` pattern). Rapid counter increments coalesce into a single read-modify-write cycle.
4. **Startup load:** On `postprocess()`, read `statistics.json` once into memory. Items use this as their initial state. Subsequent updates go through the debounced save path.
5. **Dirty tracking:** In-memory state tracks which items have changed since last save. On save, only dirty items are merged into the file.

### 8. Reporting

**Reporting payload** (POST to configured server URL):
```json
{
  "machineId": "550e8400-e29b-41d4-a716-446655440000",
  "jipipeVersion": "3.0.0",
  "timestamp": "2024-01-15T10:30:00",
  "privacyLevel": "EVERYTHING",
  "items": {
    "operating-system": "Linux",
    "total-ram-mb": 32768,
    "workflow-runs": 42,
    "node-move-distance": 12345.6,
    "popular-nodes": {"import-image": 10, "threshold": 5}
  }
}
```
Only items whose `requiredPrivacyLevel <= user's level` are included. If level is `None`, nothing is sent.

**Daily timer:**
- `javax.swing.Timer` with 1-hour tick (following `JIPipeProjectBackupServiceComponent` pattern)
- On each tick: check `lastSentTimestamp` in `statistics.json`. If >24h ago (or never sent), trigger a send.
- Handles restarts correctly — no wasted sends.
- Send runs on a background thread (`SwingWorker` or `ExecutorService`), not EDT.
- Status bar messages: `"Sending usage statistics..."` -> `"Statistics sent."` or `"Failed to send statistics (will retry later)."`
- Network failures are silent (logged via SLF4J), no error dialogs.

**Immediate send on first consent:**
- When the user first interacts with the BalloonTip (clicks "Configure" -> OK, or "Dismiss", or close button):
  - If privacy level != None: generate machine ID (if not exists), record `firstLaunchTimestamp`, send immediately
  - Set `showFirstTimePrompt = false`

**Send flow:**
1. Build payload from registry items filtered by privacy level
2. POST via `java.net.http.HttpClient` (following `JIPipeAPIEmbeddingAIModelRunner` pattern)
3. On 2xx: update `lastSentTimestamp` in `statistics.json`, save
4. On failure: log, do not update timestamp (will retry next tick)

### 9. UI Components

#### 9.1 First-Time Status Bar Button: `JIPipeDesktopStatisticsButton`

- Added to the status bar **only** when `showFirstTimePrompt == true`
- Removed from the status bar after user dismisses or configures (set `showFirstTimePrompt = false`, save settings, remove component from status bar)
- Shows a `BalloonTip` after 2s delay (following `JIPipeDesktopAuthorProfileButton` pattern at `jipipe-core/.../desktop/app/components/JIPipeDesktopAuthorProfileButton.java`):
  - Uses `BalloonTip` from `net.java.balloontip` library
  - `EdgedBalloonStyle` with theme colors
  - Title: "Help us improve JIPipe"
  - Body: explains statistics collection for DFG KPI/NFDI4BIOIMAGE funding, user can opt out
  - Buttons: "Configure" (opens config dialog), "Dismiss" (uses default "Everything", sends immediately)
  - Close button (X): same as "Dismiss"
- Uses `UIUtils.invokeMuchLater(2000, ...)` for delayed show
- Uses `workbench.getProjectWindow().registerBalloon(balloonTip)` for balloon registration

#### 9.2 Configuration Dialog: `JIPipeDesktopStatisticsConfigurationUI`

- Custom `JDialog` (not `JIPipeDesktopFormPanel`)
- Modal, with escape key listener
- Layout inspired by `GettingStartedPanel` from jipipe-2:
  - Top: `UIUtils.createInfoLabel` with 64px icon explaining why statistics are collected (DFG KPI, NFDI4BIOIMAGE)
  - Middle: `JSlider` (0-4) with `JIPipeDesktopModernSliderUI`, snap-to-ticks, labels: None / Installation / Active / Rough / Everything
  - Below slider: dynamic description panel showing what's sent at the selected level (updates on slider change via `ChangeListener`)
  - Bottom: "OK" (saves level, sends immediately if != None, closes) and "Cancel" (no changes, closes)

#### 9.3 Tools > Statistics Menu Item: `ShowStatisticsTool`

- Extends `JIPipeDesktopMenuExtension`
- Target: `JIPipeMenuExtensionTarget.ProjectToolsMenu`
- Opens the statistics viewer dialog

#### 9.4 Statistics Viewer Dialog: `JIPipeDesktopStatisticsUI`

- Custom `JDialog`
- Top section: Machine ID (read-only) + "Re-roll" button, first launch date, last sent date, current privacy level
- Center: Statistics cards in a scrolling grid, grouped by category (Machine/Usage/Fun)
- Card design: `[ICON | [title]\n[content]]` — rounded border, icon on left, title + value on right (video-game style stat cards)
- Bottom: "Send now" button (manual trigger), "Close" button

### 10. Instrumentation Points

| Statistic | Hook Point | Approach |
|-----------|-----------|----------|
| Workflow runs | `JIPipeRunnableQueue` events | Subscribe to `getFinishedEventEmitter()` in service component `postprocess()`. Count only top-level `JIPipeGraphRun` (parent == null). |
| Recent projects count | `JIPipeRecentProjectsRegistry` | Passive item — read count on demand. |
| RO-Crates created | RO-Crate export code path | Increment counter on each RO-Crate creation. **Requires investigation** to find exact hook point. |
| Node move distance | Graph canvas node drag | Hook into node manager's drag-end event. Calculate Euclidean distance from pre-drag to post-drag position. Accumulate. **Requires investigation** to find exact drag event. |
| Longest node width | Node UI rendering | Track max width when node UI components are created/resized in the graph editor. **Requires investigation** to find rendering hook. |
| Top 5 popular nodes | Graph node-addition events | Subscribe to graph's node-added event emitter. Maintain `Map<nodeTypeId, count>`, keep top 5. |
| Largest project (nodes) | Project open | On project open, count nodes in the graph. Track max. |
| Largest project (compartments) | Project open | On project open, count compartments. Track max. |
| Noodle score | Edge pathing system | Hook into edge path calculation. Track min/avg/max path lengths. **Requires investigation** to find edge pathing code. |
| OS, RAM, GPU, version | System info | Passive items — read on demand via `System.getProperty()`, `OperatingSystemMXBean`, etc. |

**Instrumentation approach:** Each persistent item that needs event subscription implements an `initialize(JIPipeStatisticsServiceComponent)` method called during the service component's `postprocess()`, where it subscribes to relevant event emitters. Passive items compute on demand.

## File Structure

All new files under `jipipe-core/src/main/java/org/hkijena/jipipe/`:

```
plugins/statistics/
  StatisticsPlugin.java
  StatisticsPrivacyLevel.java
  JIPipeStatisticsItem.java
  JIPipeStatisticsItemCategory.java
  JIPipeStatisticsRegistry.java
  items/
    MachineIdStatisticsItem.java
    OperatingSystemStatisticsItem.java
    TotalRamStatisticsItem.java
    JIPipeVersionStatisticsItem.java
    GpuInfoStatisticsItem.java
    WorkflowRunsStatisticsItem.java
    RecentProjectsCountStatisticsItem.java
    RoCratesCreatedStatisticsItem.java
    NodeMoveDistanceStatisticsItem.java
    LongestNodeWidthStatisticsItem.java
    PopularNodesStatisticsItem.java
    LargestProjectNodesStatisticsItem.java
    LargestProjectCompartmentsStatisticsItem.java
    NoodleScoreStatisticsItem.java
  settings/
    JIPipeStatisticsApplicationSettings.java
  ui/
    JIPipeDesktopStatisticsButton.java
    JIPipeDesktopStatisticsConfigurationUI.java
    ShowStatisticsTool.java
    JIPipeDesktopStatisticsUI.java

api/service/components/
  JIPipeStatisticsServiceComponent.java
```

## Design Decisions

1. **Registry-based items** (not hardcoded): Chosen for extensibility. The future project statistics panel can query the same items. Follows JIPipe's existing pattern where everything is registered (nodes, data types, parameters, menu items).

2. **Service component for lifecycle**: Follows `JIPipeProjectBackupServiceComponent` pattern. Owns timer, storage, and event subscriptions. Accessible globally via `JIPipe.getService().getStatistics()`.

3. **Machine ID in statistics.json** (not settings): Avoids needing a custom parameter type and editor. Displayed in the statistics viewer dialog where we have full UI control.

4. **Temporary status bar button**: Only shown during first-time setup, removed after interaction. Not relevant enough for permanent placement.

5. **File locking + atomic writes**: `statistics.lock` file for `FileLock`, temp file + `Files.move(ATOMIC_MOVE)` for atomic replacement. Ensures correctness with multiple JIPipe instances.

6. **1-hour timer tick with 24h check**: More robust than a 24h timer. Handles restarts correctly — checks `lastSentTimestamp` on each tick.

7. **Deferred project statistics panel**: Designed the item registry to support it without code duplication. The panel can call `registry.getItemsByCategory()` and `item.serialize()` to display stats.

## Out of Scope

- Web service (separate Spring Boot project in `jipipe-statistics-server.git`)
- Project statistics overview panel (deferred — designed for but not implemented)
- Additional "fun" statistics beyond the v1 list (the system is extensible for future additions)
