# Node relations panel - implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MR 3 of the navigator overhaul (#1358): a "Relations" dock panel that shows the current selection centrally with its upstream (above) and downstream (below) nodes arranged in BFS layers, navigable by card click, with a back/forward visitation trail whose history is visualized as an opacity ramp on the cards.

**Architecture:** All graph logic is pure and headless: `RelationLayerCalculator` (BFS layering, side assignment, distance ordering, +N split) and `RelationTrail` (10-step back/forward trail, 5-step visual ramp), both unit tested. The UI is `JIPipeDesktopRelationsPanel` (both graph editor flavors, TopLeft) whose card component (`RelationCard`) paints a lightweight node representation - fill, framework pattern, border, header - directly from `JIPipeNodeInfo` + theme utils (no full `JIPipeDesktopGraphNodeUI` instances). The panel's only visual factor over that representation is a single opacity applied to the whole card.

**Tech Stack:** Java 21, Swing (JToolBar, JPanel cards, JScrollPane), jipipe-core, JUnit 5 (pure logic only).

**Spec:** `docs/superpowers/specs/2026-11-05-navigator-overhaul-design.md` section 4 (approved). Work item #1358. Prototype: `docs/superpowers/mockups/2026-11-05-navigator-overhaul/02-relations-panel.html` (rev 3: real taxonomy hues, opacity ramp legend).

**Branch:** `1358-relations-panel` off current master.

## Global Constraints

- Java 21, Maven; all code in the `jipipe-core` module.
- No em dashes or en dashes in code, Javadoc, comments, or user-facing strings (plain hyphens only).
- **Never assume text sizes - measure them (repo policy, CRITICAL):** no hardcoded widths/heights for text-bearing components; layout must measure (preferred sizes / FontMetrics). In reused paint code or row components, force synchronous re-layout after text changes (`invalidate()` + `validate()`).
- Registered IDs (dock panel IDs) are stable string literals.
- Test policy: no Swing UI structure or interaction tests; unit tests cover the pure logic only; the user verifies UI behavior in the running app.
- Every tuning value (opacity levels, row counts, trail length, card sizes) is a named `static final` constant.
- Test command: `mvn -q test -pl jipipe-core -Dtest=<TestClass>`.
- Commits reference the work item: `(#1358)`.

## Verified interfaces (use exactly these)

- Edges: `JIPipeGraph.getSlotEdges()` -> `Set<Map.Entry<JIPipeDataSlot, JIPipeDataSlot>>` (source = first, target = second); `JIPipeDataSlot.getNode()` -> owner node. Build the adjacency from this (do NOT call `getInputConnections()`/`getOutputConnections()` - they do not exist).
- Selection: `JIPipeDesktopGraphCanvasSelectionManager` (from `graphEditorUI.getCanvasUI().getSelectionManager()`): `getNodeSelectionChangedEventEmitter()` (the event carries only the `canvasUI` via `getCanvasUI()` - read the current selection from `getSelectionManager().getSelection()` -> `Set<JIPipeDesktopGraphInteractiveObjectUI>`, filtered to node UIs, when the event arrives) and `setSelection(Set<? extends JIPipeDesktopGraphInteractiveObjectUI>)`.
- Canvas nodes: `graphEditorUI.getCanvasUI().getNodeUIs()` -> `BiMap<JIPipeGraphNode, JIPipeDesktopGraphNodeUI>`; the node UI is a `JComponent` - use `ui.getX()`/`ui.getY()`/`ui.getWidth()`/`ui.getHeight()` (real canvas coordinates; `scrollToAlgorithm` uses the same accessors).
- Node appearance (card painting, lightweight - no node UI instance): `ThemeUtils.getNodeFillColor(JIPipeNodeInfo)`, `ThemeUtils.getNodeBorderColor(JIPipeNodeInfo)`; framework pattern via `NodePatternRenderer.paint(g2, x, y, w, h, info.getFramework().getPatternStyleId(), info.getFramework().getId(), style.getNodePatternInk(), style.getNodePatternOpacity())` gated exactly like `JIPipeDesktopGraphNodeUI.isFrameworkPatternEnabled` (theme `isShowNodeFrameworkPattern()` and framework != NoFramework and `NodePatternStyles.family(...)` != null); header = icon (`info.getIcon()`, fallback `JIPipe.RESOURCES.getIcon16("actions/tag.png")`) + name.
- Navigation: canvas select = `getSelectionManager().setSelection(Set.of(ui))` + `graphEditorUI.scrollToAlgorithm(ui)` (mirror the Find panel's `navigateTo`).
- Icon: `JIPipe.RESOURCES.getIcon24("actions/link.png")` (exists).
- Registration pattern: `getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_RELATIONS, "Relations", JIPipe.RESOURCES.getIcon24("actions/link.png"), JIPipeDesktopDockPanel.PanelLocation.TopLeft, false, 0, new JIPipeDesktopRelationsPanel(getDesktopWorkbench(), this));` in both flavors; the constant `DOCK_RELATIONS = "RELATIONS"` goes on the base editor class next to `DOCK_FIND`.

## Review Focus

1. **Selection change feedback loop.** Panel navigation sets the canvas selection, which fires the selection-changed event, which re-centers the panel - must not loop or churn. Expected: equality guard (skip re-center when the event's selection equals the panel's current centers), so canvas-driven re-centers happen but panel-driven events are ignored. Owner: Task 2.
2. **Multi-selection union.** Two or more selected nodes: all selected nodes appear in the center row; layers are the union of each center's BFS, with a node assigned to the side (up/down) of the SMALLER distance; ties break to upstream. Owner: Task 1 (pure, tested) + Task 2 (manual).
3. **Trail under back/forward.** Navigating to a related node appends to the trail (cap 10, forward stack cleared); back/forward move the trail index; the opacity ramp (100/85/80/75/70/65/60) must match the trail index, not the history length; going forward must re-render correctly. Owner: Task 1 (pure, tested) + Task 2 (manual).
4. **Large pipelines.** Layer computation is O(V+E) per selection change and runs only while the panel is visible; a 2000-node graph must not lag. Owner: Task 2 (manual).
5. **Card navigation on the canvas.** Clicking a card selects that single node on the canvas and recenters the panel to it. Owner: Task 2 (manual).

---

### Task 1: Pure model - RelationLayerCalculator, RelationTrail, and card appearance

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/RelationLayerCalculator.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/RelationTrail.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/RelationCardAppearance.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/RelationLayerCalculatorTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/RelationTrailTest.java`

**Interfaces:**
- Produces `RelationLayerCalculator` (package `org.hkijena.jipipe.desktop.app.grapheditor.relationspanel`):
  - Input record `NodePos(String id, double x, double y)` - canvas position (center).
  - `public record Layer(String id, double distanceToCenter)` - one node in a layer.
  - `public record Result(Set<String> centers, List<List<Layer>> upstream, List<List<Layer>> downstream)` - `upstream` ordered FURTHEST first (top of panel), `downstream` ordered NEAREST first (just below center); each inner list ordered by ascending physical distance to the nearest center.
  - `public static Result compute(Collection<String> centerIds, Map<String, NodePos> nodes, Map<String, List<String>> downstreamAdj, Map<String, List<String>> upstreamAdj)` - BFS from the union of centers over each direction; a node reachable in BOTH directions goes to the side with the smaller distance (ties -> upstream); the centers themselves are excluded from the layers; unknown ids in adjacency are ignored.
- Produces `RelationTrail`:
  - `public RelationTrail(int maxSteps)` (the panel passes `TRAIL_LENGTH`).
  - `public void navigateTo(String nodeId)` - appends to history, clears forward, caps at `maxSteps` (drops the oldest).
  - `public boolean canGoBack()` / `public boolean canGoForward()` / `public int stepCount()`.
  - `public String goBack()` / `public String goForward()` - moves the index and returns the newly-current node, or null when impossible.
  - `public String current()` - the node at the trail index (the latest visited when at the end).
  - `public int opacityRank(String nodeId)` - 0 for the current node, 1..5 for the five steps back (the visual ramp), and -1 for anything else (rendered at the related level).
- Produces `RelationCardAppearance` (pure, no Swing):
  - `public record Appearance(Color fill, Color border, boolean patternEnabled, String patternStyleId, String patternSeed, float patternOpacity, Color patternInk)` - everything the card's `paintComponent` needs, resolved from `JIPipeNodeInfo` + the current `JIPipeDesktopModernThemeStyle` using the EXACT gate and values of `JIPipeDesktopGraphNodeUI.paintNode` / `isFrameworkPatternEnabled` (see Verified interfaces).
  - `public static Appearance of(JIPipeNodeInfo info)` - resolves the appearance; `patternEnabled` is false for NoFramework or when the theme disables the pattern.

- [ ] **Step 1: Write the failing tests**

`RelationLayerCalculatorTest`: single-node center with upstream + downstream (one level each); multi-center union with a shared upstream node (distance = min over centers, side = smaller distance); a node reachable in both directions assigned to the nearer side and a tie assigned upstream; within-layer ordering by ascending physical distance (not BFS order); furthest-first / nearest-first level ordering; a center excluded from its own layers; unknown ids in adjacency ignored (no crash, no layer entry).

`RelationTrailTest`: navigateTo appends and clears forward; cap at maxSteps (the oldest is dropped, so back-steps beyond the cap are impossible); back/forward navigation returns the correct node sequence and stops at the ends (`canGoBack`/`canGoForward` false, methods return null); `opacityRank` is 0 for current, 1..5 for the five steps back in order, -1 for the current's neighbors and for anything beyond five steps.

`RelationCardAppearance` has no unit test (it is a thin resolver over theme + node info; the repo policy exempts it - the user verifies the card look in the app).

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='RelationLayerCalculatorTest,RelationTrailTest'`
Expected: compile failure (classes do not exist).

- [ ] **Step 3: Implement the three classes**

Plain Java. `RelationLayerCalculator` and `RelationTrail` use no Swing imports. `RelationCardAppearance` may import `java.awt.Color` and the node-info/theme types it resolves from (it returns values, not components).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='RelationLayerCalculatorTest,RelationTrailTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(relations): layer calculator, visitation trail and card appearance model (#1358)"
```

---

### Task 2: JIPipeDesktopRelationsPanel, card component, toolbar, dock registration

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/RelationCard.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/relationspanel/JIPipeDesktopRelationsPanel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (add `public static final String DOCK_RELATIONS = "RELATIONS";` next to `DOCK_FIND`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (`initializeDefaultPanels`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (`initializeDefaultPanels`)

**Interfaces:**
- Consumes: everything from Task 1; the verified interfaces above.
- Produces `JIPipeDesktopRelationsPanel extends JIPipeDesktopWorkbenchPanel`:
  - `public JIPipeDesktopRelationsPanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphEditorUI graphEditorUI)`
  - constants (the tuning knobs, all `static final`): `TRAIL_LENGTH = 10`, `RAMP_STEPS = 5`, `MAX_CARDS_PER_ROW = 4`, `OPACITY_CURRENT = 1.0f`, `OPACITY_TRAIL_FIRST = 0.85f`, `OPACITY_TRAIL_STEP = 0.05f`, `OPACITY_RELATED = 0.60f`, `CARD_MIN_WIDTH = 90`, `CARD_HEIGHT = 58` (card sizes are layout minimums, not text assumptions), `MORE_TILE_MIN_WIDTH = 44`.
  - The opacity of a card = `OPACITY_CURRENT` when its node is the current center, `OPACITY_TRAIL_FIRST - OPACITY_TRAIL_STEP * (rank - 1)` for `rank` 1..`RAMP_STEPS` from the trail, and `OPACITY_RELATED` otherwise. This is the ONLY visual factor the panel adds over `RelationCardAppearance`.
  - Behavior pins (spec section 4 + approved prototype):
    - Body: a scrollable panel of horizontal rows - upstream layers (furthest first), the center row (all selected nodes), downstream layers (nearest first). Each row lays out its cards left to right by ascending canvas distance; when a layer has more than `MAX_CARDS_PER_ROW` nodes, show the first `MAX_CARDS_PER_ROW` and a `+N` tile that expands the layer in place (the tile disappears once expanded; expanding does not reorder the cards).
    - `RelationCard` paints, for its node: the `RelationCardAppearance` fill over the whole card, the framework signature pattern over the body (via `NodePatternRenderer.paint` when `patternEnabled`), the border (1px, 2px for the current-center card), and a header band (icon + name, desaturated like the node's title band - measure the name with FontMetrics, never assume its width). No slots are drawn. Clicking a card navigates (single click).
    - Toolbar (JToolBar): back / forward (enabled by `trail.canGoBack()`/`canGoForward()`), a separator, up / down (jump to the nearest node in the adjacent layer by physical distance; disabled when the current center has no such layer or when multi-selected). Buttons are flat icon buttons like the graph editor toolbar.
    - Navigation: clicking a card or the up/down buttons = `trail.navigateTo(id)` + set the canvas selection to that single node + `scrollToAlgorithm`. This is the panel-driven side of the feedback loop.
    - Selection feedback loop guard: subscribe to `getNodeSelectionChangedEventEmitter()`. When the event's selection differs from the panel's current centers, re-center the panel to the event's selection (canvas-driven re-center). When it equals the current centers (the panel's own navigation just fired it), do nothing (no loop). Recompute layers only while `isShowing()`.
    - Empty state: no selection shows a placeholder ("Select a node to see its relations"); a selection with no relations shows a single center card and the "no related nodes" text.
  - Registration in both flavors exactly per the Verified interfaces (hidden by default, TopLeft, icon actions/link.png, name "Relations").

- [ ] **Step 1: Implement `RelationCard`**

A `JComponent` that holds a `String nodeId`, a `JIPipeNodeInfo`, a `RelationCardAppearance`, and its current opacity. `paintComponent` paints per the behavior pins. `getPreferredSize` derives the width from the MEASURED name (FontMetrics) clamped to `[CARD_MIN_WIDTH, ...]` - it must not assume a fixed text width. A `setOpacity(float)` repaints. The `+N` tile is a small sibling component (a `JButton`/`JPanel`) painted as a dashed-outline tile with the measured "N" - same measurement rule.

- [ ] **Step 2: Implement the panel + toolbar + rows + registration**

Follow the behavior pins. Build the adjacency once per rebuild from `graph.getSlotEdges()` (source `getNode()`, target `getNode()`), skip `JIPipeGraphCanvasNote`/annotation nodes, and resolve canvas positions from `getCanvasUI().getNodeUIs()`. Rebuild on the selection-changed event (guarded) and on graph-structure events (nodes/edges added or removed) while visible. Wire the back/forward/up/down buttons to the trail and navigation. Add `DOCK_RELATIONS` to the base class and register in both flavors.

- [ ] **Step 3: Compile and run the module tests**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS.
Run: `mvn -q test -pl jipipe-core`
Expected: PASS (including Task 1's tests).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(relations): Relations dock panel with BFS layers, trail and card rendering (#1358)"
```

- [ ] **Step 5: Manual verification checklist (the user runs the app)**

1. Both flavors show the Relations panel hidden by default; toggling shows an empty/placeholder state; selecting a node populates center + up + down rows.
2. Single node: upstream above, downstream below, each layer by distance; a node related in both directions appears on the nearer side.
3. Click a card: it becomes the center, is selected on the canvas, and the view scrolls to it (no re-center loop, the panel does not churn).
4. Back/forward: the trail moves through the last 10 visits; the opacity ramp (100 / 85 / 80 / 75 / 70 / 65, then 60) is visible on the visited cards; forward re-renders.
5. Multi-selection: all selected nodes are centers; shared neighbors appear once on the nearer side (Review Focus 2).
6. A layer with more than 4 nodes shows a "+N" tile that expands in place (Review Focus: the card row does not reflow out of order).
7. Large project: selecting and navigating stays responsive (Review Focus 4).
8. Up/down toolbar buttons jump to the nearest node in the adjacent layer; disabled when there is none or when multi-selected.
9. Cards look like canvas nodes (fill, framework pattern, border, header) but without slots; the current card has the thicker border.

---

## Post-MR handoff

- MR 3 closes #1358 (title: "Node relations panel").
- The Find panel (MR 2, #803) is the immediately preceding MR and already landed the TopLeft dock slot this panel uses.
- Deferred tuning: the exact opacity ramp and card sizes are the named constants; adjust in place if the user wants different values after the manual pass.
