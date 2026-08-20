# Statistics Persistence & Popular Nodes Fix Design

**Work item:** #1313  
**Branch:** `1313-statistics-persistence-and-popular-nodes` (off `master`)  
**Date:** 2026-08-20  

## Overview

Fix multiple bugs in the JIPipe statistics collection that cause the "Top 10 Popular Nodes" dashboard chart to be permanently empty on the production statistics server, and cause all persistent statistics items to lose their state on restart.

## Motivation

The production dashboard at `https://jipipe.hki-jena.de/statistics/dashboard` shows an empty "Top 10 Popular Nodes" chart, while other statistics (workflow runs, installations, etc.) work correctly. Root cause investigation revealed multiple bugs in jipipe's statistics collection.

### Evidence

- `statistics.json` on a test machine has `"items": {}` despite 15 days of usage
- `popular-nodes` history: 90 entries, ALL empty `{}`
- `workflow-runs` history: 1 non-zero entry (value=2), then 0 (lost on restart)
- Production API: `popularNodes: []`, `totalWorkflowRuns: 178`

## Audit Results

A full audit of the statistics code found 11 bugs. Fixes 1-2 are critical (directly cause the empty chart). Fixes 3-5 are major (data loss or memory leaks). Fixes 6-11 are minor.

## Fix 1: Serialize items in `save()` (CRITICAL)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`

**Problem:** The `save()` method reads `statisticsData.get("items")` (the items from the initial disk load) and merges it into the on-disk data. It never calls `serialize()` on the in-memory items, so the current state is lost. All persistent items (`WorkflowRuns`, `RoCratesCreated`, `PopularNodes`, `LargestProjectNodes`, `LargestProjectCompartments`, `NodeMoveDistance`, `LongestNodeWidth`) lose their state on restart.

**Fix:** Before merging, update `statisticsData.items` with the current serialized state of all registered items:

```java
ObjectNode ourItems = statisticsData.has("items")
        ? (ObjectNode) statisticsData.get("items")
        : statisticsData.putObject("items");
for (var item : registry.getItems()) {
    JsonNode itemData = item.serialize();
    if (itemData != null) {
        ourItems.set(item.getId(), itemData);
    }
}
```

## Fix 2: Count existing nodes in `attachToWindow()` (CRITICAL)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/PopularNodesStatisticsItem.java`

**Problem:** `WINDOW_OPENED_EVENT_EMITTER.emit()` fires **before** `loadProject()`. `onWindowOpened()` defers `attachToWindow()` via `SwingUtilities.invokeLater()`, so by the time it runs, `loadProject()` has already loaded all nodes. The `nodeAddedEventEmitter` subscription misses all nodes from project files.

**Fix:** In `attachToWindow()`, after subscribing to `nodeAddedEventEmitter`, count all existing nodes in the graph:

```java
private void attachToWindow(JIPipeDesktopProjectWindow window) {
    if (!attachedWindows.add(window)) {
        return;
    }
    JIPipeProject project = window.getProject();
    if (project != null) {
        JIPipeGraph graph = project.getGraph();
        graph.getNodeAddedEventEmitter().subscribe(this::onNodeAdded);
        for (JIPipeGraphNode node : graph.getGraphNodes()) {
            countNode(node);
        }
    }
}
```

## Fix 3: Track project replacement in existing window (MAJOR)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/PopularNodesStatisticsItem.java`

**Problem:** `openProjectInThisOrNewWindow()` can load a new project into an existing window without firing `WINDOW_OPENED_EVENT_EMITTER`. Even if it did fire, `attachedWindows` already contains the window, so `attachToWindow()` would return early. The new project's graph is never subscribed to, and its nodes are never counted.

**Fix:** Subscribe to `WINDOW_CLOSED_EVENT_EMITTER` to remove windows from `attachedWindows`, allowing re-attachment when a new project is loaded. Alternatively, subscribe to a project-change event on the window.

## Fix 4: Clean up `attachedWindows` on window close (MAJOR)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/PopularNodesStatisticsItem.java`

**Problem:** The `attachedWindows` set accumulates `JIPipeDesktopProjectWindow` references but never removes them. Each closed window (and its entire project, graph, and all data) is retained in memory forever.

**Fix:** Subscribe to `WINDOW_CLOSED_EVENT_EMITTER` and remove closed windows:

```java
JIPipeDesktopProjectWindow.WINDOW_CLOSED_EVENT_EMITTER.subscribe(this::onWindowClosed);

private void onWindowClosed(WindowClosedEvent event) {
    if (event.getWindow() instanceof JIPipeDesktopProjectWindow window) {
        attachedWindows.remove(window);
    }
}
```

## Fix 5: Final save on application exit (MAJOR)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`

**Problem:** `JIPipeService.dispose()` does not call `statistics.save()` before shutting down. The `saveLaterTimer` has a 250ms debounce — if the application exits within 250ms of the last `saveLater()` call, pending changes are lost.

**Fix:** Add a `shutdown()` method that stops timers and performs a final save. Call it from `JIPipeService.dispose()`.

## Fix 6: Stop `reportingTimer` on dispose (MINOR)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`

**Problem:** The `reportingTimer` is created and started in `postprocess()` but never stopped.

**Fix:** Stop in `shutdown()` (part of Fix 5).

## Fix 7: Handle corrupted statistics file in `save()` (MINOR)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`

**Problem:** If the statistics file is corrupted, `(ObjectNode) mapper.readTree(file.toFile())` throws. The save is skipped, and the corrupted file remains. Every subsequent `save()` fails.

**Fix:** Wrap the read in a try-catch and fall back to a new `ObjectNode`:

```java
ObjectNode currentOnDisk;
if (Files.isRegularFile(file)) {
    try {
        currentOnDisk = (ObjectNode) mapper.readTree(file.toFile());
    } catch (Exception e) {
        logger.warn("Statistics file corrupted, creating new", e);
        currentOnDisk = mapper.createObjectNode();
    }
} else {
    currentOnDisk = mapper.createObjectNode();
}
```

## Fix 8: `LargestProjectNodes/Compartments` don't update after window open (MINOR)

**File:** `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LargestProjectNodesStatisticsItem.java`, `LargestProjectCompartmentsStatisticsItem.java`

**Problem:** These items only call `updateMax(window)` when a window opens. They don't subscribe to `NodeAddedEventEmitter`. If the user adds more nodes/compartments after opening, the max is never updated.

**Fix:** If the intent is to track the true maximum, subscribe to `NodeAddedEventEmitter` and update the max when nodes are added. If the intent is to sample at open time, document this.

## Out of scope

The following bugs were found but are out of scope for this work item:

- **`JIPipeEventEmitter.unsubscribe()` is broken** — the `listenerSubscriberMap` is never populated, making `unsubscribe()` a no-op. This affects the entire codebase, not just statistics. Should be fixed in a separate work item.
- **`TotalRamStatisticsItem` marked as time-tracked but value never changes** — wastes storage with redundant history entries. Design decision, not a bug.
- **Static accumulators in `NodeMoveDistanceStatisticsItem` and `LongestNodeWidthStatisticsItem` lost on crash** — consequence of Fix 1. Fixed once `save()` calls `serialize()`.

## Testing

- Unit test: `save()` should persist item state to disk; after reload, `deserialize()` should restore the state
- Unit test: `attachToWindow()` should count existing graph nodes, not just future additions
- Integration test: After restart, `popular-nodes` should retain previous counts
- Unit test: Corrupted statistics file should not prevent saving

## Impact

- All persistent statistics items will survive restarts (Fix 1)
- `PopularNodesStatisticsItem` will count nodes from opened projects (Fix 2)
- Project replacement in existing windows will be tracked (Fix 3)
- No memory leak from closed windows (Fix 4)
- No data loss on application exit (Fix 5)
- The "Top 10 Popular Nodes" chart will show data once users send updated statistics
