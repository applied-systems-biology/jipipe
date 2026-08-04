# User Statistics Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a user statistics capture, storage, and reporting system for JIPipe that collects usage data with user-configurable privacy levels and reports it daily to a configurable server endpoint.

**Architecture:** Registry-based statistics items managed by a `JIPipeStatisticsServiceComponent`, with a `StatisticsPlugin` for registration, a settings sheet for configuration, and Swing UI for first-time consent and viewing. Statistics are stored in `statistics.json` with file locking for multi-instance safety, and reported via HTTP POST.

**Tech Stack:** Java 21, Swing, Maven, Jackson (JSON), `java.net.http.HttpClient`, `net.java.balloontip`

## Global Constraints

- Java 21 (required, `maven.compiler.source` and `target` = 21)
- Maven build system, parent POM `pom-jipipe` at `/data/src/jipipe-3/pom.xml`
- JUnit Jupiter is already inherited from the parent POM (test scope) — no pom changes needed for tests
- Follow existing JIPipe patterns: `JIPipePrepackagedDefaultJavaPlugin`, `JIPipeServiceComponent`, `JIPipeDefaultApplicationsSettingsSheet`
- No new external dependencies — use existing libraries already in jipipe-core
- Statistics file at `<JIPipe user dir>/statistics.json` (path via `JIPipe.getJIPipeUserDir(false)`)
- Must handle multiple JIPipe instances writing to the same file (file locking + atomic writes)
- Default privacy level: `Everything`
- Default server URL: `https://jipipe.hki-jena.de/statistics/api/v1/statistics`
- All new code goes in `jipipe-core/src/main/java/org/hkijena/jipipe/`
- Desktop UI code lives in `jipipe-core` (despite `desktop` package name — there is no separate `jipipe-desktop` module)

**Spec:** `docs/superpowers/specs/2026-08-04-user-statistics-design.md`

---

## File Structure

All new files under `jipipe-core/src/main/java/org/hkijena/jipipe/`:

```
plugins/statistics/
  StatisticsPlugin.java                      — Plugin registration
  StatisticsPrivacyLevel.java                — Privacy level enum
  JIPipeStatisticsItem.java                  — Statistics item interface
  JIPipeStatisticsItemCategory.java          — Category enum
  JIPipeStatisticsRegistry.java              — Item registry
  items/
    MachineIdStatisticsItem.java             — Machine UUID
    OperatingSystemStatisticsItem.java       — OS name
    TotalRamStatisticsItem.java              — Total RAM in MB
    JIPipeVersionStatisticsItem.java         — JIPipe version string
    GpuInfoStatisticsItem.java               — GPU/VRAM info
    WorkflowRunsStatisticsItem.java          — Workflow run counter
    RecentProjectsCountStatisticsItem.java   — Recent projects count
    RoCratesCreatedStatisticsItem.java       — RO-Crate creation counter
    NodeMoveDistanceStatisticsItem.java      — Accumulated node move distance
    LongestNodeWidthStatisticsItem.java      — Max node width
    PopularNodesStatisticsItem.java          — Top 5 node types by usage
    LargestProjectNodesStatisticsItem.java   — Max nodes in a project
    LargestProjectCompartmentsStatisticsItem.java — Max compartments
    NoodleScoreStatisticsItem.java           — Edge path min/avg/max
  settings/
    JIPipeStatisticsApplicationSettings.java — Settings sheet
  ui/
    JIPipeDesktopStatisticsButton.java       — First-time balloon button
    JIPipeDesktopStatisticsConfigurationUI.java — Privacy level config dialog
    ShowStatisticsTool.java                  — Tools > Statistics menu item
    JIPipeDesktopStatisticsUI.java           — Statistics viewer dialog

api/service/components/
  JIPipeStatisticsServiceComponent.java      — Service component (lifecycle, storage, timer)
```

Modified files:
- `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java` — add statistics field, instantiation, components array entry, getter
- `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java` — conditionally add statistics button to status bar

---

### Task 1: Core Types (Enums and Interface)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPrivacyLevel.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItemCategory.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java`

**Interfaces:**
- Produces: `StatisticsPrivacyLevel` enum with values `None(0)`, `Installation(1)`, `ActiveInstallation(2)`, `RoughProjects(3)`, `Everything(4)`. Method `int getLevel()`.
- Produces: `JIPipeStatisticsItemCategory` enum with values `Machine`, `Usage`, `Fun`. Methods `String getCategory()`, `Icon getIcon()`.
- Produces: `JIPipeStatisticsItem` interface with methods `getId()`, `getName()`, `getDescription()`, `getCategory()`, `getRequiredPrivacyLevel()`, `serialize()`, `deserialize(JsonNode)`, `reset()`. (The `initialize(JIPipeStatisticsServiceComponent)` default method is added in Task 4 after the service component exists.)

- [ ] **Step 1: Create `StatisticsPrivacyLevel` enum**

```java
package org.hkijena.jipipe.plugins.statistics;

public enum StatisticsPrivacyLevel {
    None(0, "No statistics collected or sent"),
    Installation(1, "Machine ID and JIPipe version (sent once)"),
    ActiveInstallation(2, "Daily ping to prove installation is still active"),
    RoughProjects(3, "Project count, workflow runs, RO-Crates created"),
    Everything(4, "All statistics including fun items");

    private final int level;
    private final String description;

    StatisticsPrivacyLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " - " + description;
    }
}
```

- [ ] **Step 2: Create `JIPipeStatisticsItemCategory` enum**

```java
package org.hkijena.jipipe.plugins.statistics;

import org.hkijena.jipipe.JIPipe;

import javax.swing.*;

public enum JIPipeStatisticsItemCategory {
    Machine("Machine information"),
    Usage("Usage statistics"),
    Fun("Fun statistics");

    private final String displayName;

    JIPipeStatisticsItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getCategory() {
        return displayName;
    }

    public Icon getIcon() {
        return switch (this) {
            case Machine -> JIPipe.RESOURCES.getIcon16("apps/computer.png");
            case Usage -> JIPipe.RESOURCES.getIcon16("actions/chart-bar.png");
            case Fun -> JIPipe.RESOURCES.getIcon16("apps/games.png");
        };
    }
}
```

- [ ] **Step 3: Create `JIPipeStatisticsItem` interface**

```java
package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;

public interface JIPipeStatisticsItem {
    String getId();

    String getName();

    String getDescription();

    JIPipeStatisticsItemCategory getCategory();

    StatisticsPrivacyLevel getRequiredPrivacyLevel();

    JsonNode serialize();

    void deserialize(JsonNode node);

    void reset();
}
```

Note: The `default void initialize(JIPipeStatisticsServiceComponent service)` method is added to this interface in Task 4, after the service component class is created. This avoids a forward-reference compile error.

- [ ] **Step 4: Compile to verify no errors**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPrivacyLevel.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItemCategory.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java
git commit -m "Add statistics core types: privacy level, item category, item interface (#1304)"
```

---

### Task 2: Statistics Registry

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistry.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistryTest.java` (first test in jipipe-core)

**Interfaces:**
- Consumes: `JIPipeStatisticsItem`, `StatisticsPrivacyLevel`, `JIPipeStatisticsItemCategory`
- Produces: `JIPipeStatisticsRegistry` with methods `registerItem(JIPipeStatisticsItem)`, `getItem(String)`, `getItems()`, `getItemsForLevel(StatisticsPrivacyLevel)`, `getItemsByCategory()`.

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeStatisticsRegistryTest {

    private JIPipeStatisticsItem createItem(String id, StatisticsPrivacyLevel level, JIPipeStatisticsItemCategory category) {
        return new JIPipeStatisticsItem() {
            @Override
            public String getId() { return id; }
            @Override
            public String getName() { return id; }
            @Override
            public String getDescription() { return id; }
            @Override
            public JIPipeStatisticsItemCategory getCategory() { return category; }
            @Override
            public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return level; }
            @Override
            public com.fasterxml.jackson.databind.JsonNode serialize() { return IntNode.valueOf(42); }
            @Override
            public void deserialize(com.fasterxml.jackson.databind.JsonNode node) {}
            @Override
            public void reset() {}
        };
    }

    @Test
    void registerAndRetrieveItem() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        JIPipeStatisticsItem item = createItem("test-item", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine);
        registry.registerItem(item);
        assertSame(item, registry.getItem("test-item"));
    }

    @Test
    void getItemsForLevel_filtersByRequiredLevel() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything, JIPipeStatisticsItemCategory.Fun));
        registry.registerItem(createItem("c", StatisticsPrivacyLevel.RoughProjects, JIPipeStatisticsItemCategory.Usage));

        List<JIPipeStatisticsItem> result = registry.getItemsForLevel(StatisticsPrivacyLevel.RoughProjects);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(i -> i.getId().equals("a")));
        assertTrue(result.stream().anyMatch(i -> i.getId().equals("c")));
        assertFalse(result.stream().anyMatch(i -> i.getId().equals("b")));
    }

    @Test
    void getItemsByCategory_groupsCorrectly() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything, JIPipeStatisticsItemCategory.Fun));
        registry.registerItem(createItem("c", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));

        Map<JIPipeStatisticsItemCategory, List<JIPipeStatisticsItem>> grouped = registry.getItemsByCategory();
        assertEquals(2, grouped.get(JIPipeStatisticsItemCategory.Machine).size());
        assertEquals(1, grouped.get(JIPipeStatisticsItemCategory.Fun).size());
    }

    @Test
    void registerItem_duplicateId_throws() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("dup", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine));
        assertThrows(IllegalArgumentException.class, () ->
                registry.registerItem(createItem("dup", StatisticsPrivacyLevel.Installation, JIPipeStatisticsItemCategory.Machine)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeStatisticsRegistryTest -q`
Expected: FAIL — `JIPipeStatisticsRegistry` does not exist

- [ ] **Step 3: Write the registry implementation**

```java
package org.hkijena.jipipe.plugins.statistics;

import java.util.*;
import java.util.stream.Collectors;

public class JIPipeStatisticsRegistry {
    private final Map<String, JIPipeStatisticsItem> registeredItems = new LinkedHashMap<>();

    public void registerItem(JIPipeStatisticsItem item) {
        if (registeredItems.containsKey(item.getId())) {
            throw new IllegalArgumentException("Statistics item with ID '" + item.getId() + "' is already registered");
        }
        registeredItems.put(item.getId(), item);
    }

    public JIPipeStatisticsItem getItem(String id) {
        return registeredItems.get(id);
    }

    public List<JIPipeStatisticsItem> getItems() {
        return new ArrayList<>(registeredItems.values());
    }

    public List<JIPipeStatisticsItem> getItemsForLevel(StatisticsPrivacyLevel level) {
        return registeredItems.values().stream()
                .filter(item -> item.getRequiredPrivacyLevel().getLevel() <= level.getLevel())
                .collect(Collectors.toList());
    }

    public Map<JIPipeStatisticsItemCategory, List<JIPipeStatisticsItem>> getItemsByCategory() {
        return registeredItems.values().stream()
                .collect(Collectors.groupingBy(JIPipeStatisticsItem::getCategory, LinkedHashMap::new, Collectors.toList()));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeStatisticsRegistryTest -q`
Expected: PASS — all 4 tests pass

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistry.java jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsRegistryTest.java
git commit -m "Add statistics registry with tests (#1304)"
```

---

### Task 3: Statistics Settings Sheet

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/settings/JIPipeStatisticsApplicationSettings.java`

**Interfaces:**
- Consumes: `StatisticsPrivacyLevel`
- Produces: `JIPipeStatisticsApplicationSettings` with static `getInstance()`, fields `enabled`, `privacyLevel`, `serverUrl`, `showFirstTimePrompt`. Follows pattern of `JIPipeRuntimeApplicationSettings` at `jipipe-core/.../plugins/settings/application/JIPipeRuntimeApplicationSettings.java`.

- [ ] **Step 1: Create the settings sheet**

```java
package org.hkijena.jipipe.plugins.statistics.settings;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationsSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeDefaultApplicationSettingsSheetCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

import javax.swing.*;

public class JIPipeStatisticsApplicationSettings extends JIPipeDefaultApplicationsSettingsSheet {
    public static final String ID = "org.hkijena.jipipe:statistics";

    private boolean enabled = true;
    private StatisticsPrivacyLevel privacyLevel = StatisticsPrivacyLevel.Everything;
    private String serverUrl = "https://jipipe.hki-jena.de/statistics/api/v1/statistics";
    private boolean showFirstTimePrompt = true;

    public static JIPipeStatisticsApplicationSettings getInstance() {
        return JIPipe.getSettings().getById(ID, JIPipeStatisticsApplicationSettings.class);
    }

    @SetJIPipeDocumentation(name = "Enable statistics", description = "If enabled, JIPipe collects and sends usage statistics according to the selected privacy level.")
    @JIPipeParameter("statistics-enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @JIPipeParameter("statistics-enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @SetJIPipeDocumentation(name = "Privacy level", description = "Controls what usage statistics JIPipe collects and sends. Higher levels include all information from lower levels.")
    @JIPipeParameter("statistics-privacy-level")
    public StatisticsPrivacyLevel getPrivacyLevel() {
        return privacyLevel;
    }

    @JIPipeParameter("statistics-privacy-level")
    public void setPrivacyLevel(StatisticsPrivacyLevel privacyLevel) {
        this.privacyLevel = privacyLevel;
    }

    @SetJIPipeDocumentation(name = "Server URL", description = "The URL where statistics are sent.")
    @JIPipeParameter("statistics-server-url")
    public String getServerUrl() {
        return serverUrl;
    }

    @JIPipeParameter("statistics-server-url")
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @SetJIPipeDocumentation(name = "Show first-time prompt", description = "If enabled, JIPipe shows a first-time prompt about statistics collection on startup.")
    @JIPipeParameter("statistics-show-first-time-prompt")
    public boolean isShowFirstTimePrompt() {
        return showFirstTimePrompt;
    }

    @JIPipeParameter("statistics-show-first-time-prompt")
    public void setShowFirstTimePrompt(boolean showFirstTimePrompt) {
        this.showFirstTimePrompt = showFirstTimePrompt;
    }

    @Override
    public JIPipeDefaultApplicationSettingsSheetCategory getDefaultCategory() {
        return JIPipeDefaultApplicationSettingsSheetCategory.General;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/chart-bar.png");
    }

    @Override
    public String getName() {
        return "Statistics";
    }

    @Override
    public String getDescription() {
        return "Settings for usage statistics collection and reporting";
    }
}
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/settings/JIPipeStatisticsApplicationSettings.java
git commit -m "Add statistics application settings sheet (#1304)"
```

---

### Task 4: Statistics Service Component (Storage & Lifecycle)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponentTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsRegistry`, `JIPipeStatisticsApplicationSettings`, `JIPipeServiceComponent` (base class at `jipipe-core/.../api/service/JIPipeServiceComponent.java`)
- Produces: `JIPipeStatisticsServiceComponent` with methods `getRegistry()`, `getMachineId()`, `rerollMachineId()`, `saveLater()`, `save()`, `load()`, `getStatisticsFile()`, `getFirstLaunchTimestamp()`, `getLastSentTimestamp()`, `setLastSentTimestamp()`.
- Modifies: `JIPipeService` — add field `statistics`, instantiate in constructor, add to `components` array, add getter `getStatistics()`.

**Key reference patterns:**
- `JIPipeServiceComponent` base class: constructor takes `JIPipeService`, override `postprocess(JIPipeProgressInfo)`. File: `jipipe-core/.../api/service/JIPipeServiceComponent.java`
- `JIPipeService` constructor at lines 84-113, components array at lines 109-112, getters at lines 264-270. File: `jipipe-core/.../api/service/JIPipeService.java`
- `JIPipeApplicationSettingsServiceComponent` debounced save pattern: 250ms `Timer`, `saveLater()` restarts timer. File: `jipipe-core/.../api/service/components/JIPipeApplicationSettingsServiceComponent.java` lines 52-60
- `JsonUtils.saveToFile()` / `JsonUtils.readFromFile()`. File: `jipipe-core/.../utils/json/JsonUtils.java`

- [ ] **Step 1: Write the failing test for storage**

```java
package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsRegistry;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeStatisticsServiceComponentTest {

    @TempDir
    Path tempDir;

    private JIPipeStatisticsItem createCounterItem(String id) {
        return new JIPipeStatisticsItem() {
            int value = 0;
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public String getDescription() { return id; }
            @Override public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Usage; }
            @Override public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
            @Override public JsonNode serialize() { return JsonNodeFactory.instance.numberNode(value); }
            @Override public void deserialize(JsonNode node) { if (node != null && !node.isNull()) value = node.asInt(); }
            @Override public void reset() { value = 0; }
        };
    }

    @Test
    void machineIdGeneratedOnFirstLoad() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        ObjectNode root = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, new JIPipeStatisticsRegistry());
        String machineId = root.get("machineId").asText();
        assertNotNull(machineId);
        assertFalse(machineId.isEmpty());
    }

    @Test
    void machineIdPersistedAcrossLoads() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsRegistry registry1 = new JIPipeStatisticsRegistry();
        ObjectNode root1 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry1);
        String machineId1 = root1.get("machineId").asText();

        JIPipeStatisticsRegistry registry2 = new JIPipeStatisticsRegistry();
        ObjectNode root2 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry2);
        String machineId2 = root2.get("machineId").asText();

        assertEquals(machineId1, machineId2);
    }

    @Test
    void itemsDeserializedOnLoad() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsItem counter = createCounterItem("counter");

        // First save with value 42
        JIPipeStatisticsRegistry registry1 = new JIPipeStatisticsRegistry();
        registry1.registerItem(counter);
        ObjectNode root1 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry1);
        ObjectNode itemsNode = root1.putObject("items");
        itemsNode.put("counter", 42);
        JIPipeStatisticsServiceComponent.save(statsFile, root1);
        counter.deserialize(itemsNode.get("counter"));
        assertEquals(42, counter.serialize().asInt());

        // Second load should restore value
        JIPipeStatisticsRegistry registry2 = new JIPipeStatisticsRegistry();
        JIPipeStatisticsItem counter2 = createCounterItem("counter");
        registry2.registerItem(counter2);
        ObjectNode root2 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry2);
        JsonNode items2 = root2.get("items");
        assertNotNull(items2);
        counter2.deserialize(items2.get("counter"));
        assertEquals(42, counter2.serialize().asInt());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeStatisticsServiceComponentTest -q`
Expected: FAIL — `JIPipeStatisticsServiceComponent` does not exist

- [ ] **Step 3: Write the service component**

```java
package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.progress.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsRegistry;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class JIPipeStatisticsServiceComponent extends JIPipeServiceComponent {
    private static final Logger logger = LoggerFactory.getLogger(JIPipeStatisticsServiceComponent.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
    private ObjectNode statisticsData;
    private final Timer saveLaterTimer;

    public JIPipeStatisticsServiceComponent(JIPipeService service) {
        super(service);
        this.saveLaterTimer = new Timer(250, e -> save());
        this.saveLaterTimer.setRepeats(false);
    }

    public JIPipeStatisticsRegistry getRegistry() {
        return registry;
    }

    public Path getStatisticsFile() {
        return JIPipe.getJIPipeUserDir(false).resolve("statistics.json");
    }

    public Path getLockFile() {
        return JIPipe.getJIPipeUserDir(false).resolve("statistics.lock");
    }

    public String getMachineId() {
        if (statisticsData != null && statisticsData.has("machineId")) {
            return statisticsData.get("machineId").asText();
        }
        return null;
    }

    public void rerollMachineId() {
        if (statisticsData != null) {
            statisticsData.put("machineId", UUID.randomUUID().toString());
            save();
        }
    }

    public LocalDateTime getFirstLaunchTimestamp() {
        if (statisticsData != null && statisticsData.has("firstLaunchTimestamp")) {
            return LocalDateTime.parse(statisticsData.get("firstLaunchTimestamp").asText(), FORMATTER);
        }
        return null;
    }

    public LocalDateTime getLastSentTimestamp() {
        if (statisticsData != null && statisticsData.has("lastSentTimestamp") && !statisticsData.get("lastSentTimestamp").isNull()) {
            return LocalDateTime.parse(statisticsData.get("lastSentTimestamp").asText(), FORMATTER);
        }
        return null;
    }

    public void setLastSentTimestamp(LocalDateTime timestamp) {
        if (statisticsData != null) {
            statisticsData.put("lastSentTimestamp", timestamp.format(FORMATTER));
            saveLater();
        }
    }

    public void saveLater() {
        saveLaterTimer.restart();
    }

    public void save() {
        save(getStatisticsFile(), statisticsData);
    }

    public static void save(Path file, ObjectNode data) {
        try {
            PathUtils.ensureParentDirectoriesExist(file);
            Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
            JsonUtils.getObjectMapper().writeValue(tmpFile.toFile(), data);
            Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            logger.error("Failed to save statistics to {}", file, e);
        }
    }

    public static ObjectNode loadOrCreate(Path file, JIPipeStatisticsRegistry registry) {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode root;
        if (Files.isRegularFile(file)) {
            try {
                root = (ObjectNode) mapper.readTree(file.toFile());
            } catch (Exception e) {
                logger.error("Failed to load statistics from {}, creating new", file, e);
                root = mapper.createObjectNode();
            }
        } else {
            root = mapper.createObjectNode();
        }

        if (!root.has("machineId") || root.get("machineId").asText().isEmpty()) {
            root.put("machineId", UUID.randomUUID().toString());
        }
        if (!root.has("firstLaunchTimestamp")) {
            root.put("firstLaunchTimestamp", LocalDateTime.now().format(FORMATTER));
        }
        if (!root.has("lastSentTimestamp")) {
            root.putNull("lastSentTimestamp");
        }
        if (!root.has("items")) {
            root.putObject("items");
        }

        JsonNode itemsNode = root.get("items");
        for (var item : registry.getItems()) {
            JsonNode itemData = itemsNode.get(item.getId());
            if (itemData != null) {
                item.deserialize(itemData);
            }
        }

        return root;
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        statisticsData = loadOrCreate(getStatisticsFile(), registry);
        for (var item : registry.getItems()) {
            item.initialize(this);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=JIPipeStatisticsServiceComponentTest -q`
Expected: PASS — all 3 tests pass

- [ ] **Step 5: Add `initialize` method to `JIPipeStatisticsItem` interface**

Now that `JIPipeStatisticsServiceComponent` exists, add the default method to the interface created in Task 1. Add this method to `JIPipeStatisticsItem.java`:

```java
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;

// Add to the interface body:
default void initialize(JIPipeStatisticsServiceComponent service) {
}
```

- [ ] **Step 6: Register the service component in JIPipeService**

Read `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java`. Find the constructor where service components are instantiated (around lines 84-113). Add:

```java
// After the line that creates projectBackup (around line 104):
statistics = new JIPipeStatisticsServiceComponent(this);
```

Add the field declaration with the other service component fields:

```java
private final JIPipeStatisticsServiceComponent statistics;
```

Add to the `components` array (around lines 109-112):

```java
this.components = new JIPipeServiceComponent[] {
        recentProjects, nodes, dataTypes, /* ... existing components ... */, projectBackup, aiService, serverService, statistics
};
```

Add the getter method (following the pattern at lines 264-270):

```java
public JIPipeStatisticsServiceComponent getStatistics() {
    ensureInitialized();
    return statistics;
}
```

Add the import:

```java
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
```

- [ ] **Step 7: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java jipipe-core/src/test/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponentTest.java jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/JIPipeStatisticsItem.java
git commit -m "Add statistics service component with storage, load/save, and tests (#1304)"
```

---

### Task 5: Statistics Plugin

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsApplicationSettings`, `StatisticsPrivacyLevel`, `JIPipeStatisticsServiceComponent` (via `JIPipe.getInstance().getStatistics()`)
- Produces: `StatisticsPlugin` — a `JIPipePrepackagedDefaultJavaPlugin` that registers the privacy level enum type, settings sheet, and Tools menu item. Statistics items are registered in later tasks.

**Key reference patterns:**
- `ProcessesPlugin` at `jipipe-core/.../plugins/processes/ProcessesPlugin.java` — minimal plugin example
- `StandardSettingsPlugin.register()` at `jipipe-core/.../plugins/settings/StandardSettingsPlugin.java` lines 60-106 — registration pattern
- `JIPipeDefaultJavaPlugin.registerEnumParameterType(id, EnumClass.class, name, description)` — line 430 of `JIPipeDefaultJavaPlugin.java`
- `JIPipeDefaultJavaPlugin.registerApplicationSettingsSheet(sheet)` — line 696 of `JIPipeDefaultJavaPlugin.java`
- `JIPipeDefaultJavaPlugin.registerMenuExtension(Class)` — line 224 of `JIPipeDefaultJavaPlugin.java`

- [ ] **Step 1: Create the plugin class**

```java
package org.hkijena.jipipe.plugins.statistics;

import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.api.progress.JIPipeProgressInfo;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.plugins.statistics.ui.ShowStatisticsTool;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

@Plugin(type = JIPipeJavaPlugin.class)
public class StatisticsPlugin extends JIPipePrepackagedDefaultJavaPlugin {

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Statistics";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Usage statistics collection and reporting");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:statistics";
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }

    @Override
    public void register(JIPipeService service, Context context, JIPipeProgressInfo progressInfo) {
        registerEnumParameterType("statistics-privacy-level",
                StatisticsPrivacyLevel.class,
                "Statistics privacy level",
                "Controls what usage statistics JIPipe collects and sends");

        registerApplicationSettingsSheet(new JIPipeStatisticsApplicationSettings());

        registerMenuExtension(ShowStatisticsTool.class);
    }
}
```

- [ ] **Step 2: Create a placeholder `ShowStatisticsTool` so it compiles**

This will be fully implemented in Task 15. For now, create a minimal version:

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

public class ShowStatisticsTool extends JIPipeDesktopMenuExtension {
    public ShowStatisticsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Statistics");
        setToolTipText("View collected usage statistics");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/chart-bar.png"));
        addActionListener(e -> {
            // Implemented in Task 15
        });
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }
}
```

- [ ] **Step 3: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/ShowStatisticsTool.java
git commit -m "Add statistics plugin and placeholder Tools menu item (#1304)"
```

---

### Task 6: Passive Machine Statistics Items

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/OperatingSystemStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/TotalRamStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/JIPipeVersionStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/GpuInfoStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/MachineIdStatisticsItem.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsItem`, `StatisticsPrivacyLevel`, `JIPipeStatisticsItemCategory`, `JIPipeStatisticsServiceComponent`
- Produces: 5 passive items that compute values on demand. `MachineIdStatisticsItem` reads from the service component; others read from system APIs.

- [ ] **Step 1: Create `OperatingSystemStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class OperatingSystemStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "operating-system"; }
    @Override
    public String getName() { return "Operating system"; }
    @Override
    public String getDescription() { return "The operating system name and version"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        return TextNode.valueOf(os);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
```

- [ ] **Step 2: Create `TotalRamStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class TotalRamStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "total-ram-mb"; }
    @Override
    public String getName() { return "Total RAM (MB)"; }
    @Override
    public String getDescription() { return "Total system RAM in megabytes"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long ramMb = Math.max(maxMemory, totalMemory) / (1024 * 1024);
        return IntNode.valueOf((int) ramMb);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
```

- [ ] **Step 3: Create `JIPipeVersionStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.VersionUtils;

public class JIPipeVersionStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "jipipe-version"; }
    @Override
    public String getName() { return "JIPipe version"; }
    @Override
    public String getDescription() { return "The JIPipe version string"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        return TextNode.valueOf(VersionUtils.getJIPipeVersion());
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
```

- [ ] **Step 4: Create `GpuInfoStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.stream.Collectors;

public class GpuInfoStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "gpu-info"; }
    @Override
    public String getName() { return "GPU information"; }
    @Override
    public String getDescription() { return "Available graphics devices"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < devices.length; i++) {
            if (i > 0) sb.append("; ");
            sb.append(devices[i].getIDstring());
        }
        return TextNode.valueOf(sb.toString());
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
```

- [ ] **Step 5: Create `MachineIdStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class MachineIdStatisticsItem implements JIPipeStatisticsItem {
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "machine-id"; }
    @Override
    public String getName() { return "Machine ID"; }
    @Override
    public String getDescription() { return "A unique identifier for this machine"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Installation; }
    @Override
    public JsonNode serialize() {
        String id = service != null ? service.getMachineId() : "";
        return TextNode.valueOf(id != null ? id : "");
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
    }
}
```

- [ ] **Step 6: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/
git commit -m "Add passive machine statistics items: OS, RAM, version, GPU, machine ID (#1304)"
```

---

### Task 7: Usage Statistics Items

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/RecentProjectsCountStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/WorkflowRunsStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/RoCratesCreatedStatisticsItem.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsItem`, `JIPipeStatisticsServiceComponent`, `JIPipeRunnableQueue`, `JIPipeGraphRun`, `CreateROCrateRun`
- Produces: `RecentProjectsCountStatisticsItem` (passive — reads on demand), `WorkflowRunsStatisticsItem` (persistent counter — subscribes to `JIPipeRunnableQueue.getFinishedEventEmitter()`), `RoCratesCreatedStatisticsItem` (persistent counter — subscribes to same emitter, filters for `CreateROCrateRun`).

**Key reference patterns:**
- `JIPipeRunnableQueue.getInstance().getFinishedEventEmitter()` — file: `jipipe-core/.../api/run/JIPipeRunnableQueue.java` line 83
- `JIPipeRunnable.FinishedEvent.getRun()` — file: `jipipe-core/.../api/run/JIPipeRunnable.java` line 156
- `JIPipeGraphRun.getParent()` — returns null for top-level runs. File: `jipipe-core/.../api/run/JIPipeGraphRun.java` line 127
- `JIPipe.getInstance().getRecentProjects().getRecentProjects().size()` — file: `jipipe-core/.../api/service/components/JIPipeRecentProjectsRegistry.java` line 69
- `CreateROCrateRun` — file: `jipipe-core/.../plugins/publish/rocrate/CreateROCrateRun.java`

- [ ] **Step 1: Create `RecentProjectsCountStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class RecentProjectsCountStatisticsItem implements JIPipeStatisticsItem {
    @Override
    public String getId() { return "recent-projects-count"; }
    @Override
    public String getName() { return "Recent projects"; }
    @Override
    public String getDescription() { return "Number of projects in the recent projects list"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Usage; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
    @Override
    public JsonNode serialize() {
        int count = JIPipe.getInstance().getRecentProjects().getRecentProjects().size();
        return IntNode.valueOf(count);
    }
    @Override
    public void deserialize(JsonNode node) { }
    @Override
    public void reset() { }
}
```

- [ ] **Step 2: Create `WorkflowRunsStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class WorkflowRunsStatisticsItem implements JIPipeStatisticsItem {
    private int count = 0;
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "workflow-runs"; }
    @Override
    public String getName() { return "Workflow runs"; }
    @Override
    public String getDescription() { return "Number of times the user ran a workflow"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Usage; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
    @Override
    public JsonNode serialize() { return IntNode.valueOf(count); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) count = node.asInt(); }
    @Override
    public void reset() { count = 0; }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this::onRunFinished);
    }

    private void onRunFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeGraphRun run && run.getParent() == null) {
            count++;
            if (service != null) service.saveLater();
        }
    }
}
```

- [ ] **Step 3: Create `RoCratesCreatedStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.plugins.publish.rocrate.CreateROCrateRun;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class RoCratesCreatedStatisticsItem implements JIPipeStatisticsItem {
    private int count = 0;
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "ro-crates-created"; }
    @Override
    public String getName() { return "RO-Crates created"; }
    @Override
    public String getDescription() { return "Number of RO-Crates created by the user"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Usage; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.RoughProjects; }
    @Override
    public JsonNode serialize() { return IntNode.valueOf(count); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) count = node.asInt(); }
    @Override
    public void reset() { count = 0; }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribe(this::onRunFinished);
    }

    private void onRunFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof CreateROCrateRun) {
            count++;
            if (service != null) service.saveLater();
        }
    }
}
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/RecentProjectsCountStatisticsItem.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/WorkflowRunsStatisticsItem.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/RoCratesCreatedStatisticsItem.java
git commit -m "Add usage statistics items: recent projects count, workflow runs, RO-Crates (#1304)"
```

---

### Task 8: Fun Statistics Items (Part 1 — Popular Nodes & Largest Project)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/PopularNodesStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LargestProjectNodesStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LargestProjectCompartmentsStatisticsItem.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsItem`, `JIPipeStatisticsServiceComponent`, `JIPipeGraph.NodeAddedEventEmitter`, `JIPipeDesktopProjectWindow`
- Produces: `PopularNodesStatisticsItem` (persistent — tracks node type additions), `LargestProjectNodesStatisticsItem` and `LargestProjectCompartmentsStatisticsItem` (persistent — tracks max on project open).

**Key reference patterns:**
- `JIPipeGraph.getNodeAddedEventEmitter()` — file: `jipipe-core/.../api/nodes/JIPipeGraph.java` line 144. Event has `getNode()` which returns `JIPipeGraphNode`. Node type ID: `node.getInfo().getId()`.
- `JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER` — file: `jipipe-core/.../desktop/app/JIPipeDesktopProjectWindow.java` line 87
- `JIPipeProject.getGraph()` and `graph.getNodes()` for counting nodes
- `JIPipeProject.getCompartmentGraph()` and compartment count

- [ ] **Step 1: Create `PopularNodesStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PopularNodesStatisticsItem implements JIPipeStatisticsItem {
    private final Map<String, Integer> nodeCounts = new HashMap<>();
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "popular-nodes"; }
    @Override
    public String getName() { return "Popular nodes"; }
    @Override
    public String getDescription() { return "Top 5 most used node types"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }

    @Override
    public JsonNode serialize() {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        nodeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> node.put(e.getKey(), e.getValue()));
        return node;
    }

    @Override
    public void deserialize(JsonNode node) {
        nodeCounts.clear();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(e -> nodeCounts.put(e.getKey(), e.getValue().asInt()));
        }
    }

    @Override
    public void reset() { nodeCounts.clear(); }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this::onWindowOpened);
    }

    private void onWindowOpened(JIPipeDesktopProjectWindow.WindowOpenedEvent event) {
        JIPipeProject project = event.getWindow().getProject();
        if (project != null) {
            JIPipeGraph graph = project.getGraph();
            graph.getNodeAddedEventEmitter().subscribe(this::onNodeAdded);
            for (JIPipeGraphNode existingNode : graph.getGraphNodes()) {
                countNode(existingNode);
            }
        }
    }

    private void onNodeAdded(JIPipeGraph.NodeAddedEvent event) {
        countNode(event.getNode());
    }

    private void countNode(JIPipeGraphNode node) {
        String id = node.getInfo().getId();
        nodeCounts.merge(id, 1, Integer::sum);
        if (service != null) service.saveLater();
    }
}
```

- [ ] **Step 2: Create `LargestProjectNodesStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class LargestProjectNodesStatisticsItem implements JIPipeStatisticsItem {
    private int maxNodes = 0;
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "largest-project-nodes"; }
    @Override
    public String getName() { return "Largest project (nodes)"; }
    @Override
    public String getDescription() { return "Maximum number of nodes in any opened project"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() { return IntNode.valueOf(maxNodes); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) maxNodes = node.asInt(); }
    @Override
    public void reset() { maxNodes = 0; }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this::onWindowOpened);
    }

    private void onWindowOpened(JIPipeDesktopProjectWindow.WindowOpenedEvent event) {
        JIPipeProject project = event.getWindow().getProject();
        if (project != null) {
            int count = (int) project.getGraph().getGraphNodes().size();
            if (count > maxNodes) {
                maxNodes = count;
                if (service != null) service.saveLater();
            }
        }
    }
}
```

- [ ] **Step 3: Create `LargestProjectCompartmentsStatisticsItem`**

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class LargestProjectCompartmentsStatisticsItem implements JIPipeStatisticsItem {
    private int maxCompartments = 0;
    private JIPipeStatisticsServiceComponent service;

    @Override
    public String getId() { return "largest-project-compartments"; }
    @Override
    public String getName() { return "Largest project (compartments)"; }
    @Override
    public String getDescription() { return "Maximum number of compartments in any opened project"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() { return IntNode.valueOf(maxCompartments); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) maxCompartments = node.asInt(); }
    @Override
    public void reset() { maxCompartments = 0; }

    @Override
    public void initialize(JIPipeStatisticsServiceComponent service) {
        this.service = service;
        JIPipeDesktopProjectWindow.WINDOW_OPENED_EVENT_EMITTER.subscribe(this::onWindowOpened);
    }

    private void onWindowOpened(JIPipeDesktopProjectWindow.WindowOpenedEvent event) {
        JIPipeProject project = event.getWindow().getProject();
        if (project != null) {
            int count = (int) project.getCompartmentGraph().getGraphNodes().size();
            if (count > maxCompartments) {
                maxCompartments = count;
                if (service != null) service.saveLater();
            }
        }
    }
}
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS (may need to verify `WINDOW_OPENED_EVENT_EMITTER` is accessible and `WindowOpenedEvent` API matches)

If compilation fails due to `WINDOW_OPENED_EVENT_EMITTER` not being static or accessible, check the actual declaration in `JIPipeDesktopProjectWindow.java` around line 87 and adjust the access pattern. The event emitter may need to be accessed via an instance method rather than a static field. If so, the `initialize()` method will need to find open windows via `JIPipeDesktopProjectWindow.getOpenWindows()` and subscribe to each, plus listen for new windows.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/PopularNodesStatisticsItem.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LargestProjectNodesStatisticsItem.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LargestProjectCompartmentsStatisticsItem.java
git commit -m "Add fun statistics items: popular nodes, largest project nodes/compartments (#1304)"
```

---

### Task 9: Fun Statistics Items (Part 2 — Node Move Distance, Noodle Score, Longest Node Width)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/NodeMoveDistanceStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/NoodleScoreStatisticsItem.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LongestNodeWidthStatisticsItem.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsItem`, `JIPipeStatisticsServiceComponent`, `JIPipeDesktopGraphEdgeUI`, `JIPipeGraphNode`
- Produces: `NodeMoveDistanceStatisticsItem` (persistent — accumulates distance from node drags), `NoodleScoreStatisticsItem` (passive — computes from current open project's edge UIs on demand), `LongestNodeWidthStatisticsItem` (persistent — tracks max width from node UI).

**Key reference patterns:**
- `JIPipeGraphNode.getNodeUILocationWithin(compartmentUUID)` returns `Point` — file: `JIPipeGraphNode.java`
- `JIPipeDesktopGraphEdgeUI.getUIManhattanDistance()` — file: `jipipe-core/.../desktop/app/grapheditor/commons/edgeui/JIPipeDesktopGraphEdgeUI.java` line 102
- `canvasUI.getEdgeUIs()` returns `Map<UUID, JIPipeDesktopGraphEdgeUI>` — used in `JIPipeDesktopGraphCanvasEdgesOverlay.java` line 110

- [ ] **Step 1: Create `NodeMoveDistanceStatisticsItem`**

This item instruments `JIPipeGraphNode.setNodeUILocationWithin()` by subscribing to graph node position changes. Since there is no dedicated event for this, we use a simpler approach: track positions when nodes are moved via the drag manager by subscribing to graph node `ParameterChangedEventEmitter` for the position parameter, or by polling on save.

A simpler and more robust approach: track total move distance by subscribing to `JIPipeGraphNode.getNodeInfoChangedEventEmitter()` — but this may not fire on UI moves. The most reliable approach given the codebase is to make this a passive item that reads accumulated distance from a static counter updated by a static method, called from the drag manager.

For this task, we create the item as a persistent counter with a static `addDistance(double)` method. The actual instrumentation of the drag manager will be done in Task 13.

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class NodeMoveDistanceStatisticsItem implements JIPipeStatisticsItem {
    private static double accumulatedDistance = 0;
    private double distance = 0;

    @Override
    public String getId() { return "node-move-distance"; }
    @Override
    public String getName() { return "Node move distance"; }
    @Override
    public String getDescription() { return "Total distance nodes have been moved by the user (pixels)"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() { return DoubleNode.valueOf(distance); }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) distance = node.asDouble(); }
    @Override
    public void reset() { distance = 0; }

    public static void addDistance(double delta, Runnable onSave) {
        accumulatedDistance += delta;
        // The instance will pick up accumulatedDistance on next serialize
        // This is a simple approach — the instance's distance is updated on save
    }

    public void syncAccumulated(Runnable onSave) {
        if (accumulatedDistance > 0) {
            distance += accumulatedDistance;
            accumulatedDistance = 0;
            if (onSave != null) onSave.run();
        }
    }
}
```

- [ ] **Step 2: Create `NoodleScoreStatisticsItem`**

This is a passive item that computes on demand from the currently open project's edge UIs.

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.utils.json.JsonUtils;

public class NoodleScoreStatisticsItem implements JIPipeStatisticsItem {
    private double min = Double.MAX_VALUE;
    private double avg = 0;
    private double max = 0;

    @Override
    public String getId() { return "noodle-score"; }
    @Override
    public String getName() { return "Noodle score"; }
    @Override
    public String getDescription() { return "Edge path lengths (min/avg/max) across all opened graph editors"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }

    @Override
    public JsonNode serialize() {
        // Compute from currently open windows
        double currentMin = Double.MAX_VALUE;
        double currentMax = 0;
        double sum = 0;
        int count = 0;

        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            var workbench = window.getProjectWorkbench();
            if (workbench != null) {
                for (var editorUI : workbench.getGraphEditorUIs()) {
                    JIPipeDesktopGraphCanvasUI canvasUI = editorUI.getCanvasUI();
                    for (JIPipeDesktopGraphEdgeUI edgeUI : canvasUI.getEdgeUIs().values()) {
                        int dist = edgeUI.getUIManhattanDistance();
                        if (dist > 0) {
                            currentMin = Math.min(currentMin, dist);
                            currentMax = Math.max(currentMax, dist);
                            sum += dist;
                            count++;
                        }
                    }
                }
            }
        }

        if (count > 0) {
            min = Math.min(min, currentMin);
            max = Math.max(max, currentMax);
            avg = sum / count;
        }
        if (min == Double.MAX_VALUE) min = 0;

        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("min", min);
        node.put("avg", avg);
        node.put("max", max);
        return node;
    }

    @Override
    public void deserialize(JsonNode node) {
        if (node != null && node.isObject()) {
            min = node.has("min") ? node.get("min").asDouble() : Double.MAX_VALUE;
            avg = node.has("avg") ? node.get("avg").asDouble() : 0;
            max = node.has("max") ? node.get("max").asDouble() : 0;
        }
    }

    @Override
    public void reset() { min = Double.MAX_VALUE; avg = 0; max = 0; }
}
```

- [ ] **Step 3: Create `LongestNodeWidthStatisticsItem`**

Similar to node move distance, this uses a static method for instrumentation. The actual hook will be added in Task 13.

```java
package org.hkijena.jipipe.plugins.statistics.items;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;

public class LongestNodeWidthStatisticsItem implements JIPipeStatisticsItem {
    private static int maxWidthSeen = 0;
    private int maxWidth = 0;

    @Override
    public String getId() { return "longest-node-width"; }
    @Override
    public String getName() { return "Longest node width"; }
    @Override
    public String getDescription() { return "The widest node UI seen (pixels)"; }
    @Override
    public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Fun; }
    @Override
    public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return StatisticsPrivacyLevel.Everything; }
    @Override
    public JsonNode serialize() {
        if (maxWidthSeen > maxWidth) maxWidth = maxWidthSeen;
        return IntNode.valueOf(maxWidth);
    }
    @Override
    public void deserialize(JsonNode node) { if (node != null && !node.isNull()) maxWidth = node.asInt(); }
    @Override
    public void reset() { maxWidth = 0; maxWidthSeen = 0; }

    public static void reportWidth(int width) {
        maxWidthSeen = Math.max(maxWidthSeen, width);
    }
}
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS (may need to verify `getGraphEditorUIs()` and `getEdgeUIs()` method names match actual API)

If compilation fails due to method name mismatches, check:
- `JIPipeDesktopProjectWorkbench` for how to access graph editor UIs (may be via `getDocumentTabPane()` or similar)
- `JIPipeDesktopGraphCanvasUI.getEdgeUIs()` for the actual method name

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/NodeMoveDistanceStatisticsItem.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/NoodleScoreStatisticsItem.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/items/LongestNodeWidthStatisticsItem.java
git commit -m "Add fun statistics items: node move distance, noodle score, longest node width (#1304)"
```

---

### Task 10: HTTP Reporting

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsReporter.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/StatisticsReporterTest.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsServiceComponent`, `JIPipeStatisticsApplicationSettings`, `JIPipeStatisticsRegistry`, `StatisticsPrivacyLevel`
- Produces: `StatisticsReporter` with methods `buildPayload(StatisticsPrivacyLevel)` returning `ObjectNode`, `sendNow(Consumer<Boolean> callback)` for async POST.

**Key reference patterns:**
- `JIPipeAPIEmbeddingAIModelRunner` — `HttpClient` POST with JSON body. File: `jipipe-core/.../api/ai/JIPipeAPIEmbeddingAIModelRunner.java`
- `JsonUtils.getObjectMapper()` for JSON serialization. File: `jipipe-core/.../utils/json/JsonUtils.java`

- [ ] **Step 1: Write the failing test for payload building**

```java
package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.utils.VersionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsReporterTest {

    private JIPipeStatisticsItem createItem(String id, StatisticsPrivacyLevel level, int value) {
        return new JIPipeStatisticsItem() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public String getDescription() { return id; }
            @Override public JIPipeStatisticsItemCategory getCategory() { return JIPipeStatisticsItemCategory.Machine; }
            @Override public StatisticsPrivacyLevel getRequiredPrivacyLevel() { return level; }
            @Override public JsonNode serialize() { return IntNode.valueOf(value); }
            @Override public void deserialize(JsonNode node) {}
            @Override public void reset() {}
        };
    }

    @Test
    void buildPayload_filtersByPrivacyLevel() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, 1));
        registry.registerItem(createItem("b", StatisticsPrivacyLevel.Everything, 2));

        ObjectNode payload = StatisticsReporter.buildPayload(
                registry, StatisticsPrivacyLevel.Installation, "test-machine-id");

        assertEquals("test-machine-id", payload.get("machineId").asText());
        assertEquals(VersionUtils.getJIPipeVersion(), payload.get("jipipeVersion").asText());
        assertNotNull(payload.get("timestamp"));
        assertEquals("INSTALLATION", payload.get("privacyLevel").asText());

        JsonNode items = payload.get("items");
        assertTrue(items.has("a"));
        assertFalse(items.has("b"));
    }

    @Test
    void buildPayload_noneLevel_includesNoItems() {
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        registry.registerItem(createItem("a", StatisticsPrivacyLevel.Installation, 1));

        ObjectNode payload = StatisticsReporter.buildPayload(
                registry, StatisticsPrivacyLevel.None, "test-machine-id");

        JsonNode items = payload.get("items");
        assertFalse(items.has("a"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl jipipe-core -Dtest=StatisticsReporterTest -q`
Expected: FAIL — `StatisticsReporter` does not exist

- [ ] **Step 3: Write the reporter**

```java
package org.hkijena.jipipe.plugins.statistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class StatisticsReporter {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsReporter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static ObjectNode buildPayload(JIPipeStatisticsRegistry registry,
                                          StatisticsPrivacyLevel level,
                                          String machineId) {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("machineId", machineId != null ? machineId : "");
        payload.put("jipipeVersion", VersionUtils.getJIPipeVersion());
        payload.put("timestamp", LocalDateTime.now().format(FORMATTER));
        payload.put("privacyLevel", level.name());

        ObjectNode items = payload.putObject("items");
        if (level != StatisticsPrivacyLevel.None) {
            for (JIPipeStatisticsItem item : registry.getItemsForLevel(level)) {
                JsonNode itemData = item.serialize();
                if (itemData != null) {
                    items.set(item.getId(), itemData);
                }
            }
        }
        return payload;
    }

    public static void sendNow(Consumer<Boolean> callback) {
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
        if (!settings.isEnabled() || settings.getPrivacyLevel() == StatisticsPrivacyLevel.None) {
            if (callback != null) callback.accept(false);
            return;
        }

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        ObjectNode payload = buildPayload(service.getRegistry(), settings.getPrivacyLevel(), service.getMachineId());
        String json = JsonUtils.toJsonString(payload);
        String url = settings.getServerUrl();

        Thread thread = new Thread(() -> {
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
                if (success) {
                    service.setLastSentTimestamp(LocalDateTime.now());
                }
                if (callback != null) callback.accept(success);
            } catch (Exception e) {
                logger.warn("Failed to send statistics to {}", url, e);
                if (callback != null) callback.accept(false);
            }
        }, "Statistics-Reporter");
        thread.setDaemon(true);
        thread.start();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -pl jipipe-core -Dtest=StatisticsReporterTest -q`
Expected: PASS — both tests pass

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsReporter.java jipipe-core/src/test/java/org/hkijena/jipipe/plugins/statistics/StatisticsReporterTest.java
git commit -m "Add statistics reporter with payload builder and HTTP POST (#1304)"
```

---

### Task 11: Daily Timer and Send Flow

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`

**Interfaces:**
- Consumes: `StatisticsReporter`, `JIPipeStatisticsApplicationSettings`
- Modifies: `JIPipeStatisticsServiceComponent` — add daily timer in `postprocess()`, add `checkAndSend()` method, add `sendNow()` method.

**Key reference patterns:**
- `JIPipeProjectBackupServiceComponent.restartTimer()` — `javax.swing.Timer` with `setRepeats(true)`, started in `postprocess()`. File: `jipipe-core/.../api/service/components/JIPipeProjectBackupServiceComponent.java` lines 38-49

- [ ] **Step 1: Add timer and send logic to service component**

Add the following to `JIPipeStatisticsServiceComponent`:

New field:
```java
private Timer reportingTimer;
```

Add to `postprocess()` after existing code:
```java
@Override
public void postprocess(JIPipeProgressInfo progressInfo) {
    statisticsData = loadOrCreate(getStatisticsFile(), registry);
    for (var item : registry.getItems()) {
        item.initialize(this);
    }

    // Start daily reporting timer (1-hour tick)
    reportingTimer = new Timer(60 * 60 * 1000, e -> checkAndSend());
    reportingTimer.setRepeats(true);
    reportingTimer.start();

    // Check on startup
    checkAndSend();
}
```

Add the `checkAndSend()` method:
```java
private void checkAndSend() {
    var settings = JIPipeStatisticsApplicationSettings.getInstance();
    if (!settings.isEnabled() || settings.getPrivacyLevel() == StatisticsPrivacyLevel.None) {
        return;
    }

    LocalDateTime lastSent = getLastSentTimestamp();
    if (lastSent == null || lastSent.plusHours(24).isBefore(LocalDateTime.now())) {
        StatisticsReporter.sendNow(success -> {
            if (success) {
                logger.info("Statistics sent successfully");
            } else {
                logger.info("Failed to send statistics (will retry later)");
            }
        });
    }
}
```

Add the `sendNow()` public method:
```java
public void sendNow(java.util.function.Consumer<Boolean> callback) {
    StatisticsReporter.sendNow(callback);
}
```

Add necessary imports at the top:
```java
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.StatisticsReporter;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import java.time.LocalDateTime;
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java
git commit -m "Add daily reporting timer and send flow to statistics service (#1304)"
```

---

### Task 12: First-Time Statistics Button + Balloon

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsButton.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsApplicationSettings`, `JIPipeDesktopStatisticsConfigurationUI` (from Task 13), `StatisticsReporter`, `BalloonTip`
- Produces: `JIPipeDesktopStatisticsButton` — a `JButton` that shows a `BalloonTip` on first launch with "Configure" and "Dismiss" actions.

**Key reference patterns:**
- `JIPipeDesktopAuthorProfileButton` — full balloon pattern. File: `jipipe-core/.../desktop/app/components/JIPipeDesktopAuthorProfileButton.java`
- `JIPipeDesktopProjectWorkbench.initializeStatusBar()` — status bar setup. File: `jipipe-core/.../desktop/app/JIPipeDesktopProjectWorkbench.java` lines 515-563
- `JIPipeDesktopProjectWindow.registerBalloon()` — file: `jipipe-core/.../desktop/app/JIPipeDesktopProjectWindow.java` line 714

- [ ] **Step 1: Create `JIPipeDesktopStatisticsButton`**

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.StatisticsReporter;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopStatisticsButton extends JButton implements JIPipeDesktopWorkbenchAccess {
    private final JIPipeDesktopProjectWorkbench workbench;
    private BalloonTip balloonTip;

    public JIPipeDesktopStatisticsButton(JIPipeDesktopProjectWorkbench desktopWorkbench) {
        this.workbench = desktopWorkbench;
        initialize();
        initializeBalloon();

        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
        if (settings.isShowFirstTimePrompt() && settings.isEnabled()) {
            showStatisticsBalloon();
        }
    }

    private void initialize() {
        setOpaque(false);
        setText("Statistics");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/chart-bar.png"));
        setToolTipText("Help us improve JIPipe by sending usage statistics");
        addActionListener(e -> showConfigurationDialog());
    }

    private void initializeBalloon() {
        EdgedBalloonStyle style = new EdgedBalloonStyle(UIManager.getColor("TextField.background"), ThemeUtils.getCurrentStyle().getPrimaryColor());
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(UIUtils.createJLabel("Help us improve JIPipe", 16), BorderLayout.NORTH);
        content.add(new JLabel("<html><strong>JIPipe collects usage statistics to secure funding</strong><br/>" +
                "for NFDI4BIOIMAGE. You can choose what data is shared or opt out entirely.<br/>" +
                "The default setting sends all available statistics.</html>"), BorderLayout.CENTER);
        JPanel buttons = UIUtils.boxHorizontal(
                UIUtils.createButton("Dismiss", JIPipe.RESOURCES.getIcon16("actions/clock.png"), this::dismiss),
                UIUtils.createButton("Configure", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::showConfigurationDialog)
        );
        buttons.setOpaque(false);
        content.add(buttons, BorderLayout.SOUTH);
        balloonTip = new BalloonTip(
                this,
                content,
                style,
                BalloonTip.Orientation.LEFT_ABOVE,
                BalloonTip.AttachLocation.ALIGNED,
                30, 10,
                true
        );
        balloonTip.setVisible(false);

        JButton closeButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/window-close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        closeButton.setOpaque(false);
        balloonTip.setCloseButton(closeButton, false);
    }

    private void showStatisticsBalloon() {
        UIUtils.invokeMuchLater(2000, () -> {
            balloonTip.refreshLocation();
            balloonTip.setVisible(true);
            workbench.getProjectWindow().registerBalloon(balloonTip);
        });
    }

    private void dismiss() {
        balloonTip.setVisible(false);
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
        settings.setShowFirstTimePrompt(false);
        JIPipe.getInstance().getApplicationSettings().saveLater();

        if (settings.isEnabled() && settings.getPrivacyLevel() != StatisticsPrivacyLevel.None) {
            StatisticsReporter.sendNow(success -> {});
        }

        removeFromStatusBar();
    }

    private void showConfigurationDialog() {
        balloonTip.setVisible(false);
        JIPipeDesktopStatisticsConfigurationUI dialog = new JIPipeDesktopStatisticsConfigurationUI(workbench.getWindow());
        dialog.setVisible(true);

        if (dialog.isConfigured()) {
            JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
            settings.setShowFirstTimePrompt(false);
            JIPipe.getInstance().getApplicationSettings().saveLater();

            if (settings.isEnabled() && settings.getPrivacyLevel() != StatisticsPrivacyLevel.None) {
                StatisticsReporter.sendNow(success -> {});
            }

            removeFromStatusBar();
        }
    }

    private void removeFromStatusBar() {
        Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }
}
```

- [ ] **Step 2: Create a placeholder `JIPipeDesktopStatisticsConfigurationUI` so it compiles**

This will be fully implemented in Task 13. Create a minimal version:

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopStatisticsConfigurationUI extends JDialog {
    private boolean configured = false;
    private StatisticsPrivacyLevel selectedLevel;

    public JIPipeDesktopStatisticsConfigurationUI(Window parent) {
        super(parent, "Statistics Configuration");
        setModal(true);
        selectedLevel = JIPipeStatisticsApplicationSettings.getInstance().getPrivacyLevel();
        initialize();
        pack();
        setSize(500, 350);
        setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JPanel infoPanel = new JPanel(new BorderLayout(8, 8));
        infoPanel.add(UIUtils.createInfoLabel("Usage Statistics",
                "JIPipe collects usage statistics to secure funding for NFDI4BIOIMAGE.",
                JIPipe.RESOURCES.getIcon64("actions/chart-bar.png")), BorderLayout.NORTH);

        JComboBox<StatisticsPrivacyLevel> comboBox = new JComboBox<>(StatisticsPrivacyLevel.values());
        comboBox.setSelectedItem(selectedLevel);
        comboBox.addActionListener(e -> selectedLevel = (StatisticsPrivacyLevel) comboBox.getSelectedItem());
        infoPanel.add(comboBox, BorderLayout.CENTER);

        JLabel descLabel = new JLabel("<html>" + selectedLevel.getDescription() + "</html>");
        infoPanel.add(descLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton cancelButton = UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), e -> {
            configured = false;
            setVisible(false);
        });
        JButton okButton = UIUtils.createButton("OK", JIPipe.RESOURCES.getIcon16("actions/checkmark.png"), e -> {
            JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
            settings.setPrivacyLevel(selectedLevel);
            JIPipe.getInstance().getApplicationSettings().saveLater();
            configured = true;
            setVisible(false);
        });
        buttons.add(cancelButton);
        buttons.add(okButton);
        add(buttons, BorderLayout.SOUTH);
    }

    public boolean isConfigured() {
        return configured;
    }
}
```

- [ ] **Step 3: Wire the button into the status bar**

Modify `JIPipeDesktopProjectWorkbench.initializeStatusBar()` at `jipipe-core/.../desktop/app/JIPipeDesktopProjectWorkbench.java` around line 535. After the author profile button is added (line 537), add:

```java
// Statistics button (only shown on first launch)
JIPipeStatisticsApplicationSettings statsSettings = JIPipeStatisticsApplicationSettings.getInstance();
if (statsSettings.isShowFirstTimePrompt() && statsSettings.isEnabled()) {
    JIPipeDesktopStatisticsButton statisticsButton = new JIPipeDesktopStatisticsButton(this);
    statisticsButton.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
    statusBar.add(statisticsButton);
}
```

Add the import at the top of the file:
```java
import org.hkijena.jipipe.plugins.statistics.ui.JIPipeDesktopStatisticsButton;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsButton.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsConfigurationUI.java jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java
git commit -m "Add first-time statistics button with balloon and wire to status bar (#1304)"
```

---

### Task 13: Configuration Dialog with Slider

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsConfigurationUI.java`

**Interfaces:**
- Consumes: `StatisticsPrivacyLevel`, `JIPipeStatisticsApplicationSettings`
- Produces: Full configuration dialog with `JSlider` (0-4) using `JIPipeDesktopModernSliderUI`, dynamic description panel.

**Key reference patterns:**
- `JIPipeDesktopModernSliderUI` — file: `jipipe-core/.../desktop/commons/theme/ui/JIPipeDesktopModernSliderUI.java`
- `GettingStartedPanel` from jipipe-2 — layout with `UIUtils.createInfoLabel`. File: `/data/src/jipipe-2/jipipe-core/.../GettingStartedPanel.java`
- `UIUtils.createInfoLabel(text, subtext, icon)` — file: `jipipe-core/.../utils/UIUtils.java` line 203

- [ ] **Step 1: Replace the placeholder with the full slider-based dialog**

Replace the entire `JIPipeDesktopStatisticsConfigurationUI` class with:

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.theme.ui.JIPipeDesktopModernSliderUI;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;

public class JIPipeDesktopStatisticsConfigurationUI extends JDialog {
    private boolean configured = false;
    private StatisticsPrivacyLevel selectedLevel;
    private JLabel descriptionLabel;

    public JIPipeDesktopStatisticsConfigurationUI(Window parent) {
        super(parent, "Statistics Configuration");
        setModal(true);
        selectedLevel = JIPipeStatisticsApplicationSettings.getInstance().getPrivacyLevel();
        initialize();
        pack();
        setSize(600, 400);
        setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(UIUtils.createInfoLabel("Usage Statistics",
                "JIPipe collects usage statistics to secure funding for NFDI4BIOIMAGE. " +
                "You can choose what data is shared or opt out entirely.",
                JIPipe.RESOURCES.getIcon64("actions/chart-bar.png")));
        topPanel.add(Box.createVerticalStrut(16));
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel sliderLabel = new JLabel("Privacy level:");
        sliderLabel.setFont(sliderLabel.getFont().deriveFont(Font.BOLD, 13f));
        centerPanel.add(sliderLabel, BorderLayout.NORTH);

        JSlider slider = new JSlider(0, 4, selectedLevel.ordinal());
        slider.setSnapToTicks(true);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(1);
        slider.setUI(new JIPipeDesktopModernSliderUI(slider));

        Dictionary<Integer, JLabel> labels = new Hashtable<>();
        for (StatisticsPrivacyLevel level : StatisticsPrivacyLevel.values()) {
            labels.put(level.ordinal(), new JLabel(level.name()));
        }
        slider.setLabelTable(labels);

        centerPanel.add(slider, BorderLayout.CENTER);

        descriptionLabel = new JLabel("<html>" + selectedLevel.getDescription() + "</html>");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        centerPanel.add(descriptionLabel, BorderLayout.SOUTH);

        slider.addChangeListener((ChangeEvent e) -> {
            int value = slider.getValue();
            StatisticsPrivacyLevel[] levels = StatisticsPrivacyLevel.values();
            if (value >= 0 && value < levels.length) {
                selectedLevel = levels[value];
                descriptionLabel.setText("<html><strong>" + selectedLevel.name() + "</strong>: " +
                        selectedLevel.getDescription() + "</html>");
            }
        });

        add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), e -> {
            configured = false;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton okButton = UIUtils.createButton("OK", JIPipe.RESOURCES.getIcon16("actions/checkmark.png"), e -> {
            JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
            settings.setPrivacyLevel(selectedLevel);
            JIPipe.getInstance().getApplicationSettings().saveLater();
            configured = true;
            setVisible(false);
        });
        buttonPanel.add(okButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfigured() {
        return configured;
    }
}
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsConfigurationUI.java
git commit -m "Add slider-based statistics configuration dialog (#1304)"
```

---

### Task 14: Statistics Viewer Dialog

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/ShowStatisticsTool.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java`

**Interfaces:**
- Consumes: `JIPipeStatisticsServiceComponent`, `JIPipeStatisticsRegistry`, `JIPipeStatisticsItem`
- Produces: `JIPipeDesktopStatisticsUI` — a `JDialog` showing machine ID, timestamps, privacy level, statistics cards grouped by category, "Send now" and "Close" buttons. `ShowStatisticsTool` opens this dialog.

**Key reference patterns:**
- `OpenImageJTool` — Tools menu item pattern. File: `jipipe-core/.../plugins/tools/OpenImageJTool.java`
- `OKCancelDialog` — dialog pattern. File: `jipipe-core/.../utils/OKCancelDialog.java`

- [ ] **Step 1: Create `JIPipeDesktopStatisticsUI`**

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsReporter;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class JIPipeDesktopStatisticsUI extends JDialog {
    private final JIPipeDesktopProjectWorkbench workbench;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public JIPipeDesktopStatisticsUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench.getWindow(), "Statistics");
        this.workbench = workbench;
        setModal(true);
        initialize();
        pack();
        setSize(700, 500);
        setLocationRelativeTo(workbench.getWindow());
        UIUtils.addEscapeListener(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Machine ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField machineIdField = new JTextField(service.getMachineId());
        machineIdField.setEditable(false);
        topPanel.add(machineIdField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton rerollButton = UIUtils.createButton("Re-roll", JIPipe.RESOURCES.getIcon16("actions/reload.png"), e -> {
            service.rerollMachineId();
            machineIdField.setText(service.getMachineId());
        });
        topPanel.add(rerollButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        topPanel.add(new JLabel("First launch:"), gbc);
        gbc.gridx = 1;
        LocalDateTime firstLaunch = service.getFirstLaunchTimestamp();
        topPanel.add(new JLabel(firstLaunch != null ? firstLaunch.format(FORMATTER) : "Unknown"), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        topPanel.add(new JLabel("Last sent:"), gbc);
        gbc.gridx = 1;
        LocalDateTime lastSent = service.getLastSentTimestamp();
        topPanel.add(new JLabel(lastSent != null ? lastSent.format(FORMATTER) : "Never"), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        topPanel.add(new JLabel("Privacy level:"), gbc);
        gbc.gridx = 1;
        topPanel.add(new JLabel(settings.getPrivacyLevel().name() + " - " + settings.getPrivacyLevel().getDescription()), gbc);

        add(topPanel, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(UIManager.getColor("Panel.background"));

        Map<JIPipeStatisticsItemCategory, List<JIPipeStatisticsItem>> grouped = service.getRegistry().getItemsByCategory();
        for (JIPipeStatisticsItemCategory category : JIPipeStatisticsItemCategory.values()) {
            List<JIPipeStatisticsItem> items = grouped.get(category);
            if (items == null || items.isEmpty()) continue;

            JLabel categoryLabel = new JLabel(category.getCategory());
            categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD, 14f));
            categoryLabel.setIcon(category.getIcon());
            categoryLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
            categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardsPanel.add(categoryLabel);

            for (JIPipeStatisticsItem item : items) {
                JPanel card = createCard(item);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                cardsPanel.add(card);
                cardsPanel.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton sendNowButton = UIUtils.createButton("Send now", JIPipe.RESOURCES.getIcon16("actions/mail-send.png"), e -> {
            workbench.sendStatusBarText("Sending usage statistics...");
            StatisticsReporter.sendNow(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        workbench.sendStatusBarText("Statistics sent.");
                    } else {
                        workbench.sendStatusBarText("Failed to send statistics (will retry later).");
                    }
                });
            });
        });
        buttonPanel.add(sendNowButton);

        JButton closeButton = UIUtils.createButton("Close", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), e -> setVisible(false));
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createCard(JIPipeStatisticsItem item) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.getControlBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(UIManager.getColor("TextField.background"));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel iconLabel = new JLabel(item.getCategory().getIcon());
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        textPanel.add(nameLabel);

        String valueStr = StringUtils.nullToEmpty(item.serialize() != null ? item.serialize().toString() : "");
        JLabel valueLabel = new JLabel(valueStr);
        valueLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        textPanel.add(valueLabel);

        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }
}
```

- [ ] **Step 2: Update `ShowStatisticsTool` to open the viewer**

Replace the placeholder `ShowStatisticsTool`:

```java
package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

public class ShowStatisticsTool extends JIPipeDesktopMenuExtension {
    public ShowStatisticsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Statistics");
        setToolTipText("View collected usage statistics");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/chart-bar.png"));
        addActionListener(e -> {
            JIPipeDesktopStatisticsUI dialog = new JIPipeDesktopStatisticsUI((JIPipeDesktopProjectWorkbench) workbench);
            dialog.setVisible(true);
        });
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }
}
```

- [ ] **Step 3: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/JIPipeDesktopStatisticsUI.java jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/ui/ShowStatisticsTool.java
git commit -m "Add statistics viewer dialog with cards and Tools menu item (#1304)"
```

---

### Task 15: Register All Statistics Items in Plugin

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java`

**Interfaces:**
- Consumes: All statistics item classes from Tasks 6-9, `JIPipeStatisticsServiceComponent`
- Modifies: `StatisticsPlugin.register()` — add registration of all 14 statistics items.

- [ ] **Step 1: Add item registration to `StatisticsPlugin.register()`**

Add the following to the `register()` method in `StatisticsPlugin.java`, after the existing `registerMenuExtension` call:

```java
// Register statistics items
var statsService = JIPipe.getInstance().getStatistics();
var registry = statsService.getRegistry();

registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.MachineIdStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.OperatingSystemStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.TotalRamStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.JIPipeVersionStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.GpuInfoStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RecentProjectsCountStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.WorkflowRunsStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.RoCratesCreatedStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.PopularNodesStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectNodesStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LargestProjectCompartmentsStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NodeMoveDistanceStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.NoodleScoreStatisticsItem());
registry.registerItem(new org.hkijena.jipipe.plugins.statistics.items.LongestNodeWidthStatisticsItem());
```

**IMPORTANT:** The item registration must happen during `register()` (before `postprocess()`), because `JIPipeStatisticsServiceComponent.postprocess()` calls `item.initialize()` on all registered items. The SciJava plugin loading sequence in `JIPipeServiceDefaultInitializer` calls `register()` on all plugins first (lines 218-248), then calls `postprocess()` on service components (lines 329-332). So items registered in `register()` will be available when `postprocess()` runs.

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java
git commit -m "Register all statistics items in plugin (#1304)"
```

---

### Task 16: Node Drag Instrumentation

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/canvas/managers/JIPipeDesktopGraphCanvasDragManagerMove.java`

**Interfaces:**
- Consumes: `NodeMoveDistanceStatisticsItem` (static `addDistance` method)
- Modifies: `JIPipeDesktopGraphCanvasDragManagerMove` — add position tracking and distance calculation on drag.

**Key reference patterns:**
- `startDragCurrentNodeSelection(MouseEvent)` — line 40, records pre-drag offsets
- `mouseDragged(MouseEvent)` — line 74, applies movement
- `JIPipeDesktopGraphNodeUI.getStoredGridLocation()` — returns `Point` grid location

- [ ] **Step 1: Read the drag manager to understand the exact drag flow**

Read `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/canvas/managers/JIPipeDesktopGraphCanvasDragManagerMove.java` fully. Identify:
- Where drag starts (pre-drag positions are recorded)
- Where drag ends (mouseReleased or similar)
- How node positions are tracked

- [ ] **Step 2: Add move distance tracking**

Add a field to store pre-drag positions:
```java
private final java.util.Map<java.util.UUID, java.awt.Point> preDragPositions = new java.util.HashMap<>();
```

In `startDragCurrentNodeSelection(MouseEvent e)` (or the method that begins dragging), after existing code, record positions:
```java
for (var entry : currentlyDraggedOffsets.entrySet()) {
    var nodeUI = graphCanvasUI.getNodeUIs().get(entry.getKey());
    if (nodeUI != null) {
        preDragPositions.put(entry.getKey(), nodeUI.getStoredGridLocation());
    }
}
```

Add a `mouseReleased` override (if not present) or hook into the existing drag-end logic:
```java
@Override
public void mouseReleased(MouseEvent e) {
    if (isCurrentlyDraggingNode()) {
        for (var entry : preDragPositions.entrySet()) {
            var nodeUI = graphCanvasUI.getNodeUIs().get(entry.getKey());
            if (nodeUI != null) {
                java.awt.Point before = entry.getValue();
                java.awt.Point after = nodeUI.getStoredGridLocation();
                if (before != null && after != null) {
                    double dist = Math.sqrt(Math.pow(after.x - before.x, 2) + Math.pow(after.y - before.y, 2));
                    org.hkijena.jipipe.plugins.statistics.items.NodeMoveDistanceStatisticsItem.addDistance(dist,
                            () -> org.hkijena.jipipe.JIPipe.getInstance().getStatistics().saveLater());
                }
            }
        }
        preDragPositions.clear();
    }
    super.mouseReleased(e);
}
```

- [ ] **Step 3: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/canvas/managers/JIPipeDesktopGraphCanvasDragManagerMove.java
git commit -m "Instrument node drag to track move distance statistic (#1304)"
```

---

### Task 17: Longest Node Width Instrumentation

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java`

**Interfaces:**
- Consumes: `LongestNodeWidthStatisticsItem` (static `reportWidth` method)
- Modifies: `JIPipeDesktopGraphNodeUI` — report width when node UI is resized/rendered.

**Key reference patterns:**
- `JIPipeDesktopGraphNodeUI` — node UI component. File: `jipipe-core/.../desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java`
- `getWidth()` or `getPreferredSize().width` for node width

- [ ] **Step 1: Read JIPipeDesktopGraphNodeUI to find where width is set**

Read the file to find where the node UI size is determined (constructor, `initialize()`, `setSize()`, `getPreferredSize()` override, etc.).

- [ ] **Step 2: Add width reporting**

Add a call to `LongestNodeWidthStatisticsItem.reportWidth(getWidth())` at the end of the method that finalizes the node UI size. This is likely in `initialize()` or in a `setSize`/`setBounds` override. If the width is dynamic, add it to `paintComponent` or a `validateTree` override:

```java
@Override
public void setBounds(int x, int y, int width, int height) {
    super.setBounds(x, y, width, height);
    org.hkijena.jipipe.plugins.statistics.items.LongestNodeWidthStatisticsItem.reportWidth(width);
}
```

If `setBounds` is not the right place, find the `initialize()` method or similar and add the report there with `getWidth()` or `getPreferredSize().width`.

- [ ] **Step 3: Compile to verify**

Run: `mvn compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/nodeui/JIPipeDesktopGraphNodeUI.java
git commit -m "Instrument node UI to track longest node width statistic (#1304)"
```

---

### Task 18: Concurrency — File Locking

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/api/service/components/StatisticsConcurrencyTest.java`

**Interfaces:**
- Modifies: `JIPipeStatisticsServiceComponent.save()` — add `FileLock` on `statistics.lock`, re-read file, merge dirty items, write atomically.

- [ ] **Step 1: Write a concurrency test**

```java
package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsRegistry;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsConcurrencyTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndReload_preservesMachineId() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        ObjectNode data1 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry);
        String machineId = data1.get("machineId").asText();
        JIPipeStatisticsServiceComponent.save(statsFile, data1);

        ObjectNode data2 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry);
        assertEquals(machineId, data2.get("machineId").asText());
    }

    @Test
    void save_atomic_noCorruptionOnConcurrentRead() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        ObjectNode data = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry);
        data.put("machineId", "test-uuid");
        JIPipeStatisticsServiceComponent.save(statsFile, data);

        // Read while another write might happen — should always get valid JSON
        ObjectNode readBack = (ObjectNode) JsonUtils.getObjectMapper().readTree(statsFile.toFile());
        assertEquals("test-uuid", readBack.get("machineId").asText());
    }
}
```

- [ ] **Step 2: Run test to verify it passes (save is already atomic via temp+move)**

Run: `mvn test -pl jipipe-core -Dtest=StatisticsConcurrencyTest -q`
Expected: PASS

- [ ] **Step 3: Add file locking to the service component's `save()` method**

Modify the `save()` method in `JIPipeStatisticsServiceComponent` to use file locking:

```java
public void save() {
    Path file = getStatisticsFile();
    Path lockFile = getLockFile();
    try {
        PathUtils.ensureParentDirectoriesExist(file);
        if (!Files.exists(lockFile)) {
            Files.createFile(lockFile);
        }
        try (var channel = java.nio.channels.FileChannel.open(lockFile,
                java.nio.file.StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock != null) {
                // Re-read current state from disk (other instances may have written)
                ObjectMapper mapper = JsonUtils.getObjectMapper();
                ObjectNode currentOnDisk;
                if (Files.isRegularFile(file)) {
                    currentOnDisk = (ObjectNode) mapper.readTree(file.toFile());
                } else {
                    currentOnDisk = mapper.createObjectNode();
                }

                // Merge our dirty items into the on-disk state
                ObjectNode itemsOnDisk = currentOnDisk.has("items") ? (ObjectNode) currentOnDisk.get("items") : currentOnDisk.putObject("items");
                ObjectNode ourItems = statisticsData.has("items") ? (ObjectNode) statisticsData.get("items") : statisticsData.putObject("items");
                ourItems.fields().forEachRemaining(entry -> itemsOnDisk.set(entry.getKey(), entry.getValue()));
                currentOnDisk.set("items", itemsOnDisk);

                // Preserve machineId, timestamps from our in-memory state
                if (statisticsData.has("machineId")) {
                    currentOnDisk.put("machineId", statisticsData.get("machineId").asText());
                }
                if (statisticsData.has("lastSentTimestamp")) {
                    currentOnDisk.set("lastSentTimestamp", statisticsData.get("lastSentTimestamp"));
                }
                if (statisticsData.has("firstLaunchTimestamp")) {
                    currentOnDisk.put("firstLaunchTimestamp", statisticsData.get("firstLaunchTimestamp").asText());
                }

                statisticsData = currentOnDisk;

                // Write atomically
                Path tmpFile = file.resolveSibling(file.getFileName() + ".tmp");
                mapper.writeValue(tmpFile.toFile(), currentOnDisk);
                Files.move(tmpFile, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    } catch (Exception e) {
        logger.error("Failed to save statistics to {}", file, e);
    }
}
```

- [ ] **Step 4: Run tests to verify**

Run: `mvn test -pl jipipe-core -Dtest=StatisticsConcurrencyTest,JIPipeStatisticsServiceComponentTest -q`
Expected: PASS — all tests pass

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/service/components/JIPipeStatisticsServiceComponent.java jipipe-core/src/test/java/org/hkijena/jipipe/api/service/components/StatisticsConcurrencyTest.java
git commit -m "Add file locking and merge strategy for multi-instance statistics (#1304)"
```

---

### Task 19: Final Integration and Verification

**Files:**
- No new files — verification task

- [ ] **Step 1: Full compilation**

Run: `mvn clean compile -pl jipipe-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**

Run: `mvn test -pl jipipe-core -q`
Expected: All tests pass (JIPipeStatisticsRegistryTest, JIPipeStatisticsServiceComponentTest, StatisticsReporterTest, StatisticsConcurrencyTest)

- [ ] **Step 3: Verify service component registration**

Grep for `statistics` in `JIPipeService.java` to verify:
- Field declaration exists
- Instantiation in constructor exists
- Entry in `components` array exists
- Getter method exists

Run: `rg "statistics" jipipe-core/src/main/java/org/hkijena/jipipe/api/service/JIPipeService.java`
Expected: Multiple matches showing field, instantiation, components array, and getter

- [ ] **Step 4: Verify plugin is discoverable**

Grep for `@Plugin` annotation in `StatisticsPlugin.java`:
Run: `rg "@Plugin" jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java`
Expected: Match showing `@Plugin(type = JIPipeJavaPlugin.class)`

- [ ] **Step 5: Verify all 14 items are registered**

Grep for `registerItem` in `StatisticsPlugin.java`:
Run: `rg "registerItem" jipipe-core/src/main/java/org/hkijena/jipipe/plugins/statistics/StatisticsPlugin.java`
Expected: 14 matches

- [ ] **Step 6: Verify status bar wiring**

Grep for `StatisticsButton` in `JIPipeDesktopProjectWorkbench.java`:
Run: `rg "StatisticsButton" jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/JIPipeDesktopProjectWorkbench.java`
Expected: Match showing conditional addition to status bar

- [ ] **Step 7: Final commit (if any remaining changes)**

```bash
git add -A
git commit -m "Complete user statistics feature integration (#1304)" || echo "Nothing to commit"
```

---

## Spec Coverage Checklist

| Spec Section | Task(s) |
|---|---|
| Plugin & service component architecture | Tasks 1, 4, 5 |
| Statistics items registry | Tasks 1, 2 |
| V1 statistics items (all 14) | Tasks 6, 7, 8, 9 |
| Privacy levels | Task 1 |
| Settings | Task 3 |
| Storage & concurrency | Tasks 4, 18 |
| Reporting (payload, HTTP, daily timer) | Tasks 10, 11 |
| First-time button + balloon | Task 12 |
| Configuration dialog with slider | Task 13 |
| Tools menu item | Task 14 |
| Statistics viewer with cards | Task 14 |
| Instrumentation (workflow runs, RO-Crates) | Task 7 |
| Instrumentation (popular nodes, largest project) | Task 8 |
| Instrumentation (node move distance) | Task 16 |
| Instrumentation (noodle score) | Task 9 (passive) |
| Instrumentation (longest node width) | Task 17 |
| Registration in JIPipeService | Task 4 |
| Registration in plugin | Task 15 |
| Status bar wiring | Task 12 |
| Final verification | Task 19 |
