# Improved Memory Meter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Forward-port the improved memory meter (live graph + monitoring service + monitor window) from `1290-ai-llm-integration` to master, isolated from all AI/LLM work.

**Architecture:** A new `JIPipeMemoryMonitorServiceComponent` samples JVM heap into a bounded 100-point history (GUI mode only); a new status-bar `JIPipeDesktopMemoryManager` renders that history as a live graph and opens a new `JIPipeDesktopMemoryMonitorWindow` tool window. The old two-class meter (`JIPipeDesktopMemoryStatusUI` + `JIPipeDesktopMemoryOptionsControl`) is replaced and deleted.

**Tech Stack:** Java 21, Swing, existing JIPipe service-component framework (`JIPipeServiceComponent` lifecycle: `postprocess` starts, `dispose` stops), Maven multi-module (`jipipe-core` holds both API and desktop packages).

**Spec:** `docs/superpowers/specs/2026-10-25-improved-memory-meter-design.md`

## Global Constraints

- Port the 7 new files **verbatim** from `1290-ai-llm-integration` (git ref locally fetched as `FETCH_HEAD`; the plan carries the exact `git show FETCH_HEAD:<path>` command per file). No AI/LLM code may enter this branch.
- `StringUtils`: add `formatBytes` only — **not** `formatBinarySize`, **not** `emptyToNull` (AI-only on the source branch). No import changes to that file.
- `JIPipeService`: only the `memoryMonitor` field/construction/array-entry/getter + the approved dispose loop. **No** `aiModelStatusMonitor`, `aiAgentService`, `agentProfile` (AI-only).
- `JIPipe.java`: only the `getMemoryMonitor()` static accessor. **No** AI accessors.
- Workbench: master's existing `JIPipeDesktopAIStatusControl` (inside `if(AIApplicationSettings.getInstance().isEnableAI())`) stays exactly as is; no new status-bar controls for AI.
- All meter icons already exist on master (verified in both themes at 16/32 px) — do not add or touch any `.png`/resource.
- No new tests (test policy: Swing structure is verified by eye in the running app; the sampler and `formatBytes` are covered by the existing suite's smoke compile).
- Registration IDs: none introduced (no new node/datatype/parameter types).
- The dirty files `AGENTS.md` and `utils/fresh-node-slots.json` are unrelated — never `git add` them; commit only the files named in each task.
- The spec (`docs/superpowers/specs/2026-10-25-improved-memory-meter-design.md`) and this plan are committed on this branch in a `docs:` commit at the end of the run.
- Commit only what a task names; each commit must leave the tree compiling.

## Review Focus

The spec's inputs/failure modes no task's tests exercise, most likely to bite a user, with the task that owns the check:

1. **Sample history rendered when graph is wider/taller than 100 samples' x-step** — 100 points spread with `xStep = graphWidth/(RESOLUTION-1)`; a resize must not NPE or divide by zero. → Task 2 compile gate + user visual check (no automated test, per policy).
2. **History is empty (monitor never started, e.g. headless CLI mode) when the graph paints** — `paintComponent` returns early on `isEmpty()`; `postprocess` pre-fills 100 placeholders so GUI mode always has history. → Task 2 (verbatim file) + Task 5 dispose-loop keeps `postprocess`-started executor from leaking.
3. **`maxMemory` = 0** — `getUsagePercentage()` guards with `maxMemory > 0`, graph divides by `maxMemory` only inside history whose values came from `Runtime` (always > 0); record's `isWarning` is unused by the ported files but must not throw. → Task 1 verbatim file.
4. **App shutdown with monitoring active** — `dispose()` loop must stop the daemon sampler and not hang (5 s timeout + `shutdownNow`). → Task 5.
5. **Old meter deleted while something still references it** — only the workbench referenced the two old classes (verified); a full-project `mvn compile` after deletion is the gate. → Task 6.

---

### Task 1: Port the memory service (record + monitor component)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java`

**Interfaces:**
- Consumes: `JIPipeService` (master), `JIPipeServiceComponent` lifecycle `postprocess(JIPipeProgressInfo)` / `dispose()` (master), `JIPipeServiceMode.GUI` (master).
- Produces: `JIPipeMemoryStatus(long usedMemory, long maxMemory, long timestamp)` record with `getUsagePercentage()` and `isWarning(double)`; `JIPipeMemoryMonitorServiceComponent(JIPipeService)` with `getCurrentStatus()`, `getHistory()` (List<JIPipeMemoryStatus>, oldest→newest), `startMonitoring()`, `stopMonitoring()`, `isMonitoringActive()`.

- [ ] **Step 1: Create the two files verbatim from the source branch**

```bash
cd /workspace
mkdir -p jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java > jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java > jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java
```

Verify byte-identity with the branch (guards against a mangled redirect):

```bash
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java | diff - jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java && echo IDENTICAL
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java | diff - jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java && echo IDENTICAL
```

Expected: two lines `IDENTICAL` (empty diffs). Repeat this pattern for every verbatim file in Tasks 2 and 3.

- [ ] **Step 2: Register the component in `JIPipeService` (construction + components array + getter)**

File: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java`

Three edits (anchor on existing lines, do not reorder other entries):
1. Field: after `private final JIPipeProjectBackupServiceComponent projectBackup;` add
   `private final JIPipeMemoryMonitorServiceComponent memoryMonitor;`
2. Constructor: after `projectBackup = new JIPipeProjectBackupServiceComponent(this);` add
   `memoryMonitor = new JIPipeMemoryMonitorServiceComponent(this);` — and in the `components` array insert `memoryMonitor` between `projectBackup` and `aiService`.
3. Getter: after the `getProjectBackup()` method add

```java
    public JIPipeMemoryMonitorServiceComponent getMemoryMonitor() {
        return memoryMonitor;
    }
```

The default initializer already loops `postprocess` over the array — this alone starts monitoring in GUI mode.

- [ ] **Step 3: Add `JIPipe.getMemoryMonitor()` accessor**

File: `jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java` (imports `org.hkijena.jipipe.api.service.components.*` already present)

Insert after the `getEnvironments()` static method (before `getInstrumentation()`):

```java
    /**
     * @return The memory monitor service component
     */
    public static JIPipeMemoryMonitorServiceComponent getMemoryMonitor() {
        return getInstance().getMemoryMonitor();
    }
```

- [ ] **Step 4: Compile gate**

```bash
mvn -q clean compile
```
Expected: BUILD SUCCESS (service registration + accessor compile against the new classes; nothing consumes them yet).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/JIPipe.java
git commit -m "feat: add memory monitor service component (#1343)"
```

---

### Task 2: Port the graph + manager (status-bar UI)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryGraph.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryManager.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/StringUtils.java`

**Interfaces:**
- Consumes: `JIPipe.getMemoryMonitor().getHistory()` (Task 1), `ThemeUtils.getCurrentStyle()` color/font getters (master), `UIUtils` helpers (master), icons at `devices/media-memory.png`, `actions/clear-brush.png`, `actions/preview.png` (master, both themes).
- Produces: `JIPipeDesktopMemoryGraph(int fontSize, boolean withMaxCache)` with `startRepaintTimer()`/`stopRepaintTimer()`; `JIPipeDesktopMemoryManager(JIPipeDesktopProjectWorkbench)` (a `JIPipeDesktopProjectWorkbenchPanel`).

- [ ] **Step 1: Create the two files verbatim from the source branch**

```bash
cd /workspace
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryGraph.java > jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryGraph.java
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryManager.java > jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryManager.java
```

- [ ] **Step 2: Add `StringUtils.formatBytes(long)`**

File: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/StringUtils.java` (`java.util.Locale` import already present on master — no import change)

Insert immediately before `public static String formatSize(long size) {`:

```java
    /**
     * Formats bytes into a compact string representation with MB as the minimum unit.
     * Rounds up decimal values as requested.
     *
     * @param bytes The number of bytes to format
     * @return Formatted string like "1 MB", "1.5 GB", "0.5 MB", etc.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "0 MB";
        }

        // Convert MB to first value to check
        double value = bytes / (1024.0 * 1024.0);
        String unit = "MB";

        if (value >= 1024.0) {
            value /= 1024.0;
            unit = "GB";
            if (value >= 1024.0) {
                value /= 1024.0;
                unit = "TB";
                if (value >= 1024.0) {
                    value /= 1024.0;
                    unit = "PB";
                }
            }
        }

        // Round up and format with appropriate decimal places
        if (unit.equals("MB")) {
            double rounded = Math.ceil(value * 100) / 100;
            return String.format(Locale.ROOT, "%.2f %s", rounded, unit);
        } else {
            double rounded = Math.ceil(value * 10) / 10;
            return String.format(Locale.ROOT, "%.1f %s", rounded, unit);
        }
    }
```

- [ ] **Step 3: Compile gate**

```bash
mvn -q clean compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryGraph.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryManager.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/utils/StringUtils.java
git commit -m "feat: add memory graph and status-bar memory manager (#1343)"
```

---

### Task 3: Port the memory monitor window (tool window + overview page)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorWindow.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorPage.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorOverviewPage.java`

**Interfaces:**
- Consumes: `JIPipeDesktopMemoryGraph(int, boolean)` + `startRepaintTimer()` (Task 2), `JIPipe.getMemoryMonitor().getCurrentStatus()` (Task 1), `JIPipeDesktopTabPane(true, JIPipeDesktopTabPane.Style.TopPill)` + `CloseMode.withoutCloseButton` + `setTabPanelBorder` (master), `JIPipeDesktopFormPanel` (master), `JIPipeDesktop.saveFile` (master), `JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data` + `PathUtils.EXTENSION_FILTER_TXT` + `HTMLText.EMPTY` (master), icons `actions/document-preview.png`, `actions/quickopen.png`, `actions/view-refresh.png`, `actions/save.png`, `actions/system-run.png`, `actions/clear-brush.png` (master, both themes).
- Produces: `JIPipeDesktopMemoryMonitorWindow(JIPipeDesktopWorkbench)` with `refresh()` and `generateReport()`; abstract `JIPipeDesktopMemoryMonitorPage` with `refresh()` + `addToReport(StringBuilder)`.

- [ ] **Step 1: Create the three files verbatim from the source branch**

```bash
cd /workspace
mkdir -p jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorWindow.java > jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorWindow.java
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorPage.java > jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorPage.java
git show FETCH_HEAD:jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorOverviewPage.java > jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorOverviewPage.java
```

- [ ] **Step 2: Compile gate**

```bash
mvn -q clean compile
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/
git commit -m "feat: add memory monitor window (#1343)"
```

---

### Task 4: Rewire the workbench status bar

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java`

**Interfaces:**
- Consumes: `JIPipeDesktopMemoryManager(JIPipeDesktopProjectWorkbench)` (Task 2).
- Produces: status bar's last control is the new memory manager (replacing `JIPipeDesktopMemoryStatusUI` and the options-button block).

- [ ] **Step 1: Remove the old wiring and add the new**

In `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java`:

1. Delete field `private final JIPipeDesktopMemoryOptionsControl memoryOptionsControl;` (line ~134).
2. Delete constructor line `this.memoryOptionsControl = new JIPipeDesktopMemoryOptionsControl(this);` (line ~167).
3. Delete the status-bar block (lines ~590–595):

```java
        // Memory control
        JButton optionsButton = memoryOptionsControl.createOptionsButton();
        UIUtils.makeButtonFlat(optionsButton);
        statusBar.add(optionsButton);
        statusBar.add(Box.createHorizontalStrut(4));
```

4. Replace the line `statusBar.add(new JIPipeDesktopMemoryStatusUI());` (with its `// Memory meter` comment) with:

```java
        // Memory meter
        statusBar.add(new JIPipeDesktopMemoryManager(this));
```

5. Replace the two imports (lines ~83–84):

```java
import org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopMemoryOptionsControl;
import org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopMemoryStatusUI;
```

with:

```java
import org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopMemoryManager;
```

(One import replaces two; the AI control block above stays untouched.)

- [ ] **Step 2: Compile gate**

```bash
mvn -q clean compile
```
Expected: BUILD SUCCESS (the two old classes remain but are now unreferenced; they compile standalone).

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java
git commit -m "feat: replace old memory meter with new memory manager in status bar (#1343)"
```

---

### Task 5: Lifecycle cleanup — dispose loop in `JIPipeService`

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java`

**Interfaces:**
- Consumes: `JIPipeServiceComponent.dispose()` (master, no-op default — the monitor overrides it to `stopMonitoring()`).
- Produces: every registered service component is disposed on `JIPipeService.dispose()`.

- [ ] **Step 1: Add the dispose loop at the end of `JIPipeService.dispose()`**

Insert the loop **after** the plugin-unload loop (plugins may hold service references until then) and **before** `dispose()`'s own closing brace. The `components` array (set in the constructor) is in scope.

Master's `dispose()` currently ends:

```java
        // Unload all plugins
        for (String activatedPluginId : getPlugins().getActivatedPlugins()) {
            logService.log(LogLevel.INFO, "Unloading plugin " + activatedPluginId);
            try {
                JIPipePlugin plugin = getPlugins().getKnownPluginById(activatedPluginId);
                plugin.dispose();
            } catch (Throwable e) {
                getLogService().log(LogLevel.ERROR, "Failed to unload plugin " + activatedPluginId, e);
            }
        }
    }
```

becomes:

```java
        // Unload all plugins
        for (String activatedPluginId : getPlugins().getActivatedPlugins()) {
            logService.log(LogLevel.INFO, "Unloading plugin " + activatedPluginId);
            try {
                JIPipePlugin plugin = getPlugins().getKnownPluginById(activatedPluginId);
                plugin.dispose();
            } catch (Throwable e) {
                getLogService().log(LogLevel.ERROR, "Failed to unload plugin " + activatedPluginId, e);
            }
        }

        // Dispose all services
        for (JIPipeServiceComponent component : components) {
            try {
                component.dispose();
            } catch (Throwable e) {
                getLogService().log(LogLevel.ERROR, "Failed to dispose service component", e);
            }
        }
    }
```

- [ ] **Step 2: Compile gate**

```bash
mvn -q clean compile
```
Expected: BUILD SUCCESS.

- [ ] dispose loop placement note: the loop sits **after** plugin unload (plugins may hold service references until then) and **before** the method ends; `components` (the array set in the constructor) is in scope.

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java
git commit -m "feat: dispose all service components on shutdown (#1343)"
```

---

### Task 6: Delete the old meter classes + final verification

**Files:**
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryStatusUI.java`
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryOptionsControl.java`

**Interfaces:**
- Consumes: Task 4's rewiring (the old classes' only references are gone).
- Produces: nothing (dead-code removal).

- [ ] **Step 1: Delete the files**

```bash
cd /workspace
git rm jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryStatusUI.java \
       jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryOptionsControl.java
```

- [ ] **Step 2: Grep for stragglers**

```bash
grep -rn "JIPipeDesktopMemoryStatusUI\|JIPipeDesktopMemoryOptionsControl" --include="*.java" jipipe-core/src/ jipipe-desktop/ jipipe-cli/ jipipe-launcher/ 2>/dev/null || echo "no references"
```
Expected: `no references` (the only references were the workbench lines removed in Task 4; verified during design).

- [ ] **Step 3: Full build + existing test suite**

```bash
mvn -q clean compile && mvn -q test
```
Expected: BUILD SUCCESS; existing suite (contrib module) green. (No new tests, per the test policy: no Swing interaction tests — the user verifies the meter by eye in the running app.)

- [ ] **Step 4: Commit**

```bash
git commit -m "refactor: remove old memory meter classes (#1343)"
```

(`git rm` already staged the deletion.)

- [ ] **Step 5: User visual verification prompt**

Ask the human partner to run the app (`JIPipeDesktopMain`) and check the meter by eye (live graph in the status bar, "Open memory monitor" tool window, Run GC menu item); per the design, the look is verified in the running app. Report the branch name `1343-improved-memory-meter` and summarize the commits.
