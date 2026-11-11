# Floating map and top-left dock restructure - implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MR 1 of the navigator overhaul (#1355): move all left-dock panels to the top, remove the docked Navigator (minimap) and Bookmarks panels, and turn the map into a floating, movable, resizable, lockable panel over the canvas with layer toggles and persisted state.

**Architecture:** The map state is a new JSON value type persisted through the existing settings-service binding family (same mechanism as dock state). The floating map is a JLayeredPane overlay wrapping the editor's scroll pane, so it floats only over the canvas area. Its body reuses `JIPipeDesktopGraphCanvasPaintManager.paintMiniMap` with a new layer-mask parameter. The old docked `JIPipeDesktopGraphEditorMinimap` is deleted. The Find (#803) and Relations (#1358) panels are separate MRs.

**Tech Stack:** Java 21, Swing (JLayeredPane, JToolBar, JSplitPane-free), Jackson via `JIPipeSettingKey.object`, Maven module `jipipe-core`, JUnit 5 for pure-logic tests only.

**Spec:** `docs/superpowers/specs/2026-11-05-navigator-overhaul-design.md` (sections 1 and 2 are this MR; approved by R.Gerst). Prototypes: `docs/superpowers/mockups/2026-11-05-navigator-overhaul/01-navigator-dock-options.html` and `04-floating-map.html`.

**Branch:** continue on `1355-navigator-overhaul-design` (the spec + prototypes already live there and ride along with this MR per the spec's rollout section).

## Global Constraints

- Java 21, Maven; all code in the `jipipe-core` module.
- No em dashes or en dashes in code, Javadoc, comments, or user-facing strings (plain hyphens only).
- Registered IDs, dock panel IDs, and settings paths are stable string literals, never code-derived values.
- New settings keys: `APPLICATION` scope, block `registry:ui-graph-editor`, declared in `org.hkijena.jipipe.plugins.settings.SettingsKeys` next to `PIPELINE_DOCK_STATE`.
- Test policy (repo ruling): no Swing UI structure or interaction tests; tests cover pure logic and serialization only; the user verifies UI behavior by eye in the running app.
- Every tuning value (sizes, margins, opacity-independent constants of the map) is a named `static final` constant.
- Test command: `mvn -q test -pl jipipe-core -Dtest=<TestClass>` (jipipe-core has JUnit 5 + the ImageJ patcher `LauncherSessionListener` already registered; never add `--add-opens` or call `LegacyInjector.preinit()` yourself).
- Commits reference the work item: `(#1355)`.

## Review Focus

1. **Saved dock state referencing removed panel IDs.** A settings file written before this MR contains `MAP`/`BOOKMARKS` entries in the dock state. Expected: app starts, remaining layout is preserved, stale IDs ignored at restore. Owner: Task 1 (deserialization pin in `DockStateStalePanelIdsTest`; runtime restore verified manually per the manual checklist in Task 3).
2. **Saved dock layout template containing stale IDs.** A "Save layout" template string with `MAP`/`BOOKMARKS` entries restored from the layout menu. Expected: no crash, no resurrection of removed panels. Owner: Task 1 (same deserialization path as 1; manual check in Task 3).
3. **Map at the canvas edges while locked/unlocked.** Dragging or resizing the map to a canvas edge. Expected: clamped to the 8 px margin, never covering the editor toolbar (the overlay wraps only the scroll pane), move and resize blocked while locked, resize handle hidden while locked. Owner: Task 3 (manual).
4. **Viewport rectangle under canvas zoom.** Rect must be `visibleAmount / zoom` in graph coordinates at any zoom level; clicking the map pans the view with the correct delta (the old minimap's raw scroll-value math is wrong once zoom differs from 1 and is fixed here). Owner: Task 3 (manual).
5. **Concurrent editors / instances.** Two open editors of the same graph (or two app instances) with a live settings binding. Expected: moving the map in one applies in the other; the binding echo never yanks a map mid-drag (the panel skips incoming position applies while its own drag is active). Owner: Task 3 (manual).

---

### Task 1: Map state value type, settings keys, dock restructure

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopMapState.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopMapStateTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/utils/ui/DockStateStalePanelIdsTest.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/plugins/settings/SettingsKeys.java` (after `COMPARTMENTS_DOCK_STATE`, around line 94)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (`initializeDefaultPanels` lines 241-301, `restoreDefaultDockState` lines 563-582)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (`initializeDefaultPanels` lines 164-207, default-state builder lines 440-451)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (constants at lines 85-87)

**Interfaces:**
- Produces `JIPipeDesktopMapState` (consumed by Task 3):
  - `public static JIPipeDesktopMapState defaultState()`
  - `boolean isVisible()` / `void setVisible(boolean)`
  - `int getX()` / `int getY()` / `void setPosition(int x, int y)`
  - `int getWidth()` / `int getHeight()` / `void setSize(int w, int h)`
  - `boolean isLocked()` / `void setLocked(boolean)`
  - `boolean isLayerNodes()` / `isLayerEdges()` / `isLayerNotes()` with setters `setLayerNodes/Edges/Notes(boolean)`
  - `static final int MAP_DEFAULT_WIDTH = 280; static final int MAP_DEFAULT_HEIGHT = 200;`
  - JSON shape (exact, per spec section 2): `{"visible":true,"position":{"x":470,"y":16},"size":{"w":280,"h":200},"locked":false,"layers":{"nodes":true,"edges":true,"notes":true}}` via `@JsonGetter`/`@JsonSetter` (nested static classes `Position` with `x`,`y`; `Size` with `w`,`h`; `Layers` with `nodes`,`edges`,`notes`); missing fields fall back to the field's default value; implements `equals`/`hashCode`; constructor from a source state (copy).
- Produces `SettingsKeys.PIPELINE_MAP_STATE` and `SettingsKeys.COMPARTMENTS_MAP_STATE` (both `JIPipeSettingKey<JIPipeDesktopMapState>`, consumed by Task 3): sub-paths `pipeline/map-state` and `compartments/map-state`, block `registry:ui-graph-editor`, `APPLICATION` scope, default `JIPipeDesktopMapState.defaultState()`, names "Pipeline editor floating map" / "Compartments editor floating map".

- [ ] **Step 1: Write the failing tests**

`JIPipeDesktopMapStateTest`: round-trip (serialize a fully-populated state through `JIPipeJsonUtils`/Jackson mapper used by the settings service, deserialize, assert field equality including a non-default `locked=true`, `layers.nodes=false`); defaults (deserialize `{}` and every field equals the `defaultState()` value); stale dock state tolerance lives in the second file.

`DockStateStalePanelIdsTest`: deserialize a hand-written `JIPipeDesktopDockPanel.State` JSON that contains `"visibilities":{"MAP":true,"BOOKMARKS":true,"QUICK_GUIDE":true}` and `"locations":{"MAP":"TopLeft","BOOKMARKS":"BottomLeft"}` plus one valid entry; assert no exception, and that the maps contain the stale keys (they are just data; the runtime ignores unknown panel IDs). This pins the deserialization half of Review Focus 1 and 2.

Use the same Jackson `ObjectMapper` the settings service uses for these objects (the `JIPipeDesktopDockPanel.State` precedent at `JIPipeDesktopDockPanel.java:960-1090` shows the annotation style to mirror).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='JIPipeDesktopMapStateTest,DockStateStalePanelIdsTest'`
Expected: compile failure, `JIPipeDesktopMapState` does not exist yet.

- [ ] **Step 3: Implement `JIPipeDesktopMapState`**

Value class with the exact shape from the Interfaces block. Mirror the Jackson annotation style of `JIPipeDesktopDockPanel.State`. Field defaults: `visible=true`, position `0,0` (the panel applies its initial anchor itself when the stored position is `0,0` on a fresh state - Task 3), size `280x200` from the two constants, `locked=false`, all layers `true`.

- [ ] **Step 4: Add the two settings keys in `SettingsKeys`**

Copy the `PIPELINE_DOCK_STATE` declaration pattern (lines 78-93 of that file) with the new type and sub-paths from the Interfaces block.

- [ ] **Step 5: Restructure the docks in both flavors**

In `JIPipeDesktopPipelineGraphEditorUI.initializeDefaultPanels()` and `JIPipeDesktopCompartmentsGraphEditorUI.initializeDefaultPanels()`:
- Delete the `addDockPanel(DOCK_MAP, "Navigator", ...)` entry (pipeline lines 250-255, compartments lines 170-174).
- Delete the `addDockPanel(DOCK_BOOKMARKS, "Bookmarks", ...)` entry (pipeline lines 269-274, compartments lines 182-187).
- Change `PanelLocation.BottomLeft` to `PanelLocation.TopLeft` on the `DOCK_QUICK_GUIDE`, `DOCK_ADD_NODES`, and `DOCK_HISTORY` entries in both flavors. Leave `DOCK_LOG` (BottomBottom) and `DOCK_ERRORS` (BottomRight) untouched.
- In the default-state builders (`restoreDefaultDockState` pipeline lines 563-582; compartments builder lines 440-451): delete the `state.put(DOCK_MAP, ...)` and `state.put(DOCK_BOOKMARKS, ...)` lines; change `DOCK_QUICK_GUIDE`, `DOCK_ADD_NODES`, `DOCK_HISTORY` puts to `TopLeft`.
- In `JIPipeDesktopGraphEditorUI` (lines 85-87): first `grep -rn "DOCK_MAP\|DOCK_BOOKMARKS" jipipe-core/src/main/java` and confirm the only remaining references after the edits are the constant declarations; then delete the two constants. If `DOCK_BOOKMARKS` is referenced anywhere else (it was only used by the two flavors), keep it and say so in the commit message.
- The `JIPipeDesktopBookmarkListPanel` class itself stays (the project overview still uses it); only its dock registrations go.

- [ ] **Step 6: Run tests and compile**

Run: `mvn -q test -pl jipipe-core -Dtest='JIPipeDesktopMapStateTest,DockStateStalePanelIdsTest'`
Expected: PASS.
Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(dock): top-left dock stack, drop Navigator and Bookmarks docks, add map state setting (#1355)"
```

---

### Task 2: Layer mask for the minimap painter

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/canvas/MapLayer.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/canvas/managers/JIPipeDesktopGraphCanvasPaintManager.java` (`paintMiniMap` lines 202-217, `paintMinimapEdges` lines 219-232)

**Interfaces:**
- Produces `MapLayer` (enum, package `org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas`): constants `NODES`, `EDGES`, `NOTES`.
- Produces on `JIPipeDesktopGraphCanvasPaintManager`:
  - `public void paintMiniMap(Graphics2D graphics2D, double scale, int viewX, int viewY, Set<MapLayer> layers)`
  - The existing 4-arg `paintMiniMap` is kept as a delegate: `paintMiniMap(graphics2D, scale, viewX, viewY, EnumSet.allOf(MapLayer.class))`.

- [ ] **Step 1: Implement the enum and the overload**

Filtering rules inside the new overload:
- `EDGES` present -> `paintMinimapEdges(...)` as today.
- For each node UI: `nodeUI instanceof JIPipeDesktopAnnotationGraphNodeUI` -> requires `NOTES`; otherwise requires `NODES`. Skip nodes whose layer is absent.
No unit test (the policy exempts trivial if/else and Swing painting; the user verifies in the app in Task 3).

- [ ] **Step 2: Compile**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(map): layer mask parameter for the minimap painter (#1355)"
```

---

### Task 3: Floating map panel, editor overlay, toolbar toggle, settings wiring, minimap removal

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopFloatingMapPanel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (`initialize` lines 278-299, `initializeCommonToolbar` lines 352-373, add map-state binding helpers next to the dock-state abstracts at lines 190-192)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (binding field + `getMapStateKey` next to `pipelineDockStateBinding`, lines ~504-543)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (same, next to `compartmentsDockStateBinding`)
- Delete: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorMinimap.java` (+ its imports in both flavors)

**Interfaces:**
- Consumes: `JIPipeDesktopMapState` (Task 1), `SettingsKeys.PIPELINE_MAP_STATE` / `COMPARTMENTS_MAP_STATE` (Task 1), `paintMiniMap(Graphics2D, double, int, int, Set<MapLayer>)` (Task 2), `MapLayer` (Task 2), `canvasUI.getZoom()` (`JIPipeDesktopGraphCanvasUI:1674`), `graphEditorUI.getScrollPane()` (`JIPipeDesktopGraphEditorUI:253`), `JIPipe.RESOURCES.getIcon16("actions/map.png")` (icon exists).
- Produces `JIPipeDesktopFloatingMapPanel extends JIPipeDesktopWorkbenchPanel` (package `org.hkijena.jipipe.desktop.app.grapheditor.commons`), consumed by the editor base:
  - `public JIPipeDesktopFloatingMapPanel(JIPipeDesktopGraphEditorUI graphEditorUI)`
  - `public void applyState(JIPipeDesktopMapState state)` (idempotent; while a header/resize drag is in progress, skip position/size applies from external echoes)
  - `public JIPipeDesktopMapState getState()`
  - `public void setVisibleInCanvas(boolean visible)`
  - constants: `static final int MAP_MIN_WIDTH = 180; static final int MAP_MIN_HEIGHT = 130; static final int MAP_EDGE_MARGIN = 8; static final int MAP_DEFAULT_OFFSET = 16;`
- Produces on `JIPipeDesktopGraphEditorUI`:
  - `protected abstract JIPipeSettingKey<JIPipeDesktopMapState> getMapStateKey();` (mirrors `restoreDockStateFromSettings`/`saveDockStateToSettings` abstracts)
  - `public JIPipeDesktopFloatingMapPanel getFloatingMapPanel()`
  - a `JToggleButton` field `mapToggle` (created in `initializeCommonToolbar`, tooltip "Show or hide the floating map")
  - concrete `initializeMapStateFromSettings()` / `saveMapStateToSettings()` using the same binding pattern as `pipelineDockStateBinding`: bind on init (guard `JIPipe.isInstantiated()`), `applyState` the stored value, subscribe to live-apply, write through on changes; close the binding in `closeDockStateBinding()` alongside the dock binding.

- [ ] **Step 1: Build the panel chrome and map body**

`JIPipeDesktopFloatingMapPanel` layout (BorderLayout, opaque white-ish panel background, rounded border, drop shadow border as in the approved prototype 04):
- NORTH: a `JToolBar` (flat, `setFloatable(false)`) with: grip label (drag cursor region), title "Map", `JPopupMenu` button "Layers" opening a popup with three `JCheckBoxMenuItem`s bound to the state's `layers` booleans, `JToggleButton` "Lock" bound to `state.locked`, "Hide" button invoking the editor's hide (same path as the toolbar toggle). While locked: the header does not start drags, and the resize handle is removed.
- CENTER: an inner `JComponent` map body that paints: (a) background, (b) fit-to-content transform `scale = min(width / canvasW, height / canvasH)` centered (the old minimap's `refreshView` math from `JIPipeDesktopGraphEditorMinimap.java:114-138`), (c) `paintManager().paintMiniMap(g2, scale, viewX, viewY, stateLayers)`, (d) the viewport rectangle.
  Viewport rectangle math (zoom-corrected; the old minimap used raw scroll values and is wrong when `zoom != 1`): with `zoom = canvasUI.getZoom()`,
  - `scrollWidth = scale * hBar.getVisibleAmount() / zoom`, `scrollHeight = scale * vBar.getVisibleAmount() / zoom`
  - `scrollX = scale * hBar.getValue() / zoom`, `scrollY = scale * vBar.getValue() / zoom`
  - rectangle drawn at `viewX + scrollX, viewY + scrollY`.
  Clicking/dragging inside the body converts back: `gridX = (mapX - viewX) / scale * zoom` then `hBar.setValue(gridX - visibleAmountPx/2)` (center-on-point, the old `scrollTo` behavior adapted).
- SOUTHEAST corner: a 14 px resize handle (the only resize affordance, per prototype 04).
- The panel subscribes to the canvas updated event (`JIPipeDesktopGraphCanvasUIUpdatedEvent`, same emitter the old minimap used) and to both scroll-bar adjustments to repaint; it only repaints while visible (`isDisplayable()` guard, as in the old class).
- Repaints must be cheap: no `BufferedImage` caching of the graph image (the old minimap's `graphImage` field was vestigial; painting calls `paintMiniMap` directly, same as today's docked map).

- [ ] **Step 2: Overlay wiring in the editor base**

In `JIPipeDesktopGraphEditorUI.initialize()` replace lines 278-282:
```java
JLayeredPane mapOverlay = new JLayeredPane();
mapOverlay.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
JIPipeDesktopFloatingMapPanel mapPanel = new JIPipeDesktopFloatingMapPanel(this);
mapOverlay.add(mapPanel, JLayeredPane.DRAG_LAYER);
JPanel mainPanel = new JPanel(new BorderLayout());
mainPanel.add(mapOverlay, BorderLayout.CENTER);
mainPanel.add(toolBar, BorderLayout.NORTH);
```
(the toolbar stays outside the overlay so the map can never cover it; the overlay wraps only the scroll pane, per spec section 2). Keep `dockPanel.setMainComponent(mainPanel)` and everything after. Create `mapPanel` before `initializeCommonToolbar()` runs so the toggle (Step 3) can bind to it; store it in the `floatingMapPanel` field.

- [ ] **Step 3: Toolbar toggle and settings binding**

In `initializeCommonToolbar()` after `initializeCommonToolbarZoom()` (line 371): add `mapToggle` (icon `actions/map.png`, `UIUtils.makeButtonFlatWithSize(..., 32, 3)`, tooltip "Show or hide the floating map"), wired to `floatingMapPanel.setVisibleInCanvas(...)` and written through to the binding (`saveMapStateToSettings()` after each change).

In the two flavors, mirror the dock binding pattern exactly (pipeline: `pipelineDockStateBinding` field line 114, init/subscribe lines 504-512, write-through 536-543, close in `closeDockStateBinding` lines 546-549; compartments: the `compartmentsDockStateBinding` equivalent). Use the field type `SettingsBinding<JIPipeDesktopMapState>` (import `org.hkijena.jipipe.api.settings.SettingsBinding`), bind via `JIPipe.getSettingsService().bind(getMapStateKey())`, guard with `JIPipe.isInstantiated()`, `subscribe` for live-apply, and close in the flavor's `closeDockStateBinding()`. The base class implements `initializeMapStateFromSettings()` / `saveMapStateToSettings()` once. Save triggers: mouse release after a move/resize, lock toggle, layer toggle, visibility toggle (never per mouse-move).

Initial placement: when the applied state has position `0,0` (fresh state), anchor at `overlay.width - width - MAP_DEFAULT_OFFSET, MAP_DEFAULT_OFFSET`. On every move/resize, clamp to `[MAP_EDGE_MARGIN, overlay.width/height - size - MAP_EDGE_MARGIN]`.

- [ ] **Step 4: Delete the old minimap**

Delete `JIPipeDesktopGraphEditorMinimap.java` and its imports in both flavors (pipeline line 34, compartments line 24). Grep first: `grep -rn "GraphEditorMinimap" jipipe-core/src` must show only the class file itself before deletion.

- [ ] **Step 5: Compile and run the module tests**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS.
Run: `mvn -q test -pl jipipe-core`
Expected: PASS (no other code referenced the deleted class or the old dock entries).

- [ ] **Step 6: Manual verification checklist (the user runs the app)**

Build and start the desktop app (the normal dev launch, VM options per the repository guide). Verify, in order:
1. Fresh profile: map floats at the canvas top-right at 280x200, does not cover the toolbar; Quick guide, Add nodes, History sit in the top-left stack; no Navigator or Bookmarks dock tab; Map toggle in the toolbar.
2. Drag the map; resize from the bottom-right corner; both clamp at the canvas edge; nothing can be dragged over the toolbar.
3. Lock: header drag and resize disabled, resize handle hidden; unlock restores both.
4. Layers popup: uncheck "Canvas notes" and the notes disappear from the map only (the canvas itself is untouched); same for Nodes and Edges.
5. Zoom the canvas in/out with the toolbar zoom controls: the viewport rectangle shrinks/grows proportionally; clicking the map pans the view by the correct amount (Review Focus 4).
6. Stale state: in a scratch settings profile, pre-populate the pipeline dock state JSON with `"visibilities":{"MAP":true,"BOOKMARKS":true,...}` and a dock layout template containing `MAP`; start the app; the layout restores without the two removed panels (Review Focus 1 and 2).
7. Open a second pipeline editor tab of the same project (or a second app instance): move the map in one, it follows in the other; a mid-drag state write from the other side does not pull the map out from under the mouse (Review Focus 5).
8. Hide via the header button and restore via the toolbar toggle; both persist across a restart.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(map): floating, lockable map panel with layers and persisted state; drop docked minimap (#1355)"
```

---

## Post-MR handoff

- MR 1 closes #1355 (title: "Floating map and top-left dock restructure").
- MR 2 (#803, Find panel) and MR 3 (#1358, Relations panel) each get their own plan after MR 1 lands, built on the spec sections 3 and 4 and prototypes 03 and 02.
