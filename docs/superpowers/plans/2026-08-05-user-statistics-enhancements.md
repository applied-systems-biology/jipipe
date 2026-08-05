# User Statistics Enhancement Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Enhance the statistics feature with a tab-based UI, time-tracked graphs, proper hardware detection, and improved design.

**Base branch:** `1304-user-statistics` (21 commits ahead of master)

**Spec:** `docs/superpowers/specs/2026-08-04-user-statistics-design.md`

## Global Constraints

- Java 21, Swing, Maven
- JFreeChart 1.5.0 already in jipipe-core pom.xml
- Follow project overview tips design pattern (`JIPipeDesktopProjectOverviewUI.addToTipsPanel()`)
- Use lazy singleton tab pattern (`documentTabPane.registerSingletonTab()`)
- Port `HardwareDetector` from `/data/src/jipipe-2/jipipe-core/src/main/java/org/hkijena/jipipe/servers/llmproxy/server/HardwareDetector.java`
- No category labels — all statistics are uncategorized with neutral language
- Daily samples, 90 data points max for time-tracked stats

---

### Task 20: Port HardwareDetector and Replace GPU Info Items

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/HardwareDetector.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/system/SystemResources.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/GpuInfoStatisticsItem.java` → replace with `GpuModelStatisticsItem.java` and `GpuVramStatisticsItem.java` and `AccelerationStatisticsItem.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java` — update item registration

**Interfaces:**
- Consumes: `CUDAUtils` (already in jipipe-3), `JIPipeHardwareAccelerationApplicationSettings`
- Produces: `HardwareDetector.detect()` returns `SystemResources` with GPU model, VRAM total/used, acceleration mode, CUDA version, CPU cores, system RAM
- Produces: 3 new items replacing `GpuInfoStatisticsItem`: `GpuModelStatisticsItem` (GPU name), `GpuVramStatisticsItem` (VRAM total MB), `AccelerationStatisticsItem` (acceleration mode + CUDA version)

**Key reference:**
- jipipe-2 `HardwareDetector.java`: `/data/src/jipipe-2/jipipe-core/src/main/java/org/hkijena/jipipe/servers/llmproxy/server/HardwareDetector.java`
- jipipe-2 `SystemResources.java`: `/data/src/jipipe-2/jipipe-core/src/main/java/org/hkijena/jipipe/servers/llmproxy/shared/SystemResources.java`
- jipipe-3 `CUDAUtils.java`: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/CUDAUtils.java`
- jipipe-3 `JIPipeHardwareAccelerationApplicationSettings`: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/application/JIPipeHardwareAccelerationApplicationSettings.java`

- [ ] **Step 1: Port `SystemResources.java`** — Copy from jipipe-2, adapt package to `org.hkijena.jipipe.api.system`, keep Jackson annotations

- [ ] **Step 2: Port `HardwareDetector.java`** — Copy from jipipe-2, adapt package to `org.hkijena.jipipe.utils`, ensure `CUDAUtils` import path is correct for jipipe-3

- [ ] **Step 3: Delete `GpuInfoStatisticsItem.java`** and create 3 replacement items:
  - `GpuModelStatisticsItem` — returns GPU name string from `HardwareDetector.detect().getGpuType()`, privacy level `Everything`
  - `GpuVramStatisticsItem` — returns VRAM total in MB from `HardwareDetector.detect().getGpuVramTotalMB()`, privacy level `Everything`
  - `AccelerationStatisticsItem` — returns acceleration mode + CUDA version string from `HardwareDetector.detect()`, privacy level `Installation`

- [ ] **Step 4: Update `StatisticsPlugin.java`** — Remove `GpuInfoStatisticsItem` registration, add the 3 new items

- [ ] **Step 5: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "Port HardwareDetector and replace GPU info with detailed hardware items (#1304)"
```

---

### Task 21: Remove Categories, Use Neutral Language

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItemCategory.java` — delete or deprecate
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java` — remove `getCategory()` from interface
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistry.java` — remove `getItemsByCategory()`
- Modify: All item classes in `plugins/statistics/items/` — remove `getCategory()` implementations
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPrivacyLevel.java` — update descriptions to neutral language (remove "fun" etc.)
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistryTest.java` — remove category-related tests
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java` — remove category grouping

**Changes:**
- Remove `JIPipeStatisticsItemCategory` enum entirely
- Remove `getCategory()` from `JIPipeStatisticsItem` interface
- Remove `getItemsByCategory()` from registry
- Remove all `getCategory()` implementations from item classes
- Update `StatisticsPrivacyLevel.Everything` description from "All statistics including fun items" to "All available statistics"
- Update any UI code that groups by category

- [ ] **Step 1: Remove category from interface, registry, items, tests**
- [ ] **Step 2: Update privacy level descriptions to neutral language**
- [ ] **Step 3: Compile and test**

Run: `mvn test -pl jipipe-core -q`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "Remove statistics categories, use neutral language (#1304)"
```

---

### Task 22: Add Time-Tracking Infrastructure

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeTimeTrackedStatisticsItem.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java` — add `default boolean isTimeTracked() { return false; }` and `default JsonNode getHistory() { return null; }` and `default void setHistory(JsonNode history) {}`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java` — add history storage and daily sampling
- Modify: All numeric persistent items (WorkflowRuns, RoCratesCreated, NodeMoveDistance, LongestNodeWidth, PopularNodes, LargestProjectNodes, LargestProjectCompartments, NoodleScore, TotalRam) — implement `isTimeTracked()` returning true

**Design:**
- `statistics.json` gets a new `"history"` section:
```json
{
  "history": {
    "workflow-runs": [
      {"timestamp": "2024-01-15T10:30:00", "value": 42},
      {"timestamp": "2024-01-16T10:30:00", "value": 45}
    ]
  }
}
```
- On each daily send (in `checkAndSend()` or after successful send), sample all time-tracked items by calling `serialize()` and appending to history
- History is capped at 90 data points (oldest pruned)
- Items that are time-tracked implement `isTimeTracked() { return true; }`
- The service component manages history storage/sampling

- [ ] **Step 1: Add time-tracking methods to `JIPipeStatisticsItem` interface**

Add default methods:
```java
default boolean isTimeTracked() { return false; }
```

- [ ] **Step 2: Add history storage to `JIPipeStatisticsServiceComponent`**

Add methods:
```java
public ObjectNode getHistory() { ... }  // returns the history ObjectNode from statisticsData
public void sampleHistory() { ... }     // samples all time-tracked items, appends to history, prunes to 90
```

Call `sampleHistory()` after successful send in `checkAndSend()`.

- [ ] **Step 3: Mark numeric items as time-tracked**

Add `@Override public boolean isTimeTracked() { return true; }` to:
- `WorkflowRunsStatisticsItem`, `RoCratesCreatedStatisticsItem`, `NodeMoveDistanceStatisticsItem`
- `LongestNodeWidthStatisticsItem`, `LargestProjectNodesStatisticsItem`, `LargestProjectCompartmentsStatisticsItem`
- `TotalRamStatisticsItem`, `NoodleScoreStatisticsItem`
- `PopularNodesStatisticsItem` (track count of total nodes added)

- [ ] **Step 4: Compile and test**

Run: `mvn test -pl jipipe-core -q`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "Add time-tracking infrastructure for statistics (#1304)"
```

---

### Task 23: Convert Statistics Viewer to Tab with Redesigned UI

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java` — convert from `JDialog` to `JIPipeDesktopProjectWorkbenchPanel`, redesign with project overview tips pattern
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/ShowStatisticsTool.java` — open tab instead of dialog
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java` — register statistics singleton tab

**Design:**
- `JIPipeDesktopStatisticsUI` extends `JIPipeDesktopProjectWorkbenchPanel` (like `JIPipeDesktopProjectOverviewUI`)
- Registered as singleton tab: `documentTabPane.registerSingletonTab("STATISTICS", "Statistics", icon, () -> new JIPipeDesktopStatisticsUI(this), SingletonTabMode.Hidden)`
- `ShowStatisticsTool` calls `workbench.getDocumentTabPane().selectSingletonTab("STATISTICS")` instead of opening dialog
- Add `TAB_STATISTICS = "STATISTICS"` constant to `JIPipeDesktopProjectWorkbench`

**UI Layout (following project overview pattern):**
- Top: Header panel with title "Statistics", machine ID, first launch date, last sent date, privacy level — styled like the overview header
- Center: Scrolling panel of cards using `RoundedLineBorder` (like `addToTipsPanel`)
- Each card: `[ICON | [bold title]\n[current value]]` with a small JFreeChart `ChartPanel` below for time-tracked items
- Bottom toolbar: "Refresh" button (no send button)
- Cards are NOT grouped by category — just listed in a grid

**Card design:**
```java
private JPanel createCard(JIPipeStatisticsItem item) {
    JPanel card = new JPanel(new BorderLayout(8, 8));
    card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 0, 8, 8),
            new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 4)
    ));
    
    // Title with icon
    JLabel titleLabel = new JLabel(item.getName());
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
    card.add(titleLabel, BorderLayout.NORTH);
    
    // Current value
    JLabel valueLabel = new JLabel(formatValue(item.serialize()));
    card.add(valueLabel, BorderLayout.CENTER);
    
    // Chart for time-tracked items
    if (item.isTimeTracked()) {
        ChartPanel chartPanel = createHistoryChart(item);
        card.add(chartPanel, BorderLayout.SOUTH);
    }
    
    card.setPreferredSize(new Dimension(300, item.isTimeTracked() ? 200 : 100));
    return card;
}
```

**Chart creation:**
```java
private ChartPanel createHistoryChart(JIPipeStatisticsItem item) {
    // Get history from service component
    JsonNode history = service.getHistory().get(item.getId());
    if (history == null || !history.isArray() || history.isEmpty()) {
        return new ChartPanel(null);  // empty
    }
    
    XYSeries series = new XYSeries(item.getName());
    for (int i = 0; i < history.size(); i++) {
        JsonNode point = history.get(i);
        series.add(i, point.get("value").asDouble());
    }
    
    XYSeriesCollection dataset = new XYSeriesCollection(series);
    JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, dataset);
    chart.removeLegend();
    chart.getXYPlot().getRenderer().setDefaultStroke(new BasicStroke(2f));
    // Style: minimal, no axes labels, small
    
    ChartPanel panel = new ChartPanel(chart);
    panel.setPreferredSize(new Dimension(280, 100));
    panel.setMinimumDrawWidth(0);
    panel.setMaximumDrawWidth(Integer.MAX_VALUE);
    panel.setMinimumDrawHeight(0);
    panel.setMaximumDrawHeight(Integer.MAX_VALUE);
    return panel;
}
```

- [ ] **Step 1: Convert `JIPipeDesktopStatisticsUI` to `JIPipeDesktopProjectWorkbenchPanel`**

Change class declaration, remove dialog-specific code, add `refresh()` method.

- [ ] **Step 2: Redesign card layout with `RoundedLineBorder` and project overview pattern**

- [ ] **Step 3: Add JFreeChart chart panels for time-tracked items**

- [ ] **Step 4: Add "Refresh" button, remove "Send now" button**

- [ ] **Step 5: Register singleton tab in `JIPipeDesktopProjectWorkbench`**

Add `TAB_STATISTICS` constant and `registerSingletonTab` call.

- [ ] **Step 6: Update `ShowStatisticsTool` to open tab**

- [ ] **Step 7: Compile and verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "Convert statistics viewer to tab with redesigned UI and charts (#1304)"
```

---

### Task 24: Update Configuration Dialog (Remove Category References)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsConfigurationUI.java` — update description text to neutral language
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsButton.java` — update balloon text to neutral language

- [ ] **Step 1: Update all user-facing text to remove "fun" and category references**
- [ ] **Step 2: Compile and verify**
- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "Update statistics UI text to neutral language (#1304)"
```

---

### Task 25: Final Verification

- [ ] **Step 1: Full compilation**

Run: `mvn clean compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**

Run: `mvn test -pl jipipe-core -q`
Expected: All tests pass

- [ ] **Step 3: Verify tab registration**

Run: `rg "TAB_STATISTICS" jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java`
Expected: Match

- [ ] **Step 4: Verify no category references remain**

Run: `rg "getCategory\|ItemCategory\|Fun\|Machine\|Usage" jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/`
Expected: No matches in statistics code (may exist elsewhere)

- [ ] **Step 5: Verify HardwareDetector ported**

Run: `rg "class HardwareDetector" jipipe-core/src/main/java/org/hkijena/jipipe/utils/HardwareDetector.java`
Expected: Match

- [ ] **Step 6: Final commit if needed**
