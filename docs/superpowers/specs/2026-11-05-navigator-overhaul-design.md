# Navigator overhaul - design

Date: 2026-11-05
Work items: #1355 (floating map + dock restructure), #803 (Find panel), #1358 (node relations panel)
Prototypes: `docs/superpowers/mockups/2026-11-05-navigator-overhaul/01..04*.html` (approved by R.Gerst)

The navigator area is rebuilt from a single map dock panel into three navigation surfaces:
a floating map over the canvas, a project-wide "Find" panel, and a selection-driven
"Relations" panel. The left dock moves to the top. Work is split into three sequential,
independently shippable MRs, one per work item.

## Goals

- Navigation in large pipelines: find nodes (by name, description, UUID), trace relations
  of a selection back and forth, and keep an overview map available without dock space.
- Canvas notes become a first-class grouping mechanism for organizing and finding nodes.
- All tuning values are named constants so they can be adjusted later without refactoring.

## Non-goals

- No changes to bookmark data structures in project files (only the dock panel goes away).
- No new relation types beyond graph edges (data-trace style relations stay where they are).
- No per-project persistence of map state (application settings, like dock state).

## 1. Dock restructure (MR 1, #1355)

In both graph editor flavors (pipeline, compartments) in
`JIPipeDesktopPipelineGraphEditorUI.initializeDefaultPanels()` /
`JIPipeDesktopCompartmentsGraphEditorUI.initializeDefaultPanels()`:

- Quick guide, Add nodes, History move from `BottomLeft` to `TopLeft` (Calculator and the
  navigator slot are already there).
- The `DOCK_MAP` "Navigator" panel (the docked minimap) is removed.
- The `DOCK_BOOKMARKS` "Bookmarks" panel is removed. Its role (pinning nodes) moves to the
  Find panel (section 3). Bookmark project data and the bookmark context-menu actions stay.
- Dock state persistence is unchanged (`SettingsKeys.PIPELINE_DOCK_STATE` and the
  compartments equivalent via the settings service binding). Saved states that reference the
  removed panel IDs are tolerated: panels are matched by ID, unknown IDs are ignored on
  restore. Users who saved a dock layout keep it; only fresh layouts get the new default.

## 2. Floating map (MR 1, #1355)

The minimap becomes a floating panel hovering over the canvas (ComfyUI-style).

### Component structure

- New `JIPipeDesktopFloatingMapPanel` (package
  `org.hkijena.jipipe.desktop.app.grapheditor.commons`): chrome (header: grip, layers,
  lock, hide) + map body + resize handle, placed on a `JLayeredPane` that wraps the editor's
  scroll pane (drag layer), so it only overlays the canvas area and moves with it.
- The map body reuses the existing minimap rendering: `JIPipeDesktopGraphCanvasPaintManager
  .paintMiniMap(...)` stays the single source of the simplified graph image; the docked
  `JIPipeDesktopGraphEditorMinimap` is dissolved into the new panel (its viewport-rect and
  scrollTo logic moves over).

### Behavior

- Show/hide via a toggle button in the graph editor toolbar (next to the zoom controls),
  and via the hide button in the map header. Same visibility semantics as dock panels.
- Movable by dragging the header; resizable from the bottom-right corner handle.
  Both are blocked while locked; the resize handle is hidden while locked.
- Lockable in place via the header lock button.
- Viewport rectangle is bidirectionally live: canvas pan/zoom updates the rectangle;
  dragging inside the map body pans the view (center-on-point, as today).
- The map honors canvas zoom: the rectangle is `viewport size / zoom` in graph coordinates.
- Layer visibility toggles in a header dropdown: Nodes, Edges, Canvas notes.
  Implemented as a layer mask parameter on `paintMiniMap` (enum set), so plugins and future
  layers can extend it. Canvas notes are currently painted through
  `JIPipeDesktopAnnotationGraphNodeUI.paintMinimap`; the mask filters by node kind.

### Persistence

One JSON settings value per editor flavor, in the same binding family as dock state
(new `SettingsKeys.PIPELINE_MAP_STATE` / `COMPARTMENT_MAP_STATE`, cross-editor synced,
instance-merged like dock state):

```json
{
  "visible": true,
  "position": { "x": 470, "y": 16 },
  "size": { "w": 280, "h": 200 },
  "locked": false,
  "layers": { "nodes": true, "edges": true, "notes": true }
}
```

Applied on editor creation, saved on change (on mouse release, not per pixel).

### Tuning constants (static finals)

`MAP_DEFAULT_WIDTH/HEIGHT`, `MAP_MIN_WIDTH/HEIGHT`, `MAP_DEFAULT_OFFSET` (initial anchor
offset from the canvas top-right), `MAP_EDGE_MARGIN` (clamp margin, 8 px).

## 3. Find panel (MR 2, #803)

New `JIPipeDesktopFindPanel` (package
`org.hkijena.jipipe.desktop.app.grapheditor.findpanel`), registered at `TopLeft` in both
flavors, icon `actions/find.png`, panel name "Find". Replaces the Bookmarks panel.

### Layout

Reuses the Add nodes adaptive-tier mechanism (`NodeBrowserModel.getTier(width)` pattern,
same `TIER_COMPACT` width threshold constant):

- Compact (narrow dock): group tree only.
- Comfortable (wide dock): group tree | "In group" member list side by side
  (`JSplitPane`, resize weight 0.35). The right pane lists the members of the group that
  contains the current selection; groups are not auto-expanded.

### Tree model

Built per query over all compartments of the project:

- Canvas notes form groups: a note claims a node when the node center is inside the note
  bounds (claim rule = center containment, constant `GROUP_CLAIM`). Group name = note title
  (`DefaultTextBoxGraphCanvasNote.getTextTitle()`, empty title = no group), group icon =
  the note fill/border colors.
- A node claimed by several notes appears under each (multi-membership).
- Nodes claimed by no note are listed under an "Ungrouped" root group.
- Group order follows the notes' top-left position on the canvas (y, then x; reading order).
- While a search is active, nodes of other compartments are appended as an extra group per
  compartment with a compartment badge; they are not part of the tree when not searching.
- Search matches name, description and node UUID (UUID paste = direct hit). Search
  results are filtered lists without inline match highlighting.
- Pins: pinned node entries show a pin glyph; a "Pinned only" toolbar toggle filters the
  whole tree. Pin storage follows the existing bookmark storage (unchanged data model).

### Interaction

- Double-click (single click selects the entry) navigates: if the node is in another
  compartment, the compartment editor is opened first, then `scrollToAlgorithm` + select.
- Rebuild triggers: graph change events (nodes/notes added/moved/removed), debounced
  through the existing canvas event emitters. The "In group" pane follows the tree
  selection instead of canvas selection change events.

### Pure logic for tests

`NoteGroupingCalculator` (input: node rects + note rects/titles/colors, output: ordered
groups with members) is separated from the UI and unit tested (multi-membership, ordering,
empty-title exclusion, ungrouped fallback).

## 4. Node relations panel (MR 3, #1358)

New `JIPipeDesktopRelationsPanel` (package
`org.hkijena.jipipe.desktop.app.grapheditor.relationspanel`), registered at `TopLeft` in
both flavors, icon `actions/link.png`, panel name "Relations".

### Model

- Input: current canvas selection (single node or multiple nodes).
- Layers: BFS over directed edges from the selection. Upstream levels stack above the
  center row (furthest first), downstream levels below (nearest first). A node related in
  both directions is assigned to the side with the smaller distance.
- Within a layer, nodes are ordered by physical distance of their canvas positions to the
  selection (ascending). Only the first `MAX_CARDS_PER_ROW = 4` are shown; a "+N" tile
  expands the layer in place.
- No edges are drawn between rows.
- Relations are computed within the current compartment graph (tunnel IO nodes are the
  boundary, as on the canvas).

### Rendering

- Cards render like canvas nodes without slots: fill color
  (`ThemeUtils.getNodeFillColor(info)`), framework signature pattern
  (`NodePatternRenderer` with the node framework's pattern id), border
  (`getNodeBorderColor`), header with icon + name. A lightweight card component paints
  these directly; full `JIPipeDesktopGraphNodeUI` instances are not created.
- The panel adds exactly one visual factor, opacity (single source of truth, applied to the
  whole card): current 100%, related 60%, visitation trail 85/80/75/70/65% for one to five
  steps back; older trail members render as related (60%).
- The body has no scrollbars: the focused center row is pinned to the vertical middle,
  rows stack with a fixed row pitch, and rows beyond the body height collapse into
  "N more above"/"N more below" edge tiles that page the visible window (Refocus
  re-centers it). Rows fill the full body width (leftover width distributed
  proportionally to the cards' preferred widths, tiles keep their measured width), and
  full-width horizontal guide lines in the theme border color structure the empty lanes.

### Navigation

- The panel distinguishes the FOCUSED node (the displayed layers are built around it) from
  the CURRENT node (the visitation trail's current / selected node). Click on a card,
  back/forward and up/down navigate: they select the node on the canvas (`SelectionManager`
  + `scrollToAlgorithm`), update the current node and repaint the opacity ramp and current
  border; the row layout does not re-position.
- Panel toolbar (JToolBar): back/forward through the visitation trail (session-scoped,
  length `TRAIL_LENGTH = 10`), up/down jumps to the nearest node in the adjacent layer of
  the focused layout (by physical distance), and "Refocus" (`actions/target.png`) rebuilds
  the layers around the current node (focused := current), the only action that
  re-positions the rows. Buttons disable when the step is impossible.
- The panel subscribes to canvas selection-change events; they update the current node
  (single selections append to the trail) without re-positioning the rows, and an empty
  panel populates around the selection. Navigating from the panel sets the canvas
  selection (feedback loop guarded by an equality check to avoid churn).

### Pure logic for tests

`RelationLayerCalculator` (BFS layering, side assignment, ordering) and `TrailRamp`
(rank to opacity mapping) are separated from the UI and unit tested. The 10-step trail with
a 5-step visual ramp and the reachability of back-steps are covered.

## 5. Settings and migration

- New settings keys only for the map state (section 2). No project file format changes.
- Removed dock panels: no migration needed (ID-tolerant restore).
- The Bookmarks removal must not orphan `JIPipeDesktopBookmarkListPanel` usages outside the
  graph editor (project overview keeps its bookmark UI).

## 6. Testing

Per the repository test policy (no Swing structure/interaction tests; the user verifies UI
by eye):

- `NoteGroupingCalculatorTest`: claim rule, multi-membership, ordering, empty titles.
- `RelationLayerCalculatorTest`: layering, side assignment, distance ordering, +N split.
- `TrailRampTest`: rank-to-opacity mapping, cap at 10, older-than-5 fallback.
- Map settings round-trip test (JSON, defaults for missing fields).
- Existing taxonomy/registry tests stay green (presence-based assertions unaffected).

## 7. Rollout

Three sequential MRs, each shippable alone, one branch per work item:

1. #1355: dock restructure + floating map (+ this spec and the prototypes).
2. #803: Find panel (depends on 1 for the removed Bookmarks panel and the free TopLeft slot).
3. #1358: Relations panel (independent of 2, depends on 1 only for dock placement).

MR titles carry the work item reference (`(#1355)` etc.).

## 8. Risks

- Floating map must not swallow canvas mouse events outside its bounds (only chrome/body
  consume events); verified manually.
- Relations panel rebuild cost on selection change in very large pipelines: BFS is
  O(V+E) per selection, acceptable; rebuild is deferred to the event thread and skipped
  when the panel is not visible.
- Find panel rebuild on note drag: graph change events fire continuously during drags;
  debounce with the existing canvas update coalescing before wiring the tree model.
