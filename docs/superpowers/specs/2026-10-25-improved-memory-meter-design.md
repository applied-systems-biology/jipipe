# Design: Improved memory meter (forward-port from the AI LLM branch)

**Issue:** [#1343 "Improved memory meter"](https://asb-git.hki-jena.de/RGerst/jipipe/-/work_items/1343)
**Date:** 2026-10-25 · **Source branch:** `1290-ai-llm-integration` · **Target:** `master`
**Decision maker:** Ruman Gerst (approved recommendations during this conversation)

## Intent & why

The project workbench already shows a static memory meter
(`JIPipeDesktopMemoryStatusUI`): a fixed bar of current heap usage with an
options button (`JIPipeDesktopMemoryOptionsControl`) that can only run GC.

While building the AI LLM integration (#1290), a better memory meter was
implemented on the `1290-ai-llm-integration` branch: a live usage **graph**,
a background **monitoring service** that keeps a 100-point history, and a
full **Memory monitor tool window**. That work is well isolated from the rest
of the AI branch, so this change forward-ports *only* the memory meter to
`master`, without any of the unfinished AI/LLM code.

## Goals / non-goals

**In scope (this change):**
- A `JIPipeMemoryStatus` record (used/max memory + timestamp, % helpers).
- A `JIPipeMemoryMonitorServiceComponent`: thread-safe, samples JVM heap every
  1 s on a daemon executor, keeps a bounded 100-point history, **only active
  in GUI mode**, starts in `postprocess`, stops in `dispose`.
- A `JIPipeDesktopMemoryGraph` panel that paints the history as an area
  graph (primary color normally, warning color above 90 %), self-refreshing
  via its own repaint timer, showing "used/total" text.
- A `JIPipeDesktopMemoryManager` status-bar control: memory icon button +
  options menu ("Run garbage collector", "Open memory monitor") + the graph.
- A `JIPipeDesktopMemoryMonitorWindow` tool window (tabbed, TopPill style):
  an Overview page (graph + used/max/percentage + Run GC + Refresh), 5 s
  auto-refresh with toggle, and "Save report to text file".
- `StringUtils.formatBytes(long)` helper (MB as minimum unit, "1.5 GB" style).
- Service plumbing: `JIPipe.getMemoryMonitor()` accessor, component
  registration in `JIPipeService`, a `dispose()` loop over all service
  components (approved; also fixes missing disposal for all other components).
- Workbench rewiring: the old meter (status UI + options control) is
  **replaced** by the new manager; the two old classes are **deleted**
  (approved; they are dead after the rewire).

**Non-goals (explicitly NOT ported, all part of the AI/LLM feature):**
- `JIPipeAIModelStatusMonitor`, `JIPipeAIAgentServiceComponent`,
  `AgentProfile`, `JIPipeDesktopAIManager` (the AI status graph that the
  branch placed next to the meter — master keeps its existing
  `JIPipeDesktopAIStatusControl` in place).
- `JIPipeDesktopProjectWorkbench.startAIChatSession(...)` /
  `sendToAI(...)`, `SpeedTestUtils`, AI chat imports, AI icons,
  `WorkingMemory` / LLM calibration memory classes (different feature).
- `StringUtils.formatBinarySize` / `emptyToNull` (only used by LLM
  calibration UI).
- `oom-and-memory.md` knowledge source (AI agent resource).

## Constraints

- **No resource additions:** every icon the new files use
  (`devices/media-memory.png`, `actions/clear-brush.png`, `actions/preview.png`,
  `actions/document-preview.png`, `actions/quickopen.png`,
  `actions/view-refresh.png`, `actions/save.png`, `actions/system-run.png`)
  already exists on master in both themes at 16/32 px — verified.
- **No new dependencies:** all types/methods the 7 new files call
  (`ThemeUtils.getCurrentStyle()` color/font accessors,
  `UIUtils.makeButtonFlat25x25`/`createMenuItem`/`addReloadablePopupMenuToButton`/
  `getWindowTitle`/`getJIPipeIcon128`/`wrapInIslandPanelIfNeeded`/
  `createControlBorder`/`createButton`/`makeNonOpaque`/`repaintLater`,
  `JIPipeDesktopTabPane(true, Style.TopPill)` + `CloseMode.withoutCloseButton`
  + `setTabPanelBorder`, `JIPipeDesktopFormPanel` (WITH_SCROLLING,
  addWideToForm, addToForm, addGroupHeader), `JIPipeDesktop.saveFile`,
  `JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data`,
  `PathUtils.EXTENSION_FILTER_TXT`, `HTMLText.EMPTY`,
  `JIPipeDesktopWorkbench.getWindow()`/`getDesktopWorkbench()`,
  `JIPipeServiceComponent.postprocess`/`dispose`, `JIPipeServiceMode.GUI`)
  already exist on master — verified one by one.
- The new files are ported **verbatim** from the source branch (they contain
  no AI imports).
- **No new tests** (test policy: the component is a timer-driven sampler, the
  UI classes are Swing structure the user checks in the running app, and
  `JIPipeMemoryStatus`/`formatBytes` are simple pure functions whose behavior
  is re-stated by any test; deleting them would not catch a regression the
  user cares about).
- The pre-existing dirty files `AGENTS.md` and `utils/fresh-node-slots.json`
  are unrelated to this change and are **not** touched or committed.

## Approaches (considered)

| # | Approach | Trade-off | Decision |
|---|----------|-----------|----------|
| A | **Manual port**: recreate the 7 new files verbatim + apply the memory-only hunks to the 4 existing files on a fresh branch off master | Exact control; nothing AI-related can leak in; all dependencies verified first | **Chosen** |
| B | Cherry-pick / selective `git apply` of the branch's memory hunks | The branch interleaves memory and AI edits in the same files (workbench, JIPipeService); patches would carry AI context and need constant hand-cleaning | Rejected — more fragile than A for no gain |
| C | Merge/rebase the whole `1290-ai-llm-integration` branch | Brings the entire unfinished AI/LLM feature into master | Rejected — contradicts "well-isolated" in the issue |

## The how

Branch: `1343-improved-memory-meter` (off `master`).

1. **New files** (copied verbatim from the source branch):
   - `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeMemoryStatus.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeMemoryMonitorServiceComponent.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryGraph.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryManager.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorWindow.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorPage.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/memorymonitor/JIPipeDesktopMemoryMonitorOverviewPage.java`
2. **`JIPipe.java`**: add `getMemoryMonitor()` static accessor (only that;
   not the branch's AI accessors).
3. **`JIPipeService.java`**: `memoryMonitor` field + construction + entry in
   the `components` array (the default initializer already loops
   `postprocess` over it, so monitoring auto-starts in GUI mode) +
   `getMemoryMonitor()` getter + **approved addition**: at the end of
   `dispose()`, loop over all `components` calling `dispose()` with per-item
   try/catch (stops the sampler thread on shutdown).
4. **`StringUtils.java`**: add `formatBytes(long)` only (no
   `formatBinarySize`, no `emptyToNull`; no import changes — the branch's
   import removals in this file belong to other AI work).
5. **`JIPipeDesktopProjectWorkbench.java`**:
   - remove `memoryOptionsControl` field + its construction + the old
     "Memory control" status-bar block (options button + strut);
   - remove `import …JIPipeDesktopMemoryOptionsControl` and
     `import …JIPipeDesktopMemoryStatusUI`;
   - add `import …JIPipeDesktopMemoryManager`;
   - at the old "Memory meter" position (right of the server control,
     last in the bar) add `statusBar.add(new JIPipeDesktopMemoryManager(this));`.
     Master's existing AI status control (`JIPipeDesktopAIStatusControl`)
     stays exactly where it is.
6. **Delete** (dead after the rewire; the branch left them unused):
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryStatusUI.java`
   - `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/commons/components/tools/JIPipeDesktopMemoryOptionsControl.java`
   (their only references on master are the workbench lines removed in step 5;
   the old classes do not depend on the new service — no circular coupling.)
7. **Verify**: `mvn clean compile` (full build; the desktop package lives in
   jipipe-core), then `mvn test` (existing suite) — the only module with
   tests is `contrib/jipipe-ro-crate-java-2.1.0`; nothing pins the deleted
   classes (verified: no other references).

## What's left to the builder

- Nothing. If the compile step surfaces a signature mismatch that the
  pre-verification missed, fix it minimally and note it in the MR.
