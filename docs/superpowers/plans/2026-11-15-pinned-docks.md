# Pinned Docks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** One dock per LEFT/RIGHT/BOTTOM side can be pinned by the user; the pinned dock automatically displays as a stand-in when its side would otherwise show nothing, preventing context docks from collapsing the dock area.

**Architecture:** All pin logic lives in `JIPipeDesktopDockPanel` (jipipe-core `utils/ui`): a per-panel pin map (persisted through `State.pins`), a stand-in display plan computed at the top of `updateAll`, and a runtime pause set. Editors only lose the old always-show/fallback wiring and gain one migration call at each persisted-state entry point. The Tools dock default comes from `SettingsKeys.defaultDockState()` plus the migration of legacy states.

**Tech Stack:** Java 21, Swing, Maven (module `jipipe-core`), JUnit 5, Jackson.

**Spec:** `docs/superpowers/specs/2026-11-15-pinned-docks-design.md` (read it first; this plan argues from it).

## Global Constraints

- Run tests from the repo root, headless: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='<TestClass>'` (comma-separate classes to combine).
- No em dashes (U+2014) or en dashes (U+2013) in code, comments, docs, or user-facing strings. Plain hyphens only.
- Never hardcode text sizes. The pin indicator is a font derivation (labels mode) or a compound border (icon mode) only.
- Dock IDs are stable string literals. Reference the Tools dock as `JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL` (literal `"CONTEXT_PANEL"`).
- Test policy: no tests for Swing structure or rendering (the user verifies the UI in the running app). Logic tests are headless and construct real panels, like `JIPipeDesktopDockPanelStateTest`.
- Commit style: conventional commits scoped to the work item, e.g. `feat(#1367): ...`.
- Branch `1367-pinned-docks` off `master`. Open an MR at the end; do NOT merge - the user verifies the UI in the running app first.
- New test files start with the MIT copyright header copied verbatim from `JIPipeDesktopDockPanelStateTest.java`.

## Review Focus

Failure modes the spec implies but unit tests cannot fully exercise; each is pinned by the named test in its owning task.

1. Two dock panels in one window (graph editor + data viewer): pinning in one must never produce a stand-in in the other. Test: `pinsArePerPanelInstance` (Task 2).
2. A legacy layout template (pre-pin JSON, `always-show-right-panel-v2: true`, no pins) must restore with the Tools dock pinned, not silently lose the stand-in. Tests: `migratedPinsToolsDockForLegacyAlwaysShowRight` (Task 1) plus the template wiring step (Task 5).
3. The run managers restore an in-memory snapshot after a run; a stand-in pause must not survive that restore or the side stays collapsed. Test: `restoreStateClearsStandInPauses` (Task 4).
4. Unpinning a dock the user has opened must not close it (pin and visibility are orthogonal). Test: `unpinningDoesNotCloseAnOpenDock` (Task 2).
5. A hand-edited settings document that pins a context dock ID (`_RESULTS`) must display nothing and survive the next save. Test: `contextDockPinsAreInertButRoundTrip` (Task 4).

---

### Task 1: Pin model in `State` (`PinSide`, `pins`, `migrated()`)

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelStateTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces (later tasks rely on these exact names):
  - `enum PinSide { Left, Right, Bottom }` (nested in `JIPipeDesktopDockPanel`)
  - `PanelLocation.getPinSide()` returning `PinSide` (switch expression: TopLeft/BottomLeft -> Left, TopRight/BottomRight -> Right, BottomBottom -> Bottom)
  - `State.getPins()` / `State.setPins(Map<String, String>)` - JSON key `"pins"`, map keys are the lowercase side names `"left"`, `"right"`, `"bottom"`, values are dock IDs; `setPins(null)` stores an empty map
  - `State.migrated(String legacyRightStandInDockId)` - pure, idempotent, returns a copy
  - private static helper `pinKey(PinSide)` returning `side.name().toLowerCase(Locale.ROOT)` (used by State and the panel)

- [ ] **Step 1: Write the failing tests**

Append to `JIPipeDesktopDockPanelStateTest` (imports `JsonUtils` already exist there):

```java
@Test void migratedPinsToolsDockForLegacyAlwaysShowRight() {
    JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
    state.setAlwaysShowRightPanel(true);
    assertEquals("CONTEXT_PANEL", state.migrated("CONTEXT_PANEL").getPins().get("right"));
}

@Test void migratedAddsNoPinWithoutLegacyFlag() {
    assertTrue(new JIPipeDesktopDockPanel.State().migrated("CONTEXT_PANEL").getPins().isEmpty());
}

@Test void migratedKeepsExistingPins() {
    JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
    state.setAlwaysShowRightPanel(true);
    state.getPins().put("right", "FIND");
    assertEquals("FIND", state.migrated("CONTEXT_PANEL").getPins().get("right"));
}

@Test void migratedIsIdempotent() {
    JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
    state.setAlwaysShowRightPanel(true);
    state.put("MAP", true, JIPipeDesktopDockPanel.PanelLocation.TopLeft);
    String once = JsonUtils.toJsonString(state.migrated("CONTEXT_PANEL"));
    String twice = JsonUtils.toJsonString(state.migrated("CONTEXT_PANEL").migrated("CONTEXT_PANEL"));
    assertEquals(once, twice);
}

@Test void migratedIgnoresLegacyLeftFlag() {
    JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
    state.setAlwaysShowLeftPanel(true);
    assertTrue(state.migrated("CONTEXT_PANEL").getPins().isEmpty());
}

@Test void pinsSurviveJsonRoundTrip() {
    JIPipeDesktopDockPanel.State state = new JIPipeDesktopDockPanel.State();
    state.getPins().put("left", "FIND");
    state.getPins().put("bottom", "LOG");
    JIPipeDesktopDockPanel.State reloaded =
            JsonUtils.readFromString(JsonUtils.toJsonString(state), JIPipeDesktopDockPanel.State.class);
    assertEquals("FIND", reloaded.getPins().get("left"));
    assertEquals("LOG", reloaded.getPins().get("bottom"));
}
```

Also extend the existing fixtures: in `poisonedState()` add `state.getPins().put("right", "CONTEXT_PANEL");`, and in `sanitizingDropsContextDockVisibilitiesButKeepsEverythingElse` add `assertEquals("CONTEXT_PANEL", sanitized.getPins().get("right"));` plus a context-dock-pin case in `sanitizingDoesNotMutateTheOriginal` (`assertTrue(original.getPins().containsKey("right"))`).

- [ ] **Step 2: Run tests to verify they fail**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelStateTest'`
Expected: COMPILATION ERROR (`migrated`, `getPins` not found).

- [ ] **Step 3: Implement in `JIPipeDesktopDockPanel.java`**

- Nested `enum PinSide { Left, Right, Bottom }` after `PanelSide`; instance method `getPinSide()` on `PanelLocation` as a Java 21 switch expression.
- `State` gains `private Map<String, String> pins = new HashMap<>();` with `@JsonGetter("pins")`/`@JsonSetter("pins")` accessors; `setPins` maps null to an empty map.
- `State.migrated(String legacyRightStandInDockId)`: copy every field (including the legacy always-show flags and pins - the flags must survive so a second `migrated()` is a no-op), then: if `alwaysShowRightPanel && legacyRightStandInDockId != null && !pins.containsKey("right")`, put `("right", legacyRightStandInDockId)`. `alwaysShowLeftPanel` never produces a pin. Do not mutate `this`.
- `withoutContextDockVisibilities()`: also copy `pins` (`copy.pins.putAll(pins)`).
- private static `pinKey(PinSide)` helper on the outer class (the nested `State` can call it).

- [ ] **Step 4: Run tests to verify they pass**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelStateTest,DockStateStalePanelIdsTest'`
Expected: PASS (all, including pre-existing tests).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelStateTest.java
git commit -m "feat(#1367): dock state pin field with legacy migration"
```

---

### Task 2: Panel pin API, persistence, move clears pin

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java`
- Create: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java`

**Interfaces:**
- Consumes: `PinSide`, `PanelLocation.getPinSide()`, `pinKey`, `State.getPins()` (Task 1).
- Produces:
  - `void pinDock(String id)` - no-op for unknown or `_`-prefixed IDs; pins at the dock's current location's side, replacing that side's previous pin; idempotent; triggers `updateAll()` + `saveState()`
  - `void unpinDock(String id)` - removes the pin if present; no-op otherwise; update + save only when something was removed
  - `boolean isDockPinned(String id)` - true when `id` is the stored pin of any side
  - `String getPinnedDockId(PinSide side)` - the stored pin ID or null
  - runtime field `Map<String, String> pins` (lowercase side keys, same shape as `State.pins`; unknown side names from a document round-trip)

- [ ] **Step 1: Write the failing tests**

Create `JIPipeDesktopDockPanelPinTest.java` (MIT header from `JIPipeDesktopDockPanelStateTest`):

```java
package org.hkijena.jipipe.utils.ui;

import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class JIPipeDesktopDockPanelPinTest {

    private void addPanel(JIPipeDesktopDockPanel panel, String id, JIPipeDesktopDockPanel.PanelLocation location, boolean visible) {
        panel.addDockPanel(id, id, null, location, visible, 0, new JPanel());
    }

    @Test void pinningReplacesTheSidesPreviousPin() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        addPanel(panel, "ERRORS", JIPipeDesktopDockPanel.PanelLocation.BottomRight, false);
        panel.pinDock("TOOLS");
        assertEquals("TOOLS", panel.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
        panel.pinDock("ERRORS");
        assertEquals("ERRORS", panel.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
        assertFalse(panel.isDockPinned("TOOLS"));
    }

    @Test void pinsOnDifferentSidesCoexist() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "FIND", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("FIND");
        panel.pinDock("TOOLS");
        assertTrue(panel.isDockPinned("FIND"));
        assertTrue(panel.isDockPinned("TOOLS"));
    }

    @Test void pinningAContextDockIsANoop() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "_PARAMETERS", JIPipeDesktopDockPanel.PanelLocation.TopRight, true);
        panel.pinDock("_PARAMETERS");
        assertFalse(panel.isDockPinned("_PARAMETERS"));
    }

    @Test void pinningAnUnknownDockIsANoop() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        panel.pinDock("NOPE");
        assertFalse(panel.isDockPinned("NOPE"));
    }

    @Test void movingClearsThePin() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.movePanelToLocation("TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        assertFalse(panel.isDockPinned("TOOLS"));
    }

    @Test void unpinningDoesNotCloseAnOpenDock() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        panel.activatePanel("TOOLS", false);
        panel.unpinDock("TOOLS");
        assertFalse(panel.isDockPinned("TOOLS"));
        assertTrue(panel.getPanels().get("TOOLS").isVisible(), "unpinning must not close a user-opened dock");
    }

    @Test void pinsArePerPanelInstance() {
        JIPipeDesktopDockPanel first = new JIPipeDesktopDockPanel();
        JIPipeDesktopDockPanel second = new JIPipeDesktopDockPanel();
        addPanel(first, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        addPanel(second, "FIND", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
        first.pinDock("TOOLS");
        second.pinDock("FIND");
        assertNull(second.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
        assertNull(first.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Left));
    }

    @Test void pinsRoundTripThroughThePanel() {
        JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
        addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        panel.pinDock("TOOLS");
        JIPipeDesktopDockPanel reloaded = new JIPipeDesktopDockPanel();
        addPanel(reloaded, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
        reloaded.restoreState(panel.getCurrentState());
        assertEquals("TOOLS", reloaded.getPinnedDockId(JIPipeDesktopDockPanel.PinSide.Right));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest'`
Expected: COMPILATION ERROR (`pinDock` not found).

- [ ] **Step 3: Implement in `JIPipeDesktopDockPanel.java`**

- Field: `private final Map<String, String> pins = new HashMap<>();`
- `pinDock`: resolve the panel; skip when null, `_`-prefixed, or already the side's pin; `pins.put(pinKey(panel.getLocation().getPinSide()), id)`; then `updateAll(); saveState();`
- `unpinDock`: find the side key whose value equals the ID; if none, return; remove it, then `updateAll(); saveState();`
- `isDockPinned`: `pins.containsValue(id)`
- `getPinnedDockId`: `pins.get(pinKey(side))`
- `getCurrentState()`: `state.getPins().putAll(pins);`
- `restoreState(State)`: `pins.clear(); pins.putAll(state.getPins());` before `updateAll()`
- `movePanelToLocation(Panel, ...)`: inside the location-changed branch, remove the moved dock's pin (remove by value) so any move clears it

- [ ] **Step 4: Run tests to verify they pass**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest,JIPipeDesktopDockPanelStateTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java
git commit -m "feat(#1367): dock pin API with state persistence"
```

---

### Task 3: Stand-in display

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java`

**Interfaces:**
- Consumes: pin API (Task 2).
- Produces:
  - `boolean isDockDisplayed(String id)` - true when the dock is user-visible OR is the active stand-in of its side (this is the toggle contract #1346 builds on)
  - `String getStandInPanelId(PinSide side)` - the dock ID currently displayed as stand-in, or null
  - private `computeStandIns()` filling `Map<PinSide, Panel> activeStandIns`, called at the top of `updateAll()`, cleared and skipped when `hideToolbars` is true

- [ ] **Step 1: Write the failing tests**

Append to `JIPipeDesktopDockPanelPinTest`:

```java
@Test void pinnedDockBecomesTheSideStandIn() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    assertTrue(panel.isDockDisplayed("TOOLS"));
    assertFalse(panel.getPanels().get("TOOLS").isVisible(), "the stand-in must not flip the visible flag");
}

@Test void theStandInYieldsToAUserOpenedDock() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    addPanel(panel, "_SLOT_MANAGER", JIPipeDesktopDockPanel.PanelLocation.TopRight, true);
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    panel.removeDockPanelsIf(id -> id.startsWith("_"));
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void theStandInRespectsHideToolbars() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    panel.setHideToolbars(true);
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    panel.setHideToolbars(false);
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void theStandInIsNotPersisted() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    assertFalse(panel.getCurrentState().getVisibilities().getOrDefault("TOOLS", false));
    assertEquals("TOOLS", panel.getCurrentState().getPins().get("right"));
}

@Test void aPinForAnUnknownDockShowsNothing() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    JIPipeDesktopDockPanel.State state = panel.getCurrentState();
    state.getPins().put("right", "NOPE");
    panel.restoreState(state);
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void theLeftAndBottomSidesHaveStandIns() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "FIND", JIPipeDesktopDockPanel.PanelLocation.TopLeft, false);
    addPanel(panel, "LOG", JIPipeDesktopDockPanel.PanelLocation.BottomBottom, false);
    panel.pinDock("FIND");
    panel.pinDock("LOG");
    assertEquals("FIND", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Left));
    assertEquals("LOG", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Bottom));
}

@Test void removingAndReaddingThePinnedDockSelfHeals() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    panel.removeDockPanelsIf(p -> p.getId().equals("TOOLS"));
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest'`
Expected: COMPILATION ERROR (`getStandInPanelId` not found).

- [ ] **Step 3: Implement in `JIPipeDesktopDockPanel.java`**

- Field: `private final Map<PinSide, Panel> activeStandIns = new EnumMap<>(PinSide.class);`
- `computeStandIns()` (private): clear the map; return early when `hideToolbars`; per side, skip when any panel with that `getPinSide()` is displayed; otherwise resolve `pins.get(pinKey(side))`, skip null or `_`-prefixed IDs, and put the registered panel.
- `updateAll()`: call `computeStandIns()` first (before `updateToolbars()`).
- `isDockDisplayed(String id)` / `getStandInPanelId(PinSide)` as declared.
- `updateToolbars()`: `button.setSelected(isDockDisplayed(panel.getId()));` instead of `panel.isDisplayed()`.
- `updateContent(...)`: in the four left/right location loops accept a panel when `panel.isDisplayed() || panel == activeStandIns.get(sideOfThatLoop)`; after them, when the side's content list is empty and the side has a stand-in, instantiate its component exactly like `activatePanel` does (use the supplier, then null it) and add it; guard the old always-show fallback block with `activeStandIns.get(...) == null` so the mechanisms cannot both add a panel (the block is deleted in Task 6); in the bottom-panel selection loop accept `panel.isVisible() || panel == activeStandIns.get(PinSide.Bottom)`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest,JIPipeDesktopDockPanelStateTest,DockStateStalePanelIdsTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java
git commit -m "feat(#1367): pinned docks activate as side stand-ins"
```

---

### Task 4: Stand-in pause

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java`

**Interfaces:**
- Consumes: stand-in plan (Task 3), pin API (Task 2).
- Produces: runtime `EnumSet<PinSide> pausedStandIns`; no public API. Rules (spec section 5):
  - Set in `deactivatePanel(Panel, boolean)` when the dock is its side's pin AND no OTHER panel on that side is displayed.
  - Cleared in `activatePanel` (panel's side), both `addDockPanel` overloads (effective location's side), `pinDock`/`unpinDock` (affected side), `movePanelToLocation` (old and new side), `removeDockPanel`/`removeDockPanelsIf`/`removeAllPanels` (sides of removed panels), `restoreState` (all).

- [ ] **Step 1: Write the failing tests**

Append to `JIPipeDesktopDockPanelPinTest`:

```java
@Test void closingTheStandInPausesTheSide() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    panel.deactivatePanel("TOOLS", false);
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void aContextDockCycleReArmsTheStandIn() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    panel.deactivatePanel("TOOLS", false);
    addPanel(panel, "_PARAMETERS", JIPipeDesktopDockPanel.PanelLocation.TopRight, true);
    panel.removeDockPanelsIf(id -> id.startsWith("_"));
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void closingAUserOpenedPinnedDockPausesTheSide() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    panel.activatePanel("TOOLS", false);
    panel.deactivatePanel("TOOLS", false);
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void closingANonPinnedDockDoesNotPauseTheSide() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    addPanel(panel, "ERRORS", JIPipeDesktopDockPanel.PanelLocation.BottomRight, true);
    panel.pinDock("TOOLS");
    panel.deactivatePanel("ERRORS", false);
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void restoreStateClearsStandInPauses() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    panel.pinDock("TOOLS");
    panel.deactivatePanel("TOOLS", false);
    panel.restoreState(panel.getCurrentState());
    assertEquals("TOOLS", panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
}

@Test void contextDockPinsAreInertButRoundTrip() {
    JIPipeDesktopDockPanel panel = new JIPipeDesktopDockPanel();
    addPanel(panel, "TOOLS", JIPipeDesktopDockPanel.PanelLocation.TopRight, false);
    JIPipeDesktopDockPanel.State state = panel.getCurrentState();
    state.getPins().put("right", "_RESULTS");
    panel.restoreState(state);
    assertNull(panel.getStandInPanelId(JIPipeDesktopDockPanel.PinSide.Right));
    assertEquals("_RESULTS", panel.getCurrentState().getPins().get("right"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest'`
Expected: FAIL - the pause is not implemented, so `closingTheStandInPausesTheSide` and `closingAUserOpenedPinnedDockPausesTheSide` see the stand-in again; `contextDockPinsAreInertButRoundTrip` sees a stand-in for `_RESULTS`.

- [ ] **Step 3: Implement in `JIPipeDesktopDockPanel.java`**

- Field: `private final EnumSet<PinSide> pausedStandIns = EnumSet.noneOf(PinSide.class);`
- `computeStandIns()`: skip a side whose pause is set.
- `deactivatePanel(Panel, boolean)`: before `panel.setVisible(false)`, when the panel is its side's pin and no other panel on that side is displayed, add the side to `pausedStandIns`.
- All the clear sites listed under Interfaces. In `addDockPanel`, clear after the panel is registered (the restored location decides the side). In `removeDockPanelsIf`/`removeAllPanels`, collect the sides of removed panels first, then clear those.
- `computeStandIns` already skips `_`-prefixed pins (Task 3), which makes context-dock pins inert; the round-trip works because `restoreState` copies `State.pins` verbatim and `getCurrentState` writes the runtime map back.

- [ ] **Step 4: Run tests to verify they pass**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest,JIPipeDesktopDockPanelStateTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanelPinTest.java
git commit -m "feat(#1367): stand-in pauses while the user closed the pinned dock"
```

---

### Task 5: Migration wiring in the editors

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (apply path at `applyPipelineDockState`, `restoreDefaultDockState` at the end of the file)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (apply path at `applyCompartmentsDockState` around line 400, `restoreDefaultDockState` around line 444)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (`restoreDockStateTemplate` around line 561)

**Interfaces:**
- Consumes: `State.migrated(String)` (Task 1), `State.getPins()` (Task 1), `JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL`.
- Produces: legacy states and legacy templates migrate to the Tools pin at every persisted-state entry point; the flavors' "restore default layout" pins the Tools dock.

- [ ] **Step 1: Apply-path migration**

In both `applyPipelineDockState` and `applyCompartmentsDockState`, replace `state = state.withoutContextDockVisibilities();` with:

```java
state = state.migrated(JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL).withoutContextDockVisibilities();
```

(The JSON equality comparison below it then compares like-for-like, so no re-apply loop.)

- [ ] **Step 2: Template migration**

In `restoreDockStateTemplate`, replace `getDockPanel().restoreState(state.withoutContextDockVisibilities());` with:

```java
getDockPanel().restoreState(state.migrated(JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL).withoutContextDockVisibilities());
```

- [ ] **Step 3: Default-layout pins**

In both flavors' `restoreDefaultDockState()`, add before `getDockPanel().restoreState(state);`:

```java
state.getPins().put("right", JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL);
```

- [ ] **Step 4: Verify**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest,JIPipeDesktopDockPanelStateTest,SettingsKeysTest'`
Expected: PASS (no behavior change for fresh states; the wiring is covered by the Task 1 migration tests plus compile-time checks).

- [ ] **Step 5: Commit**

```bash
git add jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/
git commit -m "feat(#1367): migrate legacy dock states and templates to the Tools pin"
```

---

### Task 6: Remove the always-show/fallback mechanism

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (constructor lines ~191-192, `initialize()` lines ~410-411)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java` (`defaultDockState()` at the end)
- Modify: `jipipe-core/src/test/java/org/hkijena/jipipe/plugins/settings/SettingsKeysTest.java` (test at lines 148-163)

**Interfaces:**
- Consumes: everything from Tasks 1-5.
- Produces: `alwaysShowLeftPanel`/`alwaysShowRightPanel`/`fallbackLeft`/`fallbackRight` are gone from the panel; `State` keeps the legacy fields as write-only (deserialize old documents, never serialize); the default dock state pins the Tools dock.

- [ ] **Step 1: Update `SettingsKeysTest` (failing test first)**

Replace the test at lines 148-163 with:

```java
/**
 * The dock state default pins the Tools dock on the right, so a fresh
 * profile shows the pinned dock before any selection.
 */
@Test
void pipelineAndCompartmentsDockStateDefaultsPinTheToolsDockOnTheRight(@TempDir Path userDir) {
    JIPipeSettingsServiceComponent settings = JIPipeSettingsServiceComponent.forTesting(userDir);
    try {
        assertEquals("CONTEXT_PANEL", SettingsKeys.PIPELINE_DOCK_STATE.defaultValue().getPins().get("right"));
        assertEquals("CONTEXT_PANEL", SettingsKeys.COMPARTMENTS_DOCK_STATE.defaultValue().getPins().get("right"));
        assertFalse(settings.bind(SettingsKeys.PIPELINE_DOCK_STATE).isModified());
    } finally {
        settings.dispose();
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='SettingsKeysTest'`
Expected: FAIL (the default state has no pins yet).

- [ ] **Step 3: Remove the mechanism**

- `JIPipeDesktopDockPanel`: delete the panel fields `alwaysShowLeftPanel`, `alwaysShowRightPanel`, `fallbackLeft`, `fallbackRight`; their getters/setters; the "Always show right panel"/"Always show left panel" items in `reloadContextMenu`; the whole fallback block in `updateContent` (including the Task 3 bypass guard); the `state.setAlwaysShow*` lines in `getCurrentState()` and the `alwaysShow*Panel = state.*` lines in `restoreState()`.
- `State`: remove `@JsonGetter("always-show-left-panel")` and `@JsonGetter("always-show-right-panel-v2")`, add `@JsonIgnore` to `isAlwaysShowLeftPanel()`/`isAlwaysShowRightPanel()`, and keep the setters with their `@JsonSetter` annotations (write-only property: old documents load, nothing serializes the flags again; `migrated()` keeps reading them, which is what makes it idempotent). Remove the `alwaysShow*` copy lines from `withoutContextDockVisibilities()`.
- `JIPipeDesktopGraphEditorUI`: delete the `// Always show right panel` comment + `dockPanel.setAlwaysShowRightPanel(true);` in the constructor and the `dockPanel.setFallbackRight(...)`/`setFallbackLeft(...)` lines in `initialize()`. The Tools dock registration itself stays.
- `SettingsKeys.defaultDockState()`: replace `state.setAlwaysShowRightPanel(true);` with `state.getPins().put("right", JIPipeDesktopGraphEditorUI.DOCK_CONTEXT_PANEL);` (add the import).

- [ ] **Step 4: Verify no references remain**

Run: `grep -rn "setAlwaysShow\|setFallback\|getFallback\|isAlwaysShow" --include='*.java' jipipe-core/src plugins/ jipipe-desktop/ jipipe-cli/`
Expected: only the `State` getters (with `@JsonIgnore`) that `migrated()` reads, and their setters. Nothing outside `JIPipeDesktopDockPanel.java` references them except the `SettingsKeys` import of `DOCK_CONTEXT_PANEL`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='SettingsKeysTest,JIPipeDesktopDockPanelPinTest,JIPipeDesktopDockPanelStateTest,DockStateStalePanelIdsTest'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(#1367): replace always-show dock fallback with pinned docks"
```

---

### Task 7: Pin menu entry, pin indicator, constant rename

**Files:**
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/utils/ui/JIPipeDesktopDockPanel.java` (`createToggleButton`, `UI_ORDER_PINNED`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (line ~264 usage)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (line ~195 usage)

**Interfaces:**
- Consumes: `isDockPinned`, `pinDock`, `unpinDock` (Task 2).
- Produces: no API changes; `UI_ORDER_PINNED` renamed to `UI_ORDER_FIRST` (same value, -100; it means "listed first in the dock toolbar", not "pinned").

- [ ] **Step 1: Pin menu entry in `createToggleButton`**

Before the existing move entries in the popup menu, for non-context docks only:

```java
if (!panel.getId().startsWith("_")) {
    JCheckBoxMenuItem pinItem = new JCheckBoxMenuItem("Pin dock");
    pinItem.setToolTipText("Show this dock when its side would be empty");
    pinItem.setSelected(isDockPinned(panel.getId()));
    pinItem.addActionListener(e -> {
        if (pinItem.isSelected()) {
            pinDock(panel.getId());
        } else {
            unpinDock(panel.getId());
        }
    });
    popupMenu.add(pinItem);
}
```

- [ ] **Step 2: Pin indicator in `createToggleButton`**

While the dock is pinned (add `import java.awt.font.TextAttribute;` - `java.awt.*` does not cover it):

```java
boolean pinned = isDockPinned(panel.getId());
```

- Labels mode (inside the existing `if (showToolbarLabels)` block, after the font is set):

```java
if (pinned) {
    Map<TextAttribute, Object> attributes = new HashMap<>();
    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
    button.setFont(button.getFont().deriveFont(attributes));
}
```

- Icon-only mode: replace the empty border with a compound border when pinned, keeping the 8px padding:

```java
if (pinned) {
    button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeUtils.getCurrentStyle().getIconBaseColor(), 1),
            BorderFactory.createEmptyBorder(7, 7, 7, 7)));
} else {
    button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
}
```

Also change `button.setSelected(panel.isDisplayed())` to `button.setSelected(isDockDisplayed(panel.getId()));` if Task 3 did not already route it through `isDockDisplayed` (it did; keep it).

- [ ] **Step 3: Rename `UI_ORDER_PINNED` to `UI_ORDER_FIRST`**

Rename the constant and its two usages (Add-nodes registration in both flavors). Add a one-line comment on the constant: "UI order that lists a dock first in its dock toolbar".

- [ ] **Step 4: Verify**

Run: `env -u DISPLAY mvn -pl jipipe-core test -Dtest='JIPipeDesktopDockPanelPinTest'` and `grep -rn "UI_ORDER_PINNED" --include='*.java' .`
Expected: PASS; grep returns nothing.

No new tests: menu structure and border rendering are UI (verified by the user in the running app per the test policy).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(#1367): pin menu entry and pin indicator on dock toggles"
```

---

### Task 8: Full verification, push, MR

**Files:** none (verification only).

- [ ] **Step 1: Full module test suite**

Run: `env -u DISPLAY mvn -pl jipipe-core test`
Expected: BUILD SUCCESS, 0 failures, 0 errors (the suite has ~1155 tests plus the new ones; do not pin an exact total).

- [ ] **Step 2: Dash check on the diff**

Run: `git diff master..HEAD -- '*.java' '*.md' | grep -nP '[\x{2013}\x{2014}]'`
Expected: no output.

- [ ] **Step 3: Push and open the MR**

```bash
git push -u origin 1367-pinned-docks
```

Open the MR against `master`: title `Pinned docks`, description referencing work item #1367 and summarizing: per-side pinning with one pin per side, stand-in activation with pause, legacy state/template migration, Tools dock pinned by default, pin indicator. **Do not merge; the user verifies the UI in the running app first.**
