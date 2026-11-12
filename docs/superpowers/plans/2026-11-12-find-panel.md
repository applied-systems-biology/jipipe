# Find panel - implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** MR 2 of the navigator overhaul (#803): a "Find" dock panel that lists every node of the project, grouped by canvas notes, searchable by name, description and UUID, with pin filtering replacing the removed Bookmarks panel.

**Architecture:** A new `JIPipeDesktopFindPanel` dock panel (both graph editor flavors, TopLeft) mirrors the Add-nodes panel's structure (toolbar + adaptive compact/comfortable tiers). Grouping is computed by a pure `NoteGroupingCalculator` from the live canvas state of the current compartment (node UI rectangles + canvas-note rectangles), so the same class is unit-testable headless. Cross-compartment nodes appear only as search results. Navigation reuses `JIPipeDesktopProjectWorkbench.getOrOpenPipelineEditorTab` + `scrollToAlgorithm`.

**Tech Stack:** Java 21, Swing (JTree, JSplitPane), jipipe-core, JUnit 5 (pure logic only).

**Spec:** `docs/superpowers/specs/2026-11-05-navigator-overhaul-design.md` section 3 (approved). Prototype: `docs/superpowers/mockups/2026-11-05-navigator-overhaul/03-find-panel.html` (approved; group order = note top-left position, right pane = "In group" members, no auto-expand).

**Branch:** `803-find-panel` off current master (master already carries MR 1 and the CI base image).

## Global Constraints

- Java 21, Maven; all code in the `jipipe-core` module.
- No em dashes or en dashes in code, Javadoc, comments, or user-facing strings (plain hyphens only).
- Registered IDs (dock panel IDs) are stable string literals.
- Test policy: no Swing UI structure or interaction tests; unit tests cover pure logic only; the user verifies UI behavior in the running app.
- Every tuning value is a named `static final` constant.
- Test command: `mvn -q test -pl jipipe-core -Dtest=<TestClass>`.
- Commits reference the work item: `(#803)`.

## Verified interfaces (use exactly these)

- Pins: `JIPipeGraphNode.isBookmarked()` / `setBookmarked(boolean)` (the flag persists in project files; the canvas context menu toggles it).
- Compartments: `JIPipeProject.getGraph()` -> `JIPipeGraph`; `JIPipeGraph.getGraphNodes()` -> `Set<JIPipeGraphNode>` (all compartments); `JIPipeProject.getCompartments()` -> `BiMap<UUID, JIPipeProjectCompartment>`; `JIPipeProjectCompartment.getName()`; `JIPipeGraph.getNodesWithinCompartment(UUID)`.
- Current editor state: `JIPipeDesktopGraphEditorUI.getCompartment()` (UUID, nullable in group flavor), `getCanvasUI().getNodeUIs()` -> `BiMap<JIPipeGraphNode, JIPipeDesktopGraphNodeUI>` (bounds in real canvas coordinates via `ui.getBounds()`), `getGraph()`.
- Notes: canvas notes are `JIPipeGraphCanvasNote` subclasses. Group-forming notes: `instanceof DefaultTextBoxGraphCanvasNote` (has `getTextTitle()`); colors from `TextBoxGraphCanvasNote.getShapeParameters()` (`getFillColor()` is `OptionalColorParameter`: `.isEnabled()`, `.getContent()`; `getBorderColor()` is `Color`). Non-textbox note types never form groups.
- Navigation: `JIPipeDesktopProjectWorkbench.getOrOpenPipelineEditorTab(JIPipeProjectCompartment, boolean switchToTab)`, `JIPipeDesktopProjectWorkbench.findOpenPipelineEditorTabs(UUID)`; `JIPipeDesktopGraphEditorUI.scrollToAlgorithm(JIPipeDesktopGraphNodeUI)`; selection via `editor.getSelectionManager().setSelection(Set<? extends JIPipeDesktopGraphInteractiveObjectUI>)`.
- Event mirroring: `JIPipeDesktopBookmarkListPanel` implements `JIPipeGraph.GraphChangedEventListener` and `JIPipeParameterCollection.ParameterChangedEventListener` - mirror its constructor subscriptions exactly (graph structure AND parameter changes such as bookmark toggles).
- Icon: `JIPipe.RESOURCES.getIcon24("actions/find.png")` (exists).

## Review Focus

1. **Stale grouping after node or note drags.** Node positions change without a graph-structure event in some paths. Expected: the tree re-groups when the user interacts (canvas updated events) while the panel is visible; a dragged note re-groups its members without reopening the panel. Owner: Task 2 (manual checklist item 3).
2. **Bookmark toggle while the panel is open.** Right-click a node on the canvas, toggle bookmark: the pin glyph in the Find tree must appear without reopening. Owner: Task 2 (mirrored parameter-event subscription; manual checklist item 4).
3. **Accidental navigation.** Single click must only select the entry (right pane updates); navigation happens on double-click only. Owner: Task 2 (manual checklist item 5).
4. **Large pipelines.** A 2000-node project must not lag the panel: tree rebuild is O(n), runs only while the panel is visible, and event storms coalesce through Swing's own repaint coalescing. Owner: Task 2 (manual checklist item 6).
5. **Cross-compartment navigation.** Double-clicking a search result from another compartment opens (or focuses) that compartment's editor tab, then selects and scrolls. Owner: Task 2 (manual checklist item 7).

---

### Task 1: Pure model - FindEntry and NoteGroupingCalculator

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/findpanel/FindEntry.java`
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/findpanel/NoteGroupingCalculator.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/findpanel/FindEntryTest.java`
- Test: `jipipe-core/src/test/java/org/hkijena/jipipe/desktop/app/grapheditor/findpanel/NoteGroupingCalculatorTest.java`

**Interfaces:**
- Produces `FindEntry` (package `org.hkijena.jipipe.desktop.app.grapheditor.findpanel`), consumed by Task 2:
  - `public FindEntry(UUID nodeId, String name, String description, String uuidString, UUID compartmentId, String compartmentName, boolean bookmarked)`
  - accessors: `getNodeId()`, `getName()`, `getDescription()`, `getUuidString()`, `getCompartmentId()`, `getCompartmentName()`, `isBookmarked()`
  - `public boolean matches(String query)` - case-insensitive `contains` on name, description and uuidString; `null`/empty query matches everything.
- Produces `NoteGroupingCalculator`:
  - `public record NodeRef(String id, Rectangle rect, FindEntry entry)` - rect in real canvas coordinates
  - `public record NoteRef(String id, Rectangle rect, String title, Color fill, Color border)`
  - `public record Group(String noteId, String title, Color fill, Color border, List<NodeRef> nodes)`
  - `public static List<Group> group(Collection<NodeRef> nodes, Collection<NoteRef> notes)` - only notes with a non-blank title form groups; a note claims a node when the node rect's CENTER lies inside the note rect (constant `GROUP_CLAIM = "center"`, documented); a node can be claimed by several notes and appears in each; groups are ordered by note rect top-left position (y, then x); within a group, nodes are ordered by name (case-insensitive)
  - `public static List<NodeRef> ungrouped(Collection<NodeRef> nodes, Collection<NoteRef> notes)` - nodes claimed by no titled note, ordered by name

- [ ] **Step 1: Write the failing tests**

`NoteGroupingCalculatorTest` covers: center-containment claim (node center inside -> claimed; center outside but rect overlapping -> NOT claimed); multi-membership (node under two overlapping notes appears in both groups); ordering (note at y=200 before note at y=100 is second; tie on y broken by x); blank/empty note title is ignored entirely (its nodes are ungrouped); ungrouped correctness. `FindEntryTest` covers: name match, description match, uuid match, case-insensitivity, null/empty query matches all, non-match.

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q test -pl jipipe-core -Dtest='NoteGroupingCalculatorTest,FindEntryTest'`
Expected: compile failure (classes do not exist).

- [ ] **Step 3: Implement the two classes**

Plain Java, no Swing imports except `java.awt.Rectangle`/`Color`. Constants named (`GROUP_CLAIM` as a documented constant).

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q test -pl jipipe-core -Dtest='NoteGroupingCalculatorTest,FindEntryTest'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(find): note grouping and search model for the Find panel (#803)"
```

---

### Task 2: JIPipeDesktopFindPanel + dock registration

**Files:**
- Create: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/findpanel/JIPipeDesktopFindPanel.java`
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/commons/JIPipeDesktopGraphEditorUI.java` (add `public static final String DOCK_FIND = "FIND";` next to the other dock constants)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/pipeline/JIPipeDesktopPipelineGraphEditorUI.java` (`initializeDefaultPanels`)
- Modify: `jipipe-core/src/main/java/org/hkijena/jipipe/desktop/app/grapheditor/flavors/compartments/JIPipeDesktopCompartmentsGraphEditorUI.java` (`initializeDefaultPanels`)

**Interfaces:**
- Consumes: everything from Task 1; the verified interfaces above.
- Produces `JIPipeDesktopFindPanel extends JIPipeDesktopWorkbenchPanel`:
  - `public JIPipeDesktopFindPanel(JIPipeDesktopWorkbench workbench, JIPipeDesktopGraphEditorUI graphEditorUI)`
  - constants: `static final int COMPACT_TIER_WIDTH = 380;` (mirrors the Add-nodes threshold), `static final Color UNGROUPED_FILL = new Color(0xe8e8e8);`, `static final Color UNGROUPED_BORDER = new Color(0xc4c4c4);`
  - behavior pins (from spec section 3 and the approved prototype):
    - toolbar: search `JTextField` (placeholder "Search by name, description or UUID"), clear button, "Pinned only" toggle button
    - compact tier (panel width < `COMPACT_TIER_WIDTH`): tree only; comfortable tier: `JSplitPane(HORIZONTAL_SPLIT, treeScroll, inGroupScroll)`, `setResizeWeight(0.35)`, right pane titled "In group" listing the members of the group containing the current selection (ungrouped members when the selection is ungrouped); tier re-evaluated on `addComponentListener` resize, idempotent like `JIPipeDesktopNodeBrowser.applyViewIfNeeded()`
    - tree: groups as non-selectable folder rows (colored square from note fill/border, title, member count) + node leaves (name, pin glyph when `isBookmarked()`, UUID fragment, compartment badge for cross-compartment results); "Ungrouped" group last with the neutral colors; while a search is active, one group per other compartment (compartment name as title, purple badge styling per prototype) containing the matching `FindEntry`s of that compartment, appended after the current-compartment groups; canvas notes (`JIPipeGraphCanvasNote` instances) are excluded from all results
    - node enumeration (flavor-agnostic, do NOT use `getCompartment()` - it is null in the compartments and group flavors): current-canvas entries come from `graphEditorUI.getCanvasUI().getNodeUIs()` keys minus `JIPipeGraphCanvasNote` instances (the canvas shows exactly the current compartment's nodes in the pipeline flavor, the compartment graph in the compartments flavor); cross-compartment results = `project.getGraph().getGraphNodes()` minus the current-canvas node set, mapped to `FindEntry`s with their compartment name from `project.getCompartments()`; node rects for grouping come from the node UIs' `getBounds()` (real canvas coordinates), note rects likewise from their UIs
    - single click on a node leaf: select it in the tree and update the "In group" pane; double-click: navigate (`JIPipeDesktopGraphEditorUI` same compartment: `getCanvasUI().getNodeUIs().get(node)` -> `setSelection` + `scrollToAlgorithm`; other compartment: `getDesktopProjectWorkbench().getOrOpenPipelineEditorTab(compartment, true)` then select + scroll on the returned editor)
    - rebuild triggers: graph changed events, node parameter changes (bookmark toggles), and canvas updated events - subscribe exactly like `JIPipeDesktopBookmarkListPanel` does in its constructor; rebuild only while `isShowing()`
  - registration in both flavors: `getDockPanel().addDockPanel(JIPipeDesktopGraphEditorUI.DOCK_FIND, "Find", JIPipe.RESOURCES.getIcon24("actions/find.png"), JIPipeDesktopDockPanel.PanelLocation.TopLeft, false, 0, new JIPipeDesktopFindPanel(getDesktopWorkbench(), this));` (hidden by default like Add nodes - vetoable at review)

- [ ] **Step 1: Implement the panel and registration**

Follow the pins above; mirror `JIPipeDesktopNodeBrowser` for toolbar/tier structure and `JIPipeDesktopBookmarkListPanel` for event subscriptions. No new settings, no migration.

- [ ] **Step 2: Compile and run the module tests**

Run: `mvn -q compile -pl jipipe-core`
Expected: BUILD SUCCESS.
Run: `mvn -q test -pl jipipe-core`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(find): Find dock panel with note grouping, UUID search and pin filter (#803)"
```

- [ ] **Step 4: Manual verification checklist (the user runs the app)**

1. Both flavors show the Find panel hidden by default; toggling shows tree + search.
2. Groups mirror the canvas: note titles, colors from note fill/border, member counts; ungrouped last.
3. Drag a node into/out of a note: regroups while the panel is visible (Review Focus 1).
4. Bookmark a node from the canvas context menu: pin glyph appears in the tree (Review Focus 2).
5. Single click selects only; double-click navigates and selects on canvas (Review Focus 3).
6. Large project: typing in the search stays responsive (Review Focus 4).
7. Search a node that exists only in another compartment: compartment group appears; double-click opens that compartment's tab and selects the node (Review Focus 5).
8. Paste a full UUID into the search: exactly one result.
9. "Pinned only" filters the whole tree including cross-compartment results.
