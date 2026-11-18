# Structured UI operations and synthetic input - Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give AI agents a deterministic, permission-gated way to operate the JIPipe UI - fire registered actions by id, control dock panels, set composite parameter values, and (for custom-painted surfaces such as the coming 3D viewers) deliver coordinate input that is validated to JIPipe's own windows.

**Architecture:** Two layers on top of the existing automation operation layer. Layer 1 wraps primitives the codebase already has (the public `resolveAction` dispatcher, dock panel ids, the parameter model), plus id assignments for 14 context actions that lack them and composite value conversions in `convertParameterValue`. Layer 2 is a small new input package: a pure, testable target/window resolver and a two-mode event injector (AWT `dispatchEvent` on the EDT by default, `Robot` on a bounded off-EDT executor on request). `ui_probe` joins the two layers by naming the Layer 1 operation for any probed element that has an identity.

**Tech Stack:** Java 21, Swing/AWT (MouseEvent, MouseWheelEvent, KeyEvent, `Component.getComponentAt`, `Window.getWindows`, `java.awt.Robot`), JUnit 5 (pure helpers only), Jackson (`ObjectNode` results), the existing `AutomationOperation` / `AutomationPlugin` registry.

**Spec:** `docs/superpowers/specs/2026-11-18-wi1379-ui-operations-design.md`

## Global Constraints

- No em dashes or en dashes anywhere (code, comments, javadoc, strings). Regular hyphen or colon instead.
- Stable string-literal ids only. Operation ids: `fire_action`, `list_ui_actions`, `set_dock_visible`, `list_docks`, `ui_probe`, `ui_click`, `ui_drag`, `ui_scroll`, `ui_type_text`. Shortcut action ids: kebab-case under `org.hkijena.jipipe.graph.node.*` (see Task 2's exact list).
- Kebab-case operation input parameters; snake_case result keys.
- Every operation schema property carries a non-empty `description` (enforced by `AutomationOperationsSchemaTest`); `additionalProperties: false`.
- Coordinate input is accepted only inside showing `java.awt.Window` instances of the JIPipe JVM. Points outside are refused with the allowed rectangles.
- `input-mode` is `dispatch` (default) or `system`. `system` refuses when `GraphicsEnvironment.isHeadless()`.
- No committed test may deliver synthetic input events (headless CI policy). Only pure helpers get tests.
- Permission classification: `fire_action`, `ui_click`, `ui_drag`, `ui_scroll`, `ui_type_text` are Modify (Ask by default); `list_ui_actions`, `list_docks`, `ui_probe` are Query read-only; `set_dock_visible` is Query read-only (view state only, the `navigate_to_node` precedent).
- Never dispatch subagents from inside an implementation task.
- Branch: `1379-agentic-interactions`. Do not push.

## Review Focus

Inputs and conditions the spec implies but no planned test exercises, each owned by the task named:

1. **A point inside a JIPipe window but outside any component** (e.g. the window's background margin): the probe and the input operations must still resolve the window and act, not refuse - owned by Task 4 (window resolver returns the window for any in-bounds point) and Task 5's live steps.
2. **A point over a node badge or slot in the graph canvas**: the probe must name the Layer 1 action instead of inviting a raw click - owned by Task 6, tested at the pure level via a resolution table (point + canvas pick result -> suggested operation).
3. **`ui_type_text` with no focus owner, or focus outside JIPipe**: must refuse with a clear reason instead of typing into nothing - owned by Task 7.
4. **`input-mode: "system"` while headless**: must refuse clearly, never throw a bare `AWTException` through the operation envelope - owned by Task 5.
5. **A malformed composite parameter value** (bad hex color, expression text the type rejects): must produce an error naming the accepted encoding, never a silent mis-set - owned by Task 1.

---

## File Structure

| File | Responsibility |
|---|---|
| `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationUtils.java` (modify) | + `parseCompositeValue` helpers for the four composite types (pure, static; used by `convertParameterValue`) |
| `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (modify) | `convertParameterValue` gains the composite branch that calls `AutomationOperationUtils.parseCompositeValue` |
| `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutActions.java` (modify) | 14 new `JIPipeShortcutAction` constants + registration in `CORE_ACTIONS` |
| `.../grapheditor/commons/contextmenu/**` (modify, 14 files) | each id-less context action gains `getActionId()` returning its constant |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentWindowResolver.java` (create) | pure window/point resolution + allowed rectangles (Layer 2) |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentElementProbe.java` (create) | pure element-at-point resolution + Layer 1 cross-reference (Layer 2 probe) |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentInputDispatch.java` (create) | dispatch-mode event construction + delivery on the EDT |
| `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentInputSystem.java` (create) | Robot mode on a bounded off-EDT executor |
| `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UiInteractionOperations.java` (create) | the 9 operations |
| `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java` (modify) | registrations |
| `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationUtilityHelpersTest.java` (create) | composite conversion tests |
| `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/ContextActionIdsTest.java` (create) | every context action exposes a registered id |
| `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/ui/input/AgentWindowResolverTest.java` (create) | window/point resolution table |
| `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/ui/input/AgentElementProbeTest.java` (create) | probe-to-Layer-1 suggestion table |
| `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationOperationsSchemaTest.java` (modify) | presence gate gains the 9 ids |

---

### Task 1: Composite parameter conversions

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationUtils.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java` (the final `return value;` branch of `convertParameterValue`, currently line 476)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationUtilityHelpersTest.java`

**Interfaces:**
- Produces: `public static Object parseCompositeValue(String value, Class<?> fieldClass) throws IllegalArgumentException` in `AutomationOperationUtils`. Returns a non-null decoded object when `fieldClass` is a supported composite type, and `null` when the type is not supported (the caller keeps the raw string). Supported: `org.hkijena.jipipe.plugins.parameters.api.collections`-free leaves listed below.

- [ ] **Step 1: Find the real composite parameter classes and their parse entry points**

Read each class and note the exact factory/parse method (do not guess; the plan's claims are verified against these):
`org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter`, `org.hkijena.jipipe.plugins.parameters.api.collections` colour parameter class used by node parameters (search `Color` among `JIPipeParameter` subclasses), the quantity parameter class (search `class .*Quantity.*Parameter`), and `JIPipeDynamicEnumParameter`.

Record in your report: for each, the class FQN and the exact method that turns a string into an instance.

- [ ] **Step 2: Write the failing tests**

```java
package org.hkijena.jipipe.api.automation;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class AutomationUtilityHelpersTest {

    @Test
    void unsupportedTypeReturnsNull() {
        assertNull(AutomationOperationUtils.parseCompositeValue("x", String[].class));
    }

    @Test
    void colorAcceptsSixDigitHex() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("#FF8000", Color.class);
        assertEquals(new Color(255, 128, 0), parsed);
    }

    @Test
    void colorAcceptsEightDigitHexWithAlpha() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("#80FF8000", Color.class);
        assertEquals(new Color(255, 128, 0, 128), parsed);
    }

    @Test
    void malformedColorNamesTheAcceptedEncoding() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AutomationOperationUtils.parseCompositeValue("orange-ish", Color.class));
        assertTrue(e.getMessage().contains("#RRGGBB"));
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationUtilityHelpersTest`
Expected: FAIL at compile time, `parseCompositeValue` does not exist.

- [ ] **Step 4: Implement `parseCompositeValue`**

Signature as in Interfaces. Order: `Color` first (8-digit then 6-digit hex, case-insensitive, message must contain `#RRGGBB` and `#AARRGGBB`), then expression, quantity, dynamic enum (using the parse entry points found in Step 1), then `return null`. Never throw for an unsupported type; throw `IllegalArgumentException` only for a supported type with an invalid value.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest=AutomationUtilityHelpersTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Wire it into `convertParameterValue`**

Replace the final `return value;` with a `parseCompositeValue` call that returns the decoded object when non-null, otherwise the raw string. Keep every existing branch untouched.

- [ ] **Step 7: Add the expression, quantity, and dynamic-enum assertions**

Extend `AutomationUtilityHelpersTest` with one test per supported type: a valid value decodes to the expected instance, and a malformed value throws an `IllegalArgumentException` whose message names the accepted encoding. Use the parse entry points recorded in Step 1.

- [ ] **Step 8: Run the full helper test and the existing automation tests**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationUtilityHelpersTest,DataOperationsTest,MCPBrokerServerTest'`
Expected: all PASS.

- [ ] **Step 9: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/AutomationOperationUtils.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/JIPipeAutomationAPI.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationUtilityHelpersTest.java
git commit -m "feat(#1379): composite parameter value conversions for set_parameter"
```

---

### Task 2: Stable ids for the 14 id-less context actions

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutActions.java`
- Modify: 14 files under `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/` (those listed in Interfaces)
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/ContextActionIdsTest.java`

**Interfaces:**
- Produces: 14 new `public static final JIPipeShortcutAction` constants with these exact ids, names, and categories (no default chords - these are menu-only actions):

| Constant | id | name | category |
|---|---|---|---|
| `NODE_SAVE_OUTPUTS_ENABLE` | `org.hkijena.jipipe.graph.node.save-outputs-enable` | `Enable save outputs` | `Node` |
| `NODE_SAVE_OUTPUTS_DISABLE` | `org.hkijena.jipipe.graph.node.save-outputs-disable` | `Disable save outputs` | `Node` |
| `NODE_VIRTUAL_OUTPUTS_ENABLE` | `org.hkijena.jipipe.graph.node.virtual-outputs-enable` | `Enable virtual outputs` | `Node` |
| `NODE_VIRTUAL_OUTPUTS_DISABLE` | `org.hkijena.jipipe.graph.node.virtual-outputs-disable` | `Disable virtual outputs` | `Node` |
| `RUN_AND_SHOW_INTERMEDIATE_RESULTS` | `org.hkijena.jipipe.graph.run-and-show-intermediate-results` | `Run and show intermediate results` | `Run` |
| `NODE_INVERT_SELECTION` | `org.hkijena.jipipe.graph.node.invert-selection` | `Invert selection` | `Selection` |
| `NODE_ADD_EDGE_CONTROL_POINT` | `org.hkijena.jipipe.graph.node.add-edge-control-point` | `Add edge control point` | `Node` |
| `NODE_GROUP` | `org.hkijena.jipipe.graph.node.group` | `Group` | `Node` |
| `NODE_COLLAPSE_IO_INTERFACE` | `org.hkijena.jipipe.graph.node.collapse-io-interface` | `Collapse IO interface` | `Node` |
| `COMPARTMENT_EXPORT_JSON` | `org.hkijena.jipipe.graph.compartment.export-json` | `Export compartment as JSON` | `Node` |
| `NODE_OPEN_CACHE_BROWSER` | `org.hkijena.jipipe.graph.node.open-cache-browser` | `Open cache browser` | `Cache` |
| `NODE_CLEAR_CACHE` | `org.hkijena.jipipe.graph.node.clear-cache` | `Clear cache` | `Cache` |
| `BOOKMARK_ADD` | `org.hkijena.jipipe.graph.bookmark.add` | `Add bookmark` | `Navigation` |
| `BOOKMARK_REMOVE` | `org.hkijena.jipipe.graph.bookmark.remove` | `Remove bookmark` | `Navigation` |

`NodeContextActionWrapperUIContextAction` is a wrapper and gets no id.

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.api.shortcuts;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ContextActionIdsTest {

    @Test
    void everyCoreContextActionExposesARegisteredId() {
        for (JIPipeShortcutAction action : JIPipeShortcutActions.CORE_ACTIONS) {
            assertNotNull(action.id(), action.name());
            assertFalse(action.id().isBlank(), action.name());
        }
    }

    @Test
    void newNodeActionIdsAreRegisteredAfterRegisterCore() {
        JIPipeShortcutActions.registerCore();
        List<String> ids = List.of(
                "org.hkijena.jipipe.graph.node.save-outputs-enable",
                "org.hkijena.jipipe.graph.node.save-outputs-disable",
                "org.hkijena.jipipe.graph.node.virtual-outputs-enable",
                "org.hkijena.jipipe.graph.node.virtual-outputs-disable",
                "org.hkijena.jipipe.graph.run-and-show-intermediate-results",
                "org.hkijena.jipipe.graph.node.invert-selection",
                "org.hkijena.jipipe.graph.node.add-edge-control-point",
                "org.hkijena.jipipe.graph.node.group",
                "org.hkijena.jipipe.graph.node.collapse-io-interface",
                "org.hkijena.jipipe.graph.compartment.export-json",
                "org.hkijena.jipipe.graph.node.open-cache-browser",
                "org.hkijena.jipipe.graph.node.clear-cache",
                "org.hkijena.jipipe.graph.bookmark.add",
                "org.hkijena.jipipe.graph.bookmark.remove");
        for (String id : ids) {
            assertNotNull(JIPipeShortcutRegistry.getInstance().byId(id), id);
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=ContextActionIdsTest`
Expected: the second test FAILS on the first unregistered id.

- [ ] **Step 3: Add the 14 constants**

In `JIPipeShortcutActions`, copy the shape of `NODE_RAISE` (`:461`): `JIPipeShortcutAction.builder(<id>, <name>, <category>).description("<one sentence>").build()`. No `defaultChord`. Add all 14 to `CORE_ACTIONS` (`:580`).

- [ ] **Step 4: Wire each context action to its constant**

In each of the 14 context action classes, add `public static final String SHORTCUT_ACTION_ID = JIPipeShortcutActions.<CONSTANT>.id();` and make `getActionId()` return it, mirroring `DisableNodeUIContextAction.java:32/74`.

- [ ] **Step 5: Run to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest='ContextActionIdsTest,JIPipeShortcutRegistryTest,TaxonomyFullValidationTest'`
Expected: PASS. If no `JIPipeShortcutRegistryTest` exists, run `ContextActionIdsTest` plus any test class under `api/shortcuts/`.

- [ ] **Step 6: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/shortcuts/JIPipeShortcutActions.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/contextmenu/ \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/shortcuts/ContextActionIdsTest.java
git commit -m "feat(#1379): assign stable ids to the remaining graph context actions"
```

---

### Task 3: Window and point resolution (Layer 2, pure)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentWindowResolver.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/ui/input/AgentWindowResolverTest.java`

**Interfaces:**
- Produces:
  - `public static Window resolveWindow(List<? extends Window> candidates, Point screenPoint)` - the topmost showing window whose on-screen bounds contain the point; `null` when none.
  - `public static List<Rectangle> allowedRectangles(List<? extends Window> candidates)` - the on-screen bounds of every showing window, in the list order.
  - `public static boolean isInside(Window window, Point screenPoint)`.
- Consumed by: Task 4 (probe), Task 5 (dispatch), Task 6 (operations).

- [ ] **Step 1: Write the failing test with a resolution table**

Use lightweight fake windows (`java.awt.Window` cannot be instantiated without a display; use a test double implementing the two methods the resolver needs, so the resolver takes a functional interface instead: define `public record WindowBounds(Rectangle onScreenBounds, boolean showing)` **inside** `AgentWindowResolver` and have the resolver work on `List<WindowBounds>` - this keeps the helper headless-testable; the operations adapt real `Window`s to it via a static `of(Window)` adapter).

```java
package org.hkijena.jipipe.desktop.app.ui.input;

import org.junit.jupiter.api.Test;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AgentWindowResolverTest {
    private static final AgentWindowResolver.WindowBounds A =
            new AgentWindowResolver.WindowBounds(new Rectangle(0, 0, 100, 100), true);
    private static final AgentWindowResolver.WindowBounds B =
            new AgentWindowResolver.WindowBounds(new Rectangle(50, 50, 100, 100), true);
    private static final AgentWindowResolver.WindowBounds HIDDEN =
            new AgentWindowResolver.WindowBounds(new Rectangle(0, 0, 1000, 1000), false);

    @Test
    void resolvesTheTopmostShowingWindowUnderThePoint() {
        AgentWindowResolver.WindowBounds hit =
                AgentWindowResolver.resolve(List.of(A, B), new Point(60, 60));
        assertSame(B, hit);
    }

    @Test
    void singleWindowHitWhereOnlyOneContains() {
        assertSame(A, AgentWindowResolver.resolve(List.of(A, B), new Point(10, 10)));
    }

    @Test
    void hiddenWindowsAreNeverResolved() {
        assertNull(AgentWindowResolver.resolve(List.of(HIDDEN), new Point(10, 10)));
    }

    @Test
    void outsideAllWindowsResolvesNothing() {
        assertNull(AgentWindowResolver.resolve(List.of(A, B), new Point(500, 500)));
    }

    @Test
    void pointInsideAWindowButOutsideAnyComponentStillResolvesTheWindow() {
        assertSame(A, AgentWindowResolver.resolve(List.of(A), new Point(2, 98)));
    }

    @Test
    void allowedRectanglesListsShowingWindowsOnly() {
        assertEquals(List.of(A.onScreenBounds(), B.onScreenBounds()),
                AgentWindowResolver.allowedRectangles(List.of(A, HIDDEN, B)));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AgentWindowResolverTest`
Expected: FAIL, classes do not exist.

- [ ] **Step 3: Implement the resolver**

`WindowBounds` record; `resolve` iterating in reverse list order (topmost last, as `Window.getWindows()` orders it) and returning the first containing showing bounds; `allowedRectangles` filtering `showing` and mapping to `onScreenBounds`; `isInside`. Plus `public static WindowBounds of(Window window)` returning `new WindowBounds(window.getBounds(), window.isShowing())`. The operations convert the screen point into the window with `SwingUtilities.convertPointFromScreen` and use `getComponentAt` on the decorated window (the root pane handles the decoration insets).

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AgentWindowResolverTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentWindowResolver.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/ui/input/AgentWindowResolverTest.java
git commit -m "feat(#1379): agent window and point resolution helper"
```

---

### Task 4: Element probe with Layer 1 cross-reference (Layer 2, pure)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentElementProbe.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/ui/input/AgentElementProbeTest.java`

**Interfaces:**
- Produces:
  - `public record ProbeHit(String kind, String detail, String suggestedOperation, String suggestedArguments)` with `kind` in `canvas-node`, `canvas-slot`, `canvas-badge`, `component`.
  - `public enum CanvasRegion { NODE_BODY, SLOT, RUN_BADGE, INTERNAL }`
  - `public static ProbeHit suggestForCanvasRegion(CanvasRegion region, String nodeId, String slotName)`
  - `public static ProbeHit suggestForComponent(String componentClassNameChain, String accessibleName)` - the dock/action cross-reference. Rules (exact): when the chain contains `JIPipeDesktopDockPanel$Panel`, `kind = "dock"`, `detail` = the accessible name (the dock title), `suggestedOperation = "set_dock_visible"`, `suggestedArguments` = `{"dock-id": "<the id the caller passes in as accessibleName>"}`; when the chain contains a `JButton`/`JToggleButton`/`JMenuItem`, `kind = "component"`, `detail` = the chain, `suggestedOperation = null` (the caller fills in a fireable action when `list_ui_actions` has an id whose label matches the button text - the operation does that lookup, the helper stays pure). Every other chain: `kind = "component"`, no suggestion.
- Consumed by: Task 6 (`ui_probe`).

**Fixed suggestion table (pin exactly these):**

| region | suggestedOperation | suggestedArguments |
|---|---|---|
| `NODE_BODY` | `null` (no operation; the node is addressed by `query_node`/`set_parameter`) | `null` |
| `SLOT` | `null` | `null` |
| `RUN_BADGE` | `run_node` | `{"node": "<nodeId>"}` |
| `INTERNAL` | `null` | `null` |

- [ ] **Step 1: Write the failing test**

```java
package org.hkijena.jipipe.desktop.app.ui.input;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AgentElementProbeTest {

    @Test
    void runBadgeSuggestsRunNodeWithTheNodeId() {
        AgentElementProbe.ProbeHit hit = AgentElementProbe.suggestForCanvasRegion(
                AgentElementProbe.CanvasRegion.RUN_BADGE, "abc-123", null);
        assertEquals("canvas-badge", hit.kind());
        assertEquals("run_node", hit.suggestedOperation());
        assertTrue(hit.suggestedArguments().contains("abc-123"));
    }

    @Test
    void slotReportsItsNameAndSuggestsNothing() {
        AgentElementProbe.ProbeHit hit = AgentElementProbe.suggestForCanvasRegion(
                AgentElementProbe.CanvasRegion.SLOT, "abc-123", "Input 1");
        assertEquals("canvas-slot", hit.kind());
        assertNull(hit.suggestedOperation());
        assertEquals("Input 1", hit.detail());
    }

    @Test
    void nodeBodySuggestsNothing() {
        assertNull(AgentElementProbe.suggestForCanvasRegion(
                AgentElementProbe.CanvasRegion.NODE_BODY, "abc-123", null).suggestedOperation());
    }

    @Test
    void dockPanelSuggestsSetDockVisible() {
        AgentElementProbe.ProbeHit hit = AgentElementProbe.suggestForComponent(
                "JIPipeDesktopDockPanel$Panel", "ADD_NODES");
        assertEquals("dock", hit.kind());
        assertEquals("set_dock_visible", hit.suggestedOperation());
        assertTrue(hit.suggestedArguments().contains("ADD_NODES"));
    }

    @Test
    void plainComponentSuggestsNothing() {
        assertNull(AgentElementProbe.suggestForComponent(
                "JIPipeDesktopGraphCanvasUI", "canvas").suggestedOperation());
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q test -pl jipipe-core -Dtest=AgentElementProbeTest`
Expected: FAIL, class missing.

- [ ] **Step 3: Implement the record, enum, and both mappings**

Straight mapping from the two tables; `detail` = node id for node regions, slot name for `SLOT`, the dock id for a dock panel.

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -q test -pl jipipe-core -Dtest=AgentElementProbeTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentElementProbe.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/ui/input/AgentElementProbeTest.java
git commit -m "feat(#1379): canvas element probe with layer-1 cross-reference"
```

---

### Task 5: Input delivery (dispatch and system modes)

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentInputDispatch.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentInputSystem.java`

**Interfaces:**
- Produces:
  - `public sealed interface AgentInputMode permits AgentInputDispatch, AgentInputSystem` with `String name()`.
  - `AgentInputDispatch.MODE` (`"dispatch"`) implementing: `click(Component target, int x, int y, int clickCount, int button)`, `drag(Component target, int fromX, int fromY, int toX, int toY, int button)`, `scroll(Component target, int x, int y, int amount)`, `typeText(Component focusOwner, String text)` - all building the AWT events and calling `target.dispatchEvent(...)`; callers guarantee the EDT.
  - `AgentInputSystem.MODE` (`"system"`) with the same methods; `system()` runs the Robot sequence on a private 2-thread daemon executor named `jipipe-mcp-input-N` with a bounded 15 s wait, the `captureExecutor` pattern from `JIPipeDesktopMCPIntegration`; `isAvailable()` returns `!GraphicsEnvironment.isHeadless()`.
  - `public static void typeText(Component focusOwner, String text)` in both modes builds `KEY_TYPED` events per char and a `KEY_PRESSED`/`KEY_RELEASED` pair for the Enter-free text (no Enter; text only).
- Consumed by: Task 6.

No committed test (headless CI policy). Verification is the compile step here and the live steps in Task 6.

- [ ] **Step 1: Implement `AgentInputDispatch`**

`click`: `MOUSE_PRESSED` + `MOUSE_RELEASED` (+ `MOUSE_CLICKED`) at the component-local point with `System.currentTimeMillis()` and `InputEvent.BUTTON1_MASK` (`BUTTON3_MASK` for right, `BUTTON2_MASK` for middle); for `clickCount == 2` send a second pair with `clickCount = 2`. Dispatch via `target.dispatchEvent(event)`.
`drag`: `MOUSE_PRESSED` at from, then 10 interpolated `MOUSE_DRAGGED` events, then `MOUSE_RELEASED` at to.
`scroll`: one `MouseWheelEvent` with `WHEEL_UNIT_SCROLL` and the signed `amount` as both rotation and units.
`typeText`: per character, `KEY_TYPED` with `getKeyChar()`; plus `KEY_PRESSED`/`KEY_RELEASED` with `VK_UNDEFINED` to satisfy components that only read key codes.

- [ ] **Step 2: Implement `AgentInputSystem`**

Same surface; inside the executor, `Robot` `mouseMove`/`mousePress`/`mouseRelease`/`mouseWheel` at absolute screen coordinates (add the component's screen location), and `keyPress`/`keyRelease` per character via the `KeyEvent->keycode` table for printable ASCII (lowercase letters = `VK_A`+offset, digits, space, and a small punctuation map; unknown chars throw `IllegalArgumentException("unsupported character for system input: '<c>'")`). `isAvailable()` gate checked by the operation before use.

- [ ] **Step 3: Compile**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentInputDispatch.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/ui/input/AgentInputSystem.java
git commit -m "feat(#1379): dispatch and system input delivery for agent ui operations"
```

---

### Task 6: The nine operations

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UiInteractionOperations.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java`
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationOperationsSchemaTest.java`

**Interfaces:**
- Consumes: `AutomationOperationUtils.parseCompositeValue` (Task 1 does not touch this file), `JIPipeShortcutRegistry`, `JIPipeDesktopShortcutDispatcher.resolveAction(String, Component)`, `JIPipeDesktopDockPanel.getPanels()` (`Map<String, Panel>` with `Panel.getId()/isVisible()/setVisible(boolean)`), `AgentWindowResolver` (Task 3), `AgentElementProbe` (Task 4), `AgentInputDispatch`/`AgentInputSystem` (Task 5), plus the `UiStateOperations` window-resolution chain (`resolveDefaultProjectWindow`, `findProjectWindowByKey` - copy the pattern as that sub-project did, or widen to package-visible if cleaner).
- Produces: nine registered operations.

- [ ] **Step 1: Implement `list_ui_actions` and `list_docks` (simplest first)**

`list_ui_actions` (Query, read-only): `requiresEdt() = true`. Optional `project`. Resolve the target editor via the `get_ui_state` chain; list `JIPipeShortcutRegistry.getInstance().all()` filtered to ids whose `resolveAction(id, editorCanvas)` is non-null, as `{ "actions": [ { "id", "name", "category" } ] }`.
`list_docks`: optional `project`; from the resolved editor's `getDockPanel().getPanels()`, emit `{ "docks": [ { "id", "title", "visible" } ] }` (title from the panel, or the id when the panel exposes no title).

- [ ] **Step 2: Implement `fire_action` and `set_dock_visible`**

`fire_action` (Modify, `isReadOnly() = false`): `requiresEdt() = true`; params `action-id` (required), optional `project`. Resolve the action through the **target editor's** dispatcher scope: obtain the `Component` focus owner of that editor (`editorUI.getCanvasUI()`) and call `JIPipeDesktopShortcutDispatcher`'s `resolveAction` on the dispatcher instance the editor registered (find how the graph editor obtains it: `JIPipeDesktopShortcutDispatcher` is per-window; use the same accessor the editor uses). Null runnable -> error `"No action '<id>' is available in the current editor; use list_ui_actions to see the fireable ids"`. Else `runnable.run()` and return `{ "ok": true, "action_id": "<id>" }`.
`set_dock_visible` (Query class, `isReadOnly() = true`): params `dock-id` (required), optional `project`, optional `visible` (default true). Unknown id -> error listing the available ids.

- [ ] **Step 3: Implement `ui_probe`**

(Query, read-only, `requiresEdt() = true`): params `x`, `y`. Steps on the EDT: build the bounds list (`Window.getWindows()` -> `AgentWindowResolver.of`), `resolve`; null -> error `"The point (<x>, <y>) is not inside a JIPipe window"` with `allowed_rectangles`. Then `SwingUtilities.convertPointFromScreen` into the window, `window.getComponentAt(local)`; if the component is (or is inside) a `JIPipeDesktopGraphCanvasUI`, call `pickNodeUI(mouseEventAt)` and, when a node UI is hit, determine the region (slot via `getSlotActiveArea`, run badge via the node UI's badge bounds, else `NODE_BODY`) and map through `AgentElementProbe.suggestForCanvasRegion`; otherwise `kind = "component"`, `detail` = the component class chain (top three simple names) plus its accessible name. Return `{ "window": "<title>", "kind", "detail", "suggested_operation", "suggested_arguments" }`.

- [ ] **Step 4: Implement `ui_click`, `ui_drag`, `ui_scroll`, `ui_type_text`**

Each: Modify, `isReadOnly() = false`, `requiresEdt() = true` (the operation's execute runs on the EDT via the existing dispatch; the `system` mode hops off the EDT internally). Shared prelude: resolve window (error with `allowed_rectangles` when outside), compute the component under the point (the window itself when `getComponentAt` returns null), then for `dispatch` deliver via `AgentInputDispatch.MODE`; for `system`, check `AgentInputSystem.MODE.isAvailable()` (headless -> error `"input-mode 'system' requires a display"`) and deliver via `AgentInputSystem.MODE`. Response for every call: `{ "ok": true, "input_mode": "<mode>", "window": "<title>", "target_component": "<class chain>", "probe": { <the ui_probe result for the point> } }`. `ui_type_text`: resolve the keyboard focus owner (`KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()`), refuse with `"No keyboard focus owner inside a JIPipe window; click into a field first"` when null or not inside a JIPipe window; for `system` use `AgentInputSystem`, else `AgentInputDispatch`.

- [ ] **Step 5: Register all nine and extend the schema gate**

Add nine `registerAutomationOperation(...)` calls in `AutomationPlugin.register()` under a new `"UI interaction operations"` block, and add the nine ids to `AutomationOperationsSchemaTest.EXPECTED_CORE_OPERATION_IDS`.

- [ ] **Step 6: Write every schema with descriptions**

Each operation's `getInputSchema()` mirrors its parse code exactly, every property carries a `"description"` (agent-oriented, states the flag/consent/limit where relevant, e.g. `ui_click`'s description states "Delivers a click; delivery does not prove the intended effect - re-read state to confirm"), `additionalProperties: false`, kebab-case names.

- [ ] **Step 7: Run the schema + broadcast tests**

Run: `mvn -q test -pl jipipe-core -Dtest='AutomationOperationsSchemaTest,MCPBrokerServerTest'`
Expected: PASS.

- [ ] **Step 8: Live verification (report the observations)**

Start the app (the established launch line with the test project), then through the broker: `list_ui_actions` (non-empty), `list_docks` (contains `ADD_NODES`), `set_dock_visible` on it, `ui_probe` over a node body and over its run badge (the badge must suggest `run_node`), `ui_click` on a harmless area with `input-mode: "dispatch"`, and `ui_type_text` after clicking a search field. Record each response in the report.

- [ ] **Step 9: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/api/automation/operations/UiInteractionOperations.java \
        jipipe-core/src/main/java/org/hkijena/jipipe/plugins/automation/AutomationPlugin.java \
        jipipe-core/src/test/java/org/hkijena/jipipe/api/automation/AutomationOperationsSchemaTest.java
git commit -m "feat(#1379): structured ui operations and synthetic input for agents"
```

---

### Task 7: Whole-suite verification and the spec's Review Focus checks

**Files:**
- No source changes expected; fix anything this task uncovers in the owning task's files, with its tests.

- [ ] **Step 1: Run the full jipipe-core suite**

Run: `mvn -q test -pl jipipe-core`
Expected: 0 failures, 0 errors; only the pre-existing 2 skips.

- [ ] **Step 2: Walk the Review Focus list against the live app**

For each of the five items in the Review Focus section, exercise it live and record the observed behavior in the report: (1) probe at a window margin, (2) probe on a run badge, (3) `ui_type_text` with no focus owner (click the canvas first, then type), (4) `input-mode: "system"` behavior on this Wayland host (bounded, no freeze) and the headless refusal reasoned from the code, (5) a malformed color through `set_parameter` (error names the encoding).

- [ ] **Step 3: Em/en dash sweep and commit any fixes**

Run: `git diff --unified=0 origin/master...HEAD | grep -P '[\x{2013}\x{2014}]' || echo "clean"` (when `origin/master` is not yet an ancestor, compare against the branch point: `git merge-base HEAD origin/1369-mcp-server`)
Expected: `clean`. Commit fixes if any.